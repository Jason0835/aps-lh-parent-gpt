package com.zlt.aps.tc.engine.validator;

import com.ruoyi.common.exception.ServiceException;
import com.zlt.aps.tc.api.constant.TcScheduleConstants;
import com.zlt.aps.tc.api.domain.entity.TcScheduleResult;
import com.zlt.aps.tc.api.enums.TcScheduleErrorCodeEnum;

import java.math.BigDecimal;
import java.util.stream.IntStream;

/**
 * 胎侧人工插单位置校验器。
 *
 * <p>字段解析逻辑集中在该类中，业务 Service 基于解析出的班次、顺序和完成量，
 * 按同机台同班次在产规格数量执行数据库口径校验。</p>
 */
public class TcInsertPositionValidator {

    private TcInsertPositionValidator() {
    }

    /**
     * 校验人工插单位置是否在第二个在产规格之后。
     *
     * @param scheduleResult 插单排程结果
     * @throws ServiceException 插单顺序不满足业务规则时抛出
     */
    public static void validateAfterSecondSequence(TcScheduleResult scheduleResult) {
        if (!isAfterSecondSequence(scheduleResult)) {
            throw new ServiceException(TcScheduleErrorCodeEnum.TC_INSERT_POSITION_INVALID.getDefaultMessage());
        }
    }

    /**
     * 判断插单位置是否在第二个在产规格之后。
     *
     * @param scheduleResult 插单排程结果
     * @return true 表示位置合法
     */
    public static boolean isAfterSecondSequence(TcScheduleResult scheduleResult) {
        if (scheduleResult == null) {
            return true;
        }
        return IntStream.rangeClosed(1, TcScheduleConstants.TC_MAX_SHIFT_ORDER)
                .mapToObj(shiftOrder -> resolveSequence(scheduleResult, shiftOrder))
                .noneMatch(sequence -> sequence != null
                        && sequence <= TcScheduleConstants.MIN_INSERT_SEQUENCE);
    }

    /**
     * 根据横向班次顺序字段解析本次操作班次。
     *
     * @param scheduleResult 插单排程结果
     * @return 班次顺序，无法识别时返回 null
     */
    public static Integer resolveShiftOrder(TcScheduleResult scheduleResult) {
        if (scheduleResult == null) {
            return null;
        }
        return IntStream.rangeClosed(1, TcScheduleConstants.TC_MAX_SHIFT_ORDER)
                .filter(shiftOrder -> resolveSequence(scheduleResult, shiftOrder) != null)
                .boxed().findFirst().orElse(null);
    }

    /**
     * 读取指定班次顺序。
     *
     * @param scheduleResult 排程结果
     * @param shiftOrder     班次顺序
     * @return 当前班次顺序，未设置时返回 null
     */
    public static Integer resolveSequence(TcScheduleResult scheduleResult, Integer shiftOrder) {
        if (scheduleResult == null || shiftOrder == null) {
            return null;
        }
        Object value = scheduleResult.getFieldValueByFieldName(String.format(
                TcScheduleConstants.SHIFT_SEQUENCE_FIELD_TEMPLATE, shiftOrder));
        return value instanceof Integer ? (Integer) value : null;
    }

    /**
     * 读取指定班次完成量。
     *
     * @param scheduleResult 排程结果
     * @param shiftOrder     班次顺序
     * @return 完成量，未设置时返回 0
     */
    public static BigDecimal getFinishQty(TcScheduleResult scheduleResult, Integer shiftOrder) {
        if (scheduleResult == null || shiftOrder == null) {
            return BigDecimal.ZERO;
        }
        Object value = scheduleResult.getFieldValueByFieldName(String.format(
                TcScheduleConstants.SHIFT_FINISH_QTY_FIELD_TEMPLATE, shiftOrder));
        return value instanceof BigDecimal ? defaultQty((BigDecimal) value) : BigDecimal.ZERO;
    }

    /**
     * 空完成量按 0 处理。
     *
     * @param qty 完成量
     * @return 非空完成量
     */
    private static BigDecimal defaultQty(BigDecimal qty) {
        return qty == null ? BigDecimal.ZERO : qty;
    }
}
