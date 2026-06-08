package com.zlt.aps.mp.adjust.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;

import com.alibaba.fastjson.JSONArray;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ruoyi.api.gateway.system.domain.ImportErrorLog;
import com.ruoyi.api.gateway.system.domain.ImportLog;
import com.ruoyi.api.gateway.system.service.ISysDictDataCacheService;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.core.domain.SysDictData;
import com.ruoyi.common.core.utils.DateUtils;
import com.ruoyi.common.core.utils.poi.ExcelUtil;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.text.Convert;
import com.ruoyi.common.utils.StringUtils;
import com.zlt.aps.common.core.constant.ApsConstant;
import com.zlt.aps.common.core.utils.AjaxResultUtils;
import com.zlt.aps.constant.FactoryConstant;
import com.zlt.aps.enums.YesOrNoEnum;
import com.zlt.aps.mp.adjust.mapper.MpAdjustResultEntityMapper;
import com.zlt.aps.mp.adjust.service.IMpAdjustResultService;
import com.zlt.aps.mp.api.domain.entity.FactoryMonthPlanMouldDayResult;
import com.zlt.aps.mp.api.domain.entity.MpAdjustResult;
import com.zlt.aps.mp.engine.adjust.MpWeekRollAdjustEngine;
import com.zlt.aps.mp.factory.dto.MpStructureAllocationExportVo;
import com.zlt.aps.mp.factory.dto.MpStructureAllocationImportHelper;
import com.zlt.aps.mp.factory.service.IMpStructureAllocationService;
import com.zlt.bill.common.service.AbstractDocService;
import com.zlt.common.utils.PubUtil;
import com.zlt.sysdef.domain.SysDocType;
import lombok.extern.slf4j.Slf4j;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.zlt.aps.common.core.utils.ApsNumberUtils.*;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：MpAdjustResultServiceImpl.java
 * 描    述：MpAdjustResultServiceImpl调整-调整结果记录业务层处理
 *@author zlt
 *@date 2025-12-19
 *@version 1.0
 *
 *  修改记录：
 *     修改时间：...
 *     修 改 人：zlt
 *     修改内容：...
 */
@Slf4j
@Service
@Transactional(rollbackFor = Exception.class)
public class MpAdjustResultServiceImpl extends AbstractDocService<MpAdjustResult>  implements IMpAdjustResultService {

    @Autowired
    protected MpAdjustResultEntityMapper mpAdjustResultEntityMapper;

    @Autowired
    private ISysDictDataCacheService sysDictDataCacheService;

    @Autowired
    private MpWeekAdjustFactory mpWeekAdjustFactory;
    
    @Autowired
    private IMpStructureAllocationService mpStructureAllocationService;
    /**
     * 导入页签名称，仅加载一次
     */
    private static String sheetName = null;
    private static String sheetName4DayResult= null;

    @Override
    protected String getDocTypeCode() {
        return "MP0804";
    }

    @Override
    protected SysDocType getSysDocType() {
        SysDocType sysDocType = new SysDocType();
        sysDocType.setDocTypeCode("MP0804");
        return sysDocType;
    }

    @Override
    public String checkUnique(MpAdjustResult docEntityVO) {
        String unique = super.checkUnique(docEntityVO);
        if (UserConstants.NOT_UNIQUE.equals(unique)) {
            throw new ServiceException(I18nUtil.getMessage("ui.data.alert.mpAdjustResult.notUnique"));
        }
        return unique;
    }

    @Override
    protected List<String> getCheckUniqueFields() {
        // 唯一校验字段
        return Collections.emptyList();
    }

