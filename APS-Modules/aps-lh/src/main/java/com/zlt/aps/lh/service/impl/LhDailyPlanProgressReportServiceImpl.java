package com.zlt.aps.lh.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.aps.common.engine.utils.MonthPlanSurplusCalculator;
import com.zlt.aps.constant.FactoryConstant;
import com.zlt.aps.lh.api.domain.dto.LhDailyPlanProgressReportDTO;
import com.zlt.aps.lh.api.domain.entity.LhMouldChangePlan;
import com.zlt.aps.lh.api.domain.entity.LhScheduleResult;
import com.zlt.aps.lh.api.domain.vo.LhDailyPlanProgressReportRowVO;
import com.zlt.aps.lh.api.domain.vo.LhSkuSurplusVO;
import com.zlt.aps.lh.api.enums.DeleteFlagEnum;
import com.zlt.aps.lh.api.enums.TrialStatusEnum;
import com.zlt.aps.lh.component.LhDailyPlanProgressAssembler;
import com.zlt.aps.lh.component.LhScheduleContextInitializer;
import com.zlt.aps.lh.component.MonthPlanDateResolver;
import com.zlt.aps.lh.context.LhScheduleContext;
import com.zlt.aps.lh.handler.ScheduleAdjustHandler;
import com.zlt.aps.lh.mapper.LhMouldChangePlanEntityMapper;
import com.zlt.aps.lh.mapper.LhScheduleResultMapper;
import com.zlt.aps.lh.mapper.MdmSkuConstructionRefMapper;
import com.zlt.aps.lh.service.ILhBaseDataService;
import com.zlt.aps.lh.service.ILhDailyMouldCalcService;
import com.zlt.aps.lh.service.ILhDailyPlanProgressReportService;
import com.zlt.aps.lh.util.LhDailyPlanProgressExcelUtil;
import com.zlt.aps.lh.util.LhScheduleTimeUtil;
import com.zlt.aps.lh.util.MonthPlanDayQtyUtil;
import com.zlt.aps.maindata.mapper.MdmMaterialConsumeDetailMapper;
import com.zlt.aps.mdm.api.domain.entity.MdmSkuConstructionRef;
import com.zlt.aps.mp.api.domain.entity.FactoryMonthPlanProductionFinalResult;
import com.zlt.aps.mp.api.domain.entity.MdmMaterialConsumeDetail;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 日排产进度报表服务实现。
 *
 * <p>取数主线：</p>
 * <ol>
 *   <li>按排程日期复用排程上下文时间轴口径，得到 T 日、T+2 日；</li>
 *   <li>复用硫化余量基础数据加载（月计划含跨月、完成量、日计划调整、工作日历），
 *       先按「次月月末」临时末端加载，再按次月定稿版本决定报表末端是否跨月；</li>
 *   <li>基准取 t_lh_schedule_result 的 class6/7/8（含停产保机），
 *       月计划每日机台数复用 {@code getMouldByDay}（返回值 /2 即机台数）；</li>
 *   <li>T+2日排产信息取同批次 t_lh_mould_change_plan 的 plan_date 落在 T+2 的记录；</li>
 *   <li>余量/合格产量按生产月定稿行调用排程算法重算，保证与非基准 SKU 同口径。</li>
 * </ol>
 *
 * @author APS
 */
@Slf4j
@Service
public class LhDailyPlanProgressReportServiceImpl implements ILhDailyPlanProgressReportService {

    /** 日期格式 */
    private static final String DATE_PATTERN = "yyyy-MM-dd";

    /** 逻辑删除：未删除 */
    private static final int NOT_DELETED = DeleteFlagEnum.NORMAL.getCode();

    /** 胶种原材料描述前缀（BOM 消耗明细） */
    private static final String GLUE_MATERIAL_NAME_PREFIX = "AQT";

    /** 胶种展示时去掉的编码前缀 */
    private static final String GLUE_CODE_PREFIX = "AQ";

    /** 结构类型：周期 */
    private static final String STRUCTURE_TYPE_CYCLE = "01";

    /** 结构类型：常规 */
    private static final String STRUCTURE_TYPE_NORMAL = "02";

    /** IN 查询分批大小，避免超长 SQL */
    private static final int IN_QUERY_BATCH_SIZE = 900;

    /** 排程日期为空提示 */
    private static final String ALERT_SCHEDULE_DATE_REQUIRED_KEY =
            "ui.data.alert.dailyPlanProgressReport.scheduleDateRequired";

    /** 排程结果为空提示 */
    private static final String ALERT_NO_SCHEDULE_RESULT_KEY =
            "ui.data.alert.dailyPlanProgressReport.noScheduleResult";

    /** 排程基础数据未就绪提示 */
    private static final String ALERT_DATA_NOT_READY_KEY =
            "ui.data.alert.dailyPlanProgressReport.dataNotReady";

    /** T+2日排产信息：新开 */
    private static final String INFO_NEW_OPEN_KEY =
            "ui.data.column.dailyPlanProgressReport.info.newOpen";

    /** T+2日排产信息：换活字块 */
    private static final String INFO_TYPE_BLOCK_KEY =
            "ui.data.column.dailyPlanProgressReport.info.typeBlock";

    /** T+2日排产信息：提前 */
    private static final String INFO_ADVANCE_KEY =
            "ui.data.column.dailyPlanProgressReport.info.advance";

    /** T+2日排产信息：延迟 */
    private static final String INFO_DELAY_KEY =
            "ui.data.column.dailyPlanProgressReport.info.delay";

    /** T+2日排产信息：超过月计划{0}台 */
    private static final String INFO_EXCEED_MONTH_PLAN_KEY =
            "ui.data.column.dailyPlanProgressReport.info.exceedMonthPlan";

    /**
     * 排程上下文时间轴初始化器
     */
    @Resource
    private LhScheduleContextInitializer lhScheduleContextInitializer;

