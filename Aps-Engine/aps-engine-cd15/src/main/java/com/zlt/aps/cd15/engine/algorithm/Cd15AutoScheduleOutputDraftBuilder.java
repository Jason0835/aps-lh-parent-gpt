package com.zlt.aps.cd15.engine.algorithm;

import com.zlt.aps.cd15.engine.model.Cd15AutoScheduleContext;
import com.zlt.aps.cd15.engine.model.Cd15AutoScheduleOutputDraft;
import com.zlt.aps.cd15.engine.model.Cd15SteelStripSourceTrace;
import com.zlt.aps.cd15.engine.model.Cd15LaneAllocationDraft;
import com.zlt.aps.cd15.engine.model.Cd15MultiShiftExecutionResult;
import com.zlt.aps.cd15.engine.model.Cd15NewSpecAdvanceInfo;
import com.zlt.aps.cd15.engine.model.Cd15ScheduleAttemptTrace;
import com.zlt.aps.cd15.engine.model.Cd15ScheduleExplainLogDraft;
import com.zlt.aps.cd15.engine.model.Cd15ScheduleResultDraft;
import com.zlt.aps.cd15.engine.model.Cd15ScheduleShiftSlotDraft;
import com.zlt.aps.cd15.engine.model.Cd15ShiftDescriptor;
import com.zlt.aps.cd15.engine.model.Cd15ShiftScheduleTask;
import com.zlt.aps.cd15.engine.model.Cd15StorageLaneAllocation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/** 将多班内存任务归并为最终事务使用的输出草稿。 */
@Slf4j
@Component
public class Cd15AutoScheduleOutputDraftBuilder {

    private static final Pattern CLASS_FIELD_PATTERN = Pattern.compile("CLASS([1-8])");
    private static final String AUTO_SCHEDULE = "AUTO_SCHEDULE";
    private static final String AUTO_SOURCE = "0";

    /**
     * 构建排程结果、库排明细、解释日志和未排结果草稿。
     */
    public Cd15AutoScheduleOutputDraft build(Cd15AutoScheduleContext context,
                                             Cd15MultiShiftExecutionResult execution) {
        if (context == null || execution == null || execution.getRollingContext() == null) {
            throw new IllegalArgumentException("自动排程上下文、多班结果和滚动上下文不能为空");
        }
        Map<String, LocalDate> shiftDates = resolveShiftDates(context.getShifts());
        List<Cd15ShiftScheduleTask> tasks = safe(execution.getRollingContext().getCommittedTasks());
        Map<String, Cd15SteelStripSourceTrace> sourceTraceBySteelStrip =
                execution.getSteelStripSourceTraceBySteelStrip() == null
                        ? Collections.emptyMap()
                        : execution.getSteelStripSourceTraceBySteelStrip();
        // 主结果按钢带+大卷+机台归并，库排明细按主结果+班次+库排归并。
        LinkedHashMap<String, Cd15ScheduleResultDraft> resultByKey = new LinkedHashMap<>();
        LinkedHashMap<String, Cd15LaneAllocationDraft> allocationByKey = new LinkedHashMap<>();

        for (Cd15ShiftScheduleTask task : tasks) {
            // 草稿构建阶段严格校验任务完整性，避免脏任务进入最终短事务后部分落库。
            validateTask(task, shiftDates);
            String resultKey = resultKey(task);
            Cd15ScheduleResultDraft result = resultByKey.computeIfAbsent(resultKey,
                    key -> newResultDraft(key, task, sourceTraceBySteelStrip));
            this.mergeBigRollConsumption(result, task);
            // 同一主结果可能由多个任务段组成，每个任务段使用的库排都要汇总到主表展示字段。
            mergePrimaryLaneCodes(result, task);
            mergeSlot(result, task, shiftDates.get(task.getClassField()));
            mergeAllocations(allocationByKey, resultKey, task);
        }

        // CLASS槽位按CLASS1~CLASS8排序，确保实体映射和解释日志的输出顺序稳定。
        List<Cd15ScheduleResultDraft> results = new ArrayList<>(resultByKey.values());
        results.forEach(item -> item.setShiftSlots(item.getShiftSlots().stream()
                .sorted(Comparator.comparingInt(slot -> classIndex(slot.getClassField())))
                .collect(Collectors.toList())));
        attachPriorFailureAnalysis(results, execution.getAttemptTraces());
        this.attachNewSpecAdvanceAnalysis(results,
                execution.getRollingContext().getNewSpecAdvanceInfoBySteelStrip());
        List<Cd15ScheduleExplainLogDraft> logs = results.stream()
                .map(item -> Cd15ScheduleExplainLogDraft.builder()
                        .resultKey(item.getResultKey()).logType(AUTO_SCHEDULE)
                        .shiftDetails(copySlots(item.getShiftSlots())).build())
                .collect(Collectors.toList());

        log.info("[斜裁自动排程] 输出草稿归并完成, taskCount={}, resultCount={}, "
                        + "laneAllocationCount={}, unscheduledCount={}",
                tasks.size(), results.size(), allocationByKey.size(),
                safe(execution.getUnscheduledResults()).size());
        return Cd15AutoScheduleOutputDraft.builder()
                .scheduleResults(results)
                .laneAllocations(new ArrayList<>(allocationByKey.values()))
                .explainLogs(logs)
                .unscheduledResults(execution.getUnscheduledResults() == null
                        ? Collections.emptyList() : execution.getUnscheduledResults())
                .demandTraces(execution.getAttemptTraces() == null
                        ? Collections.emptyList() : execution.getAttemptTraces())
                .build();
    }

