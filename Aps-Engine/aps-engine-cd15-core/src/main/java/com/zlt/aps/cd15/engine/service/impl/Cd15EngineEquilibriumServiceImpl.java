package com.zlt.aps.cd15.engine.service.impl;

import static com.alibaba.fastjson.JSON.toJSONString;
import static com.zlt.aps.common.core.utils.ApsCommonUtil.getDoubleOrDefault;
import static com.zlt.aps.common.core.utils.ApsCommonUtil.logSplit;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.annotation.Resource;

import org.springframework.stereotype.Service;

import com.ruoyi.common.utils.StringUtils;
import com.zlt.aps.cd15.engine.service.Cd15EngineEquilibriumService;
import com.zlt.aps.cd15.engine.vo.Cd15EquilibriumVo;
import com.zlt.aps.cd15.engine.vo.Cd15ScheduleResultVo;
import com.zlt.aps.common.core.utils.BigDecimalUtil;
import com.zlt.aps.common.engine.service.AutoScheduleLogService;
import com.zlt.aps.common.engine.utils.CollectionUtil;

/**
 * 15度裁断排产均衡服务接口
 * 
 * @Description
 * @Author hakimrayn
 * @Date 2021-7-9 15:26:28
 * @Version 1.0
 */
@Service("cd15EngineEquilibriumService")
public class Cd15EngineEquilibriumServiceImpl implements Cd15EngineEquilibriumService {
	// 默认中班总量和夜班总量差额百分比：15%
	private static final Double DEFAULT_PLAN_DIFFERENCE_RATE = 15D;
	// 默认库存供应时长小时数：12小时
	private static final Double DEFAULT_SUPPLY_TIME_PASS = 12D;
	// 一百，用于百分数 -> 小数的单位换算
	private static final BigDecimal ONE_HUNDRED = new BigDecimal("100");
	@Resource
	private AutoScheduleLogService autoScheduleLogService;

	/**
	 * 均衡处理 检测中班与晚班的差异率是否超过最大差异率，超过则尽量尝试将多的一部分产量转移（中班转移到晚班，或者晚班转移到中班）
	 * 
	 * @Author hakimryan
	 * @Description
	 * @Date 2021-7-9 15:27:55
	 * @Param scheduleList 15度裁断排产结果
	 * @Param planDifferenceRate 系统参数：中班总量和夜班总量差额百分比
	 * @Param supplyTimePass 系统参数：库存供应时长小时数
	 * @Param equalShareThreshold 系统参数：各班计划量均分阈值
	 * @Return
	 */
	@Override
	public void scheduleEquilibrium(List<Cd15ScheduleResultVo> scheduleList, String planDifferenceRate,
			String supplyTimePass, String equalShareThreshold) {
		// 系统参数类型转换
		BigDecimal planDifferenceRateNum = BigDecimal
				.valueOf(getDoubleOrDefault(planDifferenceRate, DEFAULT_PLAN_DIFFERENCE_RATE));
		// 百分数转成小数
		planDifferenceRateNum = planDifferenceRateNum.divide(ONE_HUNDRED);
		BigDecimal supplyTimePassNum = BigDecimal.valueOf(getDoubleOrDefault(supplyTimePass, DEFAULT_SUPPLY_TIME_PASS));
		// 均衡运算前的排产结果，用于日志记录
		String oldScheduleList = toJSONString(scheduleList);
		// 批次号
		String batchNo = CollectionUtil.firstElement(scheduleList).getBatchNo();
		// 均衡算法需要根据每个机台单独均衡 modify by 20220113
		Map<String, List<Cd15ScheduleResultVo>> scheduleMachineMap = scheduleList.stream()
				// 过滤掉无机台或者多机台的排产记录
				.filter(s -> StringUtils.isNotEmpty(s.getMachineId()) && !s.getMachineId().contains(","))
				// 按机台ID分组
				.collect(Collectors.groupingBy(Cd15ScheduleResultVo::getMachineId));
		for (List<Cd15ScheduleResultVo> groupingList : scheduleMachineMap.values()) {
			// 各机台单独均衡
			this.equilibriumSingleMachine(groupingList, planDifferenceRateNum, supplyTimePassNum);
		}
		this.equalShare(scheduleList, equalShareThreshold);
		// 新增计算日志
		this.insertCalculateLog(batchNo, oldScheduleList, scheduleList, planDifferenceRate, supplyTimePass,
				this.createEquilibrimeVo(scheduleList));
	}

