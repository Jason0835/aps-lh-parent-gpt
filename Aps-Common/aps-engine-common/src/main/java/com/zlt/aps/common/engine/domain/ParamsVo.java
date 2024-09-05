package com.zlt.aps.common.engine.domain;

import lombok.Data;

/**
 * 纤维压延系统参数值对象
 */
@Data
public class ParamsVo {
	/**
	 * 系统参数code
	 */
	private String paramCode;
	/**
	 * 系统参数值
	 */
	private String paramValue;
}
