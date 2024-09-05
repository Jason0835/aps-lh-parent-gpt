package com.zlt.aps.common.engine.service.impl;

import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.aps.common.engine.domain.MdmMonthPlanAnalysis;
import com.zlt.aps.common.engine.domain.TCxEmbryoMonthPlanSurplus;
import com.zlt.aps.common.engine.planmain.MdmMonthPlanAmountSumService;
import com.zlt.aps.common.engine.service.MonthPlanService;
import org.apache.ibatis.javassist.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @author Gim
 */
@Service
public class MonthPlanServiceImpl implements MonthPlanService {

    @Autowired
    private MdmMonthPlanAmountSumService sumService;


    @Override
    public AjaxResult monthPlanAmountSum(String planMainVersion, String year, String month, Integer isFinal) {
        return sumService.monthPlanAmountSum(planMainVersion, year, month, isFinal);
    }

    @Override
    public void recalculateByApsVersion(String apsVersion) {
        sumService.recalculateByApsVersion(apsVersion);
    }
}
