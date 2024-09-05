package com.zlt.aps.cd90.engine.service.impl;

import static com.alibaba.fastjson.JSON.toJSONString;
import static com.zlt.aps.common.core.utils.ApsCommonUtil.getDouble;
import static com.zlt.aps.common.core.utils.ApsCommonUtil.logSplit;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.annotation.Resource;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.zlt.aps.cd90.api.domain.entity.Cd90LineSideStock;
import com.zlt.aps.cd90.api.domain.entity.Cd90MachineInfo;
import com.zlt.aps.cd90.engine.mapper.Cd90EngineBigRollMapper;
import com.zlt.aps.cd90.engine.mapper.Cd90EngineStockMapper;
import com.zlt.aps.cd90.engine.service.Cd90EngineLossService;
import com.zlt.aps.cd90.engine.service.Cd90EnginePlanQtyService;
import com.zlt.aps.cd90.engine.vo.Cd90BigRollVo;
import com.zlt.aps.cd90.engine.vo.Cd90ParamsVo;
import com.zlt.aps.cd90.engine.vo.Cd90ScheduleResultVo;
import com.zlt.aps.cd90.engine.vo.Cd90StockVo;
import com.zlt.aps.common.core.constant.ApsConstant;
import com.zlt.aps.common.core.utils.BigDecimalUtil;
import com.zlt.aps.common.engine.common.CxEngineQuotaCommonService;
import com.zlt.aps.common.engine.constants.EngineConstants;
import com.zlt.aps.common.engine.service.AutoScheduleLogService;
import com.zlt.aps.common.engine.utils.CollectionUtil;
import com.zlt.aps.common.engine.utils.GenerageMapKeyUtils;

/**
 * 90度裁断计划量信息处理服务实现类
 * 
 * @Description
 * @Author hakimrayn
 * @Date 2021-7-14 11:30:42
 * @Version 1.0
 */
@Service("cd90EnginePlanQtyService")
public class Cd90EnginePlanQtyServiceImpl implements Cd90EnginePlanQtyService {
	// 可供时长参数
	private static final BigDecimal SUPPLY_TIME_PARAM = new BigDecimal("8");
	/**
	 * 一千，用于毫米换算成米
	 */
	private static final BigDecimal ONE_THOUSAND = new BigDecimal("1000");
	/**
	 * 幅宽默认值
	 */
	private static final String DEFAULT_BREADTH = "1.45";

	@Autowired
	private Cd90EngineStockMapper cd90EngineStockMapper;
	@Autowired
	private Cd90EngineLossService cd90EngineLossService;
	@Resource
	private CxEngineQuotaCommonService cxEngineQuotaCommonService;
	@Resource
	private AutoScheduleLogService autoScheduleLogService;
	@Autowired
	private Cd90EngineBigRollMapper cd90EngineBigRollMapper;

