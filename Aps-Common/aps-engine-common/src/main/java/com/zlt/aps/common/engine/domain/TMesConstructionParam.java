package com.zlt.aps.common.engine.domain;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import lombok.Data;

/**
 * 施工表参数
 * @Description
 * @Author hakimryan
 * @Date 2021-9-13 15:12:06
 */
@Data
public class TMesConstructionParam {
	/**
	 * 胎胚ID
	 */
	private String id;
	/**
	 * 胎号
	 */
	private String embryoCode;
	/**
	 * 胎胚编号
	 */
	private String embryoVersion;
	/**
	 * sap版本，用于确认最新版本
	 */
	private String sapVersion;
	/**
	 * 最后更新时间
	 */
	private Date updateDate;
	/**
	 * 各栏位信息map，key:列明，value:这列的信息，可以有多个，需要根据不同的列名去重/合并数据
	 */
	private Map<String, List<TMesConstructionInfo>> columnMap = new HashMap<>();
	/**
	 * 栏位列表，去重/合并后的结果，每个列名只会有一笔
	 */
	private List<TMesConstructionInfo> columnList;
	/**
	 * 数值
	 */
	private Date currentDate;
	/**
	 * 对象序列号，用于最终结果列表的排序
	 */
	private String orderNo;
}
