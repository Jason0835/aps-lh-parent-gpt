package com.zlt.mix.schedule.engine.service.glueschedule.impl;

import com.ruoyi.common.utils.StringUtils;
import com.zlt.mix.common.core.constant.ZltConstant;
import com.zlt.mix.common.core.enums.ProductDayFlagEnum;
import com.zlt.mix.common.core.utils.BigDecimalUtil;
import com.zlt.mix.common.core.utils.DateUtil;
import com.zlt.mix.common.core.utils.GenerageMapKeyUtils;
import com.zlt.mix.common.engine.constants.EngineConstants;
import com.zlt.mix.common.engine.domain.MessageContent;
import com.zlt.mix.common.engine.service.GlueScheduleEngineLogService;
import com.zlt.mix.common.engine.service.impl.IncrementService;
import com.zlt.mix.common.engine.utils.CollectionUtil;
import com.zlt.mix.schedule.api.domain.entity.GlueCollectPlan;
import com.zlt.mix.schedule.api.domain.entity.GlueDecomposePlan;
import com.zlt.mix.schedule.api.domain.entity.GlueScheduleResult;
import com.zlt.mix.schedule.engine.constants.GlueEngineConstants;
import com.zlt.mix.schedule.engine.mapper.GlueScheduleEngineBaseMapper;
import com.zlt.mix.schedule.engine.mapper.GlueScheduleStockPlatMapper;
import com.zlt.mix.schedule.engine.service.basicdata.MachineEngineService;
import com.zlt.mix.schedule.engine.service.basicdata.RecipeEngineService;
import com.zlt.mix.schedule.engine.service.glueschedule.GlueScheduleEngineBaseService;
import com.zlt.mix.schedule.engine.util.*;
import com.zlt.mix.schedule.engine.vo.*;
import com.zlt.mix.setting.api.domain.entity.MixMachine;
import com.zlt.mix.setting.api.domain.entity.MixingProductionModel;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 终炼母炼排程基础服务
 * 
 * @author hakimryan
 *
 */
@Service
public class GlueScheduleEngineBaseServiceImpl implements GlueScheduleEngineBaseService {
	@Resource
	private IncrementService incrementService;
	@Autowired
	private GlueScheduleEngineBaseMapper glueScheduleEngineBaseMapper;
	@Autowired
	private MachineEngineService machineEngineService;
	@Autowired
	private GlueScheduleStockPlatMapper glueScheduleStockPlatMapper;
	@Autowired
	private GlueScheduleEngineLogService logService;
	@Autowired
	private RecipeEngineService recipeEngineService;
	

	/**
	 * 机台名称列表，缓存起来供排程时使用
	 */
	private Map<String, String> machineNameMap = new ConcurrentHashMap<>();
	/**
	 * 一百，用于计算百分比
	 */
	private BigDecimal ONE_HUNDRED = new BigDecimal("100");

	/**
	 * 生成排程批次号
	 * 
	 * @param scheduleDate 排产日期
	 * @param mixArea      密炼区
	 * @return
	 */
	@Override
	public String createBatchNo(Date scheduleDate, String mixArea) {
		// 生成批次号
		String batchNoPre = StringUtils.join(EngineConstants.GLUE_SCHEDULE_PREFIX, mixArea,
				DateUtil.formatDateYmd(scheduleDate));
		return incrementService.getSequence3(batchNoPre);
	}

	/**
	 * 生成工单号
	 * 
	 * @param batchNo 批次号
	 * @return
	 */
	@Override
	public String createOrderNo(String batchNo) {
		return incrementService.getSequence4(batchNo);
	}

	/**
	 * 根据胶料拆分明细、配方，生成排程结果列表
	 *
	 * @param scheduleDate              排产日期
	 * @param glueStock                 库存
	 * @param params                    排程参数设置
	 * @param mixArea                   密炼区
	 * @param mixingTimeMap             胶料间隔时间
	 * @param latestScheduleList        昨日早班的最后一个排程计划
	 * @param mixingPriorityProductMap  炼胶优先配置
	 * @param mixingProductionModelList 生产模式列表
	 * @param glueRecipeMap             胶料配方映射的胶料名称Map
	 * @param reserveGlueRecipeMap      胶料配方映射的反转白班计划量的Map
	 * @param glueRecipeOnlyGlueMap     胶料配方映射的胶料映射Map
	 * @return
	 */
	@Override
	public List<GlueScheduleResultVo> createBaseScheduleResultList(Date scheduleDate,
																   List<MesPmtRecipeVo> mesPmtRecipeList,
																   GlueScheduleStockPool glueStock,
																   Map<String, String> params,
																   String mixArea,
																   Map<String, Long> mixingTimeMap,
																   List<GlueScheduleResultVo> latestScheduleList,
																   Map<String, String> mixingPriorityProductMap,
																   List<MixingProductionModel> mixingProductionModelList,
																   Map<String, String> glueRecipeMap,
																   Map<String, String> reserveGlueRecipeMap,
																   Map<String, String> glueRecipeOnlyGlueMap) {
		List<MixMachine> machineList = machineEngineService.listMixMachineInfo(mixArea);
		// 加载机台名称
		machineNameMap.putAll(machineList.stream()
				.collect(Collectors.toMap(MixMachine::getMachineCode, MixMachine::getMachineName, (m1, m2) -> m1)));
		// 加载有效的配方信息，配方按胶料 + 机台分组
		Map<CombinedMapKey, List<MesPmtRecipeVo>> mesPmtRecipeMap = mesPmtRecipeList.stream().collect(Collectors
				.groupingBy(r -> CombinedMapKey.createKey(r.getRecipeMaterialName(), r.getRecipeEquipCode())));
		// 生产模式Map
		Map<String, MixingProductionModel> productionModelMap = mixingProductionModelList.stream()
				.collect(Collectors.toMap(MixingProductionModel::getGlue, Function.identity(), (v1, v2) -> v1));
		// 记录昨日库存，补全早班的部分，不扣减昨日的日用量
		GlueScheduleStockPool yesterdayGlueStockPool = glueStock.copyStockPool();
		this.caculate16pmEstimateStock(yesterdayGlueStockPool, mixArea, scheduleDate, mesPmtRecipeList, params,
				glueRecipeMap, reserveGlueRecipeMap, false);

		// 处理库存数据，计算19点预计库存
		this.caculate16pmEstimateStock(glueStock, mixArea, scheduleDate, mesPmtRecipeList, params,
				glueRecipeMap, reserveGlueRecipeMap, true);
		GlueScheduleStockPool tempGlueStock = glueStock.copyStockPool(); // 复制当天的库存信息，用于计算配方选择

		// 各胶料备选机台
		Map<String, List<FormulaMachineVo>> glueMachineMap = machineEngineService.listFormulaMachine(mixArea).stream()
				.collect(Collectors.groupingBy(FormulaMachineVo::getGlue));

		// 获取当日和昨日的的分厂需求胶料汇总量
		Map<String, GlueCollectPlan> collectPlanMap = getGlueCollectPlanMap(scheduleDate, mixArea);
		// 读取当天的胶料分解记录，根据昨日白班计划和炼胶优先排序配方选择的顺序
		List<GlueDecomposePlanVo> decomposeList = this.listGlueDecomposePlan(scheduleDate, mixArea, glueMachineMap, latestScheduleList, mixingPriorityProductMap, mesPmtRecipeMap,
				collectPlanMap, yesterdayGlueStockPool, glueRecipeOnlyGlueMap);

		// 遍历胶料分解列表，按规则生成排程记录
		String batchNo = this.createBatchNo(scheduleDate, mixArea); // 生成批次号
		List<GlueScheduleResultVo> baseScheduleResult = new ArrayList<>();
		List<GlueDecomposePlanVo> day1DecomposeList = decomposeList.stream().filter(d -> ProductDayFlagEnum.DAY1.getCode().equals(d.getDayFlag())).collect(Collectors.toList()); // 优先给第一天的需求计划分配产能
	    List<GlueDecomposePlanVo> day2DecomposeList = decomposeList.stream().filter(d -> ProductDayFlagEnum.DAY2.getCode().equals(d.getDayFlag())).collect(Collectors.toList()); // 优先给第一天的需求计划分配产能
	    boolean isDay1Flag = CollectionUtils.isNotEmpty(day1DecomposeList);
		List<GlueDecomposePlanVo> additionalDecomposeList = isDay1Flag? day1DecomposeList: day2DecomposeList;

		Map<String, GlueFactoryRequireVo> factoryRequireMap = null;
		// 各机台剩余产能列表
		Map<String, Long> machineCapacityMap = this.initMachineCapacityMap(baseScheduleResult, machineList, params, mixingTimeMap);
		// 胶料选择过的机台配方，由于配方机台优先的控制，配方机台顺序不一定会从最小的开始选
		Map<String, Set<String>> machineSelectMap = new HashMap<>();
		// 需要塑胶排产后的优先排产的记录，key 机台+胶料+配方类型+配方版本，value 需要胶料的塑胶列表
		// 长度为0的value表示塑胶的库存已充足，null的value表示无需塑胶
		Map<String, List<GlueScheduleResultVo>> needSlScheduleMap = new HashMap<>();

		while (!CollectionUtil.isEmpty(additionalDecomposeList)) { // 待生成排产记录的分解记录
			// 分解计划记录已选择的机台
			decomposeMachineSelect(machineSelectMap, additionalDecomposeList);
			// 根据分解生成排产结果
			List<GlueScheduleResultVo> newScheduleList = this.buildScheduleResult(scheduleDate, mixArea,
					additionalDecomposeList, mesPmtRecipeMap, glueStock, tempGlueStock, batchNo, params,
					glueMachineMap, machineCapacityMap, mixingTimeMap);
			if (CollectionUtil.isEmpty(newScheduleList)) {
				break;
			}
			// 本次创建的排程记录添加到总列表中
			baseScheduleResult.addAll(newScheduleList);
			// 构建分厂需求量列表，每当有新排程记录分配都要重新构建
			factoryRequireMap = this.buildGlueFactoryRequire(scheduleDate, mixArea, baseScheduleResult, yesterdayGlueStockPool,
					collectPlanMap, glueRecipeOnlyGlueMap);
			// 处理塑胶映射，保证塑胶的优先级比对应胶料的优先级更高
			Map<String, List<GlueScheduleResultVo>> slPriorityMap = buildSlPriorityMap(baseScheduleResult, needSlScheduleMap);
			// 重算胶料的优先级
			SchedulePriorityUtils.recaculatePriority(baseScheduleResult, tempGlueStock, factoryRequireMap, null,
					params, slPriorityMap, latestScheduleList, mixingPriorityProductMap, needSlScheduleMap);
			// 计算是否有超出产能的情况，需要添加的机台会构建成分解记录，机台产能超过单班的排产量也会构建分解记录
			additionalDecomposeList = this.addMachineExcessCapacity(scheduleDate, mixArea, newScheduleList,
					glueMachineMap, tempGlueStock, params, mixingTimeMap, machineCapacityMap, machineSelectMap);
			if (CollectionUtil.isEmpty(additionalDecomposeList) && isDay1Flag) { // 如果第一天需求计划已经分配完，再分配第二天的需求计划
			    isDay1Flag = false;
			    additionalDecomposeList = day2DecomposeList;
			}
		}

		// 尝试重新分配剩余量
		this.reassignSurplusQty(baseScheduleResult, decomposeList, machineList, glueMachineMap, tempGlueStock, params, mixingTimeMap);

		// 将因机台产能不足无法排产的计划移除掉
		// 调整成到排程结束后再删除 20221104 hak
//		this.removeNoSchedule(baseScheduleResult);

		// 自动排产前插单的数据要排进来
		boolean isInsert = this.addInsertOrder(baseScheduleResult, mesPmtRecipeList, scheduleDate, mixArea, batchNo);
		if (isInsert) {
			factoryRequireMap = this.buildGlueFactoryRequire(scheduleDate, mixArea, baseScheduleResult, yesterdayGlueStockPool,
					collectPlanMap, glueRecipeOnlyGlueMap);
			// 处理塑胶映射，保证塑胶的优先级比对应胶料的优先级更高
			Map<String, List<GlueScheduleResultVo>> slPriorityMap = buildSlPriorityMap(baseScheduleResult, needSlScheduleMap);
			// 有插单记录依然要重算胶料的优先级
			SchedulePriorityUtils.recaculatePriority(baseScheduleResult, tempGlueStock, factoryRequireMap, null,
					params, slPriorityMap, latestScheduleList, mixingPriorityProductMap, needSlScheduleMap);
		}

		Map<String, List<GlueScheduleResultVo>> groupScheduleMap = baseScheduleResult.stream()
				.filter(v -> StringUtils.isNotBlank(v.getGlue()))
				.collect(Collectors.groupingBy(GlueScheduleResult::getGlue));
		// // 相同胶料，前后绑定的记录是相同的，中间的胶料考虑直接排满需求量，和连续排产的记录类似
		// Map<String, GlueScheduleResultVo> aroudScheduleMap = new HashMap<>();
		// 如果是生产模式的配方，将需要前后生产的记录绑定在排产前后，如果需要前后生产的记录没有需求，则默认绑定ZZ配方的记录
		for (GlueScheduleResultVo schedule : baseScheduleResult) {
			if (!productionModelMap.containsKey(schedule.getGlue())) {
				continue;
			}
			MixingProductionModel productionModel = productionModelMap.get(schedule.getGlue());
			// 如果有限制机台，只有指定机台的配方需要考虑生产模式
			if (StringUtils.isNotBlank(productionModel.getMachineCode())
					&& StringUtils.isNotBlank(schedule.getMachineCode())
					&& !schedule.getMachineCode().equals(productionModel.getMachineCode())) {
				continue;
			}

			// GlueScheduleResultVo aroundSchedule = aroudScheduleMap.get(schedule.getGlue());
			// // 如果存在相同胶料的记录，取相同的前后生产记录
			// if (aroundSchedule != null) {
			// 	schedule.setProductionBefore(aroundSchedule.getProductionBefore());
			// 	schedule.setProductionAfter(aroundSchedule.getProductionAfter());
			//
			// 	continue;
			// }


			// 前后环绕的排程记录
			GlueScheduleResultVo beforeSchedule = getAroundSchedule(schedule, groupScheduleMap, productionModel.getBeforeQty(), productionModel.getBeforeGlue(), mesPmtRecipeMap, glueStock);
			GlueScheduleResultVo afterSchedule = getAroundSchedule(schedule, groupScheduleMap, productionModel.getAfterQty(), productionModel.getAfterGlue(), mesPmtRecipeMap, glueStock);
			schedule.setProductionBefore(beforeSchedule);
			schedule.setProductionAfter(afterSchedule);

			// // 记录绑定记录
			// aroudScheduleMap.put(schedule.getGlue(), schedule);
		}
		
        // 加载日用量（分厂需求计划）
//        for (GlueScheduleResultVo result: baseScheduleResult) {
//            GlueCollectPlan collectPlan = collectPlanMap.get(result.getGlue());
//            Double dayUseQty = 0D;
//            if (collectPlan != null && collectPlan.getTotalPlanQty() != null) {
//                dayUseQty = collectPlan.getTotalPlanQty();
//            }
//            result.setDayUseQty(dayUseQty);
//        }

		// 预先按胶料 + 配方类型优先级排好序
		return baseScheduleResult.stream().sorted(createScheduleSorter()).collect(Collectors.toList());
	}

