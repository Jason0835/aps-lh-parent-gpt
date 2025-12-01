package com.zlt.aps.monthplan.factory.controller;

import com.ruoyi.api.gateway.system.domain.ExportLog;
import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.api.gateway.system.service.IExportLogService;
import com.ruoyi.common.core.utils.PageUtils;
import com.ruoyi.common.core.utils.SecurityUtils;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;
import com.ruoyi.common.security.annotation.RequiresPermissions;
import com.tlt.aps.enums.YesOrNoEnum;
import com.zlt.aps.monthplan.api.domain.dto.ChangeSpecCodeMouldingDayResultParam;
import com.zlt.aps.monthplan.api.domain.entity.FactoryProductionVersion;
import com.zlt.aps.monthplan.api.domain.entity.MonthPlanMouldingDayResult;
import com.zlt.aps.monthplan.api.domain.vo.MonthPlanDayResultStatisticsVo;
import com.zlt.aps.monthplan.api.domain.vo.MonthPlanMouldingDayResultVo;
import com.zlt.aps.monthplan.api.domain.vo.MonthPlanStatisticsVo;
import com.zlt.aps.monthplan.common.utils.CustomerExcelUtils;
import com.zlt.aps.monthplan.common.utils.ExcelExportUtils;
import com.zlt.aps.monthplan.common.utils.JsonUtils;
import com.zlt.aps.monthplan.factory.helper.ProductionPlanExcelUtils;
import com.zlt.aps.monthplan.factory.service.IFactoryProductionVersionService;
import com.zlt.aps.monthplan.factory.service.IMonthPlanMouldingDayResultService;
import com.zlt.common.controller.BusiController;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：MonthPlanMouldingDayResultController.java
 * 描    述：分厂月生产计划排产过程-模具排产结果汇总 控制层类：....
 *
 * @author zlt
 * @version 1.0
 * <p>
 * 修改记录：
 * 修改时间：...
 * 修 改 人：zlt
 * 修改内容：...
 * @date 2025-03-17
 */
@Slf4j
@Api(tags = "分厂月生产计划排产过程-模具排产结果汇总")
@RestController
@RequiredArgsConstructor
@RequestMapping("/mouldingDayResult")
public class MonthPlanMouldingDayResultController extends BusiController<MonthPlanMouldingDayResult> {

    private final IMonthPlanMouldingDayResultService monthPlanMouldingDayResultService;

    private final IFactoryProductionVersionService factoryProductionVersionService;

    private final IExportLogService exportLogService;

    /**
     * 查询分厂月生产计划排产过程-模具排产结果汇总列表
     */
    @RequiresPermissions("monthplan:mouldingDayResult:list")
    @ApiOperation("查询列表")
    @PostMapping("/list")
    public TableDataInfo list(@RequestBody MonthPlanMouldingDayResult queryVO) {
        try {
            startPage();
            List<MonthPlanMouldingDayResult> list = monthPlanMouldingDayResultService.selectList(queryVO, true);
            list = dealList(list);
            return getDataTable(list);
        } finally {
            PageUtils.clearPage();
        }
    }

    /**
     * 统计分厂月生产计划排产
     */
    @ApiOperation("统计分厂月生产计划排产")
    @PostMapping("/statistics")
    public AjaxResult statistics(@RequestBody MonthPlanMouldingDayResult QueryVO) {
        MonthPlanStatisticsVo result = monthPlanMouldingDayResultService.statistics(QueryVO);
        return AjaxResult.success(result);
    }

    /**
     * 统计分厂月生产计划日排产规格数及日排产总量
     */
    @ApiOperation("统计分厂月生产计划日排产规格数及日排产总量")
    @PostMapping("/statisticsDay")
    public AjaxResult statisticsByDay(@RequestBody MonthPlanMouldingDayResult query) {
        if (null == query) {
            return AjaxResult.success(Collections.emptyList());
        }
        String productionVersion = query.getProductionVersion();
        if (StringUtils.isBlank(productionVersion)) {
            return AjaxResult.success(Collections.emptyList());
        }
        return AjaxResult.success(monthPlanMouldingDayResultService.statisticsDay(productionVersion));
    }

