package com.zlt.mix.schedule.engine.util.event;

import com.ruoyi.common.core.utils.DateUtils;
import com.ruoyi.common.utils.StringUtils;
import com.zlt.mix.common.core.utils.BigDecimalUtil;
import com.zlt.mix.common.core.utils.GenerageMapKeyUtils;
import com.zlt.mix.common.engine.utils.CollectionUtil;
import com.zlt.mix.schedule.api.domain.entity.GlueScheduleResult;
import com.zlt.mix.schedule.engine.constants.GlueEngineConstants;
import com.zlt.mix.schedule.engine.util.*;
import com.zlt.mix.schedule.engine.vo.GlueFactoryRequireVo;
import com.zlt.mix.schedule.engine.vo.GlueScheduleMachineProductVo;
import com.zlt.mix.schedule.engine.vo.GlueScheduleResultVo;
import com.zlt.mix.schedule.engine.vo.MesPmtRecipeVo;
import org.apache.commons.collections.CollectionUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.Map.Entry;
import java.util.stream.Collectors;

/**
 * 机台胶料查找事件，用于检索空闲的机台以及可排产的胶料，并将其绑定后安排生产
 * 
 * @author hakimryan
 *
 */
public class SearchMachineAndGlueEvent implements ScheduleEvent {
	/**
	 * 100，用于计算百分比
	 */
	private final static BigDecimal ONE_HUNDRED = new BigDecimal("100");
	/**
	 * 1000，用于毫秒换算
	 */
	private final static BigDecimal ONE_THOUSAND = new BigDecimal("1000");
	/**
	 * 首批完成冷却车数默认值：16车
	 */
	private final static String DEFAULT_FIRST_BATCH_GLUE_NUM = "16";

