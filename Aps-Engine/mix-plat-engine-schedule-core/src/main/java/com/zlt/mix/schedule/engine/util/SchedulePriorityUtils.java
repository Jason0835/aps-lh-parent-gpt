package com.zlt.mix.schedule.engine.util;

import com.ruoyi.common.core.utils.DateUtils;
import com.ruoyi.common.utils.StringUtils;
import com.zlt.mix.common.core.enums.ProductDayFlagEnum;
import com.zlt.mix.common.core.utils.BigDecimalUtil;
import com.zlt.mix.common.core.utils.GenerageMapKeyUtils;
import com.zlt.mix.common.engine.utils.CollectionUtil;
import com.zlt.mix.schedule.engine.constants.GlueEngineConstants;
import com.zlt.mix.schedule.engine.vo.GlueFactoryRequireVo;
import com.zlt.mix.schedule.engine.vo.GlueScheduleResultVo;
import com.zlt.mix.schedule.engine.vo.MesPmtRecipeVo;
import com.zlt.mix.schedule.engine.vo.SingleClassGlueScheduleResultVO;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.Map.Entry;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.math.NumberUtils;

/**
 * 排产优先级计算工具
 * 
 * @author zlt
 *
 */
public class SchedulePriorityUtils {
	/**
	 * 小批量生产数默认值：20
	 */
	private final static String DEFAULT_SMALL_BATCH_NUM = "20";

