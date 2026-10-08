package com.zlt.aps.common.engine.schedule.engine;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * TM/TC 排序重放快照公共构建器。
 *
 * <p>快照只写入现有规则轨迹对象，不改变任务排序结果，也不依赖持久化表结构。</p>
 */
public final class ScheduleSortSnapshotSupport {

    /**
     * 快照格式版本。
     */
    public static final String SNAPSHOT_VERSION = "1";
    /**
     * 比较规则版本。
     */
    public static final String COMPARATOR_VERSION = "TM_TC_TASK_SORT_V1";
    /**
     * 快照规则节点编码。
     */
    public static final String RULE_CODE = "TASK_SORT_SNAPSHOT";

    private static final DateTimeFormatter UTC_FORMATTER = DateTimeFormatter.ISO_INSTANT;

    private ScheduleSortSnapshotSupport() {
    }

    /**
     * 为排序轮次的每个参与任务追加一个独立快照节点。
     *
     * @param trace                    任务规则轨迹
     * @param phase                    排序阶段
     * @param roundId                  排序轮次编号
     * @param scope                    运行范围和实际参数快照
     * @param strategyCode             排序策略编码
     * @param sortSource               排序来源
     * @param prependSupplyHours       是否由外层追加供应时长
     * @param beforeOrder              排序前任务列表
     * @param afterOrder               排序后任务列表
     * @param strategyEvidenceProvider 策略证据提供器
     * @param comparisonKeyProvider    与实际比较器一致的比较键提供器
     * @param <T>                      任务类型
     */
    public static <T extends ScheduleTaskDraftModel> void appendTaskSnapshots(
            Function<T, ScheduleRuleTrace> traceProvider,
            String phase,
            String roundId,
            Map<String, Object> scope,
            String strategyCode,
            String sortSource,
            boolean prependSupplyHours,
            List<T> beforeOrder,
            List<T> afterOrder,
            Function<T, Map<String, Object>> strategyEvidenceProvider,
            Function<T, List<Map<String, Object>>> comparisonKeyProvider) {
        List<T> safeBeforeOrder = beforeOrder == null ? Collections.emptyList() : beforeOrder;
        List<T> safeAfterOrder = afterOrder == null ? Collections.emptyList() : afterOrder;
        String inputHash = hashOrder(safeBeforeOrder);
        String outputHash = hashOrder(safeAfterOrder);
        Map<String, Integer> beforeIndexMap = indexMap(safeBeforeOrder);
        Map<String, Integer> afterIndexMap = indexMap(safeAfterOrder);
        List<String> outputOrder = safeAfterOrder.stream().filter(Objects::nonNull)
                .map(ScheduleTaskDraftModel::getBusinessKey).collect(Collectors.toList());
        List<String> inputOrder = safeBeforeOrder.stream().filter(Objects::nonNull)
                .map(ScheduleTaskDraftModel::getBusinessKey).collect(Collectors.toList());
        for (T task : safeAfterOrder) {
            if (task == null) {
                continue;
            }
            Map<String, Object> evidence = new LinkedHashMap<>();
            evidence.put("snapshotVersion", SNAPSHOT_VERSION);
            evidence.put("comparatorVersion", COMPARATOR_VERSION);
            evidence.put("phase", phase);
            evidence.put("roundId", roundId);
            if (scope != null) {
                evidence.putAll(scope);
            }
            evidence.put("strategyCode", strategyCode);
            evidence.put("sortSource", sortSource);
            evidence.put("prependSupplyHoursPriority", prependSupplyHours);
            evidence.put("participantCount", safeAfterOrder.size());
            evidence.put("inputOrderHash", inputHash);
            evidence.put("outputOrderHash", outputHash);
            evidence.put("inputOrder", inputOrder);
            evidence.put("outputOrder", outputOrder);
            evidence.put("taskIdentity", buildTaskIdentity(task));
            evidence.put("sourceTaskBusinessKeyList", task.getSourceTaskBusinessKeyList());
            evidence.put("inputPosition", beforeIndexMap.get(task.getBusinessKey()));
            evidence.put("outputPosition", afterIndexMap.get(task.getBusinessKey()));
            evidence.put("latestStartTimeAtSort", formatTime(task.getLatestStartTime()));
            evidence.put("latestStartTimeAtSortMillis",
                    task.getLatestStartTime() == null ? null : task.getLatestStartTime().getTime());
            evidence.put("latestStartTimeAtSortZone", "UTC");
            evidence.put("inputValues", buildInputValues(task));
            Map<String, Object> strategyEvidence = strategyEvidenceProvider == null
                    ? Collections.emptyMap() : strategyEvidenceProvider.apply(task);
            evidence.put("strategyEvidence", strategyEvidence == null
                    ? Collections.emptyMap() : new LinkedHashMap<>(strategyEvidence));
            List<Map<String, Object>> comparisonKeys = comparisonKeyProvider == null
                    ? Collections.emptyList() : comparisonKeyProvider.apply(task);
            evidence.put("comparisonRules", copyKeyRules(comparisonKeys));
            evidence.put("comparisonKeys", comparisonKeys == null
                    ? Collections.emptyList() : comparisonKeys);
            evidence.put("firstDifferentCondition", resolveFirstDifferentCondition(
                    safeAfterOrder, afterIndexMap.get(task.getBusinessKey()), comparisonKeyProvider));
            evidence.put("output", buildOutputValues(task));
            traceProvider.apply(task).addRuleHit(RULE_CODE, "PASS", evidence);
        }
    }

