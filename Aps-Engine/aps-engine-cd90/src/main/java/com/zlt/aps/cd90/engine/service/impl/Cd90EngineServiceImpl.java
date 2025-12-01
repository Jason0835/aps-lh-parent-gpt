package com.zlt.aps.cd90.engine.service.impl;

import com.github.pagehelper.util.StringUtil;
import com.ruoyi.api.gateway.system.domain.ImportErrorLog;
import com.ruoyi.common.core.utils.DateUtils;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.utils.StringUtils;
import com.zlt.aps.cd90.api.domain.entity.Cd90AssistSpec;
import com.zlt.aps.cd90.api.domain.entity.Cd90ScheduleResult;
import com.zlt.aps.cd90.engine.mapper.Cd90EngineMapper;
import com.zlt.aps.cd90.engine.mapper.Cd90EngineMonthSurplusMapper;
import com.zlt.aps.cd90.engine.mapper.Cd90EngineStockMapper;
import com.zlt.aps.cd90.engine.service.*;
import com.zlt.aps.cd90.engine.vo.*;
import com.zlt.aps.common.core.constant.ApsConstant;
import com.zlt.aps.common.core.utils.BigDecimalUtil;
import com.zlt.aps.common.engine.constants.EngineConstants;
import com.zlt.aps.common.engine.domain.EngineProductConstructionInfo;
import com.zlt.aps.common.engine.mapper.CommonMapper;
import com.zlt.aps.common.engine.service.AutoScheduleLogService;
import com.zlt.aps.common.engine.service.impl.IncrementService;
import com.zlt.aps.common.engine.utils.CollectionUtil;
import com.zlt.aps.common.engine.utils.GenerageMapKeyUtils;
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
import static com.zlt.aps.common.core.utils.ApsCommonUtil.getDouble;
import static com.zlt.aps.common.core.utils.ApsCommonUtil.logSplit;
import static com.zlt.aps.common.core.utils.ImportUtil.addImportErrorLog;

/**
 * 90度裁断自动排程服务实现类
 *
 * @Description
 * @Author hakimrayn
 * @Date 2021-7-14 10:36:57
 * @Version 1.0
 */
@Service("cd90EngineService")
@Slf4j
public class Cd90EngineServiceImpl implements Cd90EngineService {
	@Autowired
	private Cd90EngineMapper cd90EngineMapper;
    @Autowired
    private Cd90EngineMonthSurplusMapper cd90EngineMonthSurplusMapper;
	@Autowired
	private Cd90EngineMachineService cd90EngineMachineService;
	@Autowired
	private Cd90EnginePlanQtyService cd90EnginePlanQtyService;
	@Autowired
	private Cd90EngineEquilibriumService cd90EngineEquilibriumService;
	@Autowired
	private Cd90EngineProductOrderService cd90EngineProductOrderService;
	@Autowired
	private Cd90EngineMonthSurplusService cd90EngineMonthSurplusService;
	@Autowired
	private Cd90EngineLossService cd90EngineLossService;
	@Resource
	private IncrementService incrementService;
	@Resource
	private AutoScheduleLogService autoScheduleLogService;
	@Resource
	private CommonMapper commonMapper;
	@Resource
	private Cd90EngineStockMapper cd90EngineStockMapper;
	/**
	 * 生产阶段校验开关状态：打开
	 */
	private final static String PRODUCTION_STAGE_ON = "1";
	/**
	 * 生产阶段：投产阶段
	 */
	private final static String PRODUCTION_STAGE = "0";
	/**
	 * 一百，用于百分比 -> 小数的单位换算
	 */
	private final static BigDecimal ONE_HUNDRED = new BigDecimal("100");
	/**
	 * 卷曲长度默认值：87
	 */
	private final static BigDecimal DEFAULT_CRIMP_LENGTH = new BigDecimal("87");
	/**
	 * 裁断最小取整卷数默认值：0.3
	 */
	private static final BigDecimal DEFAULT_MIN_ROUND_ROLL_NUM = new BigDecimal("0.3");

