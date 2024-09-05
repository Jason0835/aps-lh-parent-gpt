package com.zlt.aps.gdyy.engine.vo;

import java.math.BigDecimal;

import lombok.Data;

/**
 * 钢压大卷库存值对象
 * @Description 
 * @Author hakimrayn
 * @Date 2021-7-19 11:23:38
 * @Version 1.0
 */
@Data
public class GdyyStockVo {
	/**
	 * 钢压大卷编号
	 */
	private String bigRollCode;
	/**
	 * 库存量
	 */
	private BigDecimal stockQty;
	/**
	 * 大卷库存量
	 */
	private BigDecimal stockRollQty;
}