	/**
	 * 获取环绕的配方记录
	 *
	 * @param schedule         排产记录
	 * @param groupScheduleMap 排产记录
	 * @param beforeQty        环绕数量
	 * @param beforeGlue       环绕的胶料
	 * @param mesPmtRecipeMap  配方信息
	 * @return
	 */
	private GlueScheduleResultVo getAroundSchedule(GlueScheduleResultVo schedule,
												   Map<String, List<GlueScheduleResultVo>> groupScheduleMap,
												   Double beforeQty,
												   String beforeGlue,
												   Map<CombinedMapKey, List<MesPmtRecipeVo>> mesPmtRecipeMap, 
												   GlueScheduleStockPool glueStock) {
		GlueScheduleResultVo beforeSchedule = null;
		List<GlueScheduleResultVo> voList = groupScheduleMap.get(beforeGlue);
		MesPmtRecipeVo recipeVo = null;
		if (CollectionUtils.isNotEmpty(voList)) {
			double diffQty = beforeQty;
			// todo 暂不考虑扣减顺序
			// 扣减对应排产记录的计划量
			for (GlueScheduleResultVo itemVo : voList) {
				BigDecimal planQty = itemVo.getPlanQty();
				if (planQty != null && planQty.compareTo(BigDecimal.ZERO) > 0) {
					diffQty = BigDecimalUtil.sub(planQty.doubleValue(), diffQty);
					if (diffQty < 0) {
						itemVo.setPlanQty(BigDecimal.ZERO);
						break;
					} else {
						itemVo.setPlanQty(BigDecimal.valueOf(diffQty));
					}
				}
				// 如果存在和当前排产相同机台的记录，取出对应的配方信息
				if (itemVo.getPmtRecipe() != null && schedule.getMachineCode().equals(itemVo.getMachineCode())) {
					recipeVo = itemVo.getPmtRecipe();
				}
			}
		}

		// 如果没有配方，尝试找到对应的ZZ配方，
		if (recipeVo == null) {
			// todo 暂不考虑，生产模式和前后排产胶料的配方机台不一致的场景
			// 获取对应胶料的ZZ配方进行排产
			CombinedMapKey recipeKey = CombinedMapKey.createKey(beforeGlue, schedule.getMachineCode());
			List<MesPmtRecipeVo> recipeVoList = mesPmtRecipeMap.get(recipeKey);
			if (CollectionUtils.isNotEmpty(recipeVoList)) {
				recipeVo = recipeVoList.stream().filter(v -> GlueEngineConstants.RECIPE_TYPE_ZZ.equals(v.getRecipeTypeName())).findFirst().orElse(null);
			}
		}

		// 如果zz配方也没有，就忽略生产模式
		if (recipeVo == null) {
			return beforeSchedule;
		}

		// 补充基础字段
		beforeSchedule = new GlueScheduleResultVo();
		beforeSchedule.setProductionModelTag(true);
		beforeSchedule.setRecipeType(recipeVo.getRecipeType());
		beforeSchedule.setPmtRecipe(recipeVo);
		beforeSchedule.setRequireQty(beforeQty);
		beforeSchedule.setPlanQty(BigDecimal.ZERO);
		beforeSchedule.setGlue(beforeGlue);
		beforeSchedule.setMachineCode(schedule.getMachineCode());
		// 如果本次有排产，补充分解的字段
		GlueScheduleResultVo sameGlueSchedule = new GlueScheduleResultVo();
		if (CollectionUtils.isNotEmpty(voList)) {
			sameGlueSchedule = voList.get(0);
		}
		// 构建排程记录
		this.initBaseScheduleProperties(beforeSchedule);
		// 给胶料分解相关栏位赋值
		beforeSchedule.setIsFinishing(sameGlueSchedule.getIsFinishing());
		beforeSchedule.setDecomposeBatchNo(sameGlueSchedule.getBatchNo());
		this.copyRecipeProperties(beforeSchedule, recipeVo); // 给配方相关栏位赋值
		beforeSchedule.setBatchNo(schedule.getBatchNo());
		beforeSchedule.setOrderNo(this.createOrderNo(schedule.getBatchNo())); // 工单号
		beforeSchedule.setScheduleDate(schedule.getScheduleDate());
		beforeSchedule.setMixArea(schedule.getMixArea());
		beforeSchedule.setTotalPlanQty(0D);
		beforeSchedule.setStockQty(glueStock.getStockNum(beforeGlue, recipeVo.getMajorType()).doubleValue());
		beforeSchedule.setSafeStockQty(glueStock.getSafeStock(beforeGlue).doubleValue());

		return beforeSchedule;
	}

	/**
	 * 获取当日和昨日的的分厂需求胶料汇总量
	 *
	 * @param scheduleDate 排产
	 * @param mixArea      密炼区
	 * @return 分厂需求胶料汇总量
	 */
	public Map<String, GlueCollectPlan> getGlueCollectPlanMap(Date scheduleDate, String mixArea) {
		Map<String, GlueCollectPlan> collectPlanMap = glueScheduleEngineBaseMapper
				.selectGlueCollectPlanList(scheduleDate, mixArea).stream()
				.collect(Collectors.toMap(GlueCollectPlan::getGlue, Function.identity()));
		// 昨日的分厂需求胶料汇总量
		Map<String, GlueCollectPlan> lastDayPlanMap = glueScheduleEngineBaseMapper
				.selectGlueCollectPlanList(DateUtils.addDays(scheduleDate, -1), mixArea).stream()
				.collect(Collectors.toMap(GlueCollectPlan::getGlue, Function.identity()));
		// 计算优先级时，夜班的计划=下一天的日用量+预计库存负数（库存扣减日用量不足需要夜班生产），白班的计划=下一天一半的日用量
		computeDailyDose(collectPlanMap, lastDayPlanMap);
		return collectPlanMap;
	}

	/**
	 * 分解计划记录已选择的机台
	 *
	 * @param machineSelectMap 胶料选择过的机台
	 * @param decomposeList    分解计划
	 */
	private void decomposeMachineSelect(Map<String, Set<String>> machineSelectMap, List<GlueDecomposePlanVo> decomposeList) {
		if (CollectionUtils.isEmpty(decomposeList)) {
			return;
		}

		// 分解计划记录已选择的机台
		for (GlueDecomposePlanVo planVo : decomposeList) {
			if (StringUtils.isEmpty(planVo.getGlue()) || StringUtils.isEmpty(planVo.getMachineCode())) {
				continue;
			}
			Set<String> machineSelect = machineSelectMap.getOrDefault(planVo.getGlue(), new HashSet<>());
			machineSelect.add(planVo.getMachineCode());
			machineSelectMap.put(planVo.getGlue(), machineSelect);
		}
	}

	/**
	 * 计算优先级时，昨日日用量先满足，再优先满足今日的日用量
	 *
	 * @param collectPlanMap 汇总胶料需求计划
	 * @param lastDayPlanMap 昨日胶料需求计划
	 */
	public void computeDailyDose(Map<String, GlueCollectPlan> collectPlanMap, Map<String, GlueCollectPlan> lastDayPlanMap) {
		if (collectPlanMap.isEmpty()) {
			return;
		}
		if (lastDayPlanMap == null) {
			lastDayPlanMap = new HashMap<>();
		}

		for (GlueCollectPlan itemCollectPlan : collectPlanMap.values()) {
			GlueCollectPlan lastCollectPlan = lastDayPlanMap.get(itemCollectPlan.getGlue());
			// 夜班的计划用量=昨日日用量
			itemCollectPlan.setMidPlanQty(lastCollectPlan != null ? lastCollectPlan.getTotalPlanQty() : 0D);
			// 白班的计划用量=下一天的日用量
			itemCollectPlan.setNightPlanQty(itemCollectPlan.getTotalPlanQty());
		}
	}

	/**
	 * 构建排程对应的塑炼胶优先级映射，记录塑炼排产后优先排产的记录
	 *
	 * @param baseScheduleResult 排程结果
	 * @param needSlScheduleMap  需要塑炼排产记录
	 * @return 塑炼胶优先配方
	 */
	public Map<String, List<GlueScheduleResultVo>> buildSlPriorityMap(List<GlueScheduleResultVo> baseScheduleResult, Map<String, List<GlueScheduleResultVo>> needSlScheduleMap) {
		if (CollectionUtil.isEmpty(baseScheduleResult)) {
			return Collections.emptyMap();
		}
		needSlScheduleMap.clear();

		// 排产物料类型是塑胶的记录
		Map<String, List<GlueScheduleResultVo>> slScheduleMap = baseScheduleResult.stream()
				.filter(v -> GlueEngineConstants.MAJOR_TYPE_SL.equals(v.getMajorType()) && StringUtils.isNotBlank(v.getGlue()))
				.collect(Collectors.groupingBy(GlueScheduleResultVo::getGlue));

		// 记录塑炼胶-对应的胶料+机台
		Map<String, List<GlueScheduleResultVo>> slPriorityMap = new HashMap<>();
		// 判断对应排程使用的配方是否包含塑炼胶
		for (GlueScheduleResultVo itemResult : baseScheduleResult) {
			MesPmtRecipeVo pmtRecipe = itemResult.getPmtRecipe();
			if (pmtRecipe == null) {
				continue;
			}
			// 判断称重是否包含塑炼胶
			List<MesPmtRecipeWeightVo> weightList = pmtRecipe.getRecipeWeightList();
			if (CollectionUtil.isEmpty(weightList)) {
				continue;
			}

			for (MesPmtRecipeWeightVo itemWeight : weightList) {
				if (GlueEngineConstants.MAJOR_TYPE_SL.equals(itemWeight.getMajorType())) {
					// 记录塑胶优先
					String weightKey = itemWeight.getRecipeMaterialName();
					List<GlueScheduleResultVo> prioritySet = slPriorityMap.getOrDefault(weightKey, new ArrayList<>());
					prioritySet.add(itemResult);
					slPriorityMap.put(weightKey, prioritySet);

					// 记录塑胶生产后优先进行排产
					String needSlKey = GenerageMapKeyUtils.createMapKey(itemResult.getMachineCode(), itemResult.getGlue(), itemResult.getRecipeType(), itemResult.getRecipeVersionId());
					// 如果需要需要胶，对应塑炼胶库存充足，记录一个长度0的Set
					needSlScheduleMap.putIfAbsent(needSlKey, new ArrayList<>());
					List<GlueScheduleResultVo> slResultList = slScheduleMap.get(weightKey);
					if (CollectionUtils.isNotEmpty(slResultList)) {
						List<GlueScheduleResultVo> needSlSet = needSlScheduleMap.getOrDefault(needSlKey, new ArrayList<>());
						needSlSet.addAll(slResultList);
						needSlScheduleMap.put(needSlKey, needSlSet);
					}
				}
			}
		}

		return slPriorityMap;
	}

	/**
	 * 构建分厂需求量列表
	 *
	 * @param scheduleDate          排产日
	 * @param mixArea               密炼区
	 * @param resultList            排产列表
	 * @param glueStock             库存列表
	 * @param glueRecipeOnlyGlueMap 胶料配方映射的胶料映射Map
	 * @return
	 */
	public Map<String, GlueFactoryRequireVo> buildGlueFactoryRequire(Date scheduleDate, String mixArea,
			List<GlueScheduleResultVo> resultList, GlueScheduleStockPool glueStock, Map<String, String> glueRecipeOnlyGlueMap) {
		// 获取当日和昨日的的分厂需求胶料汇总量
		Map<String, GlueCollectPlan> collectPlanMap = getGlueCollectPlanMap(scheduleDate, mixArea);
		// 关联外厂各班需求量信息
		return this.buildGlueFactoryRequire(scheduleDate, mixArea, resultList, glueStock, collectPlanMap, glueRecipeOnlyGlueMap);
	}

	/**
	 * 构建分厂需求量列表
	 *
	 * @param scheduleDate          排产日
	 * @param mixArea               密炼区
	 * @param resultList            排产列表
	 * @param glueStock             库存列表
	 * @param collectPlanMap        分厂需求胶料列表
	 * @param glueRecipeOnlyGlueMap 胶料配方映射的胶料映射Map
	 * @return
	 */
	private Map<String, GlueFactoryRequireVo> buildGlueFactoryRequire(Date scheduleDate, String mixArea,
			List<GlueScheduleResultVo> resultList, GlueScheduleStockPool glueStock,
			Map<String, GlueCollectPlan> collectPlanMap, Map<String, String> glueRecipeOnlyGlueMap) {
		// 处理终炼胶，已处理的放到map中
		Map<String, GlueFactoryRequireVo> factoryRequireMap = this.handleFinalGlueRequire(scheduleDate, mixArea,
				resultList, glueStock, collectPlanMap, glueRecipeOnlyGlueMap);
		// 处理母炼较，已处理的放到map中，返回值包含了终胶的内容
		Map<String, GlueFactoryRequireVo> allFactoryRequireMap = this.handleMLGlueRequire(scheduleDate, mixArea, resultList, glueStock, factoryRequireMap);
		// 处理母炼胶，接着需要处理塑炼胶的需求
		return this.handleSLGlueRequire(resultList, glueStock, allFactoryRequireMap);
	}

	/**
	 * 处理塑炼胶的分厂需求
	 *
	 * @param resultList        排程列表
	 * @param glueStock         库存
	 * @param factoryRequireMap 分厂需求
	 * @return 处理的结果
	 */
	private Map<String, GlueFactoryRequireVo> handleSLGlueRequire(List<GlueScheduleResultVo> resultList,
																  GlueScheduleStockPool glueStock,
																  Map<String, GlueFactoryRequireVo> factoryRequireMap) {
		// 处理前校验是否有需要处理子胶料的需求
		// 终炼胶排产列表
		Map<String, List<GlueScheduleResultVo>> resultMap = resultList.stream()
				.collect(Collectors.groupingBy(GlueScheduleResultVo::getGlue));
		Collection<GlueFactoryRequireVo> factoryRequireList = factoryRequireMap.values();

		// 塑炼胶的合计需求称重、记录最早的需求班次
		Map<String, BigDecimal> requireQtyMap = new HashMap<>();
		Map<String, Integer> requireClassMap = new HashMap<>();
		// 记录上级胶+塑胶
		Map<String, BigDecimal> maxFactoryRequireMap = new HashMap<>();

		for (GlueFactoryRequireVo factoryRequire : factoryRequireList) {
			String glueCode = factoryRequire.getGlue();
			List<GlueScheduleResultVo> glueResultList = resultMap.get(glueCode);
			for (GlueScheduleResultVo result : glueResultList) { // 遍历待生产胶料的排产列表
				BigDecimal requireDifference = factoryRequire.getRequireDifference();
				Integer requireClass = factoryRequire.getRequireClass();
				MesPmtRecipeVo recipe = result.getPmtRecipe();
				List<MesPmtRecipeWeightVo> recipeWeightList = recipe.getRecipeWeightList(); // 配方重量明细
				BigDecimal maxSetWeight = RecipeUtil.getMaxSetWeight(recipeWeightList); // 获取称重配方中最大的终炼母炼胶
				for (MesPmtRecipeWeightVo recipeWeight : recipeWeightList) { // 遍历
					String weightGlueCode = recipeWeight.getRecipeMaterialName();
					String majorType = recipeWeight.getMajorType(); // 物料大类
					BigDecimal setWeight = BigDecimalUtil.valueOfZero(recipeWeight.getSetWeight());
					String realMajorType = RecipeUtil.getMajorType(weightGlueCode, majorType, setWeight, maxSetWeight); // 获取真正的物料类型
					// 先判断物料大类，排程涉及到的类型才需要处理：只有塑炼需要处理
					if (GlueEngineConstants.MAJOR_TYPE_SL.equals(realMajorType)) {
						// 塑炼按照称重换算
						List<GlueScheduleResultVo> weightResultList = resultMap.get(weightGlueCode);
						if (CollectionUtils.isEmpty(weightResultList)) { // 没有排产的不需要处理
							continue;
						}
						// 塑炼胶必须库存 = 父物料的需求差值 * 称重数量
						BigDecimal childGlueRequireQty = requireDifference.multiply(setWeight);
						// 如果上级胶已经计算过分厂需求，计算差值是否更大，如果更大补全差值
						String groupKey = GenerageMapKeyUtils.createMapKey(glueCode, weightGlueCode);
						BigDecimal oldRequire = maxFactoryRequireMap.get(groupKey);
						if (oldRequire == null) {
							maxFactoryRequireMap.put(groupKey, childGlueRequireQty);
						} else if (oldRequire.compareTo(childGlueRequireQty) < 0) {
							BigDecimal diffRequire = childGlueRequireQty.subtract(oldRequire);
							maxFactoryRequireMap.put(groupKey, childGlueRequireQty);
							childGlueRequireQty = diffRequire;
						}
						requireQtyMap.put(weightGlueCode, requireQtyMap.getOrDefault(weightGlueCode, BigDecimal.ZERO).add(childGlueRequireQty));
						// 合计需求量、记录最早需求班次
						Integer oldClass = requireClassMap.get(weightGlueCode);
						if (oldClass == null || oldClass > requireClass) {
							requireClassMap.put(weightGlueCode, requireClass);
						}
					}
				}
			}
		}

		if (requireQtyMap.isEmpty()) {
			return factoryRequireMap;
		}

		requireQtyMap.forEach((glueCode, requireQty) -> {
			List<GlueScheduleResultVo> slScheduleList = resultMap.get(glueCode);
			if (CollectionUtils.isEmpty(slScheduleList) || requireQty == null) {
				return;
			}
			GlueScheduleResultVo slSchedule = slScheduleList.get(0);
			String majorType = slSchedule.getMajorType();
			MesPmtRecipeVo pmtRecipe = slSchedule.getPmtRecipe();
			if (StringUtils.isBlank(majorType) || pmtRecipe == null || pmtRecipe.getLotTotalWeight() == null || pmtRecipe.getLotTotalWeight() <= 0) {
				return;
			}


			Integer requireClass = requireClassMap.getOrDefault(glueCode, GlueEngineConstants.SHIFT_CLASS_NIGHT);
			// 塑炼胶现有称重
			BigDecimal childGlueStockNum = glueStock.getStockWeight(glueCode, majorType);
			BigDecimal diffQty = requireQty.subtract(childGlueStockNum);
			if (diffQty.compareTo(BigDecimal.ZERO) > 0) {
				// 有需求量，记录需求量的车数和班次
				double reduceQty = BigDecimalUtil.roundUp(BigDecimalUtil.div(diffQty.doubleValue(), pmtRecipe.getLotTotalWeight()), 0);
				this.updateFactoryRequire(factoryRequireMap, glueCode, requireClass, BigDecimal.valueOf(reduceQty));
			}
		});

		return factoryRequireMap;
	}