    /**
     * 生成机台分配取任务快照。
     *
     * @param trace              选中任务规则轨迹
     * @param phase              阶段
     * @param roundId            轮次编号
     * @param scope              运行范围
     * @param remainingTaskList  取任务前剩余集合
     * @param selectedTask       选中任务
     * @param assignmentSequence 机台分配序号
     */
    public static void appendAssignmentSnapshot(ScheduleRuleTrace trace, String phase, String roundId,
                                                Map<String, Object> scope,
                                                List<? extends ScheduleTaskDraftModel> remainingTaskList,
                                                ScheduleTaskDraftModel selectedTask, int assignmentSequence) {
        Map<String, Object> evidence = new LinkedHashMap<>();
        evidence.put("snapshotVersion", SNAPSHOT_VERSION);
        evidence.put("comparatorVersion", COMPARATOR_VERSION);
        evidence.put("phase", phase);
        evidence.put("roundId", roundId);
        if (scope != null) {
            evidence.putAll(scope);
        }
        List<? extends ScheduleTaskDraftModel> safeRemaining = remainingTaskList == null
                ? Collections.emptyList() : remainingTaskList;
        evidence.put("participantCount", safeRemaining.size());
        evidence.put("inputOrderHash", hashOrder(safeRemaining));
        List<ScheduleTaskDraftModel> remainingAfterSelection = new ArrayList<>(safeRemaining);
        remainingAfterSelection.removeIf(task -> task != null && selectedTask != null
                && Objects.equals(task.getBusinessKey(), selectedTask.getBusinessKey()));
        evidence.put("outputOrderHash", hashOrder(remainingAfterSelection));
        evidence.put("remainingTaskBusinessKeys", safeRemaining.stream().filter(Objects::nonNull)
                .map(ScheduleTaskDraftModel::getBusinessKey).collect(Collectors.toList()));
        evidence.put("outputOrder", remainingAfterSelection.stream().filter(Objects::nonNull)
                .map(ScheduleTaskDraftModel::getBusinessKey).collect(Collectors.toList()));
        evidence.put("remainingTaskDetails", safeRemaining.stream().filter(Objects::nonNull)
                .map(task -> {
                    Map<String, Object> detail = buildTaskIdentity(task);
                    detail.put("inputValues", buildInputValues(task));
                    detail.put("latestStartTimeAtSortMillis",
                            task.getLatestStartTime() == null ? null : task.getLatestStartTime().getTime());
                    detail.put("comparisonKeys", buildAssignmentKeys(task));
                    return detail;
                }).collect(Collectors.toList()));
        evidence.put("selectedTask", buildTaskIdentity(selectedTask));
        evidence.put("machineAssignmentSequence", assignmentSequence);
        evidence.put("comparisonRules", Arrays.asList(
                rule("planCalcOrderIndex", "ASC", "LAST"),
                rule("baseSortIndex", "ASC", "LAST"),
                rule("businessKey", "ASC", "EMPTY_STRING")));
        evidence.put("comparisonKeys", buildAssignmentKeys(selectedTask));
        evidence.put("output", buildOutputValues(selectedTask));
        trace.addRuleHit(RULE_CODE, "PASS", evidence);
    }

