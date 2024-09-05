package com.zlt.aps.mps.service;

import com.ruoyi.common.core.web.domain.AjaxResult;

/**
 * 月度计划统计服务
 * 
 * @Description
 * @Author hakimryan
 * @Date 2021-9-15 9:34:22
 */
public interface MonthPlanStatisticsService {
	/**
	 * 统计月度计划实际超欠产
	 * 
	 * @return
	 */
	AjaxResult actualOverProduction();
}
