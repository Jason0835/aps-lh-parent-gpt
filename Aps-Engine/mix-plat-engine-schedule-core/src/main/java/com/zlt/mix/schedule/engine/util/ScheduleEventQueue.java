package com.zlt.mix.schedule.engine.util;

import com.zlt.mix.common.core.constant.ZltConstant;
import com.zlt.mix.common.core.utils.BigDecimalUtil;
import com.zlt.mix.common.core.utils.GenerageMapKeyUtils;
import com.zlt.mix.common.engine.domain.MessageContent;
import com.zlt.mix.schedule.api.domain.entity.GlueScheduleResult;
import com.zlt.mix.schedule.engine.constants.GlueEngineConstants;
import com.zlt.mix.schedule.engine.util.event.ClassStartEvent;
import com.zlt.mix.schedule.engine.util.event.GlueStockUpdateEvent;
import com.zlt.mix.schedule.engine.util.event.ScheduleEvent;
import com.zlt.mix.schedule.engine.util.event.SearchMachineAndGlueEvent;
import com.zlt.mix.schedule.engine.vo.*;
import com.zlt.mix.setting.api.domain.entity.MixMachine;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateUtils;

import java.math.BigDecimal;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 排产事件队列
 * 
 * @author hakimryan
 *
 */
public class ScheduleEventQueue {
	/**
	 * 存放事件的队列，已执行时间作为key
	 */
	private TreeMap<Date, LinkedList<ScheduleEvent>> queue;
	/**
	 * 机台生产状态
	 */
	private Map<String, GlueScheduleMachineProductVo> machineProductMap;
	/**
	 * 胶料库存
	 */
	private GlueScheduleStockPool glueStock;
	/**
	 * 排程列表
	 */
	private List<GlueScheduleResultVo> scheduleResult;
	/**
	 * 记录胶料-对应排产列表的Map
	 */
	private Map<String, List<GlueScheduleResultVo>> mapScheduleResult;
	/**
	 * 加载有效的配方信息，配方按胶料 + 机台分组
	 */
	Map<CombinedMapKey, List<MesPmtRecipeVo>> mesPmtRecipeMap;
	/**
	 * 排产日
	 */
	private Date scheduleDate;
	/**
	 * 事件列表的当前时间点，会随着事件的执行向后推移
	 */
	private Date currentTime;
	/**
	 * 在当前时间结束时是否需要进行搜索
	 */
	private boolean needSearch;
	/**
	 * 参数设置
	 */
	private Map<String, String> params;
	/**
	 * 机台列表
	 */
	Map<String, MixMachine> machineMap;
	/**
	 * 已发送的请跨区请求
	 */
	List<GlueSpanReceiveVo> glueSpanReceiveList;
	/**
	 * 排产日志 key：日志类型 value：日志内容
	 */
	private MessageContent errorMessage;
	/**
	 * 分厂需求列表
	 */
	private Map<String, GlueFactoryRequireVo> factoryRequireMap;
	/**
	 * 优先高耗能开始时间
	 */
	private Date highConsumptionBegin;
	/**
	 * 优先高耗能结束时间
	 */
	private Date highConsumptionEnd;
	/**
	 * 胶料间隔时间
	 */
	private Map<String, Long> mixingTimeMap;
	/**
	 * 炼胶单规格最小排产数
	 */
	private Map<String, BigDecimal> mixingMinProductMap;
	/**
	 * 塑胶优先级映射
	 */
	private Map<String, List<GlueScheduleResultVo>> slPriorityMap;
	/**
	 * 需要塑胶排产后的优先排产的记录
	 */
	private Map<String, List<GlueScheduleResultVo>> needSlScheduleMap;
	/**
	 * 查询昨日早班的最后一个排程计划
	 */
	private List<GlueScheduleResultVo> latestScheduleList;
	/**
	 * 炼胶优先配置
	 */
	private Map<String, String> mixingPriorityProductMap;
	/**
	 * 炼胶接续配置
	 */
	private Map<String, String> mixingContinueProductMap;
	/**
	 * 过滤需要进行连续排产的记录
	 */
	private List<GlueScheduleResultVo> filterContinueScheduleResult;
	/**
	 * 可被选中生产且标记为生产模式的记录
	 */
	private List<GlueScheduleResultVo> productionModelList;
	/**
	 * 胶料-生产模式排产
	 */
	private Map<String, List<GlueScheduleResultVo>> mapProductionModel;

