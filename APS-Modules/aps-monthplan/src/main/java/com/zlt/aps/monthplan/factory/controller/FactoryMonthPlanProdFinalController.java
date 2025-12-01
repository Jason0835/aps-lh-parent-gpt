package com.zlt.aps.monthplan.factory.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.ruoyi.api.gateway.system.domain.ExportLog;
import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.api.gateway.system.service.IExportLogService;
import com.ruoyi.api.gateway.system.service.IImportErrorLogService;
import com.ruoyi.api.gateway.system.service.IImportLogService;
import com.ruoyi.common.core.utils.PageUtils;
import com.ruoyi.common.core.utils.SecurityUtils;
import com.ruoyi.common.core.utils.poi.ExcelUtil;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;
import com.tlt.aps.enums.YesOrNoEnum;
import com.tlt.aps.redissonLock.annotation.RedissonLockAnno;
import com.zlt.aps.monthplan.api.domain.dto.FactoryMonthPlanProdFinalQueryDto;
import com.zlt.aps.monthplan.api.domain.entity.FactoryMonthPlanProdFinal;
import com.zlt.aps.monthplan.api.domain.entity.FactoryProductionVersion;
import com.zlt.aps.monthplan.api.domain.entity.MonthPlanRequireStock;
import com.zlt.aps.monthplan.api.domain.vo.FactoryMonthPlanDayProductionInfoVo;
import com.zlt.aps.monthplan.api.domain.vo.FactoryMonthPlanProdFinalVo;
import com.zlt.aps.monthplan.api.domain.vo.FactoryMonthPlanTypeVo;
import com.zlt.aps.monthplan.api.domain.vo.MonthPlanStatisticsVo;
import com.zlt.aps.monthplan.common.utils.CustomerExcelUtils;
import com.zlt.aps.monthplan.common.utils.ExcelExportUtils;
import com.zlt.aps.monthplan.common.utils.JsonUtils;
import com.zlt.aps.monthplan.factory.helper.ProductionPlanExcelUtils;
import com.zlt.aps.monthplan.factory.service.IFactoryMonthPlanProdFinalService;
import com.zlt.aps.monthplan.factory.service.IFactoryMonthPlanProductionFinalService;
import com.zlt.aps.monthplan.factory.service.IFactoryProductionVersionService;
import com.zlt.common.controller.BusiController;
import com.zlt.common.exception.QueryExprException;
import com.zlt.common.utils.PubUtil;
import com.zlt.core.queryformulas.QueryFormulaUtil;
import com.zlt.core.util.EntityUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.*;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：FactoryMonthPlanProdFinalController.java
 * 描    述：分厂月生产计划排产结果-生产计划排产结果 控制层类：....
 *
 * @author ZLT
 * @version 1.0
 * <p>
 * 修改记录：
 * 修改时间：...
 * 修 改 人：ZLT
 * 修改内容：...
 * @date 2025-02-14
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@Api(tags = "分厂月生产计划排产结果-生产计划排产结果")
@RequestMapping("/factoryMonthPlanProdFinal")
public class FactoryMonthPlanProdFinalController extends BusiController<FactoryMonthPlanProdFinal> {

    private final IExportLogService iExportLogService;

    private final IImportLogService iImportLogService;

    private final IImportErrorLogService iImportErrorLogService;

    private final IFactoryMonthPlanProdFinalService factoryMonthPlanProdFinalService;

    private final IFactoryProductionVersionService factoryProductionVersionService;

    private final IFactoryMonthPlanProductionFinalService factoryMonthPlanProductionFinalService;

    /**
     * 查询分厂月生产计划排产结果-生产计划排产结果列表
     * 带有分页信息
     */
    @PostMapping("/list")
    @ApiOperation("根据查询条件分页查询列表")
    public TableDataInfo list(@RequestBody FactoryMonthPlanProdFinal queryVO) {
        List<FactoryMonthPlanProdFinal> dataList = getData(queryVO, true);
        return getDataTable(dataList);
    }

    @ApiOperation("统计分厂月生产计划排产结果-排产结果列表")
    @PostMapping("/statistics")
    public AjaxResult statistics(@RequestBody FactoryMonthPlanProdFinal prodFinal) {
        MonthPlanStatisticsVo result = factoryMonthPlanProdFinalService.statistics(prodFinal);
        return AjaxResult.success(result);
    }