    @Override
    public void forceUpdateById(MpAdjustResult entity) {
        // 根据版本号+物料编号+施工阶段查询，如果没有，则新增，否则更新
        String adjVersion = StrUtil.isBlank(entity.getVersion()) ? entity.getProductionVersion() : entity.getVersion();

        LambdaQueryWrapper<MpAdjustResult> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(MpAdjustResult::getFactoryCode, entity.getFactoryCode());
        queryWrapper.eq(MpAdjustResult::getVersion, adjVersion);
        queryWrapper.eq(MpAdjustResult::getMaterialCode, entity.getMaterialCode());
        queryWrapper.eq(MpAdjustResult::getConstructionStage, entity.getConstructionStage());
        List<MpAdjustResult> mpAdjustResultList = mpAdjustResultEntityMapper.selectList(queryWrapper);

        //1、更新开始和结束日期
        String dayField;
        int realBeginDay = FactoryConstant.MONTH_MAX_DAY+1;
        int realEndDay = 0;
        int accTotalQty = 0;
        for (int i = FactoryConstant.MONTH_START_DAY; i <= FactoryConstant.MONTH_MAX_DAY; i++){
            dayField = FactoryConstant.DAY_FIELD + i;
            if (entity.getFieldValueByFieldName(dayField) != null &&
                    (Integer) entity.getFieldValueByFieldName(dayField) != 0){
                if (realBeginDay > i){
                    realBeginDay = i;
                }
                if (realEndDay < i){
                    realEndDay = i;
                }

                accTotalQty += (Integer) entity.getFieldValueByFieldName(dayField);
            }
        }
        entity.setBeginDay(realBeginDay==FactoryConstant.MONTH_MAX_DAY+1 ? 0:realBeginDay);
        entity.setEndDay(realEndDay);
         //实际调整量 = 累计排产量 - 原实际排产量
        int oriTotalQty = entity.getTotalQty()== null ? 0:entity.getTotalQty();
        entity.setAdjustFlag(oriTotalQty != accTotalQty ? YesOrNoEnum.YES.getCode() : YesOrNoEnum.NO.getCode());
        entity.setTotalQty(accTotalQty);

        if (StrUtil.isNotBlank(entity.getVersion())) {
            entity.setVersion(adjVersion);
            entity.setLastMonthPlanVersion(adjVersion);
        }
        if (StrUtil.isBlank(entity.getAdjustType())){
            LambdaQueryWrapper<MpAdjustResult> queryWrapper2 = new LambdaQueryWrapper<>();
            queryWrapper2.eq(MpAdjustResult::getFactoryCode, entity.getFactoryCode());
            queryWrapper2.eq(MpAdjustResult::getVersion, adjVersion);
            List<MpAdjustResult> mpAdjustResultList2 = mpAdjustResultEntityMapper.selectList(queryWrapper2);
            if (PubUtil.isNotEmpty(mpAdjustResultList2)){
                entity.setAdjustType(mpAdjustResultList2.get(0).getAdjustType());
            }else{
                entity.setAdjustType(ApsConstant.APS_ZERO_3);
            }
        }
        // 计算各排产量
        MpWeekRollAdjustEngine weekRollAdjustEngine = new MpWeekRollAdjustEngine();
        weekRollAdjustEngine.allocateProductionByPriority(entity);
        // 没有数据，需新增
        if (CollUtil.isEmpty(mpAdjustResultList)) {
            entity.setId(null);
            mpAdjustResultEntityMapper.insert(entity);
        } else {
            //2、更新每日调整值
            entity.setId(mpAdjustResultList.get(0).getId());
            mpAdjustResultEntityMapper.forceUpdateById(entity);
        }
    }

    @Override
    public void deleteAdjustResultByVersion(String factoryCode, String year, String month, String version,String structureName) {
        mpAdjustResultEntityMapper.deleteAdjustResultByVersion(factoryCode,year,month,version,structureName);
    }