	private ScheduleEventQueue() {

	}

	public static ScheduleEventQueue createQueue(List<GlueScheduleResultVo> scheduleResult,
			GlueScheduleStockPool glueStock, Date scheduleDate, Map<String, String> params,
			Map<String, MixMachine> machineMap, List<GlueSpanReceiveVo> glueSpanReceiveList,
			Map<String, GlueFactoryRequireVo> factoryRequireMap, Map<String, Long> mixingTimeMap,
												 Map<String, BigDecimal> mixingMinProductMap,
												 Map<String, List<GlueScheduleResultVo>> slPriorityMap,
												 Map<String, List<GlueScheduleResultVo>> needSlScheduleMap,
												 List<GlueScheduleResultVo> latestScheduleList,
												 Map<String, String> mixingPriorityProductMap,
												 List<MesPmtRecipeVo> mesPmtRecipeList) {
		ScheduleEventQueue instance = new ScheduleEventQueue();
		instance.scheduleResult = scheduleResult;
		// 记录胶料-排产列表的映射
		instance.mapScheduleResult = scheduleResult.stream()
				.filter(v -> StringUtils.isNotBlank(v.getGlue()))
				.collect(Collectors.groupingBy(GlueScheduleResult::getGlue));
		// 加载有效的配方信息，配方按胶料 + 机台分组
		instance.mesPmtRecipeMap = mesPmtRecipeList.stream().collect(Collectors
				.groupingBy(r -> CombinedMapKey.createKey(r.getRecipeMaterialName(), r.getRecipeEquipCode())));
		instance.glueStock = glueStock;
		instance.scheduleDate = scheduleDate;
		instance.params = params;
		instance.queue = new TreeMap<>();
		instance.needSearch = true;
		instance.machineMap = machineMap;
		instance.glueSpanReceiveList = glueSpanReceiveList;
		instance.errorMessage = MessageContent.newInstance();
		instance.factoryRequireMap = factoryRequireMap;
		// 机台状态，默认都是空闲
		instance.machineProductMap = scheduleResult.stream()
				.map(schedule -> instance.createMachineProductVo(schedule.getMachineCode())).collect(Collectors
						.toMap(GlueScheduleMachineProductVo::getMachineCode, Function.identity(), (s1, s2) -> s1));
		int highConsumptionBegin = Integer.parseInt(params.getOrDefault(GlueEngineConstants.HIGH_CONSUMPTION_BEGIN, GlueEngineConstants.DEFAULT_HIGH_CONSUMPTION_BEGIN));
		instance.highConsumptionBegin = DateUtils.addHours(scheduleDate, highConsumptionBegin);
		int highConsumptionEnd = Integer.parseInt(params.getOrDefault(GlueEngineConstants.HIGH_CONSUMPTION_END, GlueEngineConstants.DEFAULT_HIGH_CONSUMPTION_END));
		instance.highConsumptionEnd = DateUtils.addHours(scheduleDate, highConsumptionEnd);
		// 胶料间隔时间
		instance.mixingTimeMap = mixingTimeMap;
		// 炼胶单规格最小排产数
		instance.mixingMinProductMap = mixingMinProductMap;
		// 塑胶优先级映射
		instance.slPriorityMap = slPriorityMap;
		// 需要塑胶排产后的优先排产的记录
		instance.needSlScheduleMap = needSlScheduleMap;
		// 查询昨日早班的最后一个排程计划
		instance.latestScheduleList = latestScheduleList;
		// 炼胶优先配置
		instance.mixingPriorityProductMap = mixingPriorityProductMap;
		// 炼胶接续配置，根据炼胶优先反转
		instance.mixingContinueProductMap = new HashMap<>();
		mixingPriorityProductMap.forEach((k, v) -> instance.mixingContinueProductMap.putIfAbsent(v, k));
		// 构建C2Z掺胶配方和ZZ正式配方的绑定关系
		buildRecipeBindSchedule(scheduleResult, instance);
		// 构建排产绑定关系，过滤非连续排产外的排产记录
		buildBindSchedule(instance.filterContinueScheduleResult, factoryRequireMap, mixingPriorityProductMap, instance);
		// 记录标记生产模式的记录
		instance.productionModelList = instance.filterContinueScheduleResult.stream()
				.filter(v -> v.getProductionBefore() != null || v.getProductionAfter() != null)
				.collect(Collectors.toList());
		// 记录胶料对应生产模式的Map
		instance.mapProductionModel = instance.productionModelList.stream().collect(Collectors.groupingBy(GlueScheduleResult::getGlue));

		return instance;
	}

