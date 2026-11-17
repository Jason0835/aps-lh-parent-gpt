package com.zlt.aps.common.engine.utils;

import com.zlt.aps.common.engine.domain.LhSkuSurplusControlParam;
import com.zlt.aps.common.engine.domain.LhSurplusResultVo;
import com.zlt.aps.common.engine.domain.LhSurplusSkuInfo;
import com.zlt.aps.mp.api.domain.entity.FactoryMonthPlanProductionFinalResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 硫化余量「断点段」（{@link LhSurplusResultVo#getSegmentStart()} / {@link LhSurplusResultVo#getSegmentEnd()}）回归测试。
 *
 * <p>断点必须以<b>排产周期</b>为锚：周期内有量最大日往后找起点、再往后找连续断点。
 * 成型按胎胚维度合并余量账户时读这个段，不能用「40 天计划起始日起首个连续段」，
 * 否则窗口之前的历史段会把窗口之后的连续段截断（现网 3302001654 断在 10/26、漏并 3302001655 的 312）。</p>
 *
 * <p>注意：项目编译级别为 Java 1.8，禁止使用 List.of/Map.of。</p>
 *
 * @author APS Team
 */
public class LhSkuSurplusSegmentTest {

    /** 被测物料编码：与现网 3302001654 一致的账户维度 */
    private static final String MATERIAL_CODE = "3302001654";

    /**
     * 场景一：10 月有历史段（10/22~10/26）、10/27~11/12 空 17 天、11/13 起连续到 11/30。
     * 断点应锚在排产周期之后（11/30），不能被 10 月历史段的空档截断成 10/26。
     */
    @Test
    @DisplayName("窗口锚定断点：10月历史段不截断11月连续段")
    public void windowAnchoredSegmentIgnoresEarlierGap() {
        FactoryMonthPlanProductionFinalResult october = this.buildMonthPlan(2026, 10);
        for (int day = 22; day <= 26; day++) {
            this.setDayQty(october, day, 48);
        }
        FactoryMonthPlanProductionFinalResult november = this.buildMonthPlan(2026, 11);
        this.setDayQty(november, 13, 30);
        for (int day = 14; day <= 18; day++) {
            this.setDayQty(november, day, 96);
        }
        for (int day = 19; day <= 23; day++) {
            this.setDayQty(november, day, 48);
        }
        this.setDayQty(november, 24, 32);
        for (int day = 25; day <= 29; day++) {
            this.setDayQty(november, day, 48);
        }
        this.setDayQty(november, 30, 40);

        LhSurplusResultVo result = this.calc(october, Arrays.asList(october, november), 364, 0);

        assertNotNull(result);
        assertEquals(LocalDate.of(2026, 11, 14), result.getSegmentStart(), "段起点应为周期后首个有量日");
        assertEquals(LocalDate.of(2026, 11, 30), result.getSegmentEnd(), "断点应为窗口锚定的连续段末日");
    }

    /**
     * 场景二：周期内无量且窗口前仍有欠量 → 前段余量分支，段 = 计划起始日 ~ 最早排产日。
     */
    @Test
    @DisplayName("前段余量：段止于最早排产日并标记逾期")
    public void beforeSurplusSegmentStopsAtFirstScheduleDate() {
        FactoryMonthPlanProductionFinalResult october = this.buildMonthPlan(2026, 10);
        for (int day = 22; day <= 26; day++) {
            this.setDayQty(october, day, 48);
        }

        LhSurplusResultVo result = this.calc(october, Collections.singletonList(october), 0, 0);

        assertNotNull(result);
        assertTrue(result.isOverdueSurplusFlag(), "应标记前段（逾期）余量");
        assertEquals(LocalDate.of(2026, 10, 21), result.getSegmentStart());
        assertEquals(LocalDate.of(2026, 11, 11), result.getSegmentEnd());
    }

    /**
     * 场景三：周期内无量、窗口前无欠量，后续 11/24~11/30 另有一段（同胚另一 SKU 的典型形态）。
     * 段应取后段 11/24~11/30，供胎胚维度与种子账户合并。
     */
    @Test
    @DisplayName("周期后另一段计划：段取后段起止")
    public void forwardSegmentAfterCycle() {
        FactoryMonthPlanProductionFinalResult october = this.buildMonthPlan(2026, 10);
        for (int day = 22; day <= 26; day++) {
            this.setDayQty(october, day, 48);
        }
        FactoryMonthPlanProductionFinalResult november = this.buildMonthPlan(2026, 11);
        this.setDayQty(november, 24, 32);
        for (int day = 25; day <= 29; day++) {
            this.setDayQty(november, day, 48);
        }
        this.setDayQty(november, 30, 40);

        // 完成量已覆盖前段（240），6.1 前段分支不触发，走 6.1.2 后段
        LhSurplusResultVo result = this.calc(october, Arrays.asList(october, november), 240, 0);

        assertNotNull(result);
        assertEquals(LocalDate.of(2026, 11, 24), result.getSegmentStart());
        assertEquals(LocalDate.of(2026, 11, 30), result.getSegmentEnd());
    }

    /**
     * 构造指定年月的月计划行（同物料、同产品状态）。
     *
     * @param year  年
     * @param month 月
     * @return 月计划行
     */
    private FactoryMonthPlanProductionFinalResult buildMonthPlan(int year, int month) {
        FactoryMonthPlanProductionFinalResult plan = new FactoryMonthPlanProductionFinalResult();
        plan.setFactoryCode("116");
        plan.setYear(year);
        plan.setMonth(month);
        plan.setMaterialCode(MATERIAL_CODE);
        plan.setProductStatus("S");
        return plan;
    }

    /**
     * 设置指定日期的计划量：实体只提供 day1~day31 字段，按 Lombok setter 反射写入，避免 31 分支 switch。
     *
     * @param plan 月计划行
     * @param day  日期（1-31）
     * @param qty  计划量
     */
    private void setDayQty(FactoryMonthPlanProductionFinalResult plan, int day, int qty) {
        try {
            Method setter = FactoryMonthPlanProductionFinalResult.class.getMethod("setDay" + day, Integer.class);
            setter.invoke(plan, qty);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("设置月计划逐日量失败: day=" + day, e);
        }
    }

    /**
     * 按现网排产周期（T=2026-11-11、窗口末日=2026-11-13、计划起始日=2026-10-21）计算余量。
     *
     * @param sku               被计算账户
     * @param allMonthPlanList  全部月计划
     * @param finishedQty       完成量
     * @param maxDiscontinueDays SYS0304038 允许断开天数
     * @return 硫化余量结果
     */
    private LhSurplusResultVo calc(FactoryMonthPlanProductionFinalResult sku,
                                   List<FactoryMonthPlanProductionFinalResult> allMonthPlanList,
                                   int finishedQty, Integer maxDiscontinueDays) {
        List<Date> cycle = Arrays.asList(
                MonthPlanSurplusCalculator.getDate(LocalDate.of(2026, 11, 11)),
                MonthPlanSurplusCalculator.getDate(LocalDate.of(2026, 11, 12)),
                MonthPlanSurplusCalculator.getDate(LocalDate.of(2026, 11, 13)));
        LhSkuSurplusControlParam param = new LhSkuSurplusControlParam(
                MonthPlanSurplusCalculator.getDate(LocalDate.of(2026, 10, 21)),
                false, cycle, 0, maxDiscontinueDays);
        LhSurplusSkuInfo skuInfo = new LhSurplusSkuInfo(
                sku, null, allMonthPlanList, Collections.emptyMap(), finishedQty, Collections.emptyList());
        return LhSkuSurplusCalculator.getSurplusInfo(param, skuInfo);
    }
}
