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
 * 根据逐班帘布需求和当前库存基准构建当前直裁班次的待排候选规格。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class Cd90ScheduleCandidateBuilder {

    private final Cd90InventoryCalculator inventoryCalculator;
    private final Cd90StockGuaranteeCalculator stockGuaranteeCalculator;
    private final Cd90ScheduleCandidateSorter candidateSorter;
    private final Cd90FractionalDemandWindowSelector demandWindowSelector;

    /**
     * 构建并排序当前供应窗口的候选规格。
     *
     * @param demandShifts 帘布逐自然班次需求
     * @param stocksAtSix 库存基准快照；字段名保留全量排程旧口径
     * @param currentDemandStart 当前直裁班次对应的首个成型供应班次
     * @param depthClassQtyByCloth 按帘布匹配的备库班数
     * @param cumulativeConsumptionByCloth 6点至本班开始前的累计成型消耗，按帘布代号分组；> 0 表示续作规格
     * @return 已按缺料优先级稳定排序的候选规格
     */
    public List<Cd90ScheduleCandidate> build(List<Cd90DemandShift> demandShifts,
                                             List<Cd90StockSource> stocksAtSix,
                                             LocalDateTime currentDemandStart,
                                             Map<String, BigDecimal> depthClassQtyByCloth,
                                             Map<String, BigDecimal> continueDemandByCloth) {
        if (currentDemandStart == null) {
            throw new IllegalArgumentException("当前成型供应班次不能为空");
        }
        if (depthClassQtyByCloth == null) {
            throw new IllegalArgumentException("逐帘布备库深度不能为空");
        }

        // 先将多条库存按帘布汇总，后续所有窗口投影都使用同一库存基准。
        Map<String, BigDecimal> stockByCloth = aggregateStock(stocksAtSix);
        Map<String, LocalDateTime> stockBaselineByCloth = safe(stocksAtSix).stream()
                .filter(item -> item != null && item.getClothCode() != null)
                .filter(item -> item.getSnapshotTime() != null)
                .collect(Collectors.toMap(Cd90StockSource::getClothCode,
                        Cd90StockSource::getSnapshotTime,
                        (left, right) -> left.isBefore(right) ? left : right,
                        LinkedHashMap::new));
        Map<String, List<Cd90DemandShift>> shiftsByCloth = safe(demandShifts).stream()
                .filter(item -> item != null && StringUtils.hasText(item.getClothCode()))
                .filter(item -> item.getStartTime() != null)
                .collect(Collectors.groupingBy(Cd90DemandShift::getClothCode,
                        LinkedHashMap::new, Collectors.toList()));

        List<Cd90ScheduleCandidate> candidates = new ArrayList<>();
        for (Map.Entry<String, List<Cd90DemandShift>> entry : shiftsByCloth.entrySet()) {
            // 每个帘布按自然班次排序，避免数据库返回顺序影响缺料时间判断。
            List<Cd90DemandShift> allShifts = entry.getValue().stream()
                    .sorted(Comparator.comparing(Cd90DemandShift::getStartTime))
                    .collect(Collectors.toList());
            // 当前供应窗口之前且不早于库存基准的成型消耗必须先从库存扣除。
            BigDecimal consumedBeforeWindow = allShifts.stream()
                    .filter(item -> item.getStartTime().isBefore(currentDemandStart))
                    .filter(item -> stockBaselineByCloth.get(entry.getKey()) == null
                            || !item.getStartTime().isBefore(
                            stockBaselineByCloth.get(entry.getKey())))
                    .map(item -> value(item.getClothDemandQuantity()))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            List<Cd90DemandShift> availableShifts = allShifts.stream()
                    .filter(item -> !item.getStartTime().isBefore(currentDemandStart))
                    .collect(Collectors.toList());
            boolean hasAnyPositiveDemand = availableShifts.stream()
                    .anyMatch(item -> item.isIncluded()
                            && value(item.getClothDemandQuantity()).signum() > 0);
            if (!hasAnyPositiveDemand) {
                continue;
            }
            // 每个帘布按自身供成型机台数匹配需求深度，小数末班由选择器按比例换算。
            BigDecimal depthClassQty = this.requiredDepth(depthClassQtyByCloth, entry.getKey());
            List<Cd90DemandShift> windowShifts = demandWindowSelector.select(
                    availableShifts, depthClassQty);
            boolean hasPositiveDemand = windowShifts.stream()
                    .anyMatch(item -> item.isIncluded()
                            && value(item.getClothDemandQuantity()).signum() > 0);
            if (!hasPositiveDemand) {
                continue;
            }

            // 读取当前库存基准。
            BigDecimal stockAtSix = stockByCloth.getOrDefault(entry.getKey(), BigDecimal.ZERO);
            // 投影库存用于同时计算可供应时长和最早缺料班次，两者共同决定候选优先级。
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
                    .continueFromPreviousShift(isContinueFromPrevious(continueDemandByCloth, entry.getKey()))
                    .earliestShortageTime(earliestShortageTime)
                    .stockSupplyHours(guarantee.getSupplyHours())
                    .build());
        }

        // 排序器负责稳定处理同缺料时间候选，避免重复执行时结果顺序抖动。
        List<Cd90ScheduleCandidate> sorted = candidateSorter.sort(candidates);
        long continueCount = sorted.stream()
                .filter(Cd90ScheduleCandidate::isContinueFromPreviousShift)
                .count();
        log.info("[直裁自动排程] 当前班次候选规格构建完成, demandStart={}, depthByCloth={}, "
                        + "clothCount={}, candidateCount={}, continueCount={}",
                currentDemandStart, depthClassQtyByCloth, shiftsByCloth.size(), sorted.size(), continueCount);
        return sorted;
    }

    /** 续作判定：6点至本班开始前累计成型消耗 > 0 表示前序班次已为该规格排过产。 */
    private boolean isContinueFromPrevious(Map<String, BigDecimal> cumulative, String clothCode) {
        BigDecimal value = cumulative == null ? null : cumulative.get(clothCode);
        return value != null && value.signum() > 0;
    }

    /** 获取当前帘布的必填备库深度。 */
    private BigDecimal requiredDepth(Map<String, BigDecimal> depthClassQtyByCloth, String clothCode) {
        BigDecimal depthClassQty = depthClassQtyByCloth.get(clothCode);
        if (depthClassQty == null || depthClassQty.signum() <= 0) {
            throw new IllegalArgumentException("帘布未匹配有效备库深度, clothCode=" + clothCode);
        }
        return depthClassQty;
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
