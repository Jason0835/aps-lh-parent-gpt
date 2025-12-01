package com.zlt.aps.monthplan.factory.service;

import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.aps.monthplan.api.domain.dto.FactoryMonthPlanProdResultDto;
import com.zlt.aps.monthplan.api.domain.vo.FactoryMonthPlanAdjustPlanVo;
import com.zlt.aps.monthplan.api.domain.vo.MonthPlanAdjustInfoVo;

/**
 * 分厂控制台业务接口定义类
 *
 * @author ZLT
 * @date 20250320
 */
public interface IFactoryMonthPlanAdjustService {
    /**
     * 根据分厂、年份、月份获取计划调整控制信息对象
     *
     * @param param
     * @return
     */
    MonthPlanAdjustInfoVo getAdjustControlInfo(FactoryMonthPlanProdResultDto param);

    /**
     * 执行计划调整
     *
     * @param adjustPlan
     * @return
     */
    AjaxResult adjustMonthPlan(FactoryMonthPlanAdjustPlanVo adjustPlan);
}
