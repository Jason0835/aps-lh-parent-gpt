package com.zlt.mix.schedule.engine.service.glueschedule.impl;

import com.ruoyi.api.gateway.system.domain.ImportErrorLog;
import com.ruoyi.common.core.utils.bean.BeanUtils;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.utils.StringUtils;
import com.zlt.mix.common.core.constant.ZltConstant;
import com.zlt.mix.common.core.utils.BigDecimalUtil;
import com.zlt.mix.common.core.utils.CollectionUtil;
import com.zlt.mix.common.core.utils.GenerageMapKeyUtils;
import com.zlt.mix.common.core.utils.ImportUtil;
import com.zlt.mix.common.engine.service.GlueScheduleEngineLogService;
import com.zlt.mix.schedule.api.domain.entity.GlueScheduleResult;
import com.zlt.mix.schedule.api.domain.entity.GlueSpanReceive;
import com.zlt.mix.schedule.engine.constants.GlueEngineConstants;
import com.zlt.mix.schedule.engine.mapper.GlueScheduleEngineMapper;
import com.zlt.mix.schedule.engine.mapper.SchedulePublishEngineMapper;
import com.zlt.mix.schedule.engine.service.basicdata.*;
import com.zlt.mix.schedule.engine.service.glueschedule.*;
import com.zlt.mix.schedule.engine.util.CombinedMapKey;
import com.zlt.mix.schedule.engine.util.GlueScheduleStockPool;
import com.zlt.mix.schedule.engine.vo.*;
import com.zlt.mix.setting.api.domain.entity.MesBasMaterial;
import com.zlt.mix.setting.api.domain.entity.MixingProductionModel;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;
import java.util.Map.Entry;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 终炼胶母炼胶日计划服务
 * 
 * @author hakimryan
 *
 */
@Service
@Slf4j
public class GlueScheduleEngineServiceImpl implements GlueScheduleEngineService {
	@Autowired
	private GlueScheduleStockService glueScheduleStockService;
	@Autowired
	private GlueScheduleEngineBaseService glueScheduleEngineBaseService;
	@Autowired
	private GlueScheduleEventQueueService glueScheduleEventQueueService;
	@Autowired
	private GlueScheduleEngineModifyService glueScheduleEngineModifyService;
	@Autowired
	private ParamsEngineService paramsEngineService;
	@Autowired
	private RecipeEngineService recipeEngineService;
	@Autowired
	private MachineEngineService machineEngineService;
	@Autowired
	private GlueScheduleEnginePublishService glueScheduleEnginePublishService;
	@Autowired
	private GlueScheduleEngineLogService logService;
	@Autowired
	private GlueSpanEngineService glueSpanEngineService;
	@Autowired
	private GlueScheduleEngineMapper glueScheduleEngineMapper;
	@Autowired
	private SchedulePublishEngineMapper schedulePublishEngineMapper;
	@Autowired
	private GlueScheduleEngineSupplementService glueScheduleEngineSupplementService;
	@Autowired
	private MixingTimeEngineService mixingTimeEngineService;
	@Autowired
	private MixingMinProductEngineService mixingMinProductEngineService;
	@Autowired
	private MixingPriorityProductEngineService mixingPriorityProductEngineService;
	@Autowired
	private MixingProductionModelEngineService mixingProductionModelEngineService;
	@Autowired
	private MixingGlueRecipeMapEngineService mixingGlueRecipeMapEngineService;

	/**
	 * 终炼胶母炼胶日计划自动排程
	 * 
	 * @param scheduleDate 排产日
	 * @param mixArea      密炼区
	 */
	@Override
	@Transactional
	public void autoGlueSchedule(Date scheduleDate, String mixArea) {
		// 查询本区发送且已确认接收的跨区生产请求
		List<GlueSpanReceiveVo> glueSpanSendList = new ArrayList<>();
		// 查询委托给本区且已确认接收的跨区生产请求
		// List<GlueSpanReceiveVo> glueSpanReceiveList = this.listGlueSpanReceive(scheduleDate, mixArea);
		// 加载系统参数
		Map<String, String> params = paramsEngineService.mapGlueParams(mixArea);
		// 查询胶料配方映射的胶料名称Map
		Map<String, String> glueRecipeMap = mixingGlueRecipeMapEngineService.mapGlueRecipe(mixArea);
		// 查询胶料配方映射的胶料映射Map
		Map<String, String> glueRecipeOnlyGlueMap = mixingGlueRecipeMapEngineService.mapGlueRecipeOnlyGlue(mixArea);
		// 查询胶料配方映射反转的Map
		Map<String, String> reserveGlueRecipeMap = mixingGlueRecipeMapEngineService.mapReserveGlueRecipe(mixArea);
		// 当天的库存信息
		GlueScheduleStockPool glueStock = glueScheduleStockService.buildStockPool(scheduleDate, mixArea, reserveGlueRecipeMap);
		// 加载有效的配方信息，配方按胶料 + 机台分组
		MesPmtRecipeVo recipeParams = new MesPmtRecipeVo();
		recipeParams.setMixArea(mixArea);
		List<MesPmtRecipeVo> mesPmtRecipeList = recipeEngineService.listGlueRecipe(recipeParams);
		// 加载胶料间隔时间，按胶料 + 机台分组
		Map<String, Long> mixingTimeMap = mixingTimeEngineService.mapMixingIntervalTime(mixArea);
		// 加载炼胶单规格最小排产数，按胶料分组
		Map<String, BigDecimal> mixingMinProductMap = mixingMinProductEngineService.mapMixingMinProduct(mixArea);
		// // 查询塑料的分解配方
		// Map<String, MesPmtRecipeVo> slRecipeVoMap = recipeEngineService.mapSLGLueRecipe(recipeParams);
		// 查询昨日早班的最后一个排程计划，如果相同胶料还有生产量，尽可能选择相同配方，优先进行排产
		List<GlueScheduleResultVo> latestScheduleList = getLatestScheduleList(scheduleDate, mixArea);
		// 查询炼胶优先配置，优先的胶料先选择配方和机台，对应的胶料后选择，尽可能选择相同的机台的配方
		Map<String, String> mixingPriorityProductMap = mixingPriorityProductEngineService.mapMixingPriorityProduct(mixArea);
		// 查询生产模式列表
		MixingProductionModel productionModel = new MixingProductionModel();
		productionModel.setMixArea(mixArea);
		List<MixingProductionModel> mixingProductionModelList = mixingProductionModelEngineService.selectProductionModelList(productionModel);
		// 记录昨日库存，补全早班的部分，不扣减昨日的日用量
		GlueScheduleStockPool yesterdayGlueStockPool = glueStock.copyStockPool();
		glueScheduleEngineBaseService.caculate16pmEstimateStock(yesterdayGlueStockPool, mixArea, scheduleDate, mesPmtRecipeList, params,
				glueRecipeMap, reserveGlueRecipeMap, false);
		// 根据胶料拆分明细、配方，生成排程结果列表（含未提报胶料的排程记录）
		List<GlueScheduleResultVo> baseScheduleResult = glueScheduleEngineBaseService
				.createBaseScheduleResultList(scheduleDate, mesPmtRecipeList, glueStock, params, mixArea,
						mixingTimeMap, latestScheduleList, mixingPriorityProductMap, mixingProductionModelList,
						glueRecipeMap, reserveGlueRecipeMap, glueRecipeOnlyGlueMap);

		if (baseScheduleResult.isEmpty()) {
			throw new RuntimeException(I18nUtil.getMessage("ui.scheduleResult.autoSchedule.noGlue"));
		}
		// 记录产能分配结果
		logRecordCapacityAllocation(baseScheduleResult);
		// 备份一份原始库存数据（已经算成19点预计库存）
		GlueScheduleStockPool basicGlueStock = glueStock.copyStockPool();
		// 工厂需求量列表
		Map<String, GlueFactoryRequireVo> factoryRequireMap = glueScheduleEngineBaseService
				.buildGlueFactoryRequire(scheduleDate, mixArea, baseScheduleResult, yesterdayGlueStockPool, glueRecipeOnlyGlueMap);
		// 记录工厂需求量
		logRecordFactoryRequireMap(factoryRequireMap);

		// 表示高耗能的物料，在高耗能优先时间优先排程
		MesBasMaterial mesBasMaterial = new MesBasMaterial();
		mesBasMaterial.setIsHighConsumption(GlueEngineConstants.ISORNOT_YES);
		List<MesBasMaterial> mesBasMaterialList = recipeEngineService.selectListBasMaterial(mesBasMaterial);
		Set<String> highConsumptionSet = mesBasMaterialList.stream().map(MesBasMaterial::getMaterialName).filter(StringUtils::isNotBlank).collect(Collectors.toSet());
		baseScheduleResult.forEach(item -> item.setIsHighConsumption(highConsumptionSet.contains(item.getGlue()) ? GlueEngineConstants.ISORNOT_YES : GlueEngineConstants.ISORNOT_NO));

		// 需要塑胶排产后的优先排产的记录，key 机台+胶料+配方类型+配方版本，value 需要胶料的塑胶列表
		// 长度为0的value表示塑胶的库存已充足，null的value表示无需塑胶
		Map<String, List<GlueScheduleResultVo>> needSlScheduleMap = new HashMap<>();
		// 处理塑胶映射，保证塑胶的优先级比对应胶料的优先级更高
		Map<String, List<GlueScheduleResultVo>> slPriorityMap = glueScheduleEngineBaseService.buildSlPriorityMap(baseScheduleResult, needSlScheduleMap);
		// 使用事件队列进行排产运算
		glueScheduleEventQueueService.excuteEventQueue(baseScheduleResult, glueSpanSendList, glueStock, scheduleDate,
				params, factoryRequireMap, mixingTimeMap, mixingMinProductMap, slPriorityMap, needSlScheduleMap, latestScheduleList, mixingPriorityProductMap, mesPmtRecipeList);

		// 移除掉无法排下的计划
		glueScheduleEngineBaseService.removeNoSchedule(baseScheduleResult);

		// 计算生产顺序
		this.sortScheduleResult(baseScheduleResult);

		// 添加未提报胶料的排程记录
		GlueScheduleResultVo schedule = CollectionUtil.firstElement(baseScheduleResult);
		String batchNo = schedule.getBatchNo();
		List<GlueScheduleResultVo> noRequireSchedule = glueScheduleEngineBaseService
				.createNoRequireSchedule(scheduleDate, basicGlueStock, mixArea, batchNo, baseScheduleResult);
		baseScheduleResult.addAll(noRequireSchedule);
		// 更新总剩余量
		this.batchRecaculateTotalSurplus(baseScheduleResult, scheduleDate, mixArea);

		// 根据跨区接收情况生成排产记录
		// glueScheduleEngineModifyService.createGlueSpanReceiveSchedule(scheduleDate, mixArea, batchNo,
		// 		glueSpanReceiveList, baseScheduleResult, mesPmtRecipeList, basicGlueStock, params);
		glueScheduleEngineSupplementService.surplusQtySuppliment(scheduleDate, mixArea, baseScheduleResult,
				glueStock, factoryRequireMap, params, mixingTimeMap, slPriorityMap,
				needSlScheduleMap, latestScheduleList, mixingPriorityProductMap, mesPmtRecipeList);

		// 记录预计排产后库存车数
		logRecordStockQty(baseScheduleResult, glueStock);

        // 如果是WA的ZZ配方，直接去掉胶料编号的WA后缀
        if (CollectionUtils.isNotEmpty(baseScheduleResult)) {
            for (GlueScheduleResultVo itemVo : baseScheduleResult) {
                if (itemVo.getGlue() != null
//                        && !itemVo.getGlue().equals("31379WA")
                        && itemVo.getGlue().endsWith("WA")
                        && GlueEngineConstants.RECIPE_TYPE_ZZ.equals(itemVo.getRecipeTypeName())) {
                    itemVo.setGlue(itemVo.getGlue().substring(0, itemVo.getGlue().length() - 2));
                }
            }
        }
		
//		if (1 == 1) {// 调试用
//			throw new RuntimeException();
//		}

		logService.save(batchNo, "终炼母炼自动排程"); // 保存排程日志
		
		// 删除当天的补量记录
		glueScheduleEngineMapper.deleteGlueScheduleSupplement(scheduleDate, mixArea);

		// 计划入库
		glueScheduleEngineMapper.copyScheduleResultListToLog(scheduleDate, mixArea); // 历史数据复制到日志表
		glueScheduleEngineMapper.deleteScheduleResultList(scheduleDate, mixArea); // 删除历史数据
		glueScheduleEngineMapper.batchInsertScheduleResult(baseScheduleResult); // 批量插入排程记录
		
		// 将计划拷贝至排程初始日志表中
		glueScheduleEngineMapper.deleteScheduleInitLogList(scheduleDate, mixArea); // 清理原有数据
		glueScheduleEngineMapper.copyScheduleResultListToInitLog(scheduleDate, mixArea); // 记录新排产数据
	}