    /**
     * 基础数据服务（复用余量相关加载链路）
     */
    @Resource
    private ILhBaseDataService lhBaseDataService;

    /**
     * 硫化日模具/机台数计算服务
     */
    @Resource
    private ILhDailyMouldCalcService lhDailyMouldCalcService;

    /**
     * 排程调整处理器（复用硫化余量计算口径）
     */
    @Resource
    private ScheduleAdjustHandler scheduleAdjustHandler;

    /**
     * 硫化排程结果 Mapper
     */
    @Resource
    private LhScheduleResultMapper lhScheduleResultMapper;

    /**
     * 模具交替计划 Mapper
     */
    @Resource
    private LhMouldChangePlanEntityMapper lhMouldChangePlanEntityMapper;

    /**
     * SKU与示方书关系 Mapper
     */
    @Resource
    private MdmSkuConstructionRefMapper mdmSkuConstructionRefMapper;

    /**
     * BOM 物料消耗明细 Mapper
     */
    @Resource
    private MdmMaterialConsumeDetailMapper mdmMaterialConsumeDetailMapper;

    @Override
    public byte[] exportDailyPlanProgressReport(LhDailyPlanProgressReportDTO queryDTO) {
        // 1. 参数校验与解析
        if (Objects.isNull(queryDTO) || StringUtils.isBlank(queryDTO.getScheduleDate())) {
            throw new ServiceException(I18nUtil.getMessage(ALERT_SCHEDULE_DATE_REQUIRED_KEY));
        }
        String factoryCode = StringUtils.defaultIfBlank(
                queryDTO.getFactoryCode(), FactoryConstant.DEFAULT_FACTORY_CODE);
        Date scheduleTargetDate = this.parseScheduleDate(queryDTO.getScheduleDate());

        // 2. 构建只读排程上下文（与正式排程同一套 T/T+1/T+2 口径）
        LhScheduleContext context = new LhScheduleContext();
        lhScheduleContextInitializer.initialize(context, factoryCode, scheduleTargetDate);
        LocalDate today = this.toLocalDate(context.getScheduleDate());
        LocalDate t2Date = this.toLocalDate(context.getWindowEndDate());

        // 3. 余量相关基础数据：先按「T+2 所在月的次月月末」临时末端加载，
        //    保证「次月定稿」判定涉及的那个月份，其定稿版本与定稿行都已进入加载范围
        LocalDate provisionalEndDate = this.endOfMonth(YearMonth.from(t2Date).plusMonths(1));
        lhBaseDataService.loadMonthSurplusData(context, this.toDate(provisionalEndDate));
        if (context.isInterrupted()) {
            String interruptReason = StringUtils.defaultIfBlank(context.getInterruptReason(),
                    I18nUtil.getMessage(ALERT_DATA_NOT_READY_KEY));
            throw new ServiceException(interruptReason);
        }

        // 4. 报表末端：次月有定稿版本则延到次月月末，否则只到 T+2 所在月月末
        YearMonth t2YearMonth = YearMonth.from(t2Date);
        YearMonth nextYearMonth = t2YearMonth.plusMonths(1);
        boolean nextMonthFinal = this.isMonthFinal(context, nextYearMonth);
        LocalDate reportEndDate = nextMonthFinal
                ? this.endOfMonth(nextYearMonth) : this.endOfMonth(t2YearMonth);
        List<LocalDate> dayColumnList = this.buildDateRange(today, reportEndDate);

        // 5. 基准：当前排程日期的硫化排程结果（含停产保机）
        List<LhScheduleResult> scheduleResultList = this.queryScheduleResultList(factoryCode, context);
        if (CollectionUtils.isEmpty(scheduleResultList)) {
            throw new ServiceException(I18nUtil.getMessage(ALERT_NO_SCHEDULE_RESULT_KEY));
        }
        Map<String, LhDailyPlanProgressAssembler.BaselineSku> baselineMap =
                LhDailyPlanProgressAssembler.buildBaseline(scheduleResultList);

        // 6. 月计划定稿行索引：物料|产品状态 → 年月 → 定稿行
        Map<String, Map<YearMonth, FactoryMonthPlanProductionFinalResult>> planIndex =
                this.buildMonthPlanIndex(context.getLoadedMonthPlanList());
        if (planIndex.isEmpty() && baselineMap.isEmpty()) {
            throw new ServiceException(I18nUtil.getMessage(ALERT_NO_SCHEDULE_RESULT_KEY));
        }

        // 7. T+2日换模计划（批次隔离：仅取本次排程日期生成的计划）
        Map<String, List<LhMouldChangePlan>> mouldChangePlanMap =
                this.queryT2MouldChangePlanMap(factoryCode, context, t2Date);

        // 8. 月计划每日机台数：T+2 当日与 T+2～报表末端最大值
        this.loadDailyCapacityLimitMap(context, t2Date, reportEndDate);
        lhDailyMouldCalcService.loadMouldAdjustParamMap(context, factoryCode);
        Map<String, Integer> t2MachineQtyMap = new LinkedHashMap<>(planIndex.size());
        Map<String, Integer> maxMachineQtyMap = new LinkedHashMap<>(planIndex.size());
        for (Map.Entry<String, Map<YearMonth, FactoryMonthPlanProductionFinalResult>> entry
                : planIndex.entrySet()) {
            this.resolveMonthPlanMachineQty(context, entry.getKey(), entry.getValue(),
                    t2Date, reportEndDate, t2MachineQtyMap, maxMachineQtyMap);
        }

        // 9. 胶种映射：物料+胎胚+示方类型 → AQT 原材料描述
        Map<String, String> rubberTypeMap = this.buildRubberTypeMap(factoryCode, planIndex);

        // 10. 组装报表行（基准 ∪ 增机 ∪ 延迟）
        LhDailyPlanProgressAssembler.T2InfoLabels infoLabels = this.buildT2InfoLabels();
        List<LhDailyPlanProgressReportRowVO> rowList = new ArrayList<>();
        for (String skuKey : this.collectCandidateSkuKeys(baselineMap, planIndex)) {
            LhDailyPlanProgressReportRowVO rowVO = this.buildRow(context, skuKey, baselineMap, planIndex,
                    mouldChangePlanMap, rubberTypeMap, t2MachineQtyMap, maxMachineQtyMap,
                    dayColumnList, t2YearMonth, infoLabels);
            if (Objects.nonNull(rowVO)) {
                rowList.add(rowVO);
            }
        }
        LhDailyPlanProgressAssembler.sortRows(rowList);
        log.info("日排产进度报表组装完成, 工厂: {}, 排程日期: {}, T日: {}, T+2: {}, 报表末端: {}, "
                        + "次月定稿: {}, 日期列数: {}, 基准SKU数: {}, 输出行数: {}",
                factoryCode, this.formatDate(scheduleTargetDate), today, t2Date, reportEndDate,
                nextMonthFinal, dayColumnList.size(), baselineMap.size(), rowList.size());

        // 11. 生成 Excel
        return LhDailyPlanProgressExcelUtil.build(rowList, dayColumnList, today);
    }

