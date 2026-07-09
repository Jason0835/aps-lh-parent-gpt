package com.zlt.aps.tc.api.domain.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ruoyi.common.core.annotation.Excel;
import com.ruoyi.common.core.web.domain.BaseEntity;
import com.zlt.aps.common.core.annotation.ImportValidated;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.math.BigDecimal;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：TcMachineInfo.java
 * 描    述：胎侧机台基础表 实体类
 *
 * @author zlt
 * @version 1.0
 * <p>
 * 修改记录：
 * 修改时间：...
 * 修 改 人：zlt
 * 修改内容：...
 * @date 2026-07-07
 */
@ApiModel(value = "胎侧机台基础表对象", description = "胎侧机台基础表对象")
@Data
@TableName(value = "T_TC_MACHINE_INFO")
public class TcMachineInfo extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @Excel(name = "ui.data.column.tc.machineInfo.factoryCode", dictType = "biz_factory_name")
    @ImportValidated(required = true, isCode = true, maxLength = 50)
    @ApiModelProperty(value = "工厂编号", name = "factoryCode")
    @TableField(value = "FACTORY_CODE")
    private String factoryCode;

    @Excel(name = "ui.data.column.tc.machineInfo.machineCode")
    @ImportValidated(isCode = true, maxLength = 30)
    @ApiModelProperty(value = "机台编码", name = "machineCode")
    @TableField(value = "MACHINE_CODE")
    private String machineCode;

    @Excel(name = "ui.data.column.tc.machineInfo.machineName")
    @ImportValidated(maxLength = 50)
    @ApiModelProperty(value = "机台名称", name = "machineName")
    @TableField(value = "MACHINE_NAME")
    private String machineName;

    @Excel(name = "ui.data.column.tc.machineInfo.maxCapacity")
    @ImportValidated(number = true, min = 0, max = 999999)
    @ApiModelProperty(value = "最大班产", name = "maxCapacity")
    @TableField(value = "MAX_CAPACITY")
    private BigDecimal maxCapacity;

    @Excel(name = "ui.data.column.tc.machineInfo.openShiftCode", dictType = "class_num_three_plan")
    @ImportValidated(maxLength = 20)
    @ApiModelProperty(value = "开机班次", name = "openShiftCode")
    @TableField(value = "OPEN_SHIFT_CODE")
    private String openShiftCode;

    @Excel(name = "ui.data.column.tc.machineInfo.machineStatus")
    @ImportValidated(maxLength = 50)
    @ApiModelProperty(value = "机台状态", name = "machineStatus")
    @TableField(value = "MACHINE_STATUS")
    private String machineStatus;

    @Excel(name = "ui.common.column.remark")
    @ImportValidated(maxLength = 500)
    @ApiModelProperty("备注")
    @TableField("REMARK")
    private String remark;
}
