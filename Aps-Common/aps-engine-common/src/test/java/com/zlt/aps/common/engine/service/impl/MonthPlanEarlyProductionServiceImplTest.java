package com.zlt.aps.common.engine.service.impl;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.zlt.aps.common.engine.mapper.EarlyProductionMonthPlanMapper;
import com.zlt.aps.common.engine.mapper.EarlyProductionVersionMapper;
import com.zlt.aps.mp.api.domain.entity.FactoryMonthPlanProductionFinalResult;
import com.zlt.aps.mp.api.domain.entity.MpFactoryProductionVersion;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import static org.junit.jupiter.api.Assertions.*;

/**
 * 窗口内无正月计划的提前日期准入测试。
 *
 * @author APS
 * @date 2026/11/16
 */
class MonthPlanEarlyProductionServiceImplTest {
    /**
     * 窗口开始、中间及结束日任一天有量均排除，即使其他日负量使合计为零。
     */
    @Test
    void excludesAnyPositiveWindowDay() {
        MonthPlanEarlyProductionServiceImpl service = new MonthPlanEarlyProductionServiceImpl(null, null);
        for (int day = 16; day <= 18; day++) {
            FactoryMonthPlanProductionFinalResult plan = this.plan("S", 11);
            plan.setDay16(-10);
            plan.setFieldValueByFieldName("day" + day, 10);
            plan.setDay19(20);
            assertTrue(service.calculateFirstPlanDates(
                    "116", LocalDate.of(2026, 11, 18), 2, Collections.singletonList(plan)).isEmpty());
        }
    }

    /**
     * 同物料不同状态独立，窗口无量的状态仍返回未来最早日。
     */
    @Test
    void separatesStatusesAndFiltersZeroFutureDays() {
        FactoryMonthPlanProductionFinalResult formal = this.plan("S", 11);
        formal.setDay16(5);
        formal.setDay19(20);
        FactoryMonthPlanProductionFinalResult trial = this.plan("F", 11);
        trial.setDay18(0);
        trial.setDay19(0);
        trial.setDay20(30);
        MonthPlanEarlyProductionServiceImpl service = new MonthPlanEarlyProductionServiceImpl(null, null);
        com.zlt.aps.common.engine.domain.dto.MonthPlanFirstDateDTO result = service.calculateFirstPlanDates(
                "116", LocalDate.of(2026, 11, 18), 2,
                Arrays.asList(formal, trial)).get(0);
        assertEquals("F", result.getProductStatus());
        assertEquals(LocalDate.of(2026, 11, 20), result.getFirstPlanDate());
    }

    /**
     * 跨月多行任一窗口正量均排除整组，不受输入顺序影响。
     */
    @Test
    void excludesAcrossMonthRowsInEitherOrder() {
        FactoryMonthPlanProductionFinalResult november = this.plan("S", 11);
        november.setDay30(10);
        FactoryMonthPlanProductionFinalResult december = this.plan("S", 12);
        december.setDay2(20);
        MonthPlanEarlyProductionServiceImpl service = new MonthPlanEarlyProductionServiceImpl(null, null);
        assertTrue(service.calculateFirstPlanDates(
                "116", LocalDate.of(2026, 12, 1), 2, Arrays.asList(december, november)).isEmpty());
        assertTrue(service.calculateFirstPlanDates(
                "116", LocalDate.of(2026, 12, 1), 2, Arrays.asList(november, december)).isEmpty());
        november.setDay30(0);
        assertEquals(LocalDate.of(2026, 12, 2), service.calculateFirstPlanDates("116",
                LocalDate.of(2026, 12, 1), 2,
                Arrays.asList(november, december)).get(0).getFirstPlanDate());
    }

    /**
     * 排除同月其他需求行的窗口正计划，不能只检查未来计划所在行。
     */
    @Test
    void excludesDuplicateMonthRows() {
        FactoryMonthPlanProductionFinalResult window = this.plan("S", 11);
        window.setDay17(10);
        FactoryMonthPlanProductionFinalResult future = this.plan("S", 11);
        future.setDay19(20);
        assertTrue(new MonthPlanEarlyProductionServiceImpl(null, null).calculateFirstPlanDates(
                "116", LocalDate.of(2026, 11, 18), 2,
                Arrays.asList(future, window)).isEmpty());
    }

    /**
     * 查询范围包含窗口所在月份，不能只查未来月份；空日期或非正提前天数不查库。
     */
    @Test
    void queriesWindowMonthAndRejectsInvalidWindow() {
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(new MybatisConfiguration(), "window-test");
        assistant.setCurrentNamespace("window.test");
        TableInfoHelper.initTableInfo(assistant, MpFactoryProductionVersion.class);
        EarlyProductionVersionMapper versionMapper = Mockito.mock(EarlyProductionVersionMapper.class);
        EarlyProductionMonthPlanMapper planMapper = Mockito.mock(EarlyProductionMonthPlanMapper.class);
        Mockito.when(versionMapper.selectList(Mockito.any())).thenAnswer(invocation -> {
            LambdaQueryWrapper<MpFactoryProductionVersion> query = invocation.getArgument(0);
            query.getSqlSegment();
            assertTrue(query.getParamNameValuePairs().containsValue(11));
            assertTrue(query.getParamNameValuePairs().containsValue(12));
            return Collections.emptyList();
        });
        MonthPlanEarlyProductionServiceImpl service = new MonthPlanEarlyProductionServiceImpl(planMapper, versionMapper);
        assertTrue(service.listFirstPlanDates("116", null, 2).isEmpty());
        assertTrue(service.listFirstPlanDates("116", LocalDate.of(2026, 11, 30), 0).isEmpty());
        service.listFirstPlanDates("116", LocalDate.of(2026, 11, 30), 2);
        Mockito.verify(versionMapper, Mockito.times(1)).selectList(Mockito.any());
        Mockito.verifyNoInteractions(planMapper);
    }

    /**
     * 构造同物料的月计划行。
     *
     * @param status 产品状态
     * @param month 月份
     * @return 月计划
     */
    private FactoryMonthPlanProductionFinalResult plan(String status, int month) {
        FactoryMonthPlanProductionFinalResult plan = new FactoryMonthPlanProductionFinalResult();
        plan.setFactoryCode("116");
        plan.setMaterialCode("A");
        plan.setProductStatus(status);
        plan.setYear(2026);
        plan.setMonth(month);
        return plan;
    }
}