	/**
	 * 计算排产计划量
	 * 
	 * @Author hakimryan
	 * @Description
	 * @Date 2021-7-14 11:31:01
	 * @Param scheduleDate 排产日期
	 * @Param scheduleList 排产记录
	 * @Param defaultLossRate 默认损耗率
	 * @Param stockLossRate 库存损耗率
	 * @Param isProductionStage 仅对投产阶段规格排产
	 * @Param crimpLength 卷曲长度
	 * @Param minRoundRollNumStr 最小取整卷数
	 */
	@Override
	public void calculateSchedulePlanQty(Date scheduleDate, List<Cd90ScheduleResultVo> scheduleList,
			String defaultLossRate, BigDecimal stockLossRate, boolean isProductionStage, BigDecimal crimpLength,
			BigDecimal minRoundRollNum) {
		// 批次号
		String batchNo = CollectionUtil.firstElement(scheduleList).getBatchNo();
		// 获取库存量
		// 计算公式： (库存量 - 不良数 + 修正数) - (前日三班计划量 - 12点成型完成量) * 单耗
		Map<String, Cd90StockVo> stockMap = this.getStockMap(scheduleDate, stockLossRate, isProductionStage);

		// 获取损耗率设定
		Map<String, Double> lossRateMap = cd90EngineLossService.getLossRateMap();
		// 默认损耗率类型转换
		Double defaultLossRateNum = getDouble(defaultLossRate);
		// 计算库存相关信息：16点半部件库存、可用时长，并根据库存重算计划量
		for (Cd90ScheduleResultVo resultVo : scheduleList) {
			// 计算前的排程数据json字符串，用于日志记录
			String oldScheduleResult = toJSONString(resultVo);
			String clothCode = resultVo.getClothCode();

			// 90度裁断库存信息
			Cd90StockVo stockVo = stockMap.get(clothCode);
			// 16点半部件库存量
			BigDecimal stockQty = stockVo != null && stockVo.getStockQty() != null ? stockVo.getStockQty()
					: BigDecimal.ZERO;
			// 成型可供时长
			BigDecimal supplyTime = this.caculateSuppliyTime(resultVo, stockVo);
			// 处理中夜班量，防止二次投产
			this.handleSecondaryProduct(resultVo, stockQty);
			// 重算计划量
			// 重算后的计划量
			BigDecimal newDayPlanQty = new BigDecimal(resultVo.getDayPlanQty());
			BigDecimal newNightPlanQty = new BigDecimal(resultVo.getNightPlanQty());
			// 获取损耗率
			Double lossRate = cd90EngineLossService.getLossRate(clothCode, resultVo.getMachineId(), lossRateMap,
					defaultLossRateNum);
			// 为弥补损耗的量，计划量需要填补损耗量，计算公式：新计划量 = 原计划量+原计划量*损耗率
			newDayPlanQty = newDayPlanQty.add(newDayPlanQty.multiply(BigDecimal.valueOf(lossRate)));
			newNightPlanQty = newNightPlanQty.add(newNightPlanQty.multiply(BigDecimal.valueOf(lossRate)));

			// 重新赋值计划量给排产明细
			// 结果小数舍入方式调整，modify by 20211230
			resultVo.setDayPlanQty(newDayPlanQty.setScale(0, RoundingMode.UP).doubleValue());
			resultVo.setNightPlanQty(newNightPlanQty.setScale(0, RoundingMode.UP).doubleValue());
			resultVo.setSupplyTime(supplyTime.doubleValue());
			resultVo.setStockQty(stockQty.setScale(0, RoundingMode.DOWN).doubleValue());
			resultVo.setTotalPlanQty(new BigDecimal(
					String.valueOf(BigDecimalUtil.add(resultVo.getDayPlanQty(), resultVo.getNightPlanQty()))));
			resultVo.setCloseOutSpecFlag(ApsConstant.STATUS_DISABLE);

			// 记录计算日志
			this.insertCalculateLog(oldScheduleResult, resultVo, lossRate);
		}

		// 重算大卷数
		this.recaculatePlanNum(scheduleDate, scheduleList, isProductionStage, minRoundRollNum);

		// 记录日志
		String logDetail = logSplit("库存量与成型定额设置：" + toJSONString(stockMap), "损耗率设定：" + toJSONString(lossRateMap));
		autoScheduleLogService.insertCd90ScheduleLog(batchNo, "", "3.1、计算计划量基础数据日志", logDetail);
	}

