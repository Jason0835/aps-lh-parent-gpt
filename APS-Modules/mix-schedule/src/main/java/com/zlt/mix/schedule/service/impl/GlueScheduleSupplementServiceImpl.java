package com.zlt.mix.schedule.service.impl;

import com.github.pagehelper.util.StringUtil;
import com.ruoyi.common.core.utils.DateUtils;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.utils.StringUtils;
import com.zlt.mix.common.core.constant.ZltConstant;
import com.zlt.mix.common.core.utils.BigDecimalUtil;
import com.zlt.mix.common.core.utils.GenerageMapKeyUtils;
import com.zlt.mix.common.engine.service.impl.IncrementService;
import com.zlt.mix.common.engine.utils.CollectionUtil;
import com.zlt.mix.schedule.api.domain.entity.GlueScheduleSupplement;
import com.zlt.mix.schedule.engine.constants.GlueEngineConstants;
import com.zlt.mix.schedule.engine.mapper.GlueScheduleEngineMapper;
import com.zlt.mix.schedule.engine.service.basicdata.*;
import com.zlt.mix.schedule.engine.service.glueschedule.GlueScheduleEngineBaseService;
import com.zlt.mix.schedule.engine.service.glueschedule.GlueScheduleStockService;
import com.zlt.mix.schedule.engine.util.*;
import com.zlt.mix.schedule.engine.vo.*;
import com.zlt.mix.schedule.mapper.GlueScheduleSupplementMapper;
import com.zlt.mix.schedule.service.GlueScheduleSupplementService;
import com.zlt.mix.setting.api.domain.entity.GlueCommonDemand;
import com.zlt.mix.setting.api.domain.entity.MixMachine;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.Map.Entry;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 生产补量服务
 *
 */
@Service
public class GlueScheduleSupplementServiceImpl implements GlueScheduleSupplementService {
	@Resource
	private GlueScheduleEngineMapper glueScheduleEngineMapper;
	@Resource
	private GlueScheduleSupplementMapper glueScheduleSupplementMapper;
	@Resource
	private RecipeEngineService recipeEngineService;
	@Autowired
	private GlueScheduleStockService glueScheduleStockService;
	@Autowired
	private GlueScheduleEngineBaseService glueScheduleEngineBaseService;
	@Autowired
	private ParamsEngineService paramsEngineService;
	@Autowired
	private MachineEngineService machineEngineService;
	@Resource
	private IncrementService incrementService;
	@Resource
	private MixingTimeEngineService mixingTimeEngineService;
	@Resource
	private MixingGlueRecipeMapEngineService mixingGlueRecipeMapEngineService;

	/**
	 * 生产补量列表
	 *
	 * @param glueScheduleSupplement
	 * @return
	 */
	public List<GlueScheduleSupplement> listGlueScheduleSupplement(GlueScheduleSupplement glueScheduleSupplement) {
		if (glueScheduleSupplement.getScheduleDate() == null) {
			return CollectionUtil.emptyList();
		}
		return glueScheduleSupplementMapper.listGlueScheduleSupplement(glueScheduleSupplement);
	}

