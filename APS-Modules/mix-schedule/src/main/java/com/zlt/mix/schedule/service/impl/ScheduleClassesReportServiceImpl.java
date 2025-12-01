package com.zlt.mix.schedule.service.impl;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import javax.annotation.Resource;

import org.apache.commons.lang3.math.NumberUtils;
import org.springframework.stereotype.Service;

import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.utils.StringUtils;
import com.zlt.mix.common.core.constant.ZltConstant;
import com.zlt.mix.common.core.utils.BigDecimalUtil;
import com.zlt.mix.common.core.utils.GenerageMapKeyUtils;
import com.zlt.mix.schedule.api.domain.dto.ScheduleClassesReportDto;
import com.zlt.mix.schedule.mapper.ScheduleClassesReportMapper;
import com.zlt.mix.schedule.service.ScheduleClassesReportService;

/**
 * 日计划每日报表统计
 * 
 * @author hakimryan
 *
 */
@Service
public class ScheduleClassesReportServiceImpl implements ScheduleClassesReportService {
	@Resource
	private ScheduleClassesReportMapper scheduleClassesReportMapper;
	
	/**
	 * 获取排产日报表统计信息
	 *
	 * @param scheduleReportDto
	 * @return
	 */
	public ScheduleClassesReportDto getScheduleReportSummary(ScheduleClassesReportDto scheduleReportDto) {
		return scheduleClassesReportMapper.getScheduleReportSummary(scheduleReportDto);
	}

	/**
	 * 获取排产日报表
	 *
	 * @param ScheduleClassesReportDto
	 * @return
	 */
	@Override
	public List<ScheduleClassesReportDto> selectScheduleReportList(ScheduleClassesReportDto ScheduleClassesReportDto) {
		if (ScheduleClassesReportDto == null || ScheduleClassesReportDto.getQueryScheduleDate() == null) {
			return new ArrayList<>(); // 没有排产日期直接返回空列表
		}
		List<ScheduleClassesReportDto> result = scheduleClassesReportMapper.selectScheduleReportList(ScheduleClassesReportDto);

		// 生成汇总行
		List<ScheduleClassesReportDto> summaryList = this.buildSummaryList(result);

		// 将汇总行直接添加到列表中
		for (ScheduleClassesReportDto summary : summaryList) {
			// 计算完成率
			Integer index = null;
			// 遍历列表，找到汇总行对应的最后一笔明细的下标
			for (int i = 0, size = result.size(); i < size; i++) {
				ScheduleClassesReportDto report = result.get(i);
				if (Objects.equals(report.getMixArea(), summary.getMixArea())
						&& Objects.equals(report.getProcedure(), summary.getProcedure())) {
					index = i;
				} else if (index != null) {
					break;
				}
			}
			index++;
			result.add(index, summary); // 直接将汇总行插入到指定下标的下一位
		}
		return result;
	}

	/**
	 * 构建汇总行
	 * 
	 * @param resultList 明细数据列表
	 * @param isSummary  是否汇总统计方式
	 * @return
	 */
	private List<ScheduleClassesReportDto> buildSummaryList(List<ScheduleClassesReportDto> resultList) {
		Map<String, ScheduleClassesReportDto> summaryMap = new HashMap<>();
		for (ScheduleClassesReportDto report : resultList) {
			String mixArea = report.getMixArea();
			String procedure = report.getProcedure(); // 工序
			String key = GenerageMapKeyUtils.createMapKey(mixArea, procedure); // 每日查询的汇总行识别码：日期+密炼区+工序
			ScheduleClassesReportDto summary = summaryMap.get(key);
			if (summary == null) {
				summary = new ScheduleClassesReportDto();
				summary.setMixArea(mixArea);
				summary.setProcedure(procedure);
				summary.setMidPlanQty(0D);
				summary.setMidPlanSpec(BigDecimal.ZERO.toString());
				summary.setMidFinishQty(0D);
				summary.setMidFinishSpec(BigDecimal.ZERO.toString());
				summary.setNightPlanQty(0D);
				summary.setNightPlanSpec(BigDecimal.ZERO.toString());
				summary.setNightFinishQty(0D);
				summary.setNightFinishSpec(BigDecimal.ZERO.toString());
				summary.setDayPlanQty(0D);
				summary.setDayPlanSpec(BigDecimal.ZERO.toString());
				summary.setDayFinishQty(0D);
				summary.setDayFinishSpec(BigDecimal.ZERO.toString());
				summary.setIsSpecNumber(ZltConstant.STATUS_DISABLE);
				summary.setIsSummary(ZltConstant.STATUS_DISABLE);
				summaryMap.put(key, summary);
			}
			// 统计各班的数据
			summary.setMidPlanQty(BigDecimalUtil.add(summary.getMidPlanQty(), report.getMidPlanQty()));
			summary.setMidFinishQty(BigDecimalUtil.add(summary.getMidFinishQty(), report.getMidFinishQty()));
			summary.setMidPlanSpec(this.buildSummarySpec(summary.getMidPlanSpec(), report.getMidPlanQty()));
			summary.setMidFinishSpec(this.buildSummarySpec(summary.getMidFinishSpec(), report.getMidFinishQty()));
			summary.setNightPlanQty(BigDecimalUtil.add(summary.getNightPlanQty(), report.getNightPlanQty()));
			summary.setNightFinishQty(BigDecimalUtil.add(summary.getNightFinishQty(), report.getNightFinishQty()));
			summary.setNightPlanSpec(this.buildSummarySpec(summary.getNightPlanSpec(), report.getNightPlanQty()));
			summary.setNightFinishSpec(this.buildSummarySpec(summary.getNightFinishSpec(), report.getNightFinishQty()));
			summary.setDayPlanQty(BigDecimalUtil.add(summary.getDayPlanQty(), report.getDayPlanQty()));
			summary.setDayFinishQty(BigDecimalUtil.add(summary.getDayFinishQty(), report.getDayFinishQty()));
			summary.setDayPlanSpec(this.buildSummarySpec(summary.getDayPlanSpec(), report.getDayPlanQty()));
			summary.setDayFinishSpec(this.buildSummarySpec(summary.getDayFinishSpec(), report.getDayFinishQty()));
		}
		
		return summaryMap.values().stream().sorted(Comparator.comparing(ScheduleClassesReportDto::getMixArea) // 密炼区升序
				.thenComparing(Comparator.comparing(ScheduleClassesReportDto::getProcedure, Comparator.reverseOrder()))) // 工序倒序
				.collect(Collectors.toList());
	}
	
	/**
	 * 构建统计行的规格栏位信息
	 * @param summarySpec	统计行的规格
	 * @param reportQty	数据行的数量
	 * @return
	 */
	private String buildSummarySpec(String summarySpec, Double reportQty) {
		BigDecimal totalFinishSpec = new BigDecimal(summarySpec);
		if (reportQty!= null && reportQty > 0) { // 汇总统计，统计数量大于0的，规格数加1
			totalFinishSpec = totalFinishSpec.add(BigDecimal.ONE);
		}
		return totalFinishSpec.toString();
	}
}
