package com.zlt.aps.monthplan.api.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 月计划选择需求计划记录对象
 *
 * @author ZLT
 * @date 20251201
 */
@Data
@ApiModel(value = "月份计划-计划调整参数对象", description = "月份计划-计划调整参数对象")
public class FactoryMonthPlanVersionVo implements Serializable {

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
     * 创建时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty(value = "创建时间", name = "createTime")
    private Date createTime;
}
