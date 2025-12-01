package com.zlt.aps.xwyy.engine.service.impl;

import com.github.pagehelper.util.StringUtil;
import com.ruoyi.api.gateway.system.domain.ImportErrorLog;
import com.ruoyi.common.core.utils.DateUtils;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.utils.StringUtils;
import com.zlt.aps.common.core.constant.ApsConstant;
import com.zlt.aps.common.core.utils.BigDecimalUtil;
import com.zlt.aps.common.core.utils.BigDecimalUtils;
import com.zlt.aps.common.engine.constants.EngineConstants;
import com.zlt.aps.common.engine.domain.EngineConstructionInfo;
import com.zlt.aps.common.engine.mapper.CommonMapper;
import com.zlt.aps.common.engine.service.AutoScheduleLogService;
import com.zlt.aps.common.engine.service.impl.IncrementService;
import com.zlt.aps.common.engine.utils.CollectionUtil;
import com.zlt.aps.common.engine.utils.GenerageMapKeyUtils;
import com.zlt.aps.xwyy.api.domain.dto.XwyyScheduleResultDto;
import com.zlt.aps.xwyy.api.domain.entity.XwyyAssistSpec;
import com.zlt.aps.xwyy.api.domain.entity.XwyyBigRollOriginalBrand;
import com.zlt.aps.xwyy.api.domain.entity.XwyyBigRollRubberCarRelation;
import com.zlt.aps.xwyy.engine.common.XwyyConstants;
import com.zlt.aps.xwyy.engine.mapper.XwyyEngineMapper;
import com.zlt.aps.xwyy.engine.mapper.XwyyEngineStockMapper;
import com.zlt.aps.xwyy.engine.service.*;
import com.zlt.aps.xwyy.engine.vo.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.Map.Entry;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.alibaba.fastjson.JSON.toJSONString;
import static com.zlt.aps.common.core.utils.ApsCommonUtil.*;
import static com.zlt.aps.common.core.utils.ImportUtil.addImportErrorLog;

/**
 * 纤维压延自动排程服务实现类
 *
 * @Description
 * @Author hakimrayn
 * @Date 2021-7-22 10:36:57
 * @Version 1.0
 */
@Service("xwyyEngineService")
@Slf4j
public class XwyyEngineServiceImpl implements XwyyEngineService {
	/**
	 * 幅宽默认值：1.45
	 */
	private final static Double DEFAULT_BREADTH = new Double("1.45");
	/**
	 * 原线可破大卷数默认值：5
	 */
	private static final String DEFAULT_BREAK_ROLL_NUM = "5";
	@Autowired
	private XwyyEngineMapper xwyyEngineMapper;
	@Autowired
	private XwyyEngineStockMapper xwyyEngineStockMapper;
	@Autowired
	private XwyyEnginePlanQtyService xwyyEnginePlanQtyService;
	@Autowired
	private XwyyEngineMonthSurplusService xwyyEngineMonthSurplusService;
	@Autowired
	private XwyyEngineMachineService xwyyEngineMachineService;
	@Autowired
	private XwyyEngineLossService xwyyEngineLossService;
	@Resource
	private IncrementService incrementService;
	@Resource
	private AutoScheduleLogService autoScheduleLogService;
	@Resource
	private CommonMapper commonMapper;
	/**
	 * 生产阶段校验开关状态：打开
	 */
	private final static String PRODUCTION_STAGE_ON = "1";
	/**
	 * 是否使用外协库存计算开关
	 */
	private final static String ASSIST_STOCK_SWITCH = "ASSIST_STOCK_SWITCH";
	/**
	 * 是否使用外协库存计算开关：打开
	 */
	private final static String ASSIST_STOCK_SWITCH_NO = "1";
	/**
	 * 一百，用于百分比 -> 小数的单位换算
	 */
	private final static BigDecimal ONE_HUNDRED = new BigDecimal("100");
    private final static String DEFAULT_MACHINE_QUATA_HOUR = "12"; // 机台产能时长

	/**
	 * 纤维压延自动排程
     *
	 * @Author hakimryan
	 * @Description
	 * @Date 2021-7-22 11:26:33
	 * @Param scheduleDate 排产日期
	 * @Return
	 */
	@Transactional
	@Override
	public void autoXwyySchedule(Date scheduleDate) {
		// 加载系统参数配置
		Map<String, String> paramsMap = this.getParamsMap();
		// 参数：幅宽
		Double breadth = getDoubleOrDefault(paramsMap.get(EngineConstants.BREADTH), DEFAULT_BREADTH);
		// 本次排程批次号
		String batchNo = this.createBatchNo(scheduleDate);
		// 对应的成型批次号
		String cxBatchNo = "";
		// 判断仅对投产阶段规格排产是否打开
		boolean isProductStage = PRODUCTION_STAGE_ON
				.equals(paramsMap.getOrDefault(EngineConstants.PRODUCTION_STAGE_PRODUCE, PRODUCTION_STAGE_ON));
        boolean isBreak = PRODUCTION_STAGE_ON.equals(paramsMap.get("IS_BREAK_ROLL")); // 判断是否打开破大卷计算
        BigDecimal machineQuataHour = BigDecimalUtils.valueOf(paramsMap.getOrDefault(EngineConstants.MACHINE_QUATA_HOUR, DEFAULT_MACHINE_QUATA_HOUR)); // 机台产能时长
        String standardSize = paramsMap.get(EngineConstants.STANDARD_SIZE); // 标准长度

		/* 根据排程日期，从90度裁断排产计划中生成钢带压延排产的信息 */
		List<XwyyScheduleResultVo> scheduleList = xwyyEngineMapper.selectXwyyScheduleBaseList(scheduleDate, breadth,
				isProductStage);
		List<XwyyScheduleResultVo> lastScheduleList = xwyyEngineMapper.selectXwyyScheduleList(DateUtils.addDays(scheduleDate, -1)); // 取上一天计划
		if (CollectionUtils.isEmpty(scheduleList)) { // 今天没有成型计划的情况下尝试取上一天的计划
		    scheduleList = lastScheduleList;
		    scheduleList.stream().forEach(scheduleVo -> {
		        scheduleVo.setId(null);
		        scheduleVo.setScheduleDate(scheduleDate); // 日期改成今天
		        scheduleVo.setBatchNo(null);
		        scheduleVo.setOrderNo(null);
		        scheduleVo.setUpdateBy(null);
                scheduleVo.setUpdateTime(null);
                scheduleVo.setMachineId(null);
		    });
		}

//		List<XwyyScheduleResultVo> scheduleList = new ArrayList<>();
		// 查询本日外厂需求规格信息
		Map<String, XwyyAssistRequirement> assistMap = xwyyEngineMapper.selectAssistRequirement(scheduleDate).stream()
				.collect(Collectors.toMap(XwyyAssistRequirement::getBigRollCode, Function.identity()));
		// 添加额外的外厂需求记录
		this.addExtraAssistPlan(scheduleDate, batchNo, scheduleList, assistMap);
		if (CollectionUtil.isEmpty(scheduleList)) {
			log.info("根据成型排程记录为空，无法生成纤维压延排产");
			autoScheduleLogService.insertXwyyScheduleLog(batchNo, "", "自动排程失败",
					"自动排程失败，原因：成型排程记录为空，或没有在施工信息中找到对应的物料");
			throw new RuntimeException(I18nUtil.getMessage("engine.auto.scheule.tip1"));
		}
		// 返回值为本次排产的外协规格，需要生成对应的外协计划
		Map<String, String> assistSpecMap = this.validatedConstruction(scheduleDate, batchNo, isProductStage);
		// 生成外协计划列表
		List<XwyyScheduleResultVo> scheduleAssistList = this.createScheduleAssistList(scheduleDate, isProductStage,
				scheduleList, batchNo, assistSpecMap, breadth);
		// 记录日志
		autoScheduleLogService.insertXwyyScheduleLog(batchNo, "", "1.1、根据'90度裁断排程记录'统计出纤维压延排程记录基础数据",
				toJSONString(scheduleList));

		// 取出当天已经发布过的排程记录
		Map<String, List<XwyyScheduleResultVo>> isReleaseGroupMap = this.selectIsReleaseScheduleResult(scheduleDate);

		// 取出只有一笔排程的已发布规格
		Map<String, XwyyScheduleResultVo> isReleaseMap = isReleaseGroupMap.entrySet().stream()
				.filter(e -> e.getValue().size() == 1)
				.collect(Collectors.toMap(Entry::getKey, e -> CollectionUtil.firstElement(e.getValue())));

		// 取出有多条排程的已发布规格
		Map<String, List<XwyyScheduleResultVo>> isReleaseGroup = isReleaseGroupMap.entrySet().stream()
				.filter(e -> e.getValue().size() > 1).collect(Collectors.toMap(Entry::getKey, Entry::getValue));

		// 记录日志
		if (!isReleaseGroupMap.isEmpty()) {
			autoScheduleLogService.insertXwyyScheduleLog(batchNo, "", "1、存在上次已发布的排程记录",
					toJSONString(isReleaseGroupMap));
		}

		if (CollectionUtils.isNotEmpty(scheduleList)) {
			// 初始化排程明细，要结合当天已经发布过的排程记录设值
			this.initScheduleReulstList(batchNo, scheduleList, isReleaseMap);
			// 查询出所有原线长度配置
			Map<String, XwyyOriginalLineSpec> originalLineMap = xwyyEngineMapper.selectOriginalLineSpec().stream()
					.collect(Collectors.toMap(XwyyOriginalLineSpec::getOriginalLineCode, Function.identity(),
							(v1, v2) -> v1));
			// 校验原线相关信息
			this.validationOriginal(batchNo, scheduleList);

			/**
			 * 处理日用参考量
			 */
			// 取出排产记录的所有大卷编号
			List<String> bigRollCodeList = scheduleList.stream().map(XwyyScheduleResultVo::getBigRollCode)
					.collect(Collectors.toList());
			// 计算各大卷对应的日用参考值
			Map<String, Double> dayUsedMap = this.caculateDayUsed(scheduleDate, breadth, bigRollCodeList,
					isProductStage);
			// 根据大卷编号更新日用参考量
			scheduleList.stream().forEach(v -> {
				// 只有6厂成型工序有需求的计划才需要设定日用参考，其他外协规格不需要
				if (v.getCxClass1Plan() != null && v.getCxClass1Plan() > 0
						|| v.getCxClass2Plan() != null && v.getCxClass2Plan() > 0
						|| v.getCxClass3Plan() != null && v.getCxClass3Plan() > 0
						|| v.getCxClass4Plan() != null && v.getCxClass4Plan() > 0
						|| v.getCxClass5Plan() != null && v.getCxClass5Plan() > 0) {
					v.setDayUsed(dayUsedMap.getOrDefault(v.getBigRollCode(), 0D));
				} else {
					v.setDayUsed(0D);
				}
			});
			// 记录日志
			autoScheduleLogService.insertXwyyScheduleLog(batchNo, "", "1.2、计算日用参考量",
					"各大卷的日用参考量：" + toJSONString(dayUsedMap));

			/**
			 * 将纤维压延排程安排到具体生产线上
			 */
//			xwyyEngineMachineService.scheduleMachine(scheduleList);

	        /**
             * 计算计划量相关信息，包括库存量、重算计划量
	         */
			BigDecimal stockLossRate = this.getStockLossRate(paramsMap); // 获取库存损耗率
			List<XwyyStockVo> stockList = xwyyEngineStockMapper.selectXwyyStockQty(scheduleDate, stockLossRate);
            if (CollectionUtils.isEmpty(stockList) || stockList.stream().allMatch(s -> ApsConstant.STATUS_ENABLE.equals(s.getEstimateStockFlag()))) {// 判断当天是否有库存，或者全部库存都是预估库存的
                if (PRODUCTION_STAGE_ON.equals(paramsMap.getOrDefault(EngineConstants.ESTIMATE_STOCK_SWITCH, PRODUCTION_STAGE_ON))) { // 判断预估库存开关
                    xwyyEngineStockMapper.estimateStock(DateUtils.addDays(scheduleDate, -1)); // 根据上一天计划及其对应库存预估当天库存
                }
            }
            Map<String, Double> lastPlanQtyMap = lastScheduleList.stream().collect(Collectors.groupingBy(XwyyScheduleResultVo::getBigRollCode, Collectors.summingDouble(XwyyScheduleResultVo::getDayPlanQty)));
            scheduleList.stream().forEach(scheduleVo -> scheduleVo.setLastPlanQty(lastPlanQtyMap.getOrDefault(scheduleVo.getBigRollCode(), 0D)));
			xwyyEnginePlanQtyService.calculateSchedulePlanQty(scheduleDate, scheduleList, assistMap, originalLineMap,
					stockLossRate, breadth, isProductStage, isBreak);

			/**
			 * 根据月度计划量设置收尾备注等信息
			 */
			String closeOutNum = paramsMap.get(EngineConstants.CLOSE_OUT_NUM);
			xwyyEngineMonthSurplusService.calculateMonthSurplus(scheduleDate, scheduleList, closeOutNum);

			// 计算胶料车数
			this.caculateRubberCarNumber(scheduleList);
			// 设置原线品牌数
			this.caculateOriginalLineBrandNum(scheduleList, originalLineMap);
			// 计算原线卷数
//			Long defaultBreakRoll = new Long(
//					paramsMap.getOrDefault(EngineConstants.XWYY_BREAK_ROLL_NUM, DEFAULT_BREAK_ROLL_NUM)); // 原线默认可破大卷数
//			this.caculateOriginalLineQtyNumAuto(scheduleList, originalLineMap, defaultBreakRoll);
		}

		// 根据机台产能选择机台
		xwyyEngineMachineService.chooseMachineByCapacity(scheduleList, machineQuataHour);

		// 如果还存在上次已发布的规格但本次没有排程的规格，直接将这些规格的排程信息复制到本次排程中
		for (XwyyScheduleResultVo scheduleVo : isReleaseMap.values()) {
			scheduleVo.setBatchNo(batchNo);
			scheduleVo.setBaseVale(null);
			scheduleList.add(scheduleVo);
		}

		// 处理一个已发布规格有多个排程的情况
		this.removeGroupSchedule(batchNo, scheduleList, isReleaseGroup);

		// 赋值排产顺序
		this.calculateProduceOrder(scheduleList);

		// 将当天的历史排程记录（上一次的）转移至日志表
		this.cleanHistoryScheduleResult(scheduleDate);

		/**
		 * 创建自动排产记录
		 */
		this.createScheduleRecord(scheduleDate, cxBatchNo, batchNo);

		/**
		 * 创建排产结果明细记录
		 */
		if (CollectionUtils.isNotEmpty(scheduleList)) {
			xwyyEngineMapper.insertScheduleResultList(scheduleList);
		}

		/**
		 * 创建外协排产结果明细记录
		 */
		if (CollectionUtils.isNotEmpty(scheduleAssistList)) {
			xwyyEngineMapper.insertScheduleAssistList(scheduleAssistList);
		}

		/**
		 * 记录日志
		 */
		autoScheduleLogService.insertXwyyScheduleLog(batchNo, "", "最终排产结果", toJSONString(scheduleList));
	}

