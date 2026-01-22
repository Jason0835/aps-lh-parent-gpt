package com.zlt.aps.monthplan.demand.controller;

import cn.hutool.core.date.DateUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.google.common.collect.Lists;
import com.ruoyi.common.core.web.page.PageDomain;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.core.web.page.TableSupport;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;
import com.tlt.aps.constant.StringConstant;
import com.zlt.aps.monthplan.api.domain.entity.DpDemandPlanSummary;
import com.zlt.aps.monthplan.common.utils.CollectionKit;
import com.zlt.aps.monthplan.demand.service.IDpDemandPlanService;
import com.zlt.aps.monthplan.demand.service.IDpStockVersionService;
import com.zlt.bill.common.controller.AbstractDocBizController;
import com.zlt.bill.common.service.IDocService;
import com.zlt.common.utils.PubUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
* Copyright (c) 2022, All rights reserved。
* 文件名称：DpDemandPlanController.java
* 描    述：需求计划 控制层类：....
*@author yelq
*@date 2025-12-25
*@version 1.0
*
 *  修改记录：
*     修改时间：...
*     修 改 人：yelq
*     修改内容：...
*/
@Slf4j
@Api(tags = "需求计划")
@AllArgsConstructor
@RestController
@RequestMapping("/demandPlan")
public class DpDemandPlanSummaryController extends AbstractDocBizController<DpDemandPlanSummary> {

    private final IDpDemandPlanService dpDemandPlanService;
    // 版本库存
    private final IDpStockVersionService dpStockVersionService;
    /**
     * 查询需求计划列表
     */
    @ApiOperation("查询列表")
    @PostMapping("/list")
    @Override
    public TableDataInfo list(@RequestBody DpDemandPlanSummary queryVO) {
        TableDataInfo tableResult = super.list(queryVO);
        if(CollectionUtils.isEmpty(tableResult.getRows())) {
            return tableResult;
        }
        this.translationList(queryVO,(List<DpDemandPlanSummary>)tableResult.getRows(),false);
        return tableResult;
    }

    private void translationList(DpDemandPlanSummary queryVO,List<DpDemandPlanSummary> list,boolean fetchUpdateTime) {
        if(!fetchUpdateTime) {
            Set<String> monthPlanVersions = list.stream().map(DpDemandPlanSummary::getMonthPlanVersion).collect(Collectors.toSet());
            Map<String, Map<String,Integer>> stockQtyMap = dpStockVersionService.calculateStockQty(monthPlanVersions);
            list.forEach(demandPlanSummary -> {
                Map<String,Integer> stockMap = stockQtyMap.getOrDefault(demandPlanSummary.getMonthPlanVersionKey(),Collections.emptyMap());
                demandPlanSummary.setStockQty(stockMap.getOrDefault(StringConstant.ZERO, BigDecimal.ZERO.intValue()));
                demandPlanSummary.setCurrentYearStockQty(stockMap.getOrDefault(StringConstant.ONE,BigDecimal.ZERO.intValue()));
                demandPlanSummary.setSub1YearStockQty(stockMap.getOrDefault(StringConstant.TWO,BigDecimal.ZERO.intValue()));
                demandPlanSummary.setSub2YearStockQty(stockMap.getOrDefault(StringConstant.THREE,BigDecimal.ZERO.intValue()));
            });
            return;
        }
        Map<String, Map<String,Integer>> stockQtyMap = dpStockVersionService.calculateStockQty();
        for (DpDemandPlanSummary demandPlanSummary : list) {
            Map<String,Integer> stockMap = stockQtyMap.getOrDefault(demandPlanSummary.getMonthPlanVersionKey(),Collections.emptyMap());
            demandPlanSummary.setStockQty(stockMap.getOrDefault(StringConstant.ZERO, BigDecimal.ZERO.intValue()));
            demandPlanSummary.setCurrentYearStockQty(stockMap.getOrDefault(StringConstant.ONE,BigDecimal.ZERO.intValue()));
            demandPlanSummary.setSub1YearStockQty(stockMap.getOrDefault(StringConstant.TWO,BigDecimal.ZERO.intValue()));
            demandPlanSummary.setSub2YearStockQty(stockMap.getOrDefault(StringConstant.THREE,BigDecimal.ZERO.intValue()));
            demandPlanSummary.setUpdateDate(DateUtil.formatDateTime(demandPlanSummary.getUpdateTime()));
        }
    }


