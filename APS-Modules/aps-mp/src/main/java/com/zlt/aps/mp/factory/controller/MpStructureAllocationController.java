package com.zlt.aps.mp.factory.controller;

import com.alibaba.fastjson.JSONArray;
import com.ruoyi.api.gateway.system.domain.ExportLog;
import com.ruoyi.api.gateway.system.domain.ImportErrorLog;
import com.ruoyi.api.gateway.system.domain.ImportLog;
import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.api.gateway.system.service.IExportLogService;
import com.ruoyi.api.gateway.system.service.IImportErrorLogService;
import com.ruoyi.api.gateway.system.service.IImportLogService;
import com.ruoyi.api.gateway.system.service.ISysDictDataCacheService;
import com.ruoyi.common.core.domain.SysDictData;
import com.ruoyi.common.core.utils.DateUtils;
import com.ruoyi.common.core.utils.PageUtils;
import com.ruoyi.common.core.utils.ServletUtils;
import com.ruoyi.common.core.utils.poi.ExcelUtil;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;
import com.ruoyi.common.utils.StringUtils;
import com.zlt.aps.constant.FactoryConstant;
import com.zlt.aps.enums.YesOrNoEnum;
import com.zlt.aps.mp.api.domain.entity.FactoryMonthPlanMouldDayResult;
import com.zlt.aps.mp.api.domain.entity.FactoryMonthPlanProductionFinalResult;
import com.zlt.aps.mp.api.domain.entity.MpFactoryProductionVersion;
import com.zlt.aps.mp.api.domain.entity.MpStructureAllocation;
import com.zlt.aps.mp.api.domain.vo.MpStructureAllocationVo;
import com.zlt.aps.mp.common.utils.PubUtil;
import com.zlt.aps.mp.factory.dto.MpStructureAllocationExportStatisticsVo;
import com.zlt.aps.mp.factory.dto.MpStructureAllocationExportVo;
import com.zlt.aps.mp.factory.mapper.MpStructureAllocationEntityMapper;
import com.zlt.aps.mp.factory.service.IFactoryMonthPlanProductionFinalResultService;
import com.zlt.aps.mp.factory.service.IMpStructureAllocationService;
import com.zlt.bill.common.controller.AbstractDocBizController;
import com.zlt.bill.common.service.IDocService;
import com.zlt.common.utils.ImportExcelUtils;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.YearMonth;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：MpStructureAllocationController.java
 * 描    述：排产过程_结构排产 控制层类：....
 *
 * @author zlt
 * @version 1.0
 * <p>
 * 修改记录：
 * 修改时间：...
 * 修 改 人：zlt
 * 修改内容：...
 * @date 2025-12-29
 */
@Slf4j
@Api(tags = "排产过程_结构排产")
@RestController
@RequiredArgsConstructor
@RequestMapping("/mpStructureAllocation")
public class MpStructureAllocationController extends AbstractDocBizController<MpStructureAllocation> {

    private final IMpStructureAllocationService mpStructureAllocationService;

    private final MpStructureAllocationEntityMapper entityMapper;

    private final IFactoryMonthPlanProductionFinalResultService monthPlanProductionFinalResultService;

    @Autowired
    private IExportLogService iExportLogService;

    @Autowired
    private IImportLogService iImportLogService;

    @Autowired
    private IImportErrorLogService iImportErrorLogService;

    private final ISysDictDataCacheService sysDictDataCacheService;

    /**
     * 查询排产过程_结构排产列表
     *
     * @param queryCondition 查询条件
     */
    @ApiOperation("查询列表")
    @PostMapping("/list")
    @Override
    public TableDataInfo list(@RequestBody MpStructureAllocation queryCondition) {
        try {
            startPage();
            setProductionVersion(queryCondition);
            List<MpStructureAllocation> list = mpStructureAllocationService.getDataList(queryCondition);
            return getDataTable(list);
        } finally {
            PageUtils.clearPage();
        }
    }


    /**
     * 根据条件查询结构调整列表（周程滚动使用）
     *
     * @param queryCondition 查询条件
     */
    @ApiOperation("根据条件查询结构调整列表")
    @PostMapping("/listAdjusts")
    public TableDataInfo listAdjusts(@RequestBody MpStructureAllocationVo queryCondition) {
        try {
            startPage();
            queryCondition.setCxMachineCode(queryCondition.getScheduledMachines());
            setProductionVersion(queryCondition);
            List<MpStructureAllocation> list = mpStructureAllocationService.getDataList(queryCondition);
            return getDataTable(list);
        } finally {
            PageUtils.clearPage();
        }
    }