	/**
	 * 计算各个班次的排产顺序
	 *
	 * @param scheduleList 排程结果
	 */
	private void calculateProduceOrder(List<XwyyScheduleResultVo> scheduleList) {
		// 添加日志
		String batchNo = CollectionUtil.firstElement(scheduleList).getBatchNo();
		autoScheduleLogService.insertCd15ScheduleLog(batchNo, "", "设置生产顺序字段",
				"按机台、大卷分组，再组内按库存供应时长(从小到大)，设置班次的生产顺序（有计划量的才设置生产顺序）");
		// 先按机台、大卷分组
		Map<String, List<XwyyScheduleResultVo>> resultMap = scheduleList.stream()
				// 过滤掉无机台与多机台的情况
				.filter(r -> StringUtils.isNotBlank(r.getMachineId()) && !r.getMachineId().contains(","))
				// 按机台ID，大卷编号分组
				.collect(Collectors.groupingBy(r -> GenerageMapKeyUtils.createMapKey(r.getMachineId())));

		// 组内单独排序
		for (List<XwyyScheduleResultVo> resultList : resultMap.values()) {
			// 排产结果排序
			List<XwyyScheduleResultVo> sortScheduleList = resultList.stream().sorted(Comparator
					// 先按供应时长正序排序，没有供应时长（插单）的放最后
					.comparing(XwyyScheduleResultVo::getSupplyTime, Comparator.nullsLast(Double::compareTo))
					// 如果供应时长相等，则按开始班次正序排序
					.thenComparing(this.createProductClassSorter())
					// 如果开始班次相等，则按该班次的成型排程量倒序排序
					.thenComparing(this.createCxPlanNumSorter())).collect(Collectors.toList());

			// 中班与晚班生产顺序分开，初始值为1；
			int daySortNumer = 1;
			int nightSortNumber = 1;
			for (XwyyScheduleResultVo resultVo : sortScheduleList) {
				// 只有有排计划量的排程才需要设置生产顺序
				if (resultVo.getDayPlanQty() != null && resultVo.getDayPlanQty() > 0) {
					resultVo.setDayProduceOrder(daySortNumer);
					daySortNumer++;
				} else {
					// 为零的时候需要清空（导入的情况下）
					resultVo.setDayProduceOrder(null);
				}
				if (resultVo.getNightFinishQty() != null && resultVo.getNightFinishQty() > 0) {
					resultVo.setNightProduceOrder(nightSortNumber);
					nightSortNumber++;
				} else {
					// 为零的时候需要清空（导入的情况下）
					resultVo.setNightProduceOrder(null);
				}
			}
		}
	}

	/**
	 * 获取本规格的开始生产班次
	 *
	 * @param result
	 * @return
	 */
	private int getStartClass(XwyyScheduleResultVo result) {
		int startClass = 1;
		// 从1班开始遍历每个班次计划量，大于0的即为开始生产班次
		if (Optional.ofNullable(result.getCxClass1Plan()).orElse(0d) > 0) {
			return startClass;
		}
		startClass++;
		if (Optional.ofNullable(result.getCxClass2Plan()).orElse(0d) > 0) {
			return startClass;
		}
		startClass++;
		if (Optional.ofNullable(result.getCxClass3Plan()).orElse(0d) > 0) {
			return startClass;
		}
		startClass++;
		if (Optional.ofNullable(result.getCxClass4Plan()).orElse(0d) > 0) {
			return startClass;
		}
		return ++startClass;
	}

	/**
	 * 创建查询结果排序器——成产班次<br/>
	 * 排序规则：按开始班次正序排序
	 *
	 * @return
	 */
	private Comparator<XwyyScheduleResultVo> createProductClassSorter() {
		return new Comparator<XwyyScheduleResultVo>() {
			@Override
			public int compare(XwyyScheduleResultVo o1, XwyyScheduleResultVo o2) {
				// 新排程结果的开始班次
				int startClass1 = getStartClass(o1);
				// 原排程结果的开始班次
				int startClass2 = getStartClass(o2);
				// 开始班次较小的在前
				return Integer.valueOf(startClass1).compareTo(startClass2);
			}
		};
	}

