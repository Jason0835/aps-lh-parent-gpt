package com.zlt.aps.maindata.service;

import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.aps.mp.api.domain.entity.MpMouldDeliveryPlan;
import com.zlt.bill.common.service.IDocService;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：IMpMouldDeliveryPlanService.java
 * 描    述：IMpMouldDeliveryPlanService模具到货计划后端接口
 *
 * @author zlt
 * @version 1.0
 * <p>
 * 修改记录：
 * 修改时间：...
 * 修 改 人：zlt
 * 修改内容：...
 * @date 2025-12-05
 */
public interface IMpMouldDeliveryPlanService extends IDocService<MpMouldDeliveryPlan> {

    /**
     * 根据计划发货日期获取计划上机日期
     *
     * @param entity 计划发货日期
     * @return 结果
     */
    AjaxResult getBoardingDate(MpMouldDeliveryPlan entity);

    /**
     * 更新主花纹到物料
     *
     * @param queryVO 查询条件
     * @return 结果
     */
    AjaxResult updateMainPatternToMaterial(MpMouldDeliveryPlan queryVO);
}