    /**
     * 组装单行报表数据。
     *
     * @return 行 VO；不满足「基准 ∪ 增机 ∪ 延迟」条件时返回 null
     */
    private LhDailyPlanProgressReportRowVO buildRow(LhScheduleContext context,
                                                    String skuKey,
                                                    Map<String, LhDailyPlanProgressAssembler.BaselineSku> baselineMap,
                                                    Map<String, Map<YearMonth, FactoryMonthPlanProductionFinalResult>> planIndex,
                                                    Map<String, List<LhMouldChangePlan>> mouldChangePlanMap,
                                                    Map<String, String> rubberTypeMap,
                                                    Map<String, Integer> t2MachineQtyMap,
                                                    Map<String, Integer> maxMachineQtyMap,
                                                    List<LocalDate> dayColumnList,
                                                    YearMonth t2YearMonth,
                                                    LhDailyPlanProgressAssembler.T2InfoLabels infoLabels) {
        LhDailyPlanProgressAssembler.BaselineSku baselineSku = baselineMap.get(skuKey);
        boolean inBaseline = Objects.nonNull(baselineSku);
        int baseMachineQty = inBaseline ? baselineSku.getMachineQty() : 0;
        int t2MonthMachineQty = this.nvl(t2MachineQtyMap.get(skuKey));
        int maxMonthMachineQty = this.nvl(maxMachineQtyMap.get(skuKey));
        // 余量/合格产量：按生产月（T 所在月）定稿行重算，缺该月行时回退次月行
        FactoryMonthPlanProductionFinalResult surplusPlan = this.resolveSurplusPlanRow(context, planIndex, skuKey);
        LhSkuSurplusVO surplusVO = this.resolveSurplusSafely(context, skuKey, surplusPlan);
        int surplusQty = Objects.isNull(surplusVO) ? 0 : this.nvl(surplusVO.getSurplusQty());
        boolean delay = LhDailyPlanProgressAssembler.isDelay(
                inBaseline, baseMachineQty, t2MonthMachineQty, surplusQty);

        // 行集合：基准全量；非基准仅在月计划区间有排产（增机）时纳入
        if (!inBaseline && !LhDailyPlanProgressAssembler.isIncrease(false, 0, maxMonthMachineQty)) {
            return null;
        }

        // 固定列取 T+2 所在月份定稿行，无该月行时取区间内第一个有行的月份
        FactoryMonthPlanProductionFinalResult fixedPlan = this.resolveFixedPlanRow(
                planIndex, skuKey, t2YearMonth, dayColumnList);
        LhDailyPlanProgressReportRowVO rowVO = new LhDailyPlanProgressReportRowVO();
        rowVO.setRubberType(rubberTypeMap.get(skuKey));
        rowVO.setLhMachineQty(baseMachineQty);
        rowVO.setSurplusQty(Objects.isNull(surplusVO) ? null : surplusVO.getSurplusQty());
        rowVO.setFinishQty(Objects.isNull(surplusVO) ? null : surplusVO.getFinishedQty());
        String materialCode;
        if (Objects.nonNull(fixedPlan)) {
            materialCode = StringUtils.trimToEmpty(fixedPlan.getMaterialCode());
            rowVO.setProductStatus(StringUtils.trimToEmpty(fixedPlan.getProductStatus()));
            rowVO.setStructureName(fixedPlan.getStructureName());
            rowVO.setMainPattern(fixedPlan.getMainPattern());
            rowVO.setMouldCavityQty(fixedPlan.getMouldCavityQty());
            rowVO.setTypeBlockQty(fixedPlan.getTypeBlockQty());
            rowVO.setMainMaterialDesc(fixedPlan.getMainMaterialDesc());
            rowVO.setMaterialCode(materialCode);
            rowVO.setMaterialDesc(fixedPlan.getMaterialDesc());
            rowVO.setProdReqPlan(fixedPlan.getProdReqPlan());
            rowVO.setOriginalTotalQty(fixedPlan.getOriginalTotalQty());
            rowVO.setAdjustQty1(fixedPlan.getAdjustQty1());
            rowVO.setAdjustQty2(fixedPlan.getAdjustQty2());
            rowVO.setAdjustQty3(fixedPlan.getAdjustQty3());
            rowVO.setStructureType(StringUtils.isBlank(fixedPlan.getStructureType())
                    ? this.resolveStructureTypeByProductStatus(fixedPlan.getProductStatus())
                    : fixedPlan.getStructureType());
            rowVO.setHeightQty(fixedPlan.getHeightQty());
            rowVO.setDayLhQty(fixedPlan.getDayVulcanizationQty());
        } else {
            // 基准行完全没有定稿行时，用基准信息补齐物料维度并记录告警
            materialCode = this.resolveMaterialCodeFromKey(skuKey);
            rowVO.setProductStatus(this.resolveProductStatusFromKey(skuKey));
            rowVO.setMaterialCode(materialCode);
            log.warn("日排产进度报表：SKU 无任何月计划定稿行, skuKey: {}", skuKey);
        }
        rowVO.setT2Info(LhDailyPlanProgressAssembler.buildT2Info(
                this.resolveT2MouldChangePlanList(mouldChangePlanMap, materialCode, rowVO.getProductStatus()),
                t2MonthMachineQty, baseMachineQty, delay, infoLabels));
        rowVO.setDayQtyList(this.buildDayQtyList(planIndex, skuKey, dayColumnList));
        return rowVO;
    }