	/**
	 * 创建查询结果排序器——成型计划量<br/>
	 * 排序规则：按同班次的成型计划量倒序排序
	 *
	 * @return
	 */
	private Comparator<XwyyScheduleResultVo> createCxPlanNumSorter() {
		return new Comparator<XwyyScheduleResultVo>() {
			@Override
			public int compare(XwyyScheduleResultVo o1, XwyyScheduleResultVo o2) {
				// 新排程结果的开始生产班次的计划量
				double cxPlanNum1 = getCxPlanNum(o1);
				// 原排程结果的开始生产班次的计划量
				double cxPlanNum2 = getCxPlanNum(o2);
				// 计划量较大的在前
				return Double.valueOf(cxPlanNum2).compareTo(cxPlanNum1);
			}
		};
	}

	/**
	 * 获取本规格开始生产那一班的计划量
	 *
	 * @param result
	 * @return
	 */
	private double getCxPlanNum(XwyyScheduleResultVo result) {
		// 从1班开始遍历每个班次计划量，取出第一个计划量大于0的数值
		double planNum = Optional.ofNullable(result.getCxClass1Plan()).orElse(0d);
		if (planNum > 0) {
			return planNum;
		}
		planNum = Optional.ofNullable(result.getCxClass2Plan()).orElse(0d);
		if (planNum > 0) {
			return planNum;
		}
		planNum = Optional.ofNullable(result.getCxClass3Plan()).orElse(0d);
		if (planNum > 0) {
			return planNum;
		}
		planNum = Optional.ofNullable(result.getCxClass4Plan()).orElse(0d);
		if (planNum > 0) {
			return planNum;
		}
		return Optional.ofNullable(result.getCxClass5Plan()).orElse(0d);
	}

	/**
	 * 获取库存损耗率
     *
	 * @param paramsMap
	 * @return
	 */
	private BigDecimal getStockLossRate(Map<String, String> paramsMap) {
		// 库存损耗率
		String stockLossRate = paramsMap.getOrDefault(EngineConstants.STOCK_LOSS_RATE, "0");
		// 类型转换，单位也需要转换：百分比 -> 小数
		BigDecimal stockLossRateNum = new BigDecimal(stockLossRate);
		// 计算公式：(100 - 损耗率) / 100
		BigDecimal resultRate = ONE_HUNDRED.subtract(stockLossRateNum).divide(ONE_HUNDRED);
		return resultRate.compareTo(BigDecimal.ZERO) > 0 ? resultRate : BigDecimal.ZERO;
	}

	/**
	 * 处理一个已发布规格有多个排程的情况
     *
	 * @param batchNo        排程批次号
	 * @param scheduleList   排程明细列表
	 * @param isReleaseGroup 已发布多笔的规格排程列表
	 */
	private void removeGroupSchedule(String batchNo, List<XwyyScheduleResultVo> scheduleList,
			Map<String, List<XwyyScheduleResultVo>> isReleaseGroup) {
		for (Entry<String, List<XwyyScheduleResultVo>> entry : isReleaseGroup.entrySet()) {
			String bigRollCode = entry.getKey();
			List<XwyyScheduleResultVo> resultList = entry.getValue();
			// 取出本次排程该规格的排程记录
			XwyyScheduleResultVo result = scheduleList.stream().filter(s -> bigRollCode.equals(s.getBigRollCode()))
					.findFirst().orElse(null);
			if (result != null) {
				// 将其从排程记录中移除掉
				scheduleList.remove(result);
				// 将上次的多条排程记录全部复制过来，同时在备注添加信息：重排后中班计划量:xxx，夜班计划量:xxx
				for (XwyyScheduleResultVo oldResult : resultList) {
					oldResult.setBatchNo(batchNo);
					oldResult.setBaseVale(null);
					String tip = I18nUtil.getMessage("reschedule.double.spec.remark");
					oldResult.setRemark(StringUtils.format(tip, result.getDayPlanQty(), result.getNightPlanQty()));
					scheduleList.add(oldResult);
				}
			}
		}
	}

	/**
	 * 取出当天已经发布过的排程记录
     *
	 * @param scheduleDate 排产日
	 * @return
	 */
	private Map<String, List<XwyyScheduleResultVo>> selectIsReleaseScheduleResult(Date scheduleDate) {
		// 取出本次自动排程前已发布的规格以及其工单号，组成map<大卷编号，工单号>，用于判断是否需要保留
		Map<String, List<XwyyScheduleResultVo>> isReleaseMap = xwyyEngineMapper.selectXwyyScheduleList(scheduleDate)
				.stream()
				// 过滤出曾经发布成功的记录
				.filter(v -> v.getPublishSuccessCount() != null && v.getPublishSuccessCount() > 0)
				// 取出钢带表编号
				.collect(Collectors.groupingBy(XwyyScheduleResultVo::getBigRollCode));
		return isReleaseMap;
	}

	/**
	 * 初始化排程明细
     *
	 * @param batchNo      排程批次号
	 * @param scheduleList 排程明细列表
	 * @param isReleaseMap 已发布排程列表
	 */
	private void initScheduleReulstList(String batchNo, List<XwyyScheduleResultVo> scheduleList,
			Map<String, XwyyScheduleResultVo> isReleaseMap) {
		// 为本次的排产记录创建批次号与工单号
		for (XwyyScheduleResultVo scheduleVo : scheduleList) {
			String bigRollCode = scheduleVo.getBigRollCode();
			scheduleVo.setBatchNo(batchNo);
			// 先判断该帘布是否之前已经发布过
			if (isReleaseMap.containsKey(bigRollCode)) {
				// 发布过，则取出该规格（从map中移除）
				XwyyScheduleResultVo isReleaseVo = isReleaseMap.remove(bigRollCode);
				// 复制工单号
				scheduleVo.setOrderNo(isReleaseVo.getOrderNo());
				// 复制机台信息
				scheduleVo.setMachineId(isReleaseVo.getMachineId());
				// 复制发布次数
				scheduleVo.setPublishSuccessCount(isReleaseVo.getPublishSuccessCount());
				// 复制发布时间
				scheduleVo.setNewestPublishTime(isReleaseVo.getNewestPublishTime());
				// 发布状态为待发布
				scheduleVo.setIsRelease(ApsConstant.WAIT_RELEASING);
			} else {
				// 创建工单号
				scheduleVo.setOrderNo(this.createOrderNo(batchNo));
				scheduleVo.setPublishSuccessCount(0);
				scheduleVo.setIsRelease(ApsConstant.NO_RELEASE);
			}
			scheduleVo.setDataSource(EngineConstants.SCHEDULE_DATA_SOURCE_AUTO);
			scheduleVo.setExtraPlanFlag(XwyyConstants.EXTRA_PLAN_FLAG_NO);
			scheduleVo.setOriginalRemindFlag(XwyyConstants.ORIGINAL_REMIND_FLAG_NO);
			scheduleVo.setBaseVale(null);
		}
	}

	/**
	 * 校验原线相关信息
     *
	 * @param batchNo      批次号
	 * @param scheduleList 排程记录
	 */
	private void validationOriginal(String batchNo, List<XwyyScheduleResultVo> scheduleList) {
		for (XwyyScheduleResultVo result : scheduleList) {
			String originalLineCode = result.getOriginalLineCode();
			if (StringUtils.isEmpty(originalLineCode)) {
				String bigRollCode = result.getBigRollCode();
				String logMsg = "帘布大卷：{} 没有原线代码！";
				autoScheduleLogService.insertXwyyScheduleLog(batchNo, "", "自动排程失败",
						StringUtils.format(logMsg, bigRollCode));
				String errorMsg = StringUtils
						.format(I18nUtil.getMessage("mes.error.message.auto.schedule.noOriginalLine"), bigRollCode);
				throw new RuntimeException(errorMsg);
			}
		}
	}

	/**
	 * 添加额外的外厂需求记录
     *
	 * @param scheduleDate 排产日
	 * @param batchNo      批次好
	 * @param scheduleList 排产记录，调用前只有6厂的记录
	 * @param assistMap    外厂需求规格信息
	 */
	private void addExtraAssistPlan(Date scheduleDate, String batchNo, List<XwyyScheduleResultVo> scheduleList,
			Map<String, XwyyAssistRequirement> assistMap) {
		Set<String> bigRollCodeSet = scheduleList.stream().map(XwyyScheduleResultVo::getBigRollCode)
				.collect(Collectors.toSet());
		// 将外协规格补入排程计划中
		for (Entry<String, XwyyAssistRequirement> entry : assistMap.entrySet()) {
			String bigRollCode = entry.getKey();
			// 如果外厂需求中的规格再排程计划没有，需要将该规格补进计划中
			if (!bigRollCodeSet.contains(bigRollCode)) {
				XwyyAssistRequirement assist = entry.getValue();
				XwyyScheduleResultVo scheduleVo = new XwyyScheduleResultVo();
				scheduleVo.setBigRollCode(bigRollCode);
				scheduleVo.setScheduleDate(scheduleDate);
				scheduleVo.setOriginalLineCode(assist.getOriginalLineCode());
				scheduleVo.setBatchNo(batchNo);
				scheduleVo.setOrderNo(this.createOrderNo(batchNo));
				scheduleVo.setIsRelease(ApsConstant.NO_RELEASE);
				scheduleVo.setExtraPlanFlag(XwyyConstants.EXTRA_PLAN_FLAG_NO);
				scheduleVo.setOriginalRemindFlag(XwyyConstants.ORIGINAL_REMIND_FLAG_NO);
				// 需求量先留空，在计算库存的时候再统计计算
				scheduleVo.setDayPlanQty(0D);
				scheduleVo.setNightPlanQty(0D);
				scheduleVo.setBaseVale(null);
				// 添加外协计划
				scheduleList.add(scheduleVo);
			}
		}
	}

