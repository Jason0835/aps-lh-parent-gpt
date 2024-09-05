package com.zlt.aps.cd90.engine.vo;

import lombok.Data;

/**
 * 90度裁断损耗率设定
 * 
 * @Description
 * @Author hakimryan
 * @Date 2021-8-10 15:22:03
 */
@Data
public class Cd90LossSettingVo {
    /**
     * 损耗率key（帘布编号#机台id）
     */
	private String lossKey;
	/**
	 * 损耗率(百分比)
	 */
	private Double lossRate;
}
