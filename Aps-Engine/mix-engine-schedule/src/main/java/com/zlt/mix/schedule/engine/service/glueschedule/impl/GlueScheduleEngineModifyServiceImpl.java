package com.zlt.mix.schedule.engine.service.glueschedule.impl;

import com.ruoyi.common.core.utils.DateUtils;
import com.ruoyi.common.core.utils.bean.BeanUtils;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.utils.StringUtils;
import com.zlt.mix.common.core.constant.ZltConstant;
import com.zlt.mix.common.core.utils.BigDecimalUtil;
import com.zlt.mix.common.core.utils.MixCommonUtil;
import com.zlt.mix.common.engine.utils.CollectionUtil;
import com.zlt.mix.schedule.engine.constants.GlueEngineConstants;
import com.zlt.mix.schedule.engine.mapper.GlueScheduleEngineMapper;
import com.zlt.mix.schedule.engine.service.basicdata.MachineEngineService;
import com.zlt.mix.schedule.engine.service.basicdata.MixingTimeEngineService;
import com.zlt.mix.schedule.engine.service.glueschedule.GlueScheduleEngineBaseService;
import com.zlt.mix.schedule.engine.service.glueschedule.GlueScheduleEngineModifyService;
import com.zlt.mix.schedule.engine.util.CombinedMapKey;
import com.zlt.mix.schedule.engine.util.GlueScheduleStockPool;
import com.zlt.mix.schedule.engine.util.RecipeUtil;
import com.zlt.mix.schedule.engine.util.ShiftClassUtil;
import com.zlt.mix.schedule.engine.vo.*;
import com.zlt.mix.setting.api.domain.entity.GlueFinish;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.Map.Entry;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 终炼母炼排程更改服务
 * 
 * @author hakimryan
 *
 */
@Service
public class GlueScheduleEngineModifyServiceImpl implements GlueScheduleEngineModifyService {
	@Autowired
	private GlueScheduleEngineBaseService glueScheduleEngineBaseService;
	@Autowired
	private MachineEngineService machineEngineService;
	@Resource
	private GlueScheduleEngineMapper glueScheduleEngineMapper;
	@Autowired
	private MixingTimeEngineService mixingTimeEngineService;

	/**
	 * 1000，用于毫秒换算
	 */
	private final BigDecimal THOUSAND = new BigDecimal("1000");
	/**
	 * 一百，用于计算百分比
	 */
	private BigDecimal ONE_HUNDRED = new BigDecimal("100");

	/**
	 * 构建插单排程记录
	 * 
	 * @param scheduleResult  待插单排程信息
	 * @param recipe          配方
	 * @param glueStock       库存
	 * @param glueParams      排程设置参数
	 * @param allScheduleList 插单机台的所有排产信息
	 * @return 待新增与更新的排程记录
	 */
	@Override
	public List<GlueScheduleResultVo> insertOrder(GlueScheduleResultVo scheduleResult, MesPmtRecipeVo recipe,
			GlueScheduleStockPool glueStock, Map<String, String> glueParams,
			List<GlueScheduleResultVo> allScheduleList) {
		// 校验生产顺序是否重复
		this.checkProduceOrderRepeat(scheduleResult, allScheduleList);

		// 初始化待插单记录
		this.initInsertSchedule(scheduleResult, recipe, glueStock, allScheduleList);

		// 重算每一班的预计时间
		List<GlueScheduleResultVo> updateList = this.recaculateAllExpectTimeAdd(scheduleResult, allScheduleList, recipe,
				glueParams);

		// 将需要修改的量
		return updateList;
	}

	/**
	 * 校验生产顺序是否重复
	 * 
	 * @param scheduleResult
	 * @param allScheduleList
	 */
	private void checkProduceOrderRepeat(GlueScheduleResultVo scheduleResult,
			List<GlueScheduleResultVo> allScheduleList) {
		// 校验生产顺序是否重复
		if (scheduleResult.getMidProduceOrder() != null
				&& allScheduleList.stream().anyMatch(s -> s.getMidProduceOrder() != null
						&& MixCommonUtil.compare(scheduleResult.getMidProduceOrder(), s.getMidProduceOrder()))) {
			throw new RuntimeException("中班生产顺序重复！");
		}
		if (scheduleResult.getNightProduceOrder() != null
				&& allScheduleList.stream().anyMatch(s -> s.getNightProduceOrder() != null
						&& MixCommonUtil.compare(scheduleResult.getNightProduceOrder(), s.getNightProduceOrder()))) {
			throw new RuntimeException("夜班班生产顺序重复！");
		}
		if (scheduleResult.getDayProduceOrder() != null
				&& allScheduleList.stream().anyMatch(s -> s.getDayProduceOrder() != null
						&& MixCommonUtil.compare(scheduleResult.getDayProduceOrder(), s.getDayProduceOrder()))) {
			throw new RuntimeException("白班生产顺序重复！");
		}
	}

	/**
	 * 重算整个排程机台的预计时间
	 * 
	 * 
	 * @param scheduleDate 排产日
	 * @param scheduleList 待重算排程列表
	 * @param params       排产参数
	 * @return
	 */
	@Override
	public void recaculateExpectTimeInList(Date scheduleDate, List<GlueScheduleResultVo> scheduleList,
			Map<String, String> params) {
		this.recaculateExpectTimeSingleClassInList(scheduleDate, scheduleList, params,
				GlueEngineConstants.SHIFT_CLASS_MID);
		this.recaculateExpectTimeSingleClassInList(scheduleDate, scheduleList, params,
				GlueEngineConstants.SHIFT_CLASS_NIGHT);
		this.recaculateExpectTimeSingleClassInList(scheduleDate, scheduleList, params,
				GlueEngineConstants.SHIFT_CLASS_DAY);
	}

	/**
	 * 接收跨区生产
	 * 
	 * @param scheduleDate     排产日
	 * @param mixArea          密炼区
	 * @param batchNo          批次号
	 * @param allScheduleList  已拍计划
	 * @param mesPmtRecipeList 配方列表
	 * @param glueStock        库存信息
	 * @param params           排产参数
	 */
	@Override
	public List<GlueScheduleResultVo> createGlueSpanReceiveSchedule(Date scheduleDate, String mixArea, String batchNo,
			List<GlueSpanReceiveVo> glueSpanReceiveList, List<GlueScheduleResultVo> allScheduleList,
			List<MesPmtRecipeVo> mesPmtRecipeList, GlueScheduleStockPool glueStock, Map<String, String> params) {
		if (CollectionUtils.isEmpty(glueSpanReceiveList)) { // 没有接收的请求，则直接返回
			return new ArrayList<>(0);
		}
		// 加载排程参数
		// todo 跨区生产暂不调整取炼胶间隔表
		Long mixOntervalTime = new Long(params.getOrDefault(GlueEngineConstants.MIX_INTERVAL_TIME, "0")); // 每一车的间隔时间
		Long switchTime = new Long(params.getOrDefault(GlueEngineConstants.SCHEDULE_SWITCH_TIME, "0")); // 排程切换时间（秒）
		BigDecimal maxProductZLQty = new BigDecimal(params.getOrDefault(GlueEngineConstants.MAX_PRODUCT_QTY, "0")); // 单班最大排产数
		BigDecimal maxProductMLQty = Optional.ofNullable(params.get(GlueEngineConstants.MAX_PRODUCT_ML_QTY))
				.map(BigDecimal::new).orElse(maxProductZLQty); // 母炼胶单班最大排产数，如果参数没有配置则等于终炼胶的配置
		BigDecimal maxProductZLRate = new BigDecimal(params.getOrDefault(GlueEngineConstants.MAX_PRODUCT_RATE, "0")); // 单班可超出最大排产数比率
		BigDecimal maxProductMLRate = Optional.ofNullable(params.get(GlueEngineConstants.MAX_PRODUCT_ML_RATE))
				.map(BigDecimal::new).orElse(maxProductZLRate); // 母炼胶单班最大排产比率，如果参数没有配置则等于终炼胶的配置
		BigDecimal singleClassZLLimitPlanQty = maxProductZLQty.multiply(ONE_HUNDRED.add(maxProductZLRate))
				.divide(ONE_HUNDRED, 0, RoundingMode.UP); // 单班最大排产数上限值
		BigDecimal singleClassMLLimitPlanQty = maxProductMLQty.multiply(ONE_HUNDRED.add(maxProductMLRate))
				.divide(ONE_HUNDRED, 0, RoundingMode.UP); // 母炼胶单班最大排产数上限值
		Long dinnerTime = new Long(params.getOrDefault(GlueEngineConstants.DINNER_TIME, "0")) * 60; // 用餐时间
		// 单机台最大产能 = 每班时间 - 用餐时间
		BigDecimal maxCapacity = BigDecimal.valueOf(ShiftClassUtil.ONE_SHIFT_CLASS_TIME - dinnerTime);
		GlueScheduleResultVo result = CollectionUtil.firstElement(allScheduleList);

		// 各胶料备选机台
		List<FormulaMachineVo> formulaMachineList = machineEngineService.listFormulaMachine(mixArea);
		Map<String, List<FormulaMachineVo>> glueMachineMap = formulaMachineList.stream()
				.collect(Collectors.groupingBy(FormulaMachineVo::getGlue));

		// 配方信息，按胶料+机台+配方类型分组
		Map<CombinedMapKey, MesPmtRecipeVo> mesPmtRecipeMap = mesPmtRecipeList.stream().collect(Collectors.toMap(
				r -> CombinedMapKey.createKey(r.getRecipeMaterialName(), r.getRecipeEquipCode(), r.getRecipeType()),
				Function.identity(), (r1, r2) -> r1));
		// 配方信息，按胶料+机台分组
		Map<CombinedMapKey, List<MesPmtRecipeVo>> recipeGlueMap = mesPmtRecipeList.stream().collect(Collectors
				.groupingBy(r -> CombinedMapKey.createKey(r.getRecipeMaterialName(), r.getRecipeEquipCode())));

		// 统计排程的各种计划量
		Map<String, GlueScheduleResultVo> scheduleQtyMap = new HashMap<>();

		// 按机台分组排产结果
		Map<String, List<GlueScheduleResultVo>> scheduleMachineGroupingMap = allScheduleList.stream()
				// 排除掉未提报计划
				.filter(s -> s.getMachineCode() != null)
				.collect(Collectors.groupingBy(GlueScheduleResultVo::getMachineCode));

		// 统计各机台产能列表
		Map<String, BigDecimal> machineCapacityMap = this.statisticsMachineCapacity(formulaMachineList,
				scheduleMachineGroupingMap, maxCapacity, switchTime);
		// 记录本次会更新到的排产记录
		List<GlueScheduleResultVo> updateScheduleList = new ArrayList<>();
		for (GlueSpanReceiveVo glueSpanReceive : glueSpanReceiveList) {
			String glueCode = glueSpanReceive.getGlue();
			String machineCode = glueSpanReceive.getMachineCode();
			String recipeType = glueSpanReceive.getRecipeType();
			Long receiveQty = glueSpanReceive.getReceiveQty();
			MesPmtRecipeVo recipe = mesPmtRecipeMap.get(CombinedMapKey.createKey(glueCode, machineCode, recipeType));
			if (glueCode == null || machineCode == null || recipeType == null || receiveQty == null || recipe == null) {
				continue;
			}
			if (recipe.getMajorType() == null) {
				throw new RuntimeException(StringUtils.format("接收的跨区生产胶料{}没有对应的物料信息！", glueCode));
			}

			// 根据是否母炼胶，确定使用哪个最大生产量配置进行计算
			boolean isMLGlue = GlueEngineConstants.MAJOR_TYPE_ML.equals(glueSpanReceive.getMajorType());
			BigDecimal singleClassLimitPlanQty = isMLGlue ? singleClassMLLimitPlanQty : singleClassZLLimitPlanQty;
			BigDecimal maxProductQty = isMLGlue ? maxProductMLQty : maxProductZLQty;

			// 机台列表
			List<FormulaMachineVo> machineList = glueMachineMap.get(glueCode);
			FormulaMachineVo machine = null;
			if (CollectionUtils.isNotEmpty(machineList)) {
				machine = machineList.stream().filter(m -> m.getMachineCode().equals(glueSpanReceive.getMachineCode()))
						.findFirst().orElse(null);
			}
			if (machine == null) {
				throw new RuntimeException(StringUtils.format("接收的跨区生产胶料{}找不到机台对应关系", glueCode));
			}
			// 已排机台
			Set<String> machineSet = new HashSet<>();
			machineSet.add(machineCode);
			BigDecimal surplusQty = BigDecimal.valueOf(receiveQty); // 待分配的计划量，默认等于接收量

			// 初始化计划量统计值
			GlueScheduleResultVo statisticsQty = scheduleQtyMap.get(glueCode);
			if (statisticsQty == null) {
				statisticsQty = this.buildNewSchedule(scheduleDate, mixArea, batchNo, glueStock, result, recipe);
				statisticsQty.setPlanQty(BigDecimal.ZERO);
				statisticsQty.setProductedQty(BigDecimal.ZERO);

				scheduleQtyMap.put(glueCode, statisticsQty);
			}
			statisticsQty.setPlanQty(statisticsQty.getPlanQty().add(surplusQty));

			// 需要循环处理，直到剩余量都处理完，或者没有机台可以安排
			do {
				// 查看该机台的最后一个班是哪个班
				Integer lastClass = this.getMachineLastClass(machine);
				// 一车胶消耗产能 = 配方炼胶时长 + 单车间隔时长
				BigDecimal mixTime = recipe != null ? BigDecimal.valueOf(recipe.getSummerMixTime() + mixOntervalTime)
						: BigDecimal.ZERO;
				// 如果机台全部班别被禁用或者配方信息不正确，则需要选择用下一个备选机台
				if (lastClass != null && mixTime.compareTo(BigDecimal.ZERO) > 0) {
					// 胶料 + 机台相同的已有排程
					SingleClassGlueScheduleResultVO glueSchedule = null;
					// 找出该机台现有的计划
					List<GlueScheduleResultVo> scheduleMachineList = scheduleMachineGroupingMap.get(machineCode);
					if (CollectionUtils.isNotEmpty(scheduleMachineList)) {
						// 先判断是否有同胶料、同机台、同配方的排产记录
						glueSchedule = scheduleMachineList.stream()
								.filter(s -> glueCode.equals(s.getGlue()) && recipeType.equals(s.getRecipeType()))
								.map(s -> this.extractSingleClassSchedule(s, lastClass)).findFirst().orElse(null);
					} else {
						scheduleMachineList = new ArrayList<>();
						scheduleMachineGroupingMap.put(machineCode, scheduleMachineList);
					}

					// 本次排产可排产的开始时间、结束时间、开始班次
					SingleClassGlueScheduleResultVO classTime = this.getNewProductTime(scheduleDate, glueSchedule,
							scheduleMachineList, switchTime, lastClass);
					Date startTime = classTime.getExpectStartTime();
					Date endTime = classTime.getExpectFinishTime();
					Integer latestDayProduceOrder = classTime.getProduceOrder();

					// 限制计划量最大最小值
					BigDecimal singleClassPlanQty = surplusQty.compareTo(singleClassLimitPlanQty) > 0 ? maxProductQty
							: surplusQty; // 限制最大值
					BigDecimal capacity = singleClassPlanQty.multiply(mixTime); // 本次排产所需要的产能
					// 根据机台剩余产能限制消耗产能
					BigDecimal surplusCapacity = machineCapacityMap.getOrDefault(machineCode, maxCapacity); // 机台剩余产能
					capacity = BigDecimalUtil.least(capacity, surplusCapacity);

					// 要判断当前机台是否有足够的剩余产能安排胶料
					Date expectFinishTime = DateUtils.addSeconds(startTime, capacity.intValue()); // 计算预计完成时间
					BigDecimal actualPlanQty;
					BigDecimal actualCapacity; // 实际需要产能
					if (expectFinishTime.compareTo(endTime) > 0) { // 预计结束时间超过结束时间，说明当班产能不够，需要继续限制排产量
						// 实际消耗产能 = (班别结束时间 - 开始时间) / 1000
						actualCapacity = BigDecimal.valueOf(endTime.getTime() - startTime.getTime()).divide(THOUSAND, 0,
								RoundingMode.DOWN);
						actualPlanQty = actualCapacity.divide(mixTime, 0, RoundingMode.DOWN);
						// 剩余量需尝试排到下一个机台
					} else { // 如果预计结束时间早于结束时间，则更新结束时间
						actualCapacity = capacity;
						endTime = expectFinishTime;
					}
					// 实际量 = 实际消耗产能 / 单车炼胶时间，结果向下取整
					actualPlanQty = actualCapacity.divide(mixTime, 0, RoundingMode.DOWN);

					if (actualPlanQty.compareTo(BigDecimal.ZERO) > 0) {
						// 更新或创建排程计划
						GlueScheduleResultVo updateSchedule = this.updateOrAddNewSchedule(scheduleDate, mixArea,
								batchNo, glueStock, result, glueCode, machineCode, receiveQty, recipe, lastClass,
								glueSchedule, scheduleMachineList, allScheduleList, actualPlanQty, startTime, endTime,
								latestDayProduceOrder);
						this.addScheduleListWithIdCheck(updateSchedule, updateScheduleList);
						// 计划量统计加上本次排上的计划量
						statisticsQty.setProductedQty(statisticsQty.getProductedQty().add(actualPlanQty));
						machineCapacityMap.put(machineCode, surplusCapacity.subtract(actualCapacity)); // 更新机台剩余产能
						surplusQty = surplusQty.subtract(actualPlanQty);
					}

					if (surplusQty.compareTo(BigDecimal.ZERO) == 0) {
						break;
					}
				}

				// 本机台处理完则取下一个机台
				machine = machineList.stream().filter(m -> !machineSet.contains(m.getMachineCode())).findFirst()
						.orElse(null);
				if (machine == null) { // 如果机台已经全部安排完，则不再遍历
					break;
				}
				machineCode = machine.getMachineCode();
				machineSet.add(machineCode);
				// 新机台需要重新选择配方
				List<MesPmtRecipeVo> recipeList = recipeGlueMap.get(CombinedMapKey.createKey(glueCode, machineCode));
				if (CollectionUtil.isEmpty(recipeList)) { // 找不到配方则跳过该机台
					machine = null;
					continue;
				}
				// 有配方则优先找zz配方，没有再用其他的
				if (recipeList.stream().anyMatch(r -> checkRecipeTypeIsZZ(r))) {
					recipe = recipeList.stream().filter(r -> checkRecipeTypeIsZZ(r)).findFirst().get();
				} else {
					recipe = CollectionUtil.firstElement(recipeList);
				}
			} while (surplusQty.compareTo(BigDecimal.ZERO) > 0);
		}

		// 跨区生产有接收，原来有未提报记录的，需要移除掉该记录
		Map<String, List<GlueScheduleResultVo>> needDeleteScheduleList = allScheduleList.stream()
				// 未提报且本次跨区生产有接收的
				.filter(s -> s.getMachineCode() == null && scheduleQtyMap.containsKey(s.getGlue()))
				.collect(Collectors.groupingBy(GlueScheduleResultVo::getGlue));
		for (List<GlueScheduleResultVo> deleteSchedule : needDeleteScheduleList.values()) {
			allScheduleList.removeAll(deleteSchedule);
		}

		// 重算该胶料的剩余量
		List<GlueScheduleResultVo> updateSurplusList = this.recaculateSurplus(scheduleQtyMap, allScheduleList);
		this.mergeScheduleListWithIdCheck(updateSurplusList, updateScheduleList);
		return updateScheduleList;
	}