	/**
	 * 记录工厂需求量
	 */
	private void logRecordFactoryRequireMap(Map<String, GlueFactoryRequireVo> factoryRequireMap) {
		if (factoryRequireMap == null || factoryRequireMap.isEmpty()) {
			return;
		}
		StringBuilder builder = new StringBuilder();
		builder.append("工厂需求量:\n");

		factoryRequireMap.forEach((glueCode, requireVo) -> {
			builder.append(requireVo.getGlue()).append("+").append(requireVo.getRequireDifference())
					.append("+").append(requireVo.getRequireClass()).append("\n");
		});
		logService.record(builder.toString());
	}

	/**
	 * 记录产能分配结果
	 */
	private void logRecordCapacityAllocation(List<GlueScheduleResultVo> baseScheduleResult) {
		if (CollectionUtil.isEmpty(baseScheduleResult)) {
			return;
		}
		StringBuilder builder = new StringBuilder();
		Map<String, BigDecimal> groupMap = baseScheduleResult.stream().filter(v -> v.getPlanQty() != null && v.getPlanQty().compareTo(BigDecimal.ZERO) > 0)
				.collect(Collectors.groupingBy(v -> GenerageMapKeyUtils.createMapKey(v.getGlue(), v.getMachineCode(), v.getRecipeTypeName()),
						Collectors.mapping(GlueScheduleResultVo::getPlanQty, Collectors.reducing(BigDecimal.ZERO, BigDecimal::add))));
		builder.append("机台产能分配结果:\n");
		groupMap.forEach((groupKey, planQty) -> {
			builder.append(groupKey).append("=").append(planQty).append("\n");
		});
		logService.record(builder.toString());
	}

	/**
	 * 记录预计排产后库存车数
	 *
	 * @param baseScheduleResult 排程记录
	 * @param glueStock          库存信息
	 */
	private void logRecordStockQty(List<GlueScheduleResultVo> baseScheduleResult, GlueScheduleStockPool glueStock) {
		// 母炼库存
		Set<String> mlGlue = baseScheduleResult.stream()
				.filter(v -> GlueEngineConstants.MAJOR_TYPE_ML.equals(v.getMajorType()) || GlueEngineConstants.MAJOR_TYPE_SL.equals(v.getMajorType()))
				.map(GlueScheduleResult::getGlue).collect(Collectors.toSet());
		StringBuilder mlBuilder = new StringBuilder();
		mlBuilder.append("母炼库存:\n");
		for (String glue : mlGlue) {
			mlBuilder.append(glue).append(" ")
					.append(glueStock.getStockNum(glue, GlueEngineConstants.MAJOR_TYPE_ML)).append(" ")
					.append(glueStock.getStockWeight(glue, GlueEngineConstants.MAJOR_TYPE_ML)).append("\n");
		}
		logService.record(mlBuilder.toString());

		// 终炼库存
		Set<String> zlGlue = baseScheduleResult.stream().filter(v -> GlueEngineConstants.MAJOR_TYPE_ZL.equals(v.getMajorType())).map(GlueScheduleResult::getGlue).collect(Collectors.toSet());
		StringBuilder zlBuilder = new StringBuilder();
		zlBuilder.append("终炼库存:\n");
		for (String glue : zlGlue) {
			zlBuilder.append(glue).append(" ").append(glueStock.getStockNum(glue, GlueEngineConstants.MAJOR_TYPE_ZL)).append("\n");
		}
		logService.record(zlBuilder.toString());
	}

	/**
	 * 查询昨日早班的最后一个排程计划，如果相同胶料还有生产量，尽可能选择相同配方，优先进行排产
	 *
	 * @param scheduleDate 排程日
	 * @param mixArea      密炼区
	 * @return 昨日早班的最后一个排程计划
	 */
	private List<GlueScheduleResultVo> getLatestScheduleList(Date scheduleDate, String mixArea) {
		Date date = DateUtils.addDays(scheduleDate, -1);
		// 目前把数据放在原白班字段，便于和今日预排排程区分
		List<GlueScheduleResultVo> latestScheduleList = glueScheduleEngineMapper.selectLatestScheduleList(date, mixArea);
		for (GlueScheduleResultVo glueScheduleResultVo : latestScheduleList) {
			if (glueScheduleResultVo.getDayExpectStartTime() == null) {
				glueScheduleResultVo.setDayExpectStartTime(date);
			}
			if (glueScheduleResultVo.getDayExpectFinishTime() == null) {
				glueScheduleResultVo.setDayExpectFinishTime(date);
			}
		}
		return latestScheduleList;
	}

	/**
	 * 查询本区接收的跨区生产请求
	 * 
	 * @param scheduleDate
	 * @param mixArea
	 * @return
	 */
	private List<GlueSpanReceiveVo> listGlueSpanReceive(Date scheduleDate, String mixArea) {
		GlueSpanReceiveVo glueSpanReceiveParam = new GlueSpanReceiveVo();
		glueSpanReceiveParam.setEntrustedMixArea(mixArea);
		glueSpanReceiveParam.setScheduleDate(scheduleDate);
		List<GlueSpanReceiveVo> glueSpanReceiveList = glueSpanEngineService.listGlueSpanReceive(glueSpanReceiveParam);
		// 要过滤掉未接收的以及接收数不正确的数据
		return glueSpanReceiveList.stream()
				// 过滤掉未接收的
				.filter(glueSpan -> ZltConstant.RECEIVE_STATUS_YES.equals(glueSpan.getReceiveStatus()))
				// 过滤掉不合法的数据
				.filter(g -> g.getReceiveQty() != null && g.getReceiveQty() > 0 && g.getGlue() != null
						&& g.getMachineCode() != null && g.getRecipeType() != null)
				.collect(Collectors.toList());
	}

	/**
	 * 查询本区发送且已确认接收的跨区生产请求
	 * 
	 * @param scheduleDate
	 * @param mixArea
	 * @return
	 */
	private List<GlueSpanReceiveVo> listGlueSpanSend(Date scheduleDate, String mixArea) {
		// 查询本区在排产日发送的跨区生产请求
		GlueSpanReceiveVo glueSpanReceiveParam = new GlueSpanReceiveVo();
		glueSpanReceiveParam.setEntrustMixArea(mixArea);
		glueSpanReceiveParam.setScheduleDate(scheduleDate);
		List<GlueSpanReceiveVo> glueSpanReceiveList = glueSpanEngineService.listGlueSpanReceive(glueSpanReceiveParam)
				.stream().filter(s -> Optional.ofNullable(s.getSendQty()).orElse(0L) > 0).collect(Collectors.toList()); // 只要请求数大于0的
		for (GlueSpanReceiveVo glueSpan : glueSpanReceiveList) {
			// 只要有发送，不管对方是否有接收，都当作对方已全量接收 20221119修改 hak
			glueSpan.setReceiveStatus(ZltConstant.RECEIVE_STATUS_YES);
			Long receiveQty = glueSpan.getReceiveQty() != null? glueSpan.getReceiveQty(): 0L;
			glueSpan.setReceiveQty(BigDecimalUtil.greatest(receiveQty, glueSpan.getSendQty()));
		}
		return glueSpanReceiveList;
	}