	/**
	 * 重算计算每个胶料的优先级
	 *
	 * @param allScheduleResultList    排程记录
	 * @param glueStock                库存信息
	 * @param factoryRequireMap        分厂需求列表
	 * @param shiftClass               所在班次
	 * @param params                   排产参数
	 * @param slPriorityMap            塑炼胶优先配方
	 * @param latestScheduleList       昨日白班机台最后生产的排程
	 * @param mixingPriorityProductMap 炼胶优先配置
	 * @param needSlScheduleMap        需要塑胶排产后的优先排产的记录
	 */
	public static void recaculatePriority(List<GlueScheduleResultVo> allScheduleResultList,
										  GlueScheduleStockPool glueStock, Map<String, GlueFactoryRequireVo> factoryRequireMap, Integer shiftClass,
										  Map<String, String> params, Map<String, List<GlueScheduleResultVo>> slPriorityMap,
										  List<GlueScheduleResultVo> latestScheduleList,
										  Map<String, String> mixingPriorityProductMap,
										  Map<String, List<GlueScheduleResultVo>> needSlScheduleMap) {
		// 将排产计划那胶料编号分组
		Map<String, List<GlueScheduleResultVo>> glueGroupingMap = allScheduleResultList.stream()
				.collect(Collectors.groupingBy(GlueScheduleResultVo::getGlue));
		
		// 计算每个胶料的所需时长 = 炼胶时长 + 停放时长
		
		for (Entry<String, List<GlueScheduleResultVo>> result : glueGroupingMap.entrySet()) {
			String glueCode = result.getKey();
			List<GlueScheduleResultVo> scheduleResult = result.getValue();
			Double requireQty = scheduleResult.stream().mapToDouble(GlueScheduleResultVo::getRequireQty).sum(); // 需求量
			BigDecimal productedQty = scheduleResult.stream().map(GlueScheduleResultVo::getProductedQty)
					.reduce(BigDecimal.ZERO, BigDecimal::add); // 已排量
            Double day1RequireQty = scheduleResult.stream().filter(s -> ProductDayFlagEnum.DAY1.getCode().equals(s.getDayFlag())).mapToDouble(GlueScheduleResultVo::getRequireQty).sum(); // 第一天需求量
			// 如果已排量超过第一天的需求量，则说明同胶料第一天的需求量已经完成，可以处理第二天的数据，否则第二天的要全部取消
            boolean isDay1Finish = productedQty.compareTo(BigDecimalUtil.valueOf(day1RequireQty)) >= 0;
			
//			BigDecimal dayUseQty = BigDecimalUtil.valueOfZero(CollectionUtil.firstElement(scheduleResult).getDayUseQty()); // 日用量
			BigDecimal surplusQty = new BigDecimal(requireQty.toString()).subtract(productedQty); // 剩余量 = 需求量 - 已排量
			BigDecimal stockNum = glueStock.getQualifiedGlueStockNum(glueCode); // 库存量
			// 调整排产优先级为：根据停放时长从长到短/库存多少两因素综合考虑
			
			BigDecimal stockPriority;
			if (stockNum.compareTo(BigDecimal.ZERO) == 0) {
				// 库存为0时，优先级 = 需求量"
			    stockPriority = surplusQty;
			} else {
				// 库存不为0时，优先级 = 需求量 / 库存
			    stockPriority = surplusQty.divide(stockNum, 6, RoundingMode.HALF_DOWN);
			}
//			if (dayUseQty.compareTo(BigDecimal.ZERO) > 0 && stockNum.compareTo(dayUseQty) > 0) { // 日用量大于0的胶料，如果库存已经超过日用量，则优先级直接放到最低
//			    priority = BigDecimal.ZERO;
//			}
			
            // 停放时长优先级
            MesPmtRecipeVo recipe = scheduleResult.get(0).getPmtRecipe();
            Long mixTime = recipe.getSummerMixTime(); // 炼胶时间（秒）
            Long minParkTime = recipe.getMinParkTime() * 60 * 60; // 物料最少停放时长,需要把小时换算成秒
            long parkTime = mixTime + minParkTime; // 停放时长 = 一车的炼胶时长 + 冷却停放时长
//            BigDecimal parkTimePriority = BigDecimalUtil.valueOf(parkTime); // 冷却时长作为优先级
            BigDecimal parkTimePriority = BigDecimalUtil.valueOf(Math.negateExact(parkTime)); // 冷却时长作为优先级（取相反数）
            
            // 计算最终优先级,两个优之和
            BigDecimal priority = stockPriority.add(parkTimePriority);
			
			// 同一个胶料的优先级一致
			for (GlueScheduleResultVo groupingResult : scheduleResult) {
			    groupingResult.setDay1Finish(isDay1Finish);
			    if (checkIsDay2CanProduct(groupingResult)) {
	                groupingResult.setPriority(priority);
			    } else {
			        groupingResult.setPriority(BigDecimal.ZERO);
			    }
			}
		}

		// 全部算完之后，再根据各种特殊规则进一步细化优先级
		BigDecimal maxPriority = allScheduleResultList.stream().map(GlueScheduleResultVo::getPriority)
				.filter(Objects::nonNull).max(BigDecimal::compareTo).orElse(BigDecimal.ZERO);// 目前的最大优先级
		Map<String, SingleClassGlueScheduleResultVO> latestScheduleMap = getLatestScheduleMap(allScheduleResultList, latestScheduleList); // 每个机台最后一个排产记录
		allScheduleResultList.forEach(s -> {
			s.setPreviousSchedule(null);
			s.setNextSchedule(null);
		});
		latestScheduleList.forEach(s -> {
			s.setPreviousSchedule(null);
			s.setNextSchedule(null);
		});

		shiftClass = shiftClass != null ? shiftClass : GlueEngineConstants.SHIFT_CLASS_MID; // 所在班次默认中班
		BigDecimal smallBatchNum = new BigDecimal(
				params.getOrDefault(GlueEngineConstants.SMALL_BATCH_NUM, DEFAULT_SMALL_BATCH_NUM)); // 参数设定的小批量车数

		// TODO 根据低气味优先续做处理优先级（暂不实现）
		// 检查预计剩余量，如果已经为0的优先级提高
//		maxPriority = processExpectedRemainingQty(allScheduleResultList, maxPriority);
		// 塑炼胶优先排产：1、如果塑胶已经全部排产完成，将对应一段胶的优先级提高 2、塑炼的优先级大于对应一段胶
		maxPriority = processSlGluePriority(allScheduleResultList, slPriorityMap, needSlScheduleMap, maxPriority);
		// 同一班次，生产模式的前置胶料在前面，提高本排程的优先级，但是不做排产前后绑定关系
		maxPriority = processProductionModel(allScheduleResultList, latestScheduleMap, shiftClass, maxPriority);
		// 同一班次，根据同胶料不同配方续做处理优先级
		maxPriority = sameGlueContinuityPriority(allScheduleResultList, latestScheduleMap, shiftClass, maxPriority);
		// 同一班次，根据胶料优先配置处理优先级：1、优先胶料排产之后，同机台的对应胶料的优先级提高 2、炼胶优先排产控制，优先胶料在对应胶料之前生产
		maxPriority = processContinuePriority(allScheduleResultList, mixingPriorityProductMap, latestScheduleMap, shiftClass, maxPriority, true);
		// 根据大批量分厂需求优先处理优先级
		maxPriority = requireSchedulePriority(factoryRequireMap, glueGroupingMap, maxPriority);
		// 根据小批量分厂需求优先处理优先级
		maxPriority = requireSmallBatchNumPriority(factoryRequireMap, glueGroupingMap, smallBatchNum, maxPriority);
		// 换班时根据续做优先处理优先级（根据配方选择顺序实际上处理了同胶料同配方第一优先，同胶料不同配方第二优先）
		maxPriority = switchClassContinuityPriority(allScheduleResultList, latestScheduleMap, shiftClass, smallBatchNum,
				maxPriority);
		// 胶料优先配置的换班次优先
		Integer previousShiftClass = ShiftClassUtil.getPreviousShiftClass(shiftClass); // 上一个班，包括昨日白班的场景
		if (previousShiftClass != null) {
			maxPriority = processContinuePriority(allScheduleResultList, mixingPriorityProductMap, latestScheduleMap, previousShiftClass, maxPriority, false);
		}
	}
	
