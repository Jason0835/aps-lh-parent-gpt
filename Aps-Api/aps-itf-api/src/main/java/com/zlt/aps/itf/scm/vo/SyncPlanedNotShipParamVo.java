package com.zlt.aps.itf.scm.vo;

import java.util.Date;

import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.Data;

/**
 * Copyright (c) 2024, All rights reserved。 文件名称：SyncPlanedNotShipVo.java 描 述：
 *
 * @author zlt
 * @version 1.0
 * @date 2025/12/9
 */
@Data
public class SyncPlanedNotShipParamVo {
	/**
	 * 生产工厂
	 */
	private String factory;

	/**
	 * 数据年份
	 */
	private Integer year;

	/**
	 * 数据月份
	 */
	private Integer month;
}