	/**
	 * 单规格排产数量达到设定值（equalShareThreshold）时，中夜班数量对半分
	 * 
	 * @param scheduleList        排程列表
	 * @param equalShareThreshold 各班计划量均分阈值
	 */
	private void equalShare(List<Cd15ScheduleResultVo> scheduleList, String equalShareThreshold) {
		if (StringUtils.isBlank(equalShareThreshold)) {
			return;
		}
		Integer threshold = Integer.parseInt(equalShareThreshold);
		for (Cd15ScheduleResultVo schedule : scheduleList) {
			// 一天总计划量
			Double totalPlay = BigDecimalUtil.add(schedule.getDayPlanQty1(), schedule.getNightPlanQty1());
			if (totalPlay >= threshold) {
				Double equalSharePlan = BigDecimalUtil.div(totalPlay, 2);
				// 均分后，中班向上取整
				schedule.setDayPlanQty1(BigDecimalUtil.roundUp(equalSharePlan, 0));
				// 均分后，夜班向下取整
				schedule.setNightPlanQty1(BigDecimalUtil.roundDown(equalSharePlan, 0));
			}
		}
	}

	/**
	 * 各机台单独做排产均衡均衡
	 * 
	 * @param scheduleList          同一个机台的所有排程信息
	 * @param planDifferenceRateNum 中班总量和夜班总量差额百分比
	 * @param supplyTimePassNum     库存供应时长小时数
	 */
	private void equilibriumSingleMachine(List<Cd15ScheduleResultVo> scheduleList, BigDecimal planDifferenceRateNum,
			BigDecimal supplyTimePassNum) {
		// 构建均衡值对象，用于计算是否均衡
		Cd15EquilibriumVo equilibriumVo = this.createEquilibrimeVo(scheduleList);
		// 是否中班比晚班的计划量多
		boolean isDayQtyMore = equilibriumVo.getDayPlanQty().compareTo(equilibriumVo.getNightPlanQty()) > 0;
		if (isDayQtyMore) {
			// 中班较大，按中班计划量从小到大排序
			scheduleList = scheduleList.stream().sorted(Comparator.comparing(Cd15ScheduleResultVo::getDayPlanQty1))
					.collect(Collectors.toList());
		} else {
			// 晚班较大，按晚班计划量从小到大排序
			scheduleList = scheduleList.stream().sorted(Comparator.comparing(Cd15ScheduleResultVo::getNightPlanQty1))
					.collect(Collectors.toList());
		}
		// 记录上一次的差异率，初始化为第一次比对的差异率
		BigDecimal lastDifferenceRate = equilibriumVo.getDifferenceRate();
		// 如果差异率超过临界值，说明不均衡，需要做均衡处理
		for (Cd15ScheduleResultVo scheduleVo : scheduleList) {
			// 取出中班与晚班的计划量
			BigDecimal dayPlanQty1 = BigDecimal.valueOf(scheduleVo.getDayPlanQty1());
			BigDecimal nightPlanQty1 = BigDecimal.valueOf(scheduleVo.getNightPlanQty1());
			// 可供时长（插单数据时长为空）
			BigDecimal supplyTime = BigDecimal.valueOf(Optional.ofNullable(scheduleVo.getSupplyTime1()).orElse(0D));
			// 是否有做转移的标志
			boolean isChange = false;
			// 开始判断转移
			if (isDayQtyMore) {
				// 如果中班多于晚班，则尝试将一笔SUPPLY_TIME（可供时长） > 12的中班计划量转移到晚班计划量中
				if (dayPlanQty1.compareTo(BigDecimal.ZERO) > 0 && supplyTime.compareTo(supplyTimePassNum) > 0) {
					this.changeDayAndNightPlanQty(scheduleVo);
					isChange = true;
				}
			} else {
				// 如果中班少于晚班，则尝试将一笔晚班计划量转移到中班计划量中；
				if (nightPlanQty1.compareTo(BigDecimal.ZERO) > 0) {
					this.changeDayAndNightPlanQty(scheduleVo);
					isChange = true;
				}
			}
			if (isChange) {
				// 构建均衡值对象，判断是否均衡
				Cd15EquilibriumVo scheduleEquilibriumVo = this.createEquilibrimeVo(scheduleList);
				BigDecimal currentDifferenceRate = scheduleEquilibriumVo.getDifferenceRate();
				if (currentDifferenceRate == null) {
					// 如果本次运算后没有计算出差异率，说明本次转移将一个班的所有计划量全转移到另一班了，因此无法计算差异率（差异率无穷大）
					// 此情况需取消本次转移，还原中班与晚班的计划量，并结束运算
					this.changeDayAndNightPlanQty(scheduleVo);
					break;
				} else if (lastDifferenceRate != null && currentDifferenceRate.compareTo(lastDifferenceRate) > 0) {
					// 如果不均衡，则判断是否比上一次的差异率大，如果大了，则取消本次转移，保留上一次的运算结果，并结束运算
					// 还原中班与晚班的计划量
					this.changeDayAndNightPlanQty(scheduleVo);
					break;
				} else {
					// 如果上述情况均不符合，则保留本次运算结果，继续判断下一笔
					lastDifferenceRate = currentDifferenceRate;
				}
			}
		}
	}

