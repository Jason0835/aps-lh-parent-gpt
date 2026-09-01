package com.zlt.aps.lh.engine.strategy.support;

/**
 * 新增排产班次总计划量原子校验结果。
 *
 * @author APS
 */
public final class NewSpecShiftTotalQtyValidation {

    private final boolean allowed;
    private final int currentShiftQty;
    private final int deltaQty;
    private final int projectedShiftQty;
    private final int shiftLimit;

    public NewSpecShiftTotalQtyValidation(boolean allowed,
                                          int currentShiftQty,
                                          int deltaQty,
                                          int projectedShiftQty,
                                          int shiftLimit) {
        this.allowed = allowed;
        this.currentShiftQty = currentShiftQty;
        this.deltaQty = deltaQty;
        this.projectedShiftQty = projectedShiftQty;
        this.shiftLimit = shiftLimit;
    }

    public boolean isAllowed() {
        return allowed;
    }

    public int getCurrentShiftQty() {
        return currentShiftQty;
    }

    public int getDeltaQty() {
        return deltaQty;
    }

    public int getProjectedShiftQty() {
        return projectedShiftQty;
    }

    public int getShiftLimit() {
        return shiftLimit;
    }
}
