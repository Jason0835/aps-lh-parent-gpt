package com.zlt.aps.monthplan.factory.handler;

import com.ruoyi.common.core.web.domain.AjaxResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
/**
 * 分厂计划调整业务实现处理类
 *
 * @author ZLT
 * @date 20250320
 */
public interface FactoryMonthPlanAdjustHandler {
    /**
     * 根据调整计划列表信息，计算计划调整明细结果
     *
     * @param monthPlanAdjustHelper
     * @return
     */
    AjaxResult calculateMonthPlanAdjust(MonthPlanAdjustHelper monthPlanAdjustHelper);
}
