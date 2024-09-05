package com.zlt.aps.cd15.engine.vo;

/**
 * 排产均衡值对象
 * @Description 
 * @Author hakimrayn
 * @Date 2021-7-10 13:04:01
 * @Version 1.0
 */

import java.math.BigDecimal;

import lombok.Data;

@Data
public class Cd15EquilibriumVo {
	/**
	 * 中班计划总计值
	 */
	private BigDecimal dayPlanQty;
	/**
	 * 晚班计划总计值
	 */
	private BigDecimal nightPlanQty;
	/**
	 * 差异率
	 */
	private BigDecimal differenceRate;
}
