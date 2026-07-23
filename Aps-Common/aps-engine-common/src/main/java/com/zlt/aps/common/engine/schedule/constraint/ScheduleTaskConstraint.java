package com.zlt.aps.common.engine.schedule.constraint;

import java.math.BigDecimal;

/**
 * 排程任务约束计算快照。
 *
 * <p>胎面使用胎面编码作为规格编码，胎侧使用胎侧编码作为规格编码。
 * 对象不依赖具体业务实体，可由自动排程和人工排程分别映射。</p>
 */
public class ScheduleTaskConstraint {

    /** 规格编码。 */
    private String specCode;

    /** 主胶料编码。 */
    private String glueCode;

    /** 当前任务在目标机台的生产速度。 */
    private BigDecimal machineSpeed;

    /**
     * 获取规格编码。
     *
     * @return 规格编码
     */
    public String getSpecCode() {
        return specCode;
    }

    /**
     * 设置规格编码。
     *
     * @param specCode 规格编码
     */
    public void setSpecCode(String specCode) {
        this.specCode = specCode;
    }

    /**
     * 获取主胶料编码。
     *
     * @return 主胶料编码
     */
    public String getGlueCode() {
        return glueCode;
    }

    /**
     * 设置主胶料编码。
     *
     * @param glueCode 主胶料编码
     */
    public void setGlueCode(String glueCode) {
        this.glueCode = glueCode;
    }

    /**
     * 获取机台速度。
     *
     * @return 机台速度
     */
    public BigDecimal getMachineSpeed() {
        return machineSpeed;
    }

    /**
     * 设置机台速度。
     *
     * @param machineSpeed 机台速度
     */
    public void setMachineSpeed(BigDecimal machineSpeed) {
        this.machineSpeed = machineSpeed;
    }
}
