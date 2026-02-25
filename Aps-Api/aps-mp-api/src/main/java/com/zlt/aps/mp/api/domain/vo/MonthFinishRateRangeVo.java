package com.zlt.aps.mp.api.domain.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

@ApiModel(value = "报表管理，T月完成率明细对象", description = "报表管理，T月完成率明细对象")
@Data
public class MonthFinishRateRangeVo implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 完成率范围字典名称
     */
    @ApiModelProperty(value = "完成率范围字典名称", name = "rangeLabel")
    private String rangeLabel;

    /**
     * 完成率范围的字典值
     */
    @ApiModelProperty(value = "完成率范围的字典值，字典：", name = "rangeKey")
    private String rangeKey;

    /**
     * SKU数量
     */
    @ApiModelProperty(value = "SKU数量", name = "skuCount")
    private Integer skuCount;

    /**
     * 单位
     */
    @ApiModelProperty(value = "单位，字典：unit", name = "unit")
    private String unit;

}
