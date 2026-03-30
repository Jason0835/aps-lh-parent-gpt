package com.zlt.aps.lh.api.domain.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.common.core.annotation.Excel;
import com.ruoyi.common.core.web.domain.BaseEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.Date;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：LhMoldChangePlan.java
 * 描    述：模具变动单对象 t_lh_mold_change_plan
 *@author zlt
 *@date 2025-02-17
 *@version 1.0
 *
 *  修改记录：
 *     修改时间：...
 *     修 改 人：zlt
 *     修改内容：...
 */

@ApiModel(value = "模具变动单对象", description = "模具变动单对象 ")
@Data
@TableName(value = "T_LH_MOLD_CHANGE_PLAN")
public class LhMoldChangePlan extends BaseEntity {


    private static final long serialVersionUID = -5155531958851523752L;

    /** 排程日期 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @ApiModelProperty(value = "排程日期", name = "scheduleDate")
    @Excel(name = "ui.data.column.lhMoldChangePlan.scheduleDate")
    @TableField(value = "SCHEDULE_DATE")
    private Date scheduleDate;

    /** 分厂编号 */
    @Excel(name = "ui.data.column.lhMoldChangePlan.factoryCode")
    @ApiModelProperty(value = "分厂编号", name = "factoryCode")
    @TableField(value = "FACTORY_CODE")
    private String factoryCode;

    /** 硫化结果批次号 */
    @Excel(name = "ui.data.column.lhMoldChangePlan.lhResultBatchNo")
    @ApiModelProperty(value = "硫化结果批次号", name = "lhResultBatchNo")
    @TableField(value = "LH_RESULT_BATCH_NO")
    private String lhResultBatchNo;

    /** 模具变动单批次号 */
    @ApiModelProperty(value = "模具变动单批次号", name = "moldBatchNo")
    @TableField(value = "MOLD_BATCH_NO")
    private String moldBatchNo;

    /** 硫化机台编号 */
    @Excel(name = "ui.data.column.lhMoldChangePlan.lhMachineCode")
    @ApiModelProperty(value = "硫化机台编号", name = "lhMachineCode")
    @TableField(value = "LH_MACHINE_CODE")
    private String lhMachineCode;

    /** 硫化机台名称 */
    @Excel(name = "ui.data.column.lhMoldChangePlan.lhMachineName")
    @ApiModelProperty(value = "硫化机台名称", name = "lhMachineName")
    @TableField(value = "LH_MACHINE_NAME")
    private String lhMachineName;

    /** 前规格品号 */
    @Excel(name = "ui.data.column.lhMoldChangePlan.beforeSpecCode")
    @ApiModelProperty(value = "前规格品号", name = "beforeSpecCode")
    @TableField(value = "BEFORE_SPEC_CODE")
    private String beforeSpecCode;

    /** 前规格描述 */
    @Excel(name = "ui.data.column.lhMoldChangePlan.beforeSpecDesc")
    @ApiModelProperty(value = "前规格描述", name = "beforeSpecDesc")
    @TableField(value = "BEFORE_SPEC_DESC")
    private String beforeSpecDesc;

    /** 胎胚库存 */
    @ApiModelProperty(value = "胎胚库存", name = "tireRoughStock")
    @TableField(value = "TIRE_ROUGH_STOCK")
    private Integer tireRoughStock;

    /** 变更类型：数据字典维护拆模换、点数换、合并收尾、拆模合并、左模收尾合并、右模收尾合并 */
    @ApiModelProperty(value = "变更类型：数据字典维护拆模换、点数换、合并收尾、拆模合并、左模收尾合并、右模收尾合并", name = "changeType")
    @TableField(value = "CHANGE_TYPE")
    private String changeType;

    /** 后规格品号 */
    @Excel(name = "ui.data.column.lhMoldChangePlan.afterSpecCode")
    @ApiModelProperty(value = "后规格品号", name = "afterSpecCode")
    @TableField(value = "AFTER_SPEC_CODE")
    private String afterSpecCode;

    /** 后规格描述 */
    @Excel(name = "ui.data.column.lhMoldChangePlan.afterSpecDesc")
    @ApiModelProperty(value = "后规格描述", name = "afterSpecDesc")
    @TableField(value = "AFTER_SPEC_DESC")
    private String afterSpecDesc;

    /** 库区信息,跟主计划库区同步,维护在数据字典中 */
//    @Excel(name = "ui.data.column.lhMoldChangePlan.stockArea")
    @ApiModelProperty(value = "库区信息,跟主计划库区同步,维护在数据字典中", name = "stockArea")
    @TableField(value = "STOCK_AREA")
    private String stockArea;

    /** 更换时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Excel(name = "ui.data.column.lhMoldChangePlan.changeTime", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty(value = "更换时间", name = "changeTime")
    @TableField(value = "CHANGE_TIME")
    private Date changeTime;

    /** 生成模具变动单数据所对应的成型工单数据，多条工单则用分号分隔：CXGD001;CXGDOO2 */
//    @Excel(name = "ui.data.column.lhMoldChangePlan.sourceCxOrder")
    @ApiModelProperty(value = "生成模具变动单数据所对应的成型工单数据，多条工单则用分号分隔：CXGD001;CXGDOO2", name = "sourceCxOrder")
    @TableField(value = "SOURCE_CX_ORDER")
    private String sourceCxOrder;

    /** 是否发布，0--未发布，1--已发布。对应数据字典为：IS_RELEASE */
    @Excel(name = "ui.data.column.lhMoldChangePlan.isRelease",dictType="IS_RELEASE")
    @ApiModelProperty(value = "是否发布，0--未发布，1--已发布。对应数据字典为：IS_RELEASE", name = "isRelease")
    @TableField(value = "IS_RELEASE")
    private String isRelease;

    /**
     * 更换时间start
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty(value = "查询更换时间开始时间", name = "changeTimeStart")
    @TableField(exist = false)
    private Date changeTimeStart;

    /**
     * 更换时间end
     */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @ApiModelProperty(value = "查询更换时间结束时间", name = "changeTimeEnd")
    @TableField(exist = false)
    private Date changeTimeEnd;


//    /** 操作后次日三班计划量 */
//    // @Excel(name = "ui.data.column.lhDispatcherLog.afterClass6Plan")
//    @ApiModelProperty(value = "操作后次日三班计划量", name = "afterClass6Plan")
//    @TableField(value = "AFTER_CLASS6_PLAN")
//    private Integer afterClass6Plan;

    /**
     * 前第一班计划量
     */
    @Excel(name = "ui.data.column.lhMoldChangePlan.beforeOnePlan")
    @TableField(exist = false)
    private Integer beforeOnePlan;

    /**
     * 前第一班完成量
     */
    @Excel(name = "ui.data.column.lhMoldChangePlan.beforeOneFinish")
    @TableField(exist = false)
    private Integer beforeOneFinish;

    /**
     * 后第一班计划量
     */
    @Excel(name = "ui.data.column.lhMoldChangePlan.afterOnePlan")
    @TableField(exist = false)
    private Integer afterOnePlan;

    /**
     * 后第一班完成量
     */
    @Excel(name = "ui.data.column.lhMoldChangePlan.afterOneFinish")
    @TableField(exist = false)
    private Integer afterOneFinish;


}