	/**
	 * 重算大卷数
	 * 
	 * @param scheduleDate      排产日
	 * @param scheduleList      排产列表
	 * @param isProductionStage 是否投产
	 * @param minRoundRollNum   最小卷数
	 */
	private void recaculatePlanNum(Date scheduleDate, List<Cd90ScheduleResultVo> scheduleList,
			boolean isProductionStage, BigDecimal minRoundRollNum) {
		Map<String, String> params = cd90EngineStockMapper.listXwyyParams().stream()
				.collect(Collectors.toMap(Cd90ParamsVo::getParamCode, Cd90ParamsVo::getParamValue));

		// 标准大卷长度默认值
		BigDecimal standardSize = new BigDecimal(params.getOrDefault(EngineConstants.STANDARD_SIZE, "0"));
		BigDecimal breadth = new BigDecimal(params.getOrDefault(EngineConstants.BREADTH, DEFAULT_BREADTH));

		// 取出收尾规格
		List<String> closeOutSpecList = new ArrayList<>();
		cd90EngineStockMapper.listCloseOutSpec(scheduleDate, isProductionStage).forEach(s -> {
			List<String> specList = Arrays.stream(s.split("#")).filter(sp -> StringUtils.isNotEmpty(sp))
					.collect(Collectors.toList());
			if (CollectionUtils.isNotEmpty(specList)) {
				closeOutSpecList.addAll(specList);
			}
		});

		// 线边库库存
		Map<String, List<Cd90LineSideStock>> lineSideStockMap = cd90EngineStockMapper
				.listCd90LineSideStock(scheduleDate).stream().collect(Collectors
						.groupingBy(s -> GenerageMapKeyUtils.createMapKey(s.getMachineCode(), s.getMaterialCode())));
		// 机台map
		Map<Long, String> machineMap = cd90EngineStockMapper.listCd90MachineInfo().stream()
				.collect(Collectors.toMap(Cd90MachineInfo::getId, Cd90MachineInfo::getMachineCode));

		// 抓取钢压大卷基础信息
		List<Cd90BigRollVo> bigRollList = cd90EngineBigRollMapper.listCd90BigRoll();
		Map<String, BigDecimal> bigRollMap = bigRollList.stream().collect(
				Collectors.toMap(Cd90BigRollVo::getBigRollCode, Cd90BigRollVo::getClothLength, (v1, v2) -> v2));

		// 根据大卷对排产计划分组
		Map<String, List<Cd90ScheduleResultVo>> rollScheduleMap = scheduleList.stream()
				.sorted(Comparator.comparing(Cd90ScheduleResultVo::getTotalPlanQty, Comparator.reverseOrder()))
				.collect(Collectors.groupingBy(Cd90ScheduleResultVo::getBigRollCode));

		for (Entry<String, List<Cd90ScheduleResultVo>> rollScheduleEntry : rollScheduleMap.entrySet()) {
			String bigRollCode = rollScheduleEntry.getKey();
			List<Cd90ScheduleResultVo> rollScheduleList = rollScheduleEntry.getValue();
			Map<String, List<Cd90ScheduleResultVo>> stockMachineMap = this.chooseMachineByStock(bigRollCode,
					rollScheduleList, lineSideStockMap, machineMap); // 根据大卷线边库确定机台
			// 收尾规格打标记
			rollScheduleList.forEach(r -> {
				String classOutStatus = closeOutSpecList.contains(r.getClothCode()) ? ApsConstant.STATUS_ENABLE
						: ApsConstant.STATUS_DISABLE;
				r.setCloseOutSpecFlag(classOutStatus);
			});

			for (Entry<String, List<Cd90ScheduleResultVo>> hasStockEntry : stockMachineMap.entrySet()) {
				String machineCode = hasStockEntry.getKey();
				List<Cd90ScheduleResultVo> hasStockList = hasStockEntry.getValue();
				if (hasStockList.stream().noneMatch(s -> ApsConstant.STATUS_DISABLE.equals(s.getCloseOutSpecFlag()))) {
					continue; // 如果已经全部收尾，则不需要做取整操作
				}
				boolean isclassOutSpec = false; // 只要能走到这一步，必定是还没完全收尾

				// 汇总同种大卷的总计划量
				BigDecimal planQty = hasStockList.stream().map(Cd90ScheduleResultVo::getTotalPlanQty)
						.reduce(BigDecimal.ZERO, BigDecimal::add);
				if (Optional.ofNullable(planQty).orElse(BigDecimal.ZERO).compareTo(BigDecimal.ZERO) <= 0) {
					continue;
				}
				BigDecimal requireQty = hasStockList.stream().map(schedule -> this.caculateRollQty(schedule, breadth))
						.reduce(BigDecimal.ZERO, BigDecimal::add); // 计划量换算成大卷需求量
				if (Optional.ofNullable(requireQty).orElse(BigDecimal.ZERO).compareTo(BigDecimal.ZERO) <= 0) {
					continue;
				}
				BigDecimal lineSideStockQty = this.getLinsideStock(machineCode, hasStockList, lineSideStockMap,
						machineMap, requireQty, isclassOutSpec); // 获取线边库数据库存量
				// 重算实际排产计划
				BigDecimal newPlanQty;
				if (lineSideStockQty.compareTo(BigDecimal.ZERO) <= 0) { // 没有线边库，按照设定好的取舍规则（暂定1、2舍弃，3以上取整），取舍后的大卷个数按照系统设置的大卷设置的大卷参数长度，根据计算得出的米数进行计划下达
					// 大卷长度
					BigDecimal clothLenght = bigRollMap.getOrDefault(bigRollCode, standardSize);
					if (clothLenght.compareTo(BigDecimal.ZERO) == 0) {
						continue;
					}
					BigDecimal planNum = planQty.divide(clothLenght, 1, RoundingMode.UP); // 大卷数，保留1位小数
					// 大卷数小数部分处理
					if (planNum.subtract(planNum.setScale(0, RoundingMode.DOWN)).compareTo(minRoundRollNum) >= 0) {
						planNum = planNum.setScale(0, RoundingMode.UP); // 如果小数部分大于等于最小取整卷数，小数部分
					} else if (planNum.compareTo(minRoundRollNum) < 0) {
						planNum = planNum.setScale(0, RoundingMode.UP); // 如果原计划卷数比最小取整卷数少，直接进位
					} else {
						planNum = planNum.setScale(0, RoundingMode.DOWN); // 其余情况舍去小数部分
					}
					newPlanQty = planNum.multiply(clothLenght).setScale(0, RoundingMode.UP); // 新计划量
				} else if (requireQty.compareTo(lineSideStockQty) == 0) { // 如果线边库库存组合刚好等于需求两，则不需要处理
					newPlanQty = planQty;
				} else { // 有线边库库存，将线边库换算成计划量
					newPlanQty = hasStockList.stream()
							.map(schedule -> this.caculatePlanQty(schedule, lineSideStockQty, requireQty))
							.reduce(BigDecimal.ZERO, BigDecimal::add);
				}

				BigDecimal defferentPlanQty = newPlanQty.subtract(planQty); // 新计划 - 原计划得到的差值
				if (defferentPlanQty.compareTo(BigDecimal.ZERO) == 0) {
					continue; // 两计划无差别，则直接跳过
				}

				if (defferentPlanQty.compareTo(BigDecimal.ZERO) > 0) {
					// 新计划较大，将计划量直接加到计划量最大那一班中
					for (Cd90ScheduleResultVo schedule : rollScheduleList) {
						if (ApsConstant.STATUS_ENABLE.equals(schedule.getCloseOutSpecFlag())) { // 已收尾的不动计划量
							continue;
						}
						BigDecimal oldPlanQty;
						if (schedule.getDayPlanQty() > 0) {
							oldPlanQty = BigDecimal.valueOf(schedule.getDayPlanQty());
							schedule.setDayPlanQty(defferentPlanQty.add(oldPlanQty).doubleValue());
						} else {
							oldPlanQty = BigDecimal.valueOf(schedule.getNightPlanQty());
							schedule.setNightPlanQty(defferentPlanQty.add(oldPlanQty).doubleValue());
						}
						break;
					}
				} else {
					BigDecimal surplusQty = defferentPlanQty;// 剩余量
					// 新计划较小，从计划量最大的一班开始扣减，不够则从计划量第二大的一班开始扣减，依此类推
					for (Cd90ScheduleResultVo schedule : rollScheduleList) {
						if (ApsConstant.STATUS_ENABLE.equals(schedule.getCloseOutSpecFlag())) { // 已收尾的不动计划量
							continue;
						}
						BigDecimal oldPlanQty = schedule.getDayPlanQty() > 0
								? BigDecimal.valueOf(schedule.getDayPlanQty())
								: BigDecimal.valueOf(schedule.getNightPlanQty());
						BigDecimal reduceQty = surplusQty.compareTo(oldPlanQty) > 0 ? oldPlanQty : surplusQty;
						surplusQty = surplusQty.subtract(reduceQty);
						Double finalPlanQty = oldPlanQty.add(reduceQty).doubleValue();
						if (schedule.getDayPlanQty() > 0) {
							schedule.setDayPlanQty(finalPlanQty);
						} else {
							schedule.setNightPlanQty(finalPlanQty);
						}
					}
				}
			}
		}
	}

