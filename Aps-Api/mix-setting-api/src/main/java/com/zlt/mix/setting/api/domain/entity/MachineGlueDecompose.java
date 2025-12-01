package com.zlt.mix.setting.api.domain.entity;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableName;
import com.zlt.mix.common.core.domain.ZltBaseEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 密炼机指定胶料分解对象 t_machine_glue_decompose
 *
 * @author Liam
 * @date 2022-03-29
 */
@ApiModel(value = "密炼机指定胶料分解对象", description = "密炼机指定胶料分解对象 ")
@TableName("t_machine_glue_decompose")
@KeySequence(value = "seq_t_machine_glue_decompose", dbType = DbType.ORACLE)
@Data
@EqualsAndHashCode(callSuper = true)
public class MachineGlueDecompose extends ZltBaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID，对应自增序列为：SEQ_T_MACHINE_GLUE_DECOMPOSE
     */
    @ApiModelProperty(value = "主键ID，对应自增序列为：SEQ_T_MACHINE_GLUE_DECOMPOSE", position = 10)
    private Long id;
    /**
     * 密炼区(对应数据字典code：MIX_AREA)
     */
    @ApiModelProperty(value = "密炼区(对应数据字典code：MIX_AREA)", position = 20)
    private String mixArea;
    /**
     * 机台编号
     */
    @ApiModelProperty(value = "机台编号", position = 30)
    private String machineCode;
    /**
     * 胶料名称
     */
    @ApiModelProperty(value = "胶料名称", position = 40)
    private String glue;
    /**
     * 1段母胶
     */
    @ApiModelProperty(value = "1段母胶", position = 50)
    private String motherGlue1;
    /**
     * 2段母胶
     */
    @ApiModelProperty(value = "2段母胶", position = 60)
    private String motherGlue2;
    /**
     * 3段母胶
     */
    @ApiModelProperty(value = "3段母胶", position = 70)
    private String motherGlue3;
    /**
     * 4段母胶
     */
    @ApiModelProperty(value = "4段母胶", position = 80)
    private String motherGlue4;
    /**
     * 5段母胶
     */
    @ApiModelProperty(value = "5段母胶", position = 90)
    private String motherGlue5;
    /**
     * 6段母胶
     */
    @ApiModelProperty(value = "6段母胶", position = 100)
    private String motherGlue6;
    /**
     * 7段母胶
     */
    @ApiModelProperty(value = "7段母胶", position = 110)
    private String motherGlue7;
    /**
     * 8段母胶
     */
    @ApiModelProperty(value = "8段母胶", position = 120)
    private String motherGlue8;
    /**
     * 9段母胶
     */
    @ApiModelProperty(value = "9段母胶", position = 130)
    private String motherGlue9;
    /**
     * 备注
     */
    @ApiModelProperty(value = "备注", position = 140)
    private String remark;

}
