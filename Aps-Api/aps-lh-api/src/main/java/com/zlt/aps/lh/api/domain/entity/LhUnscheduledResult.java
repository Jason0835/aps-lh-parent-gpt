package com.zlt.aps.lh.api.domain.entity;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.common.core.web.domain.BaseEntity;
import com.zlt.aps.lh.api.domain.vo.LhMoldInfoVo;
import lombok.Data;
import com.ruoyi.common.core.annotation.Excel;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：LhUnscheduledResult.java
 * 描    述：硫化未排结果对象 t_lh_unscheduled_result
 *@author zlt
 *@date 2025-03-07
 *@version 1.0
 *
 *  修改记录：
 *     修改时间：...
 *     修 改 人：zlt
 *     修改内容：...
 */

@ApiModel(value = "硫化未排结果对象", description = "硫化未排结果对象 ")
@Data
@TableName(value = "T_LH_UNSCHEDULED_RESULT")
public class LhUnscheduledResult extends BaseEntity implements Serializable {

    private static final long serialVersionUID = 6065237210877969251L;

     /** 分厂编号 */
    @Excel(name = "ui.data.column.lhUnscheduledResult.factoryCode")
    @ApiModelProperty(value = "分厂编号", name = "factoryCode")
    @TableField(value = "FACTORY_CODE")
    private String factoryCode;

    /** 自动排程批次号 */
    @Excel(name = "ui.data.column.lhUnscheduledResult.batchNo")
    @ApiModelProperty(value = "自动排程批次号", name = "batchNo")
    @TableField(value = "BATCH_NO")
    private String batchNo;

    /** 工单号 */
    @Excel(name = "ui.data.column.lhUnscheduledResult.orderNo")
    @ApiModelProperty(value = "工单号", name = "orderNo")
    @TableField(value = "ORDER_NO")
    private String orderNo;

    /** 月度计划单号 */
    @Excel(name = "ui.data.column.lhUnscheduledResult.monthPlanNo")
    @ApiModelProperty(value = "月度计划单号", name = "monthPlanNo")
    @TableField(value = "MONTH_PLAN_NO")
    private String monthPlanNo;

    /** 月度计划版本号 */
    @ApiModelProperty(value = "月度计划版本号", name = "MONTH_PLAN_VERSION")
    @TableField(value = "MONTH_PLAN_VERSION")
    private String monthPlanVersion;

    /** 物料编号 */
    @Excel(name = "ui.data.column.lhUnscheduledResult.productCode")
    @ApiModelProperty(value = "物料编号", name = "productCode")
    @TableField(value = "PRODUCT_CODE")
    private String productCode;

    /** 规格代码 */
    @Excel(name = "ui.data.column.lhUnscheduledResult.specCode")
    @ApiModelProperty(value = "规格代码", name = "specCode")
    @TableField(value = "SPEC_CODE")
    private String specCode;

    /** 胎胚代码 */
    @Excel(name = "ui.data.column.lhUnscheduledResult.embryoCode")
    @ApiModelProperty(value = "胎胚代码", name = "embryoCode")
    @TableField(value = "EMBRYO_CODE")
    private String embryoCode;

    /** 胎胚库存 */
    @Excel(name = "ui.data.column.lhUnscheduledResult.embryoStock")
    @ApiModelProperty(value = "胎胚库存", name = "embryoStock")
    @TableField(value = "EMBRYO_STOCK")
    private Integer embryoStock;

    /** 规格描述信息 */
    @Excel(name = "ui.data.column.lhUnscheduledResult.specDesc")
    @ApiModelProperty(value = "规格描述信息", name = "specDesc")
    @TableField(value = "SPEC_DESC")
    private String specDesc;

    /** 未排数量 */
    @Excel(name = "ui.data.column.lhUnscheduledResult.unscheduledQty")
    @ApiModelProperty(value = "未排数量", name = "unscheduledQty")
    @TableField(value = "UNSCHEDULED_QTY")
    private Integer unscheduledQty;

    /** 未排原因 */
    @Excel(name = "ui.data.column.lhUnscheduledResult.unscheduledReason")
    @ApiModelProperty(value = "未排原因", name = "unscheduledReason")
    @TableField(value = "UNSCHEDULED_REASON")
    private String unscheduledReason;

    /** 月度计划模数 */
    @Excel(name = "ui.data.column.lhUnscheduledResult.mpMoldQty")
    @ApiModelProperty(value = "月度计划模数", name = "mpMoldQty")
    @TableField(value = "MP_MOLD_QTY")
    private Integer mpMoldQty;

    /** 使用模数 */
    @Excel(name = "ui.data.column.lhUnscheduledResult.moldQty")
    @ApiModelProperty(value = "使用模数", name = "moldQty")
    @TableField(value = "MOLD_QTY")
    private Integer moldQty;

    /** 排程日期 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "ui.data.column.lhUnscheduledResult.scheduleDate", width = 30, dateFormat = "yyyy-MM-dd")
    @ApiModelProperty(value = "排程日期", name = "scheduleDate")
    @TableField(value = "SCHEDULE_DATE")
    private Date scheduleDate;

    /** 数据来源：0&gt;自动排程；1&gt;插单；2：导入 */
    @Excel(name = "ui.data.column.lhUnscheduledResult.dataSource")
    @ApiModelProperty(value = "数据来源：0&gt;自动排程；1&gt;插单；2：导入", name = "dataSource")
    @TableField(value = "DATA_SOURCE")
    private String dataSource;

    /** 处理时间 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "ui.data.column.lhUnscheduledResult.processedTime", width = 30, dateFormat = "yyyy-MM-dd")
    @ApiModelProperty(value = "处理时间", name = "processedTime")
    @TableField(value = "PROCESSED_TIME")
    private Date processedTime;

    @ApiModelProperty(value = "可用模具列表,仅用于是否要补量")
    @TableField(exist = false)
    private List<LhMoldInfoVo> availLhMoldInfoVoList;
}