	/**
	 * 纤维压延插单
     *
	 * @Author hakimryan
	 * @Description
	 * @Date 2021-7-29 09:31:42
	 * @param scheduleResult 插单记录
	 */
	@Transactional
	@Override
	public int insertXwyyOrder(XwyyScheduleResultDto scheduleResult) {
		// 排程日期
		Date scheduleDate = scheduleResult.getScheduleDate();
		// 查询当前排程的批次号
		String batchNo = xwyyEngineMapper.getCurrentBatchNo(scheduleDate);
		if (StringUtils.isBlank(batchNo)) {
			// 当前的批次号为空，则新生成排程批次号
			batchNo = this.createBatchNo(scheduleDate);
			// 创建自动排程记录
			this.createScheduleRecord(scheduleDate, "", batchNo);
		}

		List<XwyyScheduleResultDto> scheduleList = new ArrayList<>();
		scheduleResult.setOriginalLineQtyNum(BigDecimal.ZERO.toString());
		scheduleList.add(scheduleResult);
		// 导入异常记录
		List<ImportErrorLog> importErrorLogs = new ArrayList<>();
		int result = this.batchUpdateOrInsertXwyySchedule(batchNo, scheduleDate, scheduleList, importErrorLogs, false);
		// 如果插单有产生异常记录，则向前端返回异常信息
		importErrorLogs.stream().findAny().ifPresent(v -> {
			throw new RuntimeException(v.getErrorDetail());
		});
		return result;
	}

	/**
	 * 批量导入纤维压延排程记录
     *
	 * @param scheduleDate 排程日志
	 * @param scheduleList 排程数据
	 * @return 导入异常日志
	 */
	@Transactional
	@Override
	public List<ImportErrorLog> batchSaveXwyySchedule(Date scheduleDate, List<XwyyScheduleResultDto> scheduleList) {
		// 批量导入前要先清除历史数据
		this.cleanHistoryScheduleResult(scheduleDate);
		// 创建新排程记录，批次号也重新生成
		String batchNo = this.createBatchNo(scheduleDate);
		// 导入异常记录
		List<ImportErrorLog> importErrorLogs = new ArrayList<>();
		// 生成新的排程明细
		this.batchUpdateOrInsertXwyySchedule(batchNo, scheduleDate, scheduleList, importErrorLogs, true);
		// 生成新批次时，如果明细能关联到成型信息，则也需要将成型信息保存到批次记录中
		String cxBatchNo = "";
		for (XwyyScheduleResultDto schedule : scheduleList) {
			if (StringUtils.isNotEmpty(schedule.getCxBatchNo())) {
				cxBatchNo = schedule.getCxBatchNo();
				break;
			}
		}
		this.createScheduleRecord(scheduleDate, cxBatchNo, batchNo);
		return importErrorLogs;
	}

	/**
	 * 批量保存纤维压延排程记录
     *
	 * @param batchNo         批次号
	 * @param scheduleDate    排程日志
	 * @param scheduleList    排程数据
	 * @param importErrorLogs 导入异常日志
	 * @param isImport        是否导入
	 */
	private int batchUpdateOrInsertXwyySchedule(String batchNo, Date scheduleDate,
			List<XwyyScheduleResultDto> scheduleList, List<ImportErrorLog> importErrorLogs, boolean isImport) {
		// 加载系统参数配置
		Map<String, String> paramsMap = this.getParamsMap();
		// 是否使用外协库存
		boolean isAssistStock = ASSIST_STOCK_SWITCH_NO.equals(paramsMap.get(ASSIST_STOCK_SWITCH));
		// 月度计划信息
		Map<String, XwyyMonthSurplusVo> monthSurplusMap = xwyyEngineMonthSurplusService
				.getMonthSurplusMap(scheduleDate);
		// 参数：收尾提醒阈值
		String closeOutNum = paramsMap.get(EngineConstants.CLOSE_OUT_NUM);
		// 参数：幅宽
		Double breadth = getDoubleOrDefault(paramsMap.get(EngineConstants.BREADTH), DEFAULT_BREADTH);
		// 判断仅对投产阶段规格排产是否打开
		boolean isProductStage = PRODUCTION_STAGE_ON
				.equals(paramsMap.getOrDefault(EngineConstants.PRODUCTION_STAGE_PRODUCE, PRODUCTION_STAGE_ON));
		// 取出本次导入排产记录所有的大卷编号
		List<String> bigRollCodeList = scheduleList.stream().map(XwyyScheduleResultDto::getBigRollCode)
				.collect(Collectors.toList());
		// 计算各大卷日用参考量
		Map<String, Double> dayUsedMap = this.caculateDayUsed(scheduleDate, breadth, bigRollCodeList, false);
		// 获取库存损耗率
		BigDecimal stockLossRate = this.getStockLossRate(paramsMap);
		// 16点半部件库存
		Map<String, XwyyStockVo> stockMap = xwyyEnginePlanQtyService.getStockMap(scheduleDate, stockLossRate, breadth,
				isProductStage, isAssistStock);
		// 前日库存信息，即昨天的库存信息
		Map<String, Double> yesStockNumMap = xwyyEnginePlanQtyService
				.getStockQtyMap(DateUtils.addDays(scheduleDate, -1), stockLossRate, isAssistStock);
		// 插单基础信息
		Map<String, XwyyScheduleResultVo> baseInfoMap = xwyyEngineMapper
				.listInsertOrderBaseInfo(scheduleList, scheduleDate, breadth).stream()
				.collect(Collectors.toMap(XwyyScheduleResultVo::getBigRollCode, Function.identity()));
		// 查询出所有原线长度配置
		Map<String, XwyyOriginalLineSpec> originalLineMap = xwyyEngineMapper.selectOriginalLineSpec().stream().collect(
				Collectors.toMap(XwyyOriginalLineSpec::getOriginalLineCode, Function.identity(), (v1, v2) -> v1));
		// 纤维大卷长度配置
		Map<String, BigDecimal> xwyyBigRollMap = xwyyEngineStockMapper.listXwyyBigRoll().stream()
				.collect(Collectors.toMap(XwyyBigRollVo::getBigRollCode, XwyyBigRollVo::getClothLength));
		// 纤维大卷标准长度
		String standardSize = paramsMap.get(EngineConstants.STANDARD_SIZE);
		// 收尾规格列表
		List<String> closeOutSpecList = xwyyEngineStockMapper.listCloseOutSpec(scheduleDate, isProductStage);

		// 添加日志
		String logDetail = logSplit("月度计划map：:" + toJSONString(monthSurplusMap), "插单基础信息map：" + baseInfoMap,
				"当日库存map：" + stockMap, "前日库存map" + yesStockNumMap, "系统参数收尾提醒阈值：" + closeOutNum, "系统参数幅宽：" + breadth,
				"日用参考量map：" + dayUsedMap);
		autoScheduleLogService.insertXwyyScheduleLog(batchNo, "", "插单或批量导入基础数据", logDetail);

		// 批量导入的初始行号，由于模板是从第3行开始是数据行，因此初始值是2
		int rowNum = 2;
		List<XwyyScheduleResultDto> newScheduleList = new ArrayList<>();
		// 遍历入参排程记录
		for (XwyyScheduleResultDto scheduleResult : scheduleList) {
			rowNum++;
			// 大卷编号
			String bigRollCode = scheduleResult.getBigRollCode();
			// 取出插单基础信息
			XwyyScheduleResultVo scheduleVo = baseInfoMap.get(bigRollCode);
			if (scheduleVo == null) {
				// 取不到说明物料编号有误，需要记录错误信息
				addImportErrorLog(null, rowNum, I18nUtil.getMessage("ui.error.message.column.materialCodeNotExist"),
						importErrorLogs);
				continue;
			}

			XwyyStockVo stockVo = stockMap.get(bigRollCode);
			// 16点半部件库存量
			BigDecimal stockQty = stockVo != null && stockVo.getTodayStock() != null ? stockVo.getTodayStock()
					: BigDecimal.ZERO;
			// 大卷长度
			BigDecimal clothLength = xwyyBigRollMap.getOrDefault(bigRollCode, new BigDecimal(standardSize));

			scheduleResult.setBatchNo(batchNo);
			scheduleResult.setOrderNo(this.createOrderNo(batchNo));
			scheduleResult.setTodayStock(stockQty.setScale(0, RoundingMode.DOWN).doubleValue());
			scheduleResult.setYesStock(Math.floor(yesStockNumMap.getOrDefault(bigRollCode, 0D)));
			scheduleResult.setDayPlanQty(Optional.ofNullable(scheduleResult.getDayPlanQty()).orElse(0D));
			scheduleResult.setDayPlanQtyNum(this.getDayPlanQtyNum(scheduleResult, clothLength));
			scheduleResult.setNightPlanQty(Optional.ofNullable(scheduleResult.getNightPlanQty()).orElse(0D));
			scheduleResult.setNightPlanQtyNum(this.getNightPlanQtyNum(scheduleResult, clothLength));
			if (scheduleResult.getCxClass1Plan() != null && scheduleResult.getCxClass1Plan() > 0
					|| scheduleResult.getCxClass2Plan() != null && scheduleResult.getCxClass2Plan() > 0
					|| scheduleResult.getCxClass3Plan() != null && scheduleResult.getCxClass3Plan() > 0
					|| scheduleResult.getCxClass4Plan() != null && scheduleResult.getCxClass4Plan() > 0
					|| scheduleResult.getCxClass5Plan() != null && scheduleResult.getCxClass5Plan() > 0) {
				scheduleResult.setDayUsed(dayUsedMap.getOrDefault(bigRollCode, 0D));
			} else {
				scheduleResult.setDayUsed(0D);
			}
			scheduleResult.setIsRelease(ApsConstant.NO_RELEASE);
			scheduleResult.setExtraPlanFlag(XwyyConstants.EXTRA_PLAN_FLAG_NO);
			scheduleResult.setOriginalRemindFlag(XwyyConstants.ORIGINAL_REMIND_FLAG_NO);
			scheduleResult.setCloseOutSpecFlag(
					closeOutSpecList.contains(bigRollCode) ? ApsConstant.STATUS_ENABLE : ApsConstant.STATUS_DISABLE);
			scheduleResult.setDelFlag(ApsConstant.DEL_FLAG_NORMAL);
			// 总计划量合计值
			Double dayPlanQty = scheduleResult.getDayPlanQty();
			Double nightPlanQty = scheduleResult.getNightPlanQty();
			scheduleResult.setTotalPlan(dayPlanQty + nightPlanQty);
			scheduleResult.setTotalPlanNum(BigDecimal.valueOf(scheduleResult.getDayPlanQtyNum())
					.add(BigDecimal.valueOf(scheduleResult.getNightPlanQtyNum())));

			// 通过接口获取设置收尾提示标识 和 生产状态字段
			scheduleVo.setDayUsed(scheduleResult.getDayUsed());
			xwyyEngineMonthSurplusService.setStatusAndCloseTip(scheduleVo, monthSurplusMap.get(bigRollCode),
					closeOutNum);
			scheduleResult.setProductionStatus(scheduleVo.getProductionStatus());
			scheduleResult.setMarkCloseOutTip(scheduleVo.getMarkCloseOutTip());
			scheduleResult.setOriginalLineCode(scheduleVo.getOriginalLineCode());

			// 如果是导入功能，需要设置成型相关信息
			if (EngineConstants.SCHEDULE_DATA_SOURCE_IMPORT.equals(scheduleResult.getDataSource())) {
				scheduleResult.setCxBatchNo(scheduleVo.getCxBatchNo());
				scheduleResult.setCxClass1Plan(scheduleVo.getCxClass1Plan());
				scheduleResult.setCxClass2Plan(scheduleVo.getCxClass2Plan());
				scheduleResult.setCxClass3Plan(scheduleVo.getCxClass3Plan());
				scheduleResult.setCxClass4Plan(scheduleVo.getCxClass4Plan());
				scheduleResult.setCxClass5Plan(scheduleVo.getCxClass5Plan());
				// 赋值计划量，避免成型无计划量时查询的计划量也为空的情况
				scheduleVo.setDayPlanQty(scheduleResult.getDayPlanQty());
				scheduleVo.setNightPlanQty(scheduleResult.getNightPlanQty());
				// 成型可供时长
				BigDecimal supplyTime = xwyyEnginePlanQtyService.caculateSuppliyTime(scheduleVo, stockVo);
				scheduleResult.setSupplyTime(supplyTime.doubleValue());
			}

			if (!isImport) {
				// 如果是插单，需要设置原线相关信息
				List<XwyyScheduleResultVo> resultList = xwyyEngineMapper.selectXwyyScheduleList(scheduleDate);
				String maxBigRollCode = resultList.stream()
						.filter(r -> scheduleVo.getOriginalLineCode().equals(r.getOriginalLineCode()))
						.map(XwyyScheduleResultVo::getBigRollCode).max((code1, code2) -> code1.compareTo(code2))
						.orElse("");
				scheduleResult.setMaxBigRollCode(maxBigRollCode);
				// 原线编号
				String originalLineCode = scheduleResult.getOriginalLineCode();
				// 原线长度
				BigDecimal originalLineLength = Optional.ofNullable(originalLineMap.get(originalLineCode))
						.map(XwyyOriginalLineSpec::getOriginalLineLength).orElse(null);
				if (originalLineLength == null) {
					// 取不到说明原线规格没有配置原线长度，需要记录错误信息
					String errorMsg = StringUtils.format(
							I18nUtil.getMessage("mes.error.message.auto.schedule.originalLine.lengthError"),
							originalLineCode);
					addImportErrorLog(null, rowNum, errorMsg, importErrorLogs);
					continue;
				}
				scheduleResult.setOriginalLineLength(originalLineLength.toString());

				// 设置原线品牌数
				this.caculateOriginalLineBrandNumInsert(scheduleResult, originalLineMap);
			}
			newScheduleList.add(scheduleResult);
		}

		// 计算胶料车数
		this.caculateRubberCarNumberBatchSave(scheduleList);

		if (isImport) {
			// 如果是导入，需要计算原线卷数
			Long defaultBreakRoll = new Long(
					paramsMap.getOrDefault(EngineConstants.XWYY_BREAK_ROLL_NUM, DEFAULT_BREAK_ROLL_NUM)); // 原线默认可破大卷数
			// 插单的原线卷数在service端修改
			this.caculateOriginalLineQtyNum(scheduleList, originalLineMap, defaultBreakRoll);
			// 设置原线品牌数
			this.caculateOriginalLineBrandNumImport(scheduleList, originalLineMap);
		}

		// 记录日志
		logDetail = logSplit("插单数据：" + toJSONString(newScheduleList), "异常情况：" + importErrorLogs);
		autoScheduleLogService.insertXwyyScheduleLog(batchNo, "", "插单或导入排程最终数据", logDetail);
		if (newScheduleList.isEmpty()) {
			// 如果没有一条导入成功，返回失败标志
			return -1;
		}
		// 批量创建插单排产记录
		return xwyyEngineMapper.mergeXwyyScheduleResult(scheduleList);
	}

