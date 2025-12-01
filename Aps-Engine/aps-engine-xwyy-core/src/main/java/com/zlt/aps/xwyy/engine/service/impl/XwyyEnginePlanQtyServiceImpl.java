package com.zlt.aps.xwyy.engine.service.impl;

import com.ruoyi.common.core.utils.DateUtils;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.utils.StringUtils;
import com.zlt.aps.common.core.constant.ApsConstant;
import com.zlt.aps.common.core.utils.BigDecimalUtil;
import com.zlt.aps.common.core.utils.BigDecimalUtils;
import com.zlt.aps.common.engine.common.CxEngineQuotaCommonService;
import com.zlt.aps.common.engine.constants.EngineConstants;
import com.zlt.aps.common.engine.service.AutoScheduleLogService;
import com.zlt.aps.common.engine.utils.CollectionUtil;
import com.zlt.aps.common.engine.utils.GenerageMapKeyUtils;
import com.zlt.aps.xwyy.api.domain.dto.XwyyReserveStockDto;
import com.zlt.aps.xwyy.api.domain.entity.XwyyBigRollRemind;
import com.zlt.aps.xwyy.api.domain.entity.XwyyMachineInfo;
import com.zlt.aps.xwyy.engine.common.XwyyConstants;
import com.zlt.aps.xwyy.engine.mapper.XwyyEngineMachineRollMappingMapper;
import com.zlt.aps.xwyy.engine.mapper.XwyyEngineMapper;
import com.zlt.aps.xwyy.engine.mapper.XwyyEngineSpecifyMachineMapper;
import com.zlt.aps.xwyy.engine.mapper.XwyyEngineStockMapper;
import com.zlt.aps.xwyy.engine.service.XwyyEngineLossService;
import com.zlt.aps.xwyy.engine.service.XwyyEnginePlanQtyService;
import com.zlt.aps.xwyy.engine.vo.*;
import org.apache.commons.collections4.CollectionUtils;
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
 * 纤维压延库存信息处理服务实现类
 * 
 * @Description
 * @Author hakimrayn
 * @Date 2021-7-22 11:30:42
 * @Version 1.0
 */
@Service("xwyyEnginePlanQtyService")
public class XwyyEnginePlanQtyServiceImpl implements XwyyEnginePlanQtyService {
	/**
	 * 可供时长参数：8小时
	 */
	private static final BigDecimal SUPPLY_TIME_PARAM = new BigDecimal("12");
	/**
	 * 计算大卷原先破卷数的特殊卷数
	 */
	private static final BigDecimal SEVEN = new BigDecimal("7");
	/**
	 * 预留库存系数默认值：1
	 */
	private static final Double DEFAULT_STOCK_RATIO = new Double("1");
	/**
	 * 原线可破大卷数默认值：5
	 */
	private static final String DEFAULT_BREAK_ROLL_NUM = "5";
	/**
	 * 一百，用于百分比 -> 小数的单位换算
	 */
	private final static BigDecimal ONE_HUNDRED = new BigDecimal("100");
	/**
	 * 二，主要用于计算数值对半分
	 */
	private static final BigDecimal TWO = new BigDecimal("2");
	/**
	 * 小于该数值四舍五入后不能变成0
	 */
	private static final BigDecimal MIN_ROUND_NUM = new BigDecimal("0.5");
	/**
	 * 过程数值分隔符
	 */
	private final static String PROCESS_VALUE_SEPARATOR = "，";
	/**
	 * 排程不计算库存开关状态：打开
	 */
	private final static String WITH_OUT_STOCK_ON = "1";
	/**
	 * 排程不计算库存开关状态：关闭
	 */
	private final static String WITH_OUT_STOCK_OFF = "0";
	/**
	 * 是否使用外协库存计算开关
	 */
	private final static String ASSIST_STOCK_SWITCH = "ASSIST_STOCK_SWITCH";
	/**
	 * 是否使用外协库存计算开关：打开
	 */
	private final static String ASSIST_STOCK_SWITCH_NO = "1";
    private final static BigDecimal HOUR24 = new BigDecimal("24"); // 24小时
    private final static String DEFAULT_PRODUCT_STOCK_HOUR = "48"; // 默认值：保库存供应时长，两天
    private final static String DEFAULT_LARGE_DEMAND = "5000"; // 需求量超过该值的算大需求量规格

	@Autowired
	private XwyyEngineStockMapper xwyyEngineStockMapper;
	@Autowired
	private XwyyEngineLossService xwyyEngineLossService;
	@Resource
	private CxEngineQuotaCommonService cxEngineQuotaCommonService;
	@Resource
	private AutoScheduleLogService autoScheduleLogService;
	@Autowired
	private XwyyEngineMapper xwyyEngineMapper;
    @Autowired
    private XwyyEngineSpecifyMachineMapper xwyyEngineSpecifyMachineMapper;

