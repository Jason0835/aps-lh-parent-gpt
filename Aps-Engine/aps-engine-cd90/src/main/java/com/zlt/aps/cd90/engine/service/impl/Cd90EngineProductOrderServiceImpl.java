package com.zlt.aps.cd90.engine.service.impl;

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
import com.zlt.aps.cd90.api.domain.entity.Cd90ScheduleResult;
import com.zlt.aps.cd90.engine.mapper.Cd90EngineProductOrderMapper;
import com.zlt.aps.cd90.engine.service.Cd90EngineProductOrderService;
import com.zlt.aps.cd90.engine.vo.Cd90ScheduleResultVo;
import com.zlt.aps.common.engine.service.AutoScheduleLogService;
import com.zlt.aps.common.engine.utils.CollectionUtil;
import com.zlt.aps.common.engine.utils.GenerageMapKeyUtils;

/**
 * 90度裁断排产顺序服务接口
 * 
 * @Description
 * @Author hakimrayn
 * @Date 2021-7-14 16:12:32
 * @Version 1.0
 */
@Service("cd90EngineProductOrderService")
public class Cd90EngineProductOrderServiceImpl implements Cd90EngineProductOrderService {
	@Resource
	private AutoScheduleLogService autoScheduleLogService;
	@Autowired
	private Cd90EngineProductOrderMapper cd90EngineProductOrderMapper;

	/**
	 * 根据帘布大卷、可供时长排序，计算排产结果的生产顺序
	 * 
	 * @Author hakimryan
	 * @Description
	 * @Date 2021-7-14 16:13:28
	 * @Param
	 * @Return
	 */
	@Override
	public void calculateProduceOrder(List<Cd90ScheduleResultVo> scheduleList) {
		// 添加日志
		String batchNo = CollectionUtil.firstElement(scheduleList).getBatchNo();
		autoScheduleLogService.insertCd90ScheduleLog(batchNo, "", "6.1、设置生产顺序字段",
				"按机台、大卷分组，再组内按库存供应时长(从小到大)，设置中班和夜班的生产顺序（有计划量的才设置生产顺序）");
		// 先按机台、大卷分组
		Map<String, List<Cd90ScheduleResultVo>> resultMap = scheduleList.stream()
				// 过滤掉无机台与多机台的情况
				.filter(r -> StringUtils.isNotBlank(r.getMachineId()) && !r.getMachineId().contains(","))
				// 按机台ID，大卷编号分组
				.collect(Collectors.groupingBy(r -> this.createGroupKey(r)));

		// 组内单独排序
		for (List<Cd90ScheduleResultVo> resultList : resultMap.values()) {
			// 先排产结果按供应时长排序
			List<Cd90ScheduleResultVo> sortScheduleList = resultList.stream().sorted(Comparator
					// 先按供应时长正序排序，没有供应时长的（插单）放最后
					.comparing(Cd90ScheduleResultVo::getSupplyTime, Comparator.nullsLast(Double::compareTo))
					// 如果供应时长相等，则按开始班次顺序排序
					.thenComparing(this.createProductClassSorter())
					// 如果开始班次相等，则按该班次的成型排程量倒序排序
					.thenComparing(this.createCxPlanNumSorter())).collect(Collectors.toList());
			// 中班与晚班生产顺序分开，初始值为1；
			long daySortNumer = 1;
			long nightSortNumber = 1;
			for (Cd90ScheduleResultVo resultVo : sortScheduleList) {
				// 只有有排计划量的排程才需要设置生产顺序
				if (resultVo.getDayPlanQty() > 0) {
					resultVo.setDayProduceOrder(daySortNumer);
					daySortNumer++;
				} else {
					// 为零的时候需要清空（导入的情况下）
					resultVo.setDayProduceOrder(null);
				}
				if (resultVo.getNightPlanQty() > 0) {
					resultVo.setNightProduceOrder(nightSortNumber);
					nightSortNumber++;
				} else {
					// 为零的时候需要清空（导入的情况下）
					resultVo.setNightProduceOrder(null);
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
	public void recalculateProduceOrder(Cd90ScheduleResult params) {
		// 获取需要重算生产顺序的15度裁断排程结果列表
		List<Cd90ScheduleResultVo> resultList = cd90EngineProductOrderMapper.selectCd90ScheduleList(params);
		// 计算排产顺序
		this.calculateProduceOrder(resultList);
		cd90EngineProductOrderMapper.updatCd90ScheduleResultOrder(resultList);
	}

	/**
	 * 创建15度裁断排产结果排序分组key，格式：机台id:大卷编号
	 * 
	 * @param result
	 * @return
	 */
	private String createGroupKey(Cd90ScheduleResultVo result) {
		return GenerageMapKeyUtils.createMapKey(result.getMachineId(), result.getBigRollCode());
	}

	/**
	 * 创建查询结果排序器——成产班次<br/>
	 * 排序规则：按开始班次正序排序
	 * 
	 * @return
	 */
	private Comparator<Cd90ScheduleResultVo> createProductClassSorter() {
		return new Comparator<Cd90ScheduleResultVo>() {
			@Override
			public int compare(Cd90ScheduleResultVo o1, Cd90ScheduleResultVo o2) {
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
	private int getStartClass(Cd90ScheduleResultVo result) {
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
	private Comparator<Cd90ScheduleResultVo> createCxPlanNumSorter() {
		return new Comparator<Cd90ScheduleResultVo>() {
			@Override
			public int compare(Cd90ScheduleResultVo o1, Cd90ScheduleResultVo o2) {
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
	private double getCxPlanNum(Cd90ScheduleResultVo result) {
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