    /**
     * 收集候选 SKU：基准 SKU ∪ 月计划定稿 SKU。
     *
     * @param baselineMap 基准 SKU
     * @param planIndex   月计划定稿索引
     * @return 候选 SKU key 集合（保持基准优先、月计划其次的顺序）
     */
    private Set<String> collectCandidateSkuKeys(
            Map<String, LhDailyPlanProgressAssembler.BaselineSku> baselineMap,
            Map<String, Map<YearMonth, FactoryMonthPlanProductionFinalResult>> planIndex) {
        Set<String> candidateKeySet = new LinkedHashSet<>(baselineMap.size() + planIndex.size());
        candidateKeySet.addAll(baselineMap.keySet());
        candidateKeySet.addAll(planIndex.keySet());
        return candidateKeySet;
    }

    /**
     * 计算单 SKU 在 [T+2, 报表末端] 的月计划机台数（T+2 当日 + 区间最大值）。
     *
     * @param context          排程上下文
     * @param skuKey           物料|产品状态
     * @param monthPlanMap     该 SKU 的年月定稿行
     * @param t2Date           T+2 日期
     * @param reportEndDate    报表末端日期
     * @param t2MachineQtyMap  T+2 机台数输出
     * @param maxMachineQtyMap 区间最大机台数输出
     */
    private void resolveMonthPlanMachineQty(LhScheduleContext context,
                                            String skuKey,
                                            Map<YearMonth, FactoryMonthPlanProductionFinalResult> monthPlanMap,
                                            LocalDate t2Date,
                                            LocalDate reportEndDate,
                                            Map<String, Integer> t2MachineQtyMap,
                                            Map<String, Integer> maxMachineQtyMap) {
        int t2MachineQty = 0;
        int maxMachineQty = 0;
        for (LocalDate productionDate = t2Date;
             !productionDate.isAfter(reportEndDate);
             productionDate = productionDate.plusDays(1)) {
            FactoryMonthPlanProductionFinalResult plan = monthPlanMap.get(YearMonth.from(productionDate));
            if (Objects.isNull(plan)) {
                continue;
            }
            // 当日无排产量时模具数为 0，直接跳过引擎调用
            if (MonthPlanDayQtyUtil.resolveDayQty(plan, productionDate.getDayOfMonth()) <= 0) {
                continue;
            }
            ILhDailyMouldCalcService.DailyMouldResult mouldResult =
                    lhDailyMouldCalcService.getDailyMouldByDay(context, plan, this.toDate(productionDate));
            int machineQty = Objects.isNull(mouldResult) ? 0 : mouldResult.getMachineQty();
            maxMachineQty = Math.max(maxMachineQty, machineQty);
            if (productionDate.isEqual(t2Date)) {
                t2MachineQty = machineQty;
            }
        }
        t2MachineQtyMap.put(skuKey, t2MachineQty);
        maxMachineQtyMap.put(skuKey, maxMachineQty);
    }

    /**
     * 构建日期列。
     *
     * @param startDate 起始日期（含）
     * @param endDate   结束日期（含）
     * @return 日期列表
     */
    private List<LocalDate> buildDateRange(LocalDate startDate, LocalDate endDate) {
        List<LocalDate> dateList = new ArrayList<>();
        for (LocalDate current = startDate; !current.isAfter(endDate); current = current.plusDays(1)) {
            dateList.add(current);
        }
        return dateList;
    }

    /**
     * 构建日期列排产量（定稿 DAY_N；无计划或为 0 时留空）。
     *
     * @param planIndex     月计划定稿索引
     * @param skuKey        物料|产品状态
     * @param dayColumnList 日期列
     * @return 日期列排产量
     */
    private List<Integer> buildDayQtyList(
            Map<String, Map<YearMonth, FactoryMonthPlanProductionFinalResult>> planIndex,
            String skuKey,
            List<LocalDate> dayColumnList) {
        List<Integer> dayQtyList = new ArrayList<>(dayColumnList.size());
        Map<YearMonth, FactoryMonthPlanProductionFinalResult> monthPlanMap = planIndex.get(skuKey);
        for (LocalDate productionDate : dayColumnList) {
            FactoryMonthPlanProductionFinalResult plan = Objects.isNull(monthPlanMap)
                    ? null : monthPlanMap.get(YearMonth.from(productionDate));
            int dayQty = Objects.isNull(plan)
                    ? 0 : MonthPlanDayQtyUtil.resolveDayQty(plan, productionDate.getDayOfMonth());
            dayQtyList.add(dayQty > 0 ? dayQty : null);
        }
        return dayQtyList;
    }

