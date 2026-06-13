package com.zlt.aps.cd90.engine.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zlt.aps.cd90.api.domain.entity.Cd90ShiftConfig;
import com.zlt.aps.cd90.engine.algorithm.Cd90ShiftWindowResolver;
import com.zlt.aps.cd90.engine.mapper.Cd90AutoScheduleShiftMapper;
import com.zlt.aps.cd90.engine.model.Cd90AutoScheduleContext;
import com.zlt.aps.cd90.engine.model.Cd90AutoScheduleParameters;
import com.zlt.aps.cd90.engine.model.Cd90ShiftDescriptor;
import com.zlt.aps.cd90.engine.service.Cd90AutoScheduleEngineService;
import com.zlt.aps.cd90.engine.service.Cd90AutoScheduleParameterService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 直裁自动排程引擎入口实现。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class Cd90AutoScheduleEngineServiceImpl implements Cd90AutoScheduleEngineService {

    private static final int ACTIVE = 1;
    private static final String STAGE_BASIC_VALIDATION = "基础数据校验";

    private final Cd90AutoScheduleShiftMapper shiftMapper;
    private final Cd90AutoScheduleParameterService parameterService;
    private final Cd90ShiftWindowResolver shiftWindowResolver;

    @Override
    public Cd90AutoScheduleContext prepare(String factoryCode, Date scheduleDate) {
        validateRequest(factoryCode, scheduleDate);
        LocalDate localScheduleDate = scheduleDate.toInstant()
                .atZone(ZoneId.systemDefault())
                .toLocalDate();

        log.info("[直裁自动排程] Engine开始准备计算上下文, factoryCode={}, scheduleDate={}, stage={}",
                factoryCode, localScheduleDate, STAGE_BASIC_VALIDATION);

        List<Cd90ShiftConfig> enabledShifts = loadEnabledShifts(factoryCode);
        Cd90AutoScheduleParameters parameters = parameterService.load(factoryCode, enabledShifts.size());
        List<Cd90ShiftDescriptor> shifts = shiftWindowResolver.resolve(localScheduleDate, enabledShifts)
                .stream().limit(parameters.getScheduleWindow()).collect(Collectors.toList());
        Cd90AutoScheduleContext context = Cd90AutoScheduleContext.builder()
                .factoryCode(factoryCode)
                .scheduleDate(localScheduleDate)
                .startTime(LocalDateTime.now())
                .currentStage(STAGE_BASIC_VALIDATION)
                .parameters(parameters)
                .shifts(shifts)
                .build();

        log.info("[直裁自动排程] Engine计算上下文准备完成, factoryCode={}, scheduleDate={}, "
                        + "enabledShiftCount={}, scheduleWindow={}, fingerprint={}",
                factoryCode, localScheduleDate, enabledShifts.size(),
                parameters.getScheduleWindow(), parameters.getFingerprint());
        return context;
    }

    /**
     * 加载当前工厂启用班次，逻辑删除由框架统一处理。
     *
     * @param factoryCode 工厂编码
     * @return 启用班次配置
     */
    private List<Cd90ShiftConfig> loadEnabledShifts(String factoryCode) {
        LambdaQueryWrapper<Cd90ShiftConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Cd90ShiftConfig::getFactoryCode, factoryCode)
                .eq(Cd90ShiftConfig::getIsActive, ACTIVE)
                .orderByAsc(Cd90ShiftConfig::getScheduleDay)
                .orderByAsc(Cd90ShiftConfig::getDayShiftOrder)
                .orderByAsc(Cd90ShiftConfig::getShiftOrder)
                .orderByAsc(Cd90ShiftConfig::getClassField);
        return shiftMapper.selectList(wrapper);
    }

    /**
     * 校验自动排程入口参数。
     *
     * @param factoryCode 工厂编码
     * @param scheduleDate 排程日期
     */
    private void validateRequest(String factoryCode, Date scheduleDate) {
        if (!StringUtils.hasText(factoryCode)) {
            throw new IllegalArgumentException("自动排程工厂编码不能为空");
        }
        if (scheduleDate == null) {
            throw new IllegalArgumentException("自动排程日期不能为空");
        }
    }
}
