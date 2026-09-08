package com.zlt.aps.lh.engine.strategy.support;

import com.zlt.aps.lh.api.enums.UnscheduledReasonEnum;
import lombok.Data;

/**
 * 单次未排原因事件。
 *
 * @author APS
 */
@Data
public class UnscheduledReasonEvent {

    /** 原因定义。 */
    private UnscheduledReasonEnum reasonCode;
    /** 真实业务分支提供的原因详情。 */
    private String detail;
    /** 同一需求内的登记顺序，仅用于稳定输出明细。 */
    private long sequence;

    /**
     * 复制原因事件。
     *
     * @return 原因事件副本
     */
    public UnscheduledReasonEvent copy() {
        UnscheduledReasonEvent target = new UnscheduledReasonEvent();
        target.setReasonCode(reasonCode);
        target.setDetail(detail);
        target.setSequence(sequence);
        return target;
    }
}
