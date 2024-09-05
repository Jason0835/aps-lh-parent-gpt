package com.zlt.aps.cd15.engine.vo;

import lombok.Data;

/**
 * 15度裁断定点机台表
 * 
 * @Description
 * @Author hakimrayn
 * @Date 2021-7-11 10:10:58
 * @Version 1.0
 */
@Data
public class Cd15SpecifyMachineVo {
	/**
	 * 钢带代码
	 */
	private String steelStripCode;

	/**
	 * 机台id（对应T_cd15_MACHINE_INFO表id）
	 */
	private String machineId;
}
