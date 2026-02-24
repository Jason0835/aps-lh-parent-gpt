package com.zlt.aps.factory.domain.dto;

import java.util.List;

import com.zlt.aps.factory.domain.vo.ProductionMouldInfoVo;

import lombok.Data;

/**
 * 搭配排产模具日使用信息对象
 *
 * @author ZLT
 * @date 20251219
 */
@Data
public class MatchingMouldDayUsedHelper {
	/**
	 * 可用模具
	 */
	private List<ProductionMouldInfoVo> mouldInfoList;
	/**
	 * 开始使用日 1~31
	 */
	private Integer beginDate;
	/**
	 * 结束使用日 1~31
	 */
	private Integer endDate;

	public MatchingMouldDayUsedHelper(List<ProductionMouldInfoVo> mouldInfoList, Integer beginDate, Integer endDate) {
		super();
		this.mouldInfoList = mouldInfoList;
		this.beginDate = beginDate;
		this.endDate = endDate;
	}
}