	/**
	 * 构建新排程记录（不生成工单号）
	 * 
	 * @param scheduleDate 排产日
	 * @param mixArea      密炼区
	 * @param batchNo      批次号
	 * @param glueStock    库存
	 * @param result       原排程信息
	 * @param recipe       配方
	 * @return
	 */
	private GlueScheduleResultVo buildNewSchedule(Date scheduleDate, String mixArea, String batchNo,
			GlueScheduleStockPool glueStock, GlueScheduleResultVo result, MesPmtRecipeVo recipe) {
		String glueCode = recipe.getRecipeMaterialName();
		String machineCode = recipe.getRecipeEquipCode();
		GlueScheduleResultVo statisticsQty = new GlueScheduleResultVo();
		// 构建排程记录
		String majorType = recipe.getMajorType();
		glueScheduleEngineBaseService.copyRecipeProperties(statisticsQty, recipe); // 给配方相关栏位赋值
		statisticsQty.setId(glueStock.nextId()); // 预设一个虚拟ID
		statisticsQty.setGlue(glueCode);
		statisticsQty.setMachineCode(machineCode);
		statisticsQty.setBatchNo(batchNo);
		statisticsQty.setScheduleDate(scheduleDate);
		statisticsQty.setMixArea(mixArea);
		statisticsQty.setTotalPlanQty(0D);
		statisticsQty.setDayProduceOrder(null);
		statisticsQty.setStockQty(glueStock.getStockNum(glueCode, majorType).doubleValue());
		statisticsQty.setSafeStockQty(glueStock.getSafeStock(glueCode).doubleValue());
		statisticsQty.setTotalSurplus(0D);
		statisticsQty.setReleaseStatus(ZltConstant.NO_RELEASE);
		statisticsQty.setDataSource(ZltConstant.GLUE_SCHEDULE_SOURCE_AUTO);
		statisticsQty.setStartShiftClass(GlueEngineConstants.SHIFT_CLASS_MID); // 可开始班次默认中班
		statisticsQty.setPublishSuccessCount(0);
		statisticsQty.setMidPlanQty(0D);
		statisticsQty.setNightPlanQty(0D);
		statisticsQty.setDayPlanQty(0D);
		statisticsQty.setBaseValue(null);
		if (result != null) {
			statisticsQty.setIsFinishing(result.getIsFinishing());
			statisticsQty.setDecomposeBatchNo(result.getDecomposeBatchNo());
		}
		return statisticsQty;
	}

	/**
	 * 检查配方是否zz类
	 * 
	 * @param recipe 配方
	 * @return
	 */
	private boolean checkRecipeTypeIsZZ(MesPmtRecipeVo recipe) {
		return GlueEngineConstants.RECIPE_TYPE_ZZ.equals(recipe.getRecipeTypeName());
	}

	/**
	 * 统计各机台最后一个班的产能列表
	 * 
	 * @param formulaMachineList         机台列表
	 * @param scheduleMachineGroupingMap 按机台分组的已排计划
	 * @param maxCapacity                最大产能
	 * @param switchTime                 切换时间
	 * @return
	 */
	private Map<String, BigDecimal> statisticsMachineCapacity(List<FormulaMachineVo> formulaMachineList,
			Map<String, List<GlueScheduleResultVo>> scheduleMachineGroupingMap, BigDecimal maxCapacity,
			Long switchTime) {
		Map<String, BigDecimal> machineCapacityMap = new HashMap<>();
		Map<String, FormulaMachineVo> machineMap = formulaMachineList.stream()
				.collect(Collectors.toMap(FormulaMachineVo::getMachineCode, Function.identity(), (m1, m2) -> m1));
		// 遍历所有已拍记录
		for (Entry<String, List<GlueScheduleResultVo>> entry : scheduleMachineGroupingMap.entrySet()) {
			String machineCode = entry.getKey();
			List<GlueScheduleResultVo> scheduleList = entry.getValue();
			// 获取机台的最后一个班
			Integer lastClass = this.getMachineLastClass(machineMap.get(machineCode));
			if (lastClass == null) {
				continue;
			}
			// 计算已消耗产能
			BigDecimal consumeCapacity = scheduleList.stream()
					// 将排产计划转换为对应班次的单班计划
					.map(s -> this.extractSingleClassSchedule(s, lastClass))
					// 排除掉当班没有排产的计划
					.filter(s -> s.getExpectStartTime() != null && s.getExpectFinishTime() != null)
					// 计算占用的产能 = 开始时间 - 结束时间 + 切换时长
					.map(s -> BigDecimal.valueOf(s.getExpectFinishTime().getTime() - s.getExpectStartTime().getTime())
							.divide(THOUSAND).add(BigDecimal.valueOf(switchTime)))
					.reduce(BigDecimal.ZERO, BigDecimal::add);
			BigDecimal surplusCapacity = maxCapacity.subtract(consumeCapacity);
			surplusCapacity = BigDecimalUtil.greatest(surplusCapacity, BigDecimal.ZERO);
			machineCapacityMap.put(machineCode, surplusCapacity);
		}
		return machineCapacityMap;
	}

	/**
	 * 重算该胶料的剩余量
	 * 
	 * @param scheduleQtyMap  本次修改涉及到的胶料
	 * @param allScheduleList
	 */
	private List<GlueScheduleResultVo> recaculateSurplus(Map<String, GlueScheduleResultVo> scheduleQtyMap,
			List<GlueScheduleResultVo> allScheduleList) {
		List<GlueScheduleResultVo> updateScheduleList = new ArrayList<>();
		Map<String, List<GlueScheduleResultVo>> glueGroupingMap = allScheduleList.stream()
				// 排除掉未提报计划
				.collect(Collectors.groupingBy(GlueScheduleResultVo::getGlue));
		// 查看本次涉及到的胶料
		for (Entry<String, GlueScheduleResultVo> entry : scheduleQtyMap.entrySet()) {
			String glueCode = entry.getKey(); // 胶料
			GlueScheduleResultVo scheduleResult = entry.getValue();
			BigDecimal planQty = scheduleResult.getPlanQty(); // 待排量
			BigDecimal productedQty = scheduleResult.getProductedQty(); // 已排量
			if (planQty.compareTo(productedQty) == 0) { // 没有剩余量，则不需要更新未排量
				continue;
			}
			List<GlueScheduleResultVo> glueGroupingList = glueGroupingMap.get(glueCode);
			if (CollectionUtil.isEmpty(glueGroupingList)) { // 如果原先没有记录，说明是产能不足的情况，需要添加一笔记录
				// 生成排程记录
				scheduleResult.setOrderNo(glueScheduleEngineBaseService.createOrderNo(scheduleResult.getBatchNo())); // 工单号
				// 将记录添加到列表中
				glueGroupingList = new ArrayList<>();
				glueGroupingList.add(scheduleResult);
				allScheduleList.add(scheduleResult);
				glueGroupingMap.put(glueCode, glueGroupingList);
			}
			BigDecimal totalSurplus = BigDecimal.ZERO;
			for (GlueScheduleResultVo schedule : glueGroupingList) {
				if (schedule.getTotalSurplus() == null) {
					schedule.setTotalSurplus(0D);
				} else if (schedule.getTotalSurplus() > 0D) {
					totalSurplus = BigDecimal.valueOf(schedule.getTotalSurplus());
					schedule.setTotalSurplus(0D);
				}
			}
			totalSurplus = BigDecimalUtil.greatest(totalSurplus.add(planQty.subtract(productedQty)), BigDecimal.ZERO);
			// 剩余量更新到计划量最小的一条排程上
			GlueScheduleResultVo updateSchedule = glueGroupingList.stream()
					.min(Comparator.comparing(GlueScheduleResultVo::getTotalPlanQty)).get();
			updateSchedule.setTotalSurplus(totalSurplus.doubleValue());
			updateScheduleList.add(updateSchedule);
		}
		return updateScheduleList;
	}

	/**
	 * 查看机台最后一个可用班次
	 * 
	 * @param machine
	 * @return
	 */
	private Integer getMachineLastClass(FormulaMachineVo machine) {
		if (machine == null) {
			return null;
		}
		Integer lastClass;
		if (machine.getDayStatus()) {
			lastClass = GlueEngineConstants.SHIFT_CLASS_DAY;
		} else if (machine.getNightStatus()) {
			lastClass = GlueEngineConstants.SHIFT_CLASS_NIGHT;
		} else if (machine.getMidStatus()) {
			lastClass = GlueEngineConstants.SHIFT_CLASS_MID;
		} else {
			lastClass = null;
		}
		return lastClass;
	}

	/**
	 * 更新或创建排程计划
	 * 
	 * @param scheduleDate
	 * @param mixArea
	 * @param batchNo
	 * @param allScheduleList
	 * @param glueStock
	 * @param result
	 * @param glueCode
	 * @param machineCode
	 * @param receiveQty
	 * @param recipe
	 * @param lastClass
	 * @param glueSchedule
	 * @param scheduleMachineList
	 * @param actualPlanQty
	 * @param startTime
	 * @param endTime
	 * @param latestDayProduceOrder
	 */
	private GlueScheduleResultVo updateOrAddNewSchedule(Date scheduleDate, String mixArea, String batchNo,
			GlueScheduleStockPool glueStock, GlueScheduleResultVo result, String glueCode, String machineCode,
			Long receiveQty, MesPmtRecipeVo recipe, Integer lastClass, SingleClassGlueScheduleResultVO glueSchedule,
			List<GlueScheduleResultVo> scheduleMachineList, List<GlueScheduleResultVo> allScheduleList,
			BigDecimal actualPlanQty, Date startTime, Date endTime, Integer latestDayProduceOrder) {
		// 上一个胶料的基础上加10
		Integer produceOrder = Optional.ofNullable(latestDayProduceOrder).orElse(0) + 10;
		if (glueSchedule != null) { // 如果计划跟上一个排产胶相同，则只需要更新计划量和计划时间
			glueSchedule.setPlanQty(glueSchedule.getPlanQty().add(actualPlanQty));
			glueSchedule.setExpectStartTime(startTime);
			glueSchedule.setExpectFinishTime(endTime);
			glueSchedule.setProduceOrder(produceOrder);
			this.updateExpectTime(glueSchedule);
			this.updatePlanQty(glueSchedule);
			return glueSchedule.getScheduleResult();
		} else {
			// 没有相同胶料、相同配方的情况下，插入排产计划
			GlueScheduleResultVo scheduleResult = this.buildNewSchedule(scheduleDate, mixArea, batchNo, glueStock,
					result, recipe);
			scheduleResult.setOrderNo(glueScheduleEngineBaseService.createOrderNo(batchNo)); // 工单号
			scheduleResult.setRequireQty(BigDecimalUtil.valueOfZero(receiveQty).doubleValue());
			scheduleResult.setPlanQty(actualPlanQty);
			SingleClassGlueScheduleResultVO tempSingleSchedule = this.extractSingleClassSchedule(scheduleResult,
					lastClass);
			tempSingleSchedule.setPlanQty(actualPlanQty);
			tempSingleSchedule.setExpectStartTime(startTime);
			tempSingleSchedule.setExpectFinishTime(endTime);
			tempSingleSchedule.setProduceOrder(produceOrder);
			this.updateExpectTime(tempSingleSchedule);
			this.updatePlanQty(tempSingleSchedule);

			allScheduleList.add(scheduleResult);
			scheduleMachineList.add(scheduleResult);
			return scheduleResult;
		}
	}