	/**
	 * 执行检索事件
	 */
	@Override
	public void excute(ScheduleEventQueue queue) {
		Date currentTime = queue.getCurrentTime(); // 队列当前时间
		Date scheduleDate = queue.getScheduleDate();
		Date endDate = ShiftClassUtil.getShiftClassEndTime(scheduleDate, GlueEngineConstants.SHIFT_CLASS_NIGHT);
		Map<String, GlueFactoryRequireVo> factoryRequireMap = queue.getFactoryRequireMap();
		if (currentTime.compareTo(endDate) >= 0) {
			// 如果已经超过排产日的结束时间，则不会继续安排
			return;
		}

		// 从队列获取排程相关的上下文信息
		List<GlueScheduleResultVo> scheduleResultList = queue.getScheduleResult(); // 待处理排程列表
		GlueScheduleStockPool glueStock = queue.getGlueStock(); // 库存信息
		Map<String, String> params = queue.getParams(); // 系统配置
		int shiftClass = ShiftClassUtil.getShiftClass(currentTime); // 当前时间对应的班次

		// 还有分厂需求时，对应机台不能占用满这部分
		Map<String, BigDecimal> advanceFactoryRequireMap = buildAdvanceFactoryRequire(queue, factoryRequireMap, params);

		// 过滤掉无法生产的胶料
		List<GlueScheduleResultVo> canProduceResultList = new ArrayList<>();
		Map<CombinedMapKey, BigDecimal> productedQtyMap = new HashMap<>();
		GlueScheduleStockPool tempGlueStock = glueStock.copyStockPool(); // 复制库存信息，用于预计排产量的时候进行扣减

		// 统计每种胶料 + 机台的当班计划量
		Map<CombinedMapKey, Double> shiftClassPlanedMap = this.statisticsShiftClassPlanQty(scheduleResultList,
				shiftClass);

		// 统计有超限制计划量的排程记录
		Map<String, List<GlueScheduleResultVo>> overLimitScheduleMap = scheduleResultList.stream()
				.filter(s -> s.getOverLimitQty() != null && s.getOverLimitQty().compareTo(BigDecimal.ZERO) > 0)
				.collect(Collectors.groupingBy(GlueScheduleResultVo::getGlue));
		// 统计各胶料的超限制计划量
		Map<String, BigDecimal> overLimitMap = new HashMap<>();
		for (Entry<String, List<GlueScheduleResultVo>> entry : overLimitScheduleMap.entrySet()) {
			String glue = entry.getKey();
			List<GlueScheduleResultVo> overLimitScheduleList = entry.getValue();
			BigDecimal overLimitQty = overLimitScheduleList.stream().map(GlueScheduleResultVo::getOverLimitQty)
					.reduce(BigDecimal.ZERO, BigDecimal::add);
			overLimitMap.put(glue, overLimitQty);
		}

		// 除需要连续生产之外的排产列表
		List<GlueScheduleResultVo> filterContinueScheduleResult = queue.getFilterContinueScheduleResult();
		for (GlueScheduleResultVo scheduleResult : filterContinueScheduleResult) {
			// 检测当前排程的开始班次是否早于或等于当前班次，晚于当前班次的都不处理
			// 空的说明已经排完最后一班，不会继续排下去
			if (scheduleResult.getStartShiftClass() == null
					|| scheduleResult.getStartShiftClass().intValue() > shiftClass) {
				continue;
			}
			scheduleResult.setOverLimitQty(null); // 每次处理前先移除超限制计划量
			String glueCode = scheduleResult.getGlue();
			String machineCode = scheduleResult.getMachineCode();
			GlueScheduleMachineProductVo machineProduct = queue.getMachineProduct(machineCode);
			BigDecimal overLimitQty = overLimitMap.getOrDefault(scheduleResult.getGlue(), BigDecimal.ZERO); // 超限制计划量
			// 排除掉胶料已经安排生产的情况
			if (GlueEngineConstants.MACHINE_STATE_ON.equals(scheduleResult.getProductState())) {
				continue;
			}
			// 排除掉机台并非待产状态的情况
			if (!GlueEngineConstants.MACHINE_STATE_WAIT.equals(machineProduct.getState())) {
				continue;
			}

			// 取出本胶料当班的已生产量
			CombinedMapKey planedQtyKey = CombinedMapKey.createKey(glueCode, machineCode);
			BigDecimal planedQty = new BigDecimal(shiftClassPlanedMap.getOrDefault(planedQtyKey, 0D).toString());

			// 计算本次可排产量，如果小于等于0，则直接跳过本胶料
			BigDecimal productedQty = this.caculateProductQty(scheduleResult, planedQty, machineProduct, shiftClass,
					tempGlueStock, params, currentTime, factoryRequireMap.get(glueCode), overLimitQty, queue.getMixingTimeMap(),
					queue.getMixingMinProductMap(),
					scheduleResultList,
					queue,
					advanceFactoryRequireMap.getOrDefault(machineCode, BigDecimal.ZERO));
			if (productedQty.compareTo(BigDecimal.ZERO) <= 0) {
				continue;
			}
			
			Double shiftClassPlaned = productedQty.add(planedQty).doubleValue();
			shiftClassPlanedMap.put(planedQtyKey, shiftClassPlaned);
			scheduleResult.setExpectedRemainingQty(BigDecimalUtil.sub(scheduleResult.getRequireQty(), shiftClassPlaned)); // 预计剩余量
			canProduceResultList.add(scheduleResult);
			productedQtyMap.put(CombinedMapKey.createKey(glueCode, machineCode, scheduleResult.getRecipeType()),
					productedQty);
		}

		// 根据优先级筛选每个空闲机台可安排的优先级最高的胶料
		List<GlueScheduleResultVo> maxPriorityScheduleList = this.chooseMaxPrioritySchedule(canProduceResultList, queue,
				glueStock, scheduleResultList, factoryRequireMap, shiftClass, params, currentTime, queue.getSlPriorityMap(),
				queue.getLatestScheduleList(),
				queue.getMixingPriorityProductMap(),
				queue.getNeedSlScheduleMap());
		// 如果有选中生产模式的配方，需要将相同胶料的其他生产模式的配方直接移除，再获取一次优先级最高的胶料
		maxPriorityScheduleList = getProductionMaxPriority(queue, maxPriorityScheduleList, canProduceResultList, glueStock, 
				scheduleResultList, factoryRequireMap, shiftClass, params, currentTime);

		// 遍历所有符合条件的排程记录，并一一执行排产
		for (GlueScheduleResultVo scheduleResult : maxPriorityScheduleList) {
			String machineCode = scheduleResult.getMachineCode();
			GlueScheduleMachineProductVo machineProduct = queue.getMachineProduct(machineCode);
			// 正常配方，直接生产即可
			if (scheduleResult.getProductionBefore() == null && scheduleResult.getProductionAfter() == null) {
				Map<String, List<GlueScheduleResultVo>> mapProductionModel = queue.getMapProductionModel();
				if (mapProductionModel.containsKey(scheduleResult.getGlue())) {
					// 将相同胶料的生产模式记录的计划量合并到当前记录，将对应计划量全部清空，本排产记录做接续生产
					List<GlueScheduleResultVo> voList = mapProductionModel.get(scheduleResult.getGlue());
					// 合计总计划量
					double sumPlanQty = 0D;
					for (GlueScheduleResultVo itemVo : voList) {
						// BigDecimal planQty = glueScheduleResultVo.getPlanQty();
						sumPlanQty = BigDecimalUtil.add(sumPlanQty, itemVo.getPlanQty().doubleValue());
						itemVo.setPlanQty(BigDecimal.ZERO);
					}
					if (sumPlanQty > 0) {
						// 调整的计划量发生变化，可能超出机台产能，直接跳过进行连续占用排产
						queue.addLog(DateUtils.parseDateToStr("HH:mm:ss", queue.getCurrentTime()) + "|占用生产模式排产起点，占用数量：" + sumPlanQty);
						// 构建一个虚拟的前置节点，拼接起来，进行连续生产
						GlueScheduleResultVo tempSchedule = new GlueScheduleResultVo();
						tempSchedule.setPlanQty(BigDecimal.ZERO);
						tempSchedule.setProductedQty(BigDecimal.ZERO);
						tempSchedule.setBindScheduleResult(scheduleResult);
						ScheduleEventUtils.continueSchedule(queue, tempSchedule, currentTime, machineProduct);

						continue;
					}
				}

				String glueCode = scheduleResult.getGlue();
				BigDecimal productedQty = productedQtyMap
						.get(CombinedMapKey.createKey(glueCode, machineCode, scheduleResult.getRecipeType())); // 本次胶料排产量
				startProductionEvent(queue, scheduleResult, productedQty, machineCode, overLimitMap, factoryRequireMap, params, currentTime, glueStock, machineProduct, shiftClass);

				continue;
			}

			// 开始排产生产模式
			startProductionModel(queue, scheduleResult, currentTime, machineProduct);
		}

		// 刷新超限制计划量
		for (Entry<String, BigDecimal> entry : overLimitMap.entrySet()) {
			String glue = entry.getKey();
			BigDecimal overLimitQty = entry.getValue();
			List<GlueScheduleResultVo> overLimitScheduleList = overLimitScheduleMap.get(glue);
			GlueScheduleResultVo overlimitSchedule = CollectionUtil.firstElement(overLimitScheduleList);
			if (overlimitSchedule != null) {
				overlimitSchedule.setOverLimitQty(overLimitQty);
			}
		}
	}

