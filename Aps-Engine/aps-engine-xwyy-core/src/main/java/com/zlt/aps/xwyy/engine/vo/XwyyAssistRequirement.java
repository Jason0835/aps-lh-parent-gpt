package com.zlt.aps.xwyy.engine.vo;

import java.math.BigDecimal;

import lombok.Data;

/**
 * 纤维压延外厂需求
 * 
 * @Description
 * @Author zlt
 * @Date 2022-3-15 9:15:55
 */
@Data
public class XwyyAssistRequirement {
	/**
	 * 大卷编号
	 */
	private String bigRollCode;
	/**
	 * 中班计划量
	 */
	private BigDecimal dayPlanQty;
	/**
	 * 夜班计划量
	 */
	private BigDecimal nightPlanQty;
	/**
	 * 当日库存
	 */
	private BigDecimal todayStock;

	/**
	 * 白班外厂应支
	 */
	private BigDecimal dayOut;

	/**
	 * 5厂中班计划量
	 */
	private BigDecimal fac5Class1Plan;

	/**
	 * 5厂晚班计划量
	 */
	private BigDecimal fac5Class2Plan;

	/**
	 * 5厂白班计划量
	 */
	private BigDecimal fac5Class3Plan;
	
	/**
	 * 原线代码
	 */
	private String originalLineCode;
}
