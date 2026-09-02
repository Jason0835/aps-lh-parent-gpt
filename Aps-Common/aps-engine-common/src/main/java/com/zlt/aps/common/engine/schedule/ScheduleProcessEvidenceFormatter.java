package com.zlt.aps.common.engine.schedule;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;

import java.lang.reflect.Array;
import java.math.BigDecimal;
import java.time.temporal.TemporalAccessor;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 自动排程结构化证据中文格式化器。
 *
 * <p>保留原证据字段编码用于核对，同时在编码前提供中文含义，确保测试人员不需要阅读 Java 对象。</p>
 */
public final class ScheduleProcessEvidenceFormatter {

    /** 结构化证据字段中文名称。 */
    private static final Map<String, String> FIELD_NAMES = buildFieldNames();

    private ScheduleProcessEvidenceFormatter() {
    }

    /**
     * 将规则证据格式化为中文可读文本。
     *
     * @param evidence 规则结构化证据
     * @return 中文可读证据
     */
    public static String format(Object evidence) {
        if (evidence == null) {
            return "未提供（本规则没有产生额外证据）";
        }
        if (evidence instanceof Map) {
            return formatMap((Map<?, ?>) evidence);
        }
        if (evidence instanceof Collection) {
            return formatCollection((Collection<?>) evidence);
        }
        if (evidence.getClass().isArray()) {
            return formatArray(evidence);
        }
        if (isSimpleValue(evidence)) {
            return formatValue(evidence);
        }
        Map<String, Object> beanMap = BeanUtil.beanToMap(evidence, new LinkedHashMap<>(), false, false);
        return beanMap.isEmpty() ? String.valueOf(evidence) : formatMap(beanMap);
    }

    /**
     * 获取证据字段中文名。
     *
     * @param fieldName 原字段名
     * @return 中文名和原字段编码
     */
    public static String displayFieldName(String fieldName) {
        String chineseName = FIELD_NAMES.get(fieldName);
        return StrUtil.isBlank(chineseName) ? "补充证据（" + fieldName + "）" : chineseName + "（" + fieldName + "）";
    }

    private static String formatMap(Map<?, ?> evidenceMap) {
        if (evidenceMap == null || evidenceMap.isEmpty()) {
            return "未提供（证据集合为空）";
        }
        return evidenceMap.entrySet().stream()
                .map(entry -> displayFieldName(String.valueOf(entry.getKey())) + "=" + format(entry.getValue()))
                .collect(Collectors.joining("；"));
    }

    private static String formatCollection(Collection<?> values) {
        if (values == null || values.isEmpty()) {
            return "空集合";
        }
        return values.stream().map(ScheduleProcessEvidenceFormatter::format)
                .collect(Collectors.joining("，", "[", "]"));
    }

    private static String formatArray(Object array) {
        int length = Array.getLength(array);
        if (length == 0) {
            return "空数组";
        }
        String[] values = new String[length];
        for (int index = 0; index < length; index++) {
            values[index] = format(Array.get(array, index));
        }
        return Arrays.stream(values).collect(Collectors.joining("，", "[", "]"));
    }

    private static boolean isSimpleValue(Object value) {
        return value instanceof CharSequence || value instanceof Number || value instanceof Boolean
                || value instanceof Enum || value instanceof Date || value instanceof TemporalAccessor;
    }

    private static String formatValue(Object value) {
        if (value == null) {
            return "未提供";
        }
        if (value instanceof BigDecimal) {
            return ((BigDecimal) value).stripTrailingZeros().toPlainString();
        }
        String text = String.valueOf(value);
        return StrUtil.isBlank(text) || "null".equalsIgnoreCase(text.trim()) ? "未提供" : text;
    }