	/**
	 * 计算需要消耗的大卷米数
	 * 
	 * @param schedule 排产记录
	 * @param breadth  幅宽
	 * @return
	 */
	public BigDecimal caculateRollQty(Cd90ScheduleResultVo schedule, BigDecimal breadth) {
		if (breadth.compareTo(BigDecimal.ZERO) <= 0) {
			breadth = new BigDecimal(DEFAULT_BREADTH);
		}
		// 大卷米数 = 钢带计划量 * (工艺 + 工艺) / 幅宽
		BigDecimal planQty = schedule.getTotalPlanQty();
		BigDecimal craft = BigDecimal.ZERO;
		if (NumberUtils.isDigits(schedule.getCraft())) {
			craft = new BigDecimal(schedule.getCraft()).divide(ONE_THOUSAND);
		}
		return planQty.multiply(craft).divide(breadth, 0, RoundingMode.UP);
	}

	/**
	 * 计算可以排产的钢带米数
	 * 
	 * @param schedule   排产记录
	 * @param stockQty   需求规格的大卷米数
	 * @param requireQty 需求规格的大卷米数
	 * @return
	 */
	public BigDecimal caculatePlanQty(Cd90ScheduleResultVo schedule, BigDecimal stockQty, BigDecimal requireQty) {
		if (requireQty.compareTo(BigDecimal.ZERO) == 0) {
			return BigDecimal.ZERO;
		}
		BigDecimal totalPlanQty = schedule.getTotalPlanQty();
		BigDecimal craft = BigDecimal.ZERO;
		if (NumberUtils.isDigits(schedule.getCraft())) {
			craft = new BigDecimal(schedule.getCraft()).divide(ONE_THOUSAND);
		}
		if (craft.compareTo(BigDecimal.ZERO) == 0) {
			return BigDecimal.ZERO;
		}
		return stockQty.multiply(totalPlanQty).divide(requireQty, 0, RoundingMode.UP); // 计算分配给本胶料的库存数
	}

