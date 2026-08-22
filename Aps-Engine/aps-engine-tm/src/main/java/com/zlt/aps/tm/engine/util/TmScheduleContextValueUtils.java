package com.zlt.aps.tm.engine.util;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import com.zlt.aps.common.core.utils.BigDecimalUtils;
import com.zlt.aps.tm.engine.domain.TmParamValue;
import com.zlt.aps.tm.engine.domain.TmRuleTrace;
import com.zlt.aps.tm.engine.domain.TmScheduleContext;
import com.zlt.aps.tm.engine.domain.TmTaskDraft;

import java.math.BigDecimal;

/**
 * 胎面排程上下文的无副作用取值工具。
 *
 * <p>统一日期格式化、卷曲长度、规则证据和参数有效值读取；参数是否 trim 由调用方显式传入，
 * 以保留各策略现有的兼容口径。</p>
 */
public final class TmScheduleContextValueUtils {

    private TmScheduleContextValueUtils() {
    }

    /**
     * 格式化排程日期。
     *
     * @param context 排程上下文
     * @return 日期缺失时返回 null，否则返回 yyyy-MM-dd 字符串
     */
    public static String formatScheduleDate(TmScheduleContext context) {
        return context == null || context.getScheduleDate() == null ? null : DateUtil.formatDate(context.getScheduleDate());
    }

    /**
     * 按任务卷曲长度优先、默认卷曲长度兜底的既有口径取值。
     *
     * @param task 任务草稿
     * @return 非负卷曲长度；任务为空或默认值为空时返回零
     */
    public static BigDecimal resolveCurlLength(TmTaskDraft task) {
        if (task != null && task.getCurlRollLength() != null
                && task.getCurlRollLength().compareTo(BigDecimal.ZERO) > 0) {
            return task.getCurlRollLength();
        }
        return task == null ? BigDecimal.ZERO : BigDecimalUtils.valueOf(task.getDefaultCurlRollLength());
    }

    /**
     * 获取任务规则证据对象，不存在时创建。
     *
     * @param context 排程上下文
     * @param task 任务草稿
     * @return 与任务业务键对应的规则证据对象
     */
    public static TmRuleTrace traceOf(TmScheduleContext context, TmTaskDraft task) {
        return context.getRuleTraceMap().computeIfAbsent(task.getBusinessKey(), key -> new TmRuleTrace());
    }

    /**
     * 读取参数有效值，并保留调用方既有的 trim 兼容口径。
     *
     * @param context 排程上下文
     * @param paramCode 参数编码
     * @param defaultValue 缺省值
     * @param trimEffectiveValue 是否去除有效值首尾空格
     * @return 有效参数值或缺省值
     */
    public static String readParam(TmScheduleContext context, String paramCode, String defaultValue,
                                   boolean trimEffectiveValue) {
        TmParamValue paramValue = context.getParamMap().get(paramCode);
        if (paramValue == null || StrUtil.isBlank(paramValue.getEffectiveValue())) {
            return defaultValue;
        }
        return trimEffectiveValue ? paramValue.getEffectiveValue().trim() : paramValue.getEffectiveValue();
    }
}