	/**
	 * 检查是否第二天需求单第一天需求还未技术
	 * @param groupingResult
	 * @return
	 */
	private static boolean checkIsDay2CanProduct(GlueScheduleResultVo groupingResult) {
	    return ProductDayFlagEnum.DAY1.getCode().equals(groupingResult.getDayFlag()) || groupingResult.isDay1Finish();
	}

	/**
	 * 同一班次，生产模式的前置胶料在前面，提高本排程的优先级，但是不做排产前后绑定关系
	 */
	private static BigDecimal processProductionModel(List<GlueScheduleResultVo> allScheduleResultList,
													 Map<String, SingleClassGlueScheduleResultVO> latestScheduleMap,
													 Integer shiftClass,
													 BigDecimal maxPriority) {
		if (shiftClass == null) {
			return maxPriority;
		}
		for (GlueScheduleResultVo schedule : allScheduleResultList) {
			if (schedule.getProductionBefore() == null) {
				continue;
			}
			GlueScheduleResultVo beforeSchedule = schedule.getProductionBefore();
			String machineCode = schedule.getMachineCode();
			String glue = beforeSchedule.getGlue();
			SingleClassGlueScheduleResultVO latestClassSchedule = latestScheduleMap.get(machineCode);
			if (latestClassSchedule != null && Objects.equals(glue, latestClassSchedule.getGlue())) {
				GlueScheduleResultVo latestSchedule = latestClassSchedule.getScheduleResult();
				if (latestSchedule == schedule) { // 最后一个排程直接忽略掉
					continue;
				}
				if (latestSchedule.getNextSchedule() != null) { // 已经与其他排产记录绑定的忽略
					continue;
				}
				if (latestClassSchedule.getShiftClass() != shiftClass) { // 机台最后一个排产在本班
					continue;
				}
				SingleClassGlueScheduleResultVO classSchedule = new SingleClassGlueScheduleResultVO(schedule,
						shiftClass); // 取出本班的排产信息
				if (classSchedule.getPlanQty().compareTo(BigDecimal.ZERO) != 0) { // 当班已经有排产的计划忽略掉
					continue;
				}
				if (!checkIsDay2CanProduct(schedule)) {
				    continue;
				}
				// 优先级提到最高，无需绑定
				maxPriority = maxPriority.add(BigDecimal.ONE);
				schedule.setPriority(maxPriority);
			}
		}

		return maxPriority;
	}

