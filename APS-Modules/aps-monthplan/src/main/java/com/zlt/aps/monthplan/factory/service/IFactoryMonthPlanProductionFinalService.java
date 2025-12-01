package com.zlt.aps.monthplan.factory.service;


import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.extension.service.IService;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.aps.monthplan.api.domain.dto.FactoryMonthPlanProdFinalQueryDto;
import com.zlt.aps.monthplan.api.domain.dto.TrialProductionPlanDto;
import com.zlt.aps.monthplan.api.domain.entity.FactoryMonthPlanProdFinal;
import com.zlt.aps.monthplan.api.domain.entity.MonthPlanProductionFinalResult;
import com.zlt.aps.monthplan.api.domain.vo.*;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：IFactoryMonthPlanProductionFinalService.java
 * 描    述：IFactoryMonthPlanProductionFinalService分厂月生产计划排产结果-生产计划排产结果-SKU后端接口
 * 一个SKU一条记录
 *
 * @author ZLT
 * @version 1.0
 * <p>
 * 修改记录：
 * 修改时间：...
 * 修 改 人：ZLT
 * 修改内容：...
 * @date 2025-09-22
 */
public interface IFactoryMonthPlanProductionFinalService extends IService<MonthPlanProductionFinalResult> {
    /**
     * 保存定稿数据
     * 将排产数据转移到定稿数据表
     * t_mp_month_production_day -> t_mp_month_plan_production_final
     *
     * @param param
     */
    void saveFinalizedData(FactoryMonthPlanProdFinal param);

    /**
     * 根据查询条件，获取列表数据
     * 通过isHandler觉得是否对开始日期和结束日期进行处理
     * true表示需要处理，false表示不处理
     *
     * @param queryWrapper
     * @param isHandler    是否对开始日期和结束日期处理
     * @return
     */
    List<MonthPlanProductionFinalResult> getList(Wrapper<MonthPlanProductionFinalResult> queryWrapper, boolean isHandler);

    /**
     * 统计分厂月生产计划排产结果-排产结果列表
     *
     * @param prodFinal
     * @return
     */
    MonthPlanStatisticsVo statistics(MonthPlanProductionFinalResult prodFinal);

    /**
     * 根据版本计划，统计日排产信息，包含日排产规格数及日排产总量
     *
     * @param query
     * @return
     */
    List<DayProductionTotalVo> statisticsDay(MonthPlanProductionFinalResult query);

    /**
     * 导入试制量试计划
     *
     * @param list        excel数据
     * @param importLogId 导入日志ID
     * @return
     */
    AjaxResult importTrialProductionPlan(List<TrialProductionPlanDto> list, long importLogId);

    /**
     * 导入调整计划--基础数据导入
     *
     * @param excelData   excel数据
     * @param importLogId 导入日志id
     * @return
     */
    @Transactional(rollbackFor = Exception.class)
    AjaxResult importAdjustPlan(List<MonthPlanProductionFinalResultVo> excelData, long importLogId);

    /**
     * 根据查询条件，获取某日的月计划排产数据
     *
     * @param queryCondition 查询条件
     * @return
     */
    List<FactoryMonthPlanDayProductionInfoVo> getMonthPlanDayProductionInfo(FactoryMonthPlanProdFinalQueryDto queryCondition);

    /**
     * 根据查询条件，获取对应的月计划定稿数据
     *
     * @param queryCondition 查询条件
     * @return
     */
    List<FactoryMonthPlanProdFinalVo> getProdResult(FactoryMonthPlanProdFinalQueryDto queryCondition);

    /**
     * 根据查询条件，获取日对应的月计划定稿数据
     *
     * @param queryCondition 查询条件
     * @return
     */
    List<FactoryMonthPlanProdFinalVo> getMonthPlanProdResult(FactoryMonthPlanProdFinalQueryDto queryCondition);
    /**
     *  .输入SAP代码后自动关联出字段：规格代码、施工代号、生胎代码、模具号、模具数量、共用模具、 月度库存量、 月均销量、理论备货量、
     * @param param SAP代码
     * @return 规格代码、施工代号、生胎代码、模具号、模具数量、共用模具、 月度库存量、 月均销量、理论备货量、
     */
    MonthPlanProductionFinalResult linkProductInfoByProductCode(MonthPlanProductionFinalResult param);
    /**
     * 输入订单数量后系统自动计算：库存分配数、净需求、排产量
     * @param param 订单数量
     * @return 库存分配数、净需求、排产量
     */
    MonthPlanProductionFinalResult calculateByOrderQty(MonthPlanProductionFinalResult param);
    /**
     * 新增规格
     * @param param 规格参数
     */
    @Transactional(rollbackFor = Exception.class)
    void addSpecifications(MonthPlanProductionFinalResult param);
    /**
     *  月计划手动调整-编辑计划
     * @param param 可编辑字段
     */
    @Transactional(rollbackFor = Exception.class)
    void editPlan(MonthPlanProductionFinalResult param);
    /**
     * 规格减量-1、规格直接减量为零
     * @param param
     */
    @Transactional(rollbackFor = Exception.class)
    void subtractSpecification(MonthPlanProductionFinalResult param);
}
