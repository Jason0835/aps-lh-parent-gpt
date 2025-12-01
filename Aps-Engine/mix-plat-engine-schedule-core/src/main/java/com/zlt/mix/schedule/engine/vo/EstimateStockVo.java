package com.zlt.mix.schedule.engine.vo;

import java.math.BigDecimal;
import java.util.Date;

import lombok.Data;

/**
 * 预计库存
 * 
 * @author hakimryan
 *
 */
@Data
public class EstimateStockVo {
	private String glueCode;

	private BigDecimal stockNum;

	private Date updateTime;
}