    /**
     * 构建月计划定稿行索引。
     *
     * @param loadedMonthPlanList 已加载的月计划定稿行（含跨月）
     * @return 物料|产品状态 → 年月 → 定稿行
     */
    private Map<String, Map<YearMonth, FactoryMonthPlanProductionFinalResult>> buildMonthPlanIndex(
            List<FactoryMonthPlanProductionFinalResult> loadedMonthPlanList) {
        Map<String, Map<YearMonth, FactoryMonthPlanProductionFinalResult>> planIndex = new LinkedHashMap<>();
        if (CollectionUtils.isEmpty(loadedMonthPlanList)) {
            return planIndex;
        }
        for (FactoryMonthPlanProductionFinalResult plan : loadedMonthPlanList) {
            if (Objects.isNull(plan) || StringUtils.isEmpty(plan.getMaterialCode())
                    || Objects.isNull(plan.getYear()) || Objects.isNull(plan.getMonth())) {
                continue;
            }
            String skuKey = LhDailyPlanProgressAssembler.buildSkuKey(
                    plan.getMaterialCode(), plan.getProductStatus());
            planIndex.computeIfAbsent(skuKey, key -> new LinkedHashMap<>())
                    .putIfAbsent(YearMonth.of(plan.getYear(), plan.getMonth()), plan);
        }
        return planIndex;
    }

    /**
     * 解析指定 SKU 在指定年月的定稿行。
     *
     * @return 定稿行；不存在返回 null
     */
    private FactoryMonthPlanProductionFinalResult resolvePlanRow(
            Map<String, Map<YearMonth, FactoryMonthPlanProductionFinalResult>> planIndex,
            String skuKey,
            YearMonth yearMonth) {
        Map<YearMonth, FactoryMonthPlanProductionFinalResult> monthPlanMap = planIndex.get(skuKey);
        return Objects.isNull(monthPlanMap) ? null : monthPlanMap.get(yearMonth);
    }

    /**
     * 解析固定列使用的定稿行：优先 T+2 所在月，其次日期列内第一个有行的月份，最后任一行。
     */
    private FactoryMonthPlanProductionFinalResult resolveFixedPlanRow(
            Map<String, Map<YearMonth, FactoryMonthPlanProductionFinalResult>> planIndex,
            String skuKey,
            YearMonth t2YearMonth,
            List<LocalDate> dayColumnList) {
        FactoryMonthPlanProductionFinalResult t2Plan = this.resolvePlanRow(planIndex, skuKey, t2YearMonth);
        if (Objects.nonNull(t2Plan)) {
            return t2Plan;
        }
        Map<YearMonth, FactoryMonthPlanProductionFinalResult> monthPlanMap = planIndex.get(skuKey);
        if (Objects.isNull(monthPlanMap) || monthPlanMap.isEmpty()) {
            return null;
        }
        for (LocalDate productionDate : dayColumnList) {
            FactoryMonthPlanProductionFinalResult plan = monthPlanMap.get(YearMonth.from(productionDate));
            if (Objects.nonNull(plan)) {
                return plan;
            }
        }
        return monthPlanMap.values().stream().findFirst().orElse(null);
    }

    /**
     * 解析余量/合格产量使用的定稿行：生产月（T 所在月）优先，缺该月行时回退次月行。
     */
    private FactoryMonthPlanProductionFinalResult resolveSurplusPlanRow(
            LhScheduleContext context,
            Map<String, Map<YearMonth, FactoryMonthPlanProductionFinalResult>> planIndex,
            String skuKey) {
        YearMonth productionYearMonth = MonthPlanSurplusCalculator.getProductionYearAndMonth(
                context.getScheduleDate());
        FactoryMonthPlanProductionFinalResult surplusPlan = this.resolvePlanRow(
                planIndex, skuKey, productionYearMonth);
        if (Objects.isNull(surplusPlan)) {
            surplusPlan = this.resolvePlanRow(planIndex, skuKey, productionYearMonth.plusMonths(1));
        }
        return surplusPlan;
    }

    /**
     * 安全计算硫化余量：算法异常只记录告警，不影响报表其余列。
     */
    private LhSkuSurplusVO resolveSurplusSafely(LhScheduleContext context,
                                                String skuKey,
                                                FactoryMonthPlanProductionFinalResult surplusPlan) {
        if (Objects.isNull(surplusPlan)) {
            log.warn("日排产进度报表：SKU 无生产月定稿行，余量与合格产量留空, skuKey: {}", skuKey);
            return null;
        }
        try {
            return scheduleAdjustHandler.resolveSurplusAndFinishedQty(context, surplusPlan);
        } catch (RuntimeException e) {
            log.warn("日排产进度报表：硫化余量计算异常，余量与合格产量留空, skuKey: {}, planYearMonth: {}-{}",
                    skuKey, surplusPlan.getYear(), surplusPlan.getMonth(), e);
            return null;
        }
    }

    /**
     * 查询当前排程日期的硫化排程结果。
     */
    private List<LhScheduleResult> queryScheduleResultList(String factoryCode, LhScheduleContext context) {
        Date scheduleTargetDate = context.getScheduleTargetDate();
        Date startDate = LhScheduleTimeUtil.clearTime(scheduleTargetDate);
        Date endDate = LhScheduleTimeUtil.getEndTime(scheduleTargetDate);
        return lhScheduleResultMapper.selectList(new LambdaQueryWrapper<LhScheduleResult>()
                .eq(LhScheduleResult::getFactoryCode, factoryCode)
                // 批次隔离：只取本次排程（SCHEDULE_DATE=排程目标日）生成的排程结果
                .ge(LhScheduleResult::getScheduleDate, startDate)
                .le(LhScheduleResult::getScheduleDate, endDate)
                .eq(LhScheduleResult::getIsDelete, NOT_DELETED));
    }

