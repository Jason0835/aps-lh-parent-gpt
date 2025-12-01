package com.zlt.aps.cd15.engine.service.impl;

import com.zlt.aps.cd15.api.domain.entity.Cd15CurlLength;
import com.zlt.aps.cd15.api.domain.entity.Cd15LineSideStock;
import com.zlt.aps.cd15.api.domain.entity.Cd15MachineInfo;
import com.zlt.aps.cd15.engine.mapper.Cd15EngineBigRollMapper;
import com.zlt.aps.cd15.engine.mapper.Cd15EngineMapper;
import com.zlt.aps.cd15.engine.mapper.Cd15EngineStockMapper;
import com.zlt.aps.cd15.engine.service.Cd15EngineLossService;
import com.zlt.aps.cd15.engine.service.Cd15EnginePlanQtyService;
import com.zlt.aps.cd15.engine.utils.Cd15EngineUtils;
import com.zlt.aps.cd15.engine.vo.Cd15ParamsVo;
import com.zlt.aps.cd15.engine.vo.Cd15ScheduleResultVo;
import com.zlt.aps.cd15.engine.vo.Cd15StockVo;
import com.zlt.aps.common.core.constant.ApsConstant;
import com.zlt.aps.common.core.utils.BigDecimalUtil;
import com.zlt.aps.common.core.utils.BigDecimalUtils;
import com.zlt.aps.common.engine.common.CxEngineQuotaCommonService;
import com.zlt.aps.common.engine.constants.EngineConstants;
import com.zlt.aps.common.engine.enums.OpenMachineClassEnums;
import com.zlt.aps.common.engine.service.AutoScheduleLogService;
import com.zlt.aps.common.engine.utils.CollectionUtil;
import com.zlt.aps.common.engine.utils.GenerageMapKeyUtils;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.alibaba.fastjson.JSON.toJSONString;
import static com.zlt.aps.common.core.utils.ApsCommonUtil.getDouble;
import static com.zlt.aps.common.core.utils.ApsCommonUtil.logSplit;

/**
 * 15度裁断库存信息处理服务实现类
 * 
 * @Description
 * @Author hakimrayn
 * @Date 2021-7-9 11:30:42
 * @Version 1.0
 */
@Service("cd15EnginePlanQtyService")
public class Cd15EnginePlanQtyServiceImpl implements Cd15EnginePlanQtyService {
	/**
	 * 可供时长参数：8小时（成型一个班的时长）
	 */
	private static final BigDecimal SUPPLY_TIME_PARAM = new BigDecimal("12");
	/**
	 * 一千，用于毫米换算成米
	 */
	private static final BigDecimal ONE_THOUSAND = new BigDecimal("1000");
	/**
	 * 幅宽默认值
	 */
	private static final String DEFAULT_BREADTH = "1";
    private final static String DEFAULT_PRODUCT_STOCK_HOUR = "12"; // 默认值：保库存供应时长默认一个班
//    private final static String DEFAULT_PRODUCT_STOCK_HOUR2 = "10";
    private final static BigDecimal DEFAULT_CRIMP_LENGTH = new BigDecimal("190"); // 卷曲长度默认值：190
    private final static String DEFAULT_ONE_ROLL_NUM = "2"; // 一次生产卷数默认值
    private static final String DEFAULT_EQUAL_SHARE_THRESHOLD = "500"; // 需求量超过该值早夜班对半分

	@Autowired
	private Cd15EngineStockMapper cd15EngineStockMapper;
	@Autowired
	private Cd15EngineLossService cd15EngineLossService;
    @Autowired
    private Cd15EngineBigRollMapper cd15EngineBigRollMapper;
	@Resource
	private CxEngineQuotaCommonService cxEngineQuotaCommonService;
	@Resource
	private AutoScheduleLogService autoScheduleLogService;