	/**
	 * 获取本次排产开始时间、结束时间，默认次序信息
	 * 
	 * @param scheduleDate
	 * @param glueSchedule
	 * @param scheduleMachineList
	 * @param switchTime
	 * @param lastClass
	 * @return
	 */
	private SingleClassGlueScheduleResultVO getNewProductTime(Date scheduleDate,
			SingleClassGlueScheduleResultVO glueSchedule, List<GlueScheduleResultVo> scheduleMachineList,
			Long switchTime, Integer lastClass) {
		Date classEndTime = ShiftClassUtil.getShiftClassEndTime(scheduleDate, lastClass); // 当班结束时间
		Date startTime = null;
		Date endTime = null;
		Integer produceOrder = null;
		SingleClassGlueScheduleResultVO classTime = new SingleClassGlueScheduleResultVO();
		if (glueSchedule != null && glueSchedule.getProduceOrder() != null) { // 有相同胶料、相同配方的情况下，更新排产计划
			Integer productOrder = glueSchedule.getProduceOrder();
			// 先看是否有下一个排程
			SingleClassGlueScheduleResultVO nextSingleSchedule = scheduleMachineList.stream()
					.map(s -> this.extractSingleClassSchedule(s, lastClass))
					// 排产顺序在之后的
					.filter(s -> s.getProduceOrder() != null && s.getProduceOrder() > productOrder)
					.min(Comparator.comparing(SingleClassGlueScheduleResultVO::getProduceOrder)).orElse(null);
			// 当班有排产的情况，开始时间不变，结束时间到下一个排程开始时间为至
			startTime = glueSchedule.getExpectStartTime();
			if (nextSingleSchedule != null) { // 有下一个排程，到其开始时间为止
				// 要到排程切换前
				endTime = DateUtils.addSeconds(nextSingleSchedule.getExpectStartTime(),
						(int) switchTime.longValue() * -1);
			} else { // 没有下一个排程，则可以排到本班结束
				endTime = classEndTime;
			}
		}

		// 如果依然无法确定排产时间，说明本班没有排相同胶料 + 配方的排程
		if (startTime == null || endTime == null) {
			// 取出最后一笔排程
			SingleClassGlueScheduleResultVO latestSchedule = scheduleMachineList.stream()
					// 将最后一班的排产信息转换为单班计划对象
					.map(s -> this.extractSingleClassSchedule(s, lastClass))
					// 过滤掉没有完成时间的
					.filter(s -> s.getExpectFinishTime() != null && s.getProduceOrder() != null)
					// 取最晚的一笔
					.max(Comparator.comparing(SingleClassGlueScheduleResultVO::getExpectFinishTime)).orElse(null);
			if (latestSchedule != null) { // 当班有其他胶料排程
				produceOrder = latestSchedule.getProduceOrder();
				// 计算开始时间 = 最后一笔计划的完成时间 + 排程切换时间
				Date latestFinishTime = latestSchedule.getExpectFinishTime();
				startTime = DateUtils.addSeconds(latestFinishTime, (int) switchTime.longValue());
			} else { // 没有任何一个排程，则开始时间 = 班次开始时间
				startTime = ShiftClassUtil.getShiftClassStartTime(scheduleDate, lastClass);
			}
			endTime = classEndTime; // 结束时间则直接到班次结束时间
		}

		// 开始时间和结束时间不能超过班次结束时间
		if (endTime.compareTo(classEndTime) > 0) {
			endTime = classEndTime;
		}
		if (startTime.compareTo(classEndTime) > 0) {
			startTime = classEndTime;
		}

		classTime.setExpectStartTime(startTime);
		classTime.setExpectFinishTime(endTime);
		classTime.setProduceOrder(produceOrder);
		return classTime;
	}

	/**
	 * 重算单班预计时间
	 * 
	 * @param scheduleDate 排产日
	 * @param scheduleList 待重算排程列表
	 * @param params       排产参数
	 * @param shiftClass   排产班次
	 */
	private void recaculateExpectTimeSingleClassInList(Date scheduleDate, List<GlueScheduleResultVo> scheduleList,
			Map<String, String> params, int shiftClass) {
		List<SingleClassGlueScheduleResultVO> classScheduleList = this
				.selectSingleClassScheduleListByImport(scheduleList, shiftClass);
		// 机台排产时间
		Map<String, Date> machineTime = new HashMap<>();
		for (SingleClassGlueScheduleResultVO classSchedule : classScheduleList) {
			GlueScheduleResultVo schedule = classSchedule.getScheduleResult(); // 排程记录
			String machineCode = classSchedule.getMachineCode(); // 机台
			MesPmtRecipeVo recipe = schedule.getPmtRecipe(); // 配方
			if (classSchedule.getProduceOrder() == null || classSchedule.getPlanQty() == null
					|| classSchedule.getPlanQty().compareTo(BigDecimal.ZERO) == 0) {
				// 序号或者计划量不完整则将预计时间清空
				classSchedule.setExpectStartTime(null);
				classSchedule.setExpectFinishTime(null);
			} else {
				// 计算需要的生产时间
				BigDecimal productTime = this.caculateProductTime(classSchedule.getPlanQty(), recipe, params, schedule.getIntervalTime());
				Long switchTime = new Long(params.getOrDefault(GlueEngineConstants.SCHEDULE_SWITCH_TIME, "0")); // 排程切换时间（秒）
				long extendTime = switchTime + productTime.longValue(); // 额外插入的时间：排程切换时间 + 生产时间（秒）
				Date currentTime = machineTime.getOrDefault(machineCode,
						ShiftClassUtil.getShiftClassStartTime(scheduleDate, shiftClass)); // 获取机台当前的排产时间
				Date startTime = currentTime;
				Date finishTime = DateUtils.addSeconds(startTime, (int) productTime.longValue());
				classSchedule.setExpectStartTime(startTime);
				classSchedule.setExpectFinishTime(finishTime);
				currentTime = DateUtils.addSeconds(currentTime, (int) extendTime); // 机台安排时间往前推进
				machineTime.put(machineCode, currentTime);
			}
			this.updateExpectTime(classSchedule);
		}
	}

	/**
	 * 过滤出单班有排程信息的数据，并按机台、生产顺序排序
	 * 
	 * @param machineScheduleList
	 * @param shiftClass
	 * @return
	 */
	private List<SingleClassGlueScheduleResultVO> selectSingleClassScheduleListByImport(
			List<GlueScheduleResultVo> machineScheduleList, int shiftClass) {
		// 该班所有排产计划
		List<SingleClassGlueScheduleResultVO> scheduleResultList = machineScheduleList.stream()
				// 排除掉删除状态的记录
				.filter(schedule -> !ZltConstant.DEL_FLAG_DEL.equals(schedule.getDelFlag()))
				// 抽取单班排产数据
				.map(schedule -> this.extractSingleClassSchedule(schedule, shiftClass))
				// 按生产顺序顺序排序
				.sorted((schedule1, schedule2) -> {
					// 先按机台排序
					String machineCode1 = schedule1.getMachineCode();
					String machineCode2 = schedule2.getMachineCode();
					int machineCompare = machineCode1.compareTo(machineCode2);
					if (machineCompare != 0) {
						return machineCompare;
					}
					// 再按序号排序
					Integer order1 = schedule1.getProduceOrder();
					Integer order2 = schedule2.getProduceOrder();
					return ObjectUtils.compare(order1, order2, true);
				}).collect(Collectors.toList());
		return scheduleResultList;
	}

	/**
	 * 添加排程数据到列表中，但是要验证ID是否已经存在，已存在则不处理该排程记录
	 * 
	 * @param schedule     待添加排程记录
	 * @param scheduleList 目标列表
	 */
	private void addScheduleListWithIdCheck(GlueScheduleResultVo schedule, List<GlueScheduleResultVo> scheduleList) {
		if (schedule == null) {
			return;
		}
		if (scheduleList == null) {
			return;
		}
		if (scheduleList.stream().noneMatch(
				targetSchedule -> targetSchedule.getId() != null && targetSchedule.getId().equals(schedule.getId()))) {
			scheduleList.add(schedule);
		}
	}

	/**
	 * 添加排程列表数据，但是要验证ID是否已经存在，已存在则不处理该排程记录
	 * 
	 * @param sourceScheduleList 待添加列表
	 * @param targetScheduleList 目标列表
	 */
	private void mergeScheduleListWithIdCheck(List<GlueScheduleResultVo> sourceScheduleList,
			List<GlueScheduleResultVo> targetScheduleList) {
		if (CollectionUtil.isEmpty(sourceScheduleList)) {
			return;
		}
		// 将排产列表整理成map<主键ID，排产记录>
		Map<Long, GlueScheduleResultVo> targetScheduleMap = targetScheduleList.stream()
				.filter(schedule -> schedule.getId() != null)
				.collect(Collectors.toMap(GlueScheduleResultVo::getId, Function.identity(), (s1, s2) -> s1));
		// 通过排产记录的ID匹配条件，取两者的并集
		for (GlueScheduleResultVo schedule : sourceScheduleList) {
			Long id = schedule.getId();
			if (id == null) {
				targetScheduleList.add(schedule);
				continue;
			}
			if (targetScheduleMap.containsKey(id)) {
				continue;
			}
			targetScheduleMap.put(id, schedule);
			targetScheduleList.add(schedule);
		}
	}

	/**
	 * 初始化待插单排程记录
	 * 
	 * @param scheduleResult  待插单排程
	 * @param recipe          配方
	 * @param glueStock       库存
	 * @param allScheduleList 当天已有的排程记录
	 * @return
	 */
	private void initInsertSchedule(GlueScheduleResultVo scheduleResult, MesPmtRecipeVo recipe,
			GlueScheduleStockPool glueStock, List<GlueScheduleResultVo> allScheduleList) {
		Date scheduleDate = scheduleResult.getScheduleDate();
		String mixArea = scheduleResult.getMixArea();
		String glueCode = scheduleResult.getGlue();
		// 初始化插单的必要信息
		scheduleResult.setBaseValue(null);
		scheduleResult.setReleaseStatus(ZltConstant.NO_RELEASE);
		scheduleResult.setPublishSuccessCount(0);
		glueScheduleEngineBaseService.copyRecipeProperties(scheduleResult, recipe);
		// 处理批次号与工单号
		String batchNo = null;
		if (!CollectionUtil.isEmpty(allScheduleList)) {
			batchNo = CollectionUtil.firstElement(allScheduleList).getBatchNo(); // 同一天有其他排产数据则直接使用该批次号
		} else {
			batchNo = glueScheduleEngineBaseService.createBatchNo(scheduleDate, mixArea); // 否则新生成一个批次号
		}
		scheduleResult.setBatchNo(batchNo);
		scheduleResult.setOrderNo(glueScheduleEngineBaseService.createOrderNo(scheduleResult.getBatchNo()));
		scheduleResult.setTotalPlanQty(BigDecimalUtil.add(scheduleResult.getMidPlanQty(),
				scheduleResult.getNightPlanQty(), scheduleResult.getDayPlanQty())); // 设置插单记录的总计划量

		// 抓取库存
		BigDecimal stockNum = Optional.ofNullable(glueStock.getStockNum(glueCode, recipe.getMajorType()))
				.orElse(BigDecimal.ZERO);
		BigDecimal safeStockQty = Optional.ofNullable(glueStock.getSafeStock(glueCode)).orElse(BigDecimal.ZERO);
		scheduleResult.setStockQty(stockNum.doubleValue());
		scheduleResult.setSafeStockQty(safeStockQty.doubleValue());
	}

	/**
	 * 修改排程后重算涉及机台的所有的预计时间
	 * 
	 * @param scheduleResult      修改后的排程
	 * @param oldScheduleResult   修改前的排程
	 * @param machineScheduleList 该机台的其他所有排程记录
	 * @param recipeList          该机台对应的配方信息
	 * @param params              排程设置
	 * @return
	 */
	@Override
	public List<GlueScheduleResultVo> recaculateAllExpectTime(GlueScheduleResultVo scheduleResult,
			GlueScheduleResultVo oldScheduleResult, List<GlueScheduleResultVo> machineScheduleList,
			MesPmtRecipeVo recipe, Map<String, String> params) {
		// 重算各班的预计时间，并整理出所有需要更新的排程数据
		List<GlueScheduleResultVo> allUpdateList = new ArrayList<>();
		allUpdateList.add(scheduleResult);
		List<GlueScheduleResultVo> updateList;
		updateList = this.recaculateSingleClassExpectTime(scheduleResult, oldScheduleResult, machineScheduleList,
				params, recipe, GlueEngineConstants.SHIFT_CLASS_MID);
		this.mergeScheduleListWithIdCheck(updateList, allUpdateList);
		updateList = this.recaculateSingleClassExpectTime(scheduleResult, oldScheduleResult, machineScheduleList,
				params, recipe, GlueEngineConstants.SHIFT_CLASS_NIGHT);
		this.mergeScheduleListWithIdCheck(updateList, allUpdateList);
		updateList = this.recaculateSingleClassExpectTime(scheduleResult, oldScheduleResult, machineScheduleList,
				params, recipe, GlueEngineConstants.SHIFT_CLASS_DAY);
		this.mergeScheduleListWithIdCheck(updateList, allUpdateList);

		return allUpdateList;
	}

	/**
	 * 重算单班预计时间
	 * 
	 * @param scheduleResult      需重算的排程记录
	 * @param oldScheduleResult   原排程记录
	 * @param machineScheduleList 同机台的排程列表
	 * @param params              排程参数设置
	 * @param recipe              配方
	 * @param shiftClass          班别
	 * @return
	 */
	private List<GlueScheduleResultVo> recaculateSingleClassExpectTime(GlueScheduleResultVo scheduleResult,
			GlueScheduleResultVo oldScheduleResult, List<GlueScheduleResultVo> machineScheduleList,
			Map<String, String> params, MesPmtRecipeVo recipe, int shiftClass) {
		// 取出对应班次的排产信息
		SingleClassGlueScheduleResultVO classResult = this.extractSingleClassSchedule(scheduleResult, shiftClass);
		SingleClassGlueScheduleResultVO oldClassResult = this.extractSingleClassSchedule(oldScheduleResult, shiftClass);
		// 判断修改了什么内容
		Integer produceOrder = classResult.getProduceOrder();
		Integer oldProduceOrder = oldClassResult.getProduceOrder();
		BigDecimal planQty = Optional.ofNullable(classResult.getPlanQty()).orElse(BigDecimal.ZERO);
		BigDecimal oldPlanQty = Optional.ofNullable(oldClassResult.getPlanQty()).orElse(BigDecimal.ZERO);
		// 判断是否修改了生产量
		boolean isModifyPlanQty = planQty.compareTo(oldPlanQty) != 0;
		// 判断是否修改了配方
		boolean isModifyRecipe = !Objects.equals(recipe.getRecipeType(), oldScheduleResult.getRecipeType())
				|| !Objects.equals(recipe.getRecipeVersionId(), oldScheduleResult.getRecipeVersionId()) // 修改了配方版本
				|| !Objects.equals(recipe.getProductStage(), oldScheduleResult.getRecipeStage()); // 修改了配方阶段
		// 判断是否修改了次序
		boolean isModifyProduceOrder = this.checkModifyProduceOrder(scheduleResult, oldScheduleResult,
				machineScheduleList, shiftClass, produceOrder, oldProduceOrder);

		if (!isModifyProduceOrder && !isModifyPlanQty && !isModifyRecipe) {
			// 没有修改任意东西，直接返回空列表
			return new ArrayList<>(0);
		}
		List<GlueScheduleResultVo> updateList = new ArrayList<>();
		// 如果序号改为空或者计划量改为0，直接删除
		if (produceOrder == null || planQty.compareTo(BigDecimal.ZERO) == 0) {
			updateList = this.recaculateExpectTimeRemove(oldScheduleResult, machineScheduleList, params, shiftClass); // 删除
			// 删除后预计时间需要清空掉
			classResult.setExpectFinishTime(null);
			classResult.setExpectStartTime(null);
			this.updateExpectTime(classResult);
		} else if (isModifyProduceOrder) {
			// 修改了次序，相当于把原次序的排程删除，再添加新次序的排程
			List<GlueScheduleResultVo> removeScheduleList = this.recaculateExpectTimeRemove(oldScheduleResult,
					machineScheduleList, params, shiftClass); // 删除
			List<GlueScheduleResultVo> addScheduleList = this.recaculateExpectTimeAdd(scheduleResult,
					machineScheduleList, recipe, params, shiftClass); // 添加
			this.mergeScheduleListWithIdCheck(removeScheduleList, updateList);
			this.mergeScheduleListWithIdCheck(addScheduleList, updateList);
		} else if (isModifyPlanQty || isModifyRecipe) {
			if (oldPlanQty.compareTo(BigDecimal.ZERO) == 0) {
				// 如果是原计划量为0，增加了计划量，则相当于新增排程
				updateList = this.recaculateExpectTimeAdd(scheduleResult, machineScheduleList, recipe, params,
						shiftClass);
			} else {
				// 只修改了量或者配方，只需要更新后续的时间
				updateList = this.recaculateExpectTimeModify(scheduleResult, machineScheduleList, recipe, params,
						shiftClass);
			}
		}
		return updateList;
	}

