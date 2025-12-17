package com.zlt.aps.itf.scm.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * Copyright (c) 2022, All rights reserved。 文件名称：OutFacScheduleVersion.java 描
 * 述：生产排程_版本对象 t_out_fac_schedule_version
 * 
 * @author zlt
 * @date 2025-12-12
 * @version 1.0
 *
 *          修改记录： 修改时间：... 修 改 人：zlt 修改内容：...
 */
@ApiModel(value = "生产排程_版本对象", description = "生产排程_版本对象 ")
@Data
public class SyncOutFacScheduleVersionVo {
	private Long id;
	/** 工厂 */
	@ApiModelProperty(value = "工厂", name = "factory")
	private String factory;

	/** 计划版本号 */
	@ApiModelProperty(value = "计划版本号", name = "planVersion")
	private String planVersion;

	/** 生产计划号 */
	@ApiModelProperty(value = "生产计划号", name = "productPlanNo")
	private String productPlanNo;

	/** 状态 */
	@ApiModelProperty(value = "状态", name = "status")
	private String status;

	/** 排产年份 */
	@ApiModelProperty(value = "排产年份", name = "year")
	private String year;

	/** 排产月份 */
	@ApiModelProperty(value = "排产月份", name = "month")
	private String month;

	/**
	 * 产品分类
	 */
	@ApiModelProperty(value = "产品分类 TBR/PCR", name = "productionCategory")
	private String productionCategory;

	/** 天数 */
	@ApiModelProperty(value = "天数", name = "dayNum")
	private String dayNum;

	/**
	 * 物料编码
	 */
	@ApiModelProperty(value = "物料编码")
	private String materialCode;

	/**
	 * 物料描述
	 */
	@ApiModelProperty(value = "物料描述")
	private String materialDesc;

	/**
	 * 行号
	 */
	@ApiModelProperty(value = "行号")
	private Long rowNo;

	/**
	 * 业务类型 内销/外销
	 */
	@ApiModelProperty(value = "业务类型 内销/外销", name = "sale_mode")
	private String busiType;
	/**
	 * 体系
	 */
	@ApiModelProperty(value = "体系")
	private String archiSystem;

	/**
	 * 品牌
	 */
	@ApiModelProperty(value = "品牌")
	private String brand;

	/**
	 * 规格
	 */
	@ApiModelProperty(value = "规格")
	private String specifications;

	/**
	 * 花纹
	 */
	@ApiModelProperty(value = "花纹")
	private String figure;

	/**
	 * 层级
	 */
	@ApiModelProperty(value = "层级")
	private String tireLevel;

	/**
	 * 速级
	 */
	@ApiModelProperty(value = "速级")
	private String speedLevel;

	/**
	 * 负荷指数
	 */
	@ApiModelProperty(value = "负荷指数")
	private String loadIndex;

	/**
	 * 总订单数
	 */
	@ApiModelProperty(value = "总订单数")
	private Integer totalOrdQtc;

	/**
	 * 已发货数量
	 */
	@ApiModelProperty(value = "已发货数量")
	private Integer shipQtc;

	/**
	 * 已计划未发货
	 */
	@ApiModelProperty(value = "已计划未发货")
	private Integer planUnshipQtc;

	/**
	 * 库存量
	 */
	@ApiModelProperty(value = "库存量")
	private Integer stockQtc;

	/**
	 * 待排产量
	 */
	@ApiModelProperty(value = "待排产量")
	private Integer unscheduleQtc;

	/**
	 * 排产数量
	 */
	@ApiModelProperty(value = "排产数量")
	private Integer scheduleQtc;

	/**
	 * 排产分类,0-单产,1-周期排产,2-常规排产
	 */
	@ApiModelProperty(value = "排产分类,0-单产,1-周期排产,2-常规排产")
	private String productionClass;

	/**
	 * 1号计划量
	 */
	@ApiModelProperty(value = "1号计划量")
	private Integer day1;

	/**
	 * 2号计划量
	 */
	@ApiModelProperty(value = "2号计划量")
	private Integer day2;

	/**
	 * 3号计划量
	 */
	@ApiModelProperty(value = "3号计划量")
	private Integer day3;

	/**
	 * 4号计划量
	 */
	@ApiModelProperty(value = "4号计划量")
	private Integer day4;

	/**
	 * 5号计划量
	 */
	@ApiModelProperty(value = "5号计划量")
	private Integer day5;

	/**
	 * 6号计划量
	 */
	@ApiModelProperty(value = "6号计划量")
	private Integer day6;

	/**
	 * 7号计划量
	 */
	@ApiModelProperty(value = "7号计划量")
	private Integer day7;

	/**
	 * 8号计划量
	 */
	@ApiModelProperty(value = "8号计划量")
	private Integer day8;

	/**
	 * 9号计划量
	 */
	@ApiModelProperty(value = "9号计划量")
	private Integer day9;

	/**
	 * 10号计划量
	 */
	@ApiModelProperty(value = "10号计划量")
	private Integer day10;

	/**
	 * 11号计划量
	 */
	@ApiModelProperty(value = "11号计划量")
	private Integer day11;

	/**
	 * 12号计划量
	 */
	@ApiModelProperty(value = "12号计划量")
	private Integer day12;

	/**
	 * 13号计划量
	 */
	@ApiModelProperty(value = "13号计划量")
	private Integer day13;

	/**
	 * 14号计划量
	 */
	@ApiModelProperty(value = "14号计划量")
	private Integer day14;

	/**
	 * 15号计划量
	 */
	@ApiModelProperty(value = "15号计划量")
	private Integer day15;

	/**
	 * 16号计划量
	 */
	@ApiModelProperty(value = "16号计划量")
	private Integer day16;

	/**
	 * 17号计划量
	 */
	@ApiModelProperty(value = "17号计划量")
	private Integer day17;

	/**
	 * 18号计划量
	 */
	@ApiModelProperty(value = "18号计划量")
	private Integer day18;

	/**
	 * 19号计划量
	 */
	@ApiModelProperty(value = "19号计划量")
	private Integer day19;

	/**
	 * 20号计划量
	 */
	@ApiModelProperty(value = "20号计划量")
	private Integer day20;

	/**
	 * 21号计划量
	 */
	@ApiModelProperty(value = "21号计划量")
	private Integer day21;

	/**
	 * 22号计划量
	 */
	@ApiModelProperty(value = "22号计划量")
	private Integer day22;

	/**
	 * 23号计划量
	 */
	@ApiModelProperty(value = "23号计划量")
	private Integer day23;

	/**
	 * 24号计划量
	 */
	@ApiModelProperty(value = "24号计划量")
	private Integer day24;

	/**
	 * 25号计划量
	 */
	@ApiModelProperty(value = "25号计划量")
	private Integer day25;

	/**
	 * 26号计划量
	 */
	@ApiModelProperty(value = "26号计划量")
	private Integer day26;

	/**
	 * 27号计划量
	 */
	@ApiModelProperty(value = "27号计划量")
	private Integer day27;

	/**
	 * 28号计划量
	 */
	@ApiModelProperty(value = "28号计划量")
	private Integer day28;

	/**
	 * 29号计划量
	 */
	@ApiModelProperty(value = "29号计划量")
	private Integer day29;

	/**
	 * 30号计划量
	 */
	@ApiModelProperty(value = "30号计划量")
	private Integer day30;

	/**
	 * 31号计划量
	 */
	@ApiModelProperty(value = "31号计划量")
	private Integer day31;
}