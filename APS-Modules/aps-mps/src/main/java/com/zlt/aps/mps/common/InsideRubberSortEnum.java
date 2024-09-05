package com.zlt.aps.mps.common;

import java.util.Arrays;

import com.ruoyi.common.utils.StringUtils;

/**
 * 内衬胶料顺序
 * 
 * @Description
 */
public enum InsideRubberSortEnum {
	/**
	 * L胶
	 */
	LRubber("HL", 1),
	/**
	 * F胶
	 */
	FRubber("HF", 2);

	// 胶料号
	private String code;
	// 顺序号
	private Integer sortNo;
	// 匹配不到的胶料号序号
	private static final Integer LAST_SORT_NO = 3;

	private InsideRubberSortEnum(String code, Integer sortNo) {
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
		return Arrays.stream(InsideRubberSortEnum.values()).filter(e -> code.contains(e.code)).map(InsideRubberSortEnum::getSortNo)
				.findAny().orElse(LAST_SORT_NO);
	}

	private Integer getSortNo() {
		return sortNo;
	}
}
