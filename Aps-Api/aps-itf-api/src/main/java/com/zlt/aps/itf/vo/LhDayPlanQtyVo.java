package com.zlt.aps.itf.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

/**
 * 硫化日计划量VO
 * <p>用于试制/量试完成量回报规则中，按 工厂+排程日期+物料+示方类型 汇总当日计划量（class3+class4+class5）</p>
 *
 * @author zlt
 */
@ApiModel(value = "硫化日计划量VO", description = "硫化日计划量VO")
@Data
public class LhDayPlanQtyVo implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "工厂编码")
    private String factoryCode;

    @ApiModelProperty(value = "物料编码")
    private String materialCode;

    @ApiModelProperty(value = "示方类型 X试验 T量试 S正规")
    private String lhType;

    @ApiModelProperty(value = "当日计划量（3班+4班+5班计划量合计）")
    private Integer dayPlanQty;
}