    /**
     * 数据导入
     * @param fileBytes
     * @param importLog
     * @return
     */
    @Override
    public AjaxResult importData(byte[] fileBytes, ImportLog importLog) {
        ExcelUtil<MpStructureAllocationExportVo> util = new ExcelUtil<>(MpStructureAllocationExportVo.class);
        ExcelUtil<FactoryMonthPlanMouldDayResult> util4DayResult = new ExcelUtil<>(FactoryMonthPlanMouldDayResult.class);
        // 工厂名称字典
        List<SysDictData> factoryDatas = Optional.ofNullable(sysDictDataCacheService.getType("biz_factory_name")).orElse(Collections.emptyList());
        Map<String, String> factoryMap = factoryDatas.stream()
                .filter(Objects::nonNull)
                .filter(v -> v.getDictLabel() != null && v.getDictValue() != null)
                .collect(Collectors.toMap(SysDictData::getDictLabel, SysDictData::getDictValue, (a, b) -> a));

        List<SysDictData> productTypeList = Optional.ofNullable(sysDictDataCacheService.getType("biz_product_type")).orElse(Collections.emptyList());
        Map<String, String> productTypeMap = productTypeList.stream()
                .filter(Objects::nonNull)
                .filter(v -> v.getDictLabel() != null && v.getDictValue() != null)
                .collect(Collectors.toMap(SysDictData::getDictLabel, SysDictData::getDictValue, (a, b) -> a));
        
        // 解析Excel文件
        MpStructureAllocationImportHelper helper = this.parseExcel(fileBytes);
        if (AjaxResultUtils.checkAjaxError(helper.getAjaxResult())) {
            return helper.getAjaxResult();
        }
        String[] params = helper.getParams();
        String[] params4DayResult = helper.getParams4DayResult();
        String monthPlanVersion = helper.getMonthPlanVersion();
        String productVersion = helper.getProductVersion();
        
        List<FactoryMonthPlanMouldDayResult> list4DayResult;
        List<MpStructureAllocationExportVo> list;
        try {
            list4DayResult = util4DayResult.importExcel(sheetName4DayResult, new ByteArrayInputStream(fileBytes), 3, 1, -1);
            list = util.importExcel(sheetName, new ByteArrayInputStream(fileBytes), 2, 2, 13);
        } catch (Exception e) {
            log.warn("importDataStructureAllocation workbook parse failed", e);
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.mpStructureAllocation.import.templateError"));
        }
        
        // 结构转产导入
        AjaxResult ajaxResult = mpStructureAllocationService.importDataStructureAllocation(list, list4DayResult, true, importLog.getId(), params, monthPlanVersion, productVersion, factoryMap, productTypeMap);

        // 月计划调整排产导入
        AjaxResult ajaxResult4DayResult = mpStructureAllocationService.importDataDayResult(list, list4DayResult, true, importLog.getId(), params4DayResult, monthPlanVersion, productVersion, factoryMap, productTypeMap, true);

        // 处理返回结果，统一
        int errorNum = 0;
        int successNum = 0;
        List<Object> importErrorLogs = new ArrayList<>();
        int[] resultParam = parseImportMsg(ajaxResult);
        if (resultParam[2] > 0) {
            List<ImportErrorLog> importErrorLogList = StringUtils.cast(ajaxResult.get(AjaxResult.DATA_TAG));
            if (CollectionUtils.isNotEmpty(importErrorLogList)) {
                String listTxt = JSONArray.toJSONString(importErrorLogList);
                importErrorLogs.addAll(JSONArray.parseArray(listTxt, ImportErrorLog.class));
            }
        }
        int[] resultParam4DayResult = parseImportMsg(ajaxResult4DayResult);
        successNum += resultParam4DayResult[0];
        if (resultParam4DayResult[2] > 0) {
            errorNum += resultParam4DayResult[1];

            List<ImportErrorLog> importErrorLogList = StringUtils.cast(ajaxResult4DayResult.get(AjaxResult.DATA_TAG));
            if (CollectionUtils.isNotEmpty(importErrorLogList)) {
                String listTxt = JSONArray.toJSONString(importErrorLogList);
                importErrorLogs.addAll(JSONArray.parseArray(listTxt, ImportErrorLog.class));
            }
        }
        
        // 构建返回数据
        Integer rowCount = list4DayResult.size();
        Map<String, Object> returnData = new HashMap<>();
        returnData.put("rowCount", rowCount);
        returnData.put("errorNum", errorNum);
        returnData.put("successNum", successNum);
        returnData.put("importErrorLogs", importErrorLogs);
        return AjaxResult.success(returnData);
    }
    