	/**
	 * 如果存在相同机台+胶料的不同配方，将排产进行绑定
	 *
	 * @param scheduleResult 完成排产列表
	 * @param instance       排产队列实例
	 */
	private static void buildRecipeBindSchedule(List<GlueScheduleResultVo> scheduleResult, ScheduleEventQueue instance) {
		// 记录需要移除的机台+胶料+配方
		Set<String> continueSet = new HashSet<>();
		// 合计胶料+配方的总计划
		Map<String, BigDecimal> sumPlanMap = new HashMap<>();
		// 将ZZ配方绑定到其他排产的后一个
		Map<String, List<GlueScheduleResultVo>> groupMap = scheduleResult.stream().collect(Collectors.groupingBy(v -> GenerageMapKeyUtils.createMapKey(v.getMachineCode(), v.getGlue(), v.getDayFlag())));
		groupMap.forEach((key, list) -> {
			if (CollectionUtils.isEmpty(list) || list.size() <= 1) {
				return;
			}
			// 如果存在ZZ配方，绑定到其他配方上
			Optional<GlueScheduleResultVo> zzRecipeOptional = list.stream().filter(v -> GlueEngineConstants.RECIPE_TYPE_ZZ.equals(v.getRecipeTypeName())).findAny();
			if (!zzRecipeOptional.isPresent()) {
				return;
			}
			GlueScheduleResultVo zzRecipe = zzRecipeOptional.get();
			for (GlueScheduleResultVo item : list) {
				// 存在非ZZ配方的记录，而且前置配方需要保证有计划量（避免无法计算生产量），两者做一个绑定关系
				if (!GlueEngineConstants.RECIPE_TYPE_ZZ.equals(item.getRecipeTypeName())
						&& item.getPlanQty() != null
						&& item.getPlanQty().compareTo(BigDecimal.ZERO) > 0) {
					item.setBindScheduleResult(zzRecipe);

					// 移除对应排产，不进行配方配方选择
					String continueKey = GenerageMapKeyUtils.createMapKey(zzRecipe.getGlue(), zzRecipe.getRecipeType(), zzRecipe.getDayFlag());
					continueSet.add(continueKey);

					// 汇总计划量
					String sumPlanKey = GenerageMapKeyUtils.createMapKey(zzRecipe.getGlue(), zzRecipe.getRecipeType(), zzRecipe.getDayFlag());
					BigDecimal sumPlanQty = sumPlanMap.getOrDefault(sumPlanKey, BigDecimal.ZERO);
					sumPlanMap.put(sumPlanKey, BigDecimal.valueOf(BigDecimalUtil.add(sumPlanQty.doubleValue(), zzRecipe.getPlanQty().doubleValue())));

					return;
				}
			}
		});

		if (!sumPlanMap.isEmpty()) {
			// 如果是汇总的记录，计算汇总对应的计划量
			Map<String, Double> sumRequireMap = new HashMap<>();
			for (GlueScheduleResultVo resultVo : scheduleResult) {
				String sumPlanKey = GenerageMapKeyUtils.createMapKey(resultVo.getGlue(), resultVo.getRecipeType(), resultVo.getDayFlag());
				if (sumPlanMap.containsKey(sumPlanKey)) {
					Double requireQty = sumRequireMap.getOrDefault(sumPlanKey, 0D);
					sumRequireMap.put(sumPlanKey, BigDecimalUtil.add(requireQty, resultVo.getPlanQty().doubleValue()));
				}
			}
			// 汇总需要的计划量，合计作为需求量，清空计划量
			for (GlueScheduleResultVo item : scheduleResult) {
				String sumPlanKey = GenerageMapKeyUtils.createMapKey(item.getGlue(), item.getRecipeType(), item.getDayFlag());
				BigDecimal sumPlan = sumPlanMap.get(sumPlanKey);
				if (sumPlan != null) {
					// 汇总需要的计划量，合计作为需求量
					item.setPlanQty(BigDecimal.ZERO);
					// 需求量根据胶料+机台相同的记录
					item.setRequireQty(sumRequireMap.getOrDefault(sumPlanKey, sumPlan.doubleValue()));
				}
			}
		}

		// 移除不能单独排产的记录
		instance.filterContinueScheduleResult = scheduleResult.stream().filter(item -> {
			String continueKey = GenerageMapKeyUtils.createMapKey( item.getGlue(), item.getRecipeType(), item.getDayFlag());
			return !continueSet.contains(continueKey);
		}).collect(Collectors.toList());

	}

