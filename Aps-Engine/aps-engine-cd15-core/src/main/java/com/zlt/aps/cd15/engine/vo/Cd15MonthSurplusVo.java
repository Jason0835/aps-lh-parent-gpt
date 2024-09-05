package com.zlt.aps.cd15.engine.vo;

import lombok.Data;

/**
 * 15度裁断月度汇总VO
 * 
 * @Description
 * @Author hakimrayn
 * @Date 2021-7-12 11:36:30
 * @Version 1.0
 */
@Data
public class Cd15MonthSurplusVo {

	/**
	 * 物料代码
	 */
	private String materialCode;
	
	/**
	 * 月度计划量
	 */
	private Double MONTH_PLAN_QTY;

	/**
	 * 月度完成量
	 */
	private Double monthFinishQty;

	/**
	 * 月度剩余量
	 */
	private Double monthRemainQty;
}
