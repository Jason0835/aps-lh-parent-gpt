package com.zlt.aps.cd15.engine.vo;

import java.math.BigDecimal;

import lombok.Data;

/**
 * 成型12点完成量值对象
 * @Description 
 * @Author hakimrayn
 * @Date 2021-7-13 11:23:38
 * @Version 1.0
 */
@Data
public class Cd15StockVo {
	/**
	 * 钢带编号
	 */
	private String beltCode;
	/**
	 * 成型机台编号，多个用逗号隔开
	 */
	private String cxMachineCode;
	/**
	 * 库存量
	 */
	private BigDecimal stockQty;
	/**
	 * 成型定额
	 */
	private BigDecimal quotaQty;
}
