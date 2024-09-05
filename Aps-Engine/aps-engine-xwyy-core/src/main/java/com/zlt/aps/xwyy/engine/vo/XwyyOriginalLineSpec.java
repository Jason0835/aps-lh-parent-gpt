package com.zlt.aps.xwyy.engine.vo;

import java.math.BigDecimal;

import lombok.Data;

/**
 * 纤维压延原线规格
 * 
 * @Description
 * @Author zlt
 * @Date 2022-3-15 9:02:12
 */
@Data
public class XwyyOriginalLineSpec {
	/**
	 * 原线规格
	 */
	private String originalLineCode;
	/**
	 * 原线长度
	 */
	private BigDecimal originalLineLength;
	/**
	 * 可破大卷数
	 */
	private Long breakRollNum;
}
