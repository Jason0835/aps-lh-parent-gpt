package com.zlt.mix.schedule.engine.service.glueschedule.impl;

import com.ruoyi.common.core.utils.DateUtils;
import com.ruoyi.common.core.utils.bean.BeanUtils;
import com.ruoyi.common.utils.StringUtils;
import com.zlt.mix.common.core.constant.ZltConstant;
import com.zlt.mix.common.core.utils.BigDecimalUtil;
import com.zlt.mix.common.core.utils.GenerageMapKeyUtils;
import com.zlt.mix.common.engine.service.GlueScheduleEngineLogService;
import com.zlt.mix.common.engine.service.impl.IncrementService;
import com.zlt.mix.common.engine.utils.CollectionUtil;
import com.zlt.mix.schedule.api.domain.entity.GlueScheduleResult;
import com.zlt.mix.schedule.engine.constants.GlueEngineConstants;
import com.zlt.mix.schedule.engine.service.basicdata.MachineEngineService;
import com.zlt.mix.schedule.engine.service.glueschedule.GlueScheduleEngineBaseService;
import com.zlt.mix.schedule.engine.service.glueschedule.GlueScheduleEngineSupplementService;
import com.zlt.mix.schedule.engine.util.*;
import com.zlt.mix.schedule.engine.vo.*;
import com.zlt.mix.setting.api.domain.entity.MixMachine;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 胶料补量服务
 *
 */
@Service
public class GlueScheduleEngineSupplementServiceImpl implements GlueScheduleEngineSupplementService {
	@Autowired
	private GlueScheduleEngineBaseService glueScheduleEngineBaseService;
	@Autowired
	private MachineEngineService machineEngineService;
	@Resource
	private IncrementService incrementService;
	@Autowired
	private GlueScheduleEngineLogService logService;