    /**
     * 解析导入Excel文件
     * @param fileBytes 导入文件字节数组
     * @return
     */
    private MpStructureAllocationImportHelper parseExcel(byte[] fileBytes) {
        MpStructureAllocationImportHelper helper = new MpStructureAllocationImportHelper();
        helper.setAjaxResult(AjaxResult.success());
        String templateErrorStr = I18nUtil.getMessage("ui.data.column.mpStructureAllocation.import.templateError");
        String templateTitleErrorStr = I18nUtil.getMessage("ui.data.column.mpStructureAllocation.import.templateTitleError");
        String monthPlanVersionNotMatchErrorStr = I18nUtil.getMessage("ui.data.column.mpStructureAllocation.import.monthPlanVersionNotMatch");
        String productionVersionNotMatchErrorStr = I18nUtil.getMessage("ui.data.column.mpStructureAllocation.import.productionVersionNotMatch");
        ClassLoader classLoader = this.getClass().getClassLoader();
        DataFormatter dataFormatter = new DataFormatter();
        
        // 加载月计划调整与结构转产表导出模板，用于获取页签名称
        if (StringUtils.isEmpty(sheetName) || StringUtils.isEmpty(sheetName4DayResult)) {
            try (InputStream inputStream = classLoader.getResourceAsStream("excelModel/mpStructureAllocationExportTemp.xlsx");
                    InputStream dayInputStream = classLoader.getResourceAsStream("excelModel/factoryMonthPlanMouldFinalResultExportTemp.xlsx");
                    XSSFWorkbook workbook = new XSSFWorkbook(inputStream);
                    XSSFWorkbook dayWorkbook = new XSSFWorkbook(dayInputStream);) {
                sheetName = workbook.getSheetName(0);
                sheetName4DayResult = dayWorkbook.getSheetName(0);
            } catch (Exception e) {
                log.warn("importDataStructureAllocation workbook parse failed", e);
                helper.setAjaxResult(AjaxResult.error(templateErrorStr));
                return helper;
            }
        }
        
        try (Workbook wb = WorkbookFactory.create(new ByteArrayInputStream(fileBytes))) {
            Sheet sheet = wb.getSheet(sheetName);
            if (sheet == null || sheet.getRow(0) == null) {
                helper.setAjaxResult(AjaxResult.error(templateErrorStr));
                return helper;
            }
            Cell titleCell = sheet.getRow(0).getCell(0);
            if (titleCell == null) {
                helper.setAjaxResult(AjaxResult.error(templateErrorStr));
                return helper;
            }
            // 解析结构转产表页签
            // 解析标题
            String titleFormat = I18nUtil.getMessage("ui.data.column.mpStructureAllocation.exportTitle");
            helper.setParams(parseFormat(titleFormat, dataFormatter.formatCellValue(titleCell)));
            if (helper.getParams() == null || helper.getParams().length < 4) {
                helper.setAjaxResult(AjaxResult.error(templateTitleErrorStr));
                return helper;
            }
            // 解析需求计划版本
            String monthPlanVersionLabel = I18nUtil.getMessage("ui.data.column.mpStructureAllocation.monthPlanVersion") + ":";
            Cell monthPlanVersionCell = sheet.getRow(0).getCell(27);
            if (monthPlanVersionCell == null) {
                helper.setAjaxResult(AjaxResult.error(templateErrorStr));
                return helper;
            }
            helper.setMonthPlanVersion(dataFormatter.formatCellValue(monthPlanVersionCell).replace(monthPlanVersionLabel, "").trim());
            // 解析生产版本
            Cell productVersionCell = sheet.getRow(0).getCell(35);
            if (productVersionCell == null) {
                helper.setAjaxResult(AjaxResult.error(templateErrorStr));
                return helper;
            }
            String productionVersionLabel = I18nUtil.getMessage("ui.data.column.mpStructureAllocation.productionVersion") + ":";
            helper.setProductVersion(dataFormatter.formatCellValue(productVersionCell).replace(productionVersionLabel, "").trim());
            
            // 解析月计划页签
            // 解析标题
            Sheet sheet4DayResult = wb.getSheet(sheetName4DayResult);
            if (sheet4DayResult == null || sheet4DayResult.getRow(0) == null) {
                helper.setAjaxResult(AjaxResult.error(templateErrorStr));
                return helper;
            }
            Cell titleCell4DayResult = sheet4DayResult.getRow(0).getCell(0);
            if (titleCell4DayResult == null) {
                helper.setAjaxResult(AjaxResult.error(templateErrorStr));
                return helper;
            }
            String titleFormat4DayResult = I18nUtil.getMessage("ui.data.column.factoryMonthPlanMouldDayResult.exportTitle");
            helper.setParams4DayResult(parseFormat(titleFormat4DayResult, dataFormatter.formatCellValue(titleCell4DayResult)));
            if (helper.getParams4DayResult() == null || helper.getParams4DayResult().length < 3) {
                helper.setAjaxResult(AjaxResult.error(templateTitleErrorStr));
                return helper;
            }
            // 解析需求计划版本
            Cell monthPlanVersionCell4DayResult = sheet4DayResult.getRow(0).getCell(64);
            if (monthPlanVersionCell4DayResult == null) {
                helper.setAjaxResult(AjaxResult.error(templateErrorStr));
                return helper;
            }
            String monthPlanVersion4Day = dataFormatter.formatCellValue(monthPlanVersionCell4DayResult).replace(monthPlanVersionLabel, "").trim();
            // 校验如果两个页签的需求版本号不一致，报失败
            if (!Objects.equals(monthPlanVersion4Day, helper.getMonthPlanVersion())) {
                helper.setAjaxResult(AjaxResult.error(monthPlanVersionNotMatchErrorStr));
                return helper;
            }
            // 解析生产版本
            Cell productVersionCell4Day = sheet4DayResult.getRow(0).getCell(69);
            if (productVersionCell4Day == null) {
                helper.setAjaxResult(AjaxResult.error(templateErrorStr));
                return helper;
            }
            String productVersion4Day = dataFormatter.formatCellValue(productVersionCell4Day).replace(productionVersionLabel, "").trim();
            // 校验如果两个页签的需求版本号不一致，报失败
            if (!Objects.equals(productVersion4Day, helper.getProductVersion())) {
                helper.setAjaxResult(AjaxResult.error(productionVersionNotMatchErrorStr));
                return helper;
            }
        } catch (Exception e) {
            log.warn("importDataStructureAllocation workbook parse failed", e);
            helper.setAjaxResult(AjaxResult.error(templateErrorStr));
        }
        return helper;
    }
    
