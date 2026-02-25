package com.zlt.aps.mp.api.domain.dto;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 胎类区分查询参数
 *
 * @author Chen
 * @date 2025/3/31
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class TireTypeReportDto extends BaseReportDto {

    /**
     * 渠道
     */
    @ApiModelProperty(value = "渠道，不传值就是查月份综合，传值查对应渠道，字典：biz_channel_name", name = "channel")
    private String channel;

    /**
     * 统计是否有缺口的数据，0=无缺口，1=有缺口，2=都统计
     */
    private String hasGap;

    /**
     * 查询类型：1=有订单，0=无订单
     */
    private String hasOrder;

    /**
     * 是否未排产，0=部分未排，1=全部未排，2=全部排产
     */
    private String hasNoProduce;

    /**
     * 是否欠产（0：默认不是，1：是）
     */
    private Integer isDebitPlan;

    /**
     * 是否备货，0：不是，1：是
     */
    private Integer isStockUp;
}