	/**
	 * 计算生产补量计划
	 *
	 * @param glueScheduleSupplement 终炼/母炼日计划排程
	 * @return 生产补量计划集合
	 */
	@Override
	public List<GlueScheduleSupplement> caculateSuppliment(GlueScheduleSupplement glueScheduleSupplement) {
		// 检查当天是否已经生成过补量了
		if (glueScheduleSupplementMapper.hasSupplement(glueScheduleSupplement)) {
			throw new RuntimeException("当天补量已生成，不可重复生成！");
		}
		String mixArea = glueScheduleSupplement.getMixArea();
		Date scheduleDate = glueScheduleSupplement.getScheduleDate();
		// 加载系统参数
		Map<String, String> params = paramsEngineService.mapGlueParams(mixArea);

		// 取出当天的排产计划
		GlueScheduleResultVo glueScheduleResult = new GlueScheduleResultVo();
		glueScheduleResult.setMixArea(glueScheduleSupplement.getMixArea());
		glueScheduleResult.setScheduleDate(glueScheduleSupplement.getScheduleDate());
		List<GlueScheduleResultVo> schedulelist = glueScheduleEngineMapper.selectScheduleResult(glueScheduleResult)
				.stream().filter(schedule -> StringUtils.isNotEmpty(schedule.getMachineCode()))
				.filter(schedule -> StringUtils.isNotEmpty(schedule.getGlue()))
				.filter(schedule -> StringUtils.isNotEmpty(schedule.getRecipeType())).collect(Collectors.toList());
		// 判断是否有自动排程生成的记录
		if (schedulelist.stream().noneMatch(s -> ZltConstant.GLUE_SCHEDULE_SOURCE_AUTO.equals(s.getDataSource()))) {
			throw new RuntimeException("只有自动排程后才可生成补量记录！");
		}

		// 取出大规格排产设置
		List<GlueCommonDemand> mainCommonDemandList = new ArrayList<>(); // 大规格在主机台的
		List<GlueCommonDemand> spareCommonDemandList = new ArrayList<>(); // 大规格在备用机台的
		this.loadCommonDemand(mixArea, mainCommonDemandList, spareCommonDemandList);
		if (mainCommonDemandList.isEmpty()) {
			return CollectionUtil.emptyList();
		}

		// 取出配方信息
		MesPmtRecipeVo recipeParams = new MesPmtRecipeVo();
		recipeParams.setMixArea(mixArea);
		List<MesPmtRecipeVo> mesPmtRecipeList = recipeEngineService.listGlueRecipe(recipeParams);
		if (CollectionUtil.isEmpty(mesPmtRecipeList)) {
			return CollectionUtil.emptyList(); // 找不到配方就直接返回
		}
		Map<CombinedMapKey, List<MesPmtRecipeVo>> recipeMap = mesPmtRecipeList.stream()
				.filter(r -> r.getRecipeTypeName() != null) // 过滤掉无配方名称的记录
				.sorted((MesPmtRecipeVo o1, MesPmtRecipeVo o2) -> {
					String recipeType1 = o1.getRecipeTypeName();
					String recipeType2 = o2.getRecipeTypeName();
					return this.getRecipeTypePriority(recipeType1).compareTo(this.getRecipeTypePriority(recipeType2));
				})// 按配方优先级排序
				.collect(Collectors
						.groupingBy(r -> CombinedMapKey.createKey(r.getRecipeEquipCode(), r.getRecipeMaterialName()))); // 按机台+胶料分组

		// 查询胶料配方映射的胶料名称Map
		Map<String, String> glueRecipeMap = mixingGlueRecipeMapEngineService.mapGlueRecipe(mixArea);
		// 查询胶料配方映射的反转白班计划量的Map
		Map<String, String> reserveGlueRecipeMap = mixingGlueRecipeMapEngineService.mapReserveGlueRecipe(mixArea);
		// 取出16点母胶预计库存
		GlueScheduleStockPool glueStock = glueScheduleStockService.buildStockPool(scheduleDate, mixArea, reserveGlueRecipeMap,
				GlueEngineConstants.MAJOR_TYPE_ML); // 加载胶母炼胶库存
		glueScheduleEngineBaseService.caculate16pmEstimateStock(glueStock, mixArea, scheduleDate, mesPmtRecipeList,
				params, glueRecipeMap, reserveGlueRecipeMap, true); // 16点预计库存

		// 扣减胶料消耗量
		this.subtractConsumeStock(glueStock, schedulelist, mesPmtRecipeList);
		
		// 查询炼胶间隔时间
		Map<String, Long> intervalMap = mixingTimeEngineService.mapMixingIntervalTime(mixArea);

		// 计算剩余产能
		List<MixMachine> machineList = machineEngineService.listMixMachineInfo(mixArea);
		Map<CombinedMapKey, BigDecimal> machineCapacityMap = this.initMachineCapacityMap(schedulelist, machineList,
				params, intervalMap); // key：机台+班别
		Map<String, String> machineMap = machineList.stream()
				.collect(Collectors.toMap(MixMachine::getMachineCode, MixMachine::getMachineName, (m1, m2) -> m1)); // 机台名称
		mesPmtRecipeList.forEach(recipe -> recipe.setMachineName(machineMap.get(recipe.getRecipeEquipCode()))); // 给配方设置上机台名称

		Map<CombinedMapKey, Integer> produceOrderMap = new HashMap<>(); // 各机台各班别最新的生产顺序列表
		// 构建补量数据
		List<GlueScheduleSupplement> supplementList = this.buildGlueScheduleSupplement(mixArea, scheduleDate,
				mainCommonDemandList, glueStock, machineCapacityMap, produceOrderMap, recipeMap, params, intervalMap);

		// 有配置一个胶料多个机台的胶料，在主机台排量全部机台处理完后，检索出一车都没排上，但是有辅助机台的胶料，再尝试往辅助机台剩余产能上排
		this.spareMachineSupplement(mixArea, scheduleDate, supplementList, spareCommonDemandList, glueStock,
				machineCapacityMap, produceOrderMap, recipeMap, params, intervalMap);

		IdGenerator id = IdGenerator.positive();
		supplementList.forEach(s -> s.setId(id.next())); // 设置虚拟ID
		supplementList.sort(Comparator.comparing(GlueScheduleSupplement::getMachineCode)
				.thenComparing(GlueScheduleSupplement::getTotalPlanQty, Comparator.reverseOrder())); // 最终结果按机台、可排计划倒序排序
		return supplementList;
	}