	/**
	 * 取出无库存胶料的物料类型
	 * 
	 * @param recipe            配方
	 * @param glueStock         胶料库存
	 * @param defaultMixType    默认掺胶类型，找不到的时候直接返回这个
	 * @param factoryRequireMap 各胶料分厂需求列表
	 * @return
	 */
	private Map<String, GlueFactoryRequireVo> handleMLGlueRequire(Date scheduleDate, String mixArea,
			List<GlueScheduleResultVo> resultList, GlueScheduleStockPool glueStock,
			Map<String, GlueFactoryRequireVo> factoryRequireMap) {
		// 处理前校验是否有需要处理子胶料的需求
		// 终炼胶排产列表
		Map<String, List<GlueScheduleResultVo>> resultMap = resultList.stream()
				.collect(Collectors.groupingBy(GlueScheduleResultVo::getGlue));
		List<GlueFactoryRequireVo> factoryRequireList = factoryRequireMap.values().stream()
				.filter(f -> !f.isRequireChild()).collect(Collectors.toList());

		// 记录母炼胶最大的分厂需求量，根据上级胶+下级胶作为key
		Map<String, GlueFactoryRequireVo> maxFactoryRequireMap = new HashMap<>();

		boolean isUpdateRequire = false;
		for (GlueFactoryRequireVo factoryRequire : factoryRequireList) {
			String glueCode = factoryRequire.getGlue();
			List<GlueScheduleResultVo> glueResultList = resultMap.get(glueCode);
			for (GlueScheduleResultVo result : glueResultList) { // 遍历待生产胶料的排产列表
				BigDecimal requireDifference = factoryRequire.getRequireDifference();
				Integer requireClass = factoryRequire.getRequireClass();
				MesPmtRecipeVo recipe = result.getPmtRecipe();
				List<MesPmtRecipeWeightVo> recipeWeightList = recipe.getRecipeWeightList(); // 配方重量明细
				BigDecimal maxSetWeight = RecipeUtil.getMaxSetWeight(recipeWeightList); // 获取称重配方中最大的终炼母炼胶
				for (MesPmtRecipeWeightVo recipteWeight : recipeWeightList) { // 遍历
					String weightGlueCode = recipteWeight.getRecipeMaterialName();
					String majorType = recipteWeight.getMajorType(); // 物料大类
					BigDecimal setWeight = BigDecimalUtil.valueOfZero(recipteWeight.getSetWeight());
					String realMajorType = RecipeUtil.getMajorType(weightGlueCode, majorType, setWeight, maxSetWeight); // 获取真正的物料类型
					// 先判断物料大类，排程涉及到的类型才需要处理：只有母炼胶需要处理
					if (GlueEngineConstants.MAJOR_TYPE_ML.equals(realMajorType)) {
						// 取出称重配方明细胶料的排产记录
						List<GlueScheduleResultVo> weightResultList = resultMap.get(weightGlueCode);
						if (CollectionUtils.isEmpty(weightResultList)) { // 没有排产的不需要处理
							continue;
						}
						// 取出库存信息
						BigDecimal conversionRatio = recipteWeight.getConversionRatio(); // 换算比率
						if (conversionRatio == null || conversionRatio.compareTo(BigDecimal.ZERO) == 0) {
							conversionRatio = BigDecimal.ONE;
						}
						// 母炼胶必须库存 = 父物料的需求差值 / 换算比率
						BigDecimal childGlueRequireQty = requireDifference.divide(conversionRatio, 0, RoundingMode.UP);
						// 母炼胶现有库存
						BigDecimal childGlueStockNum = glueStock.getStockNum(weightGlueCode, majorType);

						// 上级胶+配方+下级胶
						String groupKey = GenerageMapKeyUtils.createMapKey(glueCode, weightGlueCode);
						// 判断如果库存小于需求数，则差值需要作为需求量
						if (childGlueRequireQty.compareTo(childGlueStockNum) > 0) {
							BigDecimal reduceQty = childGlueRequireQty.subtract(childGlueStockNum); // 需求量差值

							GlueFactoryRequireVo factoryRequireVo = new GlueFactoryRequireVo();
							factoryRequireVo.setGlue(weightGlueCode);
							factoryRequireVo.setRequireClass(requireClass);
							factoryRequireVo.setRequireDifference(reduceQty);
							// 如果已有工厂需求量，取最大工厂需求量
							GlueFactoryRequireVo oldFactory = maxFactoryRequireMap.get(groupKey);
							if (oldFactory != null
									&& oldFactory.getRequireDifference() != null
									&& factoryRequireVo.getRequireDifference() != null
									&& oldFactory.getRequireDifference().compareTo(factoryRequireVo.getRequireDifference()) > 0) {
								factoryRequireVo = oldFactory;
							}
							maxFactoryRequireMap.put(groupKey, factoryRequireVo);
							// this.updateFactoryRequire(factoryRequireMap, weightGlueCode, requireClass, reduceQty);
							isUpdateRequire = true;
						}
					}
				}
			}
			// 完成处理后，该胶料全部排程需求子胶料标识设置为true，后续递归遍历就不会再处理该胶料
			factoryRequire.setRequireChild(true);
		}

		// 更新分厂胶料
		maxFactoryRequireMap.forEach((k, factoryRequireVo) -> {
			this.updateFactoryRequire(factoryRequireMap, factoryRequireVo.getGlue(), factoryRequireVo.getRequireClass(), factoryRequireVo.getRequireDifference());
		});

		if (isUpdateRequire) {
			// 递归处理再下一级的子胶
			this.handleMLGlueRequire(scheduleDate, mixArea, resultList, glueStock, factoryRequireMap);
		}
		return factoryRequireMap;
	}

	/**
	 * 处理终炼胶的各班需求量
	 *
	 * @param scheduleDate          排产日
	 * @param mixArea               密炼区
	 * @param resultList            排产列表
	 * @param glueStock             库存列表
	 * @param glueRecipeOnlyGlueMap 胶料配方映射的胶料映射Map
	 */
	private Map<String, GlueFactoryRequireVo> handleFinalGlueRequire(Date scheduleDate, String mixArea,
			List<GlueScheduleResultVo> resultList, GlueScheduleStockPool glueStock,
			Map<String, GlueCollectPlan> collectPlanMap, Map<String, String> glueRecipeOnlyGlueMap) {
		// 各胶料分厂需求列表
		Map<String, GlueFactoryRequireVo> factoryRequireMap = new HashMap<>();

		Map<String, String> glueTypeMap = resultList.stream().collect(
				Collectors.toMap(GlueScheduleResultVo::getGlue, GlueScheduleResultVo::getMajorType, (v1, v2) -> v2));

		for (Entry<String, String> entry : glueTypeMap.entrySet()) {
			String glueCode = entry.getKey();
			String majorType = entry.getValue();
			// 将终炼胶的分厂各班需求量关联到分解计划中
			GlueCollectPlan collectPlan = collectPlanMap.get(glueCode);
			if (collectPlan == null) {
				continue;
			}

			Integer requireClass = null; // 需求量按中班 -> 夜班 -> 白班的顺序扣减库存量，哪一班库存不足则记录起这一班
			BigDecimal requireDifference = BigDecimal.ZERO; // 需求差异量，大于0说明库存不够支持需求量
			BigDecimal surplusStock = glueStock.getStockNum(glueCode, majorType); // 剩余库存
			// 特别的，如果是纯胶有库存，但是纯胶有日用量的部分，还足够的库存可以作为掺胶的库存
			if (glueRecipeOnlyGlueMap.containsKey(glueCode)) {
				String mapGlue = glueRecipeOnlyGlueMap.get(glueCode);
				GlueCollectPlan mapGlueCollect = collectPlanMap.get(mapGlue);
				if (mapGlueCollect != null) {
					String mapGlueMajorType = glueTypeMap.getOrDefault(mapGlue, GlueEngineConstants.MAJOR_TYPE_ZL);
					BigDecimal mapGlueStockNum = glueStock.getStockNum(mapGlue, mapGlueMajorType);
					BigDecimal mapGluePlanQty = BigDecimalUtil.valueOfZero(mapGlueCollect.getMidPlanQty())
							.add(BigDecimalUtil.valueOfZero(mapGlueCollect.getNightPlanQty()))
							.add(BigDecimalUtil.valueOfZero(mapGlueCollect.getDayPlanQty()));
					if (mapGluePlanQty.compareTo(BigDecimal.ZERO) == 0) {
						mapGluePlanQty = BigDecimalUtil.valueOfZero(mapGlueCollect.getTotalPlanQty());
					}
					BigDecimal mapGlueSurplusQty = mapGlueStockNum.subtract(mapGluePlanQty);
					if (mapGlueSurplusQty.compareTo(BigDecimal.ZERO) > 0) {
						surplusStock = surplusStock.add(mapGlueSurplusQty);
					}
				}
			}
			// 判断库存是否足够扣减各班的分厂需求量
			BigDecimal midPlanQty = BigDecimalUtil.valueOfZero(collectPlan.getMidPlanQty());
			BigDecimal nightPlanQty = BigDecimalUtil.valueOfZero(collectPlan.getNightPlanQty());
			BigDecimal dayPlanQty = BigDecimalUtil.valueOfZero(collectPlan.getDayPlanQty());
			BigDecimal totalPlanQty = BigDecimalUtil.valueOfZero(collectPlan.getTotalPlanQty());

			if (midPlanQty.add(nightPlanQty).add(dayPlanQty).compareTo(BigDecimal.ZERO) != 0) { // 中夜白班任意一班有需求，则以看哪一班开始库存不足
				// 中班
				BigDecimal midReduceQty = BigDecimalUtil.least(surplusStock, midPlanQty); // 待扣除量，为库存与计划量的较小值
				requireDifference = requireDifference.add(midPlanQty.subtract(midReduceQty)); // 差异量需要加上当班需求量与扣减库存后的剩余量
				surplusStock = surplusStock.subtract(midReduceQty); // 更新库存
				if (requireClass == null && requireDifference.compareTo(BigDecimal.ZERO) > 0) { // 库存量不足，则记录从哪一班开始库存不足
					requireClass = GlueEngineConstants.SHIFT_CLASS_MID;
				}
				// 夜班
				BigDecimal nightReduceQty = BigDecimalUtil.least(surplusStock, nightPlanQty);
				requireDifference = requireDifference.add(nightPlanQty.subtract(nightReduceQty));
				surplusStock = surplusStock.subtract(nightReduceQty);
				if (requireClass == null && requireDifference.compareTo(BigDecimal.ZERO) > 0) {
					requireClass = GlueEngineConstants.SHIFT_CLASS_NIGHT;
				}
				// 白班
				// BigDecimal dayReduceQty = BigDecimalUtil.least(surplusStock, dayPlanQty);
				// requireDifference = requireDifference.add(dayPlanQty.subtract(dayReduceQty));
				// surplusStock = surplusStock.subtract(dayReduceQty);
				// if (requireClass == null && requireDifference.compareTo(BigDecimal.ZERO) > 0) {
				// 	requireClass = GlueEngineConstants.SHIFT_CLASS_DAY;
				// }
			} else if (totalPlanQty.compareTo(BigDecimal.ZERO) != 0) { // 如果只有总量，且库存不足，则当作白班需求
				BigDecimal reduceQty = BigDecimalUtil.least(surplusStock, totalPlanQty);
				requireDifference = requireDifference.add(totalPlanQty.subtract(reduceQty));
				surplusStock = surplusStock.subtract(reduceQty);
				if (requireClass == null && requireDifference.compareTo(BigDecimal.ZERO) > 0) {
					requireClass = GlueEngineConstants.SHIFT_CLASS_NIGHT;
				}
			}

			if (requireClass != null) {
				this.updateFactoryRequire(factoryRequireMap, glueCode, requireClass, requireDifference);
			}
		}
		return factoryRequireMap;
	}

	/**
	 * 更新分厂需求列表
	 * 
	 * @param factoryRequireMap 分厂需求列表
	 * @param glueCode          胶料号
	 * @param requireClass      需求班别
	 * @param requireDifference 需求差值
	 */
	private void updateFactoryRequire(Map<String, GlueFactoryRequireVo> factoryRequireMap, String glueCode,
			Integer requireClass, BigDecimal requireDifference) {
		GlueFactoryRequireVo factoryRequireVo = factoryRequireMap.get(glueCode);
		if (factoryRequireVo == null) {
			factoryRequireVo = new GlueFactoryRequireVo();
			factoryRequireVo.setGlue(glueCode);
			factoryRequireMap.put(glueCode, factoryRequireVo);
		}
		Integer oldRequireClass = factoryRequireVo.getRequireClass();
		BigDecimal oldRequireDifference = factoryRequireVo.getRequireDifference();
		if (oldRequireClass == null || oldRequireClass > requireClass) {
			factoryRequireVo.setRequireClass(requireClass);
		}
		BigDecimal newReduceQty = requireDifference;
		if (oldRequireDifference != null) {
			newReduceQty = newReduceQty.add(oldRequireDifference);
		}
		factoryRequireVo.setRequireDifference(newReduceQty);
	}

	/**
	 * 尝试重新分配剩余量，尝试将剩余量分配到对应有剩余产能的机台上
	 *
	 * @param scheduleResult 排产记录
	 * @param decomposeList  分解记录
	 * @param machineList    机台列表
	 * @param glueMachineMap 胶料机台列表
	 * @param params         排产参数
	 * @param mixingTimeMap  胶料间隔时间
	 */
	private void reassignSurplusQty(List<GlueScheduleResultVo> scheduleResult, List<GlueDecomposePlanVo> decomposeList,
			List<MixMachine> machineList, Map<String, List<FormulaMachineVo>> glueMachineMap,
			GlueScheduleStockPool glueStock, Map<String, String> params, Map<String, Long> mixingTimeMap) {
		// 获取机台的产能列表
		Map<String, Long> machineCapacityMap = this.initMachineCapacityMap(scheduleResult, machineList, params, mixingTimeMap);
		Long mixOntervalTime = new Long(params.getOrDefault(GlueEngineConstants.MIX_INTERVAL_TIME, "0")); // 每一车的间隔时间

		// 从胶料分解列表提取每个胶料的需求量
		Map<String, Double> decomposeProduceQtyMap = decomposeList.stream().filter(p -> p.getProduceQty() != null)
				.collect(Collectors.groupingBy(GlueDecomposePlanVo::getGlue,
						Collectors.summingDouble(GlueDecomposePlanVo::getProduceQty)));

		// 从排产计划提取每个胶料的总排产情况
		Map<String, List<GlueScheduleResultVo>> scheduleMap = scheduleResult.stream()
				.filter(s -> s.getPlanQty() != null).collect(Collectors.groupingBy(GlueScheduleResultVo::getGlue));

		// 胶料要先按优先级排序之后再去分配产能
		List<String> glueList = new LinkedList<>();
		Set<String> glueSet = new HashSet<>();
		scheduleResult.stream()
				.sorted(Comparator.comparing(GlueScheduleResultVo::getPriority, Comparator.reverseOrder())) // 优先级由大到小
				.forEach(schedule -> {
					String glue = schedule.getGlue();
					if (!glueSet.contains(glue)) {
						glueList.add(glue);
						glueSet.add(glue);
					}
				});

		// 根据排产情况看哪个胶有剩余计划量
		for (String glueCode : glueList) {
			List<GlueScheduleResultVo> scheduleList = scheduleMap.get(glueCode);
			// 计算本胶料的剩余量
			BigDecimal planQty = scheduleList.stream().map(GlueScheduleResultVo::getPlanQty).reduce(BigDecimal.ZERO,
					BigDecimal::add);
			BigDecimal produceQty = BigDecimalUtil.valueOfZero(decomposeProduceQtyMap.get(glueCode));
			BigDecimal surplusQty = produceQty.compareTo(planQty) > 0 ? produceQty.subtract(planQty) : BigDecimal.ZERO;
			if (surplusQty.compareTo(BigDecimal.ZERO) == 0) {
				continue;
			}

			// 有剩余量，则尝试从备选机台中选已有的排程记录并将剩余量安排进去
			List<FormulaMachineVo> formulaMachineList = glueMachineMap.get(glueCode);
			// 配方按照机台剩余产能排序，先分配更大产能的机台
			List<FormulaMachineVo> sortFormulaList = formulaMachineList.stream().sorted((o1, o2) -> {
				Long capacity1 = machineCapacityMap.getOrDefault(o1.getMachineCode(), 0L);
				Long capacity2 = machineCapacityMap.getOrDefault(o2.getMachineCode(), 0L);
				return Math.toIntExact((capacity2 - capacity1) % Integer.MAX_VALUE);
			}).collect(Collectors.toList());

			for (FormulaMachineVo formulaMachine : sortFormulaList) {
				if (surplusQty.compareTo(BigDecimal.ZERO) == 0) {
					break;
				}
				// 有剩余量，则检查是否可以将剩余量安排到机台上
				// 取出对应机台的产能数据
				String machineCode = formulaMachine.getMachineCode();
				// 剩余产能
				BigDecimal surplusCapacity = BigDecimalUtil.valueOfZero(machineCapacityMap.get(machineCode));
				if (surplusCapacity.compareTo(BigDecimal.ZERO) <= 0) {
					continue;
				}

				// 判断机台是否有足够的剩余产能
				// 先按机台产能从大到小，相同则按照添加到列表的顺序（如果按照排产优先级在满足夜班生产的小批量会导致掺胶配方优先级<ZZ配方）
				List<GlueScheduleResultVo> sortScheduleList = scheduleList.stream().sorted((o1, o2) -> {
							Long capacity1 = machineCapacityMap.getOrDefault(o1.getMachineCode(), 0L);
							Long capacity2 = machineCapacityMap.getOrDefault(o2.getMachineCode(), 0L);
							return Math.toIntExact((capacity2 - capacity1) % Integer.MAX_VALUE);
						})
						.collect(Collectors.toList());
				for (GlueScheduleResultVo schedule : sortScheduleList) {
					if (surplusQty.compareTo(BigDecimal.ZERO) == 0) {
						break;
					}
					// 一车胶消耗产能 = 配方炼胶时长 + 单车间隔时长
					Long intervalTime = mixingTimeMap.getOrDefault(GenerageMapKeyUtils.createMapKey(schedule.getGlue(), schedule.getMachineCode()), mixOntervalTime);
					BigDecimal maxPerCarTime = BigDecimalUtil
							.valueOf(schedule.getPmtRecipe().getSummerMixTime() + intervalTime);
					// 剩余产能实际可生产车数 = 剩余产能 / 每车消耗产能
					BigDecimal capacityPlanQty = surplusCapacity.divide(maxPerCarTime, 0, RoundingMode.DOWN);
					// 当前库存可生产车数
					BigDecimal stockPlanQty = this.caculateProductNumByStock(schedule.getPmtRecipe(), glueStock,
							surplusQty);

					// 实际可生产量，取产能可生产车数、库存可生产车数、剩余量的最小值
					BigDecimal productQty = BigDecimalUtil.least(capacityPlanQty, surplusQty, stockPlanQty);
					if (productQty.compareTo(BigDecimal.ZERO) <= 0) {
						continue;
					}
					// 将额外的计划量加到 计划量 以及 超限制计划量中
					schedule.setPlanQty(BigDecimalUtil.valueOfZero(schedule.getPlanQty()).add(productQty));
					schedule.setOverLimitQty(BigDecimalUtil.valueOfZero(schedule.getOverLimitQty()).add(productQty));
					// 剩余量扣除产能实际可生产数
					surplusQty = surplusQty.subtract(productQty);
					// 产能扣减已消耗产能
					BigDecimal consumeCapacity = productQty.multiply(maxPerCarTime);
					surplusCapacity = BigDecimalUtil.greatest(surplusCapacity.subtract(consumeCapacity),
							BigDecimal.ZERO);
					// 更新机台产能
					machineCapacityMap.put(machineCode, surplusCapacity.longValue());
				}
			}
		}
	}

