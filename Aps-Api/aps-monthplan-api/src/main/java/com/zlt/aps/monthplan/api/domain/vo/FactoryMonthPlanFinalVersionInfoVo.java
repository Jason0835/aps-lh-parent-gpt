package com.zlt.aps.monthplan.api.domain.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.Date;

/**
 * 分厂月份定稿版本信息对象
 *
 * @author ZLT
 * @date 20250320
 */
@Data
@ApiModel(value = "分厂月份定稿版本信息对象", description = "分厂月份定稿版本信息对象")
public class FactoryMonthPlanFinalVersionInfoVo {
    /**
     * 分厂编码
     */
    @ApiModelProperty(value = "分厂编码", name = "factoryCode")
    private String factoryCode;
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
     * 制造需求版本
     */
    @ApiModelProperty(value = "制造需求版本", name = "monthPlanVersion")
    private String monthPlanVersion;
    /**
     * 分厂版本
     */
    @ApiModelProperty(value = "分厂版本", name = "productionVersion")
    private String productionVersion;
    /**
     * 胎别
     */
    @ApiModelProperty(value = "胎别", name = "productTypeCode")
    private String productTypeCode;

    /**
     * 月份排产起始日
     */
    @ApiModelProperty(value = "月份排产起始日", name = "productionStartDate")
    private Date productionStartDate;

    /**
     * 月份排产最大结束日
     */
    @ApiModelProperty(value = "月份排产最大结束日", name = "productionEndDate")
    private Date productionEndDate;

    /**
     * 0 不是自然月 1 是自然月
     */
    @ApiModelProperty(value = "0 不是自然月 1 是自然月", name = "isNaturalMonth")
    private Integer isNaturalMonth;
}
