package com.zlt.aps.mdm.api.domain.entity;

import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ruoyi.common.core.annotation.Excel;
import com.zlt.common.domain.CommonBusiEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：MdmMoldingMachineB.java
 * 描    述：基础数据-成型机子对象 t_mdm_molding_machine_b
 *@author zlt
 *@date 2025-02-18
 *@version 1.0
 *
 *  修改记录：
 *     修改时间：...
 *     修 改 人：zlt
 *     修改内容：...
 */

@ApiModel(value = "基础数据-成型机子对象", description = "基础数据-成型机子对象 ")
@Data
@TableName(value = "T_MDM_MOLDING_MACHINE_B")
@KeySequence(value = "SEQ_MOLDING_MACHINE_B")
public class MdmMoldingMachineB extends CommonBusiEntity{

    private static final long serialVersionUID = 1L;

     /** 成型机ID */
    @Excel(name = "ui.data.column.mdmMoldingMachineB.moldingMachineId")
    @ApiModelProperty(value = "", name = "moldingMachineId")
    @TableField(value = "MOLDING_MACHINE_ID")
    private Long moldingMachineId;

    /** 硫化线ID */
    @Excel(name = "ui.data.column.mdmMoldingMachineB.lineId")
    @ApiModelProperty(value = "硫化线ID", name = "lineId")
    @TableField(value = "LINE_ID")
    private Long lineId;


}