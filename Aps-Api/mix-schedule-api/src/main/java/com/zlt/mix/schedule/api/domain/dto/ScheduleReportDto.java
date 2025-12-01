package com.zlt.mix.schedule.api.domain.dto;

import java.util.Date;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.common.core.annotation.Excel;
import com.ruoyi.common.core.annotation.Excel.Align;
import com.zlt.mix.common.core.constant.ZltConstant;

import lombok.Data;

/**
 * 排产日报表dto
 * 
 * @author hakimryan
 *
 */
@Data
public class ScheduleReportDto {
	/**
	 * 排产日期
	 */
    @Excel(name = "schedule.scheduleReport.scheduleDate")
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
	 * 计划规格
	 */
    @Excel(name = "schedule.scheduleReport.planSpec", align = Align.LEFT, width = 25)
	private String planSpec;

	/**
	 * 计划量
	 */
    @Excel(name = "schedule.scheduleReport.planQty")
	private Double planQty;

	/**
	 * 计完成规格
	 */
    @Excel(name = "schedule.scheduleReport.finishSpec", align = Align.LEFT, width = 25)
	private String finishSpec;

	/**
	 * 完成量
	 */
    @Excel(name = "schedule.scheduleReport.finishQty")
	private Double finishQty;

	/**
	 * 规格完成率
	 */
    @Excel(name = "schedule.scheduleReport.specRate")
	private String specRate;

	/**
	 * 计划完成率
	 */
    @Excel(name = "schedule.scheduleReport.qtyFinishRate")
	private String qtyFinishRate;
	
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
	 * 是否汇总，0明细行，1汇总行
	 */
	private String isSummary = ZltConstant.STATUS_ENABLE;

	
	/**
	 * 规格是否展示数字，0非数字，1数字（居中显示）
	 */
	private String isSpecNumber = ZltConstant.STATUS_ENABLE;
}