    /**
     * 根据集合导入分厂月生产计划排产过程-模具排产结果汇总数据
     *
     * @param importContext 导入上下文
     * @param updateSupport 已存在记录是否更新
     * @return 结果
     */
    @RequiresPermissions("monthplan:mouldingDayResult:import")
    @Log(title = "ui.data.column.monthPlanMouldingDayResult.modelName", businessType = BusinessType.IMPORT)
    @ApiOperation("导入数据")
    @PostMapping("/importData")
    public AjaxResult importData(@RequestBody ImportContext importContext, @RequestParam("updateSupport") boolean updateSupport) throws Exception {
        return super.commonImport(importContext, updateSupport);
    }


    @ApiOperation("查询对应年月+分厂+需求计划版本的分厂月计划版本")
    @PostMapping("/productionVersionList")
    AjaxResult productionVersionList(@RequestBody MonthPlanMouldingDayResult query) {
        return AjaxResult.success(monthPlanMouldingDayResultService.productionVersionList(query));
    }

    /**
     * 导出列表
     */
    @RequiresPermissions("monthplan:mouldingDayResult:export")
    @Log(title = "分厂月生产计划排产过程-模具排产结果汇总", businessType = BusinessType.EXPORT)
    @ApiOperation("导出分厂月生产计划模具排产结果")
    @PostMapping("/exportData/{fileName}")
    public byte[] exportData(@RequestBody MonthPlanMouldingDayResult queryVO, @PathVariable("fileName") String fileName,
                             HttpServletResponse response) throws IOException {
        FactoryProductionVersion version = factoryProductionVersionService.getProductionVersion(queryVO.getProductionVersion());
        if (null == version) {
            return super.commonExport(queryVO, fileName, response);
        }
        if (YesOrNoEnum.YES.getValue().equals(version.getIsNaturalMonth())) {
            return super.commonExport(queryVO, fileName, response);
        }
        List<Integer> dayList = ProductionPlanExcelUtils.getCycleDayList(version);
        String startWithName = "ui.data.column.monthPlanMouldingDayResult.day";
        List<MonthPlanMouldingDayResult> list = this.listExportData(queryVO);
        CustomerExcelUtils<MonthPlanMouldingDayResult> util = new CustomerExcelUtils<>(MonthPlanMouldingDayResult.class, dayList, startWithName, MonthPlanMouldingDayResult.class);
        ExportLog exportLog = new ExportLog();
        byte[] resultBytes = ExcelExportUtils.fillExcelAndLog(response, util, list, fileName, queryVO, exportLog, "0");
        this.exportLogService.add(exportLog);
        return resultBytes;
    }