	/**
	 * 计算当前库存可生产车数
	 * 
	 * @param recipe     配方
	 * @param glueStock  库存
	 * @param requireQty 需求数
	 * @return 可生产数
	 */
	private BigDecimal caculateProductNumByStock(MesPmtRecipeVo recipe, GlueScheduleStockPool glueStock,
			BigDecimal requireQty) {
		if (GlueEngineConstants.RECIPE_TYPE_ZZ.equals(recipe.getRecipeTypeName())) {
			return requireQty; // zz配方当可以生产全部
		}
		BigDecimal minCarNum = requireQty;
		List<MesPmtRecipeWeightVo> recipeWeightList = recipe.getRecipeWeightList();
		BigDecimal maxSetWeight = RecipeUtil.getMaxSetWeight(recipeWeightList); // 获取称重配方中最大的终炼母炼胶重量
		// 掺胶库存可生产车数
		// 遍历配方称重信息，检查其原料的库存是否足够
		for (MesPmtRecipeWeightVo recipeWeight : recipeWeightList) {
			BigDecimal setWeight = new BigDecimal(recipeWeight.getSetWeight().toString()); // 称重配方重量
			String glueCode = recipeWeight.getRecipeMaterialName();
			String majorType = recipeWeight.getMajorType(); // 物料类型
			String realMajorType = RecipeUtil.getMajorType(glueCode, majorType, setWeight, maxSetWeight); // 获取真正的物料类型
			// 只需要判断掺胶物料的库存
			if (this.checkMixMajorType(realMajorType)) {
				BigDecimal stockWeight = glueStock.getStockWeight(glueCode, majorType); // 库存重量
				if (setWeight == null || BigDecimal.ZERO.compareTo(setWeight) == 0) {
					setWeight = BigDecimal.ONE;// 如果重量为0或者空，则直接按1处理
				}
				stockWeight = stockWeight.add(glueStock.getReturnStockWeight(glueCode));// 加上返回率的量（非返回胶为0）
				BigDecimal carNum = stockWeight.divide(setWeight, 0, RoundingMode.DOWN); // 换算成可生产车数
				minCarNum = BigDecimalUtil.least(carNum, minCarNum); // 取最小车数与可生产车数的较小值
				// 当库存不足一车时，跳过本配方
				if (carNum.compareTo(BigDecimal.ZERO) == 0) {
					break;
				}
			}
		}
		return minCarNum;
	}

	/**
	 * 根据机台 + 排产情况初始化机台的产能列表
	 *
	 * @param baseScheduleResult 已排计划
	 * @param machineList        机台列表
	 * @param params             排产参数
	 * @param mixingTimeMap      胶料间隔时间
	 * @return
	 */
	private Map<String, Long> initMachineCapacityMap(List<GlueScheduleResultVo> baseScheduleResult,
			List<MixMachine> machineList, Map<String, String> params, Map<String, Long> mixingTimeMap) {
		// 各机台产能列表
		Map<String, Long> machineCapacityMap = new HashMap<>();
		Long dinnerTime = new Long(params.getOrDefault(GlueEngineConstants.DINNER_TIME, "0")) * 60; // 用餐时间
		BigDecimal mixOntervalTime = new BigDecimal(params.getOrDefault(GlueEngineConstants.MIX_INTERVAL_TIME, "0")); // 每一车的间隔时间
		BigDecimal scheduleSwitchTime = new BigDecimal(
				params.getOrDefault(GlueEngineConstants.SCHEDULE_SWITCH_TIME, "0")); // 每一个计划的切换时长
		// 各机台初始最大产能
		for (MixMachine machine : machineList) {
			String machineCode = machine.getMachineCode();
			// 计算开班班数
			int workClass = 0;
			if (ZltConstant.STATUS_ENABLE.equals(machine.getMidStatus())) {
				workClass++;
			}
			if (ZltConstant.STATUS_ENABLE.equals(machine.getNightStatus())) {
				workClass++;
			}
			// if (ZltConstant.STATUS_ENABLE.equals(machine.getDayStatus())) {
			// 	workClass++;
			// }

			// 单机台最大产能 = (每班时间 - 用餐时间) * 开启班数
			Long maxCapacity = (ShiftClassUtil.ONE_SHIFT_CLASS_TIME - dinnerTime) * workClass;
			// 先扣减原机台的产能
			Long surplusCapacity = machineCapacityMap.getOrDefault(machineCode, maxCapacity);
			machineCapacityMap.put(machineCode, surplusCapacity);
		}
		// 扣减已占用计划量
		for (GlueScheduleResultVo result : baseScheduleResult) {
			String machineCode = result.getMachineCode();
			BigDecimal planQty = Optional.ofNullable(result.getPlanQty()).orElse(BigDecimal.ZERO);
			if (result.getPmtRecipe() == null) {
				continue;
			}
			if (machineCapacityMap.containsKey(machineCode) && planQty.compareTo(BigDecimal.ZERO) > 0) {
				Long itemIntervalTime = mixingTimeMap.get(GenerageMapKeyUtils.createMapKey(result.getGlue(), result.getMachineCode()));
				BigDecimal maxPerCarTime = BigDecimalUtil.valueOf(result.getPmtRecipe().getSummerMixTime())
						.add(itemIntervalTime != null ? BigDecimal.valueOf(itemIntervalTime) : mixOntervalTime); // 一车胶消耗产能 = 配方炼胶时长+单车间隔时长
				BigDecimal consumeCapacity = maxPerCarTime.multiply(planQty).add(scheduleSwitchTime); // 生产完该计划需要消耗的产能
				BigDecimal capacity = BigDecimalUtil.valueOfZero(machineCapacityMap.get(machineCode));
				BigDecimal surplusCapacity = capacity.subtract(consumeCapacity);
				machineCapacityMap.put(machineCode,
						BigDecimalUtil.greatest(surplusCapacity, BigDecimal.ZERO).longValue());
			}
		}
		return machineCapacityMap;
	}

	/**
	 * 将因机台产能不足无法排产的计划移除掉
	 * 
	 * @param glueResultList
	 * @param totalRequireQty
	 * @param totalSurplusQty
	 */
	@Override
	public void removeNoSchedule(List<GlueScheduleResultVo> baseScheduleResult) {
		Map<String, List<GlueScheduleResultVo>> glueGroupingMap = baseScheduleResult.stream()
				.collect(Collectors.groupingBy(GlueScheduleResultVo::getGlue));
		for (List<GlueScheduleResultVo> glueResultList : glueGroupingMap.values()) {
			// 统计通过分解产生的计划共排了多少计划量
			List<GlueScheduleResultVo> removeList = new ArrayList<>(glueResultList);
			double decomposePlanQty = glueResultList.stream()
					.mapToDouble(s -> Optional.ofNullable(s.getTotalPlanQty()).orElse(0D)).sum();
			// 通过计划量确认需要保留的计划
			if (decomposePlanQty <= 0) {
				// 通过分解胶料产生的记录如果都因机台产能没排到机台上，则只保留一笔排产记录：优先保留zz的，如果没有zz的则保留任意一笔
				List<GlueScheduleResultVo> persistList = new ArrayList<>();
				if (glueResultList.stream().anyMatch(s -> checkRecipeTypeIsZZ(s))) {
					persistList.addAll(
							glueResultList.stream().filter(s -> checkRecipeTypeIsZZ(s)).collect(Collectors.toList()));
				} else {
					persistList.add(CollectionUtil.firstElement(glueResultList));
				}

				removeList.removeAll(persistList);
			} else {
				// 如果有任意一笔有计划量，移没有计划量的记录
				removeList = removeList.stream().filter(s -> s.getTotalPlanQty() == null || s.getTotalPlanQty() <= 0)
						.collect(Collectors.toList());
			}

			// 从排程列表中移除掉待删除的记录
			if (CollectionUtils.isNotEmpty(removeList)) {
				baseScheduleResult.removeAll(removeList);
			}
		}
	}

	/**
	 * 检查配方是否zz类
	 * 
	 * @param recipe 配方
	 * @return
	 */
	private boolean checkRecipeTypeIsZZ(GlueScheduleResultVo schedule) {
		return schedule.isDecomposeFlag()
				&& GlueEngineConstants.RECIPE_TYPE_ZZ.equals(schedule.getPmtRecipe().getRecipeTypeName());
	}

	/**
	 * 根据分解胶料生成排产计划
	 * 
	 * @param scheduleDate    排产日
	 * @param mixArea         密炼区
	 * @param decomposeList   胶料分解
	 * @param mesPmtRecipeMap 配方数据
	 * @param glueStock       库存
	 * @param tempGlueStock   库存中间数据，用于计算排产量
	 * @param batchNo         批次号
	 * @param params          排程参数设置
     * @param glueMachineMap  胶料可选机台
     * @param machineCapacityMap  机台剩余产能
     * @param mixingTimeMap  炼胶时长
	 * @return
	 */
	private List<GlueScheduleResultVo> buildScheduleResult(Date scheduleDate, String mixArea,
			List<GlueDecomposePlanVo> decomposeList, Map<CombinedMapKey, List<MesPmtRecipeVo>> mesPmtRecipeMap,
			GlueScheduleStockPool glueStock, GlueScheduleStockPool tempGlueStock, String batchNo,
			Map<String, String> params, Map<String, List<FormulaMachineVo>> glueMachineMap, 
			Map<String, Long> machineCapacityMap, Map<String, Long> mixingTimeMap) {
	    Map<String, Long> copyMachineCapacityMap = machineCapacityMap.entrySet().stream().collect(Collectors.toMap(Entry::getKey, Entry::getValue)); // 复制一份产能列表，仅用于选择配方时判断
		List<GlueScheduleResultVo> baseScheduleResult = new ArrayList<>(); // 排程结果列表
		Date dayClassEndTime = ShiftClassUtil.getShiftClassEndTime(scheduleDate, GlueEngineConstants.SHIFT_CLASS_NIGHT); // 白班结束时间
        BigDecimal mixOntervalTime = new BigDecimal(params.getOrDefault(GlueEngineConstants.MIX_INTERVAL_TIME, "0")); // 每一车的间隔时间
        BigDecimal scheduleSwitchTime = new BigDecimal(params.getOrDefault(GlueEngineConstants.SCHEDULE_SWITCH_TIME, "0")); // 每一个计划的切换时长
		for (GlueDecomposePlanVo decompose : decomposeList) {
			String glueCode = decompose.getGlue(); // 胶料编号
			String machineCode = decompose.getMachineCode(); // 胶料编号
			if (StringUtils.isEmpty(machineCode)) { // 没指定机台的，从剩余产能最多的候选机台上选择一个
			    // 从候选机台选择剩余产能最多的机台
			    List<FormulaMachineVo> machineList = glueMachineMap.get(glueCode);
			    if (CollectionUtils.isNotEmpty(machineList)) {
			        FormulaMachineVo machine = machineList.stream().max((m1, m2) -> {
			            Long capacity1 = copyMachineCapacityMap.get(m1.getMachineCode());
			            Long capacity2 = copyMachineCapacityMap.get(m2.getMachineCode());
			            int result = capacity1.compareTo(capacity2);
			            if (result != 0) {
			                return result;
			            }
			            result = m1.getMachineOrder().compareTo(m2.getMachineOrder());
			            return result;
			        }).orElse(null);
			        if (machine != null) {
			            machineCode = machine.getMachineCode();
			        }
			    }
			}
			
			if (StringUtils.isEmpty(machineCode)) { // 机台为空的，说明是已经发送跨区请求且确认接收的规格，直接跳过
				continue;
			}
			// 从分解计划取出所需生产量
			BigDecimal requireQty;
			if (decompose.getProduceQty() != null) {
				requireQty = new BigDecimal(decompose.getProduceQty().toString());
			} else {
				requireQty = BigDecimal.ZERO;
			}
			// 计划生产量为0，说明不用排产，直接跳过
			if (requireQty.compareTo(BigDecimal.ZERO) <= 0) {
				continue;
			}

			// 选择符合条件的配方
			List<MesPmtRecipeVo> recipeList = this.chooseRecipe(glueCode, machineCode, mesPmtRecipeMap, tempGlueStock,
					decompose.isDecomposeFlag());
			String majorType = decompose.getMajorType(); // 物料类型
			BigDecimal planedQty = BigDecimal.ZERO; // 已排计划量
			// 遍历配方
			for (MesPmtRecipeVo recipe : recipeList) {
				// 生成排程记录，并计算最小生产车数
				GlueScheduleResultVo scheduleResult = this.caculateMinProductCarNum(scheduleDate, recipe, tempGlueStock,
						requireQty, planedQty, params, glueMachineMap);
				BigDecimal productQty = scheduleResult.getPlanQty();
				// 最小车数大于0的时候，才可安排生产
				if (productQty.compareTo(BigDecimal.ZERO) <= 0) {
					continue;
				}
				BigDecimal stockQty = glueStock.getStockNum(glueCode, majorType);// 胶料库存
				// 构建排程记录
				this.initBaseScheduleProperties(scheduleResult);
				this.copyDecomposeProperties(scheduleResult, decompose); // 给胶料分解相关栏位赋值
				this.copyRecipeProperties(scheduleResult, recipe); // 给配方相关栏位赋值
				scheduleResult.setBatchNo(batchNo);
				scheduleResult.setOrderNo(this.createOrderNo(batchNo)); // 工单号
				scheduleResult.setScheduleDate(scheduleDate);
				scheduleResult.setMixArea(mixArea);
				scheduleResult.setMachineCode(machineCode); // 以机台选择逻辑选中的机台为准
				scheduleResult.setRequireQty(requireQty.doubleValue());
				scheduleResult.setTotalPlanQty(0D);
				scheduleResult.setStockQty(stockQty.doubleValue());
				scheduleResult.setSafeStockQty(glueStock.getSafeStock(glueCode).doubleValue());
				// 库存到期时间，取与白班结束时间比较的较小值
				Date stockValidTime = this.getEarliestValidTime(glueStock, recipe);
				if (stockValidTime == null) {
					stockValidTime = dayClassEndTime;
				} else {
					stockValidTime = stockValidTime.compareTo(dayClassEndTime) < 0 ? stockValidTime : dayClassEndTime;
				}
				scheduleResult.setStockValidTime(stockValidTime);

				baseScheduleResult.add(scheduleResult);

	            // 更新机台产能
	            Long itemIntervalTime = mixingTimeMap.get(GenerageMapKeyUtils.createMapKey(glueCode, machineCode));
	            BigDecimal maxPerCarTime = BigDecimalUtil.valueOf(recipe.getSummerMixTime())
	                    .add(itemIntervalTime != null ? BigDecimal.valueOf(itemIntervalTime) : mixOntervalTime); // 一车胶消耗产能 = 配方炼胶时长+单车间隔时长
	            BigDecimal consumeCapacity = maxPerCarTime.multiply(productQty).add(scheduleSwitchTime); // 生产完该计划需要消耗的产能
	            BigDecimal capacity = BigDecimalUtil.valueOfZero(copyMachineCapacityMap.get(machineCode));
	            BigDecimal surplusCapacity = capacity.subtract(consumeCapacity);
	            copyMachineCapacityMap.put(machineCode, BigDecimalUtil.greatest(surplusCapacity, BigDecimal.ZERO).longValue());
				
				// 记录已安排生产量
				planedQty = planedQty.add(productQty);
				if (planedQty.compareTo(requireQty) >= 0) {
					// 如果已经全部排完，则结束本胶料的排产
					break;
				}
			}
		}
		return baseScheduleResult;
	}

