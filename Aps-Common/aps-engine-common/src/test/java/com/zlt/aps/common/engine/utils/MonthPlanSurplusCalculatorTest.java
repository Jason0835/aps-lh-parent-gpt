package com.zlt.aps.common.engine.utils;

import com.zlt.aps.mp.api.domain.entity.FactoryMonthPlanProductionFinalResult;
import org.junit.Assert;
import org.junit.Test;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * 月计划硫化余量共享计算器测试。
 *
 * @author APS Team
 */
public class MonthPlanSurplusCalculatorTest {

    /**
     * 验证物料与产品状态使用独立复合键。
     */
    @Test
    public void shouldBuildIndependentKeysForDifferentProductStatuses() {
        String formalKey = MonthPlanSurplusCalculator.buildMaterialStatusKey("MAT001", "S");
        String trialKey = MonthPlanSurplusCalculator.buildMaterialStatusKey("MAT001", "T");

        Assert.assertEquals("MAT001|*|S", formalKey);
        Assert.assertEquals("MAT001|*|T", trialKey);
        Assert.assertNotEquals(formalKey, trialKey);
    }

    /**
     * 验证空状态和字段两侧空格按统一规则归一化。
     */
    @Test
    public void shouldNormalizeBlankProductStatus() {
        Assert.assertEquals("MAT001|*|",
                MonthPlanSurplusCalculator.buildMaterialStatusKey(" MAT001 ", null));
        Assert.assertEquals("MAT001|*|",
                MonthPlanSurplusCalculator.buildMaterialStatusKey("MAT001", "  "));
    }

    /**
     * 验证同一物料不同产品状态的计划量不会合并。
     */
    @Test
    public void shouldCalculatePlanQtyByProductStatus() {
        FactoryMonthPlanProductionFinalResult formalPlan = this.buildPlan("S", 100);
        FactoryMonthPlanProductionFinalResult trialPlan = this.buildPlan("T", 200);
        List<FactoryMonthPlanProductionFinalResult> monthPlans = Arrays.asList(formalPlan, trialPlan);
        List<Date> productionDates = Collections.singletonList(
                MonthPlanSurplusCalculator.getDate(LocalDate.of(2026, 7, 1)));

        Map<YearMonth, Integer> formalPlanQtyMap = MonthPlanSurplusCalculator.getPlanQty(
                productionDates, monthPlans, formalPlan, 1);
        Map<YearMonth, Integer> trialPlanQtyMap = MonthPlanSurplusCalculator.getPlanQty(
                productionDates, monthPlans, trialPlan, 1);

        Assert.assertEquals(Integer.valueOf(100), formalPlanQtyMap.get(YearMonth.of(2026, 7)));
        Assert.assertEquals(Integer.valueOf(200), trialPlanQtyMap.get(YearMonth.of(2026, 7)));
    }

    /**
     * 构建测试月计划。
     *
     * @param productStatus 产品状态
     * @param day1Qty       一日计划量
     * @return 月计划
     */
    private FactoryMonthPlanProductionFinalResult buildPlan(String productStatus, int day1Qty) {
        FactoryMonthPlanProductionFinalResult plan = new FactoryMonthPlanProductionFinalResult();
        plan.setFactoryCode("116");
        plan.setMaterialCode("MAT001");
        plan.setProductStatus(productStatus);
        plan.setYear(2026);
        plan.setMonth(7);
        plan.setDay1(day1Qty);
        return plan;
    }
}
