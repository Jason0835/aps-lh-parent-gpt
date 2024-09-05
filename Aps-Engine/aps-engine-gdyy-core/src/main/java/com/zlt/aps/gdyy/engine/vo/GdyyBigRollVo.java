package com.zlt.aps.gdyy.engine.vo;

import java.math.BigDecimal;

import lombok.Data;

/**
 * 钢带大卷信息
 * @Description 
 * @Author hakimrayn
 * @Date 2021-7-19 10:12:19
 * @Version 1.0
 */
@Data
public class GdyyBigRollVo {
	/**
	 * 大卷编号
	 */
	private String bigRollCode;
	/**
	 * 布卷长度
	 */
	private BigDecimal clothLength;
}