	/**
	 * 批量重算整个排程的预计剩余量
	 * 
	 * @param scheduleResultList 待重算排程列表
	 * @param scheduleDate       排产日
	 * @param mixArea            密炼区
	 */
	private void batchRecaculateTotalSurplus(List<GlueScheduleResultVo> scheduleResultList, Date scheduleDate,
			String mixArea) {
		// 获取排产日的胶料分解表
		List<GlueDecomposePlanVo> decomposeList = glueScheduleEngineMapper.selectGlueDecomposePlan(scheduleDate,
				mixArea, null);
		// 统计胶料分解计划中的总生产量，以胶料号为维度进行统计
		Map<String, Double> produceQtyMap = decomposeList.stream()
				// 过滤掉没有计划生产量的分解计划
				.filter(plan -> plan.getProduceQty() != null)
				// 以胶料号为维度分组统计
				.collect(Collectors.groupingBy(GlueDecomposePlanVo::getGlue,
						Collectors.summingDouble(GlueDecomposePlanVo::getProduceQty)));
		// 遍历排产列表
		scheduleResultList.stream()
				// 过滤掉未提报记录
				.filter(schedule -> schedule.getMachineCode() != null && schedule.getRecipeType() != null)
				.collect(Collectors.groupingBy(GlueScheduleResultVo::getGlue)).entrySet().stream().forEach(entry -> {
					String glueCode = entry.getKey();
					List<GlueScheduleResultVo> scheduleList = entry.getValue();
					Double produceQty = produceQtyMap.get(glueCode);
					if (produceQty == null || produceQty <= 0) {
						// 如果没有生产量，则不需要重算
						return;
					}
					// 更新总剩余量
					this.updateTotalSurplus(produceQty, scheduleList);
				});
	}

	/**
	 * 插单
	 * 
	 * @param resultList 待插单数据
	 */
	@Override
	@Transactional
	public List<GlueScheduleResult> insertOrder(GlueScheduleResult scheduleResult) {
		Date scheduleDate = scheduleResult.getScheduleDate();
		String mixArea = scheduleResult.getMixArea();
		String machineCode = scheduleResult.getMachineCode();
		String glueCode = scheduleResult.getGlue();
		String recipeType = scheduleResult.getRecipeType();
		// 插单数据校验
		if (scheduleDate == null || mixArea == null || machineCode == null || glueCode == null || recipeType == null) {
			throw new RuntimeException(I18nUtil.getMessage("ui.scheduleResult.insertOrder.dataError"));
		}
		Double midPlanQty = Optional.ofNullable(scheduleResult.getMidPlanQty()).orElse(0D);
		Double nightPlanQty = Optional.ofNullable(scheduleResult.getNightPlanQty()).orElse(0D);
		Double dayPlanQty = Optional.ofNullable(scheduleResult.getDayPlanQty()).orElse(0D);
		boolean isProductOrderNull = true;
		if (midPlanQty > 0 && scheduleResult.getMidProduceOrder() != null) {
			isProductOrderNull = false;
		}
		if (nightPlanQty > 0 && scheduleResult.getNightProduceOrder() != null) {
			isProductOrderNull = false;
		}
		if (dayPlanQty > 0 && scheduleResult.getDayProduceOrder() != null) {
			isProductOrderNull = false;
		}
		if (isProductOrderNull) {
			throw new RuntimeException(I18nUtil.getMessage("ui.scheduleResult.insertOrder.produceOrderError"));
		}
		if (midPlanQty <= 0 && nightPlanQty <= 0 && dayPlanQty <= 0) {
			throw new RuntimeException(I18nUtil.getMessage("ui.scheduleResult.insertOrder.planQtyLTZero"));
		}

		// 查询当天的所有排产计划
		GlueScheduleResultVo scheduleParams = new GlueScheduleResultVo();
		scheduleParams.setMixArea(mixArea);
		scheduleParams.setScheduleDate(scheduleDate);
		List<GlueScheduleResultVo> allScheduleList = glueScheduleEngineMapper.selectScheduleResult(scheduleParams);
		// 查询当天该机台的已排产信息
		List<GlueScheduleResultVo> machineScheduleList = allScheduleList.stream()
				.filter(result -> machineCode.equals(result.getMachineCode())).collect(Collectors.toList());

		// 加载系统参数
		Map<String, String> glueParams = paramsEngineService.mapGlueParams(mixArea);

		// 查询胶料配方映射反转的Map
		Map<String, String> reserveGlueRecipeMap = mixingGlueRecipeMapEngineService.mapReserveGlueRecipe(mixArea);
		// 加载当天的库存信息
		GlueScheduleStockPool glueStock = glueScheduleStockService.buildStockPool(scheduleDate, mixArea, reserveGlueRecipeMap);

		// 加载配方数据
		MesPmtRecipeVo recipeParams = new MesPmtRecipeVo();
		recipeParams.setMixArea(mixArea);
		recipeParams.setIsModify(true); // 获取插单时可选的配方
		List<MesPmtRecipeVo> recipeList = recipeEngineService.listGlueRecipe(recipeParams);
		MesPmtRecipeVo recipe = recipeList
				.stream().filter(r -> glueCode.equals(r.getRecipeMaterialName())
						&& machineCode.equals(r.getRecipeEquipCode()) && recipeType.equals(r.getRecipeType()))
				.findAny().orElse(null);
		if (recipe == null) {
			throw new RuntimeException(I18nUtil.getMessage("ui.scheduleResult.noRecipt"));
		}

		// 如果插单ID给个初始值用于识别
		scheduleResult.setId(glueStock.nextId());

		// 插单排程类型转换
		GlueScheduleResultVo scheduleResultVo = new GlueScheduleResultVo();
		BeanUtils.copyProperties(scheduleResult, scheduleResultVo);

		// 调用日计划更改服务的插单功能，获取本次需求插入/更新的所有排程记录
		List<GlueScheduleResultVo> scheduleResultList = glueScheduleEngineModifyService.insertOrder(scheduleResultVo,
				recipe, glueStock, glueParams, machineScheduleList);

		// 排程合并列表
		List<GlueScheduleResultVo> scheduleMergeList = scheduleResultList;
		// 联动修改母胶计划
		if (scheduleResult.getIsChangeMasterbatch() != null && scheduleResult.getIsChangeMasterbatch()) {
			Set<String> modifyGlueSet = new HashSet<>();
			modifyGlueSet.add(glueCode);
			Map<String, List<MesPmtRecipeVo>> recipeMap = recipeList.stream()
					.collect(Collectors.groupingBy(MesPmtRecipeVo::getRecipeMaterialName));
			// 级联更新子胶料计划量
			List<GlueScheduleResultVo> cascadeUpdateList = glueScheduleEngineModifyService
					.cascadeUpdateChildGlueSchedule(scheduleResultVo, glueCode, allScheduleList, recipeMap, glueParams,
							glueStock, modifyGlueSet);
			// 替换掉排程对象
			this.replaceSchedule(cascadeUpdateList, allScheduleList);
			// 将修改信息合并进去
			scheduleMergeList = this.mergeUpdateScheduleList(cascadeUpdateList, scheduleMergeList);
		}

		// 本次更新涉及胶料集合
		Set<String> updateGlueSet = scheduleMergeList.stream().map(GlueScheduleResultVo::getGlue)
				.collect(Collectors.toSet());
		List<GlueScheduleResultVo> updateSurplusList = this.recaculateTotalSurplus(updateGlueSet, allScheduleList,
				scheduleDate, mixArea); // 重算总剩余量
		// 合并需要更新完成量以及预计时间的排程记录
		List<GlueScheduleResultVo> mergeList = this.mergeUpdateScheduleList(scheduleMergeList, updateSurplusList);

		// 数据入库
		glueScheduleEngineMapper.mergeScheduleResult(mergeList);

		// 本次联动新增的记录需要把已入库的ID刷到对象上
		this.fitIsAddNewScheduleId(mergeList);

		// 修改排程记录需要转换类型
		List<GlueScheduleResult> finalResultList = mergeList.stream().map(schedule -> {
			GlueScheduleResult target = new GlueScheduleResult();
			BeanUtils.copyProperties(schedule, target);
			if (target.getIsAddNew() == null) {
				target.setIsAddNew(false);
			}
			return target;
		}).collect(Collectors.toList());
		return finalResultList;
	}

	/**
	 * 从所有排程列表取出ID相同的排程对象，并替换到待处理列表中
	 * 
	 * @param scheduleList    待处理的排程列表
	 * @param allScheduleList 所有排程列表
	 */
	private void replaceSchedule(List<GlueScheduleResultVo> scheduleList, List<GlueScheduleResultVo> allScheduleList) {
		Map<Long, GlueScheduleResultVo> allScheduleMap = allScheduleList.stream()
				.collect(Collectors.toMap(GlueScheduleResultVo::getId, Function.identity()));
		for (int i = scheduleList.size() - 1; i >= 0; i--) {
			GlueScheduleResultVo schedule = scheduleList.get(i);
			if (schedule.getId() != null && allScheduleMap.containsKey(schedule.getId())) {
				GlueScheduleResultVo oldSchedule = allScheduleMap.get(schedule.getId());
				scheduleList.remove(i);
				scheduleList.add(i, oldSchedule);
			}
		}
	}