	/**
	 * 构建需要预占机台产能的工厂需求记录
	 */
	private Map<String, BigDecimal> buildAdvanceFactoryRequire(ScheduleEventQueue queue, Map<String, GlueFactoryRequireVo> factoryRequireMap, Map<String, String> params) {
		Map<String, BigDecimal> advanceFactoryRequireMap = new HashMap<>();

		// 记录已经占用过的胶料机台
		Set<String> advanceFactorySet = new HashSet<>();
		Map<String, List<GlueScheduleResultVo>> mapScheduleResult = queue.getMapScheduleResult();
		Map<String, Long> mixingTimeMap = queue.getMixingTimeMap();
		BigDecimal switchTime = BigDecimal.valueOf(new Integer(params.getOrDefault(GlueEngineConstants.SCHEDULE_SWITCH_TIME, "0"))); // 排程切换时间
		factoryRequireMap.forEach((glue, requireVo) -> {
			if (requireVo == null || requireVo.getRequireDifference().compareTo(BigDecimal.ZERO) <= 0) {
				return;
			}

			// 找出对应机台有计划量的部分，提前计算需要预占的产能
			List<GlueScheduleResultVo> voList = mapScheduleResult.get(glue);
			if (CollectionUtils.isEmpty(voList)) {
				return;
			}

			for (GlueScheduleResultVo itemSchedule : voList) {
				String advanceKey = GenerageMapKeyUtils.createMapKey(itemSchedule.getMachineCode(), itemSchedule.getGlue());
				if (itemSchedule.getPlanQty() == null
						|| itemSchedule.getPlanQty().compareTo(BigDecimal.ZERO) <= 0
						|| itemSchedule.getPmtRecipe() == null
						|| advanceFactorySet.contains(advanceKey)) {
					continue;
				}

				// 只要有排计划量，提前占用分厂需求量
				MesPmtRecipeVo recipe = itemSchedule.getPmtRecipe();
				Long mixTime = recipe.getSummerMixTime(); // 炼胶时间
				// 炼胶间隔时间
				Long intervalTime = mixingTimeMap.getOrDefault(GenerageMapKeyUtils.createMapKey(itemSchedule.getGlue(), itemSchedule.getMachineCode()),
						new Long(params.get(GlueEngineConstants.MIX_INTERVAL_TIME)));
				// （炼胶时间 + 间隔时间）* 计划数 + 切换时间
				BigDecimal expectProductTime = BigDecimal.valueOf(mixTime + intervalTime).multiply(requireVo.getRequireDifference()).add(switchTime);
				String machineCode = itemSchedule.getMachineCode();
				// 合计需要预占的部分
				advanceFactoryRequireMap.put(machineCode, advanceFactoryRequireMap.getOrDefault(machineCode, BigDecimal.ZERO).add(expectProductTime));
				// 记录预占的记录
				advanceFactorySet.add(advanceKey);
			}
		});

		return advanceFactoryRequireMap;
	}

	/**
	 * 开始排产生产模式
	 */
	private void startProductionModel(ScheduleEventQueue queue,
									  GlueScheduleResultVo scheduleResult,
									  Date currentTime, 
									  GlueScheduleMachineProductVo machineProduct) {
		// 记录即将连续生产的排产记录
		List<GlueScheduleResultVo> continueList = new ArrayList<>();
		// 加载有效的配方信息，配方按胶料 + 机台分组
		Map<CombinedMapKey, List<MesPmtRecipeVo>> mesPmtRecipeMap = queue.getMesPmtRecipeMap();

		MesPmtRecipeVo pmtRecipe = scheduleResult.getPmtRecipe();
		if (GlueEngineConstants.RECIPE_TYPE_C2Z.equals(pmtRecipe.getRecipeTypeName())) {
			// 如果生产模式的胶料配方是掺胶配方，将相同胶料的所有可生产计划量做一个汇总，设置为需求量的上限，并将所有相同胶料+配方的计划量置为0，如果有绑定的记录，且当前记录没有绑定，直接将两者进行绑定
			String scheduleKey = GenerageMapKeyUtils.createMapKey(scheduleResult.getGlue(), scheduleResult.getRecipeType());
			Map<String, List<GlueScheduleResultVo>> mapScheduleResult = queue.getMapScheduleResult();
			List<GlueScheduleResultVo> voList = mapScheduleResult.get(scheduleResult.getGlue());
			double sumPlanQty = 0D;
			for (GlueScheduleResultVo item : voList) {
				String itemKey = GenerageMapKeyUtils.createMapKey(item.getGlue(), item.getRecipeType());
				if (!scheduleKey.equals(itemKey)) {
					continue;
				}

				// 汇总可排计划量
				sumPlanQty = BigDecimalUtil.add(sumPlanQty, item.getPlanQty().doubleValue());
				// 将所有相同配方的可生产计划量置为0
				item.setPlanQty(BigDecimal.ZERO);

				if (scheduleResult.getBindScheduleResult() != null || item.getBindScheduleResult() == null) {
					continue;
				}

				// 如果当前记录没有绑定记录，指定排产有绑定记录，绑定到当前排产记录
				GlueScheduleResultVo bindScheduleResult = item.getBindScheduleResult();
				// 如果绑定记录机台和当前排产记录一致，则直接绑定上
				if (scheduleResult.getMachineCode().equals(bindScheduleResult.getMachineCode())) {
					scheduleResult.setBindScheduleResult(bindScheduleResult);
					continue;
				}

				CombinedMapKey recipeKey = CombinedMapKey.createKey(bindScheduleResult.getGlue(), scheduleResult.getMachineCode());
				List<MesPmtRecipeVo> mesPmtRecipeVoList = mesPmtRecipeMap.get(recipeKey);
				if (CollectionUtils.isNotEmpty(mesPmtRecipeVoList)) {
					// 尝试找到相同配方类型的记录，找不到则不绑定
					// todo 暂不考虑，生产模式和前后排产胶料的配方机台不一致的场景
					MesPmtRecipeVo bingRecipeVo = mesPmtRecipeVoList.stream()
							.filter(v -> v.getRecipeType() != null && v.getRecipeType().equals(bindScheduleResult.getRecipeType())).findFirst().orElse(null);
					if (bingRecipeVo != null) {
						bindScheduleResult.setPmtRecipe(bingRecipeVo);
						bindScheduleResult.setMachineCode(scheduleResult.getMachineCode());
						scheduleResult.setBindScheduleResult(bindScheduleResult);
					}
				}
			}
			// 总和作为需求量的上限
			scheduleResult.setRequireQty(sumPlanQty);
		} else {

			// 如果生产模式不是掺胶配方，则为ZZ配方，直接拉满需求量，清空计划量
			scheduleResult.setPlanQty(BigDecimal.ZERO);
		}

		// 如果有前置节点，先走前置节点开始排产
		GlueScheduleResultVo startSchedule = scheduleResult;
		continueList.add(scheduleResult);
		if (scheduleResult.getProductionBefore() != null) {
			// 从前置节点开始
			startSchedule = scheduleResult.getProductionBefore();
			// 记录连续，拼接连续节点
			addContinueRelation(scheduleResult.getProductionBefore(), scheduleResult, continueList);
			// 前置节点添加到排产记录
			queue.getScheduleResult().add(scheduleResult.getProductionBefore());
		}
		if (scheduleResult.getProductionAfter() != null) {
			// 绑定后置节点
			addContinueRelation(scheduleResult, scheduleResult.getProductionAfter(), continueList);
			// 后置节点添加到排产记录
			queue.getScheduleResult().add(scheduleResult.getProductionAfter());
		}
		// 调整的计划量发生变化，可能超出机台产能，直接跳过进行连续占用排产
		queue.addLog(DateUtils.parseDateToStr("HH:mm:ss", queue.getCurrentTime()) + "|生产模式排产起点");

		// 构建一个虚拟的前置节点，拼接起来，进行连续生产
		GlueScheduleResultVo tempSchedule = new GlueScheduleResultVo();
		tempSchedule.setPlanQty(BigDecimal.ZERO);
		tempSchedule.setProductedQty(BigDecimal.ZERO);
		tempSchedule.setBindScheduleResult(startSchedule);
		ScheduleEventUtils.continueSchedule(queue, tempSchedule, currentTime, machineProduct);

		// 断开连续关系，避免生产完成后继续触发
		for (GlueScheduleResultVo item : continueList) {
			if (item != null) {
				item.setBindScheduleResult(null);
			}
		}
	}

