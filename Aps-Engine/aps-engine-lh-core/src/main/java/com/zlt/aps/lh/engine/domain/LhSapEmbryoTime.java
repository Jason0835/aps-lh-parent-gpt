package com.zlt.aps.lh.engine.domain;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.Date;

/**
 * 硫化使用到胎胚的供应时间段
 */
@Data
@ApiModel(value = "外胎对应成型任务的时间点对象", description = "外胎对应成型任务的时间点对象 ")
public class LhSapEmbryoTime {

    /**
     * sap品号
     */
    @ApiModelProperty(value = "sap品号")
    private String sapCode;

    /**
     * 胎胚代码
     */
    @ApiModelProperty(value = "胎胚代码")
    private String embryoCode;

    /**
     * 胎胚施工版本
     */
    @ApiModelProperty(value = "胎胚施工版本")
    private String bomDataVersion;

    /**
     * 成型机台编号
     */
    @ApiModelProperty(value = "成型机台编号")
    private String cxMachineCode;

    /**
     * 成型机台名称
     */
    @ApiModelProperty(value = "成型机台名称")
    private String cxMachineName;

    /**
     * 成型工单号
     */
    @ApiModelProperty(value = "成型工单号")
    private String cxOrderNo;

    /**
     * 机台任务胎胚生产顺序
     */
    @ApiModelProperty(value = "机台任务胎胚生产顺序")
    private Integer productOrder;

    /**
     * 胎胚开始生产时间
     */
    @ApiModelProperty(value = "胎胚开始生产时间")
    private Date estimateStartTime;

    /**
     * 胎胚开始结束时间
     */
    @ApiModelProperty(value = "胎胚开始结束时间")
    private Date estimateEndTime;
}