	/**
	 * 根据线边库库存选择机台
	 * 
	 * @param bigRollCode      大卷编号
	 * @param scheduleList     待分配计划
	 * @param lineSideStockMap 线边库库存信息
	 * @param machineMap       机台信息
	 * @return
	 */
	private Map<String, List<Cd90ScheduleResultVo>> chooseMachineByStock(String bigRollCode,
			List<Cd90ScheduleResultVo> scheduleList, Map<String, List<Cd90LineSideStock>> lineSideStockMap,
			Map<Long, String> machineMap) {
		Map<String, List<Cd90ScheduleResultVo>> hasStockMap = new HashMap<>();
		for (Cd90ScheduleResultVo schedule : scheduleList) {
			String machineCode = StringUtils.EMPTY;
			if (schedule.getMachineId() != null) { // 有机台才需要关联线边库库存
				for (String machineId : StringUtils.split(schedule.getMachineId(), ",")) { // 如果有多个机台，则选定有线边库的机台作为生产机台
					if (NumberUtils.isDigits(machineId)) {
						String tempMachineCode = machineMap.get(new Long(machineId));
						List<Cd90LineSideStock> stockList = lineSideStockMap
								.get(GenerageMapKeyUtils.createMapKey(tempMachineCode, bigRollCode));
						if (stockList != null && stockList.stream()
								.anyMatch(stock -> stock.getStockNum().compareTo(BigDecimal.ZERO) > 0)) {
							machineCode = tempMachineCode;
							break;
						}
					}
				}
			}
			List<Cd90ScheduleResultVo> hasStockList = hasStockMap.get(machineCode); // 无论有没有机台都按机台分组
			if (CollectionUtil.isEmpty(hasStockList)) {
				hasStockList = new ArrayList<>();
				hasStockMap.put(machineCode, hasStockList);
			}
			hasStockList.add(schedule);
		}
		return hasStockMap;
	}

	/**
	 * 计算成型可供时长
	 * 
	 * @param resultVo 排产结果
	 * @param stockVo  库存信息
	 * @return
	 */
	@Override
	public BigDecimal caculateSuppliyTime(Cd90ScheduleResultVo resultVo, Cd90StockVo stockVo) {
		// 16点半部件库存量
		BigDecimal stockQty = Optional.ofNullable(stockVo).map(Cd90StockVo::getStockQty).orElse(BigDecimal.ZERO);
		// 库存消耗量
		BigDecimal stockConsume = BigDecimal.ZERO;
		BigDecimal supplyTime = BigDecimal.ZERO;
		// 剩余库存，不足以支持8个小时的库存量
		Double remainStock = 0D;

		out: {
			Double class1Plan = Optional.ofNullable(resultVo.getCxClass1Plan()).orElse(0d);
			if (class1Plan <= stockQty.subtract(stockConsume).doubleValue()) {
				// 比较剩余库存与计划量，库存较大说明可以支持本班完成生产，因此可供时长至少能支持8个小时
				stockConsume = stockConsume.add(new BigDecimal(class1Plan));
				supplyTime = SUPPLY_TIME_PARAM;
			} else {
				remainStock = class1Plan;
				break out;
			}

			Double class2Plan = Optional.ofNullable(resultVo.getCxClass2Plan()).orElse(0d);
			if (class2Plan <= stockQty.subtract(stockConsume).doubleValue()) {
				stockConsume = stockConsume.add(new BigDecimal(class2Plan));
				supplyTime = supplyTime.add(SUPPLY_TIME_PARAM);
			} else {
				remainStock = class2Plan;
				break out;
			}

			Double class3Plan = Optional.ofNullable(resultVo.getCxClass3Plan()).orElse(0d);
			if (class3Plan <= stockQty.subtract(stockConsume).doubleValue()) {
				stockConsume = stockConsume.add(new BigDecimal(class3Plan));
				supplyTime = supplyTime.add(SUPPLY_TIME_PARAM);
			} else {
				remainStock = class3Plan;
				break out;
			}

			Double class4Plan = Optional.ofNullable(resultVo.getCxClass4Plan()).orElse(0d);
			if (class4Plan <= stockQty.subtract(stockConsume).doubleValue()) {
				stockConsume = stockConsume.add(new BigDecimal(class4Plan));
				supplyTime = supplyTime.add(SUPPLY_TIME_PARAM);
			} else {
				remainStock = class4Plan;
				break out;
			}

			Double class5Plan = Optional.ofNullable(resultVo.getCxClass5Plan()).orElse(0d);
			if (class5Plan <= stockQty.subtract(stockConsume).doubleValue()) {
				stockConsume = stockConsume.add(new BigDecimal(class5Plan));
				supplyTime = supplyTime.add(SUPPLY_TIME_PARAM);
			} else {
				remainStock = class5Plan;
				break out;
			}
		}

		if (remainStock != 0) {
			// 如果有剩余不足8小时供应时长的库存，则按比例计算可供时长，公式：该班预计库存*8/该班计划
			BigDecimal remainSupplyTime = stockQty.subtract(stockConsume).multiply(SUPPLY_TIME_PARAM)
					.divide(new BigDecimal(remainStock), 1, RoundingMode.DOWN);
			supplyTime = supplyTime.add(remainSupplyTime);
		}

		return supplyTime;
	}

