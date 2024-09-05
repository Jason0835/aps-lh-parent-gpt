package com.zlt.aps.mps.service;

import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.aps.common.engine.domain.TSyncMps2ApsFac;

import java.util.List;

/**
 * @author Gim
 * 月计划汇总
 */
public interface MonthPlanSumService {

//    public List<TSyncMps2ApsFac> getMps2ApsFac(Integer year, Integer month, String productVersion);

    int checkMpsExist(Integer year, Integer month, String productVersion);

    /**
     * 月度计划数据抽取
     * 主计划同步
     */
    public AjaxResult monthPlanAmountSum(String planMainVersion, String year, String month, Integer isFinal);

}