	/**
	 * 保存生产补量记录
	 * 
	 * @param supplementDto 待保存的生产补量记录
	 * @return
	 */
	@Override
	@Transactional
	public AjaxResult saveSupplement(List<GlueScheduleSupplement> glueScheduleSupplementList) {
		// 数据校验
		List<GlueScheduleSupplement> supplementList = new ArrayList<>();
		for (GlueScheduleSupplement supplement : glueScheduleSupplementList) {
			supplement.setMidPlanQty(BigDecimalUtil.valueOfZero(supplement.getMidPlanQty()));
			supplement.setNightPlanQty(BigDecimalUtil.valueOfZero(supplement.getNightPlanQty()));
			supplement.setDayPlanQty(BigDecimalUtil.valueOfZero(supplement.getDayPlanQty()));
			supplement.setMidCapacity(BigDecimalUtil.valueOfZero(supplement.getMidCapacity()));
			supplement.setNightCapacity(BigDecimalUtil.valueOfZero(supplement.getNightCapacity()));
			supplement.setDayCapacity(BigDecimalUtil.valueOfZero(supplement.getDayCapacity()));

			if (StringUtil.isEmpty(supplement.getMachineCode()) || StringUtil.isEmpty(supplement.getGlue())
					|| StringUtil.isEmpty(supplement.getRecipeType())) {
				return AjaxResult.error("数据异常，请重新打开补量页面");
			}

			if (supplement.getMidPlanQty().compareTo(BigDecimal.ZERO) == 0
					&& supplement.getNightPlanQty().compareTo(BigDecimal.ZERO) == 0
					&& supplement.getDayPlanQty().compareTo(BigDecimal.ZERO) == 0) {
				continue;
			}

			if (BigDecimalUtil.valueOfZero(supplement.getMidCapacity()).compareTo(BigDecimal.ZERO) < 0
					|| BigDecimalUtil.valueOfZero(supplement.getNightCapacity()).compareTo(BigDecimal.ZERO) < 0
					|| BigDecimalUtil.valueOfZero(supplement.getDayCapacity()).compareTo(BigDecimal.ZERO) < 0) {
				return AjaxResult.error(supplement.getMachineName() + "产能不足");
			}

			if (supplement.getMidPlanQty().add(supplement.getNightPlanQty()).add(supplement.getDayPlanQty())
					.compareTo(supplement.getTotalPlanQty()) > 0) {
				return AjaxResult.error(supplement.getMachineName() + "的" + supplement.getGlue() + "胶料超过最大可生产数");
			}

			supplementList.add(supplement);
		}
		if (CollectionUtil.isEmpty(supplementList)) {
			return AjaxResult.error("没有可补量的机台");
		}

		String mixArea = CollectionUtil.firstElement(supplementList).getMixArea();
		Date scheduleDate = CollectionUtil.firstElement(supplementList).getScheduleDate();

		if (StringUtil.isEmpty(mixArea) || scheduleDate == null) {

			return AjaxResult.error("数据异常，请重新打开补量页面");
		}

		// 检查当天是否已经生成过补量了
		GlueScheduleSupplement glueScheduleSupplement = new GlueScheduleSupplement();
		glueScheduleSupplement.setMixArea(mixArea);
		glueScheduleSupplement.setScheduleDate(scheduleDate);
		if (glueScheduleSupplementMapper.hasSupplement(glueScheduleSupplement)) {
			return AjaxResult.error("当天补量已生成，不可重复生成！");
		}

		// 取出当天的自动排程计划
		GlueScheduleResultVo glueScheduleResult = new GlueScheduleResultVo();
		glueScheduleResult.setMixArea(mixArea);
		glueScheduleResult.setScheduleDate(scheduleDate);
		List<GlueScheduleResultVo> schedulelist = glueScheduleEngineMapper.selectScheduleResult(glueScheduleResult)
				.stream().filter(schedule -> StringUtils.isNotEmpty(schedule.getMachineCode()))
				.filter(schedule -> StringUtils.isNotEmpty(schedule.getGlue()))
				.filter(schedule -> StringUtils.isNotEmpty(schedule.getRecipeType())).collect(Collectors.toList());
		// 判断是否有自动排程生成的记录
		if (schedulelist.stream().noneMatch(s -> ZltConstant.GLUE_SCHEDULE_SOURCE_AUTO.equals(s.getDataSource()))) {
			return AjaxResult.error("只有自动排程后才可生成补量记录！");
		}

		// 生成排程记录
		List<GlueScheduleResultVo> newScheduleList = new ArrayList<>();
		List<GlueScheduleResultVo> modifyScheduleList = new ArrayList<>(); // 需更新的排程记录
		String batchNo = CollectionUtil.firstElement(schedulelist).getBatchNo();
		IdGenerator id = IdGenerator.negative();// 虚拟ID
		for (GlueScheduleSupplement supplement : supplementList) {
			GlueScheduleResultVo newSchedule = new GlueScheduleResultVo();
			newSchedule.setId(id.next());
			newSchedule.setBatchNo(batchNo);
			newSchedule.setOrderNo(incrementService.getSequence4(batchNo));
			newSchedule.setScheduleDate(scheduleDate);
			newSchedule.setMixArea(mixArea);
			newSchedule.setMachineCode(supplement.getMachineCode());
			newSchedule.setGlue(supplement.getGlue());
			newSchedule.setRecipeMaterialCode(supplement.getRecipeMaterialCode());
			newSchedule.setSapCode(supplement.getSapCode());
			newSchedule.setMachineCode(supplement.getMachineCode());
			newSchedule.setRecipeType(supplement.getRecipeType());
			newSchedule.setRecipeVersionId(supplement.getRecipeVersionId());
			newSchedule.setRecipeStage(supplement.getRecipeStage());
			newSchedule.setRecipeMaterialCode(supplement.getRecipeMaterialCode());
			newSchedule.setFormulaWeight(supplement.getFormulaWeight());
			newSchedule.setFormulaTime(supplement.getFormulaTime().doubleValue());
			newSchedule.setStockQty(supplement.getStockQty());
			newSchedule.setSafeStockQty(supplement.getSafeStockQty());
			newSchedule.setMidPlanQty(supplement.getMidPlanQty().doubleValue());
			newSchedule.setNightPlanQty(supplement.getNightPlanQty().doubleValue());
			newSchedule.setDayPlanQty(supplement.getDayPlanQty().doubleValue());
			newSchedule.setTotalPlanQty(supplement.getMidPlanQty().add(supplement.getNightPlanQty())
					.add(supplement.getDayPlanQty()).doubleValue());

			newSchedule.setDataSource(ZltConstant.GLUE_SCHEDULE_SOURCE_SUPPLEMENT);
			newSchedule.setPublishSuccessCount(0);
			newSchedule.setReleaseStatus(ZltConstant.NO_RELEASE);
			newSchedule.setBaseValue(null);

			schedulelist.add(newSchedule);
			newScheduleList.add(newSchedule);

			// 有计划量的班别都要重算预计时间
			if (newSchedule.getMidPlanQty() > 0) {
				this.caculateSupplementExpectTime(newSchedule, supplement, GlueEngineConstants.SHIFT_CLASS_MID,
						schedulelist, modifyScheduleList); // 计算中班预计时间
			}
			if (newSchedule.getNightPlanQty() > 0) {
				this.caculateSupplementExpectTime(newSchedule, supplement, GlueEngineConstants.SHIFT_CLASS_NIGHT,
						schedulelist, modifyScheduleList); // 计算夜班预计时间
			}
			if (newSchedule.getDayPlanQty() > 0) {
				this.caculateSupplementExpectTime(newSchedule, supplement, GlueEngineConstants.SHIFT_CLASS_DAY,
						schedulelist, modifyScheduleList); // 计算白班预计时间
			}
		}

		// 修改列表排除掉新增的记录
		modifyScheduleList = modifyScheduleList.stream().filter(s -> s.getId() != null && s.getId() > 0)
				.collect(Collectors.toList());

		// 数据入库
		glueScheduleSupplementMapper.saveSupplement(supplementList);
		glueScheduleEngineMapper.batchInsertScheduleResult(newScheduleList);
		if (CollectionUtils.isNotEmpty(modifyScheduleList)) {
			glueScheduleEngineMapper.mergeScheduleResult(modifyScheduleList);
		}

		return AjaxResult.success();
	}

