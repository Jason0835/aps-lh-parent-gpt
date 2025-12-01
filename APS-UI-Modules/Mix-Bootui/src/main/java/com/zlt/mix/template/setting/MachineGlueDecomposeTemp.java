package com.zlt.mix.template.setting;

import com.ruoyi.common.core.annotation.Excel;
import com.zlt.mix.common.core.annotation.ImportValidated;
import com.zlt.mix.common.core.domain.ZltBaseEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 密炼机指定胶料分解对象Temp t_machine_glue_decompose
 * 只作为导入的专用模板,不作为导出的Excel
 *
 * @author Liam
 * @date 2022-03-30
 */
@ApiModel(value = "密炼机指定胶料分解对象Temp", description = "密炼机指定胶料分解对象Temp")
@Data
@EqualsAndHashCode(callSuper = true)
public class MachineGlueDecomposeTemp extends ZltBaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 密炼区(对应数据字典code：MIX_AREA)
     */
    @Excel(name = "setting.machineGlueDecompose.mixArea", dictType = "MIX_AREA")
    @ImportValidated(name = "setting.machineGlueDecompose.mixArea", maxLength = 10, required = true, isCode = true)
    @ApiModelProperty(value = "密炼区(对应数据字典code：MIX_AREA)", position = 20)
    private String mixArea;
    /**
     * 机台名称
     */
    @Excel(name = "setting.machineGlueDecompose.machineName")
    @ImportValidated(name = "setting.machineGlueDecompose.machineName", maxLength = 30, isCode = true)
    @ApiModelProperty(value = "机台名称", position = 35)
    private String machineName;
    /**
     * 胶料名称
     */
    @Excel(name = "setting.machineGlueDecompose.glue")
    @ImportValidated(name = "setting.machineGlueDecompose.glue", maxLength = 30, required = true, isCode = true)
    @ApiModelProperty(value = "胶料名称", position = 40)
    private String glue;
    /**
     * 1段母胶
     */
    @Excel(name = "setting.machineGlueDecompose.motherGlue1")
    @ImportValidated(name = "setting.machineGlueDecompose.motherGlue1", maxLength = 30, isCode = true)
    @ApiModelProperty(value = "1段母胶", position = 50)
    private String motherGlue1;
    /**
     * 2段母胶
     */
    @Excel(name = "setting.machineGlueDecompose.motherGlue2")
    @ImportValidated(name = "setting.machineGlueDecompose.motherGlue2", maxLength = 30, isCode = true)
    @ApiModelProperty(value = "2段母胶", position = 60)
    private String motherGlue2;
    /**
     * 3段母胶
     */
    @Excel(name = "setting.machineGlueDecompose.motherGlue3")
    @ImportValidated(name = "setting.machineGlueDecompose.motherGlue3", maxLength = 30, isCode = true)
    @ApiModelProperty(value = "3段母胶", position = 70)
    private String motherGlue3;
    /**
     * 4段母胶
     */
    @Excel(name = "setting.machineGlueDecompose.motherGlue4")
    @ImportValidated(name = "setting.machineGlueDecompose.motherGlue4", maxLength = 30, isCode = true)
    @ApiModelProperty(value = "4段母胶", position = 80)
    private String motherGlue4;
    /**
     * 5段母胶
     */
    @Excel(name = "setting.machineGlueDecompose.motherGlue5")
    @ImportValidated(name = "setting.machineGlueDecompose.motherGlue5", maxLength = 30, isCode = true)
    @ApiModelProperty(value = "5段母胶", position = 90)
    private String motherGlue5;
    /**
     * 6段母胶
     */
    @Excel(name = "setting.machineGlueDecompose.motherGlue6")
    @ImportValidated(name = "setting.machineGlueDecompose.motherGlue6", maxLength = 30, isCode = true)
    @ApiModelProperty(value = "6段母胶", position = 100)
    private String motherGlue6;
    /**
     * 7段母胶
     */
    @Excel(name = "setting.machineGlueDecompose.motherGlue7")
    @ImportValidated(name = "setting.machineGlueDecompose.motherGlue7", maxLength = 30, isCode = true)
    @ApiModelProperty(value = "7段母胶", position = 110)
    private String motherGlue7;
    /**
     * 8段母胶
     */
    @Excel(name = "setting.machineGlueDecompose.motherGlue8")
    @ImportValidated(name = "setting.machineGlueDecompose.motherGlue8", maxLength = 30, isCode = true)
    @ApiModelProperty(value = "8段母胶", position = 120)
    private String motherGlue8;
    /**
     * 9段母胶
     */
    @Excel(name = "setting.machineGlueDecompose.motherGlue9")
    @ImportValidated(name = "setting.machineGlueDecompose.motherGlue9", maxLength = 30, isCode = true)
    @ApiModelProperty(value = "9段母胶", position = 130)
    private String motherGlue9;
    /**
     * 备注
     */
    @Excel(name = "setting.machineGlueDecompose.remark")
    @ImportValidated(name = "setting.machineGlueDecompose.remark", maxLength = 300)
    @ApiModelProperty(value = "备注", position = 140)
    private String remark;

}
