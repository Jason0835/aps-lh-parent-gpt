package com.zlt.aps.gdyy.engine.vo;

import lombok.Data;

/**
 * 钢带压延帘线大卷与机台的映射表
 * 
 * @Description
 * @Author steve
 * @Date 2025-2-17 20:47:35
 * @Version 1.0
 */
@Data
public class GdyyMachineRollMappingVo {
	/**
	 * 钢压大卷编号
	 */
	private String bigRollCode;

	/**
	 * 机台id（对应T_CD90_MACHINE_INFO表id）
	 */
	private String machineId;
}