	/**
	 * 导入排程
	 * 
	 * @param resultList 待插单数据
	 */
	@Override
	@Transactional
	public List<ImportErrorLog> importSchedule(List<GlueScheduleResult> resultList) {
		// 查询库中是否存在当天的排程
		GlueScheduleResult firstResult = CollectionUtil.firstElement(resultList);
		String mixArea = firstResult.getMixArea();
		Date scheduleDate = firstResult.getScheduleDate();
		GlueScheduleResultVo params = new GlueScheduleResultVo();
		params.setScheduleDate(scheduleDate);
		params.setMixArea(mixArea);

		// 加载系统参数
		Map<String, String> glueParams = paramsEngineService.mapGlueParams(mixArea);
		// 查询胶料配方映射反转的Map
		Map<String, String> reserveGlueRecipeMap = mixingGlueRecipeMapEngineService.mapReserveGlueRecipe(mixArea);
		// 加载当天的库存信息
		GlueScheduleStockPool glueStock = glueScheduleStockService.buildStockPool(scheduleDate, mixArea, reserveGlueRecipeMap);
		// 加载炼胶间隔时间
		Map<String, Long> intervalTimeMap = mixingTimeEngineService.mapMixingIntervalTime(mixArea);
		
		// 加载配方数据
		MesPmtRecipeVo recipeParams = new MesPmtRecipeVo();
		recipeParams.setMixArea(mixArea);
		recipeParams.setIsModify(true); // 获取导入时可选的配方
		List<MesPmtRecipeVo> mesPmtRecipeList = recipeEngineService.listGlueRecipe(recipeParams);
		Map<CombinedMapKey, MesPmtRecipeVo> recipeMap = mesPmtRecipeList.stream().collect(Collectors.toMap(
				r -> CombinedMapKey.createKey(r.getRecipeMaterialName(), r.getRecipeEquipCode(), r.getRecipeType()),
				Function.identity(), (r1, r2) -> r1));
		// 没有就生成一个批次号
		String batchNo = glueScheduleEngineBaseService.createBatchNo(scheduleDate, mixArea);

		// 处理掉导入时生产顺序的0值
		this.handleImportProduceOrder(resultList);

		// 根据机台 + 班别 + 序号汇总Map，用于校验数据次序顺序合法性
		Map<CombinedMapKey, List<GlueScheduleResult>> repeatOrderMap = this.buildRepeatOrderCheckMap(resultList);

		List<ImportErrorLog> importErrorLogs = new ArrayList<>(); // 错误日志列表
		List<GlueScheduleResultVo> insertList = new ArrayList<>(); // 需要插单的记录

		// 遍历生成数据
		for (int rowNum = 0, size = resultList.size(); rowNum < size; rowNum++) {
			GlueScheduleResult newResult = resultList.get(rowNum);
			String glue = newResult.getGlue();
			String machineCode = newResult.getMachineCode();
			String recipeType = newResult.getRecipeType();
			// 校验每个班的顺序是否重复
			if (!checkProductOrderRepeate(newResult, repeatOrderMap, rowNum, importErrorLogs)) {
				continue; // 校验不通过则只记录错误日志
			}
			// 胶料 + 机台 + 配方校验是否已经存在
			MesPmtRecipeVo recipe = recipeMap.get(CombinedMapKey.createKey(glue, machineCode, recipeType));
			if (recipe == null) {
				String errorMessage = I18nUtil.getMessage("schedule.glueScheduleResult.message.import.recipeNotExist");
				errorMessage = StringUtils.format(errorMessage, mixArea, newResult.getMachineName(),
						newResult.getGlue(), newResult.getRecipeTypeName());
				log.error(errorMessage);
				ImportUtil.addImportErrorLog(null, rowNum, errorMessage, importErrorLogs);
				continue;
			}

			GlueScheduleResultVo result = new GlueScheduleResultVo();
			BeanUtils.copyProperties(newResult, result);
			result.setBaseValue(null);
			result.setBatchNo(batchNo);
			result.setOrderNo(glueScheduleEngineBaseService.createOrderNo(batchNo));
			glueScheduleEngineBaseService.copyRecipeProperties(result, recipe);
			result.setPublishSuccessCount(0);
			result.setReleaseStatus(ZltConstant.NO_RELEASE);
			result.setDataSource(ZltConstant.GLUE_SCHEDULE_SOURCE_IMPORT);
			result.setTotalPlanQty(
					BigDecimalUtil.add(result.getMidPlanQty(), result.getNightPlanQty(), result.getDayPlanQty()));
			// 设置库存
			BigDecimal stockNum = Optional.ofNullable(glueStock.getStockNum(glue, recipe.getMajorType()))
					.orElse(BigDecimal.ZERO);
			BigDecimal safeStockQty = Optional.ofNullable(glueStock.getSafeStock(glue)).orElse(BigDecimal.ZERO);
			result.setStockQty(stockNum.doubleValue());
			result.setSafeStockQty(safeStockQty.doubleValue());
			// 记录指定的间隔时间
			Long intervalTime = intervalTimeMap.get(GenerageMapKeyUtils.createMapKey(result.getGlue(), result.getMachineCode()));
			result.setIntervalTime(intervalTime != null ? BigDecimal.valueOf(intervalTime) : null);

			insertList.add(result);
		}
		glueScheduleEngineMapper.copyScheduleResultListToLog(scheduleDate, mixArea); // 历史数据复制到日志表
		glueScheduleEngineMapper.deleteScheduleResultList(scheduleDate, mixArea); // 保存前先删除历史数据
		if (!CollectionUtil.isEmpty(insertList)) {
			// 全部重算时间
			glueScheduleEngineModifyService.recaculateExpectTimeInList(scheduleDate, insertList, glueParams);
			// 批量重算总剩余量
			this.batchRecaculateTotalSurplus(insertList, scheduleDate, mixArea);
			glueScheduleEngineMapper.batchInsertScheduleResult(insertList);
			
			// 将计划拷贝至排程初始日志表中
			glueScheduleEngineMapper.deleteScheduleInitLogList(scheduleDate, mixArea); // 清理原有数据
			glueScheduleEngineMapper.copyScheduleResultListToInitLog(scheduleDate, mixArea); // 记录新排产数据
		}
		return importErrorLogs;
	}

	/**
	 * 处理导入排程数据生产顺序的0值
	 * 
	 * @param resultList
	 */
	private void handleImportProduceOrder(List<GlueScheduleResult> resultList) {
		for (GlueScheduleResult result : resultList) {
			// 中班
			Integer midProduceOrder = result.getMidProduceOrder();
			Double midPlanQty = result.getMidPlanQty();
			// 当生产顺序为0，且计划量为0时，需要把生产顺序置成空
			if (midProduceOrder != null && midProduceOrder.intValue() == 0
					&& BigDecimalUtil.valueOfZero(midPlanQty).compareTo(BigDecimal.ZERO) == 0) {
				result.setMidProduceOrder(null);
			}
			// 夜班
			Integer nightProduceOrder = result.getNightProduceOrder();
			Double nightPlanQty = result.getNightPlanQty();
			if (nightProduceOrder != null && nightProduceOrder.intValue() == 0
					&& BigDecimalUtil.valueOfZero(nightPlanQty).compareTo(BigDecimal.ZERO) == 0) {
				result.setNightProduceOrder(null);
			}
			// 白班
			Integer dayProduceOrder = result.getDayProduceOrder();
			Double dayPlanQty = result.getDayPlanQty();
			if (dayProduceOrder != null && dayProduceOrder.intValue() == 0
					&& BigDecimalUtil.valueOfZero(dayPlanQty).compareTo(BigDecimal.ZERO) == 0) {
				result.setDayProduceOrder(null);
			}
		}
	}

	/**
	 * 校验每个班的顺序是否重复
	 * 
	 * @param schedule        排程记录
	 * @param repeatOrderMap  重复顺序排程记录
	 * @param rowNum          行号
	 * @param importErrorLogs 错误日志列表
	 * @return
	 */
	private boolean checkProductOrderRepeate(GlueScheduleResult schedule,
			Map<CombinedMapKey, List<GlueScheduleResult>> repeatOrderMap, int rowNum,
			List<ImportErrorLog> importErrorLogs) {
		// 中班顺序
		CombinedMapKey checkKey = this.createRepeatOrderKey(schedule, GlueEngineConstants.SHIFT_CLASS_MID);
		List<GlueScheduleResult> repeatList = repeatOrderMap.get(checkKey);
		if (repeatList != null && repeatList.size() > 1) { // 超过1行则说明有重复，需要返回校验失败并记录错误日志
			String errorMessage = I18nUtil
					.getMessage("schedule.glueScheduleResult.message.import.midProductOrderRepeat");
			ImportUtil.addImportErrorLog(null, rowNum, errorMessage, importErrorLogs);
			return false;
		}
		// 夜班顺序
		checkKey = this.createRepeatOrderKey(schedule, GlueEngineConstants.SHIFT_CLASS_NIGHT);
		repeatList = repeatOrderMap.get(checkKey);
		if (repeatList != null && repeatList.size() > 1) { // 超过1行则说明有重复，需要返回校验失败并记录错误日志
			String errorMessage = I18nUtil
					.getMessage("schedule.glueScheduleResult.message.import.nightProductOrderRepeat");
			ImportUtil.addImportErrorLog(null, rowNum, errorMessage, importErrorLogs);
			return false;
		}
		// 白班顺序
		checkKey = this.createRepeatOrderKey(schedule, GlueEngineConstants.SHIFT_CLASS_DAY);
		repeatList = repeatOrderMap.get(checkKey);
		if (repeatList != null && repeatList.size() > 1) { // 超过1行则说明有重复，需要返回校验失败并记录错误日志
			String errorMessage = I18nUtil
					.getMessage("schedule.glueScheduleResult.message.import.dayProductOrderRepeat");
			ImportUtil.addImportErrorLog(null, rowNum, errorMessage, importErrorLogs);
			return false;
		}
		return true;
	}