	/**
	 * 判断是否修改了次序
	 * 
	 * @param scheduleResult      本次修改排产记录
	 * @param oldScheduleResult   修改前记录
	 * @param machineScheduleList 本机台所有排程记录
	 * @param shiftClass          班次
	 * @param produceOrder        修改后次序
	 * @param oldProduceOrder     修改前次序
	 * @return
	 */
	private boolean checkModifyProduceOrder(GlueScheduleResultVo scheduleResult, GlueScheduleResultVo oldScheduleResult,
			List<GlueScheduleResultVo> machineScheduleList, int shiftClass, Integer produceOrder,
			Integer oldProduceOrder) {
		boolean isModifyProduceOrder = produceOrder != oldProduceOrder;
		if (isModifyProduceOrder && produceOrder != null && oldProduceOrder != null) {
			// 如果有修改过次序，还要校验实际顺序是否有变化，例如原先是10 < 20 < 30，如果第二个改成23，实际顺序是10 < 23 < 30，顺序实际没有变化
			// 取出当班有完整排产信息的排程记录，且按生产顺序排序
			List<GlueScheduleResultVo> machineAllScheduleList = new ArrayList<>(machineScheduleList);
			machineAllScheduleList.add(oldScheduleResult); // 把旧排产记录加回去
			List<SingleClassGlueScheduleResultVO> scheduleList = this
					.selectSingleClassScheduleList(machineAllScheduleList, shiftClass);
			// 按顺序遍历当班的排产记录
			SingleClassGlueScheduleResultVO previousSchedule = null; // 记录每一次遍历的上一个排产计划
			isModifyProduceOrder = false; // 修改次序标识重置
			for (SingleClassGlueScheduleResultVO scheduleItem : scheduleList) {
				GlueScheduleResultVo tempScheduleResult = scheduleItem.getScheduleResult();
				if (scheduleResult.getId().equals(tempScheduleResult.getId())) {
					// 遍历到修改前的排程记录，看上一个排产计划记录
					if (previousSchedule != null && previousSchedule.getProduceOrder() > produceOrder) {
						// 如果新顺序调整成比原先上一个排程的排产顺序小，说明顺序确实有调整
						isModifyProduceOrder = true;
						break;
					}
				} else if (previousSchedule != null
						&& scheduleResult.getId().equals(previousSchedule.getScheduleResult().getId())) {
					// 遍历到修改前排程记录的下一个排产计划记录
					if (scheduleItem.getProduceOrder() < produceOrder) {
						// 如果新顺序调整成比下一个排程的排产顺序大，说明顺序确实有调整
						isModifyProduceOrder = true;
						break;
					}
				}
				previousSchedule = scheduleItem;
			}
		}
		return isModifyProduceOrder;
	}

	/**
	 * 新增排程后重算所有的预计时间
	 * 
	 * @param scheduleResult      新增的排程
	 * @param machineScheduleList 该机台的所有排程记录
	 * @param recipe              新增排程对应的配方信息
	 * @param glueParams          排程设置
	 * @return 有重算过预计时间的排程（含新增/修改的排程）
	 */
	public List<GlueScheduleResultVo> recaculateAllExpectTimeAdd(GlueScheduleResultVo scheduleResult,
			List<GlueScheduleResultVo> machineScheduleList, MesPmtRecipeVo recipe, Map<String, String> glueParams) {
		// 需更新的排程
		List<GlueScheduleResultVo> updateList = new ArrayList<>();
		updateList.add(scheduleResult); // 本次插单/更新的记录默认加到列表里

		// 判断哪一班有计划量，就说明需要向哪一班插入计划量，该班的预计时间就需要重算
		// 计算中班
		if (scheduleResult.getMidPlanQty() != null && scheduleResult.getMidPlanQty() > 0) {
			List<GlueScheduleResultVo> scheduleList = this.recaculateExpectTimeAdd(scheduleResult, machineScheduleList,
					recipe, glueParams, GlueEngineConstants.SHIFT_CLASS_MID);
			this.mergeScheduleListWithIdCheck(scheduleList, updateList);
		}
		// 计算夜班
		if (scheduleResult.getNightPlanQty() != null && scheduleResult.getNightPlanQty() > 0) {
			List<GlueScheduleResultVo> scheduleList = this.recaculateExpectTimeAdd(scheduleResult, machineScheduleList,
					recipe, glueParams, GlueEngineConstants.SHIFT_CLASS_NIGHT);
			this.mergeScheduleListWithIdCheck(scheduleList, updateList);
		}
		// 计算白班
		if (scheduleResult.getDayPlanQty() != null && scheduleResult.getDayPlanQty() > 0) {
			List<GlueScheduleResultVo> scheduleList = this.recaculateExpectTimeAdd(scheduleResult, machineScheduleList,
					recipe, glueParams, GlueEngineConstants.SHIFT_CLASS_DAY);
			this.mergeScheduleListWithIdCheck(scheduleList, updateList);
		}
		return updateList;
	}

	/**
	 * 转机台服务
	 * 
	 * @param scheduleList         待插单排程信息
	 * @param recipeList           配方列表
	 * @param params               排程设置参数
	 * @param allScheduleList      插单机台的所有排产信息
	 * @param isUpdateProduceOrder 是否更新生产顺序，true：需要以参数传入的顺序为准
	 * @return
	 */
	@Override
	public List<GlueScheduleResultVo> changeMachine(List<GlueScheduleResultVo> scheduleList,
			List<MesPmtRecipeVo> recipeList, Map<String, String> glueParams, List<GlueScheduleResultVo> allScheduleList,
			boolean isUpdateProduceOrder) {
		Map<Long, GlueScheduleResultVo> allScheduleMap = allScheduleList.stream()
				.collect(Collectors.toMap(GlueScheduleResultVo::getId, Function.identity()));

		Map<CombinedMapKey, MesPmtRecipeVo> mesPmtRecipeMap = recipeList.stream().collect(Collectors.toMap(
				r -> CombinedMapKey.createKey(r.getRecipeMaterialName(), r.getRecipeEquipCode(), r.getRecipeType()),
				Function.identity(), (r1, r2) -> r1));

		// 遍历(实际场景只会有一个机台一个胶料，不会循环多次)
		List<GlueScheduleResultVo> updateList = new ArrayList<>();
		for (GlueScheduleResultVo scheduleResultVo : scheduleList) {
			// 取出转机台前的记录，并移除掉
			GlueScheduleResultVo oldSchedule = allScheduleMap.remove(scheduleResultVo.getId());
			if (oldSchedule == null) {
				throw new RuntimeException(scheduleResultVo.getGlue() + "胶料转机台数据有误");
			}

			// 拷贝一份旧排产记录
			GlueScheduleResultVo oldScheduleBackup = new GlueScheduleResultVo();
			BeanUtils.copyProperties(oldSchedule, oldScheduleBackup);

			String oldMachineCode = oldSchedule.getMachineCode(); // 旧机台
			boolean isNoRequire = oldMachineCode == null; // 转机台前如果机台为空，说明是未提报胶料排程
			String glueCode = scheduleResultVo.getGlue();
			String machineCode = scheduleResultVo.getMachineCode();
			String recipeType = scheduleResultVo.getRecipeType();
			String mixArea = scheduleResultVo.getMixArea();
			Date scheduleDate = scheduleResultVo.getScheduleDate();
			String orderNo = scheduleResultVo.getOrderNo();
			String batchNo = scheduleResultVo.getBatchNo();

			if (StringUtils.isEmpty(recipeType)) {
				throw new RuntimeException("配方类型不能为空"); // 未提报胶料转机台必须确认配方类型
			}

			if (!isNoRequire && oldMachineCode.equals(machineCode)) {
				throw new RuntimeException("不能转到相同的机台上。");
			}

			// 获取胶料完成量，<工单号， 完成量>
			Map<String, GlueFinish> finishMap = glueScheduleEngineMapper.selectGlueFinishList(scheduleDate, mixArea)
					.stream().collect(Collectors.toMap(GlueFinish::getOrderNo, Function.identity(), (f1, f2) -> f2));
			GlueFinish glueFinish = finishMap.get(orderNo);
			if (glueFinish == null || glueFinish.getTotalFinishQty().compareTo(BigDecimal.ZERO) == 0) {
				// 未执行工单，生成新的工单号，将原工单的胶料名称、各班次计划量等信息全部转到新机台，原工单的计划量均修改为0
				oldSchedule.setMidPlanQty(0D);
				oldSchedule.setNightPlanQty(0D);
				oldSchedule.setDayPlanQty(0D);
				oldSchedule.setTotalPlanQty(0D);
				// 计划量为0，则生产顺序同样要清空
				oldSchedule.setMidProduceOrder(null);
				oldSchedule.setNightProduceOrder(null);
				oldSchedule.setDayProduceOrder(null);
			} else {
				// 已执行工单，原工单已完成的量保留在原机台，计划量=完成量；剩余计划量作为新的工单转到新机台生产，新工单的计划量=原工单计划量-原工单完成量
				BigDecimal midPlanQty = BigDecimalUtil.valueOfZero(oldScheduleBackup.getMidPlanQty())
						.subtract(glueFinish.getMidFinishQty());
				BigDecimal nightPlanQty = BigDecimalUtil.valueOfZero(oldScheduleBackup.getNightPlanQty())
						.subtract(glueFinish.getNightFinishQty());
				BigDecimal dayPlanQty = BigDecimalUtil.valueOfZero(oldScheduleBackup.getDayPlanQty())
						.subtract(glueFinish.getDayFinishQty());

				// 更新新记录各班计划量
				scheduleResultVo.setMidPlanQty(BigDecimalUtil.greatest(midPlanQty, BigDecimal.ZERO).doubleValue());
				scheduleResultVo.setNightPlanQty(BigDecimalUtil.greatest(nightPlanQty, BigDecimal.ZERO).doubleValue());
				scheduleResultVo.setDayPlanQty(BigDecimalUtil.greatest(dayPlanQty, BigDecimal.ZERO).doubleValue());
				scheduleResultVo.setTotalPlanQty(BigDecimalUtil.add(scheduleResultVo.getMidPlanQty(),
						scheduleResultVo.getNightPlanQty(), scheduleResultVo.getDayPlanQty()));
				if (scheduleResultVo.getTotalPlanQty().compareTo(0D) == 0) {
					throw new RuntimeException("计划量已全部完成生产，无需继续转机台！");
				}
				// 旧记录计划量更新为已完成量
				oldSchedule.setMidPlanQty(glueFinish.getMidFinishQty().doubleValue());
				oldSchedule.setNightPlanQty(glueFinish.getNightFinishQty().doubleValue());
				oldSchedule.setDayPlanQty(glueFinish.getDayFinishQty().doubleValue());
				oldSchedule.setTotalPlanQty(BigDecimalUtil.add(oldSchedule.getMidPlanQty(),
						oldSchedule.getNightPlanQty(), oldSchedule.getDayPlanQty()));
				// 计划量为0，则生产顺序同样要清空
				oldSchedule.setMidProduceOrder(
						glueFinish.getMidFinishQty().doubleValue() > 0 ? oldSchedule.getMidProduceOrder() : null);
				oldSchedule.setNightProduceOrder(
						glueFinish.getNightFinishQty().doubleValue() > 0 ? oldSchedule.getNightProduceOrder() : null);
				oldSchedule.setDayProduceOrder(
						glueFinish.getDayFinishQty().doubleValue() > 0 ? oldSchedule.getDayProduceOrder() : null);
			}

			scheduleResultVo.setMidProduceOrder(
					this.getProduceOrder(scheduleResultVo.getMidPlanQty(), scheduleResultVo.getMidProduceOrder()));
			scheduleResultVo.setNightProduceOrder(
					this.getProduceOrder(scheduleResultVo.getNightPlanQty(), scheduleResultVo.getNightProduceOrder()));
			scheduleResultVo.setDayProduceOrder(
					this.getProduceOrder(scheduleResultVo.getDayPlanQty(), scheduleResultVo.getDayProduceOrder()));

			String machineName = machineEngineService.mapMixMachineName(mixArea).getOrDefault(machineCode, machineCode);
			String recipeTypeName = scheduleResultVo.getRecipeTypeName();

			MesPmtRecipeVo recipe = mesPmtRecipeMap.get(CombinedMapKey.createKey(glueCode, machineCode, recipeType));
			if (recipe == null) {
				throw new RuntimeException(glueCode + "、" + machineName + "、" + recipeTypeName + "没有找到配方信息！"); // 如果没有符合条件的配方需要报错提醒
			} else if (recipe.getMajorType() == null) {
				throw new RuntimeException(glueCode + "没有对应的物料信息！");
			}

			List<GlueScheduleResultVo> machineScheduleList = allScheduleList.stream()
					// 过滤出旧机台的所有排程
					.filter(allSchedule -> machineCode.equals(allSchedule.getMachineCode())
							&& allSchedule != oldSchedule)
					.collect(Collectors.toList());

			// 把配方信息复制到排程记录中
			glueScheduleEngineBaseService.copyRecipeProperties(scheduleResultVo, recipe);
			if (isNoRequire) {
				// 如果是未提报，则需要将配方信息、计划量设定好
				// 取最后一班最后一个计划
				Integer shiftClass = GlueEngineConstants.SHIFT_CLASS_DAY;
				this.setProductOrderLatest(scheduleResultVo, shiftClass, machineScheduleList);
				// 直接将所有剩余量排到计划量中
				scheduleResultVo.setTotalPlanQty(scheduleResultVo.getTotalSurplus());
				scheduleResultVo.setDayPlanQty(scheduleResultVo.getTotalSurplus());
				scheduleResultVo.setTotalSurplus(0D);
			} else if (isUpdateProduceOrder) {
				// 正常转机台，按前端的排产顺序，需要判断是否有重复的顺序
				this.checkChangeMachineOrderRepeat(scheduleResultVo, machineScheduleList);
			} else {
				// 正常转机台，且不按前端的排产顺序，则需要排到每一个班最后一个计划之后
				Integer shiftClass;
				if (scheduleResultVo.getMidPlanQty() != null && scheduleResultVo.getMidPlanQty() > 0) {
					shiftClass = GlueEngineConstants.SHIFT_CLASS_MID;
					this.setProductOrderLatest(scheduleResultVo, shiftClass, machineScheduleList);
				}
				if (scheduleResultVo.getNightPlanQty() != null && scheduleResultVo.getNightPlanQty() > 0) {
					shiftClass = GlueEngineConstants.SHIFT_CLASS_NIGHT;
					this.setProductOrderLatest(scheduleResultVo, shiftClass, machineScheduleList);
				}
				if (scheduleResultVo.getDayPlanQty() != null && scheduleResultVo.getDayPlanQty() > 0) {
					shiftClass = GlueEngineConstants.SHIFT_CLASS_DAY;
					this.setProductOrderLatest(scheduleResultVo, shiftClass, machineScheduleList);
				}
			}

			// 发布信息
			scheduleResultVo.setPublishSuccessCount(oldSchedule.getPublishSuccessCount());
			scheduleResultVo.setNewestPublishTime(oldSchedule.getNewestPublishTime());
			if (oldSchedule.getPublishSuccessCount() != null && oldSchedule.getPublishSuccessCount() > 0) {
				oldSchedule.setReleaseStatus(ZltConstant.WAIT_RELEASING);
				scheduleResultVo.setReleaseStatus(ZltConstant.WAIT_RELEASING);
			} else {
				oldSchedule.setReleaseStatus(ZltConstant.NO_RELEASE);
				scheduleResultVo.setReleaseStatus(ZltConstant.NO_RELEASE);
			}

			// 清除掉新记录的ID，生成新工单号，但是未提报记录转机台不需要处理
			if (!isNoRequire) {
				scheduleResultVo.setId(0L);
				scheduleResultVo.setBaseValue(null);
				scheduleResultVo.setOrderNo(glueScheduleEngineBaseService.createOrderNo(batchNo));
				scheduleResultVo.setIsAddNew(true);
				scheduleResultVo.setIsFinishing(oldSchedule.getIsFinishing());
				scheduleResultVo.setSourceOrderNo(orderNo);
			}

			// 针对转入的机台排程重算时间
			List<GlueScheduleResultVo> recaculateScheduleList = this.recaculateAllExpectTimeAdd(scheduleResultVo,
					machineScheduleList, recipe, glueParams);
			this.mergeScheduleListWithIdCheck(recaculateScheduleList, updateList);

			// 如果原先是未提报胶料，不需要重算旧机台时间
			if (isNoRequire) {
				continue;
			}

			// 针对转出的机台排程重算时间
			List<GlueScheduleResultVo> oldMachineScheduleList = allScheduleList.stream()
					// 过滤出旧机台的所有排程
					.filter(allSchedule -> oldMachineCode.equals(allSchedule.getMachineCode()))
					// 排除掉预计移除的排程
					.filter(allSchedule -> !oldSchedule.getId().equals(allSchedule.getId()))
					.collect(Collectors.toList());
			// 取出旧机台的配方信息
			MesPmtRecipeVo oldRecipe = mesPmtRecipeMap
					.get(CombinedMapKey.createKey(glueCode, oldMachineCode, oldSchedule.getRecipeType()));
			// 更新旧机台的生产时间
			List<GlueScheduleResultVo> updateExpectTimeList = this.recaculateAllExpectTime(oldSchedule,
					oldScheduleBackup, oldMachineScheduleList, oldRecipe, glueParams);
			this.mergeScheduleListWithIdCheck(updateExpectTimeList, updateList);
		}

		return updateList;
	}