	/**
	 * 剩余计划量补量
	 *
	 * @param scheduleDate
	 * @param mixArea
	 * @param baseScheduleList         排产列表
	 * @param glueStock                库存列表
	 * @param factoryRequireMap        分厂需求列表
	 * @param params                   排产参数
	 * @param mixingTimeMap            胶料间隔时间
	 * @param slPriorityMap            塑胶优先级映射
	 * @param needSlScheduleMap        需要塑胶排产后的优先排产的记录
	 * @param latestScheduleList       查询昨日早班的最后一个排程计划
	 * @param mixingPriorityProductMap 炼胶优先配置
	 */
	@Override
	public void surplusQtySuppliment(Date scheduleDate, String mixArea, List<GlueScheduleResultVo> scheduleResultList,
			GlueScheduleStockPool glueStock, Map<String, GlueFactoryRequireVo> factoryRequireMap,
			Map<String, String> params, Map<String, Long> mixingTimeMap, Map<String, List<GlueScheduleResultVo>> slPriorityMap,
									 Map<String, List<GlueScheduleResultVo>> needSlScheduleMap,
									 List<GlueScheduleResultVo> latestScheduleList,
									 Map<String, String> mixingPriorityProductMap,
									 List<MesPmtRecipeVo> mesPmtRecipeList) {
		// 校验开关是否有打开
		if (!GlueEngineConstants.YES_OR_NO_YES_1.equals(params.get(GlueEngineConstants.GLUE_SUPPLEMENT_SWITCH))) {
			return;
		}

		List<GlueScheduleResultVo> baseScheduleList = scheduleResultList.stream().filter(s -> s.getPmtRecipe() != null)
				.collect(Collectors.toList()); // 先过滤掉无效的计划
		if (CollectionUtils.isEmpty(baseScheduleList)) {
			return;
		}
		baseScheduleList.forEach(schedule -> { // 补上一些空值字段，防止后续报错
			if (schedule.getProductedQty() == null) {
				schedule.setProductedQty(BigDecimal.ZERO);
			}
		});
		BigDecimal mixOntervalTime = new BigDecimal(params.getOrDefault(GlueEngineConstants.MIX_INTERVAL_TIME, "0")); // 每一车的间隔时间，单位秒
		BigDecimal scheduleSwitchTime = new BigDecimal(
				params.getOrDefault(GlueEngineConstants.SCHEDULE_SWITCH_TIME, "0")); // 每一个计划的切换时长，单位秒

		Map<String, Double> glueSurplusMap = baseScheduleList.stream()
				.filter(s -> s.getPmtRecipe() != null && s.getTotalSurplus() != null && s.getTotalSurplus() > 0)
				.collect(Collectors.toMap(GlueScheduleResultVo::getGlue, GlueScheduleResultVo::getTotalSurplus,
						(s1, s2) -> s1)); // 剩余量列表
		if (glueSurplusMap.isEmpty()) {
			return;
		}

		// 补全剩余量的配方
		addSurplusRecipe(mixArea, glueStock, mesPmtRecipeList, baseScheduleList, glueSurplusMap);

		// 计算剩余产能
		List<MixMachine> machineList = machineEngineService.listMixMachineInfo(mixArea);
		Map<CombinedMapKey, BigDecimal> machineCapacityMap = this.initMachineCapacityMap(baseScheduleList, machineList,
				params, mixingTimeMap); // key：机台+班别
		Map<String, List<GlueScheduleResultVo>> machineGroupingMap = baseScheduleList.stream()
				.filter(s -> s.getPmtRecipe() != null)
				.collect(Collectors.groupingBy(GlueScheduleResultVo::getMachineCode)); // 按机台对排产列表分组

		Map<CombinedMapKey, GlueScheduleResultVo> newScheduleMap = new HashMap<>(); // 同一个机台同一个胶料最多新增一笔记录
		Integer shiftClass = GlueEngineConstants.SHIFT_CLASS_NIGHT; // 当前处理班别，从早班开始补量，避免中断连续生产
		do {
			SchedulePriorityUtils.recaculatePriority(baseScheduleList, glueStock, factoryRequireMap, shiftClass,
					params, slPriorityMap, latestScheduleList, mixingPriorityProductMap, needSlScheduleMap); // 重算优先级
			baseScheduleList.sort(glueScheduleEngineBaseService.createScheduleSorter()); // 重新排序

			// 遍历剩余量尝试安排上有产能的机台
			for (GlueScheduleResultVo schedule : baseScheduleList) {
				String glueCode = schedule.getGlue();
				Double surplusPlanQty = glueSurplusMap.getOrDefault(glueCode, 0D);
				MesPmtRecipeVo recipe = schedule.getPmtRecipe();
				String machineCode = schedule.getMachineCode();
				if (surplusPlanQty <= 0) { // 如果没有剩余量了，则直接跳过
					continue;
				}

				// 根据计划量构建排产计划
				SingleClassGlueScheduleResultVO newSingleSchedule = this.getNewSchedule(newScheduleMap, schedule,
						shiftClass);
				if (newSingleSchedule.getPlanQty().compareTo(BigDecimal.ZERO) > 0) {
					// 如果同一个机台同一个胶料同一个班次已经新增过，则跳过
					continue;
				}

				BigDecimal planQty = BigDecimalUtil.valueOf(surplusPlanQty); // 可排产量，默认等于剩余量

				// 检查产能是否足够支持生产，不够则需要减少计划量
				CombinedMapKey machineKey = CombinedMapKey.createKey(machineCode, shiftClass); // 机台 + 班别的组合key
				BigDecimal capacity = machineCapacityMap.getOrDefault(machineKey, BigDecimal.ZERO);
				Long itemIntervalTime = mixingTimeMap.get(GenerageMapKeyUtils.createMapKey(schedule.getGlue(), schedule.getMachineCode()));
				BigDecimal intervalTime = itemIntervalTime != null ? BigDecimal.valueOf(itemIntervalTime) : mixOntervalTime;
				BigDecimal maxPerCarTime = BigDecimalUtil.valueOf(recipe.getSummerMixTime()).add(intervalTime); // 一车胶消耗产能=配方炼胶时长+单车间隔时长
				List<GlueScheduleResultVo> schedulelist = machineGroupingMap.get(machineCode);
				planQty = this.restrictPlanQtyByCapacity(planQty, capacity, scheduleDate, shiftClass, schedulelist,
						scheduleSwitchTime, maxPerCarTime); // 通过产能限制排产计划量
				if (planQty.compareTo(BigDecimal.ZERO) <= 0) { // 可生产量大于0的继续生成
					continue;
				}

				// 检查原料库存是否足够支持生产，不够则需要减少计划量
				planQty = this.restrictPlanQtyByStock(planQty, glueStock, recipe); // 通过原料库存限制排产计划量
				if (planQty.compareTo(BigDecimal.ZERO) <= 0) { // 可生产量大于0的继续生成
					continue;
				}

				// 将计划量安排到指定机台、班次、顺序上
				newSingleSchedule.setPlanQty(planQty);
				this.caculateSupplmentSchedule(newSingleSchedule, schedulelist, intervalTime, scheduleSwitchTime,
						BigDecimalUtil.valueOf(recipe.getSummerMixTime()));
				// 将排产数据刷新到排程记录中
				newSingleSchedule.updatePlanQty();
				newSingleSchedule.updateExpectTime();

				// 更新过程数据，下一次轮询的时候才有最新数据
				BigDecimal midRealConsumeCapacity = planQty.multiply(maxPerCarTime).add(scheduleSwitchTime); // 实际消耗产能
				BigDecimal midSurplueCapacity = capacity.subtract(midRealConsumeCapacity); // 剩余产能
				machineCapacityMap.put(machineKey, midSurplueCapacity); // 更新机台的剩余产能
				glueStock.addStock(glueCode, schedule.getMajorType(), planQty); // 直接将排产量加到库存中
				glueSurplusMap.put(glueCode, BigDecimalUtil.valueOf(surplusPlanQty).subtract(planQty).doubleValue()); // 更新剩余量
				GlueScheduleResultVo newSchedule = newSingleSchedule.getScheduleResult();
				if (schedulelist.stream().noneMatch(s -> s == newSchedule)) {
					schedulelist.add(newSchedule); // 本次新增的数据要加到列表中
					newSchedule.setOrderNo(incrementService.getSequence4(newSchedule.getBatchNo()));
					CombinedMapKey newScheduleKey = CombinedMapKey.createKey(schedule.getMachineCode(),
							schedule.getGlue());
					newScheduleMap.put(newScheduleKey, newSchedule);
				}
			}
			shiftClass = ShiftClassUtil.getNextShiftClass(shiftClass); // 切换到下个班别
		} while (shiftClass != null);

		if (newScheduleMap.isEmpty()) {
			return;
		}
		this.saveScheduleLog(newScheduleMap.values()); // 记录日志
		scheduleResultList.addAll(newScheduleMap.values());
		baseScheduleList.addAll(newScheduleMap.values()); // 补量记录全部添加到排程列表中
		// 更新计划列表的剩余量
		baseScheduleList.stream().filter(s -> glueSurplusMap.containsKey(s.getGlue())).forEach(s -> {
			String glueCode = s.getGlue();
			if (s.getTotalSurplus() != null && s.getTotalSurplus() > 0) {
				s.setTotalSurplus(glueSurplusMap.get(glueCode));
			} else {
				s.setTotalSurplus(0D);
			}
		});
	}