    private Cd15ScheduleResultDraft newResultDraft(
            String key, Cd15ShiftScheduleTask task,
            Map<String, Cd15SteelStripSourceTrace> steelStripSourceTraceBySteelStrip) {
        Cd15SteelStripSourceTrace sourceTrace = steelStripSourceTraceBySteelStrip.get(task.getSteelStripCode());
        return Cd15ScheduleResultDraft.builder()
                .resultKey(key).materialKey(task.getMaterialKey())
                .steelStripCode(task.getSteelStripCode())
                .bigRollCode(task.getBigRollCode()).cordSpec(task.getCordSpec())
                .cuttingAngle(task.getCuttingAngle())
                .craftWidth(task.getCraftWidth())
                .unitConsumeMillimeter(task.getUnitConsumeMillimeter())
                .curlLength(task.getCurlLength())
                .cordWidth(task.getCordWidth())
                .bigRollConsumeQuantity(BigDecimal.ZERO)
                .cutMode(task.getCutMode())
                .splitGroupKey(task.getSplitGroupKey())
                .cxBatchNo(sourceTrace == null ? null : sourceTrace.getCxBatchNo())
                .cxMachineCodes(sourceTrace == null ? null : sourceTrace.getCxMachineCodes())
                .planSurplusQty(sourceTrace == null ? null : sourceTrace.getPlanSurplusQty())
                .machineCode(task.getMachineCode())
                .dataSource(AUTO_SOURCE).shiftSlots(new ArrayList<>()).build();
    }

    /** 同一材料跨任务段归并时累计GDYY大卷占用量。 */
    private void mergeBigRollConsumption(
            Cd15ScheduleResultDraft result, Cd15ShiftScheduleTask task) {
        BigDecimal existing = result.getBigRollConsumeQuantity() == null
                ? BigDecimal.ZERO : result.getBigRollConsumeQuantity();
        BigDecimal current = task.getBigRollConsumeQuantity() == null
                ? BigDecimal.ZERO : task.getBigRollConsumeQuantity();
        result.setBigRollConsumeQuantity(existing.add(current));
    }