	/**
	 * 超出产能添加机台<br/>
	 * 检查每个机台每个胶料是否可生产完<br/>
	 * （1）、检查每个机台是否存在产能无法支撑所计划的情况<br/>
	 * 总产能 = 生产时间，每班8小时，每天3个班 = 8 * 60 * 60 * 3，如果有用餐时间，每班需要扣减30分钟<br/>
	 * 产能计算方式：计算每个排程每班的产能消耗 = 计划需求量 * （炼胶时间 + 间隔时间），计划需求量受最大排产数限制<br/>
	 * （2）、检查每个胶料是否可能超出最大排产限制<br/>
	 * 最大排产限制 = 每个班单规格最大排产数 * 班数<br/>
	 * 产能不足的计划，从配方机台关系中按优先级取出机台，并将需求量按顺序、按产能、按排产限制安排到机台上<br/>
	 * 需要选择配方，刷新机台产能<br/>
	 * 全部安排完后，如果还有剩余就放到剩余量中<br/>
	 *
	 * @param scheduleDate       排产日
	 * @param mixArea            密炼取
	 * @param baseScheduleResult 已排产记录
	 * @param glueMachineMap     各胶料备选机台
	 * @param glueStock          库存
	 * @param params             排程参数设置
	 * @param mixingTimeMap      胶料间隔时间
	 * @param machineCapacityMap 机台剩余产能
	 * @param machineSelectMap   胶料已选择的机台
	 * @return
	 */
	private List<GlueDecomposePlanVo> addMachineExcessCapacity(Date scheduleDate, String mixArea,
			List<GlueScheduleResultVo> baseScheduleResult, Map<String, List<FormulaMachineVo>> glueMachineMap,
			GlueScheduleStockPool glueStock, Map<String, String> params, Map<String, Long> mixingTimeMap,
															   Map<String, Long> machineCapacityMap,
															   Map<String, Set<String>> machineSelectMap) {
		// 保留优先级顺序
		LinkedHashMap<CombinedMapKey, List<GlueScheduleResultVo>> machineScheduleMap = new LinkedHashMap<>();
		for (GlueScheduleResultVo schedule : baseScheduleResult) {
			CombinedMapKey mapKey = CombinedMapKey.createKey(schedule.getMachineCode(), schedule.getGlue());
			List<GlueScheduleResultVo> list = machineScheduleMap.getOrDefault(mapKey, new ArrayList<>());
			list.add(schedule);
			machineScheduleMap.put(mapKey, list);
		}
		// Map<CombinedMapKey, List<GlueScheduleResultVo>> machineScheduleMap = baseScheduleResult.stream()
		// 		.sorted(this.createScheduleSorter()).collect(Collectors.groupingBy(
		// 				schedule -> CombinedMapKey.createKey(schedule.getMachineCode(), schedule.getGlue())));
		BigDecimal maxProductZLQty = new BigDecimal(params.getOrDefault(GlueEngineConstants.MAX_PRODUCT_QTY, "0")); // 单班最大排产数
		BigDecimal maxProductMLQty = Optional.ofNullable(params.get(GlueEngineConstants.MAX_PRODUCT_ML_QTY))
				.map(BigDecimal::new).orElse(maxProductZLQty); // 母炼胶单班最大排产数，如果参数没有配置则等于终炼胶的配置
		BigDecimal maxProductZLRate = new BigDecimal(params.getOrDefault(GlueEngineConstants.MAX_PRODUCT_RATE, "0")); // 单班可超出最大排产数比率
		BigDecimal maxProductMLRate = Optional.ofNullable(params.get(GlueEngineConstants.MAX_PRODUCT_ML_RATE))
				.map(BigDecimal::new).orElse(maxProductZLRate); // 母炼胶单班最大排产比率，如果参数没有配置则等于终炼胶的配置
		Long mixOntervalTime = new Long(params.getOrDefault(GlueEngineConstants.MIX_INTERVAL_TIME, "0")); // 每一车的间隔时间
		Long scheduleSwitchTime = new Long(params.getOrDefault(GlueEngineConstants.SCHEDULE_SWITCH_TIME, "0")); // 排程切换时长（秒）
		Long dinnerTime = new Long(params.getOrDefault(GlueEngineConstants.DINNER_TIME, "0")) * 60; // 用餐时间
		BigDecimal singleClassZLLimitPlanQty = maxProductZLQty.multiply(ONE_HUNDRED.add(maxProductZLRate))
				.divide(ONE_HUNDRED, 0, RoundingMode.UP); // 终胶单班最大排产数上限值
		BigDecimal singleClassMLLimitPlanQty = maxProductMLQty.multiply(ONE_HUNDRED.add(maxProductMLRate))
				.divide(ONE_HUNDRED, 0, RoundingMode.UP); // 母炼单班最大排产数上限值
		List<GlueDecomposePlanVo> newDecomposeList = new ArrayList<>();

		for (List<GlueScheduleResultVo> scheduleList : machineScheduleMap.values()) {
			GlueScheduleResultVo resultVo = CollectionUtil.firstElement(scheduleList);
			String machineCode = resultVo.getMachineCode();
			String glueCode = resultVo.getGlue();
			Integer machineOrder = resultVo.getMachineOrder();

			// 取出候选机台
			List<FormulaMachineVo> machineList = glueMachineMap.get(glueCode);
			if (CollectionUtil.isEmpty(machineList)) {
				continue;
			}

			// 根据是否母炼胶，确定使用哪个最大生产量配置进行计算
			boolean isMLGlue = GlueEngineConstants.MAJOR_TYPE_ML.equals(resultVo.getMajorType());
			BigDecimal singleClassLimitPlanQty = isMLGlue ? singleClassMLLimitPlanQty : singleClassZLLimitPlanQty;
			BigDecimal maxProductQty = isMLGlue ? maxProductMLQty : maxProductZLQty;

			// 计算开班班数
			int workClass = 0;
			FormulaMachineVo formulaMachine = machineList.stream()
					.filter(machine -> machine.getMachineCode() != null && machine.getMachineCode().equals(machineCode)).findAny()
					.orElse(null); // 取出配方机台
			if (formulaMachine != null) {
				workClass = (formulaMachine.getMidStatus() ? 1 : 0) + (formulaMachine.getNightStatus() ? 1 : 0);
						// + (formulaMachine.getDayStatus() ? 1 : 0);
			}
			// 单机台最大产能 = (每班时间 - 用餐时间) * 开启班数
			Long maxCapacity = (ShiftClassUtil.ONE_SHIFT_CLASS_TIME - dinnerTime) * workClass;

			// 总需求量
			BigDecimal totalSurplusQty = scheduleList.stream().map(GlueScheduleResultVo::getPlanQty)
					.reduce(BigDecimal.ZERO, BigDecimal::add);

			// 受单班排产限制后的最大排产量
			BigDecimal singleGlueLimitPlanQty = totalSurplusQty.compareTo(singleClassLimitPlanQty) > 0
					? maxProductQty.multiply(BigDecimalUtil.valueOf(workClass))
					: totalSurplusQty;
			for (GlueScheduleResultVo schedule : scheduleList) {
				// 原先分配的已排量
				BigDecimal oldPlanQty = schedule.getPlanQty();
				// 如果总剩余量大于单班最大数，则需要限制单班量为设置的量
				BigDecimal singleClassPlanQty = totalSurplusQty.compareTo(singleClassLimitPlanQty) > 0 ? maxProductQty
						: singleClassLimitPlanQty;
				// 一天最大可排产数，初始值 = 单班排产数 * 开启班数
				BigDecimal dayLimitPlanQty = singleClassPlanQty.multiply(new BigDecimal(workClass));
				// 一车胶消耗产能 = 配方炼胶时长 + 单车间隔时长（间隔时长优先取炼胶间隔表配置的）
				Long maxPerCarTime = schedule.getPmtRecipe().getSummerMixTime() + mixingTimeMap.getOrDefault(GenerageMapKeyUtils.createMapKey(schedule.getGlue(), schedule.getMachineCode()), mixOntervalTime);

				// 先扣减原机台的产能
				Long surplusCapacity = machineCapacityMap.getOrDefault(machineCode, maxCapacity);
				// 如果当前班制至少是两班制，而且目前机台产能已经达到了单班最大产能
				if (workClass >= 2 && (maxCapacity - surplusCapacity) >= ShiftClassUtil.ONE_SHIFT_CLASS_TIME) {
					// 跳过这个配方，选择其他机台，如果跳过了所有配方，重新计算剩余量时会补全这部分
					surplusCapacity = 0L;
					logService.record(glueCode + ":" + machineCode + "超过单班最大产能，先分配到其他机台");
				}
				// 剩余产能实际可生产车数 = 剩余产能 / 每车消耗产能
				Long dayCapacityPlanQty = new BigDecimal(surplusCapacity)
						.divide(new BigDecimal(maxPerCarTime), 0, RoundingMode.DOWN).longValue();
				// 一天可排产量 = 总剩余量、 单班限制排产量、一天最大可排产数、实际产能可排产数、原先分配的已排量 中的较小值
				Long dayPlanQty = BigDecimalUtil.least(totalSurplusQty.longValue(), singleGlueLimitPlanQty.longValue(),
						dayLimitPlanQty.longValue(), dayCapacityPlanQty, oldPlanQty.longValue());
				// 总剩余量需要扣减掉
				totalSurplusQty = totalSurplusQty.subtract(BigDecimalUtil.valueOf(dayPlanQty));
				// 排产限制剩余可排量也需要扣减
				singleGlueLimitPlanQty = singleGlueLimitPlanQty.subtract(BigDecimalUtil.valueOf(dayPlanQty));
				// 一天需消耗产能 = 一车炼胶时长 * 一天可排产量 + 排程切换时长
				Long dayConsumeCapacity = dayPlanQty * maxPerCarTime + scheduleSwitchTime;
				Long currentConsumeCapacity = BigDecimalUtil.least(dayConsumeCapacity, surplusCapacity); // 当日消耗产能
				machineCapacityMap.put(machineCode, surplusCapacity - currentConsumeCapacity); // 更新剩余产能
				schedule.setPlanQty(new BigDecimal(dayPlanQty)); // 重新设置排产量
				// 更新配方重量表物料对应的库存信息
				this.updateRecipeWeightStock(schedule, oldPlanQty, glueStock);
			}

			// 看原先的单机台是否能将剩余量全部排晚
			if (totalSurplusQty.compareTo(BigDecimal.ZERO) <= 0) {
				continue;
			}
			// 没法排完则需要额外增加机台
			if (machineOrder == null) {
				logService.record(glueCode + "需要安排额外机台，但无可选机台");
				continue;
			}
			Set<String> machineSelectSet = machineSelectMap.getOrDefault(glueCode, Collections.emptySet());
			// 选机台，跳过已经选择过的机台
			FormulaMachineVo machine = machineList.stream()
					.filter(m -> m.getMachineOrder() != null && !machineSelectSet.contains(m.getMachineCode()))
					.min(Comparator.comparing(FormulaMachineVo::getMachineOrder)).orElse(null);
			if (machine == null) {
				logService.record(glueCode + "需要安排额外机台，但无可选机台");
				continue;
			}
			logService.record(glueCode + "安排额外机台：" + machineNameMap.get(machine.getMachineCode()));

			// 将剩余量全部安排到下一个候选机台上
			GlueDecomposePlanVo decompose = new GlueDecomposePlanVo();
			decompose.setMachineCode(machine.getMachineCode());
			decompose.setGlue(glueCode);
			decompose.setPlanDate(scheduleDate);
			decompose.setMixArea(mixArea);
			decompose.setProduceQty(totalSurplusQty.doubleValue());
			decompose.setRequireQty(resultVo.getRequireQty());
			decompose.setUpGlue(resultVo.getUpGlue());
			decompose.setBatchNo(resultVo.getDecomposeBatchNo());
			decompose.setIsFinishing(resultVo.getIsFinishing());
			decompose.setMajorType(resultVo.getMajorType());
			decompose.setMachineOrder(machine.getMachineOrder());
			decompose.setDecomposeFlag(false); // 排程过程中新增的
			decompose.setDayFlag(resultVo.getDayFlag());
			newDecomposeList.add(decompose);
		}
		return newDecomposeList;
	}

	/**
	 * 创建排产列表排序方式
	 * 
	 * @return
	 */
	@Override
	public Comparator<GlueScheduleResultVo> createScheduleSorter() {
		return // 先按选配方优先级顺序排序
		Comparator.comparing(GlueScheduleResultVo::getRecipeOrder, Comparator.nullsLast(Comparator.naturalOrder()))
				// 再按机台排序
				.thenComparing(GlueScheduleResultVo::getMachineOrder, Comparator.nullsLast(Integer::compareTo))
				// 再按胶料优先级倒序排序
				.thenComparing(Comparator.comparing(GlueScheduleResultVo::getPriority, Comparator.reverseOrder()))
				// 最后按配方类型优先级倒序排序
				.thenComparing(new Comparator<GlueScheduleResultVo>() {
					@Override
					public int compare(GlueScheduleResultVo o1, GlueScheduleResultVo o2) {
						// 胶料优先级一致的情况下，按配方优先级倒序排序，数字越大优先级越高
						Integer recipePriority1 = RecipeUtil.getRecipeTypePriority(o1.getRecipeTypeName());
						Integer recipePriority2 = RecipeUtil.getRecipeTypePriority(o2.getRecipeTypeName());
						return recipePriority2.compareTo(recipePriority1);
					}
				});
	}

	/**
	 * 更新配方重量表物料对应的库存信息
	 * 
	 * @param schedule   排程记录
	 * @param oldPlanQty 旧计划量
	 * @param glueStock  库存信息
	 */
	private void updateRecipeWeightStock(GlueScheduleResultVo schedule, BigDecimal oldPlanQty,
			GlueScheduleStockPool glueStock) {
		// 如果计划量有更新，更新原料的库存
		if (oldPlanQty.compareTo(schedule.getPlanQty()) != 0) {
			MesPmtRecipeVo recipe = schedule.getPmtRecipe();
			List<MesPmtRecipeWeightVo> recipeWeightList = recipe.getRecipeWeightList(); // 配方重量
			BigDecimal maxSetWeight = RecipeUtil.getMaxSetWeight(recipeWeightList); // 获取称重配方中最大的终炼母炼胶重量
			// 计算差值，由于新计划量不可能比旧计划量大，所以新值一定比旧值小
			BigDecimal differentQty = oldPlanQty.subtract(schedule.getPlanQty());
			// 遍历配方称重信息，将差值换算成原料的库存后加上去
			for (MesPmtRecipeWeightVo recipeWeight : recipeWeightList) {
				BigDecimal setWeight = new BigDecimal(recipeWeight.getSetWeight().toString()); // 称重配方重量
				String weightGlueCode = recipeWeight.getRecipeMaterialName();
				String majorType = recipeWeight.getMajorType(); // 物料类型
				String realMajorType = RecipeUtil.getMajorType(weightGlueCode, majorType, setWeight, maxSetWeight); // 获取真正的物料类型
				// 只需要处理掺胶物料的库存
				if (this.checkMixMajorType(realMajorType)) {
					glueStock.addStockWeight(weightGlueCode, majorType, differentQty.multiply(setWeight));
				}
			}
		}
	}

	/**
	 * 将分厂未提报，但是不够安全库存的胶料，生成排程
	 * 
	 * @param scheduleDate 排产日期
	 * @param glueStock    库存信息
	 * @param mixArea      密炼区
	 * @param batchNo      批次号
	 * @param scheduleList 分厂提报的胶料
	 */
	@Override
	public List<GlueScheduleResultVo> createNoRequireSchedule(Date scheduleDate, GlueScheduleStockPool glueStock,
			String mixArea, String batchNo, List<GlueScheduleResultVo> scheduleList) {
		// 未提报胶料排程列表
		List<GlueScheduleResultVo> noRequireScheduleList = new ArrayList<>();
		// 获取所有已排胶料
		Set<String> glueSet = scheduleList.stream().map(GlueScheduleResultVo::getGlue).collect(Collectors.toSet());
		// 胶料分解批次号
		String decomposeBatchNo = scheduleList.stream().filter(schedule -> schedule.getDecomposeBatchNo() != null)
				.map(GlueScheduleResultVo::getDecomposeBatchNo).max(String::compareTo).orElse(null);

		Map<String, BigDecimal> safeStockMap = glueStock.getSafeStock();
		for (Entry<String, BigDecimal> entry : safeStockMap.entrySet()) {
			String glueCode = entry.getKey();
			// 先校验安全库存的胶料是否有提报，如果有则跳过
			if (glueSet.contains(glueCode)) {
				continue;
			}

			// 计算需求量 = 安全库存 - 合格胶库存
			BigDecimal salfStockNum = Optional.ofNullable(entry.getValue()).orElse(BigDecimal.ZERO); // 安全库存
			BigDecimal stockNum = glueStock.getQualifiedGlueStockNum(glueCode); // 合格胶库存
			BigDecimal requireQty = salfStockNum.subtract(stockNum);

			// 只有需求量大于0的才需要创建
			if (requireQty.compareTo(BigDecimal.ZERO) <= 0) {
				continue;
			}

			GlueScheduleResultVo scheduleResult = new GlueScheduleResultVo();
			this.initBaseScheduleProperties(scheduleResult);
			scheduleResult.setGlue(glueCode);
			scheduleResult.setRequireQty(requireQty.doubleValue());
			scheduleResult.setTotalPlanQty(0D);
			scheduleResult.setTotalSurplus(requireQty.doubleValue());
			scheduleResult.setBatchNo(batchNo);
			scheduleResult.setOrderNo(this.createOrderNo(batchNo)); // 工单号
			scheduleResult.setScheduleDate(scheduleDate);
			scheduleResult.setMixArea(mixArea);
			scheduleResult.setPlanQty(BigDecimal.ZERO);
			scheduleResult.setStockQty(stockNum.doubleValue());
			scheduleResult.setSafeStockQty(salfStockNum.doubleValue());
			scheduleResult.setIsFinishing("0");
			scheduleResult.setDecomposeBatchNo(decomposeBatchNo);
			noRequireScheduleList.add(scheduleResult);
		}
		return noRequireScheduleList;
	}