    /**
     * 查询 T+2 日换模计划并按「后物料编码」分组。
     *
     * <p>批次隔离只取本次排程日期生成的计划（按 SCHEDULE_DATE 过滤），
     * 再按 PLAN_DATE 落在 T+2 当天筛选；后物料编码即 T+2 上机（新开/换活字块）的 SKU 物料。</p>
     *
     * <p>换模计划的产品状态字段为示方类型（S/T/X），与排程结果同域，
     * 归属 SKU 时由 {@link #resolveT2MouldChangePlanList} 再按产品状态过滤。</p>
     *
     * <p>注：若后续库中出现 PLAN_DATE 为空的换模计划（例如历史导入数据），
     * 需在此补充独立的 OR 分支，且仍必须同时限定 SCHEDULE_DATE 为本批次，避免破坏批次隔离。</p>
     */
    private Map<String, List<LhMouldChangePlan>> queryT2MouldChangePlanMap(String factoryCode,
                                                                          LhScheduleContext context,
                                                                          LocalDate t2Date) {
        Date scheduleDate = LhScheduleTimeUtil.clearTime(context.getScheduleTargetDate());
        Date t2Start = this.toDate(t2Date);
        Date t2End = LhScheduleTimeUtil.getEndTime(t2Start);
        List<LhMouldChangePlan> mouldChangePlanList = lhMouldChangePlanEntityMapper.selectList(
                new LambdaQueryWrapper<LhMouldChangePlan>()
                        .eq(LhMouldChangePlan::getFactoryCode, factoryCode)
                        .eq(LhMouldChangePlan::getScheduleDate, scheduleDate)
                        .eq(LhMouldChangePlan::getIsDelete, NOT_DELETED)
                        .ge(LhMouldChangePlan::getPlanDate, t2Start)
                        .le(LhMouldChangePlan::getPlanDate, t2End));
        if (CollectionUtils.isEmpty(mouldChangePlanList)) {
            return new LinkedHashMap<>(0);
        }
        Map<String, List<LhMouldChangePlan>> mouldChangePlanMap = new LinkedHashMap<>();
        for (LhMouldChangePlan plan : mouldChangePlanList) {
            if (Objects.isNull(plan) || StringUtils.isEmpty(plan.getAfterMaterialCode())) {
                continue;
            }
            mouldChangePlanMap.computeIfAbsent(
                    StringUtils.trimToEmpty(plan.getAfterMaterialCode()),
                    key -> new ArrayList<>()).add(plan);
        }
        return mouldChangePlanMap;
    }

    /**
     * 取该 SKU 在 T+2 的换模计划：按物料编码命中后，再按产品状态（示方类型）过滤。
     *
     * @param materialChangePlanMap 物料编码 → T+2 换模计划
     * @param materialCode          物料编码
     * @param productStatus         产品状态（S/T/X）
     * @return 命中该 SKU 的换模计划，无命中返回空列表
     */
    private List<LhMouldChangePlan> resolveT2MouldChangePlanList(
            Map<String, List<LhMouldChangePlan>> materialChangePlanMap,
            String materialCode,
            String productStatus) {
        List<LhMouldChangePlan> planList = materialChangePlanMap.get(StringUtils.trimToEmpty(materialCode));
        if (CollectionUtils.isEmpty(planList)) {
            return Collections.emptyList();
        }
        String trimmedProductStatus = StringUtils.trimToEmpty(productStatus);
        return planList.stream()
                .filter(plan -> StringUtils.isBlank(plan.getProductStatus())
                        || StringUtils.equals(StringUtils.trim(plan.getProductStatus()), trimmedProductStatus))
                .collect(Collectors.toList());
    }

    /**
     * 按报表涉及月份逐月构建日产能限制 Map（机台数计算前置）。
     */
    private void loadDailyCapacityLimitMap(LhScheduleContext context,
                                           LocalDate startDate,
                                           LocalDate endDate) {
        Set<YearMonth> yearMonthSet = new LinkedHashSet<>();
        for (LocalDate current = startDate; !current.isAfter(endDate); current = current.plusDays(1)) {
            yearMonthSet.add(YearMonth.from(current));
        }
        for (YearMonth yearMonth : yearMonthSet) {
            lhDailyMouldCalcService.loadDailyCapacityLimitMap(
                    context, yearMonth.getYear(), yearMonth.getMonthValue());
        }
    }

    /**
     * 构建「胶种」映射：物料+胎胚+示方类型 → BOM 消耗明细中的 AQT 原材料描述。
     *
     * <p>两段关联（与成型导出同口径）：SKU 与示方书关系表取制造示方书号，再按制造示方书号
     * 关联 BOM 物料消耗明细，取 AQT 开头的原材料描述并去掉 AQ 前缀。</p>
     *
     * @param factoryCode 工厂编码
     * @param planIndex   月计划定稿索引
     * @return 物料|产品状态 → 胶种（逗号分隔）
     */
    private Map<String, String> buildRubberTypeMap(
            String factoryCode,
            Map<String, Map<YearMonth, FactoryMonthPlanProductionFinalResult>> planIndex) {
        Map<String, String> rubberTypeMap = new LinkedHashMap<>();
        if (planIndex.isEmpty()) {
            return rubberTypeMap;
        }
        // 1. 收集物料编码与胎胚编码
        List<String> materialCodeList = planIndex.values().stream()
                .flatMap(monthPlanMap -> monthPlanMap.values().stream())
                .map(FactoryMonthPlanProductionFinalResult::getMaterialCode)
                .filter(StringUtils::isNotEmpty)
                .distinct()
                .collect(Collectors.toList());
        Set<String> embryoCodeSet = planIndex.values().stream()
                .flatMap(monthPlanMap -> monthPlanMap.values().stream())
                .map(FactoryMonthPlanProductionFinalResult::getEmbryoCode)
                .filter(StringUtils::isNotEmpty)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (materialCodeList.isEmpty() || embryoCodeSet.isEmpty()) {
            return rubberTypeMap;
        }
        // 2. 第一段：SKU 与示方书关系，按物料|胎胚|示方类型建立映射（同键保留最新）
        Map<String, Map<String, MdmSkuConstructionRef>> refIndex = this.querySkuConstructionRefIndex(
                factoryCode, materialCodeList, embryoCodeSet);
        if (refIndex.isEmpty()) {
            return rubberTypeMap;
        }
        // 3. 收集制造示方书号，第二段查询 BOM 消耗明细
        Set<String> embryoVersionSet = refIndex.values().stream()
                .flatMap(typeRefMap -> typeRefMap.values().stream())
                .map(MdmSkuConstructionRef::getEmbryoNo)
                .filter(StringUtils::isNotEmpty)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Map<String, Set<String>> embryoVersionGlueMap = this.queryEmbryoVersionGlueMap(
                factoryCode, embryoVersionSet);
        if (embryoVersionGlueMap.isEmpty()) {
            return rubberTypeMap;
        }
        // 4. 组装：示方类型精确匹配，未命中按 S→T→X 降级链匹配
        for (Map.Entry<String, Map<YearMonth, FactoryMonthPlanProductionFinalResult>> entry
                : planIndex.entrySet()) {
            FactoryMonthPlanProductionFinalResult plan = entry.getValue().values().stream()
                    .findFirst().orElse(null);
            if (Objects.isNull(plan)) {
                continue;
            }
            Map<String, MdmSkuConstructionRef> typeRefMap = refIndex.get(this.buildRefIndexKey(
                    plan.getMaterialCode(), plan.getEmbryoCode()));
            if (Objects.isNull(typeRefMap) || typeRefMap.isEmpty()) {
                continue;
            }
            MdmSkuConstructionRef matchedRef = this.resolveConstructionRef(typeRefMap, plan.getProductStatus());
            if (Objects.isNull(matchedRef) || StringUtils.isEmpty(matchedRef.getEmbryoNo())) {
                continue;
            }
            Set<String> glueNameSet = embryoVersionGlueMap.get(StringUtils.trim(matchedRef.getEmbryoNo()));
            if (CollectionUtils.isEmpty(glueNameSet)) {
                continue;
            }
            rubberTypeMap.put(entry.getKey(), String.join(",", glueNameSet));
        }
        return rubberTypeMap;
    }