	/**
	 * 计算排产库存
	 * 
	 * @Author hakimryan
	 * @Description
	 * @Date 2021-7-22 11:31:01
	 * @param scheduleDate 排产日期
	 * @Param scheduleList 排产记录
	 * @param assistMap       外协规格需求
	 * @param originalLineMap 原线配置
	 * @Param stockLossRate 库存损耗率
	 * @param breadth           幅宽
	 * @param isProductionStage 仅对投产阶段规格排产
     * @param isBreak 是否计算破大卷
	 * @Return
	 */
	@Override
	public void calculateSchedulePlanQty(Date scheduleDate, List<XwyyScheduleResultVo> scheduleList,
			Map<String, XwyyAssistRequirement> assistMap, Map<String, XwyyOriginalLineSpec> originalLineMap,
			BigDecimal stockLossRate, Double breadth, boolean isProductionStage, boolean isBreak) {
		// 加载排产参数设置
		Map<String, String> paramsMap = xwyyEngineMapper.listXwyyParams().stream()
				.collect(Collectors.toMap(XwyyParamsVo::getParamCode, XwyyParamsVo::getParamValue, (v1, v2) -> v2));
		String stockRatio = paramsMap.get(EngineConstants.STOCK_RATIO); // 预留库存比率
		String defaultLossRate = paramsMap.get(EngineConstants.LOSS_RATE); // 默认损耗率
		boolean isAssistStock = ASSIST_STOCK_SWITCH_NO.equals(paramsMap.get(ASSIST_STOCK_SWITCH));
		String standardSize = paramsMap.get(EngineConstants.STANDARD_SIZE); // 标准长度
		boolean isNoStock = WITH_OUT_STOCK_ON.equals(paramsMap.getOrDefault(EngineConstants.SCHEDULE_WITH_OUT_STOCK, WITH_OUT_STOCK_OFF)); // 判断忽略库存是否打开
//		String defaultBreakRollNum = paramsMap.getOrDefault(EngineConstants.XWYY_BREAK_ROLL_NUM, DEFAULT_BREAK_ROLL_NUM); // 原线默认可破大卷数
        BigDecimal productStockHour = new BigDecimal(paramsMap.getOrDefault(EngineConstants.PRODUCT_STOCK_HOUR, DEFAULT_PRODUCT_STOCK_HOUR));
        BigDecimal productStockDay = productStockHour.divide(HOUR24, 2, RoundingMode.HALF_UP); // 预生产库存天数
        BigDecimal largeDemand = new BigDecimal(paramsMap.getOrDefault(EngineConstants.LARGE_DEMAND, DEFAULT_LARGE_DEMAND)); // 大需求量阈值

		String batchNo = CollectionUtil.firstElement(scheduleList).getBatchNo();
		// 获取当日库存
		Map<String, XwyyStockVo> stockMap = this.getStockMap(scheduleDate, stockLossRate, breadth, isProductionStage,
				isAssistStock);
//		// 先抓取90度裁断的库存损耗率
//		BigDecimal cd90StockLossRate = this.getCd90StockLossRate();
//		// 获取90度裁断（帘布）换算成的大卷库存
//		Map<String, BigDecimal> cd90StockMap = xwyyEngineStockMapper
//				.selectCd90Stock(scheduleDate, cd90StockLossRate, breadth).stream()
//				.collect(Collectors.toMap(XwyyStockVo::getBigRollCode, XwyyStockVo::getStockQty, (v1, v2) -> v2));
		// 获取大卷提醒配置中不提醒的大卷编号
		Set<String> noNeedRemaind = xwyyEngineStockMapper.listBigRollRemind().stream()
				.filter(r -> XwyyConstants.REMAIND_FLAG_NO.equals(r.getRemindFlag()))
				.map(XwyyBigRollRemind::getBigRollCode).collect(Collectors.toSet());
        // 纤维大卷长度配置
        Map<String, BigDecimal> xwyyBigRollMap = xwyyEngineStockMapper.listXwyyBigRoll().stream()
                .collect(Collectors.toMap(XwyyBigRollVo::getBigRollCode, XwyyBigRollVo::getClothLength));
		// 收尾规格列表
		List<String> closeOutSpecList = xwyyEngineStockMapper.listCloseOutSpec(scheduleDate, isProductionStage);

		// 获取前日库存
		Map<String, Double> yesStockMap = this.getStockQtyMap(DateUtils.addDays(scheduleDate, -1), stockLossRate,
				isAssistStock);
		// 获取损耗率设定
		Map<String, Double> lossRateMap = xwyyEngineLossService.getLossRateMap();
		// 默认损耗率类型转换
		Double defaultLossRateNum = getDouble(defaultLossRate);
		// 参数：库存预留系数
		BigDecimal stockRatioNum = BigDecimal.valueOf(getDoubleOrDefault(stockRatio, DEFAULT_STOCK_RATIO));
		List<String> codeList = scheduleList.stream().map(XwyyScheduleResultVo::getBigRollCode).distinct().collect(Collectors.toList());
		Map<String, BigDecimal> reserveStockMap = this.getReserveStockMap(codeList, stockRatioNum.doubleValue());
		// 记录算法描述日志
		this.insertDescriptionLog(batchNo);
		// 记录基础参数日志
		this.insertBasedataLog(assistMap, originalLineMap, batchNo, stockMap, lossRateMap, noNeedRemaind);
		List<String> machineIdList = scheduleList.stream().map(XwyyScheduleResultVo::getMachineId).distinct().collect(Collectors.toList());
		// 查询机台的开机班次，如果有是一个班，则全部安排在这个班，两个班，则平均分配
		List<List<String>> splitList = CollectionUtil.splitList(machineIdList, 500);
		List<XwyyMachineInfo> machineList = new ArrayList<>();
		for (List<String> machineIds : splitList) {
			machineList.addAll(xwyyEngineMapper.listMachineShift(machineIds));
		}
		Map<Long, String> machineClassShiftMap = machineList.stream().collect(Collectors.toMap(XwyyMachineInfo::getId, XwyyMachineInfo::getOpenMachineClass, (v1, v2) -> v1));

		// 计划排序，根据库存/用量/库存倍率，顺序排序
        for (XwyyScheduleResultVo resultVo : scheduleList) {
            XwyyStockVo stockVo = stockMap.get(resultVo.getBigRollCode());
            BigDecimal todayStockQty = Optional.ofNullable(stockVo).map(XwyyStockVo::getTodayStock).orElse(BigDecimal.ZERO); // 取出当天库存信息
            BigDecimal planQtyOneDay = BigDecimalUtils.add(resultVo.getCxClass3Plan(), resultVo.getCxClass4Plan());
            BigDecimal stockPlanRate = null;
            if (planQtyOneDay.compareTo(BigDecimal.ZERO) != 0) {
                stockPlanRate = todayStockQty.divide(planQtyOneDay.multiply(stockRatioNum), 2, RoundingMode.HALF_UP);
            }
            resultVo.setTodayStockQty(todayStockQty);
            resultVo.setStockPlanRate(stockPlanRate);
        }
        scheduleList = scheduleList.stream().sorted(Comparator.comparing(XwyyScheduleResultVo::getStockPlanRate, Comparator.nullsLast(BigDecimal::compareTo))).collect(Collectors.toList());
		
		// 计算库存相关信息：根据库存重算计划量
		for (XwyyScheduleResultVo resultVo : scheduleList) {
			// 根据开机班次计算计划量
//			this.computePlanQtyByMachineClassShift(resultVo, machineClassShiftMap);
			// 计算前的排程数据json字符串，用于日志记录
			String oldScheduleResult = toJSONString(resultVo);
			String bigRollCode = resultVo.getBigRollCode();
			BigDecimal resultStockRatio = reserveStockMap.getOrDefault(bigRollCode, stockRatioNum);
			// 获取6厂中班、晚班计划量，都要乘库存倍率
			BigDecimal dayPlanQty = BigDecimal.valueOf(resultVo.getDayPlanQty()).multiply(resultStockRatio);
			BigDecimal nightPlanQty = BigDecimal.valueOf(resultVo.getNightPlanQty()).multiply(resultStockRatio);
			// 晚中半计划量合计
			BigDecimal allPlanQty = dayPlanQty.add(nightPlanQty);
			// 纤维压延库存信息
			XwyyStockVo stockVo = stockMap.get(bigRollCode);
			// 当日库存量
			BigDecimal todayStockQty = resultVo.getTodayStockQty();
			// 16点成型预计消耗量
//			BigDecimal cxUseQty = Optional.ofNullable(stockVo).map(XwyyStockVo::getCxUseQty).orElse(BigDecimal.ZERO);
//			BigDecimal cxUseQty = BigDecimalUtils.add(resultVo.getCxClass1Plan(), resultVo.getCxClass2Plan());
			// 90度裁断存量换算成大卷的库存量
//			BigDecimal cd90StockQty = cd90StockMap.getOrDefault(bigRollCode, BigDecimal.ZERO);
			// 当日库存量计算值，要加上90度库存的换算量，并减去成型预计消耗量；如果忽略库存开关打开，则赋值为0
//			BigDecimal todayStock = isNoStock ? BigDecimal.ZERO : todayStockQty.add(cd90StockQty).subtract(cxUseQty);
			BigDecimal todayStock = todayStockQty;
			// 上一天库存量
			Double yesStockQty = yesStockMap.getOrDefault(bigRollCode, 0D);
			
			// 计算库存可供成型时长
			BigDecimal supplyTime = this.caculateSuppliyTime(resultVo, stockVo);
			// 外协规格，以及需求的计划量
			XwyyAssistRequirement assist = assistMap.get(bigRollCode);
			BigDecimal assistDayPlan = BigDecimal.ZERO;
			BigDecimal assistNightPlan = BigDecimal.ZERO;
			// 白班应支
			BigDecimal dayOut = BigDecimal.ZERO;
			// 是否5厂需求的规格
			boolean isFac5 = false;
			if (assist != null) {
				// 外厂（3/4厂）中夜班
				BigDecimal assistDayPlanQty = Optional.ofNullable(assist.getDayPlanQty()).orElse(BigDecimal.ZERO);
				BigDecimal assistNightPlanQty = Optional.ofNullable(assist.getNightPlanQty()).orElse(BigDecimal.ZERO);
				// 白班应支
				dayOut = Optional.ofNullable(assist.getDayOut()).orElse(BigDecimal.ZERO);
				// 5厂中夜白班
				BigDecimal fac5Class1Plan = Optional.ofNullable(assist.getFac5Class1Plan()).orElse(BigDecimal.ZERO);
				BigDecimal fac5Class2Plan = Optional.ofNullable(assist.getFac5Class2Plan()).orElse(BigDecimal.ZERO);
				BigDecimal fac5Class3Plan = Optional.ofNullable(assist.getFac5Class3Plan()).orElse(BigDecimal.ZERO);

				// 如果5厂有需求，则中夜班以5厂为准
				if (fac5Class1Plan.compareTo(BigDecimal.ZERO) != 0 || fac5Class2Plan.compareTo(BigDecimal.ZERO) != 0
						|| fac5Class3Plan.compareTo(BigDecimal.ZERO) != 0) {
					// 5厂中夜班需求放中班生产（要加上白班的计划，可能是欠产的）
					assistDayPlan = fac5Class1Plan.add(fac5Class2Plan);
					// 5厂白班需求放夜班生产
					assistNightPlan = fac5Class3Plan;
					isFac5 = true;
					// 如果日用参考大于0，说明6厂成型工序有需要使用该规格，需要加上5厂的计划量
					if (resultVo.getDayUsed() != null && resultVo.getDayUsed() > 0) {
						BigDecimal dayUsed = BigDecimal.valueOf(resultVo.getDayUsed());
						resultVo.setDayUsed(dayUsed.add(assistDayPlan).add(assistNightPlan).doubleValue());
					}
				} else {
					// 如果是其他厂的需求，则分别放中夜班生产
					assistDayPlan = assistDayPlanQty;
					assistNightPlan = assistNightPlanQty;
				}
				// 如果算库存，则需要扣减掉白班应支的量
				todayStock = isNoStock ? BigDecimal.ZERO : todayStock.subtract(dayOut);
			}
			// 库存算出负数则直接赋值成0
			todayStock = BigDecimalUtils.greatest(todayStock, BigDecimal.ZERO);

			// 判断是否需要处理原线破大卷，开关打开且6厂或者5厂都有需求量
			boolean isBreakRoll = isBreak && (allPlanQty.compareTo(BigDecimal.ZERO) != 0 || isFac5);
			
//			// 中班总需求量，6厂中班计划量 * 倍率 + 外协中班需求量
//			BigDecimal dayPlanTotalQty = dayPlanQty.add(assistDayPlan);
//			// 夜班总需求量，6厂夜班计划量 * 倍率 + 外协夜班需求量
//			BigDecimal nightPlanTotalQty = nightPlanQty.add(assistNightPlan);

			BigDecimal dayPlanResult = BigDecimal.ZERO;
			BigDecimal nightPlanResult = BigDecimal.ZERO;

			// 比较库存与 中班总需求量
//			if (todayStock.compareTo(dayPlanTotalQty) >= 0) {
//			    // 如果库存量足够，则中班计划量为0，全部安排到晚班生产
//			    dayPlanResult = BigDecimal.ZERO;
//			    // 夜班计划量排库存不足的部分
//			    nightPlanResult = dayPlanTotalQty.add(nightPlanTotalQty).subtract(todayStock);
//			} else {
//			    // 如果库存量不足，则中班排不足的部分
//			    dayPlanResult = dayPlanTotalQty.subtract(todayStock);
//			    // 剩下的排到夜班
//			    nightPlanResult = nightPlanTotalQty;
//			}
//			// 计划量不能小于0（库存量很大的场景）
//			dayPlanResult = dayPlanResult.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : dayPlanResult;
//			// 计划量不能小于0（库存量或者中班很大的场景）
//			nightPlanResult = nightPlanResult.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : nightPlanResult;

            // 需求量=(成型早班需求+成型次日夜班需求)*预生产天数
			// 根据库存可用天数判断是否需要生产
			BigDecimal planQtyOneDay = BigDecimalUtils.add(resultVo.getCxClass3Plan(), resultVo.getCxClass4Plan()).multiply(stockRatioNum); // 一天的需求量，要计算库存倍率
            BigDecimal planQty = BigDecimalUtils.multiply(planQtyOneDay, productStockDay); // 预生产库存 = 一天的需求量 * 预生产天数
            if (planQty.compareTo(largeDemand) > 0 || todayStock.compareTo(planQty) < 0) { // 日用量超过阈值，或者库存较小的，则按一天需求量生产
                dayPlanResult = planQtyOneDay; // 计划暂时先全部放到夜班
                nightPlanResult = BigDecimal.ZERO;
            } else {
                dayPlanResult = BigDecimal.ZERO;
                nightPlanResult = BigDecimal.ZERO;
            }

			// 判断是否收尾规格
			String closeOutSpecFlag = closeOutSpecList.contains(bigRollCode) ? ApsConstant.STATUS_ENABLE
					: ApsConstant.STATUS_DISABLE;

			// 将重算后的计划量赋值给排产记录
			// 计划量的小数舍入在算完原线大卷以及损耗率之后在进行，modify by 20220317
			resultVo.setDayPlanQty(dayPlanResult.doubleValue());
			resultVo.setNightPlanQty(nightPlanResult.doubleValue());
			resultVo.setTotalPlan(BigDecimalUtil.add(resultVo.getDayPlanQty(), resultVo.getNightPlanQty()));
			resultVo.setTodayStock(todayStockQty.setScale(0, RoundingMode.DOWN).doubleValue());
			resultVo.setTodayStockQty(todayStockQty);
			resultVo.setYesStock(Math.floor(yesStockQty));
			resultVo.setSupplyTime(supplyTime.doubleValue());
			resultVo.setBreakRollFlag(isBreakRoll);
			resultVo.setCloseOutSpecFlag(closeOutSpecFlag);
			resultVo.getParams().put(EngineConstants.STOCK_RATIO, stockRatioNum);
			// 默认无需提醒
			resultVo.setOriginalRemindFlag(XwyyConstants.ORIGINAL_REMIND_FLAG_NO);
			// 记录每个规格的计算日志
			this.insertCalculateLog(oldScheduleResult, resultVo);
		}
		// 根据损耗率重算计划量
		this.caculatePlanForLossRate(scheduleList, lossRateMap, defaultLossRateNum);
		// 中夜班均衡
		this.equilibrium(scheduleList);
		// 计算计划量大卷个数
//		this.caculatePlanQtyNumber(scheduleList, xwyyBigRollMap, standardSize);
		// 根据原线处理计划量，并计算大卷个数
		this.caculatePlanForOriginalLine(scheduleList, originalLineMap, xwyyBigRollMap, standardSize);
		// 根据开机班次计算计划量
		for (XwyyScheduleResultVo resultVo : scheduleList) {
			this.computePlanQtyByMachineClassShift(resultVo, machineClassShiftMap);
		}
	}


