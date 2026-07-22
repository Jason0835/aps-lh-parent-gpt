package com.zlt.aps.tm.engine.domain.manual;

import lombok.Data;

/**
 * 单机台人工滚动的最早影响位置。
 */
@Data
public class TmManualRollingScope {

    /** 机台编码 */
    private String machineCode;

    /** 最早影响班次 */
    private Integer startShiftOrder;

    /** 最早影响顺序 */
    private Integer startSequence;

    /**
     * 合并新的影响位置，只保留时间上最早的位置。
     *
     * @param shiftOrder 班次
     * @param sequence   班内顺序
     */
    public void merge(Integer shiftOrder, Integer sequence) {
        int normalizedShiftOrder = shiftOrder == null ? 1 : shiftOrder;
        int normalizedSequence = sequence == null ? 1 : sequence;
        if (this.startShiftOrder == null || normalizedShiftOrder < this.startShiftOrder
                || (normalizedShiftOrder == this.startShiftOrder
                && (this.startSequence == null || normalizedSequence < this.startSequence))) {
            this.startShiftOrder = normalizedShiftOrder;
            this.startSequence = normalizedSequence;
        }
    }
}
