package com.zlt.aps.cd90.engine.vo;

import java.math.BigDecimal;

import lombok.Data;

/**
 * 帘线大卷信息
 * @Description 
 * @Author hakimrayn
 * @Date 2021-7-14 10:12:19
 * @Version 1.0
 */
@Data
public class Cd90BigRollVo {
	/**
	 * 大卷编号
	 */
	private String bigRollCode;
	/**
	 * 布卷长度
	 */
	private BigDecimal clothLength;
}