	/**
	 * 计算补量的预计时间
	 * 
	 * @param newSchedule
	 * @param supplement
	 * @param shiftClass
	 * @param schedulelist
	 * @param modifyScheduleList
	 */
	private void caculateSupplementExpectTime(GlueScheduleResultVo newSchedule, GlueScheduleSupplement supplement,
			int shiftClass, List<GlueScheduleResultVo> schedulelist, List<GlueScheduleResultVo> modifyScheduleList) {
		// 查找同一个机台是否已经有安排相同胶料的计划
		String machineCode = supplement.getMachineCode();
		String glue = supplement.getGlue();
		SingleClassGlueScheduleResultVO newSingleSchedule = new SingleClassGlueScheduleResultVO(newSchedule,
				shiftClass);
		List<SingleClassGlueScheduleResultVO> singleScheduleList = schedulelist.stream()
				.map(s -> new SingleClassGlueScheduleResultVO(s, shiftClass)) // 抽取指定班别的排产信息
				.filter(s -> machineCode.equals(s.getMachineCode())).filter(s -> s.getExpectFinishTime() != null) // 需要同一机台、同一班别有计划完成时间的计划
				.sorted(Comparator.comparing(SingleClassGlueScheduleResultVO::getExpectFinishTime)) // 按完成时间倒序排序
				.collect(Collectors.toList());
		SingleClassGlueScheduleResultVO lastestSchedule = singleScheduleList.stream()
				.filter(s -> glue.equals(s.getGlue()))
				.max((s1, s2) -> s1.getExpectFinishTime().compareTo(s2.getExpectFinishTime())) // 取出同胶料的最晚的一笔
				.orElse(null);
		// 如果没有匹配，则直接插到当班组后一笔之后
		if (CollectionUtils.isEmpty(singleScheduleList)) { // 如果该机台没有排任何胶料，则虚拟一个初始匹配项目
			lastestSchedule = new SingleClassGlueScheduleResultVO();
			lastestSchedule.setExpectFinishTime(
					ShiftClassUtil.getShiftClassStartTime(newSchedule.getScheduleDate(), shiftClass));
			lastestSchedule.setProduceOrder(0);
		} else if (lastestSchedule == null) {
			lastestSchedule = singleScheduleList.get(singleScheduleList.size() - 1); // 取最末尾的一笔
		}

		// 将记录插单匹配项到后一位，并更新其后的序号以及完成时间
		this.recaculateFollowScheduleExpectTime(supplement, lastestSchedule, singleScheduleList, newSingleSchedule,
				modifyScheduleList);
	}

	/**
	 * 重算后续排程的预计时间
	 * 
	 * @param supplement
	 * @param lastestSchedule
	 * @param singleScheduleList
	 * @param newSingleSchedule
	 * @param modifyScheduleList
	 */
	private void recaculateFollowScheduleExpectTime(GlueScheduleSupplement supplement,
			SingleClassGlueScheduleResultVO lastestSchedule, List<SingleClassGlueScheduleResultVO> singleScheduleList,
			SingleClassGlueScheduleResultVO newSingleSchedule, List<GlueScheduleResultVo> modifyScheduleList) {
		// 计算插单数据的生产时长
		BigDecimal formulaTime = BigDecimalUtil.valueOfZero(supplement.getFormulaTime()); // 配方时长
		BigDecimal mixOntervalTime = BigDecimalUtil.valueOfZero(supplement.getMixOntervalTime()); // 每车间隔
		BigDecimal scheduleSwitchTime = BigDecimalUtil.valueOfZero(supplement.getScheduleSwitchTime()); // 切换时长
		BigDecimal planQty = newSingleSchedule.getPlanQty();
		BigDecimal produceTime = formulaTime.add(mixOntervalTime).multiply(planQty).add(scheduleSwitchTime);
		int lastestProduceOrder = lastestSchedule.getProduceOrder() + 1; // 序号从匹配项 + 1开始
		newSingleSchedule.setProduceOrder(lastestProduceOrder);
		newSingleSchedule.setExpectStartTime(lastestSchedule.getExpectFinishTime());
		newSingleSchedule.setExpectFinishTime(
				DateUtils.addSeconds(lastestSchedule.getExpectFinishTime(), produceTime.intValue()));

		newSingleSchedule.updateExpectTime();
		boolean isMatch = false;
		for (SingleClassGlueScheduleResultVO singleSchedule : singleScheduleList) {
			// 先遍历到匹配项的下一个计划
			if (singleSchedule == lastestSchedule) {
				isMatch = true;
				continue;
			}
			if (!isMatch) {
				continue;
			}
			Date expectStartTime = singleSchedule.getExpectStartTime();
			Date expectFinishTime = singleSchedule.getExpectFinishTime();
			Integer productOrder = singleSchedule.getProduceOrder();
			if (expectStartTime != null) {
				singleSchedule.setExpectStartTime(DateUtils.addSeconds(expectStartTime, produceTime.intValue()));
			}
			singleSchedule.setExpectFinishTime(DateUtils.addSeconds(expectFinishTime, produceTime.intValue()));
			if (productOrder != null && productOrder.intValue() == lastestProduceOrder) {
				singleSchedule.setProduceOrder(++lastestProduceOrder);
			}

			GlueScheduleResultVo modifySchedule = singleSchedule.getScheduleResult();
			if (modifySchedule.getId() != null && modifySchedule.getId() > 0) {
				if (modifySchedule.getPublishSuccessCount() > 0) {
					modifySchedule.setReleaseStatus(ZltConstant.WAIT_RELEASING);
				}
				singleSchedule.updateExpectTime();
				modifySchedule.setBaseValue(modifySchedule.getId());
				mergeScheduleListWithIdCheck(modifySchedule, modifyScheduleList); // 合并到待更新列表中
			}
		}
	}

	/**
	 * 添加排程列表数据，但是要验证ID是否已经存在，已存在则不处理该排程记录
	 * 
	 * @param sourceScheduleList 待添加列表
	 * @param targetScheduleList 目标列表
	 */
	private void mergeScheduleListWithIdCheck(GlueScheduleResultVo sourceSchedule,
			List<GlueScheduleResultVo> targetScheduleList) {
		if (CollectionUtil.isEmpty(targetScheduleList)) {
			return;
		}
		// 将排产列表整理成map<主键ID，排产记录>
		Set<Long> targetScheduleSet = targetScheduleList.stream().map(GlueScheduleResultVo::getId)
				.filter(Objects::nonNull).collect(Collectors.toSet());
		if (Optional.ofNullable(sourceSchedule).map(GlueScheduleResultVo::getId).isPresent()
				&& !targetScheduleSet.contains(sourceSchedule.getId())) {
			targetScheduleList.add(sourceSchedule);
		}
	}

