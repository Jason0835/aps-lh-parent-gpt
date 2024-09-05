package com.zlt.aps.xwyy.engine.vo;

import lombok.Data;

/**
 * 纤维压延月度汇总VO
 * 
 * @Description
 * @Author hakimrayn
 * @Date 2021-7-22 11:36:30
 * @Version 1.0
 */
@Data
public class XwyyMonthSurplusVo {

	/**
	 * 物料代码
	 */
	private String materialCode;

	/**
	 * 月度计划完成量
	 */
	private Double monthFinishQty;

	/**
	 * 月度剩余量
	 */
	private Double monthRemainQty;


	/**
	 * 月度计划完成量（个）
	 */
	private Double monthFinishQty2;

	/**
	 * 月度剩余量（个）
	 */
	private Double monthRemainQty2;
}
