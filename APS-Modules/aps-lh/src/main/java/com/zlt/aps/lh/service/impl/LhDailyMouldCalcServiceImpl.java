package com.zlt.aps.lh.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.beans.BeanUtils;
import com.zlt.aps.enums.YesOrNoEnum;
import com.zlt.aps.lh.api.constant.LhScheduleConstant;
import com.zlt.aps.lh.component.MonthPlanDateResolver;
import com.zlt.aps.lh.context.LhScheduleContext;
import com.zlt.aps.lh.service.ILhDailyMouldCalcService;
import com.zlt.aps.lh.util.MonthPlanDayQtyUtil;
import com.zlt.aps.maindata.enums.MonthPlanEnums;
import com.zlt.aps.maindata.mapper.FactoryParamMapper;
import com.zlt.aps.maindata.utils.FactoryParamUtils;
import com.zlt.aps.mp.api.domain.capacity.MpDailyCapacityLimitVo;
import com.zlt.aps.mp.api.domain.entity.FactoryMonthPlanProductionFinalResult;
import com.zlt.aps.mp.api.domain.entity.FactoryParam;
import com.zlt.aps.mp.api.domain.vo.FactoryMonthPlanFinalAdjustVo;
import com.zlt.aps.mp.engine.adjust.MpWeekRollAdjustEngine;
import com.zlt.aps.mp.engine.capacity.MpAdjustDailyCapacityLimit;
import com.zlt.aps.mdm.api.domain.entity.MdmWorkCalendar;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 硫化日模具/机台数计算服务实现
 * <p>封装 {@link MpWeekRollAdjustEngine#getMouldByDay} 的调用链：
 * 月计划实体 → 调整Vo 转换、日产能限制Map构建、排产参数加载、模具数计算。</p>
 * <p>支持排程窗口及跨窗口判断范围：跨月月计划直接复用上下文
 * {@code monthPlanByMaterialMonthMap}（LhBaseDataServiceImpl.loadMonthPlan 已按
 * 排程窗口所需月份批量加载并构建索引），不重复查库。</p>
 *
 * @author APS
 */
@Slf4j
@Service
public class LhDailyMouldCalcServiceImpl implements ILhDailyMouldCalcService {

    /**
     * 跨月续作使用三日滚动视图的中间日执行既有增减模计算。
     */
    private static final int CROSS_MONTH_CALCULATION_DAY = 2;

    /**
     * 月计划日数量动态字段模板。
     */
    private static final String DAY_PLAN_FIELD_TEMPLATE = "day%d";

    /** 月计划调整引擎（无状态，直接实例化） */
    private final MpWeekRollAdjustEngine weekRollAdjustEngine = new MpWeekRollAdjustEngine();

    /** 日产能限制实例（无状态，直接实例化） */
    private final MpAdjustDailyCapacityLimit adjustDailyCapacityLimitObj = new MpAdjustDailyCapacityLimit();

    @Resource
    private FactoryParamMapper factoryParamMapper;

    /**
     * 模具计算所需排产参数编码列表
     * <p>SYS0203003 换模时SKU首日排产量、SYS0203004 换活字块时SKU收尾量与日硫化量差值、
     * SYS0203005 换活字块时后SKU首日排产量、SYS0203006 换活字块时后SKU首日排产量(大差值)</p>
     */
    private static final String[] MOULD_PARAM_CODES = {
            MonthPlanEnums.CHANGE_MOULD_FIRST_QTY.getCode(),
            MonthPlanEnums.CHANGE_TYPE_BLOCK_QTY_DIFF.getCode(),
            MonthPlanEnums.CHANGE_TYPE_BLOCK_QTY.getCode(),
            MonthPlanEnums.CHANGE_TYPE_BLOCK_MAX_QTY.getCode()
    };

    @Override
    public void loadDailyCapacityLimitMap(LhScheduleContext context, int year, int month) {
        // 1. 先按完整自然日构建工作日历索引，开产首日判断才能正确跨月读取前一天状态。
        YearMonth targetYm = YearMonth.of(year, month);
        Map<LocalDate, MdmWorkCalendar> calendarByDate = context.getWorkCalendarList().stream()
                .filter(cal -> cal != null && cal.getProductionDate() != null)
                .collect(Collectors.toMap(
                        cal -> this.toLocalDate(cal.getProductionDate()),
                        cal -> cal,
                        (existing, replacement) -> existing,
                        LinkedHashMap::new));
        List<MdmWorkCalendar> targetCalendarList = calendarByDate.values().stream()
                .filter(cal -> targetYm.equals(YearMonth.from(this.toLocalDate(cal.getProductionDate()))))
                .sorted(Comparator.comparing(MdmWorkCalendar::getDay,
                        Comparator.nullsLast(Comparator.naturalOrder())))
                .collect(Collectors.toList());

        // 2. 逐日构建日产能限制Vo，key=日期(LocalDate)，跨月不冲突。
        Map<LocalDate, MpDailyCapacityLimitVo> dailyCapacityMap = new LinkedHashMap<>(targetCalendarList.size());
        for (MdmWorkCalendar workCalendar : targetCalendarList) {
            Integer day = workCalendar.getDay();
            if (day == null) {
                continue;
            }
            MpDailyCapacityLimitVo limitVo = new MpDailyCapacityLimitVo();
            limitVo.setDayProductionRate(workCalendar.getRate());
            LocalDate productionDate = this.toLocalDate(workCalendar.getProductionDate());
            MdmWorkCalendar previousCalendar = calendarByDate.get(productionDate.minusDays(1));
            // 开产首日：当前日在产且前一自然日明确停产；跨月时仍按完整自然日索引判断。
            limitVo.setOpenProductionFirstDay(YesOrNoEnum.YES.getCode().equals(workCalendar.getDayFlag())
                    && Objects.nonNull(previousCalendar)
                    && !YesOrNoEnum.YES.getCode().equals(previousCalendar.getDayFlag()));
            dailyCapacityMap.put(productionDate, limitVo);
        }

        // 3. 合并放入上下文（多次调用不同年月时累积，支持跨月窗口）
        context.getDailyCapacityLimitVoMap().putAll(dailyCapacityMap);
        log.info("日产能限制Map加载完成(合并), 工厂: {}, 年月: {}-{}, 本次天数: {}, 累计天数: {}",
                context.getFactoryCode(), year, month, dailyCapacityMap.size(),
                context.getDailyCapacityLimitVoMap().size());
    }

    @Override
    public Map<String, Object> loadMouldAdjustParamMap(LhScheduleContext context, String factoryCode) {
        // 从 T_MP_FACTORY_PARAM 加载模具计算所需排产参数（逻辑删除由框架自动过滤）
        LambdaQueryWrapper<FactoryParam> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(FactoryParam::getFactoryCode, factoryCode);
        queryWrapper.in(FactoryParam::getParamCode, Arrays.asList(MOULD_PARAM_CODES));
        List<FactoryParam> paramList = factoryParamMapper.selectList(queryWrapper);

        Map<String, Object> paramMap = paramList.stream()
                .filter(param -> param.getParamCode() != null && param.getParamValue() != null)
                .collect(Collectors.toMap(
                        FactoryParam::getParamCode,
                        FactoryParamUtils::getParamValue,
                        (existing, replacement) -> existing
                ));

        // 放入上下文
        context.setMouldAdjustParamMap(paramMap);
        log.info("模具计算排产参数加载完成, 工厂: {}, 参数数量: {}", factoryCode, paramMap.size());
        return paramMap;
    }

    @Override
    public DailyMouldResult getDailyMouldByDay(LhScheduleContext context,
                                               FactoryMonthPlanProductionFinalResult monthPlanEntity,
                                               Date date) {
        return this.getDailyMouldByLocalDate(context, monthPlanEntity, this.toLocalDate(date));
    }

    /**
     * 获取单个SKU在指定自然日的模具数和机台数，月初续作按真实前日计划衔接。
     *
     * @param context 已加载月计划、日产能限制和模具计算参数的排程上下文
     * @param monthPlanEntity 目标自然日所属月份的月计划定稿实体
     * @param localDate 目标自然日，可为跨月日期
     * @return 保留实际自然日和月内日序号的模具数、机台数计算结果
     */
    private DailyMouldResult getDailyMouldByLocalDate(LhScheduleContext context,
                                                      FactoryMonthPlanProductionFinalResult monthPlanEntity,
                                                      LocalDate localDate) {
        // 月内天序号(1~31)，与月计划 day1~day31 字段对应（跨月时 date 所在月的计划行）
        int iDay = localDate.getDayOfMonth();

        // 1. 月计划实体 → 调整Vo（BeanUtils.copyProperties，FactoryMonthPlanFinalAdjustVo 继承自实体类）
        FactoryMonthPlanFinalAdjustVo mpFinalVo = new FactoryMonthPlanFinalAdjustVo();
        BeanUtils.copyProperties(monthPlanEntity, mpFinalVo);

        // 2. 获取该日的日产能限制Vo（key=LocalDate，支持跨月）
        MpDailyCapacityLimitVo dailyCapacityLimitVo = context.getDailyCapacityLimitVoMap().get(localDate);
        if (Objects.isNull(dailyCapacityLimitVo)) {
            // 工作日历缺失不是业务目标0，返回null让统一Map保留“维度缺失”状态，释放型决策据此保持当前机台。
            log.warn("日产能限制Vo不存在，目标机台数结果不落Map, 工厂: {}, 物料: {}, 产品状态: {}, "
                            + "date: {}, 已加载天数: {}",
                    context.getFactoryCode(), monthPlanEntity.getMaterialCode(), monthPlanEntity.getProductStatus(),
                    localDate, context.getDailyCapacityLimitVoMap().size());
            return null;
        }

        // 3. 只调整独立计算视图，月初续作不能把当日计划当作前日计划而丢失增机。
        int calculationDay = this.prepareCrossMonthContinuationView(context, monthPlanEntity, localDate, mpFinalVo);
        int mouldQty = weekRollAdjustEngine.getMouldByDay(
                adjustDailyCapacityLimitObj,
                context.getMouldAdjustParamMap(),
                calculationDay,
                mpFinalVo,
                dailyCapacityLimitVo
        );

        if (calculationDay != iDay) {
            log.info("月初续作目标机台数按真实前日计划衔接, 工厂: {}, 批次: {}, 物料: {}, 状态: {}, "
                            + "日期: {}, 前日计划: {}, 当日计划: {}, 次日计划: {}, 目标模具数: {}",
                    context.getFactoryCode(), context.getBatchNo(), monthPlanEntity.getMaterialCode(),
                    monthPlanEntity.getProductStatus(), localDate,
                    mpFinalVo.getFieldValueByFieldName(String.format(DAY_PLAN_FIELD_TEMPLATE, calculationDay - 1)),
                    mpFinalVo.getFieldValueByFieldName(String.format(DAY_PLAN_FIELD_TEMPLATE, calculationDay)),
                    mpFinalVo.getFieldValueByFieldName(String.format(DAY_PLAN_FIELD_TEMPLATE, calculationDay + 1)),
                    mouldQty);
        }

        DailyMouldResult result = new DailyMouldResult(monthPlanEntity.getMaterialCode(),
                monthPlanEntity.getProductStatus(),
                monthPlanEntity.getStructureName(), iDay, mouldQty);
        result.setDate(localDate);
        return result;
    }

    /**
     * 月初存在同状态前月末正计划时，构建真实前日、当日和次日的独立滚动计算视图。
     *
     * @param context 已按物料、状态和年月加载月计划的上下文
     * @param monthPlanEntity 当日所属月份的原始月计划
     * @param productionDate 实际生产自然日
     * @param calculationView 独立调整视图，仅改写其中三个日数量字段
     * @return 原月内日序号，或跨月滚动视图的中间日序号
     */
    private int prepareCrossMonthContinuationView(LhScheduleContext context,
            FactoryMonthPlanProductionFinalResult monthPlanEntity, LocalDate productionDate,
            FactoryMonthPlanFinalAdjustVo calculationView) {
        if (productionDate.getDayOfMonth() != 1) {
            return productionDate.getDayOfMonth();
        }
        LocalDate previousDate = productionDate.minusDays(1);
        String materialStatusKey = MonthPlanDateResolver.buildMaterialStatusKey(
                monthPlanEntity.getMaterialCode(), monthPlanEntity.getProductStatus());
        FactoryMonthPlanProductionFinalResult previousPlan = context.getMonthPlanByMaterialMonthMap().get(
                MonthPlanDateResolver.buildMaterialMonthKey(materialStatusKey,
                        previousDate.getYear(), previousDate.getMonthValue()));
        int previousQty = MonthPlanDayQtyUtil.resolveDayQty(previousPlan, previousDate.getDayOfMonth());
        if (previousQty <= 0) {
            return productionDate.getDayOfMonth();
        }
        // 先读取原值再改写视图，避免day2覆盖后丢失真实次日量；原实体和共享日计划账本均不修改。
        Object currentQty = monthPlanEntity.getFieldValueByFieldName(
                String.format(DAY_PLAN_FIELD_TEMPLATE, productionDate.getDayOfMonth()));
        Object nextQty = monthPlanEntity.getFieldValueByFieldName(
                String.format(DAY_PLAN_FIELD_TEMPLATE, productionDate.plusDays(1).getDayOfMonth()));
        calculationView.setFieldValueByFieldName(
                String.format(DAY_PLAN_FIELD_TEMPLATE, CROSS_MONTH_CALCULATION_DAY - 1), previousQty);
        calculationView.setFieldValueByFieldName(
                String.format(DAY_PLAN_FIELD_TEMPLATE, CROSS_MONTH_CALCULATION_DAY), currentQty);
        calculationView.setFieldValueByFieldName(
                String.format(DAY_PLAN_FIELD_TEMPLATE, CROSS_MONTH_CALCULATION_DAY + 1), nextQty);
        return CROSS_MONTH_CALCULATION_DAY;
    }

    @Override
    public List<DailyMouldResult> getDailyMouldByDate(LhScheduleContext context, Date scheduleDate) {
        // 1. 解析排程日期 → 年、月
        LocalDate localDate = this.toLocalDate(scheduleDate);
        int year = localDate.getYear();
        int month = localDate.getMonthValue();

        // 2. 确保日产能限制Map和排产参数已加载
        if (context.getDailyCapacityLimitVoMap().isEmpty()) {
            this.loadDailyCapacityLimitMap(context, year, month);
        }
        if (context.getMouldAdjustParamMap().isEmpty()) {
            this.loadMouldAdjustParamMap(context, context.getFactoryCode());
        }

        // 3. 从上下文取排程当月月计划列表，逐SKU计算模具数
        List<FactoryMonthPlanProductionFinalResult> monthPlanList = context.getMonthPlanList();
        if (monthPlanList == null || monthPlanList.isEmpty()) {
            log.warn("月计划列表为空, 跳过日模具计算, 排程日期: {}", scheduleDate);
            return new ArrayList<>(0);
        }

        // 4. 逐SKU计算模具数
        List<DailyMouldResult> resultList = new ArrayList<>(monthPlanList.size());
        for (FactoryMonthPlanProductionFinalResult entity : monthPlanList) {
            DailyMouldResult result = this.getDailyMouldByDay(context, entity, scheduleDate);
            if (Objects.nonNull(result)) {
                resultList.add(result);
            }
        }

        log.info("日模具计算完成, 排程日期: {}, SKU数: {}, 总模具数: {}, 总机台数: {}",
                scheduleDate, resultList.size(),
                resultList.stream().mapToInt(DailyMouldResult::getMouldQty).sum(),
                resultList.stream().mapToInt(DailyMouldResult::getMachineQty).sum());

        return resultList;
    }

    @Override
    public void loadDailyMouldSummary(LhScheduleContext context) {
        Date scheduleDate = context.getScheduleDate();
        List<FactoryMonthPlanProductionFinalResult> loadedMonthPlanList = context.getLoadedMonthPlanList();
        if (Objects.isNull(scheduleDate) || CollectionUtils.isEmpty(loadedMonthPlanList)) {
            log.warn("排程日期或已加载月计划列表为空, 跳过目标机台数预计算, 工厂: {}", context.getFactoryCode());
            return;
        }

        // 1. 统一计算范围覆盖停产保机前看、实际排程窗口、T+3增机以及特殊材料窗口后两日判断。
        //    该范围只扩大目标机台数Map，不改变实际排程窗口、日计划扣账或班次生成范围。
        LocalDate calculationStartDate = this.resolveRequiredMachineCalculationStartDate(context);
        LocalDate calculationEndDate = this.resolveRequiredMachineCalculationEndDate(context);
        List<LocalDate> calculationDates = new ArrayList<LocalDate>(
                Math.max(1, (int) java.time.temporal.ChronoUnit.DAYS.between(
                        calculationStartDate, calculationEndDate) + 1));
        for (LocalDate productionDate = calculationStartDate;
             !productionDate.isAfter(calculationEndDate);
             productionDate = productionDate.plusDays(1)) {
            calculationDates.add(productionDate);
        }

        // 2. 计算范围涉及年月逐月加载日产能限制Map（合并式，跨月不冲突）和模具调整参数。
        calculationDates.stream().map(YearMonth::from).distinct()
                .forEach(ym -> this.loadDailyCapacityLimitMap(context, ym.getYear(), ym.getMonthValue()));
        if (context.getMouldAdjustParamMap().isEmpty()) {
            this.loadMouldAdjustParamMap(context, context.getFactoryCode());
        }

        // 3. 以全部已加载月计划中的物料+产品状态为全集，保证窗口前后判断涉及的续作SKU也有完整结果。
        //    月计划行直接复用 monthPlanByMaterialMonthMap；某月无对应计划时显式写0，区分真实0与Map缺失。
        Map<String, FactoryMonthPlanProductionFinalResult> materialMonthPlanMap =
                context.getMonthPlanByMaterialMonthMap();
        Map<String, DailyMouldSummary> summaryMap = new LinkedHashMap<>(loadedMonthPlanList.size());
        for (FactoryMonthPlanProductionFinalResult entity : loadedMonthPlanList) {
            if (Objects.isNull(entity) || StringUtils.isEmpty(entity.getMaterialCode())
                    || StringUtils.isEmpty(entity.getProductStatus())) {
                continue;
            }
            String cacheKey = DailyMouldResult.buildCacheKey(entity.getMaterialCode(), entity.getProductStatus());
            // 多个月份可能存在同一物料+状态，汇总对象只创建一次，逐日再按自然月读取对应计划行。
            if (summaryMap.containsKey(cacheKey)) {
                continue;
            }
            String materialStatusKey = MonthPlanDateResolver.buildMaterialStatusKey(
                    entity.getMaterialCode(), entity.getProductStatus());
            DailyMouldSummary summary = new DailyMouldSummary(
                    entity.getMaterialCode(), entity.getProductStatus(), entity.getStructureName());
            for (LocalDate productionDate : calculationDates) {
                FactoryMonthPlanProductionFinalResult dayPlanEntity = materialMonthPlanMap.get(
                        MonthPlanDateResolver.buildMaterialMonthKey(materialStatusKey,
                                productionDate.getYear(), productionDate.getMonthValue()));
                DailyMouldResult dayResult;
                if (Objects.isNull(dayPlanEntity)) {
                    // 该月无此SKU计划行时显式记0，保证整个跨窗口计算范围日期完整。
                    dayResult = new DailyMouldResult(entity.getMaterialCode(), entity.getProductStatus(),
                            entity.getStructureName(), productionDate.getDayOfMonth(), 0);
                    dayResult.setDate(productionDate);
                } else {
                    dayResult = this.getDailyMouldByLocalDate(context, dayPlanEntity, productionDate);
                }
                if (Objects.nonNull(dayResult)) {
                    summary.putDayMould(productionDate, dayResult);
                }
            }
            summaryMap.put(cacheKey, summary);
        }
        context.setDailyMouldResultMap(Collections.unmodifiableMap(summaryMap));

        // 4. 输出统一Map的完整日期边界和T日汇总，供后续过程日志按同一数据快照对账。
        LocalDate tDate = this.toLocalDate(scheduleDate);
        int tDayMachineQtyTotal = summaryMap.values().stream()
                .mapToInt(summary -> summary.getDayMachineQty(tDate))
                .sum();
        log.info("目标机台数统一Map预计算完成, 工厂: {}, 批次: {}, 排程T日: {}, 计算开始日: {}, "
                        + "计算结束日: {}, SKU状态数: {}, T日总机台数: {}",
                context.getFactoryCode(), context.getBatchNo(), tDate, calculationStartDate,
                calculationEndDate, summaryMap.size(), tDayMachineQtyTotal);
    }

    @Override
    public int getRequiredMachineCount(LhScheduleContext context,
                                       String materialCode,
                                       String productStatus,
                                       LocalDate productionDate) {
        if (!this.hasRequiredMachineCount(context, materialCode, productStatus, productionDate)) {
            log.warn("目标机台数统一Map查询缺失, 工厂: {}, 批次: {}, 物料: {}, 产品状态: {}, 日期: {}, 可用日期范围: {}",
                    Objects.isNull(context) ? null : context.getFactoryCode(),
                    Objects.isNull(context) ? null : context.getBatchNo(), materialCode, productStatus,
                    productionDate, this.formatAvailableDateRange(context, materialCode, productStatus));
            return 0;
        }
        DailyMouldSummary summary = this.getDailyMouldSummary(context, materialCode, productStatus);
        return summary.getDayMachineQty(productionDate);
    }

    @Override
    public boolean hasRequiredMachineCount(LhScheduleContext context,
                                           String materialCode,
                                           String productStatus,
                                           LocalDate productionDate) {
        if (Objects.isNull(context) || StringUtils.isEmpty(materialCode)
                || StringUtils.isEmpty(productStatus) || Objects.isNull(productionDate)) {
            return false;
        }
        DailyMouldSummary summary = this.getDailyMouldSummary(context, materialCode, productStatus);
        return Objects.nonNull(summary) && Objects.nonNull(summary.getDayMould(productionDate));
    }

    @Override
    public DailyMouldSummary getDailyMouldSummary(LhScheduleContext context, String materialCode, String productStatus) {
        if (Objects.isNull(context) || StringUtils.isEmpty(materialCode) || StringUtils.isEmpty(productStatus)) {
            return null;
        }
        String cacheKey = DailyMouldResult.buildCacheKey(materialCode, productStatus);
        return context.getDailyMouldResultMap().get(cacheKey);
    }

    @Override
    public DailyMouldResult getDailyMouldResult(LhScheduleContext context,
                                                String materialCode,
                                                String productStatus,
                                                LocalDate productionDate) {
        DailyMouldSummary summary = this.getDailyMouldSummary(context, materialCode, productStatus);
        if (Objects.isNull(summary) || Objects.isNull(productionDate)) {
            return null;
        }
        return summary.getDayMould(productionDate);
    }

    /**
     * 解析目标机台数统一Map计算开始日。
     *
     * @param context 排程上下文
     * @return 计算开始自然日
     */
    private LocalDate resolveRequiredMachineCalculationStartDate(LhScheduleContext context) {
        LocalDate scheduleStartDate = this.toLocalDate(context.getScheduleDate());
        int lookAroundDays = context.getScheduleConfig().getContinuousMouldOfflineCheckDays();
        return scheduleStartDate.minusDays(lookAroundDays);
    }

    /**
     * 解析目标机台数统一Map计算结束日。
     *
     * @param context 排程上下文
     * @return 计算结束自然日
     */
    private LocalDate resolveRequiredMachineCalculationEndDate(LhScheduleContext context) {
        LocalDate windowEndDate = this.toLocalDate(context.getWindowEndDate());
        int lookAheadDays = Math.max(
                context.getScheduleConfig().getContinuousMouldOfflineCheckDays(),
                LhScheduleConstant.REQUIRED_MACHINE_CROSS_WINDOW_EXTRA_DAYS);
        LocalDate defaultCalculationEndDate = windowEndDate.plusDays(lookAheadDays);
        /*
         * 续作增机提前会在窗口最后业务日检查参数范围内的窗口外首次增机日。
         * 目标机台数统一Map必须覆盖同一固定截止日，否则准入虽然允许后看，
         * 但机台数维度缺失仍会把合法候选静默过滤。
         */
        LocalDate earlyProductionMaxDate = Objects.isNull(context.getEarlyProductionMaxDate())
                ? null : this.toLocalDate(context.getEarlyProductionMaxDate());
        return Objects.nonNull(earlyProductionMaxDate)
                && earlyProductionMaxDate.isAfter(defaultCalculationEndDate)
                ? earlyProductionMaxDate : defaultCalculationEndDate;
    }

    /**
     * 格式化指定物料状态在统一Map中的可用日期范围。
     *
     * @param context 排程上下文
     * @param materialCode 物料编码
     * @param productStatus 产品状态
     * @return 可用日期范围；无结果时返回“-”
     */
    private String formatAvailableDateRange(LhScheduleContext context,
                                            String materialCode,
                                            String productStatus) {
        DailyMouldSummary summary = this.getDailyMouldSummary(context, materialCode, productStatus);
        if (Objects.isNull(summary) || summary.getDayMouldMap().isEmpty()) {
            return "-";
        }
        LocalDate firstDate = summary.getDayMouldMap().keySet().iterator().next();
        LocalDate lastDate = null;
        for (LocalDate productionDate : summary.getDayMouldMap().keySet()) {
            lastDate = productionDate;
        }
        return firstDate + "~" + lastDate;
    }

    /**
     * Date → LocalDate 转换
     *
     * @param date 日期
     * @return LocalDate
     */
    private LocalDate toLocalDate(Date date) {
        return date.toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDate();
    }
}
