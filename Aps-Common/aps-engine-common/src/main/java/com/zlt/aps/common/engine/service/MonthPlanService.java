package com.zlt.aps.common.engine.service;

import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.aps.common.engine.domain.MdmMonthPlanAnalysis;
import com.zlt.aps.common.engine.domain.TCxEmbryoMonthPlanSurplus;
import org.apache.ibatis.javassist.NotFoundException;

import java.util.List;

/**
 * @author Gim
 * 月计划汇总
 */
public interface MonthPlanService {

    /**
     * 月计划剩余量汇总
     * @param planMainVersion 主计划版本
     * @param year 年
     * @param month 月
     * @param isFinal 是否定稿 0是1否
     */
    public AjaxResult monthPlanAmountSum(String planMainVersion, String year, String month, Integer isFinal);

    /**
     * 根据版本号进行重算
     * @param apsVersion 生产排程版本
     */
    public void recalculateByApsVersion(String apsVersion);

}