	/**
	 * 补全剩余量的配方
	 */
	private void addSurplusRecipe(String mixArea, GlueScheduleStockPool glueStock, List<MesPmtRecipeVo> mesPmtRecipeList, List<GlueScheduleResultVo> baseScheduleList, Map<String, Double> glueSurplusMap) {
		// 加载有效的配方信息，配方按胶料 + 机台分组
		Map<CombinedMapKey, List<MesPmtRecipeVo>> mesPmtRecipeMap = mesPmtRecipeList.stream().collect(Collectors
				.groupingBy(r -> CombinedMapKey.createKey(r.getRecipeMaterialName(), r.getRecipeEquipCode())));
		// 记录已经选择的配方
		Map<String, List<GlueScheduleResultVo>> groupMap = baseScheduleList.stream()
				.collect(Collectors.groupingBy(GlueScheduleResultVo::getGlue));
		// 各胶料备选机台
		Map<String, List<FormulaMachineVo>> glueMachineMap = machineEngineService.listFormulaMachine(mixArea).stream()
				.collect(Collectors.groupingBy(FormulaMachineVo::getGlue));
		// 如果有剩余量的胶料，将配方和机台补全
		glueSurplusMap.forEach((glueCode, surplusQty) -> {
			if (surplusQty == null || surplusQty <= 0) {
				return;
			}

			List<GlueScheduleResultVo> voList = groupMap.get(glueCode);
			if (CollectionUtils.isEmpty(voList)) {
				return;
			}
			GlueScheduleResultVo firstResultVo = voList.get(0);

			// 记录已经选择过的机台配方
			Map<String, List<GlueScheduleResultVo>> machineMap = voList.stream()
					.filter(v -> StringUtils.isNotBlank(v.getMachineCode()))
					.collect(Collectors.groupingBy(GlueScheduleResult::getMachineCode));

			List<FormulaMachineVo> formulaMachineVoList = glueMachineMap.get(glueCode);
			if (CollectionUtils.isEmpty(formulaMachineVoList)) {
				return;
			}

			// 补全没有选择过的配方机台
			for (FormulaMachineVo formulaMachineVo : formulaMachineVoList) {
				// 如果已经选择过ZZ配方了，无需补全配方
				List<GlueScheduleResultVo> recipeSchedule = machineMap.getOrDefault(formulaMachineVo.getMachineCode(), new ArrayList<>());
				if (recipeSchedule.stream().anyMatch(v -> GlueEngineConstants.RECIPE_TYPE_ZZ.equals(v.getRecipeTypeName()))) {
					continue;
				}

				// 过滤已选的机台配方
				Set<String> recipeTypeNameSet = recipeSchedule.stream().map(GlueScheduleResult::getRecipeTypeName).filter(StringUtils::isNotBlank).collect(Collectors.toSet());
				List<MesPmtRecipeVo> recipeList =
						glueScheduleEngineBaseService.chooseRecipe(glueCode, formulaMachineVo.getMachineCode(), mesPmtRecipeMap, glueStock, false);
				// 过滤已经选择的配方
				List<MesPmtRecipeVo> noSelectRecipe = recipeList.stream().filter(v -> !recipeTypeNameSet.contains(v.getRecipeTypeName())).collect(Collectors.toList());
				for (MesPmtRecipeVo recipe : noSelectRecipe) {
					GlueScheduleResultVo newSchedule = new GlueScheduleResultVo();
					BeanUtils.copyProperties(firstResultVo, newSchedule);
					glueScheduleEngineBaseService.initBaseScheduleProperties(newSchedule); // 重置基础信息
					glueScheduleEngineBaseService.copyRecipeProperties(newSchedule, recipe);
					newSchedule.setMachineCode(formulaMachineVo.getMachineCode());
					baseScheduleList.add(newSchedule);

					// 选择一个ZZ配方即可
					if (GlueEngineConstants.RECIPE_TYPE_ZZ.equals(recipe.getRecipeTypeName())) {
						break;
					}
				}
			}
		});
	}