    /**
     * 查询 SKU 与示方书关系，并按 物料|胎胚 → 示方类型 → 关系行 建索引（同键保留最新）。
     */
    private Map<String, Map<String, MdmSkuConstructionRef>> querySkuConstructionRefIndex(
            String factoryCode, List<String> materialCodeList, Set<String> embryoCodeSet) {
        Map<String, Map<String, MdmSkuConstructionRef>> refIndex = new LinkedHashMap<>();
        for (List<String> materialChunk : this.splitList(materialCodeList)) {
            List<MdmSkuConstructionRef> refList = mdmSkuConstructionRefMapper.selectList(
                    new LambdaQueryWrapper<MdmSkuConstructionRef>()
                            .eq(MdmSkuConstructionRef::getFactoryCode, factoryCode)
                            .eq(MdmSkuConstructionRef::getIsDelete, NOT_DELETED)
                            .in(MdmSkuConstructionRef::getMaterialCode, materialChunk)
                            .in(MdmSkuConstructionRef::getEmbryoCode, new ArrayList<>(embryoCodeSet))
                            .orderByDesc(MdmSkuConstructionRef::getUpdateTime));
            for (MdmSkuConstructionRef ref : refList) {
                if (Objects.isNull(ref) || StringUtils.isEmpty(ref.getMaterialCode())
                        || StringUtils.isEmpty(ref.getEmbryoCode())) {
                    continue;
                }
                refIndex.computeIfAbsent(
                                this.buildRefIndexKey(ref.getMaterialCode(), ref.getEmbryoCode()),
                                key -> new LinkedHashMap<>())
                        .putIfAbsent(StringUtils.trimToEmpty(ref.getEmbryoType()), ref);
            }
        }
        return refIndex;
    }

    /**
     * 查询 BOM 物料消耗明细中的胶种：制造示方书号 → AQT 原材料描述集合（去 AQ 前缀）。
     */
    private Map<String, Set<String>> queryEmbryoVersionGlueMap(String factoryCode,
                                                               Set<String> embryoVersionSet) {
        Map<String, Set<String>> embryoVersionGlueMap = new LinkedHashMap<>();
        if (CollectionUtils.isEmpty(embryoVersionSet)) {
            return embryoVersionGlueMap;
        }
        for (List<String> versionChunk : this.splitList(new ArrayList<>(embryoVersionSet))) {
            List<MdmMaterialConsumeDetail> consumeDetailList = mdmMaterialConsumeDetailMapper.selectList(
                    new LambdaQueryWrapper<MdmMaterialConsumeDetail>()
                            .eq(MdmMaterialConsumeDetail::getFactoryCode, factoryCode)
                            .eq(MdmMaterialConsumeDetail::getIsDelete, NOT_DELETED)
                            .in(MdmMaterialConsumeDetail::getEmbryoVersion, versionChunk)
                            .likeRight(MdmMaterialConsumeDetail::getChildMaterialName,
                                    GLUE_MATERIAL_NAME_PREFIX));
            for (MdmMaterialConsumeDetail detail : consumeDetailList) {
                if (Objects.isNull(detail) || StringUtils.isEmpty(detail.getChildMaterialName())) {
                    continue;
                }
                String embryoVersion = StringUtils.trimToEmpty(detail.getEmbryoVersion());
                String glueName = StringUtils.trim(detail.getChildMaterialName());
                // 去掉 AQ 前缀后展示（AQTxxx → Txxx）
                String displayName = glueName.startsWith(GLUE_CODE_PREFIX)
                        ? glueName.substring(GLUE_CODE_PREFIX.length()) : glueName;
                embryoVersionGlueMap.computeIfAbsent(embryoVersion, key -> new LinkedHashSet<>())
                        .add(displayName);
            }
        }
        return embryoVersionGlueMap;
    }

