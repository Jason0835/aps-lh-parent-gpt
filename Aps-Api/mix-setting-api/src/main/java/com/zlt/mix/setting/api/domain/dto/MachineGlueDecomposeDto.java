package com.zlt.mix.setting.api.domain.dto;

import com.ruoyi.common.core.annotation.Excel;
import com.zlt.mix.common.core.annotation.ImportValidated;
import com.zlt.mix.common.core.domain.ZltBaseEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 密炼机指定胶料分解对象dto t_machine_glue_decompose
 *
 * @author Liam
 * @date 2022-03-30
 */
@ApiModel(value = "密炼机指定胶料分解对象dto", description = "密炼机指定胶料分解对象dto ")
@Data
@EqualsAndHashCode(callSuper = true)
public class MachineGlueDecomposeDto extends ZltBaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID，对应自增序列为：SEQ_T_MACHINE_GLUE_DECOMPOSE
     */
    @ApiModelProperty(value = "主键ID，对应自增序列为：SEQ_T_MACHINE_GLUE_DECOMPOSE", position = 10)
    private Long id;
    /**
     * 密炼区(对应数据字典code：MIX_AREA)
     */
    @Excel(name = "setting.machineGlueDecompose.mixArea", dictType = "MIX_AREA")
    @ImportValidated(name = "setting.machineGlueDecompose.mixArea", maxLength = 10, required = true, isCode = true)
    @ApiModelProperty(value = "密炼区(对应数据字典code：MIX_AREA)", position = 20)
    private String mixArea;
    /**
     * 机台编号,导入不需要校验这个值,会从密炼机台信息中获取
     */
    @Excel(name = "setting.machineGlueDecompose.machineCode")
    @ApiModelProperty(value = "机台编号", position = 30)
    private String machineCode;
    /**
     * 机台名称
     */
    @Excel(name = "setting.machineGlueDecompose.machineName")
    @ImportValidated(name = "setting.machineGlueDecompose.machineName", maxLength = 40, required = true)
    @ApiModelProperty(value = "机台名称", position = 35)
    private String machineName;
    /**
     * 胶料名称
     */
    @Excel(name = "setting.machineGlueDecompose.glue")
    @ImportValidated(name = "setting.machineGlueDecompose.glue", maxLength = 30, required = true)
    @ApiModelProperty(value = "胶料名称", position = 40)
    private String glue;
    /**
     * 1段母胶
     */
    @Excel(name = "setting.machineGlueDecompose.motherGlue1")
    @ImportValidated(name = "setting.machineGlueDecompose.motherGlue1", maxLength = 30)
    @ApiModelProperty(value = "1段母胶", position = 50)
    private String motherGlue1;
    /**
     * 2段母胶
     */
    @Excel(name = "setting.machineGlueDecompose.motherGlue2")
    @ImportValidated(name = "setting.machineGlueDecompose.motherGlue2", maxLength = 30)
    @ApiModelProperty(value = "2段母胶", position = 60)
    private String motherGlue2;
    /**
     * 3段母胶
     */
    @Excel(name = "setting.machineGlueDecompose.motherGlue3")
    @ImportValidated(name = "setting.machineGlueDecompose.motherGlue3", maxLength = 30)
    @ApiModelProperty(value = "3段母胶", position = 70)
    private String motherGlue3;
    /**
     * 4段母胶
     */
    @Excel(name = "setting.machineGlueDecompose.motherGlue4")
    @ImportValidated(name = "setting.machineGlueDecompose.motherGlue4", maxLength = 30)
    @ApiModelProperty(value = "4段母胶", position = 80)
    private String motherGlue4;
    /**
     * 5段母胶
     */
    @Excel(name = "setting.machineGlueDecompose.motherGlue5")
    @ImportValidated(name = "setting.machineGlueDecompose.motherGlue5", maxLength = 30)
    @ApiModelProperty(value = "5段母胶", position = 90)
    private String motherGlue5;
    /**
     * 6段母胶
     */
    @Excel(name = "setting.machineGlueDecompose.motherGlue6")
    @ImportValidated(name = "setting.machineGlueDecompose.motherGlue6", maxLength = 30)
    @ApiModelProperty(value = "6段母胶", position = 100)
    private String motherGlue6;
    /**
     * 7段母胶
     */
    @Excel(name = "setting.machineGlueDecompose.motherGlue7")
    @ImportValidated(name = "setting.machineGlueDecompose.motherGlue7", maxLength = 30)
    @ApiModelProperty(value = "7段母胶", position = 110)
    private String motherGlue7;
    /**
     * 8段母胶
     */
    @Excel(name = "setting.machineGlueDecompose.motherGlue8")
    @ImportValidated(name = "setting.machineGlueDecompose.motherGlue8", maxLength = 30)
    @ApiModelProperty(value = "8段母胶", position = 120)
    private String motherGlue8;
    /**
     * 9段母胶
     */
    @Excel(name = "setting.machineGlueDecompose.motherGlue9")
    @ImportValidated(name = "setting.machineGlueDecompose.motherGlue9", maxLength = 30)
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
