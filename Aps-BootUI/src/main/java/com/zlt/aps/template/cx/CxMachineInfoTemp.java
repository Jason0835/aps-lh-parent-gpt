package com.zlt.aps.template.cx;

import com.ruoyi.common.core.annotation.Excel;
import com.zlt.aps.common.core.annotation.ImportValidated;
import com.zlt.aps.common.core.domain.ApsBaseEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * 成型机台信息
 *
 * @ClassName CxMachineInfo
 * @Description 成型机台信息
 * @Author Joran.Zhang
 * @Date 2021-05-29 13:55
 * @Version 1.0
 **/
@Data
@ApiModel(value = "CxMachineInfo对象", description = "成型机台信息")
public class CxMachineInfoTemp extends ApsBaseEntity {


    @ApiModelProperty(value = "机台编号")
    @ImportValidated(required = true, isCode = true, maxLength = 30)
    @Excel(name = "ui.data.column.machine.machineCode",sort = 1)
    private String machineCode;

    @ApiModelProperty(value = "机台名称")
    @ImportValidated(required = true, maxLength = 20)
    @Excel(name = "ui.data.column.machine.machineName",sort = 5)
    private String machineName;

    @ApiModelProperty(value = "机台类别：VMI-256等")
    @Excel(name = "ui.data.column.machine.machineType" , dictType = "CX_MACHINE_TYPE", width = 32,sort = 10)
    private String type;

    @ApiModelProperty(value = "寸口范围下限")
    @ImportValidated(number = true, min = 0, max = 9999.99)
    @Excel(name = "ui.data.column.cx.machine.dimensionMiniMum",sort = 20)
    private Double dimensionMiniMum;

    @ApiModelProperty(value = "寸口范围上限")
    @ImportValidated(number = true, min = 0, max = 9999.99)
    @Excel(name = "ui.data.column.cx.machine.dimensionMaxiMum",sort = 25)
    private Double dimensionMaxiMum;

    @ApiModelProperty(value = "班制")
    @Excel(name = "ui.data.column.machine.classShift", dictType = "CLASS_SHIFT",sort = 35)
    private String classShift;

    @ApiModelProperty(value = "生产定额")
    @ImportValidated(digits = true, min = 0, max = 99999999, required = true)
    @Excel(name = "ui.data.column.machine.quata",sort = 30)
    private Long quata;

    @ApiModelProperty(value = "定额系数")
    @Excel(name = "ui.data.column.cx.machine.quotaRatio",sort = 8)
    @ImportValidated(number = true, min = 0, max = 9999.99)
    private Double quotaRatio;

    @ApiModelProperty(value = "操作人员数量")
    @ImportValidated(digits = true, min = 0, max = 999)
    @Excel(name = "ui.data.column.cx.machine.operatorQty",sort = 45)
    private Integer operatorQty;

    @ApiModelProperty(value = "机台状态，0--启用，1--禁用。对应数据字典STATUS")
    @ImportValidated(required = true)
    @Excel(name = "ui.data.column.machine.status", dictType = "STATUS",sort = 40)
    private String status;

    @ApiModelProperty(value = "机台类型:一次法；二次法")
    @Excel(name = "ui.data.column.cx.machine.type", dictType = "MACHINE_TYPE",sort = 15)
    private String machineType;

    @Excel(name = "ui.common.column.remark",sort = 50)
    @ImportValidated(maxLength = 300)
    private String remark;
}
