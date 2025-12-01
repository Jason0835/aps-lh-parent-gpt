package com.zlt.mix.schedule.engine.vo;

import com.zlt.mix.common.core.domain.ZltBaseEntity;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 排程发布日志明细
 * 
 * @author hakimryan
 *
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class SchedulePublishLogVo extends ZltBaseEntity {
	private static final long serialVersionUID = -5712336572505880689L;
	/**
	 * 发布记录主表ID
	 */
	private Long recordId;
	/**
	 * 排产日期
	 */
	private String scheduleDate;
	/**
	 * 工单号
	 */
	private String orderNo;
	/**
	 * 机台号
	 */
	private String machineCode;
	/**
	 * 物料号
	 */
	private String recipeMaterialCode;
	/**
	 * 配方版本号
	 */
	private Integer recipeVersionId;
	/**
	 * 配方类型
	 */
	private Integer recipeType;
	/**
	 * 计划量
	 */
	private Integer planQty;
	/**
	 * 班别
	 */
	private Integer shiftClassId;
	/**
	 * 排产顺序
	 */
	private Integer produceOrder;
	/**
	 * 接口执行结果
	 */
	private String invokeResult;
	/**
	 * 发布结果，1=成功，0=失败
	 */
	private String publishResult;
	/**
	 * 密炼区
	 */
	private String mixArea;
}