    @Override
    protected String getOrderBy() {
        return "create_time desc,id desc";
    }


    /**
     * 导出列表
     */
    @Log(title = "需求计划", businessType = BusinessType.EXPORT)
    @ApiOperation("导出数据")
    @PostMapping("/exportData/{fileName}")
    @Override
    public byte[] exportData(@RequestBody DpDemandPlanSummary queryVO, @PathVariable("fileName") String fileName,
                             HttpServletResponse response) throws IOException {
        return super.exportData(queryVO, fileName, response);
    }


    @Override
    protected List<DpDemandPlanSummary> listExportData(DpDemandPlanSummary queryVO) {
        List<DpDemandPlanSummary> list = Lists.newArrayList();
        TableDataInfo tableResult = super.list(queryVO);
        int page_size = 5000;// 定义每页数据数量
        long list_count =tableResult.getTotal();
        //总数量除以每页显示条数等于页数
        int export_times = (int) (list_count % page_size > 0 ? list_count / page_size
            + 1 : list_count / page_size);
        PageDomain pageDomain = TableSupport.buildPageRequest();
        //循环获取产生每页数据
        for (int m = 0; m < export_times; m++) {
            pageDomain.setPageSize(m+1);
            pageDomain.setPageSize(page_size);
            tableResult = super.list(queryVO);
            if(CollectionKit.isNotEmpty(tableResult.getRows())) {
                list.addAll((List<DpDemandPlanSummary>)tableResult.getRows());
            }
        }
        this.translationList(queryVO,list,true);
        return list;
    }


    @Override
    protected IDocService getDocService(){
        return dpDemandPlanService;
    }

