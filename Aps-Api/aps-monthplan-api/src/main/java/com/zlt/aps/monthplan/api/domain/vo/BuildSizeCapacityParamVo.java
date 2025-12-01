package com.zlt.aps.monthplan.api.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.Date;

/**
 * 生成寸口产能配置参数对象
 *
 * @author ZLT
 * @date 20250710
 */
@Data
@ApiModel(value = "生成寸口产能配置参数对象", description = "生成寸口产能配置参数对象")
public class BuildSizeCapacityParamVo implements Serializable {
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
    @ApiModelProperty(required = true, value = "月生产计划需求版本", name = "monthPlanVersion")
    private String monthPlanVersion;

    /**
     * 成型日期
     */
    @NotNull
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    @ApiModelProperty(required = true, value = "成型日期", name = "formingDate")
    private Date formingDate;
}
