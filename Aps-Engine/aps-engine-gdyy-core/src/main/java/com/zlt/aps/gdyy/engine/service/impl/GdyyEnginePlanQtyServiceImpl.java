package com.zlt.aps.gdyy.engine.service.impl;

import com.ruoyi.common.core.utils.bean.BeanUtils;
import com.ruoyi.common.utils.StringUtils;
import com.zlt.aps.common.core.constant.ApsConstant;
import com.zlt.aps.common.core.utils.BigDecimalUtil;
import com.zlt.aps.common.core.utils.BigDecimalUtils;
import com.zlt.aps.common.engine.constants.EngineConstants;
import com.zlt.aps.common.engine.service.AutoScheduleLogService;
import com.zlt.aps.common.engine.utils.CollectionUtil;
import com.zlt.aps.gdyy.api.domain.dto.GdyyReserveStockDto;
import com.zlt.aps.gdyy.api.domain.entity.GdyyMachineInfo;
import com.zlt.aps.gdyy.engine.mapper.GdyyEngineBigRollMapper;
import com.zlt.aps.gdyy.engine.mapper.GdyyEngineMapper;
import com.zlt.aps.gdyy.engine.mapper.GdyyEngineOriginlLineSpecMapper;
import com.zlt.aps.gdyy.engine.mapper.GdyyEngineStockMapper;
import com.zlt.aps.gdyy.engine.service.GdyyEngineLossService;
import com.zlt.aps.gdyy.engine.service.GdyyEnginePlanQtyService;
import com.zlt.aps.gdyy.engine.vo.GdyyBigRollVo;
import com.zlt.aps.gdyy.engine.vo.GdyyOriginalLineSpecVo;
import com.zlt.aps.gdyy.engine.vo.GdyyParamsVo;
import com.zlt.aps.gdyy.engine.vo.GdyyScheduleResultVo;
import com.zlt.aps.gdyy.engine.vo.GdyyStockVo;

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

import static com.alibaba.fastjson.JSON.toJSONString;
import static com.zlt.aps.common.core.utils.ApsCommonUtil.*;

/**
 * 钢带压延计划量信息处理服务实现类
 * 
 * @Description
 * @Author hakimrayn
 * @Date 2021-7-19 11:30:42
 * @Version 1.0
 */
@Service("gdyyEnginePlanQtyService")
public class GdyyEnginePlanQtyServiceImpl implements GdyyEnginePlanQtyService {
	// 预留库存系数默认值：2
	private static final Double DEFAULT_STOCK_RATIO = new Double("1.2");
	/**
	 * 一百，用于百分比 -> 小数的单位换算
	 */
	private final static BigDecimal ONE_HUNDRED = new BigDecimal("100");
	/**
	 * 4，用于三舍四入等特殊运算
	 */
	private final static BigDecimal FOUR = new BigDecimal("4");
	/**
	 * 3，用于三个班均分计划量
	 */
	private final static BigDecimal THREE = new BigDecimal("3");
	/**
	 * 用于两个班均分计划量
	 */
	private final static BigDecimal TWO = new BigDecimal("2");
    private final static BigDecimal HOUR24 = new BigDecimal("24"); // 24小时
    private final static String DEFAULT_PRODUCT_STOCK_HOUR = "48"; // 默认值：保库存供应时长，两天
    private final static BigDecimal DEFAULT_ORIGINL_LINE_LENGTH = new BigDecimal("1700"); // 默认原线长度

	@Autowired
	private GdyyEngineStockMapper gdyyEngineStockMapper;
	@Autowired
	private GdyyEngineLossService gdyyEngineLossService;
	@Autowired
	private GdyyEngineBigRollMapper gdyyEngineBigRollMapper;
	@Autowired
	private GdyyEngineMapper gdyyEngineMapper;
	@Autowired
    private GdyyEngineOriginlLineSpecMapper gdyyEngineOriginlLineSpecMapper;
	@Resource
	private AutoScheduleLogService autoScheduleLogService;

