package com.zlt.aps.xwyy.engine.vo;

import lombok.Data;

/**
 * 纤维压延损耗率设定
 * 
 * @Description
 * @Author hakimryan
 * @Date 2021-8-10 17:22:03
 */
@Data
public class XwyyLossSettingVo {
    /**
     * 损耗率key（大卷编号#机台id）
     */
	private String lossKey;
	/**
	 * 损耗率(百分比)
	 */
	private Double lossRate;
}
