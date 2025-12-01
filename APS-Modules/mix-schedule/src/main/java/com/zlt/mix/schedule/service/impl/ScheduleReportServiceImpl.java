package com.zlt.mix.schedule.service.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
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
import com.zlt.mix.schedule.api.domain.dto.ScheduleReportDto;
import com.zlt.mix.schedule.mapper.ScheduleReportMapper;
import com.zlt.mix.schedule.service.ScheduleReportService;

/**
 * 日计划每日报表统计
 * 
 * @author hakimryan
 *
 */
@Service
public class ScheduleReportServiceImpl implements ScheduleReportService {
	@Resource
	private ScheduleReportMapper scheduleReportMapper;
	/**
	 * 100，用于处理百分比
	 */
	private final static BigDecimal ONE_HUNDRED = new BigDecimal("100");

	/**
	 * 获取排产日报表
	 *
	 * @param scheduleReportDto
	 * @return
	 */
	@Override
	public List<ScheduleReportDto> selectScheduleReportList(ScheduleReportDto scheduleReportDto) {
		if (scheduleReportDto == null || scheduleReportDto.getQueryStartDate() == null
				|| scheduleReportDto.getQueryEndDate() == null) {
			return new ArrayList<>(); // 没有排产日期直接返回空列表
		}
		List<ScheduleReportDto> result = scheduleReportMapper.selectScheduleReportList(scheduleReportDto);

		// 生成汇总行
		List<ScheduleReportDto> summaryList = this.buildSummaryList(result, false);

		// 判断是是每日统计还是汇总统计，每日统计需要展示明细数据与汇总行；汇总统计只需要展示汇总行
		boolean isSummary = scheduleReportDto.getQueryStartDate().compareTo(scheduleReportDto.getQueryEndDate()) != 0; // 同时查多天的即为汇总统计
		if (isSummary) {
			// 汇总统计，则直接将汇总行按排产日、密炼区、工序排好序直接返回前端
			result = summaryList;
			// 再构建整体汇总行
			summaryList = this.buildSummaryList(result, true);
			// 整体汇总行除了第一行，其余的日期全部清空
			for (int i = 1, size = summaryList.size(); i < size; i++) {
				summaryList.get(i).setScheduleDate(StringUtils.EMPTY);
			}
			result.addAll(summaryList);
		} else {
			// 将汇总行直接添加到列表中
			String summaryString = I18nUtil.getMessage("schedule.scheduleReport.summary");
			for (ScheduleReportDto summary : summaryList) {
				// 计算完成率
				Integer index = null;
				// 遍历列表，找到汇总行对应的最后一笔明细的下标
				for (int i = 0, size = result.size(); i < size; i++) {
					ScheduleReportDto report = result.get(i);
					if (Objects.equals(report.getScheduleDate(), summary.getScheduleDate())
							&& Objects.equals(report.getMixArea(), summary.getMixArea())
							&& Objects.equals(report.getProcedure(), summary.getProcedure())) {
						index = i;
					} else if (index != null) {
						break;
					}
				}
				index++;
				result.add(index, summary); // 直接将汇总行插入到指定下标的下一位

				// 设置汇总行特有内容
				summary.setIsSummary(ZltConstant.STATUS_DISABLE);
				summary.setMixArea(StringUtils.EMPTY);
				summary.setProcedure(StringUtils.EMPTY);
				summary.setScheduleDate(summaryString);
			}
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
	private List<ScheduleReportDto> buildSummaryList(List<ScheduleReportDto> resultList, boolean isSummary) {
		String summaryString = I18nUtil.getMessage("schedule.scheduleReport.summary");
		Map<String, ScheduleReportDto> summaryMap = new HashMap<>();
		for (ScheduleReportDto report : resultList) {
			String scheduleDate = report.getScheduleDate();
			String mixArea = report.getMixArea();
			String procedure = report.getProcedure(); // 工序
			String key = isSummary ? procedure // 汇总查询的汇总行识别码：工序
					: GenerageMapKeyUtils.createMapKey(scheduleDate, mixArea, procedure); // 每日查询的汇总行识别码：日期+密炼区+工序
			ScheduleReportDto summary = summaryMap.get(key);
			if (summary == null) {
				summary = new ScheduleReportDto();
				summary.setMixArea(mixArea);
				summary.setScheduleDate(scheduleDate);
				summary.setProcedure(procedure);
				summary.setPlanQty(0D);
				summary.setPlanSpec(BigDecimal.ZERO.toString());
				summary.setFinishQty(0D);
				summary.setFinishSpec(BigDecimal.ZERO.toString());
				summaryMap.put(key, summary);
			}

			BigDecimal totalPlanSpec = new BigDecimal(summary.getPlanSpec());
			if (isSummary && NumberUtils.isDigits(report.getPlanSpec())) { // 汇总统计，且规格是数字，直接把数字加上去
				totalPlanSpec = totalPlanSpec.add(new BigDecimal(report.getPlanSpec()));
			} else if (StringUtils.isNotEmpty(report.getPlanSpec())) {
				totalPlanSpec = totalPlanSpec.add(BigDecimal.ONE);
			}
			BigDecimal totalFinishSpec = new BigDecimal(summary.getFinishSpec());
			if (isSummary && NumberUtils.isDigits(report.getFinishSpec())) { // 汇总统计，且规格是数字，直接把数字加上去
				totalFinishSpec = totalFinishSpec.add(new BigDecimal(report.getFinishSpec()));
			} else if (StringUtils.isNotEmpty(report.getFinishSpec())) {
				totalFinishSpec = totalFinishSpec.add(BigDecimal.ONE);
			}
			Double totalPlanQty = BigDecimalUtil.add(summary.getPlanQty(), report.getPlanQty());
			Double totalFinishQty = BigDecimalUtil.add(summary.getFinishQty(), report.getFinishQty());
			summary.setPlanQty(totalPlanQty);
			summary.setFinishQty(totalFinishQty);
			summary.setPlanSpec(String.valueOf(totalPlanSpec.longValue()));
			summary.setFinishSpec(String.valueOf(totalFinishSpec.longValue()));
			summary.setIsSpecNumber(ZltConstant.STATUS_DISABLE);
		}

		// 重算汇总行的完成率
		for (ScheduleReportDto summary : summaryMap.values()) {
			BigDecimal planQty = BigDecimalUtil.valueOf(summary.getPlanQty());
			BigDecimal finishQty = BigDecimalUtil.valueOf(summary.getFinishQty());
			BigDecimal planSpec = new BigDecimal(summary.getPlanSpec());
			BigDecimal finishSpec = new BigDecimal(summary.getFinishSpec());

			// 完成率 = 完成 / 计划，要校验除0的异常，计划为0完成率也按0算
			BigDecimal qtyFinishRate = planQty.compareTo(BigDecimal.ZERO) != 0
					? finishQty.multiply(ONE_HUNDRED).divide(planQty, 2, RoundingMode.HALF_UP)
					: BigDecimal.ZERO;
			BigDecimal specRate = planSpec.compareTo(BigDecimal.ZERO) != 0
					? finishSpec.multiply(ONE_HUNDRED).divide(planSpec, 2, RoundingMode.HALF_UP)
					: BigDecimal.ZERO;

			summary.setQtyFinishRate(qtyFinishRate.toString() + "%");
			summary.setSpecRate(specRate.toString() + "%");

			if (isSummary) {
				summary.setScheduleDate(summaryString);
				summary.setMixArea(StringUtils.EMPTY);
				summary.setIsSummary(ZltConstant.STATUS_DISABLE);
			}
		}
		return summaryMap.values().stream().sorted(Comparator.comparing(ScheduleReportDto::getScheduleDate) // 排产日升序
				.thenComparing(Comparator.comparing(ScheduleReportDto::getMixArea)) // 密炼区升序
				.thenComparing(Comparator.comparing(ScheduleReportDto::getProcedure, Comparator.reverseOrder()))) // 工序倒序
				.collect(Collectors.toList());
	}
}
