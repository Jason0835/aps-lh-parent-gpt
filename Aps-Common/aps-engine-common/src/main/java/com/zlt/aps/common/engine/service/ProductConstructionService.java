package com.zlt.aps.common.engine.service;

import java.util.List;

import com.zlt.aps.common.engine.domain.MdmMonthProdPlan;
/**
 * 投产施工服务接口
 * @Description
 */
public interface ProductConstructionService {
	/**
	 * 根据月度计划初始化施工信息
	 * 月度计划涉及胎胚所有版本均同步至投产施工表（已有的版本不同步）
	 * 并将只有一个版本的胎胚的版本号赋值到月度计划中
	 * 
	 * @param prodList	月度计划明细
	 */
	void initBomDataVersionByPlan(List<MdmMonthProdPlan> prodList);
}