	/**
	 * 获取日计划量个数
     *
	 * @param scheduleResult
	 * @param clothLength
	 * @return
	 */
	private Double getDayPlanQtyNum(XwyyScheduleResultDto scheduleResult, BigDecimal clothLength) {
		Double planQty = Optional.ofNullable(scheduleResult.getDayPlanQty()).orElse(0D);
		Double planQtyNum = Optional.ofNullable(scheduleResult.getDayPlanQtyNum()).orElse(0D);
		if (planQty != 0D && planQtyNum == 0D) {
			return new BigDecimal(String.valueOf(planQty)).divide(clothLength, 1, RoundingMode.UP).doubleValue();
		} else if (planQty == 0D && planQtyNum != 0D) {
			return 0D;
		} else {
			return planQtyNum;
		}
	}

	/**
	 * 获取日计划量个数
     *
	 * @param scheduleResult
	 * @param clothLength
	 * @return
	 */
	private Double getNightPlanQtyNum(XwyyScheduleResultDto scheduleResult, BigDecimal clothLength) {
		Double planQty = Optional.ofNullable(scheduleResult.getNightPlanQty()).orElse(0D);
		Double planQtyNum = Optional.ofNullable(scheduleResult.getNightPlanQtyNum()).orElse(0D);
		if (planQty != 0D && planQtyNum == 0D) {
			return new BigDecimal(String.valueOf(planQty)).divide(clothLength, 1, RoundingMode.UP).doubleValue();
		} else if (planQty == 0D && planQtyNum != 0D) {
			return 0D;
		} else {
			return planQtyNum;
		}
	}

	/**
	 * 计算胶料车数
     *
	 * @param scheduleList
	 */
	private void caculateRubberCarNumber(List<XwyyScheduleResultVo> scheduleList) {
		// 查询大卷的胶料配置
		Map<String, XwyyBigRollRubberCarRelation> rubberMap = xwyyEngineMapper.selectXwyyBigRollRubCarRelation()
				.stream().collect(Collectors.toMap(XwyyBigRollRubberCarRelation::getBigRollCode, Function.identity()));
		for (XwyyScheduleResultVo result : scheduleList) {
			// 通过大卷编号取出胶料配置
			XwyyBigRollRubberCarRelation rubber = rubberMap.get(result.getBigRollCode());
			if (rubber != null) {
				// 设置胶料编号以及胶料车数
				result.setRubberCode(rubber.getRubberCode());
				BigDecimal carNumber = Optional.ofNullable(rubber.getCarNumber()).orElse(BigDecimal.ZERO);
				Double totalPlanNumber = BigDecimalUtil.add(result.getDayPlanQtyNum(), result.getNightPlanQtyNum());
				result.setRubberCarNumber(carNumber.multiply(new BigDecimal(String.valueOf(totalPlanNumber))));
			}
		}
	}

	/**
	 * 批量保存时计算胶料车数
     *
	 * @param scheduleList
	 */
	private void caculateRubberCarNumberBatchSave(List<XwyyScheduleResultDto> scheduleList) {
		List<XwyyScheduleResultVo> scheduleVoList = new ArrayList<>();
		for (XwyyScheduleResultDto s : scheduleList) {
			XwyyScheduleResultVo vo = new XwyyScheduleResultVo();
			vo.setBigRollCode(s.getBigRollCode());
			vo.setDayPlanQtyNum(s.getDayPlanQtyNum());
			vo.setNightPlanQtyNum(s.getNightPlanQtyNum());
			scheduleVoList.add(vo);
		}
		this.caculateRubberCarNumber(scheduleVoList);
		for (int i = 0, size = scheduleList.size(); i < size; i++) {
			XwyyScheduleResultDto scheduleDto = scheduleList.get(i);
			XwyyScheduleResultVo scheduleVo = scheduleVoList.get(i);
			scheduleDto.setRubberCarNumber(scheduleVo.getRubberCarNumber());
			scheduleDto.setRubberCode(scheduleVo.getRubberCode());
		}
	}

