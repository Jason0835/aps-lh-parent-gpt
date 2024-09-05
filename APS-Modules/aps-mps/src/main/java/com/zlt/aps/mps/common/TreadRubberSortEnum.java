package com.zlt.aps.mps.common;

import java.util.Arrays;

import com.ruoyi.common.utils.StringUtils;

/**
 * 胎面胶料顺序
 * 
 * @Description
 */
public enum TreadRubberSortEnum {
	/**
	 * 顶胶
	 */
	TopRubber("HT", 1),
	/**
	 * 边胶
	 */
	SideRubber("HS", 2),
	/**
	 * 底胶
	 */
	BottomRubber("HB", 3);

	// 胶料号
	private String code;
	// 顺序号
	private Integer sortNo;
	// 匹配不到的胶料号序号
	private static final Integer LAST_SORT_NO = 4;

	private TreadRubberSortEnum(String code, Integer sortNo) {
		this.code = code;
		this.sortNo = sortNo;
	}

	/**
	 * 通过胶料编号获取顺序号
	 * 
	 * @param code 胶料编号
	 * @return
	 */
	public static Integer getSortNo(String code) {
		if (StringUtils.isBlank(code)) {
			return LAST_SORT_NO;
		}
		return Arrays.stream(TreadRubberSortEnum.values()).filter(e -> code.contains(e.code)).map(TreadRubberSortEnum::getSortNo)
				.findAny().orElse(LAST_SORT_NO);
	}

	private Integer getSortNo() {
		return sortNo;
	}
}