	/**
	 * 根据机台 + 班别 + 序号汇总Map，用于校验数据次序顺序合法性
	 * 
	 * @param resultList
	 * @return
	 */
	private Map<CombinedMapKey, List<GlueScheduleResult>> buildRepeatOrderCheckMap(
			List<GlueScheduleResult> resultList) {
		Map<CombinedMapKey, List<GlueScheduleResult>> repeatOrderMap = new HashMap<>();
		// 分别取出各班顺序不为空的记录，并根据 机台 + 班别 + 序号 汇总
		Map<CombinedMapKey, List<GlueScheduleResult>> repeatMidOrderMap = resultList.stream()
				.filter(schedule -> schedule.getMidProduceOrder() != null
						&& Optional.ofNullable(schedule.getMidPlanQty()).orElse(0D) > 0)
				.collect(Collectors.groupingBy(
						schedule -> this.createRepeatOrderKey(schedule, GlueEngineConstants.SHIFT_CLASS_MID)));
		Map<CombinedMapKey, List<GlueScheduleResult>> repeatNightOrderMap = resultList.stream()
				.filter(schedule -> schedule.getNightProduceOrder() != null
						&& Optional.ofNullable(schedule.getNightPlanQty()).orElse(0D) > 0)
				.collect(Collectors.groupingBy(
						schedule -> this.createRepeatOrderKey(schedule, GlueEngineConstants.SHIFT_CLASS_NIGHT)));
		Map<CombinedMapKey, List<GlueScheduleResult>> repeatDayOrderMap = resultList.stream()
				.filter(schedule -> schedule.getDayProduceOrder() != null
						&& Optional.ofNullable(schedule.getDayPlanQty()).orElse(0D) > 0)
				.collect(Collectors.groupingBy(
						schedule -> this.createRepeatOrderKey(schedule, GlueEngineConstants.SHIFT_CLASS_DAY)));
		repeatOrderMap.putAll(repeatMidOrderMap);
		repeatOrderMap.putAll(repeatNightOrderMap);
		repeatOrderMap.putAll(repeatDayOrderMap);
		return repeatOrderMap;
	}

	/**
	 * 构建对应班次的重复生产顺序key
	 * 
	 * @param schedule   排产记录
	 * @param shiftClass 工序
	 * @return
	 */
	private CombinedMapKey createRepeatOrderKey(GlueScheduleResult schedule, int shiftClass) {
		switch (shiftClass) {
		case GlueEngineConstants.SHIFT_CLASS_MID:
			return CombinedMapKey.createKey(schedule.getMachineCode(), shiftClass, schedule.getMidProduceOrder());
		case GlueEngineConstants.SHIFT_CLASS_NIGHT:
			return CombinedMapKey.createKey(schedule.getMachineCode(), shiftClass, schedule.getNightProduceOrder());
		case GlueEngineConstants.SHIFT_CLASS_DAY:
			return CombinedMapKey.createKey(schedule.getMachineCode(), shiftClass, schedule.getDayProduceOrder());
		default:
			return null;
		}
	}

	/**
	 * 转机台
	 * 
	 * @param scheduelResult 更新后的排产记录
	 */
	@Override
	@Transactional
	public List<GlueScheduleResult> changeMachine(List<GlueScheduleResult> resultList) {
		// 对待转机台的排程记录按密炼区 + 排产日 + 机台 分组，如果有多个，说明数据有异常
		Set<CombinedMapKey> keySet = resultList.stream()
				.map(scheduleResult -> CombinedMapKey.createKey(scheduleResult.getMixArea(),
						scheduleResult.getScheduleDate(), scheduleResult.getMachineCode()))
				.collect(Collectors.toSet());
		if (keySet.size() != 1) {
			throw new RuntimeException(I18nUtil.getMessage("ui.scheduleResult.changeMachine.mixAreaError"));
		}
		CombinedMapKey key = CollectionUtil.firstElement(keySet);
		String mixArea = (String) key.getKey(0);
		Date scheduleDate = (Date) key.getKey(1);
		String machineCode = (String) key.getKey(2);
		if (mixArea == null || scheduleDate == null || machineCode == null) {
			throw new RuntimeException(I18nUtil.getMessage("ui.scheduleResult.changeMachine.dataError"));
		}
		// 类型转换
		List<GlueScheduleResultVo> resultVoList = resultList.stream().map(result -> {
			GlueScheduleResultVo newResult = new GlueScheduleResultVo();
			BeanUtils.copyProperties(result, newResult);
			newResult.setGlueScheduleResult(result);
			return newResult;
		}).collect(Collectors.toList());
		// 查询当天的已排产信息
		GlueScheduleResultVo scheduleParams = new GlueScheduleResultVo();
		scheduleParams.setMixArea(mixArea);
		scheduleParams.setScheduleDate(scheduleDate);
		List<GlueScheduleResultVo> allScheduleList = glueScheduleEngineMapper.selectScheduleResult(scheduleParams);

		// 加载系统参数
		Map<String, String> glueParams = paramsEngineService.mapGlueParams(mixArea);

		// 加载配方数据
		MesPmtRecipeVo recipeParams = new MesPmtRecipeVo();
		recipeParams.setMixArea(mixArea);
		recipeParams.setIsModify(true); // 获取新增/修改时可选的配方
		List<MesPmtRecipeVo> recipeList = recipeEngineService.listGlueRecipe(recipeParams);

		// 触发转机台
		List<GlueScheduleResultVo> scheduleResultList = glueScheduleEngineModifyService.changeMachine(resultVoList,
				recipeList, glueParams, allScheduleList, true);

		// 查询胶料配方映射反转的Map
		Map<String, String> reserveGlueRecipeMap = mixingGlueRecipeMapEngineService.mapReserveGlueRecipe(mixArea);
		// 加载当天的库存信息
		GlueScheduleStockPool glueStock = glueScheduleStockService.buildStockPool(scheduleDate, mixArea, reserveGlueRecipeMap);
		// 排程合并列表
		List<GlueScheduleResultVo> scheduleMergeList = scheduleResultList;
		// 更新原记录的预计时间
		for (GlueScheduleResultVo result : resultVoList) {
			allScheduleList.add(result); // 转机台后新增的记录都加到总列表中
			GlueScheduleResult scheduleReulst = result.getGlueScheduleResult();
			// 联动修改母胶计划
			if (result.getIsChangeMasterbatch() != null && result.getIsChangeMasterbatch()) {
				Set<String> modifyGlueSet = new HashSet<>();
				modifyGlueSet.add(result.getGlue());
				Map<String, List<MesPmtRecipeVo>> recipeMap = recipeList.stream()
						.collect(Collectors.groupingBy(MesPmtRecipeVo::getRecipeMaterialName));
				// 级联更新子胶料计划量
				List<GlueScheduleResultVo> cascadeUpdateList = glueScheduleEngineModifyService
						.cascadeUpdateChildGlueSchedule(result, result.getGlue(), allScheduleList, recipeMap,
								glueParams, glueStock, modifyGlueSet);
				// 替换掉排程对象
				this.replaceSchedule(cascadeUpdateList, allScheduleList);
				// 将修改信息合并进去
				scheduleMergeList = this.mergeUpdateScheduleList(cascadeUpdateList, scheduleMergeList);
			}

			if (scheduleReulst != null) {
				scheduleReulst.setMidExpectFinishTime(result.getMidExpectFinishTime());
				scheduleReulst.setMidExpectStartTime(result.getMidExpectStartTime());
				scheduleReulst.setNightExpectFinishTime(result.getNightExpectFinishTime());
				scheduleReulst.setNightExpectStartTime(result.getNightExpectStartTime());
				scheduleReulst.setDayExpectFinishTime(result.getDayExpectFinishTime());
				scheduleReulst.setDayExpectStartTime(result.getDayExpectStartTime());
			}
		}

		// 本次更新涉及胶料集合
		Set<String> updateGlueSet = scheduleMergeList.stream().map(GlueScheduleResultVo::getGlue)
				.collect(Collectors.toSet());
		List<GlueScheduleResultVo> updateSurplusList = this.recaculateTotalSurplus(updateGlueSet, allScheduleList,
				scheduleDate, mixArea); // 重算总剩余量
		// 合并需要更新完成量以及预计时间的排程记录
		scheduleMergeList = this.mergeUpdateScheduleList(scheduleMergeList, updateSurplusList);

		glueScheduleEngineMapper.mergeScheduleResult(scheduleMergeList);

		// 本次联动新增的记录需要把已入库的ID刷到对象上
		this.fitIsAddNewScheduleId(scheduleMergeList);

		// 修改排程记录需要转换类型
		List<GlueScheduleResult> finalResultList = scheduleMergeList.stream().map(schedule -> {
			GlueScheduleResult target = new GlueScheduleResult();
			BeanUtils.copyProperties(schedule, target);
			if (target.getIsAddNew() == null) {
				target.setIsAddNew(false);
			}
			return target;
		}).collect(Collectors.toList());
		return finalResultList;
	}