	/**
	 * 扣减胶料消耗量
	 * 
	 * @param glueStock        胶料
	 * @param schedulelist     当天排程
	 * @param mesPmtRecipeList 配方列表
	 */
	private void subtractConsumeStock(GlueScheduleStockPool glueStock, List<GlueScheduleResultVo> schedulelist,
			List<MesPmtRecipeVo> mesPmtRecipeList) {
		Map<CombinedMapKey, MesPmtRecipeVo> recipeScheduleMap = mesPmtRecipeList.stream().collect(Collectors.toMap(
				r -> CombinedMapKey.createKey(r.getRecipeEquipCode(), r.getRecipeMaterialName(), r.getRecipeType()), // 按机台+胶料分组+配方类型分组
				Function.identity(), (r1, r2) -> r1));
		for (GlueScheduleResultVo schedule : schedulelist) {
			MesPmtRecipeVo recipe = recipeScheduleMap.get(
					CombinedMapKey.createKey(schedule.getMachineCode(), schedule.getGlue(), schedule.getRecipeType()));
			if (recipe == null) {
				continue;
			}
			glueScheduleEngineBaseService.copyRecipeProperties(schedule, recipe);
			BigDecimal planQty = BigDecimalUtil.valueOfZero(schedule.getTotalPlanQty());
			schedule.setPlanQty(planQty);
			if (planQty.compareTo(BigDecimal.ZERO) > 0) {
				glueStock.subtractChildGlueStock(BigDecimalUtil.valueOf(schedule.getTotalPlanQty()), recipe, true); // 根据当天计划量扣减原料库存
			}
		}
	}

	/**
	 * 备用机台补量
	 * 
	 * @param mixArea
	 * @param scheduleDate
	 * @param supplementList
	 * @param spareCommonDemandList
	 * @param glueStock
	 * @param machineCapacityMap
	 * @param produceOrderMap
	 * @param recipeMap
	 * @param params
	 * @param intervalMap          炼胶间隔时间
	 */
	private void spareMachineSupplement(String mixArea, Date scheduleDate, List<GlueScheduleSupplement> supplementList,
			List<GlueCommonDemand> spareCommonDemandList, GlueScheduleStockPool glueStock,
			Map<CombinedMapKey, BigDecimal> machineCapacityMap, Map<CombinedMapKey, Integer> produceOrderMap,
			Map<CombinedMapKey, List<MesPmtRecipeVo>> recipeMap, Map<String, String> params, Map<String, Long> intervalMap) {
		while (true) { // 需要重复检查，有配置几个备选机台要补量就重复几次
			List<GlueScheduleSupplement> surplueSupplementList = supplementList.stream()
					.filter(s -> s.getMidPlanQty() == null && s.getNightPlanQty() == null && s.getDayPlanQty() == null)
					.collect(Collectors.toList());
			if (CollectionUtils.isEmpty(surplueSupplementList)) {
				break;
			}
			Map<String, List<GlueCommonDemand>> spareCommonDemanMap = spareCommonDemandList.stream()
					.collect(Collectors.groupingBy(GlueCommonDemand::getGlue));
			List<GlueCommonDemand> spareDemandList = new ArrayList<>();
			for (GlueScheduleSupplement surplusSupplement : surplueSupplementList) { // 检查没排上的物料机台
				String glue = surplusSupplement.getGlue();
				GlueCommonDemand spare = CollectionUtil.firstElement(spareCommonDemanMap.get(glue)); // 取出备选机台
				if (spare == null) {
					continue;
				}
				spareDemandList.add(spare);
				spareCommonDemanMap.get(glue).remove(0);
				CollectionUtil.remove(supplementList, surplusSupplement); // 需要将没排上、且有备选机台的的记录删除掉
			}
			if (spareDemandList.isEmpty()) {
				break;
			}
			List<GlueScheduleSupplement> surplusList = this.buildGlueScheduleSupplement(mixArea, scheduleDate,
					spareDemandList, glueStock, machineCapacityMap, produceOrderMap, recipeMap, params, intervalMap);
			supplementList.addAll(surplusList);
		}
	}

