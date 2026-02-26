package com.zlt.aps.itf.scm.vo;

import java.math.BigDecimal;
import java.util.Date;

import com.fasterxml.jackson.annotation.JsonFormat;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * Copyright (c) 2024, All rights reserved。 文件名称：SalesOrderPool.java 描
 * 述：已计划未发货同步接口结果vo
 * 
 * @author zlt
 * @date 2025-12-04
 * @version 1.0
 *
 *          修改记录： 修改时间：... 修 改 人：zlt 修改内容：...
 */

@Data
public class SyncPlanedNotShipResultVo {
	/**
	 * 主键
	 */
	private Long Id;

	/**
	 * EUDR标识
	 */
	@ApiModelProperty(value = "EUDR标识", name = "eudrFlag")
	private String eudrFlag;

	/**
	 * 生产工厂
	 */
	@ApiModelProperty(value = "生产工厂", name = "factory")
	private String factory;

	/**
	 * 区域
	 */
	@ApiModelProperty(value = "区域", name = "employeeDept")
	private Long employeeDept;

	/**
	 * 销售客户
	 */
	@ApiModelProperty(value = "销售客户")
	private String salCode;

	/** 销售客户名称 */
	@ApiModelProperty(value = "销售客户名称", notes = "虚字段从salCode转义", name = "custName", hidden = true)
	private String custName;

	/**
	 * 贸易国别
	 */
	@ApiModelProperty(value = "贸易国别", name = "salNCode")
	private String salNCode;

	/** 目的国 */
	@ApiModelProperty(value = "目的国", name = "natCode")
	private String natCode;

	/**
	 * 品牌
	 */
	@ApiModelProperty(value = "品牌")
	private String brand;

	/**
	 * 品牌(英文名称)
	 */
	@ApiModelProperty(value = "品牌(英文名称)", name = "engBrand")
	private String engBrand;

	/**
	 * 品牌
	 */
	@ApiModelProperty(value = "品牌名称")
	private String brandName;

	/**
	 * 客户PO
	 */
	@ApiModelProperty(value = "客户PO", name = "salCodePo")
	private String salCodePo;

	/**
	 * 内外销
	 */
    @ApiModelProperty(value = "内外销", name = "locationType")
    private String locationType;

	/**
	 * 原物料编码
	 */
	@ApiModelProperty(value = "原物料编码")
	private String oriMaterialCode;

	/**
	 * 物料描述
	 */
	@ApiModelProperty(value = "物料描述")
	private String materialDesc;

	/**
	 * 业务名称
	 */
	@ApiModelProperty(value = "业务名称")
	private String businessName;

	/**
	 * 已计划未发货数(交货单数量)
	 */
	@ApiModelProperty(value = "已计划未发货数(交货单数量)")
	private BigDecimal planedNotShipQty;

	/**
	 * 计划数
	 */
	@ApiModelProperty(value = "计划满足数量", name = "planQty")
	private Integer planQty;

	/** 交货单号 */
	@ApiModelProperty(value = "交货单号", name = "dnBillNo")
	private String dnBillNo;

	/** 出运单号 */
	@ApiModelProperty(value = "出运单号", name = "shipNo")
	private String shipNo;
    
    /** 质控状态 */
    @ApiModelProperty(value = "质控状态", name = "qualityStateCode")
    private String qualityStateCode;

	/**
	 * 规格
	 */
	@ApiModelProperty(value = "规格")
	private String specDesc;

	/**
	 * 花纹
	 */
	@ApiModelProperty(value = "花纹")
	private String figure;

	/**
	 * 层级
	 */
	@ApiModelProperty(value = "层级")
	private String iLevel;

	/** 速度级别 */
	@ApiModelProperty(value = "速度级别", name = "speedLevel")
	private String speedLevel;

	/** 负荷指数 */
	@ApiModelProperty(value = "负荷指数", name = "loadIndex")
	private String loadIndex;

	/**
	 * 速度负荷指数
	 */
	@ApiModelProperty(value = "速度负荷指数", name = "speedLevelLoadIndex")
	private String speedLevelLoadIndex;

	/**
	 * 订单数量
	 */
	@ApiModelProperty(value = "订单数量")
	private BigDecimal ordQty;

	/**
	 * 首次整单货好时间
	 */
	@JsonFormat(pattern = "yyyy-MM-dd")
	@ApiModelProperty(value = "首次整单货好时间", name = "fstOrdFinDate")
	private Date fstOrdFinDate;

	/**
	 * 提报日期
	 */
	@JsonFormat(pattern = "yyyy-MM-dd")
	@ApiModelProperty(value = "提报日期", name = "billDate")
	private Date billDate;

	/**
	 * 订单月份
	 */
	@JsonFormat(pattern = "yyyy-MM")
	@ApiModelProperty(value = "订单月份", name = "ordCreateTimeMonth")
	private Date ordCreateTimeMonth;

	/** 请求交货日期(销售订单期望发货日期) */
	@JsonFormat(pattern = "yyyy-MM-dd")
	@ApiModelProperty(value = "请求交货日期(销售订单期望发货日期)", name = "expeShipDate")
	private Date expeShipDate;

	/**
	 * 销售订单号
	 */
	@ApiModelProperty(value = "销售订单号", name = "salOrdNo")
	private String salOrdNo;

	/** 年周号要求 */
	@ApiModelProperty(value = "年周号要求", name = "weekYearRequirement")
	private String weekYearRequirement;

	/** 发货模式 */
	@ApiModelProperty(value = "发货模式", name = "shipType")
	private String shipType;

	/**
	 * 订单日期
	 */
	@ApiModelProperty(value = "订单日期", name = "ordCreateTime")
	private Date ordCreateTime;

	/** 交货单创建时间 */
	@JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
	@ApiModelProperty(value = "交货单创建时间", name = "dnCreateTime")
	private Date dnCreateTime;

	@ApiModelProperty(value = "产品类型", name = "productType")
	private String productType;

	/** 销售优先级 */
	@ApiModelProperty(value = "销售优先级", name = "salPriority")
	private String salPriority;

	/** APS数据同步状态，0：未同步，1：已同步 */
	@ApiModelProperty(value = "APS数据同步状态", name = "apsSyncStatus")
	private String apsSyncStatus;

	/** 销售订单明细关联ID */
	@ApiModelProperty(value = "销售订单明细关联ID", name = "saleBillDetailId")
	private Long saleBillDetailId;

	/** 数据年份 */
	@ApiModelProperty(value = "数据年份", name = "year")
	private Integer year;

	/** 数据月份 */
	@ApiModelProperty(value = "数据月份", name = "month")
	private Integer month;
}