package com.zlt.aps.mps.common;

/**
 * 完成量班次
 */
public enum FinishClassEnum {
	CLASS1("CLASS1"),

	CLASS2("CLASS2"),

	CLASS3("CLASS3"),
	// 两班制白班
	CLASS3_2("CLASS3_2"),
	;

	private String code;

	private FinishClassEnum(String code) {
		this.code = code;
	}

	public String getCode() {
		return code;
	}
}
