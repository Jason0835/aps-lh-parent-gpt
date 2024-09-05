package com.zlt.aps.template.cd90;

import com.ruoyi.common.core.annotation.Excel;
import com.zlt.aps.common.core.annotation.ImportValidated;
import com.zlt.aps.common.core.domain.ApsBaseEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.math.BigDecimal;

@Data
@ApiModel(value = "90°裁断机台信息对象", description = "90°裁断机台信息对象 ")
public class Cd90MachineInfoTemp {

    /**
     * 机台编号
     */
    @ApiModelProperty(value = "机台编号", position = 20)
    @Excel(name = "ui.data.column.machine.machineCode")
    private String machineCode;

    @ApiModelProperty(value = "机台名称", position = 30)
    @Excel(name = "ui.data.column.machine.machineName")
    private String machineName;

    @ApiModelProperty(value = "是否支持贴胶片", position = 40)
    @Excel(name = "ui.data.column.machine.isStickFilm", dictType = "IS_SUPPORTED")
    private String isStickFilm;

    @ApiModelProperty(value = "帘布宽度（上限）", position = 75)
    @Excel(name = "ui.data.column.machine.clothWithMax")
    private BigDecimal clothWithMax;

    @ApiModelProperty(value = "帘布宽度（下限）", position = 75)
    @Excel(name = "ui.data.column.machine.clothWithMin")
    private BigDecimal clothWithMin;

    /**
     * 生产定额，是指单班一次能生产的量，单位：吨/班
     */
    @ApiModelProperty(value = "生产定额", position = 75)
    @Excel(name = "ui.data.column.machine.quata")
    private BigDecimal quata;

    /**
     * 班制，如：三班制，两班制；对应数据字典CLASS_SHIFT
     */
    @ApiModelProperty(value = "班制", position = 80)
    @Excel(name = "ui.data.column.machine.classShift", dictType = "CLASS_SHIFT")
    private String classShift;

    /**
     * 开机班次，如：中班、夜班；对应数据字典CLASS_NUM
     */
    @ApiModelProperty(value = "开机班次", position = 85)
    @Excel(name = "ui.data.column.machine.openMachineClass", dictType = "CLASS_NUM_THREE",dictTypeToExcelEnable = false)
    private String openMachineClass;

    @ApiModelProperty(value = "机台状态", position = 90)
    @Excel(name = "ui.data.column.machine.status", dictType = "STATUS")
    private String status;

    @Excel(name = "ui.common.column.remark")
    private String remark;
}
