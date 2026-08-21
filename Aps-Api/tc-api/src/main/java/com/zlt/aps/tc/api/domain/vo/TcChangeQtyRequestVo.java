package com.zlt.aps.tc.api.domain.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 胎侧人工调量请求。
 */
@Data
@ApiModel(value = "胎侧人工调量请求")
public class TcChangeQtyRequestVo implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 排程结果 ID。 */
    @ApiModelProperty(value = "排程结果 ID", required = true)
    private Long resultId;

    /** 待调班次。 */
    @ApiModelProperty(value = "待调班次", required = true)
    private Integer shiftOrder;

    /** 新计划量。 */
    @ApiModelProperty(value = "新计划量", required = true)
    private BigDecimal newPlanQty;

    /** 新原因分析。 */
    @ApiModelProperty(value = "新原因分析")
    private String newAnalysis;

    /** 期望任务版本。 */
    @ApiModelProperty(value = "期望任务版本", required = true)
    private Long expectedTaskVersion;

    /** 操作原因。 */
    @ApiModelProperty(value = "操作原因", required = true)
    private String reason;
}
