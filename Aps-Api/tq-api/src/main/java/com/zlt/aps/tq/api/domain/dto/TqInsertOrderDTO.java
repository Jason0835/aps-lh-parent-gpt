package com.zlt.aps.tq.api.domain.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 胎圈排程插单DTO
 * 包含插单所需的排程日期、胎圈代码、机台、6个班次的计划量/顺序/原因分析等
 *
 * @author APS
 */
@Data
@ApiModel(value = "胎圈排程插单DTO", description = "胎圈排程插单请求参数")
public class TqInsertOrderDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 排程日期 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @ApiModelProperty(value = "排程日期", name = "scheduleDate")
    private Date scheduleDate;

    /** 胎圈代码 */
    @ApiModelProperty(value = "胎圈代码", name = "beadCode")
    private String beadCode;

    /** 机台编号 */
    @ApiModelProperty(value = "机台编号", name = "machineCode")
    private String machineCode;

    /** 1班计划量（D日中班） */
    @ApiModelProperty(value = "1班计划量", name = "class1PlanQty")
    private Integer class1PlanQty;

    /** 1班顺序 */
    @ApiModelProperty(value = "1班顺序", name = "class1Sequence")
    private Integer class1Sequence;

    /** 1班原因分析 */
    @ApiModelProperty(value = "1班原因分析", name = "class1Analysis")
    private String class1Analysis;

    /** 2班计划量（D+1日夜班） */
    @ApiModelProperty(value = "2班计划量", name = "class2PlanQty")
    private Integer class2PlanQty;

    /** 2班顺序 */
    @ApiModelProperty(value = "2班顺序", name = "class2Sequence")
    private Integer class2Sequence;

    /** 2班原因分析 */
    @ApiModelProperty(value = "2班原因分析", name = "class2Analysis")
    private String class2Analysis;

    /** 3班计划量（D+1日早班） */
    @ApiModelProperty(value = "3班计划量", name = "class3PlanQty")
    private Integer class3PlanQty;

    /** 3班顺序 */
    @ApiModelProperty(value = "3班顺序", name = "class3Sequence")
    private Integer class3Sequence;

    /** 3班原因分析 */
    @ApiModelProperty(value = "3班原因分析", name = "class3Analysis")
    private String class3Analysis;

    /** 4班计划量（D+1日中班） */
    @ApiModelProperty(value = "4班计划量", name = "class4PlanQty")
    private Integer class4PlanQty;

    /** 4班顺序 */
    @ApiModelProperty(value = "4班顺序", name = "class4Sequence")
    private Integer class4Sequence;

    /** 4班原因分析 */
    @ApiModelProperty(value = "4班原因分析", name = "class4Analysis")
    private String class4Analysis;

    /** 5班计划量（D+2日夜班） */
    @ApiModelProperty(value = "5班计划量", name = "class5PlanQty")
    private Integer class5PlanQty;

    /** 5班顺序 */
    @ApiModelProperty(value = "5班顺序", name = "class5Sequence")
    private Integer class5Sequence;

    /** 5班原因分析 */
    @ApiModelProperty(value = "5班原因分析", name = "class5Analysis")
    private String class5Analysis;

    /** 6班计划量（D+2日早班） */
    @ApiModelProperty(value = "6班计划量", name = "class6PlanQty")
    private Integer class6PlanQty;

    /** 6班顺序 */
    @ApiModelProperty(value = "6班顺序", name = "class6Sequence")
    private Integer class6Sequence;

    /** 6班原因分析 */
    @ApiModelProperty(value = "6班原因分析", name = "class6Analysis")
    private String class6Analysis;

    /** 备注 */
    @ApiModelProperty(value = "备注", name = "remark")
    private String remark;
}
