package com.zlt.aps.cd90.engine.vo;

import lombok.Data;

/**
 * 钢带压延系统参数值对象
 * @Description 
 * @Author hakimrayn
 * @Date 2021-7-27 15:19:26
 * @Version 1.0
 */
@Data
public class Cd90ParamsVo {
	/**
	 * 系统参数code
	 */
	private String paramCode;
	/**
	 * 系统参数值
	 */
	private String paramValue;
}
