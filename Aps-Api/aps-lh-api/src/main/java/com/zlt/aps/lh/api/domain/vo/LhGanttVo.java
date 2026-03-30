package com.zlt.aps.lh.api.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * @author Chen
 * @date 2025/6/30
 */
@Data
public class LhGanttVo implements Serializable {

    /**
     * 排程日期
     */
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    @ApiModelProperty(value = "排程日期")
    private Date scheduleDate;

    /**
     * 硫化机编号
     */
    @ApiModelProperty(value = "硫化机编号")
    private String lhMachineCode;

    /**
     * 规格描述
     */
    @ApiModelProperty(value = "规格描述")
    private String specDesc;

    /**
     * 规格代号
     */
    @ApiModelProperty(value = "规格代号")
    private String specCode;

    /**
     * 胎胚代码
     */
    @ApiModelProperty(value = "胎胚代码")
    private String embryoCode;

    /**
     * 排程开始时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    @ApiModelProperty(value = "排程开始时间")
    private Date startDate;

    /**
     * 排程结束时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    @ApiModelProperty(value = "排程结束时间")
    private Date endDate;

    /**
     * 计划数量
     */
    @ApiModelProperty(value = "计划数量")
    private Double planQty;

    /**
     * 完成数量
     */
    @ApiModelProperty(value = "完成数量")
    private Double finishQty;

    /**
     * 开始日
     */
    private String startDay;

    /**
     * 结束日
     */
    private String endDay;

    /**
     * 开始时间
     */
    private String startHour;

    /**
     * 结束时间
     */
    private String endHour;

    /**
     * 时差间隔
     */
    private int hourInterval;

    /**
     * 72小时制的起始时间
     */
    private int hourStart;

}