	/**
	 * 通过原料库存限制排产计划量
	 * 
	 * @param planQty
	 * @param glueStock
	 * @param recipe
	 * @return
	 */
	private BigDecimal restrictPlanQtyByStock(BigDecimal planQty, GlueScheduleStockPool glueStock,
			MesPmtRecipeVo recipe) {
		BigDecimal newPlanQty = planQty;
		// 有剩余量，尝试计算产能、母胶是否足够
		List<MesPmtRecipeWeightVo> recipeWeightList = recipe.getRecipeWeightList();
		BigDecimal maxSetWeight = RecipeUtil.getMaxSetWeight(recipeWeightList); // 获取称重配方中最大的终炼母炼胶重量
		for (MesPmtRecipeWeightVo recipteWeight : recipeWeightList) {
			BigDecimal setWeight = BigDecimalUtil.valueOfZero(recipteWeight.getSetWeight());
			String weightGlueCode = recipteWeight.getRecipeMaterialName();
			String majorType = RecipeUtil.getMajorType(weightGlueCode, recipteWeight.getMajorType(), setWeight,
					maxSetWeight);
			BigDecimal productNum;
			if (GlueEngineConstants.MAJOR_TYPE_ML.equals(majorType)) { // 取出母炼胶
				// 可生产终胶 = 母炼胶库存 * 换算比率
				BigDecimal conversionRatio = Optional.ofNullable(recipteWeight.getConversionRatio())
						.orElse(BigDecimal.ONE); // 换算比率
				BigDecimal stockNum = glueStock.getStockNum(weightGlueCode, majorType); // 母炼胶现有库存
				productNum = stockNum.multiply(conversionRatio).setScale(0, RoundingMode.DOWN);
			} else if (GlueEngineConstants.MIX_MAJOR_TYPE.contains(majorType) || GlueEngineConstants.MAJOR_TYPE_SL.contains(majorType)) {
				// 掺胶类型 或者 塑炼胶，需要按重量计算
				setWeight = setWeight.compareTo(BigDecimal.ZERO) == 0 ? BigDecimal.ONE : setWeight;
				// 取出掺胶的库存重量
				BigDecimal stockWeight = glueStock.getStockWeight(weightGlueCode, majorType);
				productNum = stockWeight.divide(setWeight, 0, RoundingMode.DOWN); // 库存重量换算成可提供生产车数
			} else {
				continue;
			}
			newPlanQty = BigDecimalUtil.least(newPlanQty, productNum);
		}
		if (planQty.compareTo(BigDecimal.ZERO) <= 0) { // 可生产量大于0的继续生成
			return planQty;
		}
		glueStock.subtractChildGlueStock(newPlanQty, recipe); // 扣减原料库存
		return newPlanQty;
	}

