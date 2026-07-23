package com.zlt.aps.common.engine.schedule.constraint;

import java.math.BigDecimal;

/**
 * 相邻排程任务切换约束计算结果。
 */
public class ScheduleTransitionConstraintResult {

    /** 规格切换折算产能。 */
    private BigDecimal specSwitchCapacityDeduct = BigDecimal.ZERO;

    /** 主胶料切换固定产能扣减。 */
    private BigDecimal glueSwitchCapacityDeduct = BigDecimal.ZERO;

    /**
     * 获取规格切换折算产能。
     *
     * @return 非负扣减量
     */
    public BigDecimal getSpecSwitchCapacityDeduct() {
        return specSwitchCapacityDeduct;
    }

    /**
     * 设置规格切换折算产能。
     *
     * @param specSwitchCapacityDeduct 非负扣减量
     */
    public void setSpecSwitchCapacityDeduct(BigDecimal specSwitchCapacityDeduct) {
        this.specSwitchCapacityDeduct = specSwitchCapacityDeduct;
    }

    /**
     * 获取胶料切换固定产能扣减。
     *
     * @return 非负扣减量
     */
    public BigDecimal getGlueSwitchCapacityDeduct() {
        return glueSwitchCapacityDeduct;
    }

    /**
     * 设置胶料切换固定产能扣减。
     *
     * @param glueSwitchCapacityDeduct 非负扣减量
     */
    public void setGlueSwitchCapacityDeduct(BigDecimal glueSwitchCapacityDeduct) {
        this.glueSwitchCapacityDeduct = glueSwitchCapacityDeduct;
    }

    /**
     * 汇总相邻任务的全部切换产能扣减。
     *
     * @return 规格与主胶料切换扣减合计
     */
    public BigDecimal getTotalCapacityDeduct() {
        return this.specSwitchCapacityDeduct.add(this.glueSwitchCapacityDeduct);
    }
}