	/**
	 * 获取库存数据
	 * @param stockMap
	 * @param resultVo
	 * @return
	 */
    private BigDecimal getStock(Map<String, XwyyStockVo> stockMap, XwyyScheduleResultVo resultVo) {
        XwyyStockVo stockVo = stockMap.get(resultVo.getBigRollCode());
        // 取出当天库存信息
        Optional<XwyyStockVo> stockOptional = Optional.ofNullable(stockVo);
        BigDecimal todayStockQty = stockOptional.map(XwyyStockVo::getTodayStock).orElse(BigDecimal.ZERO);
        return todayStockQty;
    }
    
	/**
	 * 根据开机班次计算各班计划量
	 * @param resultVo 排程结果
	 * @param machineClassShiftMap 机台班次Map
	 */
	private void computePlanQtyByMachineClassShift(XwyyScheduleResultVo resultVo, Map<Long, String> machineClassShiftMap) {
		String machineIdStr = resultVo.getMachineId();
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
		Double dayQty = resultVo.getDayPlanQty();
		Double nightQty = resultVo.getNightPlanQty();
		double sumPlan = BigDecimalUtil.add(dayQty, nightQty);
		if (openClassShift.contains(",")) {
			double divPlan = BigDecimalUtil.div(sumPlan, 2);
			resultVo.setDayPlanQty(divPlan);
			resultVo.setNightPlanQty(divPlan);
		} else if (EngineConstants.NIGHT_CLASS_SHIFT.equals(openClassShift)) {
			resultVo.setDayPlanQty(sumPlan);
			resultVo.setNightPlanQty(0D);
		} else if (EngineConstants.DAY_CLASS_SHIFT.equals(openClassShift)) {
			resultVo.setDayPlanQty(0D);
			resultVo.setNightPlanQty(sumPlan);
		}
	}

	/**
	 * 中夜班均衡
	 * 
	 * @param scheduleList 待均衡排产列表
	 */
	private void equilibrium(List<XwyyScheduleResultVo> scheduleList) {
		// 只有中班夜班都有值的需要均衡合并
		// 当日库存量 < (成型一班消耗量 + 成型二班消耗量），直接将夜班合并到中班
		// 比较中班总计划量 与 夜班总计划量

		// 取出中夜班都有计划量的5、6厂计划数据，按总计划量排序，作为待均衡的计划列表
		List<XwyyScheduleResultVo> equilibriumList = scheduleList.stream()
				.filter(s -> s.isBreakRollFlag() && s.getDayPlanQty() > 0D && s.getNightPlanQty() > 0)
				.sorted(Comparator.comparing(XwyyScheduleResultVo::getTotalPlan)).collect(Collectors.toList());

		// 先统一将待均衡的排产计划计划量合并到中班
		for (XwyyScheduleResultVo schedule : equilibriumList) {
			schedule.setDayPlanQty(BigDecimalUtil.add(schedule.getDayPlanQty(), schedule.getNightPlanQty()));
			schedule.setNightPlanQty(0D);
		}

		// 计算中夜班计划量与差异值（总计划量是包含非5、6厂、非中夜班都有计划量的）
		BigDecimal totalDayPlanQty = BigDecimal
				.valueOf(scheduleList.stream().mapToDouble(XwyyScheduleResultVo::getDayPlanQty).sum());
		BigDecimal totalNightPlanQty = BigDecimal
				.valueOf(scheduleList.stream().mapToDouble(XwyyScheduleResultVo::getNightPlanQty).sum());
		// 上一次比较的差异率差异率
		BigDecimal previousRate = this.getDifferenceRate(totalDayPlanQty, totalNightPlanQty);
		for (XwyyScheduleResultVo schedule : equilibriumList) {
			if (totalDayPlanQty.compareTo(totalNightPlanQty) <= 0) {
				// 白班计划量 = 夜班计划量，说明已经均衡，不需要处理
				// 白班计划量 < 夜班计划量，由于之前已经将可进行均衡的计划都预先合并到中班
				// 因此当中班的计划量小于夜班，则说明均衡已不可继续进行，则直接可以直接结束本次均衡处理
				return;
			}

			// 先判断本条计划是否当日库存量 < (成型一班消耗量 + 成型二班消耗量）
			if (schedule.getCxClass1Plan() != null && schedule.getCxClass2Plan() != null) { // 只有6厂的需要做这个判断
				Double stock = schedule.getTodayStock();
				Double cxPlan = BigDecimalUtil.add(schedule.getCxClass1Plan(), schedule.getCxClass2Plan());
				if (stock < cxPlan) { // 如果库存不足，则必须将夜班合并到中班
					// 由于循环前已经统一合并到中班，因此不需要处理
					continue;
				}
			}

			// 尝试将中班计划量放到夜班
			BigDecimal planQty = BigDecimal.valueOf(schedule.getDayPlanQty());
			schedule.setDayPlanQty(0D);
			schedule.setNightPlanQty(planQty.doubleValue());
			totalDayPlanQty = totalDayPlanQty.subtract(planQty);
			totalNightPlanQty = totalNightPlanQty.add(planQty);

			// 计算差异率
			BigDecimal differenceRate = this.getDifferenceRate(totalDayPlanQty, totalNightPlanQty);
			if (differenceRate != null && (previousRate == null || previousRate.compareTo(differenceRate) >= 0)) {
				// 本次差异率能算出来，并满足下述任意一个条件，则继续比对下一条
				// 上一个差异率无法算出来（有一边为0） 或者 且本次差异率不超过上一次的比率
				previousRate = differenceRate;
				continue;
			} else {
				// 不满足上述条件则直接还原中夜班，并结束比对
				schedule.setDayPlanQty(planQty.doubleValue());
				schedule.setNightPlanQty(0D);
				break;
			}
		}
	}