	/**
	 * 计算排产库存
	 * 
	 * @Author hakimryan
	 * @Description
	 * @Date 2021-7-9 11:31:01
	 * @Param scheduleDate 排产日期
	 * @Param scheduleList 排产记录
	 * @Param defaultLossRate 默认损耗率
	 * @Param stockLossRate 库存损耗率
	 * @Param isProductionStage 仅对投产阶段规格排产
	 * @Param crimpLength 卷曲长度
	 * @Param minRoundRollNumStr 最小取整卷数
	 */
	@Override
	public void calculateSchedulePlanQty(Date scheduleDate, List<Cd15ScheduleResultVo> scheduleList,
			String defaultLossRate, BigDecimal stockLossRate, boolean isProductionStage, BigDecimal crimpLength,
			BigDecimal minRoundRollNum) {
		// 批次号
		String batchNo = CollectionUtil.firstElement(scheduleList).getBatchNo();
		// 获取库存量
		// 加载库存信息，16点预计库存量
		// 计算公式： (库存量 - 不良数 + 修正数) - (前日三班计划量 - 12点成型完成量) * 单耗
//		Map<String, Cd15StockVo> stockMap = this.getStockMap(scheduleDate, stockLossRate, isProductionStage);
		Map<String, BigDecimal> stockMap = this.loadCd15Stock(scheduleDate);
		Map<String, Double> lastDayMidPlanMap = this.loadLastDayMidPlan(scheduleDate);
        Map<String, String> paramsMap = cd15EngineStockMapper.listCd15Params().stream()
                .collect(Collectors.toMap(Cd15ParamsVo::getParamCode, Cd15ParamsVo::getParamValue, (v1, v2) -> v2));

		// 获取损耗率设定
		Map<String, Double> lossRateMap = cd15EngineLossService.getLossRateMap();
		// 默认损耗率类型转换
		Double defaultLossRateNum = getDouble(defaultLossRate);

		// 计算库存相关信息：16点半部件库存、可用时长，并根据库存重算计划量
		for (Cd15ScheduleResultVo resultVo : scheduleList) {
			// 计算前的排程数据json字符串，用于日志记录
			String oldScheduleResult = toJSONString(resultVo);
			String steelStripCode1 = resultVo.getSteelStripCode1();
			String steelStripCode2 = resultVo.getSteelStripCode2();
			String machineId = resultVo.getMachineId();
            resultVo.setCloseOutSpecFlag(ApsConstant.STATUS_DISABLE); // 默认非收尾

			// 15度裁断库存信息
			BigDecimal stockQty = stockMap.getOrDefault(steelStripCode1, BigDecimal.ZERO);
			// 16点半部件2号钢带库存量
			BigDecimal stockQty2 = stockMap.getOrDefault(steelStripCode2, BigDecimal.ZERO);
			// 成型可供时长
			BigDecimal supplyTime = this.caculateSuppliyTime(resultVo, stockQty);
			BigDecimal supplyTime2 = this.caculateSuppliyTime(resultVo, stockQty2);

			// 处理中夜班量，分别处理两段钢带的数值
            BigDecimal lastMidPlnQty1 = BigDecimalUtils.valueOf(lastDayMidPlanMap.get(resultVo.getSteelStripCode1()));
            BigDecimal lastMidPlnQty2 = BigDecimalUtils.valueOf(lastDayMidPlanMap.get(resultVo.getSteelStripCode2()));
            resultVo.setLastMidPlanQty1(lastMidPlnQty1.doubleValue());
            resultVo.setLastMidPlanQty2(lastMidPlnQty2.doubleValue());
            resultVo.setStock1Qty1(stockQty.doubleValue());
            resultVo.setStock1Qty2(stockQty2.doubleValue());
            BigDecimal stockDiff = stockQty.add(lastMidPlnQty1).subtract(stockQty2.add(lastMidPlnQty2)).abs(); // 计算两端钢带的备库量差值（库存+昨日早班计划）
            boolean isMatchStock = stockDiff.compareTo(crimpLength) > 0; // 如果两段钢带的库存差异较大（一卷以上），则两段钢带的量可以不一样
            Double cxPlanQty = this.getCxClassPlanCumulative(resultVo, OpenMachineClassEnums.CLASS_FOUR); // 成型两天的用量
//            boolean isCloseOutSpec1 = BigDecimalUtils.sub(resultVo.getSurplusQty(), cxPlanQty).compareTo(crimpLength) < 0; // 1#钢带临近收尾标记
//            boolean isCloseOutSpec2 = BigDecimalUtils.sub(resultVo.getSurplusQty2(), cxPlanQty).compareTo(crimpLength) < 0; // 2#钢带临近收尾标记
            boolean isCloseOutSpec1 = resultVo.getSurplusQty() <= cxPlanQty; // 1#钢带临近收尾标记
            boolean isCloseOutSpec2 = resultVo.getSurplusQty2() <= cxPlanQty; // 2#钢带临近收尾标记
            boolean isSeparate = isMatchStock || isCloseOutSpec1 || isCloseOutSpec2; // 任意一个规格需要收尾，则都需要分开算两个规格
			if (isSeparate) { // 如果需要配平库存，先计算1#钢带的计划量，再计算2#钢带的计划量
                this.handleSecondaryProduct(resultVo, resultVo.getSteelStripCode1(), resultVo.getStock1Qty1(), resultVo.getLastMidPlanQty1(), resultVo.getSurplusQty(), true, paramsMap);
			    this.handleSecondaryProduct(resultVo, resultVo.getSteelStripCode2(), resultVo.getStock1Qty2(), resultVo.getLastMidPlanQty2(), resultVo.getSurplusQty2(), true, paramsMap);
			} else { // 如果不需要分开，则以库存最少的为准计算计划量
			    boolean isFirst = stockQty.compareTo(stockQty2) < 0;
			    String steelStripCode = isFirst? resultVo.getSteelStripCode1(): resultVo.getSteelStripCode2();
			    Double stock1Qty = isFirst? resultVo.getStock1Qty1(): resultVo.getStock1Qty2();
			    double lastMidPlanQty = isFirst? resultVo.getLastMidPlanQty1(): resultVo.getLastMidPlanQty2();
			    double surplusQty = isFirst? resultVo.getSurplusQty(): resultVo.getSurplusQty2();
		        this.handleSecondaryProduct(resultVo, steelStripCode, stock1Qty, lastMidPlanQty, surplusQty, false, paramsMap);
			}
			
			// 计算损耗率
			// 重算后的计划量
			BigDecimal newDayPlanQty1 = BigDecimalUtils.valueOf(resultVo.getDayPlanQty1());
            BigDecimal newDayPlanQty2 = BigDecimalUtils.valueOf(resultVo.getDayPlanQty2());
			BigDecimal newNightPlanQty1 = BigDecimalUtils.valueOf(resultVo.getNightPlanQty1());
            BigDecimal newNightPlanQty2 = BigDecimalUtils.valueOf(resultVo.getNightPlanQty2());
			// 获取损耗率
			Double lossRate = cd15EngineLossService.getLossRate(steelStripCode1, machineId, lossRateMap,
					defaultLossRateNum);
			// 为弥补损耗的量，计划量需要填补损耗量，计算公式：新计划量 = 原计划量+原计划量*损耗率
			newDayPlanQty1 = newDayPlanQty1.add(newDayPlanQty1.multiply(BigDecimal.valueOf(lossRate))).setScale(0, RoundingMode.UP);
            newDayPlanQty2 = newDayPlanQty2.add(newDayPlanQty2.multiply(BigDecimal.valueOf(lossRate))).setScale(0, RoundingMode.UP);
			newNightPlanQty1 = newNightPlanQty1.add(newNightPlanQty1.multiply(BigDecimal.valueOf(lossRate))).setScale(0, RoundingMode.UP);
            newNightPlanQty2 = newNightPlanQty2.add(newNightPlanQty2.multiply(BigDecimal.valueOf(lossRate))).setScale(0, RoundingMode.UP);

			// 给排产明细重新赋值
			// 结果小数舍入方式调整，modify by 20211230
			resultVo.setSupplyTime1(supplyTime.doubleValue());
			resultVo.setSupplyTime2(supplyTime2.doubleValue());
			resultVo.setStock1Qty1(stockQty.setScale(0, RoundingMode.DOWN).doubleValue());
			resultVo.setStock1Qty2(stockQty2.setScale(0, RoundingMode.DOWN).doubleValue());
			resultVo.setDayPlanQty1(newDayPlanQty1.doubleValue());
            resultVo.setDayPlanQty2(newDayPlanQty2.doubleValue());
            resultVo.setNightPlanQty1(newNightPlanQty1.doubleValue());
			resultVo.setNightPlanQty2(newNightPlanQty2.doubleValue());
			resultVo.setNextDayPlanQty(BigDecimalUtils.greatest(resultVo.getNextDayPlanQty(), BigDecimal.ZERO).doubleValue());
            resultVo.setNextDayPlanQty2(BigDecimalUtils.greatest(resultVo.getNextDayPlanQty2(), BigDecimal.ZERO).doubleValue());
			resultVo.setTotalPlanQty(newDayPlanQty1.add(newNightPlanQty1));

			OpenMachineClassEnums currentClass = OpenMachineClassEnums.CLASS_TWO;
			while (currentClass.getClassIndex() <= OpenMachineClassEnums.CLASS_FOUR.getClassIndex()) { // 循环检查夜班、早班、次日夜班三个班的计划
			    OpenMachineClassEnums nextClass = currentClass.getNextClass();
			    Double planQty1 = this.getPlanQty(resultVo, currentClass, true); // 1#钢带当班计划量
			    Double planQty2 = this.getPlanQty(resultVo, currentClass, false); // 2#钢带当班计划量
			    boolean hasPlanQty1 = planQty1 > 0;
                boolean hasPlanQty2 = planQty2 > 0;
                if (hasPlanQty1 ^ hasPlanQty2) { // 如果当班一个钢带有另一个钢带没有，则需要尝试把本班没有下一个班有的提前，让两个班都有计划
                    Double nextPlanQty = this.getPlanQty(resultVo, nextClass, !hasPlanQty1); // 本班计划量为0那一段
                    Double nextPlanQty2 = this.getPlanQty(resultVo, nextClass, hasPlanQty1);
                    if (nextPlanQty > 0 && nextPlanQty2 == 0) { // 如果下个班两段钢带都有计划，则不处理
                        this.setPlanQty(resultVo, currentClass, nextPlanQty, !hasPlanQty1);
                        this.setPlanQty(resultVo, nextClass, 0D, !hasPlanQty1);
                    }
                }
                currentClass = nextClass;
			}
			
			// 记录计算日志
			this.insertCalculateLog(oldScheduleResult, resultVo, lossRate);
		}

		// 重算大卷数
//		this.recaculatePlanNum(scheduleDate, scheduleList, isProductionStage, minRoundRollNum, paramsMap);

		// 记录日志
		String logDetail = logSplit("库存量与成型定额设置：" + toJSONString(stockMap), "损耗率设定：" + toJSONString(lossRateMap));
		autoScheduleLogService.insertCd15ScheduleLog(batchNo, "", "3.1、计划量计算基础数据日志", logDetail);
	}
	
