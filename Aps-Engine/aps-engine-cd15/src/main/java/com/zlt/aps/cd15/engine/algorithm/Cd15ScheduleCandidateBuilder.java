package com.zlt.aps.cd15.engine.algorithm;

import com.zlt.aps.cd15.engine.model.Cd15DemandShift;
import com.zlt.aps.cd15.engine.model.Cd15InventoryProjection;
import com.zlt.aps.cd15.engine.model.Cd15ScheduleCandidate;
import com.zlt.aps.cd15.engine.model.Cd15StockGuaranteeResult;
import com.zlt.aps.cd15.engine.model.Cd15StockSource;
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
 * 根据逐班钢带需求和6点库存构建当前斜裁班次的待排候选规格。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class Cd15ScheduleCandidateBuilder {

    private final Cd15InventoryCalculator inventoryCalculator;
    private final Cd15StockGuaranteeCalculator stockGuaranteeCalculator;
    private final Cd15ScheduleCandidateSorter candidateSorter;
    private final Cd15FractionalDemandWindowSelector demandWindowSelector;

    /**
     * 构建并排序当前供应窗口的候选规格。
     *
     * @param demandShifts 钢带逐自然班次需求
     * @param stocksAtSix 6点库存快照
     * @param currentDemandStart 当前斜裁班次对应的首个成型供应班次
     * @param depthClassQtyBySteelStrip 按钢带匹配的备库班数
     * @param cumulativeConsumptionBySteelStrip 6点至本班开始前的累计成型消耗，按钢带代号分组；> 0 表示续作规格
     * @return 已按缺料优先级稳定排序的候选规格
     */
    public List<Cd15ScheduleCandidate> build(List<Cd15DemandShift> demandShifts,
                                             List<Cd15StockSource> stocksAtSix,
                                             LocalDateTime currentDemandStart,
                                             Map<String, BigDecimal> depthClassQtyBySteelStrip,
                                             Map<String, BigDecimal> continueDemandBySteelStrip) {
        if (currentDemandStart == null) {
            throw new IllegalArgumentException("当前成型供应班次不能为空");
        }
        if (depthClassQtyBySteelStrip == null) {
            throw new IllegalArgumentException("逐钢带备库深度不能为空");
        }

        // 6点库存按钢带形成唯一共享池，先统一扣除窗口前全部材料的成型消耗。
        Map<String, BigDecimal> stockBySteelStrip = aggregateStock(stocksAtSix);
        Map<String, LocalDateTime> stockBaselineBySteelStrip = safe(stocksAtSix).stream()
                .filter(item -> item != null && item.getSteelStripCode() != null)
                .filter(item -> item.getSnapshotTime() != null)
                .collect(Collectors.toMap(Cd15StockSource::getSteelStripCode,
                        Cd15StockSource::getSnapshotTime,
                        (left, right) -> left.isBefore(right) ? left : right,
                        LinkedHashMap::new));
        Map<String, List<Cd15DemandShift>> shiftsByMaterial = safe(demandShifts).stream()
                .filter(item -> item != null && StringUtils.hasText(item.getSteelStripCode()))
                .filter(item -> StringUtils.hasText(item.getMaterialKey()))
                .filter(item -> item.getStartTime() != null)
                .collect(Collectors.groupingBy(Cd15DemandShift::getMaterialKey,
                        LinkedHashMap::new, Collectors.toList()));
        Map<String, BigDecimal> consumedBeforeBySteelStrip = safe(demandShifts).stream()
                .filter(item -> item != null && StringUtils.hasText(item.getSteelStripCode()))
                .filter(item -> item.getStartTime() != null
                        && item.getStartTime().isBefore(currentDemandStart))
                .filter(item -> stockBaselineBySteelStrip.get(item.getSteelStripCode()) == null
                        || !item.getStartTime().isBefore(
                        stockBaselineBySteelStrip.get(item.getSteelStripCode())))
                .collect(Collectors.groupingBy(Cd15DemandShift::getSteelStripCode,
                        Collectors.reducing(BigDecimal.ZERO,
                                item -> value(item.getSteelStripDemandQuantity()),
                                BigDecimal::add)));
        Map<String, BigDecimal> remainingStockBySteelStrip = new LinkedHashMap<>();
        shiftsByMaterial.values().stream().flatMap(List::stream)
                .map(Cd15DemandShift::getSteelStripCode).distinct()
                .forEach(steelStripCode -> remainingStockBySteelStrip.put(
                        steelStripCode,
                        stockBySteelStrip.getOrDefault(steelStripCode, BigDecimal.ZERO)
                                .subtract(consumedBeforeBySteelStrip.getOrDefault(
                                        steelStripCode, BigDecimal.ZERO))
                                .max(BigDecimal.ZERO)));
        List<Map.Entry<String, List<Cd15DemandShift>>> materialEntries =
                shiftsByMaterial.entrySet().stream()
                        .sorted(Comparator
                                .comparing((Map.Entry<String, List<Cd15DemandShift>> entry) ->
                                        entry.getValue().get(0).getSteelStripCode())
                                .thenComparing(entry -> entry.getValue().stream()
                                        .map(Cd15DemandShift::getStartTime)
                                        .min(LocalDateTime::compareTo)
                                        .orElse(LocalDateTime.MAX))
                                .thenComparing(Map.Entry::getKey))
                        .collect(Collectors.toList());

        List<Cd15ScheduleCandidate> candidates = new ArrayList<>();
        for (Map.Entry<String, List<Cd15DemandShift>> entry : materialEntries) {
            // 每种施工材料按自然班次排序，避免数据库返回顺序影响缺料时间判断。
            List<Cd15DemandShift> allShifts = entry.getValue().stream()
                    .sorted(Comparator.comparing(Cd15DemandShift::getStartTime))
                    .collect(Collectors.toList());
            Cd15DemandShift materialDemand = allShifts.get(0);
            String steelStripCode = materialDemand.getSteelStripCode();
            List<Cd15DemandShift> availableShifts = allShifts.stream()
                    .filter(item -> !item.getStartTime().isBefore(currentDemandStart))
                    .collect(Collectors.toList());
            boolean hasAnyPositiveDemand = availableShifts.stream()
                    .anyMatch(item -> item.isIncluded()
                            && value(item.getSteelStripDemandQuantity()).signum() > 0);
            if (!hasAnyPositiveDemand) {
                continue;
            }
            // 每个钢带按自身供成型机台数匹配需求深度，小数末班由选择器按比例换算。
            BigDecimal depthClassQty = this.requiredDepth(depthClassQtyBySteelStrip, steelStripCode);
            List<Cd15DemandShift> windowShifts = demandWindowSelector.select(
                    availableShifts, depthClassQty);
            boolean hasPositiveDemand = windowShifts.stream()
                    .anyMatch(item -> item.isIncluded()
                            && value(item.getSteelStripDemandQuantity()).signum() > 0);
            if (!hasPositiveDemand) {
                continue;
            }

            BigDecimal sharedAvailableStock = remainingStockBySteelStrip.getOrDefault(
                    steelStripCode, BigDecimal.ZERO);
            // 当前材料只使用共享池尚未被前序材料占用的库存。
            Cd15InventoryProjection projection = inventoryCalculator.project(
                    sharedAvailableStock, BigDecimal.ZERO, BigDecimal.ZERO);
            Cd15StockGuaranteeResult guarantee = stockGuaranteeCalculator.calculate(
                    projection.getExpectedAvailableStock(), windowShifts);
            LocalDateTime earliestShortageTime = findEarliestShortageTime(
                    projection.getExpectedAvailableStock(), windowShifts);
            LocalDateTime firstWindowStart = windowShifts.get(0).getStartTime();

            candidates.add(Cd15ScheduleCandidate.builder()
                    .materialKey(materialDemand.getMaterialKey())
                    .steelStripCode(steelStripCode)
                    .bigRollCode(materialDemand.getBigRollCode())
                    .cuttingAngle(materialDemand.getCuttingAngle())
                    .craftWidth(materialDemand.getCraftWidth())
                    .unitConsumeMillimeter(materialDemand.getUnitConsumeMillimeter())
                    .shortageInCurrentShift(firstWindowStart.equals(earliestShortageTime))
                    .cordWidth(materialDemand.getCordWidth())
                    .curlLength(materialDemand.getCurlLength())
                    .continueFromPreviousShift(isContinueFromPrevious(
                            continueDemandBySteelStrip, materialDemand.getMaterialKey()))
                    .earliestShortageTime(earliestShortageTime)
                    .stockSupplyHours(guarantee.getSupplyHours())
                    .build());
            BigDecimal remainingStock = sharedAvailableStock.subtract(
                    this.windowDemand(windowShifts)).max(BigDecimal.ZERO);
            remainingStockBySteelStrip.put(steelStripCode, remainingStock);
        }

        // 排序器负责稳定处理同缺料时间候选，避免重复执行时结果顺序抖动。
        List<Cd15ScheduleCandidate> sorted = candidateSorter.sort(candidates);
        long continueCount = sorted.stream()
                .filter(Cd15ScheduleCandidate::isContinueFromPreviousShift)
                .count();
        log.info("[斜裁自动排程] 当前班次候选规格构建完成, demandStart={}, depthBySteelStrip={}, "
                        + "materialCount={}, candidateCount={}, continueCount={}",
                currentDemandStart, depthClassQtyBySteelStrip, shiftsByMaterial.size(), sorted.size(), continueCount);
        return sorted;
    }

    /** 续作判定：6点至本班开始前累计成型消耗 > 0 表示前序班次已为该规格排过产。 */
    private boolean isContinueFromPrevious(Map<String, BigDecimal> cumulative, String steelStripCode) {
        BigDecimal value = cumulative == null ? null : cumulative.get(steelStripCode);
        return value != null && value.signum() > 0;
    }

    /** 获取当前钢带的必填备库深度。 */
    private BigDecimal requiredDepth(Map<String, BigDecimal> depthClassQtyBySteelStrip, String steelStripCode) {
        BigDecimal depthClassQty = depthClassQtyBySteelStrip.get(steelStripCode);
        if (depthClassQty == null || depthClassQty.signum() <= 0) {
            throw new IllegalArgumentException("钢带未匹配有效备库深度, steelStripCode=" + steelStripCode);
        }
        return depthClassQty;
    }

    private Map<String, BigDecimal> aggregateStock(List<Cd15StockSource> stocksAtSix) {
        Map<String, BigDecimal> result = new LinkedHashMap<>();
        for (Cd15StockSource stock : safe(stocksAtSix)) {
            if (stock == null || !StringUtils.hasText(stock.getSteelStripCode())) {
                continue;
            }
            BigDecimal quantity = value(stock.getStockQuantity());
            if (quantity.signum() < 0) {
                throw new IllegalArgumentException("6点库存不能小于0, steelStripCode=" + stock.getSteelStripCode());
            }
            result.merge(stock.getSteelStripCode(), quantity, BigDecimal::add);
        }
        return result;
    }

    private LocalDateTime findEarliestShortageTime(BigDecimal availableStock,
                                                    List<Cd15DemandShift> windowShifts) {
        BigDecimal remaining = availableStock;
        for (Cd15DemandShift shift : windowShifts) {
            if (!shift.isIncluded()) {
                continue;
            }
            BigDecimal demand = value(shift.getSteelStripDemandQuantity());
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

    /** 汇总当前材料窗口内实际参与排程的钢带需求量。 */
    private BigDecimal windowDemand(List<Cd15DemandShift> windowShifts) {
        return safe(windowShifts).stream()
                .filter(Cd15DemandShift::isIncluded)
                .map(Cd15DemandShift::getSteelStripDemandQuantity)
                .map(this::value)
                .filter(quantity -> quantity.signum() > 0)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private <T> List<T> safe(List<T> values) {
        return values == null ? Collections.emptyList() : values;
    }

    private BigDecimal value(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
