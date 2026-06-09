package com.zlt.aps.tq.engine.exception;

import com.zlt.aps.tq.engine.enums.TqScheduleStepEnum;
import lombok.Getter;

/**
 * 胎圈排程领域异常。
 *
 * <p>携带步骤信息，便于定位排程在哪个阶段失败。</p>
 *
 * @author APS
 */
@Getter
public class TqScheduleException extends RuntimeException {

    /** 发生异常的排程步骤 */
    private final TqScheduleStepEnum step;

    /** 批次号 */
    private final String batchNo;

    /**
     * 构造排程异常
     *
     * @param step    发生异常的步骤
     * @param message 异常信息
     */
    public TqScheduleException(TqScheduleStepEnum step, String message) {
        super(message);
        this.step = step;
        this.batchNo = null;
    }

    /**
     * 构造排程异常（带批次号）
     *
     * @param step     发生异常的步骤
     * @param batchNo  批次号
     * @param message  异常信息
     */
    public TqScheduleException(TqScheduleStepEnum step, String batchNo, String message) {
        super(message);
        this.step = step;
        this.batchNo = batchNo;
    }

    /**
     * 构造排程异常（带原因）
     *
     * @param step    发生异常的步骤
     * @param message 异常信息
     * @param cause   原始异常
     */
    public TqScheduleException(TqScheduleStepEnum step, String message, Throwable cause) {
        super(message, cause);
        this.step = step;
        this.batchNo = null;
    }
}
