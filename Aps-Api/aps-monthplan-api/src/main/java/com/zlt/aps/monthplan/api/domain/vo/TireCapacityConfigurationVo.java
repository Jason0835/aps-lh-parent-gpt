package com.zlt.aps.monthplan.api.domain.vo;

import com.zlt.aps.monthplan.api.domain.entity.TireCapacityConfiguration;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * 寸口产能配置查询列表对象
 *
 * @author ZLT
 * @date 20250604
 */
@Data
@ApiModel(value = "轮胎类型产能配置查询列表对象", description = "轮胎类型产能配置查询列表对象 ")
public class TireCapacityConfigurationVo extends TireCapacityConfiguration {

    private static final long serialVersionUID = 1L;

    /**
     * 总需求
     */
    @ApiModelProperty(value = "总需求", name = "demandQty")
    private Long demandQty;

    /**
     * 净需求
     */
    @ApiModelProperty(value = "净需求", name = "netDemandQty")
    private Long netDemandQty;

    /**
     * 备货需求
     */
    @ApiModelProperty(value = "备货需求", name = "stockUpDemandQty")
    private Integer stockUpDemandQty;

}