package com.zlt.aps.monthplan.api.domain.entity;

import com.ruoyi.common.core.web.domain.BaseEntity;
import lombok.Data;
import com.ruoyi.common.core.annotation.Excel;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.EqualsAndHashCode;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：MpAdjustStructureLog.java
 * 描    述：调整-操作日志对象 t_mp_adjust_structure_log
 *@author zlt
 *@date 2025-12-19
 *@version 1.0
 *
 *  修改记录：
 *     修改时间：...
 *     修 改 人：zlt
 *     修改内容：...
 */

@EqualsAndHashCode(callSuper = false)
@ApiModel(value = "调整-操作日志对象", description = "调整-操作日志对象 ")
@Data
@TableName(value = "T_MP_ADJUST_STRUCTURE_LOG")
public class MpAdjustStructureLog extends BaseEntity {

    private static final long serialVersionUID = 1L;

     /** 产品结构 */
    @Excel(name = "ui.data.column.mpAdjustStructureLog.structureName")
    @ApiModelProperty(value = "产品结构", name = "structureName")
    @TableField(value = "STRUCTURE_NAME")
    private String structureName;

    /** 排产机台,多个机台用逗号分隔 */
    @Excel(name = "ui.data.column.mpAdjustStructureLog.scheduledMachines")
    @ApiModelProperty(value = "排产机台,多个机台用逗号分隔", name = "scheduledMachines")
    @TableField(value = "SCHEDULED_MACHINES")
    private String scheduledMachines;

    /** MES物料编码 */
    @Excel(name = "ui.data.column.mpAdjustStructureLog.mesMaterialCode")
    @ApiModelProperty(value = "MES物料编码", name = "mesMaterialCode")
    @TableField(value = "MES_MATERIAL_CODE")
    private String mesMaterialCode;

    /** 物料编码 */
    @Excel(name = "ui.data.column.mpAdjustStructureLog.materialCode")
    @ApiModelProperty(value = "物料编码", name = "materialCode")
    @TableField(value = "MATERIAL_CODE")
    private String materialCode;

    /** 物料描述 */
    @Excel(name = "ui.data.column.mpAdjustStructureLog.materialDesc")
    @ApiModelProperty(value = "物料描述", name = "materialDesc")
    @TableField(value = "MATERIAL_DESC")
    private String materialDesc;

    /** 是否含特殊材料            2、0-否，1-是 */
    @Excel(name = "ui.data.column.mpAdjustStructureLog.hasSpecialMaterial")
    @ApiModelProperty(value = "是否含特殊材料            2、0-否，1-是", name = "hasSpecialMaterial")
    @TableField(value = "HAS_SPECIAL_MATERIAL")
    private String hasSpecialMaterial;

    /** 调整前净需求量（上周） */
    @Excel(name = "ui.data.column.mpAdjustStructureLog.previousNetQty", readConverterExp = "上=周")
    @ApiModelProperty(value = "调整前净需求量", name = "previousNetQty")
    @TableField(value = "PREVIOUS_NET_QTY")
    private Integer previousNetQty;

    /** 净需求变动-操作前 */
    @Excel(name = "ui.data.column.mpAdjustStructureLog.beforeNetQtyChange")
    @ApiModelProperty(value = "净需求变动-操作前", name = "beforeNetQtyChange")
    @TableField(value = "BEFORE_NET_QTY_CHANGE")
    private Integer beforeNetQtyChange;

    /** 月计划已排产量-操作前 */
    @Excel(name = "ui.data.column.mpAdjustStructureLog.beforeMonthScheduledQty")
    @ApiModelProperty(value = "月计划已排产量-操作前", name = "beforeMonthScheduledQty")
    @TableField(value = "BEFORE_MONTH_SCHEDULED_QTY")
    private Integer beforeMonthScheduledQty;

    /** 待调整量-操作前 */
    @Excel(name = "ui.data.column.mpAdjustStructureLog.beforePendingQty")
    @ApiModelProperty(value = "待调整量-操作前", name = "beforePendingQty")
    @TableField(value = "BEFORE_PENDING_QTY")
    private Integer beforePendingQty;

    /** 确认调整量-操作前 */
    @Excel(name = "ui.data.column.mpAdjustStructureLog.beforeConfirmAdjustQty")
    @ApiModelProperty(value = "确认调整量-操作前", name = "beforeConfirmAdjustQty")
    @TableField(value = "BEFORE_CONFIRM_ADJUST_QTY")
    private Integer beforeConfirmAdjustQty;