	/**
	 * 构建排产绑定关系，过滤非连续排产外的排产记录
	 *
	 * @param scheduleResult           完整排产列表
	 * @param factoryRequireMap        分厂需求量
	 * @param mixingPriorityProductMap 胶料优先配置
	 * @param instance                 排程队列实例
	 */
	private static void buildBindSchedule(List<GlueScheduleResultVo> scheduleResult,
										  Map<String, GlueFactoryRequireVo> factoryRequireMap,
										  Map<String, String> mixingPriorityProductMap,
										  ScheduleEventQueue instance) {
		// 判断对应胶料是否存在
		Set<String> glueSet = scheduleResult.stream()
				.map(v -> GenerageMapKeyUtils.createMapKey(v.getGlue(), v.getDayFlag())).collect(Collectors.toSet());
		// 对应机台+胶料映射对应排产的记录
		Map<String, List<GlueScheduleResultVo>> machineGlueMap = scheduleResult.stream()
				.collect(Collectors.groupingBy(v -> GenerageMapKeyUtils.createMapKey(v.getMachineCode(), v.getGlue(), v.getDayFlag())));

		// 先找可以连续排产的记录
		List<GlueScheduleResultVo> continueScheduleList = scheduleResult.stream().filter(v -> {
			String glue = v.getGlue();
			String dayFlag = v.getDayFlag();
			String priorityGlue = mixingPriorityProductMap.get(glue);
			// 没有优先排产的胶料
			if (StringUtils.isBlank(priorityGlue)) {
				return false;
			}
			// 优先排产的胶料，没有排产计划
			if (!glueSet.contains(GenerageMapKeyUtils.createMapKey(priorityGlue, dayFlag))) {
				return false;
			}
			String machineGlueKey = GenerageMapKeyUtils.createMapKey(v.getMachineCode(), priorityGlue, dayFlag);
			// 优先排产的胶料，没有相同机台的排产计划
			if (!machineGlueMap.containsKey(machineGlueKey)) {
				return false;
			}
			// 优先胶料需要保证有计划量
			List<GlueScheduleResultVo> priorityList = machineGlueMap.get(machineGlueKey);
			BigDecimal sumPlanQty = priorityList.stream().map(GlueScheduleResultVo::getPlanQty).filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);
			if (sumPlanQty.compareTo(BigDecimal.ZERO) <= 0) {
				return false;
			}

			// 无论有没有夜班需求，都直接绑定在一起
			return true;
			// // 如果当前胶料和优先胶料有一个存在夜班需求，就不考虑连续生产，而是优先考虑满足夜班需求量
			// return !ScheduleRequireUtils.checkNightRequire(factoryRequireMap, glue)
			// 		&& !ScheduleRequireUtils.checkNightRequire(factoryRequireMap, priorityGlue);
		})
				// 清空计划量，便于生产模式接续
				.peek(v->v.setPlanQty(BigDecimal.ZERO))
				.collect(Collectors.toList());