	/**
	 * 构建补量列表
	 *
	 * @param mixArea
	 * @param scheduleDate
	 * @param mainCommonDemandList 大规格配置列表
	 * @param glueStock            当日库存，16点预计库存-16后消耗量
	 * @param machineCapacityMap   机台剩余产能
	 * @param produceOrderMap      各机台各班别最新的生产顺序列表
	 * @param recipeMap            配方
	 * @param params               排程参数
	 * @param intervalMap          炼胶间隔时间
	 * @return
	 */
	private List<GlueScheduleSupplement> buildGlueScheduleSupplement(String mixArea, Date scheduleDate,
			List<GlueCommonDemand> mainCommonDemandList, GlueScheduleStockPool glueStock,
			Map<CombinedMapKey, BigDecimal> machineCapacityMap, Map<CombinedMapKey, Integer> produceOrderMap,
			Map<CombinedMapKey, List<MesPmtRecipeVo>> recipeMap, Map<String, String> params, Map<String, Long> intervalMap) {
		BigDecimal mixOntervalTime = new BigDecimal(params.getOrDefault(GlueEngineConstants.MIX_INTERVAL_TIME, "0")); // 每一车的间隔时间
		BigDecimal scheduleSwitchTime = new BigDecimal(
				params.getOrDefault(GlueEngineConstants.SCHEDULE_SWITCH_TIME, "0")); // 每一个计划的切换时长
		List<GlueScheduleSupplement> supplementList = new ArrayList<>();
		// 计算每个大规格的最大可生产数
		for (GlueCommonDemand demand : mainCommonDemandList) {
			String glue = demand.getGlue();
			String machineCode = demand.getMachineCode();

			List<MesPmtRecipeVo> recipeList = recipeMap.get(CombinedMapKey.createKey(machineCode, glue)); // 配方
			if (CollectionUtil.isEmpty(recipeList)) {
				continue;
			}
			MesPmtRecipeVo recipe = CollectionUtil.firstElement(recipeList);
			BigDecimal planQty = null;
			List<MesPmtRecipeWeightVo> recipeWeightList = recipe.getRecipeWeightList();
			BigDecimal maxSetWeight = RecipeUtil.getMaxSetWeight(recipeWeightList); // 获取称重配方中最大的终炼母炼胶重量
			for (MesPmtRecipeWeightVo recipteWeight : recipeWeightList) {
				BigDecimal setWeight = BigDecimalUtil.valueOfZero(recipteWeight.getSetWeight());
				String weightGlueCode = recipteWeight.getRecipeMaterialName();
				String majorType = RecipeUtil.getMajorType(weightGlueCode, recipteWeight.getMajorType(), setWeight,
						maxSetWeight);
				if (GlueEngineConstants.MAJOR_TYPE_ML.equals(majorType)) { // 取出母炼胶
					// 可生产终胶 = 母炼胶库存 * 换算比率
					BigDecimal conversionRatio = Optional.ofNullable(recipteWeight.getConversionRatio())
							.orElse(BigDecimal.ONE); // 换算比率
					BigDecimal stockNum = glueStock.getStockNum(weightGlueCode, majorType); // 母炼胶现有库存
					BigDecimal productNum = stockNum.multiply(conversionRatio).setScale(0, RoundingMode.DOWN);
					if (planQty == null) {
						planQty = productNum;
					} else {
						planQty = BigDecimalUtil.least(planQty, productNum);
					}
				}
			}
			if (planQty.compareTo(BigDecimal.ZERO) <= 0) { // 可生产量大于0的继续生成
				continue;
			}

			// 再根据终胶反算出母胶的量
			for (MesPmtRecipeWeightVo recipteWeight : recipeWeightList) {
				BigDecimal setWeight = BigDecimalUtil.valueOfZero(recipteWeight.getSetWeight());
				String weightGlueCode = recipteWeight.getRecipeMaterialName();
				String majorType = RecipeUtil.getMajorType(weightGlueCode, recipteWeight.getMajorType(), setWeight,
						maxSetWeight);
				if (GlueEngineConstants.MAJOR_TYPE_ML.equals(majorType)) { // 取出母炼胶
					BigDecimal conversionRatio = recipteWeight.getConversionRatio(); // 换算比率
					if (conversionRatio == null || conversionRatio.compareTo(BigDecimal.ZERO) == 0) {
						conversionRatio = BigDecimal.ONE;
					}
					BigDecimal stockNum = planQty.divide(conversionRatio, 0, RoundingMode.UP); // 母胶消耗量
					glueStock.subtractStock(weightGlueCode, majorType, stockNum); // 母胶库存扣减掉消耗量
				}
			}

			GlueScheduleSupplement supplement = new GlueScheduleSupplement();
			supplement.setGlue(glue);
			supplement.setScheduleDate(scheduleDate);
			supplement.setMixArea(mixArea);
			supplement.setMachineCode(machineCode);
			supplement.setMachineName(recipe.getMachineName());
			supplement.setSapCode(recipe.getSapMaterialCode());
			supplement.setRecipeType(recipe.getRecipeType());
			supplement.setRecipeTypeName(recipe.getRecipeTypeName());
			supplement.setRecipeVersionId(recipe.getRecipeVersionId());
			supplement.setRecipeStage(recipe.getProductStage());
			supplement.setRecipeMaterialCode(recipe.getRecipeMaterialCode());
			supplement.setFormulaWeight(recipe.getLotTotalWeight());
			supplement.setFormulaTime(recipe.getSummerMixTime());
			supplement.setTotalPlanQty(planQty);
			supplement.setStockQty(glueStock.getStockNum(glue, GlueEngineConstants.MAJOR_TYPE_ZL).doubleValue());
			supplement.setSafeStockQty(glueStock.getSafeStock(glue).doubleValue());
			Long intervalTime = intervalMap.get(GenerageMapKeyUtils.createMapKey(glue, machineCode));
			supplement.setMixOntervalTime(intervalTime != null ? BigDecimal.valueOf(intervalTime) : mixOntervalTime);
			supplement.setScheduleSwitchTime(scheduleSwitchTime);

			// 机台各班的初始产能
			CombinedMapKey midKey = CombinedMapKey.createKey(machineCode, GlueEngineConstants.SHIFT_CLASS_MID);
			supplement.setOldMidCapacity(machineCapacityMap.getOrDefault(midKey, BigDecimal.ZERO));
			CombinedMapKey nightKey = CombinedMapKey.createKey(machineCode, GlueEngineConstants.SHIFT_CLASS_NIGHT);
			supplement.setOldNightCapacity(machineCapacityMap.getOrDefault(nightKey, BigDecimal.ZERO));
			CombinedMapKey dayKey = CombinedMapKey.createKey(machineCode, GlueEngineConstants.SHIFT_CLASS_DAY);
			supplement.setOldDayCapacity(machineCapacityMap.getOrDefault(dayKey, BigDecimal.ZERO));

			supplementList.add(supplement);
		}

		// 给每个大规格分配产能，按班别升序、规格最大可生产数降序分配产能，直到全部母胶库存耗完或者全部产能耗完为至
		supplementList.sort(Comparator.comparing(GlueScheduleSupplement::getTotalPlanQty, Comparator.reverseOrder())); // 按计划量从大到小重新排序
		for (GlueScheduleSupplement supplement : supplementList) {
			String glue = supplement.getGlue();
			String machineCode = supplement.getMachineCode();
			BigDecimal planQty = supplement.getTotalPlanQty();
			List<MesPmtRecipeVo> recipeList = recipeMap.get(CombinedMapKey.createKey(machineCode, glue)); // 配方
			if (CollectionUtil.isEmpty(recipeList)) {
				continue;
			}
			MesPmtRecipeVo recipe = CollectionUtil.firstElement(recipeList);
			Long intervalTime = intervalMap.get(GenerageMapKeyUtils.createMapKey(supplement.getGlue(), supplement.getMachineCode()));
			BigDecimal maxPerCarTime = BigDecimalUtil.valueOf(recipe.getSummerMixTime()).add(intervalTime != null ? BigDecimal.valueOf(intervalTime) : mixOntervalTime); // 一车胶消耗产能=配方炼胶时长+单车间隔时长

			// 根据机台产能计算能在每个班分别排多少计划量
			// 中班
			CombinedMapKey midKey = CombinedMapKey.createKey(machineCode, GlueEngineConstants.SHIFT_CLASS_MID);
			BigDecimal midCapacity = machineCapacityMap.getOrDefault(midKey, BigDecimal.ZERO);
			BigDecimal midConsumeCapacity = maxPerCarTime.multiply(planQty).add(scheduleSwitchTime); // 生产完该计划需要消耗的产能
			BigDecimal midRealCapacity = BigDecimalUtil.least(midCapacity, midConsumeCapacity); // 实际可用产能
			BigDecimal midPlanQty = midRealCapacity.subtract(scheduleSwitchTime).divide(maxPerCarTime, 0,
					RoundingMode.DOWN); // 实际可生产量
			BigDecimal midRealConsumeCapacity = midPlanQty.multiply(maxPerCarTime).add(scheduleSwitchTime); // 实际消耗产能
			BigDecimal midSurplueCapacity = midCapacity.subtract(midRealConsumeCapacity); // 剩余产能
			if (midPlanQty.compareTo(BigDecimal.ZERO) > 0) {
				machineCapacityMap.put(midKey, midSurplueCapacity);
				supplement.setMidPlanQty(midPlanQty);
				Integer midOrder = produceOrderMap.getOrDefault(midKey, 0);
				midOrder += 1;
				produceOrderMap.put(midKey, midOrder);
				supplement.setMidProduceOrder(midOrder);
				planQty = planQty.subtract(midPlanQty); // 总计划量扣减掉中班排上的量
				if (planQty.compareTo(BigDecimal.ZERO) <= 0) {
					continue;
				}
			}

			// 夜班
			CombinedMapKey nightKey = CombinedMapKey.createKey(machineCode, GlueEngineConstants.SHIFT_CLASS_NIGHT);
			BigDecimal nightCapacity = machineCapacityMap.get(nightKey);
			BigDecimal nightConsumeCapacity = maxPerCarTime.multiply(planQty).add(scheduleSwitchTime); // 生产完该计划需要消耗的产能
			BigDecimal nightRealCapacity = BigDecimalUtil.least(nightCapacity, nightConsumeCapacity); // 实际可用产能
			BigDecimal nightPlanQty = nightRealCapacity.subtract(scheduleSwitchTime).divide(maxPerCarTime, 0,
					RoundingMode.DOWN); // 实际可生产量
			BigDecimal nightRealConsumeCapacity = nightPlanQty.multiply(maxPerCarTime).add(scheduleSwitchTime); // 实际消耗产能
			BigDecimal nightSurplueCapacity = nightCapacity.subtract(nightRealConsumeCapacity); // 剩余产能
			if (nightPlanQty.compareTo(BigDecimal.ZERO) > 0) {
				machineCapacityMap.put(nightKey, nightSurplueCapacity);
				supplement.setNightPlanQty(nightPlanQty);
				Integer nightOrder = produceOrderMap.getOrDefault(nightKey, 0);
				nightOrder += 1;
				produceOrderMap.put(nightKey, nightOrder);
				supplement.setNightProduceOrder(nightOrder);
				planQty = planQty.subtract(nightPlanQty); // 总计划量扣减掉夜班排上的量
				if (planQty.compareTo(BigDecimal.ZERO) <= 0) {
					continue;
				}
			}

			// 白班
			CombinedMapKey dayKey = CombinedMapKey.createKey(machineCode, GlueEngineConstants.SHIFT_CLASS_DAY);
			BigDecimal dayCapacity = machineCapacityMap.get(dayKey);
			BigDecimal dayConsumeCapacity = maxPerCarTime.multiply(planQty).add(scheduleSwitchTime); // 生产完该计划需要消耗的产能
			BigDecimal dayRealCapacity = BigDecimalUtil.least(dayCapacity, dayConsumeCapacity); // 实际可用产能
			BigDecimal dayPlanQty = dayRealCapacity.subtract(scheduleSwitchTime).divide(maxPerCarTime, 0,
					RoundingMode.DOWN); // 实际可生产量
			BigDecimal dayRealConsumeCapacity = dayPlanQty.multiply(maxPerCarTime).add(scheduleSwitchTime); // 实际消耗产能
			BigDecimal daySurplueCapacity = dayCapacity.subtract(dayRealConsumeCapacity); // 剩余产能
			if (dayPlanQty.compareTo(BigDecimal.ZERO) > 0) {
				machineCapacityMap.put(dayKey, daySurplueCapacity);
				supplement.setDayPlanQty(dayPlanQty);
				Integer dayOrder = produceOrderMap.getOrDefault(dayKey, 0);
				dayOrder += 1;
				produceOrderMap.put(dayKey, dayOrder);
				supplement.setDayProduceOrder(dayOrder);
			}
		}

		// 取机台各班的剩余产能
		for (GlueScheduleSupplement supplement : supplementList) {
			String machineCode = supplement.getMachineCode();
			CombinedMapKey midKey = CombinedMapKey.createKey(machineCode, GlueEngineConstants.SHIFT_CLASS_MID);
			supplement.setMidCapacity(machineCapacityMap.getOrDefault(midKey, BigDecimal.ZERO));
			CombinedMapKey nightKey = CombinedMapKey.createKey(machineCode, GlueEngineConstants.SHIFT_CLASS_NIGHT);
			supplement.setNightCapacity(machineCapacityMap.getOrDefault(nightKey, BigDecimal.ZERO));
			CombinedMapKey dayKey = CombinedMapKey.createKey(machineCode, GlueEngineConstants.SHIFT_CLASS_DAY);
			supplement.setDayCapacity(machineCapacityMap.getOrDefault(dayKey, BigDecimal.ZERO));
		}

		return supplementList;
	}