	/**
	 * 计算原线品牌数
     *
	 * @param scheduleList    排产计划
	 * @param originalLineMap 原线规格配置
	 */
	private void caculateOriginalLineBrandNum(List<XwyyScheduleResultVo> scheduleList,
			Map<String, XwyyOriginalLineSpec> originalLineMap) {
		// 获取大卷品牌设置，相同大卷的全部拼接起来
		Map<String, String> bigRollBrandMap = xwyyEngineMapper.selectXwyyBigRollOriginalBrand().stream()
				.collect(Collectors.groupingBy(XwyyBigRollOriginalBrand::getBigRollCode,
						Collectors.mapping(XwyyBigRollOriginalBrand::getBrand, Collectors.joining(","))));
		// 先按原线代码对排产记录分组
		Map<String, List<XwyyScheduleResultVo>> scheduleLineMap = scheduleList.stream()
				.collect(Collectors.groupingBy(XwyyScheduleResultVo::getOriginalLineCode));
		// 按品牌+原线代码统计原线数
		Map<String, BigDecimal> originalBrandMap = new HashMap<>();

		for (Entry<String, List<XwyyScheduleResultVo>> entry : scheduleLineMap.entrySet()) {
			String originalLineCode = entry.getKey();
			List<XwyyScheduleResultVo> originalScheduleList = entry.getValue();
			// 先将有设置品牌的计划统计出来
			for (XwyyScheduleResultVo schedule : originalScheduleList) {
				String brand = bigRollBrandMap.get(schedule.getBigRollCode());
				if (StringUtils.isNotEmpty(brand)) {
					// 超过长度限制的直接截取掉
					if (brand.length() > 100) {
						brand = brand.substring(0, 99);
					}
					// 有设置品牌，则直接将计划量加到同一个品牌内
					String key = this.createOriginalBrandKey(originalLineCode, brand);
					BigDecimal originalBrandNum = originalBrandMap.getOrDefault(key, BigDecimal.ZERO);
					originalBrandMap.put(key, originalBrandNum.add(new BigDecimal(schedule.getTotalPlan())));
					schedule.setOriginalBrand(brand);
				}
				schedule.setOriginalBrandNum(BigDecimal.ZERO);
			}
		}

		// 将有设置品牌的规格按原线代码 + 品牌分组
		Map<String, List<XwyyScheduleResultVo>> resultMap = scheduleList.stream()
				.filter(vo -> vo.getOriginalBrand() != null).collect(Collectors
						.groupingBy(vo -> createOriginalBrandKey(vo.getOriginalLineCode(), vo.getOriginalBrand())));
		for (Entry<String, List<XwyyScheduleResultVo>> entry : resultMap.entrySet()) {
			String brandKey = entry.getKey();
			BigDecimal originalBrandNum = originalBrandMap.get(brandKey);
			if (originalBrandNum != null) {
				List<XwyyScheduleResultVo> brandScheduleList = entry.getValue();
				// 取出字符串最大的大卷编号
				String maxBigRollCode = brandScheduleList.stream().map(XwyyScheduleResultVo::getBigRollCode).distinct()
						.max((code1, code2) -> code1.compareTo(code2)).get();
				brandScheduleList.stream().filter(v -> v.getBigRollCode().equals(maxBigRollCode)).forEach(v -> {
					// 获取原线长度
					BigDecimal originalLineLength = Optional.ofNullable(originalLineMap.get(v.getOriginalLineCode()))
							.map(XwyyOriginalLineSpec::getOriginalLineLength).orElse(null);
					if (originalLineLength != null && originalLineLength.compareTo(BigDecimal.ZERO) != 0) {
						// 计算大卷数 = 同品牌的计划量 / 大卷长度
						v.setOriginalBrandNum(originalBrandNum.divide(originalLineLength, 1, RoundingMode.UP));
					}
				});
			}
		}
	}

	/**
	 * 插单时计算原线品牌数
     *
	 * @param XwyyScheduleResultDto 排产计划
	 * @param originalLineMap       原线规格配置
	 */
	private void caculateOriginalLineBrandNumInsert(XwyyScheduleResultDto scheduleResult,
			Map<String, XwyyOriginalLineSpec> originalLineMap) {
		List<XwyyScheduleResultVo> scheduleVoList = new ArrayList<>();
		// 查询当天的排程数据
		xwyyEngineMapper.selectXwyyScheduleList(scheduleResult.getScheduleDate()).stream()
				// 过滤出原线代码的规格
				.filter(v -> v.getOriginalLineCode().equals(scheduleResult.getOriginalLineCode()))
				// 批量转换成vo
				.forEach(s -> {
					XwyyScheduleResultVo vo = new XwyyScheduleResultVo();
					vo.setId(s.getId());
					vo.setBigRollCode(s.getBigRollCode());
					vo.setOriginalLineCode(s.getOriginalLineCode());
					vo.setTotalPlan(s.getTotalPlan());
					scheduleVoList.add(vo);
				});
		// 把插单记录添加到列表中
		XwyyScheduleResultVo insertSchedule = new XwyyScheduleResultVo();
		insertSchedule.setBigRollCode(scheduleResult.getBigRollCode());
		insertSchedule.setOriginalLineCode(scheduleResult.getOriginalLineCode());
		insertSchedule.setTotalPlan(scheduleResult.getTotalPlan());
		scheduleVoList.add(insertSchedule);
		// 计算品牌个数
		this.caculateOriginalLineBrandNum(scheduleVoList, originalLineMap);
		// 经过计算，如果有原线品牌，则需要更新
		if (insertSchedule.getOriginalBrand() != null) {
			// 品牌
			String originalBrand = insertSchedule.getOriginalBrand();
			// 待更新记录
			List<XwyyScheduleResultVo> updateScheduleList = new ArrayList<>();
			for (XwyyScheduleResultVo scheduleVo : scheduleVoList) {
				if (scheduleVo == insertSchedule) {
					// 插单记录，回写品牌与品牌数
					scheduleResult.setOriginalBrand(originalBrand);
					scheduleResult.setOriginalBrandNum(scheduleVo.getOriginalBrandNum());
				} else if (originalBrand.equals(scheduleVo.getOriginalBrand())) {
					scheduleVo.setBaseVale(scheduleVo.getId());
					// 品牌与插单记录相同的，需要更新数据
					updateScheduleList.add(scheduleVo);
				}
			}
			if (!updateScheduleList.isEmpty()) {
				xwyyEngineMapper.createTempTable();
				xwyyEngineMapper.insertTempTable(updateScheduleList);
				xwyyEngineMapper.updateScheduleResultOriginalBrand(DateUtils.parseDateToStr("yyyy-MM-dd", scheduleResult.getScheduleDate()), updateScheduleList);
//				xwyyEngineMapper.dropTempTable();
			}
		}
	}

	/**
	 * 导入时计算原线品牌数
     *
	 * @param scheduleList    排产计划
	 * @param originalLineMap 原线规格配置
	 */
	private void caculateOriginalLineBrandNumImport(List<XwyyScheduleResultDto> scheduleList,
			Map<String, XwyyOriginalLineSpec> originalLineMap) {
		List<XwyyScheduleResultVo> scheduleVoList = new ArrayList<>();
		for (XwyyScheduleResultDto s : scheduleList) {
			XwyyScheduleResultVo vo = new XwyyScheduleResultVo();
			vo.setBigRollCode(s.getBigRollCode());
			vo.setOriginalLineCode(s.getOriginalLineCode());
			vo.setTotalPlan(s.getTotalPlan());
			scheduleVoList.add(vo);
		}
		this.caculateOriginalLineBrandNum(scheduleVoList, originalLineMap);
		for (int i = 0, size = scheduleList.size(); i < size; i++) {
			XwyyScheduleResultDto scheduleDto = scheduleList.get(i);
			XwyyScheduleResultVo scheduleVo = scheduleVoList.get(i);
			scheduleDto.setOriginalBrand(scheduleVo.getOriginalBrand());
			scheduleDto.setOriginalBrandNum(scheduleVo.getOriginalBrandNum());
		}
	}

	/**
	 * 构建原线代码与品牌的key，用于统计原线卷数
     *
	 * @param originalLineCode
	 * @param brand
	 * @return
	 */
	private String createOriginalBrandKey(String originalLineCode, String brand) {
		return StringUtils.join(originalLineCode, "##", brand);
	}

	/**
	 * 自动排程时计算原线卷数
     *
	 * @param scheduleList    排产计划
	 * @param originalLineMap 原线规格配置
	 */
	private void caculateOriginalLineQtyNumAuto(List<XwyyScheduleResultVo> scheduleList,
			Map<String, XwyyOriginalLineSpec> originalLineMap, Long defaultBreakRoll) {
		List<XwyyScheduleResultDto> scheduleDtoList = new ArrayList<>();
		for (XwyyScheduleResultVo s : scheduleList) {
			XwyyScheduleResultDto vo = new XwyyScheduleResultDto();
			vo.setBigRollCode(s.getBigRollCode());
			vo.setOriginalLineCode(s.getOriginalLineCode());
			vo.setTotalPlan(s.getTotalPlan());
			vo.setTotalPlanNum(s.getTotalPlanNum());
			scheduleDtoList.add(vo);
		}
		this.caculateOriginalLineQtyNum(scheduleDtoList, originalLineMap, defaultBreakRoll);
		for (int i = 0, size = scheduleList.size(); i < size; i++) {
			XwyyScheduleResultDto scheduleDto = scheduleDtoList.get(i);
			XwyyScheduleResultVo scheduleVo = scheduleList.get(i);
			scheduleVo.setOriginalLineQtyNum(new BigDecimal(scheduleDto.getOriginalLineQtyNum()));
		}
	}