	/**
	 * 如果有选中生产模式的配方，需要将相同胶料的其他生产模式的配方直接移除，再获取一次优先级最高的胶料
	 */
	private List<GlueScheduleResultVo> getProductionMaxPriority(ScheduleEventQueue queue,
																List<GlueScheduleResultVo> maxPriorityScheduleList,
																List<GlueScheduleResultVo> canProduceResultList,
																GlueScheduleStockPool glueStock,
																List<GlueScheduleResultVo> scheduleResultList,
																Map<String, GlueFactoryRequireVo> factoryRequireMap,
																int shiftClass,
																Map<String, String> params, Date currentTime) {
		// 记录生产模式的记录
		Set<String> maxPrioritySet = maxPriorityScheduleList.stream().map(GlueScheduleResult::getGlue).filter(StringUtils::isNotBlank).collect(Collectors.toSet());
		Map<String, Integer> productionIndexMap = new HashMap<>();
		for (int i = 0; i < canProduceResultList.size(); i++) {
			GlueScheduleResultVo item = canProduceResultList.get(i);
			// 如果是生产模式，记录首个出现的下标
			if (maxPrioritySet.contains(item.getGlue()) && item.getProductionBefore() != null || item.getProductionAfter() != null) {
				productionIndexMap.putIfAbsent(item.getGlue(), i);
			}
		}
		// 移除生产模式之外的其他选中的配方，因为生产模式考虑直接满排需求量
		if (!productionIndexMap.isEmpty()) {
			int beforeSize = canProduceResultList.size();
			for (int i = canProduceResultList.size() - 1; i >= 0; i--) {
				GlueScheduleResultVo item = canProduceResultList.get(i);
				if (productionIndexMap.containsKey(item.getGlue())) {
					Integer index = productionIndexMap.get(item.getGlue());
					// 如果是选中的生产模式配方之外的其他配方，直接移除
					if (i != index) {
						canProduceResultList.remove(i);
					}
				}
			}
			if (beforeSize != canProduceResultList.size()) {
				// 可生产记录发生变化，重新计算优先级最高的配方记录
				maxPriorityScheduleList = this.chooseMaxPrioritySchedule(canProduceResultList, queue,
						glueStock, scheduleResultList, factoryRequireMap, shiftClass, params, currentTime, queue.getSlPriorityMap(),
						queue.getLatestScheduleList(),
						queue.getMixingPriorityProductMap(),
						queue.getNeedSlScheduleMap());
			}
		}
		return maxPriorityScheduleList;
	}

	/**
	 * 拼接节点关系，记录连续记录
	 */
	private void addContinueRelation(GlueScheduleResultVo beforeSchedule, GlueScheduleResultVo continueSchedule, List<GlueScheduleResultVo> continueList) {
		if (beforeSchedule == null || continueSchedule == null) {
			return;
		}
		GlueScheduleResultVo startSchedule = beforeSchedule;
		continueList.add(startSchedule);
		if (startSchedule.getBindScheduleResult() != null) {
			startSchedule = startSchedule.getBindScheduleResult();
			continueList.add(startSchedule);
		}

		startSchedule.setBindScheduleResult(continueSchedule);
		continueList.add(continueSchedule);
	}