	/**
	 * 1、优先胶料排产之后，同机台的对应胶料的优先级提高
	 * 2、炼胶优先排产控制，优先胶料在对应胶料之前生产
	 *
	 * @param allScheduleResultList    排产列表
	 * @param mixingPriorityProductMap 胶料优先列表
	 * @param latestScheduleMap        机台最后一个排产记录
	 * @param shiftClass               需要比较最后排产的班制
	 * @param maxPriority              当前最大的优先级
	 * @param maxBasePriority          需要取所有二号胶料的优先级
	 * @return 调整后的优先级
	 */
	private static BigDecimal processContinuePriority(List<GlueScheduleResultVo> allScheduleResultList,
													  Map<String, String> mixingPriorityProductMap,
													  Map<String, SingleClassGlueScheduleResultVO> latestScheduleMap,
													  Integer shiftClass,
													  BigDecimal maxPriority,
													  boolean maxBasePriority) {
		if (shiftClass == null) {
			return maxPriority;
		}

		// 记录优先胶料至少应该叠加的优先级
		Map<String, BigDecimal> priorityMap = new HashMap<>();

		for (GlueScheduleResultVo schedule : allScheduleResultList) {
			String machineCode = schedule.getMachineCode();
			String glue = schedule.getGlue();
			// 优先胶料
			String priorityGlue = mixingPriorityProductMap.get(glue);
			// 优先胶料在指定班次
			if (StringUtils.isNotBlank(priorityGlue)) {
				// 记录接续胶料的最大优先级
				BigDecimal itemPriority = priorityMap.getOrDefault(priorityGlue, BigDecimal.ZERO);
				if(maxBasePriority){
					priorityMap.put(priorityGlue, BigDecimalUtil.greatest(itemPriority, schedule.getPriority()));
				}
				
				SingleClassGlueScheduleResultVO latestClassSchedule = latestScheduleMap.get(machineCode);
				if (latestClassSchedule != null && Objects.equals(priorityGlue, latestClassSchedule.getGlue())) {
					GlueScheduleResultVo latestSchedule = latestClassSchedule.getScheduleResult();
					if (latestSchedule == schedule) { // 最后一个排程直接忽略掉
						continue;
					}
					if (latestSchedule.getNextSchedule() != null) { // 已经与其他排产记录绑定的忽略
						continue;
					}
					if (latestClassSchedule.getShiftClass() != shiftClass) { // 机台最后一个排产和指定班制不同
						continue;
					}
					SingleClassGlueScheduleResultVO classSchedule = new SingleClassGlueScheduleResultVO(schedule,
							shiftClass); // 取出本班的排产信息
					if (classSchedule.getPlanQty().compareTo(BigDecimal.ZERO) != 0) { // 当班已经有排产的计划忽略掉
						continue;
					}
					if (!checkIsDay2CanProduct(schedule)) {
					    continue;
					}
					// 互相绑定两个排产记录，并将优先级提到最高
					latestSchedule.setNextSchedule(schedule);
					schedule.setPreviousSchedule(latestSchedule);
					maxPriority = maxPriority.add(BigDecimal.ONE);
					schedule.setPriority(maxPriority);
					// 更新接续胶料的最大优先级
					priorityMap.put(priorityGlue, BigDecimalUtil.greatest(itemPriority, schedule.getPriority()));
				}
			}
		}

		// 处理优先胶料的优先级
		if (!priorityMap.isEmpty()) {
			for (GlueScheduleResultVo schedule : allScheduleResultList) {
				BigDecimal priority = priorityMap.get(schedule.getGlue());
				if (priority != null && checkIsDay2CanProduct(schedule)) {
					schedule.setPriority(BigDecimalUtil.greatest(schedule.getPriority(), priority.add(BigDecimal.ONE)));
					maxPriority = BigDecimalUtil.greatest(maxPriority, schedule.getPriority());
				}
			}
		}

		return maxPriority;
	}
	

    /**
     * 处理塑炼的优先级大于对应一段胶
     *
     * @param allScheduleResultList 排程列表
     * @param slPriorityMap         塑料胶配方
     * @param needSlScheduleMap     需要塑胶排产后的优先排产的记录
     * @param maxPriority           当前最大优先级
     * @return 处理后的优先级
     */
    private static BigDecimal processExpectedRemainingQty(List<GlueScheduleResultVo> allScheduleResultList,
                                                    BigDecimal maxPriority) {
        if (CollectionUtil.isEmpty(allScheduleResultList)) {
            return maxPriority;
        }
        
        // 取出还有未排量，但是预计排产量已经为0的记录，并按原优先级排序
        List<GlueScheduleResultVo> expectedRemainingResultList = allScheduleResultList.stream().filter(s -> {
            if (s.getExpectedRemainingQty() == null || s.getExpectedRemainingQty() > 0D) {
                return false;
            }
            BigDecimal productQty = s.getProductedQty();
            BigDecimal requireQty = BigDecimalUtil.valueOfZero(s.getRequireQty());
            return productQty.compareTo(requireQty) != 0;
        }).sorted(Comparator.comparing(GlueScheduleResultVo::getPriority, Comparator.nullsFirst(BigDecimal::compareTo)))
                .collect(Collectors.toList());
        if (CollectionUtil.isEmpty(expectedRemainingResultList)) {
            return maxPriority;
        }
        // 直接放到最后
        for (GlueScheduleResultVo itemSchedule : expectedRemainingResultList) {
            if (checkIsDay2CanProduct(itemSchedule)) {
                maxPriority = maxPriority.add(BigDecimal.ONE);
                itemSchedule.setPriority(maxPriority);
            }
        }
        return maxPriority;
    }