    /** 按任务出现顺序汇总主结果使用过的库排号，并在跨任务场景下去重。 */
    private void mergePrimaryLaneCodes(Cd15ScheduleResultDraft result,
                                       Cd15ShiftScheduleTask task) {
        LinkedHashSet<String> laneCodes = new LinkedHashSet<>();
        if (StringUtils.hasText(result.getPrimaryLaneCode())) {
            java.util.Arrays.stream(result.getPrimaryLaneCode().split(","))
                    .map(String::trim)
                    .filter(StringUtils::hasText)
                    .forEach(laneCodes::add);
        }
        safe(task.getLaneAllocations()).stream()
                .map(Cd15StorageLaneAllocation::getLaneCode)
                .filter(StringUtils::hasText)
                .forEach(laneCodes::add);
        result.setPrimaryLaneCode(laneCodes.isEmpty() ? null : String.join(",", laneCodes));
    }

    private void mergeSlot(Cd15ScheduleResultDraft result,
                           Cd15ShiftScheduleTask task,
                           LocalDate scheduleDate) {
        Cd15ScheduleShiftSlotDraft slot = result.getShiftSlots().stream()
                .filter(item -> task.getClassField().equals(item.getClassField()))
                .findFirst().orElse(null);
        if (slot == null) {
            // 自动排程刚生成时完成量和完成率均为0，后续由MES回传更新。
            result.getShiftSlots().add(Cd15ScheduleShiftSlotDraft.builder()
                    .classField(task.getClassField()).scheduleDate(scheduleDate)
                    .planQuantity(task.getPlanQuantity()).finishQuantity(BigDecimal.ZERO)
                    .produceOrder(task.getProduceOrder()).finishRate(BigDecimal.ZERO)
                    .expectedStartTime(task.getExpectedStartTime())
                    .expectedEndTime(task.getExpectedEndTime()).build());
            return;
        }
        // 同一主结果同一班可能包含多个连续任务段，合并数量并扩展起止时间边界。
        slot.setPlanQuantity(slot.getPlanQuantity().add(task.getPlanQuantity()));
        slot.setProduceOrder(Math.min(slot.getProduceOrder(), task.getProduceOrder()));
        slot.setExpectedStartTime(earlier(slot.getExpectedStartTime(), task.getExpectedStartTime()));
        slot.setExpectedEndTime(later(slot.getExpectedEndTime(), task.getExpectedEndTime()));
    }

    private void mergeAllocations(Map<String, Cd15LaneAllocationDraft> allocationByKey,
                                  String resultKey,
                                  Cd15ShiftScheduleTask task) {
        int allocationVehicles = task.getLaneAllocations().stream()
                .mapToInt(Cd15StorageLaneAllocation::getVehicleCount).sum();
        if (allocationVehicles != task.getVehicleCount()) {
            throw new IllegalArgumentException("任务库排分配车数与计划入库车数不一致, steelStripCode="
                    + task.getSteelStripCode() + ", classField=" + task.getClassField());
        }
        BigDecimal remaining = task.getPlanQuantity();
        for (int index = 0; index < task.getLaneAllocations().size(); index++) {
            Cd15StorageLaneAllocation allocation = task.getLaneAllocations().get(index);
            // 前序库排按车数比例分配，最后库排承接舍入余量，确保明细合计等于主任务量。
            BigDecimal quantity = index == task.getLaneAllocations().size() - 1
                    ? normalize(remaining) : proportional(task.getPlanQuantity(),
                            allocation.getVehicleCount(), task.getVehicleCount());
            remaining = remaining.subtract(quantity);
            String key = resultKey + "|" + task.getClassField() + "|" + allocation.getLaneCode();
            Cd15LaneAllocationDraft existing = allocationByKey.get(key);
            if (existing == null) {
                allocationByKey.put(key, Cd15LaneAllocationDraft.builder()
                        .resultKey(resultKey).classField(task.getClassField())
                        .laneCode(allocation.getLaneCode()).allocationQuantity(quantity)
                        .vehicleCount(allocation.getVehicleCount()).build());
            } else {
                existing.setAllocationQuantity(existing.getAllocationQuantity().add(quantity));
                existing.setVehicleCount(existing.getVehicleCount() + allocation.getVehicleCount());
            }
        }
    }

