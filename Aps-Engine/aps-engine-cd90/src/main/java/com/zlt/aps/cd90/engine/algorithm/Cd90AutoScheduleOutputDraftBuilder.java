package com.zlt.aps.cd90.engine.algorithm;

import com.zlt.aps.cd90.engine.model.Cd90AutoScheduleContext;
import com.zlt.aps.cd90.engine.model.Cd90AutoScheduleOutputDraft;
import com.zlt.aps.cd90.engine.model.Cd90LaneAllocationDraft;
import com.zlt.aps.cd90.engine.model.Cd90MultiShiftExecutionResult;
import com.zlt.aps.cd90.engine.model.Cd90ScheduleAttemptTrace;
import com.zlt.aps.cd90.engine.model.Cd90ScheduleExplainLogDraft;
import com.zlt.aps.cd90.engine.model.Cd90ScheduleResultDraft;
import com.zlt.aps.cd90.engine.model.Cd90ScheduleShiftSlotDraft;
import com.zlt.aps.cd90.engine.model.Cd90ShiftDescriptor;
import com.zlt.aps.cd90.engine.model.Cd90ShiftScheduleTask;
import com.zlt.aps.cd90.engine.model.Cd90StorageLaneAllocation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/** 将多班内存任务归并为最终事务使用的输出草稿。 */
@Slf4j
@Component
public class Cd90AutoScheduleOutputDraftBuilder {

    private static final Pattern CLASS_FIELD_PATTERN = Pattern.compile("CLASS([1-8])");
    private static final String AUTO_SCHEDULE = "AUTO_SCHEDULE";
    private static final String AUTO_SOURCE = "0";

    /**
     * 构建排程结果、库排明细、解释日志和未排结果草稿。
     */
    public Cd90AutoScheduleOutputDraft build(Cd90AutoScheduleContext context,
                                             Cd90MultiShiftExecutionResult execution) {
        if (context == null || execution == null || execution.getRollingContext() == null) {
            throw new IllegalArgumentException("自动排程上下文、多班结果和滚动上下文不能为空");
        }
        Map<String, LocalDate> shiftDates = resolveShiftDates(context.getShifts());
        List<Cd90ShiftScheduleTask> tasks = safe(execution.getRollingContext().getCommittedTasks());
        // 主结果按帘布+大卷+机台归并，库排明细按主结果+班次+库排归并。
        LinkedHashMap<String, Cd90ScheduleResultDraft> resultByKey = new LinkedHashMap<>();
        LinkedHashMap<String, Cd90LaneAllocationDraft> allocationByKey = new LinkedHashMap<>();

        for (Cd90ShiftScheduleTask task : tasks) {
            // 草稿构建阶段严格校验任务完整性，避免脏任务进入最终短事务后部分落库。
            validateTask(task, shiftDates);
            String resultKey = resultKey(task);
            Cd90ScheduleResultDraft result = resultByKey.computeIfAbsent(resultKey,
                    key -> newResultDraft(key, task));
            mergeSlot(result, task, shiftDates.get(task.getClassField()));
            mergeAllocations(allocationByKey, resultKey, task);
        }

        // CLASS槽位按CLASS1~CLASS8排序，确保实体映射和解释日志的输出顺序稳定。
        List<Cd90ScheduleResultDraft> results = new ArrayList<>(resultByKey.values());
        results.forEach(item -> item.setShiftSlots(item.getShiftSlots().stream()
                .sorted(Comparator.comparingInt(slot -> classIndex(slot.getClassField())))
                .collect(Collectors.toList())));
        attachPriorFailureAnalysis(results, execution.getAttemptTraces());
        List<Cd90ScheduleExplainLogDraft> logs = results.stream()
                .map(item -> Cd90ScheduleExplainLogDraft.builder()
                        .resultKey(item.getResultKey()).logType(AUTO_SCHEDULE)
                        .shiftDetails(copySlots(item.getShiftSlots())).build())
                .collect(Collectors.toList());

        log.info("[直裁自动排程] 输出草稿归并完成, taskCount={}, resultCount={}, "
                        + "laneAllocationCount={}, unscheduledCount={}",
                tasks.size(), results.size(), allocationByKey.size(),
                safe(execution.getUnscheduledResults()).size());
        return Cd90AutoScheduleOutputDraft.builder()
                .scheduleResults(results)
                .laneAllocations(new ArrayList<>(allocationByKey.values()))
                .explainLogs(logs)
                .unscheduledResults(execution.getUnscheduledResults() == null
                        ? Collections.emptyList() : execution.getUnscheduledResults())
                .demandTraces(execution.getAttemptTraces() == null
                        ? Collections.emptyList() : execution.getAttemptTraces())
                .build();
    }

