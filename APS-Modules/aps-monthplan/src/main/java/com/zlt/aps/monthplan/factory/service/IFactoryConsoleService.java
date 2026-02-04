package com.zlt.aps.monthplan.factory.service;

import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.aps.monthplan.api.domain.entity.FactoryMonthPlanProdFinal;
import com.zlt.aps.monthplan.api.domain.entity.MpFactoryProductionVersion;
import com.zlt.aps.monthplan.api.domain.vo.*;
import com.zlt.aps.monthplan.factory.dto.FactoryProductionPlanVersionDto;

import java.util.List;

/**
 * 工厂控制台业务接口定义类
 *
 * @author ZLT
 * @date 20251203
 */
public interface IFactoryConsoleService {
    /**
     * 根据条件，获取对应工厂的销售需求计划版本及排产版本信息列表
     *
     * @param queryCondition
     * @return
     */
    List<FactoryProductionPlanVersionDto> getProductionVersionList(FactoryProductionPlanVo queryCondition);

    /**
     * 根据条件，获取对应工厂的还未选择的销售需求计划版本信息
     *
     * @param queryCondition
     * @return
     */
    List<FactoryMonthPlanVersionVo> getNoSelectedVersionList(FactoryProductionPlanVo queryCondition);

    /**
     * 一键排产，生产模具排产计划
     * 1.第一步初始化
     * 2.第二步排分组产能(结构)
     * 3.第三步模具排产
     *
     * @param factoryProductionParam
     * @return
     */
    AjaxResult oneClickProductionProcess(FactoryProductionParamVo factoryProductionParam);

    /**
     * 重新初始化模具排产的初始化数据
     *
     * @param factoryProductionParam
     * @return
     */
    AjaxResult reinitializeMouldingProduction(FactoryProductionParamVo factoryProductionParam);

    /**
     * 分组计划产能重新分配排产
     *
     * @param factoryProductionParam
     * @return
     */
    AjaxResult groupPlanCapacityResetAllocationProduction(FactoryProductionParamVo factoryProductionParam);
    /**
     * 重新进行模具排产
     *
     * @param factoryProductionParam
     * @return
     */
    AjaxResult reMouldingProduction(FactoryProductionParamVo factoryProductionParam);

    /**
     * 按工厂 + 年月 + 需求版本的方式，删除对应的需求版本的排产版本记录及排产版本数据
     *
     * @param factoryProductionParam
     * @return
     */
    AjaxResult deleteMonthPlanRequire(FactoryProductionParamVo factoryProductionParam);

    /**
     * 按工厂 + 年月 + 排产版本的方式删除排产计划版本
     *
     * @param factoryProductionParam
     * @return
     */
    AjaxResult deleteMonthPlanProductionVersion(FactoryProductionParamVo factoryProductionParam);

    /**
     * 查询对应年月+分厂的需求计划版本
     */
    List<String> versionList(MpFactoryProductionVersion query);

    /**
     * 查询对应年月+分厂+需求计划版本的分厂月计划版本
     *
     * @param query 查询条件
     * @return
     */
    List<String> productionVersionList(MpFactoryProductionVersion query);

    /**
     * 获取月份排产模式--Date 不为空则表示非自然月排产，Date为空表示自然月排产
     *
     * @param query
     * @return
     */
    FactoryMonthPlanTypeVo getProductionMonthType(FactoryMonthPlanProdFinal query);

    FactoryMonthPlanFinalVersionInfoVo getFinalVersionInfo(String factoryCode, Integer year, Integer month);

    /**
     * 检测
     * @param factoryProductionParam
     * @return
     */
    AjaxResult checkProductionDemandPlan(FactoryProductionParamVo factoryProductionParam);
}
