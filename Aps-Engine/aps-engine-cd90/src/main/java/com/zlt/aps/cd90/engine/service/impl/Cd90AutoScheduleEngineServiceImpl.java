package com.zlt.aps.cd90.engine.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.aps.cd90.api.domain.entity.Cd90ShiftConfig;
import com.zlt.aps.cd90.engine.algorithm.Cd90EnabledShiftConfigValidator;
import com.zlt.aps.cd90.engine.algorithm.Cd90ShiftWindowResolver;
import com.zlt.aps.cd90.engine.algorithm.Cd90AutoScheduleOutputDraftBuilder;
import com.zlt.aps.cd90.engine.algorithm.Cd90MultiShiftScheduleExecutor;
import com.zlt.aps.cd90.engine.mapper.Cd90AutoScheduleShiftMapper;
import com.zlt.aps.cd90.engine.model.Cd90AutoScheduleContext;
import com.zlt.aps.cd90.engine.model.Cd90AutoScheduleParameters;
import com.zlt.aps.cd90.engine.model.Cd90AutoScheduleOutputDraft;
import com.zlt.aps.cd90.engine.model.Cd90MultiShiftExecutionResult;
import com.zlt.aps.cd90.engine.model.Cd90ShiftDescriptor;
import com.zlt.aps.cd90.engine.service.Cd90AutoScheduleEngineService;
import com.zlt.aps.cd90.engine.service.Cd90AutoScheduleParameterService;
import com.zlt.aps.cd90.engine.service.Cd90AutoScheduleInputVersionService;
import com.zlt.aps.cd90.engine.service.Cd90ScheduleProgressListener;
import com.zlt.aps.cd90.engine.algorithm.Cd90AutoScheduleRuntimeGuard;
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
    private final Cd90EnabledShiftConfigValidator enabledShiftConfigValidator;
    private final Cd90AutoScheduleParameterService parameterService;
    private final Cd90ShiftWindowResolver shiftWindowResolver;
    private final Cd90MultiShiftScheduleExecutor multiShiftScheduleExecutor;
    private final Cd90AutoScheduleOutputDraftBuilder outputDraftBuilder;
    private final Cd90AutoScheduleInputVersionService inputVersionService;
    private final Cd90AutoScheduleRuntimeGuard runtimeGuard;

    @Override
    public Cd90AutoScheduleContext prepare(String factoryCode, Date scheduleDate) {
        // 第一步只准备不可变计算上下文，不在此阶段创建任务或写入排程结果。
        if (!StringUtils.hasText(factoryCode)) {
            // 信息：自动排程工厂编码不能为空
            throw new ServiceException(I18nUtil.getMessage("ui.cd90.autoSchedule.factoryCodeEmpty"));
        }
        if (scheduleDate == null) {
            // 信息：自动排程日期不能为空
            throw new ServiceException(I18nUtil.getMessage("ui.cd90.autoSchedule.scheduleDateEmpty"));
        }
        LocalDate localScheduleDate = scheduleDate.toInstant()
                .atZone(ZoneId.systemDefault())
                .toLocalDate();

        log.info("[直裁自动排程] Engine开始准备计算上下文, factoryCode={}, scheduleDate={}, stage={}",
                factoryCode, localScheduleDate, STAGE_BASIC_VALIDATION);

        // 班次数量参与参数校验，必须先加载班次，再解析输出窗口等强类型参数。
        List<Cd90ShiftConfig> enabledShifts = loadEnabledShifts(factoryCode);
        enabledShiftConfigValidator.validate(enabledShifts);
        Cd90AutoScheduleParameters parameters = parameterService.load(factoryCode, enabledShifts.size());
        // 输出窗口按业务班次顺序截取，保证后续滚动计算和结果CLASS字段顺序一致。
        List<Cd90ShiftDescriptor> shifts = shiftWindowResolver.resolve(localScheduleDate, enabledShifts)
                .stream().limit(parameters.getScheduleWindow()).collect(Collectors.toList());
        LocalDateTime startTime = LocalDateTime.now();
        Cd90ShiftDescriptor resourceBaselineShift = shiftWindowResolver
                .resolveScheduleBaselineShift(localScheduleDate, enabledShifts);
        // 页面全窗口共用排程窗口首班次的一份资源基线，最终事务前按同一基线复核版本。
        Cd90AutoScheduleContext context = Cd90AutoScheduleContext.builder()
                .factoryCode(factoryCode)
                .scheduleDate(localScheduleDate)
                .startTime(startTime)
                .resourceBaselineDate(resourceBaselineShift.getScheduleDate())
                .resourceBaselineShiftCode(resourceBaselineShift.getShiftCode())
                .currentStage(STAGE_BASIC_VALIDATION)
                .parameters(parameters)
                .shifts(shifts)
                .enabledShiftCount(enabledShifts.size())
                .inputVersionFingerprint(inputVersionService.fingerprint(
                        factoryCode, localScheduleDate,
                        resourceBaselineShift.getScheduleDate(),
                        resourceBaselineShift.getShiftCode()))
                .build();

        log.info("[直裁自动排程] Engine计算上下文准备完成, factoryCode={}, scheduleDate={}, "
                        + "resourceBaselineDate={}, resourceBaselineShiftCode={}, "
                        + "enabledShiftCount={}, scheduleWindow={}, fingerprint={}",
                factoryCode, localScheduleDate,
                resourceBaselineShift.getScheduleDate(), resourceBaselineShift.getShiftCode(),
                enabledShifts.size(),
                parameters.getScheduleWindow(), parameters.getFingerprint());
        return context;
    }

    @Override
    public Cd90AutoScheduleOutputDraft execute(Cd90AutoScheduleContext context) {
        return execute(context, Cd90ScheduleProgressListener.NO_OP);
    }

    @Override
    public Cd90AutoScheduleOutputDraft execute(Cd90AutoScheduleContext context,
                                               Cd90ScheduleProgressListener listener) {
        if (context == null) {
            throw new IllegalArgumentException("自动排程计算上下文不能为空");
        }
        log.info("[直裁自动排程] Engine开始执行多班排程, factoryCode={}, scheduleDate={}",
                context.getFactoryCode(), context.getScheduleDate());
        // 多班循环、草稿归并前后均检查总耗时，超时任务不得进入最终持久化阶段。
        runtimeGuard.checkNotTimedOut(context, "多班排程开始");
        Cd90MultiShiftExecutionResult execution = multiShiftScheduleExecutor.execute(context, listener);
        runtimeGuard.checkNotTimedOut(context, "输出草稿构建前");
        // Engine仅生成可持久化草稿，批次号、数据库ID和旧批次失效由外层最终事务处理。
        Cd90AutoScheduleOutputDraft output = outputDraftBuilder.build(context, execution);
        runtimeGuard.checkNotTimedOut(context, "输出草稿构建完成");
        if (listener != null) listener.onProgress(90, "BUILD_OUTPUT", "输出草稿构建完成", null);
        log.info("[直裁自动排程] Engine输出草稿构建完成, resultCount={}, unscheduledCount={}",
                output.getScheduleResults().size(), output.getUnscheduledResults().size());
        return output;
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


}
