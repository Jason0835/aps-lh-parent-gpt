package com.zlt.aps.cd90.engine.vo;

import lombok.Data;

/**
 * 90度裁断帘线大卷与机台的映射表
 * 
 * @Description
 * @Author hakimrayn
 * @Date 2021-7-14 10:10:58
 * @Version 1.0
 */
@Data
public class Cd90MachineRollMappingVo {
	/**
	 * 帘线大卷编号
	 */
	private String bigRollCode;

	/**
	 * 机台id（对应T_CD15_MACHINE_INFO表id）
	 */
	private String machineId;
}
