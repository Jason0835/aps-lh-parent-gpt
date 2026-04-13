package com.zlt.aps.lh.service;

import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.aps.mp.api.domain.entity.MdmDevMaintenancePlan;

import java.util.List;

/**
 * 精度计划自动推算服务接口
 *
 * @author APS Team
 * @since 2026/04/13
 */
public interface IPrecisionPlanAutoCalculateService {

    /**
     * 自动推算硫化精度计划（年度）
     *
     * @param year 年度
     * @return 推算结果
     */
    AjaxResult autoCalculateLhPrecisionPlan(Integer year);

    /**
     * 根据设备保养计划ID列表生成并推算精度计划
     *
     * @param maintenancePlanIds 设备保养计划ID列表
     * @param precisionType 精度类型
     * @return 生成结果
     */
    AjaxResult generateFromMaintenancePlanByIds(List<Long> maintenancePlanIds, String precisionType);

    /**
     * 根据设备保养计划自动生成并推算精度计划
     *
     * @param maintenancePlans 设备保养计划列表
     * @return 生成结果
     */
    AjaxResult generateAndCalculateFromMaintenancePlan(List<MdmDevMaintenancePlan> maintenancePlans);
}
