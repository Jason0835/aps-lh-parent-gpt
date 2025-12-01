package com.zlt.aps.monthplan.factory.handler.impl;

import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.aps.monthplan.factory.handler.FactoryMonthPlanAdjustHandler;
import com.zlt.aps.monthplan.factory.handler.MonthPlanAdjustHelper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 原规格计划的调整处理实现
 * 需要根据每日判断是调增还是调减
 *
 * @author ZLT
 * @date 20250322
 */
@Slf4j
@Service("originalProductAdjustService")
public class OriginalProductAdjustService implements FactoryMonthPlanAdjustHandler {
    @Override
    public AjaxResult calculateMonthPlanAdjust(MonthPlanAdjustHelper monthPlanAdjustHelper) {
        return null;
    }
}
