package com.zlt.mix.schedule.engine.vo;

import java.math.BigDecimal;

import lombok.Data;
/**
 * 分厂需求量值对象
 * @author hakimryan
 *
 */
@Data
public class GlueFactoryRequireVo {
	/**
	 * 胶料
	 */
	private String glue;
	
	/**
	 * 分厂需求量差值
	 */
	private BigDecimal requireDifference;
	
	/**
	 * 分厂需求班次
	 */
	private Integer requireClass;
	
	/**
	 * 是否下级胶料也有需求
	 */
	private boolean requireChild = false;
}
