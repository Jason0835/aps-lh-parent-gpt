package com.zlt.aps.common.engine.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zlt.aps.common.engine.domain.dto.MonthPlanFirstDateDTO;
import com.zlt.aps.common.engine.mapper.EarlyProductionMonthPlanMapper;
import com.zlt.aps.common.engine.mapper.EarlyProductionVersionMapper;
import com.zlt.aps.common.engine.service.IMonthPlanEarlyProductionService;
import com.zlt.aps.mp.api.domain.entity.FactoryMonthPlanProductionFinalResult;
import com.zlt.aps.mp.api.domain.entity.MpFactoryProductionVersion;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * 批量解析月计划首个可提前日期，查询入口和快照入口共用同一日期计算。
 *
 * @author APS
 * @date 2026/11/16
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class MonthPlanEarlyProductionServiceImpl implements IMonthPlanEarlyProductionService {

    /**
     * 有效定稿标识，与月计划排产版本口径一致。
     */
    private static final String FINAL_VERSION_FLAG = "1";

    /**
     * 排程窗口固定为 T 至 T+2，共三个自然日。
     */
    private static final int SCHEDULE_WINDOW_DAYS = 3;

    /**
     * 原始月计划日量字段模板。
     */
    private static final String DAY_QUANTITY_FIELD = "day%d";

    /**
     * 定稿月计划批量查询接口。
     */
    private final EarlyProductionMonthPlanMapper monthPlanMapper;

    /**
     * 定稿排产版本批量查询接口。
     */
    private final EarlyProductionVersionMapper versionMapper;

    /**
     * 是否提供首个可提前计划日期，未配置时默认关闭。
     */
    @Value("${aps.month-plan.early-production-first-date-enabled:false}")
    private boolean firstPlanDateEnabled;

    /**
     * 批量加载范围内各月有效定稿计划并计算首个正计划量日期。
     *
     * @param factoryCode 工厂编码
     * @param windowEndDate 排程窗口结束自然日
     * @param advanceDays 调用方解析后的提前天数
     * @return 窗口内无正计划的物料与产品状态的最早日期，参数缺失或无匹配时为空列表
     */
    @Override
    public List<MonthPlanFirstDateDTO> listFirstPlanDates(
            String factoryCode, LocalDate windowEndDate, int advanceDays) {
        if (!firstPlanDateEnabled) {
            log.info("提前生产首个计划日期功能关闭, factoryCode: {}, windowEndDate: {}, firstPlanDate: null",
                    factoryCode, windowEndDate);
            return Collections.emptyList();
        }
        if (StringUtils.isBlank(factoryCode) || Objects.isNull(windowEndDate) || advanceDays <= 0) {
            return Collections.emptyList();
        }
        LocalDate windowStartDate = windowEndDate.minusDays(SCHEDULE_WINDOW_DAYS - 1);
        YearMonth firstMonth = YearMonth.from(windowStartDate);
        YearMonth lastMonth = YearMonth.from(windowEndDate.plusDays(advanceDays));
        LambdaQueryWrapper<MpFactoryProductionVersion> versionQuery = new LambdaQueryWrapper<>();
        versionQuery.eq(MpFactoryProductionVersion::getFactoryCode, factoryCode)
                .eq(MpFactoryProductionVersion::getIsFinal, FINAL_VERSION_FLAG)
                .and(monthQuery -> {
                    for (YearMonth month = firstMonth; !month.isAfter(lastMonth); month = month.plusMonths(1)) {
                        int yearValue = month.getYear();
                        int monthValue = month.getMonthValue();
                        monthQuery.or().nested(monthCondition -> monthCondition
                                .eq(MpFactoryProductionVersion::getYear, yearValue)
                                .eq(MpFactoryProductionVersion::getMonth, monthValue));
                    }
                })
                .orderByDesc(MpFactoryProductionVersion::getUpdateTime)
                .orderByDesc(MpFactoryProductionVersion::getId);
        List<MpFactoryProductionVersion> versions = versionMapper.selectList(versionQuery);
        if (CollectionUtils.isEmpty(versions)) {
            return Collections.emptyList();
        }
        Map<YearMonth, MpFactoryProductionVersion> versionByMonth = new LinkedHashMap<>();
        for (MpFactoryProductionVersion version : versions) {
            versionByMonth.putIfAbsent(YearMonth.of(version.getYear(), version.getMonth()), version);
        }
        // 最新定稿行缺少排产版本时按无数据处理，不能回退到旧定稿行。
        versionByMonth.values().removeIf(version -> StringUtils.isBlank(version.getProductionVersion()));
        if (versionByMonth.isEmpty()) {
            return Collections.emptyList();
        }
        LambdaQueryWrapper<FactoryMonthPlanProductionFinalResult> planQuery = new LambdaQueryWrapper<>();
        planQuery.eq(FactoryMonthPlanProductionFinalResult::getFactoryCode, factoryCode)
                .and(monthQuery -> versionByMonth.forEach((month, version) -> monthQuery.or()
                        .nested(monthCondition -> monthCondition
                                .eq(FactoryMonthPlanProductionFinalResult::getYear, month.getYear())
                                .eq(FactoryMonthPlanProductionFinalResult::getMonth, month.getMonthValue())
                                .eq(FactoryMonthPlanProductionFinalResult::getProductionVersion,
                                        version.getProductionVersion()))));
        // 同一排产版本可能包含原始及调整需求版本，不能追加 MONTH_PLAN_VERSION 条件。
        return this.calculateFirstPlanDates(factoryCode, windowEndDate, advanceDays,
                monthPlanMapper.selectList(planQuery));
    }

    /**
     * 按记录所属年月扫描范围内原始日量，按物料与产品状态归并最早日期。
     *
     * @param factoryCode 工厂编码
     * @param windowEndDate 排程窗口结束自然日
     * @param advanceDays 调用方解析后的提前天数
     * @param monthPlans 有效定稿计划快照，不修改其中任何记录
     * @return 按物料和产品状态排序的匹配列表，无匹配时为空列表
     */
    @Override
    public List<MonthPlanFirstDateDTO> calculateFirstPlanDates(
            String factoryCode, LocalDate windowEndDate, int advanceDays,
            List<FactoryMonthPlanProductionFinalResult> monthPlans) {
        if (!firstPlanDateEnabled) {
            log.info("提前生产首个计划日期功能关闭, factoryCode: {}, windowEndDate: {}, firstPlanDate: null",
                    factoryCode, windowEndDate);
            return Collections.emptyList();
        }
        if (StringUtils.isBlank(factoryCode) || Objects.isNull(windowEndDate)
                || advanceDays <= 0 || CollectionUtils.isEmpty(monthPlans)) {
            return Collections.emptyList();
        }
        LocalDate windowStartDate = windowEndDate.minusDays(SCHEDULE_WINDOW_DAYS - 1);
        LocalDate rangeStart = windowEndDate.plusDays(1);
        LocalDate rangeEnd = windowEndDate.plusDays(advanceDays);
        Map<String, Map<String, LocalDate>> datesByMaterial = new LinkedHashMap<>();
        Map<String, Set<String>> windowPlannedStatuses = new LinkedHashMap<>();
        for (FactoryMonthPlanProductionFinalResult plan : monthPlans) {
            if (Objects.isNull(plan) || !factoryCode.equals(plan.getFactoryCode())
                    || StringUtils.isBlank(plan.getMaterialCode())
                    || Objects.isNull(plan.getYear()) || Objects.isNull(plan.getMonth())) {
                continue;
            }
            String productStatus = StringUtils.trimToEmpty(plan.getProductStatus());
            // 同物料状态可能跨月或存在多行需求，任一窗口正日量都排除整组，不能只跳过当前行。
            if (Objects.nonNull(this.findFirstPlanDate(plan, windowStartDate, windowEndDate))) {
                windowPlannedStatuses.computeIfAbsent(plan.getMaterialCode(), ignored -> new HashSet<>())
                        .add(productStatus);
            }
            LocalDate firstPlanDate = this.findFirstPlanDate(plan, rangeStart, rangeEnd);
            if (Objects.nonNull(firstPlanDate)) {
                datesByMaterial.computeIfAbsent(plan.getMaterialCode(), ignored -> new LinkedHashMap<>())
                        .merge(productStatus, firstPlanDate,
                                (existingDate, candidateDate) -> existingDate.isBefore(candidateDate)
                                        ? existingDate : candidateDate);
            }
        }
        List<MonthPlanFirstDateDTO> results = new ArrayList<>();
        datesByMaterial.forEach((materialCode, statusDates) -> statusDates.forEach((productStatus, firstDate) -> {
            if (windowPlannedStatuses.getOrDefault(materialCode, Collections.emptySet()).contains(productStatus)) {
                return;
            }
            MonthPlanFirstDateDTO result = new MonthPlanFirstDateDTO();
            result.setMaterialCode(materialCode);
            result.setProductStatus(productStatus);
            result.setFirstPlanDate(firstDate);
            results.add(result);
        }));
        results.sort(Comparator.comparing(MonthPlanFirstDateDTO::getMaterialCode)
                .thenComparing(MonthPlanFirstDateDTO::getProductStatus));
        results.forEach(result -> log.info(
                "提前生产首个计划日期匹配, factoryCode: {}, materialCode: {}, productStatus: {}, firstPlanDate: {}, "
                        + "windowEndDate: {}, advanceDays: {}",
                factoryCode, result.getMaterialCode(), result.getProductStatus(), result.getFirstPlanDate(),
                windowEndDate, advanceDays));
        log.info("提前生产首个计划日期计算完成, enabled: true, factoryCode: {}, windowEndDate: {}, "
                        + "advanceDays: {}, matchedSkuCount: {}",
                factoryCode, windowEndDate, advanceDays, results.size());
        return results;
    }

    /**
     * 在单行计划所属自然月与允许范围的交集中寻找首个正日量。
     *
     * @param plan 月计划行
     * @param rangeStart 允许提前范围开始日，包含当天
     * @param rangeEnd 允许提前范围结束日，包含当天
     * @return 首个正计划量日期，范围无交集或无正日量时返回 null
     */
    private LocalDate findFirstPlanDate(FactoryMonthPlanProductionFinalResult plan,
                                        LocalDate rangeStart, LocalDate rangeEnd) {
        YearMonth planMonth = YearMonth.of(plan.getYear(), plan.getMonth());
        LocalDate scanStart = planMonth.atDay(1).isAfter(rangeStart) ? planMonth.atDay(1) : rangeStart;
        LocalDate scanEnd = planMonth.atEndOfMonth().isBefore(rangeEnd) ? planMonth.atEndOfMonth() : rangeEnd;
        for (LocalDate date = scanStart; !date.isAfter(scanEnd); date = date.plusDays(1)) {
            Integer quantity = (Integer) plan.getFieldValueByFieldName(
                    String.format(DAY_QUANTITY_FIELD, date.getDayOfMonth()));
            if (Objects.nonNull(quantity) && quantity > 0) {
                return date;
            }
        }
        return null;
    }
}
