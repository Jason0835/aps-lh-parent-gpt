package com.zlt.aps.tm.engine.validator;

import com.ruoyi.common.exception.ServiceException;
import com.zlt.aps.tm.api.domain.entity.TmScheduleResult;
import com.zlt.aps.tm.api.enums.TmScheduleErrorCodeEnum;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

/**
 * 胎面人工插单位置校验器。
 *
 * <p>字段解析逻辑集中在该类中，业务 Service 基于解析出的班次、顺序和完成量，
 * 按同机台同班次在产规格数量执行数据库口径校验。</p>
 */
public class TmInsertPositionValidator {

    private static final int MIN_INSERT_SEQUENCE = 2;

    private TmInsertPositionValidator() {
    }

    /**
     * 校验人工插单位置是否在第二个在产规格之后。
     *
     * @param scheduleResult 插单排程结果
     * @throws ServiceException 插单顺序不满足业务规则时抛出
     */
    public static void validateAfterSecondSequence(TmScheduleResult scheduleResult) {
        if (!isAfterSecondSequence(scheduleResult)) {
            throw new ServiceException(TmScheduleErrorCodeEnum.TM_INSERT_POSITION_INVALID.getDefaultMessage());
        }
    }

    /**
     * 判断插单位置是否在第二个在产规格之后。
     *
     * @param scheduleResult 插单排程结果
     * @return true 表示位置合法
     */
    public static boolean isAfterSecondSequence(TmScheduleResult scheduleResult) {
        if (scheduleResult == null) {
            return true;
        }
        List<Integer> sequenceList = Arrays.asList(scheduleResult.getClass1Sequence(), scheduleResult.getClass2Sequence(),
                scheduleResult.getClass3Sequence(), scheduleResult.getClass4Sequence(), scheduleResult.getClass5Sequence(),
                scheduleResult.getClass6Sequence());
        return sequenceList.stream().noneMatch(sequence -> sequence != null && sequence <= MIN_INSERT_SEQUENCE);
    }

    /**
     * 根据横向班次顺序字段解析本次操作班次。
     *
     * @param scheduleResult 插单排程结果
     * @return 班次顺序，无法识别时返回 null
     */
    public static Integer resolveShiftOrder(TmScheduleResult scheduleResult) {
        if (scheduleResult == null) {
            return null;
        }
        if (scheduleResult.getClass1Sequence() != null) {
            return 1;
        }
        if (scheduleResult.getClass2Sequence() != null) {
            return 2;
        }
        if (scheduleResult.getClass3Sequence() != null) {
            return 3;
        }
        if (scheduleResult.getClass4Sequence() != null) {
            return 4;
        }
        if (scheduleResult.getClass5Sequence() != null) {
            return 5;
        }
        if (scheduleResult.getClass6Sequence() != null) {
            return 6;
        }
        return null;
    }

    /**
     * 读取指定班次顺序。
     *
     * @param scheduleResult 排程结果
     * @param shiftOrder     班次顺序
     * @return 当前班次顺序，未设置时返回 null
     */
    public static Integer resolveSequence(TmScheduleResult scheduleResult, Integer shiftOrder) {
        if (scheduleResult == null || shiftOrder == null) {
            return null;
        }
        if (Integer.valueOf(1).equals(shiftOrder)) {
            return scheduleResult.getClass1Sequence();
        }
        if (Integer.valueOf(2).equals(shiftOrder)) {
            return scheduleResult.getClass2Sequence();
        }
        if (Integer.valueOf(3).equals(shiftOrder)) {
            return scheduleResult.getClass3Sequence();
        }
        if (Integer.valueOf(4).equals(shiftOrder)) {
            return scheduleResult.getClass4Sequence();
        }
        if (Integer.valueOf(5).equals(shiftOrder)) {
            return scheduleResult.getClass5Sequence();
        }
        if (Integer.valueOf(6).equals(shiftOrder)) {
            return scheduleResult.getClass6Sequence();
        }
        return null;
    }

    /**
     * 读取指定班次完成量。
     *
     * @param scheduleResult 排程结果
     * @param shiftOrder     班次顺序
     * @return 完成量，未设置时返回 0
     */
    public static BigDecimal getFinishQty(TmScheduleResult scheduleResult, Integer shiftOrder) {
        if (scheduleResult == null || shiftOrder == null) {
            return BigDecimal.ZERO;
        }
        if (Integer.valueOf(1).equals(shiftOrder)) {
            return defaultQty(scheduleResult.getClass1FinishQty());
        }
        if (Integer.valueOf(2).equals(shiftOrder)) {
            return defaultQty(scheduleResult.getClass2FinishQty());
        }
        if (Integer.valueOf(3).equals(shiftOrder)) {
            return defaultQty(scheduleResult.getClass3FinishQty());
        }
        if (Integer.valueOf(4).equals(shiftOrder)) {
            return defaultQty(scheduleResult.getClass4FinishQty());
        }
        if (Integer.valueOf(5).equals(shiftOrder)) {
            return defaultQty(scheduleResult.getClass5FinishQty());
        }
        if (Integer.valueOf(6).equals(shiftOrder)) {
            return defaultQty(scheduleResult.getClass6FinishQty());
        }
        return BigDecimal.ZERO;
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
