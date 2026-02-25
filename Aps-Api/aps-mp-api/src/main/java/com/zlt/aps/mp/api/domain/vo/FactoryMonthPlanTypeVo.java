package com.zlt.aps.mp.api.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 工厂月计划方式
 *
 * @author ZLT
 * @data 20250521
 */
@Data
public class FactoryMonthPlanTypeVo implements Serializable {

    /**
     * 工厂编码
     */
    @ApiModelProperty(value = "工厂编码", name = "factoryCode")
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
     * 工厂排产版本号
     */
    @ApiModelProperty(value = "工厂排产版本号", name = "productionVersion")
    private String productionVersion;
    /**
     * 月份排产起始日
     */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @ApiModelProperty(value = "月份排产起始日", name = "productionStartDate")
    private Date productionStartDate;
    /**
     * 0 不是自然月 1 是自然月
     */
    @ApiModelProperty(value = "0 不是自然月 1 是自然月", name = "isNaturalMonth")
    private Integer isNaturalMonth;

}
