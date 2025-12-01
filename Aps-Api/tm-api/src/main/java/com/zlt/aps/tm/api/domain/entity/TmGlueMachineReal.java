package com.zlt.aps.tm.api.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ruoyi.common.core.annotation.Excel;
import com.ruoyi.common.core.web.domain.BaseEntity;
import com.zlt.aps.common.core.annotation.ImportValidated;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * <p>
 * 胎面卷曲信息维护表
 * </p>
 *
 * @author zlt
 * @since 2023-09-04
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("T_TM_GLUE_MACHINE_REAL")
@ApiModel(value = "胎面胶料机台关系对象", description = "胎面胶料机台关系对象")
public class TmGlueMachineReal extends BaseEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "主键ID")
    @TableId(value = "ID", type = IdType.AUTO)
    private Long id;

    @ApiModelProperty(value = "胶料代码")
    @TableField("GLUE_CODE")
    @Excel(name = "ui.glueMachine.column.glueCode")
    @ImportValidated(required = true, maxLength = 20)
    private String glueCode;

    @ApiModelProperty(value = "机台ID")
    @TableField(value = "MACHINE_ID")
    private Long machineId;

    @ApiModelProperty(value = "机台名称")
    @TableField(value = "MACHINE_NAME")
    @Excel(name = "ui.glueMachine.column.machineName")
    @ImportValidated(required = true)
    private String machineName;

    @ApiModelProperty(value = "机台班次")
    @TableField(value = "MACHINE_CLASS")
    @Excel(name = "ui.glueMachine.column.machineClass", dictType = "CLASS_NUM")
    private String machineClass;
}
