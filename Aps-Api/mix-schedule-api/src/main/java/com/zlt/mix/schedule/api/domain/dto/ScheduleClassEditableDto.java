package com.zlt.mix.schedule.api.domain.dto;

import java.util.Date;

import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.Data;

/**
 * 排产班次可编辑状态DTO
 * 
 * @author hakimryan
 *
 */
@Data
public class ScheduleClassEditableDto {
	/**
	 * 排产日
	 */
	@JsonFormat(pattern = "yyyy-MM-dd HH:mm", timezone = "GMT+8")
	private Date scheduleDate;
	/**
	 * 班次
	 */
	private Integer classShift;
	/**
	 * 中班可编辑状态
	 */
	private boolean midEditable;
	/**
	 * 夜班可编辑状态
	 */
	private boolean nightEditable;
	/**
	 * 吧白班可编辑状态
	 */
	private boolean dayEditable;
}
