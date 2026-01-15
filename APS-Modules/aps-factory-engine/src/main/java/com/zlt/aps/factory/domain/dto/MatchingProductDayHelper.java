package com.zlt.aps.factory.domain.dto;

import lombok.Data;

/**
 * 搭配排产生产日期对象
 *
 * @author ZLT
 * @date 20251219
 */
@Data
public class MatchingProductDayHelper {
	/**
	 * 开始日期
	 */
	private Integer startDay;
	/**
	 * 结束日期
	 */
	private Integer endDay;

	public MatchingProductDayHelper(Integer startDay, Integer endDay) {
		super();
		this.startDay = startDay;
		this.endDay = endDay;
	}
}