    /**
     * 调整优先级-操作前
     * 2、针对增量
     * 2.1）在产SKU增量，先补；
     * 2.2）新增SKU，按调整优先级1.2.3…
     * 3、该列默认空，允许编辑；
     */
    @Excel(name = "ui.data.column.mpAdjustStructureLog.beforeAdjustPriority")
    @ApiModelProperty(value = "调整优先级-操作前", name = "beforeAdjustPriority")
    @TableField(value = "BEFORE_ADJUST_PRIORITY")
    private Integer beforeAdjustPriority;

    /** 实际调整-操作前 */
    @Excel(name = "ui.data.column.mpAdjustStructureLog.beforeActualAdjustQty")
    @ApiModelProperty(value = "实际调整-操作前", name = "beforeActualAdjustQty")
    @TableField(value = "BEFORE_ACTUAL_ADJUST_QTY")
    private Integer beforeActualAdjustQty;

    /** 调整原因-操作前 */
    @Excel(name = "ui.data.column.mpAdjustStructureLog.beforeAdjustReason")
    @ApiModelProperty(value = "调整原因-操作前", name = "beforeAdjustReason")
    @TableField(value = "BEFORE_ADJUST_REASON")
    private String beforeAdjustReason;

    /** 净需求变动-操作后 */
    @Excel(name = "ui.data.column.mpAdjustStructureLog.afterNetQtyChange")
    @ApiModelProperty(value = "净需求变动-操作后", name = "afterNetQtyChange")
    @TableField(value = "AFTER_NET_QTY_CHANGE")
    private Integer afterNetQtyChange;

    /** 月计划已排产量-操作后 */
    @Excel(name = "ui.data.column.mpAdjustStructureLog.afterMonthScheduledQty")
    @ApiModelProperty(value = "月计划已排产量-操作后", name = "afterMonthScheduledQty")
    @TableField(value = "AFTER_MONTH_SCHEDULED_QTY")
    private Integer afterMonthScheduledQty;

    /** 待调整量-操作后 */
    @Excel(name = "ui.data.column.mpAdjustStructureLog.afterPendingQty")
    @ApiModelProperty(value = "待调整量-操作后", name = "afterPendingQty")
    @TableField(value = "AFTER_PENDING_QTY")
    private Integer afterPendingQty;

    /** 确认调整量-操作后 */
    @Excel(name = "ui.data.column.mpAdjustStructureLog.afterConfirmAdjustQty")
    @ApiModelProperty(value = "确认调整量-操作后", name = "afterConfirmAdjustQty")
    @TableField(value = "AFTER_CONFIRM_ADJUST_QTY")
    private Integer afterConfirmAdjustQty;

    /**
     * 调整优先级-操作后
     * 2、针对增量
     * 2.1）在产SKU增量，先补；
     * 2.2）新增SKU，按调整优先级1.2.3…
     * 3、该列默认空，允许编辑；
     */
    @Excel(name = "ui.data.column.mpAdjustStructureLog.afterAdjustPriority")
    @ApiModelProperty(value = "调整优先级-操作后", name = "afterAdjustPriority")
    @TableField(value = "AFTER_ADJUST_PRIORITY")
    private Integer afterAdjustPriority;

    /** 实际调整-操作后 */
    @Excel(name = "ui.data.column.mpAdjustStructureLog.afterActualAdjustQty")
    @ApiModelProperty(value = "实际调整-操作后", name = "afterActualAdjustQty")
    @TableField(value = "AFTER_ACTUAL_ADJUST_QTY")
    private Integer afterActualAdjustQty;

    /** 调整原因-操作后 */
    @Excel(name = "ui.data.column.mpAdjustStructureLog.afterAdjustReason")
    @ApiModelProperty(value = "调整原因-操作后", name = "afterAdjustReason")
    @TableField(value = "AFTER_ADJUST_REASON")
    private String afterAdjustReason;

    /** 平移方向 */
    @Excel(name = "ui.data.column.mpAdjustStructureLog.translationDirection")
    @ApiModelProperty(value = "平移方向", name = "translationDirection")
    @TableField(value = "TRANSLATION_DIRECTION")
    private String translationDirection;

    /** 是否发生平移 */
    @Excel(name = "ui.data.column.mpAdjustStructureLog.isTranslationDirection")
    @ApiModelProperty(value = "是否发生平移", name = "isTranslationDirection")
    @TableField(value = "IS_TRANSLATION_DIRECTION")
    private String isTranslationDirection;

    /** 操作动作 */
    @Excel(name = "ui.data.column.mpAdjustStructureLog.action")
    @ApiModelProperty(value = "操作动作", name = "action")
    @TableField(value = "ACTION")
    private String action;

    /** 操作员 */
    @Excel(name = "ui.data.column.mpAdjustStructureLog.operator")
    @ApiModelProperty(value = "操作员", name = "operator")
    @TableField(value = "OPERATOR")
    private String operator;


}