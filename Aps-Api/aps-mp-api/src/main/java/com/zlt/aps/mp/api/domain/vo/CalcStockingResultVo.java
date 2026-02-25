package com.zlt.aps.mp.api.domain.vo;

import com.zlt.aps.mp.api.domain.entity.MpHistorySaleQty;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 计算备货结果Vo
 *
 * @author hsc
 * @since 2025/2/18
 */
@Data
@ApiModel(value = "计算备货结果对象VO", description = "计算备货结果对象VO")
public class CalcStockingResultVo extends MpHistorySaleQty {
    /**
     * 品牌
     */
    @ApiModelProperty(value = "品牌", name = "brand")
    private String brand;
    /**
     * 花纹
     */
    @ApiModelProperty(value = "花纹", name = "pattern")
    private String pattern;
    /**
     * 规格
     */
    @ApiModelProperty(value = "规格", name = "specifications")
    private String specifications;
    /**
     * 寸口
     */
    @ApiModelProperty(value = "寸口", name = "proSize")
    private String proSize;

    /**
     * 月平均量
     */
    @ApiModelProperty(value = "月平均量", name = "augQty")
    private Long augQty;

    /**
     * 备货系数
     */
    @ApiModelProperty(value = "备货系数", name = "factorValue")
    private BigDecimal factorValue;

    /**
     * 备货量
     */
    @ApiModelProperty(value = "备货量", name = "stockQty")
    private Integer stockQty;
}
