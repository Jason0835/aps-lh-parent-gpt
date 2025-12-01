package com.zlt.mix.schedule.engine.vo;

import java.math.BigDecimal;

import lombok.Data;

/**
 * 日返回胶库存
 * 
 * @author hakimryan
 *
 */
@Data
public class DailyReturnGlueStockVo {
	/**
	 * 胶料号
	 */
	private String glue;
	/**
	 * 返回胶重量
	 */
	private BigDecimal stockWeight;
	/**
	 * 夜班库存量
	 */
	private BigDecimal nightStock;
	/**
	 * 白班库存库存量
	 */
	private BigDecimal dayStock;

	public DailyReturnGlueStockVo(String glue, BigDecimal stockWeight, BigDecimal nightStock, BigDecimal dayStock) {
		super();
		this.glue = glue;
		this.stockWeight = stockWeight;
		this.nightStock = nightStock;
		this.dayStock = dayStock;
	}
}
