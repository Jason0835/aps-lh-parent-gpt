package com.zlt.aps.cx.service;

import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.aps.cx.api.domain.entity.CxMachineInfo;
import com.zlt.aps.cx.api.domain.entity.CxPrecisionPlan;
import com.zlt.aps.mp.api.domain.entity.MdmDevMaintenancePlan;
import com.zlt.bill.common.service.IDocService;

import java.util.List;

public interface ICxPrecisionPlanService extends IDocService<CxPrecisionPlan> {

    String checkUnique(CxPrecisionPlan entity);

    AjaxResult importData(List<CxPrecisionPlan> list, boolean updateSupport, Long importLogId);

    int generatePlansFromMes(Integer year);

    int autoGenerateYearlyPlans(Integer year);

    int checkWarning();

    int batchUpdateDaysToDue();

    boolean updateActualDate(Long mesSourceId, String actualDate);

    /**
     * 根据设备保养计划生成成型精度计划
     *
     * @param maintenancePlans 设备保养计划列表
     * @param cycleDays 周期天数（15/60）
     * @return 生成数量
     */
    int generateFromMaintenancePlan(List<MdmDevMaintenancePlan> maintenancePlans, Integer cycleDays);

    /**
     * 按周期自动生成成型精度计划
     *
     * @param year 年度
     * @param cycleDays 周期天数（15/60）
     * @return 生成数量
     */
    int autoGenerateByCycle(Integer year, Integer cycleDays);


}
