package com.zlt.aps.mp.factory.controller;

import com.ruoyi.api.gateway.system.domain.ExportLog;
import com.ruoyi.api.gateway.system.domain.ImportErrorLog;
import com.ruoyi.api.gateway.system.domain.ImportLog;
import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.api.gateway.system.service.IExportLogService;
import com.ruoyi.api.gateway.system.service.IImportErrorLogService;
import com.ruoyi.api.gateway.system.service.IImportLogService;
import com.ruoyi.common.core.utils.DateUtils;
import com.ruoyi.common.core.utils.PageUtils;
import com.ruoyi.common.core.utils.ServletUtils;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;
import com.ruoyi.common.utils.StringUtils;
import com.zlt.aps.mp.api.domain.entity.FactoryMonthPlanProductionFinalResult;
import com.zlt.aps.mp.api.domain.entity.MpStructureAllocation;
import com.zlt.aps.mp.api.domain.vo.AdjustsCxMachineVo;
import com.zlt.aps.mp.api.domain.vo.MpStructureAllocationVo;
import com.zlt.aps.mp.common.utils.CommaFieldSortUtil;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

    /**
     * 查询排产过程_结构排产列表
     *
     * @param queryCondition 查询条件
     */
    @Override
    @ApiOperation("查询列表")
    @PostMapping("/list")
    public TableDataInfo list(@RequestBody MpStructureAllocation queryCondition) {
        try {
            startPage();
            setProductionVersion(queryCondition);
            List<MpStructureAllocation> list = mpStructureAllocationService.getDataList(queryCondition, false);
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
            List<MpStructureAllocation> list = mpStructureAllocationService.getDataList(queryCondition, true);
            // 集合逗号分隔字段升序排序
            CommaFieldSortUtil.sortAndUpdateCommaField(list, MpStructureAllocation::getCxMachineCode, MpStructureAllocation::setCxMachineCode);
            return getDataTable(list);
        } finally {
            PageUtils.clearPage();
        }
    }


    /**
     * 设置排产版本
     * 排产版本为空，默认查询当前年月最新的排产版本
     *
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
     *
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
        return mpStructureAllocationService.getDataList(condition, false);
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
     *
     * @param queryCondition 查询条件
     */
    @ApiOperation("获取日期最接近的下一个结构")
    @PostMapping("/getNextStructure")
    public MpStructureAllocation getNextStructure(@RequestBody MpStructureAllocation queryCondition) {
        return mpStructureAllocationService.getNextStructure(queryCondition);
    }

    /**
     * 获取日期最接近的上一个结构
     *
     * @param queryCondition 查询条件
     */
    @ApiOperation("获取日期最接近的上一个结构")
    @PostMapping("/getPreviousStructure")
    public MpStructureAllocation getPreviousStructure(@RequestBody MpStructureAllocation queryCondition) {
        return mpStructureAllocationService.getPreviousStructure(queryCondition);
    }

    /**
     * 从缓存中获取调整机台
     */
    @ApiOperation("从缓存中获取调整机台")
    @PostMapping("/getAdjustsCxMachineFromRedis")
    public AdjustsCxMachineVo getAdjustsCxMachineFromRedis() {
        return mpStructureAllocationService.getAdjustsCxMachineFromRedis();
    }

    /**
     * 调整机台设置到缓存
     *
     * @param adjustsCxMachineVo
     */
    @ApiOperation("调整机台设置到缓存")
    @PostMapping("/setAdjustsCxMachineFromRedis")
    public void setAdjustsCxMachineFromRedis(@RequestBody AdjustsCxMachineVo adjustsCxMachineVo) {
        mpStructureAllocationService.setAdjustsCxMachineFromRedis(adjustsCxMachineVo);
    }

    /**
     * 导出列表
     */
    @Override
    @Log(title = "排产过程_结构排产", businessType = BusinessType.EXPORT)
    @ApiOperation("导出数据")
    @PostMapping("/exportData/{fileName}")
    public byte[] exportData(@RequestBody MpStructureAllocation queryVO, @PathVariable("fileName") String fileName,
                             HttpServletResponse response) throws IOException {
        Date beginTime = DateUtils.getNowDate();
        setProductionVersion(queryVO);
        MpStructureAllocationExportStatisticsVo statisticsVo = mpStructureAllocationService.getExportVo(queryVO, false);
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
    @SuppressWarnings("unchecked")
    public AjaxResult importDataStructureAllocation(@RequestBody ImportContext importContext, @RequestParam("updateSupport") boolean updateSupport) throws Exception {
        if (importContext == null || importContext.getFileBytes() == null || importContext.getFileBytes().length == 0) {
            return AjaxResult.error("导入文件不能为空");
        }
        Date beginTime = DateUtils.getNowDate();
        byte[] fileBytes = importContext.getFileBytes();
        ImportLog importLog = ImportExcelUtils.getImportLogAndUploadFile(fileBytes, importContext.getImportFilePath(), importContext.getProcedureCode(), importContext.getFunctionName(), importContext.getOriFileName(), 1);
        // 执行导入逻辑
        AjaxResult ajaxResult = mpStructureAllocationService.importData(fileBytes, importLog);
        importLog = this.iImportLogService.add(importLog);
        // 处理返回结果
        Map<String, Object> returnData = (Map<String, Object>) (ajaxResult.get(AjaxResult.DATA_TAG));
        Integer rowCount = 0;
        Integer errorNum = 0;
        Integer successNum = 0;
        List<ImportErrorLog> importErrorLogs = Collections.EMPTY_LIST;
        if (returnData != null) {
            rowCount = (Integer) returnData.getOrDefault("rowCount", 0);
            errorNum = (Integer) returnData.getOrDefault("errorNum", 0);
            successNum = (Integer) returnData.getOrDefault("successNum", 0);
            importErrorLogs = (List<ImportErrorLog>) returnData.get("importErrorLogs");
        }
        if (importErrorLogs != null && importLog != null && importLog.getId() != null) {
            Long importLogId = importLog.getId();
            importErrorLogs.forEach(err -> err.setImportLogId(importLogId));
        }
        AjaxResult logResult; // 日志消息，用于更新日志
        AjaxResult finalResult;
        if (errorNum > 0) {
            finalResult = AjaxResult.error(StringUtils.format(I18nUtil.getMessage("ui.message.import.fail"), successNum, errorNum), importErrorLogs);
            logResult = AjaxResult.error(I18nUtil.getMessage("ui.message.import.fail") + "," + successNum + "," + errorNum, importErrorLogs);
        } else if (successNum > 0) {
            finalResult = AjaxResult.success(StringUtils.format(I18nUtil.getMessage("ui.message.import.success"), successNum));
            logResult = finalResult;
        } else {
            finalResult = ajaxResult;
            logResult = finalResult;
        }
        Date endTime = DateUtils.getNowDate();
        importLog.setRowCount(rowCount);
        importLog.setBeginTime(beginTime);
        importLog.setEndTime(endTime);
        importLog.setSpendTime(DateUtils.getDiffTime(endTime, beginTime));
        ImportExcelUtils.updateImportLogAndFormatMsg(importLog, logResult, this.iImportLogService);
        ImportExcelUtils.saveImportErrorLogs(logResult, this.iImportErrorLogService);
        return finalResult;
    }

    /**
     * 从格式化后的字符串中，反向解析出原始参数
     *
     * @param format       String.format 使用的模板（如 "年份:%d 月份:%d 工厂:%s 产品:%s"）
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