    /**
     * 设置排产版本
     * 排产版本为空，默认查询当前年月最新的排产版本
     * @param queryCondition
     */
    private void setProductionVersion(MpStructureAllocation queryCondition) {
        if (StringUtils.isNotEmpty(queryCondition.getProductionVersion())) {
            return;
        }
        // 排产版本为空，默认查询当前年月最新的排产版本
        List<FactoryMonthPlanProductionFinalResult> monthPlanResultList = listMonthProdFinalPlans(queryCondition);
        if (PubUtil.isEmpty(monthPlanResultList)) {
            return;
        }
        // 排产版本
        String productionVersion = monthPlanResultList.get(0).getProductionVersion();
        log.info("排产版本为空，默认查询当前年月最新的排产版本:{}", productionVersion);
        queryCondition.setProductionVersion(productionVersion);
    }


    /**
     * 获取定稿版本的月度计划
     * @param queryCondition
     */
    private List<FactoryMonthPlanProductionFinalResult> listMonthProdFinalPlans(MpStructureAllocation queryCondition) {
        FactoryMonthPlanProductionFinalResult param = new FactoryMonthPlanProductionFinalResult();
        param.setFactoryCode(queryCondition.getFactoryCode());
        param.setYear(queryCondition.getYear());
        param.setMonth(queryCondition.getMonth());
        return monthPlanProductionFinalResultService.listMonthProdFinalPlans(param);
    }



    @Override
    protected List<MpStructureAllocation> listExportData(MpStructureAllocation condition) {
        if (null == condition) {
            return Collections.emptyList();
        }
        if (null == condition.getYear() || null == condition.getMonth() || StringUtils.isBlank(condition.getFactoryCode())) {
            return Collections.emptyList();
        }
        if (StringUtils.isBlank(condition.getMonthPlanVersion()) || StringUtils.isBlank(condition.getProductionVersion())) {
            return Collections.emptyList();
        }
        return mpStructureAllocationService.getDataList(condition);
    }

    /**
     * 保存
     */
    @Log(title = "ui.data.column.mpStructureAllocation.modelName", businessType = BusinessType.INSERT_OR_UPDATE)
    @ApiOperation("保存")
    @PostMapping("/save")
    @Override
    public AjaxResult save(@RequestBody MpStructureAllocation billVO) {
        return super.save(billVO);
    }

    /**
     * 删除
     */
    @Log(title = "ui.data.column.mpStructureAllocation.modelName", businessType = BusinessType.DELETE)
    @ApiOperation("删除")
    @DeleteMapping("/remove")
    @Override
    public AjaxResult removeByIds(@RequestBody List<Long> ids) {
        return super.removeByIds(ids);
    }

    @Override
    protected IDocService getDocService() {
        return mpStructureAllocationService;
    }

    @Override
    protected String getTypeCode() {
        return "MDM0408";
    }


    /**
     * 查询版本列表
     */
    @ApiOperation("查询版本列表")
    @PostMapping("/getVersionList")
    public TableDataInfo getVersionList(@RequestBody MpStructureAllocation queryVO) {
        this.startPage();
        List<MpStructureAllocation> list = entityMapper.getVersionList(queryVO);
        this.clearPage();
        return this.getDataTable(list);
    }

    /**
     * 获取日期最接近的下一个结构
     * @param queryCondition 查询条件
     */
    @ApiOperation("获取日期最接近的下一个结构")
    @PostMapping("/getNextStructure")
    public MpStructureAllocation getNextStructure(@RequestBody MpStructureAllocation queryCondition) {
        return mpStructureAllocationService.getNextStructure(queryCondition);
    }

    /**
     * 获取日期最接近的上一个结构
     * @param queryCondition 查询条件
     */
    @ApiOperation("获取日期最接近的上一个结构")
    @PostMapping("/getPreviousStructure")
    public MpStructureAllocation getPreviousStructure(@RequestBody MpStructureAllocation queryCondition) {
        return mpStructureAllocationService.getPreviousStructure(queryCondition);
    }

    /**
     * 导出列表
     */
    @Log(title = "排产过程_结构排产", businessType = BusinessType.EXPORT)
    @ApiOperation("导出数据")
    @PostMapping("/exportData/{fileName}")
    public byte[] exportData(@RequestBody MpStructureAllocation queryVO, @PathVariable("fileName") String fileName,
                             HttpServletResponse response) throws IOException {
        Date beginTime = DateUtils.getNowDate();
        setProductionVersion(queryVO);
        MpStructureAllocationExportStatisticsVo statisticsVo = mpStructureAllocationService.getExportVo(queryVO);
        List<MpStructureAllocationExportVo> list = statisticsVo.getRecordList();
        byte[] resultBytes = mpStructureAllocationService.getMpStructureAllocationExportByte(statisticsVo);
        Date endTime = DateUtils.getNowDate();
        ExportLog exportLog = new ExportLog();
        exportLog.setProcedureCode("0");
        exportLog.setExportParams(queryVO.toString());
        String uri = ServletUtils.getRequest().getRequestURI();
        exportLog.setFunctionCode(uri.split("/")[1]);
        exportLog.setFunctionName(fileName);
        exportLog.setFileName(fileName + ".xlsx");
        exportLog.setRowCount(list.size());
        exportLog.setBeginTime(beginTime);
        exportLog.setEndTime(endTime);
        exportLog.setSpendTime(DateUtils.getDiffTime(endTime, beginTime));
        this.iExportLogService.add(exportLog);
        return resultBytes;
    }