    /**
     * 计算排产计划量
     * 
     * @Author hakimryan
     * @Description
     * @Date 2021-7-19 11:31:01
     * @Param scheduleList 排产记录
     * @param scheduleDate    排产日期
     * @param stockRatio      预留库存系数
     * @param defaultLossRate 默认损耗率
     * @Param stockLossRate 库存损耗率
     * @param standardSize      默认标准长度
     * @param isRollStock       是否按大卷计算库存
     * @param breadth           幅宽
     * @param isProductionStage 仅对投产阶段规格排产
     */
    @Override
    public void calculateSchedulePlanQty(Date scheduleDate, List<GdyyScheduleResultVo> scheduleList, String stockRatio,
            String defaultLossRate, BigDecimal stockLossRate, Double standardSize, boolean isRollStock, Double breadth,
            boolean isProductionStage) {
        // 加载排产参数设置
        Map<String, String> paramsMap = gdyyEngineMapper.listGdyyParams().stream()
                .collect(Collectors.toMap(GdyyParamsVo::getParamCode, GdyyParamsVo::getParamValue, (v1, v2) -> v2));
//		String batchNo = CollectionUtil.firstElement(scheduleList).getBatchNo();
		// 获取大卷信息
		Map<String, BigDecimal> bigRollMap = gdyyEngineBigRollMapper.listCd15BigRoll().stream()
				.collect(Collectors.toMap(GdyyBigRollVo::getBigRollCode, GdyyBigRollVo::getClothLength));
		// 获取库存量
		Map<String, GdyyStockVo> stockMap = this.getStockMap(scheduleDate, stockLossRate, bigRollMap, standardSize,
				isRollStock);
		BigDecimal productStockHour = new BigDecimal(paramsMap.getOrDefault(EngineConstants.PRODUCT_STOCK_HOUR, DEFAULT_PRODUCT_STOCK_HOUR));
        BigDecimal productStockDay = productStockHour.divide(HOUR24, 2, RoundingMode.HALF_UP); // 预生产库存天数

		// 收尾规格列表
		List<String> closeOutSpecList = gdyyEngineStockMapper.listCloseOutSpec(scheduleDate, isProductionStage);

		// 先抓取15度裁断的库存损耗率
//		BigDecimal cd15StockLossRate = this.getCd15StockLossRate();
//		// 获取15度裁断（钢带）换算成的大卷库存
//		Map<String, GdyyStockVo> cd15StockMap = gdyyEngineStockMapper
//				.selectCd15Stock(scheduleDate, cd15StockLossRate, breadth, isProductionStage).stream()
//				.collect(Collectors.toMap(GdyyStockVo::getBigRollCode, Function.identity(), (v1, v2) -> v2));

		// 获取损耗率设定
		Map<String, Double> lossRateMap = gdyyEngineLossService.getLossRateMap();
		// 默认损耗率类型转换
		Double defaultLossRateNum = getDouble(defaultLossRate);
		// 记录日志
//		String logDetail = logSplit("库存量信息：" + toJSONString(stockMap), "15度库存换算的大卷库存" + toJSONString(cd15StockMap),
//				"损耗率设定：" + toJSONString(lossRateMap));
//		autoScheduleLogService.insertGdyyScheduleLog(batchNo, "", "2.1、计算排产计划量基础数据日志", logDetail);
		// 预留库存系数
		List<String> codeList = scheduleList.stream().map(GdyyScheduleResultVo::getBigRollCode).distinct().collect(Collectors.toList());
		Map<String, BigDecimal> reserveStockMap = this.getReserveStockMap(codeList, Double.valueOf(stockRatio));

		List<String> machineIdList = scheduleList.stream().map(GdyyScheduleResultVo::getMachineCode).distinct().collect(Collectors.toList());
		// 查询机台的开机班次，如果有是一个班，则全部安排在这个班，两个班，则平均分配
		List<List<String>> splitList = CollectionUtil.splitList(machineIdList, 500);
		List<GdyyMachineInfo> machineList = new ArrayList<>();
		for (List<String> machineIds : splitList) {
			machineList.addAll(gdyyEngineMapper.listMachineShift(machineIds));
		}
		for (GdyyScheduleResultVo resultVo : scheduleList) { // 先初始化部分信息
            BigDecimal stockQty = Optional.ofNullable(stockMap.get(resultVo.getBigRollCode())).map(GdyyStockVo::getStockQty).orElse(BigDecimal.ZERO); // 半部件库存量
            resultVo.setStockQty(stockQty.setScale(0, RoundingMode.DOWN).doubleValue());
            resultVo.setClass1Plan(0D); // 先清除掉预计的计划量
            resultVo.setClass2Plan(0D);
		}
		Map<Long, String> machineClassShiftMap = machineList.stream().collect(Collectors.toMap(GdyyMachineInfo::getId, GdyyMachineInfo::getOpenMachineClass, (v1, v2) -> v1));
		scheduleList = scheduleList.stream().sorted((r1, r2) -> this.compareResultPlan(r1, r2, false)).collect(Collectors.toList()); // 按供需比升序的顺序处理，优先处理库存缺口最大的计划
		Set<String> originalLineSet = new HashSet<>(); // 原线集合，记录已排产的原线，部分原线是原线规格一致但是锭子数不一致，两者不可混用
		// 计算库存相关信息：根据库存重算计划量
		for (GdyyScheduleResultVo resultVo : scheduleList) {
			// 计算前的排程数据json字符串，用于日志记录
			String oldScheduleResult = toJSONString(resultVo);
			String bigRollCode = resultVo.getBigRollCode();
			String steelLineCode = resultVo.getSteelLineCode();
			BigDecimal class1Plan = BigDecimal.valueOf(resultVo.getClass1Plan());
			BigDecimal class2Plan = BigDecimal.valueOf(resultVo.getClass2Plan());
			
			// 半部件库存量
			BigDecimal stockQty = BigDecimalUtils.valueOf(resultVo.getStockQty());
			// 15度裁断存量换算成大卷的库存量
//			BigDecimal cd15StockQty = Optional.ofNullable(cd15StockMap.get(bigRollCode)).map(GdyyStockVo::getStockQty)
//					.orElse(BigDecimal.ZERO);
			// 处理库存预留系数
			BigDecimal stockRatioNum = reserveStockMap.getOrDefault(bigRollCode, BigDecimal.valueOf(getDoubleOrDefault(stockRatio, DEFAULT_STOCK_RATIO)));
//			BigDecimal stockRatioNum = BigDecimal.valueOf(getDoubleOrDefault(stockRatio, DEFAULT_STOCK_RATIO));
			// 总计划量，公式：（中班计划量+晚班计划量）* 预留系数 - 库存 - 15度裁断库存

//			class1Plan = class1Plan.multiply(stockRatioNum);
//			class2Plan = class2Plan.multiply(stockRatioNum);
//			allDayPlan = class1Plan.add(class2Plan);
            
            // 需求量=(成型早班需求+成型次日夜班需求)*预生产天数
            BigDecimal planQtyOneDay = BigDecimalUtils.add(resultVo.getCxClass3Plan(), resultVo.getCxClass4Plan()).multiply(stockRatioNum); // 一天的需求量，要计算库存倍率
            BigDecimal planQty = BigDecimalUtils.multiply(planQtyOneDay, productStockDay); // 预生产库存 = 一天的需求量 * 预生产天数
            boolean isSteelLineConflict = steelLineCode == null
                    || steelLineCode.endsWith("_") && originalLineSet.contains(steelLineCode.substring(0, steelLineCode.lastIndexOf("_")))
                    || !steelLineCode.endsWith("_") && originalLineSet.contains(steelLineCode + "_"); // 同一种钢丝原线不同钢丝锭子数的规格不要同时排产
            if (!isSteelLineConflict && stockQty.compareTo(planQty) < 0) { // 库存较小，则按一天需求量生产
                class1Plan = planQtyOneDay; // 计划暂时先全部放到夜班
                class2Plan = BigDecimal.ZERO;
                originalLineSet.add(steelLineCode);
            } else {
                class1Plan = BigDecimal.ZERO;
                class2Plan = BigDecimal.ZERO;
            }
            // 计划量合计：中班 + 晚班计划量
            BigDecimal allDayPlan = class1Plan.add(class2Plan);

			// 三班计划量
			BigDecimal class3Plan = BigDecimal.ZERO;

			// 不计库存总计划量，公式：（中班计划量+晚班计划量）* 预留系数
			BigDecimal totalPlanQtyNoStock = allDayPlan.multiply(stockRatioNum);
			// 不计库存中夜班计划量
			BigDecimal class1PlanNoStock = class1Plan;
			BigDecimal class2PlanNoStock = totalPlanQtyNoStock.subtract(class1Plan);
			BigDecimal class3PlanNoStock = BigDecimal.ZERO;

			// 获取损耗率
			Double lossRate = gdyyEngineLossService.getLossRate(bigRollCode, resultVo.getMachineCode(), lossRateMap, defaultLossRateNum);
			// 为弥补损耗的量，无库存计划量也需要填补损耗量，计算公式：新计划量 = 原计划量+原计划量*损耗率
			class1Plan = class1Plan.add(class1Plan.multiply(BigDecimal.valueOf(lossRate))).setScale(0, RoundingMode.UP);
			class2Plan = class2Plan.add(class2Plan.multiply(BigDecimal.valueOf(lossRate))).setScale(0, RoundingMode.UP);
			class3Plan = class3Plan.add(class3Plan.multiply(BigDecimal.valueOf(lossRate))).setScale(0, RoundingMode.UP);

			// 将重算后的计划量赋值给排产记录
			// 结果小数舍入方式调整，modify by 20211230
			resultVo.setStockQty(stockQty.setScale(0, RoundingMode.DOWN).doubleValue());
			// 大卷标准长度，没有则获取默认长度
			BigDecimal newStandardSize = bigRollMap.getOrDefault(bigRollCode, BigDecimal.valueOf(standardSize));
			// 计算大卷数 = 计划量 / 标准长度
			BigDecimal class1PlanNum = class1Plan.divide(newStandardSize, 0, RoundingMode.UP);
			BigDecimal class2PlanNum = class2Plan.divide(newStandardSize, 0, RoundingMode.UP);
			BigDecimal class3PlanNum = class3Plan.divide(newStandardSize, 0, RoundingMode.UP);
			// 判断是否收尾规格
			boolean isCloseOutSpec = closeOutSpecList.contains(bigRollCode);
//			// 总大卷数
//			BigDecimal totalPlanNum = class1PlanNum.add(class2PlanNum).add(class3PlanNum);
//			// 重算计划量，但收尾规格不需要重算
//			// 只有总大卷数超过1才需要重算
//			if (!isCloseOutSpec && totalPlanNum.compareTo(BigDecimal.ONE) >= 0) {
//				// 3舍4入处理
//				BigDecimal remainder = totalPlanNum.multiply(BigDecimal.TEN).remainder(BigDecimal.TEN);
//				if (remainder.compareTo(FOUR) < 0) {
//					totalPlanNum = totalPlanNum.setScale(0, RoundingMode.DOWN);
//				} else {
//					totalPlanNum = totalPlanNum.setScale(0, RoundingMode.UP);
//				}
//
//				// 将计划量均分至三个班，结果向上取整，从1班开始分配，
//				class1PlanNum = totalPlanNum.divide(TWO, RoundingMode.UP);
//				class2PlanNum = totalPlanNum.subtract(class1PlanNum);
////				class3PlanNum = totalPlanNum.subtract(class1PlanNum).subtract(class2PlanNum);
//
//				// 大卷数重算后，再反算出对应的计划量
//				class1Plan = class1PlanNum.multiply(newStandardSize);
//				class2Plan = class2PlanNum.multiply(newStandardSize);
//				class3Plan = class3PlanNum.multiply(newStandardSize);
//			}
			// 大卷取整
			class1Plan = class1PlanNum.multiply(newStandardSize);
            class2Plan = class2PlanNum.multiply(newStandardSize);
            class3Plan = class3PlanNum.multiply(newStandardSize);

			resultVo.setClass1Plan(class1Plan.doubleValue());
			resultVo.setClass2Plan(class2Plan.doubleValue());
			resultVo.setClass3Plan(class3Plan.doubleValue());
			resultVo.setClass1PlanNum(class1PlanNum.doubleValue());
			resultVo.setClass2PlanNum(class2PlanNum.doubleValue());
			resultVo.setClass3PlanNum(class3PlanNum.doubleValue());
			resultVo.setCloseOutSpecFlag(isCloseOutSpec ? ApsConstant.STATUS_ENABLE : ApsConstant.STATUS_DISABLE);
            resultVo.getParams().put(EngineConstants.STANDARD_SIZE, newStandardSize);
			

            BigDecimal stockPlanRate = null;
            if (planQtyOneDay.compareTo(BigDecimal.ZERO) != 0) {
                stockPlanRate = stockQty.divide(planQtyOneDay.multiply(stockRatioNum), 2, RoundingMode.HALF_UP);
            }
            resultVo.setStockPlanRate(stockPlanRate);

			// 根据开机班次计算计划量
			this.computePlanQtyByMachineClassShift(resultVo, machineClassShiftMap);

			// 无库存计划量
			// 为弥补损耗的量，计划量需要填补损耗量，计算公式：新计划量 = 原计划量+原计划量*损耗率
			class1PlanNoStock = class1PlanNoStock.add(class1PlanNoStock.multiply(BigDecimal.valueOf(lossRate)));
			class2PlanNoStock = class2PlanNoStock.add(class2PlanNoStock.multiply(BigDecimal.valueOf(lossRate)));
			class3PlanNoStock = class3PlanNoStock.add(class3PlanNoStock.multiply(BigDecimal.valueOf(lossRate)));
			resultVo.setClass1PlanNoStock(class1PlanNoStock.setScale(0, RoundingMode.UP).doubleValue());
			resultVo.setClass2PlanNoStock(class2PlanNoStock.setScale(0, RoundingMode.UP).doubleValue());
			resultVo.setClass3PlanNoStock(class3PlanNoStock.setScale(0, RoundingMode.UP).doubleValue());

			// 记录计算日志
			this.insertCalculateLog(oldScheduleResult, resultVo);
		}

		// 最终计划量必须等于钢丝卷长的整倍数（参数配置，单位米），不足的量要将总计划补到该值，多出来的量添加到计划量最大的规格上
		this.addSchedulePlanQty(scheduleList, standardSize, closeOutSpecList, bigRollMap, machineClassShiftMap);
	}

