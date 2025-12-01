package com.zlt.aps.monthplan.factory.handler;

import com.zlt.aps.monthplan.api.domain.entity.FactoryMonthPlanProdFinal;
import com.zlt.aps.monthplan.api.domain.vo.FactoryMonthPlanFinalVersionInfoVo;
import com.zlt.aps.monthplan.api.domain.vo.FactoryMonthPlanProdFinalVo;
import lombok.Data;

import java.io.Serializable;
import java.util.List;
import java.util.Set;

/**
 * 分厂月份计划调整辅助类
 *
 * @author ZLT
 * @date 20250320
 */
@Data
public class MonthPlanAdjustHelper implements Serializable {
    /**
     * 调整后的计划
     */
    private FactoryMonthPlanProdFinalVo adjustPlan;
    /**
     * 旧计划数据--可为空
     */
    private FactoryMonthPlanProdFinal originalPlan;
    /**
     * 版本信息
     */
    private FactoryMonthPlanFinalVersionInfoVo finalVersion;
    /**
     * 调整结果明细
     */
    private List<FactoryMonthPlanProdFinal> adjustDetailList;
    /**
     * 最大可调整日列表
     */
    private Set<Integer> adjustDateList;

    /**
     * @param adjustPlan   需要调整的计划
     * @param originalPlan 原有计划信息--可为空
     * @param finalVersion 定稿版本
     */
    public MonthPlanAdjustHelper(FactoryMonthPlanProdFinalVo adjustPlan, FactoryMonthPlanProdFinal originalPlan, FactoryMonthPlanFinalVersionInfoVo finalVersion) {
        this.adjustPlan = adjustPlan;
        this.originalPlan = originalPlan;
        this.finalVersion = finalVersion;
    }
}
