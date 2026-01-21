package com.zlt.aps.monthplan.factory.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.zlt.aps.monthplan.api.domain.entity.MonthPlanNoProductionPlan;
import com.zlt.aps.monthplan.factory.mapper.MonthPlanNoProductionPlanMapper;
import com.zlt.aps.monthplan.factory.service.IMonthPlanNoProductionPlanService;
import com.zlt.common.utils.PubUtil;
import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import lombok.extern.slf4j.Slf4j;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;


import com.ruoyi.common.core.web.page.TableDataInfo;

import com.zlt.bill.common.controller.AbstractDocBizController;
import com.zlt.bill.common.service.IDocService ;

/**
* Copyright (c) 2022, All rights reserved。
* 文件名称：MonthPlanNoProductionPlanController.java
* 描    述：S2-0606.排产结果-未排产计划 控制层类：....
*@author yelq
*@date 2026-01-21
*@version 1.0
*
 *  修改记录：
*     修改时间：...
*     修 改 人：yelq
*     修改内容：...
*/
@Slf4j
@Api(tags = "S2-0606.排产结果-未排产计划")
@RestController
@RequestMapping("/monthPlanNoProductionPlan")
public class MonthPlanNoProductionPlanController extends AbstractDocBizController<MonthPlanNoProductionPlan> {

    @Autowired
    private IMonthPlanNoProductionPlanService monthPlanNoProductionPlanService;

    @Autowired
    private MonthPlanNoProductionPlanMapper entityMapper;

    /**
     * 查询S2-0606.排产结果-未排产计划列表
     */
    @ApiOperation("查询列表")
    @PostMapping("/list")
    @Override
    public TableDataInfo list(@RequestBody MonthPlanNoProductionPlan queryVO) {
        return super.list(queryVO);
    }

    @Override
    protected String getOrderBy() {
        return "create_time desc";
    }

    /**
     * 保存
     */
    @Log(title = "ui.data.column.monthPlanNoProductionPlan.modelName", businessType = BusinessType.INSERT_OR_UPDATE)
    @ApiOperation("保存")
    @PostMapping("/save")
    @Override
    public AjaxResult save(@RequestBody MonthPlanNoProductionPlan billVO){
        return super.save(billVO);
    }

    /**
     * 删除
     */
    @Log(title = "ui.data.column.monthPlanNoProductionPlan.modelName", businessType = BusinessType.DELETE)
    @ApiOperation("删除")
    @DeleteMapping("/remove")
    @Override
    public AjaxResult removeByIds(@RequestBody List<Long> ids){
        return super.removeByIds(ids);
    }


    /**
     * 获取S2-0606.排产结果-未排产计划详细信息
     */
    @ApiOperation("获取详细信息")
    @GetMapping(value = "/{billId}")
    @Override
    public MonthPlanNoProductionPlan getInfo(@PathVariable("billId") Long billId) {
        return super.getInfo(billId);
    }


    /**
     * 根据集合导入S2-0606.排产结果-未排产计划数据
     * @param importContext 导入上下文
     * @param updateSupport 已存在记录是否更新
     * @return 结果
     */
    @Log(title = "ui.data.column.monthPlanNoProductionPlan.modelName", businessType = BusinessType.IMPORT)
    @ApiOperation("导入数据")
    @PostMapping("/importData")
    @Override
    public AjaxResult importData(@RequestBody ImportContext importContext, @RequestParam("updateSupport") boolean updateSupport) throws Exception {
        return super.importData(importContext,updateSupport);
    }

    /**
     * 导出列表
     */
    @Log(title = "S2-0606.排产结果-未排产计划", businessType = BusinessType.EXPORT)
    @ApiOperation("导入数据")
    @PostMapping("/exportData/{fileName}")
    @Override
    public byte[] exportData(@RequestBody MonthPlanNoProductionPlan queryVO, @PathVariable("fileName") String fileName,
                             HttpServletResponse response) throws IOException {
        return super.exportData(queryVO, fileName, response);
    }

    @Override
    protected List<MonthPlanNoProductionPlan> listExportData(MonthPlanNoProductionPlan obj) {
        QueryWrapper<MonthPlanNoProductionPlan> wrapper = new QueryWrapper<>();
        this.builderCondition(wrapper, obj);
        return entityMapper.selectList(wrapper);
    }

    @Override
    protected IDocService getDocService(){
        return monthPlanNoProductionPlanService;
    }

