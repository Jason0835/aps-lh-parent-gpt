package com.zlt.mix.schedule.engine.vo;

import com.zlt.mix.common.core.utils.BigDecimalUtil;
import com.zlt.mix.schedule.engine.constants.GlueEngineConstants;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;
import java.util.Optional;

/**
 * 单班胶料排程值对象
 * 
 * @author hakimryan
 *
 */
@Data
public class SingleClassGlueScheduleResultVO {

	public SingleClassGlueScheduleResultVO() {

	}

	public SingleClassGlueScheduleResultVO(GlueScheduleResultVo scheduleResult, int shiftClass) {
		this.setScheduleResult(scheduleResult);
		this.setShiftClass(shiftClass);
		this.setMachineCode(scheduleResult.getMachineCode());
		this.setScheduleDate(scheduleResult.getScheduleDate());
		this.setGlue(scheduleResult.getGlue());
		switch (shiftClass) {
		case GlueEngineConstants.SHIFT_CLASS_MID:
			this.setProduceOrder(scheduleResult.getMidProduceOrder());
			this.setPlanQty(Optional.ofNullable(scheduleResult.getMidPlanQty())
					.map(planQty -> new BigDecimal(planQty.toString())).orElse(BigDecimal.ZERO));
			this.setExpectStartTime(scheduleResult.getMidExpectStartTime());
			this.setExpectFinishTime(scheduleResult.getMidExpectFinishTime());
			break;
		case GlueEngineConstants.SHIFT_CLASS_NIGHT:
			this.setProduceOrder(scheduleResult.getNightProduceOrder());
			this.setPlanQty(Optional.ofNullable(scheduleResult.getNightPlanQty())
					.map(planQty -> new BigDecimal(planQty.toString())).orElse(BigDecimal.ZERO));
			this.setExpectStartTime(scheduleResult.getNightExpectStartTime());
			this.setExpectFinishTime(scheduleResult.getNightExpectFinishTime());
			break;
		default:
			this.setProduceOrder(scheduleResult.getDayProduceOrder());
			this.setPlanQty(Optional.ofNullable(scheduleResult.getDayPlanQty())
					.map(planQty -> new BigDecimal(planQty.toString())).orElse(BigDecimal.ZERO));
			this.setExpectStartTime(scheduleResult.getDayExpectStartTime());
			this.setExpectFinishTime(scheduleResult.getDayExpectFinishTime());
			break;
		}
	}

	/**
	 * 排产日
	 */
	private Date scheduleDate;
	/**
	 * 机台
	 */
	private String machineCode;
	/**
	 * 胶料号
	 */
	private String glue;
	/**
	 * 班次
	 */
	private int shiftClass;
	/**
	 * 计划量
	 */
	private BigDecimal planQty;
	/**
	 * 预计开始生产时间
	 */
	private Date expectStartTime;
	/**
	 * 预计完成生产时间
	 */
	private Date expectFinishTime;
	/**
	 * 生产顺序
	 */
	private Integer produceOrder;
	/**
	 * 对应的生产排程记录
	 */
	private GlueScheduleResultVo scheduleResult;

	/**
	 * 根据班次更新预计日期相关信息
	 * 
	 */
	public void updateExpectTime() {
		GlueScheduleResultVo scheduleResult = this.getScheduleResult();
		int shiftClass = this.getShiftClass();
		Integer productOrder = this.getProduceOrder();
		// 更新对应班次的字段值
		switch (shiftClass) {
		case GlueEngineConstants.SHIFT_CLASS_MID:
			scheduleResult.setMidExpectStartTime(this.getExpectStartTime());
			scheduleResult.setMidExpectFinishTime(this.getExpectFinishTime());
			scheduleResult.setMidProduceOrder(productOrder);
			break;
		case GlueEngineConstants.SHIFT_CLASS_NIGHT:
			scheduleResult.setNightExpectStartTime(this.getExpectStartTime());
			scheduleResult.setNightExpectFinishTime(this.getExpectFinishTime());
			scheduleResult.setNightProduceOrder(productOrder);
			break;
		default:
			scheduleResult.setDayExpectStartTime(this.getExpectStartTime());
			scheduleResult.setDayExpectFinishTime(this.getExpectFinishTime());
			scheduleResult.setDayProduceOrder(productOrder);
			break;
		}
	}

	/**
	 * 根据班次更新计划量
	 * 
	 */
	public void updatePlanQty() {
		GlueScheduleResultVo scheduleResult = this.getScheduleResult();
		int shiftClass = this.getShiftClass();
		BigDecimal planQty = this.getPlanQty();
		// 更新对应班次的字段值
		switch (shiftClass) {
		case GlueEngineConstants.SHIFT_CLASS_MID:
			scheduleResult.setMidPlanQty(planQty.doubleValue());
			break;
		case GlueEngineConstants.SHIFT_CLASS_NIGHT:
			scheduleResult.setNightPlanQty(planQty.doubleValue());
			break;
		default:
			scheduleResult.setDayPlanQty(planQty.doubleValue());
			break;
		}
		// 总计划量重算 = 中班计划量 + 夜班计划量 + 白班计划量
		Double totalPlanQty = BigDecimalUtil.add(scheduleResult.getMidPlanQty(), scheduleResult.getNightPlanQty(),
				scheduleResult.getDayPlanQty());
		scheduleResult.setTotalPlanQty(totalPlanQty);

	}
}