    /**
     * 将同一钢带在成功班次前的失败尝试写入该成功班次的系统分析。
     * <p>
     * 例如C02在前序班次均因库排不足失败、后续班次成功，则成功班次ANALYSIS记录前序原因，
     * 页面展示时优先使用夜班07/20这类班次名称并用</br>分隔，便于复盘为什么最终排到了后续班次。
     * </p>
     */
    private void attachPriorFailureAnalysis(List<Cd15ScheduleResultDraft> results,
                                            List<Cd15ScheduleAttemptTrace> traces) {
        Map<String, Cd15ScheduleAttemptTrace> successTraceBySteelStripClass = safe(traces).stream()
                .filter(item -> item != null && StringUtils.hasText(item.getSteelStripCode())
                        && StringUtils.hasText(item.getClassField()))
                .filter(item -> !StringUtils.hasText(item.getFailureReason()))
                .filter(item -> item.getScheduledQuantity() != null
                        && item.getScheduledQuantity().signum() > 0)
                .collect(Collectors.toMap(item -> this.traceKey(item.getSteelStripCode(), item.getBigRollCode(),
                                item.getCuttingAngle(), item.getClassField()),
                        item -> item,
                        (left, right) -> left.getSequence() <= right.getSequence() ? left : right));
        results.forEach(result -> safe(result.getShiftSlots()).forEach(slot -> {
            Cd15ScheduleAttemptTrace successTrace = successTraceBySteelStripClass.get(
                    this.traceKey(result.getSteelStripCode(), result.getBigRollCode(),
                            result.getCuttingAngle(), slot.getClassField()));
            if (successTrace == null) {
                return;
            }
            List<String> analysisItems = new ArrayList<>();
            String priorFailureAnalysis = safe(traces).stream()
                    .filter(item -> item != null
                            && result.getSteelStripCode().equals(item.getSteelStripCode())
                            && java.util.Objects.equals(result.getBigRollCode(), item.getBigRollCode())
                            && java.util.Objects.equals(result.getCuttingAngle(), item.getCuttingAngle()))
                    .filter(item -> StringUtils.hasText(item.getFailureReason()))
                    .filter(item -> item.getSequence() < successTrace.getSequence())
                    .sorted(Comparator.comparingInt(Cd15ScheduleAttemptTrace::getSequence))
                    .map(this::failureAnalysis)
                    .distinct()
                    .collect(Collectors.joining("</br>"));
            if (StringUtils.hasText(priorFailureAnalysis)) {
                analysisItems.add(priorFailureAnalysis);
            }
            String partialAnalysis = partialScheduleAnalysis(successTrace);
            if (StringUtils.hasText(partialAnalysis)) {
                analysisItems.add(partialAnalysis);
            }
            if (!analysisItems.isEmpty()) {
                slot.setAnalysis(String.join("</br>", analysisItems));
            }
        }));
    }

    /** 将新增规格识别窗口、原需求日期和目标生产日追加到实际成功班次分析。 */
    private void attachNewSpecAdvanceAnalysis(
            List<Cd15ScheduleResultDraft> results,
            Map<String, Cd15NewSpecAdvanceInfo> infoBySteelStrip) {
        if (infoBySteelStrip == null || infoBySteelStrip.isEmpty()) {
            return;
        }
        results.forEach(result -> {
            Cd15NewSpecAdvanceInfo info = infoBySteelStrip.get(result.getSteelStripCode());
            if (info == null || !StringUtils.hasText(info.getAnalysis())) {
                return;
            }
            safe(result.getShiftSlots()).forEach(slot -> {
                if (StringUtils.hasText(slot.getAnalysis())) {
                    if (!slot.getAnalysis().contains(info.getAnalysis())) {
                        slot.setAnalysis(slot.getAnalysis() + "</br>" + info.getAnalysis());
                    }
                } else {
                    slot.setAnalysis(info.getAnalysis());
                }
            });
        });
    }

    private String traceKey(String steelStripCode, String bigRollCode,
                            String cuttingAngle, String classField) {
        return safeKey(steelStripCode) + "|" + safeKey(bigRollCode)
                + "|" + safeKey(cuttingAngle) + "|" + safeKey(classField);
    }