	/**
	 * 通过产能限制排产计划量
	 * 
	 * @param planQty            原计划量
	 * @param capacity           机台剩余产能
	 * @param scheduleDate       排产日
	 * @param shiftClass         班次
	 * @param scheduleList       本机台已排计划
	 * @param scheduleSwitchTime 切换时间
	 * @param maxPerCarTime      每车时长
	 * @return
	 */
	private BigDecimal restrictPlanQtyByCapacity(BigDecimal planQty, BigDecimal capacity, Date scheduleDate,
			Integer shiftClass, List<GlueScheduleResultVo> scheduleList, BigDecimal scheduleSwitchTime,
			BigDecimal maxPerCarTime) {
		SingleClassGlueScheduleResultVO lastestSchedule = scheduleList.stream()
				.map(s -> new SingleClassGlueScheduleResultVO(s, shiftClass)) // 抽取指定班别的排产信息
				.filter(s -> s.getExpectFinishTime() != null) // 需要同一机台、同一班别有计划完成时间的计划
				.max(Comparator.comparing(SingleClassGlueScheduleResultVO::getExpectFinishTime)) // 取出同机台的最晚的一笔
				.orElse(null);
		Date nextStartTime = null;
		if (lastestSchedule != null) {
			nextStartTime = DateUtils.addSeconds(lastestSchedule.getExpectFinishTime(), scheduleSwitchTime.intValue());
		} else {
			nextStartTime = ShiftClassUtil.getShiftClassStartTime(scheduleDate, shiftClass);
		}
		Date classEndTime = ShiftClassUtil.getShiftClassEndTime(scheduleDate, shiftClass);
		BigDecimal classSurplusTime = BigDecimalUtil.valueOf(DateUtils.getDiffMillTime(nextStartTime, classEndTime))
				.divide(GlueEngineConstants.THOUSAND, 0, RoundingMode.DOWN); // 班别剩余时长

		BigDecimal consumeCapacity = maxPerCarTime.multiply(planQty).add(scheduleSwitchTime); // 生产完该计划需要消耗的产能
		BigDecimal realCapacity = BigDecimalUtil.least(classSurplusTime, capacity, consumeCapacity); // 实际可用产能
		return realCapacity.subtract(scheduleSwitchTime).divide(maxPerCarTime, 0, RoundingMode.DOWN); // 实际可生产量
	}