    /**
     * 统计分厂月生产计划日排产规格数及日排产总量
     */
    @ApiOperation("统计分厂月生产计划日排产规格数及日排产总量")
    @PostMapping("/statisticsDay")
    public AjaxResult statisticsByDay(@RequestBody FactoryMonthPlanProdFinal query) {
        if (null == query) {
            return AjaxResult.success(Collections.emptyList());
        }
        return AjaxResult.success(factoryMonthPlanProdFinalService.statisticsDay(query));
    }

    /**
     * 获取月份排产模式--Date 不为空则表示非自然月排产，Date为空表示自然月排产
     */
    @ApiOperation("获取月份排产模式--Date 不为空则表示非自然月排产，Date为空表示自然月排产")
    @PostMapping("/getProductionMonthType")
    public AjaxResult getProductionMonthType(@RequestBody FactoryMonthPlanProdFinal query) {
        if (null == query) {
            return AjaxResult.success(new FactoryMonthPlanTypeVo());
        }
        return AjaxResult.success(factoryMonthPlanProdFinalService.getProductionMonthType(query));
    }

    /**
     * 根据查询条件，获取对应的月计划定稿数据
     *
     * @param queryCondition 查询条件
     * @return
     */
    @PostMapping("/getProdResult")
    @ApiOperation("根据查询条件，获取对应的月计划定稿数据")
    public List<FactoryMonthPlanProdFinalVo> getProdResult(@RequestBody FactoryMonthPlanProdFinalQueryDto queryCondition) {
        QueryWrapper<FactoryMonthPlanProdFinal> queryWrapper = new QueryWrapper<>();
        builderCondition(queryWrapper, queryCondition);
        return factoryMonthPlanProductionFinalService.getProdResult(queryCondition);
    }

    /**
     * 根据查询条件，获取日对应的月计划定稿数据
     *
     * @param queryCondition 查询条件
     * @return
     */
    @PostMapping("/getMonthPlanProdResult")
    @ApiOperation("根据查询条件，获取日对应的月计划定稿数据")
    public List<FactoryMonthPlanProdFinalVo> getMonthPlanProdResult(@RequestBody FactoryMonthPlanProdFinalQueryDto queryCondition) {
        if (null == queryCondition || StringUtils.isBlank(queryCondition.getFactoryCode()) || null == queryCondition.getProductionDate()) {
            return Collections.emptyList();
        }
        FactoryProductionVersion finalVersion = factoryProductionVersionService.getFinalVersion(queryCondition.getFactoryCode(), queryCondition.getProductionDate());
        if (null == finalVersion) {
            return Collections.emptyList();
        }
        queryCondition.setYear(finalVersion.getYear());
        queryCondition.setMonth(finalVersion.getMonth());
        QueryWrapper<FactoryMonthPlanProdFinal> queryWrapper = new QueryWrapper<>();
        builderCondition(queryWrapper, queryCondition);
        List<FactoryMonthPlanProdFinalVo> dataList = factoryMonthPlanProductionFinalService.getMonthPlanProdResult(queryCondition);
        if (CollectionUtils.isEmpty(dataList)) {
            return Collections.emptyList();
        }
        Date productionStartDate = finalVersion.getProductionStartDate();
        Date productionEndDate = finalVersion.getProductionEndDate();
        Integer startDays = com.zlt.aps.factory.utils.DateUtils.getDaysByMonth(productionStartDate);
        Integer maxDays = com.zlt.aps.factory.utils.DateUtils.getMaxDaysByMonth(productionStartDate);
        Integer addDays = maxDays - startDays;
        List<FactoryMonthPlanProdFinalVo> resultList = new ArrayList<>(dataList.size());
        dataList.forEach(result -> {
            result.setProductionStartDate(productionStartDate);
            result.setProductionEndDate(productionEndDate);
            if (YesOrNoEnum.YES.getValue().equals(finalVersion.getIsNaturalMonth())) {
                result.setAddDays(BigDecimal.ZERO.intValue());
            } else {
                result.setAddDays(addDays);
            }
            resultList.add(result);
        });
        return resultList;
    }

    /**
     * 根据查询条件，获取某日的月计划排产数据
     *
     * @param queryCondition 查询条件
     * @return
     */
    @ApiOperation("根据查询条件，获取某日的月计划排产数据")
    @PostMapping("/getDayProductionInfo")
    public List<FactoryMonthPlanDayProductionInfoVo> getMonthPlanProductionInfo(@RequestBody FactoryMonthPlanProdFinalQueryDto queryCondition) {
        if (null == queryCondition || StringUtils.isBlank(queryCondition.getFactoryCode()) || null == queryCondition.getProductionDate()) {
            return Collections.emptyList();
        }
        return factoryMonthPlanProdFinalService.getMonthPlanDayProductionInfo(queryCondition);
    }

