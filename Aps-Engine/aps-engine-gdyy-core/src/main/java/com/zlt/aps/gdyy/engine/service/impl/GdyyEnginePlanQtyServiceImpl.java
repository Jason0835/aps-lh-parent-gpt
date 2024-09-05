package com.zlt.aps.gdyy.engine.service.impl;

import static com.alibaba.fastjson.JSON.toJSONString;
import static com.zlt.aps.common.core.utils.ApsCommonUtil.getDouble;
import static com.zlt.aps.common.core.utils.ApsCommonUtil.getDoubleOrDefault;
import static com.zlt.aps.common.core.utils.ApsCommonUtil.logSplit;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.annotation.Resource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.zlt.aps.common.core.constant.ApsConstant;
import com.zlt.aps.common.engine.constants.EngineConstants;
import com.zlt.aps.common.engine.service.AutoScheduleLogService;
import com.zlt.aps.common.engine.utils.CollectionUtil;
import com.zlt.aps.gdyy.engine.mapper.GdyyEngineBigRollMapper;
import com.zlt.aps.gdyy.engine.mapper.GdyyEngineStockMapper;
import com.zlt.aps.gdyy.engine.service.GdyyEngineLossService;
import com.zlt.aps.gdyy.engine.service.GdyyEnginePlanQtyService;
import com.zlt.aps.gdyy.engine.vo.GdyyBigRollVo;
import com.zlt.aps.gdyy.engine.vo.GdyyParamsVo;
import com.zlt.aps.gdyy.engine.vo.GdyyScheduleResultVo;
import com.zlt.aps.gdyy.engine.vo.GdyyStockVo;

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

	@Autowired
	private GdyyEngineStockMapper gdyyEngineStockMapper;
	@Autowired
	private GdyyEngineLossService gdyyEngineLossService;
	@Autowired
	private GdyyEngineBigRollMapper gdyyEngineBigRollMapper;
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
		String batchNo = CollectionUtil.firstElement(scheduleList).getBatchNo();
		// 获取大卷信息
		Map<String, BigDecimal> bigRollMap = gdyyEngineBigRollMapper.listCd15BigRoll().stream()
				.collect(Collectors.toMap(GdyyBigRollVo::getBigRollCode, GdyyBigRollVo::getClothLength));
		// 获取库存量
		Map<String, GdyyStockVo> stockMap = this.getStockMap(scheduleDate, stockLossRate, bigRollMap, standardSize,
				isRollStock);

		// 收尾规格列表
		List<String> closeOutSpecList = gdyyEngineStockMapper.listCloseOutSpec(scheduleDate, isProductionStage);

		// 先抓取15度裁断的库存损耗率
		BigDecimal cd15StockLossRate = this.getCd15StockLossRate();
		// 获取15度裁断（钢带）换算成的大卷库存
		Map<String, GdyyStockVo> cd15StockMap = gdyyEngineStockMapper
				.selectCd15Stock(scheduleDate, cd15StockLossRate, breadth, isProductionStage).stream()
				.collect(Collectors.toMap(GdyyStockVo::getBigRollCode, Function.identity(), (v1, v2) -> v2));

		// 获取损耗率设定
		Map<String, Double> lossRateMap = gdyyEngineLossService.getLossRateMap();
		// 默认损耗率类型转换
		Double defaultLossRateNum = getDouble(defaultLossRate);
		// 记录日志
		String logDetail = logSplit("库存量信息：" + toJSONString(stockMap), "15度库存换算的大卷库存" + toJSONString(cd15StockMap),
				"损耗率设定：" + toJSONString(lossRateMap));
		autoScheduleLogService.insertGdyyScheduleLog(batchNo, "", "2.1、计算排产计划量基础数据日志", logDetail);

		// 计算库存相关信息：根据库存重算计划量
		for (GdyyScheduleResultVo resultVo : scheduleList) {
			// 计算前的排程数据json字符串，用于日志记录
			String oldScheduleResult = toJSONString(resultVo);
			String bigRollCode = resultVo.getBigRollCode();
			BigDecimal class1Plan = BigDecimal.valueOf(resultVo.getClass1Plan());
			BigDecimal class2Plan = BigDecimal.valueOf(resultVo.getClass2Plan());
			// 计划量合计：中班 + 晚班计划量
			BigDecimal allDayPlan = class1Plan.add(class2Plan);
			// 钢压大卷库存信息
			Optional<GdyyStockVo> stockOptional = Optional.ofNullable(stockMap.get(bigRollCode));
			// 半部件库存量
			BigDecimal stockQty = stockOptional.map(GdyyStockVo::getStockQty).orElse(BigDecimal.ZERO);
			// 15度裁断存量换算成大卷的库存量
			BigDecimal cd15StockQty = Optional.ofNullable(cd15StockMap.get(bigRollCode)).map(GdyyStockVo::getStockQty)
					.orElse(BigDecimal.ZERO);
			// 处理库存预留系数
			BigDecimal stockRatioNum = BigDecimal.valueOf(getDoubleOrDefault(stockRatio, DEFAULT_STOCK_RATIO));
			// 总计划量，公式：（中班计划量+晚班计划量）* 预留系数 - 库存 - 15度裁断库存
			BigDecimal totalPlanQty = allDayPlan.multiply(stockRatioNum).subtract(stockQty).subtract(cd15StockQty);

			// 总计划量不能小于0（库存量很大的场景）
			totalPlanQty = totalPlanQty.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : totalPlanQty;
			// 三班计划量
			BigDecimal class3Plan = BigDecimal.ZERO;

			// 不计库存总计划量，公式：（中班计划量+晚班计划量）* 预留系数
			BigDecimal totalPlanQtyNoStock = allDayPlan.multiply(stockRatioNum);
			// 不计库存中夜班计划量
			BigDecimal class1PlanNoStock = class1Plan;
			BigDecimal class2PlanNoStock = class2Plan;
			BigDecimal class3PlanNoStock = totalPlanQtyNoStock.subtract(allDayPlan);

			// 比较总计划量与 中班+晚班计划量，如果总计划量较大，则增加三班计划量
			if (totalPlanQty.compareTo(allDayPlan) > 0) {
				// 三班计划量 = 总计划量 - 中班计划量 - 晚班计划量。中晚班计划量不变
				class3Plan = totalPlanQty.subtract(allDayPlan);
			} else if (totalPlanQty.compareTo(class1Plan) > 0) {
				// 总计划量大于中班计划量，中班计划量不变，晚班计划量=总计划量 - 中班，三班计划量为0
				class2Plan = totalPlanQty.subtract(class1Plan);
			} else {
				// 总计划量小于中班计划量，中班计划量 = 总计划量，晚班计划量=0，三班计划量为0
				class1Plan = totalPlanQty;
				class2Plan = BigDecimal.ZERO;
			}
			// 获取损耗率
			Double lossRate = gdyyEngineLossService.getLossRate(bigRollCode, lossRateMap, defaultLossRateNum);
			// 为弥补损耗的量，无库存计划量也需要填补损耗量，计算公式：新计划量 = 原计划量+原计划量*损耗率
			class1Plan = class1Plan.add(class1Plan.multiply(BigDecimal.valueOf(lossRate)));
			class2Plan = class2Plan.add(class2Plan.multiply(BigDecimal.valueOf(lossRate)));
			class3Plan = class3Plan.add(class3Plan.multiply(BigDecimal.valueOf(lossRate)));

			// 将重算后的计划量赋值给排产记录
			// 结果小数舍入方式调整，modify by 20211230
			resultVo.setStockQty(stockQty.setScale(0, RoundingMode.DOWN).doubleValue());
			// 大卷标准长度，没有则获取默认长度
			BigDecimal newStandardSize = bigRollMap.getOrDefault(bigRollCode, BigDecimal.valueOf(standardSize));
			// 计算大卷数 = 计划量 / 标准长度
			BigDecimal class1PlanNum = class1Plan.divide(newStandardSize, 1, RoundingMode.UP);
			BigDecimal class2PlanNum = class2Plan.divide(newStandardSize, 1, RoundingMode.UP);
			BigDecimal class3PlanNum = class3Plan.divide(newStandardSize, 1, RoundingMode.UP);
			// 判断是否收尾规格
			boolean isCloseOutSpec = closeOutSpecList.contains(bigRollCode);
			// 总大卷数
			BigDecimal totalPlanNum = class1PlanNum.add(class2PlanNum).add(class3PlanNum);
			// 重算计划量，但收尾规格不需要重算
			// 只有总大卷数超过1才需要重算
			if (!isCloseOutSpec && totalPlanNum.compareTo(BigDecimal.ONE) >= 0) {
				// 3舍4入处理
				BigDecimal remainder = totalPlanNum.multiply(BigDecimal.TEN).remainder(BigDecimal.TEN);
				if (remainder.compareTo(FOUR) < 0) {
					totalPlanNum = totalPlanNum.setScale(0, RoundingMode.DOWN);
				} else {
					totalPlanNum = totalPlanNum.setScale(0, RoundingMode.UP);
				}

				// 将计划量均分至三个班，结果向上取整，从1班开始分配，
				class1PlanNum = totalPlanNum.divide(THREE, RoundingMode.UP);
				class2PlanNum = class1PlanNum;
				class3PlanNum = totalPlanNum.subtract(class1PlanNum).subtract(class2PlanNum);

				// 大卷数重算后，再反算出对应的计划量
				class1Plan = class1PlanNum.multiply(newStandardSize);
				class2Plan = class2PlanNum.multiply(newStandardSize);
				class3Plan = class3PlanNum.multiply(newStandardSize);
			}

			resultVo.setClass1Plan(class1Plan.doubleValue());
			resultVo.setClass2Plan(class2Plan.doubleValue());
			resultVo.setClass3Plan(class3Plan.doubleValue());
			resultVo.setClass1PlanNum(class1PlanNum.doubleValue());
			resultVo.setClass2PlanNum(class2PlanNum.doubleValue());
			resultVo.setClass3PlanNum(class3PlanNum.doubleValue());
			resultVo.setCloseOutSpecFlag(isCloseOutSpec ? ApsConstant.STATUS_ENABLE : ApsConstant.STATUS_DISABLE);

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
	 * @param paramsMap
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
}
