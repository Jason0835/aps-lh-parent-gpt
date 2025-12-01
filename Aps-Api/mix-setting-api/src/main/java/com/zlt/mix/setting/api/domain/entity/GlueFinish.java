package com.zlt.mix.setting.api.domain.entity;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.common.core.annotation.Excel;
import com.zlt.mix.common.core.annotation.ImportValidated;
import com.zlt.mix.common.core.domain.ZltBaseEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 胶料完成量信息对象 t_glue_finish
 *
 * @author Gim
 * @date 2022-03-29
 */
@ApiModel(value = "胶料完成量信息对象", description = "胶料完成量信息对象 ")
@TableName("t_glue_finish")
@KeySequence(value = "seq_t_glue_finish", dbType = DbType.ORACLE)
@Data
@EqualsAndHashCode(callSuper = true)
public class GlueFinish extends ZltBaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID，对应自增序列为：SEQ_T_GLUE_FINISH
     */
    @ApiModelProperty(value = "主键ID，对应自增序列为：SEQ_T_GLUE_FINISH", position = 10)
    private Long id;

    /**
     * 工单号
     */
    @Excel(name = "setting.glueFinish.orderNo")
    @ImportValidated(name = "setting.glueFinish.orderNo", isCode = true, maxLength = 30, required = true)
    @ApiModelProperty(value = "工单号", position = 40)
    private String orderNo;

    /**
     * 排程日期
     */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "setting.glueFinish.scheduleDate", width = 30, dateFormat = "yyyy-MM-dd")
    @ImportValidated(name = "setting.glueFinish.scheduleDate", required = true, date = true)
    @ApiModelProperty(value = "排程日期", position = 20)
    private Date scheduleDate;

    /**
     * 密炼区(对应数据字典code：MIX_AREA)
     */
    @Excel(name = "setting.glueFinish.mixArea", dictType = "MIX_AREA")
    @ImportValidated(name = "setting.glueFinish.mixArea", maxLength = 10, required = true)
    @ApiModelProperty(value = "密炼区(对应数据字典code：MIX_AREA)", position = 30)
    private String mixArea;
    /**
     * 胶料名称
     */
    @Excel(name = "setting.glueFinish.glue")
    @ImportValidated(name = "setting.glueFinish.glue", maxLength = 30, required = true)
    @ApiModelProperty(value = "胶料名称", position = 40)
    private String glue;
    /**
     * 机台编号
     */
    @ApiModelProperty(value = "机台编号", position = 50)
    private String machineCode;

    /**
     * 临时存储机台名称
     */
    @Excel(name = "setting.glueFinish.machineCode")
    @ImportValidated(name = "setting.glueFinish.machineCode", maxLength = 40, required = true)
    @TableField(exist = false)
    private String machineName;

    @ApiModelProperty(value = "总完成量(车)", position = 80)
    @Excel(name = "setting.glueFinish.totalFinishQty")
    private BigDecimal totalFinishQty;
    /**
     * 夜班完成量(车)
     */
    @Excel(name = "setting.glueFinish.nightFinishQty")
    @ImportValidated(name = "setting.glueFinish.nightFinishQty", digits = true, min = 0, max = 9999999)
    @ApiModelProperty(value = "夜班完成量(车)", position = 70)
    private BigDecimal midFinishQty;
    /**
     * 白班完成量(车)
     */
    @Excel(name = "setting.glueFinish.dayFinishQty")
    @ImportValidated(name = "setting.glueFinish.dayFinishQty", digits = true, min = 0, max = 9999999)
    @ApiModelProperty(value = "白班完成量(车)", position = 80)
    private BigDecimal nightFinishQty;
    /**
     * 白班完成量(车)
     */
    // @Excel(name = "setting.glueFinish.dayFinishQty")
    // @ImportValidated(name = "setting.glueFinish.dayFinishQty", digits = true, min = 0, max = 9999999)
    // @ApiModelProperty(value = "白班完成量(车)", position = 80)
    private BigDecimal dayFinishQty;

}
