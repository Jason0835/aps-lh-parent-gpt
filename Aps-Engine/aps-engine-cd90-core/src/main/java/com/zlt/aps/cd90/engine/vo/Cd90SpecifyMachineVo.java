package com.zlt.aps.cd90.engine.vo;

import lombok.Data;

/**
 * 90度裁断定点机台表
 * 
 * @Description
 * @Author hakimrayn
 * @Date 2021-7-14 10:10:58
 * @Version 1.0
 */
@Data
public class Cd90SpecifyMachineVo {
	/**
	 * 帘布代码
	 */
	private String clothCode;

	/**
	 * 机台id（对应T_cd15_MACHINE_INFO表id）
	 */
	private String machineId;
}