		// 接续排产的记录，映射机台+优先胶料=连续排产
		Map<String, GlueScheduleResultVo> continueScheduleMap = continueScheduleList.stream()
				.collect(Collectors.toMap(v -> GenerageMapKeyUtils.createMapKey(v.getMachineCode(), mixingPriorityProductMap.get(v.getGlue()), v.getDayFlag()),
						Function.identity(), (v1, v2) -> v1));

		// 可以连续排产的胶料
		Set<String> continueGlueSet = continueScheduleList.stream()
				.map(v -> GenerageMapKeyUtils.createMapKey(v.getGlue(), v.getDayFlag())).filter(StringUtils::isNotBlank).collect(Collectors.toSet());
		// 过滤可以连续排产的记录
		instance.filterContinueScheduleResult = scheduleResult.stream()
				.filter(v -> !continueGlueSet.contains(GenerageMapKeyUtils.createMapKey(v.getGlue(), v.getDayFlag())))
				// 将需要连续排产的记录绑定上
				.peek(v -> {
					String continueKey = GenerageMapKeyUtils.createMapKey(v.getMachineCode(), v.getGlue(), v.getDayFlag());
					if(continueScheduleMap.containsKey(continueKey)){
						if (v.getBindScheduleResult() == null) {
							v.setBindScheduleResult(continueScheduleMap.get(continueKey));
						} else {
							GlueScheduleResultVo bindScheduleResult = v.getBindScheduleResult();
							if (bindScheduleResult.getGlue().equals(v.getGlue()) && bindScheduleResult.getBindScheduleResult() == null) {
								// 如果是同胶料不同配方的绑定关系，可以再插入一个绑定关系
								bindScheduleResult.setBindScheduleResult(continueScheduleMap.get(continueKey));
							}
						}
					}
				})
				.collect(Collectors.toList());
	}

	/**
	 * 创建机台生产状态对象
	 * 
	 * @param machineCode 机台编号
	 * @return
	 */
	private GlueScheduleMachineProductVo createMachineProductVo(String machineCode) {
		GlueScheduleMachineProductVo machineProduct = new GlueScheduleMachineProductVo();
		machineProduct.setState(GlueEngineConstants.MACHINE_STATE_WAIT);
		machineProduct.setMachineCode(machineCode);
		// 计算各班的可排产时间（秒） = 单班时间 - 用餐时间
		long dinnerTime = new Long(params.getOrDefault(GlueEngineConstants.DINNER_TIME, "0")) * 60;
		BigDecimal realProductTime = new BigDecimal(ShiftClassUtil.ONE_SHIFT_CLASS_TIME - dinnerTime);
		machineProduct.setMidProductTime(realProductTime);
		machineProduct.setNightProductTime(realProductTime);
		machineProduct.setDayProductTime(realProductTime);
		MixMachine machine = machineMap.get(machineCode);
		if (machine != null && ZltConstant.STATUS_ENABLE.equals(machine.getStatus())) {
			// 把机台各班状态填入vo中
			machineProduct.setMidStatus(ZltConstant.STATUS_ENABLE.equals(machine.getMidStatus()));
			machineProduct.setNightStatus(ZltConstant.STATUS_ENABLE.equals(machine.getNightStatus()));
			machineProduct.setDayStatus(ZltConstant.STATUS_DISABLE.equals(machine.getDayStatus()));
		} else {
			// 找不到机台或者机台状态禁用，则各班都禁用
			machineProduct.setMidStatus(false);
			machineProduct.setNightStatus(false);
			machineProduct.setDayStatus(false);
		}
		return machineProduct;
	}

	/**
	 * 添加事件
	 * 
	 * @param event
	 * @param runTime
	 */
	public void addEvent(ScheduleEvent event, Date runTime) {
		LinkedList<ScheduleEvent> eventList = queue.get(runTime);
		if (eventList == null) {
			eventList = new LinkedList<>();
			queue.put(runTime, eventList);
		}
		eventList.add(event);
	}

	/**
	 * 添加初始事件
	 * 
	 * @param event
	 * @param runTime
	 */
	private void addSearchEvent(Date runTime) {
		// 添加一个搜索事件
		this.addEvent(new SearchMachineAndGlueEvent(), runTime);
		// 搜索完成后，取消搜索标记
		this.needSearch = false;
	}

	/**
	 * 判断事件点是否还有未执行的事件
	 * 
	 * @param runTime
	 * @return
	 */
	public boolean hasEvent(Date runTime) {
		return CollectionUtils.isNotEmpty(queue.get(runTime));
	}

	/**
	 * 事件队列运行，按事件顺序执行队列中的所有事件
	 * 
	 * @param runTime
	 */
	public void start() {
		// 初始化队列
		this.initQueue();
		// 开始循环执行队列
		while (!queue.isEmpty()) {
			// 时间点推移到最近的一个事件的触发时间
			this.currentTime = queue.firstKey();
			glueStock.updateCurrentDate(this.currentTime);// 时间点推进要同时刷新库存
			LinkedList<ScheduleEvent> eventList = queue.firstEntry().getValue();
			if (CollectionUtils.isNotEmpty(eventList)) {
				ScheduleEvent event = eventList.removeFirst();
				event.excute(this);
			}
			// 当前时间点的事件全部执行完之后，再添加一次搜索事件，尝试搜索是否还有可以排产的胶料或者机台
			if (CollectionUtils.isEmpty(eventList)) {
				if (this.needSearch) {
					this.addSearchEvent(currentTime);
				} else {
					// 如果经过初始事件搜索依然没有新事件添加进来，则本时间点任务结束
					queue.remove(currentTime);
					// 每个时间点结束后，需要重置搜索标识
					this.needSearch = true;
				}
			}
		}
	}

	/**
	 * 初始化队列
	 */
	private void initQueue() {
		// 计算每个班的开始时间
		Date midStartTime = ShiftClassUtil.getShiftClassStartTime(scheduleDate, GlueEngineConstants.SHIFT_CLASS_MID);
		Date nightStartTime = ShiftClassUtil.getShiftClassStartTime(scheduleDate,
				GlueEngineConstants.SHIFT_CLASS_NIGHT);
		// Date dayStartTime = ShiftClassUtil.getShiftClassStartTime(scheduleDate, GlueEngineConstants.SHIFT_CLASS_DAY);

		// 在每个班的开始时间添加开班事件
		ClassStartEvent classStartEvent = new ClassStartEvent();
		this.addEvent(classStartEvent, midStartTime);
		this.addEvent(classStartEvent, nightStartTime);
		// this.addEvent(classStartEvent, dayStartTime);

		// 有跨区请求的要增加库存更新事件
		// if (CollectionUtils.isNotEmpty(glueSpanReceiveList)) {
		// 	for (GlueSpanReceiveVo glueSpan : glueSpanReceiveList) {
		// 		BigDecimal receiveQty = BigDecimal.valueOf(glueSpan.getReceiveQty());
		// 		GlueScheduleResultVo r = new GlueScheduleResultVo();
		// 		r.setGlue(glueSpan.getGlue());
		// 		r.setMajorType(glueSpan.getMajorType());
		// 		MesPmtRecipeVo recipe = new MesPmtRecipeVo();
		// 		recipe.setLotTotalWeight(glueSpan.getLotTotalWeight());
		// 		r.setPmtRecipe(recipe);
		// 		GuleColdDownEvent guleColdDownEvent = new GuleColdDownEvent(r, receiveQty);
		// 		// 添加到白班开班时间点
		// 		this.addEvent(guleColdDownEvent, dayStartTime);
		// 	}
		// }

		// 添加预计返回胶库存更新事件
		for (DailyReturnGlueStockVo returnStock : glueStock.listReturnStockWeight()) {
			String glueCode = returnStock.getGlue();
			String majorType = GlueEngineConstants.MAJOR_TYPE_FH;
			BigDecimal nightStock = returnStock.getNightStock();
			// BigDecimal dayStock = returnStock.getDayStock();
			if (nightStock.compareTo(BigDecimal.ZERO) > 0) {
				GlueStockUpdateEvent nightStockUpdateEvent = new GlueStockUpdateEvent(glueCode, majorType, null,
						nightStock);
				this.addEvent(nightStockUpdateEvent, nightStartTime);
			}
			// if (dayStock.compareTo(BigDecimal.ZERO) > 0) {
			// 	GlueStockUpdateEvent dayStockUpdateEvent = new GlueStockUpdateEvent(glueCode, majorType, null,
			// 			dayStock);
			// 	this.addEvent(dayStockUpdateEvent, dayStartTime);
			// }
		}
	}

	/**
	 * 获取机台设置，如果原来没有则初始化一个
	 * 
	 * @param machineCode 机台编号
	 * @return
	 */
	public GlueScheduleMachineProductVo getMachineProduct(String machineCode) {
		GlueScheduleMachineProductVo machineProductVo = this.machineProductMap.get(machineCode);
		if (machineProductVo == null) {
			machineProductVo = this.createMachineProductVo(machineCode);
			this.machineProductMap.put(machineCode, machineProductVo);
		}
		return machineProductVo;
	}

	/**
	 * 添加日志
	 * 
	 * @param log
	 */
	public void addLog(String log) {
		errorMessage.addMessage(log);
	}

	/**
	 * 获取日志
	 * 
	 * @return
	 */
	public String getLog() {
		return errorMessage.toString();
	}

	public Map<String, GlueScheduleMachineProductVo> getMachineProductMap() {
		return machineProductMap;
	}

	public GlueScheduleStockPool getGlueStock() {
		return glueStock;
	}

	public List<GlueScheduleResultVo> getScheduleResult() {
		return scheduleResult;
	}

	public Date getCurrentTime() {
		return currentTime;
	}

	public Map<String, String> getParams() {
		return params;
	}

	public Date getScheduleDate() {
		return scheduleDate;
	}

	public List<GlueSpanReceiveVo> getGlueSpanReceiveList() {
		return glueSpanReceiveList;
	}

	public Map<String, GlueFactoryRequireVo> getFactoryRequireMap() {
		return factoryRequireMap;
	}

    public Date getHighConsumptionBegin() {
        return highConsumptionBegin;
    }

    public Date getHighConsumptionEnd() {
        return highConsumptionEnd;
    }

	public Map<String, Long> getMixingTimeMap() {
		return mixingTimeMap;
	}

	public Map<String, BigDecimal> getMixingMinProductMap() {
		return mixingMinProductMap;
	}

	public Map<String, List<GlueScheduleResultVo>> getSlPriorityMap() {
		return slPriorityMap;
	}

	public Map<String, List<GlueScheduleResultVo>> getNeedSlScheduleMap() {
		return needSlScheduleMap;
	}

	public Map<String, String> getMixingPriorityProductMap() {
		return mixingPriorityProductMap;
	}

	public List<GlueScheduleResultVo> getLatestScheduleList() {
		return latestScheduleList;
	}

	public Map<String, String> getMixingContinueProductMap() {
		return mixingContinueProductMap;
	}

	public List<GlueScheduleResultVo> getFilterContinueScheduleResult() {
		return filterContinueScheduleResult;
	}

	public List<GlueScheduleResultVo> getProductionModelList() {
		return productionModelList;
	}
	public Map<String, List<GlueScheduleResultVo>> getMapScheduleResult() {
		return mapScheduleResult;
	}
	public Map<String, List<GlueScheduleResultVo>> getMapProductionModel() {
		return mapProductionModel;
	}

	public Map<CombinedMapKey, List<MesPmtRecipeVo>> getMesPmtRecipeMap() {
		return mesPmtRecipeMap;
	}
}
