package com.zlt.aps.cd90.engine.vo;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 排产均衡值对象
 * @Description 
 * @Author hakimrayn
 * @Date 2021-7-14 11:12:19
 * @Version 1.0
 */
@Data
public class Cd90EquilibriumVo {
	/**
	 * 中班计划总计值
	 */
	private BigDecimal dayPlanQty;
	/**
	 * 晚班计划总计值
	 */
	private BigDecimal nightPlanQty;
	/**
     * 次日早班计划总计值
     */
    private BigDecimal nextDayPlanQty;
    /**
	 * 差异率
	 */
	private BigDecimal differenceRate;
}