	/**
	 * 开始生产
	 */
	private static Date startProductionEvent(ScheduleEventQueue queue, GlueScheduleResultVo scheduleResult, BigDecimal productedQty, String machineCode, Map<String, BigDecimal> overLimitMap, Map<String, GlueFactoryRequireVo> factoryRequireMap, Map<String, String> params, Date currentTime, GlueScheduleStockPool glueStock, GlueScheduleMachineProductVo machineProduct, int shiftClass) {
		String glueCode = scheduleResult.getGlue();
		MesPmtRecipeVo recipe = scheduleResult.getPmtRecipe();

		// 如果实际排产量超过原排产量，则说明部分超限制计划量已经排进去，则超限制计划量要扣除掉已排的部分
		BigDecimal overLimitQty = overLimitMap.getOrDefault(glueCode, BigDecimal.ZERO);
		if (overLimitQty.compareTo(BigDecimal.ZERO) > 0) {
			BigDecimal newOverLimitQty = overLimitQty.subtract(productedQty);
			overLimitMap.put(glueCode, BigDecimalUtil.greatest(newOverLimitQty, BigDecimal.ZERO));
		}
		// 更新分厂需求量
		boolean isRequire = ScheduleEventUtils.reduceFactoryRequire(factoryRequireMap, glueCode, productedQty);

		// 计算生产时间
		Long mixTime = recipe.getSummerMixTime(); // 炼胶时间（秒）
		Long intervalTime = queue.getMixingTimeMap().getOrDefault(GenerageMapKeyUtils.createMapKey(scheduleResult.getGlue(), scheduleResult.getMachineCode()), new Long(params.get(GlueEngineConstants.MIX_INTERVAL_TIME))); // 炼胶间隔时间（秒）
		Long productTime = (mixTime + intervalTime) * productedQty.longValue(); // 总生产时间 = （炼胶时间 + 间隔时间）* 计划数
		Date finishTime = DateUtils.addSeconds(currentTime, (int) productTime.longValue()); // 完成生产时间

		// 确定需要排产的胶料，需要对库存扣减
		glueStock.subtractChildGlueStock(productedQty, recipe);

		// 设置机台的生产状态
		machineProduct.setState(GlueEngineConstants.MACHINE_STATE_ON);
		machineProduct.setStartProductTime(currentTime);
		scheduleResult.setProductState(GlueEngineConstants.MACHINE_STATE_ON);
		scheduleResult.setProductedQty(scheduleResult.getProductedQty().add(productedQty)); // 已生产量要加上本次安排的生产量

		ScheduleEventUtils.setShiftPlanField(scheduleResult, shiftClass, productedQty, currentTime, finishTime, isRequire);
		// 自动排产每个排程一个班次只能排产一次，做不完的排到下个班
		scheduleResult.setStartShiftClass(ShiftClassUtil.getNextShiftClass(shiftClass));

		queue.addLog(DateUtils.parseDateToStr("HH:mm:ss", queue.getCurrentTime()) + "|排产" + scheduleResult.getGlue()
				+ "+" + machineCode + "+" + scheduleResult.getRecipeTypeName() + "===" + productedQty);
		// 添加生产完成事件和首批停放事件
		ScheduleEventUtils.addFinishEvent(queue, scheduleResult, productedQty, params, finishTime, mixTime, intervalTime, currentTime, true);

		Integer switchTime = new Integer(params.getOrDefault(GlueEngineConstants.SCHEDULE_SWITCH_TIME, "0")); // 排程切换时间

        return DateUtils.addSeconds(finishTime, switchTime.intValue());
	}

	/**
	 * 统计排程计划中每种胶料的当班已排量
	 * 
	 * @param scheduleResultList 排程列表
	 * @param shiftClass         班次
	 * @return
	 */
	private Map<CombinedMapKey, Double> statisticsShiftClassPlanQty(List<GlueScheduleResultVo> scheduleResultList,
			int shiftClass) {
		Map<CombinedMapKey, Double> shiftClassPlanedMap = scheduleResultList.stream().collect(Collectors.groupingBy(
				// 按胶料 + 机台作为统计维度
				schedule -> CombinedMapKey.createKey(schedule.getGlue(), schedule.getMachineCode()),
				// 统计对应班次的计划量
				Collectors.collectingAndThen(
						// 根据班次统计胶料的当班计划量
						Collectors.toList(), scheduleResult -> {
							switch (shiftClass) {
							case GlueEngineConstants.SHIFT_CLASS_MID:
								return scheduleResult.stream()
										.collect(Collectors.summingDouble(GlueScheduleResultVo::getMidPlanQty));
							case GlueEngineConstants.SHIFT_CLASS_NIGHT:
								return scheduleResult.stream()
										.collect(Collectors.summingDouble(GlueScheduleResultVo::getNightPlanQty));
							default:
								return scheduleResult.stream()
										.collect(Collectors.summingDouble(GlueScheduleResultVo::getDayPlanQty));
							}
						})));
		return shiftClassPlanedMap;
	}

