package com.zlt.aps.cd15.engine.service.impl;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.annotation.Resource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ruoyi.common.utils.StringUtils;
import com.zlt.aps.cd15.api.domain.entity.Cd15ScheduleResult;
import com.zlt.aps.cd15.engine.mapper.Cd15EngineProductOrderMapper;
import com.zlt.aps.cd15.engine.service.Cd15EngineProductOrderService;
import com.zlt.aps.cd15.engine.vo.Cd15ScheduleResultVo;
import com.zlt.aps.common.engine.service.AutoScheduleLogService;
import com.zlt.aps.common.engine.utils.CollectionUtil;
import com.zlt.aps.common.engine.utils.GenerageMapKeyUtils;

/**
 * 15度裁断排产顺序服务接口
 * 
 * @Description
 * @Author hakimrayn
 * @Date 2021-7-10 16:12:32
 * @Version 1.0
 */
@Service("cd15EngineProductOrderService")
public class Cd15EngineProductOrderServiceImpl implements Cd15EngineProductOrderService {
	@Resource
	private AutoScheduleLogService autoScheduleLogService;
	@Autowired
	private Cd15EngineProductOrderMapper cd15EngineProductOrderMapper;

	/**
	 * 根据钢压大卷、裁断角度排序，计算排产结果的生产顺序
	 * 
	 * @Author hakimryan
	 * @Description
	 * @Date 2021-7-10 16:13:28
	 * @Param
	 * @Return
	 */
	@Override
	public void calculateProduceOrder(List<Cd15ScheduleResultVo> scheduleList) {
		// 添加日志
		String batchNo = CollectionUtil.firstElement(scheduleList).getBatchNo();
		autoScheduleLogService.insertCd15ScheduleLog(batchNo, "", "6.1、设置生产顺序字段",
				"按机台、大卷分组，再组内按库存供应时长(从小到大)，设置中班和夜班的生产顺序（有计划量的才设置生产顺序）");
		// 先按机台、大卷分组
		Map<String, List<Cd15ScheduleResultVo>> resultMap = scheduleList.stream()
				// 过滤掉无机台与多机台的情况
				.filter(r -> StringUtils.isNotBlank(r.getMachineId()) && !r.getMachineId().contains(","))
				// 按机台ID，大卷编号分组
				.collect(Collectors.groupingBy(r -> this.createGroupKey(r)));

		// 组内单独排序
		for (List<Cd15ScheduleResultVo> resultList : resultMap.values()) {
			// 排产结果排序
			List<Cd15ScheduleResultVo> sortScheduleList = resultList.stream().sorted(Comparator
					// 先按供应时长正序排序，没有供应时长（插单）的放最后
					.comparing(Cd15ScheduleResultVo::getSupplyTime1, Comparator.nullsLast(Double::compareTo))
					// 如果供应时长相等，则按开始班次正序排序
					.thenComparing(this.createProductClassSorter())
					// 如果开始班次相等，则按该班次的成型排程量倒序排序
					.thenComparing(this.createCxPlanNumSorter())).collect(Collectors.toList());

			// 中班与晚班生产顺序分开，初始值为1；
			long daySortNumer = 1;
			long nightSortNumber = 1;
			for (Cd15ScheduleResultVo resultVo : sortScheduleList) {
				// 只有有排计划量的排程才需要设置生产顺序
				if (resultVo.getDayPlanQty1() > 0) {
					resultVo.setDayProduceOrder1(daySortNumer);
					daySortNumer++;
				} else {
					// 为零的时候需要清空（导入的情况下）
					resultVo.setDayProduceOrder1(null);
				}
				if (resultVo.getNightPlanQty1() > 0) {
					resultVo.setNightProduceOrder1(nightSortNumber);
					nightSortNumber++;
				} else {
					// 为零的时候需要清空（导入的情况下）
					resultVo.setNightProduceOrder1(null);
				}
			}
		}
	}

