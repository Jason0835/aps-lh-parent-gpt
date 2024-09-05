package com.zlt.aps.cd15.engine.vo;

import lombok.Data;

/**
 * 15度裁断钢压大卷与机台的映射表值对象
 * 
 * @Description
 * @Author hakimrayn
 * @Date 2021-7-11 10:10:58
 * @Version 1.0
 */
@Data
public class Cd15MachineRollMappingVo {
	/**
	 * 钢压大卷编号
	 */
	private String bigRollCode;

	/**
	 * 机台id，如果有多个用“,”分隔
	 */
	private String machineId;
}