	/**
	 * 获取库存信息
	 * 
	 * @param scheduleDate 排产日期
	 * @Param stockLossRate 库存损耗率
	 * @param bigRollMap   大卷长度设置
	 * @param standardSize 默认标准长度
	 * @param isRollStock  是否按大卷计算库存
	 * @return
	 */
	private Map<String, GdyyStockVo> getStockMap(Date scheduleDate, BigDecimal stockLossRate,
			Map<String, BigDecimal> bigRollMap, Double standardSize, boolean isRollStock) {
		// 取出排产日的库存信息
		List<GdyyStockVo> stockList = gdyyEngineStockMapper.selectGdyyStockQty(scheduleDate, stockLossRate);
		Map<String, GdyyStockVo> stockMap = stockList.stream()
				.collect(Collectors.toMap(GdyyStockVo::getBigRollCode, Function.identity(), (v1, v2) -> v2));
		if (isRollStock) {
			// 按大卷计算库存
			for (GdyyStockVo stockVo : stockList) {
				String bigRollCode = stockVo.getBigRollCode();
				// 取出大卷库存
				BigDecimal stockRollQty = Optional.ofNullable(stockVo.getStockRollQty()).orElse(BigDecimal.ZERO);
				// 大卷标准长度，没有则获取默认长度
				BigDecimal newStandardSize = bigRollMap.getOrDefault(bigRollCode, BigDecimal.valueOf(standardSize));
				// 计量单位需要换算：将“卷”换算为“米”，换算公式：钢带长度 =卷数 * 标准长度，结果向上取整
				stockVo.setStockQty(stockRollQty.multiply(newStandardSize));
			}
		}
		return stockMap;
	}

