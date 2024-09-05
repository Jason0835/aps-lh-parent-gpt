package com.zlt.aps.mps.domain;

import lombok.Data;

/**
 * 月度计划状态值对象
 */
@Data
public class MonthSurplusStatusVo {

	/**
	 * 收尾提示标识(0:提示收尾；1:不需要提示)
	 */
	private String markCloseOutTip;

	/**
	 * 生产状态:0-未生产；1-生产中；2-生产完成
	 */
	private String productionStatus;

	/**
	 * 物料号
	 */
	private String materialCode;

	/**
	 * 工单
	 */
	private String orderNo;

	/**
	 * 是否修改，默认未修改
	 */
	private boolean isModify = false;
}
