package com.zlt.aps.itf.scm.vo;

import java.math.BigDecimal;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * Copyright (c) 2024, All rights reserved。 文件名称：SalesOrderPool.java 描
 * 述：发货明细同步接口结果vo
 * 
 * @author zlt
 * @date 2025-12-11
 * @version 1.0
 *
 *          修改记录： 修改时间：... 修 改 人：zlt 修改内容：...
 */

@Data
public class SyncOutShipDmdOrdResultVo {
	/**
	 * 生产工厂
	 */
	@ApiModelProperty(value = "生产工厂", name = "factory")
	private String factory;

	/** 数据年份 */
	@ApiModelProperty(value = "数据年份", name = "year")
	private Integer year;

	/** 数据月份 */
	@ApiModelProperty(value = "数据月份", name = "month")
	private Integer month;

	/** 产品品类 */
	@ApiModelProperty(value = "产品品类", name = "productType")
	private String productType;

	/** 区域 */
	@ApiModelProperty(value = "区域", name = "employeeDept")
	private Long employeeDept;

	/** 品牌 */
	@ApiModelProperty(value = "品牌", name = "brand")
	private String brand;

	/** 原物料编码 */
	@ApiModelProperty(value = "原物料编码", name = "oriMaterialCode")
	private String oriMaterialCode;

	/** 物料描述 */
	@ApiModelProperty(value = "物料描述", name = "materialDesc")
	private String materialDesc;

	/** 交货单数量 */
	@ApiModelProperty(value = "交货单数量", name = "dnNum")
	private BigDecimal dnNum;
}