    /**
     * 导入数据
     */
    @Log(title = "排产过程_结构排产", businessType = BusinessType.IMPORT)
    @ApiOperation("导入数据")
    @PostMapping("/importDataStructureAllocation")
    public AjaxResult importDataStructureAllocation(@RequestBody ImportContext importContext, @RequestParam("updateSupport") boolean updateSupport) throws Exception {
        Date beginTime = DateUtils.getNowDate();
        ImportLog importLog = ImportExcelUtils.getImportLogAndUploadFile(importContext.getFileBytes(), importContext.getImportFilePath(), importContext.getProcedureCode(), importContext.getFunctionName(), importContext.getOriFileName(), 1);
        importLog = this.iImportLogService.add(importLog);
        ExcelUtil<MpStructureAllocationExportVo> util = new ExcelUtil<>(MpStructureAllocationExportVo.class);
        InputStream is = new ByteArrayInputStream(importContext.getFileBytes());
        // 工厂名称字典
        List<SysDictData> factoryDatas = sysDictDataCacheService.getType("biz_factory_name");
        Map<String, String> factoryMap = factoryDatas.stream().collect(Collectors.toMap(SysDictData::getDictLabel, SysDictData::getDictValue));

        List<SysDictData> productTypeList = sysDictDataCacheService.getType("biz_product_type");
        Map<String, String> productTypeMap = productTypeList.stream().collect(Collectors.toMap(SysDictData::getDictLabel, SysDictData::getDictValue));

        Workbook wb = WorkbookFactory.create(is);
        String sheetName = "结构转产表";
        Sheet sheet = wb.getSheet(sheetName);
        // 表头单元格
        Cell titleCell = sheet.getRow(0).getCell(0);
        String titleFormat = I18nUtil.getMessage("ui.data.column.mpStructureAllocation.exportTitle");
        String[] params = parseFormat(titleFormat, titleCell.getStringCellValue());
        // 月计划版本单元格
        String monthPlanVersionLabel = I18nUtil.getMessage("ui.data.column.mpStructureAllocation.monthPlanVersion") + ": ";
        String productionVersionLabel = I18nUtil.getMessage("ui.data.column.mpStructureAllocation.productionVersion") + ": ";

        Cell monthPlanVersionCell = sheet.getRow(0).getCell(26);
        String monthPlanVersion = monthPlanVersionCell.getStringCellValue().replace(monthPlanVersionLabel, "");
        // 生产版本单元格
        Cell productVersionCell = sheet.getRow(0).getCell(34);
        String productVersion4Cell = productVersionCell.getStringCellValue().replace(productionVersionLabel, "");
        String productVersion = "I" + DateUtils.dateTimeNow();

        // 表头单元格
        String sheetName4DayResult = "月计划";
        Sheet sheet4DayResult = wb.getSheet(sheetName4DayResult);
        Cell titleCell4DayResult = sheet4DayResult.getRow(0).getCell(0);
        String titleFormat4DayResult = I18nUtil.getMessage("ui.data.column.factoryMonthPlanMouldDayResult.exportTitle");
        String[] params4DayResult = parseFormat(titleFormat4DayResult, titleCell4DayResult.getStringCellValue());
        ExcelUtil<FactoryMonthPlanMouldDayResult> util4DayResult = new ExcelUtil<>(FactoryMonthPlanMouldDayResult.class);
        List<FactoryMonthPlanMouldDayResult> list4DayResult = util4DayResult.importExcel(sheetName4DayResult, new ByteArrayInputStream(importContext.getFileBytes()), 3, 1, -1);

        // 结构转产导入
        List<MpStructureAllocationExportVo> list = util.importExcel(sheetName, new ByteArrayInputStream(importContext.getFileBytes()), 2, 2, 10);
        AjaxResult ajaxResult = mpStructureAllocationService.importDataStructureAllocation(list, updateSupport, importLog.getId(), params, monthPlanVersion, productVersion, factoryMap, productTypeMap);

        // 月计划排产导入
        AjaxResult ajaxResult4DayResult = mpStructureAllocationService.importDataDayResult(list4DayResult, updateSupport, importLog.getId(), params4DayResult, monthPlanVersion, productVersion, factoryMap, productTypeMap);

        if (!ajaxResult.get(AjaxResult.CODE_TAG).equals(AjaxResult.Type.ERROR.value()) && !ajaxResult4DayResult.get(AjaxResult.CODE_TAG).equals(AjaxResult.Type.ERROR.value())) {
            // 版本关系存到版本表
            MpFactoryProductionVersion version = new MpFactoryProductionVersion();
            if (factoryMap.containsKey(params[2])) {
                version.setFactoryCode(factoryMap.get(params[2]));
            }
            if (productTypeMap.containsKey(params[3])) {
                version.setProductTypeCode(productTypeMap.get(params[3]));
            }
            int year = Integer.parseInt(params[0]);
            version.setYear(year);
            int month = Integer.parseInt(params[1]);
            version.setMonth(month);
            version.setMonthPlanVersion(monthPlanVersion);
            version.setProductionVersion(productVersion);
            version.setPlanType("01");
            version.setIsSelectedDemand(YesOrNoEnum.YES.getCode());
            version.setProductionInitVersion(productVersion);
            version.setProductionStVersion(productVersion);
//            version.setIsNaturalMonth("04");
            YearMonth yearMonth = YearMonth.of(year, month);
            version.setProductionStartDate(com.zlt.aps.mp.engine.utils.DateUtils.getDate(yearMonth.atDay(FactoryConstant.MONTH_START_DAY)));
            version.setProductionEndDate(com.zlt.aps.mp.engine.utils.DateUtils.getDate(yearMonth.atEndOfMonth()));
            version.setIsFinal(YesOrNoEnum.NO.getCode());
            baseDao.save(version);
        }
        // 处理返回结果，统一
        int errorNum = 0;
        int successNum = 0;
        List<Object> importErrorLogs = new ArrayList<>();
        String[] resultParam = ajaxResult.get(AjaxResult.MSG_TAG).toString().split(",");
        successNum += Integer.parseInt(resultParam[1]);
        if (resultParam.length > 2) {
            errorNum += Integer.parseInt(resultParam[2]);

            List<ImportErrorLog> importErrorLogList = StringUtils.cast(ajaxResult.get(AjaxResult.DATA_TAG));
            if (CollectionUtils.isNotEmpty(importErrorLogList)) {
                String listTxt = JSONArray.toJSONString(importErrorLogList);
                importErrorLogs.addAll(JSONArray.parseArray(listTxt, ImportErrorLog.class));
            }
        }
        String[] resultParam4DayResult = ajaxResult4DayResult.get(AjaxResult.MSG_TAG).toString().split(",");
        successNum += Integer.parseInt(resultParam4DayResult[1]);
        if (resultParam4DayResult.length > 2) {
            errorNum += Integer.parseInt(resultParam4DayResult[2]);

            List<ImportErrorLog> importErrorLogList = StringUtils.cast(ajaxResult4DayResult.get(AjaxResult.DATA_TAG));
            if (CollectionUtils.isNotEmpty(importErrorLogList)) {
                String listTxt = JSONArray.toJSONString(importErrorLogList);
                importErrorLogs.addAll(JSONArray.parseArray(listTxt, ImportErrorLog.class));
            }
        }

        Date endTime = DateUtils.getNowDate();
        importLog.setRowCount(list.size());
        importLog.setBeginTime(beginTime);
        importLog.setEndTime(endTime);
        importLog.setSpendTime(DateUtils.getDiffTime(endTime, beginTime));
        ImportExcelUtils.updateImportLogAndFormatMsg(importLog, ajaxResult, this.iImportLogService);
        ImportExcelUtils.saveImportErrorLogs(ajaxResult, this.iImportErrorLogService);
        ImportExcelUtils.updateImportLogAndFormatMsg(importLog, ajaxResult4DayResult, this.iImportLogService);
        ImportExcelUtils.saveImportErrorLogs(ajaxResult4DayResult, this.iImportErrorLogService);

        if (errorNum > 0) {
            return AjaxResult.error(StringUtils.format(I18nUtil.getMessage("ui.message.import.fail"), successNum, errorNum), importErrorLogs);
        } else {
            return AjaxResult.success(StringUtils.format(I18nUtil.getMessage("ui.message.import.success"), successNum));
        }
    }

    /**
     * 从格式化后的字符串中，反向解析出原始参数
     * @param format String.format 使用的模板（如 "年份:%d 月份:%d 工厂:%s 产品:%s"）
     * @param formattedStr 格式化后的最终字符串
     * @return 解析出的参数数组，null=解析失败
     */
    public static String[] parseFormat(String format, String formattedStr) {
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
