package com.zlt.aps.template.gsq;

import com.ruoyi.common.core.annotation.Excel;
import com.zlt.aps.common.core.annotation.ImportValidated;
import com.zlt.aps.common.core.domain.ApsBaseEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.math.BigDecimal;

@Data
@ApiModel(value = "钢丝圈机台信息导入模板", description = "钢丝圈机台信息导入模板 ")
public class GsqMachineInfoTemp {

    @ApiModelProperty(value = "机台编号", position = 20)
    @Excel(name = "ui.data.column.machine.machineCode")
    private String machineCode;

    @ApiModelProperty(value = "机台名称", position = 30)
    @Excel(name = "ui.data.column.machine.machineName")
    private String machineName;

    @ApiModelProperty(value = "工装信息", position = 40)
    @Excel(name = "ui.data.column.machine.toolingInfo")
    private String toolingInfo;

    @ApiModelProperty(value = "生产定额", position = 75)
    @Excel(name = "ui.data.column.machine.quata")
    private BigDecimal quata;

    @ApiModelProperty(value = "班制", position = 80)
    @Excel(name = "ui.data.column.machine.classShift", dictType = "CLASS_SHIFT")
    private String classShift;

    @ApiModelProperty(value = "开机班次", position = 85)
    @Excel(name = "ui.data.column.machine.openMachineClass", dictType = "CLASS_NUM_THREE",dictTypeToExcelEnable = false)
    private String openMachineClass;

    @ApiModelProperty(value = "机台状态", position = 90)
    @Excel(name = "ui.data.column.machine.status", dictType = "STATUS")
    private String status;

    @Excel(name = "ui.common.column.remark")
    private String remark;
}
