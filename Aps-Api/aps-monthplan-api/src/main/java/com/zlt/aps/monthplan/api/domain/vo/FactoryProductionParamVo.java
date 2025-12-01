package com.zlt.aps.monthplan.api.domain.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotNull;
import java.io.Serializable;

/**
 * 分厂排产条件对象
 *
 * @author ZLT
 * @date 20250310
 */
@Data
@ApiModel(value = "分厂排产条件对象", description = "分厂排产条件对象")
public class FactoryProductionParamVo implements Serializable {
    /**
     * 分厂编号
     */
    @NotNull
    @ApiModelProperty(required = true, value = "分厂编号", name = "factoryCode")
    private String factoryCode;

    /**
     * 年度
     */
    @NotNull
    @ApiModelProperty(required = true, value = "计划年度", name = "year")
    private Integer year;

    /**
     * 月份
     */
    @NotNull
    @ApiModelProperty(required = true, value = "计划月份", name = "month")
    private Integer month;

    /**
     * 月度销售生产需求计划版本
     */
    @NotNull
    @ApiModelProperty(required = true, value = "月生产计划版本", name = "monthPlanVersion")
    private String monthPlanVersion;

    /**
     * 排产版本
     */
    @ApiModelProperty(required = false, value = "排产版本", name = "productionVersion")
    private String productionVersion;


    /**
     * 版本前缀
     */
    @ApiModelProperty(required = false, value = "排产版本前缀", name = "prefixVersion")
    private String prefixVersion;
}