	/**
	 * 计算本次可排产量
	 *
	 * @param scheduleResult        排产记录
	 * @param planedQty             本胶料当班已排计划量
	 * @param machineProduct        本机台排产情况
	 * @param shiftClass            当前班次
	 * @param glueStock             胶料库存
	 * @param params                系统参数设置
	 * @param currentTime           当前排产时间
	 * @param factoryRequire        当前排产时间
	 * @param overLimitQty          超限制计划量，在预算机台产能时计算出来需要在单班总量需限制上，额外多安排的计划量
	 * @param mixingTimeMap         炼胶间隔时间
	 * @param mixingMinProductMap   炼胶单规格最小排产数
	 * @param scheduleResultList    完成排程记录
	 * @param advanceFactoryRequire 提前预占机台产能的分厂需求量
	 * @return
	 */
	private BigDecimal caculateProductQty(GlueScheduleResultVo scheduleResult, BigDecimal planedQty,
										  GlueScheduleMachineProductVo machineProduct, Integer shiftClass, GlueScheduleStockPool glueStock,
										  Map<String, String> params, Date currentTime, GlueFactoryRequireVo factoryRequire,
										  BigDecimal overLimitQty, Map<String, Long> mixingTimeMap, Map<String, BigDecimal> mixingMinProductMap,
										  List<GlueScheduleResultVo> scheduleResultList,
										  ScheduleEventQueue queue,
										  BigDecimal advanceFactoryRequire) {
		Date scheduleDate = scheduleResult.getScheduleDate();
		BigDecimal planQty = scheduleResult.getPlanQty(); // 总计划排产量
		Map<String, List<GlueScheduleResultVo>> mapProductionModel = queue.getMapProductionModel(); // 生产模式Map
		Map<String, List<GlueScheduleResultVo>> mapScheduleResult = queue.getMapScheduleResult(); // 胶料映射

		BigDecimal surplusProductQty = planQty.subtract(scheduleResult.getProductedQty()); // 待排产计划量
		BigDecimal maxZLCarNum = new BigDecimal(params.get(GlueEngineConstants.MAX_PRODUCT_QTY)); // 单班单规格最大排产数
		BigDecimal maxMLCarNum = Optional.ofNullable(params.get(GlueEngineConstants.MAX_PRODUCT_ML_QTY))
				.map(BigDecimal::new).orElse(maxZLCarNum); // 母炼胶单班最大排产数，如果参数没有配置则等于终炼胶的配置
		BigDecimal maxCarZLRate = new BigDecimal(params.get(GlueEngineConstants.MAX_PRODUCT_RATE)); // 单班单规格最大排产数超范围比率
		BigDecimal maxCarMLRate = Optional.ofNullable(params.get(GlueEngineConstants.MAX_PRODUCT_ML_RATE))
				.map(BigDecimal::new).orElse(maxCarZLRate); // 母炼胶单班最大排产比率，如果参数没有配置则等于终炼胶的配置

		// 根据是否母炼胶，确定使用哪个最大生产配置进行计算
		BigDecimal maxCarNum = GlueEngineConstants.MAJOR_TYPE_ML.equals(scheduleResult.getMajorType()) ? maxMLCarNum
				: maxZLCarNum;
		BigDecimal maxCarRate = GlueEngineConstants.MAJOR_TYPE_ML.equals(scheduleResult.getMajorType()) ? maxCarMLRate
				: maxCarZLRate;
		
		// 单班车数上限，向上取整
		BigDecimal limitCarNum = maxCarNum.multiply(maxCarRate.add(ONE_HUNDRED)).divide(ONE_HUNDRED, 2,
				RoundingMode.CEILING);

		// 计算本次计划排产
		BigDecimal toProductQty = surplusProductQty;
		// 有分厂需求量，则需要排够分厂需求量
		BigDecimal requireDifference = null;
		if (factoryRequire != null && factoryRequire.getRequireDifference() != null) {
			if (factoryRequire.getRequireDifference().compareTo(BigDecimal.ZERO) > 0) {
				requireDifference = factoryRequire.getRequireDifference();
			}
			toProductQty = BigDecimalUtil.greatest(toProductQty, factoryRequire.getRequireDifference());// 取两者的较大值
		}
		// 计算上限时，需要加上本班次同一个胶料不同排程的计划量
		BigDecimal totalProductQty = toProductQty.add(planedQty);
		if (totalProductQty.compareTo(limitCarNum) >= 0) {
			// 当排产数超过上上限，则只将计划量补够本班最大排产数，其余放到下一班
			toProductQty = maxCarNum.subtract(planedQty);
		}

		// 处理额外安排量
		// 如果存在超限制计划量，则需要在原有基础上加上超限制计划量作为本班的预计生产量，且不要超过剩余排产量
		toProductQty = BigDecimalUtil.least(toProductQty.add(overLimitQty), surplusProductQty);
		// 没有可排生产量，则直接返回
		if (toProductQty.compareTo(BigDecimal.ZERO) <= 0) {
			return BigDecimal.ZERO;
		}

		MesPmtRecipeVo recipe = scheduleResult.getPmtRecipe();
		// 计算生产时间
		Long mixTime = recipe.getSummerMixTime(); // 炼胶时间
		Long intervalTime = mixingTimeMap.getOrDefault(GenerageMapKeyUtils.createMapKey(scheduleResult.getGlue(), scheduleResult.getMachineCode()), new Long(params.get(GlueEngineConstants.MIX_INTERVAL_TIME))); // 炼胶间隔时间
		Long productTime = (mixTime + intervalTime) * toProductQty.longValue(); // 总生产时间 = （炼胶时间 + 间隔时间）* 计划数
		BigDecimal expectProductTime = BigDecimalUtil.valueOf(productTime); // 预计生产时长

		// 判断完成时间时间是否超过班次结束时间，如果超过了同样要限制生产量
		Date classEndTime = ShiftClassUtil.getShiftClassEndTime(scheduleDate, shiftClass); // 本版结束时间
		BigDecimal surplusClassTime = BigDecimalUtil.valueOf(classEndTime.getTime() - currentTime.getTime())
				.divide(ONE_THOUSAND, 0, RoundingMode.DOWN); // 本班剩余可生产时长 = 班次结束时间 - 当前时间
		BigDecimal machineProductTime = machineProduct.getProductTime(shiftClass); // 机台剩余时长
		// 计算需要预留的机台产能
		BigDecimal requireDifferenceTime = null;
		if (requireDifference != null) {
			requireDifferenceTime = BigDecimal.valueOf(mixTime + intervalTime).multiply(requireDifference);
			// 本次需要的预占产能
			advanceFactoryRequire = advanceFactoryRequire.subtract(requireDifferenceTime);
		}
		if (advanceFactoryRequire == null || advanceFactoryRequire.compareTo(BigDecimal.ZERO) < 0) {
			advanceFactoryRequire = BigDecimal.ZERO;
		}

		// 剩余生产时长超过本班/本机台剩余产能的情况下，需要限制生产量
		BigDecimal machineTime = BigDecimalUtil.least(surplusClassTime, machineProductTime);
		BigDecimal actualTime = machineTime.subtract(advanceFactoryRequire);
		// 如果可生产的部分不足夜班需求量的情况，产能足够时补够夜班需求
		if (requireDifferenceTime != null && requireDifferenceTime.compareTo(actualTime) > 0 && machineTime.compareTo(requireDifferenceTime) >= 0) {
			actualTime = requireDifferenceTime;
		}
		if (expectProductTime.compareTo(actualTime) > 0) {
			// 实际可生产车数 = 可生产时长 / （炼胶时间 + 间隔时间），结果向下取整
			BigDecimal actualProductQty = actualTime.divide(new BigDecimal(mixTime + intervalTime), 0,
					RoundingMode.DOWN);
			if (actualProductQty.compareTo(BigDecimal.ZERO) <= 0) {
				// 当班机台一车都生产不了，则只能排到下一班
				return BigDecimal.ZERO;
			}
			// 如果产能还有剩余，则生产数要调整成实际可生产车数
			// 取两者较小值
			toProductQty = BigDecimalUtil.least(toProductQty, actualProductQty);
		}

		// 没有可排生产量，则直接返回
		if (toProductQty.compareTo(BigDecimal.ZERO) <= 0) {
			return BigDecimal.ZERO;
		}

		// 当前排程如果存在连续生产的记录
		if (scheduleResult.getBindScheduleResult() != null) {
			GlueScheduleResultVo bindScheduleResult = scheduleResult.getBindScheduleResult();
			// 如果连续生产的记录已经完成
			double surplusQty = ScheduleEventUtils.getContinueSurplusQty(scheduleResultList, bindScheduleResult);
			// 如果一车都无法生产
			if (surplusQty > 0 && !checkProduceOneCar(bindScheduleResult, glueStock)) {
				return BigDecimal.ZERO;
			}
		}

		// 如果当前排产是生产模式，需要判断前置的母胶原料和后置的母胶原料是否至少能够生产至少一车
		if (!checkProduceOneCar(scheduleResult.getProductionBefore(), glueStock)
				|| !checkProduceOneCar(scheduleResult.getProductionAfter(), glueStock)) {
			// 否则，跳过选择该排程
			return BigDecimal.ZERO;
		}
		// 如果是生产模式，需要判断非生产模式的记录是否有生产量，如果有生产量则跳过
		if (scheduleResult.getProductionBefore() != null || scheduleResult.getProductionAfter() != null) {
			double totalQty = ScheduleEventUtils.getTotalQtyByGlue(mapScheduleResult.get(scheduleResult.getGlue()), scheduleResult.getGlue());
			if (totalQty > 0) {
				return BigDecimal.ZERO;
			}
		}

		// 如果胶料存在生产模式的记录
		if (mapProductionModel.containsKey(scheduleResult.getGlue())) {
			// 如果有生产量，直接跳过当前记录
			List<GlueScheduleResultVo> scheduleResultVoList = mapProductionModel.get(scheduleResult.getGlue());
			double totalQty = ScheduleEventUtils.getTotalQtyByGlue(scheduleResultVoList, scheduleResult.getGlue());
			if (totalQty > 0) {
				return BigDecimal.ZERO;
			}
		}

		// 判断配方是否叶子节点 或者 需要塑胶
//		boolean isLeaf = recipe.getRecipeWeightList().stream()
//				.noneMatch(weight -> this.checkStockMajorType(weight.getMajorType()));
		boolean isLeaf = true; // 暂时固定排产，不校验母胶库存
		
		if (isLeaf) {
			// 叶子节点不需要判断其原料库存
			return toProductQty;
		}

		// 单规格最小排产数
		BigDecimal minStock = new BigDecimal(params.get(GlueEngineConstants.MIN_PRODUCT_STOCK)); // 单规格最小排产数
		// 校验作为原料的母炼胶库存是否足够最小生产数
		// 遍历称重信息，取出其中的母炼胶进行库存校验
		BigDecimal newProductQty = toProductQty;
		newProductQty = ScheduleEventUtils.getProductQtyByWeight(glueStock, recipe, newProductQty, minStock, toProductQty, requireDifference);

		// 如果有排产量,需要扣减原料库存
		if (newProductQty.compareTo(BigDecimal.ZERO) > 0) {
			glueStock.subtractChildGlueStock(newProductQty, recipe);
		}
		return newProductQty;
	}