    private String failureAnalysis(Cd15ScheduleAttemptTrace trace) {
        return traceDisplayName(trace) + "：" + failureDescription(trace.getFailureReason());
    }

    private String partialScheduleAnalysis(Cd15ScheduleAttemptTrace trace) {
        BigDecimal netDemand = trace.getNetDemandQuantity();
        BigDecimal scheduled = trace.getScheduledQuantity();
        if (netDemand == null || scheduled == null || scheduled.signum() <= 0
                || scheduled.compareTo(netDemand) >= 0) {
            return null;
        }
        BigDecimal remaining = netDemand.subtract(scheduled);
        return traceDisplayName(trace) + "：" + partialReasonDescription(trace.getPartialReason())
                + "，仅部分排" + plain(scheduled)
                + "m，剩余" + plain(remaining) + "m转后续班次重算";
    }

    /** 优先使用业务班次展示名，历史轨迹没有展示名时回退CLASS字段。 */
    private String traceDisplayName(Cd15ScheduleAttemptTrace trace) {
        if (trace == null) {
            return "";
        }
        return StringUtils.hasText(trace.getShiftDisplayName())
                ? trace.getShiftDisplayName() : trace.getClassField();
    }
    private String partialReasonDescription(String partialReason) {
        if ("STORAGE_LANE_LIMIT".equals(partialReason)) {
            return "库排容量不足";
        }
        if ("TOOLING_LIMIT".equals(partialReason) || "ROLL_TOOL_LIMIT".equals(partialReason)) {
            return "工装不足";
        }
        if ("CAPACITY_LIMIT".equals(partialReason)) {
            return "机台产能不足";
        }
        if ("AGING_PERIOD_LIMIT".equals(partialReason)) {
            return "大卷静置期限制";
        }
        if ("EQUAL_SHARE".equals(partialReason)) {
            return "按均分策略拆分";
        }
        return "资源限制";
    }

    private String failureDescription(String failureReason) {
        if ("STORAGE_LANE_LIMIT".equals(failureReason)) {
            return "库排容量不足";
        }
        if ("AGING_PERIOD_LIMIT".equals(failureReason)) {
            return "大卷静置期未满";
        }
        if ("ROLL_TOOL_LIMIT".equals(failureReason) || "TOOLING_LIMIT".equals(failureReason)) {
            return "工装不足";
        }
        if ("CAPACITY_LIMIT".equals(failureReason)) {
            return "机台产能不足";
        }
        if ("MACHINE_PROHIBITED".equals(failureReason)) {
            return "绑定机台均不可作业";
        }
        if ("WIDTH_MISMATCH".equals(failureReason)) {
            return "施工斜裁宽度超出机台裁断宽度范围";
        }
        if ("ANGLE_WIDTH_MISMATCH".equals(failureReason)) {
            return "施工斜裁宽度超出当前角度最大宽度";
        }
        if ("ANGLE_WIDTH_CONFIG_MISSING".equals(failureReason)) {
            return "裁断角度宽度配置缺失";
        }
        if ("CONSTRUCTION_MISSING".equals(failureReason)) {
            return "施工信息或必要基础数据缺失";
        }
        if ("SPEC_START_COUNT_LIMIT".equals(failureReason)) {
            return "连续四班上机次数达到上限";
        }
        if ("SCHEDULE_WINDOW_LIMIT".equals(failureReason)) {
            return "排程窗口结束仍有未安排数量";
        }
        return "动态状态或产能约束导致无可选候选机台";
    }

    private String plain(BigDecimal value) {
        BigDecimal normalized = value.stripTrailingZeros();
        return (normalized.scale() < 0 ? normalized.setScale(0) : normalized).toPlainString();
    }

    private BigDecimal proportional(BigDecimal total, int vehicles, int totalVehicles) {
        return total.multiply(BigDecimal.valueOf(vehicles))
                .divide(BigDecimal.valueOf(totalVehicles), 10, RoundingMode.HALF_UP);
    }