	/**
	 * 根据生产顺序重算预计时间，修改次序或者计划量的时候执行
	 * 
	 * @param result 待修改的排程
	 * @return 返回本次修改到的记录
	 */
	@Override
	@Transactional
	public List<GlueScheduleResult> recaculateExpectTime(GlueScheduleResult scheduleResult) {
		// 查询修改排程的原记录
		GlueScheduleResultVo scheduleParams = new GlueScheduleResultVo();
		scheduleParams.setIdList(Arrays.asList(new Long[] { scheduleResult.getId() }));
		GlueScheduleResultVo oldScheduleResult = CollectionUtil
				.firstElement(glueScheduleEngineMapper.selectScheduleResult(scheduleParams));
		if (oldScheduleResult == null) {
			throw new RuntimeException(I18nUtil.getMessage("ui.scheduleResult.noRecord"));
		}

		Date scheduleDate = oldScheduleResult.getScheduleDate();
		String mixArea = oldScheduleResult.getMixArea();
		String glueCode = oldScheduleResult.getGlue();
		String oldMachineCode = oldScheduleResult.getMachineCode();
		String oldRecipeType = oldScheduleResult.getRecipeType();
		String machineCode = scheduleResult.getMachineCode();
		String recipeType = scheduleResult.getRecipeType();
		if (oldMachineCode == null || oldRecipeType == null || machineCode == null || recipeType == null) {
			// 没有机台或者配方类型，则说明是未提报胶料不需要重算
			return new ArrayList<>();
		}

		// 处理必要字段
		this.copyExpectTime(scheduleResult, oldScheduleResult); // 拷贝原先的预计时间
		if (scheduleResult.getIsAddNew() == null) {
			scheduleResult.setIsAddNew(false); // 新增标识
		}
		if (scheduleResult.getTotalPlanQty() == null) {
			scheduleResult.setTotalPlanQty(BigDecimalUtil.add(scheduleResult.getMidPlanQty(),
					scheduleResult.getNightPlanQty(), scheduleResult.getDayPlanQty())); // 重算总计划量
		}

		// 查询当天所有已排产信息
		scheduleParams = new GlueScheduleResultVo();
		scheduleParams.setScheduleDate(scheduleDate);
		scheduleParams.setMixArea(mixArea);
		List<GlueScheduleResultVo> allScheduleList = glueScheduleEngineMapper.selectScheduleResult(scheduleParams);
		// 查询当天该机台的已排产信息，配需要排除掉修改那笔记录
		List<GlueScheduleResultVo> machineScheduleList = allScheduleList.stream()
				.filter(result -> machineCode.equals(result.getMachineCode()))
				.filter(result -> !result.getId().equals(scheduleResult.getId())).collect(Collectors.toList());

		// 加载系统参数
		Map<String, String> glueParams = paramsEngineService.mapGlueParams(mixArea);

		// 加载配方数据
		MesPmtRecipeVo recipeParams = new MesPmtRecipeVo();
		recipeParams.setMixArea(mixArea);
		recipeParams.setIsModify(true); // 获取插单时可选的配方
		List<MesPmtRecipeVo> recipeList = recipeEngineService.listGlueRecipe(recipeParams);
		MesPmtRecipeVo recipe = recipeList
				.stream().filter(r -> glueCode.equals(r.getRecipeMaterialName())
						&& machineCode.equals(r.getRecipeEquipCode()) && recipeType.equals(r.getRecipeType()))
				.findAny().orElse(null);
		if (recipe == null) {
			throw new RuntimeException(I18nUtil.getMessage("ui.scheduleResult.noRecipt"));
		}

		// 取出配方类型名称
		Map<String, String> recipeTypeMap = recipeEngineService.mapRecipeType();
		Map<String, String> machineMap = machineEngineService.mapMixMachineName(mixArea);

		// 修改排程类型转换
		GlueScheduleResultVo scheduleResultVo = new GlueScheduleResultVo();
		BeanUtils.copyProperties(scheduleResult, scheduleResultVo); // 先将旧数据复制到新对象中
		scheduleResultVo.setPmtRecipe(recipe);

		// 调用接口重算
		List<GlueScheduleResultVo> updateExpectTimeList = glueScheduleEngineModifyService
				.recaculateAllExpectTime(scheduleResultVo, oldScheduleResult, machineScheduleList, recipe, glueParams);
		// 拷贝预计时间
		this.copyExpectTime(scheduleResult, scheduleResultVo);

		// 排程合并列表
		List<GlueScheduleResultVo> scheduleMergeList = updateExpectTimeList;
		// 联动修改母胶计划
		if (scheduleResult.getIsChangeMasterbatch() != null && scheduleResult.getIsChangeMasterbatch()) {
			// 查询胶料配方映射反转的Map
			Map<String, String> reserveGlueRecipeMap = mixingGlueRecipeMapEngineService.mapReserveGlueRecipe(mixArea);
			// 加载当天的库存信息
			GlueScheduleStockPool glueStock = glueScheduleStockService.buildStockPool(scheduleDate, mixArea, reserveGlueRecipeMap);
			Set<String> modifyGlueSet = new HashSet<>();
			modifyGlueSet.add(glueCode);
			Map<String, List<MesPmtRecipeVo>> recipeMap = recipeList.stream()
					.collect(Collectors.groupingBy(MesPmtRecipeVo::getRecipeMaterialName));
			// 级联更新子胶料计划量
			List<GlueScheduleResultVo> cascadeUpdateList = glueScheduleEngineModifyService
					.cascadeUpdateChildGlueSchedule(scheduleResultVo, glueCode, allScheduleList, recipeMap, glueParams,
							glueStock, modifyGlueSet);
			// 替换掉排程对象
			this.replaceSchedule(cascadeUpdateList, allScheduleList);
			// 将修改信息合并进去
			scheduleMergeList = this.mergeUpdateScheduleList(cascadeUpdateList, scheduleMergeList);
		}

		// 本次更新涉及胶料集合
		Set<String> updateGlueSet = scheduleMergeList.stream().map(GlueScheduleResultVo::getGlue)
				.collect(Collectors.toSet());
		List<GlueScheduleResultVo> updateSurplusList = this.recaculateTotalSurplus(updateGlueSet, allScheduleList,
				scheduleDate, mixArea); // 重算总剩余量
		// 合并需要更新完成量以及预计时间的排程记录
		scheduleMergeList = this.mergeUpdateScheduleList(scheduleMergeList, updateSurplusList);

		// 更新前先排除掉本次修改的记录（要包含本次联动新增的记录）
		List<GlueScheduleResultVo> updateList = scheduleMergeList.stream()
				.filter(schedule -> !scheduleResult.getId().equals(schedule.getId())).collect(Collectors.toList());
		if (!CollectionUtil.isEmpty(updateList)) {
			// 批量更新相关的记录
			glueScheduleEngineMapper.mergeScheduleResult(updateList);
			// 本次联动新增的记录需要把已入库的ID刷到对象上
			this.fitIsAddNewScheduleId(updateList);
		}
		scheduleResult.setTotalSurplus(scheduleResultVo.getTotalSurplus());

		// 修改排程记录需要转换类型
		List<GlueScheduleResult> finalResultList = updateList.stream().map(schedule -> {
			GlueScheduleResult target = new GlueScheduleResult();
			BeanUtils.copyProperties(schedule, target);
			target.setRecipeTypeName(recipeTypeMap.get(target.getRecipeType()));
			target.setMachineName(machineMap.get(target.getMachineCode()));
			return target;
		}).collect(Collectors.toList());
		finalResultList.add(scheduleResult); // 本次修改的排程要加回去
		return finalResultList;
	}

	/**
	 * 本次联动新增的记录需要把已入库的ID刷到对象上
	 * 
	 * @param updateList 更新列表
	 */
	private void fitIsAddNewScheduleId(List<GlueScheduleResultVo> updateList) {
		// 取出新增记录
		List<GlueScheduleResultVo> addNewScheduleList = updateList.stream()
				.filter(schedule -> schedule.getIsAddNew() != null && schedule.getIsAddNew())
				.collect(Collectors.toList());
		if (!CollectionUtil.isEmpty(addNewScheduleList)) {
			// 根据工单号从数据库取出排程数据
			Map<String, GlueScheduleResultVo> orderNoGroupingMap = addNewScheduleList.stream()
					.collect(Collectors.toMap(GlueScheduleResultVo::getOrderNo, Function.identity(), (v1, v2) -> v1));
			GlueScheduleResultVo scheduleParams = new GlueScheduleResultVo();
			scheduleParams.setOrderNoList(new ArrayList<>(orderNoGroupingMap.keySet()));
			List<GlueScheduleResultVo> addNewScheduleDbList = glueScheduleEngineMapper
					.selectScheduleResult(scheduleParams);
			for (GlueScheduleResultVo result : addNewScheduleDbList) {
				// 将ID填充到记录中
				orderNoGroupingMap.get(result.getOrderNo()).setId(result.getId());
			}
		}
	}

	/**
	 * 合并需要更新完成量以及预计时间的排程记录
	 * 
	 * @param scheduleList1 待合并排程列表1
	 * @param scheduleList2 待合并排程列表2
	 * @return
	 */
	private List<GlueScheduleResultVo> mergeUpdateScheduleList(List<GlueScheduleResultVo> scheduleList1,
			List<GlueScheduleResultVo> scheduleList2) {
		// 如果任意一个更新列表为空，直接返回另一个更新列表即可，为空也可以
		if (CollectionUtil.isEmpty(scheduleList1)) {
			return scheduleList2;
		}
		if (CollectionUtil.isEmpty(scheduleList2)) {
			return scheduleList1;
		}
		List<GlueScheduleResultVo> resultList = new ArrayList<>(scheduleList1);

		Map<Long, GlueScheduleResultVo> updateMap = scheduleList1.stream().filter(schedule -> schedule.getId() != null)
				.collect(Collectors.toMap(GlueScheduleResultVo::getId, Function.identity(), (s1, s2) -> s1));
		for (GlueScheduleResultVo surplusSchedule : scheduleList2) {
			Long id = surplusSchedule.getId();
			if (id == null) {
				resultList.add(surplusSchedule);
				continue;
			}
			GlueScheduleResultVo updateSchedule = updateMap.get(id);
			if (updateSchedule == null) {
				updateMap.put(id, surplusSchedule);
				resultList.add(surplusSchedule);
			}
		}

		return resultList;
	}

