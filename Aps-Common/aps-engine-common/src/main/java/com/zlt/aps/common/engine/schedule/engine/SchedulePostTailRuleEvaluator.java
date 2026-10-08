package com.zlt.aps.common.engine.schedule.engine;

import java.util.ArrayList;
import java.util.List;

/**
 * 收尾后起排班次公共纯规则组件。
 *
 * <p>组件只处理参数解析和 C/S/N 比较，不查询数据库，不依赖 TM/TC 业务类，
 * 由两个领域服务负责提供参数快照和实际收尾状态。</p>
 */
public final class SchedulePostTailRuleEvaluator {

    /**
     * 参数缺失或禁用时的回退原因。
     */
    public static final String FALLBACK_MISSING_OR_DISABLED = "MISSING_OR_DISABLED";

    /**
     * 参数空值时的回退原因。
     */
    public static final String FALLBACK_BLANK_VALUE = "BLANK_VALUE";

    /**
     * 参数编码冲突时的回退原因。
     */
    public static final String FALLBACK_PARAM_CODE_CONFLICT = "PARAM_CODE_CONFLICT";

    /**
     * 参数非正整数时的回退原因。
     */
    public static final String FALLBACK_NON_POSITIVE = "NON_POSITIVE";

    /**
     * 参数不是整数或发生整数溢出时的回退原因。
     */
    public static final String FALLBACK_INVALID_INTEGER = "INVALID_INTEGER_OR_OVERFLOW";

    /**
     * 未确认实际收尾时的规则原因。
     */
    public static final String DECISION_CLOSE_OUT_NOT_CONFIRMED = "ACTUAL_CLOSE_OUT_NOT_CONFIRMED";

    /**
     * 目标班次无效时的规则原因。
     */
    public static final String DECISION_TARGET_SHIFT_UNRESOLVED = "TARGET_SHIFT_UNRESOLVED";

    /**
     * 目标班次不晚于收尾班次时的规则原因。
     */
    public static final String DECISION_TARGET_NOT_AFTER_CLOSE_OUT = "TARGET_NOT_AFTER_CLOSE_OUT";

    /**
     * 目标班次达到恢复班次时的规则原因。
     */
    public static final String DECISION_RECOVERY_SHIFT_REACHED = "RECOVERY_SHIFT_REACHED";

    /**
     * 目标班次仍处于收尾间隔时的规则原因。
     */
    public static final String DECISION_POST_TAIL_INTERVAL = "POST_TAIL_INTERVAL";

    private SchedulePostTailRuleEvaluator() {
    }

    /**
     * 解析一次排程使用的收尾后起排班次参数。
     *
     * @param parameterSnapshot 参数快照；为空表示缺失或禁用
     * @param paramCode         参数编码
     * @param expectedParamName 约定参数名称
     * @param defaultValue      代码默认值
     * @return 参数解析快照
     */
    public static SchedulePostTailShiftParameter resolveParameter(
            ScheduleParamValueModel parameterSnapshot, String paramCode,
            String expectedParamName, int defaultValue) {
        SchedulePostTailShiftParameter result = new SchedulePostTailShiftParameter();
        result.setParamCode(paramCode);
        result.setExpectedParamName(expectedParamName);
        result.setEffectiveShiftCount(defaultValue);
        result.setSource(parameterSnapshot == null || isBlank(parameterSnapshot.getSource())
                ? "DEFAULT" : parameterSnapshot.getSource());
        result.setRawValue(parameterSnapshot == null ? null : parameterSnapshot.getParamValue());
        result.setDefaultValue(parameterSnapshot == null ? String.valueOf(defaultValue)
                : parameterSnapshot.getDefaultValue());

        if (parameterSnapshot == null) {
            result.setFallbackReason(FALLBACK_MISSING_OR_DISABLED);
            return result;
        }
        if ((!isBlank(parameterSnapshot.getParamCode())
                && !paramCode.equals(parameterSnapshot.getParamCode().trim()))
                || (!isBlank(parameterSnapshot.getParamName())
                && !expectedParamName.equals(parameterSnapshot.getParamName().trim()))) {
            result.setFallbackReason(FALLBACK_PARAM_CODE_CONFLICT);
            return result;
        }
        String candidateValue = parameterSnapshot.getParamValue();
        if (isBlank(candidateValue)) {
            if ("DEFAULT".equals(parameterSnapshot.getSource())
                    && !isBlank(parameterSnapshot.getDefaultValue())) {
                candidateValue = parameterSnapshot.getDefaultValue();
                result.setFallbackReason(FALLBACK_MISSING_OR_DISABLED);
            } else {
                result.setFallbackReason("DEFAULT".equals(parameterSnapshot.getSource())
                        ? FALLBACK_MISSING_OR_DISABLED : FALLBACK_BLANK_VALUE);
                return result;
            }
        }
        try {
            int parsedValue = Integer.parseInt(candidateValue.trim());
            if (parsedValue <= 0) {
                result.setFallbackReason(FALLBACK_NON_POSITIVE);
                return result;
            }
            result.setEffectiveShiftCount(parsedValue);
            return result;
        } catch (NumberFormatException exception) {
            result.setFallbackReason(FALLBACK_INVALID_INTEGER);
            return result;
        }
    }