	/**
	 * 计算中夜班差异率
	 * 
	 * @param dayPlanQty   中班完成量
	 * @param nightPlanQty 夜班完成量
	 * @return
	 */
	private BigDecimal getDifferenceRate(BigDecimal dayPlanQty, BigDecimal nightPlanQty) {
		BigDecimal differenceRate = null;
		// 是否日计划更大
		if (dayPlanQty.compareTo(nightPlanQty) > 0 && nightPlanQty.compareTo(BigDecimal.ZERO) != 0) {
			differenceRate = dayPlanQty.subtract(nightPlanQty).divide(nightPlanQty, 4, RoundingMode.HALF_UP);
		} else if (dayPlanQty.compareTo(nightPlanQty) < 0 && dayPlanQty.compareTo(BigDecimal.ZERO) != 0) {
			differenceRate = nightPlanQty.subtract(dayPlanQty).divide(dayPlanQty, 4, RoundingMode.HALF_UP);
		}
		return differenceRate;
	}

	/**
	 * 处理过程记录，仅用于上限初期给用户核对数据
	 * 
	 * @param resultVo
	 * @param dayPlanQty
	 * @param nightPlanQty
	 * @param cxUseQty
	 * @param cd90StockQty
	 * @param assistDayPlan
	 * @param assistNightPlan
	 * @param dayOut
	 * @param isFac5
	 */
	private void handleProcessValue(XwyyScheduleResultVo resultVo, BigDecimal dayPlanQty, BigDecimal nightPlanQty,
			BigDecimal cxUseQty, BigDecimal cd90StockQty, BigDecimal assistDayPlan, BigDecimal assistNightPlan,
			BigDecimal dayOut, boolean isFac5) {
		// 构建过程值，主要用于用户核对数据
		// 6厂中夜班需求量
		this.addDayAndNightProcessValue(resultVo, "ui.data.column.scheduleResult.fac6Plan", dayPlanQty, nightPlanQty);
		if (isFac5) {
			// 5厂中夜班需求量
			this.addDayAndNightProcessValue(resultVo, "ui.data.column.scheduleResult.fac5Plan", assistDayPlan,
					assistNightPlan);
		} else {
			// 3/4厂中夜班需求量
			this.addDayAndNightProcessValue(resultVo, "ui.data.column.scheduleResult.fac34Plan", assistDayPlan,
					assistNightPlan);
		}
		// 库存
		this.addProcessValue(resultVo, "ui.data.column.scheduleResult.cd90Stock", cd90StockQty);
		// 成型消耗量
		this.addProcessValue(resultVo, "ui.data.column.scheduleResult.cxUseQty", cxUseQty);
		// 白班应支
		this.addProcessValue(resultVo, "ui.data.column.scheduleResult.dayOut", dayOut);
	}

	/**
	 * 计算计划量大卷个数
	 * 
	 * @param scheduleList   排程列表
	 * @param xwyyBigRollMap 大卷配置
	 * @param standardSize   默认标准长度
	 */
	private void caculatePlanQtyNumber(List<XwyyScheduleResultVo> scheduleList, Map<String, BigDecimal> xwyyBigRollMap,
            String standardSize) {
		BigDecimal standardSizeNum = new BigDecimal(standardSize);
		for (XwyyScheduleResultVo resultVo : scheduleList) {
			String bigRollCode = resultVo.getBigRollCode();
			// 没有配置大卷长度的使用标准长度
			BigDecimal clothLength = xwyyBigRollMap.getOrDefault(bigRollCode, standardSizeNum);
			if (clothLength.compareTo(BigDecimal.ZERO) != 0) {
				// 大卷个数 = 计划米数 / 标准长度
				BigDecimal dayPlanQty = new BigDecimal(resultVo.getDayPlanQty().toString());
				BigDecimal nightPlanQty = new BigDecimal(resultVo.getNightPlanQty().toString());
				// 大卷数
				BigDecimal dayPlanQtyNum = dayPlanQty.divide(clothLength, 1, RoundingMode.UP);
				BigDecimal nightPlanQtyNum = nightPlanQty.divide(clothLength, 1, RoundingMode.UP);
				resultVo.setDayPlanQtyNum(dayPlanQtyNum.doubleValue());
				resultVo.setNightPlanQtyNum(nightPlanQtyNum.doubleValue());
				resultVo.setTotalPlanNum(dayPlanQtyNum.add(nightPlanQtyNum));
			} else {
				resultVo.setDayPlanQtyNum(0D);
				resultVo.setNightPlanQtyNum(0D);
				resultVo.setTotalPlanNum(BigDecimal.ZERO);
			}
		}
	}

	/**
	 * 根据损耗率重算计划量
	 * 
	 * @param scheduleList       排产计划
	 * @param lossRateMap        损耗率配置
	 * @param defaultLossRateNum 默认损耗率
	 */
	private void caculatePlanForLossRate(List<XwyyScheduleResultVo> scheduleList, Map<String, Double> lossRateMap,
			Double defaultLossRateNum) {
		for (XwyyScheduleResultVo resultVo : scheduleList) {
			// 获取中班、晚班
			BigDecimal dayPlanQty = BigDecimal.valueOf(resultVo.getDayPlanQty());
			BigDecimal nightPlanQty = BigDecimal.valueOf(resultVo.getNightPlanQty());
			String bigRollCode = resultVo.getBigRollCode();
			// 获取损耗率
			Double lossRate = xwyyEngineLossService.getLossRate(bigRollCode, resultVo.getMachineId(), lossRateMap,
					defaultLossRateNum);
			// 为弥补损耗的量，计划量需要填补损耗量，计算公式：新计划量 = 原计划量+原计划量*损耗率
			BigDecimal dayLossQty = dayPlanQty.multiply(BigDecimal.valueOf(lossRate));
			BigDecimal nightLossQty = nightPlanQty.multiply(BigDecimal.valueOf(lossRate));
			dayPlanQty = dayPlanQty.add(dayLossQty);
			nightPlanQty = nightPlanQty.add(nightLossQty);
			resultVo.setDayPlanQty(dayPlanQty.setScale(0, RoundingMode.UP).doubleValue());
			resultVo.setNightPlanQty(nightPlanQty.setScale(0, RoundingMode.UP).doubleValue());
			resultVo.setTotalPlan(BigDecimalUtil.add(resultVo.getDayPlanQty(), resultVo.getNightPlanQty()));

			// 过程值添加损耗量
			this.addDayAndNightProcessValue(resultVo, "ui.data.column.scheduleResult.lossQty", dayLossQty,
					nightLossQty);
		}
	}
	