	/**
	 * 90度裁断自动排程
     *
	 * @Author hakimryan
	 * @Description
	 * @Date 2021-7-14 13:37:28
	 * @param scheduleDate 排产日期
	 */
	@Transactional
	@Override
	public void autoCd90Schedule(Date scheduleDate) {
		// 加载系统参数
		Map<String, String> paramsMap = this.getParamsMap();
		// 判断仅对投产阶段规格排产是否打开
		boolean isProductStage = PRODUCTION_STAGE_ON
				.equals(paramsMap.getOrDefault(EngineConstants.PRODUCTION_STAGE_PRODUCE, PRODUCTION_STAGE_ON));
		/* 根据排程日期，从成型排产计划中生成90度排产的信息 */
		List<Cd90ScheduleResultVo> scheduleList = cd90EngineMapper.selectCd90ScheduleBaseList(scheduleDate,
				isProductStage);
		// 本次排程批次号
		String batchNo = this.createBatchNo(scheduleDate);
		// 记录日志
		autoScheduleLogService.insertCd90ScheduleLog(batchNo, "", "1.1、根据'成型排程记录'统计出90度裁断排程记录基础数据",
				toJSONString(scheduleList));
		if (scheduleList == null || scheduleList.isEmpty()) {
			log.info("根据成型排程记录为空，无法生成90度裁断排产");
			autoScheduleLogService.insertCd90ScheduleLog(batchNo, "", "自动排程失败", "自动排程失败，原因：成型排程数据为空，或没有在施工信息中找到对应的物料");
			throw new RuntimeException(I18nUtil.getMessage("engine.auto.scheule.tip1"));
		}
		// 校验成型排程记录的胎胚code在施工表中是否都能找到对应记录，如果不能则提示
		// 返回值为本次排产的外协规格，需要生成对应的外协计划
		Map<String, String> assistSpecMap = this.validatedConstruction(scheduleDate, batchNo, isProductStage);

		// 生成外协计划列表
		List<Cd90ScheduleResultVo> scheduleAssistList = this.createScheduleAssistList(scheduleDate, isProductStage,
				scheduleList, batchNo, assistSpecMap);

		// 取出当天已经发布过的排程记录
		Map<String, List<Cd90ScheduleResultVo>> isReleaseGroupMap = this.selectIsReleaseScheduleResult(scheduleDate);

		// 取出只有一笔排程的已发布规格
		Map<String, Cd90ScheduleResultVo> isReleaseMap = isReleaseGroupMap.entrySet().stream()
				.filter(e -> e.getValue().size() == 1)
				.collect(Collectors.toMap(Entry::getKey, e -> CollectionUtil.firstElement(e.getValue())));

		// 取出有多条排程的已发布规格
		Map<String, List<Cd90ScheduleResultVo>> isReleaseGroup = isReleaseGroupMap.entrySet().stream()
				.filter(e -> e.getValue().size() > 1).collect(Collectors.toMap(Entry::getKey, Entry::getValue));

		// 记录日志
		if (!isReleaseGroupMap.isEmpty()) {
			autoScheduleLogService.insertCd90ScheduleLog(batchNo, "", "1、存在上次已发布的排程记录",
					toJSONString(isReleaseGroupMap));
		}
		if (CollectionUtils.isNotEmpty(scheduleList)) {
			// 初始化排程明细，要结合当天已经发布过的排程记录设值
			this.initScheduleReulstList(batchNo, scheduleList, isReleaseMap);

			/**
			 * 将90度裁断排程安排到具体生产线上
			 */
//			cd90EngineMachineService.scheduleMachine(scheduleList);

			/**
			 * 计算计划量信息，包括库存量、重算计划量、计算可用时长
			 */
			this.loadMonthSurplus(scheduleDate, scheduleList); // 加载月度计划剩余量
			String lossRate = paramsMap.get(EngineConstants.LOSS_RATE);
			// 获取库存损耗率
			BigDecimal stockLossRate = this.getStockLossRate(paramsMap);
			// 卷曲长度
			BigDecimal crimpLength = Optional.ofNullable(paramsMap.get(EngineConstants.CRIMP_LENGTH))
					.map(p -> new BigDecimal(p)).orElse(DEFAULT_CRIMP_LENGTH);
			// 最小取整卷数
			BigDecimal minRoundRollNum = Optional.ofNullable(paramsMap.get(EngineConstants.MIN_ROUND_ROLL_NUM))
					.map(p -> new BigDecimal(p)).orElse(DEFAULT_MIN_ROUND_ROLL_NUM);
			cd90EnginePlanQtyService.calculateSchedulePlanQty(scheduleDate, scheduleList, lossRate, stockLossRate,
					isProductStage, crimpLength, minRoundRollNum, paramsMap);

			/**
			 * 根据月度计划量重新调整计划量，包括收尾与计划量整卷取整
			 */
			String closeOutNum = paramsMap.get(EngineConstants.CLOSE_OUT_NUM);
			cd90EngineMonthSurplusService.calculateMonthSurplus(scheduleDate, scheduleList, closeOutNum, lossRate);

			/**
			 * 均衡排产，减少中班与晚班的计划量差异
			 */
			String planDifferenceRate = paramsMap.get(EngineConstants.PLAN_DIFFERENCE_RATE);
			String supplyTimePass = paramsMap.get(EngineConstants.SUPPLY_TIME_PASS);
			String equalShareThreshold = paramsMap.get(EngineConstants.EQUAL_SHARE_THRESHOLD);
			cd90EngineEquilibriumService.scheduleEquilibrium(scheduleList, planDifferenceRate, supplyTimePass,
					equalShareThreshold);

			// 根据机台产能选择机台
			cd90EngineMachineService.chooseMachineByCapacity(scheduleList);

			/**
			 * 计算排产结果的生产顺序
			 */
			cd90EngineProductOrderService.calculateProduceOrder(scheduleList);
		}

		// 如果还存在上次已发布的规格但本次没有排程的规格，直接将这些规格的排程信息复制到本次排程中
		for (Cd90ScheduleResultVo scheduleVo : isReleaseMap.values()) {
			scheduleVo.setBatchNo(batchNo);
			scheduleVo.setBaseVale(null);
			scheduleList.add(scheduleVo);
		}

		// 处理一个已发布规格有多个排程的情况
		this.removeGroupSchedule(batchNo, scheduleList, isReleaseGroup);

		// 将当天的历史排程记录（上一次的）转移至日志表
		this.cleanHistoryScheduleResult(scheduleDate);

		/**
		 * 创建自动排产记录
		 */
		// 成型批次号
		String cxBatchNo = CollectionUtil.firstElement(scheduleList).getCxBatchNo();
		this.createScheduleRecord(scheduleDate, cxBatchNo, batchNo);

		/**
		 * 创建排产结果明细记录
		 */
		if (CollectionUtils.isNotEmpty(scheduleList)) {
			cd90EngineMapper.insertScheduleResultList(scheduleList);
		}

		/**
		 * 创建外协排产结果明细记录
		 */
		if (CollectionUtils.isNotEmpty(scheduleAssistList)) {
			cd90EngineMapper.insertScheduleAssistList(scheduleAssistList);
		}

		/**
		 * 记录日志
		 */
		autoScheduleLogService.insertCd90ScheduleLog(batchNo, "", "7.1、最终排产结果", toJSONString(scheduleList));
	}