    /**
     * 获取排产计划指定班次的计划量
     * @param resultVo
     * @param currentClass
     * @return
     */
    private Double getPlanQty(Cd15ScheduleResultVo resultVo, OpenMachineClassEnums currentClass, boolean isFirstSteelStrip) {
        if (currentClass == OpenMachineClassEnums.CLASS_ONE) {
            return isFirstSteelStrip? resultVo.getLastMidPlanQty1(): resultVo.getLastMidPlanQty2();
        } else if (currentClass == OpenMachineClassEnums.CLASS_TWO) {
            return isFirstSteelStrip? resultVo.getDayPlanQty1(): resultVo.getDayPlanQty2();
        } else if (currentClass == OpenMachineClassEnums.CLASS_THREE) {
            return isFirstSteelStrip? resultVo.getNightPlanQty1(): resultVo.getNightPlanQty2();
        } else if (currentClass == OpenMachineClassEnums.CLASS_FOUR) {
            return isFirstSteelStrip? resultVo.getNextDayPlanQty(): resultVo.getNextDayPlanQty2();
        }
        return 0D;
    }
    
    /**
     * 设定计划量到排产计划指定班次中
     * @param resultVo
     * @param currentClass
     * @param planQty
     * @return
     */
    private void setPlanQty(Cd15ScheduleResultVo resultVo, OpenMachineClassEnums currentClass, Double planQty, boolean isFirstSteelStrip) {
        if (currentClass == OpenMachineClassEnums.CLASS_TWO) {
            if (isFirstSteelStrip) {
                resultVo.setDayPlanQty1(planQty);
            } else {
                resultVo.setDayPlanQty2(planQty);
            }
        } else if (currentClass == OpenMachineClassEnums.CLASS_THREE) {
            if (isFirstSteelStrip) {
            resultVo.setNightPlanQty1(planQty);
            } else {
                resultVo.setNightPlanQty2(planQty);
            }
        } else if (currentClass == OpenMachineClassEnums.CLASS_FOUR) {
            if (isFirstSteelStrip) {
            resultVo.setNextDayPlanQty(planQty);
            } else {
                resultVo.setNextDayPlanQty2(planQty);
            }
        }
    }