	/**
	 * 获取排产日的16点半部件库存
	 * 
	 * @param scheduleDate 排产日期
	 * @param standardSize 默认标准长度
	 * @Param stockLossRate 库存损耗率
	 * @param isRollStock 是否按大卷计算库存
	 * @return key：帘布编号，value：库存量
	 */
	@Override
	public Map<String, Double> getStockQtyMap(Date scheduleDate, BigDecimal stockLossRate, Double standardSize,
			boolean isRollStock) {
		// 获取大卷信息
		Map<String, BigDecimal> bigRollMap = gdyyEngineBigRollMapper.listCd15BigRoll().stream()
				.collect(Collectors.toMap(GdyyBigRollVo::getBigRollCode, GdyyBigRollVo::getClothLength));
		// 取出排产日的库存信息
		Map<String, GdyyStockVo> stockMap = this.getStockMap(scheduleDate, stockLossRate, bigRollMap, standardSize,
				isRollStock);
		// 转换成map<大卷编号，库存量>的格式
		Map<String, Double> stockQtyMap = new HashMap<>();
		for (Entry<String, GdyyStockVo> entry : stockMap.entrySet()) {
			String bigRollCode = entry.getKey();
			GdyyStockVo stockVo = entry.getValue();
			Double stockQty = Optional.ofNullable(stockVo.getStockQty()).map(v -> v.doubleValue()).orElse(0D);
			stockQtyMap.put(bigRollCode, stockQty);
		}
		return stockQtyMap;
	}