	/**
	 * 判断排程是否能够至少一车也无法生产
	 *
	 * @param aroundSchedule 环绕排程
	 * @param glueStock      胶料
	 * @return 是否能够至少生产一车
	 */
	private boolean checkProduceOneCar(GlueScheduleResultVo aroundSchedule, GlueScheduleStockPool glueStock) {
		// 没有环绕排程，可以直接生产
		if (aroundSchedule == null) {
			return true;
		}

		BigDecimal bindProductQty = ScheduleEventUtils.getProductQtyByWeight(glueStock, aroundSchedule.getPmtRecipe(), BigDecimal.ONE, BigDecimal.ZERO, BigDecimal.ONE, null);
        // 否则，跳过选择该排程
        return bindProductQty != null && bindProductQty.compareTo(BigDecimal.ZERO) > 0;
    }

	/**
	 * 根据优先级筛选每个空闲机台可安排的优先级最高的胶料
	 *
	 * @param scheduleResultList    排程记录
	 * @param queue                 队列信息
	 * @param glueStock             库存信息
	 * @param allScheduleResultList 本次排产完整的排程记录，用于计算优先级
	 * @param shiftClass            当前班次
	 * @param params                排产参数
	 * @param slPriorityMap         塑胶优先级映射
	 * @return
	 */
	private List<GlueScheduleResultVo> chooseMaxPrioritySchedule(List<GlueScheduleResultVo> scheduleResultList,
			ScheduleEventQueue queue, GlueScheduleStockPool glueStock, List<GlueScheduleResultVo> allScheduleResultList,
			Map<String, GlueFactoryRequireVo> factoryRequireMap, Integer shiftClass, Map<String, String> params, Date currentTime, Map<String, List<GlueScheduleResultVo>> slPriorityMap,
																 List<GlueScheduleResultVo> latestScheduleList,
																 Map<String, String> mixingPriorityProductMap,
																 Map<String, List<GlueScheduleResultVo>> needSlScheduleMap) {
		// 先重算胶料的排产优先级
		SchedulePriorityUtils.recaculatePriority(allScheduleResultList, glueStock, factoryRequireMap, shiftClass,
				params, slPriorityMap, latestScheduleList, mixingPriorityProductMap, needSlScheduleMap);

		// 遍历待排产记录，选出每个机台优先级最高的胶料
		Map<String, GlueScheduleResultVo> machineScheduleMap = new HashMap<>();
		for (GlueScheduleResultVo scheduleResultItem : scheduleResultList) {
			String machineCode = scheduleResultItem.getMachineCode();
			String glueCode = scheduleResultItem.getGlue();
			// 取出包含父级节点的最高优先级
			BigDecimal priority = scheduleResultItem.getPriority();
			// 如果当前胶料是高耗能胶 且 当前时间为高耗能优先时间点，将高耗能胶的优先级设置为最高
			if (GlueEngineConstants.ISORNOT_YES.equals(scheduleResultItem.getIsHighConsumption())
					&& (queue.getHighConsumptionBegin().before(currentTime) || queue.getHighConsumptionBegin().equals(currentTime))
					&& queue.getHighConsumptionEnd().after(currentTime)) {
				priority = BigDecimal.valueOf(Long.MAX_VALUE);
			}
			
			GlueScheduleResultVo productedScheduleResult = machineScheduleMap.get(machineCode); // 获取已安排到该机台的排产信息
			if (productedScheduleResult != null) {
				// 如果已经有其他胶料安排到该机台，需要比较两者的优先级
				BigDecimal productedPriority = productedScheduleResult.getPriority();
				String productedGlueCode = productedScheduleResult.getGlue();
				// 先判断已排胶料跟待排胶料是否同一种胶料
				if (productedGlueCode.equals(glueCode)) {
					// 相同的胶料按配方优先级排，数字越大优先级越高
					Integer recipePriority = RecipeUtil.getRecipeTypePriority(scheduleResultItem.getRecipeTypeName());
					Integer productedRecipePriority = RecipeUtil
							.getRecipeTypePriority(productedScheduleResult.getRecipeTypeName());
					if (recipePriority.compareTo(productedRecipePriority) < 0) {
						continue; // 优先级不高跳过
					}
				} else if (priority.compareTo(productedPriority) < 0) {
					continue; // 优先级不高跳过
				} else if (productedScheduleResult.getPriority().compareTo(priority) == 0) {
					// 如果优先级相等，则判断上级胶料的优先级
					BigDecimal parentPriority = this.getMaxPriorityInTree(scheduleResultItem, allScheduleResultList);
					BigDecimal parentProductedPriority = this.getMaxPriorityInTree(productedScheduleResult,
							allScheduleResultList);
					if (parentPriority.compareTo(parentProductedPriority) < 0) {
						continue; // 优先级不高跳过
					}
				}
			}
			// 校验通过则将胶料安排至机台上
			machineScheduleMap.put(machineCode, scheduleResultItem);
		}
		return new ArrayList<>(machineScheduleMap.values());
	}