	/**
	 * 重算大卷数
	 * 
	 * @param scheduleDate      排产日
	 * @param scheduleList      排产列表
	 * @param isProductionStage 是否投产
	 * @param minRoundRollNum   最小卷数
	 */
	private void recaculatePlanNum(Date scheduleDate, List<Cd15ScheduleResultVo> scheduleList,
			boolean isProductionStage, BigDecimal minRoundRollNum, Map<String, String> cd15Params) {
		Map<String, String> params = cd15EngineStockMapper.listGdyyParams().stream()
				.collect(Collectors.toMap(Cd15ParamsVo::getParamCode, Cd15ParamsVo::getParamValue));
		// 标准大卷长度默认值
		BigDecimal standardSize = new BigDecimal(params.getOrDefault(EngineConstants.STANDARD_SIZE, "0"));
		BigDecimal breadth = new BigDecimal(params.getOrDefault(EngineConstants.BREADTH, DEFAULT_BREADTH));
		// 标准大卷长度默认值
		BigDecimal crimpLength = new BigDecimal(cd15Params.getOrDefault(EngineConstants.CRIMP_LENGTH, "0"));

		// 取出收尾规格
		List<String> closeOutSpecList = cd15EngineStockMapper.listCloseOutSpec(scheduleDate, isProductionStage);

		// 线边库库存
		Map<String, List<Cd15LineSideStock>> lineSideStockMap = cd15EngineStockMapper
				.listCd15LineSideStock(scheduleDate).stream().collect(Collectors
						.groupingBy(s -> GenerageMapKeyUtils.createMapKey(s.getMachineCode(), s.getMaterialCode())));
		// 机台map
		Map<Long, String> machineMap = cd15EngineStockMapper.listCd15MachineInfo().stream()
				.collect(Collectors.toMap(Cd15MachineInfo::getId, Cd15MachineInfo::getMachineCode));

		// 抓取钢压大卷基础信息
		List<Cd15CurlLength> bigRollList = cd15EngineBigRollMapper.listCd15CurlLength();
		Map<String, BigDecimal> bigRollMap = bigRollList.stream().collect(
				Collectors.toMap(Cd15CurlLength::getSteelStripCode, Cd15CurlLength::getCurlLength, (v1, v2) -> v2));

		// 根据大卷对排产计划分组
		Map<String, List<Cd15ScheduleResultVo>> codeGroupMap = scheduleList.stream()
				.sorted(Comparator.comparing(Cd15ScheduleResultVo::getTotalPlanQty, Comparator.reverseOrder()))
				.collect(Collectors.groupingBy(item -> String.join("|", item.getSteelStripCode1(), item.getSteelStripCode2())));

		// 收尾规格打标记
		scheduleList.forEach(r -> {
			String classOutStatus = closeOutSpecList.contains(this.createCloseOutSpecKey(r))
					? ApsConstant.STATUS_ENABLE
					: ApsConstant.STATUS_DISABLE;
			r.setCloseOutSpecFlag(classOutStatus);
		});

		// 遍历对计划量做取整操作
		for (Map.Entry<String, List<Cd15ScheduleResultVo>> entry : codeGroupMap.entrySet()) {
			String codeKey = entry.getKey();
			List<Cd15ScheduleResultVo> value = entry.getValue();
			for (Cd15ScheduleResultVo cd15ScheduleResultVo : value) {
				if (EngineConstants.CLOSE_TIP_NEED.equals(cd15ScheduleResultVo.getCloseOutSpecFlag())) {
					continue; // 如果已经全部收尾，则不需要做取整操作
				}
				boolean isclassOutSpec = false; // 只要能走到这一步，必定是还没完全收尾
				// 汇总同种大卷的总计划量
				BigDecimal planQty = cd15ScheduleResultVo.getTotalPlanQty();
				if (Optional.ofNullable(planQty).orElse(BigDecimal.ZERO).compareTo(BigDecimal.ZERO) <= 0) {
					continue;
				}
				BigDecimal requireQty = this.caculateRollQty(cd15ScheduleResultVo, breadth); // 计划量换算成需求量
				if (Optional.ofNullable(requireQty).orElse(BigDecimal.ZERO).compareTo(BigDecimal.ZERO) <= 0) {
					continue;
				}
				BigDecimal lineSideStockQty = BigDecimal.ZERO;

				// 重算实际排产计划
				BigDecimal newPlanQty;
				if (lineSideStockQty.compareTo(BigDecimal.ZERO) <= 0) { // 没有线边库，按照设定好的取舍规则（暂定1、2舍弃，3以上取整），取舍后的大卷个数按照系统设置的大卷设置的大卷参数长度，根据计算得出的米数进行计划下达
					String steelStripCode1 = cd15ScheduleResultVo.getSteelStripCode1();
					String steelStripCode2 = cd15ScheduleResultVo.getSteelStripCode2();
					String steelStripCode = "";
					if (StringUtils.isNotBlank(steelStripCode1)) {
						steelStripCode = steelStripCode1;
					} else if (StringUtils.isNotBlank(steelStripCode2)) {
						steelStripCode = steelStripCode2;
					}
					// 大卷长度
					BigDecimal clothLength = bigRollMap.getOrDefault(steelStripCode, crimpLength);
					if (clothLength.compareTo(BigDecimal.ZERO) == 0) {
						continue;
					}
					BigDecimal planNum = planQty.divide(clothLength, 1, RoundingMode.UP); // 大卷数，保留1位小数
					// 大卷数小数部分处理
					if (planNum.subtract(planNum.setScale(0, RoundingMode.DOWN)).compareTo(minRoundRollNum) >= 0) {
						planNum = planNum.setScale(0, RoundingMode.UP); // 如果小数部分大于等于最小取整卷数，小数部分
					} else if (planNum.compareTo(minRoundRollNum) < 0) {
						planNum = planNum.setScale(0, RoundingMode.UP); // 如果原计划卷数比最小取整卷数少，直接进位
					} else {
						planNum = planNum.setScale(0, RoundingMode.DOWN); // 其余情况舍去小数部分
					}
					newPlanQty = planNum.multiply(clothLength).setScale(0, RoundingMode.UP); // 新计划量
				} else if (requireQty.compareTo(lineSideStockQty) == 0) { // 如果线边库库存组合刚好等于需求两，则不需要处理
					newPlanQty = planQty;
				} else { // 有线边库库存，将线边库换算成计划量
					newPlanQty = BigDecimal.ZERO;
				}

				BigDecimal defferentPlanQty = newPlanQty.subtract(planQty); // 新计划 - 原计划得到的差值
				if (defferentPlanQty.compareTo(BigDecimal.ZERO) == 0) {
					continue; // 两计划无差别，则直接跳过
				}

				if (defferentPlanQty.compareTo(BigDecimal.ZERO) > 0) {
					// 新计划较大，将计划量直接加到计划量最大那一班中
					for (Cd15ScheduleResultVo schedule : value) {
						if (ApsConstant.STATUS_ENABLE.equals(schedule.getCloseOutSpecFlag())) { // 已收尾的不动计划量
							continue;
						}
						BigDecimal oldPlanQty;
						if (schedule.getDayPlanQty1() > 0) {
							oldPlanQty = BigDecimal.valueOf(schedule.getDayPlanQty1());
							schedule.setDayPlanQty1(defferentPlanQty.add(oldPlanQty).doubleValue());
						} else {
							oldPlanQty = BigDecimal.valueOf(schedule.getNightPlanQty1());
							schedule.setNightPlanQty1(defferentPlanQty.add(oldPlanQty).doubleValue());
						}
						break;
					}
				} else {
					BigDecimal surplusQty = defferentPlanQty;// 剩余量
					// 新计划较小，从计划量最大的一班开始扣减，不够则从计划量第二大的一班开始扣减，依此类推
					for (Cd15ScheduleResultVo schedule : value) {
						if (ApsConstant.STATUS_ENABLE.equals(schedule.getCloseOutSpecFlag())) { // 已收尾的不动计划量
							continue;
						}
						BigDecimal oldPlanQty = schedule.getDayPlanQty1() > 0
								? BigDecimal.valueOf(schedule.getDayPlanQty1())
								: BigDecimal.valueOf(schedule.getNightPlanQty1());
						BigDecimal reduceQty = surplusQty.compareTo(oldPlanQty) > 0 ? oldPlanQty : surplusQty;
						surplusQty = surplusQty.subtract(reduceQty);
						Double finalPlanQty = oldPlanQty.add(reduceQty).doubleValue();
						if (schedule.getDayPlanQty1() > 0) {
							schedule.setDayPlanQty1(finalPlanQty);
						} else {
							schedule.setNightPlanQty1(finalPlanQty);
						}
					}
				}

			}
		}

		// 取整完成后计算边胶用量
		for (Cd15ScheduleResultVo cd15ScheduleResultVo : scheduleList) {
			// 边胶用量 = (夜班计划+早班计划)除以10
			double totalPlan = cd15ScheduleResultVo.getDayPlanQty1() + cd15ScheduleResultVo.getNightPlanQty1();
			cd15ScheduleResultVo.setEdgeGluePlan(totalPlan / 10);
		}
	}

	/**
	 * 创建收尾规格key，用于判断是否收尾规格
	 * 
	 * @param resultVo
	 * @return
	 */
	private String createCloseOutSpecKey(Cd15ScheduleResultVo resultVo) {
		String steelStripCode1 = resultVo.getSteelStripCode1();
		String steelStripCode2 = resultVo.getSteelStripCode2();
		String cuttingAngleStr = "";
		Double cuttingAngle = resultVo.getCuttingAngle();
		if (cuttingAngle != null) {
			cuttingAngleStr = BigDecimal.valueOf(cuttingAngle).stripTrailingZeros().toPlainString();
		}
		return StringUtils.join(new String[] { steelStripCode1, steelStripCode2, cuttingAngleStr }, "#");
	}

	/**
	 * 计算需要消耗的大卷米数
	 * 
	 * @param schedule 排产记录
	 * @param breadth  幅宽
	 * @return
	 */
	public BigDecimal caculateRollQty(Cd15ScheduleResultVo schedule, BigDecimal breadth) {
		if (breadth.compareTo(BigDecimal.ZERO) <= 0) {
			breadth = new BigDecimal(DEFAULT_BREADTH);
		}
		// 大卷米数 = 钢带计划量 * (1号工艺 + 2号工艺) / 幅宽
		BigDecimal planQty = schedule.getTotalPlanQty();
		BigDecimal craft1 = BigDecimal.ZERO;
		BigDecimal craft2 = BigDecimal.ZERO;
		if (BigDecimalUtil.isDigits(schedule.getCraft1())) {
			craft1 = new BigDecimal(schedule.getCraft1()).divide(ONE_THOUSAND);
		}
		if (BigDecimalUtil.isDigits(schedule.getCraft2())) {
			craft2 = new BigDecimal(schedule.getCraft2()).divide(ONE_THOUSAND);
		}
		return planQty.multiply(craft1.add(craft2)).divide(breadth, 0, RoundingMode.UP);
	}

