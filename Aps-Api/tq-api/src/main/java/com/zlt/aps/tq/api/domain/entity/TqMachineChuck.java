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
@TableName("T_TQ_MACHINE_CHUCK")
@ApiModel(value = "胎圈机台寸口对应对象", description = "胎圈机台寸口对应对象")
public class TqMachineChuck extends BaseEntity {
    private static final long serialVersionUID = 1L;

    @Excel(name = "ui.tq.machineChuck.column.machineCode")
    @ApiModelProperty(value = "机台编号", position = 20)
    @TableField("MACHINE_CODE")
    private String machineCode;

    @Excel(name = "ui.specifyMachine.column.machineName")
    @ApiModelProperty(value = "机台名称", position = 26)
    @TableField(exist = false)
    private String machineName;

    @Excel(name = "ui.tq.machineChuck.column.chuckCode")
    @ApiModelProperty(value = "寸口编码", position = 30)
    @TableField("CHUCK_CODE")
    @ImportValidated(required = true, isCode = true, maxLength = 50)
    private String chuckCode;

    @Excel(name = "ui.tq.machineChuck.column.chuckName")
    @ApiModelProperty(value = "寸口名称", position = 40)
    @TableField("CHUCK_NAME")
    @ImportValidated(maxLength = 100)
    private String chuckName;

    @Excel(name = "ui.tq.machineChuck.column.inchSize")
    @ApiModelProperty(value = "英寸尺寸", position = 50)
    @TableField("INCH_SIZE")
    private BigDecimal inchSize;

    @Excel(name = "ui.common.column.remark")
    @ApiModelProperty(value = "备注", position = 500)
    @TableField("REMARK")
    @ImportValidated(maxLength = 500)
    private String remark;
}
