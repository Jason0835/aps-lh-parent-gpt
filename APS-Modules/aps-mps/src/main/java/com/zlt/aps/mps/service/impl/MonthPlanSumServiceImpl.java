package com.zlt.aps.mps.service.impl;

import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.aps.common.engine.domain.TSyncMps2ApsFac;
import com.zlt.aps.common.engine.mapper.TSyncMps2ApsFacMapper;
import com.zlt.aps.common.engine.service.MonthPlanService;
import com.zlt.aps.mps.mapper.TServiceSyncLogMapper;
import com.zlt.aps.mps.service.MonthPlanSumService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

/**
 * @author Gim
 * 月计划汇总
 */
@Service
public class MonthPlanSumServiceImpl implements MonthPlanSumService {

    @Autowired
    private MonthPlanService monthPlanService;

    @Resource
//    private TSyncMps2ApsFacMapper syncMps2ApsFacMapper;
    private TServiceSyncLogMapper syncLogMapper;


//    @Override
//    public List<TSyncMps2ApsFac> getMps2ApsFac(Integer year, Integer month, String productVersion) {
//        return syncMps2ApsFacMapper.selectAllByYearAndMonthAndProductionVersion(year, month, productVersion);
//    }

    @Override
    public int checkMpsExist(Integer year, Integer month, String productVersion) {
        return syncLogMapper.checkMpsExist(year, month, productVersion);
    }

    @Override
    public AjaxResult monthPlanAmountSum(String planMainVersion, String year, String month, Integer isFinal) {
        return monthPlanService.monthPlanAmountSum(planMainVersion, year, month, isFinal);
    }

}
