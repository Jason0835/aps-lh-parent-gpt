package com.zlt.aps.xwyy.engine.vo;

import lombok.Data;

/**
 * 纤维压延断定点机台表
 * 
 * @Description
 * @Author hakimrayn
 * @Date 2021-7-22 10:10:58
 * @Version 1.0
 */
@Data
public class XwyySpecifyMachineVo {
	/**
	 * 帘线大卷编号
	 */
	private String bigRollCode;

	/**
	 * 机台id（对应T_cd15_MACHINE_INFO表id）
	 */
	private String machineId;
}
