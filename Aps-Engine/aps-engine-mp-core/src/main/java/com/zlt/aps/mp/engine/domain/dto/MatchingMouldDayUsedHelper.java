package com.zlt.aps.mp.engine.domain.dto;

import java.util.List;

import com.zlt.aps.mp.engine.domain.vo.ProductionMouldInfoVo;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 搭配排产模具日使用信息对象
 *
 * @author ZLT
 * @date 20251219
 */
@Data
@AllArgsConstructor
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
	/**
	 * 硫化日产
	 */
	private Integer dayVulcanizationQty;
}
