package com.zlt.mix.schedule.api.domain.dto;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

import com.fasterxml.jackson.annotation.JsonFormat;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * 硫磺辅料超期硬统计
 * @author zlt
 * @date 2023-05-11
 */
@Data
public class MaterialExpireWarningDto implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "密炼区")
    private String mixArea;

    @ApiModelProperty(value = "小料名称")
    private String materialName;

    @ApiModelProperty(value = "有效期")
	@JsonFormat(pattern = "yyyy-MM-dd HH:mm", timezone = "GMT+8")
    private Date validTime;

    @ApiModelProperty(value = "预警数量")
    private BigDecimal warningQty;

    @ApiModelProperty(value = "预警重量")
    private BigDecimal warningWeight;
}
