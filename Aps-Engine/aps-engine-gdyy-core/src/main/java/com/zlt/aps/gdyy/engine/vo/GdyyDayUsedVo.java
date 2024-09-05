package com.zlt.aps.gdyy.engine.vo;

import lombok.Data;

/**
 * 钢带压延日用参考值对象
 * 
 * @Description
 * @Author hakimryan
 * @Date 2021-8-13 16:37:03
 */
@Data
public class GdyyDayUsedVo {
	/**
	 * 大卷编号
	 */
	private String bigRollCode;
	/**
	 * 日用参考
	 */
	private Double dayUsed;
}
