package com.zlt.aps.lh.api.domain.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 排产小结报表查询VO
 *
 * <p>用于硫化日计划页面的排产小结导出功能，
 * 传入排程日期和分厂编码作为查询条件。</p>
 *
 * @author APS Team
 */
@Data
@ApiModel(value = "排产小结报表查询VO", description = "排产小结报表导出查询条件")
public class ScheduleSummaryReportVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "排程日期")
    private String scheduleDate;

    @ApiModelProperty(value = "分厂编码")
    private String factoryCode;
}