    /**
     * 切换硫化规格代码
     *
     * @param changeParam 需换规格的计划
     */
    @ApiOperation("切换硫化规格代码")
    @PostMapping("/changeSpecCode")
    public AjaxResult changeSpecCodeForPlan(@RequestBody ChangeSpecCodeMouldingDayResultParam changeParam) {
        if (null == changeParam || null == changeParam.getProductionId() || StringUtils.isBlank(changeParam.getSpecCode())) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.monthPlanMouldingDayResult.checkChangeSpecCode"));
        }
        return monthPlanMouldingDayResultService.changePlanSpecCode(changeParam);
    }

    @Override
    protected List<MonthPlanMouldingDayResult> listExportData(MonthPlanMouldingDayResult obj) {
        List<MonthPlanMouldingDayResult> resultList = monthPlanMouldingDayResultService.selectList(obj, true);
        dealList(resultList);
        return resultList;
    }

    @Override
    protected AjaxResult doImportData(List<MonthPlanMouldingDayResult> list, boolean updateSupport, long importLogId) {
        return monthPlanMouldingDayResultService.doImportData(list, updateSupport, importLogId);
    }

    /**
     * 处理语言包问题 将未排原因的json转换处理
     *
     * @param list
     */
    private List<MonthPlanMouldingDayResult> dealList(List<MonthPlanMouldingDayResult> list) {
        if (CollectionUtils.isNotEmpty(list)) {
            //获取当前语言包
            Locale language = SecurityUtils.getUserLang();
            JsonUtils.parseJsonRemarkList(list, language.toString(), "reason");
        }
        return list;
    }

    /**
     * 查询分厂月生产计划合并SKU-合并SKU
     */
    @ApiOperation("查询分厂月生产计划合并SKU-合并SKU")
    @PostMapping("/listFacProduct")
    public TableDataInfo listFacProduct(@RequestBody MonthPlanMouldingDayResult queryVO) {
        try {
            startPage();
            List<MonthPlanMouldingDayResultVo> list = monthPlanMouldingDayResultService.listFacProduct(queryVO);
            return getDataTable(list);
        } finally {
            PageUtils.clearPage();
        }
    }

    /**
     * 导出分厂月生产计划合并SKU-合并SKU
     */
    @Log(title = "分厂月生产计划排产过程-合并SKU", businessType = BusinessType.EXPORT)
    @ApiOperation("导出分厂月生产计划合并SKU-合并SKU")
    @PostMapping("/exportFacProductData/{fileName}")
    public byte[] exportFacProductData(@RequestBody MonthPlanMouldingDayResult queryVO, @PathVariable("fileName") String fileName,
                                       HttpServletResponse response) throws IOException {
        FactoryProductionVersion version = factoryProductionVersionService.getProductionVersion(queryVO.getProductionVersion());
        if (null == version) {
            return super.commonExport(queryVO, fileName, response);
        }
        if (YesOrNoEnum.YES.getValue().equals(version.getIsNaturalMonth())) {
            return super.commonExport(queryVO, fileName, response);
        }
        List<Integer> dayList = ProductionPlanExcelUtils.getCycleDayList(version);
        String startWithName = "ui.data.column.monthPlanMouldingDayResult.day";
        List<MonthPlanMouldingDayResultVo> list = monthPlanMouldingDayResultService.listFacProduct(queryVO);
        CustomerExcelUtils<MonthPlanMouldingDayResultVo> util = new CustomerExcelUtils<>(MonthPlanMouldingDayResultVo.class, dayList, startWithName, MonthPlanMouldingDayResultVo.class);
        ExportLog exportLog = new ExportLog();
        byte[] resultBytes = ExcelExportUtils.fillExcelAndLog(response, util, list, fileName, queryVO, exportLog, "0");
        this.exportLogService.add(exportLog);
        return resultBytes;
    }

    /**
     * 查询分厂月生产计划合并SKU-合并SKU
     */
    @ApiOperation("查询月计划排产统计")
    @PostMapping("/listFacProductStatistics")
    public TableDataInfo listFacProductStatistics(@RequestBody MonthPlanMouldingDayResult queryVO) {
        try {
            startPage();
            List<MonthPlanDayResultStatisticsVo> list = monthPlanMouldingDayResultService.listFacProductStatistics(queryVO);
            return getDataTable(list);
        } finally {
            PageUtils.clearPage();
        }
    }

    /**
     * 导出月计划排产统计
     */
    @Log(title = "月计划排产统计", businessType = BusinessType.EXPORT)
    @ApiOperation("导出月计划排产统计")
    @PostMapping("/exportFacProductStatisticsData/{fileName}")
    public byte[] exportFacProductStatisticsData(@RequestBody MonthPlanMouldingDayResult queryVO, @PathVariable("fileName") String fileName,
                                       HttpServletResponse response) throws IOException {
        FactoryProductionVersion version = factoryProductionVersionService.getProductionVersion(queryVO.getProductionVersion());
        if (null == version) {
            return super.commonExport(queryVO, fileName, response);
        }
        if (YesOrNoEnum.YES.getValue().equals(version.getIsNaturalMonth())) {
            return super.commonExport(queryVO, fileName, response);
        }
        List<Integer> dayList = ProductionPlanExcelUtils.getCycleDayList(version);
        String startWithName = "ui.data.column.monthPlanMouldingDayResult.day";
        List<MonthPlanDayResultStatisticsVo> list = monthPlanMouldingDayResultService.listFacProductStatistics(queryVO);
        CustomerExcelUtils<MonthPlanDayResultStatisticsVo> util = new CustomerExcelUtils<>(MonthPlanDayResultStatisticsVo.class, dayList, startWithName, MonthPlanDayResultStatisticsVo.class);
        ExportLog exportLog = new ExportLog();
        byte[] resultBytes = ExcelExportUtils.fillExcelAndLog(response, util, list, fileName, queryVO, exportLog, "0");
        this.exportLogService.add(exportLog);
        return resultBytes;
    }
}
