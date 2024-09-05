package com.zlt.aps.mps.service.impl;

import java.util.Date;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ruoyi.common.core.utils.DateUtils;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.aps.mps.mapper.MonthPlanStatisticsMapper;
import com.zlt.aps.mps.service.MonthPlanStatisticsService;

/**
 * 月度计划统计服务
 * 
 * @Description
 * @Author hakimryan
 * @Date 2021-9-15 9:34:22
 */
@Service("monthPlanStatisticsService")
public class MonthPlanStatisticsServiceImpl implements MonthPlanStatisticsService {
	@Autowired
	private MonthPlanStatisticsMapper monthPlanStatisticsMapper;

	/**
	 * 统计月度计划实际超欠产
	 * 
	 * @return
	 */
	@Override
	@Transactional
	public AjaxResult actualOverProduction() {
		Date currentDate = DateUtils.getNowDate();
		// 取出本月的相关时间节点
		String year = DateUtils.parseDateToStr("yyyy", currentDate);
		String month = DateUtils.parseDateToStr("MM", currentDate);
		Date startDate = DateUtils.setDays(currentDate, 1);
		Date endDate = DateUtils.addDays(DateUtils.addMonths(currentDate, 1), -1);
		// 根据物料号将实际产欠产更新至月度计划中
		monthPlanStatisticsMapper.updateActualOverProduction(year, month, startDate, endDate);
		return AjaxResult.success();
	}

}
