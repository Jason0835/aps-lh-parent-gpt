package com.zlt.mix.schedule.engine.vo;

import lombok.Data;

/**
 * 胶料、密炼区、机台code VO
 */
@Data
public class GlueAreaMachineVo {

    public GlueAreaMachineVo(){}

    public GlueAreaMachineVo(String glue) {
        this.glue = glue;
    }

    public GlueAreaMachineVo(String mixArea, String glue) {
        this.glue = glue;
        this.mixArea = mixArea;
    }

    public GlueAreaMachineVo(String mixArea, String glue,  String machineCode) {
        this.glue = glue;
        this.mixArea = mixArea;
        this.machineCode = machineCode;
    }

    /**
     * 胶料
     */
    private String glue;

    /**
     * 密炼区
     */
    private String mixArea;

    /**
     * 机台code
     */
    private String machineCode;

    /**
     * 胶料的单车总重
     */
    private Double weight;
}