	/**
	 * 添加均衡日志
	 * 
	 * @param scheduleList
	 * @param paramsMap
	 * @param totalPlanQtyVo
	 */
	private void insertCalculateLog(String batchNo, String oldScheduleList, List<Cd15ScheduleResultVo> scheduleList,
			String planDifferenceRate, String supplyTimePass, Cd15EquilibriumVo totalPlanQtyVo) {
		String logDetail = logSplit(
				"对排产结果进行均衡操作。中班总量和夜班总量的差额百分比超过了参数配置的百分比，则需要做均衡处理，也就是说要把其中一班的计划量合并到另外一班，"
						+ "一直合并到中班和夜班计划量总量的差额不超过参数配置的百分比。其中中班合并到夜班还需要遵循一个规则，就是只有库存供应时长必须要大于参数配置的值的时候，才允许从中班合并到夜班。",
				"参数配置‘PLAN_DIFFERENCE_RATE（中班总量和夜班总量差额百分比）’:" + planDifferenceRate + "，以及‘SUPPLY_TIME_PASS（库存供应时长小时数）’："
						+ supplyTimePass,
				"各班总计划量：" + toJSONString(totalPlanQtyVo), "均衡前的排程数据列表：" + oldScheduleList,
				"均衡后的排产数据列表：" + toJSONString(scheduleList));
		autoScheduleLogService.insertCd15ScheduleLog(batchNo, "", "5.1、计划量均衡处理", logDetail);
	}

	/**
	 * 调换中班与晚班的计划量
	 * 
	 * @Author hakimryan
	 * @Description
	 * @Date 2021-7-10 10:34:13
	 * @Param scheduleVo 15度排产计划值对象
	 * @Return
	 */
	private void changeDayAndNightPlanQty(Cd15ScheduleResultVo scheduleVo) {
		// 获取中班与晚班的计划量
		Double dayPlanQty = scheduleVo.getDayPlanQty1();
		Double nightPlanQty = scheduleVo.getNightPlanQty1();
		// 交换赋值
		scheduleVo.setDayPlanQty1(nightPlanQty);
		scheduleVo.setNightPlanQty1(dayPlanQty);
	}

	/**
	 * 判断是否均衡
	 * 
	 * @Author hakimryan
	 * @Description
	 * @Date 2021-7-10 10:26:48
	 * @Param differenceRate 差异率
	 * @Param planDifferenceRate 配置差异率标准值
	 * @Return true：以均衡；false：未均衡
	 */
	private boolean isBalance(BigDecimal differenceRate, BigDecimal planDifferenceRate) {
		return differenceRate != null && planDifferenceRate.compareTo(differenceRate) > 0;
	}

	/**
	 * 构建均衡值对象，用于计算目前的排产是否均衡
	 * 
	 * @Author hakimryan
	 * @Description
	 * @Date 2021-7-10 10:26:59
	 * @Param scheduleList 排产记录
	 * @Return
	 */
	private Cd15EquilibriumVo createEquilibrimeVo(List<Cd15ScheduleResultVo> scheduleList) {
		// 将晚班计划量与中班计划量加总
		BigDecimal dayPlanQty = BigDecimal.ZERO;
		BigDecimal nightPlanQty = BigDecimal.ZERO;
		for (Cd15ScheduleResultVo scheduleVo : scheduleList) {
			dayPlanQty = dayPlanQty.add(BigDecimal.valueOf(scheduleVo.getDayPlanQty1()));
			nightPlanQty = nightPlanQty.add(BigDecimal.valueOf(scheduleVo.getNightPlanQty1()));
		}
		// 是否中班比晚班的计划量多
		boolean isDayQtyMore = dayPlanQty.compareTo(nightPlanQty) > 0;
		// 差异率，默认为空
		BigDecimal differenceRate = null;
		// 需要做除数为0的校验。如果除数为0，保持差异率为空
		if (isDayQtyMore && nightPlanQty.compareTo(BigDecimal.ZERO) > 0) {
			// 中班较大，公式（中班 - 晚班）/ 晚班
			differenceRate = dayPlanQty.subtract(nightPlanQty).divide(nightPlanQty, 4, RoundingMode.HALF_UP);
		} else if (!isDayQtyMore && dayPlanQty.compareTo(BigDecimal.ZERO) > 0) {
			// 晚班较大，公式（晚班 - 中班）/ 中班
			differenceRate = nightPlanQty.subtract(dayPlanQty).divide(dayPlanQty, 4, RoundingMode.HALF_UP);
		}
		Cd15EquilibriumVo equilibriumVo = new Cd15EquilibriumVo();
		equilibriumVo.setDayPlanQty(dayPlanQty);
		equilibriumVo.setNightPlanQty(nightPlanQty);
		equilibriumVo.setDifferenceRate(differenceRate);
		return equilibriumVo;
	}

}