	/**
	 * 根据原线长度重算计划量，计划量等于原线长度的整倍数
	 * @param scheduleList
	 * @param originalLineMap
	 */
	private void caculatePlanForOriginalLine(List<XwyyScheduleResultVo> scheduleList, Map<String, XwyyOriginalLineSpec> originalLineMap, Map<String, BigDecimal> xwyyBigRollMap, String standardSize) {
	    // 数据按原线规格分组，并按计划量由大到小排序
	    Map<String, List<XwyyScheduleResultVo>> originalLineGroupMap = scheduleList.stream().sorted(Comparator.comparing(XwyyScheduleResultVo::getTotalPlan, Comparator.reverseOrder())).collect(Collectors.groupingBy(XwyyScheduleResultVo::getOriginalLineCode));
	    BigDecimal standardSizeNum = BigDecimalUtils.valueOf(standardSize);
	    for (Entry<String, List<XwyyScheduleResultVo>> entry: originalLineGroupMap.entrySet()) {
	        String originalLineCode = entry.getKey();
	        List<XwyyScheduleResultVo> groupScheduleList = entry.getValue();
	        XwyyOriginalLineSpec originalLineSpec = originalLineMap.get(originalLineCode); // 原线配置
	        if (originalLineSpec == null || BigDecimalUtils.valueOf(originalLineSpec.getOriginalLineLength()).compareTo(BigDecimal.ZERO) <= 0) {
	            continue; // 原线配置不正确的情况下不处理
	        }
	        BigDecimal wireCoilLength = originalLineSpec.getOriginalLineLength();
	        BigDecimal totalPlan = BigDecimalUtils.valueOf(groupScheduleList.stream().map(XwyyScheduleResultVo::getTotalPlan).reduce(0D, (a, b) -> BigDecimalUtil.add(a, b))); // 同一原线总计划量
	        BigDecimal newTotalPlan = totalPlan.divide(wireCoilLength, 0, RoundingMode.UP).multiply(wireCoilLength);
	        XwyyScheduleResultVo greatestSchedule = CollectionUtil.firstElement(groupScheduleList);
	        double diffNum = newTotalPlan.subtract(totalPlan).doubleValue();
	        greatestSchedule.setDayPlanQty(BigDecimalUtil.add(greatestSchedule.getDayPlanQty(), diffNum));
	        greatestSchedule.setTotalPlan(BigDecimalUtil.add(greatestSchedule.getTotalPlan(), diffNum));
	        // 计算整数
	        for (XwyyScheduleResultVo resultVo : groupScheduleList) {
                // 大卷个数 = 计划米数 / 标准长度
	            BigDecimal clothLength = xwyyBigRollMap.getOrDefault(resultVo.getBigRollCode(), standardSizeNum);
                BigDecimal dayPlanQty = new BigDecimal(resultVo.getDayPlanQty().toString());
                BigDecimal nightPlanQty = new BigDecimal(resultVo.getNightPlanQty().toString());
                BigDecimal dayPlanQtyNum = dayPlanQty.divide(clothLength, 0, RoundingMode.UP);
                BigDecimal nightPlanQtyNum = nightPlanQty.divide(clothLength, 0, RoundingMode.UP);
                resultVo.setDayPlanQtyNum(dayPlanQtyNum.doubleValue());
                resultVo.setNightPlanQtyNum(nightPlanQtyNum.doubleValue());
                resultVo.setTotalPlanNum(dayPlanQtyNum.add(nightPlanQtyNum));
                resultVo.getParams().put(EngineConstants.ORIGINAL_LINE_LENGTH, wireCoilLength); // 原线长，缓存到params中
                resultVo.getParams().put(EngineConstants.STANDARD_SIZE, clothLength); // 大卷长度，缓存到params中
	        }
	    }
    }

	/**
	 * 计算原线代码取整卷的量<br/>
	 * 六厂与5厂的规格，同一原线代码的规格，需要确保合计起来的计划量刚好够破整数卷<br/>
	 * 不足整卷的小数部分四舍五入<br/>
	 * 破整卷规则： 1.原线破5卷（1、2 舍；3、4 进） 2.原线破4卷（1、2 舍；3 进） 3.原线破3卷（1 舍；2 进） <br/>
	 * 进位多出来的量，按照一个大卷米数占同原线的规格各日用参考米数百分比比较，选择占用百分比最低的规格，增加一卷原线后<br/>
	 * 再次计算各规格增加大卷占用日用参考米数的百分比，再次选择占用百分比最低的规格进行添加
	 * <p/>
	 * 舍的量需要更新到计划量最多的一班中<br/>
	 * 特例1：计划量>0，但不足破整卷数的1倍，则必须使用进位 <br/>
	 * 特例2：计划量取整后需要舍，但是舍的量会导致计划量最多的一班扣减后小于等于0，则必须使用进位，且进位的量要平均加到计划量最大的几班上 <br/>
	 * 
	 * 
	 * @param scheduleList        排产计划
	 * @param originalLineMap     原线规格配置
	 * @param noNeedRemaind       大卷原线无需提醒配置
	 * @param defaultBreakRollNum 原线默认可破大卷数
	 */
	private void caculatePlanForOriginalLine(List<XwyyScheduleResultVo> scheduleList,
			Map<String, BigDecimal> xwyyBigRollMap, String standardSize,
			Map<String, XwyyOriginalLineSpec> originalLineMap, Set<String> noNeedRemaind, String defaultBreakRollNum) {
		// 先过滤数据：
		// 1、需要整卷破原线大卷的规格
		// 2、过滤掉收尾规格
		// 3、过滤掉计划量为0的
		// 然后需要按计划量倒序排序
		// 最后按原线代码对排产记录分组
		Map<String, List<XwyyScheduleResultVo>> scheduleLineMap = scheduleList.stream()
				.filter(s -> s.getTotalPlanNum().compareTo(BigDecimal.ZERO) > 0 && s.isBreakRollFlag()
						&& ApsConstant.STATUS_DISABLE.equals(s.getCloseOutSpecFlag()))
//				.sorted(Comparator.comparing(XwyyScheduleResultVo::getTotalPlanNum, Comparator.reverseOrder()))
				.collect(Collectors.groupingBy(XwyyScheduleResultVo::getOriginalLineCode));
		BigDecimal standardSizeNum = new BigDecimal(standardSize); // 大卷标准长度
		// 遍历过滤后的
		for (Entry<String, List<XwyyScheduleResultVo>> entry : scheduleLineMap.entrySet()) {
			List<XwyyScheduleResultVo> resultList = entry.getValue();
			// 需要根据大卷数反算出计划量米数
			for (XwyyScheduleResultVo schedule : resultList) {
				String bigRollCode = schedule.getBigRollCode();
				schedule.setRollStandardSize(xwyyBigRollMap.getOrDefault(bigRollCode, standardSizeNum)); // 每卷标准长度
			}
			// 原线代码
			String originalLineCode = entry.getKey();
			// 原线的可破大卷数，先从配置中获取，没有则使用系统配置的默认值
			Long breakRollNum = Optional.ofNullable(originalLineMap.get(originalLineCode))
					.map(XwyyOriginalLineSpec::getBreakRollNum).orElse(Long.valueOf(defaultBreakRollNum));
			BigDecimal breakRollNumDecimal = BigDecimal.valueOf(breakRollNum);
			// 重算计划大卷数
			this.recacluatePlanRollNum(resultList, breakRollNumDecimal);
			// 根据大卷数更新计划量
			this.caculatePlanQtyFromRollNum(resultList);
			// 原线中夜班均衡
			this.originalLineEquilibrium(resultList, breakRollNumDecimal);
		}
	}

	/**
	 * 原线的中夜班均衡，保证中夜班计划量只能是用原线卷数的倍数
	 *
	 * @param scheduleList        排程列表
	 * @param breakRollNumDecimal 原线的可破大卷数
	 */
	private void originalLineEquilibrium(List<XwyyScheduleResultVo> scheduleList, BigDecimal breakRollNumDecimal) {
		// 如果中班计划量个数达到原线卷数的整数倍，无需均衡
		BigDecimal remainder = BigDecimal.valueOf(scheduleList.stream()
						.filter(v -> v.getDayPlanQtyNum() != null)
						.mapToDouble(XwyyScheduleResultVo::getDayPlanQtyNum)
						.sum())
				.remainder(breakRollNumDecimal);
		if (remainder.compareTo(BigDecimal.ZERO) == 0) {
			return;
		}
		remainder=breakRollNumDecimal.subtract(remainder);


		// 可连续生产优先均衡
		List<XwyyScheduleResultVo> continuousList = scheduleList.stream()
				.filter(v -> v.getDayPlanQtyNum() != null && v.getDayPlanQtyNum() > 0 && v.getNightPlanQtyNum() != null && v.getNightPlanQtyNum() > 0)
				.collect(Collectors.toList());
		remainder = originalLineEquilibriumByListAndDiff(continuousList, remainder);

		int compare = BigDecimal.ZERO.compareTo(remainder);
		// 完成均衡
		if (compare == 0) {
			return;
		}

		// 非连续生产补充均衡
		List<XwyyScheduleResultVo> notContinuousList = scheduleList.stream()
				.filter(v -> (v.getDayPlanQtyNum() == null || v.getDayPlanQtyNum() <= 0) && v.getNightPlanQtyNum() != null && v.getNightPlanQtyNum() > 0)
				.collect(Collectors.toList());
		// 可连续生产均衡失败,直接将夜班计划量/计划量个数全部移动到中班
		if (compare > 0) {
			nightPlanToDayPlanAndNum(notContinuousList);
			return;
		}
		originalLineEquilibriumByListAndDiff(notContinuousList, remainder);
	}

