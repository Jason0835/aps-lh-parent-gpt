package com.zlt.aps.monthplan.api.domain.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

/**
 * 分厂生产计划查询条件类
 *
 * @author ZLT
 * @date 20250213
 */
@Data
@ApiModel(value = "分厂生产计划控制台查询条件对象", description = "分厂生产计划控制台查询条件对象")
public class FactoryProductionPlanVo implements Serializable {

    /**
     * 年份
     */
    @ApiModelProperty(value = "年份", name = "year")
    private Integer year;

    /**
     * 月份
     */
    @ApiModelProperty(value = "月份", name = "month")
    private Integer month;

    /**
     * 工厂编码
     */
    @ApiModelProperty(value = "工厂编码", name = "factoryCode")
    private String factoryCode;

    /**
     * 胎别
     */
    @ApiModelProperty(value = "胎别", name = "productTypeCode")
    private String productTypeCode;

    /**
     * 销售生产需求计划版本
     */
    @ApiModelProperty(value = "销售生产需求计划版本", name = "monthPlanVersion")
    private String monthPlanVersion;

}
