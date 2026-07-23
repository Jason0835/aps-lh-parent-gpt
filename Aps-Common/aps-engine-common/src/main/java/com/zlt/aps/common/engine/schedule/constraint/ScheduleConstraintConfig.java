package com.zlt.aps.common.engine.schedule.constraint;

import java.math.BigDecimal;

/**
 * 排程任务链约束参数。
 *
 * <p>该对象只承载胎面、胎侧均可复用的纯计算参数，不包含数据库实体或模块专属配置键。
 * 业务模块负责将 TM/TC 参数转换为本对象。</p>
 */
public class ScheduleConstraintConfig {

    /** 主胶料切换固定产能扣减。 */
    private BigDecimal glueChangeCapacityDeduct = BigDecimal.ZERO;

    /** 规格切换分钟数。 */
    private BigDecimal specChangeMinutes = BigDecimal.ZERO;

    /** 胶料切换分钟数。 */
    private BigDecimal glueChangeMinutes = BigDecimal.ZERO;

    /**
     * 获取主胶料切换固定产能扣减。
     *
     * @return 非空扣减量
     */
    public BigDecimal getGlueChangeCapacityDeduct() {
        return glueChangeCapacityDeduct;
    }

    /**
     * 设置主胶料切换固定产能扣减。
     *
     * @param glueChangeCapacityDeduct 扣减量
     */
    public void setGlueChangeCapacityDeduct(BigDecimal glueChangeCapacityDeduct) {
        this.glueChangeCapacityDeduct = glueChangeCapacityDeduct;
    }

    /**
     * 获取规格切换分钟数。
     *
     * @return 非空分钟数
     */
    public BigDecimal getSpecChangeMinutes() {
        return specChangeMinutes;
    }

    /**
     * 设置规格切换分钟数。
     *
     * @param specChangeMinutes 分钟数
     */
    public void setSpecChangeMinutes(BigDecimal specChangeMinutes) {
        this.specChangeMinutes = specChangeMinutes;
    }

    /**
     * 获取胶料切换分钟数。
     *
     * @return 非空分钟数
     */
    public BigDecimal getGlueChangeMinutes() {
        return glueChangeMinutes;
    }

    /**
     * 设置胶料切换分钟数。
     *
     * @param glueChangeMinutes 分钟数
     */
    public void setGlueChangeMinutes(BigDecimal glueChangeMinutes) {
        this.glueChangeMinutes = glueChangeMinutes;
    }
}
