package com.zlt.aps.lh.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.zlt.aps.lh.api.domain.dto.SkuScheduleDTO;
import com.zlt.aps.lh.api.domain.entity.LhScheduleResult;
import com.zlt.aps.lh.context.LhScheduleContext;
import com.zlt.aps.lh.engine.strategy.support.ActiveMachineBinding;
import com.zlt.aps.lh.engine.strategy.support.OffMachineDecision;
import com.zlt.aps.lh.engine.strategy.support.PreviousAlternatePlanReleaseEvent;
import com.zlt.aps.lh.engine.strategy.support.TimedMachineOffRequest;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** 时间下机整批预演快照；通用候选快照之外补齐续作提交和历史事件的可变状态。 */
final class TimedMachineOffPreviewSnapshot {
    /** 通用数量、模具、首检、机台及结果快照，只在整批预演开始时捕获一次。 */
    private final ScheduleSubstitutionAttemptSnapshot resources;
    /** 原历史事件身份和字段，恢复时继续使用原对象。 */
    private final Map<String, PreviousAlternatePlanReleaseEvent> events;
    private final Map<PreviousAlternatePlanReleaseEvent, PreviousAlternatePlanReleaseEvent> eventStates = new IdentityHashMap<>();
    /** 时间下机请求画像为只读对象，保留原结果身份。 */
    private final List<TimedMachineOffRequest> requests;
    private final Map<LhScheduleResult, OffMachineDecision> decisions;
    /** 正式续作只能扣账一次，模拟扣账后必须恢复该标记。 */
    private final boolean dailyQuotaSynced;
    private final boolean loadPreview;
    private final boolean alternateGroupPreview;
    private final String currentStep;
    /** 原机台扫描范围及续作释放状态。 */
    private final Set<String> machineScope;
    private final Set<String> releasedMachines;
    private final Set<String> typeBlockReleasedMachines;
    private final Set<String> firstDayReleasedMachines;
    private final Set<String> activeStopHoldMachines;
    private final Set<String> releasedStopHoldMachines;
    private final Map<String, Integer> releaseBoundaries;
    private final Map<String, LocalDate> groupReleaseDates;
    /** 预演生成的候选、历史模具选择及零量清理不能带入正式阶段。 */
    private final List<SkuScheduleDTO> continuousSkus;
    private final List<SkuScheduleDTO> trialCandidates;
    private final Map<String, List<String>> plannedMoulds;
    private final Set<LhScheduleResult> endingResults;
    /** 已有跨日绑定字段在预演续排中可变，不能只恢复列表。 */
    private final Map<ActiveMachineBinding, ActiveMachineBinding> bindingStates = new IdentityHashMap<>();
    /** 模拟候选的告警不能出现在正式批次。 */
    private final List<String> warnings;

    /** @param context 待预演上下文 @param sources 全部可能被预演消费的来源SKU */
    TimedMachineOffPreviewSnapshot(LhScheduleContext context, Collection<SkuScheduleDTO> sources) {
        resources = ScheduleSubstitutionAttemptSnapshot.capture(context, sources);
        events = new LinkedHashMap<>(context.getPreviousAlternateReleaseEventMap());
        events.values().forEach(event -> eventStates.put(event,
                BeanUtil.copyProperties(event, PreviousAlternatePlanReleaseEvent.class)));
        requests = new ArrayList<>(context.getTimedMachineOffRequests());
        decisions = new IdentityHashMap<>(context.getTimedMachineOffDecisionMap());
        dailyQuotaSynced = context.isContinuousDailyQuotaSynced();
        loadPreview = context.isTimedMachineOffLoadPreview();
        alternateGroupPreview = context.isPreviousAlternateGroupPreview();
        currentStep = context.getCurrentStep();
        machineScope = new LinkedHashSet<>(context.getNewSpecMachineResourceScopeCodeSet());
        releasedMachines = new LinkedHashSet<>(context.getReleasedContinuousMachineCodeSet());
        typeBlockReleasedMachines = new LinkedHashSet<>(context.getTypeBlockReleasedContinuousMachineCodeSet());
        firstDayReleasedMachines = new LinkedHashSet<>(context.getFirstDayNoPlanReleasedContinuousMachineCodeSet());
        activeStopHoldMachines = new LinkedHashSet<>(context.getActiveContinuousStopHoldMachineCodeSet());
        releasedStopHoldMachines = new LinkedHashSet<>(context.getReleasedContinuousStopHoldMachineCodeSet());
        releaseBoundaries = new LinkedHashMap<>(context.getContinuousReducedMachineReleaseBoundaryShiftIndexMap());
        groupReleaseDates = new LinkedHashMap<>(context.getReducedContinuationGroupLastReleaseDateMap());
        continuousSkus = new ArrayList<>(context.getContinuousSkuList());
        trialCandidates = new ArrayList<>(context.getTrialVirtualMachineCandidateList());
        plannedMoulds = new LinkedHashMap<>();
        context.getPreviousAlternatePlannedMouldMap().forEach((machine, moulds) ->
                plannedMoulds.put(machine, new ArrayList<>(moulds)));
        endingResults = Collections.newSetFromMap(new IdentityHashMap<>());
        endingResults.addAll(context.getContinuationSurplusEndingAllocatedResults());
        context.getPreScheduledMachineBindingList().forEach(binding -> bindingStates.put(binding, this.copyBinding(binding)));
        warnings = new ArrayList<>(context.getWarningMessageList());
    }