	/**
	 * 自动排产前插单的数据要排进来
	 * 
	 * @param resultList       根据胶料分解表生成的排产记录
	 * @param mesPmtRecipeList 配方信息
	 * @param scheduleDate     排产日
	 * @param mixArea          密炼区
	 * @param batchNo          批次号
	 * @return 是否有插单记录
	 */
	private boolean addInsertOrder(List<GlueScheduleResultVo> resultList, List<MesPmtRecipeVo> mesPmtRecipeList,
			Date scheduleDate, String mixArea, String batchNo) {
		// 读取当天的已下发数据（可能是插单）
		GlueScheduleResultVo scheduleParams = new GlueScheduleResultVo();
		scheduleParams.setMixArea(mixArea);
		scheduleParams.setScheduleDate(scheduleDate);
		scheduleParams.setPublishSuccessCount(1);
		List<GlueScheduleResultVo> releasedScheduleList = glueScheduleEngineBaseMapper
				.selectScheduleResult(scheduleParams);
		if (CollectionUtil.isEmpty(releasedScheduleList)) {
			return false;
		}

		// 加载有效的配方信息，配方按胶料 + 机台分组
		MesPmtRecipeVo recipeParams = new MesPmtRecipeVo();
		recipeParams.setMixArea(mixArea);
		recipeParams.setIsModify(true); // 获取插单时可选的配方
		recipeParams.setIsOnlySkip(true); // 只要被忽略的配方
		List<MesPmtRecipeVo> skipRecipeList = recipeEngineService.listGlueRecipe(recipeParams);
		List<MesPmtRecipeVo> allRecipeList = new ArrayList<>(mesPmtRecipeList);
		allRecipeList.addAll(skipRecipeList);

		// 按胶料 + 机台 + 配方类型对配方分组
		Map<CombinedMapKey, MesPmtRecipeVo> mesPmtRecipeMap = allRecipeList.stream().collect(Collectors.toMap(
				r -> CombinedMapKey.createKey(r.getRecipeMaterialName(), r.getRecipeEquipCode(), r.getRecipeType()),
				Function.identity(), (r1, r2) -> r1));

		// 排产数据按胶料 + 机台 + 配方类型分组
		Map<CombinedMapKey, GlueScheduleResultVo> matchMap = resultList.stream()
				.collect(Collectors.toMap(decompose -> CombinedMapKey.createKey(decompose.getGlue(),
						decompose.getMachineCode(), decompose.getRecipeType()), Function.identity()));
		// 胶料分解批次号
		String decomposeBatchNo = resultList.stream().filter(schedule -> schedule.getDecomposeBatchNo() != null)
				.map(GlueScheduleResultVo::getDecomposeBatchNo).max(String::compareTo).orElse(null);

		Map<String, Double> glueRequireMap = resultList.stream().collect(Collectors.groupingBy(
				GlueScheduleResultVo::getGlue, Collectors.summingDouble(GlueScheduleResultVo::getRequireQty)));
		/**
		 * 有插单排产的胶料
		 */
		Map<String, Double> modifyRequireMap = new HashMap<>();

		// 遍历已有的排程数据
		for (GlueScheduleResultVo schedule : releasedScheduleList) {
			String glueCode = schedule.getGlue();
			String machineCode = schedule.getMachineCode();
			CombinedMapKey matchKey = CombinedMapKey.createKey(glueCode, machineCode, schedule.getRecipeType());
			if (!matchMap.containsKey(matchKey)) {
				// 判断如果胶料分解里没有该胶料 + 机台 + 配方类型的组合，需要添加到待排产列表中
				MesPmtRecipeVo recipe = mesPmtRecipeMap.get(matchKey);
				if (recipe == null) {
					String machineName = machineNameMap.getOrDefault(machineCode, machineCode);
					throw new RuntimeException(MessageContent.getI18nMessage("ui.scheduleResult.noRecipt.detailed",
							glueCode, machineName));
				}
				// 计算总排产量，作为本次排产的计划量
				Double planQty = BigDecimalUtil.add(schedule.getDayPlanQty(), schedule.getMidPlanQty(),
						schedule.getNightPlanQty());
				schedule.setPlanQty(new BigDecimal(planQty.toString()));
				this.copyRecipeProperties(schedule, recipe);
				schedule.setStartShiftClass(GlueEngineConstants.SHIFT_CLASS_MID); // 可开始班次默认中班
				// 各班计划量归0，重新分配
				schedule.setMidPlanQty(0D);
				schedule.setNightPlanQty(0D);
				schedule.setDayPlanQty(0D);
				schedule.setTotalSurplus(0D);
				schedule.setProductedQty(BigDecimal.ZERO);
				schedule.setDecomposeBatchNo(decomposeBatchNo);
				schedule.setReleaseStatus(ZltConstant.WAIT_RELEASING);
				// 需求胶料加上新增的计划量
				Double oldRequireQty = glueRequireMap.getOrDefault(glueCode, 0D);
				Double newRequireQty = BigDecimalUtil.add(planQty, oldRequireQty);
				glueRequireMap.put(glueCode, newRequireQty);
				modifyRequireMap.put(glueCode, newRequireQty);

				resultList.add(schedule);
			} else {
				// 如果已经有，则只需要更新发布状态更新
				GlueScheduleResultVo matchSchedule = matchMap.get(matchKey);
				matchSchedule.setReleaseStatus(ZltConstant.WAIT_RELEASING);
				matchSchedule.setPublishSuccessCount(schedule.getPublishSuccessCount());
				matchSchedule.setNewestPublishTime(schedule.getNewestPublishTime());
				matchSchedule.setOrderNo(schedule.getOrderNo());
			}
		}

		// 将排程按胶料分好组
		Map<String, List<GlueScheduleResultVo>> glueGroupingMap = resultList.stream()
				.collect(Collectors.groupingBy(GlueScheduleResultVo::getGlue));
		// 循环刷新胶料需求量
		for (Entry<String, Double> entry : modifyRequireMap.entrySet()) {
			String modifyGlueCode = entry.getKey();
			Double requireQty = entry.getValue();
			List<GlueScheduleResultVo> glueGroupingList = glueGroupingMap.get(modifyGlueCode);
			for (GlueScheduleResultVo groupingSchedule : glueGroupingList) {
				groupingSchedule.setRequireQty(requireQty);
			}
		}
		return true;
	}

	/**
	 * 计算19点的预计库存
	 *
	 * @param mixArea                密炼区
	 * @param scheduleDate           排产日期
	 * @param params                 排产参数
	 * @param glueRecipeMap          胶料配方映射的胶料名称Map
	 * @param reserveGlueRecipeMap   胶料配方映射的反转白班计划量的Map
	 * @param deductYesterdayRequire 扣减昨日需求
	 * @return
	 */
	@Override
	public void caculate16pmEstimateStock(GlueScheduleStockPool glueStock, String mixArea, Date scheduleDate,
										  List<MesPmtRecipeVo> mesPmtRecipeList, Map<String, String> params,
										  Map<String, String> glueRecipeMap,
										  Map<String, String> reserveGlueRecipeMap,
										  boolean deductYesterdayRequire) {
		// 按胶料 + 机台 + 配方类型对配方分组
		Map<CombinedMapKey, MesPmtRecipeVo> mesPmtRecipeMap = mesPmtRecipeList.stream().collect(Collectors.toMap(
				r -> CombinedMapKey.createKey(r.getRecipeMaterialName(), r.getRecipeEquipCode(), r.getRecipeType()),
				Function.identity(), (r1, r2) -> r1));
		// 库存日期，排产日的上一天
		Date stockDate = DateUtils.addDays(scheduleDate, -1);

		// 查询上一天的排产数据
		GlueScheduleResultVo searchParams = new GlueScheduleResultVo();
		searchParams.setScheduleDate(stockDate);
		searchParams.setMixArea(mixArea);
		List<GlueScheduleResultVo> resultList = glueScheduleEngineBaseMapper.selectScheduleResult(searchParams);
		// 有白班计划量的胶料要将计划量作为库存算到19点库存中
		for (GlueScheduleResultVo result : resultList) {
			String glueCode = result.getGlue();
			// 如果是WA的ZZ配方 且 区分掺胶和纯胶的日用量 ，将掺胶转为纯胶
			String glueRecipeMapKey = result.getGlueRecipeMapKey();
			if (glueRecipeMap.containsKey(glueRecipeMapKey)) {
				glueCode = glueRecipeMap.get(glueRecipeMapKey);
			}

			// 如果是不区分纯胶和掺胶日用量，将纯胶转为掺胶
			if (reserveGlueRecipeMap.containsKey(glueCode)) {
				glueCode = reserveGlueRecipeMap.get(glueCode);
			}
			
			String machineCode = result.getMachineCode();
			String recipeType = result.getRecipeType();
			if (glueCode == null && machineCode == null && recipeType == null) {
				// 过滤掉异常的数据
				continue;
			}
			BigDecimal dayPlanQty = BigDecimalUtil.valueOfZero(result.getNightPlanQty()); // 取出白班的计划量
			if (dayPlanQty.compareTo(BigDecimal.ZERO) > 0) { // 如果中班有计划量，则直接增加到库存中
				// 根据胶料 + 机台 + 配方类型取出配方
				MesPmtRecipeVo recipe = mesPmtRecipeMap
						.get(CombinedMapKey.createKey(glueCode, machineCode, recipeType));
				if (recipe == null) {
					continue;
				}
				result.setPmtRecipe(recipe);
				glueStock.addStock(glueCode, recipe.getMajorType(), dayPlanQty);
				// 如果是塑料胶，还需要补充重量
				if (GlueEngineConstants.MAJOR_TYPE_SL.equals(recipe.getMajorType())) {
					glueStock.addStockWeight(glueCode, recipe.getMajorType(), BigDecimal.valueOf(BigDecimalUtil.mul(dayPlanQty.doubleValue(), recipe.getLotTotalWeight())));
				}
			}
		}
		// 扣减作为原料的母胶或掺胶库存
		for (GlueScheduleResultVo result : resultList) {
			// 根据胶料 + 机台 + 配方类型取出配方
			MesPmtRecipeVo recipe = result.getPmtRecipe();
			if (recipe == null) {
				continue;
			}
			BigDecimal dayPlanQty = BigDecimalUtil.valueOfZero(result.getNightPlanQty()); // 取出白班的计划量
			if (dayPlanQty.compareTo(BigDecimal.ZERO) > 0) { // 如果中班有计划量，则需要扣减掉作为原料的母胶或掺胶库存
				// 因为早班可能存在返回胶的配方，预计库存需要扣减这部分
				glueStock.subtractChildGlueStock(dayPlanQty, recipe, false);
			}
		}

		// 如果不扣减昨日的需求量，跳过后续逻辑
		if (!deductYesterdayRequire) {
			return;
		}

		// 查询上一天的汇总胶料需求量
		List<GlueCollectPlan> glueCollectPlanList = glueScheduleEngineBaseMapper.selectGlueCollectPlanList(stockDate, mixArea);
		// 终炼胶库存还需要扣减昨日日用量；（结果小于0以0计算）
		for (GlueCollectPlan decomposePlanVo : glueCollectPlanList) {
			String glueCode = decomposePlanVo.getGlue();
			// 昨日日用量
			Double nightPlanQty = decomposePlanVo.getTotalPlanQty();
			// if (BigDecimalUtil.add(decomposePlanVo.getDayPlanQty(), decomposePlanVo.getNightPlanQty()) <= 0) {
			// 	nightPlanQty = decomposePlanVo.getTotalPlanQty();
			// }
			BigDecimal planQty = nightPlanQty != null ? BigDecimalUtil.valueOf(nightPlanQty) : BigDecimal.ZERO;
			// 有待支领量，则需要扣减掉对应的库存
			if (planQty.compareTo(BigDecimal.ZERO) > 0) {
				glueStock.subtractStock(glueCode, GlueEngineConstants.MAJOR_TYPE_ZL, planQty); // 扣减待支领量，固定是终炼胶
			}
		}
	}

	/**
	 * 将配方的排产相关信息拷贝至排程记录中
	 * 
	 * @param scheduleResult 胶料排程记录
	 * @param recipe         配方
	 * @return
	 */
	@Override
	public void copyRecipeProperties(GlueScheduleResultVo scheduleResult, MesPmtRecipeVo recipe) {
		scheduleResult.setSapCode(recipe.getSapMaterialCode());
		scheduleResult.setRecipeMaterialCode(recipe.getRecipeMaterialCode());
		scheduleResult.setRecipeType(recipe.getRecipeType());
		scheduleResult.setRecipeTypeName(recipe.getRecipeTypeName());
		scheduleResult.setRecipeVersionId(recipe.getRecipeVersionId());
		scheduleResult.setRecipeStage(recipe.getProductStage());
		scheduleResult.setFormulaWeight(recipe.getLotTotalWeight());
		scheduleResult.setFormulaTime((double) recipe.getSummerMixTime().longValue());
		scheduleResult.setMajorType(recipe.getMajorType());
		scheduleResult.setPmtRecipe(recipe);
	}

	/**
	 * 初始化胶料排程记录的基本字段
	 * 
	 * @param scheduleResult 胶料排程记录
	 * @return
	 */
	@Override
	public void initBaseScheduleProperties(GlueScheduleResultVo scheduleResult) {
		scheduleResult.setTotalPlanQty(0D);
		scheduleResult.setTotalSurplus(0D);
		scheduleResult.setReleaseStatus(ZltConstant.NO_RELEASE);
		scheduleResult.setDataSource(ZltConstant.GLUE_SCHEDULE_SOURCE_AUTO);
		scheduleResult.setProductedQty(BigDecimal.ZERO);
		scheduleResult.setStartShiftClass(GlueEngineConstants.SHIFT_CLASS_MID); // 可开始班次默认中班
		scheduleResult.setPublishSuccessCount(0);
		scheduleResult.setMidPlanQty(0D);
		scheduleResult.setMidProduceOrder(null);
		scheduleResult.setMidExpectStartTime(null);
		scheduleResult.setMidExpectFinishTime(null);
		scheduleResult.setNightPlanQty(0D);
		scheduleResult.setNightProduceOrder(null);
		scheduleResult.setNightExpectStartTime(null);
		scheduleResult.setNightExpectFinishTime(null);
		scheduleResult.setDayPlanQty(0D);
		scheduleResult.setDayProduceOrder(null);
		scheduleResult.setDayExpectStartTime(null);
		scheduleResult.setDayExpectFinishTime(null);
		scheduleResult.setBaseValue(null);
	}

	/**
	 * 将分解记录的排产相关信息拷贝至排程记录中
	 * 
	 * @param scheduleResult 胶料排程记录
	 * @param decompose      胶料分解记录
	 * @return
	 */
	private void copyDecomposeProperties(GlueScheduleResultVo scheduleResult, GlueDecomposePlanVo decompose) {
		scheduleResult.setGlue(decompose.getGlue());
		scheduleResult.setUpGlue(decompose.getUpGlue());
		scheduleResult.setMachineCode(decompose.getMachineCode());
		scheduleResult.setIsFinishing(decompose.getIsFinishing());
		scheduleResult.setDecomposeBatchNo(decompose.getBatchNo());
		scheduleResult.setMachineOrder(decompose.getMachineOrder());
		scheduleResult.setDecomposeFlag(decompose.isDecomposeFlag());
		scheduleResult.setDayFlag(decompose.getDayFlag());
		// 选配方优先级
		scheduleResult.setRecipeOrder(decompose.getRecipeOrder());
	}