    /**
     * 根据查询条件，获取某日的月计划排产数据
     *
     * @return
     */
    @ApiOperation("根据查询条件，获取某日的月计划排产数据")
    @PostMapping("/getSaleMonthPlanRequireStock")
    public List<MonthPlanRequireStock> getSaleMonthPlanRequireStock(String monthPlanVersion) {
        if (null == monthPlanVersion) {
            return Collections.emptyList();
        }
        return factoryMonthPlanProdFinalService.getSaleMonthPlanRequireStock(monthPlanVersion);
    }
    /**
     * 导入调整计划
     *
     * @param importContext 导入上下文
     * @param updateSupport 已存在记录是否更新
     * @return 结果
     */
    @Log(title = "ui.data.adjust.monthPlan.modelName", businessType = BusinessType.IMPORT)
    @PostMapping("/importData")
    @ApiOperation("导入分厂月生产计划排产最终结果数据-即定稿后的数据，包含调整")
    public AjaxResult importData(@RequestBody ImportContext importContext, @RequestParam("updateSupport") boolean updateSupport) throws Exception {
        return super.commonImport(importContext, updateSupport);
    }


    @Override
    protected AjaxResult doImportData(List<FactoryMonthPlanProdFinal> list, boolean updateSupport, long importLogId) {
        return factoryMonthPlanProdFinalService.doImportData(list, updateSupport, importLogId);
    }

    /**
     * 导出列表
     */
    @Log(title = "分厂月生产计划排产结果-生产计划排产结果", businessType = BusinessType.EXPORT)
    @PostMapping("/exportData/{fileName}")
    @ApiOperation("导出分厂月生产计划排产最终结果数据-即定稿后的数据，包含调整")
    public byte[] exportData(@RequestBody FactoryMonthPlanProdFinal queryVO, @PathVariable("fileName") String fileName,
                             HttpServletResponse response) throws IOException {
        FactoryProductionVersion version = factoryProductionVersionService.getFinalVersionByYearMonth(queryVO.getFactoryCode(), queryVO.getYear(), queryVO.getMonth());
        ExportLog exportLog = new ExportLog();
        List<FactoryMonthPlanProdFinal> list = getData(queryVO, false);
        if (null == version || YesOrNoEnum.YES.getValue().equals(version.getIsNaturalMonth())) {
            ExcelUtil<FactoryMonthPlanProdFinal> util = new ExcelUtil(FactoryMonthPlanProdFinal.class);
            byte[] resultBytes = ExcelExportUtils.fillExcelAndLog(response, util, list, fileName, queryVO, exportLog, "0");
            this.iExportLogService.add(exportLog);
            return resultBytes;
        }
        List<Integer> dayList = ProductionPlanExcelUtils.getCycleDayList(version);
        String startWithName = "ui.data.column.factoryMonthPlanProdFinal.day";
        CustomerExcelUtils<FactoryMonthPlanProdFinal> util = new CustomerExcelUtils<>(FactoryMonthPlanProdFinal.class, dayList, startWithName, FactoryMonthPlanProdFinal.class);
        byte[] resultBytes = ExcelExportUtils.fillExcelAndLog(response, util, list, fileName, queryVO, exportLog, "0");
        this.iExportLogService.add(exportLog);
        return resultBytes;
    }

    @ApiOperation("定稿 - 年月+分厂+需求计划版本+分厂月计划版本")
    @PostMapping("/finalized")
    @RedissonLockAnno(uniqueMark = "redissonLock:factoryMonthPlanProdFinal:finalized:",
            expressions = {"#factoryMonthPlanProdFinal.factoryCode", "#factoryMonthPlanProdFinal.year", "#factoryMonthPlanProdFinal.month"},
            msgKey = "ui.data.alert.finalized.run",
            waitTime = 5,
            leaseTime = 600
    )
    public AjaxResult finalized(@RequestBody FactoryMonthPlanProdFinal factoryMonthPlanProdFinal) {
        return factoryMonthPlanProdFinalService.finalized(factoryMonthPlanProdFinal);
    }

