package com.zlt.mix.schedule.api.domain.dto;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

/**
 * 硫磺辅料日计划机台统计
 * @author Liam
 * @date 2022-07-18
 */
@Data
public class MaterialScheduleResultStatisticsDto implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "机台名称")
    private String machineName;

    @ApiModelProperty(value = "班制", position = 140)
    private Double classShift;

    @ApiModelProperty(value = "需求量合计", position = 150)
    private Double demandQtyStatistics;

    @ApiModelProperty(value = "总计划合计", position = 150)
    private Double totalPlanQtyStatistics;

    @ApiModelProperty(value = "中班计划量合计")
    private Double midPlanQtyStatistics;

    @ApiModelProperty(value = "夜班计划量合计")
    private Double nightPlanQtyStatistics;

    @ApiModelProperty(value = "白班计划量合计")
    private Double dayPlanQtyStatistics;
}
