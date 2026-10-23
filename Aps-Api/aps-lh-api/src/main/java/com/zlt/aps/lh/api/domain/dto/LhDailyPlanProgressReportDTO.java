package com.zlt.aps.lh.api.domain.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

/**
 * 日排产进度报表查询条件。
 *
 * <p>前端在硫化排程页面点击「日排产进度报表」时传入工厂与排程日期，
 * 后端按该排程日期的硫化日计划（class6/7/8 即 T+2）生成进度报表 Excel。</p>
 *
 * @author APS
 */
@Data
@ApiModel(value = "日排产进度报表查询条件", description = "日排产进度报表导出查询条件")
public class LhDailyPlanProgressReportDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 分厂编码（为空时取系统默认工厂）
     */
    @ApiModelProperty(value = "分厂编码", name = "factoryCode")
    private String factoryCode;

    /**
     * 排程日期（业务口径 T+1，格式 yyyy-MM-dd）
     */
    @ApiModelProperty(value = "排程日期（业务口径 T+1，格式 yyyy-MM-dd）", name = "scheduleDate")
    private String scheduleDate;
}
