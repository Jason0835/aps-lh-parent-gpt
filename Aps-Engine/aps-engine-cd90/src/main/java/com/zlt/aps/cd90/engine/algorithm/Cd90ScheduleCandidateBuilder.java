package com.zlt.aps.cd90.engine.algorithm;

import com.zlt.aps.cd90.engine.model.Cd90DemandShift;
import com.zlt.aps.cd90.engine.model.Cd90InventoryProjection;
import com.zlt.aps.cd90.engine.model.Cd90ScheduleCandidate;
import com.zlt.aps.cd90.engine.model.Cd90StockGuaranteeResult;
import com.zlt.aps.cd90.engine.model.Cd90StockSource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 根据逐班帘布需求和6点库存构建当前直裁班次的待排候选规格。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class Cd90ScheduleCandidateBuilder {

    private final Cd90InventoryCalculator inventoryCalculator;
    private final Cd90StockGuaranteeCalculator stockGuaranteeCalculator;
    private final Cd90ScheduleCandidateSorter candidateSorter;

    /**
     * 构建并排序当前供应窗口的候选规格。
     *
     * @param demandShifts 帘布逐自然班次需求
     * @param stocksAtSix 6点库存快照
     * @param currentDemandStart 当前直裁班次对应的首个成型供应班次
     * @param demandWindow 成型需求窗口班数
     * @return 已按缺料优先级稳定排序的候选规格
     */
    public List<Cd90ScheduleCandidate> build(List<Cd90DemandShift> demandShifts,
                                             List<Cd90StockSource> stocksAtSix,
                                             LocalDateTime currentDemandStart,
                                             int demandWindow) {
        if (currentDemandStart == null) {
            throw new IllegalArgumentException("当前成型供应班次不能为空");
        }
        if (demandWindow <= 0) {
            throw new IllegalArgumentException("成型需求窗口班数必须大于0");
        }

        Map<String, BigDecimal> stockByCloth = aggregateStock(stocksAtSix);
        Map<String, List<Cd90DemandShift>> shiftsByCloth = safe(demandShifts).stream()
                .filter(item -> item != null && StringUtils.hasText(item.getClothCode()))
                .filter(item -> item.getStartTime() != null)
                .collect(Collectors.groupingBy(Cd90DemandShift::getClothCode,
                        LinkedHashMap::new, Collectors.toList()));

        List<Cd90ScheduleCandidate> candidates = new ArrayList<>();
        for (Map.Entry<String, List<Cd90DemandShift>> entry : shiftsByCloth.entrySet()) {
            List<Cd90DemandShift> allShifts = entry.getValue().stream()
                    .sorted(Comparator.comparing(Cd90DemandShift::getStartTime))
                    .collect(Collectors.toList());
            BigDecimal consumedBeforeWindow = allShifts.stream()
                    .filter(item -> item.getStartTime().isBefore(currentDemandStart))
                    .map(item -> value(item.getClothDemandQuantity()))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            List<Cd90DemandShift> windowShifts = allShifts.stream()
                    .filter(item -> !item.getStartTime().isBefore(currentDemandStart))
                    .limit(demandWindow)
                    .collect(Collectors.toList());
            boolean hasPositiveDemand = windowShifts.stream()
                    .anyMatch(item -> item.isIncluded()
                            && value(item.getClothDemandQuantity()).signum() > 0);
            if (!hasPositiveDemand) {
                continue;
            }

            BigDecimal stockAtSix = stockByCloth.getOrDefault(entry.getKey(), BigDecimal.ZERO);
            Cd90InventoryProjection projection = inventoryCalculator.project(
                    stockAtSix, consumedBeforeWindow, BigDecimal.ZERO);
            Cd90StockGuaranteeResult guarantee = stockGuaranteeCalculator.calculate(
                    projection.getExpectedAvailableStock(), windowShifts);
            LocalDateTime earliestShortageTime = findEarliestShortageTime(
                    projection.getExpectedAvailableStock(), windowShifts);
            LocalDateTime firstWindowStart = windowShifts.get(0).getStartTime();

            candidates.add(Cd90ScheduleCandidate.builder()
                    .clothCode(entry.getKey())
                    .shortageInCurrentShift(firstWindowStart.equals(earliestShortageTime))
                    .earliestShortageTime(earliestShortageTime)
                    .stockSupplyHours(guarantee.getSupplyHours())
                    .build());
        }

        List<Cd90ScheduleCandidate> sorted = candidateSorter.sort(candidates);
        log.info("[直裁自动排程] 当前班次候选规格构建完成, demandStart={}, demandWindow={}, "
                        + "clothCount={}, candidateCount={}",
                currentDemandStart, demandWindow, shiftsByCloth.size(), sorted.size());
        return sorted;
    }

    private Map<String, BigDecimal> aggregateStock(List<Cd90StockSource> stocksAtSix) {
        Map<String, BigDecimal> result = new LinkedHashMap<>();
        for (Cd90StockSource stock : safe(stocksAtSix)) {
            if (stock == null || !StringUtils.hasText(stock.getClothCode())) {
                continue;
            }
            BigDecimal quantity = value(stock.getStockQuantity());
            if (quantity.signum() < 0) {
                throw new IllegalArgumentException("6点库存不能小于0, clothCode=" + stock.getClothCode());
            }
            result.merge(stock.getClothCode(), quantity, BigDecimal::add);
        }
        return result;
    }

    private LocalDateTime findEarliestShortageTime(BigDecimal availableStock,
                                                    List<Cd90DemandShift> windowShifts) {
        BigDecimal remaining = availableStock;
        for (Cd90DemandShift shift : windowShifts) {
            if (!shift.isIncluded()) {
                continue;
            }
            BigDecimal demand = value(shift.getClothDemandQuantity());
            if (demand.signum() <= 0) {
                continue;
            }
            if (remaining.compareTo(demand) < 0) {
                return shift.getStartTime();
            }
            remaining = remaining.subtract(demand);
        }
        return null;
    }

    private <T> List<T> safe(List<T> values) {
        return values == null ? Collections.emptyList() : values;
    }

    private BigDecimal value(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
