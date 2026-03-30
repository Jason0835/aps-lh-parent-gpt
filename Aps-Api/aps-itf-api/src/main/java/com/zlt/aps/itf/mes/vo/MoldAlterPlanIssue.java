package com.zlt.aps.itf.mes.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 模具交替计划下发接口
 *
 * @author APS Team
 * @since 2026/03/29
 */
@ApiModel(value = "模具交替计划下发接口", description = "模具交替计划下发接口")
@Data
public class MoldAlterPlanIssue {

    @ApiModelProperty(value = "主键")
    private Long id;

    @ApiModelProperty(value = "硫化批次号")
    private String lhBatchNo;

    @ApiModelProperty(value = "工单号")
    private String orderNo;

    @ApiModelProperty(value = "计划日期")
    private Date scheduleDate;

    @ApiModelProperty(value = "班次")
    private String classIndex;

    @ApiModelProperty(value = "硫化机台编号")
    private String lhMachineCode;

    @ApiModelProperty(value = "计划顺位")
    private BigDecimal planIndex;

    @ApiModelProperty(value = "左右模")
    private String leftRightMold;

    @ApiModelProperty(value = "当前物料编码（NC）")
    private String materialCode;

    @ApiModelProperty(value = "当前物料编码（MES）")
    private String mesMaterialCode;

    @ApiModelProperty(value = "当前物料描述")
    private String specDesc;

    @ApiModelProperty(value = "计划物料编码（NC）")
    private String planMaterialCode;

    @ApiModelProperty(value = "计划物料编码（MES）")
    private String mesPlanMaterialCode;

    @ApiModelProperty(value = "计划物料描述")
    private String planSpecDesc;

    @ApiModelProperty(value = "交替类型")
    private String changeMoldType;

    @ApiModelProperty(value = "模具号")
    private String moldNo;

    @ApiModelProperty(value = "备注")
    private String remark;

    @ApiModelProperty(value = "模具交替完成状态")
    private String finishStatus;

    @ApiModelProperty(value = "版本号")
    private String dataVersion;

    @ApiModelProperty(value = "分公司编码")
    private String companyCode;

    @ApiModelProperty(value = "厂别")
    private String factoryCode;

}
