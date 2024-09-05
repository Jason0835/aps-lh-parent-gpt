package com.ruoyi.job.task;

import javax.annotation.Resource;

import org.springframework.stereotype.Component;

import com.ruoyi.job.service.IStatisticsService;

/**
 * 统计任务
 * 
 * @Description
 * @Author hakimryan
 * @Date 2021-9-15 9:20:36
 */
@Component("statisticsTask")
public class StatisticsTask {
	@Resource
	private IStatisticsService iStatisticsService;

	/**
	 * 月度计划实际超欠产统计
	 */
	public void monthPlanActualOverProduction() {
		iStatisticsService.monthPlanActualOverProduction();
	}
}