	/**
	 * 根据[可进行原线的中夜班均衡的列表]和[待均衡值]进行原线的中夜班均衡
	 *
	 * @param canEquilibriumList 可进行原线的中夜班均衡的列表
	 * @param remainder          待均衡值
	 * @return 返回0表示完成均衡;返回大于0的数表示还需均衡;返回小于0的数表示存在多个相同夜班计划量个数和可供应时长的记录,需要将夜班计划量/计划量个数全部移动到中班
	 */
	private BigDecimal originalLineEquilibriumByListAndDiff(List<XwyyScheduleResultVo> canEquilibriumList, BigDecimal remainder) {
		if (CollectionUtil.isEmpty(canEquilibriumList) || BigDecimal.ZERO.compareTo(remainder) >= 0) {
			return remainder;
		}

		// 记录相同夜班计划量个数和可供应时长记录的个数大于1的Set
		Function<XwyyScheduleResultVo, String> keyFunction = v -> GenerageMapKeyUtils.createMapKey(v.getNightPlanQtyNum().toString(), v.getSupplyTime().toString());
		Map<String, Long> nightPlanAndSupplyTimeMap = canEquilibriumList.stream().collect(Collectors.groupingBy(keyFunction, Collectors.counting()));
		Set<String> nightPlanAndSupplyTimeSet = new HashSet<>();
		for (Entry<String, Long> entry : nightPlanAndSupplyTimeMap.entrySet()) {
			if (entry.getValue() > 1) {
				nightPlanAndSupplyTimeSet.add(entry.getKey());
			}
		}

		// 根据夜班计划量个数、可供应时长从小到大排序
		List<XwyyScheduleResultVo> sortEquilibriumList = canEquilibriumList.stream()
				.sorted(Comparator.comparing(XwyyScheduleResultVo::getNightPlanQtyNum)
						.thenComparing(XwyyScheduleResultVo::getSupplyTime)).collect(Collectors.toList());

		// 找恰好等于中班进行原线凑整的差值的夜班计划量
		Optional<XwyyScheduleResultVo> scheduleResultVo = sortEquilibriumList.stream()
				.filter(v -> remainder.compareTo(BigDecimal.valueOf(v.getNightPlanQtyNum())) == 0)
				.findFirst();
		if (scheduleResultVo.isPresent()) {
			XwyyScheduleResultVo xwyyScheduleResultVo = scheduleResultVo.get();

			// 如果存在相同夜班计划量和可供应时长，将可均衡的夜班计划量/计划量个数全部移动到中班
			if (nightPlanAndSupplyTimeSet.contains(keyFunction.apply(xwyyScheduleResultVo))) {
				this.nightPlanToDayPlanAndNum(canEquilibriumList);

				return BigDecimal.valueOf(-1D);
			}

			// 将该记录的夜班计划量/计划量个数全部移动到中班
			this.nightPlanToDayPlanAndNum(Collections.singletonList(xwyyScheduleResultVo));

			return BigDecimal.ZERO;
		}

		// 待均衡列表
		List<XwyyScheduleResultVo> waitEquilibriumList = new ArrayList<>();
		// 从小到大凑中班进行原线凑整的差值
		BigDecimal sum = BigDecimal.ZERO;
		for (XwyyScheduleResultVo xwyyScheduleResultVo : sortEquilibriumList) {
			// 如果存在相同夜班计划量和可供应时长，将可均衡的夜班计划量/计划量个数全部移动到中班
			if (nightPlanAndSupplyTimeSet.contains(keyFunction.apply(xwyyScheduleResultVo))) {
				this.nightPlanToDayPlanAndNum(canEquilibriumList);

				return BigDecimal.valueOf(-1D);
			}

			// 叠加夜班计划量,与待均衡值的比较
			BigDecimal nightPlanQtyNum = BigDecimal.valueOf(xwyyScheduleResultVo.getNightPlanQtyNum());
			BigDecimal advanceSum = sum.add(nightPlanQtyNum);
			int compare = advanceSum.compareTo(remainder);

			// 判断是否可以结束均衡
			if (compare == 0) {
				waitEquilibriumList.add(xwyyScheduleResultVo);
				// 将待均衡的夜班计划量/计划量个数全部移动到中班
				this.nightPlanToDayPlanAndNum(waitEquilibriumList);

				return BigDecimal.ZERO;
			}

			// 超过待均衡值
			if (compare > 0) {
				// 将待均衡的夜班计划量/计划量个数全部移动到中班
				this.nightPlanToDayPlanAndNum(waitEquilibriumList);

				// 超过待均衡值,取出部分夜班计划量/计划量个数移动到中班
				BigDecimal planDiff = remainder.subtract(sum);
				// 计划量的差值
				double rollStandardSizeDiff = xwyyScheduleResultVo.getRollStandardSize().multiply(planDiff).doubleValue();
				// 中班补充夜班的计划量/计划量个数
				xwyyScheduleResultVo.setDayPlanQty(xwyyScheduleResultVo.getDayPlanQty() + rollStandardSizeDiff);
				xwyyScheduleResultVo.setDayPlanQtyNum(xwyyScheduleResultVo.getDayPlanQtyNum() + planDiff.doubleValue());
				// 扣减夜班计划量/计划量个数
				xwyyScheduleResultVo.setNightPlanQty(xwyyScheduleResultVo.getNightPlanQty() - rollStandardSizeDiff);
				xwyyScheduleResultVo.setNightPlanQtyNum(xwyyScheduleResultVo.getNightPlanQtyNum() - planDiff.doubleValue());

				// 过程值添加夜班到中班的变化
				this.addProcessValue(xwyyScheduleResultVo, "ui.data.column.scheduleResult.nightToDay",
						BigDecimal.valueOf(rollStandardSizeDiff));

				return BigDecimal.ZERO;
			}

			// 填充待均衡列表,汇总凑整的值
			waitEquilibriumList.add(xwyyScheduleResultVo);
			sum = advanceSum;
		}

		// 还未达到均衡,将待均衡的夜班计划量/计划量个数全部移动到中班
		this.nightPlanToDayPlanAndNum(waitEquilibriumList);

		// 下阶段的待均衡值
		return remainder.subtract(sum);
	}

	/**
	 * 将夜班计划量/计划量个数全部移动到中班
	 *
	 * @param scheduleList 排程列表
	 */
	private void nightPlanToDayPlanAndNum(List<XwyyScheduleResultVo> scheduleList) {
		for (XwyyScheduleResultVo xwyyScheduleResultVo : scheduleList) {
			// 过程值添加夜班到中班的变化
			this.addProcessValue(xwyyScheduleResultVo, "ui.data.column.scheduleResult.nightToDay",
					BigDecimal.valueOf(xwyyScheduleResultVo.getNightPlanQty()));

			// 中班补充夜班的计划量/计划量个数
			xwyyScheduleResultVo.setDayPlanQty(xwyyScheduleResultVo.getDayPlanQty() + xwyyScheduleResultVo.getNightPlanQty());
			xwyyScheduleResultVo.setDayPlanQtyNum(xwyyScheduleResultVo.getDayPlanQtyNum() + xwyyScheduleResultVo.getNightPlanQtyNum());
			// 清零夜班计划量/计划量个数
			xwyyScheduleResultVo.setNightPlanQty(0D);
			xwyyScheduleResultVo.setNightPlanQtyNum(0D);
		}
	}

