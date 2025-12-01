package com.zlt.mix.schedule.engine.vo;

import com.ruoyi.common.core.annotation.Excel;
import com.zlt.mix.common.core.annotation.ImportValidated;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * 胶料、密炼区、机台code VO
 */
@Data
public class MaterialAreaMachineVo {

    public MaterialAreaMachineVo(){}

    public MaterialAreaMachineVo(String materialName) {
        this.materialName = materialName;
    }

    public MaterialAreaMachineVo(String mixArea, String materialCode, String materialName) {
        this.materialCode = materialCode;
        this.materialName = materialName;
        this.mixArea = mixArea;
    }

    public MaterialAreaMachineVo(String mixArea, String materialCode, String materialName, String machineCode) {
        this.materialCode = materialCode;
        this.materialName = materialName;
        this.mixArea = mixArea;
        this.machineCode = machineCode;
    }

    /**
     * 物料编号
     */
    private String materialCode;

    /**
     * 物料名称
     */
    private String materialName;

    /**
     * 密炼区
     */
    private String mixArea;

    /**
     * 机台code
     */
    private String machineCode;

    /**
     * 中班状态
     */
    private String midStatus;

    /**
     * 夜班状态
     */
    private String nightStatus;

    /**
     * 白班状态
     */
    private String dayStatus;
}