	/**
	 * 获取库存损耗率
	 *
	 * @return
	 */
	private BigDecimal getCd15StockLossRate() {
		// 读取库存损耗率
		String stockLossRate = gdyyEngineStockMapper.listCd15Params(EngineConstants.STOCK_LOSS_RATE).stream()
				.findFirst().map(GdyyParamsVo::getParamValue).orElse("0");
		// 类型转换，单位也需要转换：百分比 -> 小数
		BigDecimal stockLossRateNum = new BigDecimal(stockLossRate);
		// 计算公式：(100 - 损耗率) / 100
		BigDecimal resultRate = ONE_HUNDRED.subtract(stockLossRateNum).divide(ONE_HUNDRED);
		return resultRate.compareTo(BigDecimal.ZERO) > 0 ? resultRate : BigDecimal.ZERO;
	}

	/**
	 * 记录计划量运算的日志信息
	 * 
	 * @Author hakimryan
	 * @Description
	 * @Date 2021-7-19 16:31:44
	 * @param oldScheduleResult
	 * @param scheduleVo
	 */
	private void insertCalculateLog(String oldScheduleResult, GdyyScheduleResultVo scheduleVo) {
		String logDetail = logSplit("开始计算中班、晚班和三班计划量", "计算前排程数据：" + oldScheduleResult,
				"根据库存判断是否增加三班计划量class3Plan：如果 （中班+晚班计划量）*2 - 库存 > 中班+晚班计划量，则三班计划量 = （中班+晚班计划量）*2 - 库存 - (中班+晚班计划量)；否则三班计划量 = 0",
				"如果不增加三班，计算中班计划量class1Plan：原中班计划量*x > 库存，则 中班计划量 = 原中班计划量*x-库存；否则中班计划量 = 0",
				"如果不增加三班，计算晚班计划量class2Plan：如果 原中班计划量*x > 库存，则 晚班计划量 = 原晚班计划量*x ；否则晚班计划量 = 原中班计划量*x+原晚班计划量*x - 库存）",
				"计划量计算好后的排程数据：" + toJSONString(scheduleVo));
		autoScheduleLogService.insertGdyyScheduleLog(scheduleVo.getBatchNo(), scheduleVo.getOrderNo(), "2.2、计算各班计划量",
				logDetail);
	}