	/**
	 * 获取生产顺序
	 * 
	 * @param planQty
	 * @param produceOrder
	 * @return
	 */
	private Integer getProduceOrder(Double planQty, Integer produceOrder) {
		if (planQty == null || planQty.doubleValue() <= 0) {
			return null;
		}
		return produceOrder;
	}

	/**
	 * 检查转机台是否有顺序重复的记录，有重复直接返回错误提示
	 * 
	 * @param scheduleResultVo
	 * @param machineScheduleList
	 */
	private void checkChangeMachineOrderRepeat(GlueScheduleResultVo scheduleResultVo,
			List<GlueScheduleResultVo> machineScheduleList) {
		if (scheduleResultVo.getMidPlanQty() != null && scheduleResultVo.getMidPlanQty() > 0
				&& scheduleResultVo.getMidProduceOrder() != null) {
			Integer produceOrder = scheduleResultVo.getMidProduceOrder();
			if (machineScheduleList.stream().anyMatch(
					s -> s.getMidProduceOrder() != null && produceOrder.compareTo(s.getMidProduceOrder()) == 0)) {
				throw new RuntimeException("中班生产顺序重复，无法转机台！");
			}
		}
		if (scheduleResultVo.getNightPlanQty() != null && scheduleResultVo.getNightPlanQty() > 0
				&& scheduleResultVo.getNightProduceOrder() != null) {
			Integer produceOrder = scheduleResultVo.getNightProduceOrder();
			if (machineScheduleList.stream().anyMatch(
					s -> s.getNightProduceOrder() != null && produceOrder.compareTo(s.getNightProduceOrder()) == 0)) {
				throw new RuntimeException("夜班生产顺序重复，无法转机台！");
			}
		}
		if (scheduleResultVo.getDayPlanQty() != null && scheduleResultVo.getDayPlanQty() > 0
				&& scheduleResultVo.getDayProduceOrder() != null) {
			Integer produceOrder = scheduleResultVo.getDayProduceOrder();
			if (machineScheduleList.stream().anyMatch(
					s -> s.getDayProduceOrder() != null && produceOrder.compareTo(s.getDayProduceOrder()) == 0)) {
				throw new RuntimeException("白班生产顺序重复，无法转机台！");
			}
		}
	}

	/**
	 * 级联修改子胶料排程
	 * <p/>
	 * 1、修改配方类型，可能导致新增胶料或者减少胶料，修改前后都用到的胶料不受影响<br/>
	 * 2、修改数量，所使用的胶料会受影响<br/>
	 * 3、受影响的胶料只限终炼或者母联<br/>
	 * 4、增加胶料的场景：包括新增胶料与增加计划量：<br/>
	 * 1）遍历现有计划，优先级：有机台 > 未提报；<br/>
	 * 2）能找到有机台的计划，将计划量加到有计划最晚那一班<br/>
	 * 3）只能找到未提报计划，计划量加到总剩余量中<br/>
	 * 4）都找不到，新增一个未提报计划，假话零加到总剩余量中
	 * <p/>
	 * 5、减少胶料的场景：包括移除胶料与减少计划量：<br/>
	 * 1）遍历现有计划，优先级：有机台 > 未提报；<br/>
	 * 2）能找到有机台的计划，从最晚有计划量的一班开始往前扣，扣完继续往前一班扣，扣到0为止<br/>
	 * 3）只能找到未提报计划，从总剩余量中扣减，扣到0为止
	 * <p/>
	 * 6、修改一个有机台的计划后，要递归触发其原料的计划量的修改<br/>
	 * 
	 * @param newSchedule     修改的排程记录
	 * @param glue            前端修改的胶料号
	 * @param allScheduleList 所有排程记录
	 * @param recipeMap       配方信息
	 * @param stockPool       库存信息
	 * @param modifyGlueSet   已修改胶料列表，主要用于记录已级联修改的胶料，防止配方层级有错形成闭环出现死循环
	 * @return
	 */
	@Override
	public List<GlueScheduleResultVo> cascadeUpdateChildGlueSchedule(GlueScheduleResultVo newSchedule, String glue,
			List<GlueScheduleResultVo> allScheduleList, Map<String, List<MesPmtRecipeVo>> recipeMap,
			Map<String, String> params, GlueScheduleStockPool glueStock, Set<String> modifyGlueSet) {
		List<GlueScheduleResultVo> allUpdateList = new ArrayList<>(0);
		allUpdateList.add(newSchedule);
		// 如果修改的是未提报记录，则不需要继续往下计算
		if (newSchedule.getMachineCode() == null || newSchedule.getPmtRecipe() == null) {
			return allUpdateList;
		}
		// 查找出有修改计划量的排产数据
		List<GlueScheduleResultVo> modifyPlanQtyList = this.findCascadeModifyPlanQty(newSchedule, allScheduleList,
				recipeMap);

		// 过滤掉已经级联修改过的胶料
		modifyPlanQtyList = modifyPlanQtyList.stream()
				.filter(schedule -> schedule.getGlue() != null && !modifyGlueSet.contains(schedule.getGlue()))
				.collect(Collectors.toList());

		if (!CollectionUtil.isEmpty(modifyPlanQtyList)) {
			// 把本次需要级联修改的子胶料也全部添加到已修改胶料列表中
			modifyGlueSet
					.addAll(modifyPlanQtyList.stream().map(GlueScheduleResultVo::getGlue).collect(Collectors.toSet()));
			// 更新子胶料计划
			List<GlueScheduleResultVo> cascadeUpdateList = this.cascadeUpdateModifyList(newSchedule, glue,
					modifyPlanQtyList, allScheduleList, recipeMap, params, glueStock, modifyGlueSet);
			this.mergeScheduleListWithIdCheck(cascadeUpdateList, allUpdateList);
		}

		return allUpdateList;
	}

	/**
	 * 根据计划更新列表对子胶料进行更新
	 * 
	 * @param newSchedule       修改的排程
	 * @param modifyPlanQtyList 计划更新列表
	 * @param allScheduleList   当天所有的排程记录
	 * @param params            排程参数
	 * @param glueStock         胶料库存
	 * @return 本次级联更新后会影响到的排产计划
	 */
	private List<GlueScheduleResultVo> cascadeUpdateModifyList(GlueScheduleResultVo newSchedule, String upGlue,
			List<GlueScheduleResultVo> modifyPlanQtyList, List<GlueScheduleResultVo> allScheduleList,
			Map<String, List<MesPmtRecipeVo>> recipeMap, Map<String, String> params, GlueScheduleStockPool glueStock,
			Set<String> modifyGlueSet) {
		Map<String, List<GlueScheduleResultVo>> scheduleGlueGroupingMap = allScheduleList.stream()
				.collect(Collectors.groupingBy(GlueScheduleResultVo::getGlue));
		List<GlueScheduleResultVo> allUpdateList = new ArrayList<>();
		modifyGlueSet.add(newSchedule.getGlue());
		for (GlueScheduleResultVo modifyPlan : modifyPlanQtyList) {
			String glue = modifyPlan.getGlue();
			BigDecimal modifyPlanQty = modifyPlan.getPlanQty(); // 计划修改量

			// 取出同一个胶料的所有排程结果
			List<GlueScheduleResultVo> glueScheduleList = scheduleGlueGroupingMap.get(glue);
			List<GlueScheduleResultVo> noRequireList = new ArrayList<>();
			// 按班次 + 计划量整理排程数据
			List<SingleClassGlueScheduleResultVO> classScheduleList = null;
			if (glueScheduleList != null) {
				// 未提报记录
				noRequireList = glueScheduleList.stream().filter(schedule -> schedule.getRecipeType() == null)
						.collect(Collectors.toList());
				// 待处理
				glueScheduleList = glueScheduleList.stream()
						// 过滤掉没有计划量的排产记录
						.filter(schedule -> schedule.getRecipeType() != null && schedule.getTotalPlanQty() != null)
						// 先按配方类型排序
						.sorted(Comparator.comparing(GlueScheduleResultVo::getRecipeType, Comparator.reverseOrder())
								// 再按计划量排序
								.thenComparing(Comparator.comparing(GlueScheduleResultVo::getTotalPlanQty,
										Comparator.reverseOrder())))
						.collect(Collectors.toList());
				// 按班次 + 计划量整理排程数据
				classScheduleList = this.selectSingleClassScheduleListByFinishTime(glueScheduleList);
			}

			// 本次遍历涉及需要级联调整的排程计划
			GlueScheduleResultVo modlfyScheduleResult;
			// 按不同的情况处理
			if (modifyPlanQty.compareTo(BigDecimal.ZERO) > 0) { // 加计划量
				// 级联增加胶料计划量
				modlfyScheduleResult = this.cascadeIncrementPlanQty(newSchedule, allScheduleList, params, glueStock,
						recipeMap, allUpdateList, glue, modifyPlanQty, glueScheduleList, noRequireList,
						classScheduleList);
			} else { // 减计划量
				// 级联减少胶料计划量
				modlfyScheduleResult = this.cascaDedecrementPlanQty(newSchedule, allScheduleList, params, recipeMap,
						allUpdateList, modifyPlanQty, noRequireList, classScheduleList);
			}

			// 子胶料有修改的情况，也需要级联修改其下一级胶料
			if (modlfyScheduleResult != null) {
				modlfyScheduleResult.setRemark(StringUtils
						.format(I18nUtil.getMessage("schedule.glueScheduleResult.cascade.update.remark"), upGlue)); // 备注信息调整
				if (modlfyScheduleResult.getMachineCode() != null) {
					List<GlueScheduleResultVo> updateList = this.cascadeUpdateChildGlueSchedule(modlfyScheduleResult,
							upGlue, allScheduleList, recipeMap, params, glueStock, modifyGlueSet); // 递归调用
					this.mergeScheduleListWithIdCheck(updateList, allUpdateList);
				}
			}
		}
		return allUpdateList;
	}

	/**
	 * 过滤出有排程信息的数据，并按中班 > 夜班 > 白班的生产顺序排序
	 * 
	 * @param scheduleList 排产计划
	 * @return
	 */
	private List<SingleClassGlueScheduleResultVO> selectSingleClassScheduleListByFinishTime(
			List<GlueScheduleResultVo> scheduleList) {
		int shiftClass = GlueEngineConstants.SHIFT_CLASS_MID;
		List<SingleClassGlueScheduleResultVO> classScheduleList = this
				.selectSingleClassScheduleListByFinishTime(scheduleList, shiftClass);
		shiftClass = GlueEngineConstants.SHIFT_CLASS_NIGHT;
		classScheduleList.addAll(this.selectSingleClassScheduleListByFinishTime(scheduleList, shiftClass));
		shiftClass = GlueEngineConstants.SHIFT_CLASS_DAY;
		classScheduleList.addAll(this.selectSingleClassScheduleListByFinishTime(scheduleList, shiftClass));
		return classScheduleList;
	}

	/**
	 * 级联减少胶料计划量
	 * 
	 * @param newSchedule       本次修改计划
	 * @param allScheduleList   所有已排计划
	 * @param params            排产参数设置
	 * @param allUpdateList     本次所有需要修改的计划
	 * @param modifyPlanQty     修改量
	 * @param noRequireList     未提报排程列表
	 * @param classScheduleList 按班次 + 计划量整理的排程数据
	 * @return
	 */
	private GlueScheduleResultVo cascaDedecrementPlanQty(GlueScheduleResultVo newSchedule,
			List<GlueScheduleResultVo> allScheduleList, Map<String, String> params,
			Map<String, List<MesPmtRecipeVo>> recipeMap, List<GlueScheduleResultVo> allUpdateList,
			BigDecimal modifyPlanQty, List<GlueScheduleResultVo> noRequireList,
			List<SingleClassGlueScheduleResultVO> classScheduleList) {
		GlueScheduleResultVo modlfyScheduleResult = null;
		List<GlueScheduleResultVo> updateList = new ArrayList<>();
		if (!CollectionUtil.isEmpty(classScheduleList)) { // 如果修改前已经有这个胶料的排程，则从最后一个有计划的班别开始扣减量，不够扣则往上一个班别扣
			BigDecimal surplusPlanQty = modifyPlanQty; // 修改值：减量，因此是负数
			for (int i = classScheduleList.size() - 1; i >= 0; i--) {
				if (surplusPlanQty.compareTo(BigDecimal.ZERO) == 0) {
					break;
				}
				SingleClassGlueScheduleResultVO classSchedule = classScheduleList.get(i);
				GlueScheduleResultVo oldScheduleResult = classSchedule.getScheduleResult();
				if (!this.fitScheduleRecipe(oldScheduleResult, recipeMap)) {
					return null;
				}
				// 复制一份排程记录
				modlfyScheduleResult = new GlueScheduleResultVo();
				BeanUtils.copyProperties(oldScheduleResult, modlfyScheduleResult);
				String machineCode = modlfyScheduleResult.getMachineCode();
				BigDecimal newPlanQty;
				if (classSchedule.getPlanQty().compareTo(surplusPlanQty.abs()) > 0) {
					newPlanQty = classSchedule.getPlanQty().add(surplusPlanQty);
					surplusPlanQty = BigDecimal.ZERO;
				} else {
					newPlanQty = BigDecimal.ZERO;
					surplusPlanQty = classSchedule.getPlanQty().add(surplusPlanQty);
				}
				classSchedule.setPlanQty(newPlanQty);
				classSchedule.setScheduleResult(modlfyScheduleResult);
				this.updatePlanQty(classSchedule);
				// 取出对应机台的所有排程记录
				List<GlueScheduleResultVo> childMachineScheduleList = allScheduleList.stream()
						.filter(schedule -> newSchedule.getMachineCode().equals(machineCode)
								&& !Objects.equals(newSchedule.getId(), schedule.getId()))
						.collect(Collectors.toList());
				// 更新数据
				updateList.addAll(this.recaculateExpectTimeModify(modlfyScheduleResult, childMachineScheduleList,
						modlfyScheduleResult.getPmtRecipe(), params, classSchedule.getShiftClass()));
				Integer publishSuccess = Optional.ofNullable(modlfyScheduleResult.getPublishSuccessCount()).orElse(0);
				if (publishSuccess > 0) {
					modlfyScheduleResult.setReleaseStatus(ZltConstant.WAIT_RELEASING);
				} else {
					modlfyScheduleResult.setReleaseStatus(ZltConstant.NO_RELEASE);
				}
			}
		} else if (!CollectionUtil.isEmpty(noRequireList)) { // 如果修改前只有未提报排程，则在未提报的总剩余中扣减
			modlfyScheduleResult = CollectionUtil.firstElement(noRequireList);
			Double oldSurplus = modlfyScheduleResult.getTotalSurplus(); // 原剩余量
			Double newSurplus;
			if (modifyPlanQty.abs().doubleValue() < oldSurplus) {
				newSurplus = BigDecimalUtil.add(oldSurplus, modifyPlanQty.doubleValue());
			} else {
				newSurplus = 0D;
			}
			modlfyScheduleResult.setTotalSurplus(newSurplus);
			// 更新数据
			updateList.add(modlfyScheduleResult);
		}
		this.mergeScheduleListWithIdCheck(updateList, allUpdateList);
		this.addScheduleListWithIdCheck(modlfyScheduleResult, allUpdateList);
		return modlfyScheduleResult;
	}

