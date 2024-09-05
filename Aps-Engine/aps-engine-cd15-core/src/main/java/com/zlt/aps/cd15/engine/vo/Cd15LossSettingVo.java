package com.zlt.aps.cd15.engine.vo;

import lombok.Data;

/**
 * 15度裁断损耗率设定
 * 
 * @Description
 * @Author hakimryan
 * @Date 2021-8-10 13:22:03
 */
@Data
public class Cd15LossSettingVo {
    /**
     * 损耗率key（钢带编号#机台id）
     */
	private String lossKey;
	/**
	 * 损耗率(百分比)
	 */
	private Double lossRate;
}
