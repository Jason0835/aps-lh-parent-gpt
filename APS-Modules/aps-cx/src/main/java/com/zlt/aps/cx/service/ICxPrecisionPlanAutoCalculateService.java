package com.zlt.aps.cx.service;

import com.ruoyi.common.core.web.domain.AjaxResult;

import java.util.List;

/**
 * 成型精度计划自动推算服务接口
 *
 * @author APS Team
 * @since 2026/04/13
 */
public interface ICxPrecisionPlanAutoCalculateService {

    /**
     * 自动推算成型精度计划（15天周期）
     *
     * @param year 年度
     * @return 推算结果
     */
    AjaxResult autoCalculateCxPrecisionPlan15Days(Integer year);

    /**
     * 自动推算成型精度计划（60天周期）
     *
     * @param year 年度
     * @return 推算结果
     */
    AjaxResult autoCalculateCxPrecisionPlan60Days(Integer year);

    /**
     * 根据设备保养计划ID列表生成并推算成型精度计划
     *
     * @param maintenancePlanIds 设备保养计划ID列表
     * @param cycleDays 周期天数（15/60）
     * @return 生成结果
     */
    AjaxResult generateFromMaintenancePlanByIds(List<Long> maintenancePlanIds, Integer cycleDays);
}
