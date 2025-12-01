package com.zlt.mix.schedule.engine.vo;

import java.math.BigDecimal;

import lombok.Data;

/**
 * 胶料待支领量
 * 
 * @author hakimryan
 *
 */
@Data
public class GlueUnclaimed {
	/**
	 * 密炼区
	 */
	private String mixArea;
	/**
	 * 排产日期
	 */
	private String scheduleDate;
	/**
	 * 胶料
	 */
	private String glue;
	/**
	 * 待支领量1 = 胶料汇总 - MES中夜班已支领量
	 */
	private BigDecimal shelfNum1;
	/**
	 * 待支领量2，导入的数据
	 */
	private BigDecimal shelfNum2;
}