	/**
	 * 
	 * @param schedule
	 * @param shiftClass
	 * @param newScheduleMap
	 * @return
	 */
	private SingleClassGlueScheduleResultVO getNewSchedule(Map<CombinedMapKey, GlueScheduleResultVo> newScheduleMap,
			GlueScheduleResultVo schedule, Integer shiftClass) {
		CombinedMapKey newScheduleKey = CombinedMapKey.createKey(schedule.getMachineCode(), schedule.getGlue());
		GlueScheduleResultVo newSchedule = newScheduleMap.get(newScheduleKey); // 取出本次插单的记录
		if (newSchedule == null) {
			newSchedule = new GlueScheduleResultVo();
			BeanUtils.copyProperties(schedule, newSchedule);
			glueScheduleEngineBaseService.initBaseScheduleProperties(newSchedule); // 重置基础信息
		}
		return new SingleClassGlueScheduleResultVO(newSchedule, shiftClass);
	}

	/**
	 * 计算补量计划
	 * 
	 * @param newSingleSchedule  补量排程计划
	 * @param schedulelist       本机台排程列表
	 * @param mixOntervalTime    每一车的间隔时间
	 * @param scheduleSwitchTime 规格切换时间
	 * @param formulaTime        炼胶时长
	 */
	private void caculateSupplmentSchedule(SingleClassGlueScheduleResultVO newSingleSchedule,
			List<GlueScheduleResultVo> schedulelist, BigDecimal mixOntervalTime, BigDecimal scheduleSwitchTime,
			BigDecimal formulaTime) {
		Integer shiftClass = newSingleSchedule.getShiftClass();
		List<SingleClassGlueScheduleResultVO> singleScheduleList = schedulelist.stream()
				.map(s -> new SingleClassGlueScheduleResultVO(s, shiftClass)) // 抽取指定班别的排产信息
				.filter(s -> s.getExpectFinishTime() != null) // 需要同一机台、同一班别有计划完成时间的计划
				.sorted(Comparator.comparing(SingleClassGlueScheduleResultVO::getExpectFinishTime,
						Comparator.reverseOrder())) // 按完成时间倒序排序
				.collect(Collectors.toList());
		Integer produceOrder; // 本次排产顺序
		Date startTime; // 本次开始时间
		SingleClassGlueScheduleResultVO lastestSchedule = CollectionUtil.firstElement(singleScheduleList);
		if (lastestSchedule != null) { // 如果本班有排产，则接着最后一笔排
			produceOrder = lastestSchedule.getProduceOrder() + 10;
			startTime = DateUtils.addSeconds(lastestSchedule.getExpectFinishTime(), scheduleSwitchTime.intValue());
		} else { // 没有排产，则当作第一笔排产
			produceOrder = 10;
			startTime = ShiftClassUtil.getShiftClassStartTime(newSingleSchedule.getScheduleDate(), shiftClass);
		}
		// 将记录插单匹配项到后一位
		BigDecimal planQty = newSingleSchedule.getPlanQty();
		BigDecimal produceTime = formulaTime.add(mixOntervalTime).multiply(planQty).add(scheduleSwitchTime);
		newSingleSchedule.setProduceOrder(produceOrder); // 序号为最后一笔 + 10开始
		newSingleSchedule.setExpectStartTime(startTime);
		newSingleSchedule.setExpectFinishTime(DateUtils.addSeconds(startTime, produceTime.intValue()));
	}

