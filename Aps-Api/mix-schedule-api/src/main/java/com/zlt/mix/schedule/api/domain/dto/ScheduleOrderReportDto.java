package com.zlt.mix.schedule.api.domain.dto;

import java.util.Date;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.common.core.annotation.Excel;
import com.ruoyi.common.core.annotation.Excel.Align;

import lombok.Data;

/**
 * 各工序工单完成统计报表dto
 * 
 * @author hakimryan
 *
 */
@Data
public class ScheduleOrderReportDto {
	/**
	 * 排产日期
	 */
	@Excel(name = "schedule.scheduleReport.scheduleDate", width = 25)
	private String scheduleDate;

	/**
	 * 密炼区
	 */
	@Excel(name = "schedule.scheduleReport.mixArea", dictType = "MIX_AREA")
	private String mixArea;

	/**
	 * 工序（物料类型）
	 */
	@Excel(name = "schedule.scheduleReport.procedure", dictType = "REPORT_PROCEDURE_CODE")
	private String procedure;

	/**
	 * 工单数量
	 */
	@Excel(name = "schedule.ScheduleOrderReport.orderQty")
	private Integer orderQty;

	/**
	 * 物料规格
	 */
	@Excel(name = "schedule.ScheduleOrderReport.planSpec", align = Align.LEFT, width = 25)
	private String planSpec;

	/**
	 * 计划生产量
	 */
	@Excel(name = "schedule.ScheduleOrderReport.planQty")
	private Double planQty;

	/**
	 * 实际完成量
	 */
	@Excel(name = "schedule.ScheduleOrderReport.finishQty")
	private Double finishQty;

	/**
	 * 完成率
	 */
	@Excel(name = "schedule.ScheduleOrderReport.finishRate")
	private String finishRate;

	/**
	 * 查询区间开始时间
	 */
	@JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
	private Date queryStartDate;

	/**
	 * 查询区间结束时间
	 */
	@JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
	private Date queryEndDate;

	/**
	 * 查询汇总方式
	 */
	private String summaryType;
}
