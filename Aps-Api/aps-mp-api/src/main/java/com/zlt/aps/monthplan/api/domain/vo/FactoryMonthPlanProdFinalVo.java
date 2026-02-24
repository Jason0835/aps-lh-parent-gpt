package com.zlt.aps.monthplan.api.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.zlt.aps.monthplan.api.domain.entity.FactoryMonthPlanProdFinal;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;
import java.util.Set;

/**
 * 分厂月生产计划排产结果-最终版本(包含调整单调整的结果)
 *
 * @author ZLT
 * @data 20250214
 */
@Data
public class FactoryMonthPlanProdFinalVo extends FactoryMonthPlanProdFinal {

    /**
     * 每天单模最大硫化时间 --单位到秒
     */
    private BigDecimal dayMaxCuringTime;
    /**
     * 月份最大天数
     */
    private Integer maxDays;
    /**
     * 最大模具
     */
    private Set<String> maxMouldSet;
    /**
     * 换规格损耗时间
     */
    private BigDecimal changeProductConsumeTime;
    /**
     * 排产周期开始日
     */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private Date productionStartDate;

    /**
     * 月份排产最大结束日
     */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @ApiModelProperty(value = "月份排产最大结束日", name = "productionEndDate")
    private Date productionEndDate;
    /**
     * 增加的日期数
     */
    private Integer addDays;

}