	/**
	 * 重算总剩余量
	 * 
	 * @param idList    id列表
	 * @param isExclude 是否排除掉参数中的id，主要用于删除
	 */
	@Override
	@Transactional
	public void recaculateTotalSurplus(List<Long> idList, boolean isExclude) {
		if (CollectionUtil.isEmpty(idList)) {
			return;
		}
		List<GlueScheduleResultVo> scheduleResultList = glueScheduleEngineMapper.selectScheduleResultSameGlue(idList);
		if (CollectionUtil.isEmpty(scheduleResultList)) {
			return;
		}

		if (isExclude) {
			// 排除掉ID本身的排程记录，主要用于删除功能
			scheduleResultList = scheduleResultList.stream().filter(schedule -> !idList.contains(schedule.getId()))
					.collect(Collectors.toList());
		}

		// 按排产日 + 密炼区分组，再对每个组各自修改剩余量
		Map<CombinedMapKey, List<GlueScheduleResultVo>> groupingMap = scheduleResultList.stream().collect(Collectors
				.groupingBy(schedule -> CombinedMapKey.createKey(schedule.getScheduleDate(), schedule.getMixArea())));
		for (Entry<CombinedMapKey, List<GlueScheduleResultVo>> entry : groupingMap.entrySet()) {
			Date scheduleDate = (Date) entry.getKey().getKey(0);
			String mixArea = (String) entry.getKey().getKey(1);
			List<GlueScheduleResultVo> updateScheduleList = entry.getValue();
			this.batchRecaculateTotalSurplus(updateScheduleList, scheduleDate, mixArea);
		}
		if (!CollectionUtil.isEmpty(scheduleResultList)) {
			glueScheduleEngineMapper.mergeScheduleResult(scheduleResultList);
		}
	}

	/**
	 * 删除排程后重算相关信息
	 * 
	 * @param idList              id列表
	 * @param isChangeMasterbatch 是否联级修改母炼胶标识
	 */
	@Transactional
	public void deleteSchedule(List<Long> idList, Boolean isChangeMasterbatch) {
		if (CollectionUtil.isEmpty(idList)) {
			return;
		}
		if (!isChangeMasterbatch) {
			// 如果不需要联动修改母胶则只重算剩余量
			this.recaculateTotalSurplus(idList, true);
			return;
		}
		// 查询同一天相同区域所有胶的排程记录
		List<GlueScheduleResultVo> scheduleResultList = glueScheduleEngineMapper.selectScheduleResultSameArea(idList);
		if (CollectionUtil.isEmpty(scheduleResultList)) {
			return;
		}

		// 按排产日 + 密炼区分组，再对每个组各自修改剩余量（正常业务不存在多天、多密炼区一起删除的场景）
		Map<CombinedMapKey, List<GlueScheduleResultVo>> groupingMap = scheduleResultList.stream().collect(Collectors
				.groupingBy(schedule -> CombinedMapKey.createKey(schedule.getScheduleDate(), schedule.getMixArea())));
		// 排程合并列表
		List<GlueScheduleResultVo> scheduleMergeList = new ArrayList<>();

		for (Entry<CombinedMapKey, List<GlueScheduleResultVo>> entry : groupingMap.entrySet()) {
			Date scheduleDate = (Date) entry.getKey().getKey(0);
			String mixArea = (String) entry.getKey().getKey(1);
			// 本区域当天的所有排产计划
			List<GlueScheduleResultVo> allScheduleList = entry.getValue();
			List<GlueScheduleResultVo> updateList = allScheduleList.stream()
					.filter(schedule -> idList.contains(schedule.getId())).collect(Collectors.toList());
			// 查询胶料配方映射反转的Map
			Map<String, String> reserveGlueRecipeMap = mixingGlueRecipeMapEngineService.mapReserveGlueRecipe(mixArea);
			GlueScheduleStockPool glueStock = glueScheduleStockService.buildStockPool(scheduleDate, mixArea, reserveGlueRecipeMap);
			// 加载配方数据
			MesPmtRecipeVo recipeParams = new MesPmtRecipeVo();
			recipeParams.setMixArea(mixArea);
			recipeParams.setIsModify(true); // 获取插单时可选的配方
			List<MesPmtRecipeVo> recipeList = recipeEngineService.listGlueRecipe(recipeParams);
			// 加载系统参数
			Map<String, String> glueParams = paramsEngineService.mapGlueParams(mixArea);

			for (GlueScheduleResultVo scheduleResultVo : updateList) {
				String glueCode = scheduleResultVo.getGlue();
				String machineCode = scheduleResultVo.getMachineCode();
				String recipeType = scheduleResultVo.getRecipeType();
				if (glueCode == null || machineCode == null || recipeType == null) {
					// 如果是未提报记录，不需要做任何处理
					continue;
				}

				MesPmtRecipeVo recipe = recipeList.stream()
						.filter(r -> glueCode.equals(r.getRecipeMaterialName())
								&& machineCode.equals(r.getRecipeEquipCode()) && recipeType.equals(r.getRecipeType()))
						.findAny().orElse(null);
				if (recipe == null) {
					throw new RuntimeException(I18nUtil.getMessage("ui.scheduleResult.noRecipt"));
				}

				// 复制一份排产记录，并清除记录上的所有计划量
				GlueScheduleResultVo deleteSchedule = new GlueScheduleResultVo();
				BeanUtils.copyProperties(scheduleResultVo, deleteSchedule);
				deleteSchedule.setMidPlanQty(0D);
				deleteSchedule.setNightPlanQty(0D);
				deleteSchedule.setDayPlanQty(0D);
				deleteSchedule.setTotalPlanQty(0D);
				deleteSchedule.setPmtRecipe(recipe);

				// 联动修改母胶计划
				Set<String> modifyGlueSet = new HashSet<>();
				modifyGlueSet.add(glueCode);
				Map<String, List<MesPmtRecipeVo>> recipeMap = recipeList.stream()
						.collect(Collectors.groupingBy(MesPmtRecipeVo::getRecipeMaterialName));
				// 级联更新子胶料计划量
				List<GlueScheduleResultVo> cascadeUpdateList = glueScheduleEngineModifyService
						.cascadeUpdateChildGlueSchedule(deleteSchedule, glueCode, allScheduleList, recipeMap,
								glueParams, glueStock, modifyGlueSet);
				// 替换掉排程对象
				this.replaceSchedule(cascadeUpdateList, allScheduleList);
				// 将修改信息合并进去
				scheduleMergeList = this.mergeUpdateScheduleList(cascadeUpdateList, scheduleMergeList);
			}
			// 本次更新涉及胶料集合
			Set<String> updateGlueSet = scheduleMergeList.stream().map(GlueScheduleResultVo::getGlue)
					.collect(Collectors.toSet());
			List<GlueScheduleResultVo> updateSurplusList = this.recaculateTotalSurplus(updateGlueSet, allScheduleList,
					scheduleDate, mixArea); // 重算总剩余量
			// 合并需要更新完成量以及预计时间的排程记录
			scheduleMergeList = this.mergeUpdateScheduleList(scheduleMergeList, updateSurplusList);
		}
		// 排除掉本次删除的记录
		scheduleMergeList = scheduleMergeList.stream().filter(schedule -> !idList.contains(schedule.getId()))
				.collect(Collectors.toList());
		if (!CollectionUtil.isEmpty(scheduleMergeList)) {
			glueScheduleEngineMapper.mergeScheduleResult(scheduleMergeList);
		}
	}

	/**
	 * 重算总剩余量
	 * 
	 * @param updateGlueSet   排次修改涉及的胶料
	 * @param allScheduleList 本日完整排程列表
	 * @param scheduleDate    排程日期
	 * @param mixArea         密炼区
	 * @return 返回本次修改到的记录
	 */
	private List<GlueScheduleResultVo> recaculateTotalSurplus(Set<String> updateGlueSet,
			List<GlueScheduleResultVo> allScheduleList, Date scheduleDate, String mixArea) {
		if (scheduleDate == null || mixArea == null || CollectionUtil.isEmpty(updateGlueSet)) {
			return new ArrayList<>(0);
		}
		// 获取排产日的胶料分解表
		List<GlueDecomposePlanVo> decomposeList = glueScheduleEngineMapper.selectGlueDecomposePlan(scheduleDate,
				mixArea, null);
		// 统计总生产量
		Map<String, Double> produceQtyMap = decomposeList.stream().filter(plan -> plan.getProduceQty() != null)
				.collect(Collectors.groupingBy(GlueDecomposePlanVo::getGlue,
						Collectors.summingDouble(GlueDecomposePlanVo::getProduceQty)));
		// 胶料排产计划根据胶料分组
		Map<String, List<GlueScheduleResultVo>> glueScheduleGroupingMap = allScheduleList.stream()
				// 排除掉未提报排产记录
				.filter(schedule -> schedule.getMachineCode() != null)
				// 排除掉本次更新未修改到的排程记录
				.filter(schedule -> updateGlueSet.contains(schedule.getGlue()))
				.collect(Collectors.groupingBy(GlueScheduleResultVo::getGlue));
		List<GlueScheduleResultVo> allUpdateList = new ArrayList<>();
		// 遍历分组，重算每一个已更新胶料的剩余量
		for (Entry<String, List<GlueScheduleResultVo>> entry : glueScheduleGroupingMap.entrySet()) {
			String glueCode = entry.getKey();
			Double produceQty = produceQtyMap.get(glueCode);
			if (produceQty == null || produceQty < 0) {
				produceQty = 0D; // 不合法的生产量，当0处理
			}
			// 更新总剩余量
			List<GlueScheduleResultVo> updateList = this.updateTotalSurplus(produceQty, entry.getValue());
			// 更新同一个胶料的总剩余量
			allUpdateList = this.mergeUpdateScheduleList(updateList, allUpdateList);
		}

		return allUpdateList;
	}

