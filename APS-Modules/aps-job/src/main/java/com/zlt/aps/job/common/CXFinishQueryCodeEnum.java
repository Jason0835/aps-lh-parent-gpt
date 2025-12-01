package com.zlt.aps.job.common;

/**
 * 成型完成量查询码
 * 
 * @Description
 * @Author zlt
 * @Date 2022-2-24 9:32:09
 */
public enum CXFinishQueryCodeEnum {
	CLASS1("CLASS1"), // 中班
	CLASS2("CLASS2"), // 晚班
	CLASS3("CLASS3"), // 白班
	CLASS3_2("CLASS3_2"), // 两班制白班
	TOTAL("TOTAL"),// 整天
	;

	private String code;

	private CXFinishQueryCodeEnum(String code) {
		this.code = code;
	}

	public String getCode() {
		return code;
	}

	public void setCode(String code) {
		this.code = code;
	}
}