	/**
	 * 根据机台 + 排产情况获取机台的剩余产能列表
	 *
	 * @param baseScheduleResult 已排计划
	 * @param machineList        机台列表
	 * @param params             排产参数
	 * @param mixingTimeMap      胶料间隔时间
	 * @return
	 */
	private Map<CombinedMapKey, BigDecimal> initMachineCapacityMap(List<GlueScheduleResultVo> baseScheduleResult,
			List<MixMachine> machineList, Map<String, String> params, Map<String, Long> mixingTimeMap) {
		// 各机台产能列表
		Map<CombinedMapKey, BigDecimal> machineCapacityMap = new HashMap<>();
		Long dinnerTime = new Long(params.getOrDefault(GlueEngineConstants.DINNER_TIME, "0")) * 60; // 用餐时间
		BigDecimal mixOntervalTime = new BigDecimal(params.getOrDefault(GlueEngineConstants.MIX_INTERVAL_TIME, "0")); // 每一车的间隔时间
		BigDecimal scheduleSwitchTime = new BigDecimal(
				params.getOrDefault(GlueEngineConstants.SCHEDULE_SWITCH_TIME, "0")); // 每一个计划的切换时长
		BigDecimal singleCapacity = BigDecimalUtil.valueOf(ShiftClassUtil.ONE_SHIFT_CLASS_TIME - dinnerTime); // 单班最大产能
		for (MixMachine machine : machineList) {
			String machineCode = machine.getMachineCode();
			// 机台有启用的班次，产能默认单班最大产能
			if (ZltConstant.STATUS_ENABLE.equals(machine.getStatus()) && ZltConstant.STATUS_ENABLE.equals(machine.getMidStatus())) {
				machineCapacityMap.put(CombinedMapKey.createKey(machineCode, GlueEngineConstants.SHIFT_CLASS_MID),
						singleCapacity);
			}
			if (ZltConstant.STATUS_ENABLE.equals(machine.getStatus()) && ZltConstant.STATUS_ENABLE.equals(machine.getNightStatus())) {
				machineCapacityMap.put(CombinedMapKey.createKey(machineCode, GlueEngineConstants.SHIFT_CLASS_NIGHT),
						singleCapacity);
			}
			// if (ZltConstant.STATUS_ENABLE.equals(machine.getStatus()) &&  ZltConstant.STATUS_ENABLE.equals(machine.getDayStatus())) {
			// 	machineCapacityMap.put(CombinedMapKey.createKey(machineCode, GlueEngineConstants.SHIFT_CLASS_DAY),
			// 			singleCapacity);
			// }
		}

		// 扣减已排计划的产能
		for (GlueScheduleResultVo result : baseScheduleResult) {
			if (result.getPmtRecipe() == null) {
				continue;
			}
			String machineCode = result.getMachineCode();
			Long itemIntervalTime = mixingTimeMap.get(GenerageMapKeyUtils.createMapKey(result.getGlue(), result.getMachineCode()));
			BigDecimal maxPerCarTime = BigDecimalUtil.valueOf(result.getPmtRecipe().getSummerMixTime())
					.add(itemIntervalTime != null ? BigDecimal.valueOf(itemIntervalTime) : mixOntervalTime); // 一车胶消耗产能 = 配方炼胶时长+单车间隔时长
			BigDecimal midPlanQty = BigDecimalUtil.valueOfZero(result.getMidPlanQty());
			CombinedMapKey midKey = CombinedMapKey.createKey(machineCode, GlueEngineConstants.SHIFT_CLASS_MID);
			BigDecimal midCapacity = machineCapacityMap.getOrDefault(midKey, BigDecimal.ZERO); // 机台中班剩余产能
			if (midCapacity.compareTo(BigDecimal.ZERO) > 0 && midPlanQty.compareTo(BigDecimal.ZERO) > 0) {
				BigDecimal consumeCapacity = maxPerCarTime.multiply(midPlanQty).add(scheduleSwitchTime); // 生产完该计划需要消耗的产能
				BigDecimal surplusCapacity = midCapacity.subtract(consumeCapacity); // 机台扣减掉销量的的剩余产能
				machineCapacityMap.put(midKey, BigDecimalUtil.greatest(surplusCapacity, BigDecimal.ZERO));
			}

			BigDecimal nightPlanQty = BigDecimalUtil.valueOfZero(result.getNightPlanQty());
			CombinedMapKey nightKey = CombinedMapKey.createKey(machineCode, GlueEngineConstants.SHIFT_CLASS_NIGHT);
			BigDecimal nightCapacity = machineCapacityMap.getOrDefault(nightKey, BigDecimal.ZERO); // 机台夜班剩余产能
			if (nightCapacity.compareTo(BigDecimal.ZERO) > 0 && nightPlanQty.compareTo(BigDecimal.ZERO) > 0) {
				BigDecimal consumeCapacity = maxPerCarTime.multiply(nightPlanQty).add(scheduleSwitchTime); // 生产完该计划需要消耗的产能
				BigDecimal surplusCapacity = nightCapacity.subtract(consumeCapacity); // 机台扣减掉销量的的剩余产能
				machineCapacityMap.put(nightKey, BigDecimalUtil.greatest(surplusCapacity, BigDecimal.ZERO));
			}

			// BigDecimal dayPlanQty = BigDecimalUtil.valueOfZero(result.getDayPlanQty());
			// CombinedMapKey dayKey = CombinedMapKey.createKey(machineCode, GlueEngineConstants.SHIFT_CLASS_DAY);
			// BigDecimal dayCapacity = machineCapacityMap.getOrDefault(dayKey, BigDecimal.ZERO); // 机台白班剩余产能
			// if (dayCapacity.compareTo(BigDecimal.ZERO) > 0 && dayPlanQty.compareTo(BigDecimal.ZERO) > 0) {
			// 	BigDecimal consumeCapacity = maxPerCarTime.multiply(dayPlanQty).add(scheduleSwitchTime); // 生产完该计划需要消耗的产能
			// 	BigDecimal surplusCapacity = dayCapacity.subtract(consumeCapacity); // 机台扣减掉销量的的剩余产能
			// 	machineCapacityMap.put(dayKey, BigDecimalUtil.greatest(surplusCapacity, BigDecimal.ZERO));
			// }
		}
		return machineCapacityMap;
	}

