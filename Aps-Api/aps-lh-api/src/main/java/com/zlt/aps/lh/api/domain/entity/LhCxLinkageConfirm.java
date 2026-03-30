package com.zlt.aps.lh.api.domain.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.common.core.web.domain.BaseEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.util.Date;

/**
 * Copyright (c) 2025, All rights reserved。
 * 文件名称：LhCxLinkageConfirm.java
 * 描    述：硫化成型联动确认对象 t_lh_params
 *
 * @author zlt
 * @version 1.0
 * <p>
 * 修改记录：
 * 修改时间：...
 * 修 改 人：zlt
 * 修改内容：...
 * @date 2025-02-18
 */
@EqualsAndHashCode(callSuper = false)
@Data
@TableName("T_LH_CX_LINKAGE_CONFIRM")
@ApiModel(value = "硫化成型联动确认", description = "硫化成型联动确认")
public class LhCxLinkageConfirm extends BaseEntity implements Serializable {

    @ApiModelProperty(value = "分厂编号")
    private String factoryCode;

    @ApiModelProperty(value = "自动排程批次号信息，每重新生成一次排程结果，批次号就递增。规则：工序+年月日+3位定长自增序号")
    private String batchNo;

    @ApiModelProperty(value = "工单号，自动生成（工序+日期+三位顺序号001,002）")
    private String orderNo;

    @ApiModelProperty(value = "调整批次号")
    private String adjustBatchNo;

    @ApiModelProperty(value = "硫化排程ID")
    private Long lhScheduleId;

    @ApiModelProperty(value = "硫化机台编号")
    private String lhMachineCode;

    @ApiModelProperty(value = "规格代码")
    private String specCode;

    @ApiModelProperty(value = "生胎代码")
    private String embryoCode;

    @ApiModelProperty(value = "规格描述信息")
    private String specDesc;

    @ApiModelProperty(value = "调整类型")
    private String adjustType;

    @ApiModelProperty(value = "调整量")
    private Long adjustQty;

    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    @ApiModelProperty(value = "排程日期")
    private Date scheduleDate;

    @ApiModelProperty(value = "成型排程ID")
    private Long cxScheduleId;

    @ApiModelProperty(value = "原成型机台编号")
    private String oriCxMachineCode;

    @ApiModelProperty(value = "原成型规格代码")
    private String oriCxSpecCode;

    @ApiModelProperty(value = "原成型生胎代码")
    private String oriCxEmbryoCode;

    @ApiModelProperty(value = "原成型一班计划量")
    private Integer oriCxClass1PlanQty;

    @ApiModelProperty(value = "原成型二班计划量")
    private Integer oriCxClass2PlanQty;

    @ApiModelProperty(value = "原成型三班计划量")
    private Integer oriCxClass3PlanQty;

    @ApiModelProperty(value = "原成型次日一班计划数")
    private Integer oriCxClass4PlanQty;

    @ApiModelProperty(value = "原成型次日二班计划量")
    private Integer oriCxClass5PlanQty;

    @ApiModelProperty(value = "原成型次日三班计划量")
    private Integer oriCxClass6PlanQty;

    @ApiModelProperty(value = "新成型机台编号")
    private String newCxMachineCode;

    // @ApiModelProperty(value = "新成型机台名称")
    // private String newCxMachineName;

    @ApiModelProperty(value = "新成型规格代码")
    private String newCxSpecCode;

    @ApiModelProperty(value = "新成型生胎代码")
    private String newCxEmbryoCode;

    @ApiModelProperty(value = "新成型一班计划量")
    private Integer newCxClass1PlanQty;

    @ApiModelProperty(value = "新成型二班计划量")
    private Integer newCxClass2PlanQty;

    @ApiModelProperty(value = "新成型三班计划量")
    private Integer newCxClass3PlanQty;

    @ApiModelProperty(value = "新成型次日一班计划量")
    private Integer newCxClass4PlanQty;

    @ApiModelProperty(value = "新成型次日二班计划量")
    private Integer newCxClass5PlanQty;

    @ApiModelProperty(value = "新成型次日三班计划量")
    private Integer newCxClass6PlanQty;

    @ApiModelProperty(value = "是否确认")
    private String isConfirm;
}