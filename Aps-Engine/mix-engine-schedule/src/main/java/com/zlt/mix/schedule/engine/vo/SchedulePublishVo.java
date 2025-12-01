package com.zlt.mix.schedule.engine.vo;

import java.util.Date;

import lombok.Data;

/**
 * 排程发布值对象
 * 
 * @author hakimryan
 *
 */
@Data
public class SchedulePublishVo {
	/**
	 * 主键
	 */
	Long id;
	/**
	 * 密炼区
	 */
	String mixArea;
	/**
	 * 排产日
	 */
	Date scheduleDate;
	/**
	 * 中班计划量
	 */
	Double midPlanQty;
	/**
	 * 晚班计划量
	 */
	Double nightPlanQty;
	/**
	 * 白班计划量
	 */
	Double dayPlanQty;
	/**
	 * 中班排产次序
	 */
	Integer midProduceOrder;
	/**
	 * 晚班排产次序
	 */
	Integer nightProduceOrder;
	/**
	 * 白班排产次序
	 */
	Integer dayProduceOrder;
	/**
	 * 机台号
	 */
	String machineCode;
	/**
	 * 物料号
	 */
	String recipeMaterialCode;
	/**
	 * 物料名称
	 */
	String materialName;
	/**
	 * 配方类型
	 */
	String recipeType;
	/**
	 * 配方版本
	 */
	String recipeVersionId;
	/**
	 * 工单号
	 */
	String orderNo;
	/**
	 * 发布状态
	 */
	String releaseStatus;
	/**
	 * 最新发布时间
	 */
	Date newestPublishTime;
	/**
	 * 发布成功次数
	 */
	Integer publishSuccessCount;
	/**
	 * 中班发布状态，默认未发布成功
	 */
	String midPublishStatus;
	/**
	 * 中班发布状态
	 */
	String nightPublishStatus;
	/**
	 * 中班发布状态
	 */
	String dayPublishStatus;
}
