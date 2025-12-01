package com.zlt.aps.cxlh.cx.api.domain.vo;

import lombok.Data;

/**
 * 施工表映射表
 * 
 * @Description
 * @Author hakimryan
 */
@Data
public class TMesConstructionMapping {
	/**
	 * 列名
	 */
	private String columnName;
	/**
	 * 验证正则表达式
	 */
	private String regularExpression;
	/**
	 * 错误提示
	 */
	private String errorTips;
}
