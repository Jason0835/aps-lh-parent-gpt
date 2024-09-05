package com.zlt.aps.mps.domain;

import java.math.BigDecimal;
import java.util.Date;

import lombok.Data;

/**
 * 硫化外胎施工信息
 * @Description
 */
@Data
public class LhTireConstructionInfo {
	/**
	 * 外胎SAP品号
	 */
	private String sapCode;
	/**
	 * 胎胚代码(代码)施工号
	 */
	private String embryoCode;
	/**
	 * 胎胚版本
	 */
	private String embryoVersion;
	/**
	 * 规格描述
	 */
	private String specDesc;
	/**
	 * 合模压力
	 */
	private BigDecimal clampingPressure;
	/**
	 * 硫化时间
	 */
	private BigDecimal curingTime;
	/**
	 * 删除标识（0未删除；1已删除）
	 */
	private String delFlag;
	/**
	 * 更新时间
	 */
	private Date updateTime;
	
}
