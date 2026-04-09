package com.zlt.aps.cx.api.domain.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 插单请求VO
 *
 * @author APS Team
 */
@Data
@ApiModel(value = "插单请求对象")
public class ScheduleInsertVo {

    @ApiModelProperty(value = "排程日期", required = true)
    private String scheduleDate;

    @ApiModelProperty(value = "机台编码", required = true)
    private String cxMachineCode;

    @ApiModelProperty(value = "机台名称")
    private String cxMachineName;

    @ApiModelProperty(value = "胎胚描述/物料编码", required = true)
    private String embryoCode;

    @ApiModelProperty(value = "NC物料编码", required = true)
    private String materialCode;

    @ApiModelProperty(value = "物料描述")
    private String specDesc;

    @ApiModelProperty(value = "示方版本")
    private String exampleNo;

    @ApiModelProperty(value = "夜班计划量")
    private BigDecimal class1PlanQty;

    @ApiModelProperty(value = "早班计划量")
    private BigDecimal class2PlanQty;

    @ApiModelProperty(value = "中班计划量")
    private BigDecimal class3PlanQty;

    @ApiModelProperty(value = "夜班原因分析")
    private String class1Analysis;

    @ApiModelProperty(value = "早班原因分析")
    private String class2Analysis;

    @ApiModelProperty(value = "中班原因分析")
    private String class3Analysis;
}
