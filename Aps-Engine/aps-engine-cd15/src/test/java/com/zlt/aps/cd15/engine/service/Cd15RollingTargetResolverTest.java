package com.zlt.aps.cd15.engine.service;

import com.zlt.aps.cd15.api.domain.entity.Cd15ScheduleResult;
import com.zlt.aps.cd15.api.domain.entity.Cd15ShiftConfig;
import com.zlt.aps.cd15.engine.algorithm.Cd15ShiftWindowResolver;
import com.zlt.aps.cd15.engine.mapper.Cd15AutoScheduleShiftMapper;
import com.zlt.aps.cd15.engine.mapper.Cd15EngineScheduleResultMapper;
import com.zlt.aps.cd15.engine.model.Cd15RollingParameters;
import com.zlt.aps.cd15.engine.model.Cd15RollingTarget;
import com.zlt.aps.cd15.engine.service.impl.Cd15RollingTargetResolverImpl;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.sql.Date;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/** 定时滚动目标班次解析测试。 */
public class Cd15RollingTargetResolverTest {

    @Mock private Cd15AutoScheduleShiftMapper shiftMapper;
    @Mock private Cd15EngineScheduleResultMapper resultMapper;
    private Cd15RollingTargetResolver resolver;

    @Before
    public void setUp() {
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(
                new MybatisConfiguration(), "");
        TableInfoHelper.initTableInfo(assistant, Cd15ScheduleResult.class);
        TableInfoHelper.initTableInfo(assistant, Cd15ShiftConfig.class);
        MockitoAnnotations.initMocks(this);
        resolver = new Cd15RollingTargetResolverImpl(
                shiftMapper, resultMapper, new Cd15ShiftWindowResolver());
        when(shiftMapper.selectList(any())).thenReturn(Arrays.asList(
                shift("01", "CLASS3", "06:00:00", 2, 1),
                shift("02", "CLASS4", "14:00:00", 2, 2),
                shift("03", "CLASS5", "22:00:00", 2, 3)));
    }

    @Test
    public void shouldResolveEarlyWindowToUpcomingShift() {
        when(resultMapper.selectList(any())).thenReturn(Collections.singletonList(
                result(LocalDate.of(2026, 7, 3), "B1")));

        Optional<Cd15RollingTarget> target = resolver.resolve(
                "116", LocalDateTime.of(2026, 7, 3, 13, 40), parameters());

        assertTrue(target.isPresent());
        assertEquals("02", target.get().getTargetShiftCode());
        assertEquals("CLASS4", target.get().getTargetClassField());
        assertEquals(LocalDateTime.of(2026, 7, 3, 14, 0),
                target.get().getHandoverTime());
    }

    @Test
    public void shouldReturnEmptyOutsideRollingWindow() {
        when(resultMapper.selectList(any())).thenReturn(Collections.singletonList(
                result(LocalDate.of(2026, 7, 3), "B1")));

        Optional<Cd15RollingTarget> target = resolver.resolve(
                "116", LocalDateTime.of(2026, 7, 3, 12, 0), parameters());

        assertFalse(target.isPresent());
    }

    private Cd15RollingParameters parameters() {
        return Cd15RollingParameters.builder()
                .earlyMinutes(30).lateMinutes(15).stableMinutes(5).build();
    }

    private Cd15ScheduleResult result(LocalDate date, String batchNo) {
        Cd15ScheduleResult result = new Cd15ScheduleResult();
        result.setScheduleDate(Date.valueOf(date));
        result.setCd15BatchNo(batchNo);
        return result;
    }

    private Cd15ShiftConfig shift(String code, String classField, String start,
                                  int scheduleDay, int shiftOrder) {
        Cd15ShiftConfig config = new Cd15ShiftConfig();
        config.setFactoryCode("116");
        config.setShiftCode(code);
        config.setClassField(classField);
        config.setStartTime(start);
        if ("06:00:00".equals(start)) {
            config.setEndTime("14:00:00");
        } else if ("14:00:00".equals(start)) {
            config.setEndTime("22:00:00");
        } else {
            config.setEndTime("06:00:00");
            config.setIsCrossDay(1);
        }
        config.setScheduleDay(scheduleDay);
        config.setDayShiftOrder(shiftOrder);
        config.setShiftOrder(shiftOrder);
        config.setIsActive(1);
        if (config.getIsCrossDay() == null) {
            config.setIsCrossDay(0);
        }
        return config;
    }
}