	/**
	 * 获取最大优先级，按胶料分解的树形结构向根节点抓取最大优先级
	 * 
	 * @param scheduleResult        待计算优先级的排产记录
	 * @param allScheduleResultList 排产记录集合
	 * @return
	 */
	private BigDecimal getMaxPriorityInTree(GlueScheduleResultVo scheduleResult,
			List<GlueScheduleResultVo> allScheduleResultList) {
		// 将排产计划那胶料编号分组
		Map<String, List<GlueScheduleResultVo>> glueGroupingMap = allScheduleResultList.stream()
				.collect(Collectors.groupingBy(GlueScheduleResultVo::getGlue));
		BigDecimal priority = scheduleResult.getPriority();
		String upGlueCode = scheduleResult.getUpGlue();
		if (StringUtils.isBlank(upGlueCode)) {
			return priority;
		}

		Set<String> handleGlueSet = new HashSet<>(); // 已计算的胶料
		// 存在多个上级胶，取所有上级胶最大的优先级
		List<String> upGlueList = Arrays.stream(upGlueCode.split(",")).filter(StringUtils::isNotBlank).distinct().collect(Collectors.toList());
		while (CollectionUtils.isNotEmpty(upGlueList)) {
			Set<String> newGlueSet = new HashSet<>();
			for (String upGlueItem : upGlueList) {
				if (handleGlueSet.contains(upGlueItem)) {
					continue; // 如果又循环到已经计算过的胶料，说明进入死循环了（可能是胶料分解的配置有误引起），需要直接跳出
				}
				handleGlueSet.add(upGlueItem);
				List<GlueScheduleResultVo> upGlueScheduleList = glueGroupingMap.get(upGlueItem);
				if (CollectionUtil.isEmpty(upGlueScheduleList)) {
					continue;
				}
				GlueScheduleResultVo upGlueSchedule = CollectionUtil.firstElement(upGlueScheduleList);
				if (upGlueSchedule != null) {
					BigDecimal upGluePriority = upGlueSchedule.getPriority();
					priority = BigDecimalUtil.greatest(upGluePriority, priority);
					upGlueCode = upGlueSchedule.getUpGlue();
					if (StringUtils.isNotBlank(upGlueCode)) {
						newGlueSet.addAll(Arrays.stream(upGlueCode.split(",")).filter(StringUtils::isNotBlank).distinct().collect(Collectors.toList()));
					}
				}
			}

			upGlueList = new ArrayList<>(newGlueSet);
		}
		
		return priority;
	}

	/**
	 * 判断物料类型是否是掺胶物料
	 * 
	 * @param majorType 物料类型
	 * @return
	 */
	private boolean checkMixMajorType(String majorType) {
		return GlueEngineConstants.MIX_MAJOR_TYPE.contains(majorType);
	}

	/**
	 * 判断物料类型是否会消耗库存的物料
	 * 
	 * @param majorType 物料类型
	 * @return
	 */
	private boolean checkStockMajorType(String majorType) {
		return GlueEngineConstants.STOCK_MAJOR_TYPE.contains(majorType) || GlueEngineConstants.MAJOR_TYPE_SL.equals(majorType);
	}
}