	/**
	 * 处理排产计划量，防止出现二次投产
	 * 
	 * @param resultVo 排产信息
	 * @param stockQty 库存量，自动排程时需要传入，手工平衡时可以放空
	 */
	@Override
	public void handleSecondaryProduct(Cd90ScheduleResultVo resultVo, BigDecimal stockQty) {
		// 原中班计划量
		BigDecimal dayPlanQty = BigDecimal.valueOf(resultVo.getDayPlanQty());
		// 原晚班计划量
		BigDecimal nightPlanQty = BigDecimal.valueOf(resultVo.getNightPlanQty());
		// 空值处理
		stockQty = stockQty == null ? BigDecimal.ZERO : stockQty;

		// 重算后的计划量
		BigDecimal newDayPlanQty;
		BigDecimal newNightPlanQty;
		// 如果 原中班计划量>库存，则 中班计划量 = 原中班计划量 -库存；晚班计划量 = 原晚班计划量
		// 如果 原中班计划量<=库存，则 中班计划量 = 0；晚班计划量 = （原中班计划量+原晚班计划量-库存）
		if (dayPlanQty.compareTo(stockQty) > 0) {
			newDayPlanQty = dayPlanQty.subtract(stockQty);
			newNightPlanQty = nightPlanQty;
		} else {
			newDayPlanQty = BigDecimal.ZERO;
			newNightPlanQty = dayPlanQty.add(nightPlanQty).subtract(stockQty);
			// 如果中班 + 晚班计划量仍然比库存量小，则晚班的计划量同样要设置为0，相当于当天库存可满足成型生产
			newNightPlanQty = newNightPlanQty.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : newNightPlanQty;
		}

		// 合并中班晚班计划量
		// 如果 中班计划量 > 0，那么中班计划量=中班计划量+晚班计划量；晚班计划量=0
		// 如果 中班计划量 = 0，那么晚班计划量不变
		if (newDayPlanQty.compareTo(BigDecimal.ZERO) > 0) {
			newDayPlanQty = newDayPlanQty.add(newNightPlanQty);
			newNightPlanQty = BigDecimal.ZERO;
		}
		resultVo.setDayPlanQty(newDayPlanQty.doubleValue());
		resultVo.setNightPlanQty(newNightPlanQty.doubleValue());
	}

	/**
	 * 获取排产日的16点半部件库存
	 * 
	 * @param scheduleDate 排产日期
	 * @Param stockLossRate 库存损耗率
	 * @Param isProductionStage 仅对投产阶段规格排产
	 * @return key：帘布编号，value：库存量
	 */
	@Override
	public Map<String, Cd90StockVo> getStockMap(Date scheduleDate, BigDecimal stockLossRate,
			boolean isProductionStage) {
		return cd90EngineStockMapper.selectCd90Stock(scheduleDate, stockLossRate, isProductionStage).stream()
				.collect(Collectors.toMap(Cd90StockVo::getClothCode, Function.identity(), (v1, v2) -> v1));
	}

