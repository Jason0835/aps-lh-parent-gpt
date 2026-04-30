package com.zlt.aps.template.tq;

import com.ruoyi.common.core.annotation.Excel;
import com.zlt.aps.common.core.domain.ApsBaseEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.math.BigDecimal;

@Data
@ApiModel(value = "胎圈机台生产速度导入模板", description = "胎圈机台生产速度导入模板")
public class TqMachineSpecSpeedTemp extends ApsBaseEntity {

    @Excel(name = "ui.specifyMachine.column.machineName")
    @ApiModelProperty(value = "机台名称")
    private String machineName;

    @Excel(name = "ui.tq.machineSpecSpeed.column.materialCode")
    @ApiModelProperty(value = "胎圈编码")
    private String materialCode;

    @Excel(name = "ui.tq.machineSpecSpeed.column.standardSpeed")
    @ApiModelProperty(value = "标准生产速度（个/小时）")
    private BigDecimal standardSpeed;

    @Excel(name = "ui.tq.machineSpecSpeed.column.quota")
    @ApiModelProperty(value = "生产定额")
    private Integer quota;

    @Excel(name = "ui.tq.machineSpecSpeed.column.quotaMes")
    @ApiModelProperty(value = "MES生产定额")
    private Integer quotaMes;

    @ApiModelProperty(value = "备注", position = 500)
    @Excel(name = "ui.common.column.remark")
    private String remark;
}
