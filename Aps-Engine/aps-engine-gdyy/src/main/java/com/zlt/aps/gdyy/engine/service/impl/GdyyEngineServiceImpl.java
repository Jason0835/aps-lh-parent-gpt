package com.zlt.aps.gdyy.engine.service.impl;

import static com.alibaba.fastjson.JSON.toJSONString;
import static com.zlt.aps.common.core.utils.ApsCommonUtil.getDoubleOrDefault;
import static com.zlt.aps.common.core.utils.ApsCommonUtil.logSplit;
import static com.zlt.aps.common.core.utils.ImportUtil.addImportErrorLog;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.annotation.Resource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.github.pagehelper.util.StringUtil;
import com.ruoyi.api.gateway.system.domain.ImportErrorLog;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.utils.StringUtils;
import com.zlt.aps.common.core.constant.ApsConstant;
import com.zlt.aps.common.engine.constants.EngineConstants;
import com.zlt.aps.common.engine.domain.EngineConstructionInfo;
import com.zlt.aps.common.engine.service.AutoScheduleLogService;
import com.zlt.aps.common.engine.service.impl.IncrementService;
import com.zlt.aps.common.engine.utils.CollectionUtil;
import com.zlt.aps.gdyy.api.domain.dto.GdyyScheduleResultDto;
import com.zlt.aps.gdyy.engine.mapper.GdyyEngineMapper;
import com.zlt.aps.gdyy.engine.mapper.GdyyEngineStockMapper;
import com.zlt.aps.gdyy.engine.service.GdyyEngineMonthSurplusService;
import com.zlt.aps.gdyy.engine.service.GdyyEnginePlanQtyService;
import com.zlt.aps.gdyy.engine.service.GdyyEngineService;
import com.zlt.aps.gdyy.engine.vo.GdyyDayUsedVo;
import com.zlt.aps.gdyy.engine.vo.GdyyMonthSurplusVo;
import com.zlt.aps.gdyy.engine.vo.GdyyParamsVo;
import com.zlt.aps.gdyy.engine.vo.GdyyScheduleRecordVo;
import com.zlt.aps.gdyy.engine.vo.GdyyScheduleResultVo;

import lombok.extern.slf4j.Slf4j;

/**
 * 钢带压延自动排程服务实现类
 * 
 * @Description
 * @Author hakimrayn
 * @Date 2021-7-19 10:36:57
 * @Version 1.0
 */
@Service("gdyyEngineService")
@Slf4j
public class GdyyEngineServiceImpl implements GdyyEngineService {
	/**
	 * 幅宽默认值：1
	 */
	private final static Double DEFAULT_BREADTH = new Double("1");
	/**
	 * 大卷标准长度默认值：600
	 */
	private final static Double DEFAULT_STANDARD_SIZE = new Double("600");
	/**
	 * 一百，用于百分比 -> 小数的单位换算
	 */
	private final static BigDecimal ONE_HUNDRED = new BigDecimal("100");

	@Autowired
	private GdyyEngineMapper gdyyEngineMapper;
	@Autowired
	private GdyyEngineStockMapper gdyyEngineStockMapper;
	@Autowired
	private GdyyEnginePlanQtyService gdyyEnginePlanQtyService;
	@Autowired
	private GdyyEngineMonthSurplusService gdyyEngineMonthSurplusService;
	@Resource
	private IncrementService incrementService;
	@Resource
	private AutoScheduleLogService autoScheduleLogService;
	/**
	 * 生产阶段校验开关状态：打开
	 */
	private final static String PRODUCTION_STAGE_ON = "1";