	/**
	 * 计算可以排产的钢带米数
	 * 
	 * @param schedule   排产记录
	 * @param stockQty   线边库大卷米数
	 * @param requireQty 需求规格的总大卷米数
	 * @return
	 */
	public BigDecimal caculatePlanQty(Cd15ScheduleResultVo schedule, BigDecimal stockQty, BigDecimal requireQty) {
		if (requireQty.compareTo(BigDecimal.ZERO) == 0) {
			return BigDecimal.ZERO;
		}
		BigDecimal totalPlanQty = schedule.getTotalPlanQty();
		BigDecimal craft1 = BigDecimal.ZERO;
		BigDecimal craft2 = BigDecimal.ZERO;
		if (NumberUtils.isDigits(schedule.getCraft1())) {
			craft1 = new BigDecimal(schedule.getCraft1()).divide(ONE_THOUSAND);
		}
		if (NumberUtils.isDigits(schedule.getCraft2())) {
			craft2 = new BigDecimal(schedule.getCraft2()).divide(ONE_THOUSAND);
		}
		if (craft1.add(craft2).compareTo(BigDecimal.ZERO) == 0) {
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
	private Map<String, List<Cd15ScheduleResultVo>> chooseMachineByStock(String bigRollCode,
			List<Cd15ScheduleResultVo> scheduleList, Map<String, List<Cd15LineSideStock>> lineSideStockMap,
			Map<Long, String> machineMap) {
		Map<String, List<Cd15ScheduleResultVo>> hasStockMap = new HashMap<>();
		for (Cd15ScheduleResultVo schedule : scheduleList) {
			String machineCode = StringUtils.EMPTY;
			if (schedule.getMachineId() != null) { // 有机台才需要关联线边库库存
				for (String machineId : StringUtils.split(schedule.getMachineId(), ",")) { // 如果有多个机台，则选定有线边库的机台作为生产机台
					if (NumberUtils.isDigits(machineId)) {
						String tempMachineCode = machineMap.get(new Long(machineId));
						List<Cd15LineSideStock> stockList = lineSideStockMap
								.get(GenerageMapKeyUtils.createMapKey(tempMachineCode, bigRollCode));
						if (stockList != null && stockList.stream()
								.anyMatch(stock -> stock.getStockNum().compareTo(BigDecimal.ZERO) > 0)) {
							machineCode = tempMachineCode;
							break;
						}
					}
				}
			}
			List<Cd15ScheduleResultVo> hasStockList = hasStockMap.get(machineCode); // 无论有没有机台都按机台分组
			if (CollectionUtil.isEmpty(hasStockList)) {
				hasStockList = new ArrayList<>();
				hasStockMap.put(machineCode, hasStockList);
			}
			hasStockList.add(schedule);
		}
		return hasStockMap;
	}
	
	/**
	 * 计算库存的成型可供时长
	 * @param resultVo 帘布排产记录
	 * @param stockQty 库存
	 * @return
	 */
    private BigDecimal caculateSuppliyTime(Cd15ScheduleResultVo resultVo, BigDecimal stockQty) {
        Cd15StockVo stockVo = new Cd15StockVo();
        stockVo.setStockQty(stockQty);
        return caculateSuppliyTime(resultVo, stockVo);
    }

	/**
	 * 计算成型可供时长，计算逻辑：<br/>
	 * 从成型1班开始顺序往后遍历每一班的计划量，只要库存量足够支持该班的成型计划量，则排产则每一班顺延8个小时，<br/>
	 * 直导库存不足，则该班的时长为（预计库存*8/该班计划）<br/>
	 * 最多5班40小时
	 * 
	 * @param resultVo 排产结果
	 * @param stockVo  库存信息
	 * @return
	 */
	@Override
	public BigDecimal caculateSuppliyTime(Cd15ScheduleResultVo resultVo, Cd15StockVo stockVo) {
		// 16点半部件库存量
		BigDecimal stockQty = Optional.ofNullable(stockVo).map(Cd15StockVo::getStockQty).orElse(BigDecimal.ZERO);
		// 库存消耗量
		BigDecimal stockConsume = BigDecimal.ZERO;
		BigDecimal supplyTime = BigDecimal.ZERO;
		// 剩余库存，不足以支持8个小时的库存量
		Double remainStock = 0D;

		out: {
//			Double class1Plan = Optional.ofNullable(resultVo.getCxClass1Plan()).orElse(0d);
//			if (class1Plan <= stockQty.subtract(stockConsume).doubleValue()) {
//				// 比较剩余库存与计划量，库存较大说明可以支持本班完成生产，因此可供时长至少能支持8个小时
//				stockConsume = stockConsume.add(new BigDecimal(class1Plan));
//				supplyTime = SUPPLY_TIME_PARAM;
//			} else {
//				remainStock = class1Plan;
//				break out;
//			}

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
	 * 获取排产日的16点半部件库存
	 * 
	 * @param scheduleDate 排产日期
	 * @Param stockLossRate 库存损耗率
	 * @Param isProductionStage 仅对投产阶段规格排产
	 * @return key：帘布编号，value：库存量
	 */
	@Override
	public Map<String, Cd15StockVo> getStockMap(Date scheduleDate, BigDecimal stockLossRate,
			boolean isProductionStage) {
		return cd15EngineStockMapper.selectCd15Stock(scheduleDate, stockLossRate, isProductionStage).stream()
				.collect(Collectors.toMap(Cd15StockVo::getBeltCode, Function.identity(), (v1, v2) -> v1));
	}
	
    /**
     * 加载当天库存
     *
     * @param scheduleDate
     * @return
     */
    private Map<String, BigDecimal> loadCd15Stock(Date scheduleDate) {
        return cd15EngineStockMapper.selectCd15StockQty(scheduleDate).stream()
                .filter(v -> StringUtils.isNotEmpty(v.getBeltCode()))
                .collect(Collectors.toMap(Cd15StockVo::getBeltCode, Cd15StockVo::getStockQty));
    }

    /**
     * 加载上一天的早班计划
     *
     * @param scheduleDate
     * @return
     */
    private Map<String, Double> loadLastDayMidPlan(Date scheduleDate) {
        // sql已将一号二号钢带分开统计并统一放置到1号钢带的相关栏位中
        return cd15EngineStockMapper.listLastDayMidPlan(scheduleDate).stream()
                .filter(v -> StringUtils.isNotEmpty(v.getSteelStripCode1()))
                .collect(Collectors.groupingBy(Cd15ScheduleResultVo::getSteelStripCode1, Collectors.summingDouble(Cd15ScheduleResultVo::getNightPlanQty1)));
    }
    
    /**
     * 
     * 处理排产计划量，防止出现二次投产
     * 
     * @param resultVo        排产信息
     * @param steelStripCode  本次计算的钢带
     * @param stockQty        库存量，自动排程时需要传入，手工平衡时可以放空
     * @param lastMidPlanQty  早班计划量
     * @param totalConsumeQty 剩余量
     * @param isSeparate      是否分开计算
     * @param paramsMap       排产参数
     */
    public void handleSecondaryProduct(Cd15ScheduleResultVo resultVo, String steelStripCode, Double stockQty,
            Double lastMidPlanQty, Double totalConsumeQty, boolean isSeparate, Map<String, String> paramsMap) {
        if (StringUtils.isEmpty(steelStripCode)) {
            return;
        }
	    BigDecimal productStockHour = new BigDecimal(paramsMap.getOrDefault(EngineConstants.PRODUCT_STOCK_HOUR, DEFAULT_PRODUCT_STOCK_HOUR));
        BigDecimal crimpLength = Optional.ofNullable(paramsMap.get(EngineConstants.CRIMP_LENGTH)).map(p -> new BigDecimal(p)).orElse(DEFAULT_CRIMP_LENGTH);// 卷曲长度
        BigDecimal oneRollNum = new BigDecimal(paramsMap.getOrDefault(EngineConstants.ONE_ROLL_NUM, DEFAULT_ONE_ROLL_NUM)); // 一次生产卷数
        List<String> nightSpecList = Arrays.asList(paramsMap.getOrDefault(EngineConstants.NIGHT_SPEC, "").split(",")); // 固定夜班规格（大卷规格）
        BigDecimal oneProductQty = oneRollNum.multiply(crimpLength); // 最低生产数 = 一次生产卷数 * 卷长
        boolean isFirstSteelStrip = Objects.equals(steelStripCode, resultVo.getSteelStripCode1()); // 是否计算1#钢带
        resultVo.setIsNightSpec(nightSpecList.contains(String.valueOf(resultVo.getBigRollCode()))); // 固定夜班规格标记初始化，大卷规格设定为固定的需要如此设置
        
        double supplyClass = productStockHour.divide(BigDecimalUtils.HOUR24, 2, RoundingMode.HALF_UP).doubleValue(); // 预生产库存天数

        // 每个早班计算交接班库存 = 上一天交接班库存 + 上一天成型计划量总量 - 上一天成型两个班的消耗量
        // 交接班库存要按生产几个小时库存算，例如预生产12小时库存，则交接班库存要 > 当天成型需求量 / 2，最多超过一车
        // 上一天成型计划总量原则上平均分配给两个班，但是早班的计划量要 > 上一天成型两个班的需求量 - 上一天交接班库存
        double cxPlanQty1 = BigDecimalUtil.add(resultVo.getCxClass1Plan(), resultVo.getCxClass2Plan());// 第一天成型两个班消耗量
        double cxPlanQty2 = BigDecimalUtil.add(resultVo.getCxClass3Plan(), resultVo.getCxClass4Plan());// 第二天成型两个班消耗量
        double cxPlanQty3 = cxPlanQty2;// 第三天成型两个班消耗量（成型没有，如果未收尾暂时先预计与第二天一样）
        
        // 计算第一天相关数值
        double classStock1 = stockQty; // 第一天交接班库存，初始为当天库存
        double classStock2 = BigDecimalUtil.roundDown(BigDecimalUtil.mul(cxPlanQty2, supplyClass), 0); // 第二天交接班库存，第二天成型早班的消耗量 * 预生产天数
//        if (lastMidPlanQty > 0) { // 早班有计划则，交接班库存可以只不要超过早班的需求量
//            classStock2 = BigDecimalUtils.least(classStock2, resultVo.getCxClass3Plan()).doubleValue(); // 交接班库存控制最多是明天早班的需求量
//        }
        // 计算第一天相关数值
        double planQty1 = BigDecimalUtil.add(BigDecimalUtil.sub(classStock2, classStock1), cxPlanQty1);// 第一天成型计划量 = 第二天交接班库存 - 第一天交接班库存 + 第一天成型两个班的消耗量
        planQty1 = planQty1 > 0 ? planQty1 : 0D; // 上一天交接班库存过多会计算成负数，需要处理成0
        double class1PlanQty1 = lastMidPlanQty;// 第一天早班计划 = 前日早班计划
        double class2PlanQty1 = BigDecimalUtil.sub(planQty1, class1PlanQty1);// 第一天夜班计划 = 等于第一天成型计划 - 第一天早班计划
        if (resultVo.getIsNightSpec()) { // 如果是固定夜班的规格，则必须先满足隔天早上的需求量
            // 早班库存 + 早班计划 + 夜班计划 - 第一天需求量 = 第二天早班可用的量（即第二天早班需求量）
            // => 夜班计划 = 第一天需求量 + 第二天早班需求量 - （早班库存 + 早班计划）
            double newClass2PlanQty1 = BigDecimalUtil.sub(BigDecimalUtil.add(cxPlanQty1, resultVo.getCxClass3Plan()), BigDecimalUtil.add(classStock1, class1PlanQty1));
            if (resultVo.getClass4Sort() <= 1) { // 如果成型次日夜班第一顺位需求，则要次日夜班的计划都要备足
                newClass2PlanQty1 = BigDecimalUtil.sub(BigDecimalUtil.add(cxPlanQty1, cxPlanQty2), BigDecimalUtil.add(classStock1, class1PlanQty1));
            }
            class2PlanQty1 = BigDecimalUtils.greatest(newClass2PlanQty1, class2PlanQty1).doubleValue(); // 取两者较大值
        }
        // 如果库存 + 早班计划 >= 当天需求量，隔天只差一点点（不到一卷），则夜班先不做，因为占用工装太多
//        if (BigDecimalUtil.add(classStock1, class1PlanQty1) >= cxPlanQty1 && BigDecimalUtils.valueOf(class2PlanQty1).compareTo(crimpLength) <= 0) {
//            class2PlanQty1 = 0D;
//        }
        class2PlanQty1 = this.limitProductQty(class2PlanQty1, oneProductQty, isSeparate); // 控制生产量不要小于最低生产量
        double newClass2PlanQty1 = this.planQtyRounding(resultVo, isFirstSteelStrip, class2PlanQty1, crimpLength, totalConsumeQty,
                OpenMachineClassEnums.CLASS_TWO, classStock1); // 整车取整
        
        double dayPlanQty = newClass2PlanQty1; // 夜班计划
        if (!isSeparate) { // 如果不需要分开计算，1号2号计划一致
            resultVo.setDayPlanQty1(dayPlanQty);
            resultVo.setDayPlanQty2(dayPlanQty);
        } else if (isFirstSteelStrip) {
            resultVo.setDayPlanQty1(dayPlanQty);
        } else {
//            if (resultVo.getDayPlanQty1() == 0 && dayPlanQty > 0) { // 如果1号夜班没有但是2号夜班有，则把1号的早班提前到夜班
//                resultVo.setDayPlanQty1(resultVo.getNightPlanQty1());
//                resultVo.setNightPlanQty1(0D);
//            }
            resultVo.setDayPlanQty2(dayPlanQty);
        }
        // 根据排好的计划量重算相关数值
        planQty1 = BigDecimalUtil.add(class1PlanQty1, dayPlanQty); // 刷新第一天成型计划量
        classStock2 = BigDecimalUtil.sub(BigDecimalUtil.add(planQty1, classStock1), cxPlanQty1);// 刷新第二天交接班库存
        resultVo.setClassStock(classStock2); // 保存交接班库存，用于均衡计算
        resultVo.setSupplyDemandRatio(BigDecimalUtil.div(classStock2, cxPlanQty2, 4)); // 计算交接班库存供需比率，第二天交接班库存 / 成型第二天需求量，用于均衡计算
        
        // 计算第二天相关数值
        double classStock3 = BigDecimalUtil.roundDown(BigDecimalUtil.mul(cxPlanQty3, supplyClass), 0); // 第三天交接班库存，第三天成型两个班的消耗量 * 预生产天数
        double planQty2 = BigDecimalUtil.add(BigDecimalUtil.sub(classStock3, classStock2), cxPlanQty2);// 第二天成型计划量 = 第三天交接班库存 - 第二天交接班库存 + 第二天成型两个班的消耗量
        planQty2 = planQty2 > 0 ? planQty2 : 0D; // 上一天交接班库存过多会计算成负数，需要处理成0
        double class1PlanQty2;// 第二天早班计划
        double nightStock = BigDecimalUtil.mul(resultVo.getCxClass4Plan(), supplyClass);
        double dayLackPlanQty = BigDecimalUtil.add(BigDecimalUtil.sub(resultVo.getCxClass3Plan(), classStock2), nightStock); // 早班先早班库存缺口，并备夜班的需求*供应班数
        if (resultVo.getIsNightSpec()) { // 固定夜班规格，早班不排产
            dayLackPlanQty = 0D;
        } else if (dayLackPlanQty <= 0) { // 如果早班没有缺口，则补下午计划量
            dayLackPlanQty = BigDecimalUtil.sub(cxPlanQty2, classStock2);
        }
//        dayLackPlanQty = limitProductQty(dayLackPlanQty, oneProductQty); // 控制生产量不要小于最低生产量
        class1PlanQty2 = this.planQtyRounding(resultVo, isFirstSteelStrip, dayLackPlanQty, crimpLength, totalConsumeQty,
                OpenMachineClassEnums.CLASS_THREE, classStock1); // 整车取整
        double nightPlanQty = class1PlanQty2; // 早班计划
        if (!isSeparate) { // 如果不需要分开计算，1号2号计划一致
            resultVo.setNightPlanQty1(nightPlanQty);
            resultVo.setNightPlanQty2(nightPlanQty);
        } else if (isFirstSteelStrip) {
            resultVo.setNightPlanQty1(nightPlanQty);
        } else { // 计算2号时，要与1号的班次匹配上，不要出现1号在夜班，2号在早班的情况，以班次较早的为准
//            if (resultVo.getDayPlanQty1() > 0 && dayPlanQty == 0) { // 如果1号夜班有但是2号早没有，则把2号的早班提前到夜班
//                dayPlanQty = nightPlanQty;
//                nightPlanQty = 0;
//                resultVo.setDayPlanQty2(nightPlanQty);
//            } else if (resultVo.getDayPlanQty1() == 0 && dayPlanQty > 0) { // 如果1号夜班没有但是2号夜班有，则把1号的早班奇谭到夜班
//                resultVo.setDayPlanQty1(resultVo.getNightPlanQty1());
//                resultVo.setNightPlanQty1(0D);
//            }
            resultVo.setNightPlanQty2(nightPlanQty);
        }
        double class2PlanQty2 = BigDecimalUtil.sub(planQty2, class1PlanQty2);// 第二天夜班计划 = 等于第二天成型计划 - 第二天早班计划
//        class2PlanQty2 = limitProductQty(class2PlanQty2, oneProductQty); // 控制生产量不要小于最低生产量
        double nextDayPlanQty = this.planQtyRounding(resultVo, isFirstSteelStrip, class2PlanQty2, crimpLength, totalConsumeQty,
                OpenMachineClassEnums.CLASS_FOUR, classStock1); // 次日夜班计划 = 第二天夜班计划整车取整
//        planQty2 = BigDecimalUtil.add(nightPlanQty, nextDayPlanQty); // 刷新第二天计划量
//        classStock3 = BigDecimalUtil.sub(BigDecimalUtil.add(planQty2, classStock2), cxPlanQty2);// 刷新第三天交接班库存
//        if (classStock3 > cxPlanQty2 && nextDayPlanQty >= crimpLength.doubleValue()) { // 如果交接班库存比第二天需求量还多，则从次日夜班减一卷
//            nextDayPlanQty = BigDecimalUtil.sub(nextDayPlanQty, crimpLength.doubleValue());
//        }
        if (!isSeparate) { // 如果不需要分开计算，1号2号计划一致
            resultVo.setNextDayPlanQty(nextDayPlanQty);
            resultVo.setNextDayPlanQty2(nextDayPlanQty);
        } else if (isFirstSteelStrip) {
            resultVo.setNextDayPlanQty(nextDayPlanQty);
        } else { // 计算2号时，要与1号的班次匹配上，不要出现1号在早班，2号在次日班的情况，以班次较早的为准
//            if (resultVo.getNightPlanQty1() > 0 && nightPlanQty == 0) { // 1号早班有、2号没有计划，将本班计划提前到早班
//                resultVo.setNightPlanQty2(nextDayPlanQty);
//            } else {
//                if (resultVo.getDayPlanQty1() == 0 && nightPlanQty > 0) {
//                    resultVo.setDayPlanQty1(resultVo.getNextDayPlanQty());
//                }
//                resultVo.setNextDayPlanQty2(nextDayPlanQty);
//            }
            resultVo.setNextDayPlanQty2(nextDayPlanQty);
        }
        
        // 其他属性赋值
        resultVo.getParams().put(EngineConstants.CRIMP_LENGTH, crimpLength);
	}

    /**
     * 控制生产量不要小于最低生产量
     * 
     * @param planQty       排产量
     * @param oneProductQty 最少排产量
     * @param isSeparate    是否1号2号分开算，分开则不需要限制最低生产量
     * @return
     */
    private double limitProductQty(double planQty, BigDecimal oneProductQty, boolean isSeparate) {
        if (isSeparate) {
            return planQty;
        }
        if (planQty > 0 && planQty < oneProductQty.doubleValue()) { // 如果有生产量，则不要小于最低生产量
            planQty = oneProductQty.doubleValue();
        }
        return planQty;
    }
	
	/**
     * 计划量取整
     *
     * @param scheduleVo      排产记录
     * @param planQty         原计划量
     * @param toolCapacity    一车可以放的胎面量
     * @param totalConsumeQty 总需期间量，用于判断收尾规格是否超量
     * @param isCloseOutSpec  是否收尾
     * @param classNum        当前班次，从前日早班开始
     * @return
     */
    private double planQtyRounding(Cd15ScheduleResultVo scheduleVo, boolean isFirstSteelStrip, double planQty, BigDecimal toolCapacity,
                                   Double totalConsumeQty, OpenMachineClassEnums classNum, Double stockQty) {
        if (planQty <= 0D) { // 不排的情况直接返回0即可
            return 0D;
        }
        double roudingPlanQty = BigDecimalUtils.valueOf(planQty).divide(toolCapacity, 0, RoundingMode.CEILING)
                .multiply(toolCapacity).doubleValue(); // 取整车
        if (classNum == null) {
            return roudingPlanQty;
        }
        double lastPlanCumulative = this.getCd15ClassPlanCumulative(scheduleVo, isFirstSteelStrip, classNum.getPreviousClass()); // 到上个班次班次班的累计已排计划量
        double newPlanQty = BigDecimalUtil.add(lastPlanCumulative, roudingPlanQty, stockQty); // 库存+已排计划+本班计划
        Double result = roudingPlanQty;
        // 如果库存+计划已经超过总需求量，则本班的计划量要限制住不允许超量
        if (newPlanQty > totalConsumeQty) {
            Double increaseMidPlanQty = BigDecimalUtil.sub(newPlanQty, totalConsumeQty);
            result = BigDecimalUtil.sub(roudingPlanQty, increaseMidPlanQty);
            result = Math.max(result, 0);
            // 收尾时，数量多出不足0.1卷则忽略掉
            if (result < toolCapacity.divide(BigDecimal.TEN, 2).doubleValue()) {
                result = 0D;
            }
        }
        scheduleVo.setCloseOutSpecFlag(newPlanQty >= totalConsumeQty? ApsConstant.STATUS_ENABLE: ApsConstant.STATUS_DISABLE);
        return result;
    }



    /**
     * 获取各班计划量的累计值（从前日早班开始）
     *
     * @param scheduleVo
     * @param isFirstSteelStrip 是否计算1#钢带
     * @param classNum
     * @return
     */
    private Double getCd15ClassPlanCumulative(Cd15ScheduleResultVo scheduleVo, boolean isFirstSteelStrip, OpenMachineClassEnums classNum) {
        Double planQty = 0D;
        if (classNum == null) {
            return planQty;
        }
        planQty = BigDecimalUtil.add(planQty, isFirstSteelStrip? scheduleVo.getLastMidPlanQty1(): scheduleVo.getLastMidPlanQty2());
        if (classNum == OpenMachineClassEnums.CLASS_ONE) {
            return planQty;
        }
        planQty = BigDecimalUtil.add(planQty, isFirstSteelStrip? scheduleVo.getDayPlanQty1(): scheduleVo.getDayPlanQty2());
        if (classNum == OpenMachineClassEnums.CLASS_TWO) {
            return planQty;
        }
        planQty = BigDecimalUtil.add(planQty, isFirstSteelStrip? scheduleVo.getNightPlanQty1(): scheduleVo.getNightPlanQty2());
        if (classNum == OpenMachineClassEnums.CLASS_THREE) {
            return planQty;
        }
        return BigDecimalUtil.add(planQty, isFirstSteelStrip? scheduleVo.getNextDayPlanQty(): scheduleVo.getNextDayPlanQty2());
    }

    /**
     * 获取各班需求量的累计值（从前日早班开始）
     *
     * @param scheduleVo
     * @param classNum
     * @return
     */
    private Double getCxClassPlanCumulative(Cd15ScheduleResultVo scheduleVo, OpenMachineClassEnums classNum) {
        Double cxClass1Plan = (scheduleVo.getCxClass1Plan() == null ? 0D : scheduleVo.getCxClass1Plan());  //对应成型前日早班的计划量
        Double cxClass2Plan = (scheduleVo.getCxClass2Plan() == null ? 0D : scheduleVo.getCxClass2Plan());  //对应成型夜班的计划量
        Double cxClass3Plan = (scheduleVo.getCxClass3Plan() == null ? 0D : scheduleVo.getCxClass3Plan());  //对应成型早班的计划量
        Double cxClass4Plan = (scheduleVo.getCxClass4Plan() == null ? 0D : scheduleVo.getCxClass4Plan());  //对应成型次日夜班的计划量
        Double cxClass5Plan = (scheduleVo.getCxClass5Plan() == null ? 0D : scheduleVo.getCxClass5Plan());  //对应成型次日早班的计划量
        Double planQty = 0D;
        if (classNum == null) {
            return planQty;
        }
        planQty = BigDecimalUtil.add(planQty, cxClass1Plan);
        if (classNum == OpenMachineClassEnums.CLASS_ONE) {
            return planQty;
        }
        planQty = BigDecimalUtil.add(planQty, cxClass2Plan);
        if (classNum == OpenMachineClassEnums.CLASS_TWO) {
            return planQty;
        }
        planQty = BigDecimalUtil.add(planQty, cxClass3Plan);
        if (classNum == OpenMachineClassEnums.CLASS_THREE) {
            return planQty;
        }
        planQty = BigDecimalUtil.add(planQty, cxClass4Plan);
        if (classNum == OpenMachineClassEnums.CLASS_FOUR) {
            return planQty;
        }
        return BigDecimalUtil.add(planQty, cxClass5Plan);
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
	private BigDecimal getLinsideStock(String machineCode, List<Cd15ScheduleResultVo> scheduleList,
			Map<String, List<Cd15LineSideStock>> lineSideStockMap, Map<Long, String> machineMap, BigDecimal planQty,
			boolean isCloseOut) {
		if (StringUtils.isEmpty(machineCode)) {
			return BigDecimal.ZERO;
		}
		Cd15ScheduleResultVo schedule = CollectionUtil.firstElement(scheduleList);
		String bigRollCode = schedule.getBigRollCode(); // 对应的大卷编号
		boolean isUniqueMaterial = scheduleList.stream().filter(s -> s.getTotalPlanQty().compareTo(BigDecimal.ZERO) > 0)
				.map(s -> GenerageMapKeyUtils.createMapKey(s.getSteelStripCode1(), s.getSteelStripCode2())).distinct()
				.collect(Collectors.counting()) == 1; // 判断是否大卷唯一规格，1号 + 2号
		boolean hasCxFiveClass = scheduleList.stream().anyMatch(s -> s.getTotalPlanQty().compareTo(BigDecimal.ZERO) > 0
				&& Optional.ofNullable(s.getCxClass5Plan()).orElse(0D) > 0); // 是否有成型第五个班
		List<Cd15LineSideStock> lineSideStockList = lineSideStockMap
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
			Cd15LineSideStock stock = lineSideStockList.get(j);
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
		List<Cd15LineSideStock> resultSideStock;
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
		return resultSideStock.stream().map(Cd15LineSideStock::getStockNum).reduce(BigDecimal.ZERO, BigDecimal::add);
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
	private void insertCalculateLog(String oldScheduleResult, Cd15ScheduleResultVo scheduleVo, double lossRate) {
		String logDetail = logSplit("开始计算中班和夜班计划量", "计算前排程数据：" + oldScheduleResult,
				"根据库存重新计算中班计划量dayPlanQty：如果 原中班计划量>库存，则 中班计划量 = 原中班计划量 -库存；否则中班计划量 = 0",
				"根据库存重新计算夜班计划量nightPlanQty：如果 原中班计划量>库存，则 晚班计划量=0 ；否则晚班计划量 = （原中班计划量+原晚班计划量-库存）",
				"获得损耗率（从损耗率表获取对应的损耗率，获取顺序：机台+物料编号 > 胎面代码 > 机台 >工序参数配置），耗损率：" + lossRate,
				"如果中班计划量>0，那么中班计划量=中班计划量+夜班计划量，夜班计划量=0（为了让相同的胶在同一个班生产，而且又不能延误生产）",
				"计划量计算好后的排程数据：" + toJSONString(scheduleVo));
		autoScheduleLogService.insertCd15ScheduleLog(scheduleVo.getBatchNo(), scheduleVo.getOrderNo(), "3.2、计算各班计划量",
				logDetail);
	}
}
