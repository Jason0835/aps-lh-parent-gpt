package com.zlt.mix.setting.api.domain.entity;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ruoyi.common.core.annotation.Excel;
import com.zlt.mix.common.core.annotation.ImportValidated;
import com.zlt.mix.common.core.domain.ZltBaseEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 终炼母炼分解对象 t_glue_decompose
 *
 * @author Liam
 * @date 2022-03-28
 */
@ApiModel(value = "终炼母炼分解对象", description = "终炼母炼分解对象 ")
@TableName("t_glue_decompose")
@KeySequence(value = "seq_t_glue_decompose", dbType = DbType.ORACLE)
@Data
@EqualsAndHashCode(callSuper = true)
public class GlueDecompose extends ZltBaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID，对应自增序列为：SEQ_T_GLUE_DECOMPOSE
     */
    @ApiModelProperty(value = "主键ID，对应自增序列为：SEQ_T_GLUE_DECOMPOSE", position = 10)
    private Long id;
    /**
     * 胶料名称
     */
    @Excel(name = "setting.decompose.glue")
    @ImportValidated(name = "setting.decompose.glue", maxLength = 30, required = true)
    @ApiModelProperty(value = "胶料名称", position = 20)
    private String glue;
    /**
     * 段数(对应数据字典：SEGMENT；1：1段，2：2段，3：3段，10:10段)
     */
    @Excel(name = "setting.decompose.segment", dictType = "SEGMENT")
    @ImportValidated(name = "setting.decompose.segment", number = true, min = 0, max = 99999, required = true)
    @ApiModelProperty(value = "段数(对应数据字典：SEGMENT；1：1段，2：2段，3：3段，10:10段)", position = 30)
    private Integer segment;
    /**
     * 1段母胶
     */
    @Excel(name = "setting.decompose.motherGlue1")
    @ImportValidated(name = "setting.decompose.motherGlue1", maxLength = 30)
    @ApiModelProperty(value = "1段母胶", position = 40)
    private String motherGlue1;
    /**
     * 2段母胶
     */
    @Excel(name = "setting.decompose.motherGlue2")
    @ImportValidated(name = "setting.decompose.motherGlue2", maxLength = 30)
    @ApiModelProperty(value = "2段母胶", position = 50)
    private String motherGlue2;
    /**
     * 3段母胶
     */
    @Excel(name = "setting.decompose.motherGlue3")
    @ImportValidated(name = "setting.decompose.motherGlue3", maxLength = 30)
    @ApiModelProperty(value = "3段母胶", position = 60)
    private String motherGlue3;
    /**
     * 4段母胶
     */
    @Excel(name = "setting.decompose.motherGlue4")
    @ImportValidated(name = "setting.decompose.motherGlue4", maxLength = 30)
    @ApiModelProperty(value = "4段母胶", position = 70)
    private String motherGlue4;
    /**
     * 5段母胶
     */
    @Excel(name = "setting.decompose.motherGlue5")
    @ImportValidated(name = "setting.decompose.motherGlue5", maxLength = 30)
    @ApiModelProperty(value = "5段母胶", position = 80)
    private String motherGlue5;
    /**
     * 6段母胶
     */
    @Excel(name = "setting.decompose.motherGlue6")
    @ImportValidated(name = "setting.decompose.motherGlue6", maxLength = 30)
    @ApiModelProperty(value = "6段母胶", position = 90)
    private String motherGlue6;
    /**
     * 7段母胶
     */
    @Excel(name = "setting.decompose.motherGlue7")
    @ImportValidated(name = "setting.decompose.motherGlue7", maxLength = 30)
    @ApiModelProperty(value = "7段母胶", position = 100)
    private String motherGlue7;
    /**
     * 8段母胶
     */
    @Excel(name = "setting.decompose.motherGlue8")
    @ImportValidated(name = "setting.decompose.motherGlue8", maxLength = 30)
    @ApiModelProperty(value = "8段母胶", position = 110)
    private String motherGlue8;
    /**
     * 9段母胶
     */
    @Excel(name = "setting.decompose.motherGlue9")
    @ImportValidated(name = "setting.decompose.motherGlue9", maxLength = 30)
    @ApiModelProperty(value = "9段母胶", position = 120)
    private String motherGlue9;
    /**
     * 备注
     */
    @Excel(name = "setting.decompose.remark")
    @ImportValidated(name = "setting.decompose.remark", maxLength = 300)
    @ApiModelProperty(value = "备注", position = 130)
    private String remark;

}
