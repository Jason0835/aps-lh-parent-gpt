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


}
