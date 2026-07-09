package com.zlt.aps.template.tq;

import com.ruoyi.common.core.annotation.Excel;
import com.zlt.aps.common.core.annotation.ImportValidated;
import com.zlt.aps.common.core.domain.ApsBaseEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
@ApiModel(value = "胎圈机台信息导入模板", description = "胎圈机台信息导入模板")
public class TqMachineInfoTemp extends ApsBaseEntity {

    /** 机台编号 */
    @ApiModelProperty(value = "机台编号", position = 20)
    @Excel(name = "ui.data.column.machine.machineCode")
    @ImportValidated(required = true, isCode = true, maxLength = 30)
    private String machineCode;

    /** 机台名称，比如：1线、2线 */
    @ApiModelProperty(value = "机台名称", position = 30)
    @Excel(name = "ui.data.column.machine.machineName")
    @ImportValidated(required = true, maxLength = 60)
    private String machineName;

    /** 班制，如：三班制，两班制；对应数据字典CLASS_SHIFT */
    @ApiModelProperty(value = "班制", position = 40)
    @Excel(name = "ui.data.column.machine.classShift", dictType = "CLASS_SHIFT")
    @ImportValidated(maxLength = 20, required = true)
    private String classShift;

    /** 开机班次，如：中班、夜班；对应数据字典CLASS_NUM */
    @ApiModelProperty(value = "开机班次", position = 50)
    @Excel(name = "ui.data.column.machine.openMachineClass", dictType = "CLASS_NUM_THREE", dictTypeToExcelEnable = false)
    @ImportValidated(maxLength = 20)
    private String openMachineClass;

    /** 机台状态，0--启用，1--禁用。对应数据字典STATUS */
    @ApiModelProperty(value = "机台状态", position = 60)
    @Excel(name = "ui.data.column.machine.status", dictType = "STATUS")
    @ImportValidated(maxLength = 1, required = true)
    private String status;

    @ApiModelProperty(value = "备注", position = 70)
    @Excel(name = "ui.common.column.remark")
    @ImportValidated(maxLength = 900)
    private String remark;

    /** 定额：该机台单班标准产量 */
    @ApiModelProperty(value = "定额：该机台单班标准产量", position = 80)
    @Excel(name = "ui.data.column.machine.quata")
    @ImportValidated(number = true, min = 0, max = 999999)
    private Double quota;
}
