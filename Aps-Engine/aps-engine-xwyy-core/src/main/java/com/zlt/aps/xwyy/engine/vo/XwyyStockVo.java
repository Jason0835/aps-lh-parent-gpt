package com.zlt.aps.xwyy.engine.vo;

import java.math.BigDecimal;

import lombok.Data;

/**
 * 帘线大卷库存值对象
 * 
 * @Description
 * @Author hakimrayn
 * @Date 2021-7-22 11:23:38
 * @Version 1.0
 */
@Data
public class XwyyStockVo {
	/**
	 * 大卷编号
	 */
	private String bigRollCode;
	/**
	 * 库存量
	 */
	private BigDecimal stockQty;
	/**
	 * 当日库存
	 */
	private BigDecimal todayStock;
	/**
	 * 成型机台编号，多个用逗号隔开
	 */
	private String cxMachineCode;
	/**
	 * 压延的单耗，用于计算可供成型时长
	 */
	private BigDecimal unitConsume;
	/**
	 * 成型定额
	 */
	private BigDecimal quotaQty;
	/**
	 * 成型16点预计消耗量
	 */
	private BigDecimal cxUseQty;
}