	/**
	 * 记录排程日志
	 * 
	 * @param newScheduleMap
	 */
	private void saveScheduleLog(Collection<GlueScheduleResultVo> newScheduleList) {
		logService.record("自动补量：");
		for (GlueScheduleResultVo schedule : newScheduleList) {
			StringBuilder logs = new StringBuilder();
			logs.append(schedule.getOrderNo()).append(" == ").append(schedule.getGlue()).append("+")
					.append(schedule.getMachineCode());
			if (schedule.getMidPlanQty() != null && schedule.getMidPlanQty() > 0) {
				logs.append(" == 新夜班：").append("[").append(schedule.getMidProduceOrder()).append("]");
				logs.append(schedule.getMidPlanQty());
			}
			if (schedule.getNightPlanQty() != null && schedule.getNightPlanQty() > 0) {
				logs.append(" == 新白班：").append("[").append(schedule.getNightProduceOrder()).append("]");
				logs.append(schedule.getNightPlanQty());
			}
			if (schedule.getDayPlanQty() != null && schedule.getDayPlanQty() > 0) {
				logs.append(" == ！！！错误白班：").append("[").append(schedule.getDayProduceOrder()).append("]");
				logs.append(schedule.getDayPlanQty());
			}
			logService.record(logs.toString());
		}
	}
}