	/**
	 * 加载大规格配置
	 * 
	 * @param mixArea
	 * @param mainCommonDemandList  主机台配置
	 * @param spareCommonDemandList 辅助机台配置
	 */
	private void loadCommonDemand(String mixArea, List<GlueCommonDemand> mainCommonDemandList,
			List<GlueCommonDemand> spareCommonDemandList) {
		GlueCommonDemand glueCommonDemand = new GlueCommonDemand();
		glueCommonDemand.setMixArea(mixArea);
		List<GlueCommonDemand> commonDemandList = glueScheduleSupplementMapper
				.listGlueCommonDemandList(glueCommonDemand);
		if (CollectionUtil.isEmpty(commonDemandList)) {
			return; // 找不到配置就直接返回
		}
		// 将大规格配置分成主机台与备用机台的配置
		Map<String, List<FormulaMachineVo>> glueMachineMap = machineEngineService.listFormulaMachine(mixArea).stream()
				.collect(Collectors.groupingBy(FormulaMachineVo::getGlue));
		Map<String, List<GlueCommonDemand>> commonDemandMap = commonDemandList.stream()
				.filter(commonDemand -> StringUtils.isNotEmpty(commonDemand.getGlue()))
				.filter(commonDemand -> StringUtils.isNotEmpty(commonDemand.getMachineCode()))
				.collect(Collectors.groupingBy(GlueCommonDemand::getGlue)); // 按胶料给大规格配置分好组
		for (Entry<String, List<GlueCommonDemand>> entry : commonDemandMap.entrySet()) {
			String glue = entry.getKey();
			List<GlueCommonDemand> glueCommonDemandList = entry.getValue();
			if (glueCommonDemandList.size() == 1) { // 如果只有一笔，则直接加入到主机机台列表即可
				mainCommonDemandList.addAll(glueCommonDemandList);
				continue;
			}
			Map<String, Integer> machineOrderMap = glueMachineMap.getOrDefault(glue, CollectionUtil.emptyList())
					.stream().collect(Collectors.toMap(FormulaMachineVo::getMachineCode,
							FormulaMachineVo::getMachineOrder, (v1, v2) -> v1)); // 可生产该胶料的机台顺序列表
			// 超过两笔的，将机台需要最小的放到主机台上，其余放到备用机台上
			GlueCommonDemand commonDemand = glueCommonDemandList.stream().min((g1, g2) -> {
				Integer order1 = Optional.ofNullable(machineOrderMap.get(g1.getMachineCode())).orElse(99);
				Integer order2 = Optional.ofNullable(machineOrderMap.get(g2.getMachineCode())).orElse(99);
				return order1.compareTo(order2);
			}).get();
			String machineCode = commonDemand.getMachineCode();
			if (StringUtils.isEmpty(glue) || StringUtils.isEmpty(machineCode)) {
				continue;
			}
			mainCommonDemandList.add(commonDemand);
			spareCommonDemandList.addAll(glueCommonDemandList.stream().filter(demand -> demand != commonDemand)
					.collect(Collectors.toList())); // 排除掉主机台后规格，其余放到备用机台上
		}
	}

