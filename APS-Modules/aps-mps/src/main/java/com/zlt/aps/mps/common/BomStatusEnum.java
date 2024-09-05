package com.zlt.aps.mps.common;

/**
 * bom数据状态
 * 
 * @Description
 * @Author hakimryan
 * @Date 2021-10-13 10:20:13
 */
public enum BomStatusEnum {
	/**
	 * 启用
	 */
	ENABLE("1"),
	/**
	 * 废止
	 */
	DISABLE("3");

	private String code;

	private BomStatusEnum(String code) {
		this.code = code;
	}

	public String getCode() {
		return code;
	}
}
