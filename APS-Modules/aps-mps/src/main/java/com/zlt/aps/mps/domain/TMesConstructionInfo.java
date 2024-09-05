package com.zlt.aps.mps.domain;

import java.math.BigDecimal;
import java.util.Date;

import lombok.Data;

/**
 * 从mes的bom解析出来的施工信息，仅描述某一胎胚的某个字段的数据信息
 * 
 * @Description
 * @Author hakimryan
 * @Date 2021-9-13 14:13:00
 */
@Data
public class TMesConstructionInfo {
	/**
	 * 唯一键，用于排序
	 */
	private String id;
	/**
	 * 胎号
	 */
	private String embryoCode;
	/**
	 * 胎胚版本号
	 */
	private String embryoVersion;
	/**
	 * 胎胚SAP版本，用于确认最新版
	 */
	private String sapVersion;
	/**
	 * 列名
	 */
	private String columnName;
	/**
	 * 列数值
	 */
	private String columnValue;
	/**
	 * 子物料号
	 */
	private String childMaterialCode;
	/**
	 * 子物料名称编码(名称中文映射)
	 */
	private String childMaterialNameCode;
	/**
	 * 父物料号，硫化施工表需要用到
	 */
	private String parentMaterialCode;
	/**
	 * 排序号，用于去重逻辑
	 */
	private String sortCode;
	/**
	 * 用量，用于部分字段合并判断条件
	 */
	private BigDecimal dosage;
	/**
	 * BOM更新时间，用于去重
	 */
	private Date updateDate;
	/**
	 * BOM创建时间，用于去重
	 */
	private Date createDate;
	/**
	 * PLM更新时间，用于去重
	 */
	private Date plmUpdateDate;
	/**
	 * 备注
	 */
	private String remark;
	/**
	 * 是否更新，只有版本号正确的栏位才需要更新
	 * 默认都全更新，modify by 20220104
	 */
	private boolean isModify = true;
	/**
	 * 启用状态
	 */
	private String status;
}