	/**
	 * 自动排程
	 * 
	 * @Author hakimryan
	 * @Description
	 * @Date 2021-7-19 11:26:33
	 * @Param scheduleDate 排产日期
	 * @Return
	 */
	@Transactional
	@Override
	public void autoGdyySchedule(Date scheduleDate) {
		// 加载系统参数
		Map<String, String> paramsMap = this.getParamsMap();
		// 获取系统参数配置
		// 幅宽
		Double breadth = getDoubleOrDefault(paramsMap.get(EngineConstants.BREADTH), DEFAULT_BREADTH);
		// 标准大卷长度默认值
		Double standardSize = getDoubleOrDefault(paramsMap.get(EngineConstants.STANDARD_SIZE), DEFAULT_STANDARD_SIZE);
		// 本次排程批次号
		String batchNo = this.createBatchNo(scheduleDate);
		// 对应的成型批次号
		String cxBatchNo = "";
		// 判断仅对投产阶段规格排产是否打开
		boolean isProductStage = PRODUCTION_STAGE_ON
				.equals(paramsMap.getOrDefault(EngineConstants.PRODUCTION_STAGE_PRODUCE, PRODUCTION_STAGE_ON));

		/* 根据排程日期，从15度裁断排产计划中生成钢带压延排产的信息 */
		List<GdyyScheduleResultVo> scheduleList = gdyyEngineMapper.selectGdyyScheduleBaseList(scheduleDate, breadth,
				isProductStage);
		if (CollectionUtil.isEmpty(scheduleList)) {
			log.info("根据15度裁断排程记录为空，无法生成钢带压延排产");
			autoScheduleLogService.insertGdyyScheduleLog(batchNo, "", "自动排程失败",
					"自动排程失败，原因：15度裁断排程记录为空，或没有在施工信息中找到对应的物料");
			throw new RuntimeException(I18nUtil.getMessage("engine.auto.scheule.tip1"));
		}
		// 记录日志
		autoScheduleLogService.insertGdyyScheduleLog(batchNo, "", "1.1、根据成'型排程记录'统计出钢带压延排程记录基础数据",
				toJSONString(scheduleList));
		validatedConstruction(scheduleDate, batchNo, isProductStage);
		// 取出当天已经发布过的排程记录
		Map<String, GdyyScheduleResultVo> isReleaseMap = this.selectIsReleaseScheduleResult(scheduleDate);
		// 初始化排程明细，要结合当天已经发布过的排程记录设值
		this.initScheduleReulstList(batchNo, scheduleList, isReleaseMap);
		// 记录日志
		if (!isReleaseMap.isEmpty()) {
			autoScheduleLogService.insertCd90ScheduleLog(batchNo, "", "1.1、存在上次已发布的排程记录", toJSONString(isReleaseMap));
		}

		/**
		 * 处理日用参考量
		 */
		// 取出排产记录的所有大卷编号
		List<String> bigRollCodeList = scheduleList.stream().map(GdyyScheduleResultVo::getBigRollCode)
				.collect(Collectors.toList());
		// 计算各大卷对应的日用参考值
		Map<String, Double> dayUsedMap = this.caculateDayUsed(scheduleDate, breadth, bigRollCodeList, isProductStage);
		// 根据大卷编号更新日用参考量
		scheduleList.stream().forEach(v -> {
			v.setDayUsed(dayUsedMap.getOrDefault(v.getBigRollCode(), 0D));
		});
		// 记录日志
		autoScheduleLogService.insertGdyyScheduleLog(batchNo, "", "1.2、计算日用参考量",
				"各大卷的日用参考量：" + toJSONString(dayUsedMap));

		/**
		 * 计算计划量相关信息，包括库存量、重算计划量
		 */
		String stockRatio = paramsMap.get(EngineConstants.STOCK_RATIO);
		String lossRate = paramsMap.get(EngineConstants.LOSS_RATE);
		// 库存是否按大卷计算
		boolean isRoll = EngineConstants.GDYY_STOCK_ROLL_SWITCH_ON
				.equals(paramsMap.get(EngineConstants.GDYY_STOCK_ROLL_SWITCH));
		// 获取库存损耗率
		BigDecimal stockLossRate = this.getStockLossRate(paramsMap);
		gdyyEnginePlanQtyService.calculateSchedulePlanQty(scheduleDate, scheduleList, stockRatio, lossRate,
				stockLossRate, standardSize, isRoll, breadth, isProductStage);

		/**
		 * 根据月度计划量设置收尾备注等信息
		 */
		String minRemainQty = paramsMap.get(EngineConstants.CLOSE_OUT_NUM);
		gdyyEngineMonthSurplusService.calculateMonthSurplus(scheduleDate, scheduleList, minRemainQty);

		// 如果还存在上次已发布的规格但本次没有排程的规格，直接将这些规格的排程信息复制到本次排程中
		for (GdyyScheduleResultVo scheduleVo : isReleaseMap.values()) {
			scheduleVo.setBatchNo(batchNo);
			scheduleVo.setBaseVale(null);
			scheduleList.add(scheduleVo);
		}

		// 将当天的历史排程记录（上一次的）转移至日志表
		this.cleanHistoryScheduleResult(scheduleDate);

		// 创建自动排产记录
		this.createScheduleRecord(scheduleDate, cxBatchNo, batchNo);

		// 保存排产结果明细记录
		gdyyEngineMapper.insertScheduleResultList(scheduleList);

		/**
		 * 记录日志
		 */
		autoScheduleLogService.insertGdyyScheduleLog(batchNo, "", "4.1、最终排产结果", toJSONString(scheduleList));
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
	 * 取出当天已经发布过的排程记录
	 * 
	 * @param scheduleDate 排产日
	 * @return
	 */
	private Map<String, GdyyScheduleResultVo> selectIsReleaseScheduleResult(Date scheduleDate) {
		// 取出本次自动排程前已发布的规格以及其工单号，组成map<钢带编号，工单号>，用于判断是否需要保留
		Map<String, GdyyScheduleResultVo> isReleaseMap = gdyyEngineMapper.selectGdyyScheduleList(scheduleDate).stream()
				// 过滤出曾经发布成功的记录
				.filter(v -> v.getPublishSuccessCount() != null && v.getPublishSuccessCount() > 0)
				// 取出钢带表编号
				.collect(Collectors.toMap(GdyyScheduleResultVo::getBigRollCode, Function.identity(), (v1, v2) -> v2));
		return isReleaseMap;
	}

	/**
	 * 初始化排程明细
	 * 
	 * @param batchNo      排程批次号
	 * @param scheduleList 排程明细列表
	 * @param isReleaseMap 已发布排程列表
	 */
	private void initScheduleReulstList(String batchNo, List<GdyyScheduleResultVo> scheduleList,
			Map<String, GdyyScheduleResultVo> isReleaseMap) {
		// 为本次的排产记录创建批次号与工单号
		for (GdyyScheduleResultVo scheduleVo : scheduleList) {
			String bigRollCode = scheduleVo.getBigRollCode();
			scheduleVo.setBatchNo(batchNo);
			// 先判断该钢带是否之前已经发布过
			if (isReleaseMap.containsKey(bigRollCode)) {
				// 发布过，则取出该规格（从map中移除）
				GdyyScheduleResultVo isReleaseVo = isReleaseMap.remove(bigRollCode);
				// 复制工单号
				scheduleVo.setOrderNo(isReleaseVo.getOrderNo());
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
	 * 钢带压延度裁断插单
	 * 
	 * @Author hakimryan
	 * @Description
	 * @Date 2021-7-28 09:31:42
	 * @param scheduleResult 插单记录
	 */
	@Override
	public int insertGdyyOrder(GdyyScheduleResultDto scheduleResult) {
		// 排程日期
		Date scheduleDate = scheduleResult.getScheduleDate();
		// 查询当前排程的批次号
		String batchNo = gdyyEngineMapper.getCurrentBatchNo(scheduleDate);
		if (StringUtils.isBlank(batchNo)) {
			// 当前的批次号为空，则新生成排程批次号
			batchNo = this.createBatchNo(scheduleDate);
			// 创建自动排程记录
			this.createScheduleRecord(scheduleDate, "", batchNo);
		}

		List<GdyyScheduleResultDto> scheduleList = new ArrayList<>();
		scheduleList.add(scheduleResult);
		// 导入异常记录
		List<ImportErrorLog> importErrorLogs = new ArrayList<>();
		int result = this.batchUpdateOrInsertGdyySchedule(batchNo, scheduleDate, scheduleList, importErrorLogs);
		// 如果插单有产生异常记录，则向前端返回异常信息
		importErrorLogs.stream().findAny().ifPresent(v -> {
			throw new RuntimeException(v.getErrorDetail());
		});
		return result;
	}

	/**
	 * 批量导入的钢带压延排程记录
	 * 
	 * @param scheduleDate 排程日志
	 * @param scheduleList 排程数据
	 * @return 导入异常日志
	 */
	@Transactional
	@Override
	public List<ImportErrorLog> batchSaveGdyySchedule(Date scheduleDate, List<GdyyScheduleResultDto> scheduleList) {
		// 批量导入前要先清除历史数据
		this.cleanHistoryScheduleResult(scheduleDate);
		// 创建新排程记录，批次号也重新生成
		String batchNo = this.createBatchNo(scheduleDate);
		// 导入异常记录
		List<ImportErrorLog> importErrorLogs = new ArrayList<>();
		// 生成新的排程明细
		this.batchUpdateOrInsertGdyySchedule(batchNo, scheduleDate, scheduleList, importErrorLogs);
		// 生成新批次时，如果明细能关联到成型信息，则也需要将成型信息保存到批次记录中
		String cxBatchNo = "";
		for (GdyyScheduleResultDto schedule : scheduleList) {
			if (StringUtils.isNotEmpty(schedule.getCxBatchNo())) {
				cxBatchNo = schedule.getCxBatchNo();
				break;
			}
		}
		this.createScheduleRecord(scheduleDate, cxBatchNo, batchNo);
		return importErrorLogs;
	}

	/**
	 * 批量保存钢带压延排程记录
	 * 
	 * @param batchNo         批次号
	 * @param scheduleDate    排程日志
	 * @param scheduleList    排程数据
	 * @param importErrorLogs 导入异常日志
	 */
	private int batchUpdateOrInsertGdyySchedule(String batchNo, Date scheduleDate,
			List<GdyyScheduleResultDto> scheduleList, List<ImportErrorLog> importErrorLogs) {
		// 加载系统参数
		Map<String, String> paramsMap = this.getParamsMap();
		// 月度计划信息
		Map<String, GdyyMonthSurplusVo> monthSurplusMap = gdyyEngineMonthSurplusService
				.getMonthSurplusMap(scheduleDate);
		// 参数：收尾提醒阈值
		String closeOutNum = paramsMap.get(EngineConstants.CLOSE_OUT_NUM);
		// 参数：幅宽
		Double breadth = getDoubleOrDefault(paramsMap.get(EngineConstants.BREADTH), DEFAULT_BREADTH);
		// 取出本次导入排产记录所有的大卷编号
		List<String> bigRollCodeList = scheduleList.stream().map(GdyyScheduleResultDto::getBigRollCode)
				.collect(Collectors.toList());
		// 计算各大卷日用参考量
		Map<String, Double> dayUsedMap = this.caculateDayUsed(scheduleDate, breadth, bigRollCodeList, false);
		// 插单基础信息
		Map<String, GdyyScheduleResultVo> baseInfoMap = gdyyEngineMapper
				.listInsertOrderBaseInfo(scheduleList, scheduleDate).stream()
				.collect(Collectors.toMap(GdyyScheduleResultVo::getBigRollCode, Function.identity()));

		// 库存是否按大卷计算
		boolean isRoll = EngineConstants.GDYY_STOCK_ROLL_SWITCH_ON
				.equals(paramsMap.get(EngineConstants.GDYY_STOCK_ROLL_SWITCH));
		// 标准大卷长度默认值
		Double standardSize = getDoubleOrDefault(paramsMap.get(EngineConstants.STANDARD_SIZE), DEFAULT_STANDARD_SIZE);
		// 判断仅对投产阶段规格排产是否打开
		boolean isProductStage = PRODUCTION_STAGE_ON
				.equals(paramsMap.getOrDefault(EngineConstants.PRODUCTION_STAGE_PRODUCE, PRODUCTION_STAGE_ON));
		// 获取库存损耗率
		BigDecimal stockLossRate = this.getStockLossRate(paramsMap);
		// 取出当天各大卷库存信息（实际是上一天的库存）20211030
		Map<String, Double> stockMap = gdyyEnginePlanQtyService.getStockQtyMap(scheduleDate, stockLossRate,
				standardSize, isRoll);
		// 收尾规格列表
		List<String> closeOutSpecList = gdyyEngineStockMapper.listCloseOutSpec(scheduleDate, isProductStage);

		// 添加日志
		String logDetail = logSplit("月度计划map：:" + toJSONString(monthSurplusMap), "插单基础信息map：" + baseInfoMap,
				"收尾提醒阈值：" + closeOutNum);
		autoScheduleLogService.insertGdyyScheduleLog(batchNo, "", "插单或批量导入基础数据", logDetail);

		// 批量导入的初始行号，由于模板是从第3行开始是数据行，因此初始值是2
		int rowNum = 2;
		List<GdyyScheduleResultDto> newScheduleList = new ArrayList<>();
		// 遍历入参排程记录
		for (GdyyScheduleResultDto scheduleResult : scheduleList) {
			rowNum++;
			// 大卷编号
			String bigRollCode = scheduleResult.getBigRollCode();
			// 取出插单基础信息
			GdyyScheduleResultVo scheduleVo = baseInfoMap.get(bigRollCode);
			if (scheduleVo == null) {
				// 取不到说明物料编号有误，需要记录错误信息
				addImportErrorLog(null, rowNum, I18nUtil.getMessage("ui.error.message.column.materialCodeNotExist"),
						importErrorLogs);
				continue;
			}

			scheduleResult.setBatchNo(batchNo);
			scheduleResult.setOrderNo(this.createOrderNo(batchNo));
			scheduleResult.setDayUsed(dayUsedMap.getOrDefault(bigRollCode, 0D));
			// 库存直接取当天库存，不从前端接收该数值 20211030
			scheduleResult.setStockQty(Math.floor(stockMap.getOrDefault(bigRollCode, 0D)));
			scheduleResult.setClass1Plan(Optional.ofNullable(scheduleResult.getClass1Plan()).orElse(0D));
			scheduleResult.setClass2Plan(Optional.ofNullable(scheduleResult.getClass2Plan()).orElse(0D));
			scheduleResult.setClass3Plan(Optional.ofNullable(scheduleResult.getClass3Plan()).orElse(0D));
			scheduleResult.setClass1PlanNum(Optional.ofNullable(scheduleResult.getClass1PlanNum()).orElse(0D));
			scheduleResult.setClass2PlanNum(Optional.ofNullable(scheduleResult.getClass2PlanNum()).orElse(0D));
			scheduleResult.setClass3PlanNum(Optional.ofNullable(scheduleResult.getClass3PlanNum()).orElse(0D));
			scheduleResult.setClass1PlanNoStock(Optional.ofNullable(scheduleResult.getClass1PlanNoStock()).orElse(0D));
			scheduleResult.setClass2PlanNoStock(Optional.ofNullable(scheduleResult.getClass2PlanNoStock()).orElse(0D));
			scheduleResult.setClass3PlanNoStock(Optional.ofNullable(scheduleResult.getClass3PlanNoStock()).orElse(0D));
			scheduleResult.setIsRelease(ApsConstant.NO_RELEASE);
			scheduleResult.setDelFlag(ApsConstant.DEL_FLAG_NORMAL);
			scheduleResult.setCloseOutSpecFlag(
					closeOutSpecList.contains(bigRollCode) ? ApsConstant.STATUS_ENABLE : ApsConstant.STATUS_DISABLE);
			// 通过接口获取设置收尾提示标识 和 生产状态字段
			scheduleVo.setDayUsed(scheduleResult.getDayUsed());
			gdyyEngineMonthSurplusService.setStatusAndCloseTip(scheduleVo,
					monthSurplusMap.get(scheduleVo.getBigRollCode()), closeOutNum);
			scheduleResult.setProductionStatus(scheduleVo.getProductionStatus());
			scheduleResult.setMarkCloseOutTip(scheduleVo.getMarkCloseOutTip());

			// 如果是导入功能，需要设置成型相关信息
			if (EngineConstants.SCHEDULE_DATA_SOURCE_IMPORT.equals(scheduleResult.getDataSource())) {
				scheduleResult.setCxBatchNo(scheduleVo.getCxBatchNo());
			}

			newScheduleList.add(scheduleResult);
		}
		// 记录日志
		logDetail = logSplit("插单数据：" + toJSONString(newScheduleList), "异常情况：" + importErrorLogs);
		autoScheduleLogService.insertGdyyScheduleLog(batchNo, "", "插单或导入排程最终数据", logDetail);
		if (newScheduleList.isEmpty()) {
			// 如果没有一条导入成功，返回失败标志
			return -1;
		}
		// 批量创建插单排产记录
		return gdyyEngineMapper.mergeGdyyScheduleResult(scheduleList);
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
		List<GdyyDayUsedVo> dayUsedList = gdyyEngineMapper.listGdyyDayUsed(scheduleDate, breadth, bigRollCodeList,
				isProductStage);
		// 转换成map
		return dayUsedList.stream().collect(Collectors.toMap(GdyyDayUsedVo::getBigRollCode, GdyyDayUsedVo::getDayUsed));
	}

	/**
	 * 清理排产日当天的历史排程数据
	 * 
	 * @param scheduleDate 排程日期
	 */
	private void cleanHistoryScheduleResult(Date scheduleDate) {
		gdyyEngineMapper.insertGdyyScheduleLog(scheduleDate);
		gdyyEngineMapper.deleteGdyyScheduleResult(scheduleDate);
		GdyyScheduleRecordVo recordVo = new GdyyScheduleRecordVo();
		recordVo.setBaseVale(null);
		recordVo.setScheduleDate(scheduleDate);
		gdyyEngineMapper.logicDeleteGdyyScheduleRecord(recordVo);

	}

	/**
	 * 验证成型排程记录的胎胚code在施工表中是否都能找到对应记录，如果不能则提示
	 * 
	 * @param scheduleDate   排程日志
	 * @param batchNo        批次号
	 * @param isProductStage 仅对投产阶段规格排产
	 */
	private void validatedConstruction(Date scheduleDate, String batchNo, boolean isProductStage) {
		List<String> list = gdyyEngineMapper.listLossConstructionForCd15(scheduleDate);
		if (list != null && !list.isEmpty()) {
			String tip = I18nUtil.getMessage("engine.auto.scheule.validated");
			String embryoCodes = String.join(",", list);
			tip = String.format(tip, embryoCodes);
			autoScheduleLogService.insertGdyyScheduleLog(batchNo, "", "自动排程失败",
					"自动排程失败，原因：15度裁断排程数据为空，或没有在施工信息中找到对应的物料");
			throw new RuntimeException(tip);
		}
		// 校验成型对应的施工信息是否完整
		List<EngineConstructionInfo> constructionList = gdyyEngineMapper.listConstructionInfo(scheduleDate,
				isProductStage);
		for (EngineConstructionInfo constructionInfo : constructionList) {
			String embryoCode = constructionInfo.getEmbryoCode();
			// 钢带规格
			if (StringUtil.isEmpty(constructionInfo.getArticleCrownSpec())) {
				this.returnValidatedErrorMsg(batchNo, embryoCode, "ui.construction.articleCrownSpec");
			}
			// 贴合鼓周长
			if (constructionInfo.getFitDrumPerimeter() == null || constructionInfo.getFitDrumPerimeter() == 0L) {
				this.returnValidatedErrorMsg(batchNo, embryoCode, "ui.construction.fitDrumPerimeter");
			}
			// 裁断角度
			if (StringUtil.isEmpty(constructionInfo.getBeltCuttingAngle())) {
				this.returnValidatedErrorMsg(batchNo, embryoCode, "ui.construction.beltCuttingAngle");
			}
			// 1#钢带编号
			if (StringUtil.isEmpty(constructionInfo.getBeltCode1())) {
				this.returnValidatedErrorMsg(batchNo, embryoCode, "ui.construction.beltCode1");
			}
			// 1#钢带工艺
			if (constructionInfo.getBeltCraft1() == null || constructionInfo.getBeltCraft1() == 0L) {
				this.returnValidatedErrorMsg(batchNo, embryoCode, "ui.construction.beltCraft1");
			}
			// 2#钢带编号
			if (StringUtil.isEmpty(constructionInfo.getBeltCode2())) {
				this.returnValidatedErrorMsg(batchNo, embryoCode, "ui.construction.beltCode2");
			}
		}
	}

	/**
	 * 反馈校验错误信息
	 * 
	 * @param batchNo       排产批次号
	 * @param embryoCode    胎号
	 * @param columnNameKey 校验字段名称key（国际化）
	 */
	private void returnValidatedErrorMsg(String batchNo, String embryoCode, String columnNameKey) {
		String columnName = I18nUtil.getMessage(columnNameKey);
		String logMsg = "自动排程失败，原因：胎胚“{}”的施工信息不完整：{}为空。";
		autoScheduleLogService.insertGdyyScheduleLog(batchNo, "", "自动排程失败",
				StringUtils.format(logMsg, embryoCode, columnName));
		String tip = StringUtils.format(I18nUtil.getMessage("mes.error.message.auto.schedule.validate"), embryoCode,
				columnName);
		throw new RuntimeException(tip);
	}

	/**
	 * 获取工序参数map
	 * 
	 * @return
	 */
	private Map<String, String> getParamsMap() {
		return gdyyEngineMapper.listGdyyParams().stream()
				.collect(Collectors.toMap(GdyyParamsVo::getParamCode, GdyyParamsVo::getParamValue, (v1, v2) -> v2));
	}

	/**
	 * 创建自动排程记录
	 * 
	 * @Author hakimryan
	 * @Description
	 * @Date 2021-7-19 10:54:58
	 * @Param
	 * @Return
	 */
	private void createScheduleRecord(Date scheduleDate, String cxBatchNo, String batchNo) {
		GdyyScheduleRecordVo recordVo = new GdyyScheduleRecordVo();
		recordVo.setScheduleDate(scheduleDate);
		recordVo.setCxBatchNo(cxBatchNo);
		recordVo.setBatchNo(batchNo);
		recordVo.setStatus(ApsConstant.STATUS_ENABLE);
		recordVo.setBaseVale(null);
		gdyyEngineMapper.insertScheduleRecord(recordVo);
	}

	/**
	 * 创建批次号
	 * 
	 * @Author hakimryan
	 * @Description
	 * @Date 2021-7-19 10:02:41
	 * @Param scheduleDate 排产日期
	 * @Return
	 */
	private String createBatchNo(Date scheduleDate) {
		String strScheduleDate = new SimpleDateFormat("yyyyMMdd").format(scheduleDate);
		return incrementService.getSequence3(EngineConstants.GDYY_BATCH_NO_PREFIX + strScheduleDate);
	}

	/**
	 * 创建工单号
	 * 
	 * @Author hakimryan
	 * @Description
	 * @Date 2021-7-19 10:02:41
	 * @Param batchNo 批次号
	 * @Return
	 */
	private String createOrderNo(String batchNo) {
		return incrementService.getSequence4(batchNo);
	}
}