	/**
	 * 处理塑炼的优先级大于对应一段胶
	 *
	 * @param allScheduleResultList 排程列表
	 * @param slPriorityMap         塑料胶配方
	 * @param needSlScheduleMap     需要塑胶排产后的优先排产的记录
	 * @param maxPriority           当前最大优先级
	 * @return 处理后的优先级
	 */
	private static BigDecimal processSlGluePriority(List<GlueScheduleResultVo> allScheduleResultList,
													Map<String, List<GlueScheduleResultVo>> slPriorityMap,
													Map<String, List<GlueScheduleResultVo>> needSlScheduleMap,
													BigDecimal maxPriority) {
		if (CollectionUtil.isEmpty(allScheduleResultList)) {
			return maxPriority;
		}

		// 如果塑炼胶已全部排产，或者塑炼胶的库存充足，将对应一段胶的优先级提高
		if (needSlScheduleMap != null && !needSlScheduleMap.isEmpty()) {
			for (GlueScheduleResultVo itemResult : allScheduleResultList) {
				String needSlKey = GenerageMapKeyUtils.createMapKey(itemResult.getMachineCode(), itemResult.getGlue(), itemResult.getRecipeType(), itemResult.getRecipeVersionId());
				List<GlueScheduleResultVo> needSlSet = needSlScheduleMap.get(needSlKey);
				// 无需塑炼胶，直接跳过
				if (needSlSet == null) {
					continue;
				}

				// 塑炼胶库存充足，优先排产
				if (needSlSet.isEmpty() && checkIsDay2CanProduct(itemResult)) {
					maxPriority = maxPriority.add(BigDecimal.ONE);
					itemResult.setPriority(BigDecimalUtil.greatest(maxPriority, itemResult.getPriority()));
					continue;
				}

				// 如果塑炼胶还需要生产，判断需要的塑炼胶是否全部生产完成
				boolean needSlProduct = needSlSet.stream().anyMatch(slResultVo -> slResultVo.getPlanQty() != null
						&& (slResultVo.getProductedQty() == null || slResultVo.getPlanQty().compareTo(slResultVo.getProductedQty()) > 0));
				// 如果塑炼胶无需生产，对应一段胶优先排产，尽快消耗塑炼胶
				if (!needSlProduct && checkIsDay2CanProduct(itemResult)) {
					maxPriority = maxPriority.add(BigDecimal.ONE);
					itemResult.setPriority(BigDecimalUtil.greatest(maxPriority, itemResult.getPriority()));
				}
			}
		}


		// 塑炼胶在需要塑炼胶的一段胶之前生产
		if (slPriorityMap == null || slPriorityMap.isEmpty()) {
			return maxPriority;
		}
		for (GlueScheduleResultVo itemSchedule : allScheduleResultList) {
			// 如果当前排程为塑炼胶，取需要塑炼胶的所有的一段胶的最大优先级+1，作为当前优先级
			List<GlueScheduleResultVo> resultVoSet = slPriorityMap.get(itemSchedule.getGlue());
			if (resultVoSet == null || resultVoSet.isEmpty()) {
				continue;
			}
			if (itemSchedule.getPriority() == null) {
				continue;
			}
			for (GlueScheduleResultVo checkResultVo : resultVoSet) {
				if (checkResultVo.getPriority() == null) {
					continue;
				}
				BigDecimal itemPriority = BigDecimalUtil.greatest(itemSchedule.getPriority(), checkResultVo.getPriority());
				if (itemSchedule.getPriority().compareTo(itemPriority) < 0 && checkIsDay2CanProduct(itemSchedule)) {
					itemSchedule.setPriority(itemPriority.add(BigDecimal.ONE));
					maxPriority = BigDecimalUtil.greatest(maxPriority, itemSchedule.getPriority());
				}
			}
		}

		return maxPriority;
	}

	/**
	 * 小批量分厂需求的胶料优先，不受中夜白班的需求优先限制
	 * 
	 * @param factoryRequireMap
	 * @param glueGroupingMap
	 * @param smallBatchNum
	 * @param maxPriority
	 * @return
	 */
	private static BigDecimal requireSmallBatchNumPriority(Map<String, GlueFactoryRequireVo> factoryRequireMap,
			Map<String, List<GlueScheduleResultVo>> glueGroupingMap, BigDecimal smallBatchNum, BigDecimal maxPriority) {
		// 需求量待生产量小于20车的，优先往前排
		List<GlueFactoryRequireVo> lessRequireList = factoryRequireMap.values().stream()
				.filter(s -> s.getRequireDifference().compareTo(smallBatchNum) < 0)
				.sorted(Comparator.comparing(GlueFactoryRequireVo::getRequireDifference)).collect(Collectors.toList());
		for (GlueFactoryRequireVo requireVo : lessRequireList) {
			String glueCode = requireVo.getGlue();
			List<GlueScheduleResultVo> scheduleList = glueGroupingMap.get(glueCode);
			for (GlueScheduleResultVo schedule : scheduleList) {
				// 首次确认优先级 = 最大优先级 + 1
				maxPriority = maxPriority.add(BigDecimal.ONE);
				schedule.setPriority(maxPriority);
			}
		}
		return maxPriority;
	}