	/**
	 * 根据大卷数反算计划量
	 * 
	 * @param resultList 排程计划
	 */
	private void caculatePlanQtyFromRollNum(List<XwyyScheduleResultVo> resultList) {
		// 需要根据大卷数反算出计划量米数
		for (XwyyScheduleResultVo schedule : resultList) {
			BigDecimal clothLength = schedule.getRollStandardSize(); // 每卷标准长度
			BigDecimal totalPlan = BigDecimal.valueOf(schedule.getTotalPlan()); // 原计划量
			BigDecimal newTotalPlan = schedule.getTotalPlanNum().multiply(clothLength); // 新计划量 = 大卷数 * 标准长度
			// 重算该规格的总计划量
			schedule.setTotalPlan(newTotalPlan.doubleValue());
			if (schedule.getDayPlanQty() > 0) {
				schedule.setDayPlanQty(newTotalPlan.doubleValue());
				schedule.setDayPlanQtyNum(schedule.getTotalPlanNum().doubleValue());
				// 过程值添加整原线卷的增量
				this.addDayAndNightProcessValue(schedule, "ui.data.column.scheduleResult.originalLinePlan",
						newTotalPlan.subtract(totalPlan), null);
			} else {
				schedule.setNightPlanQty(newTotalPlan.doubleValue());
				schedule.setNightPlanQtyNum(schedule.getTotalPlanNum().doubleValue());
				// 过程值添加整原线卷的增量
				this.addDayAndNightProcessValue(schedule, "ui.data.column.scheduleResult.originalLinePlan", null,
						newTotalPlan.subtract(totalPlan));
			}
		}
	}

	/**
	 * 重算计划量大卷数
	 * 
	 * @param resultList	同一种原线的排产记录
	 * @param breakRollNumDecimal	可破卷数
	 * @return
	 */
	private void recacluatePlanRollNum(List<XwyyScheduleResultVo> resultList, BigDecimal breakRollNumDecimal) {
		// 卷数全部四舍五入，并求和
		BigDecimal newTotalPlanNum = BigDecimal.ZERO;
		for (XwyyScheduleResultVo schedule : resultList) {
			BigDecimal tempTotalPlanNum;
			if (schedule.getTotalPlanNum().compareTo(MIN_ROUND_NUM) <= 0) {
				tempTotalPlanNum = BigDecimal.ONE; // 小于0.5卷的四舍五入处理要变成1，不能直接变成0
			} else {
				tempTotalPlanNum = schedule.getTotalPlanNum().setScale(0, RoundingMode.HALF_UP);
			}
			newTotalPlanNum = newTotalPlanNum.add(tempTotalPlanNum);
			schedule.setTotalPlanNum(tempTotalPlanNum);
		}
		// 计算预计大卷数
		BigDecimal estimateRollNum = this.caculateEstimateRollNum(newTotalPlanNum, breakRollNumDecimal);
		// 如果四舍五入后的卷数与预计卷数一致，则不需要继续处理
		if (estimateRollNum.compareTo(newTotalPlanNum) == 0) {
			return;
		}

		// 根据预计卷数与四舍五入后的实际计划量的差异确定是补量还是扣减量
		BigDecimal differentPlanNum = estimateRollNum.subtract(newTotalPlanNum);
		XwyyScheduleResultVo maxPlanSchedule = resultList.stream()
				.max(Comparator.comparing(XwyyScheduleResultVo::getTotalPlanNum)).get();
		if (differentPlanNum.compareTo(BigDecimal.ZERO) < 0 && newTotalPlanNum.compareTo(SEVEN) != 0
				&& maxPlanSchedule.getTotalPlanNum().compareTo(differentPlanNum.abs()) > 0) {
			// 预计量较小，说明需要扣减计划
			// 7卷是列外，要走进位逻辑（需求里有要求）
			// 扣减前，需要先校验计划量最大的规格是否足够扣减，如果不够（包括0），则需要改成进位
			maxPlanSchedule.setTotalPlanNum(maxPlanSchedule.getTotalPlanNum().add(differentPlanNum));
		} else {// 其余情况，说明需要进位补计划量
			BigDecimal tempDifferentPlanNum;
			if (differentPlanNum.compareTo(BigDecimal.ZERO) < 0) { // 如果差异值小于0，说明是特殊情况需要转换成进位
				// 由舍转为入，只需要差异值 + 破大卷数即可
				tempDifferentPlanNum = differentPlanNum.add(breakRollNumDecimal);
			} else {
				tempDifferentPlanNum = differentPlanNum;
			}
			// 每次将一卷分配给 大卷米数/日用参考最小的一笔
			while (tempDifferentPlanNum.compareTo(BigDecimal.ZERO) > 0) {
				XwyyScheduleResultVo minRateSchedule = resultList.stream().min((r1, r2) -> {
					return this.caculatePlanDayUsedRate(r1).compareTo(this.caculatePlanDayUsedRate(r2));
				}).get();
				tempDifferentPlanNum = tempDifferentPlanNum.subtract(BigDecimal.ONE);
				minRateSchedule.setTotalPlanNum(minRateSchedule.getTotalPlanNum().add(BigDecimal.ONE));
			}
		}
	}

	/**
	 * 计算计划量（米）与日用参考量的比值
	 * 
	 * @param schedule
	 * @return
	 */
	private BigDecimal caculatePlanDayUsedRate(XwyyScheduleResultVo schedule) {
		if (schedule.getDayUsed() != null && schedule.getDayUsed() > 0) {
			BigDecimal clothLength = schedule.getRollStandardSize(); // 每卷标准长度
			BigDecimal totalPlan = schedule.getTotalPlanNum().multiply(clothLength); // 计划量 = 大卷数 * 标准长度
			return totalPlan.divide(BigDecimal.valueOf(schedule.getDayUsed()), 8, RoundingMode.HALF_UP); // 计划量 / 日用参考量
		}
		// 日用参考量为0时，优先级比较低，因此直接返回一个较大的值
		return BigDecimal.valueOf(Integer.MAX_VALUE);
	}

	/**
	 * 根据计划量与可破大卷数计算预计卷数
	 * 
	 * @param totalPlanNum        计划量
	 * @param breakRollNumDecimal 可破大卷数
	 * @return
	 */
	private BigDecimal caculateEstimateRollNum(BigDecimal totalPlanNum, BigDecimal breakRollNumDecimal) {

		if (totalPlanNum.compareTo(breakRollNumDecimal) <= 0) {// 如果计划卷数不大于破大卷数，则需要将计划卷数补到破大卷数，即预计卷数 = 破大卷数
			return breakRollNumDecimal;
		}

		BigDecimal[] divideResult = totalPlanNum.divideAndRemainder(breakRollNumDecimal);
		BigDecimal divide = divideResult[0]; // 取商，总计划卷数/破大卷数的结果向下取整
		BigDecimal remainder = divideResult[1]; // 取模，总计划卷数%破大卷数
		if (remainder.compareTo(BigDecimal.ZERO) == 0) {
			// 如果模数为0，即满足按整卷破原线的要求
			return totalPlanNum;
		}
		// 通过模判断是进还是舍，判断公式：模小于可破大卷数的一半，舍；模大于等于破大卷数的一半，进。
		boolean isUpper = remainder.compareTo(breakRollNumDecimal.divide(TWO, 2, RoundingMode.HALF_UP)) > 0;
		if (isUpper) { // 如果进，预计大卷数 = （商 + 1）* 可破大卷数
			return divide.add(BigDecimal.ONE).multiply(breakRollNumDecimal);
		} else { // 如果舍，预计大卷数 = 商 * 可破大卷数
			return divide.multiply(breakRollNumDecimal);
		}
	}

