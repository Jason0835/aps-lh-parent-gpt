package com.zlt.aps.monthplan.factory.service;

import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.aps.monthplan.api.domain.entity.FactoryMonthPlanProdFinal;
import com.zlt.aps.monthplan.api.domain.vo.DaySizeCapacityConfigurationDetailVo;
import com.zlt.aps.monthplan.api.domain.vo.DaySizeCapacityConfigurationMouldMethodDetailVo;
import com.zlt.aps.monthplan.factory.helper.AddQtyAdjustPlanHelper;
import com.zlt.aps.monthplan.factory.helper.AdjustProductConstructionInfoHelper;

import java.util.List;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：IFactoryMonthPlanAdjustPlanBusinessService.java
 * 描    述：IFactoryMonthPlanAdjustPlanBusinessService-月计划计划调整业务接口类
 *
 * @author zlt
 * @version 1.0
 * <p>
 * 修改记录：
 * 修改时间：...
 * 修 改 人：zlt
 * 修改内容：...
 * @date 20250607
 */
public interface IFactoryMonthPlanAdjustPlanBusinessService {
    /**
     * 校验从startDate的最大产能能否支撑其调整量
     * 模具产能限制，寸口产能限制，天产能限制
     *
     * @param helper     施工信息
     * @param addQtyInfo 增量信息
     * @return
     */
    AjaxResult checkMaxQtyByStartDate(AdjustProductConstructionInfoHelper helper, AddQtyAdjustPlanHelper addQtyInfo);

    /**
     * 校验从startDate的最大模具产能能否支撑其调整量
     *
     * @param helper
     * @param addQtyInfo
     * @return
     */
    AjaxResult checkMaxMouldQtyByStartDate(AdjustProductConstructionInfoHelper helper, AddQtyAdjustPlanHelper addQtyInfo);

    /**
     * 对相应计划调减后，是否可进行增量计划
     * 天产能数，寸口产能数
     *
     * @param helper
     * @param addQtyInfo
     * @param updateToDateSubtractList
     * @return
     */
    AjaxResult checkAfterSubtractOtherPlan(AdjustProductConstructionInfoHelper helper, AddQtyAdjustPlanHelper addQtyInfo, List<FactoryMonthPlanProdFinal> updateToDateSubtractList);

    /**
     * 对相应计划调减后，是否可进行增量计划
     * 模具产能校验
     *
     * @param helper
     * @param addQtyInfo
     * @param updateToDateSubtractList
     * @return
     */
    AjaxResult checkAfterSubtractOtherPlanByMould(AdjustProductConstructionInfoHelper helper, AddQtyAdjustPlanHelper addQtyInfo, List<FactoryMonthPlanProdFinal> updateToDateSubtractList);

    /**
     * 获取分厂，年，月的产能配置详情情况
     *
     * @param factoryCode 分厂
     * @param year        年
     * @param month       月
     * @return
     */
    List<DaySizeCapacityConfigurationDetailVo> getDaySizeCapacityInfo(String factoryCode, Integer year, Integer month);


    /**
     * 获取分厂，年，月的产能配置详情情况
     *
     * @param factoryCode 分厂
     * @param year        年
     * @param month       月
     * @return
     */
    List<DaySizeCapacityConfigurationMouldMethodDetailVo> getDaySizeCapacityInfoByMouldMethod(String factoryCode, Integer year, Integer month);
}