    /**
     * 计算任务列表完整性哈希。
     *
     * @param taskList 任务列表
     * @return SHA-256十六进制摘要
     */
    public static String hashOrder(List<? extends ScheduleTaskDraftModel> taskList) {
        String canonical = (taskList == null ? Collections.<ScheduleTaskDraftModel>emptyList() : taskList)
                .stream().map(task -> task == null ? "<null>" : canonicalTask(task))
                .collect(Collectors.joining("\n"));
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(canonical.getBytes(StandardCharsets.UTF_8));
            StringBuilder result = new StringBuilder(bytes.length * 2);
            for (byte value : bytes) {
                result.append(String.format("%02x", value));
            }
            return result.toString();
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256不可用", exception);
        }
    }

    private static Map<String, Integer> indexMap(List<? extends ScheduleTaskDraftModel> taskList) {
        Map<String, Integer> result = new LinkedHashMap<>();
        for (int index = 0; index < taskList.size(); index++) {
            ScheduleTaskDraftModel task = taskList.get(index);
            if (task != null) {
                result.putIfAbsent(task.getBusinessKey(), index + 1);
            }
        }
        return result;
    }

    private static String canonicalTask(ScheduleTaskDraftModel task) {
        return String.join("|", value(task.getBusinessKey()), value(task.getProcessCode()),
                value(task.getShiftOrder()), value(task.getGlueCode()), value(task.getBaseGlueCode()),
                value(task.getMouthPlateCode()), decimal(task.getSupplyHours()),
                value(task.getLatestStartTime() == null ? null : task.getLatestStartTime().getTime()));
    }

    private static Map<String, Object> buildTaskIdentity(ScheduleTaskDraftModel task) {
        Map<String, Object> result = new LinkedHashMap<>();
        if (task == null) {
            return result;
        }
        result.put("businessKey", task.getBusinessKey());
        result.put("processCode", task.getProcessCode());
        result.put("orderNo", task.getOrderNo());
        result.put("sourceOrderNos", task.getSourceOrderNos());
        result.put("planGroupKey", task.getPlanGroupKey());
        result.put("sourceExplainTask", task.getSourceExplainTask());
        result.put("sourceShiftOrder", task.getSourceShiftOrder());
        result.put("shiftOrder", task.getShiftOrder());
        result.put("glueCode", task.getGlueCode());
        result.put("baseGlueCode", task.getBaseGlueCode());
        result.put("mouthPlateCode", task.getMouthPlateCode());
        return result;
    }

    private static Map<String, Object> buildInputValues(ScheduleTaskDraftModel task) {
        Map<String, Object> result = buildTaskIdentity(task);
        result.put("supplyHours", task.getSupplyHours());
        result.put("rollingStockQty", task.getRollingStockQty());
        result.put("sixClockStockQty", task.getSixClockStockQty());
        result.put("currentShiftDemandQty", task.getCurrentShiftDemandQty());
        result.put("nextShiftDemandQty", task.getNextShiftDemandQty());
        result.put("guardDemandQty", task.getGuardDemandQty());
        result.put("formingGuardWindowQtyMap", task.getFormingGuardWindowQtyMap());
        result.put("formingGuardWindowHoursMap", task.getFormingGuardWindowHoursMap());
        result.put("initialStockQty", task.getRollingStockQty());
        result.put("planCalcOrderIndexAtSort", task.getPlanCalcOrderIndex());
        result.put("baseSortIndexAtSort", task.getBaseSortIndex());
        return result;
    }

    private static Map<String, Object> buildOutputValues(ScheduleTaskDraftModel task) {
        Map<String, Object> result = new LinkedHashMap<>();
        if (task == null) {
            return result;
        }
        result.put("planCalcOrderIndex", task.getPlanCalcOrderIndex());
        result.put("baseSortIndex", task.getBaseSortIndex());
        result.put("machineAssignmentSequence", task.getMachineAssignmentSequence());
        result.put("machineCode", task.getMachineCode());
        result.put("planQty", task.getPlanQty());
        return result;
    }

    private static List<Map<String, Object>> copyKeyRules(List<Map<String, Object>> keys) {
        if (keys == null) {
            return Collections.emptyList();
        }
        return keys.stream().map(key -> {
            Map<String, Object> result = new LinkedHashMap<>();
            if (key != null) {
                result.put("field", key.get("field"));
                result.put("direction", key.get("direction"));
                result.put("nullRule", key.get("nullRule"));
                result.put("stringRule", key.get("stringRule"));
                result.put("type", key.get("type"));
            }
            return result;
        }).collect(Collectors.toList());
    }

    private static List<Map<String, Object>> buildAssignmentKeys(ScheduleTaskDraftModel task) {
        List<Map<String, Object>> result = new ArrayList<>();
        result.add(key("planCalcOrderIndex", task == null ? null : task.getPlanCalcOrderIndex(),
                "ASC", "LAST", "DECIMAL", null));
        result.add(key("baseSortIndex", task == null ? null : task.getBaseSortIndex(),
                "ASC", "LAST", "DECIMAL", null));
        result.add(key("businessKey", task == null ? null : task.getBusinessKey(),
                "ASC", "EMPTY_STRING", "STRING", "JAVA_STRING_ORDINAL"));
        return result;
    }

    private static Map<String, Object> rule(String field, String direction, String nullRule) {
        return key(field, null, direction, nullRule, "UNKNOWN", null);
    }

    /**
     * 构建可被离线工具消费的比较键。
     *
     * @param field      字段名
     * @param value      字段值
     * @param direction  排序方向
     * @param nullRule   空值规则
     * @param type       类型
     * @param stringRule 字符串比较规则
     * @return 比较键对象
     */
    public static Map<String, Object> key(String field, Object value, String direction,
                                          String nullRule, String type, String stringRule) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("field", field);
        result.put("value", value);
        result.put("direction", direction);
        result.put("nullRule", nullRule);
        result.put("type", type);
        result.put("stringRule", stringRule);
        return result;
    }

    private static <T extends ScheduleTaskDraftModel> List<Map<String, Object>> resolveFirstDifferentCondition(
            List<T> sortedTaskList, Integer position,
            Function<T, List<Map<String, Object>>> keyProvider) {
        if (position == null || position <= 1 || keyProvider == null) {
            return Collections.emptyList();
        }
        T previous = sortedTaskList.get(position - 2);
        T current = sortedTaskList.get(position - 1);
        List<Map<String, Object>> previousKeys = keyProvider.apply(previous);
        List<Map<String, Object>> currentKeys = keyProvider.apply(current);
        int size = Math.min(previousKeys == null ? 0 : previousKeys.size(),
                currentKeys == null ? 0 : currentKeys.size());
        for (int index = 0; index < size; index++) {
            Object previousValue = previousKeys.get(index).get("value");
            Object currentValue = currentKeys.get(index).get("value");
            if (!Objects.equals(previousValue, currentValue)) {
                Map<String, Object> result = new LinkedHashMap<>();
                result.put("field", currentKeys.get(index).get("field"));
                result.put("previousValue", previousValue);
                result.put("currentValue", currentValue);
                result.put("direction", currentKeys.get(index).get("direction"));
                return Collections.singletonList(result);
            }
        }
        return Collections.emptyList();
    }

    private static String formatTime(Date value) {
        return value == null ? null : UTC_FORMATTER.format(Instant.ofEpochMilli(value.getTime()).atOffset(ZoneOffset.UTC));
    }

    private static String value(Object value) {
        return value == null ? "<null>" : String.valueOf(value);
    }

    private static String decimal(BigDecimal value) {
        return value == null ? "<null>" : value.stripTrailingZeros().toPlainString();
    }
}