	/**
	 * 换班续做，如果上一班最后一个规格的剩余待安排数小于20车要优先做
	 * 
	 * @param allScheduleResultList
	 * @param latestScheduleMap
	 * @param shiftClass
	 * @param smallBatchNum
	 * @param maxPriority
	 * @return
	 */
	private static BigDecimal switchClassContinuityPriority(List<GlueScheduleResultVo> allScheduleResultList,
			Map<String, SingleClassGlueScheduleResultVO> latestScheduleMap, Integer shiftClass,
			BigDecimal smallBatchNum, BigDecimal maxPriority) {
		if (shiftClass == null) {
			return maxPriority;
		}
		Integer previousShiftClass = ShiftClassUtil.getPreviousShiftClass(shiftClass); // 上一个班，包括昨日白班的场景
		if (previousShiftClass == null) {
			return maxPriority;
		}
		List<SingleClassGlueScheduleResultVO> newScheduleList = new ArrayList<>(); // 待处理排产记录：本班同一个机台有排相同胶料的排产记录
		for (GlueScheduleResultVo schedule : allScheduleResultList) {
			String machineCode = schedule.getMachineCode();
			String glue = schedule.getGlue();
			SingleClassGlueScheduleResultVO latestClassSchedule = latestScheduleMap.get(machineCode);
			// 同胶料连续优先，无需考虑小于最小批量才连续
			// BigDecimal surplusQty = schedule.getPlanQty().subtract(schedule.getProductedQty());
			// if (surplusQty.compareTo(smallBatchNum) >= 0) {
			// 	continue; // 剩余车数大于20车的忽略，不需要特别处理优先级
			// }
			if (latestClassSchedule != null && Objects.equals(glue, latestClassSchedule.getGlue())) {
				GlueScheduleResultVo latestSchedule = latestClassSchedule.getScheduleResult();
				if (latestSchedule.getNextSchedule() != null) { // 已经与其他排产记录绑定的忽略
					continue;
				}
				if (latestClassSchedule.getShiftClass() != previousShiftClass) { // 机台最后一个排产在上个班
					continue;
				}
				SingleClassGlueScheduleResultVO classSchedule = new SingleClassGlueScheduleResultVO(schedule,
						shiftClass); // 取出本班的排产信息
				if (classSchedule.getPlanQty().compareTo(BigDecimal.ZERO) != 0) { // 当班已经有排产的计划忽略掉
					continue;
				}
				if (!checkIsDay2CanProduct(schedule)) {
				    continue;
				}
				// 互相绑定两个排产记录，并将优先级提到最高
				latestSchedule.setNextSchedule(schedule);
				schedule.setPreviousSchedule(latestSchedule);
				maxPriority = maxPriority.add(BigDecimal.ONE);
				schedule.setPriority(maxPriority);
				newScheduleList.add(classSchedule);
			}
		}
		return maxPriority;
	}