    private Cd90ScheduleResultDraft newResultDraft(String key, Cd90ShiftScheduleTask task) {
        String primaryLane = task.getLaneAllocations().isEmpty()
                ? null : task.getLaneAllocations().get(0).getLaneCode();
        return Cd90ScheduleResultDraft.builder()
                .resultKey(key).clothCode(task.getClothCode())
                .bigRollCode(task.getBigRollCode()).cordSpec(task.getCordSpec())
                .machineCode(task.getMachineCode()).primaryLaneCode(primaryLane)
                .dataSource(AUTO_SOURCE).shiftSlots(new ArrayList<>()).build();
    }

    private void mergeSlot(Cd90ScheduleResultDraft result,
                           Cd90ShiftScheduleTask task,
                           LocalDate scheduleDate) {
        Cd90ScheduleShiftSlotDraft slot = result.getShiftSlots().stream()
                .filter(item -> task.getClassField().equals(item.getClassField()))
                .findFirst().orElse(null);
        if (slot == null) {
            // 自动排程刚生成时完成量和完成率均为0，后续由MES回传更新。
            result.getShiftSlots().add(Cd90ScheduleShiftSlotDraft.builder()
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

    private void mergeAllocations(Map<String, Cd90LaneAllocationDraft> allocationByKey,
                                  String resultKey,
                                  Cd90ShiftScheduleTask task) {
        int allocationVehicles = task.getLaneAllocations().stream()
                .mapToInt(Cd90StorageLaneAllocation::getVehicleCount).sum();
        if (allocationVehicles != task.getVehicleCount()) {
            throw new IllegalArgumentException("任务库排分配车数与计划入库车数不一致, clothCode="
                    + task.getClothCode() + ", classField=" + task.getClassField());
        }
        BigDecimal remaining = task.getPlanQuantity();
        for (int index = 0; index < task.getLaneAllocations().size(); index++) {
            Cd90StorageLaneAllocation allocation = task.getLaneAllocations().get(index);
            // 前序库排按车数比例分配，最后库排承接舍入余量，确保明细合计等于主任务量。
            BigDecimal quantity = index == task.getLaneAllocations().size() - 1
                    ? normalize(remaining) : proportional(task.getPlanQuantity(),
                            allocation.getVehicleCount(), task.getVehicleCount());
            remaining = remaining.subtract(quantity);
            String key = resultKey + "|" + task.getClassField() + "|" + allocation.getLaneCode();
            Cd90LaneAllocationDraft existing = allocationByKey.get(key);
            if (existing == null) {
                allocationByKey.put(key, Cd90LaneAllocationDraft.builder()
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
     * 将同一帘布在成功班次前的失败尝试写入该成功班次的系统分析。
     * <p>
     * 例如C02在CLASS1~CLASS3均因库排不足失败、CLASS4成功，则CLASS4_ANALYSIS记录前三班原因，
     * 页面展示时用</br>分隔，便于复盘为什么最终排到了后续班次。
     * </p>
     */
    private void attachPriorFailureAnalysis(List<Cd90ScheduleResultDraft> results,
                                            List<Cd90ScheduleAttemptTrace> traces) {
        Map<String, Integer> successSequenceByClothClass = safe(traces).stream()
                .filter(item -> item != null && StringUtils.hasText(item.getClothCode())
                        && StringUtils.hasText(item.getClassField()))
                .filter(item -> !StringUtils.hasText(item.getFailureReason()))
                .filter(item -> item.getScheduledQuantity() != null
                        && item.getScheduledQuantity().signum() > 0)
                .collect(Collectors.toMap(item -> item.getClothCode() + "|" + item.getClassField(),
                        Cd90ScheduleAttemptTrace::getSequence, Math::min));
        results.forEach(result -> safe(result.getShiftSlots()).forEach(slot -> {
            Integer successSequence = successSequenceByClothClass.get(
                    result.getClothCode() + "|" + slot.getClassField());
            if (successSequence == null) {
                return;
            }
            String analysis = safe(traces).stream()
                    .filter(item -> item != null && result.getClothCode().equals(item.getClothCode()))
                    .filter(item -> StringUtils.hasText(item.getFailureReason()))
                    .filter(item -> item.getSequence() < successSequence)
                    .sorted(Comparator.comparingInt(Cd90ScheduleAttemptTrace::getSequence))
                    .map(this::failureAnalysis)
                    .distinct()
                    .collect(Collectors.joining("</br>"));
            if (StringUtils.hasText(analysis)) {
                slot.setAnalysis(analysis);
            }
        }));
    }

    private String failureAnalysis(Cd90ScheduleAttemptTrace trace) {
        return trace.getClassField() + "：" + failureDescription(trace.getFailureReason());
    }

    private String failureDescription(String failureReason) {
        if ("STORAGE_LANE_LIMIT".equals(failureReason)) {
            return "库排容量不足";
        }
        if ("ROLL_TOOL_LIMIT".equals(failureReason)) {
            return "工装不足";
        }
        if ("MACHINE_PROHIBITED".equals(failureReason)) {
            return "绑定机台均不可作业";
        }
        if ("NO_MACHINE_MAPPING".equals(failureReason)) {
            return "大卷未配置绑定机台";
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
        return "动态状态或产能约束导致无可用候选机台";
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

    private void validateTask(Cd90ShiftScheduleTask task, Map<String, LocalDate> shiftDates) {
        if (task == null || !StringUtils.hasText(task.getClothCode())
                || !StringUtils.hasText(task.getMachineCode())
                || !StringUtils.hasText(task.getClassField())
                || task.getPlanQuantity() == null || task.getPlanQuantity().signum() <= 0) {
            throw new IllegalArgumentException("排程任务的帘布、机台、班次和计划量不能为空");
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

    private Map<String, LocalDate> resolveShiftDates(List<Cd90ShiftDescriptor> shifts) {
        Map<String, LocalDate> result = new HashMap<>();
        for (Cd90ShiftDescriptor shift : safe(shifts)) {
            if (shift != null && StringUtils.hasText(shift.getClassField())
                    && shift.getStartTime() != null) {
                result.put(shift.getClassField(), shift.getStartTime().toLocalDate());
            }
        }
        return result;
    }

    private String resultKey(Cd90ShiftScheduleTask task) {
        return safeKey(task.getClothCode()) + "|" + safeKey(task.getBigRollCode())
                + "|" + safeKey(task.getMachineCode());
    }

    private int classIndex(String classField) {
        Matcher matcher = CLASS_FIELD_PATTERN.matcher(classField == null ? "" : classField);
        if (!matcher.matches()) {
            throw new IllegalArgumentException("班次字段只能取CLASS1至CLASS8");
        }
        return Integer.parseInt(matcher.group(1));
    }

    private List<Cd90ScheduleShiftSlotDraft> copySlots(List<Cd90ScheduleShiftSlotDraft> slots) {
        return slots.stream().map(item -> Cd90ScheduleShiftSlotDraft.builder()
                .classField(item.getClassField()).scheduleDate(item.getScheduleDate())
                .planQuantity(item.getPlanQuantity()).finishQuantity(item.getFinishQuantity())
                .produceOrder(item.getProduceOrder()).finishRate(item.getFinishRate())
                .analysis(item.getAnalysis())
                .expectedStartTime(item.getExpectedStartTime())
                .expectedEndTime(item.getExpectedEndTime()).build())
                .collect(Collectors.toList());
    }

    private LocalDateTime earlier(LocalDateTime left, LocalDateTime right) {
        if (left == null) return right;
        if (right == null) return left;
        return left.isBefore(right) ? left : right;
    }

    private LocalDateTime later(LocalDateTime left, LocalDateTime right) {
        if (left == null) return right;
        if (right == null) return left;
        return left.isAfter(right) ? left : right;
    }

    private String safeKey(String value) {
        return value == null ? "" : value;
    }

    private <T> List<T> safe(List<T> values) {
        return values == null ? Collections.emptyList() : values;
    }
}