	/**
	 * 抓取当天所有的胶料分解记录
	 *
	 * @param scheduleDate             排产日
	 * @param mixArea                  密炼区
	 * @param glueMachineMap           各胶料备选机台
	 * @param latestScheduleList       昨日早班的最后一个排程计划
	 * @param mixingPriorityProductMap 炼胶优先配置
	 * @param mesPmtRecipeMap          有效的配方信息，配方按胶料 + 机台分组
	 * @param glueRecipeOnlyGlueMap    胶料配方映射的胶料映射Map
	 * @return
	 */
	private List<GlueDecomposePlanVo> listGlueDecomposePlan(Date scheduleDate, String mixArea,
															Map<String, List<FormulaMachineVo>> glueMachineMap,
															List<GlueScheduleResultVo> latestScheduleList,
															Map<String, String> mixingPriorityProductMap,
															Map<CombinedMapKey, List<MesPmtRecipeVo>> mesPmtRecipeMap,
															Map<String, GlueCollectPlan> collectPlanMap,
															GlueScheduleStockPool glueStock,
															Map<String, String> glueRecipeOnlyGlueMap) {
		// 获取排产日的胶料分解表
		List<GlueDecomposePlanVo> decomposeList = glueScheduleEngineBaseMapper.selectGlueDecomposePlan(scheduleDate,
				mixArea, null);
		
		// 按胶料合并去重，确保一个胶料只会有一笔记录
//		Collection<GlueDecomposePlanVo> distinctDecomposeList = decomposeList.stream()
//				.collect(Collectors.groupingBy(GlueDecomposePlanVo::getGlue, // 按胶料分组
//						Collectors.collectingAndThen(Collectors.toList(), gruopingList -> {
//							GlueDecomposePlanVo glueDecompose = CollectionUtil.firstElement(gruopingList);
//							if (gruopingList.size() == 0) {
//								return glueDecompose;
//							}
//							// 重复胶料的记录只保留一个，计划量合并到一起
//							Double produceQty = gruopingList.stream().filter(v -> v.getProduceQty() != null)
//									.mapToDouble(GlueDecomposePlanVo::getProduceQty).sum();
//							glueDecompose.setProduceQty(produceQty);
//							return glueDecompose;
//						})))
//				.values();
//		// 检查是否有经过去重逻辑移除部分记录，有的话需要以去重后的为准
//		if (CollectionUtils.isNotEmpty(distinctDecomposeList) && distinctDecomposeList.size() != decomposeList.size()) {
//			decomposeList = new ArrayList<>(distinctDecomposeList);
//		}
//
//		Map<String, GlueDecomposePlanVo> glueMap = decomposeList.stream()
//				.collect(Collectors.toMap(GlueDecomposePlanVo::getGlue, Function.identity(), (d1, d2) -> d2)); // 按胶料汇总

		// 构建胶料分解的树层级
		// Map<String, List<GlueDecomposePlanVo>> upGlueDecomposeMap = decomposeList.stream()
		// 		// 去掉所有根节点
		// 		.filter(decompose -> decompose.getUpGlue() != null)
		// 		// 按父胶料进行分组
		// 		.collect(Collectors.groupingBy(GlueDecomposePlanVo::getUpGlue));

		// 昨日早班最晚生产的机台
		Map<String, String> latestMachineMap = latestScheduleList.stream()
				.filter(v -> StringUtils.isNotBlank(v.getGlue()) && StringUtils.isNotBlank(v.getMachineCode()))
				.collect(Collectors.toMap(GlueScheduleResult::getGlue, GlueScheduleResult::getMachineCode, (v1, v2) -> v1));
		// 优先生产的胶料
		Set<String> priorityGlueSet = new HashSet<>(mixingPriorityProductMap.values());
		// 优先生产的胶料之后的连续生产的胶料
		Set<String> continueGlueSet = mixingPriorityProductMap.keySet();

		// 假设根据ZZ配方定义虚拟的排程列表，计算分厂需求
		List<GlueScheduleResultVo> baseScheduleResult = new ArrayList<>();
		Map<String, String> fixMachineGlue = new HashMap<>(); // 固定机台的胶料与机台关系
		
		// 遍历每个胶料，在分解胶料父胶料map中抓取所有其子胶料，存放与children中
		for (GlueDecomposePlanVo decompose : decomposeList) {
			decompose.setDecomposeFlag(true); // 默认是从胶料分解来的计划
			decompose.setFixedMachineFlag(false);

			String glue = decompose.getGlue();
			String machineCode = latestMachineMap.get(glue);
			List<FormulaMachineVo> machineList = glueMachineMap.get(glue);
			// 如果需要提前选用的机台状态不为可用的，应该忽略
			if (StringUtils.isNotBlank(machineCode)
					&& machineList != null
					&& machineList.stream().anyMatch(machine -> machineCode.equals(machine.getMachineCode()) && machine.getMachineOrder() != null)) {
			    if (this.isDay1(decompose) || this.isDay2(decompose) && fixMachineGlue.containsKey(glue)) { // 如果是次日需求，则先看是否有当日需求，没有的话才限定机台
			        // 昨日白班生产的胶料最优先选择机台和配方
			        decompose.setRecipeOrder(100D);
			        // 切换机台为昨日白班最后生产的机台
			        decompose.setMachineCode(machineCode);
			        fixMachineGlue.put(glue, machineCode);
			        decompose.setFixedMachineFlag(true);
			        logService.record(glue + ":" + machineCode + "接续昨日白班计划，优先选择机台");
			    }
			    
				
			} else if (priorityGlueSet.contains(glue)) {
				// 炼胶优先配置的胶料第二优先选择配方
				decompose.setRecipeOrder(200D);
				
			} else if (continueGlueSet.contains(glue)) {
				// 优先生产的胶料之后的连续生产的胶料第三优先选择配方和机台
				decompose.setRecipeOrder(300D);
				// 如果对应优先炼的胶料是昨日白班排程，尽量先选择相同机台，如果没有对应机台配方除外，机台状态不可用的除外
				String priorityGlue = mixingPriorityProductMap.get(glue);
				String priorityMachineCode = latestMachineMap.get(priorityGlue);
				List<FormulaMachineVo> priorityMachineList = glueMachineMap.get(priorityGlue);
				if (StringUtils.isNotBlank(priorityGlue)
						&& StringUtils.isNotBlank(priorityMachineCode)
						&& mesPmtRecipeMap.containsKey(CombinedMapKey.createKey(glue, priorityMachineCode))
						&& priorityMachineList != null
						&& priorityMachineList.stream().anyMatch(machine -> priorityMachineCode.equals(machine.getMachineCode()) && machine.getMachineOrder() != null)
						&& machineList != null
						&& machineList.stream().anyMatch(machine -> priorityMachineCode.equals(machine.getMachineCode()) && machine.getMachineOrder() != null)
				) {
				    if (this.isDay1(decompose) || this.isDay2(decompose) && fixMachineGlue.containsKey(glue)) { // 如果是次日需求，则先看是否有当日需求，没有的话才限定机台
    					decompose.setMachineCode(priorityMachineCode);
                        fixMachineGlue.put(glue, machineCode);
    	                decompose.setFixedMachineFlag(true);
    					logService.record(glue + ":" + priorityMachineCode + "优先胶料接续昨日白班计划，优先选择机台");
				    }
				}
			}

			// 找对应的ZZ的配方，构建临时的排程记录，计算分厂需求量
			List<MesPmtRecipeVo> recipeVoList = mesPmtRecipeMap.get(CombinedMapKey.createKey(decompose.getGlue(), decompose.getMachineCode()));
			if (CollectionUtils.isNotEmpty(recipeVoList)) {
				Optional<MesPmtRecipeVo> zzRecipeOpt = recipeVoList.stream().filter(v -> GlueEngineConstants.RECIPE_TYPE_ZZ.equals(v.getRecipeTypeName())).findFirst();
				if (zzRecipeOpt.isPresent()) {
					MesPmtRecipeVo recipeVo = zzRecipeOpt.get();
					GlueScheduleResultVo itemResultVo = new GlueScheduleResultVo();
					itemResultVo.setGlue(decompose.getGlue());
					itemResultVo.setMajorType(recipeVo.getMajorType());
					itemResultVo.setMachineCode(decompose.getMachineCode());
					itemResultVo.setPmtRecipe(recipeVo);
					baseScheduleResult.add(itemResultVo);
				}
			}

			// 仅解析上级胶记录
//			if (StringUtils.isNotBlank(decompose.getUpGlue())) {
//				List<String> upGlueList = Arrays.stream(decompose.getUpGlue().split(",")).filter(StringUtils::isNotBlank).distinct().collect(Collectors.toList());
//				for (String upGlue : upGlueList) {
//					GlueDecomposePlanVo upGlueDecomposePlan = glueMap.get(upGlue);
//					if (upGlueDecomposePlan != null) {
//						upGlueDecomposePlan.getChildren().add(decompose);
//						decompose.getFather().add(upGlueDecomposePlan);
//					}
//				}
//			}

			// 设置机台顺序
			if (!CollectionUtil.isEmpty(machineList)) {
				Integer machineOrder = machineList.stream()
						.filter(machine -> decompose.getMachineCode().equals(machine.getMachineCode())
								&& machine.getMachineOrder() != null)
						.map(FormulaMachineVo::getMachineOrder).findAny().orElse(null);
				decompose.setMachineOrder(machineOrder);
			}
		}

		Map<String, GlueDecomposePlanVo> decompostMap = decomposeList.stream().filter(v -> StringUtils.isNotBlank(v.getGlue()))
				.collect(Collectors.toMap(GlueDecomposePlan::getGlue, Function.identity(), (v1, v2) -> v1));
		// 计算分厂需求
		Map<String, GlueFactoryRequireVo> factoryRequireMap = this.buildGlueFactoryRequire(scheduleDate, mixArea, baseScheduleResult, glueStock,
				collectPlanMap, glueRecipeOnlyGlueMap);
		// 找出夜班需求含昨日日用量部分
		List<GlueFactoryRequireVo> yesterdayRequireList = factoryRequireMap.values().stream()
				.filter(v -> v.getRequireClass() != null && v.getRequireClass() == GlueEngineConstants.SHIFT_CLASS_MID && v.getRequireDifference() != null)
				.collect(Collectors.toList());
		// 找出夜班需求不含昨日日用量部分
		List<GlueFactoryRequireVo> nightRequireList = factoryRequireMap.values().stream()
				.filter(v -> v.getRequireClass() != null && v.getRequireClass() == GlueEngineConstants.SHIFT_CLASS_NIGHT && v.getRequireDifference() != null)
				.collect(Collectors.toList());
		
		// 将当天需求计划关联到分解计划中
		for (GlueDecomposePlanVo decompose: decomposeList) {
		    BigDecimal planQty = BigDecimal.ZERO;
		    GlueCollectPlan collectPlan = collectPlanMap.get(decompose.getGlue());
		    if (collectPlan != null) {
		        planQty = BigDecimalUtil.valueOf(collectPlan.getTotalPlanQty());
		    }
		    decompose.setRequireQty(planQty.doubleValue());
		    if (!decompose.getFixedMachineFlag()) {
		        decompose.setMachineCode(null);// TODO 非固定机台的规格，先清空机台，以后续的产能分配为准
		    }
		}

		// 如果有夜班需求，优先选中配方和占用机台产能
		double requireStartOrder = 10000D;
		requireStartOrder = computeRecipeOrderByRequire(yesterdayRequireList, decompostMap, requireStartOrder);
		requireStartOrder = computeRecipeOrderByRequire(nightRequireList, decompostMap, requireStartOrder);

		// 调整配方的优先选择顺序
		return decomposeList.stream()
				.sorted(Comparator.comparing(GlueDecomposePlanVo::getRecipeOrder,Comparator.nullsLast(Comparator.naturalOrder())))
				.collect(Collectors.toList());
	}
	
	/**
	 * 判断是否第一天的需求计划
	 * @param decompose
	 * @return
	 */
	private boolean isDay1(GlueDecomposePlanVo decompose) {
	    return ProductDayFlagEnum.DAY1.getCode().equals(decompose.getDayFlag());
	}
	
    /**
     * 判断是否第二天的需求计划
     * @param decompose
     * @return
     */
    private boolean isDay2(GlueDecomposePlanVo decompose) {
        return ProductDayFlagEnum.DAY2.getCode().equals(decompose.getDayFlag());
    }

	/**
	 * 根据需求量计算选配方优先级
	 * @param requireList 需求量
	 * @param decompostMap 分解映射
	 * @param requireStartOrder 开始的需求量
	 * @return 最大的配方优先级
	 */
	private double computeRecipeOrderByRequire(List<GlueFactoryRequireVo> requireList, Map<String, GlueDecomposePlanVo> decompostMap, double requireStartOrder) {
		double maxRecipeOrder = requireStartOrder;

		for (GlueFactoryRequireVo factoryRequireVo : requireList) {
			GlueDecomposePlanVo decomposePlanVo = decompostMap.get(factoryRequireVo.getGlue());
			double diff = factoryRequireVo.getRequireDifference().doubleValue();
			double newRecipeOrder = BigDecimalUtil.add(diff, requireStartOrder);
			maxRecipeOrder = Math.max(maxRecipeOrder, newRecipeOrder);
			if (decomposePlanVo != null) {
				Double recipeOrder = decomposePlanVo.getRecipeOrder();
				if (recipeOrder == null || recipeOrder > newRecipeOrder) {
					decomposePlanVo.setRecipeOrder(newRecipeOrder);
				}
			}
		}

		return maxRecipeOrder;
	}

	/**
	 * 
	 * 根据指定条件选择配方<br/>
	 * 1、zz类型的配方<br/>
	 * 2、最后一段母炼胶的所有配方<br/>
	 * 3、其他配方则选择原料库存充足的配方
	 * 
	 * @param glueCode        胶料
	 * @param machineCode     机台
	 * @param mesPmtRecipeMap 配方集合
	 * @param glueStock       胶料库存
	 * @param isDecompose     是否分解胶料产生的
	 * @param params          排程参数设置
	 * @return
	 */
	public List<MesPmtRecipeVo> chooseRecipe(String glueCode, String machineCode,
			Map<CombinedMapKey, List<MesPmtRecipeVo>> mesPmtRecipeMap, GlueScheduleStockPool glueStock,
			boolean isDecompose) {
		// 取出配方
		List<MesPmtRecipeVo> recipeList = new ArrayList<>(); // 符合条件的配方
		String noStockMixType = null; // 缺少库存的掺胶类型
		List<MesPmtRecipeVo> baserecipeList = mesPmtRecipeMap.get(CombinedMapKey.createKey(glueCode, machineCode));
		if (CollectionUtils.isNotEmpty(baserecipeList)) {
			// 取出匹配的配方，且要将配方按优先级由高到低排好序
			for (MesPmtRecipeVo recipe : baserecipeList) {
				if (this.checkRecipeStock(recipe, glueStock)) { // 符合条件的，直接加到列表中
					recipeList.add(recipe);
				} else { // 不符合条件的，把掺胶类型取出来
					noStockMixType = this.getNoStockGlueMajorType(recipe, glueStock, noStockMixType);
				}
			}
		}
		// 此时优先级暂不考虑根据昨日白班的机台配方，如果有返回胶，还是优先考虑返回胶的配方
		recipeList = recipeList.stream()
				// 按优先级重新排序
				.sorted(this.createRecipeSorter(glueStock)).collect(Collectors.toList());

		if (CollectionUtil.isEmpty(recipeList)) {
			if (isDecompose) { // 如果是分解胶料产生的计划，则需要给用户提示错误信息
				this.returnNoRecipeError(glueCode, machineCode, noStockMixType);
			} else { // 如果不是分解胶料产生的计划，说明是备用机台的，则只需要跳过即可
				return new ArrayList<>(0);
			}
		}
		// 胶料配方的物料信息、配方类型信息
		for (int i = recipeList.size() - 1; i >= 0; i--) {
			MesPmtRecipeVo recipe = recipeList.get(i);
			if (recipe.getMajorType() == null) {
				if (isDecompose) { // 如果是分解胶料产生的计划，则必须要校验通过
					throw new RuntimeException(MessageContent.getI18nMessage("ui.scheduleResult.noMaterial", glueCode));
				} else { // 如果不是分解胶料产生的计划，说明是备用机台的，则只需要跳过即可
					recipeList.remove(i);
				}
			}
		}
		return recipeList;
	}

	/**
	 * 取出无库存胶料的物料类型
	 * 
	 * @param recipe         配方
	 * @param glueStock      胶料库存
	 * @param defaultMixType 默认掺胶类型，找不到的时候直接返回这个
	 * @return
	 */
	private String getNoStockGlueMajorType(MesPmtRecipeVo recipe, GlueScheduleStockPool glueStock,
			String defaultMixType) {
		List<MesPmtRecipeWeightVo> recipeWeightList = recipe.getRecipeWeightList(); // 配方重量
		if (CollectionUtils.isEmpty(recipeWeightList)) {
			return defaultMixType;
		}
		BigDecimal maxSetWeight = RecipeUtil.getMaxSetWeight(recipeWeightList); // 获取称重配方中最大的终炼母炼胶重量
		// 取出库存不足的掺胶类型
		for (MesPmtRecipeWeightVo recipteWeight : recipeWeightList) {
			String weightGlueCode = recipteWeight.getRecipeMaterialName();
			String majorType = recipteWeight.getMajorType(); // 物料大类
			BigDecimal setWeight = BigDecimalUtil.valueOfZero(recipteWeight.getSetWeight());
			String realMajorType = RecipeUtil.getMajorType(weightGlueCode, majorType, setWeight, maxSetWeight); // 获取真正的物料类型
			// 先判断物料大类，排程涉及到的类型才需要处理：只有掺胶的需要看库存重量，母炼胶不用
			if (this.checkMixMajorType(realMajorType)) {
				// 取出库存信息
				BigDecimal stockWeight = glueStock.getStockWeight(weightGlueCode, majorType);
				if (stockWeight.compareTo(BigDecimal.ZERO) <= 0) {
					return realMajorType;
				}
			}
		}
		return defaultMixType;
	}

	/**
	 * 返回无配方异常错误
	 * 
	 * @param glueCode       胶料
	 * @param machineCode    机台
	 * @param noStockMixType 无库存掺胶类型
	 */
	private void returnNoRecipeError(String glueCode, String machineCode, String noStockMixType) {
		String machineName = machineNameMap.getOrDefault(machineCode, machineCode);
		if (StringUtils.isNotEmpty(noStockMixType)) { // 因掺胶库存不足无法排产的情况，则根据掺胶类型提示对应信息
			String noStockMixTypeName;
			switch (noStockMixType) {
			case GlueEngineConstants.MAJOR_TYPE_FH:
				noStockMixTypeName = MessageContent.getI18nMessage("ui.scheduleResult.returnGlue");
				break;
			case GlueEngineConstants.MAJOR_TYPE_BHG:
				noStockMixTypeName = MessageContent.getI18nMessage("ui.scheduleResult.unqualifiedGlue");
				break;
			case GlueEngineConstants.MAJOR_TYPE_WASH:
				noStockMixTypeName = MessageContent.getI18nMessage("ui.scheduleResult.washGlue");
				break;
			default:
				noStockMixTypeName = MessageContent.getI18nMessage("ui.scheduleResult.mixGlue");
				break;
			}
			throw new RuntimeException(MessageContent.getI18nMessage("ui.scheduleResult.noMixGlueStock", glueCode,
					machineName, noStockMixTypeName));
		} else { // 没有掺胶库存不足情况，则必定是没有可用配方
			throw new RuntimeException(
					MessageContent.getI18nMessage("ui.scheduleResult.noRecipt.detailed", glueCode, machineName));
		}
	}

