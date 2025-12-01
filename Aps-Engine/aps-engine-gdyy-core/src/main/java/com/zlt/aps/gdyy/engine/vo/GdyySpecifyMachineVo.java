package com.zlt.aps.gdyy.engine.vo;

import lombok.Data;

/**
 * 钢带压延断定点机台表
 * 
 * @Description
 * @Author steve
 * @date 2025-2-17 20:51:57
 * @Version 1.0
 */
@Data
public class GdyySpecifyMachineVo {
	/**
	 * 帘线大卷编号
	 */
	private String bigRollCode;

	/**
	 * 机台id（对应T_cd90_MACHINE_INFO表id）
	 */
	private String machineId;
}
