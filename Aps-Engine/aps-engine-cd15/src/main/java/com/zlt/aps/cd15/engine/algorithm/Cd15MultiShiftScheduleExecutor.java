package com.zlt.aps.cd15.engine.algorithm;

import com.zlt.aps.cd15.engine.model.Cd15AutoScheduleContext;
import com.zlt.aps.cd15.engine.model.Cd15AutoScheduleInput;
import com.zlt.aps.cd15.engine.model.Cd15SteelStripSourceTrace;
import com.zlt.aps.cd15.engine.model.Cd15ConstructionMaterial;
import com.zlt.aps.cd15.engine.model.Cd15MultiShiftExecutionResult;
import com.zlt.aps.cd15.engine.model.Cd15NewSpecAdvanceResult;
import com.zlt.aps.cd15.engine.model.Cd15RollingScheduleContext;
import com.zlt.aps.cd15.engine.model.Cd15ScheduleAttemptTrace;
import com.zlt.aps.cd15.engine.model.Cd15ShiftDescriptor;
import com.zlt.aps.cd15.engine.model.Cd15ShiftExecutionResult;
import com.zlt.aps.cd15.engine.model.Cd15ShiftResourceState;
import com.zlt.aps.cd15.engine.service.Cd15AutoScheduleInputService;
import com.zlt.aps.cd15.engine.service.impl.Cd15NewSpecAdvanceInputPreparer;
import com.zlt.aps.cd15.engine.service.Cd15ShiftDemandProvider;
import com.zlt.aps.cd15.engine.service.Cd15SingleShiftScheduleService;
import com.zlt.aps.cd15.engine.service.Cd15ScheduleProgressListener;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 按输出窗口顺序执行全部斜裁班次的内存滚动编排器。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class Cd15MultiShiftScheduleExecutor {

    private final Cd15AutoScheduleInputService inputService;
    private final Cd15NewSpecAdvanceInputPreparer newSpecAdvanceInputPreparer;
    private final Cd15SingleShiftScheduleService singleShiftScheduleService;
    private final Cd15ShiftDemandProvider demandProvider;
    private final Cd15RollingScheduleContextManager rollingContextManager;
    private final Cd15UnscheduledResultAggregator unscheduledResultAggregator;
    private final Cd15AutoScheduleRuntimeGuard runtimeGuard;

    /**
     * 每个班次重新加载输入、生成候选和提交任务，当前阶段不写数据库。
     */
    public Cd15MultiShiftExecutionResult execute(Cd15AutoScheduleContext context) {
        return execute(context, Cd15ScheduleProgressListener.NO_OP);
    }

    /** 每班开始和结束均执行超时检查并通知任务进度。 */
    public Cd15MultiShiftExecutionResult execute(Cd15AutoScheduleContext context,
                                                 Cd15ScheduleProgressListener listener) {
        if (context == null || context.getParameters() == null
                || context.getShifts() == null || context.getShifts().isEmpty()) {
            throw new IllegalArgumentException("多班排程上下文、参数和班次窗口不能为空");
        }
        List<Cd15ShiftExecutionResult> shiftResults = new ArrayList<>();
        List<Cd15ScheduleAttemptTrace> attemptTraces = new ArrayList<>();
        Cd15RollingScheduleContext rolling = null;
        Map<String, Cd15SteelStripSourceTrace> steelStripSourceTraceBySteelStrip = Collections.emptyMap();

        log.info("[斜裁自动排程] 多班循环执行开始, factoryCode={}, scheduleDate={}, shiftCount={}",
                context.getFactoryCode(), context.getScheduleDate(), context.getShifts().size());
        Cd15ScheduleProgressListener progressListener = listener == null
                ? Cd15ScheduleProgressListener.NO_OP : listener;
        int shiftCount = context.getShifts().size();
        for (int index = 0; index < shiftCount; index++) {
            Cd15ShiftDescriptor shift = context.getShifts().get(index);
            // 每班独立做超时检查和进度上报，便于异步任务准确显示当前卡点。
            runtimeGuard.checkNotTimedOut(context, shiftStageName(shift, "班次开始"));
            progressListener.onProgress(progress(index, shiftCount), "SHIFT_EXECUTION",
                    shiftStageName(shift, "班次开始"), shift);
            // 每班重新读取需求、库存和大卷数据；库排始终读取任务启动时当前班次基线。
            Cd15AutoScheduleInput input = inputService.load(
                    context.getFactoryCode(), context.getScheduleDate(),
                    shift.getClassField(), shift.getShiftCode(),
                    context.getResourceBaselineDate(),
                    context.getResourceBaselineShiftCode(),
                    context.getParameters().getAgingPeriodHours());
            if (rolling == null) {
                steelStripSourceTraceBySteelStrip = input.getSteelStripSourceTraceBySteelStrip() == null
                        ? Collections.emptyMap()
                        : Collections.unmodifiableMap(
                                new HashMap<>(input.getSteelStripSourceTraceBySteelStrip()));
                // 首班锁定新增规格判定与需求搬移快照，后续班次不得重查历史改变同批次口径。
                Cd15NewSpecAdvanceResult advanceResult = this.newSpecAdvanceInputPreparer
                        .prepare(context, input);
                input.setPlanningDemandShifts(advanceResult.getAdjustedDemandShifts());
                input.setNewSpecAdvanceInfoBySteelStrip(advanceResult.getAdvanceInfoBySteelStrip());
                // 仅首班使用6点库排和新增规格证据建立基线。
                rolling = this.rollingContextManager.initialize(
                        input.getStorageLanesAtSix(), advanceResult.getAdvanceInfoBySteelStrip());
            } else {
                // 每班重载原始需求后，使用首班证据重建去重计划视图。
                this.newSpecAdvanceInputPreparer.applySnapshot(
                        input, rolling.getNewSpecAdvanceInfoBySteelStrip());
            }
            // 先累计成型消耗，再叠加前序实际/计划入库，得到当前班开始时可见的资源状态。
            Map<String, BigDecimal> cumulativeConsumptionBySteelStrip = demandProvider
                    .cumulativeConsumptionBySteelStripBeforeShift(context, input, shift);
            rollingContextManager.updateCumulativeConsumption(rolling, cumulativeConsumptionBySteelStrip);
            Cd15ShiftResourceState initialState = rollingContextManager.openShift(
                    rolling, shift, buildCurlLengthBySteelStrip(input, context.getParameters().getRollCoilMeter()),
                    context.getParameters().getRollCoilMeter(), context.getParameters().getRollTotalCount(),
                    Collections.emptyList());
            initialState.setBigRollAgingStocks(rollingContextManager.restoreBigRollAllocations(
                    rolling, input.getBigRollAgingStocks()));
            // 单班执行只修改当前班的内存副本；完成后才推进跨班滚动状态。
            Cd15ShiftExecutionResult result = singleShiftScheduleService.execute(
                    context, input, shift, initialState, rolling);
            rollingContextManager.completeShift(rolling, result.getState());
            shiftResults.add(result);
            if (result.getAttemptTraces() != null) {
                // 将班内序号转换为全窗口稳定序号，未排原因按该顺序去重和确定主因。
                for (Cd15ScheduleAttemptTrace trace : result.getAttemptTraces()) {
                    trace.setSequence(attemptTraces.size() + 1);
                    attemptTraces.add(trace);
                }
            }
            runtimeGuard.checkNotTimedOut(context, shiftStageName(shift, "班次完成"));
            progressListener.onProgress(progress(index + 1, shiftCount), "SHIFT_EXECUTION",
                    shiftStageName(shift, "班次完成"), shift);
        }
        log.info("[斜裁自动排程] 多班循环执行完成, factoryCode={}, scheduleDate={}, "
                        + "shiftCount={}, taskCount={}",
                context.getFactoryCode(), context.getScheduleDate(), shiftResults.size(),
                rolling == null ? 0 : rolling.getCommittedTasks().size());
        // 所有班次结束后统一汇总未排，避免某一班失败就过早判定最终未排数量。
        return Cd15MultiShiftExecutionResult.builder()
                .shiftResults(shiftResults).rollingContext(rolling)
                .attemptTraces(attemptTraces)
                .unscheduledResults(unscheduledResultAggregator.aggregate(attemptTraces))
                .steelStripSourceTraceBySteelStrip(steelStripSourceTraceBySteelStrip)
                .build();
    }

    private String shiftStageName(Cd15ShiftDescriptor shift, String suffix) {
        String displayName = shift == null ? null : shift.getShiftDisplayName();
        String classField = shift == null ? "" : shift.getClassField();
        String shiftName = displayName == null || displayName.trim().isEmpty()
                ? classField : displayName;
        return shiftName + suffix;
    }
    private Map<String, BigDecimal> buildCurlLengthBySteelStrip(Cd15AutoScheduleInput input,
                                                           BigDecimal fallbackCoilMeter) {
        if (input == null || input.getConstructionMaterials() == null) {
            return new HashMap<>();
        }
        return input.getConstructionMaterials().stream()
                .filter(item -> item != null && item.getSteelStripCode() != null)
                .collect(Collectors.toMap(Cd15ConstructionMaterial::getSteelStripCode,
                        item -> item.getCurlLength() == null || item.getCurlLength().signum() <= 0
                                ? fallbackCoilMeter : item.getCurlLength(),
                        (first, second) -> first));
    }

    private int progress(int completedShiftCount, int shiftCount) {
        return 20 + (completedShiftCount * 65 / shiftCount);
    }
}