    /** 去除最后库排承接余量的无效尾零，保留数值本身不变。 */
    private BigDecimal normalize(BigDecimal value) {
        BigDecimal normalized = value.stripTrailingZeros();
        return normalized.scale() < 0 ? normalized.setScale(0) : normalized;
    }

    private void validateTask(Cd15ShiftScheduleTask task, Map<String, LocalDate> shiftDates) {
        if (task == null || !StringUtils.hasText(task.getSteelStripCode())
                || !StringUtils.hasText(task.getMachineCode())
                || !StringUtils.hasText(task.getClassField())
                || task.getPlanQuantity() == null || task.getPlanQuantity().signum() <= 0) {
            throw new IllegalArgumentException("排程任务的钢带、机台、班次和计划量不能为空");
        }
        classIndex(task.getClassField());
        if (!shiftDates.containsKey(task.getClassField())) {
            throw new IllegalArgumentException("排程任务班次未出现在本次输出窗口: " + task.getClassField());
        }
        if (task.getVehicleCount() <= 0 || task.getLaneAllocations() == null
                || task.getLaneAllocations().isEmpty()) {
            throw new IllegalArgumentException("排程任务计划入库车数和库排分配不能为空");
        }
        task.getLaneAllocations().forEach(allocation -> {
            if (allocation == null || !StringUtils.hasText(allocation.getLaneCode())
                    || allocation.getVehicleCount() <= 0) {
                throw new IllegalArgumentException("库排编码和分配车数必须有效");
            }
        });
    }

    private Map<String, LocalDate> resolveShiftDates(List<Cd15ShiftDescriptor> shifts) {
        return safe(shifts).stream()
                .filter(shift -> shift != null && StringUtils.hasText(shift.getClassField()))
                .map(shift -> new AbstractMap.SimpleEntry<>(shift.getClassField(), shiftScheduleDate(shift)))
                .filter(entry -> entry.getValue() != null)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue,
                        (left, right) -> right, HashMap::new));
    }

    private LocalDate shiftScheduleDate(Cd15ShiftDescriptor shift) {
        if (shift.getScheduleDate() != null) {
            return shift.getScheduleDate();
        }
        return shift.getStartTime() == null ? null : shift.getStartTime().toLocalDate();
    }

    private String resultKey(Cd15ShiftScheduleTask task) {
        return safeKey(task.getMaterialKey()) + "|" + safeKey(task.getMachineCode())
                + "|" + safeKey(task.getCutMode()) + "|" + safeKey(task.getSplitGroupKey());
    }

    private int classIndex(String classField) {
        Matcher matcher = CLASS_FIELD_PATTERN.matcher(classField == null ? "" : classField);
        if (!matcher.matches()) {
            throw new IllegalArgumentException("班次字段只能取CLASS1至CLASS8");
        }
        return Integer.parseInt(matcher.group(1));
    }

    private List<Cd15ScheduleShiftSlotDraft> copySlots(List<Cd15ScheduleShiftSlotDraft> slots) {
        return slots.stream().map(item -> Cd15ScheduleShiftSlotDraft.builder()
                .classField(item.getClassField()).scheduleDate(item.getScheduleDate())
                .planQuantity(item.getPlanQuantity()).finishQuantity(item.getFinishQuantity())
                .produceOrder(item.getProduceOrder()).finishRate(item.getFinishRate())
                .analysis(item.getAnalysis())
                .expectedStartTime(item.getExpectedStartTime())
                .expectedEndTime(item.getExpectedEndTime()).build())
                .collect(Collectors.toList());
    }

    private LocalDateTime earlier(LocalDateTime left, LocalDateTime right) {
        if (left == null) {
            return right;
        }
        if (right == null) {
            return left;
        }
        return left.isBefore(right) ? left : right;
    }

    private LocalDateTime later(LocalDateTime left, LocalDateTime right) {
        if (left == null) {
            return right;
        }
        if (right == null) {
            return left;
        }
        return left.isAfter(right) ? left : right;
    }

    private String safeKey(String value) {
        return value == null ? "" : value;
    }

    private <T> List<T> safe(List<T> values) {
        return values == null ? Collections.emptyList() : values;
    }
}
