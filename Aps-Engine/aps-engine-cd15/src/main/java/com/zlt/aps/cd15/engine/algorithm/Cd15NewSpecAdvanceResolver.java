package com.zlt.aps.cd15.engine.algorithm;

import com.zlt.aps.cd15.engine.model.Cd15DemandShift;
import com.zlt.aps.cd15.engine.model.Cd15NewSpecAdvanceInfo;
import com.zlt.aps.cd15.engine.model.Cd15NewSpecAdvanceResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 新增规格判定与未来需求提前归并解析器。
 *
 * <p>该组件只处理内存数据，不访问数据库。原始成型需求保留给库存消耗和库排释放，
 * 返回的调整后需求仅用于候选构建和斜裁净需求计算。</p>
 */
@Slf4j
@Component
public class Cd15NewSpecAdvanceResolver {

    /**
     * 根据历史已排钢带集合识别新增规格并生成去重计划需求。
     *
     * @param scheduleDate 自动排程日期
     * @param lookbackDays 历史排程计划量回看天数
     * @param advanceDays 未来需求前瞻天数
     * @param demandShifts 原始成型需求
     * @param scheduledSteelStripCodes 历史窗口内已有排程计划量的钢带代号
     * @return 提前生产证据和去重计划需求
     */
    public Cd15NewSpecAdvanceResult resolve(LocalDate scheduleDate,
                                            int lookbackDays,
                                            int advanceDays,
                                            List<Cd15DemandShift> demandShifts,
                                            Set<String> scheduledSteelStripCodes) {
        this.validate(scheduleDate, lookbackDays, advanceDays);
        LocalDate historyStartDate = scheduleDate.minusDays(lookbackDays);
        LocalDate historyEndDate = scheduleDate.minusDays(1);
        LocalDate demandEndDate = scheduleDate.plusDays(advanceDays - 1L);
        LocalDate targetProductionDate = scheduleDate.minusDays(1);
        Set<String> scheduledCodes = scheduledSteelStripCodes == null
                ? Collections.emptySet() : scheduledSteelStripCodes;

        Map<String, List<Cd15DemandShift>> advanceSources = this.safe(demandShifts).stream()
                .filter(this::isPositiveIncludedDemand)
                .filter(item -> this.inWindow(item.getStartTime().toLocalDate(),
                        scheduleDate, demandEndDate))
                .filter(item -> !scheduledCodes.contains(item.getSteelStripCode()))
                .collect(Collectors.groupingBy(Cd15DemandShift::getSteelStripCode,
                        LinkedHashMap::new, Collectors.toList()));
        Map<String, Cd15NewSpecAdvanceInfo> infoBySteelStrip = advanceSources.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey,
                        entry -> this.buildInfo(entry.getKey(), entry.getValue(),
                                historyStartDate, historyEndDate,
                                targetProductionDate, advanceDays),
                        (first, second) -> first, LinkedHashMap::new));
        List<Cd15DemandShift> adjustedDemands = this.applySnapshot(demandShifts, infoBySteelStrip);

        log.info("[斜裁自动排程] 新增规格提前需求解析完成, scheduleDate={}, historyRange={}~{}, "
                        + "demandRange={}~{}, scheduledSteelStripCount={}, newSpecSteelStripCount={}",
                scheduleDate, historyStartDate, historyEndDate, scheduleDate, demandEndDate,
                scheduledCodes.size(), infoBySteelStrip.size());
        infoBySteelStrip.values().forEach(info ->
                log.info("[斜裁自动排程] 新增规格需求提前归并, steelStripCode={}, sourceDates={}, "
                                + "advanceQuantity={}, targetProductionDate={}",
                        info.getSteelStripCode(), info.getSourceDemandDates(),
                        info.getAdvanceDemandQuantity(), info.getTargetProductionDate()));
        return Cd15NewSpecAdvanceResult.builder()
                .adjustedDemandShifts(adjustedDemands)
                .advanceInfoBySteelStrip(infoBySteelStrip)
                .build();
    }

    /**
     * 将首班锁定的提前生产证据应用到重新加载的原始需求。
     *
     * @param demandShifts 重新加载的原始成型需求
     * @param infoBySteelStrip 首班锁定的提前生产证据
     * @return 去重后的计划需求副本
     */
    public List<Cd15DemandShift> applySnapshot(
            List<Cd15DemandShift> demandShifts,
            Map<String, Cd15NewSpecAdvanceInfo> infoBySteelStrip) {
        Set<String> movedDemandKeys = infoBySteelStrip == null
                ? Collections.emptySet()
                : infoBySteelStrip.values().stream()
                        .filter(item -> item != null && item.getSourceDemandKeys() != null)
                        .map(Cd15NewSpecAdvanceInfo::getSourceDemandKeys)
                        .flatMap(Collection::stream)
                        .collect(Collectors.toSet());
        return this.safe(demandShifts).stream()
                .map(item -> this.copyDemand(item,
                        item != null && movedDemandKeys.contains(this.demandKey(item))))
                .collect(Collectors.toList());
    }

    /** 构建单个钢带的提前生产证据和解释文案。 */
    private Cd15NewSpecAdvanceInfo buildInfo(String steelStripCode,
                                             List<Cd15DemandShift> sources,
                                             LocalDate historyStartDate,
                                             LocalDate historyEndDate,
                                             LocalDate targetProductionDate,
                                             int advanceDays) {
        List<LocalDate> sourceDemandDates = sources.stream()
                .map(Cd15DemandShift::getStartTime)
                .map(java.time.LocalDateTime::toLocalDate)
                .distinct().sorted().collect(Collectors.toList());
        List<String> sourceDemandKeys = sources.stream()
                .map(this::demandKey).distinct().collect(Collectors.toList());
        BigDecimal advanceDemandQuantity = sources.stream()
                .map(Cd15DemandShift::getSteelStripDemandQuantity)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        String sourceDates = sourceDemandDates.stream()
                .map(LocalDate::toString).collect(Collectors.joining("、"));
        String analysis = "新增规格提前生产：钢带" + steelStripCode + "在"
                + historyStartDate + "至" + historyEndDate
                + "无历史排程计划，原需求日期" + sourceDates
                + "，按提前生产参数前瞻" + advanceDays + "天，归并至"
                + targetProductionDate + "生产。";
        return Cd15NewSpecAdvanceInfo.builder()
                .steelStripCode(steelStripCode)
                .historyStartDate(historyStartDate)
                .historyEndDate(historyEndDate)
                .sourceDemandDates(sourceDemandDates)
                .sourceDemandKeys(sourceDemandKeys)
                .advanceDemandQuantity(advanceDemandQuantity)
                .targetProductionDate(targetProductionDate)
                .analysis(analysis)
                .build();
    }

    /** 复制需求；命中提前生产时仅清空计划需求，不修改原对象。 */
    private Cd15DemandShift copyDemand(Cd15DemandShift source, boolean moved) {
        if (source == null) {
            return null;
        }
        return Cd15DemandShift.builder()
                .materialKey(source.getMaterialKey())
                .steelStripCode(source.getSteelStripCode())
                .bigRollCode(source.getBigRollCode())
                .cuttingAngle(source.getCuttingAngle())
                .craftWidth(source.getCraftWidth())
                .unitConsumeMillimeter(source.getUnitConsumeMillimeter())
                .reinforcement(source.isReinforcement())
                .classField(source.getClassField())
                .shiftKey(source.getShiftKey())
                .startTime(source.getStartTime())
                .formingQuantity(source.getFormingQuantity())
                .steelStripDemandQuantity(moved
                        ? BigDecimal.ZERO : source.getSteelStripDemandQuantity())
                .shiftHours(source.getShiftHours())
                .windowWeight(source.getWindowWeight())
                .included(!moved && source.isIncluded())
                .stopped(source.isStopped())
                .build();
    }

    /** 判断需求是否可进入新增规格前瞻窗口。 */
    private boolean isPositiveIncludedDemand(Cd15DemandShift demand) {
        return demand != null
                && StringUtils.hasText(demand.getSteelStripCode())
                && demand.getStartTime() != null
                && demand.isIncluded()
                && this.value(demand.getSteelStripDemandQuantity()).signum() > 0;
    }

    /** 生成跨班重载稳定的需求唯一键。 */
    private String demandKey(Cd15DemandShift demand) {
        String sourceKey = StringUtils.hasText(demand.getShiftKey())
                ? demand.getShiftKey()
                : String.valueOf(demand.getClassField()) + "|" + demand.getStartTime();
        return demand.getSteelStripCode() + "#" + sourceKey;
    }

    /** 判断日期是否位于包含首尾的窗口内。 */
    private boolean inWindow(LocalDate value, LocalDate start, LocalDate end) {
        return !value.isBefore(start) && !value.isAfter(end);
    }

    /** 校验规则计算所需的日期和参数。 */
    private void validate(LocalDate scheduleDate, int lookbackDays, int advanceDays) {
        if (scheduleDate == null) {
            throw new IllegalArgumentException("新增规格提前生产的排程日期不能为空");
        }
        // if (lookbackDays <= 0 || advanceDays <= 0) {
        //     throw new IllegalArgumentException("新增规格回看天数和前瞻天数必须为正整数");
        // }
    }

    /** 空列表保护。 */
    private List<Cd15DemandShift> safe(List<Cd15DemandShift> values) {
        return values == null ? Collections.emptyList() : values;
    }

    /** 空数值按0处理。 */
    private BigDecimal value(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