    private static Map<String, String> buildFieldNames() {
        Map<String, String> fieldNames = new LinkedHashMap<>();
        fieldNames.put("batchNo", "批次号");
        fieldNames.put("traceId", "追踪号");
        fieldNames.put("date", "业务日期");
        fieldNames.put("sourceDate", "来源日期");
        fieldNames.put("mode", "匹配模式");
        fieldNames.put("strategyCode", "策略编码");
        fieldNames.put("ruleCode", "规则编码");
        fieldNames.put("result", "规则结果");
        fieldNames.put("reason", "处理原因");
        fieldNames.put("reasonCode", "原因编码");
        fieldNames.put("reasonDesc", "原因说明");
        fieldNames.put("formula", "计算公式");
        fieldNames.put("calcFormulaDesc", "计划量公式");
        fieldNames.put("paramCode", "参数编码");
        fieldNames.put("sourceType", "来源类型");
        fieldNames.put("sourceTask", "来源任务");
        fieldNames.put("task", "任务业务键");
        fieldNames.put("planGroupKey", "计划组业务键");
        fieldNames.put("sourceCount", "来源任务数量");
        fieldNames.put("sourceOrderNos", "来源订单号");
        fieldNames.put("sourceShiftOrder", "来源班次");
        fieldNames.put("sourceShiftOrders", "来源班次集合");
        fieldNames.put("targetShiftOrder", "目标班次");
        fieldNames.put("shiftOrder", "当前班次");
        fieldNames.put("sortIndex", "排序名次");
        fieldNames.put("startupShift", "是否开班班次");
        fieldNames.put("startupSortPriority", "开班排序优先级");
        fieldNames.put("originalDemandQty", "调整前需求量");
        fieldNames.put("currentShiftDemandQty", "当班需求量");
        fieldNames.put("guardDemandQty", "保证窗口需求量");
        fieldNames.put("demandQty", "有效需求量");
        fieldNames.put("rollingStockQty", "滚动库存量");
        fieldNames.put("stockDeductQty", "库存抵扣量");
        fieldNames.put("baseDemandQty", "基础应排量");
        fieldNames.put("planQty", "计划量");
        fieldNames.put("originalPlanQty", "调整前计划量");
        fieldNames.put("finalPlanQty", "调整后计划量");
        fieldNames.put("preLossPlanQty", "损耗前计划量");
        fieldNames.put("lossRate", "损耗率");
        fieldNames.put("lossAddQty", "损耗补量");
        fieldNames.put("planQtyBeforeToolLimit", "工装限制前计划量");
        fieldNames.put("toolOverflowQty", "工装溢出量");
        fieldNames.put("totalToolQty", "工装总量");
        fieldNames.put("effectiveToolQtyLimit", "有效工装上限");
        fieldNames.put("availableToolQty", "可用工装量");
        fieldNames.put("availableToolQtyBeforeShift", "班前可用工装量");
        fieldNames.put("formingReleaseQtyBeforePlan", "班前成型释放工装量");
        fieldNames.put("availableToolQtyBeforePlan", "班前计划可用工装量");
        fieldNames.put("releaseDetails", "班前成型释放明细");
        fieldNames.put("toolUsedQty", "已占用工装量");
        fieldNames.put("remainingToolQty", "剩余工装量");
        fieldNames.put("curlRollLength", "卷长");
        fieldNames.put("adjustedQty", "本次调整量");
        fieldNames.put("assignedQty", "已分配量");
        fieldNames.put("beforeAssignQty", "分配前数量");
        fieldNames.put("overflowQty", "溢出量");
        fieldNames.put("remainingQty", "剩余未排量");
        fieldNames.put("carryoverQty", "顺延量");
        fieldNames.put("beforeMergePlanQty", "合并前计划量");
        fieldNames.put("afterMergePlanQty", "合并后计划量");
        fieldNames.put("machineCode", "机台编码");
        fieldNames.put("targetMachineCode", "目标机台");
        fieldNames.put("selectedMachineCode", "选中机台");
        fieldNames.put("originalMachineCode", "原绑定机台");
        fieldNames.put("remainCapacity", "剩余产能");
        fieldNames.put("machineSpeed", "机台速度");
        fieldNames.put("score", "总得分");
        fieldNames.put("selectedScore", "选中得分");
        fieldNames.put("scoreItems", "评分项");
        fieldNames.put("description", "评分说明");
        fieldNames.put("priority", "决胜优先级");
        fieldNames.put("supplyHours", "库存供应时长");
        fieldNames.put("guardShiftCount", "保证班数");
        fieldNames.put("guardRangeHours", "保证范围小时数");
        fieldNames.put("glueCode", "主胶料编码");
        fieldNames.put("baseGlueCode", "基部胶编码");
        fieldNames.put("mouthPlateCode", "口型编码");
        fieldNames.put("requestedVersion", "请求版本");
        fieldNames.put("selectedVersion", "选中版本");
        fieldNames.put("fallback", "是否回退");
        fieldNames.put("threshold", "阈值");
        fieldNames.put("thresholdSource", "阈值来源");
        fieldNames.put("groupCurrentShiftDemandQty", "组内当班需求量");
        fieldNames.put("groupGuardDemandQty", "组内保证需求量");
        fieldNames.put("groupBaseDemandQty", "组内基础应排量");
        fieldNames.put("groupMinStartAdjustQty", "组内最小起排调整量");
        fieldNames.put("groupRoundAdjustQty", "组内卷长取整调整量");
        fieldNames.put("groupFinalPlanQty", "组内最终计划量");
        return Collections.unmodifiableMap(fieldNames);
    }
}