	/**
	 * 级联增加胶料计划量
	 * 
	 * @param newSchedule       本次修改计划
	 * @param allScheduleList   所有已排计划
	 * @param params            排产参数设置
	 * @param glueStock         库存
	 * @param allUpdateList     本次所有需要修改的计划
	 * @param glue              本次级联修改的胶料号
	 * @param modifyPlanQty     修改量
	 * @param glueScheduleList  同一胶料的所有排程计划
	 * @param noRequireList     未提报排程列表
	 * @param classScheduleList 按班次 + 计划量整理的排程数据
	 * @return
	 */
	private GlueScheduleResultVo cascadeIncrementPlanQty(GlueScheduleResultVo newSchedule,
			List<GlueScheduleResultVo> allScheduleList, Map<String, String> params, GlueScheduleStockPool glueStock,
			Map<String, List<MesPmtRecipeVo>> recipeMap, List<GlueScheduleResultVo> allUpdateList, String glue,
			BigDecimal modifyPlanQty, List<GlueScheduleResultVo> glueScheduleList,
			List<GlueScheduleResultVo> noRequireList, List<SingleClassGlueScheduleResultVO> classScheduleList) {
		GlueScheduleResultVo modlfyScheduleResult = null;
		String batchNo = newSchedule.getBatchNo();
		Date scheduleDate = newSchedule.getScheduleDate();
		String mixArea = newSchedule.getMixArea();
		String decomposeBatchNo = newSchedule.getDecomposeBatchNo();

		List<GlueScheduleResultVo> updateList = new ArrayList<>();
		// 根据不同条件处理排程记录表
		if (!CollectionUtil.isEmpty(classScheduleList)) { // 如果修改前已经有这个胶料的排程，则直接加到最后一个有计划的班别
			SingleClassGlueScheduleResultVO latestSchedule = classScheduleList.get(classScheduleList.size() - 1);
			GlueScheduleResultVo oldScheduleResult = latestSchedule.getScheduleResult();
			if (!this.fitScheduleRecipe(oldScheduleResult, recipeMap)) {
				return null;
			}
			// 复制一份排程记录
			modlfyScheduleResult = new GlueScheduleResultVo();
			BeanUtils.copyProperties(oldScheduleResult, modlfyScheduleResult);
			String machineCode = oldScheduleResult.getMachineCode();
			latestSchedule.setPlanQty(latestSchedule.getPlanQty().add(modifyPlanQty));
			latestSchedule.setScheduleResult(modlfyScheduleResult);
			this.updatePlanQty(latestSchedule);
			// 取出对应机台的所有排程记录
			List<GlueScheduleResultVo> childMachineScheduleList = allScheduleList.stream()
					.filter(schedule -> newSchedule.getMachineCode().equals(machineCode)
							&& !Objects.equals(newSchedule.getId(), schedule.getId()))
					.collect(Collectors.toList());
			// 更新数据
			updateList = this.recaculateExpectTimeModify(modlfyScheduleResult, childMachineScheduleList,
					modlfyScheduleResult.getPmtRecipe(), params, latestSchedule.getShiftClass());
			Integer publishSuccess = Optional.ofNullable(modlfyScheduleResult.getPublishSuccessCount()).orElse(0);
			if (publishSuccess > 0) {
				modlfyScheduleResult.setReleaseStatus(ZltConstant.WAIT_RELEASING);
			} else {
				modlfyScheduleResult.setReleaseStatus(ZltConstant.NO_RELEASE);
			}
		} else if (!CollectionUtil.isEmpty(glueScheduleList)) { // 如果修改前计划量为0，则加到第一班该机台的末尾
			GlueScheduleResultVo oldScheduleResult = CollectionUtil.firstElement(glueScheduleList);
			if (!this.fitScheduleRecipe(oldScheduleResult, recipeMap)) {
				return null;
			}
			// 复制一份排程记录
			modlfyScheduleResult = new GlueScheduleResultVo();
			BeanUtils.copyProperties(oldScheduleResult, modlfyScheduleResult);
			modlfyScheduleResult.setMidPlanQty(modifyPlanQty.doubleValue());
			modlfyScheduleResult.setMidProduceOrder(10);
			modlfyScheduleResult.setTotalPlanQty(modifyPlanQty.doubleValue());
			String machineCode = modlfyScheduleResult.getMachineCode();
			// 取出对应机台的所有排程记录
			List<GlueScheduleResultVo> childMachineScheduleList = allScheduleList.stream()
					.filter(schedule -> newSchedule.getMachineCode().equals(machineCode)
							&& !Objects.equals(newSchedule.getId(), schedule.getId()))
					.collect(Collectors.toList());
			// 更新数据
			updateList = this.recaculateExpectTimeModify(modlfyScheduleResult, childMachineScheduleList,
					modlfyScheduleResult.getPmtRecipe(), params, GlueEngineConstants.SHIFT_CLASS_MID);
			updateList.add(modlfyScheduleResult);
			Integer publishSuccess = Optional.ofNullable(modlfyScheduleResult.getPublishSuccessCount()).orElse(0);
			if (publishSuccess > 0) {
				modlfyScheduleResult.setReleaseStatus(ZltConstant.WAIT_RELEASING);
			} else {
				modlfyScheduleResult.setReleaseStatus(ZltConstant.NO_RELEASE);
			}
		} else if (!CollectionUtil.isEmpty(noRequireList)) { // 如果修改前没有这个胶料的排程，则添加到未提报的总剩余中
			modlfyScheduleResult = CollectionUtil.firstElement(noRequireList);
			// 复制一份排程记录
			modlfyScheduleResult.setTotalSurplus(
					modifyPlanQty.add(new BigDecimal(modlfyScheduleResult.getTotalSurplus().toString())).doubleValue());
			// 更新数据
			updateList.add(modlfyScheduleResult);
		} else { // 如果连未提报计划都没有，则新增一个未提报计划，计划量添加到总剩余中
			modlfyScheduleResult = this.buildNoRequireSchedule(scheduleDate, mixArea, glue, batchNo, decomposeBatchNo,
					modifyPlanQty, glueStock);
			// 新增的未提报胶料需要加到列表中
			noRequireList.add(modlfyScheduleResult);
			allScheduleList.add(modlfyScheduleResult);
			updateList.add(modlfyScheduleResult);
		}
		this.mergeScheduleListWithIdCheck(updateList, allUpdateList);
		this.addScheduleListWithIdCheck(modlfyScheduleResult, allUpdateList);
		return modlfyScheduleResult;
	}

	/**
	 * 填充排产计划的配方
	 * 
	 * @param scheduleResult 排产计划
	 * @param recipeMap      配方列表
	 * @return
	 */
	private boolean fitScheduleRecipe(GlueScheduleResultVo scheduleResult,
			Map<String, List<MesPmtRecipeVo>> recipeMap) {
		String guleCode = scheduleResult.getGlue();
		String machineCode = scheduleResult.getMachineCode();
		String recipeType = scheduleResult.getRecipeType();
		if (scheduleResult.getPmtRecipe() == null && recipeMap.containsKey(guleCode)) {
			MesPmtRecipeVo recipe = recipeMap.get(guleCode).stream()
					.filter(r -> r.getRecipeEquipCode().equals(machineCode) && r.getRecipeType().equals(recipeType))
					.findAny().orElse(null);
			scheduleResult.setPmtRecipe(recipe);
			return recipe != null;
		} else {
			return scheduleResult.getPmtRecipe() != null;
		}
	}

	/**
	 * 构建未提报排程记录
	 * 
	 * @param scheduleDate     排程日期
	 * @param mixArea          密炼区
	 * @param glue             胶料号
	 * @param batchNo          批次号
	 * @param decomposeBatchNo 分解批次号
	 * @param planQty          新增的计划量
	 * @param glueStock        库存
	 * @return
	 */
	private GlueScheduleResultVo buildNoRequireSchedule(Date scheduleDate, String mixArea, String glue, String batchNo,
			String decomposeBatchNo, BigDecimal planQty, GlueScheduleStockPool glueStock) {
		GlueScheduleResultVo scheduleResult = new GlueScheduleResultVo();
		// 生成的记录临时给一个唯一标识（负数）用于区分，每新增一个ID减1
		scheduleResult.setId(glueStock.nextId());
		scheduleResult.setTotalSurplus(0D);
		scheduleResult.setReleaseStatus(ZltConstant.NO_RELEASE);
		scheduleResult.setDataSource(ZltConstant.GLUE_SCHEDULE_SOURCE_CASCADE);
		scheduleResult.setProductedQty(BigDecimal.ZERO);
		scheduleResult.setStartShiftClass(GlueEngineConstants.SHIFT_CLASS_MID); // 可开始班次默认中班
		scheduleResult.setPublishSuccessCount(0);
		scheduleResult.setMidPlanQty(0D);
		scheduleResult.setNightPlanQty(0D);
		scheduleResult.setDayPlanQty(0D);
		scheduleResult.setBaseValue(null);
		scheduleResult.setGlue(glue);
		scheduleResult.setRequireQty(planQty.doubleValue());
		scheduleResult.setTotalPlanQty(0D);
		scheduleResult.setTotalSurplus(planQty.doubleValue());
		scheduleResult.setBatchNo(batchNo);
		scheduleResult.setOrderNo(glueScheduleEngineBaseService.createOrderNo(batchNo)); // 工单号
		scheduleResult.setScheduleDate(scheduleDate);
		scheduleResult.setMixArea(mixArea);
		scheduleResult.setPlanQty(BigDecimal.ZERO);
		scheduleResult.setStockQty(glueStock.getQualifiedGlueStockNum(glue).doubleValue());
		scheduleResult.setSafeStockQty(glueStock.getSafeStock(glue).doubleValue());
		scheduleResult.setIsFinishing("0");
		scheduleResult.setDecomposeBatchNo(decomposeBatchNo);
		scheduleResult.setIsAddNew(true);
		return scheduleResult;
	}

