package com.zlt.mix.schedule.engine.vo;

import java.math.BigDecimal;
import java.util.Date;

import com.zlt.mix.schedule.engine.constants.GlueEngineConstants;

import lombok.Data;

/**
 * 胶料排程机台生产信息
 * 
 * @author hakimryan
 *
 */
@Data
public class GlueScheduleMachineProductVo {
	/**
	 * 机台编号
	 */
	private String machineCode;

	/**
	 * 机台状态
	 */
	private String state;

	/**
	 * 开始生产时间点
	 */
	private Date startProductTime;

	/**
	 * 中班生产时长
	 */
	private BigDecimal midProductTime;

	/**
	 * 夜班生产时长
	 */
	private BigDecimal nightProductTime;

	/**
	 * 白班生产时长
	 */
	private BigDecimal dayProductTime;

	/**
	 * 中班状态
	 */
	private Boolean midStatus = true;

	/**
	 * 夜班状态
	 */
	private Boolean nightStatus = true;

	/**
	 * 白班状态
	 */
	private Boolean dayStatus = true;

	/**
	 * 获取指定班次的机台状态
	 * 
	 * @param shiftClass 班次
	 * @return
	 */
	public Boolean getStatus(Integer shiftClass) {
		switch (shiftClass) {
		case GlueEngineConstants.SHIFT_CLASS_MID:
			return midStatus;
		case GlueEngineConstants.SHIFT_CLASS_NIGHT:
			return nightStatus;
		default:
			return dayStatus;
		}
	}

	/**
	 * 根据班次获取指定班次的生产时长
	 * 
	 * @param shiftClass 班次
	 * @return
	 */
	public BigDecimal getProductTime(Integer shiftClass) {
		switch (shiftClass) {
		case GlueEngineConstants.SHIFT_CLASS_MID:
			return midProductTime;
		case GlueEngineConstants.SHIFT_CLASS_NIGHT:
			return nightProductTime;
		default:
			return dayProductTime;
		}
	}

	/**
	 * 根据班次更新指定班次的生产时长
	 * 
	 * @param productTime 生产时长
	 * @param shiftClass  班次
	 * @return
	 */
	public void updateProductTime(BigDecimal productTime, Integer shiftClass) {
		switch (shiftClass) {
		case GlueEngineConstants.SHIFT_CLASS_MID:
			midProductTime = productTime;
			break;
		case GlueEngineConstants.SHIFT_CLASS_NIGHT:
			nightProductTime = productTime;
			break;
		default:
			dayProductTime = productTime;
			break;
		}
	}
}
