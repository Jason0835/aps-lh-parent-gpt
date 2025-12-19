package com.zlt.aps.monthplan.api.domain.entity;

import com.ruoyi.common.core.web.domain.BaseEntity;
import lombok.Data;
import com.ruoyi.common.core.annotation.Excel;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import com.zlt.common.annotation.EntityMapping;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.zlt.common.domain.CommonBusiEntity;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：MpAdjustStructureIn.java
 * 描    述：调整-结构内调整记录对象 t_mp_adjust_structure_in
 *@author zlt
 *@date 2025-12-19
 *@version 1.0
 *
 *  修改记录：
 *     修改时间：...
 *     修 改 人：zlt
 *     修改内容：...
 */

@ApiModel(value = "调整-结构内调整记录对象", description = "调整-结构内调整记录对象 ")
@Data
@TableName(value = "T_MP_ADJUST_STRUCTURE_IN")
public class MpAdjustStructureIn extends BaseEntity {

    private static final long serialVersionUID = 1L;

     /** 年份 */
    @Excel(name = "ui.data.column.mpAdjustStructureIn.year")
    @ApiModelProperty(value = "年份", name = "year")
    @TableField(value = "YEAR")
    private Integer year;

    /** 月份 */
    @Excel(name = "ui.data.column.mpAdjustStructureIn.month")
    @ApiModelProperty(value = "月份", name = "month")
    @TableField(value = "MONTH")
    private Integer month;

    /** 版本规则：ADJ+年月日+3位流水号； */
    @Excel(name = "ui.data.column.mpAdjustStructureIn.version")
    @ApiModelProperty(value = "版本规则：ADJ+年月日+3位流水号；", name = "version")
    @TableField(value = "VERSION")
    private String version;

    /** 产品结构 */
    @Excel(name = "ui.data.column.mpAdjustStructureIn.structureName")
    @ApiModelProperty(value = "产品结构", name = "structureName")
    @TableField(value = "STRUCTURE_NAME")
    private String structureName;

    /** 排产机台,多个机台用逗号分隔 */
    @Excel(name = "ui.data.column.mpAdjustStructureIn.scheduledMachines")
    @ApiModelProperty(value = "排产机台,多个机台用逗号分隔", name = "scheduledMachines")
    @TableField(value = "SCHEDULED_MACHINES")
    private String scheduledMachines;

    /** MES物料编码 */
    @Excel(name = "ui.data.column.mpAdjustStructureIn.mesMaterialCode")
    @ApiModelProperty(value = "MES物料编码", name = "mesMaterialCode")
    @TableField(value = "MES_MATERIAL_CODE")
    private String mesMaterialCode;

    /** 物料编码 */
    @Excel(name = "ui.data.column.mpAdjustStructureIn.materialCode")
    @ApiModelProperty(value = "物料编码", name = "materialCode")
    @TableField(value = "MATERIAL_CODE")
    private String materialCode;

    /** 物料描述 */
    @Excel(name = "ui.data.column.mpAdjustStructureIn.materialDesc")
    @ApiModelProperty(value = "物料描述", name = "materialDesc")
    @TableField(value = "MATERIAL_DESC")
    private String materialDesc;

    /** 是否含特殊材料            2、0-否，1-是 */
    @Excel(name = "ui.data.column.mpAdjustStructureIn.hasSpecialMaterial")
    @ApiModelProperty(value = "是否含特殊材料            2、0-否，1-是", name = "hasSpecialMaterial")
    @TableField(value = "HAS_SPECIAL_MATERIAL")
    private String hasSpecialMaterial;

    /** 调整前净需求量（上周） */
    @Excel(name = "ui.data.column.mpAdjustStructureIn.previousNetQty", readConverterExp = "上=周")
    @ApiModelProperty(value = "调整前净需求量", name = "previousNetQty")
    @TableField(value = "PREVIOUS_NET_QTY")
    private Integer previousNetQty;

    /** 当前净需求量 */
    @Excel(name = "ui.data.column.mpAdjustStructureIn.currentNetQty")
    @ApiModelProperty(value = "当前净需求量", name = "currentNetQty")
    @TableField(value = "CURRENT_NET_QTY")
    private Integer currentNetQty;

    /** 净需求变动 */
    @Excel(name = "ui.data.column.mpAdjustStructureIn.netQtyChange")
    @ApiModelProperty(value = "净需求变动", name = "netQtyChange")
    @TableField(value = "NET_QTY_CHANGE")
    private Integer netQtyChange;

    /** 月计划已排产量 */
    @Excel(name = "ui.data.column.mpAdjustStructureIn.monthScheduledQty")
    @ApiModelProperty(value = "月计划已排产量", name = "monthScheduledQty")
    @TableField(value = "MONTH_SCHEDULED_QTY")
    private Integer monthScheduledQty;

    /** 待调整量 */
    @Excel(name = "ui.data.column.mpAdjustStructureIn.pendingQty")
    @ApiModelProperty(value = "待调整量", name = "pendingQty")
    @TableField(value = "PENDING_QTY")
    private Integer pendingQty;

    /** 确认调整量 */
    @Excel(name = "ui.data.column.mpAdjustStructureIn.confirmAdjustQty")
    @ApiModelProperty(value = "确认调整量", name = "confirmAdjustQty")
    @TableField(value = "CONFIRM_ADJUST_QTY")
    private Integer confirmAdjustQty;

    /** 调整优先级            2、针对增量            2.1）在产SKU增量，先补；            2.2）新增SKU，按调整优先级1.2.3…            3、该列默认空，允许编辑； */
    @Excel(name = "ui.data.column.mpAdjustStructureIn.adjustPriority")
    @ApiModelProperty(value = "调整优先级            2、针对增量            2.1）在产SKU增量，先补；            2.2）新增SKU，按调整优先级1.2.3…            3、该列默认空，允许编辑；", name = "adjustPriority")
    @TableField(value = "ADJUST_PRIORITY")
    private Integer adjustPriority;

    /** 实际调整 */
    @Excel(name = "ui.data.column.mpAdjustStructureIn.actualAdjustQty")
    @ApiModelProperty(value = "实际调整", name = "actualAdjustQty")
    @TableField(value = "ACTUAL_ADJUST_QTY")
    private Integer actualAdjustQty;

    /** 调整原因 */
    @Excel(name = "ui.data.column.mpAdjustStructureIn.adjustReason")
    @ApiModelProperty(value = "调整原因", name = "adjustReason")
    @TableField(value = "ADJUST_REASON")
    private String adjustReason;


}