	/**
	 * 查找出修改了计划量会联动修改到的胶料数据
	 * 
	 * @param newSchedule     本次修改的排产计划
	 * @param allScheduleList 本日本密炼区完成排产计划
	 * @param recipeMap       本密炼区完整配方数据
	 * @return
	 */
	private List<GlueScheduleResultVo> findCascadeModifyPlanQty(GlueScheduleResultVo newSchedule,
			List<GlueScheduleResultVo> allScheduleList, Map<String, List<MesPmtRecipeVo>> recipeMap) {
		String upGlueCode = newSchedule.getGlue();
		MesPmtRecipeVo newRecipe = newSchedule.getPmtRecipe();
		List<MesPmtRecipeWeightVo> newWeightList = newRecipe.getRecipeWeightList();
		if (newWeightList != null) {
			BigDecimal maxSetWeight = RecipeUtil.getMaxSetWeight(newWeightList); // 获取称重配方中最大的终炼母炼胶重量
			newWeightList = newWeightList.stream()
					// 过滤掉掺胶、小料等称重信息
					.filter(weight -> {
						String glueCode = weight.getRecipeMaterialName();
						String majorType = weight.getMajorType();
						BigDecimal setWeight = BigDecimalUtil.valueOfZero(weight.getSetWeight());
						return GlueEngineConstants.SCHEDULE_MAJOR_TYPE
								.contains(RecipeUtil.getMajorType(glueCode, majorType, setWeight, maxSetWeight));
					}).collect(Collectors.toList());
		} else {
			newWeightList = new ArrayList<>(0);
		}

		BigDecimal newLotTotalWeight = new BigDecimal(newRecipe.getLotTotalWeight().toString());
		Double newTotalPlanQty = newSchedule.getTotalPlanQty();

		// 判断记录是否通过转机台生成
		boolean isChangeMachine = StringUtils.isNotEmpty(newSchedule.getSourceOrderNo());
		GlueScheduleResultVo oldSchedule = null; // 原排程记录
		if (newSchedule.getId() != null && !isChangeMachine) {
			oldSchedule = allScheduleList.stream().filter(schedule -> newSchedule.getId().equals(schedule.getId()))
					.findAny().orElse(null);
		}
		// 如果是转机台记录，则取来源排程记录作为原排程记录
		if (isChangeMachine) {
			oldSchedule = allScheduleList.stream()
					.filter(schedule -> newSchedule.getSourceOrderNo().equals(schedule.getOrderNo())).findAny()
					.orElse(null);
		}
		// 是否新增或修改
		boolean isAddNew = oldSchedule == null;
		if (!isAddNew && oldSchedule.getTotalPlanQty() == null) {
			// 防止旧记录的总计划量为空
			oldSchedule.setTotalPlanQty(BigDecimalUtil.add(oldSchedule.getMidPlanQty(), oldSchedule.getNightPlanQty(),
					oldSchedule.getDayPlanQty()));
		}
		boolean isModifyPlanQty = isAddNew ? false
				: newSchedule.getTotalPlanQty().compareTo(oldSchedule.getTotalPlanQty()) != 0;
		boolean isModifyRecipe = !isAddNew && (!Objects.equals(newRecipe.getRecipeType(), oldSchedule.getRecipeType()) // 修改了配方类型
				|| !Objects.equals(newRecipe.getRecipeVersionId(), oldSchedule.getRecipeVersionId()) // 修改了配方版本
				|| !Objects.equals(newRecipe.getRecipeEquipCode(), oldSchedule.getMachineCode()) // 修改了机台
				|| !Objects.equals(newRecipe.getProductStage(), oldSchedule.getRecipeStage())); // 修改了配方阶段

		// 如果是转机台新增的记录，则不走新增分支，直接确定走修改配方分支
		isAddNew = isChangeMachine ? false : isAddNew;
		isModifyRecipe = isChangeMachine ? true : isModifyRecipe;

		if (!isAddNew && !isModifyRecipe && !isModifyPlanQty) {
			return new ArrayList<>();
		}

		Map<String, List<GlueScheduleResultVo>> scheduleGlueGroupingMap = allScheduleList.stream()
				.filter(schedule -> schedule.getMachineCode() != null)
				.collect(Collectors.groupingBy(GlueScheduleResultVo::getGlue));

		// 需要修改计划量的胶料信息
		List<GlueScheduleResultVo> modifyPlanQtyList = new ArrayList<>();
		// 新增
		if (isAddNew) {
			if (!newSchedule.getTotalPlanQty().equals(0D)) {
				for (MesPmtRecipeWeightVo weight : newWeightList) {
					String weightGlueCode = weight.getRecipeMaterialName();
					// 根据父胶料计算子胶料的计划量
					BigDecimal weightTotalPlanQty = this.getMlGluePlanQty(weightGlueCode, newTotalPlanQty,
							newLotTotalWeight.doubleValue(), recipeMap);
					GlueScheduleResultVo result = new GlueScheduleResultVo();
					result.setGlue(weightGlueCode);
					result.setPlanQty(weightTotalPlanQty);
					modifyPlanQtyList.add(result);
				}
			}
		} else { // 修改配方或者计划量，或者两者都有修改
			// 计算计划量差值
			// 如果是转机台，相当于有部分计划量的配方类型变了，因此只需要计算这部分计划量配方类型修改后引起的级联更新
			Double oldTotalPlanQty = isChangeMachine ? newTotalPlanQty : oldSchedule.getTotalPlanQty();
			BigDecimal diffentPlanQty = new BigDecimal(newTotalPlanQty.toString())
					.subtract(new BigDecimal(oldTotalPlanQty.toString()));

			// 取出旧记录的配方称重信息
			MesPmtRecipeVo oldRecipe = null;
			Double oldLotTotalWeight = null;
			Map<String, MesPmtRecipeWeightVo> oldWeightMap = new HashMap<>(0);
			if (this.fitScheduleRecipe(oldSchedule, recipeMap)) {
				oldRecipe = oldSchedule.getPmtRecipe();
				oldLotTotalWeight = oldRecipe.getLotTotalWeight();
				List<MesPmtRecipeWeightVo> oldWeightList = oldRecipe.getRecipeWeightList();
				if (oldWeightList != null) {
					BigDecimal maxSetWeight = RecipeUtil.getMaxSetWeight(newWeightList); // 获取称重配方中最大的终炼母炼胶重量
					oldWeightMap = oldWeightList.stream()
							// 过滤掉掺胶、小料等称重信息
							.filter(weight -> {
								String glueCode = weight.getRecipeMaterialName();
								String majorType = weight.getMajorType();
								BigDecimal setWeight = BigDecimalUtil.valueOfZero(weight.getSetWeight());
								return GlueEngineConstants.SCHEDULE_MAJOR_TYPE.contains(
										RecipeUtil.getMajorType(glueCode, majorType, setWeight, maxSetWeight));
							}).collect(Collectors.toMap(MesPmtRecipeWeightVo::getRecipeMaterialName,
									Function.identity(), (w1, w2) -> w1));
				}
			}

			for (MesPmtRecipeWeightVo weight : newWeightList) {
				String weightGlueCode = weight.getRecipeMaterialName();
				// 移除掉胶料号能匹配上的称重信息
				oldWeightMap.remove(weightGlueCode);
				// 如果原来没有这个胶料的排产，说明是新增的
				boolean isAddNewWeight = scheduleGlueGroupingMap.get(weightGlueCode) == null;

				// 根据上级胶料计算母胶料的计划量
				BigDecimal weightTotalPlanQty = BigDecimal.ZERO;
				if (isAddNewWeight) { // 新增的母胶料直接用母胶料计算
					weightTotalPlanQty = this.getMlGluePlanQty(weightGlueCode, newTotalPlanQty,
							newLotTotalWeight.doubleValue(), recipeMap);
				} else { // 修改量的母胶料需要用计划量差值进行计算
					// 查找配方
					MesPmtRecipeVo weightRecipe = this.findRecipe(weightGlueCode, recipeMap, scheduleGlueGroupingMap);
					// 计算母胶的计划修改量
					if (weightRecipe != null) { // 能取到母胶配方
						// 取出单车总重
						BigDecimal weightLotTotalWeight = new BigDecimal(weightRecipe.getLotTotalWeight().toString());
						if (diffentPlanQty.compareTo(BigDecimal.ZERO) != 0) { // 上级胶料计划量有修改，直接按修改量计算
							// 母胶料的计划量 = 上级胶料计划量修改量 * 上级胶料配方重量 / 母胶料配方重量
							weightTotalPlanQty = diffentPlanQty.multiply(newLotTotalWeight).divide(weightLotTotalWeight,
									0, RoundingMode.UP);
						} else { // 如果上级胶料没有修改量，即只有配方变更，则计算修改前修改后的差值
							BigDecimal oldWeightPlanQty = new BigDecimal(oldTotalPlanQty.toString())
									.multiply(new BigDecimal(oldLotTotalWeight.toString()))
									.divide(weightLotTotalWeight, 0, RoundingMode.UP);
							BigDecimal newWeightPlanQty = new BigDecimal(oldTotalPlanQty.toString())
									.multiply(newLotTotalWeight).divide(weightLotTotalWeight, 0, RoundingMode.UP);
							weightTotalPlanQty = newWeightPlanQty.subtract(oldWeightPlanQty);
						}
					}
				}
				// 如果计划修改量为0，则不处理
				if (weightTotalPlanQty.compareTo(BigDecimal.ZERO) == 0) {
					continue;
				}
				GlueScheduleResultVo result = new GlueScheduleResultVo();
				result.setGlue(weightGlueCode);
				result.setPlanQty(weightTotalPlanQty);
				modifyPlanQtyList.add(result);
			}
			// 剩余的称重信息就是原来的配方有的胶料，但是新配方没有，需要添加配方数量减少的
			for (Entry<String, MesPmtRecipeWeightVo> entry : oldWeightMap.entrySet()) {
				String glueCode = entry.getKey();
				GlueScheduleResultVo result = new GlueScheduleResultVo();
				// 根据上级胶料计算母胶料的计划量
				BigDecimal weightTotalPlanQty = this.getMlGluePlanQty(glueCode, newTotalPlanQty,
						newLotTotalWeight.doubleValue(), recipeMap);
				// 如果计划修改量为0，则不处理
				if (weightTotalPlanQty.compareTo(BigDecimal.ZERO) == 0) {
					continue;
				}
				result.setGlue(glueCode);
				result.setPlanQty(weightTotalPlanQty.negate()); // 直接取负数
				modifyPlanQtyList.add(result);
			}

			// 完成比较后，将新数据刷到旧排程计划对象中
			if (!isChangeMachine) {// 转机台记录不需要刷
				this.copyModifyScheduleResult(newSchedule, oldSchedule);
			}
		}
		return modifyPlanQtyList;
	}

	/**
	 * 查找配方，如果现有排程列表中已有该胶料，则优先选择这个配方
	 * 
	 * @param glueCode                胶料号
	 * @param recipeMap               配方列表
	 * @param scheduleGlueGroupingMap 已有排程记录
	 * @return
	 */
	private MesPmtRecipeVo findRecipe(String glueCode, Map<String, List<MesPmtRecipeVo>> recipeMap,
			Map<String, List<GlueScheduleResultVo>> scheduleGlueGroupingMap) {
		// 通过胶料取出已有的排程记录
		List<GlueScheduleResultVo> scheduleList = scheduleGlueGroupingMap.get(glueCode);
		if (!CollectionUtil.isEmpty(scheduleList)) {
			for (GlueScheduleResultVo weightScheduele : scheduleList) {
				this.fitScheduleRecipe(weightScheduele, recipeMap);
			}
			// 取出已有的母胶配方
			GlueScheduleResultVo oldWeightSchedule = scheduleList.stream().filter(
					schedule -> schedule.getPmtRecipe() != null && schedule.getPmtRecipe().getLotTotalWeight() != null)
					// 如果有多个，则取单车总重最小的一个
					.min((v1, v2) -> v1.getPmtRecipe().getLotTotalWeight()
							.compareTo(v2.getPmtRecipe().getLotTotalWeight()))
					.orElse(null);
			if (oldWeightSchedule != null) {
				return oldWeightSchedule.getPmtRecipe();
			}
		}

		// 如果没有找到，则直接取单车总重最小的一个配方
		return recipeMap.get(glueCode).stream().filter(r -> r.getLotTotalWeight() != null)
				.min(Comparator.comparing(MesPmtRecipeVo::getLotTotalWeight)).orElse(null);
	}

	/**
	 * 将源排程的排产与配方部分数据复制到目标排程记录中
	 * 
	 * @param sourceSchedule 源排程
	 * @param targetSchedule 目标排程
	 */
	private void copyModifyScheduleResult(GlueScheduleResultVo sourceSchedule, GlueScheduleResultVo targetSchedule) {
		targetSchedule.setBaseValue(targetSchedule.getId());
		targetSchedule.setMidProduceOrder(sourceSchedule.getMidProduceOrder());
		targetSchedule.setMidPlanQty(sourceSchedule.getMidPlanQty());
		targetSchedule.setMidExpectStartTime(sourceSchedule.getMidExpectStartTime());
		targetSchedule.setMidExpectFinishTime(sourceSchedule.getMidExpectFinishTime());
		targetSchedule.setNightProduceOrder(sourceSchedule.getNightProduceOrder());
		targetSchedule.setNightPlanQty(sourceSchedule.getNightPlanQty());
		targetSchedule.setNightExpectStartTime(sourceSchedule.getNightExpectStartTime());
		targetSchedule.setNightExpectFinishTime(sourceSchedule.getNightExpectFinishTime());
		targetSchedule.setDayProduceOrder(sourceSchedule.getDayProduceOrder());
		targetSchedule.setDayPlanQty(sourceSchedule.getDayPlanQty());
		targetSchedule.setDayExpectStartTime(sourceSchedule.getDayExpectStartTime());
		targetSchedule.setDayExpectFinishTime(sourceSchedule.getDayExpectFinishTime());
		targetSchedule.setTotalPlanQty(sourceSchedule.getTotalPlanQty());
		targetSchedule.setMachineCode(sourceSchedule.getMachineCode());
		targetSchedule.setReleaseStatus(sourceSchedule.getReleaseStatus());
		targetSchedule.setRemark(sourceSchedule.getRemark());
		glueScheduleEngineBaseService.copyRecipeProperties(targetSchedule, sourceSchedule.getPmtRecipe());
	}

	/**
	 * 过滤出单班有排程信息的数据，并按生产顺序排序
	 * 
	 * @param scheduleList 排产记录列表
	 * @param shiftClass   班次
	 * @return
	 */
	private List<SingleClassGlueScheduleResultVO> selectSingleClassScheduleListByFinishTime(
			List<GlueScheduleResultVo> scheduleList, int shiftClass) {
		// 该班所有排产计划
		List<SingleClassGlueScheduleResultVO> scheduleResultList = scheduleList.stream()
				// 抽取单班排产数据
				.map(schedule -> this.extractSingleClassSchedule(schedule, shiftClass))
				// 过滤掉在该班次排产信息不完整的计划
				.filter(classSchedule -> {
					if (classSchedule.getPlanQty() == null
							|| classSchedule.getPlanQty().compareTo(BigDecimal.ZERO) == 0) {
						return false;
					}
					if (classSchedule.getProduceOrder() == null) {
						return false;
					}
					if (classSchedule.getPlanQty() == null) {
						return false;
					}
					if (classSchedule.getExpectFinishTime() == null) {
						return false;
					}
					return true;
				})
				// 按生产顺序顺序排序
				.sorted((schedule1, schedule2) -> {
					Date finishTime1 = schedule1.getExpectFinishTime();
					Date finishTime2 = schedule2.getExpectFinishTime();
					return ObjectUtils.compare(finishTime1, finishTime2, true);
				}).collect(Collectors.toList());
		return scheduleResultList;
	}

	/**
	 * 根据父胶料计算母胶料的计划量
	 * 
	 * @param glueCode             母胶料编号
	 * @param upGluePlanQty        上级胶料的计划量
	 * @param upGlueLotTotalWeight 上级胶料的配方重量
	 * @param recipeMap            配方列表
	 * @return
	 */
	private BigDecimal getMlGluePlanQty(String glueCode, Double upGluePlanQty, Double upGlueLotTotalWeight,
			Map<String, List<MesPmtRecipeVo>> recipeMap) {
		// 取出母胶配方重量较小的配方，这样算出来的母胶结果会比较大
		MesPmtRecipeVo weightRecipe = recipeMap.get(glueCode).stream().filter(r -> r.getLotTotalWeight() != null)
				.min(Comparator.comparing(MesPmtRecipeVo::getLotTotalWeight)).orElse(null);
		if (weightRecipe == null) {
			return BigDecimal.ZERO;
		}
		Double weightLotTotalWeight = weightRecipe.getLotTotalWeight();
		// 母胶料的计划量 = 上级胶料计划量 * 上级胶料配方重量 / 母胶料配方重量
		return new BigDecimal(upGluePlanQty).multiply(new BigDecimal(upGlueLotTotalWeight))
				.divide(new BigDecimal(weightLotTotalWeight), 0, RoundingMode.UP);
	}

	/**
	 * 将排程顺序更新到指定班次的末尾
	 * 
	 * @param scheduleResultVo 待设置的排程记录
	 * @param shiftClass       指定班次
	 * @param allScheduleList  所有排程记录
	 */
	private void setProductOrderLatest(GlueScheduleResultVo scheduleResultVo, Integer shiftClass,
			List<GlueScheduleResultVo> allScheduleList) {
		SingleClassGlueScheduleResultVO classResult = this.extractSingleClassSchedule(scheduleResultVo, shiftClass);
		List<SingleClassGlueScheduleResultVO> matchList = this.selectSingleClassScheduleList(allScheduleList,
				shiftClass);
		Integer productOrder = 10; // 排产顺序
		if (!CollectionUtil.isEmpty(matchList)) {
			// 如果有安排计划，则相当于要插到最后一个排程后
			SingleClassGlueScheduleResultVO latestResult = matchList.get(matchList.size() - 1);
			Integer latestProductOrder = latestResult.getProduceOrder();
			productOrder += latestProductOrder;
		}
		classResult.setProduceOrder(productOrder);
		this.updateExpectTime(classResult);
	}

	/**
	 * 移除排程后重算指定班次排程的预计时间
	 * 
	 * @param scheduleResult      移除的排程
	 * @param machineScheduleList 该机台的所有排程记录
	 * @param recipe              移除排程对应的配方信息
	 * @param params              排程设置
	 * @return 有重算过预计时间的排程
	 */
	private List<GlueScheduleResultVo> recaculateAllExpectTimeRemove(GlueScheduleResultVo removeSchedule,
			List<GlueScheduleResultVo> machineScheduleList, Map<String, String> params) {
		List<GlueScheduleResultVo> updateList = new ArrayList<>();
		if (removeSchedule.getMidProduceOrder() != null) {
			// 计算中班
			if (removeSchedule.getMidPlanQty() != null && removeSchedule.getMidPlanQty() > 0) {
				List<GlueScheduleResultVo> removeScheduleList = recaculateExpectTimeRemove(removeSchedule,
						machineScheduleList, params, GlueEngineConstants.SHIFT_CLASS_MID);
				this.mergeScheduleListWithIdCheck(removeScheduleList, updateList);
			}
			// 计算晚班
			if (removeSchedule.getNightPlanQty() != null && removeSchedule.getNightPlanQty() > 0) {
				List<GlueScheduleResultVo> removeScheduleList = recaculateExpectTimeRemove(removeSchedule,
						machineScheduleList, params, GlueEngineConstants.SHIFT_CLASS_NIGHT);
				this.mergeScheduleListWithIdCheck(removeScheduleList, updateList);
			}
			// 计算白班
			if (removeSchedule.getDayPlanQty() != null && removeSchedule.getDayPlanQty() > 0) {
				List<GlueScheduleResultVo> removeScheduleList = recaculateExpectTimeRemove(removeSchedule,
						machineScheduleList, params, GlueEngineConstants.SHIFT_CLASS_DAY);
				this.mergeScheduleListWithIdCheck(removeScheduleList, updateList);
			}
		}
		return updateList;
	}