	/**
	 * 计算原线卷数
     *
	 * @param scheduleList    排产计划
	 * @param originalLineMap 原线规格配置
	 */
	private void caculateOriginalLineQtyNum(List<XwyyScheduleResultDto> scheduleList,
			Map<String, XwyyOriginalLineSpec> originalLineMap, Long defaultBreakRoll) {
		// 先按原线代码对排产记录分组
		Map<String, List<XwyyScheduleResultDto>> scheduleLineMap = scheduleList.stream()
				.collect(Collectors.groupingBy(XwyyScheduleResultDto::getOriginalLineCode));
		for (Entry<String, List<XwyyScheduleResultDto>> entry : scheduleLineMap.entrySet()) {
			// 原线代码
			String originalLineCode = entry.getKey();
			List<XwyyScheduleResultDto> resultList = entry.getValue();
			// 原线的可破大卷数，先从配置中获取，没有则使用系统配置的默认值
			Long breakRollNum = Optional.ofNullable(originalLineMap.get(originalLineCode))
					.map(XwyyOriginalLineSpec::getBreakRollNum).orElse(defaultBreakRoll);

			// 统计同一原线的规格总计划量
			BigDecimal totalPlanNum = resultList.stream().filter(s -> s.getTotalPlanNum() != null)
					.map(XwyyScheduleResultDto::getTotalPlanNum).reduce(BigDecimal.ZERO, BigDecimal::add);
			// 计算原线卷数 = 使用相同原线的帘布大卷数 / 破大卷数
			BigDecimal originalLineQtyNum = totalPlanNum.divide(BigDecimal.valueOf(breakRollNum), 1, RoundingMode.UP);

			// 取出最大的大卷编号
			String maxBigRollCode = resultList.stream().map(XwyyScheduleResultDto::getBigRollCode).distinct()
					.max((code1, code2) -> code1.compareTo(code2)).get();
			for (XwyyScheduleResultDto resultVo : resultList) {
				// 根据大卷编号设置原线卷数
				if (resultVo.getBigRollCode().equals(maxBigRollCode)) {
					// 使用同一原线的大卷规格排程，编号最大那个规格设置原线卷数
					resultVo.setOriginalLineQtyNum(originalLineQtyNum.toString());
				} else {
					// 其他设置原线卷数为0
					resultVo.setOriginalLineQtyNum(BigDecimal.ZERO.toString());
				}
			}
		}
	}

	/**
	 * 纤维压延转机台
     *
	 * @Author hakimryan
	 * @Description
	 * @Date 2021-7-29 11:32:01
	 * @param oldMachineIds  转机台前，旧的机台id
	 * @param scheduleResult 转机台排产记录
	 */
	@Transactional
	@Override
	public void changeXwyyMachine(String oldMachineIds, XwyyScheduleResultDto scheduleResult) {
		String batchNo = scheduleResult.getBatchNo();
		String orderNo = scheduleResult.getOrderNo();
		// 记录日志
		String logdetail = logSplit("旧机台ID：" + oldMachineIds, "转机台后排产数据：" + toJSONString(scheduleResult));
		autoScheduleLogService.insertXwyyScheduleLog(batchNo, orderNo, "转机台结束后的排程数据", logdetail);
	}

	/**
     *
	 * 确认机台
     *
	 * @param scheduleResult 确认后的排产记录
	 */
	@Transactional
	@Override
	public void confirmXwyyMachine(XwyyScheduleResultDto scheduleResult) {
		String batchNo = scheduleResult.getBatchNo();
		String orderNo = scheduleResult.getOrderNo();
		// 转机台机台处理前的排程数据json字符串，用于日志记录
		String oldScheduleResult = toJSONString(scheduleResult);

		Map<String, Double> lossRateMap = xwyyEngineLossService.getLossRateMap(); // 损耗率map
		Map<String, String> paramsMap = this.getParamsMap(); // 获取工序参数map
		double paramLossRate = getDouble(paramsMap.get(EngineConstants.LOSS_RATE));

		// 选定机台后，需要根据损耗率重新计算计划量
		// 取出物料 + 机台对应的损耗率设置
		double lossRate = xwyyEngineLossService.getLossRate(scheduleResult.getBigRollCode(),
				scheduleResult.getMachineId(), lossRateMap, paramLossRate);
		// 原中班计划量
		Double dayPlanQty = Optional.ofNullable(scheduleResult.getDayPlanQty()).orElse(0D);
		// 原晚班计划量
		Double nightPlanQty = Optional.ofNullable(scheduleResult.getNightPlanQty()).orElse(0D);
		// 算上损耗率之后的计划量，公式：新计划量 = 原计划量 + 原计划量 * 损耗率
		dayPlanQty = BigDecimalUtil.add(dayPlanQty, BigDecimalUtil.mul(dayPlanQty, lossRate));
		nightPlanQty = BigDecimalUtil.add(nightPlanQty, BigDecimalUtil.mul(nightPlanQty, lossRate));

		// 结果需要向上取整，modify by 20211230
		scheduleResult.setDayPlanQty(BigDecimalUtil.roundUp(dayPlanQty, 0));
		scheduleResult.setNightPlanQty(BigDecimalUtil.roundUp(nightPlanQty, 0));
		// 重算三个厂的总计划量合计值
		Double fac2TotalPlan = Optional.ofNullable(scheduleResult.getFac2TotalPlan()).orElse(0D);
		Double fac5TotalPlan = Optional.ofNullable(scheduleResult.getFac5TotalPlan()).orElse(0D);
		scheduleResult.setTotalPlan(
				scheduleResult.getDayPlanQty() + scheduleResult.getNightPlanQty() + fac2TotalPlan + fac5TotalPlan);

		// 记录日志
		String logdetail = logSplit("确认机台后的耗损率：" + lossRate, "确认机台前排程数据：" + oldScheduleResult,
				"确认机台后排产数据：" + toJSONString(scheduleResult));
		autoScheduleLogService.insertXwyyScheduleLog(batchNo, orderNo, "确认机台后排程需要根据耗损率重算计划量", logdetail);
	}

	/**
	 * 计算各大卷对应的日用参考值
     *
	 * @param scheduleDate    排产日期
	 * @param breadth         幅宽
	 * @param bigRollCodeList 大卷编号列表
	 * @Param isProductStage 仅对投产阶段的规格排产
	 * @return 计算结果，key:大卷编号，value:日用参考量
	 */
	private Map<String, Double> caculateDayUsed(Date scheduleDate, Double breadth, List<String> bigRollCodeList,
			boolean isProductStage) {
		// 计算出当天成型排程计划消耗的大卷数
		List<XwyyDayUsedVo> dayUsedList = xwyyEngineMapper.listXwyyDayUsed(scheduleDate, breadth, bigRollCodeList,
				isProductStage);
		// 转换成map
		return dayUsedList.stream().collect(Collectors.toMap(XwyyDayUsedVo::getBigRollCode, XwyyDayUsedVo::getDayUsed));
	}

	/**
	 * 清理排产日当天的历史排程数据
     *
	 * @param scheduleDate 排程日期
	 */
	private void cleanHistoryScheduleResult(Date scheduleDate) {
		xwyyEngineMapper.insertXwyyScheduleLog(scheduleDate);
		xwyyEngineMapper.deleteXwyyScheduleResult(scheduleDate);
		xwyyEngineMapper.deleteXwyyScheduleAssist(scheduleDate);
		XwyyScheduleRecordVo recordVo = new XwyyScheduleRecordVo();
		recordVo.setBaseVale(null);
		recordVo.setScheduleDate(scheduleDate);
		xwyyEngineMapper.logicDeleteXwyyScheduleRecord(recordVo);
	}

