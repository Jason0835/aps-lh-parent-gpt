package com.zlt.mix.schedule.engine.vo;

import lombok.Data;

/**
 * 配方与机台对应值对象
 * 
 * @author hakimryan
 *
 */
@Data
public class FormulaMachineVo {
	/**
	 * 密炼区
	 */
	private String mixArea;
	/**
	 * 胶料名称
	 */
	private String glue;
	/**
	 * 生产机台编号
	 */
	private String machineCode;
	/**
	 * 机台顺序
	 */
	private Integer machineOrder;
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
}