	/**
	 * 重算排产结果的生产顺序
	 * 
	 * @Param 需要重算的排程结果过滤条件
	 * @Return
	 */
	@Transactional
	@Override
	public void recalculateProduceOrder(Cd15ScheduleResult params) {
		// 获取需要重算生产顺序的15度裁断排程结果列表
		List<Cd15ScheduleResultVo> resultList = cd15EngineProductOrderMapper.selectCd15ScheduleList(params);
		// 计算排产顺序
		this.calculateProduceOrder(resultList);
		cd15EngineProductOrderMapper.updatCd15ScheduleResultOrder(resultList);
	}

	/**
	 * 创建15度裁断排产结果排序分组key，格式：机台id:大卷编号
	 * 
	 * @param result
	 * @return
	 */
	private String createGroupKey(Cd15ScheduleResultVo result) {
		return GenerageMapKeyUtils.createMapKey(result.getMachineId(), result.getBigRollCode());
	}

	/**
	 * 创建查询结果排序器——成产班次<br/>
	 * 排序规则：按开始班次正序排序
	 * 
	 * @return
	 */
	private Comparator<Cd15ScheduleResultVo> createProductClassSorter() {
		return new Comparator<Cd15ScheduleResultVo>() {
			@Override
			public int compare(Cd15ScheduleResultVo o1, Cd15ScheduleResultVo o2) {
				// 新排程结果的开始班次
				int startClass1 = getStartClass(o1);
				// 原排程结果的开始班次
				int startClass2 = getStartClass(o2);
				// 开始班次较小的在前
				return Integer.valueOf(startClass1).compareTo(startClass2);
			}
		};
	}

	/**
	 * 获取本规格的开始生产班次
	 * 
	 * @param result
	 * @return
	 */
	private int getStartClass(Cd15ScheduleResultVo result) {
		int startClass = 1;
		// 从1班开始遍历每个班次计划量，大于0的即为开始生产班次
		if (Optional.ofNullable(result.getCxClass1Plan()).orElse(0d) > 0) {
			return startClass;
		}
		startClass++;
		if (Optional.ofNullable(result.getCxClass2Plan()).orElse(0d) > 0) {
			return startClass;
		}
		startClass++;
		if (Optional.ofNullable(result.getCxClass3Plan()).orElse(0d) > 0) {
			return startClass;
		}
		startClass++;
		if (Optional.ofNullable(result.getCxClass4Plan()).orElse(0d) > 0) {
			return startClass;
		}
		return ++startClass;
	}

	/**
	 * 创建查询结果排序器——成型计划量<br/>
	 * 排序规则：按同班次的成型计划量倒序排序
	 * 
	 * @return
	 */
	private Comparator<Cd15ScheduleResultVo> createCxPlanNumSorter() {
		return new Comparator<Cd15ScheduleResultVo>() {
			@Override
			public int compare(Cd15ScheduleResultVo o1, Cd15ScheduleResultVo o2) {
				// 新排程结果的开始生产班次的计划量
				double cxPlanNum1 = getCxPlanNum(o1);
				// 原排程结果的开始生产班次的计划量
				double cxPlanNum2 = getCxPlanNum(o2);
				// 计划量较大的在前
				return Double.valueOf(cxPlanNum2).compareTo(cxPlanNum1);
			}
		};
	}

	/**
	 * 获取本规格开始生产那一班的计划量
	 * 
	 * @param result
	 * @return
	 */
	private double getCxPlanNum(Cd15ScheduleResultVo result) {
		// 从1班开始遍历每个班次计划量，取出第一个计划量大于0的数值
		double planNum = Optional.ofNullable(result.getCxClass1Plan()).orElse(0d);
		if (planNum > 0) {
			return planNum;
		}
		planNum = Optional.ofNullable(result.getCxClass2Plan()).orElse(0d);
		if (planNum > 0) {
			return planNum;
		}
		planNum = Optional.ofNullable(result.getCxClass3Plan()).orElse(0d);
		if (planNum > 0) {
			return planNum;
		}
		planNum = Optional.ofNullable(result.getCxClass4Plan()).orElse(0d);
		if (planNum > 0) {
			return planNum;
		}
		return Optional.ofNullable(result.getCxClass5Plan()).orElse(0d);
	}
}
