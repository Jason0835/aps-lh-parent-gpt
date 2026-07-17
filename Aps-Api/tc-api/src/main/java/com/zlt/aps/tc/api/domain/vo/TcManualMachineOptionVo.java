package com.zlt.aps.tc.api.domain.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 胎侧人工操作机台选项。
 */
@Data
@ApiModel(value = "胎侧人工操作机台选项")
public class TcManualMachineOptionVo implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 机台编码。 */
    @ApiModelProperty(value = "机台编码")
    private String machineCode;

    /** 机台名称。 */
    @ApiModelProperty(value = "机台名称")
    private String machineName;

    /** 单班最大产能。 */
    @ApiModelProperty(value = "单班最大产能")
    private BigDecimal maxCapacity;
}