	/**
	 * 根据同胶料不同配方续做处理优先级
	 * 
	 * @param maxPriority
	 * @return
	 */
	private static BigDecimal sameGlueContinuityPriority(List<GlueScheduleResultVo> allScheduleResultList,
			Map<String, SingleClassGlueScheduleResultVO> latestScheduleMap, Integer shiftClass,
			BigDecimal maxPriority) {
		if (shiftClass == null) {
			return maxPriority;
		}
//		Integer previousShiftClass = ShiftClassUtil.getPreviousShiftClass(shiftClass); // 上一个班
		List<SingleClassGlueScheduleResultVO> newScheduleList = new ArrayList<>(); // 待处理排产记录：本班同一个机台有排相同胶料的排产记录
		for (GlueScheduleResultVo schedule : allScheduleResultList) {
			String machineCode = schedule.getMachineCode();
			String glue = schedule.getGlue();
			SingleClassGlueScheduleResultVO latestClassSchedule = latestScheduleMap.get(machineCode);
			if (latestClassSchedule != null && Objects.equals(glue, latestClassSchedule.getGlue())) {
				GlueScheduleResultVo latestSchedule = latestClassSchedule.getScheduleResult();
				if (latestSchedule == schedule) { // 最后一个排程直接忽略掉
					continue;
				}
				if (latestSchedule.getNextSchedule() != null) { // 已经与其他排产记录绑定的忽略
					continue;
				}
				if (latestClassSchedule.getShiftClass() != shiftClass) { // 机台最后一个排产在本班
					continue;
				}
//				if (Objects.equals(latestSchedule.getRecipeType(), schedule.getRecipeType())) { // 跟最后一个不能是同一个
//					continue;
//				}
				SingleClassGlueScheduleResultVO classSchedule = new SingleClassGlueScheduleResultVO(schedule,
						shiftClass); // 取出本班的排产信息
				if (classSchedule.getPlanQty().compareTo(BigDecimal.ZERO) != 0) { // 当班已经有排产的计划忽略掉
					continue;
				}
				if (!checkIsDay2CanProduct(schedule)) {
				    continue;
				}
				// 互相绑定两个排产记录，并将优先级提到最高
				latestSchedule.setNextSchedule(schedule);
				schedule.setPreviousSchedule(latestSchedule);
				maxPriority = maxPriority.add(BigDecimal.ONE);
				schedule.setPriority(maxPriority);
				newScheduleList.add(classSchedule);
			}
		}

		// 掉排除胶料 + 机台 + 配方本班是否已经排产的记录
//		Map<CombinedMapKey, List<SingleClassGlueScheduleResultVO>> newScheduleMap = newScheduleList.stream()
//				.collect(Collectors.groupingBy(s -> CombinedMapKey.createKey(s.getGlue(), s.getMachineCode())));
//		for (Entry<CombinedMapKey, List<SingleClassGlueScheduleResultVO>> entry : newScheduleMap.entrySet()) {
//			String glueCode = (String) entry.getKey().getKey(0);
//			String machineCode = (String) entry.getKey().getKey(1);
//			List<SingleClassGlueScheduleResultVO> scheduleGroupingList = entry.getValue();
//			SingleClassGlueScheduleResultVO classSchedule = latestScheduleMap.get(machineCode);
//			GlueScheduleResultVo latestSchedule = classSchedule.getScheduleResult();
//			
//			for (SingleClassGlueScheduleResultVO scheduleGrouping : scheduleGroupingList) {
//				GlueScheduleResultVo schedule = scheduleGrouping.getScheduleResult();
//				// 同一个配方类型
//				if (Objects.equals(latestSchedule.getRecipeType(), schedule.getRecipeType())) {
//					newScheduleList.remove(scheduleGrouping);
//				}
//			}
//		}
		return maxPriority;
	}

	private static Map<String, SingleClassGlueScheduleResultVO> getLatestScheduleMap(
			List<GlueScheduleResultVo> allScheduleResultList, List<GlueScheduleResultVo> latestScheduleList) {
		Map<String, SingleClassGlueScheduleResultVO> scheduleMap = new HashMap<>();
		// Map<String, SingleClassGlueScheduleResultVO> dayScheduleMap = getSingleClassScheduleMap(allScheduleResultList,
		// 		GlueEngineConstants.SHIFT_CLASS_DAY, scheduleMap);
		// scheduleMap.putAll(dayScheduleMap);
		Map<String, SingleClassGlueScheduleResultVO> nightScheduleMap = getSingleClassScheduleMap(allScheduleResultList,
				GlueEngineConstants.SHIFT_CLASS_NIGHT, scheduleMap);
		scheduleMap.putAll(nightScheduleMap);
		Map<String, SingleClassGlueScheduleResultVO> midScheduleMap = getSingleClassScheduleMap(allScheduleResultList,
				GlueEngineConstants.SHIFT_CLASS_MID, scheduleMap);
		scheduleMap.putAll(midScheduleMap);
		// 记录昨日早班最后一个排程，作为区分白班连续排程
		Map<String, SingleClassGlueScheduleResultVO> dayScheduleMap = getSingleClassScheduleMap(latestScheduleList,
				GlueEngineConstants.SHIFT_CLASS_DAY, scheduleMap);
		scheduleMap.putAll(dayScheduleMap);
		return scheduleMap;
	}

	private static Map<String, SingleClassGlueScheduleResultVO> getSingleClassScheduleMap(
			List<GlueScheduleResultVo> allScheduleResultList, Integer shiftClass,
			Map<String, SingleClassGlueScheduleResultVO> scheduleMap) {
		return allScheduleResultList.stream().map(s -> new SingleClassGlueScheduleResultVO(s, shiftClass))
				.filter(s -> s.getPlanQty().compareTo(BigDecimal.ZERO) > 0 && !scheduleMap.containsKey(s.getMachineCode())
						&& s.getExpectFinishTime() != null)
				.collect(Collectors.toMap(SingleClassGlueScheduleResultVO::getMachineCode, Function.identity(),
						(s1, s2) -> s1.getExpectFinishTime().compareTo(s2.getExpectFinishTime()) >= 0 ? s1 : s2));
	}