	/**
	 *  加载月度计划剩余量
	 * @param scheduleDate
	 * @param scheduleList
	 */
    private void loadMonthSurplus(Date scheduleDate, List<Cd90ScheduleResultVo> scheduleList) {
        String year = DateUtils.parseDateToStr("yyyy", scheduleDate);
        String month = DateUtils.parseDateToStr("MM", scheduleDate);
        List<Cd90MonthSurplusVo> monthSurplusList = cd90EngineMonthSurplusMapper.listCd90MonthPlanSurplus(year, month);
        Map<String, Cd90MonthSurplusVo> monthSurplus = monthSurplusList.stream()
                .collect(Collectors.toMap(s -> GenerageMapKeyUtils.createMapKey(s.getMaterialCode(), s.getRemark()),
                        Function.identity(), (v1, v2) -> v2)); // 按帘布+层级分组
        scheduleList.stream()
                .forEach(s -> s.setSurplusQty(Optional
                        .ofNullable(monthSurplus.get(GenerageMapKeyUtils.createMapKey(s.getClothCode(), String.valueOf(s.getLayers()))))
                        .map(Cd90MonthSurplusVo::getMonthRemainQty).orElse(0D))); // 按帘布+层级获取剩余量
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
	private void removeGroupSchedule(String batchNo, List<Cd90ScheduleResultVo> scheduleList,
			Map<String, List<Cd90ScheduleResultVo>> isReleaseGroup) {
		for (Entry<String, List<Cd90ScheduleResultVo>> entry : isReleaseGroup.entrySet()) {
			String steelStripCode = entry.getKey();
			List<Cd90ScheduleResultVo> resultList = entry.getValue();
			// 取出本次排程该规格的排程记录
			Cd90ScheduleResultVo result = scheduleList.stream().filter(s -> steelStripCode.equals(s.getClothCode()))
					.findFirst().orElse(null);
			if (result != null) {
				// 将其从排程记录中移除掉
				scheduleList.remove(result);
			}
			// 将上次的多条排程记录全部复制过来，同时在备注添加信息：重排后中班计划量:xxx，夜班计划量:xxx
			for (Cd90ScheduleResultVo oldResult : resultList) {
				oldResult.setBatchNo(batchNo);
				oldResult.setBaseVale(null);
				String tip = I18nUtil.getMessage("reschedule.double.spec.remark");
				if (result != null) {
					oldResult.setRemark(StringUtils.format(tip, result.getDayPlanQty(), result.getNightPlanQty()));
				}
				scheduleList.add(oldResult);
			}
		}
	}

	/**
	 * 取出当天已经发布过的排程记录
     *
	 * @param scheduleDate 排产日
	 * @return
	 */
	private Map<String, List<Cd90ScheduleResultVo>> selectIsReleaseScheduleResult(Date scheduleDate) {
		// 取出本次自动排程前已发布的规格以及其工单号，组成map<帘布编号，工单号>，用于判断是否需要保留
		Map<String, List<Cd90ScheduleResultVo>> isReleaseMap = cd90EngineMapper.selectCd90ScheduleList(scheduleDate)
				.stream()
				// 过滤出曾经发布成功的记录
				.filter(v -> v.getPublishSuccessCount() != null && v.getPublishSuccessCount() > 0)
				// 取出钢带表编号
				.collect(Collectors.groupingBy(Cd90ScheduleResultVo::getClothCode));
		return isReleaseMap;
	}

	/**
	 * 初始化排程明细
     *
	 * @param batchNo      排程批次号
	 * @param scheduleList 排程明细列表
	 * @param isReleaseMap 已发布排程列表
	 */
	private void initScheduleReulstList(String batchNo, List<Cd90ScheduleResultVo> scheduleList,
			Map<String, Cd90ScheduleResultVo> isReleaseMap) {
		// 为本次的排产记录创建批次号与工单号
		for (Cd90ScheduleResultVo scheduleVo : scheduleList) {
			String clothCode = scheduleVo.getClothCode();
			scheduleVo.setBatchNo(batchNo);
			// 先判断该帘布是否之前已经发布过
			if (isReleaseMap.containsKey(clothCode)) {
				// 发布过，则取出该规格（从map中移除）
				Cd90ScheduleResultVo isReleaseVo = isReleaseMap.remove(clothCode);
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
			scheduleVo.setBaseVale(null);
		}
	}

	/**
	 * 90度裁断插单
     *
	 * @Author hakimryan
	 * @Description
	 * @Date 2021-7-25 09:31:42
	 * @param scheduleResult 插单记录
	 */
	@Transactional
	@Override
	public int insertCd90Order(Cd90ScheduleResult scheduleResult) {
		// 排程日期
		Date scheduleDate = scheduleResult.getScheduleDate();
		// 查询当前排程的批次号
		String batchNo = cd90EngineMapper.getCurrentBatchNo(scheduleDate);
		if (StringUtils.isBlank(batchNo)) {
			// 当前的批次号为空，则新生成排程批次号
			batchNo = this.createBatchNo(scheduleDate);
			// 创建自动排程记录
			this.createScheduleRecord(scheduleDate, "", batchNo);
		}

		List<Cd90ScheduleResult> scheduleList = new ArrayList<>();
		scheduleList.add(scheduleResult);
		// 异常记录列表
		List<ImportErrorLog> importErrorLogs = new ArrayList<>();
		// 处理插单的排程明细
		int result = this.batchUpdateOrInsertCd90Schedule(batchNo, scheduleDate, scheduleList, importErrorLogs);
		// 如果有异常记录，则向前端返回异常信息
		importErrorLogs.stream().findAny().ifPresent(v -> {
			throw new RuntimeException(v.getErrorDetail());
		});
		return result;
	}

	/**
	 * 批量导入90度裁断排程记录
     *
	 * @param scheduleDate 排程日志
	 * @param scheduleList 排程数据
	 */
	@Transactional
	@Override
	public List<ImportErrorLog> batchSaveCd90Schedule(Date scheduleDate, List<Cd90ScheduleResult> scheduleList) {
		// 批量导入前要先清除历史数据
		this.cleanHistoryScheduleResult(scheduleDate);
		// 创建新排程记录，批次号也重新生成
		String batchNo = this.createBatchNo(scheduleDate);
		// 导入异常记录
		List<ImportErrorLog> importErrorLogs = new ArrayList<>();
		// 处理导入排程明细
		this.batchUpdateOrInsertCd90Schedule(batchNo, scheduleDate, scheduleList, importErrorLogs);
		// 生成新批次时，如果明细能关联到成型信息，则也需要将成型信息保存到批次记录中
		String cxBatchNo = "";
		for (Cd90ScheduleResult schedule : scheduleList) {
			if (StringUtils.isNotEmpty(schedule.getCxBatchNo())) {
				cxBatchNo = schedule.getCxBatchNo();
				break;
			}
		}
		// 创建自动排程记录
		this.createScheduleRecord(scheduleDate, cxBatchNo, batchNo);

		return importErrorLogs;
	}

	/**
	 * 批量保存90度裁断排程记录
     *
	 * @param batchNo      批次号
	 * @param scheduleDate 排程日志
	 * @param scheduleList 排程数据
	 */
	private int batchUpdateOrInsertCd90Schedule(String batchNo, Date scheduleDate,
			List<Cd90ScheduleResult> scheduleList, List<ImportErrorLog> importErrorLogs) {
		// 加载系统参数
		Map<String, String> paramsMap = this.getParamsMap();
		// 月度计划信息
		Map<String, Cd90MonthSurplusVo> monthSurplusMap = cd90EngineMonthSurplusService
				.getMonthSurplusMap(scheduleDate);
		// 判断仅对投产阶段规格排产是否打开
		boolean isProductStage = PRODUCTION_STAGE_ON
				.equals(paramsMap.getOrDefault(EngineConstants.PRODUCTION_STAGE_PRODUCE, PRODUCTION_STAGE_ON));
		// 获取库存损耗率
		BigDecimal stockLossRate = this.getStockLossRate(paramsMap);
		// 16点半部件库存
		Map<String, Cd90StockVo> stockMap = cd90EnginePlanQtyService.getStockMap(scheduleDate, stockLossRate,
				isProductStage);
		// 排程基础信息
		Map<String, Cd90ScheduleResultVo> baseInfoMap = cd90EngineMapper
				.listInsertOrderBaseInfo(scheduleList, scheduleDate).stream()
				.collect(Collectors.toMap(Cd90ScheduleResultVo::getClothCode, Function.identity()));
		// 参数：收尾提醒阈值
		String closeOutNum = this.getParamsMap().get(EngineConstants.CLOSE_OUT_NUM);

		// 批量导入的初始行号，由于模板是从第3行开始是数据行，因此初始值是2
		int rowNum = 2;
		List<Cd90ScheduleResult> newScheduleList = new ArrayList<>();
		// 遍历入参排程记录
		for (Cd90ScheduleResult scheduleResult : scheduleList) {
			// 帘布编号
			String clothCode = scheduleResult.getClothCode();

			// 取出对应的插单信息
			Cd90ScheduleResultVo baseInfo = baseInfoMap.get(clothCode);
			if (baseInfo == null) {
				// 取不到说明物料编号有误，需要记录错误信息
				addImportErrorLog(null, rowNum, I18nUtil.getMessage("ui.error.message.column.materialCodeNotExist"),
						importErrorLogs);
				continue;
			}

			// 90度裁断库存信息
			Cd90StockVo stockVo = stockMap.get(clothCode);
			// 16点半部件库存量
			BigDecimal stockQty = stockVo != null && stockVo.getStockQty() != null ? stockVo.getStockQty()
					: BigDecimal.ZERO;

			scheduleResult.setBatchNo(batchNo);
			scheduleResult.setOrderNo(this.createOrderNo(batchNo));
			scheduleResult.setBigRollCode(baseInfo.getBigRollCode());
			scheduleResult.setUnitConsume(baseInfo.getUnitConsume());
			scheduleResult.setCraft(baseInfo.getCraft());
			scheduleResult.setEdgeGlue(baseInfo.getEdgeGlue());
			scheduleResult.setDayPlanQty(Optional.ofNullable(scheduleResult.getDayPlanQty()).orElse(0D));
			scheduleResult.setNightPlanQty(Optional.ofNullable(scheduleResult.getNightPlanQty()).orElse(0D));
			scheduleResult.setStockQty(stockQty.setScale(0, RoundingMode.DOWN).doubleValue());
			scheduleResult.setIsRelease(ApsConstant.NO_RELEASE);
			scheduleResult.setDelFlag(ApsConstant.DEL_FLAG_NORMAL);
			// 设置生产状态与收尾提示
			cd90EngineMonthSurplusService.setStatusAndCloseTip(baseInfo, monthSurplusMap.get(clothCode), closeOutNum);
			scheduleResult.setProductionStatus(baseInfo.getProductionStatus());
			scheduleResult.setMarkCloseOutTip(baseInfo.getMarkCloseOutTip());

			// 如果是导入功能，需要设置成型相关信息
			if (EngineConstants.SCHEDULE_DATA_SOURCE_IMPORT.equals(scheduleResult.getDataSource())) {
				scheduleResult.setCxBatchNo(baseInfo.getCxBatchNo());
				scheduleResult.setCxClass1Plan(baseInfo.getCxClass1Plan());
				scheduleResult.setCxClass2Plan(baseInfo.getCxClass2Plan());
				scheduleResult.setCxClass3Plan(baseInfo.getCxClass3Plan());
				scheduleResult.setCxClass4Plan(baseInfo.getCxClass4Plan());
				scheduleResult.setCxClass5Plan(baseInfo.getCxClass5Plan());
				// 成型可供时长
				BigDecimal supplyTime = cd90EnginePlanQtyService.caculateSuppliyTime(baseInfo, stockVo);
				scheduleResult.setSupplyTime(supplyTime.doubleValue());
			}
			newScheduleList.add(scheduleResult);
		}
		// 添加日志
		String logDetail = logSplit("月度计划map：:" + toJSONString(monthSurplusMap), "16点半部件库存量map：" + stockMap,
				"插单基础信息map：" + baseInfoMap, "收尾提醒阈值：" + closeOutNum);
		autoScheduleLogService.insertCd90ScheduleLog(batchNo, "", "插单或批量导入基础数据", logDetail);
		// 记录日志
		logDetail = logSplit("插单数据：" + toJSONString(newScheduleList), "异常情况：" + importErrorLogs);
		autoScheduleLogService.insertCd90ScheduleLog(batchNo, "", "插单或导入排程最终数据", logDetail);
		if (newScheduleList.isEmpty()) {
			return -1;
		}
		// 批量创建插单排产记录
		return cd90EngineMapper.mergeCd90ScheduleResult(newScheduleList);
	}

	/**
     *
	 * 90度裁断转机台
     *
	 * @Author hakimryan
	 * @Description
	 * @Date 2021-7-25 11:32:01
	 * @param oldMachineIds  转机台前，旧的机台id
	 * @param scheduleResult 转机台排产记录
	 */
	@Transactional
	@Override
	public void changeCd90Machine(String oldMachineIds, Cd90ScheduleResult scheduleResult) {
		String batchNo = scheduleResult.getBatchNo();
		String orderNo = scheduleResult.getOrderNo();
		// 记录日志
		String logdetail = logSplit("旧机台ID：" + oldMachineIds, "转机台后排产数据：" + toJSONString(scheduleResult));
		autoScheduleLogService.insertCd90ScheduleLog(batchNo, orderNo, "转机台结束后的排程数据", logdetail);
	}

	/**
     *
	 * 确认机台
     *
	 * @param scheduleResult 确认后的排产记录
	 */
	@Transactional
	@Override
	public void confirmCd90Machine(Cd90ScheduleResult scheduleResult) {
		String batchNo = scheduleResult.getBatchNo();
		String orderNo = scheduleResult.getOrderNo();
		// 转机台机台处理前的排程数据json字符串，用于日志记录
		String oldScheduleResult = toJSONString(scheduleResult);

		// 损耗率map
		Map<String, Double> lossRateMap = cd90EngineLossService.getLossRateMap();
		// 获取工序参数map
		Map<String, String> paramsMap = this.getParamsMap();
		double paramLossRate = getDouble(paramsMap.get(EngineConstants.LOSS_RATE));

		// 转机台后，如果损耗率不一样，需要重新计算计划量
		// 取出物料 + 机台对应的损耗率设置
		double lossRate = cd90EngineLossService.getLossRate(scheduleResult.getClothCode(),
				scheduleResult.getMachineId(), lossRateMap, paramLossRate);
		// 原中班计划量
		Double dayPlanQty = Optional.ofNullable(scheduleResult.getDayPlanQty()).orElse(0D);
		// 原晚班计划量
		Double nightPlanQty = Optional.ofNullable(scheduleResult.getNightPlanQty()).orElse(0D);
		// 算上损耗率之后的计划量，公式：新计划量 = 原计划量 + 原计划量 * 损耗率
		dayPlanQty = BigDecimalUtil.add(dayPlanQty, BigDecimalUtil.mul(dayPlanQty, lossRate));
		nightPlanQty = BigDecimalUtil.add(nightPlanQty, BigDecimalUtil.mul(nightPlanQty, lossRate));

		// 处理精度后重新赋值
		// 结果需要向上取整，modify by 20211230
		scheduleResult.setDayPlanQty(BigDecimalUtil.roundUp(dayPlanQty, 0));
		scheduleResult.setNightPlanQty(BigDecimalUtil.roundUp(nightPlanQty, 0));
		// 记录日志
		String logdetail = logSplit("确认机台后的耗损率：" + lossRate, "确认机台前排程数据：" + oldScheduleResult,
				"确认机台后排产数据：" + toJSONString(scheduleResult));
		autoScheduleLogService.insertCd90ScheduleLog(batchNo, orderNo, "确认机台后排程需要根据耗损率重算计划量", logdetail);

	}

	/**
	 * 将指定日期的15度裁断排产结果做平衡处理
     *
	 * @param scheduleDate 排产日期
	 */
	@Transactional
	@Override
	public void handleEquilibrium(Date scheduleDate) {
		// 取出排产日的15度裁断排程记录
		List<Cd90ScheduleResultVo> scheduleList = cd90EngineMapper.selectCd90ScheduleList(scheduleDate);
		if (CollectionUtil.isEmpty(scheduleList)) {
			return;
		}
		// 待处理批次号
		String batchNo = CollectionUtil.firstElement(scheduleList).getBatchNo();
		// 选择机台前的排程数据json字符串，用于日志记录
		String oldScheduleResult = toJSONString(scheduleList);
		for (Cd90ScheduleResultVo schedule : scheduleList) {
			schedule.setBaseVale(schedule.getId());
			// 如果发布次数大于0，则需要将状态更新为待发布
			if (Optional.ofNullable(schedule.getPublishSuccessCount()).orElse(0) > 0) {
				schedule.setIsRelease(ApsConstant.WAIT_RELEASING);
			} else {
				schedule.setIsRelease(ApsConstant.NO_RELEASE);
			}
			// 处理排产计划量，防止二次投产
			cd90EnginePlanQtyService.handleSecondaryProduct(schedule, null);
		}
		// 取出平衡参数
		Map<String, String> paramsMap = this.getParamsMap();
		String planDifferenceRate = paramsMap.get(EngineConstants.PLAN_DIFFERENCE_RATE);
		String supplyTimePass = paramsMap.get(EngineConstants.SUPPLY_TIME_PASS);
		String equalShareThreshold = paramsMap.get(EngineConstants.EQUAL_SHARE_THRESHOLD);
		// 平衡中夜班产量
		cd90EngineEquilibriumService.scheduleEquilibrium(scheduleList, planDifferenceRate, supplyTimePass,
				equalShareThreshold);
		// 计算生产顺序
		cd90EngineProductOrderService.calculateProduceOrder(scheduleList);
		// 更新排程数据至数据库
		cd90EngineMapper.createTempTable();
		cd90EngineMapper.insertTempTable(scheduleList);
		cd90EngineMapper.updateCd90ScheduleResultPlanQty(DateUtils.parseDateToStr("yyyy-MM-dd", scheduleDate), scheduleList);
//		cd90EngineMapper.dropTempTable();
		// 记录日志
		String logdetail = logSplit("平衡前排程数据：" + oldScheduleResult, "平衡后排产数据：" + toJSONString(scheduleList));
		autoScheduleLogService.insertCd90ScheduleLog(batchNo, null, "手工平衡中夜班产量", logdetail);
	}

	/**
	 * 清理排产日当天的历史排程数据
     *
	 * @param scheduleDate 排程日期
	 */
	private void cleanHistoryScheduleResult(Date scheduleDate) {
		cd90EngineMapper.insertCd90ScheduleLog(scheduleDate);
		cd90EngineMapper.deleteCd90ScheduleResult(scheduleDate);
		cd90EngineMapper.deleteCd90ScheduleAssist(scheduleDate);

		Cd90ScheduleRecordVo recordVo = new Cd90ScheduleRecordVo();
		recordVo.setBaseVale(null);
		recordVo.setScheduleDate(scheduleDate);
		cd90EngineMapper.logicDeleteCd90ScheduleRecord(recordVo);
	}

	/**
	 * 验证成型排程记录的胎胚code在施工表中是否都能找到对应记录，如果不能则提示
     *
	 * @param scheduleDate   排程日志
	 * @param batchNo        批次号
	 * @param isProductStage 仅对投产阶段规格排产
	 * @return 外协规格清单map<规格编号, 校验错误信息>
	 */
	private Map<String, String> validatedConstruction(Date scheduleDate, String batchNo, boolean isProductStage) {
		// 获取90度裁断外协清单
		List<Cd90AssistSpec> assistSpecList = cd90EngineMapper.selectCd90AssistSpecList();
		// 本次排产施工不完整的外协规格
		Map<String, String> assistSpecMap = new HashMap<>();
		for (Cd90AssistSpec assistSpec : assistSpecList) {
			assistSpecMap.put(assistSpec.getMaterialCode(), null);
		}
		// 校验成型对应的施工信息是否完整
		List<EngineProductConstructionInfo> constructionList = cd90EngineMapper.listConstructionInfo(scheduleDate);
		for (EngineProductConstructionInfo constructionInfo : constructionList) {
			if (isProductStage && StringUtils.isNotEmpty(constructionInfo.getProductionStage())
					&& !PRODUCTION_STAGE.equals(constructionInfo.getProductionStage())) {
				// 判断如果仅投产阶段开关打开，则将非投产阶段的规格过滤掉
				continue;
			}
			String embryoCode = constructionInfo.getEmbryoCode();
			String version = constructionInfo.getEmbryoVersion();
			// 校验成型是否有选版本
			if (StringUtils.isEmpty(version)) {
				String tip = I18nUtil.getMessage("engine.auto.scheule.validated.noversion");
				tip = String.format(tip, embryoCode);
				autoScheduleLogService.insertCd15ScheduleLog(batchNo, "", "自动排程失败", tip);
				throw new RuntimeException(tip);
			}
			// 校验施工是否存在
			if (constructionInfo.getId() == null) {
				String tip = I18nUtil.getMessage("engine.auto.scheule.validated");
				tip = String.format(tip, embryoCode + "$" + version);
				autoScheduleLogService.insertCd15ScheduleLog(batchNo, "", "自动排程失败", tip);
				throw new RuntimeException(tip);
			}
			String errorMsg = null;
			// 1#胎体布编号
			if (StringUtil.isEmpty(constructionInfo.getTireFabricCode1())) {
				errorMsg = this.returnValidatedErrorMsg(batchNo, embryoCode, "ui.construction.tireFabricCode1");
				if (StringUtils.isNotEmpty(errorMsg)) {
					this.returnErrorMessage(errorMsg);
				}
			}
			validate: {
				// 帘线规格
//				if (StringUtil.isEmpty(constructionInfo.getCordSpec())) {
//					errorMsg = this.returnValidatedErrorMsg(batchNo, embryoCode, "ui.construction.cordSpec");
//				}
//				if (StringUtils.isNotEmpty(errorMsg)) {
//					break validate;
//				}
				// 胎侧长度
				if (constructionInfo.getSidewallLength() == null || constructionInfo.getSidewallLength() == 0) {
					errorMsg = this.returnValidatedErrorMsg(batchNo, embryoCode, "ui.construction.sidewallLength");
				}
			}
			// 如果有校验失败，则判断物料号（一号帘布编号）是否在外协规格清单中
			String materialCode = constructionInfo.getTireFabricCode1();
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
	 * 生成外协计划列表
     *
	 * @param scheduleDate   排产日
	 * @param isProductStage 是否只排生产阶段
	 * @param scheduleList   原排产计划
	 * @param batchNo        批次号
	 * @param assistSpecMap  外协规格清单map
	 * @return
	 */
	private List<Cd90ScheduleResultVo> createScheduleAssistList(Date scheduleDate, boolean isProductStage,
			List<Cd90ScheduleResultVo> scheduleList, String batchNo, Map<String, String> assistSpecMap) {
		List<Cd90ScheduleResultVo> scheduleAssistList;
		if (!assistSpecMap.isEmpty()) {
			// 将正常排程计划中的外协规格转移出来
			scheduleAssistList = this.removeAssistSpec(scheduleList, assistSpecMap);
			// 初始化外协排程计划记录
			for (Cd90ScheduleResultVo scheduleVo : scheduleAssistList) {
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
				String message = assistSpecMap.get(scheduleVo.getClothCode());
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
	private List<Cd90ScheduleResultVo> removeAssistSpec(List<Cd90ScheduleResultVo> scheduleList,
			Map<String, String> assistSpecMap) {
		List<Cd90ScheduleResultVo> list = new ArrayList<>();
		if (!assistSpecMap.isEmpty()) {
			for (int i = scheduleList.size() - 1; i >= 0; i--) {
				Cd90ScheduleResultVo scheduleVo = scheduleList.get(i);
				if (assistSpecMap.containsKey(scheduleVo.getClothCode())) {
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
	 * 反馈校验错误信息
     *
	 * @param batchNo       排产批次号
	 * @param embryoCode    胎号
	 * @param columnNameKey 校验字段名称key（国际化）
	 */
	private String returnValidatedErrorMsg(String batchNo, String embryoCode, String columnNameKey) {
		String columnName = I18nUtil.getMessage(columnNameKey);
		String logMsg = "自动排程失败，原因：胎胚“{}”的施工信息不完整：{}为空。";
		autoScheduleLogService.insertCd90ScheduleLog(batchNo, "", "自动排程失败",
				StringUtils.format(logMsg, embryoCode, columnName));
		String tip = StringUtils.format(I18nUtil.getMessage("mes.error.message.auto.schedule.validate"), embryoCode,
				columnName);
		return tip;
	}

	/**
	 * 创建自动排程记录
     *
	 * @Author hakimryan
	 * @Description
	 * @Date 2021-7-14 10:54:58
	 * @Param
	 * @Return
	 */
	private void createScheduleRecord(Date scheduleDate, String cxBatchNo, String batchNo) {
		Cd90ScheduleRecordVo recordVo = new Cd90ScheduleRecordVo();
		recordVo.setScheduleDate(scheduleDate);
		recordVo.setCxBatchNo(cxBatchNo);
		recordVo.setBatchNo(batchNo);
		recordVo.setStatus(ApsConstant.STATUS_ENABLE);
		recordVo.setBaseVale(null);
		cd90EngineMapper.insertScheduleRecord(recordVo);
	}

	/**
	 * 创建批次号
     *
	 * @Author hakimryan
	 * @Description
	 * @Date 2021-7-14 10:02:41
	 * @Param scheduleDate 排产日期
	 * @Return
	 */
	private String createBatchNo(Date scheduleDate) {
		String strScheduleDate = new SimpleDateFormat("yyyyMMdd").format(scheduleDate);
		return incrementService.getSequence3(EngineConstants.CD90_BATCH_NO_PREFIX + strScheduleDate);
	}

	/**
	 * 创建工单号
     *
	 * @Author hakimryan
	 * @Description
	 * @Date 2021-7-14 10:02:41
	 * @Param batchNo 批次号
	 * @Return
	 */
	private String createOrderNo(String batchNo) {
		return incrementService.getSequence4(batchNo);
	}

	/**
	 * 获取工序参数map
     *
	 * @Author hakimryan
	 * @Description
	 * @Date 2021-8-10 16:09:34
	 * @return
	 */
	private Map<String, String> getParamsMap() {
		return cd90EngineMapper.listCd90Params().stream()
				.collect(Collectors.toMap(Cd90ParamsVo::getParamCode, Cd90ParamsVo::getParamValue, (v1, v2) -> v2));
	}

    @Override
    public void batchUpdateBatchNoAndOrderNo(Date scheduleDate) {
        List<Cd90ScheduleResultVo> scheduleResultVoList = cd90EngineMapper.selectCd90ScheduleList(scheduleDate);
        //查询当前排程的批次号
        String batchNo = cd90EngineMapper.getCurrentBatchNo(scheduleDate);
        if (StringUtils.isBlank(batchNo)) {
            //当前的批次号为空，说明还没”自动排程“或者做的批量导入（需要删掉已排的数据），那么自己生成一个排程批次号
            //排程批次号
            batchNo = this.createBatchNo(scheduleDate);
            //创建自动排程记录
            this.createScheduleRecord(scheduleDate, "", batchNo);
        }
        for (Cd90ScheduleResultVo scheduleResult : scheduleResultVoList) {
            //批次号
            scheduleResult.setBatchNo(batchNo);
            //工单号
            String orderNo = this.createOrderNo(batchNo);
            scheduleResult.setOrderNo(orderNo);
        }

		if (org.apache.commons.collections.CollectionUtils.isNotEmpty(scheduleResultVoList)) {
			cd90EngineMapper.batchUpdateBatchNoAndOrderNo(scheduleResultVoList);
		}
    }
}