	/**
	 * 移除排程后重算指定班次排程的预计时间
	 * 
	 * @param scheduleResult      待移除的排程
	 * @param machineScheduleList 该机台的所有排程记录
	 * @param params              排程设置
	 * @param shiftClass          班次
	 * @return 已有排程里需重算预计时间的记录
	 */
	private List<GlueScheduleResultVo> recaculateExpectTimeRemove(GlueScheduleResultVo scheduleResult,
			List<GlueScheduleResultVo> machineScheduleList, Map<String, String> params, int shiftClass) {
		// 取出当前班次的排产信息
		SingleClassGlueScheduleResultVO classResult = this.extractSingleClassSchedule(scheduleResult, shiftClass);
		Date startTime = classResult.getExpectStartTime();
		Date finishTime = classResult.getExpectFinishTime();
		Integer produceOrder = classResult.getProduceOrder();
		if (startTime == null || finishTime == null || produceOrder == null) {
			return new ArrayList<>(0);
		}
		// 过滤出单班排程信息
		List<SingleClassGlueScheduleResultVO> scheduleResultList = this
				.selectSingleClassScheduleList(machineScheduleList, shiftClass);
		if (CollectionUtil.isEmpty(scheduleResultList)) {
			return new ArrayList<>(0);
		}

		List<GlueScheduleResultVo> updateList = new ArrayList<>(); // 最终需要重算时间的排程列表
		// 遍历本班该机台上已有的排程记录
		for (SingleClassGlueScheduleResultVO machineSchedule : scheduleResultList) {
			if (produceOrder < machineSchedule.getProduceOrder()) {
				// 如果需要在新增的排程之后，则加入到最终列表中，后续要重算其时间
				updateList.add(machineSchedule.getScheduleResult());
			}
		}

		BigDecimal productTime = BigDecimal.ZERO;
		// 计算需要提前的时间 = 结束时间 - 开始时间 + 排程切换时间，取结果的相反数
		String switchTime = params.getOrDefault(GlueEngineConstants.SCHEDULE_SWITCH_TIME, "0"); // 排程切换时间设置
		productTime = new BigDecimal(finishTime.getTime() - startTime.getTime()).divide(THOUSAND)
				.add(new BigDecimal(switchTime)).negate();
		if (productTime.compareTo(BigDecimal.ZERO) == 0) {
			// 如果生产时间没有差，则不需要更新后续排程的时间
			return new ArrayList<>(0);
		}

		// 遍历待修改数据，将所有记录的生产时间时间提前移除的生产时间
		for (GlueScheduleResultVo updateSchedule : updateList) {
			SingleClassGlueScheduleResultVO tempClassResult = this.extractSingleClassSchedule(updateSchedule,
					shiftClass);
			// 时间提前
			Date tempStartTime = DateUtils.addSeconds(tempClassResult.getExpectStartTime(), productTime.intValue());
			Date tempFinishTime = DateUtils.addSeconds(tempClassResult.getExpectFinishTime(), productTime.intValue());
			tempClassResult.setExpectStartTime(tempStartTime);
			tempClassResult.setExpectFinishTime(tempFinishTime);
			this.updateExpectTime(tempClassResult);
			tempClassResult.getScheduleResult().setBaseValue(tempClassResult.getScheduleResult().getId());
		}

		return updateList;
	}

	/**
	 * 新增排程后重算指定班次排程的预计时间
	 * 
	 * @param scheduleResult      新增的排程
	 * @param machineScheduleList 该机台的所有排程记录
	 * @param recipe              新增排程对应的配方信息
	 * @param params              排程设置
	 * @param shiftClass          班次
	 * @return 已有排程里需重算预计时间的记录
	 */
	private List<GlueScheduleResultVo> recaculateExpectTimeAdd(GlueScheduleResultVo scheduleResult,
			List<GlueScheduleResultVo> machineScheduleList, MesPmtRecipeVo recipe, Map<String, String> params,
			int shiftClass) {
		// 取出当前班次的排产信息
		SingleClassGlueScheduleResultVO classResult = this.extractSingleClassSchedule(scheduleResult, shiftClass);
		// 过滤出单班排程信息
		List<SingleClassGlueScheduleResultVO> scheduleResultList = this
				.selectSingleClassScheduleList(machineScheduleList, shiftClass);

		List<GlueScheduleResultVo> updateList = new ArrayList<>(); // 最终需要重算时间的排程列表
		int produceOrder = classResult.getProduceOrder();
		SingleClassGlueScheduleResultVO preScheduleResult = null; // 在插单次序的前一个排程记录
		// 遍历本班该机台上已有的排程记录
		for (SingleClassGlueScheduleResultVO machineSchedule : scheduleResultList) {
			if (produceOrder >= machineSchedule.getProduceOrder()) {
				// 如果序号在新增的排程之前，则忽略掉，并记录起来
				preScheduleResult = machineSchedule;
			} else {
				// 如果需要在新增的排程之后，则加入到最终列表中，后续要重算其时间
				updateList.add(machineSchedule.getScheduleResult());
			}
		}

		// 查询对应炼胶间隔时长（触发节点较多，基本上单记录修改触发，暂时不做批量赋值）
		BigDecimal intervalTime = mixingTimeEngineService.getIntervalTime(scheduleResult.getMixArea(), scheduleResult.getGlue(), scheduleResult.getMachineCode());
		// 计算生产时长
		BigDecimal productTime = this.caculateProductTime(classResult.getPlanQty(), recipe, params, intervalTime);
		Long switchTime = new Long(params.getOrDefault(GlueEngineConstants.SCHEDULE_SWITCH_TIME, "0")); // 排程切换时间
		Date startTime;
		if (preScheduleResult != null && preScheduleResult.getExpectFinishTime() != null) {
			// 如果在插单计划前有排程，则插单计划的开始时间 = 前一个排程的结束时间 + 排程切换时间
			startTime = DateUtils.addSeconds(preScheduleResult.getExpectFinishTime(), switchTime.intValue());
		} else {
			startTime = ShiftClassUtil.getShiftClassStartTime(scheduleResult.getScheduleDate(), shiftClass);
		}
		// 计算完成时间：开始时间 + 生产时长
		Date finishTime = DateUtils.addSeconds(startTime, (int) productTime.longValue());
		classResult.setExpectStartTime(startTime);
		classResult.setExpectFinishTime(finishTime);
		// 更新生产预计日期相关信息
		this.updateExpectTime(classResult);
		long extendTime = switchTime + productTime.longValue(); // 额外插入的时间：排程切换时间 + 生产时间

		// 遍历待修改数据，将所有记录的生产时间时间延后插单的生产时间
		for (GlueScheduleResultVo updateSchedule : updateList) {
			SingleClassGlueScheduleResultVO tempClassResult = this.extractSingleClassSchedule(updateSchedule,
					shiftClass);
			// 开始时间、完成时间全部延后新增排程的时长
			Date tempStartTime = DateUtils.addSeconds(tempClassResult.getExpectStartTime(), (int) extendTime);
			Date tempFinishTime = DateUtils.addSeconds(tempClassResult.getExpectFinishTime(), (int) extendTime);
			tempClassResult.setExpectStartTime(tempStartTime);
			tempClassResult.setExpectFinishTime(tempFinishTime);
			this.updateExpectTime(tempClassResult);
			tempClassResult.getScheduleResult().setBaseValue(tempClassResult.getScheduleResult().getId());
		}

		return updateList;
	}

	/**
	 * 修改排程计划里昂后重算指定班次排程的预计时间
	 * 
	 * @param scheduleResult      新增的排程
	 * @param machineScheduleList 该机台的所有排程记录
	 * @param recipe              新增排程对应的配方信息
	 * @param params              排程设置
	 * @param shiftClass          班次
	 * @return 已有排程里需重算预计时间的记录
	 */
	private List<GlueScheduleResultVo> recaculateExpectTimeModify(GlueScheduleResultVo scheduleResult,
			List<GlueScheduleResultVo> machineScheduleList, MesPmtRecipeVo recipe, Map<String, String> params,
			int shiftClass) {
		// 取出当前班次的排产信息
		SingleClassGlueScheduleResultVO classResult = this.extractSingleClassSchedule(scheduleResult, shiftClass);
		Date startTime = classResult.getExpectStartTime();
		Date oldFinishTime = classResult.getExpectFinishTime();
		if (oldFinishTime == null || startTime == null) {
			return new ArrayList<>(0);
		}
		// 过滤出单班排程信息
		List<SingleClassGlueScheduleResultVO> scheduleResultList = this
				.selectSingleClassScheduleList(machineScheduleList, shiftClass);
		List<GlueScheduleResultVo> updateList = new ArrayList<>(); // 最终需要重算时间的排程列表
		int produceOrder = Optional.ofNullable(classResult.getProduceOrder()).orElse(0);
		// 遍历本班该机台上已有的排程记录
		for (SingleClassGlueScheduleResultVO machineSchedule : scheduleResultList) {
			int tempProduceOrder = Optional.ofNullable(machineSchedule.getProduceOrder()).orElse(0);
			if (produceOrder < tempProduceOrder) {
				// 如果在更新的排程之后，则加入到最终列表中，后续要重算其时间
				updateList.add(machineSchedule.getScheduleResult());
			}
		}

		// 查询对应炼胶间隔时长（触发节点较多，基本上单记录修改触发，暂时不做批量赋值）
		BigDecimal intervalTime = mixingTimeEngineService.getIntervalTime(scheduleResult.getMixArea(), scheduleResult.getGlue(), scheduleResult.getMachineCode());
		// 计算生产时长
		BigDecimal productTime = this.caculateProductTime(classResult.getPlanQty(), recipe, params, intervalTime);
		// 如果原先有开始时间
		// 计算旧的生产时长 = 结束时间 - 开始时间
		BigDecimal oldProductTime = new BigDecimal(oldFinishTime.getTime() - startTime.getTime()).divide(THOUSAND);
		// 计算完成时间：开始时间 + 生产时长
		Date finishTime = DateUtils.addSeconds(startTime, (int) productTime.longValue());
		classResult.setExpectFinishTime(finishTime);
		// 更新生产预计日期相关信息
		this.updateExpectTime(classResult);
		// 生产时间变化量
		Long diffentTime = productTime.subtract(oldProductTime).longValue();
		if (diffentTime == 0) {
			// 如果生产时间没有差，则不需要更新后续排程的时间
			return new ArrayList<>(0);
		}
		// 遍历待修改数据，将所有记录的生产时间时间延后插单的生产时间
		for (GlueScheduleResultVo updateSchedule : updateList) {
			SingleClassGlueScheduleResultVO tempClassResult = this.extractSingleClassSchedule(updateSchedule,
					shiftClass);
			// 开始时间、完成时间全部延后新增排程的时长
			Date tempStartTime = DateUtils.addSeconds(tempClassResult.getExpectStartTime(),
					(int) diffentTime.longValue());
			Date tempFinishTime = DateUtils.addSeconds(tempClassResult.getExpectFinishTime(),
					(int) diffentTime.longValue());
			tempClassResult.setExpectStartTime(tempStartTime);
			tempClassResult.setExpectFinishTime(tempFinishTime);
			this.updateExpectTime(tempClassResult);
			tempClassResult.getScheduleResult().setBaseValue(tempClassResult.getScheduleResult().getId());
		}

		return updateList;
	}

	/**
	 * 过滤出单班有排程信息的数据，并按生产顺序排序
	 * 
	 * @param machineScheduleList
	 * @param shiftClass
	 * @return
	 */
	private List<SingleClassGlueScheduleResultVO> selectSingleClassScheduleList(
			List<GlueScheduleResultVo> machineScheduleList, int shiftClass) {
		// 该班所有排产计划
		List<SingleClassGlueScheduleResultVO> scheduleResultList = machineScheduleList.stream()
				// 抽取单班排产数据
				.map(schedule -> this.extractSingleClassSchedule(schedule, shiftClass))
				// 过滤掉在该班次排产信息不完整的计划
				.filter(classSchedule -> {
					if (classSchedule.getPlanQty() == null
							|| classSchedule.getPlanQty().compareTo(BigDecimal.ZERO) == 0) {
						return false;
					}
					if (classSchedule.getProduceOrder() == null) {
						return false;
					}
					if (classSchedule.getExpectStartTime() == null) {
						return false;
					}
					if (classSchedule.getExpectFinishTime() == null) {
						return false;
					}
					return true;
				})
				// 按生产顺序顺序排序
				.sorted((schedule1, schedule2) -> {
					Integer order1 = schedule1.getProduceOrder();
					Integer order2 = schedule2.getProduceOrder();
					return ObjectUtils.compare(order1, order2, true);
				}).collect(Collectors.toList());
		return scheduleResultList;
	}

	/**
	 * 抽取单个排产班次对应的排程信息
	 * 
	 * @param scheduleResult 排程记录
	 * @param shiftClass     排产班次
	 * @return
	 */
	private SingleClassGlueScheduleResultVO extractSingleClassSchedule(GlueScheduleResultVo scheduleResult,
			int shiftClass) {
		return new SingleClassGlueScheduleResultVO(scheduleResult, shiftClass);
	}

	/**
	 * 计算总生产时长
	 *
	 * @param productedQty     生产数量
	 * @param recipe           配方
	 * @param params           参数配置
	 * @param glueIntervalTime 炼胶间隔时间
	 * @return
	 */
	private BigDecimal caculateProductTime(BigDecimal productedQty, MesPmtRecipeVo recipe, Map<String, String> params, BigDecimal glueIntervalTime) {
		BigDecimal productedDecimal = Optional.ofNullable(productedQty).orElse(BigDecimal.ZERO);
		BigDecimal mixTime = new BigDecimal(recipe.getSummerMixTime().toString()); // 炼胶时间
		BigDecimal intervalTime = glueIntervalTime != null ? glueIntervalTime : new BigDecimal(params.get(GlueEngineConstants.MIX_INTERVAL_TIME)); // 炼胶间隔时间
		BigDecimal productTime = mixTime.add(intervalTime).multiply(productedDecimal); // 总生产时间 = （炼胶时间 + 间隔时间）* 计划数
		return productTime;
	}

	/**
	 * 根据班次更新预计日期相关信息
	 * 
	 * @param classResult 单班次排程记录
	 */
	private void updatePlanQty(SingleClassGlueScheduleResultVO classResult) {
		GlueScheduleResultVo scheduleResult = classResult.getScheduleResult();
		int shiftClass = classResult.getShiftClass();
		// 更新对应班次的字段值
		switch (shiftClass) {
		case GlueEngineConstants.SHIFT_CLASS_MID:
			scheduleResult.setMidPlanQty(classResult.getPlanQty().doubleValue());
			break;
		case GlueEngineConstants.SHIFT_CLASS_NIGHT:
			scheduleResult.setNightPlanQty(classResult.getPlanQty().doubleValue());
			break;
		default:
			scheduleResult.setDayPlanQty(classResult.getPlanQty().doubleValue());
			break;
		}
		scheduleResult.setTotalPlanQty(BigDecimalUtil.add(scheduleResult.getMidPlanQty(),
				scheduleResult.getNightPlanQty(), scheduleResult.getDayPlanQty()));
	}

	/**
	 * 根据班次更新预计日期相关信息
	 * 
	 * @param classResult 单班次排程记录
	 */
	private void updateExpectTime(SingleClassGlueScheduleResultVO classResult) {
		classResult.updateExpectTime();
	}

}