	/**
	 * 计算分厂需求优先级
	 * 
	 * @param factoryRequireMap
	 * @param glueGroupingMap
	 * @param maxPriority
	 */
	private static BigDecimal requireSchedulePriority(Map<String, GlueFactoryRequireVo> factoryRequireMap,
			Map<String, List<GlueScheduleResultVo>> glueGroupingMap, BigDecimal maxPriority) {
		// 统计各分厂需求情况，<需求班次，需求差值列表>
		Map<Integer, List<GlueFactoryRequireVo>> requireMap = factoryRequireMap.values().stream()
				.sorted(Comparator.comparing(GlueFactoryRequireVo::getRequireDifference))
//				.sorted((r1, r2) -> {
//					// 排序顺序：需求量大于等于20，顺序排序 -> 需求量小于20，倒序排序
//					BigDecimal requireQty1 = r1.getRequireDifference();
//					BigDecimal requireQty2 = r2.getRequireDifference();
//					
//					boolean isAbove1 = requireQty1.compareTo(new BigDecimal("20")) >= 0;
//					boolean isAbove2 = requireQty2.compareTo(new BigDecimal("20")) >= 0;
//					if (isAbove1 && isAbove2) {	// 两个都超过20的时候顺序排序
//						return requireQty1.compareTo(requireQty2);
//					} else {
//						return requireQty2.compareTo(requireQty1); // 其余情况倒序排序
//					}
//				}) // 按需求量排序
				.collect(Collectors.groupingBy(GlueFactoryRequireVo::getRequireClass)); // 按需求班别分组
		// 分别取出各班需求的排程列表
		List<GlueFactoryRequireVo> midRequireList = requireMap.get(GlueEngineConstants.SHIFT_CLASS_MID);
		List<GlueFactoryRequireVo> nightRequireList = requireMap.get(GlueEngineConstants.SHIFT_CLASS_NIGHT);
		// List<GlueFactoryRequireVo> dayRequireList = requireMap.get(GlueEngineConstants.SHIFT_CLASS_DAY);
		Map<String, BigDecimal> gluePriorityMap = new HashMap<>();
		// 从白班开始往回更新有需求班次的胶料优先级，时间越早优先级越高
		// maxPriority = updateRequireSchedulePriority(glueGroupingMap, dayRequireList, maxPriority, gluePriorityMap);
		maxPriority = updateRequireSchedulePriority(glueGroupingMap, nightRequireList, maxPriority, gluePriorityMap);
		maxPriority = updateRequireSchedulePriority(glueGroupingMap, midRequireList, maxPriority, gluePriorityMap);
		return maxPriority;
	}

	/**
	 * 更新有需求班次的胶料优先级
	 * 
	 * @param scheduleList    待更新胶料
	 * @param maxPriority     目前最大优先级
	 * @param gluePriorityMap 已确认优先级的胶料
	 * @return 更新后的最大优先级
	 */
	private static BigDecimal updateRequireSchedulePriority(Map<String, List<GlueScheduleResultVo>> glueGroupingMap,
			List<GlueFactoryRequireVo> requireList, BigDecimal maxPriority, Map<String, BigDecimal> gluePriorityMap) {
		if (CollectionUtil.isEmpty(requireList)) {
			return maxPriority;
		}
		BigDecimal tempMaxPriority = maxPriority;
		for (GlueFactoryRequireVo require : requireList) {
			String glueCode = require.getGlue();
			List<GlueScheduleResultVo> scheduleList = glueGroupingMap.get(glueCode);
			for (GlueScheduleResultVo result : scheduleList) {
			    if (!checkIsDay2CanProduct(result)) {
			        continue;
			    }
				BigDecimal priority = gluePriorityMap.get(glueCode);
				if (priority != null) { // 同一个胶料的优先级都是一样的，因此已确认优先的胶料统一赋值首次确认的优先级
					result.setPriority(priority);
					continue;
				}
				// 首次确认优先级 = 最大优先级 + 1
				tempMaxPriority = tempMaxPriority.add(BigDecimal.ONE);
				gluePriorityMap.put(glueCode, tempMaxPriority);
				result.setPriority(tempMaxPriority);
			}
		}

		return tempMaxPriority;
	}
}
