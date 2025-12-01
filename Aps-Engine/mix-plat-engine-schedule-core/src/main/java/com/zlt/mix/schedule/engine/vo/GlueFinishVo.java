package com.zlt.mix.schedule.engine.vo;

import java.math.BigDecimal;
import java.util.Date;

import lombok.Data;

/**
 * 胶料完成量值对象
 * 
 * @author hakimryan
 *
 */
@Data
public class GlueFinishVo {
	/**
	 * 工单号
	 */
	private String orderNo;
	/**
	 * 完成量
	 */
	private BigDecimal finishQty;
}
