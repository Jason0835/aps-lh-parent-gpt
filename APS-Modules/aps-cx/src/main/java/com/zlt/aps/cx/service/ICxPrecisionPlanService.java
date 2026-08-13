package com.zlt.aps.cx.service;

import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.aps.cx.api.domain.entity.CxMachineInfo;
import com.zlt.aps.cx.api.domain.entity.CxPrecisionPlan;
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


    AjaxResult autoCalculateCxPrecisionPlan(Integer year);

    /**
     * 按设备保养计划(MES同步数据)分发写入成型精度计划表
     * 现逻辑：MES全权决定计划时间(OPER_TIME)和实际完成时间(FIRST_WASH_TIME)，
     * APS侧不再回填实际日期、不再生成下一次精度计划。
     * 本方法根据MES字段值直接计算派生字段并upsert到T_CX_PRECISION_PLAN。
     * 匹配键：MES_SOURCE_ID（=T_MDM_DEV_MAINTENANCE_PLAN.ID）
     * 精度类型映射：MES "成型精度15天"→APS PRECISION_TYPE='成型精度', PRECISION_CYCLE='15'
     *
     * @param maintenancePlanIds 设备保养计划ID列表（处理PRECISION_TYPE以'成型精度'开头的数据）
     * @return 分发写入的记录数
     */
    int dispatchFromMaintenancePlan(List<Long> maintenancePlanIds);
}