	/**
	 * 根据机台 + 排产情况获取机台的剩余产能列表
	 *
	 * @param baseScheduleResult 已排计划
	 * @param machineList        机台列表
	 * @param params             排产参数
	 * @param intervalMap        炼胶间隔时间
	 * @return
	 */
	private Map<CombinedMapKey, BigDecimal> initMachineCapacityMap(List<GlueScheduleResultVo> baseScheduleResult,
			List<MixMachine> machineList, Map<String, String> params, Map<String, Long> intervalMap) {
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
			if (ZltConstant.STATUS_ENABLE.equals(machine.getMidStatus())) {
				machineCapacityMap.put(CombinedMapKey.createKey(machineCode, GlueEngineConstants.SHIFT_CLASS_MID),
						singleCapacity);
			}
			if (ZltConstant.STATUS_ENABLE.equals(machine.getNightStatus())) {
				machineCapacityMap.put(CombinedMapKey.createKey(machineCode, GlueEngineConstants.SHIFT_CLASS_NIGHT),
						singleCapacity);
			}
			if (ZltConstant.STATUS_ENABLE.equals(machine.getDayStatus())) {
				machineCapacityMap.put(CombinedMapKey.createKey(machineCode, GlueEngineConstants.SHIFT_CLASS_DAY),
						singleCapacity);
			}
		}

		// 扣减已排计划的产能
		for (GlueScheduleResultVo result : baseScheduleResult) {
			if (result.getPmtRecipe() == null) {
				continue;
			}
			String machineCode = result.getMachineCode();
			Long intervalTime = intervalMap.get(GenerageMapKeyUtils.createMapKey(result.getGlue(), result.getMachineCode()));
			BigDecimal maxPerCarTime = BigDecimalUtil.valueOf(result.getPmtRecipe().getSummerMixTime())
					.add(intervalTime != null ? BigDecimal.valueOf(intervalTime) : mixOntervalTime); // 一车胶消耗产能 = 配方炼胶时长+单车间隔时长
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

			BigDecimal dayPlanQty = BigDecimalUtil.valueOfZero(result.getDayPlanQty());
			CombinedMapKey dayKey = CombinedMapKey.createKey(machineCode, GlueEngineConstants.SHIFT_CLASS_DAY);
			BigDecimal dayCapacity = machineCapacityMap.getOrDefault(dayKey, BigDecimal.ZERO); // 机台白班剩余产能
			if (dayCapacity.compareTo(BigDecimal.ZERO) > 0 && dayPlanQty.compareTo(BigDecimal.ZERO) > 0) {
				BigDecimal consumeCapacity = maxPerCarTime.multiply(dayPlanQty).add(scheduleSwitchTime); // 生产完该计划需要消耗的产能
				BigDecimal surplusCapacity = dayCapacity.subtract(consumeCapacity); // 机台扣减掉销量的的剩余产能
				machineCapacityMap.put(dayKey, BigDecimalUtil.greatest(surplusCapacity, BigDecimal.ZERO));
			}
		}
		return machineCapacityMap;
	}

	/**
	 * 获取配方类型优先级，ZZ最优先，F\X的排最后，其他中间
	 * 
	 * @param recipeType
	 * @return
	 */
	private Integer getRecipeTypePriority(String recipeType) {
		if (StringUtils.isEmpty(recipeType)) {
			return 4;
		}
		boolean isZZ = recipeType.startsWith(GlueEngineConstants.RECIPE_TYPE_ZZ);
		boolean isLast = recipeType.contains(GlueEngineConstants.RECIPE_TYPE_F)
				|| recipeType.contains(GlueEngineConstants.RECIPE_TYPE_X)
				|| recipeType.equals(GlueEngineConstants.RECIPE_TYPE_PTZ);
		if (isZZ) {
			return 1;
		} else if (isLast) {
			return 3;
		} else {
			return 2;
		}
	}
}
