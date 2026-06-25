package com.zlt.aps.tm.engine.validator;

import com.ruoyi.common.exception.ServiceException;
import com.zlt.aps.tm.api.domain.entity.TmScheduleResult;
import org.junit.Test;

import java.math.BigDecimal;

import static org.junit.Assert.*;

/**
 * 胎面插单位置校验工具测试。
 *
 * <p>覆盖六班顺序解析、计划顺序读取、完成量兜底和第二顺序之后插单限制。</p>
 */
public class TmInsertPositionValidatorTest {

    /**
     * 测试内容：验证空排程结果不阻断插单位置判断。
     * 测试场景：位置校验入参为 null。
     * 预期结果：返回 true，由上层业务必填校验负责拦截空对象。
     */
    @Test
    public void isAfterSecondSequenceShouldAllowNullScheduleResult() {
        assertTrue(TmInsertPositionValidator.isAfterSecondSequence(null));
    }

    /**
     * 测试内容：验证插单顺序小于等于第二顺序时拒绝。
     * 测试场景：一班顺序为 2。
     * 预期结果：抛出业务异常。
     */
    @Test(expected = ServiceException.class)
    public void validateAfterSecondSequenceShouldRejectFirstTwoSequence() {
        TmScheduleResult result = new TmScheduleResult();
        result.setClass1Sequence(2);

        TmInsertPositionValidator.validateAfterSecondSequence(result);
    }

    /**
     * 测试内容：验证插单顺序大于第二顺序时允许。
     * 测试场景：四班顺序为 3。
     * 预期结果：返回 true，校验不抛异常。
     */
    @Test
    public void isAfterSecondSequenceShouldAcceptSequenceAfterSecond() {
        TmScheduleResult result = new TmScheduleResult();
        result.setClass4Sequence(3);

        assertTrue(TmInsertPositionValidator.isAfterSecondSequence(result));
    }

    /**
     * 测试内容：验证六班顺序解析使用第一个有顺序的班次。
     * 测试场景：只设置五班顺序。
     * 预期结果：班次解析为 5，并能读取五班顺序。
     */
    @Test
    public void resolveShiftOrderAndSequenceShouldReadClassFive() {
        TmScheduleResult result = new TmScheduleResult();
        result.setClass5Sequence(8);

        Integer shiftOrder = TmInsertPositionValidator.resolveShiftOrder(result);

        assertEquals(Integer.valueOf(5), shiftOrder);
        assertEquals(Integer.valueOf(8), TmInsertPositionValidator.resolveSequence(result, shiftOrder));
    }

    /**
     * 测试内容：验证无法识别班次时返回空。
     * 测试场景：未设置任何 classNSequence。
     * 预期结果：班次和顺序都返回 null。
     */
    @Test
    public void resolveShiftOrderAndSequenceShouldReturnNullWhenNoSequence() {
        TmScheduleResult result = new TmScheduleResult();

        assertNull(TmInsertPositionValidator.resolveShiftOrder(result));
        assertNull(TmInsertPositionValidator.resolveSequence(result, null));
    }

    /**
     * 测试内容：验证完成量读取按班次映射并对空值兜底。
     * 测试场景：六班完成量为 12，三班完成量未维护。
     * 预期结果：六班返回 12，三班返回 0。
     */
    @Test
    public void getFinishQtyShouldReadTargetShiftAndDefaultZero() {
        TmScheduleResult result = new TmScheduleResult();
        result.setClass6FinishQty(new BigDecimal("12"));

        assertEquals(new BigDecimal("12"), TmInsertPositionValidator.getFinishQty(result, 6));
        assertEquals(BigDecimal.ZERO, TmInsertPositionValidator.getFinishQty(result, 3));
    }
}
