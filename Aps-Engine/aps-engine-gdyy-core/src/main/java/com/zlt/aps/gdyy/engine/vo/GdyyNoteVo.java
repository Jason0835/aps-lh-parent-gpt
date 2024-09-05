package com.zlt.aps.gdyy.engine.vo;

import lombok.Data;

/**
 * 钢带压延注意事项配置
 * @Description 
 * @Author hakimrayn
 * @Date 2021-7-19 16:02:52
 * @Version 1.0
 */
@Data
public class GdyyNoteVo {
	/**
	 * 钢压大卷编号
	 */
	private String bigRollCode;
	/**
	 * 注意事项
	 */
	private String notes;
}
