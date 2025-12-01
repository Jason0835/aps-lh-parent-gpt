package com.zlt.mix.schedule.api.domain.dto;

import java.util.Date;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.common.core.annotation.Excel;
import com.ruoyi.common.core.annotation.Excel.Align;
import com.zlt.mix.common.core.constant.ZltConstant;

import lombok.Data;

/**
 * 排产班次报表dto
 * 
 * @author hakimryan
 *
 */
@Data
public class ScheduleClassesReportDto {
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
	 * 中班计划规格
	 */
    @Excel(name = "schedule.scheduleClassesReport.mid.planSpec", align = Align.LEFT, width = 25)
	private String midPlanSpec;

	/**
	 * 中班计划量
	 */
    @Excel(name = "schedule.scheduleClassesReport.mid.planQty")
	private Double midPlanQty;

	/**
	 * 中班计完成规格
	 */
    @Excel(name = "schedule.scheduleClassesReport.mid.finishSpec", align = Align.LEFT, width = 25)
	private String midFinishSpec;

	/**
	 * 中班完成量
	 */
    @Excel(name = "schedule.scheduleClassesReport.mid.finishQty")
	private Double midFinishQty;

	/**
	 * 夜班计划规格
	 */
    @Excel(name = "schedule.scheduleClassesReport.night.planSpec", align = Align.LEFT, width = 25)
	private String nightPlanSpec;

	/**
	 * 夜班计划量
	 */
    @Excel(name = "schedule.scheduleClassesReport.night.planQty")
	private Double nightPlanQty;

	/**
	 * 夜班计完成规格
	 */
    @Excel(name = "schedule.scheduleClassesReport.night.finishSpec", align = Align.LEFT, width = 25)
	private String nightFinishSpec;

	/**
	 * 夜班完成量
	 */
    @Excel(name = "schedule.scheduleClassesReport.night.finishQty")
	private Double nightFinishQty;

	/**
	 * 白班计划规格
	 */
    @Excel(name = "schedule.scheduleClassesReport.day.planSpec", align = Align.LEFT, width = 25)
	private String dayPlanSpec;

	/**
	 * 白班计划量
	 */
    @Excel(name = "schedule.scheduleClassesReport.day.planQty")
	private Double dayPlanQty;

	/**
	 * 白班计完成规格
	 */
    @Excel(name = "schedule.scheduleClassesReport.day.finishSpec", align = Align.LEFT, width = 25)
	private String dayFinishSpec;

	/**
	 * 白班完成量
	 */
    @Excel(name = "schedule.scheduleClassesReport.day.finishQty")
	private Double dayFinishQty;
    
    /**
     * 查询日期
     */
	@JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
	private Date queryScheduleDate;

	/**
	 * 是否汇总，0明细行，1汇总行
	 */
	private String isSummary = ZltConstant.STATUS_ENABLE;

	
	/**
	 * 规格是否展示数字，0非数字，1数字（居中显示）
	 */
	private String isSpecNumber = ZltConstant.STATUS_ENABLE;
}