    /**
     * 按产品状态精确匹配示方书关系，未命中时按 S→T→X 降级链匹配。
     */
    private MdmSkuConstructionRef resolveConstructionRef(Map<String, MdmSkuConstructionRef> typeRefMap,
                                                         String productStatus) {
        String exactRefKey = StringUtils.trimToEmpty(productStatus);
        MdmSkuConstructionRef exactRef = typeRefMap.get(exactRefKey);
        if (Objects.nonNull(exactRef) && StringUtils.isNotEmpty(exactRef.getEmbryoNo())) {
            return exactRef;
        }
        for (String fallbackStatus : this.resolveFallbackTrialStatusList(productStatus)) {
            MdmSkuConstructionRef fallbackRef = typeRefMap.get(fallbackStatus);
            if (Objects.nonNull(fallbackRef) && StringUtils.isNotEmpty(fallbackRef.getEmbryoNo())) {
                return fallbackRef;
            }
        }
        return null;
    }

    /**
     * 构建产品状态降级链：正规(S) → 量试(T) → 试制(X)；量试(T) → 试制(X)；试制(X) 无降级。
     */
    private List<String> resolveFallbackTrialStatusList(String productStatus) {
        TrialStatusEnum trialStatus = TrialStatusEnum.getByCode(productStatus);
        if (Objects.isNull(trialStatus)) {
            return Collections.emptyList();
        }
        switch (trialStatus) {
            case FORMAL:
                return Arrays.asList(TrialStatusEnum.MASS_TRIAL.getCode(), TrialStatusEnum.TRIAL.getCode());
            case MASS_TRIAL:
                return Collections.singletonList(TrialStatusEnum.TRIAL.getCode());
            case TRIAL:
            default:
                return Collections.emptyList();
        }
    }

    /**
     * 构建示方书索引键：物料编码|胎胚编码。
     */
    private String buildRefIndexKey(String materialCode, String embryoCode) {
        return StringUtils.trimToEmpty(materialCode) + "|" + StringUtils.trimToEmpty(embryoCode);
    }

    /**
     * 构建「T+2日排产信息」文案词。
     */
    private LhDailyPlanProgressAssembler.T2InfoLabels buildT2InfoLabels() {
        LhDailyPlanProgressAssembler.T2InfoLabels infoLabels =
                new LhDailyPlanProgressAssembler.T2InfoLabels();
        infoLabels.setNewOpen(I18nUtil.getMessage(INFO_NEW_OPEN_KEY));
        infoLabels.setTypeBlock(I18nUtil.getMessage(INFO_TYPE_BLOCK_KEY));
        infoLabels.setAdvance(I18nUtil.getMessage(INFO_ADVANCE_KEY));
        infoLabels.setDelay(I18nUtil.getMessage(INFO_DELAY_KEY));
        infoLabels.setExceedMonthPlanPattern(I18nUtil.getMessage(INFO_EXCEED_MONTH_PLAN_KEY));
        return infoLabels;
    }

    /**
     * 判断指定年月是否有定稿排产版本。
     */
    private boolean isMonthFinal(LhScheduleContext context, YearMonth yearMonth) {
        Map<String, String> productionVersionMap = context.getProductionVersionByYearMonthMap();
        if (CollectionUtils.isEmpty(productionVersionMap)) {
            return false;
        }
        return productionVersionMap.containsKey(
                MonthPlanDateResolver.buildYearMonthKey(yearMonth.getYear(), yearMonth.getMonthValue()));
    }

    /**
     * 产品状态推断结构类型（定稿行缺失结构类型时的兜底）：S-正规按常规、T/X 按周期。
     */
    private String resolveStructureTypeByProductStatus(String productStatus) {
        return StringUtils.equals(TrialStatusEnum.FORMAL.getCode(), productStatus)
                ? STRUCTURE_TYPE_NORMAL : STRUCTURE_TYPE_CYCLE;
    }

    /**
     * 从 SKU key 中解析物料编码。
     */
    private String resolveMaterialCodeFromKey(String skuKey) {
        return StringUtils.substringBefore(skuKey, "|");
    }

    /**
     * 从 SKU key 中解析产品状态。
     */
    private String resolveProductStatusFromKey(String skuKey) {
        return StringUtils.substringAfter(skuKey, "|");
    }

    /**
     * 解析排程日期。
     */
    private Date parseScheduleDate(String scheduleDate) {
        try {
            return LhScheduleTimeUtil.clearTime(
                    cn.hutool.core.date.DateUtil.parse(scheduleDate, DATE_PATTERN));
        } catch (RuntimeException e) {
            log.warn("日排产进度报表：排程日期格式非法, scheduleDate: {}", scheduleDate, e);
            throw new ServiceException(I18nUtil.getMessage(ALERT_SCHEDULE_DATE_REQUIRED_KEY));
        }
    }

    /**
     * 列表按批次大小切分。
     */
    private List<List<String>> splitList(List<String> sourceList) {
        List<List<String>> chunkList = new ArrayList<>();
        if (CollectionUtils.isEmpty(sourceList)) {
            return chunkList;
        }
        for (int index = 0; index < sourceList.size(); index += IN_QUERY_BATCH_SIZE) {
            chunkList.add(sourceList.subList(index,
                    Math.min(index + IN_QUERY_BATCH_SIZE, sourceList.size())));
        }
        return chunkList;
    }

    /**
     * 取指定年月的月末日期。
     *
     * @param yearMonth 年月
     * @return 月末日期
     */
    private LocalDate endOfMonth(YearMonth yearMonth) {
        return yearMonth.atEndOfMonth();
    }

    /**
     * Date → LocalDate。
     */
    private LocalDate toLocalDate(Date date) {
        return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
    }

    /**
     * LocalDate → Date（自然日零点）。
     */
    private Date toDate(LocalDate date) {
        return Date.from(date.atStartOfDay(ZoneId.systemDefault()).toInstant());
    }

    /**
     * 格式化日期用于日志。
     */
    private String formatDate(Date date) {
        return LhScheduleTimeUtil.formatDate(date);
    }

    /**
     * 空值转 0。
     */
    private int nvl(Integer value) {
        return Objects.isNull(value) ? 0 : value;
    }
}
