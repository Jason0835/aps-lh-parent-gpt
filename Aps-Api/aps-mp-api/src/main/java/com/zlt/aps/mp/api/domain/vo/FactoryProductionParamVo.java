package com.zlt.aps.mp.api.domain.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotNull;

/**
 * 工厂排产条件对象
 *
 * @author ZLT
 * @date 20251203
 */
@Data
@ApiModel(value = "工厂排产条件对象", description = "工厂排产条件对象")
public class FactoryProductionParamVo extends FactoryProductionPlanVo {
    /**
     * 工厂编码
     */
    @NotNull
    @ApiModelProperty(required = true, value = "工厂编码", name = "factoryCode")
    private String factoryCode;

    /**
     * 年份
     */
    @NotNull
    @ApiModelProperty(required = true, value = "年份", name = "year")
    private Integer year;

    /**
     * 月份
     */
    @NotNull
    @ApiModelProperty(required = true, value = "月份", name = "month")
    private Integer month;

    /**
     * 需求版本
     */
    @NotNull
    @ApiModelProperty(required = true, value = "需求版本", name = "monthPlanVersion")
    private String monthPlanVersion;

    /**
     * 排产版本
     */
    @ApiModelProperty(value = "排产版本", name = "productionVersion")
    private String productionVersion;


    /**
     * 版本前缀
     */
    @ApiModelProperty(value = "排产版本前缀", name = "prefixVersion")
    private String prefixVersion;
}