	/**
	 * 计算合适的线边库存
	 * 
	 * @param machineCode      机台
	 * @param scheduleList     待计算的排产计划
	 * @param lineSideStockMap 线边库库存信息
	 * @param machineMap       机台
	 * @param planQty          计划量
	 * @param isCloseOut       是否收尾
	 * @return
	 */
	private BigDecimal getLinsideStock(String machineCode, List<Cd90ScheduleResultVo> scheduleList,
			Map<String, List<Cd90LineSideStock>> lineSideStockMap, Map<Long, String> machineMap, BigDecimal planQty,
			boolean isCloseOut) {
		if (StringUtils.isEmpty(machineCode)) {
			return BigDecimal.ZERO;
		}
		Cd90ScheduleResultVo schedule = CollectionUtil.firstElement(scheduleList);
		String bigRollCode = schedule.getBigRollCode(); // 对应的大卷编号
		boolean isUniqueMaterial = scheduleList.stream().filter(s -> s.getTotalPlanQty().compareTo(BigDecimal.ZERO) > 0)
				.map(Cd90ScheduleResultVo::getClothCode).distinct().collect(Collectors.counting()) == 1; // 判断是否大卷唯一规格
		boolean hasCxFiveClass = scheduleList.stream().anyMatch(s -> s.getTotalPlanQty().compareTo(BigDecimal.ZERO) > 0
				&& Optional.ofNullable(s.getCxClass5Plan()).orElse(0D) > 0); // 是否有成型第五个班
		List<Cd90LineSideStock> lineSideStockList = lineSideStockMap
				.getOrDefault(GenerageMapKeyUtils.createMapKey(machineCode, bigRollCode), new ArrayList<>(0)).stream()
				.filter(s -> Optional.ofNullable(s.getStockNum()).orElse(BigDecimal.ZERO)
						.compareTo(BigDecimal.ZERO) > 0)
				.collect(Collectors.toList()); // 根据机台 + 物料好取出线边库库存，只要库存量大于0的;
		if (CollectionUtil.isEmpty(lineSideStockList)) {
			return BigDecimal.ZERO;
		}

		BigDecimal largerStock = BigDecimal.ZERO; // 大于计划量但最接近的一个值
		BigDecimal smallerStock = BigDecimal.ZERO; // 小于计划量但最接近的一个值
		BigDecimal totalStock = BigDecimal.ZERO; // 总库存量
		int targetIndex = 0; // 单个卷米数刚搞等于计划量的下标
		int largerSeq = 0; // 高于计划量但最接近一个组合的下标（二进制）
		int smallerSeq = 0; // 低于计划量但最接近一个组合的下标（二进制）
		int size = lineSideStockList.size();
		// 由于使用穷举算法，长度增加运算量会指数增长，长度超过25时运算缓慢，且不符合实际业务逻辑，可视作垃圾数据直接返回0
		if (size > 25) {
			return BigDecimal.ZERO;
		}
		
		for (int j = 0; j < size; j++) {
			Cd90LineSideStock stock = lineSideStockList.get(j);
			BigDecimal stockNum = stock.getStockNum();
			if (stockNum.compareTo(planQty) == 0) {
				// 如果有与需求量直接相等的大卷，则直接返回即可
				return stockNum;
			} else if (stockNum.compareTo(planQty) > 0
					&& (largerStock == BigDecimal.ZERO || largerStock.compareTo(stockNum) > 0)) {
				// 判断是否长度最接近计划量的大卷
				largerStock = stockNum;
				largerSeq = 1 << j; // 利用位运算将下标转换成二进制码
			} else if (stockNum.compareTo(planQty) < 0
					&& (smallerStock == BigDecimal.ZERO || smallerStock.compareTo(stockNum) < 0)) {
				// 判断是否长度最接近计划量的大卷
				smallerStock = stockNum;
				smallerSeq = 1 << j; // 利用位运算将下标转换成二进制码
			}
			totalStock = totalStock.add(stockNum);
		}

		if (totalStock.compareTo(planQty) < 0) { // 总库存都不够需求量，直接当无库存处理
			return BigDecimal.ZERO;
		} else if (totalStock.compareTo(planQty) == 0) { // 总库存刚好等于需求量，直接返回库存总量
			return totalStock;
		}

		// 穷举法列举所有的组合，得出最接近的组合，包括高于计划量与低于计划量的最接近组合
		for (int i = 1, len = (int) Math.pow(2, size); i < len; i++) { // 穷举法的遍历次数 = 2的size次方次，从1开始
			BigDecimal total = BigDecimal.ZERO;
			for (int j = 0; j < size; j++) {
				BigDecimal stockNum = lineSideStockList.get(j).getStockNum();
				total = total.add(stockNum.multiply(new BigDecimal((i >> j) & 1)));
				if (total.compareTo(planQty) == 0) {
					return total; // 有任意一个组合匹配，则直接使用该组合
				} else if (total.compareTo(planQty) < 0
						&& (smallerStock == BigDecimal.ZERO || smallerStock.compareTo(total) < 0)) {
					smallerSeq = i;
					smallerStock = total;
				} else if (total.compareTo(planQty) > 0
						&& (largerStock == BigDecimal.ZERO || largerStock.compareTo(total) > 0)) {
					largerSeq = i;
					largerStock = total;
				} else if (largerStock != BigDecimal.ZERO && total.compareTo(largerStock) >= 0) {
					break; // 超过高值则不需要继续计算
				}
			}
		}
		// 判断最接近计划量的组合
		boolean isUseSamller = false;
		// 只有高值与低值相差量一样的时候，需要判断是否使用低值，否则统一使用高值
		if (planQty.subtract(smallerStock).compareTo(largerStock.subtract(planQty)) == 0) {
			// 判断1、该大卷对应生产的钢带是唯一规格，2、该规格未收尾，3、该规格在成型计划的第五个班次里有计划量。
			// 满足上述条件用高值，否则用低值
			isUseSamller = !(isUniqueMaterial && !isCloseOut && hasCxFiveClass);
		}
		List<Cd90LineSideStock> resultSideStock;
		if (targetIndex > 0) { // 有组合相等的情况，直接使用该组合
			resultSideStock = CollectionUtil.filterList(lineSideStockList, targetIndex);
		} else if (isUseSamller) { // 其余情况按高低值使用标记判断使用哪些线边库存
			resultSideStock = CollectionUtil.filterList(lineSideStockList, smallerSeq);
		} else {
			resultSideStock = CollectionUtil.filterList(lineSideStockList, largerSeq);
		}

		if (CollectionUtils.isNotEmpty(resultSideStock)) {
			StringBuilder logDetail = new StringBuilder();
			logDetail.append("大卷编号：").append(bigRollCode).append("，线边库库存：").append(resultSideStock.toString());
			autoScheduleLogService.insertCd90ScheduleLog(schedule.getBatchNo(), "", "3.3、计算计划量线边库库存选择",
					logDetail.toString());
		}
		return resultSideStock.stream().map(Cd90LineSideStock::getStockNum).reduce(BigDecimal.ZERO, BigDecimal::add);
	}

