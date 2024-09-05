package com.zlt.aps.xwyy.engine.vo;

import lombok.Data;

/**
 * 纤维压延日用参考值对象
 * 
 * @Description
 * @Author hakimryan
 * @Date 2021-8-13 13:37:03
 */
@Data
public class XwyyDayUsedVo {
	/**
	 * 大卷编号
	 */
	private String bigRollCode;
	/**
	 * 日用参考
	 */
	private Double dayUsed;
}
