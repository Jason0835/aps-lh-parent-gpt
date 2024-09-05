package com.zlt.aps.cd15.engine.vo;

import java.math.BigDecimal;

import lombok.Data;

/**
 * 钢带大卷信息
 * @Description 
 * @Author hakimrayn
 * @Date 2021-7-12 10:12:19
 * @Version 1.0
 */
@Data
public class Cd15BigRollVo {
	/**
	 * 钢带大卷
	 */
	private String bigRollCode;
	/**
	 * 布卷长度
	 */
	private BigDecimal clothLength;
}