    /**
     * 根据查询条件，获取查询数据-带有分页处理
     *
     * @param queryVO 查询条件
     * @return
     */
    private List<FactoryMonthPlanProdFinal> getData(FactoryMonthPlanProdFinal queryVO, boolean isPage) {
        QueryWrapper<FactoryMonthPlanProdFinal> queryWrapper = new QueryWrapper<>();
        //构建查询条件
        this.factoryMonthPlanProdFinalService.builderCondition(queryWrapper, queryVO);
        //分页
        PageUtils.startPage(isPage, getOrderBy(queryVO));
        List<FactoryMonthPlanProdFinal> dataList = factoryMonthPlanProdFinalService.getList(queryWrapper, true);
        dealList(dataList);
        if (isPage) {
            clearPage();
        }
        //数据字典转换或是基础数据转换
        try {
            QueryFormulaUtil.execFormula(dataList, getQueryFormulas());
        } catch (QueryExprException var6) {
            throw new ServiceException("执行查询公式时发生错误.");
        }
        return dataList;
    }

    /**
     * 解析不排产原因
     */
    private void dealList(List<FactoryMonthPlanProdFinal> list) {
        if (CollectionUtils.isEmpty(list)) {
            return;
        }
        Locale language = SecurityUtils.getUserLang();
        JsonUtils.parseJsonRemarkList(list, language.toString(), "reason");
    }

    /**
     * 构建查询条件
     *
     * @param queryWrapper
     * @param queryCondition
     */
    private void builderCondition(QueryWrapper<FactoryMonthPlanProdFinal> queryWrapper, FactoryMonthPlanProdFinalQueryDto queryCondition) {
        String factoryCode = queryCondition.getFactoryCode();
        queryWrapper.eq(PubUtil.isNotEmpty(factoryCode), "FACTORY_CODE", factoryCode);
        Integer year = queryCondition.getYear();
        queryWrapper.eq(PubUtil.isNotEmpty(year), "YEAR", year);
        Integer month = queryCondition.getMonth();
        queryWrapper.eq(PubUtil.isNotEmpty(month), "MONTH", month);
        String monthPlanVersion = queryCondition.getMonthPlanVersion();
        queryWrapper.eq(PubUtil.isNotEmpty(monthPlanVersion), "MONTH_PLAN_VERSION", monthPlanVersion);
        String productionVersion = queryCondition.getProductionVersion();
        queryWrapper.eq(PubUtil.isNotEmpty(productionVersion), "PRODUCTION_VERSION", productionVersion);
        String productCode = queryCondition.getProductCode();
        queryWrapper.eq(PubUtil.isNotEmpty(productCode), "PRODUCT_CODE", productCode);
        String productDesc = queryCondition.getProductDesc();
        queryWrapper.eq(PubUtil.isNotEmpty(productDesc), "PRODUCT_DESC", productDesc);
        String specifications = queryCondition.getSpecifications();
        queryWrapper.eq(PubUtil.isNotEmpty(specifications), "SPECIFICATIONS", specifications);
        String pattern = queryCondition.getPattern();
        queryWrapper.eq(PubUtil.isNotEmpty(pattern), "PATTERN", pattern);
        BigDecimal proSize = queryCondition.getProSize();
        queryWrapper.eq(PubUtil.isNotEmpty(proSize), "PRO_SIZE", proSize);
        String locationType = queryCondition.getLocationType();
        queryWrapper.eq(PubUtil.isNotEmpty(locationType), "LOCATION_TYPE", locationType);
        String channel = queryCondition.getChannel();
        queryWrapper.eq(PubUtil.isNotEmpty(channel), "CHANNEL", channel);
        String brand = queryCondition.getBrand();
        queryWrapper.eq(PubUtil.isNotEmpty(brand), "BRAND", brand);
    }

    /**
     * 设置排序信息
     *
     * @param queryVO
     * @return
     */
    private String getOrderBy(FactoryMonthPlanProdFinal queryVO) {
        Map<String, Object> params = queryVO.getParams();
        if (params != null && params.containsKey("orderBy")) {
            String orderByField = (String) params.get("orderBy");
            String dbField = EntityUtil.getColumnNameByFieldName(this.getTClass(), orderByField);
            String isAscStr = (String) params.get("isAsc");
            return dbField + " " + (isAscStr.equals("1") ? "asc" : "desc");
        } else {
            return null;
        }
    }

    /**
     * 设置转换
     *
     * @return
     */
    private String[] getQueryFormulas() {
        return null;
    }
}
