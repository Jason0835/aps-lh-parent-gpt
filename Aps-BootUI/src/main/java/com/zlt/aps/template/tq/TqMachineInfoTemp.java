package com.zlt.aps.template.tq;

import com.ruoyi.common.core.annotation.Excel;
import com.zlt.aps.common.core.annotation.ImportValidated;
import com.zlt.aps.common.core.domain.ApsBaseEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.math.BigDecimal;

@Data
@ApiModel(value = "胎圈机台信息导入模板", description = "胎圈机台信息导入模板")
public class TqMachineInfoTemp extends ApsBaseEntity {

    /** 机台编号 */
    @ApiModelProperty(value = "机台编号", position =20)
    @Excel(name = "ui.data.column.machine.machineCode")
    private String machineCode;

    /** 机台名称，比如：1线、2线 */
    @ApiModelProperty(value = "机台名称", position =30)
    @Excel(name = "ui.data.column.machine.machineName")
    private String machineName;

    /** 效率（每分钟） */
    @Excel(name = "ui.data.column.machine.efficiency")
    @ApiModelProperty(value = "效率", position =40)
    private Double efficiency;

    /** 工装信息。 */
    @Excel(name = "ui.data.column.machine.toolingInfo")
    @ApiModelProperty(value = "工装信息", position =50)
    private String toolingInfo;

    /** 生产定额，是指单班一次能生产的量，单位：吨/班 */
    @ApiModelProperty(value = "生产定额", position =60)
    @Excel(name = "ui.data.column.machine.quata")
    private BigDecimal quata;

    /** 班制，如：三班制，两班制；对应数据字典CLASS_SHIFT */
    @ApiModelProperty(value = "班制", position =80)
    @Excel(name = "ui.data.column.machine.classShift",dictType="CLASS_SHIFT")
    private String classShift;

    /** 开机班次，如：中班、夜班；对应数据字典CLASS_NUM */
    @ApiModelProperty(value = "开机班次", position =85)
    @Excel(name = "ui.data.column.machine.openMachineClass",dictType = "CLASS_NUM_THREE",dictTypeToExcelEnable = false)
    private String openMachineClass;

    /** 机台状态，0--启用，1--禁用。对应数据字典STATUS */
    @ApiModelProperty(value = "机台状态", position =90)
    @Excel(name = "ui.data.column.machine.status",dictType="STATUS")
    private String status;

    @Excel(name = "ui.common.column.remark")
    private String remark;
}
