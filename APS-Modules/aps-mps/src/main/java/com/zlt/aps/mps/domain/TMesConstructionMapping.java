package com.zlt.aps.mps.domain;

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
	
	private String constructionColumn;
	
	private String parentMaterialNameCode;
	
	private String childMaterialNameCode;
	
	private String paramCode;
	
	private String sourceColumn;
	
	private String sortCode;
	/**
	 * 验证正则表达式
	 */
	private String regularExpression;
	/**
	 * 错误提示
	 */
	private String errorTips;
}
