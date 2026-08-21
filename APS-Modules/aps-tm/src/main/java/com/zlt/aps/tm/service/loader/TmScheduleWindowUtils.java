package com.zlt.aps.tm.service.loader;

import cn.hutool.core.date.DateUnit;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import com.zlt.aps.tm.api.constant.TmScheduleConstants;
import com.zlt.aps.tm.api.domain.entity.TmScheduleResult;
import com.zlt.aps.tm.api.domain.entity.TmShiftConfig;
import com.zlt.aps.tm.api.enums.TmYesNoEnum;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

/**
 * 胎面自动与人工排程共用的班次时间窗口工具。
 *
 * <p>仅处理已加载的班次配置和横向结果字段，不访问数据库，也不写入排程上下文，
 * 以保证自动排程与人工滚动使用相同的跨天、重叠时长和末班识别口径。</p>
 */
public final class TmScheduleWindowUtils {

    private TmScheduleWindowUtils() {
    }

    /**
     * 根据排程日期和班次配置构建有序的完整时间窗口。
     *
     * @param scheduleDate 排程日期
     * @param shiftConfigList 已按班次顺序加载的配置列表
     * @param parseFailureConsumer 班次时间解析失败的诊断回调，可为空
     * @return 班次顺序到开始、结束时间的映射；无有效配置时返回空映射
     */
    public static Map<Integer, Date[]> buildShiftWindowMap(Date scheduleDate,
                                                            List<TmShiftConfig> shiftConfigList,
                                                            BiConsumer<TmShiftConfig, Exception> parseFailureConsumer) {
        Map<Integer, Date[]> shiftWindowMap = new LinkedHashMap<>();
        if (scheduleDate == null || shiftConfigList == null || shiftConfigList.isEmpty()) {
            return shiftWindowMap;
        }
        String scheduleDateText = DateUtil.formatDate(scheduleDate);
        Date previousEndTime = null;
        for (TmShiftConfig config : shiftConfigList) {
            if (config == null || config.getShiftOrder() == null
                    || StrUtil.isBlank(config.getPlanStartTime()) || StrUtil.isBlank(config.getPlanEndTime())) {
                continue;
            }
            try {
                Date startTime = DateUtil.parse(scheduleDateText + " " + config.getPlanStartTime());
                Date endTime = DateUtil.parse(scheduleDateText + " " + config.getPlanEndTime());
                if (TmYesNoEnum.YES.getCode().equals(config.getCrossDayFlag()) || !endTime.after(startTime)) {
                    endTime = DateUtil.offsetDay(endTime, 1);
                }
                while (previousEndTime != null && startTime.before(previousEndTime)) {
                    startTime = DateUtil.offsetDay(startTime, 1);
                    endTime = DateUtil.offsetDay(endTime, 1);
                }
                shiftWindowMap.put(config.getShiftOrder(), new Date[]{startTime, endTime});
                previousEndTime = endTime;
            } catch (Exception exception) {
                if (parseFailureConsumer != null) {
                    parseFailureConsumer.accept(config, exception);
                }
            }
        }
        return shiftWindowMap;
    }

    /**
     * 计算两个时间区间的重叠小时数。
     *
     * @param sourceStart 源区间开始时间
     * @param sourceEnd 源区间结束时间
     * @param targetStart 目标区间开始时间
     * @param targetEnd 目标区间结束时间
     * @return 重叠小时数；参数不完整或无重叠时返回 0
     */
    public static BigDecimal calculateOverlapHours(Date sourceStart, Date sourceEnd,
                                                    Date targetStart, Date targetEnd) {
        if (sourceStart == null || sourceEnd == null || targetStart == null || targetEnd == null) {
            return BigDecimal.ZERO;
        }
        Date overlapStart = sourceStart.after(targetStart) ? sourceStart : targetStart;
        Date overlapEnd = sourceEnd.before(targetEnd) ? sourceEnd : targetEnd;
        if (!overlapStart.before(overlapEnd)) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.valueOf(DateUtil.between(overlapStart, overlapEnd, DateUnit.MINUTE))
                .divide(BigDecimal.valueOf(TmScheduleConstants.MINUTES_PER_HOUR),
                        TmScheduleConstants.DECIMAL_CALCULATION_SCALE, RoundingMode.HALF_UP);
    }

    /**
     * 从横向排程结果识别最后一个存在计划量和顺序的班次。
     *
     * @param scheduleResult 横向排程结果
     * @param acceptNumericStringSequence 是否兼容数字字符串形式的顺序值
     * @return 最后有效班次顺序；不存在时返回 null
     */
    public static Integer resolveLatestShiftOrder(TmScheduleResult scheduleResult,
                                                   boolean acceptNumericStringSequence) {
        if (scheduleResult == null) {
            return null;
        }
        for (int shiftOrder = TmScheduleConstants.TM_MAX_SHIFT_ORDER; shiftOrder >= 1; shiftOrder--) {
            Object planQtyValue = scheduleResult.getFieldValueByFieldName(
                    String.format(TmScheduleConstants.SHIFT_PLAN_QTY_FIELD_TEMPLATE, shiftOrder));
            Object sequenceValue = scheduleResult.getFieldValueByFieldName(
                    String.format(TmScheduleConstants.SHIFT_SEQUENCE_FIELD_TEMPLATE, shiftOrder));
            if (toBigDecimal(planQtyValue).compareTo(BigDecimal.ZERO) <= 0
                    || resolveSequence(sequenceValue, acceptNumericStringSequence) == null) {
                continue;
            }
            return shiftOrder;
        }
        return null;
    }

    /**
     * 将横向计划量值转换为数值，兼容历史空字符串。
     *
     * @param value 横向字段原始值
     * @return 非空计划量数值
     */
    private static BigDecimal toBigDecimal(Object value) {
        if (value == null || (value instanceof String && StrUtil.isBlank((String) value))) {
            return BigDecimal.ZERO;
        }
        try {
            return new BigDecimal(String.valueOf(value));
        } catch (NumberFormatException exception) {
            return BigDecimal.ZERO;
        }
    }

    /**
     * 按调用方既有兼容策略解析顺序字段。
     *
     * @param value 横向顺序字段原始值
     * @param acceptNumericStringSequence 是否兼容数字字符串
     * @return 有效顺序；无效时返回 null
     */
    private static Integer resolveSequence(Object value, boolean acceptNumericStringSequence) {
        if (value instanceof Integer) {
            return (Integer) value;
        }
        if (!acceptNumericStringSequence || value == null) {
            return null;
        }
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        if (!(value instanceof String) || StrUtil.isBlank((String) value)) {
            return null;
        }
        try {
            return Integer.valueOf((String) value);
        } catch (NumberFormatException exception) {
            return null;
        }
    }
}