	/**
	 * 记录计划量运算的日志信息
	 * 
	 * @Author hakimryan
	 * @Description
	 * @Date 2021-7-26 14:41:21
	 * @param oldScheduleResult 计算前排产结果
	 * @param scheduleVo        计算后排产结果
	 * @param lossRate          损耗率
	 */
	private void insertCalculateLog(String oldScheduleResult, Cd90ScheduleResultVo scheduleVo, double lossRate) {
		String logDetail = logSplit("开始计算中班和夜班计划量", "计算前排程数据：" + oldScheduleResult,
				"根据库存重新计算中班计划量dayPlanQty：如果 原中班计划量>库存，则 中班计划量 = 原中班计划量 -库存；否则中班计划量 = 0",
				"根据库存重新计算夜班计划量nightPlanQty：如果 原中班计划量>库存，则 晚班计划量=0 ；否则晚班计划量 = （原中班计划量+原晚班计划量-库存）",
				"获得损耗率（从损耗率表获取对应的损耗率，获取顺序：机台+物料编号 > 胎面代码 > 机台 >工序参数配置），耗损率：" + lossRate,
				"如果中班计划量>0，那么中班计划量=中班计划量+夜班计划量，夜班计划量=0（为了让相同的胶在同一个班生产，而且又不能延误生产）",
				"计划量计算好后的排程数据：" + toJSONString(scheduleVo));
		autoScheduleLogService.insertCd90ScheduleLog(scheduleVo.getBatchNo(), scheduleVo.getOrderNo(), "3.2、计算各班计划量",
				logDetail);
	}
}
