package com.zlt.aps.monthplan.factory.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 分厂生产计划控制版本对象
 *
 * @author ZLT
 * @date 20250213
 */
@Data
@ApiModel(value = "分厂生产计划控制版本对象", description = "分厂生产计划控制版本对象")
public class FactoryProductionPlanVersionDto implements Serializable {

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
     * 分厂编码
     */
    @ApiModelProperty(value = "分厂编码", name = "factoryCode")
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
    /**
     * 分厂初始化排产版本
     */
    @ApiModelProperty(value = "分厂初始化排产版本", name = "initVersion")
    private String initVersion;
    /**
     * 分厂排产版本
     */
    @ApiModelProperty(value = "分厂排产版本集合", name = "productionVersion")
    private String productionVersion;
    /**
     * 是否定稿
     */
    @ApiModelProperty(value = "是否定稿", name = "isFinal")
    private Integer isFinal;
    /**
     * 创建时间
     */
    @ApiModelProperty(value = "创建时间", name = "createTime")
    private Date createTime;

    /**
     * 月份排产起始日
     */
    @ApiModelProperty(value = "月份排产起始日", name = "productionStartDate")
    private Date productionStartDate;

    /**
     * 0 不是自然月 1 是自然月
     */
    @ApiModelProperty(value = "0 不是自然月 1 是自然月", name = "isNaturalMonth")
    private Integer isNaturalMonth;

    /**
     * 获取分厂销售需求版本计划分组Key
     *
     * @return
     */
    public String getMonthPlanVersionKey() {
        String keyFormat = "%d|*|%d|*|%s|*|%s|*|%s";
        return String.format(keyFormat, this.year, this.month, this.factoryCode, this.productTypeCode, this.monthPlanVersion);
    }
}