	/**
	 * 根据开机班次计算各班计划量
	 * @param resultVo 排程结果
	 * @param machineClassShiftMap 机台班次Map
	 */
	private void computePlanQtyByMachineClassShift(GdyyScheduleResultVo resultVo, Map<Long, String> machineClassShiftMap) {
		String machineIdStr = resultVo.getMachineCode();
		if (StringUtils.isBlank(machineIdStr)) {
			return;
		}
		String[] machineIdArr = machineIdStr.split(",");
		String openClassShift = "";
		for (String machine : machineIdArr) {
			Long machineId = Long.valueOf(machine);
			if (machineClassShiftMap.containsKey(machineId)) {
				if (StringUtils.isNotBlank(openClassShift) && !openClassShift.contains(",")) {
					continue;
				}
				openClassShift = machineClassShiftMap.get(machineId);
			}
		}
		Double dayQty = resultVo.getClass1Plan();
		Double nightQty = resultVo.getClass2Plan();
		Double dayQtyNum = resultVo.getClass1PlanNum();
		Double nightQtyNum = resultVo.getClass2PlanNum();
		double sumPlan = BigDecimalUtil.add(dayQty, nightQty);
		double sumPlanNum = BigDecimalUtil.add(dayQtyNum, nightQtyNum);
		if (openClassShift.contains(",")) {
			double divPlan = BigDecimalUtil.div(sumPlan, 2);
			double divPlanNum = BigDecimalUtil.div(sumPlanNum, 2);
			resultVo.setClass1Plan(divPlan);
			resultVo.setClass1PlanNum(divPlanNum);
			resultVo.setClass2Plan(divPlan);
			resultVo.setClass2PlanNum(divPlanNum);
		} else if (EngineConstants.NIGHT_CLASS_SHIFT.equals(openClassShift)) {
			resultVo.setClass1Plan(sumPlan);
			resultVo.setClass1PlanNum(sumPlanNum);
			resultVo.setClass2Plan(0D);
			resultVo.setClass2PlanNum(0D);
		} else if (EngineConstants.DAY_CLASS_SHIFT.equals(openClassShift)) {
			resultVo.setClass1Plan(0D);
			resultVo.setClass1PlanNum(0D);
			resultVo.setClass2Plan(sumPlan);
			resultVo.setClass2PlanNum(sumPlanNum);
		}
	}