    /**
     * 条件拼接
     *
     * @param queryWrapper
     * @param queryVO
     */
    @Override
    protected void builderCondition(QueryWrapper<MonthPlanNoProductionPlan> queryWrapper, MonthPlanNoProductionPlan queryVO) {
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("factoryCode")), "FACTORY_CODE", queryVO.getFieldValueByFieldName("factoryCode"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("year")), "YEAR", queryVO.getFieldValueByFieldName("year"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("month")), "MONTH", queryVO.getFieldValueByFieldName("month"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("monthPlanVersion")), "MONTH_PLAN_VERSION", queryVO.getFieldValueByFieldName("monthPlanVersion"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("productionVersion")), "PRODUCTION_VERSION", queryVO.getFieldValueByFieldName("productionVersion"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("monthPlanId")), "MONTH_PLAN_ID", queryVO.getFieldValueByFieldName("monthPlanId"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("productTypeCode")), "PRODUCT_TYPE_CODE", queryVO.getFieldValueByFieldName("productTypeCode"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("locationType")), "LOCATION_TYPE", queryVO.getFieldValueByFieldName("locationType"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("brand")), "BRAND", queryVO.getFieldValueByFieldName("brand"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("isPrioritize")), "IS_PRIORITIZE", queryVO.getFieldValueByFieldName("isPrioritize"));
        queryWrapper.like(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("structureName")), "STRUCTURE_NAME", queryVO.getFieldValueByFieldName("structureName"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("mainMaterialDesc")), "MAIN_MATERIAL_DESC", queryVO.getFieldValueByFieldName("mainMaterialDesc"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("mesMaterialCode")), "MES_MATERIAL_CODE", queryVO.getFieldValueByFieldName("mesMaterialCode"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("materialCode")), "MATERIAL_CODE", queryVO.getFieldValueByFieldName("materialCode"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("materialDesc")), "MATERIAL_DESC", queryVO.getFieldValueByFieldName("materialDesc"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("specifications")), "SPECIFICATIONS", queryVO.getFieldValueByFieldName("specifications"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("pattern")), "PATTERN", queryVO.getFieldValueByFieldName("pattern"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("productionType")), "PRODUCTION_TYPE", queryVO.getFieldValueByFieldName("productionType"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("constructionStage")), "CONSTRUCTION_STAGE", queryVO.getFieldValueByFieldName("constructionStage"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("yearWeek")), "YEAR_WEEK", queryVO.getFieldValueByFieldName("yearWeek"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("isDynamicBalance")), "IS_DYNAMIC_BALANCE", queryVO.getFieldValueByFieldName("isDynamicBalance"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("isTrialPlan")), "IS_TRIAL_PLAN", queryVO.getFieldValueByFieldName("isTrialPlan"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("isUniformity")), "IS_UNIFORMITY", queryVO.getFieldValueByFieldName("isUniformity"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("isProduction")), "IS_PRODUCTION", queryVO.getFieldValueByFieldName("isProduction"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("orderQty")), "ORDER_QTY", queryVO.getFieldValueByFieldName("orderQty"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("stockQty")), "STOCK_QTY", queryVO.getFieldValueByFieldName("stockQty"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("averageSaleQty")), "AVERAGE_SALE_QTY", queryVO.getFieldValueByFieldName("averageSaleQty"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("plannedSurplus")), "PLANNED_SURPLUS", queryVO.getFieldValueByFieldName("plannedSurplus"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("netQty")), "NET_QTY", queryVO.getFieldValueByFieldName("netQty"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("postponeNetQty")), "POSTPONE_NET_QTY", queryVO.getFieldValueByFieldName("postponeNetQty"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("unPostponeNetQty")), "UN_POSTPONE_NET_QTY", queryVO.getFieldValueByFieldName("unPostponeNetQty"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("heightQty")), "HEIGHT_QTY", queryVO.getFieldValueByFieldName("heightQty"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("midQty")), "MID_QTY", queryVO.getFieldValueByFieldName("midQty"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("postponeQty")), "POSTPONE_QTY", queryVO.getFieldValueByFieldName("postponeQty"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("cycleReserveQty")), "CYCLE_RESERVE_QTY", queryVO.getFieldValueByFieldName("cycleReserveQty"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("conventionReserveQty")), "CONVENTION_RESERVE_QTY", queryVO.getFieldValueByFieldName("conventionReserveQty"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("heightLossQty")), "HEIGHT_LOSS_QTY", queryVO.getFieldValueByFieldName("heightLossQty"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("factProdReqQty")), "FACT_PROD_REQ_QTY", queryVO.getFieldValueByFieldName("factProdReqQty"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("unProductionQty")), "UN_PRODUCTION_QTY", queryVO.getFieldValueByFieldName("unProductionQty"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("reason")), "REASON", queryVO.getFieldValueByFieldName("reason"));
    }


    @Override
    protected String getTypeCode(){
        return "2026012110";
    }


}
