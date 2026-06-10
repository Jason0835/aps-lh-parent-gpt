package com.zlt.aps.tm.api.domain.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ruoyi.common.core.annotation.Excel;
import com.ruoyi.common.core.web.domain.BaseEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.math.BigDecimal;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：TmMachineInfo.java
 * 描    述：胎面机台基础表 实体类
 *
 * @author zlt
 * @version 1.0
 * <p>
 * 修改记录：
 * 修改时间：...
 * 修 改 人：zlt
 * 修改内容：...
 * @date 2025-12-12
 */
@ApiModel(value = "胎面机台基础表对象", description = "胎面机台基础表对象")
@Data
@TableName(value = "T_TM_MACHINE_INFO")
public class TmMachineInfo extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @Excel(name = "ui.data.column.tm.MachineInfo.factoryCode")
    @ApiModelProperty(value = "工厂编号", name = "factoryCode")
    @TableField(value = "FACTORY_CODE")
    private String factoryCode;

    @Excel(name = "ui.data.column.tm.MachineInfo.machineCode")
    @ApiModelProperty(value = "机台编码", name = "machineCode")
    @TableField(value = "MACHINE_CODE")
    private String machineCode;

    @Excel(name = "ui.data.column.tm.MachineInfo.machineName")
    @ApiModelProperty(value = "机台名称", name = "machineName")
    @TableField(value = "MACHINE_NAME")
    private String machineName;

    @Excel(name = "ui.data.column.tm.MachineInfo.maxCapacity")
    @ApiModelProperty(value = "最大班产", name = "maxCapacity")
    @TableField(value = "MAX_CAPACITY")
    private BigDecimal maxCapacity;

    @Excel(name = "ui.data.column.tm.MachineInfo.openShiftCode")
    @ApiModelProperty(value = "开放班次编码", name = "openShiftCode")
    @TableField(value = "OPEN_SHIFT_CODE")
    private String openShiftCode;

    @Excel(name = "ui.data.column.tm.MachineInfo.machineStatus")
    @ApiModelProperty(value = "机台状态", name = "machineStatus")
    @TableField(value = "MACHINE_STATUS")
    private String machineStatus;

    @Excel(name = "ui.data.column.tm.MachineInfo.shiftCode")
    @ApiModelProperty(value = "班次编码", name = "shiftCode")
    @TableField(value = "SHIFT_CODE")
    private String shiftCode;
}