    /** @param context 模拟上下文，正常结束和异常退出均完整恢复 */
    void restore(LhScheduleContext context) {
        eventStates.forEach((event, state) -> BeanUtil.copyProperties(state, event));
        bindingStates.forEach((binding, state) -> BeanUtil.copyProperties(state, binding));
        context.setPreviousAlternateReleaseEventMap(new LinkedHashMap<>(events));
        context.setTimedMachineOffRequests(new ArrayList<>(requests));
        context.setTimedMachineOffDecisionMap(new IdentityHashMap<>(decisions));
        context.setContinuousDailyQuotaSynced(dailyQuotaSynced);
        context.setTimedMachineOffLoadPreview(loadPreview);
        context.setPreviousAlternateGroupPreview(alternateGroupPreview);
        context.setCurrentStep(currentStep);
        context.setNewSpecMachineResourceScopeCodeSet(new LinkedHashSet<>(machineScope));
        context.setReleasedContinuousMachineCodeSet(new LinkedHashSet<>(releasedMachines));
        context.setTypeBlockReleasedContinuousMachineCodeSet(new LinkedHashSet<>(typeBlockReleasedMachines));
        context.setFirstDayNoPlanReleasedContinuousMachineCodeSet(new LinkedHashSet<>(firstDayReleasedMachines));
        context.setActiveContinuousStopHoldMachineCodeSet(new LinkedHashSet<>(activeStopHoldMachines));
        context.setReleasedContinuousStopHoldMachineCodeSet(new LinkedHashSet<>(releasedStopHoldMachines));
        context.setContinuousReducedMachineReleaseBoundaryShiftIndexMap(new LinkedHashMap<>(releaseBoundaries));
        context.setReducedContinuationGroupLastReleaseDateMap(new LinkedHashMap<>(groupReleaseDates));
        context.setContinuousSkuList(new ArrayList<>(continuousSkus));
        context.setTrialVirtualMachineCandidateList(new ArrayList<>(trialCandidates));
        context.setPreviousAlternatePlannedMouldMap(new LinkedHashMap<>(plannedMoulds));
        Set<LhScheduleResult> restoredEndingResults = Collections.newSetFromMap(new IdentityHashMap<>());
        restoredEndingResults.addAll(endingResults);
        context.setContinuationSurplusEndingAllocatedResults(restoredEndingResults);
        resources.restore(context);
        context.setWarningMessageList(new ArrayList<>(warnings));
    }

    /** @param binding 已有绑定 @return 保留不可变身份并冻结三个可变跨日字段 */
    private ActiveMachineBinding copyBinding(ActiveMachineBinding binding) {
        ActiveMachineBinding copied = new ActiveMachineBinding(binding.getSkuKey(), binding.getSku(),
                binding.getMachineCode(), binding.getPairMachineCode(), binding.getScheduleResult(),
                binding.getPairScheduleResult(), binding.isEndingTarget());
        copied.setMachineDemandStartDate(binding.getMachineDemandStartDate());
        copied.setPreparedThroughDate(binding.getPreparedThroughDate());
        copied.setEstimatedEndTime(binding.getEstimatedEndTime());
        return copied;
    }
}