	/**
	 * 计算使用指定配方后的最小生产车数
	 * 
	 * @param recipe         配方
	 * @param glueStock      库存列表
	 * @param requireQty     总计划量
	 * @param planedQty      已排产量
	 * @param params         排程参数设置
	 * @param glueMachineMap 各胶料备选机台
	 * @return
	 */
	private GlueScheduleResultVo caculateMinProductCarNum(Date scheduleDate, MesPmtRecipeVo recipe,
			GlueScheduleStockPool glueStock, BigDecimal requireQty, BigDecimal planedQty, Map<String, String> params,
			Map<String, List<FormulaMachineVo>> glueMachineMap) {
		List<MesPmtRecipeWeightVo> recipeWeightList = recipe.getRecipeWeightList(); // 配方重量
		String upGlueCode = recipe.getRecipeMaterialName(); // 物料编号
		String machineCode = recipe.getRecipeEquipCode();
		BigDecimal minCarNum = requireQty.subtract(planedQty); // 最小生产车数，初始值为未安排生产的计划量
		GlueScheduleResultVo scheduleResult = new GlueScheduleResultVo();
		scheduleResult.setPlanQty(BigDecimal.ZERO);
		// 遍历到“正正”配方时，不需要校验配方库存，直接加计划
		if (GlueEngineConstants.RECIPE_TYPE_ZZ.equals(recipe.getRecipeTypeName())) {
			scheduleResult.setPlanQty(minCarNum);
			return scheduleResult;
		}
		BigDecimal maxProductZLQty = new BigDecimal(params.getOrDefault(GlueEngineConstants.MAX_PRODUCT_QTY, "0")); // 单班最大排产数
		BigDecimal maxProductMLQty = Optional.ofNullable(params.get(GlueEngineConstants.MAX_PRODUCT_ML_QTY))
				.map(BigDecimal::new).orElse(maxProductZLQty); // 母炼胶单班最大排产数，如果参数没有配置则等于终炼胶的配置
		BigDecimal maxProductZLRate = new BigDecimal(params.getOrDefault(GlueEngineConstants.MAX_PRODUCT_RATE, "0")); // 单班可超出最大排产数比率
		BigDecimal maxProductMLRate = Optional.ofNullable(params.get(GlueEngineConstants.MAX_PRODUCT_ML_RATE))
				.map(BigDecimal::new).orElse(maxProductZLRate); // 母炼胶单班最大排产比率，如果参数没有配置则等于终炼胶的配置
		BigDecimal singleClassZLLimitPlanQty = maxProductZLQty.multiply(ONE_HUNDRED.add(maxProductZLRate)).divide(ONE_HUNDRED,
				0, RoundingMode.UP);// 终炼单班最大排产数上限值
		BigDecimal singleClassMLLimitPlanQty = maxProductMLQty.multiply(ONE_HUNDRED.add(maxProductMLRate))
				.divide(ONE_HUNDRED, 0, RoundingMode.UP); // 母炼单班最大排产数上限值
		
		// 根据是否母炼胶，确定使用哪个最大生产量配置进行计算
		boolean isMLGlue = GlueEngineConstants.MAJOR_TYPE_ML.equals(recipe.getMajorType());
		BigDecimal singleClassLimitPlanQty = isMLGlue ? singleClassMLLimitPlanQty : singleClassZLLimitPlanQty;
		BigDecimal maxProductQty = isMLGlue ? maxProductMLQty : maxProductZLQty;
		
		BigDecimal singleClassPlanQty = minCarNum.compareTo(singleClassLimitPlanQty) > 0 ? maxProductQty
				: singleClassLimitPlanQty;

		// 获取胶料机台对应信息，用于计算具体可排产班次
		FormulaMachineVo machine = null;
		List<FormulaMachineVo> machineList = glueMachineMap.get(upGlueCode);
		if (CollectionUtils.isNotEmpty(machineList)) {
			machine = machineList.stream().filter(m -> Objects.equals(m.getMachineCode(), machineCode)).findAny()
					.orElse(null);
		}
		if (machine == null) { // 找不到机台则直接忽略本配方
			return scheduleResult;
		}

		Map<CombinedMapKey, BigDecimal> stockWeightConsumeMap = new HashMap<>(); // 库存重量消耗量，物料类型 + 胶料编号作为唯一键
		BigDecimal maxSetWeight = RecipeUtil.getMaxSetWeight(recipeWeightList); // 获取称重配方中最大的终炼母炼胶重量
		Map<CombinedMapKey, BigDecimal> returnStockWeightConsumeMap = new HashMap<>(); // 预计返回胶的的消耗量，胶料编号 + 班次作为唯一建
		// 遍历配方称重信息，检查其原料的库存是否足够
		for (MesPmtRecipeWeightVo recipeWeight : recipeWeightList) {
			BigDecimal setWeight = new BigDecimal(recipeWeight.getSetWeight().toString()); // 称重配方重量
			String glueCode = recipeWeight.getRecipeMaterialName();
			String majorType = recipeWeight.getMajorType(); // 物料类型
			String realMajorType = RecipeUtil.getMajorType(glueCode, majorType, setWeight, maxSetWeight); // 获取真正的物料类型
			// 只需要判断掺胶物料的库存
			if (this.checkMixMajorType(realMajorType)) {
				BigDecimal stockWeight = glueStock.getStockWeight(glueCode, majorType); // 库存重量，要加上预计返回胶的量
				if (setWeight == null || BigDecimal.ZERO.compareTo(setWeight) == 0) {
					minCarNum = BigDecimal.ZERO; // 如果配方重量为0或者空，则直接当做无法排产，跳过本配方
					continue;
				}
				BigDecimal nightReturnWeight = BigDecimal.ZERO; // 夜班预计返回胶数
				BigDecimal dayReturnWeight = BigDecimal.ZERO; // 白班预计返回胶数
				boolean isDayClass = false; // 白班是否开班

				// 排产日可进行生产的班数，默认两个班
				Integer workClass = ShiftClassUtil.SHIFT_CLASS;
				// 根据机台各班状态判断是否可排满
				Integer tempWorkClass = 0;
				if (machine.getMidStatus()) {
					tempWorkClass++;
				}
				if (machine.getNightStatus()) {
					tempWorkClass++;
					nightReturnWeight = glueStock.getReturnWeight(glueCode, GlueEngineConstants.SHIFT_CLASS_NIGHT,
							true);
				}
				// if (machine.getDayStatus()) {
				// 	tempWorkClass++;
				// 	isDayClass = true;
				// 	// 机台白班有开班，则可以将夜班、白班的预计返回胶都纳入考虑范围
				// 	nightReturnWeight = glueStock.getReturnWeight(glueCode, GlueEngineConstants.SHIFT_CLASS_NIGHT,
				// 			true);
				// 	dayReturnWeight = glueStock.getReturnWeight(glueCode, GlueEngineConstants.SHIFT_CLASS_DAY, true);
				// }
				workClass = BigDecimalUtil.least(workClass, tempWorkClass);

				// 计算最多计划量（上限） = 可生产班数 * 单班最大生产量
				BigDecimal maxCarNum = singleClassPlanQty.multiply(BigDecimalUtil.valueOfZero(workClass));
				minCarNum = BigDecimalUtil.least(maxCarNum, minCarNum); // 取最小车数与最大可生产车数的较小值作为计划数上限

				// 库存可生产数
				BigDecimal carNum = stockWeight.divide(setWeight, 0, RoundingMode.DOWN); // 掺胶库存换算成可生产车数
				BigDecimal returnGlueConsume = BigDecimal.ZERO; // 预计返回胶消耗数
				BigDecimal returnWeight = nightReturnWeight.add(dayReturnWeight); // 预计返回胶总量 = 夜班 + 白班的返回胶量
				if (returnWeight.compareTo(BigDecimal.ZERO) > 0) { // 如果对应的预计返回胶有库存，需要加上库存后再算总量
					BigDecimal totalCarNum = stockWeight.add(returnWeight).divide(setWeight, 0, RoundingMode.DOWN); // 库存加上预计返回胶换算成可生产车数
					totalCarNum = BigDecimalUtil.least(totalCarNum, minCarNum); // 限制总生产车数，不能超过计划数上限
					BigDecimal returnCarNum = totalCarNum.subtract(carNum); // 预计返回胶可生产数 = 总可生产数 - 库存可生产数
					returnGlueConsume = returnCarNum.multiply(setWeight); // 预计库存可生产数换算成需要消耗的重量
					carNum = totalCarNum; // 可生产车数要算上预计返回胶的量
				}
				minCarNum = BigDecimalUtil.least(carNum, minCarNum); // 取最小车数与可生产车数的较小值作为计划数上限
				// 当可生产数不足一车时，跳过本配方
				if (minCarNum.compareTo(BigDecimal.ZERO) == 0) {
					break;
				}
				BigDecimal stockConsume = minCarNum.multiply(setWeight); // 反算出库存消耗重量

				// 统计库存消耗量
				if (stockConsume.compareTo(BigDecimal.ZERO) > 0) {
					CombinedMapKey key = CombinedMapKey.createKey(majorType, glueCode);
					BigDecimal stockWeightConsume = stockWeightConsumeMap.getOrDefault(key, BigDecimal.ZERO);
					stockWeightConsumeMap.put(key, stockWeightConsume.add(stockConsume));
				}
				if (returnGlueConsume.compareTo(BigDecimal.ZERO) > 0) {
					// 白班有开班则把返回胶消耗算到白班上，否则算到夜班上
					Integer shiftClass = isDayClass ? GlueEngineConstants.SHIFT_CLASS_DAY
							: GlueEngineConstants.SHIFT_CLASS_NIGHT;
					CombinedMapKey key = CombinedMapKey.createKey(glueCode, shiftClass);
					BigDecimal consume = returnStockWeightConsumeMap.getOrDefault(key, BigDecimal.ZERO);
					returnStockWeightConsumeMap.put(key, consume.add(returnGlueConsume));
				}
			}
		}
		if (minCarNum.compareTo(BigDecimal.ZERO) > 0) {
			scheduleResult.setPlanQty(minCarNum);
			// 如果最小车数大于0，则说明原料的库存会被消耗，需要更新库存数
			for (Entry<CombinedMapKey, BigDecimal> entry : stockWeightConsumeMap.entrySet()) {
				CombinedMapKey matralkey = entry.getKey();
				String majorType = (String) matralkey.getKey(0);
				String glueCode = (String) matralkey.getKey(1);
				BigDecimal stockWeightConsume = entry.getValue();
				glueStock.subtractStockWeight(glueCode, majorType, stockWeightConsume); // 库存重量扣减
			}
			// 更新预计返回胶数
			for (Entry<CombinedMapKey, BigDecimal> entry : returnStockWeightConsumeMap.entrySet()) {
				CombinedMapKey key = entry.getKey();
				String glueCode = (String) key.getKey(0); // 胶料
				Integer shiftClass = (Integer) key.getKey(1); // 班次
				BigDecimal consume = entry.getValue(); // 消耗量
				glueStock.subtractReturnGlueWeight(glueCode, shiftClass, consume);
			}
		}

		return scheduleResult;
	}

	/**
	 * 检查配方是否符合条件：<br/>
	 * 1、原材料库存<br/>
	 * 2、
	 * 
	 * @param recipe    配方
	 * @param glueStock 库存
	 * @return
	 */
	private boolean checkRecipeStock(MesPmtRecipeVo recipe, GlueScheduleStockPool glueStock) {
		// 正正配方，无条件选用
		if (GlueEngineConstants.RECIPE_TYPE_ZZ.equals(recipe.getRecipeTypeName())) {
			return true;
		}
		// 取不到称重信息的配方过滤掉
		List<MesPmtRecipeWeightVo> recipeWeightList = recipe.getRecipeWeightList();
		if (CollectionUtil.isEmpty(recipeWeightList)) {
			return false;
		}
		// 判断配方是否叶子节点（即使需要塑胶也无需考虑，分解已补充塑胶的需求量）
		boolean isLeaf = recipe.getRecipeWeightList().stream()
				.noneMatch(weight -> this.checkStockMajorType(weight.getMajorType()));
		if (isLeaf) {
			// 叶子节点不需要判断其原料库存
			return true;
		}

		boolean isCheckSuccess = true;
		BigDecimal maxSetWeight = RecipeUtil.getMaxSetWeight(recipeWeightList); // 获取称重配方中最大的终炼母炼胶重量
		for (MesPmtRecipeWeightVo recipteWeight : recipeWeightList) {
			String glueCode = recipteWeight.getRecipeMaterialName();
			String majorType = recipteWeight.getMajorType(); // 物料大类
			BigDecimal setWeight = BigDecimalUtil.valueOfZero(recipteWeight.getSetWeight());
			String realMajorType = RecipeUtil.getMajorType(glueCode, majorType, setWeight, maxSetWeight); // 获取真正的物料类型
			// 先判断物料大类，排程涉及到的类型才需要处理：只有掺胶的需要看库存重量，母炼胶不用
			if (this.checkMixMajorType(realMajorType)) {
				// 取出库存信息
				BigDecimal stockWeight = glueStock.getStockWeight(glueCode, majorType);
				BigDecimal returnWeight = glueStock.getReturnStockWeight(glueCode); // 日返回胶库存

				if (stockWeight.compareTo(BigDecimal.ZERO) <= 0 && returnWeight.compareTo(BigDecimal.ZERO) <= 0) {
					// 任意一个不符合条件，都直接忽略掉这个配方
					return false;
				}
			}
		}
		return isCheckSuccess;
	}

	/**
	 * 生成配方优先级排序器<br/>
	 * 1、配方类型优先级：C2Z > x > ZZ > S<br/>
	 * 2、相同层级的，以配方称重里的原料有效期较早优先
	 * 
	 * @param glueStock 库存
	 * @return
	 */
	private Comparator<MesPmtRecipeVo> createRecipeSorter(GlueScheduleStockPool glueStock) {
		return new Comparator<MesPmtRecipeVo>() {
			@Override
			public int compare(MesPmtRecipeVo recipe1, MesPmtRecipeVo recipe2) {
				return compareRecipePriority(glueStock, recipe1, recipe2);
			}
		};
	}

	/**
	 * 比较配方优先级
	 * 
	 * @param glueStock 库存
	 * @param recipe1   配方1
	 * @param recipe2   配方2
	 * @return
	 */
	private int compareRecipePriority(GlueScheduleStockPool glueStock, MesPmtRecipeVo recipe1, MesPmtRecipeVo recipe2) {
		String recipeTypeName = recipe1.getRecipeTypeName();
		String targetRecipeTypeName = recipe2.getRecipeTypeName();
		// 取出配方类型优先级
		Integer priority = RecipeUtil.getRecipeTypePriority(recipeTypeName);
		Integer targetPriority = RecipeUtil.getRecipeTypePriority(targetRecipeTypeName);
		// 比较优先级不一样的话，直接返回比较结果，优先级较大的在前
		int result = targetPriority.compareTo(priority);
		if (result != 0) {
			return result;
		}
		// 如果配方类型优先级一致，则比较原料库存
		// 获取两者最早的库存到期时间
		Date minValidTime = getEarliestValidTime(glueStock, recipe1);
		Date targetMinValidTime = getEarliestValidTime(glueStock, recipe2);
		// 先对到期时间做空值校验
		if (minValidTime == null && targetMinValidTime != null) {
			return 1;
		} else if (minValidTime != null && targetMinValidTime == null) {
			return -1;
		} else if (minValidTime == null && targetMinValidTime == null) {
			return 0;
		}
		// 根据到期时间排序比较到期时间，时间较小的在前
		return minValidTime.compareTo(targetMinValidTime);
	}

	/**
	 * 获取最早的库存到期时间
	 * 
	 * @param glueStock 库存
	 * @param recipe    配方
	 * @return
	 */
	private Date getEarliestValidTime(GlueScheduleStockPool glueStock, MesPmtRecipeVo recipe) {
		Date minValidTime = null;
		for (MesPmtRecipeWeightVo recipteWeight : recipe.getRecipeWeightList()) {
			String glueCode = recipteWeight.getRecipeMaterialName();
			String majorType = recipteWeight.getMajorType();
			String realMajorType = RecipeUtil.getMajorType(glueCode, majorType); // 获取真正的物料类型
			// 只需要考虑排产需要的物料类型
			if (checkStockMajorType(realMajorType)) {
				// 查询库存
				Date validTime = glueStock.getValidTime(glueCode, majorType);
				if (validTime != null) {
					// 选出库存最早的一笔
					minValidTime = minValidTime == null || validTime.compareTo(minValidTime) < 0 ? validTime
							: minValidTime;
				} else {
					// 如果没有库存，则跳过本配方
					return null;
				}
			}
		}
		return minValidTime;
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
		return GlueEngineConstants.STOCK_MAJOR_TYPE.contains(majorType);
	}
}