	/**
	 * 计算成型可供时长
	 * 
	 * @param resultVo 排产结果
	 * @param stockVo  库存信息
	 * @return
	 */
	@Override
	public BigDecimal caculateSuppliyTime(XwyyScheduleResultVo resultVo, XwyyStockVo stockVo) {
		// 16点半部件库存量，用于计算可供成型时长
		BigDecimal stockQty = Optional.ofNullable(stockVo).map(XwyyStockVo::getStockQty).orElse(BigDecimal.ZERO);
		// 库存消耗量
		BigDecimal stockConsume = BigDecimal.ZERO;
		BigDecimal supplyTime = BigDecimal.ZERO;
		// 剩余库存，不足以支持8个小时的库存量
		Double remainStock = 0D;

		// 如果中夜班都没有计划，说明是外协规格，成型可供时长也应该是0
		Double dayPlanQty = Optional.ofNullable(resultVo.getDayPlanQty()).orElse(0D);
		Double nightPlanQty = Optional.ofNullable(resultVo.getNightPlanQty()).orElse(0D);
		if (dayPlanQty == 0 && nightPlanQty == 0) {
			return supplyTime;
		}

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
	 * 获取指定日期的半部件库存量
	 * 
	 * @param scheduleDate 库存日期
	 * @Param stockLossRate 库存损耗率
	 * @param isAssistStock 是否使用外厂需求的库存
	 * @return key：大卷编号，value：库存量
	 */
	@Override
	public Map<String, Double> getStockQtyMap(Date scheduleDate, BigDecimal stockLossRate, boolean isAssistStock) {
		// 取出排产日的库存信息
		List<XwyyStockVo> stockList = xwyyEngineStockMapper.selectXwyyStockQty(scheduleDate, stockLossRate);

		// 取出排产日的库存信息
//		if (isAssistStock) {
//			stockList = xwyyEngineStockMapper.selectXwyyAssistStockQty(scheduleDate);
//		} else {
//			stockList = xwyyEngineStockMapper.selectXwyyStockQty(scheduleDate, stockLossRate);
//		}

		Map<String, Double> stockNumMap = stockList.stream().collect(
				Collectors.toMap(XwyyStockVo::getBigRollCode, s -> s.getStockQty().doubleValue(), (v1, v2) -> v1));
		return stockNumMap;
	}

	/**
	 * 获取排产日的16点半部件库存
	 * 
	 * @param scheduleDate 排产日期
	 * @Param stockLossRate 库存损耗率
	 * @param breadth 幅宽
	 * @Param isProductionStage 仅对投产阶段规格排产
	 * @param isAssistStock 是否使用外厂需求的库存
	 * @return key：帘布编号，value：库存量
	 */
	@Override
	public Map<String, XwyyStockVo> getStockMap(Date scheduleDate, BigDecimal stockLossRate, double breadth,
			boolean isProductionStage, boolean isAssistStock) {

		List<XwyyStockVo> stockList;
		// 取出排产日的库存信息
		if (isAssistStock) {
			// 从外厂需求取库存
			stockList = xwyyEngineStockMapper.selectXwyyAssistStock(scheduleDate, breadth, isProductionStage);
		} else {
			// 从库存管理取库存
			// 计算公式： (库存量 - 不良数 + 修正数) - (前日三班计划量 - 12点成型完成量) * 单耗
			stockList = xwyyEngineStockMapper.selectXwyyStock(scheduleDate, stockLossRate, breadth, isProductionStage);
		}

		return stockList.stream()
				.collect(Collectors.toMap(XwyyStockVo::getBigRollCode, Function.identity(), (v1, v2) -> v1));
	}

	/**
	 * 记录算法描述日志
	 * 
	 * @param batchNo
	 */
	private void insertDescriptionLog(String batchNo) {
		String logDetail = logSplit("开始计算中班和晚班计划量", "根据库存重新计算中班计划量dayPlanQty：如果 库存 >= 中班计划量，则 中班计划量 = 0；",
				"库存 < 中班计划量时，如果可供成型时长超过12小时，中班计划量 = （原中班+原晚班）*X-当日库存；时长不足12小时，则中班需求量不变",
				"根据库存重新计算晚班计划量nightPlanQty：如果 库存 >= 中班计划量，则 晚班计划量 = （原中班+原晚班）*X-当日库存；",
				"库存 < 中班计划量时，如果可供成型时长超过12小时，晚班计划量 =  0；时长不足12小时，则晚班计划量 = （原中班+原晚班）*X - 当日库存 - 原中班");
		autoScheduleLogService.insertXwyyScheduleLog(batchNo, null, "3.1、计算各班计划量", logDetail);
	}

	/**
	 * 记录基础资料日志
	 * 
	 * @param assistMap
	 * @param originalLineMap
	 * @param batchNo
	 * @param stockMap
	 * @param lossRateMap
	 * @param noNeedRemaind
	 */
	private void insertBasedataLog(Map<String, XwyyAssistRequirement> assistMap,
			Map<String, XwyyOriginalLineSpec> originalLineMap, String batchNo, Map<String, XwyyStockVo> stockMap,
			Map<String, Double> lossRateMap, Set<String> noNeedRemaind) {
		String logDetail = logSplit("库存量与成型定额：" + toJSONString(stockMap), "损耗率设定：" + toJSONString(lossRateMap),
				"外厂需求规格：" + toJSONString(assistMap), "原线规格配置：" + toJSONString(originalLineMap),
				"大卷原线无需提醒配置：" + toJSONString(noNeedRemaind));
		autoScheduleLogService.insertXwyyScheduleLog(batchNo, "", "3.2、计算排产计划量基础数据日志", logDetail);
	}

	/**
	 * 获取库存损耗率
	 * 
	 * @param paramsMap
	 * @return
	 */
	private BigDecimal getCd90StockLossRate() {
		// 读取库存损耗率
		String stockLossRate = xwyyEngineStockMapper.listCd90Params(EngineConstants.STOCK_LOSS_RATE).stream()
				.findFirst().map(XwyyParamsVo::getParamValue).orElse("0");
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
	private void insertCalculateLog(String oldScheduleResult, XwyyScheduleResultVo scheduleVo) {
		String logDetail = logSplit("开始计算中班和晚班计划量", "计算前排程数据：" + oldScheduleResult,
				"计划量计算好后的排程数据：" + toJSONString(scheduleVo));
		autoScheduleLogService.insertXwyyScheduleLog(scheduleVo.getBatchNo(), scheduleVo.getOrderNo(), "3.3、计算各班计划量",
				logDetail);
	}

	/**
	 * 给排程结果添加对应班次的过程值
	 * 
	 * @param schedule   排程结果
	 * @param msgKey     过程值国际化code
	 * @param dayValue   白班过程值
	 * @param nightValue 夜班过程值
	 */
	private void addDayAndNightProcessValue(XwyyScheduleResultVo schedule, String msgKey, BigDecimal dayValue,
			BigDecimal nightValue) {
		String processValue = I18nUtil.getMessage(msgKey);
		boolean isAdd = false;
		// 中班
		if (dayValue != null && dayValue.compareTo(BigDecimal.ZERO) != 0) {
			String classMsg = I18nUtil.getMessage("ui.data.column.scheduleResult.class.day");
			// 拼接数值，数值都统一向上取整
			processValue += classMsg + dayValue.setScale(0, RoundingMode.UP).toString();
			isAdd = true;
		}
		// 夜班
		if (nightValue != null && nightValue.compareTo(BigDecimal.ZERO) != 0) {
			String classMsg = I18nUtil.getMessage("ui.data.column.scheduleResult.class.night");
			// 如果中班已经
			processValue += isAdd ? PROCESS_VALUE_SEPARATOR : "";
			// 拼接数值，数值都统一向上取整
			processValue += classMsg + nightValue.setScale(0, RoundingMode.UP).toString();
			isAdd = true;
		}
		// 中班或夜班任意一个数值添加成功，则更新过程数值
		if (isAdd) {
			String dayProcessvalue = schedule.getDayProcessValue();
			dayProcessvalue = dayProcessvalue == null ? processValue
					: dayProcessvalue + PROCESS_VALUE_SEPARATOR + processValue;
			schedule.setDayProcessValue(dayProcessvalue);
		}
	}

	/**
	 * 给排程结果添加过程值
	 * 
	 * @param schedule 排程结果
	 * @param msgKey   过程值国际化code
	 * @param value    过程值
	 */
	private void addProcessValue(XwyyScheduleResultVo schedule, String msgKey, BigDecimal value) {
		if (value == null || value.compareTo(BigDecimal.ZERO) == 0) {
			// 数值为0或者为空不需要添加
			return;
		}
		// 拼接数值，数值都统一向上取整
		String processValue = I18nUtil.getMessage(msgKey) + value.setScale(0, RoundingMode.UP).toString();
		String dayProcessvalue = schedule.getDayProcessValue();
		dayProcessvalue = dayProcessvalue == null ? processValue : dayProcessvalue + "，" + processValue;
		schedule.setDayProcessValue(dayProcessvalue);
	}

	/**
	 * 取预生产库存倍数Map
	 * @param codeList 要查询的steelRingCode列表
	 * @param reserveStockRate 预生产库存倍数
	 * @return 结果
	 */
	private Map<String, BigDecimal> getReserveStockMap(List<String> codeList, Double reserveStockRate) {
		List<XwyyReserveStockDto> reserveStockList = new ArrayList<>();
		List<List<String>> splitList = CollectionUtil.splitList(codeList, 500);
		for (List<String> list : splitList) {
			reserveStockList.addAll(xwyyEngineStockMapper.listReserveStock(list));
		}
		if (CollectionUtils.isEmpty(reserveStockList)) {
			return Collections.emptyMap();
		}
		return reserveStockList.stream().collect(Collectors.toMap(XwyyReserveStockDto::getBigRollCode, XwyyReserveStockDto::getReserveStockRate, (v1, v2) -> v1));
	}
}
