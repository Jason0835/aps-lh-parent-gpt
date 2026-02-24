package com.zlt.aps.mp.engine.domain.dto;

import java.io.Serializable;

/**
 * 机台数量对象
 *
 * @author ZLT
 * @date 20250219
 */
public class MachineCountDto implements Serializable {

    /**
     * 成型机机台数量
     */
    private Integer formingMachineCount;
    /**
     * 硫化机机台数量
     */
    private Integer vulcanizationMachineCount;

    /**
     * 构建分厂机台对象信息
     *
     * @param formingMachineCount       成型机机台数
     * @param vulcanizationMachineCount 硫化机机台数
     */
    public MachineCountDto(Integer formingMachineCount, Integer vulcanizationMachineCount) {
        this.formingMachineCount = formingMachineCount;
        this.vulcanizationMachineCount = vulcanizationMachineCount;
    }

    /**
     * 成型机机台数
     *
     * @return
     */
    public Integer getFormingMachineCount() {
        return formingMachineCount;
    }

    /**
     * 硫化机机台数
     *
     * @return
     */
    public Integer getVulcanizationMachineCount() {
        return vulcanizationMachineCount;
    }
}
