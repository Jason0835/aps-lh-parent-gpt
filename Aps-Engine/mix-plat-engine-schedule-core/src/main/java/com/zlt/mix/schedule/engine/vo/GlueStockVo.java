package com.zlt.mix.schedule.engine.vo;

import java.math.BigDecimal;
import java.util.Date;

import com.zlt.mix.setting.api.domain.entity.BhgGlueStock;
import com.zlt.mix.setting.api.domain.entity.FhGlueStock;
import com.zlt.mix.setting.api.domain.entity.GlueStock;
import com.zlt.mix.setting.api.domain.entity.MlGlueStock;

import lombok.Data;

/**
 * 胶料库存VO
 * 
 * @author hakimryan
 *
 */
@Data
public class GlueStockVo {
	/**
	 * 库存日期
	 */
	private Date stockDate;
	/**
	 * 条码
	 */
	private String barCode;
	/**
	 * 有效期
	 */
	private Date validTime;
	/**
	 * 胶料号
	 */
	private String glue;
	/**
	 * 库存量
	 */
	private BigDecimal stockNum;
	/**
	 * 库存重量
	 */
	private BigDecimal stockWeight;
	
	public GlueStockVo() {
		super();
	}

	public GlueStockVo(String glue, BigDecimal stockNum, BigDecimal stockWeight, Date validTime) {
		this.glue = glue;
		this.stockNum = stockNum;
		this.validTime = validTime;
		this.stockWeight = stockWeight;
	}

	/**
	 * 通过终炼胶构建VO
	 * 
	 * @param stock
	 */
	public GlueStockVo(GlueStock stock) {
		this.stockDate = stock.getStockDate();
		this.barCode = stock.getBarCode();
		this.validTime = stock.getValidTime();
		this.glue = stock.getGlue();
	}

	/**
	 * 通过母炼胶构建VO
	 * 
	 * @param stock
	 */
	public GlueStockVo(MlGlueStock stock) {
		this.stockDate = stock.getStockDate();
		this.barCode = stock.getBarCode();
		this.validTime = stock.getValidTime();
		this.glue = stock.getGlue();
	}

	/**
	 * 通过返回胶构建VO
	 * 
	 * @param stock
	 */
	public GlueStockVo(FhGlueStock stock) {
		this.stockDate = stock.getStockDate();
		this.barCode = stock.getBarCode();
		this.validTime = stock.getValidTime();
		this.glue = stock.getGlue();
	}

	/**
	 * 通过不合格胶构建VO
	 * 
	 * @param stock
	 */
	public GlueStockVo(BhgGlueStock stock) {
		this.stockDate = stock.getStockDate();
		this.barCode = stock.getBarCode();
		this.validTime = stock.getValidTime();
		this.glue = stock.getGlue();
	}
}