	/**
	 * 取预生产库存倍数Map
	 * @param codeList 要查询的steelRingCode列表
	 * @param reserveStockRate 预生产库存倍数
	 * @return 结果
	 */
	private Map<String, BigDecimal> getReserveStockMap(List<String> codeList, Double reserveStockRate) {
		List<GdyyReserveStockDto> reserveStockList = new ArrayList<>();
		List<List<String>> splitList = CollectionUtil.splitList(codeList, 500);
		for (List<String> list : splitList) {
			reserveStockList.addAll(gdyyEngineStockMapper.listReserveStock(list));
		}
		if (CollectionUtils.isEmpty(reserveStockList)) {
			return Collections.emptyMap();
		}
		return reserveStockList.stream().collect(Collectors.toMap(GdyyReserveStockDto::getBigRollCode, GdyyReserveStockDto::getReserveStockRate, (v1, v2) -> v1));
	}

	/**
	 * 最终计划量必须等于钢丝卷长的整倍数（参数配置，单位米），不足的量要将总计划补到该值，多出来的量添加到计划量最大的规格上
	 * @param scheduleList 排程列表
	 * @param standardSize 大卷标准长度
	 * @param wireCoilLength 机台默认卷长
	 * @param closeOutSpecList 收尾规格列表
	 * @param bigRollMap 钢压大卷卷长map
	 * @param machineClassShiftMap 机台班次map
	 */
	private void addSchedulePlanQty(List<GdyyScheduleResultVo> scheduleList, Double standardSize, List<String> closeOutSpecList, Map<String, BigDecimal> bigRollMap, Map<Long, String> machineClassShiftMap) {
		if (CollectionUtils.isEmpty(scheduleList)) {
			return;
		}
		Map<String, List<GdyyScheduleResultVo>> machineGroupMap = scheduleList.stream()
				.filter(item -> StringUtils.isNotBlank(item.getMachineCode()) && !item.getMachineCode().contains(","))
				.collect(Collectors.groupingBy(GdyyScheduleResultVo::getMachineCode)); // 过滤出有单机台的计划
		machineGroupMap.put("", scheduleList.stream().filter(item -> StringUtils.isBlank(item.getMachineCode())).collect(Collectors.toList())); // 添加无机台的计划
        // 加载钢丝卷长，不同钢丝卷长不一样
        Map<String, BigDecimal> wireCoilLengthMap = gdyyEngineOriginlLineSpecMapper.listGdyyOriginalLineSpec().stream()
                .collect(Collectors.toMap(GdyyOriginalLineSpecVo::getOriginalLineCode,
                        GdyyOriginalLineSpecVo::getOriginalLineLength));
		for (Entry<String, List<GdyyScheduleResultVo>> entry : machineGroupMap.entrySet()) {
			List<GdyyScheduleResultVo> matchScheduleList = entry.getValue();
			// 根据原丝分组
            Map<String, List<GdyyScheduleResultVo>> wireGrouppingMap = matchScheduleList.stream()
                    .filter(r -> StringUtils.isNotEmpty(r.getSteelLineCode()))
                    .collect(Collectors.groupingBy(GdyyScheduleResultVo::getSteelLineCode));			
			
            for (Entry<String, List<GdyyScheduleResultVo>> wireEntry: wireGrouppingMap.entrySet()) {
                List<GdyyScheduleResultVo> wireScheduleList = wireEntry.getValue();
                BigDecimal wireCoilLengthNum = wireCoilLengthMap.getOrDefault(wireEntry.getKey(), DEFAULT_ORIGINL_LINE_LENGTH); // 获取原线长度
                BigDecimal totalPlan = BigDecimal.ZERO;
                for (GdyyScheduleResultVo gdyyScheduleResultVo: wireScheduleList) {
                    gdyyScheduleResultVo.getParams().put(EngineConstants.ORIGINAL_LINE_LENGTH, wireCoilLengthNum);
                    totalPlan = BigDecimalUtils.add(totalPlan, BigDecimal.valueOf(gdyyScheduleResultVo.getTotalPlanQty()));
                }
                if (totalPlan.compareTo(BigDecimal.ZERO) == 0) {
                    continue;
                }
                BigDecimal totalPlanRollNum = totalPlan.divide(wireCoilLengthNum, 0, RoundingMode.HALF_UP); // 计算钢丝原线卷数
                totalPlanRollNum = totalPlanRollNum.compareTo(BigDecimal.ZERO) == 0? BigDecimal.ONE: totalPlanRollNum; // 至少1卷
                BigDecimal multiplyPlan = totalPlanRollNum.multiply(wireCoilLengthNum);
                BigDecimal diffPlan = multiplyPlan.subtract(totalPlan); // 计算取整后的计划量与原计划量的差值
                boolean isCapacityPass = diffPlan.compareTo(BigDecimal.ZERO) < 0; // 是否超量，超量要扣减
                while (diffPlan.compareTo(BigDecimal.ZERO) != 0) {
                    // 同一组钢丝的规格计划量均衡：“（库存+计划量）/需求”的值尽可能接近
                    // 将同一种原线的压延物计划量调整为整卷，每次处理一个标准长度
                    GdyyScheduleResultVo scheduleVo = wireScheduleList.stream()
                            .filter(r -> !isCapacityPass || r.getTotalPlanQty().doubleValue() > 0)
                            .sorted((r1, r2) -> this.compareResultPlan(r1, r2, isCapacityPass)).findFirst()
                            .orElse(null);
                    if (scheduleVo == null) {
                        break;
                    }
                    BigDecimal newStandardSize = bigRollMap.getOrDefault(scheduleVo.getBigRollCode(), BigDecimal.valueOf(standardSize)); // 大卷标准长度，没有则获取默认长度
                    BigDecimal addPlan;
                    BigDecimal class1Plan = BigDecimalUtils.valueOf(scheduleVo.getClass1Plan());
                    BigDecimal class2Plan = BigDecimalUtils.valueOf(scheduleVo.getClass2Plan());
                    if (isCapacityPass) { // 超量需要扣减
                        addPlan = BigDecimalUtils.least(diffPlan.abs(), newStandardSize, scheduleVo.getTotalPlanQty()); // 取标准长度、差值、计划总量的较小值作为本次处理的值
                        BigDecimal class1SubPlan = BigDecimalUtils.least(class1Plan, addPlan);
                        class1Plan = class1Plan.subtract(class1SubPlan);
                        class2Plan = class2Plan.subtract(addPlan.subtract(class1SubPlan));
                    } else { // 缺量需要补值
                        addPlan = BigDecimalUtils.least(diffPlan.abs(), newStandardSize); // 取标准长度、差值的较小值作为本次处理的值
                        class1Plan = class2Plan.compareTo(BigDecimal.ZERO) == 0? class1Plan.add(addPlan): class1Plan;
                        class2Plan = class1Plan.compareTo(BigDecimal.ZERO) == 0? class2Plan.add(addPlan): class2Plan;
                    }
                    scheduleVo.setClass1Plan(class1Plan.doubleValue());
                    scheduleVo.setClass2Plan(class2Plan.doubleValue());
                    diffPlan = isCapacityPass? diffPlan.add(addPlan): diffPlan.subtract(addPlan);
                    if (diffPlan.compareTo(BigDecimal.ZERO) < 0 ^ isCapacityPass) {
                        break;
                    }
                }
			}
			// steve+ :最终计划量必须等于钢丝卷长的整倍数（参数配置，单位米），不足的量要将总计划补到该值，多出来的量添加到计划量最大的规格上 end
		}
	}