    /**
     * 解析导入结果文本
     * @param ajaxResult
     * @return
     */
    private int[] parseImportMsg(AjaxResult ajaxResult) {
        int[] result = new int[]{0, 0, 0};
        if (ajaxResult == null) {
            return result;
        }
        Object msgObj = ajaxResult.get(AjaxResult.MSG_TAG);
        if (msgObj == null) {
            log.warn("import result msg is null");
            return result;
        }
        String[] msgArr = msgObj.toString().split(",");
        if (msgArr.length < 2) {
            log.warn("import result msg format invalid: {}", msgObj);
            return result;
        }
        result[0] = Convert.toInt(msgArr[1], 0);
        if (msgArr.length > 2) {
            result[1] = Convert.toInt(msgArr[2], 0);
            result[2] = 1;
        }
        return result;
    }
    
    /**
     * 从格式化后的字符串中，反向解析出原始参数
     * @param format String.format 使用的模板（如 "年份:%d 月份:%d 工厂:%s 产品:%s"）
     * @param formattedStr 格式化后的最终字符串
     * @return 解析出的参数数组，null=解析失败
     */
    private String[] parseFormat(String format, String formattedStr) {
        if (format == null || formattedStr == null) {
            return null;
        }

        // 1. 把 format 模板 转成 正则表达式（核心步骤）
        // 转义正则特殊字符 . * + ? | ( ) [ ] { } \ ^ $
        String regex = format.replaceAll("([.*+?|()\\[\\]{}^$\\\\])", "\\\\$1");

        // 2. 替换所有占位符为 正则捕获组
        // 支持：%d %s %f %tY 等所有常用占位符
        regex = regex.replaceAll("%(?:\\d+\\$)?[+-]?(?:\\d+)?(?:\\.\\d+)?[a-zA-Z]", "(.*?)");

        // 3. 首尾加锚定，确保完全匹配整个字符串
        regex = "^" + regex + "$";

        // 4. 匹配
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(formattedStr);

        if (!matcher.matches()) {
            // 不匹配，解析失败
            return null;
        }

        // 5. 提取所有捕获组（group 0 是整个字符串，从 1 开始）
        String[] params = new String[matcher.groupCount()];
        for (int i = 0; i < params.length; i++) {
            params[i] = matcher.group(i + 1);
        }

        return params;
    }
}
