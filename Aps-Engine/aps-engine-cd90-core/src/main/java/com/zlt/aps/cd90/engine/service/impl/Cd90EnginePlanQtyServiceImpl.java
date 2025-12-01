package com.zlt.aps.cd90.engine.service.impl;

import com.zlt.aps.cd90.api.domain.entity.Cd90CurlLength;
import com.zlt.aps.cd90.api.domain.entity.Cd90LineSideStock;
import com.zlt.aps.cd90.api.domain.entity.Cd90MachineInfo;
import com.zlt.aps.cd90.engine.mapper.Cd90EngineBigRollMapper;
import com.zlt.aps.cd90.engine.mapper.Cd90EngineStockMapper;
import com.zlt.aps.cd90.engine.service.Cd90EngineLossService;
import com.zlt.aps.cd90.engine.service.Cd90EnginePlanQtyService;
import com.zlt.aps.cd90.engine.vo.Cd90ParamsVo;
import com.zlt.aps.cd90.engine.vo.Cd90ScheduleResultVo;
import com.zlt.aps.cd90.engine.vo.Cd90StockConsumeVo;
import com.zlt.aps.cd90.engine.vo.Cd90StockVo;
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
import java.util.Map.Entry;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.alibaba.fastjson.JSON.toJSONString;
import static com.zlt.aps.common.core.utils.ApsCommonUtil.getDouble;
import static com.zlt.aps.common.core.utils.ApsCommonUtil.getDoubleOrDefault;
import static com.zlt.aps.common.core.utils.ApsCommonUtil.logSplit;

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
	private static final BigDecimal SUPPLY_TIME_PARAM = new BigDecimal("12");
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
    private final static String DEFAULT_TOOL_ROLL_NUM = "4"; // 工装包含大卷数
    private final static String DEFAULT_LARGE_DEMAND = "28"; // 需求量超过该值的算大需求量规格
    private final static String DEFAULT_LARGE_DEMAND_REDUCE = "12"; // 大需求量规格不生产的卷数
	private final static String DEFAULT_PRODUCT_STOCK_HOUR = "12"; // 保库存供应时长
    private final static String DEFAULT_SUPPLY_SPEC_CONCENTRATE = "3"; // 供应成型规格数集中生产阈值
    private final static String DEFAULT_SUPPLY_SPEC_DISTRIBUTE = "3"; // 供应成型规格数分散生产阈值
	private final static BigDecimal HOUR24 = new BigDecimal("24"); // 24小时
	private final static Double MIN_PLAN_QTY = 10D; // 最小排产量限制

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
										 BigDecimal minRoundRollNum, Map<String, String> paramsMap) {
		// 批次号
		String batchNo = CollectionUtil.firstElement(scheduleList).getBatchNo();
		// 获取库存量
		// 计算公式： (库存量 - 不良数 + 修正数) - (前日三班计划量 - 12点成型完成量) * 单耗
//		Map<String, Cd90StockVo> planStockMap = this.getStockMap(scheduleDate, stockLossRate, isProductionStage);
        BigDecimal largeDemand = new BigDecimal(paramsMap.getOrDefault(EngineConstants.LARGE_DEMAND, DEFAULT_LARGE_DEMAND)); // 大需求量阈值
        BigDecimal largeDemandReduce = new BigDecimal(paramsMap.getOrDefault(EngineConstants.LARGE_DEMAND_REDUCE, DEFAULT_LARGE_DEMAND_REDUCE));
        BigDecimal toolRollNum = new BigDecimal(paramsMap.getOrDefault(EngineConstants.TOOL_ROLL_NUM, DEFAULT_TOOL_ROLL_NUM)); // 工装包含大卷数量
        BigDecimal supplySpecConcentrate = new BigDecimal(paramsMap.getOrDefault(EngineConstants.SUPPLY_SPEC_CONCENTRATE, DEFAULT_SUPPLY_SPEC_CONCENTRATE)); // 供应成型规格数集中生产阈值
        BigDecimal supplySpecDistribute = new BigDecimal(paramsMap.getOrDefault(EngineConstants.SUPPLY_SPEC_DISTRIBUTE, DEFAULT_SUPPLY_SPEC_DISTRIBUTE)); // 供应成型规格数分散生产阈值
        List<String> nightSpecList = Arrays.asList(paramsMap.getOrDefault(EngineConstants.NIGHT_SPEC, "").split(",")); // 固定夜班规格（大卷规格）
        BigDecimal shareThreshold = BigDecimalUtils.valueOf(paramsMap.get(EngineConstants.EQUAL_SHARE_THRESHOLD)); // 均分阈值
        BigDecimal toolCapacity = crimpLength.multiply(toolRollNum); // 一个工装车包含的总米数
//        List<String> closeOutSpecList = cd90EngineStockMapper.listCloseOutSpec(scheduleDate, isProductionStage); // 加载收尾规格
        BigDecimal productStockHour = new BigDecimal(paramsMap.getOrDefault(EngineConstants.PRODUCT_STOCK_HOUR, DEFAULT_PRODUCT_STOCK_HOUR));
        double supplyClass = productStockHour.divide(HOUR24, 2, RoundingMode.HALF_UP).doubleValue(); // 预生产库存天数
        
		// 获取损耗率设定
		Map<String, Double> lossRateMap = cd90EngineLossService.getLossRateMap();
		// 默认损耗率类型转换
		Double defaultLossRateNum = getDouble(defaultLossRate);
		// 计算库存相关信息：16点半部件库存、可用时长，并根据库存重算计划量
		// 加载库存
		Map<String, BigDecimal> stockMap = new HashMap<>();
        Map<Integer, Map<String, BigDecimal>> layersStockMap = this.loadCd90Stock(scheduleDate); // 加载按层分开的库存数据
		// 加载昨日早班计划
        Map<String, Double> lastDayMidPlanMap = new HashMap<>();
        Map<Integer, Map<String, Double>> lastDayMidLayersPlanMap = this.loadLastDayMidPlan(scheduleDate);
		for (Cd90ScheduleResultVo resultVo : scheduleList) {
			// 计算前的排程数据json字符串，用于日志记录
			/*String oldScheduleResult = toJSONString(resultVo);
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
			BigDecimal newNightPlanQty = new BigDecimal(resultVo.getNightPlanQty());*/
			String clothCode = resultVo.getClothCode();
			stockMap = layersStockMap.getOrDefault(resultVo.getLayers(), stockMap); // 根据胎体布层数取出对应的库存数据
			lastDayMidPlanMap = lastDayMidLayersPlanMap.getOrDefault(resultVo.getLayers(), lastDayMidPlanMap); // 根据胎体布赠书取出对应的昨日计划量数据
//			boolean isCloseOutSpec = closeOutSpecList.contains(clothCode); // 是否收尾规格，根据收尾列表判断
//			resultVo.setCloseOutSpecFlag(isCloseOutSpec? ApsConstant.STATUS_ENABLE: ApsConstant.STATUS_DISABLE); // 重新给收尾标记赋值
            resultVo.setIsNightSpec(nightSpecList.contains(String.valueOf(resultVo.getBigRollCode()))); // 固定夜班规格标记初始化，大卷规格设定为固定的需要如此设置
			String oldScheduleResult = toJSONString(resultVo); // 没计算前的排程数据json字符串（日志使用）
			BigDecimal stock = stockMap.get(clothCode);
			resultVo.setStockQty(stock == null ? 0D : stock.doubleValue());
			Double stockQty = resultVo.getStockQty(); // 库存
			Double lastDayQty = lastDayMidPlanMap.getOrDefault(clothCode, 0D);
			resultVo.setLastMidPlanQty(lastDayQty);
			Double lastMidPlanQty = resultVo.getLastMidPlanQty(); // 前日白班计划
//			Double totalConsumeQty = this.getCxClassPlanCumulative(resultVo, OpenMachineClassEnums.CLASS_FOUR); // 总需求量，前四个班
//            totalConsumeQty = BigDecimalUtils.greatest(totalConsumeQty, resultVo.getSurplusQty()).doubleValue();
			Double totalConsumeQty = resultVo.getSurplusQty(); // 剩余量
	        resultVo.setCloseOutSpecFlag(ApsConstant.STATUS_ENABLE); // 收尾标记默认非收尾


			// 每个早班计算交接班库存 = 上一天交接班库存 + 上一天成型计划量总量 - 上一天成型两个班的消耗量
	        // 交接班库存要按生产几个小时库存算，例如预生产12小时库存，则交接班库存要 > 当天成型需求量 / 2，最多超过一车
	        // 上一天成型计划总量原则上平均分配给两个班，但是早班的计划量要 > 上一天成型两个班的需求量 - 上一天交接班库存
	        double cxPlanQty1 = BigDecimalUtil.add(resultVo.getCxClass1Plan(), resultVo.getCxClass2Plan());// 第一天成型两个班消耗量
	        double cxPlanQty2 = BigDecimalUtil.add(resultVo.getCxClass3Plan(), resultVo.getCxClass4Plan());// 第二天成型两个班消耗量
	        double cxPlanQty3 = cxPlanQty2;// 第三天成型两个班消耗量（成型没有，如果未收尾暂时先预计与第二天一样）
	        double classStock1 = stockQty; // 第一天交接班库存，初始为当天库存
	        double classStock2 = BigDecimalUtil.roundDown(BigDecimalUtil.mul(cxPlanQty2, supplyClass), 0); // 第二天交接班库存，第二天成型两个班的消耗量 * 预生产天数
	        if (this.isLargeDemandSpec(cxPlanQty2, crimpLength, largeDemand, toolCapacity)) { // 如果是大需求量规格，则满需求减扣减数
//	            double newClassStock2 = BigDecimalUtils.qtySub(cxPlanQty2, largeDemandReduce.multiply(crimpLength));
	            BigDecimal newClassStock2 = BigDecimalUtils.half(cxPlanQty2);
	            classStock2 = BigDecimalUtils.least(classStock2, newClassStock2).doubleValue(); // 取计算前后的较小值
	        }
	        // 计算第一天相关数值
	        double planQty1 = BigDecimalUtil.add(BigDecimalUtil.sub(classStock2, classStock1), cxPlanQty1);// 第一天成型计划量 = 第二天交接班库存 - 第一天交接班库存 + 第一天成型两个班的消耗量
	        planQty1 = planQty1 > 0 ? planQty1 : 0D; // 上一天交接班库存过多会计算成负数，需要处理成0
	        double class1PlanQty1 = lastMidPlanQty;// 第一天早班计划 = 前日早班计划
	        double day1lackPlanQty = BigDecimalUtil.sub(cxPlanQty1, BigDecimalUtil.add(classStock1, class1PlanQty1)); // 第一天库存缺口
	        double class2PlanQty1 = BigDecimalUtil.sub(planQty1, class1PlanQty1);// 第一天夜班计划 = 等于第一天成型计划 - 第一天早班计划
	        if (resultVo.getIsNightSpec()) { // 如果是固定夜班的规格，则必须先满足隔天早上的需求量
	            // 早班库存 + 早班计划 + 夜班计划 - 第一天需求量 = 第二天早班可用的量（即第二天早班需求量）
	            // => 夜班计划 = 第一天需求量 + 第二天早班需求量 - （早班库存 + 早班计划）
	            double newClass2PlanQty1 = BigDecimalUtil.sub(BigDecimalUtil.add(cxPlanQty1, resultVo.getCxClass3Plan()), BigDecimalUtil.add(classStock1, class1PlanQty1));
	            class2PlanQty1 = BigDecimalUtils.greatest(newClass2PlanQty1, class2PlanQty1).doubleValue(); // 取两者较大值
	        } else if (lastDayQty > 0 && day1lackPlanQty <= 0 && BigDecimalUtils.valueOf(class2PlanQty1).compareTo(toolCapacity) <= 0) {
	            class2PlanQty1 = 0D; // 非夜班规格，早班有排计划，且如果当前库存不缺，隔天只差一点点（不到一个工装），则夜班先不做，因为占用工装太多
	        }
	        if (lastDayQty > 0 && class2PlanQty1 > 0) {
	            // 如果早夜班都有排计划，则尝试将夜班补够均分阈值，尝试补够“两天需求量减库存的差值”与“均分阈值”的较小值
	            BigDecimal lackPlanQtyAll = BigDecimalUtils.sub(BigDecimalUtils.add(cxPlanQty1, cxPlanQty2), classStock1);
	            BigDecimal day1PlanQty = BigDecimalUtils.least(shareThreshold, lackPlanQtyAll);
	            BigDecimal day1PlanQtyDiff = day1PlanQty.subtract(BigDecimalUtils.add(lastMidPlanQty, class2PlanQty1));
	            if (day1PlanQtyDiff.compareTo(BigDecimal.ZERO) > 0) {
	                class2PlanQty1 = BigDecimalUtil.add(class2PlanQty1, day1PlanQtyDiff.doubleValue());
	            }
            }
	        double newClass2PlanQty1 = this.planQtyRounding(resultVo, class2PlanQty1, toolCapacity, totalConsumeQty, OpenMachineClassEnums.CLASS_TWO); // 整车取整
	        
	        double dayPlanQty = newClass2PlanQty1; // 夜班计划
	        resultVo.setDayPlanQty(dayPlanQty);
	        // 根据排好的计划量重算相关数值
	        planQty1 = BigDecimalUtil.add(class1PlanQty1, dayPlanQty); // 刷新第一天成型计划量
	        classStock2 = BigDecimalUtil.sub(BigDecimalUtil.add(planQty1, classStock1), cxPlanQty1);// 刷新第二天交接班库存
	        resultVo.setClassStock(classStock2); // 保存交接班库存，用于均衡计算
	        resultVo.setSupplyDemandRatio(BigDecimalUtil.div(classStock2, cxPlanQty2, 4)); // 计算交接班库存供需比率，第二天交接班库存 / 成型第二天需求量，用于均衡计算

	        // 计算第二天相关数值
	        double classStock3 = BigDecimalUtil.roundDown(BigDecimalUtil.mul(cxPlanQty3, supplyClass), 0); // 第三天交接班库存，第三天成型两个班的消耗量 * 预生产天数
	        boolean isLargeDemandSpec = this.isLargeDemandSpec(cxPlanQty3, crimpLength, largeDemand, toolCapacity);
	        if (isLargeDemandSpec) { // 如果是大需求量规格，则只需要备特定量的交接班库存
//                double newClassStock3 = BigDecimalUtils.qtySub(cxPlanQty3, largeDemandReduce.multiply(crimpLength)); // 需求量减10卷
	            BigDecimal newClassStock3 = BigDecimalUtils.half(cxPlanQty3); // 需求量的一半
	            classStock3 = BigDecimalUtils.least(classStock3, newClassStock3).doubleValue(); // 计算后的新库存更大，则保留原库存
	        }
	        double planQty2 = BigDecimalUtil.add(BigDecimalUtil.sub(classStock3, classStock2), cxPlanQty2);// 第二天成型计划量 = 第三天交接班库存 - 第二天交接班库存 + 第二天成型两个班的消耗量
	        planQty2 = planQty2 > 0 ? planQty2 : 0D; // 上一天交接班库存过多会计算成负数，需要处理成0
	        double class1PlanQty2 = 0;// 第二天早班计划
            double lackPlanQty = BigDecimalUtil.sub(cxPlanQty2, classStock2); // 早班先补交接班库存缺口
	        double class3lackPlanQty = BigDecimalUtil.sub(resultVo.getCxClass3Plan(), classStock2); // 早班库存缺口
	        boolean isPassHalfDemand = this.isLargeDemandSpec(cxPlanQty3, crimpLength, largeDemand.divide(BigDecimalUtils.TWO), toolCapacity); // 判断需求量是否达到大规格阈值的一半
	        if (resultVo.getIsNightSpec()) { // 固定夜班规格，早班不排产
	            class1PlanQty2 = 0D;
	        } else if (class3lackPlanQty > 0 || lackPlanQty > 0) { // 早班或者整天需求有缺口，先补缺口
	            class1PlanQty2 = BigDecimalUtils.greatest(class3lackPlanQty, lackPlanQty).doubleValue();
	        } else if (resultVo.getSpecCount() >= supplySpecDistribute.doubleValue()) { // 供应规格数超过阈值的，中夜班对半分
	            class1PlanQty2 = BigDecimalUtils.div(planQty2, BigDecimalUtils.TWO, 0).doubleValue();
//            } else if (Math.abs(lackPlanQty) < toolCapacity.doubleValue()) { // 交接班库存比需求量多但是又不足一卷时，推迟到夜班
//                class1PlanQty2 = 0;
            } else if (!isPassHalfDemand && resultVo.getSpecCount() < supplySpecConcentrate.doubleValue()) { // 供应规格数没有达到阈值的，但不超过大需求规格一半的，统一放置到夜班处理
                resultVo.setIsNightSpec(true);
            }
	        class1PlanQty2 = this.planQtyRounding(resultVo, class1PlanQty2, toolCapacity, totalConsumeQty, OpenMachineClassEnums.CLASS_THREE); // 整车取整
	        double nightPlanQty = class1PlanQty2; // 早班计划
	        resultVo.setNightPlanQty(nightPlanQty);
	        double class2PlanQty2 = BigDecimalUtil.sub(planQty2, class1PlanQty2);// 第二天夜班计划 = 等于第二天成型计划 - 第二天早班计划
	        double nextDayPlanQty = this.planQtyRounding(resultVo, class2PlanQty2, toolCapacity, totalConsumeQty, OpenMachineClassEnums.CLASS_FOUR); // 次日夜班计划 = 第二天夜班计划整车取整
	        resultVo.setNextDayPlanQty(nextDayPlanQty);

			BigDecimal newDayPlanQty = BigDecimal.valueOf(resultVo.getDayPlanQty());
			BigDecimal newNightPlanQty = BigDecimal.valueOf(nightPlanQty);
			BigDecimal newNextDayPlanQty = BigDecimal.valueOf(nextDayPlanQty);
			// 获取损耗率
			Double lossRate = cd90EngineLossService.getLossRate(clothCode, resultVo.getMachineId(), lossRateMap, defaultLossRateNum);
			// 为弥补损耗的量，计划量需要填补损耗量，计算公式：新计划量 = 原计划量+原计划量*损耗率
			newDayPlanQty = newDayPlanQty.add(newDayPlanQty.multiply(BigDecimal.valueOf(lossRate)));
			newNightPlanQty = newNightPlanQty.add(newNightPlanQty.multiply(BigDecimal.valueOf(lossRate)));
			newNextDayPlanQty = newNextDayPlanQty.add(newNextDayPlanQty.multiply(BigDecimal.valueOf(lossRate)));

			// 重新赋值计划量给排产明细
			// 结果小数舍入方式调整，modify by 20211230
			resultVo.setDayPlanQty(newDayPlanQty.setScale(0, RoundingMode.UP).doubleValue());
			resultVo.setNightPlanQty(newNightPlanQty.setScale(0, RoundingMode.UP).doubleValue());
			resultVo.setNextDayPlanQty(newNextDayPlanQty.setScale(0, RoundingMode.UP).doubleValue());
			resultVo.setStockQty(stockQty);
			resultVo.setTotalPlanQty(new BigDecimal(String.valueOf(BigDecimalUtil.add(resultVo.getDayPlanQty(), resultVo.getNightPlanQty()))));
			resultVo.setIsLargeDemandSpec(isLargeDemandSpec);
			resultVo.getParams().put(EngineConstants.STANDARD_CRIMP_LENGTH, crimpLength); // 一卷的长度
			resultVo.getParams().put(EngineConstants.TOOL_CAPACITY, toolCapacity); // 一个工装的长度
			
			// 计算可用时长
			Double planStock = BigDecimalUtils.qtySub(BigDecimalUtils.add(stockQty, lastDayQty).doubleValue(), resultVo.getCxClass1Plan());
			Cd90StockVo cd90StockVo = new Cd90StockVo();
			cd90StockVo.setStockQty(BigDecimalUtils.valueOf(planStock));
            resultVo.setSupplyTime(this.caculateSuppliyTime(resultVo, cd90StockVo).doubleValue());

			// 记录计算日志
			this.insertCalculateLog(oldScheduleResult, resultVo, lossRate);
		}

		// 重算大卷数
//		this.recaculatePlanNum(scheduleDate, scheduleList, isProductionStage, minRoundRollNum);

		// 记录日志
		String logDetail = logSplit("库存量与成型定额设置：" + toJSONString(stockMap), "损耗率设定：" + toJSONString(lossRateMap));
		autoScheduleLogService.insertCd90ScheduleLog(batchNo, "", "3.1、计算计划量基础数据日志", logDetail);
	}

	/**
	 * 判断是否大需求量规格
	 * @param planQty  计划量
	 * @param largeDemand  大需求量阈值
	 * @param crimpLength  卷曲长度
	 * @param toolCapacity 工装数量
	 * @return
	 */
    private boolean isLargeDemandSpec(double planQty, BigDecimal crimpLength, BigDecimal largeDemand,
            BigDecimal toolCapacity) {
//        double roudingPlanQty = BigDecimalUtils.valueOf(planQty).divide(toolCapacity, 0, RoundingMode.CEILING)
//                .multiply(toolCapacity).doubleValue(); // 取整车
//        return roudingPlanQty > largeDemand.multiply(crimpLength).doubleValue();
        return false; // TODO 暂不在计划量考虑，到均衡统一处理
    }

	/**
	 * 获取各班需求量的累计值（从前日早班开始）
	 *
	 * @param scheduleVo
	 * @param classNum
	 * @return
	 */
	private Double getCxClassPlanCumulative(Cd90ScheduleResultVo scheduleVo, OpenMachineClassEnums classNum) {
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
     * 加载当天库存（按层数分开）
     *
     * @param scheduleDate
     * @return
     */
    private Map<Integer, Map<String, BigDecimal>> loadCd90Stock(Date scheduleDate) {
        return cd90EngineStockMapper.selectCd90StockQty(scheduleDate).stream()
                .filter(v -> StringUtils.isNotEmpty(v.getClothCode()))
                .collect(Collectors.groupingBy(Cd90StockVo::getLayers,
                        Collectors.collectingAndThen(Collectors.toList(), list -> list.stream()
                                .collect(Collectors.toMap(Cd90StockVo::getClothCode, Cd90StockVo::getStockQty)))));
    }

	/**
	 * 加载上一天的早班计划（按层数分开）
	 *
	 * @param scheduleDate
	 * @return
	 */
	private Map<Integer, Map<String, Double>> loadLastDayMidPlan(Date scheduleDate) {
		return cd90EngineStockMapper.listLastDayMidPlan(scheduleDate).stream()
				.filter(v -> StringUtils.isNotEmpty(v.getClothCode()))
				.collect(Collectors.groupingBy(Cd90StockConsumeVo::getLayers,
                        Collectors.collectingAndThen(Collectors.toList(), list -> list.stream()
                                .collect(Collectors.toMap(Cd90StockConsumeVo::getClothCode, Cd90StockConsumeVo::getConsume)))));
	}

	/**
	 * 计划量取整
	 *
	 * @param scheduleVo      排产记录
	 * @param planQty         原计划量
	 * @param toolCapacity    一车可以放的胎面量
	 * @param totalConsumeQty 总需期间量，用于判断收尾规格是否超量
	 * @param classNum        当前班次，从前日早班开始
	 * @return
	 */
	private double planQtyRounding(Cd90ScheduleResultVo scheduleVo, double planQty, BigDecimal toolCapacity,
								   Double totalConsumeQty, OpenMachineClassEnums classNum) {
		if (planQty <= 0D) { // 不排的情况直接返回0即可
			return 0D;
		}
		double roudingPlanQty = BigDecimalUtils.valueOf(planQty).divide(toolCapacity, 0, RoundingMode.CEILING)
				.multiply(toolCapacity).doubleValue(); // 取整车
		if (classNum == null) {
			return roudingPlanQty;
		}
		OpenMachineClassEnums lastClass = classNum;
		if (classNum != OpenMachineClassEnums.CLASS_ONE) { // 取出上一班的班次
			Integer classIndex = classNum.getClassIndex();
			lastClass = OpenMachineClassEnums.getClassEnums(classIndex - 1);
		}
		double lastPlanCumulative = this.getCd90ClassPlanCumulative(scheduleVo, lastClass); // 到上个班次班次班的累计已排计划量
		double newPlanQty = BigDecimalUtil.add(lastPlanCumulative, roudingPlanQty, scheduleVo.getStockQty()); // 库存+已排计划+本班计划
		double result = roudingPlanQty;
		// 如果库存+计划已经超过总需求量，则本班的计划量要限制住不允许超量
		if (newPlanQty > totalConsumeQty) {
			Double increaseMidPlanQty = BigDecimalUtil.sub(newPlanQty, totalConsumeQty);
			result = BigDecimalUtil.sub(roudingPlanQty, increaseMidPlanQty);
		}
		scheduleVo.setCloseOutSpecFlag(newPlanQty >= totalConsumeQty? ApsConstant.STATUS_ENABLE: ApsConstant.STATUS_DISABLE);
		if (result < MIN_PLAN_QTY) { // 只差一点点，不处理
		    return 0;
		}
		return result;
	}

	/**
	 * 获取各班计划量的累计值（从前日早班开始）
	 *
	 * @param scheduleVo
	 * @param classNum
	 * @return
	 */
	private Double getCd90ClassPlanCumulative(Cd90ScheduleResultVo scheduleVo, OpenMachineClassEnums classNum) {
		Double planQty = 0D;
		if (classNum == null) {
			return planQty;
		}
		planQty = BigDecimalUtil.add(planQty, scheduleVo.getLastMidPlanQty());
		if (classNum == OpenMachineClassEnums.CLASS_ONE) {
			return planQty;
		}
		planQty = BigDecimalUtil.add(planQty, scheduleVo.getDayPlanQty());
		if (classNum == OpenMachineClassEnums.CLASS_TWO) {
			return planQty;
		}
		planQty = BigDecimalUtil.add(planQty, scheduleVo.getNightPlanQty());
		if (classNum == OpenMachineClassEnums.CLASS_THREE) {
			return planQty;
		}
		return BigDecimalUtil.add(planQty, scheduleVo.getNextDayPlanQty());
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
		Map<String, String> cd90Params = cd90EngineStockMapper.listCd90Params().stream()
				.collect(Collectors.toMap(Cd90ParamsVo::getParamCode, Cd90ParamsVo::getParamValue));

		// 标准大卷长度默认值
		BigDecimal standardSize = new BigDecimal(params.getOrDefault(EngineConstants.STANDARD_SIZE, "0"));
		BigDecimal breadth = new BigDecimal(params.getOrDefault(EngineConstants.BREADTH, DEFAULT_BREADTH));
		BigDecimal crimpLength = new BigDecimal(cd90Params.getOrDefault(EngineConstants.CRIMP_LENGTH, "0"));

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
		List<Cd90CurlLength> bigRollList = cd90EngineBigRollMapper.listCd90CurlLength();
		Map<String, BigDecimal> bigRollMap = bigRollList.stream().collect(
				Collectors.toMap(Cd90CurlLength::getClothCode, Cd90CurlLength::getCurlLength, (v1, v2) -> v2));

		// 根据大卷对排产计划分组
		Map<String, List<Cd90ScheduleResultVo>> rollScheduleMap = scheduleList.stream()
				.sorted(Comparator.comparing(Cd90ScheduleResultVo::getTotalPlanQty, Comparator.reverseOrder()))
				.collect(Collectors.groupingBy(Cd90ScheduleResultVo::getClothCode));

		// 收尾规格打标记
		scheduleList.forEach(r -> {
			String classOutStatus = closeOutSpecList.contains(r.getClothCode()) ? ApsConstant.STATUS_ENABLE
					: ApsConstant.STATUS_DISABLE;
			r.setCloseOutSpecFlag(classOutStatus);
		});

		for (Entry<String, List<Cd90ScheduleResultVo>> rollScheduleEntry : rollScheduleMap.entrySet()) {
			String bigRollCode = rollScheduleEntry.getKey();
			List<Cd90ScheduleResultVo> rollScheduleList = rollScheduleEntry.getValue();

			for (Cd90ScheduleResultVo cd15ScheduleResultVo : rollScheduleList) {
				if (EngineConstants.CLOSE_TIP_NEED.equals(cd15ScheduleResultVo.getCloseOutSpecFlag())) {
					continue; // 如果已经全部收尾，则不需要做取整操作
				}
				boolean isclassOutSpec = false; // 只要能走到这一步，必定是还没完全收尾

				// 汇总同种大卷的总计划量
				BigDecimal planQty = cd15ScheduleResultVo.getTotalPlanQty();
				if (Optional.ofNullable(planQty).orElse(BigDecimal.ZERO).compareTo(BigDecimal.ZERO) <= 0) {
					continue;
				}
				BigDecimal requireQty = this.caculateRollQty(cd15ScheduleResultVo, breadth); // 计划量换算成大卷需求量
				if (Optional.ofNullable(requireQty).orElse(BigDecimal.ZERO).compareTo(BigDecimal.ZERO) <= 0) {
					continue;
				}
				BigDecimal lineSideStockQty = BigDecimal.ZERO;
				// 重算实际排产计划
				BigDecimal newPlanQty;
				if (lineSideStockQty.compareTo(BigDecimal.ZERO) <= 0) { // 没有线边库，按照设定好的取舍规则（暂定1、2舍弃，3以上取整），取舍后的大卷个数按照系统设置的大卷设置的大卷参数长度，根据计算得出的米数进行计划下达
					// 大卷长度
					String clothCode = cd15ScheduleResultVo.getClothCode();
					BigDecimal clothLength = bigRollMap.getOrDefault(clothCode, crimpLength);
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
		if (BigDecimalUtil.isDigits(schedule.getCraft())) {
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
	 * @param resultVo        计算后排产结果
	 * @param lossRate          损耗率
	 */
	private void insertCalculateLog(String oldScheduleResult, Cd90ScheduleResultVo resultVo, double lossRate) {
		String logDetail = logSplit("开始计算中班和夜班计划量", "计算前排程数据：" + oldScheduleResult,
				"根据库存重新计算中班计划量dayPlanQty：如果 原中班计划量>库存，则 中班计划量 = 原中班计划量 -库存；否则中班计划量 = 0",
				"根据库存重新计算夜班计划量nightPlanQty：如果 原中班计划量>库存，则 晚班计划量=0 ；否则晚班计划量 = （原中班计划量+原晚班计划量-库存）",
				"获得损耗率（从损耗率表获取对应的损耗率，获取顺序：机台+物料编号 > 胎面代码 > 机台 >工序参数配置），耗损率：" + lossRate,
				"如果中班计划量>0，那么中班计划量=中班计划量+夜班计划量，夜班计划量=0（为了让相同的胶在同一个班生产，而且又不能延误生产）",
				"计划量计算好后的排程数据：" + toJSONString(resultVo));
		autoScheduleLogService.insertCd90ScheduleLog(resultVo.getBatchNo(), resultVo.getOrderNo(), "3.2、计算各班计划量",
				logDetail);
	}
}
