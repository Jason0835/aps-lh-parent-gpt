package com.zlt.aps.lh.api.domain.entity;

import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;
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
 * 文件名称：LhMoldChangePlanLog.java
 * 描    述：模具变动单日志对象 t_lh_mold_change_plan_log
 *@author zlt
 *@date 2025-03-17
 *@version 1.0
 *
 *  修改记录：
 *     修改时间：...
 *     修 改 人：zlt
 *     修改内容：...
 */

@ApiModel(value = "模具变动单日志对象", description = "模具变动单日志对象 ")
@Data
@TableName(value = "T_LH_MOLD_CHANGE_PLAN_LOG")
public class LhMoldChangePlanLog extends BaseEntity {

    private static final long serialVersionUID = -9092716119984322417L;

    /** 排程日期 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @ApiModelProperty(value = "排程日期", name = "scheduleDate")
    @TableField(value = "SCHEDULE_DATE")
    private Date scheduleDate;

    /** 分厂编号 */
    @Excel(name = "ui.data.column.lhMoldChangePlanLog.factoryCode")
    @ApiModelProperty(value = "分厂编号", name = "factoryCode")
    @TableField(value = "FACTORY_CODE")
    private String factoryCode;

    /** 硫化结果批次号 */
    @Excel(name = "ui.data.column.lhMoldChangePlan.lhResultBatchNo")
    @ApiModelProperty(value = "硫化结果批次号", name = "lhResultBatchNo")
    @TableField(value = "LH_RESULT_BATCH_NO")
    private String lhResultBatchNo;

    /** 模具变动单批次号 */
    @Excel(name = "ui.data.column.lhMoldChangePlanLog.moldBatchNo")
    @ApiModelProperty(value = "模具变动单批次号", name = "moldBatchNo")
    @TableField(value = "MOLD_BATCH_NO")
    private String moldBatchNo;

    /** 硫化机台编号 */
    @Excel(name = "ui.data.column.lhMoldChangePlanLog.lhMachineCode")
    @ApiModelProperty(value = "硫化机台编号", name = "lhMachineCode")
    @TableField(value = "LH_MACHINE_CODE")
    private String lhMachineCode;

    /** 硫化机台名称 */
    @Excel(name = "ui.data.column.lhMoldChangePlanLog.lhMachineName")
    @ApiModelProperty(value = "硫化机台名称", name = "lhMachineName")
    @TableField(value = "LH_MACHINE_NAME")
    private String lhMachineName;

    /** 前规格品号 */
    @Excel(name = "ui.data.column.lhMoldChangePlanLog.beforeSpecCode")
    @ApiModelProperty(value = "前规格品号", name = "beforeSpecCode")
    @TableField(value = "BEFORE_SPEC_CODE")
    private String beforeSpecCode;

    /** 前规格描述 */
    @Excel(name = "ui.data.column.lhMoldChangePlanLog.beforeSpecDesc")
    @ApiModelProperty(value = "前规格描述", name = "beforeSpecDesc")
    @TableField(value = "BEFORE_SPEC_DESC")
    private String beforeSpecDesc;

    /** 胎胚库存 */
    @Excel(name = "ui.data.column.lhMoldChangePlanLog.tireRoughStock")
    @ApiModelProperty(value = "胎胚库存", name = "tireRoughStock")
    @TableField(value = "TIRE_ROUGH_STOCK")
    private Integer tireRoughStock;

    /** 变更类型：数据字典维护拆模换、点数换、合并收尾、拆模合并、左模收尾合并、右模收尾合并 */
    @Excel(name = "ui.data.column.lhMoldChangePlanLog.changeType")
    @ApiModelProperty(value = "变更类型：数据字典维护拆模换、点数换、合并收尾、拆模合并、左模收尾合并、右模收尾合并", name = "changeType")
    @TableField(value = "CHANGE_TYPE")
    private String changeType;

    /** 后规格品号 */
    @Excel(name = "ui.data.column.lhMoldChangePlanLog.afterSpecCode")
    @ApiModelProperty(value = "后规格品号", name = "afterSpecCode")
    @TableField(value = "AFTER_SPEC_CODE")
    private String afterSpecCode;

    /** 后规格描述 */
    @Excel(name = "ui.data.column.lhMoldChangePlanLog.afterSpecDesc")
    @ApiModelProperty(value = "后规格描述", name = "afterSpecDesc")
    @TableField(value = "AFTER_SPEC_DESC")
    private String afterSpecDesc;

    /** 库区信息,跟主计划库区同步,维护在数据字典中 */
    @Excel(name = "ui.data.column.lhMoldChangePlanLog.stockArea")
    @ApiModelProperty(value = "库区信息,跟主计划库区同步,维护在数据字典中", name = "stockArea")
    @TableField(value = "STOCK_AREA")
    private String stockArea;

    /** 更换时间 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "ui.data.column.lhMoldChangePlanLog.changeTime", width = 30, dateFormat = "yyyy-MM-dd")
    @ApiModelProperty(value = "更换时间", name = "changeTime")
    @TableField(value = "CHANGE_TIME")
    private Date changeTime;

    /** （发布不包含此字段）生成模具变动单数据所对应的成型工单数据，多条工单则用分号分隔：            CXGD001;CXGDOO2 */
    @Excel(name = "ui.data.column.lhMoldChangePlanLog.sourceCxOrder", readConverterExp = "发=布不包含此字段")
    @ApiModelProperty(value = "", name = "sourceCxOrder")
    @TableField(value = "SOURCE_CX_ORDER")
    private String sourceCxOrder;

    /** 是否发布，0--未发布，1--已发布。对应数据字典为：IS_RELEASE */
    @Excel(name = "ui.data.column.lhMoldChangePlanLog.isRelease")
    @ApiModelProperty(value = "是否发布，0--未发布，1--已发布。对应数据字典为：IS_RELEASE", name = "isRelease")
    @TableField(value = "IS_RELEASE")
    private String isRelease;


}