	/**
	 * 更新总完成量
	 * 
	 * @param produceQty   计划生产数
	 * @param scheduleList 待更新总完成量的排程列表
	 */
	private List<GlueScheduleResultVo> updateTotalSurplus(Double produceQty, List<GlueScheduleResultVo> scheduleList) {
		// 计算总排产量
		Double totalPlanQty = scheduleList.stream().filter(schedule -> schedule.getTotalPlanQty() != null)
				.collect(Collectors.summingDouble(GlueScheduleResultVo::getTotalPlanQty));

		// 总剩余量 = 生产量 - 已排产量
		Double totalSurplus = produceQty > totalPlanQty ? BigDecimalUtil.sub(produceQty, totalPlanQty) : 0D;
		// 如果原来都没有剩余量，则放到剩余量最小的一笔记录中
		GlueScheduleResultVo updateSchedule = scheduleList.stream()
				.filter(schedule -> schedule.getTotalPlanQty() != null)
				.min(Comparator.comparing(GlueScheduleResultVo::getTotalPlanQty)).orElse(null);
		// 将剩余量更新到当天该胶料计划量最大的记录中，其余都设置成0
		List<GlueScheduleResultVo> updateList = new ArrayList<>();
		scheduleList.stream().forEach(schedule -> {
			Double newTotalSurplus = updateSchedule == schedule ? totalSurplus : 0D;
			schedule.setTotalSurplus(newTotalSurplus);
			schedule.setBaseValue(schedule.getId());
			updateList.add(schedule);
		});
		return updateList;
	}

	/**
	 * 拷贝预计完成时间
	 * 
	 * @param target 待更新目标
	 * @param srouce 数据源
	 */
	private void copyExpectTime(GlueScheduleResult target, GlueScheduleResultVo srouce) {
		target.setMidExpectStartTime(srouce.getMidExpectStartTime());
		target.setMidExpectFinishTime(srouce.getMidExpectFinishTime());
		target.setNightExpectStartTime(srouce.getNightExpectStartTime());
		target.setNightExpectFinishTime(srouce.getNightExpectFinishTime());
		target.setDayExpectStartTime(srouce.getDayExpectStartTime());
		target.setDayExpectFinishTime(srouce.getDayExpectFinishTime());
	}

	/**
	 * 根据预计生产时间设置生产顺序
	 * 
	 * @param baseScheduleResult 排程结果列表
	 */
	private void sortScheduleResult(List<GlueScheduleResultVo> baseScheduleResult) {
		Map<String, List<GlueScheduleResultVo>> resultMap = baseScheduleResult.stream()
				.collect(Collectors.groupingBy(GlueScheduleResultVo::getMachineCode));
		for (List<GlueScheduleResultVo> resultList : resultMap.values()) {
			// 对中班计划量排序，按中班预计开始时间排序，空值不需要排
			List<GlueScheduleResultVo> sortedResultList = resultList.stream()
					.filter(r -> r.getMidExpectStartTime() != null)
					.sorted(Comparator.comparing(GlueScheduleResultVo::getMidExpectStartTime))
					.collect(Collectors.toList());
			int index = 1;
			for (GlueScheduleResultVo result : sortedResultList) {
				result.setMidProduceOrder(index++ * 10); // 序号要求以10递增
			}
			// 对夜班计划量排序，按夜班预计开始时间排序，空值不需要排
			sortedResultList = resultList.stream().filter(r -> r.getNightExpectStartTime() != null)
					.sorted(Comparator.comparing(GlueScheduleResultVo::getNightExpectStartTime))
					.collect(Collectors.toList());
			index = 1;
			for (GlueScheduleResultVo result : sortedResultList) {
				result.setNightProduceOrder(index++ * 10);
			}
			// 对白班计划量排序，按白班预计开始时间排序，空值不需要排
			sortedResultList = resultList.stream().filter(r -> r.getDayExpectStartTime() != null)
					.sorted(Comparator.comparing(GlueScheduleResultVo::getDayExpectStartTime))
					.collect(Collectors.toList());
			index = 1;
			for (GlueScheduleResultVo result : sortedResultList) {
				result.setDayProduceOrder(index++ * 10);
			}
		}
	}

	/**
	 * 下发排程数据给MES
	 * 
	 * @param resultIdList 待下发的排程ID列表
	 */
	@Override
	@Transactional
	public AjaxResult publishToMes(GlueScheduleResult glueScheduleResult, List<Long> resultIdList) {
		if (CollectionUtil.isEmpty(resultIdList)) {
			return AjaxResult.error(I18nUtil.getMessage("schedule.materialScheduleResult.norecord"));
		}
		GlueScheduleResultVo params = new GlueScheduleResultVo();
		params.setIdList(resultIdList);
		List<GlueScheduleResultVo> resultList = glueScheduleEngineMapper.selectScheduleResult(params); // 通过ID查询
		if (CollectionUtil.isEmpty(resultList)) {
			return AjaxResult.error(I18nUtil.getMessage("schedule.materialScheduleResult.norecord"));
		}
		for (GlueScheduleResultVo result : resultList) {
			String releaseState = result.getReleaseStatus();
			if (!ZltConstant.NO_RELEASE.equals(releaseState) && !ZltConstant.WAIT_RELEASING.equals(releaseState)
					&& !ZltConstant.FAILURE_RELEASE.equals(releaseState)) {
				return AjaxResult.error(I18nUtil.getMessage("schedule.materialScheduleResult.publish.status")); // 发布失败的也可以重发
			}
		}
		String dataVersion = glueScheduleResult.getDataVersion();
        String factoryCode = glueScheduleResult.getFactoryCode();
        String companyCode = glueScheduleResult.getCompanyCode();
		schedulePublishEngineMapper.publishToMes(dataVersion, resultIdList, factoryCode, companyCode);

		return AjaxResult.success();
	}
	

    /**
     * 更新下发状态
     * 
     * @param resultIdList 待下发的排程ID列表
     */
    public AjaxResult updateRelaseStatus(List<Long> resultIdList, String relaseStatus) {
        GlueScheduleResultVo params = new GlueScheduleResultVo();
        params.setIdList(resultIdList);
        List<GlueScheduleResultVo> resultList = glueScheduleEngineMapper.selectScheduleResult(params); // 通过ID查询
        schedulePublishEngineMapper.updateScheduleReleseState(resultList);
        return AjaxResult.success();
    }

	/**
	 * 跨区接收引擎算法
	 * 
	 * @param receiveList 批量接收的记录
	 */
	@Override
	@Transactional
	public void glueSpanReceive(List<GlueSpanReceive> receiveList) {
		if (CollectionUtils.isEmpty(receiveList)) {
			throw new RuntimeException(I18nUtil.getMessage("ui.scheduleResult.glueSpan.noRecord"));
		}
		// 要过滤掉未接收的以及接收数不正确的数据
		List<GlueSpanReceiveVo> glueSpanReceiveList = receiveList.stream()
				// 转换成VO
				.map(r -> {
					GlueSpanReceiveVo receiveVo = new GlueSpanReceiveVo();
					BeanUtils.copyProperties(r, receiveVo);
					return receiveVo;
				})
				// 过滤掉未接收的
				.filter(glueSpan -> ZltConstant.RECEIVE_STATUS_YES.equals(glueSpan.getReceiveStatus()))
				// 过滤掉数据不合法的数据
				.filter(g -> g.getReceiveQty() != null && g.getReceiveQty() > 0 && g.getGlue() != null
						&& g.getMachineCode() != null && g.getRecipeType() != null)
				.collect(Collectors.toList());
		if (CollectionUtils.isEmpty(glueSpanReceiveList)) {
			return;
		}
		GlueSpanReceive firstGlueSpan = CollectionUtil.firstElement(receiveList);
		Date scheduleDate = firstGlueSpan.getScheduleDate();
		String mixArea = firstGlueSpan.getEntrustedMixArea();

		// 当天的所有排程记录
		GlueScheduleResultVo params = new GlueScheduleResultVo();
		params.setScheduleDate(scheduleDate);
		params.setMixArea(mixArea);
		List<GlueScheduleResultVo> allScheduleList = glueScheduleEngineMapper.selectScheduleResult(params);
		if (CollectionUtils.isEmpty(allScheduleList)) { // 如果还没有生成排程，则不需要处理
			return;
		}

		// 获取批次号
		String batchNo = CollectionUtil.firstElement(allScheduleList).getBatchNo();

		// 加载所有的配方
		MesPmtRecipeVo recipeParams = new MesPmtRecipeVo();
		recipeParams.setMixArea(mixArea);
		List<MesPmtRecipeVo> mesPmtRecipeList = recipeEngineService.listGlueRecipe(recipeParams);

		// 查询胶料配方映射反转的Map
		Map<String, String> reserveGlueRecipeMap = mixingGlueRecipeMapEngineService.mapReserveGlueRecipe(mixArea);
		// 当天的库存信息
		GlueScheduleStockPool glueStock = glueScheduleStockService.buildStockPool(scheduleDate, mixArea, reserveGlueRecipeMap);

		// 加载系统参数
		Map<String, String> scheduleParams = paramsEngineService.mapGlueParams(mixArea);

		List<GlueScheduleResultVo> updateList = glueScheduleEngineModifyService.createGlueSpanReceiveSchedule(
				scheduleDate, mixArea, batchNo, glueSpanReceiveList, allScheduleList, mesPmtRecipeList, glueStock,
				scheduleParams);
		if (CollectionUtils.isNotEmpty(updateList)) {
			glueScheduleEngineMapper.mergeScheduleResult(updateList);
		}
	}
}
