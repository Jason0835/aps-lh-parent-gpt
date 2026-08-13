package com.zlt.aps.cd90.engine.algorithm;

import com.zlt.aps.cd90.engine.model.Cd90AutoScheduleContext;
import com.zlt.aps.cd90.engine.model.Cd90AutoScheduleInput;
import com.zlt.aps.cd90.engine.model.Cd90ClothSourceTrace;
import com.zlt.aps.cd90.engine.model.Cd90ConstructionMaterial;
import com.zlt.aps.cd90.engine.model.Cd90MultiShiftExecutionResult;
import com.zlt.aps.cd90.engine.model.Cd90NewSpecAdvanceResult;
import com.zlt.aps.cd90.engine.model.Cd90RollingScheduleContext;
import com.zlt.aps.cd90.engine.model.Cd90ScheduleAttemptTrace;
import com.zlt.aps.cd90.engine.model.Cd90ShiftDescriptor;
import com.zlt.aps.cd90.engine.model.Cd90ShiftExecutionResult;
import com.zlt.aps.cd90.engine.model.Cd90ShiftResourceState;
import com.zlt.aps.cd90.engine.service.Cd90AutoScheduleInputService;
import com.zlt.aps.cd90.engine.service.impl.Cd90NewSpecAdvanceInputPreparer;
import com.zlt.aps.cd90.engine.service.Cd90ShiftDemandProvider;
import com.zlt.aps.cd90.engine.service.Cd90SingleShiftScheduleService;
import com.zlt.aps.cd90.engine.service.Cd90ScheduleProgressListener;
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
 * 按输出窗口顺序执行全部直裁班次的内存滚动编排器。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class Cd90MultiShiftScheduleExecutor {

    private final Cd90AutoScheduleInputService inputService;
    private final Cd90NewSpecAdvanceInputPreparer newSpecAdvanceInputPreparer;
    private final Cd90SingleShiftScheduleService singleShiftScheduleService;
    private final Cd90ShiftDemandProvider demandProvider;
    private final Cd90RollingScheduleContextManager rollingContextManager;
    private final Cd90UnscheduledResultAggregator unscheduledResultAggregator;
    private final Cd90AutoScheduleRuntimeGuard runtimeGuard;

    /**
     * 每个班次重新加载输入、生成候选和提交任务，当前阶段不写数据库。
     */
    public Cd90MultiShiftExecutionResult execute(Cd90AutoScheduleContext context) {
        return execute(context, Cd90ScheduleProgressListener.NO_OP);
    }

    /** 每班开始和结束均执行超时检查并通知任务进度。 */
    public Cd90MultiShiftExecutionResult execute(Cd90AutoScheduleContext context,
                                                 Cd90ScheduleProgressListener listener) {
        if (context == null || context.getParameters() == null
                || context.getShifts() == null || context.getShifts().isEmpty()) {
            throw new IllegalArgumentException("多班排程上下文、参数和班次窗口不能为空");
        }
        List<Cd90ShiftExecutionResult> shiftResults = new ArrayList<>();
        List<Cd90ScheduleAttemptTrace> attemptTraces = new ArrayList<>();
        Cd90RollingScheduleContext rolling = null;
        Map<String, Cd90ClothSourceTrace> clothSourceTraceByCloth = Collections.emptyMap();

        log.info("[直裁自动排程] 多班循环执行开始, factoryCode={}, scheduleDate={}, shiftCount={}",
                context.getFactoryCode(), context.getScheduleDate(), context.getShifts().size());
        Cd90ScheduleProgressListener progressListener = listener == null
                ? Cd90ScheduleProgressListener.NO_OP : listener;
        int shiftCount = context.getShifts().size();
        for (int index = 0; index < shiftCount; index++) {
            Cd90ShiftDescriptor shift = context.getShifts().get(index);
            // 每班独立做超时检查和进度上报，便于异步任务准确显示当前卡点。
            runtimeGuard.checkNotTimedOut(context, shiftStageName(shift, "班次开始"));
            progressListener.onProgress(progress(index, shiftCount), "SHIFT_EXECUTION",
                    shiftStageName(shift, "班次开始"), shift);
            // 每班重新读取需求和大卷数据；库存、库排统一读取任务启动时冻结的资源基线。
            Cd90AutoScheduleInput input = inputService.load(
                    context.getFactoryCode(), context.getScheduleDate(),
                    shift.getClassField(), shift.getShiftCode(),
                    context.getResourceBaselineDate(),
                    context.getResourceBaselineShiftCode(),
                    context.getParameters().getAgingPeriodHours());
            if (rolling == null) {
                clothSourceTraceByCloth = input.getClothSourceTraceByCloth() == null
                        ? Collections.emptyMap()
                        : Collections.unmodifiableMap(
                                new HashMap<>(input.getClothSourceTraceByCloth()));
                // 首班锁定新增规格判定与需求搬移快照，后续班次不得重查历史改变同批次口径。
                Cd90NewSpecAdvanceResult advanceResult = this.newSpecAdvanceInputPreparer
                        .prepare(context, input);
                input.setPlanningDemandShifts(advanceResult.getAdjustedDemandShifts());
                input.setNewSpecAdvanceInfoByCloth(advanceResult.getAdvanceInfoByCloth());
                // 仅首班使用6点库排和新增规格证据建立基线。
                rolling = this.rollingContextManager.initialize(
                        input.getStorageLanesAtSix(), advanceResult.getAdvanceInfoByCloth());
            } else {
                // 每班重载原始需求后，使用首班证据重建去重计划视图。
                this.newSpecAdvanceInputPreparer.applySnapshot(
                        input, rolling.getNewSpecAdvanceInfoByCloth());
            }
            // 先累计成型消耗，再叠加前序实际/计划入库，得到当前班开始时可见的资源状态。
            Map<String, BigDecimal> cumulativeConsumptionByCloth = demandProvider
                    .cumulativeConsumptionByClothBeforeShift(context, input, shift);
            rollingContextManager.updateCumulativeConsumption(rolling, cumulativeConsumptionByCloth);
            Cd90ShiftResourceState initialState = rollingContextManager.openShift(
                    rolling, shift, buildCurlLengthByCloth(input, context.getParameters().getRollCoilMeter()),
                    context.getParameters().getRollCoilMeter(), context.getParameters().getRollTotalCount(),
                    Collections.emptyList());
            initialState.setBigRollAgingStocks(rollingContextManager.restoreBigRollAllocations(
                    rolling, input.getBigRollAgingStocks()));
            // 单班执行只修改当前班的内存副本；完成后才推进跨班滚动状态。
            Cd90ShiftExecutionResult result = singleShiftScheduleService.execute(
                    context, input, shift, initialState, rolling);
            rollingContextManager.completeShift(rolling, result.getState());
            shiftResults.add(result);
            if (result.getAttemptTraces() != null) {
                // 将班内序号转换为全窗口稳定序号，未排原因按该顺序去重和确定主因。
                for (Cd90ScheduleAttemptTrace trace : result.getAttemptTraces()) {
                    trace.setSequence(attemptTraces.size() + 1);
                    attemptTraces.add(trace);
                }
            }
            runtimeGuard.checkNotTimedOut(context, shiftStageName(shift, "班次完成"));
            progressListener.onProgress(progress(index + 1, shiftCount), "SHIFT_EXECUTION",
                    shiftStageName(shift, "班次完成"), shift);
        }
        log.info("[直裁自动排程] 多班循环执行完成, factoryCode={}, scheduleDate={}, "
                        + "shiftCount={}, taskCount={}",
                context.getFactoryCode(), context.getScheduleDate(), shiftResults.size(),
                rolling == null ? 0 : rolling.getCommittedTasks().size());
        // 所有班次结束后统一汇总未排，避免某一班失败就过早判定最终未排数量。
        return Cd90MultiShiftExecutionResult.builder()
                .shiftResults(shiftResults).rollingContext(rolling)
                .attemptTraces(attemptTraces)
                .unscheduledResults(unscheduledResultAggregator.aggregate(attemptTraces))
                .clothSourceTraceByCloth(clothSourceTraceByCloth)
                .build();
    }

    private String shiftStageName(Cd90ShiftDescriptor shift, String suffix) {
        String displayName = shift == null ? null : shift.getShiftDisplayName();
        String classField = shift == null ? "" : shift.getClassField();
        String shiftName = displayName == null || displayName.trim().isEmpty()
                ? classField : displayName;
        return shiftName + suffix;
    }
    private Map<String, BigDecimal> buildCurlLengthByCloth(Cd90AutoScheduleInput input,
                                                           BigDecimal fallbackCoilMeter) {
        if (input == null || input.getConstructionMaterials() == null) {
            return new HashMap<>();
        }
        return input.getConstructionMaterials().stream()
                .filter(item -> item != null && item.getClothCode() != null)
                .collect(Collectors.toMap(Cd90ConstructionMaterial::getClothCode,
                        item -> item.getCurlLength() == null || item.getCurlLength().signum() <= 0
                                ? fallbackCoilMeter : item.getCurlLength(),
                        (first, second) -> first));
    }

    private int progress(int completedShiftCount, int shiftCount) {
        return 20 + (completedShiftCount * 65 / shiftCount);
    }
}
