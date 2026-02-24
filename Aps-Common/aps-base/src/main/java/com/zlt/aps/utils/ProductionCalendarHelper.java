package com.zlt.aps.utils;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 停开工日历辅助类
 *
 * @author ZLT
 * @date 20250618
 */
@Data
public class ProductionCalendarHelper implements Serializable {
    /**
     * 分厂编号
     */
    @ApiModelProperty(value = "分厂编号", name = "factoryCode")
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
     * 停车开始日期
     */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @ApiModelProperty(value = "停车开始日期", name = "beginDate")
    private Date beginDate;

    /**
     * 停车结束日期
     */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @ApiModelProperty(value = "停车结束日期", name = "endDate")
    private Date endDate;
}
