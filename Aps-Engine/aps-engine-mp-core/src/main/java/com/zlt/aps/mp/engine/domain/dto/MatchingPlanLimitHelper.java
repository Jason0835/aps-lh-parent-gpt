package com.zlt.aps.mp.engine.domain.dto;

import java.util.Optional;

import lombok.Data;

/**
 * 搭配排产排产限制对象
 *
 * @author ZLT
 * @date 20251219
 */
@Data
public class MatchingPlanLimitHelper {
	/**
	 * 模具数量限制
	 */
	private Integer mouldQty;
	/**
	 * 已排产量
	 */
	private Integer planQty;
	/**
	 * 模具数量限制
	 */
	private Integer maxMouldQty;
	/**
	 * 已排产量
	 */
	private Integer maxPlanQty;
	/**
	 * 硫化产能限制
	 */
//	private Integer dayVulcanizationQty;

	/**
	 * 判断是否满足生产条件
	 * 
	 * @return
	 */
	public boolean isProduct() {
		Integer planQty = Optional.ofNullable(this.planQty).orElse(0);
		Integer mouldQty = Optional.ofNullable(this.mouldQty).orElse(0);
		Integer maxPlanQty = Optional.ofNullable(this.maxPlanQty).orElse(0);
		Integer maxMouldQty = Optional.ofNullable(this.maxMouldQty).orElse(0);
//		return maxPlanQty > planQty && maxMouldQty > mouldQty;
        return maxPlanQty > planQty || maxMouldQty > mouldQty;
	}
}