    /**
     * 比对排产计划，按计划量》需求量排
     * @param scheduleVo1
     * @param scheduleVo2
     * @param isPass    是否超产能
     * @return
     */
    private int compareResultPlan(GdyyScheduleResultVo scheduleVo1, GdyyScheduleResultVo scheduleVo2, boolean isPass) {
        // 一天消耗量
        Double cxPlanQty1 = BigDecimalUtil.add(scheduleVo1.getCxClass3Plan(), scheduleVo1.getCxClass4Plan());
        Double cxPlanQty2 = BigDecimalUtil.add(scheduleVo2.getCxClass3Plan(), scheduleVo2.getCxClass4Plan());
        // 一天生产量+库存
        Double planQty1 = BigDecimalUtil.add(scheduleVo1.getStockQty(), scheduleVo1.getTotalPlanQty());
        Double planQty2 = BigDecimalUtil.add(scheduleVo2.getStockQty(), scheduleVo2.getTotalPlanQty());

        // 供需比
        BigDecimal stockRate1 = BigDecimalUtils.div(planQty1, cxPlanQty1, 2); 
        BigDecimal stockRate2 = BigDecimalUtils.div(planQty2, cxPlanQty2, 2); 
        if (isPass) {
            return stockRate2.compareTo(stockRate1); // 超产能需要扣减，因此先处理比率高的，即库存多或消耗少的的（倒序）
        } else {
            return stockRate1.compareTo(stockRate2);
        }
    }
}