    /**
     * 按实际收尾班次 C、目标生产班次 S 和参数 N 执行统一判定。
     *
     * @param closeOutState    实际收尾状态
     * @param targetShiftOrder 目标生产逻辑班次 S
     * @param parameter        参数快照
     * @return 计划量入口判定
     */
    public static SchedulePostTailDecision evaluate(SchedulePostTailCloseOutState closeOutState,
                                                    Integer targetShiftOrder,
                                                    SchedulePostTailShiftParameter parameter) {
        SchedulePostTailDecision result = new SchedulePostTailDecision();
        result.setCloseOutConfirmed(closeOutState != null
                && Boolean.TRUE.equals(closeOutState.getConfirmed()));
        result.setActualCloseOutShiftOrder(closeOutState == null
                ? null : closeOutState.getActualCloseOutShiftOrder());
        result.setTargetShiftOrder(targetShiftOrder);
        result.setBlocked(Boolean.FALSE);
        result.setAllowNormalPlan(Boolean.TRUE);
        if (!Boolean.TRUE.equals(result.getCloseOutConfirmed())) {
            result.setDecisionReason(DECISION_CLOSE_OUT_NOT_CONFIRMED);
            return result;
        }
        if (targetShiftOrder == null || targetShiftOrder <= 0
                || result.getActualCloseOutShiftOrder() == null
                || result.getActualCloseOutShiftOrder() <= 0) {
            result.setDecisionReason(DECISION_TARGET_SHIFT_UNRESOLVED);
            return result;
        }
        int interval = targetShiftOrder - result.getActualCloseOutShiftOrder();
        result.setIntervalShiftCount(interval);
        int effectiveShiftCount = resolveEffectiveShiftCount(parameter);
        long recoveryShiftOrder = (long) result.getActualCloseOutShiftOrder() + effectiveShiftCount;
        result.setRecoveryShiftOrder(recoveryShiftOrder > Integer.MAX_VALUE
                ? null : (int) recoveryShiftOrder);
        result.setSkippedTargetShiftOrders(thisSkippedShifts(result.getActualCloseOutShiftOrder(),
                targetShiftOrder, effectiveShiftCount));
        if (interval <= 0) {
            result.setDecisionReason(DECISION_TARGET_NOT_AFTER_CLOSE_OUT);
            return result;
        }
        if (interval >= effectiveShiftCount) {
            result.setDecisionReason(DECISION_RECOVERY_SHIFT_REACHED);
            return result;
        }
        result.setBlocked(Boolean.TRUE);
        result.setAllowNormalPlan(Boolean.FALSE);
        result.setDecisionReason(DECISION_POST_TAIL_INTERVAL);
        return result;
    }

    /**
     * 获取运行态参数中的有效班次数，防止非参数加载入口传入非法值。
     *
     * @param parameter 参数快照
     * @return 正整数班次数
     */
    private static int resolveEffectiveShiftCount(SchedulePostTailShiftParameter parameter) {
        if (parameter == null || parameter.getEffectiveShiftCount() == null
                || parameter.getEffectiveShiftCount() <= 0) {
            return 2;
        }
        return parameter.getEffectiveShiftCount();
    }

    private static List<Integer> thisSkippedShifts(Integer closeOutShiftOrder,
                                                   Integer targetShiftOrder, int effectiveShiftCount) {
        List<Integer> skippedShiftOrders = new ArrayList<>();
        if (closeOutShiftOrder == null || targetShiftOrder == null || targetShiftOrder <= closeOutShiftOrder) {
            return skippedShiftOrders;
        }
        long lastSkippedShiftOrder = Math.min((long) targetShiftOrder - 1,
                (long) closeOutShiftOrder + Math.max(effectiveShiftCount - 1, 0));
        for (long shiftOrder = closeOutShiftOrder + 1; shiftOrder <= lastSkippedShiftOrder;
             shiftOrder++) {
            if (shiftOrder <= Integer.MAX_VALUE) {
                skippedShiftOrders.add((int) shiftOrder);
            }
        }
        return skippedShiftOrders;
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
