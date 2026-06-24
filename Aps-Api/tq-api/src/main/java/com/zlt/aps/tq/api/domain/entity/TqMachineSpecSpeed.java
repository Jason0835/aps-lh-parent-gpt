package com.zlt.aps.tq.api.domain.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ruoyi.common.core.annotation.Excel;
import com.ruoyi.common.core.web.domain.BaseEntity;
import com.zlt.aps.common.core.annotation.ImportValidated;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = false)
@TableName("T_TQ_MACHINE_SPEC_SPEED")
@ApiModel(value = "胎圈机台生产速度对象", description = "胎圈机台生产速度对象")
public class TqMachineSpecSpeed extends BaseEntity {
    private static final long serialVersionUID = 1L;

    @Excel(name = "ui.tq.machineSpecSpeed.column.machineCode")
    @ApiModelProperty(value = "机台编号", position = 20)
    @TableField("MACHINE_CODE")
    @ImportValidated(required = true, isCode = true, maxLength = 50)
    private String machineCode;

    @Excel(name = "ui.tq.machineSpecSpeed.column.beadCode")
    @ApiModelProperty(value = "胎圈编码", position = 30)
    @TableField("BEAD_CODE")
    @ImportValidated(required = true, isCode = true, maxLength = 20)
    private String beadCode;

    @Excel(name = "ui.tq.machineSpecSpeed.column.standardSpeed")
    @ApiModelProperty(value = "标准生产速度（个/小时）", position = 40)
    @TableField("STANDARD_SPEED")
    @ImportValidated(required = true)
    private BigDecimal standardSpeed;

    @Excel(name = "ui.tq.machineSpecSpeed.column.quota")
    @ApiModelProperty(value = "生产定额", position = 50)
    @TableField("QUOTA")
    private Integer quota;

    @Excel(name = "ui.tq.machineSpecSpeed.column.quotaMes")
    @ApiModelProperty(value = "MES生产定额", position = 60)
    @TableField("QUOTA_MES")
    private Integer quotaMes;

    @Excel(name = "ui.common.column.remark")
    @ApiModelProperty(value = "备注", position = 500)
    @TableField("REMARK")
    @ImportValidated(maxLength = 500)
    private String remark;

    @Excel(name = "ui.specifyMachine.column.machineName")
    @ApiModelProperty(value = "机台名称", position = 80)
    @TableField(exist = false)
    private String machineName;
}