    /**
     * 条件拼接
     *
     * @param queryWrapper
     * @param queryVO
     */
    @Override
    protected void builderCondition(QueryWrapper<DpDemandPlanSummary> queryWrapper, DpDemandPlanSummary queryVO) {
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("factoryCode")), "FACTORY_CODE", queryVO.getFieldValueByFieldName("factoryCode"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("year")), "YEAR", queryVO.getFieldValueByFieldName("year"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("month")), "MONTH", queryVO.getFieldValueByFieldName("month"));
        queryWrapper.like(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("monthPlanVersion")), "MONTH_PLAN_VERSION", queryVO.getFieldValueByFieldName("monthPlanVersion"));
        queryWrapper.like(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("materialCode")), "MATERIAL_CODE", queryVO.getFieldValueByFieldName("materialCode"));
        queryWrapper.like(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("structureName")), "STRUCTURE_NAME", queryVO.getFieldValueByFieldName("structureName"));
        queryWrapper.like(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("materialDesc")), "MATERIAL_DESC", queryVO.getFieldValueByFieldName("materialDesc"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("orderPriority")), "ORDER_PRIORITY", queryVO.getFieldValueByFieldName("orderPriority"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("scmPriority")), "SCM_PRIORITY", queryVO.getFieldValueByFieldName("scmPriority"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("isAlternateMaterial")), "IS_ALTERNATE_MATERIAL", queryVO.getFieldValueByFieldName("isAlternateMaterial"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("productTypeCode")), "PRODUCT_TYPE_CODE", queryVO.getFieldValueByFieldName("productTypeCode"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("locationType")), "LOCATION_TYPE", queryVO.getFieldValueByFieldName("locationType"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("brand")), "BRAND", queryVO.getFieldValueByFieldName("brand"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("mainPattern")), "MAIN_PATTERN", queryVO.getFieldValueByFieldName("mainPattern"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("productionType")), "PRODUCTION_TYPE", queryVO.getFieldValueByFieldName("productionType"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("yearWeek")), "YEAR_WEEK", queryVO.getFieldValueByFieldName("yearWeek"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("isDynamicBalance")), "IS_DYNAMIC_BALANCE", queryVO.getFieldValueByFieldName("isDynamicBalance"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("isUniformity")), "IS_UNIFORMITY", queryVO.getFieldValueByFieldName("isUniformity"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("orderQty")), "ORDER_QTY", queryVO.getFieldValueByFieldName("orderQty"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("stockQty")), "STOCK_QTY", queryVO.getFieldValueByFieldName("stockQty"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("remainingQty")), "REMAINING_QTY", queryVO.getFieldValueByFieldName("remainingQty"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("averageSaleQty")), "AVERAGE_SALE_QTY", queryVO.getFieldValueByFieldName("averageSaleQty"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("plannedSurplus")), "PLANNED_SURPLUS", queryVO.getFieldValueByFieldName("plannedSurplus"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("netQty")), "NET_QTY", queryVO.getFieldValueByFieldName("netQty"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("isProduction")), "IS_PRODUCTION", queryVO.getFieldValueByFieldName("isProduction"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("postponeNetQty")), "POSTPONE_NET_QTY", queryVO.getFieldValueByFieldName("postponeNetQty"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("unPostponeNetQty")), "UN_POSTPONE_NET_QTY", queryVO.getFieldValueByFieldName("unPostponeNetQty"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("heightQty")), "HEIGHT_QTY", queryVO.getFieldValueByFieldName("heightQty"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("midQty")), "MID_QTY", queryVO.getFieldValueByFieldName("midQty"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("postponeQty")), "POSTPONE_QTY", queryVO.getFieldValueByFieldName("postponeQty"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("cycleReserveQty")), "CYCLE_RESERVE_QTY", queryVO.getFieldValueByFieldName("cycleReserveQty"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("conventionReserveQty")), "CONVENTION_RESERVE_QTY", queryVO.getFieldValueByFieldName("conventionReserveQty"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("isReachMinProductionQty")), "IS_REACH_MIN_PRODUCTION_QTY", queryVO.getFieldValueByFieldName("isReachMinProductionQty"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("minProductionQty")), "MIN_PRODUCTION_QTY", queryVO.getFieldValueByFieldName("minProductionQty"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("planType")), "PLAN_TYPE", queryVO.getFieldValueByFieldName("planType"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("mesMaterialCode")), "MES_MATERIAL_CODE", queryVO.getFieldValueByFieldName("mesMaterialCode"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("channel")), "CHANNEL", queryVO.getFieldValueByFieldName("channel"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("proSize")), "PRO_SIZE", queryVO.getFieldValueByFieldName("proSize"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("specifications")), "SPECIFICATIONS", queryVO.getFieldValueByFieldName("specifications"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("pattern")), "PATTERN", queryVO.getFieldValueByFieldName("pattern"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("hierarchy")), "HIERARCHY", queryVO.getFieldValueByFieldName("hierarchy"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("speed")), "SPEED", queryVO.getFieldValueByFieldName("speed"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("isImportantCustom")), "IS_IMPORTANT_CUSTOM", queryVO.getFieldValueByFieldName("isImportantCustom"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("isEnsurePlan")), "IS_ENSURE_PLAN", queryVO.getFieldValueByFieldName("isEnsurePlan"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("isEmergency")), "IS_EMERGENCY", queryVO.getFieldValueByFieldName("isEmergency"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("isDebitPlan")), "IS_DEBIT_PLAN", queryVO.getFieldValueByFieldName("isDebitPlan"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("deliveryDateDue")), "DELIVERY_DATE_DUE", queryVO.getFieldValueByFieldName("deliveryDateDue"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("isImport")), "IS_IMPORT", queryVO.getFieldValueByFieldName("isImport"));
    }


    @Override
    protected String getTypeCode(){
        return "2026012215";
    }


}