	/**
	 * 验证成型排程记录的胎胚code在施工表中是否都能找到对应记录，如果不能则提示
     *
	 * @param scheduleDate 排程日志
	 * @param batchNo      批次号
	 * @Param isProductStage 仅对投产阶段的规格排产
	 * @return 外协规格清单map<规格编号, 校验错误信息>
	 */
	private Map<String, String> validatedConstruction(Date scheduleDate, String batchNo, boolean isProductStage) {
		// 获取纤维压延外协规格清单
		List<XwyyAssistSpec> assistSpecList = xwyyEngineMapper.selectXwyyAssistSpecList();
		// 本次排产施工不完整的外协规格
		Map<String, String> assistSpecMap = new HashMap<>();
		for (XwyyAssistSpec assistSpec : assistSpecList) {
			assistSpecMap.put(assistSpec.getMaterialCode(), null);
		}

		List<String> list = xwyyEngineMapper.listLossConstructionForCd90(scheduleDate);
		if (list != null && !list.isEmpty()) {
			String tip = I18nUtil.getMessage("engine.auto.scheule.validated");
			String embryoCodes = String.join(",", list);
			tip = String.format(tip, embryoCodes);
			autoScheduleLogService.insertXwyyScheduleLog(batchNo, "", "自动排程失败",
					"自动排程失败，原因：90度裁断排程数据为空，或没有在施工信息中找到对应的物料");
			throw new RuntimeException(tip);
		}

		// 校验成型对应的施工信息是否完整
		List<EngineConstructionInfo> constructionList = xwyyEngineMapper.listConstructionInfo(scheduleDate,
				isProductStage);
		for (EngineConstructionInfo constructionInfo : constructionList) {
			String embryoCode = constructionInfo.getEmbryoCode();
			String errorMsg = null;
			// 帘线规格
			if (StringUtil.isEmpty(constructionInfo.getCordSpec())) {
				errorMsg = this.returnValidatedErrorMsg(batchNo, embryoCode, "ui.construction.cordSpec");
				if (StringUtils.isNotEmpty(errorMsg)) {
					this.returnErrorMessage(errorMsg);
				}
			}
			validate: {
				// 胎侧长度
				if (constructionInfo.getSidewallLength() == null || constructionInfo.getSidewallLength() == 0) {
					errorMsg = this.returnValidatedErrorMsg(batchNo, embryoCode, "ui.construction.sidewallLength");
				}
				if (StringUtils.isNotEmpty(errorMsg)) {
					break validate;
				}
				// 1#胎体布一定不能为空
				if (StringUtil.isEmpty(constructionInfo.getTireFabricCode1())) {
					errorMsg = this.returnValidatedErrorMsg(batchNo, embryoCode, "ui.construction.tireFabricCode1");
				}
				if (StringUtils.isNotEmpty(errorMsg)) {
					break validate;
				}
				// 如果1#胎体布编号不为空，则1#胎体布工艺也不能为空
				if (StringUtil.isNotEmpty(constructionInfo.getTireFabricCode1())
						&& (StringUtil.isEmpty(constructionInfo.getTireFabricCraft1())
								|| "0".equals(constructionInfo.getTireFabricCraft1()))) {
					errorMsg = this.returnValidatedErrorMsg(batchNo, embryoCode, "ui.construction.tireFabricCraft1");
				}
				if (StringUtils.isNotEmpty(errorMsg)) {
					break validate;
				}
				// 2#胎体布工艺，2#胎体布编号不为空时工艺也不允许为空
				if (StringUtil.isNotEmpty(constructionInfo.getTireFabricCode2())
						&& (StringUtil.isEmpty(constructionInfo.getTireFabricCraft2())
								|| "0".equals(constructionInfo.getTireFabricCraft2()))) {
					errorMsg = this.returnValidatedErrorMsg(batchNo, embryoCode, "ui.construction.tireFabricCraft2");
				}
				if (StringUtils.isNotEmpty(errorMsg)) {
					break validate;
				}
				// 3#胎体布工艺，3#胎体布编号不为空时工艺也不允许为空
				if (StringUtil.isNotEmpty(constructionInfo.getTireFabricCode3())
						&& (StringUtil.isEmpty(constructionInfo.getTireFabricCraft3())
								|| "0".equals(constructionInfo.getTireFabricCraft3()))) {
					errorMsg = this.returnValidatedErrorMsg(batchNo, embryoCode, "ui.construction.tireFabricCraft3");
				}
			}
			// 如果有校验失败，则判断物料号（帘线规格）是否在外协规格清单中
			String materialCode = constructionInfo.getCordSpec();
			if (assistSpecMap.containsKey(materialCode)) {
				// 符合条件，加入外协计划
				assistSpecMap.putIfAbsent(materialCode, errorMsg);
			} else if (StringUtils.isNotEmpty(errorMsg)) {
				// 否则直接报错：自动排程失败
				this.returnErrorMessage(errorMsg);
			}
		}
		return assistSpecMap;
	}

	/**
	 * 反馈校验错误信息
     *
	 * @param batchNo       排产批次号
	 * @param embryoCode    胎号
	 * @param columnNameKey 校验字段名称key（国际化）
	 * @return 错误提示信息
	 */
	private String returnValidatedErrorMsg(String batchNo, String embryoCode, String columnNameKey) {
		String columnName = I18nUtil.getMessage(columnNameKey);
		String logMsg = "自动排程失败，原因：胎胚“{}”的施工信息不完整：{}为空。";
		autoScheduleLogService.insertXwyyScheduleLog(batchNo, "", "自动排程失败",
				StringUtils.format(logMsg, embryoCode, columnName));
		return StringUtils.format(I18nUtil.getMessage("mes.error.message.auto.schedule.validate"), embryoCode,
				columnName);
	}

	/**
	 * 生成外协计划列表
     *
	 * @param scheduleDate   排产日
	 * @param isProductStage 是否只排生产阶段
	 * @param scheduleList   原排产计划
	 * @param batchNo        批次号
	 * @param assistSpecMap  外协规格清单map
	 * @param breadth        幅宽
	 * @return
	 */
	private List<XwyyScheduleResultVo> createScheduleAssistList(Date scheduleDate, boolean isProductStage,
			List<XwyyScheduleResultVo> scheduleList, String batchNo, Map<String, String> assistSpecMap,
			Double breadth) {
		List<XwyyScheduleResultVo> scheduleAssistList;
		if (!assistSpecMap.isEmpty()) {
			// 重新生成外协规格的排程计划
			scheduleAssistList = this.removeAssistSpec(scheduleList, assistSpecMap);
			// 初始化外协排程计划记录
			for (XwyyScheduleResultVo scheduleVo : scheduleAssistList) {
				// 批次号
				scheduleVo.setBatchNo(batchNo);
				// 创建工单号
				scheduleVo.setOrderNo(this.createOrderNo(batchNo));
				scheduleVo.setIsRelease(ApsConstant.NO_RELEASE);
				scheduleVo.setBaseVale(null);
				// 计划量需要向上取整
				scheduleVo.setDayPlanQty(Math.ceil(Optional.ofNullable(scheduleVo.getDayPlanQty()).orElse(0D)));
				scheduleVo.setNightPlanQty(Math.ceil(Optional.ofNullable(scheduleVo.getNightPlanQty()).orElse(0D)));
				// 通过帘布编号匹配外协规格清单，获取对应的异常提示信息
				String message = assistSpecMap.get(scheduleVo.getBigRollCode());
				// 早班有计划量则设置到早班的提示信息，否则设置到晚班提示信息中
				if (scheduleVo.getDayPlanQty() > 0) {
					scheduleVo.setDaySysAnalysis(message);
				} else {
					scheduleVo.setNightSysAnalysis(message);
				}
			}
		} else {
			scheduleAssistList = new ArrayList<>(0);
		}
		return scheduleAssistList;
	}

	/**
	 * 将正常排程计划中的外协规格转移出来
     *
	 * @param scheduleList  原排产计划
	 * @param assistSpecMap 外协规格清单map
	 * @return
	 */
	private List<XwyyScheduleResultVo> removeAssistSpec(List<XwyyScheduleResultVo> scheduleList,
			Map<String, String> assistSpecMap) {
		List<XwyyScheduleResultVo> list = new ArrayList<>();
		if (!assistSpecMap.isEmpty()) {
			for (int i = scheduleList.size() - 1; i >= 0; i--) {
				XwyyScheduleResultVo scheduleVo = scheduleList.get(i);
				if (assistSpecMap.containsKey(scheduleVo.getBigRollCode())) {
					list.add(scheduleList.remove(i));
				}
			}
		}
		return list;
	}

	/**
	 * 返回错误信息（抛出异常）
     *
	 * @param errorMsg
	 */
	private void returnErrorMessage(String errorMsg) {
		// 前端提示信息需要增加：自动排程失败
		String tip = I18nUtil.getMessage("mes.error.message.auto.schedule.failed");
		throw new RuntimeException(tip + errorMsg);
	}

	/**
	 * 获取工序参数map
     *
	 * @return
	 */
	private Map<String, String> getParamsMap() {
		return xwyyEngineMapper.listXwyyParams().stream()
				.collect(Collectors.toMap(XwyyParamsVo::getParamCode, XwyyParamsVo::getParamValue, (v1, v2) -> v2));
	}

	/**
	 * 创建自动排程记录
     *
	 * @Author hakimryan
	 * @Description
	 * @Date 2021-7-22 10:54:58
	 * @Param
	 * @Return
	 */
	private void createScheduleRecord(Date scheduleDate, String cxBatchNo, String batchNo) {
		XwyyScheduleRecordVo recordVo = new XwyyScheduleRecordVo();
		recordVo.setScheduleDate(scheduleDate);
		recordVo.setCxBatchNo(cxBatchNo);
		recordVo.setBatchNo(batchNo);
		recordVo.setStatus(ApsConstant.STATUS_ENABLE);
		recordVo.setBaseVale(null);
		xwyyEngineMapper.insertScheduleRecord(recordVo);
	}

	/**
	 * 创建批次号
     *
	 * @Author hakimryan
	 * @Description
	 * @Date 2021-7-22 10:02:41
	 * @Param scheduleDate 排产日期
	 * @Return
	 */
	private String createBatchNo(Date scheduleDate) {
		String strScheduleDate = new SimpleDateFormat("yyyyMMdd").format(scheduleDate);
		return incrementService.getSequence3(EngineConstants.XWYY_BATCH_NO_PREFIX + strScheduleDate);
	}

	/**
	 * 创建工单号
     *
	 * @Author hakimryan
	 * @Description
	 * @Date 2021-7-22 10:02:41
	 * @Param batchNo 批次号
	 * @Return
	 */
	private String createOrderNo(String batchNo) {
		return incrementService.getSequence4(batchNo);
	}

    @Override
    public void batchUpdateBatchNoAndOrderNo(Date scheduleDate) {
        List<XwyyScheduleResultVo> scheduleResultVoList = xwyyEngineMapper.selectXwyyScheduleList(scheduleDate);
        //查询当前排程的批次号
        String batchNo = xwyyEngineMapper.getCurrentBatchNo(scheduleDate);
        if (StringUtils.isBlank(batchNo)) {
            //当前的批次号为空，说明还没”自动排程“或者做的批量导入（需要删掉已排的数据），那么自己生成一个排程批次号
            //排程批次号
            batchNo = this.createBatchNo(scheduleDate);
            //创建自动排程记录
            this.createScheduleRecord(scheduleDate, "", batchNo);
        }
        for (XwyyScheduleResultVo scheduleResult : scheduleResultVoList) {
            //批次号
            scheduleResult.setBatchNo(batchNo);
            //工单号
            String orderNo = this.createOrderNo(batchNo);
            scheduleResult.setOrderNo(orderNo);
        }
        xwyyEngineMapper.batchUpdateBatchNoAndOrderNo(scheduleResultVoList);
    }
}
