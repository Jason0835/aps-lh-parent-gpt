package com.zlt.aps.monthplan.demand.service;


import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.zlt.aps.monthplan.api.domain.entity.DpDemandPlan;
import com.zlt.aps.monthplan.api.domain.entity.MpFactoryProductionVersion;
import com.zlt.bill.common.service.IDocService;

import java.util.List;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：IDpDemandPlanService.java
 * 描    述：IDpDemandPlanService需求计划后端接口
 *
 * @author yelq
 * @version 1.0
 * <p>
 * 修改记录：
 * 修改时间：...
 * 修 改 人：yelq
 * 修改内容：...
 * @date 2025-12-25
 */
public interface IDpDemandPlanService extends IDocService<DpDemandPlan> {
    /**
     * 生成需求计划
     *
     * @param createCondition 参数
     */
    void createMonthRequire(DpDemandPlan createCondition);

    /**
     * 根据需求版本号获取需求计划
     *
     * @param finalVersion 最终排产版本
     * @return 需求计划
     */
    List<DpDemandPlan> findDemandPlanByMonthPlanVersion(MpFactoryProductionVersion finalVersion);
    /**
     *  获取需求计划版本号列表
     * @param queryCondition 查询条件
     * @return 需求计划版本号列表
     */
    List<String> findMonthPlanVersion(DpDemandPlan queryCondition);

    /**
     * 生成调整需求计划
     *
     * @param createCondition 参数
     */
    List<DpDemandPlan> createAdjustRequire(DpDemandPlan createCondition);
    /**
     *  生成预测需求计划
     * @param createCondition
     * @param finalVersion
     * @return
     */
    List<DpDemandPlan> createPredictionRequire(DpDemandPlan createCondition,MpFactoryProductionVersion finalVersion);
    /**
     *  列表查询数据
     * @param queryWrapper
     * @return
     */
    List<DpDemandPlan> list(QueryWrapper<DpDemandPlan> queryWrapper);
}
