package com.zlt.aps.gdyy.engine.vo;

import lombok.Data;

/**
 * 钢带压延损耗率设定
 * 
 * @Description
 * @Author hakimryan
 * @Date 2021-8-10 16:42:03
 */
@Data
public class GdyyLossSettingVo {
    /**
     * 大卷编号
     */
	private String bigRollCode;
	/**
	 * 损耗率(百分比)
	 */
	private Double lossRate;
}
