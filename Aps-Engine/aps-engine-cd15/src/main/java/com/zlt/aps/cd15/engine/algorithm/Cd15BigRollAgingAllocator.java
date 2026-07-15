package com.zlt.aps.cd15.engine.algorithm;

import com.zlt.aps.cd15.engine.model.Cd15BigRollAgingAllocation;
import com.zlt.aps.cd15.engine.model.Cd15BigRollAgingAllocationItem;
import com.zlt.aps.cd15.engine.model.Cd15BigRollAgingStock;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * GDYY大卷静置成熟流水分配器。
 */
@Slf4j
@Component
public class Cd15BigRollAgingAllocator {

    /** 大卷静置期未满。 */
    public static final String AGING_PERIOD_LIMIT = "AGING_PERIOD_LIMIT";

    /**
     * 试算本次任务需要等待到哪个时间开裁，不修改库存流水占用量。
     */
    public Cd15BigRollAgingAllocation preview(List<Cd15BigRollAgingStock> stocks,
                                              String bigRollCode,
                                              BigDecimal requestedQuantity,
                                              LocalDateTime originalStartTime) {
        BigDecimal requestQty = requestedQuantity == null ? BigDecimal.ZERO : requestedQuantity;
        if (requestQty.compareTo(BigDecimal.ZERO) <= 0 || originalStartTime == null) {
            return this.failure(requestQty, originalStartTime);
        }

        List<Cd15BigRollAgingAllocationItem> items = new ArrayList<>();
        BigDecimal remaining = requestQty;
        LocalDateTime latestReleaseTime = originalStartTime;
        for (Cd15BigRollAgingStock stock : this.sortedAvailableStocks(stocks, bigRollCode)) {
            BigDecimal stockRemaining = stock.getRemainingQuantity();
            if (stockRemaining.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            BigDecimal used = stockRemaining.min(remaining);
            items.add(Cd15BigRollAgingAllocationItem.builder().stock(stock).quantity(used).build());
            remaining = remaining.subtract(used);
            if (stock.getReleaseTime().isAfter(latestReleaseTime)) {
                latestReleaseTime = stock.getReleaseTime();
            }
            if (remaining.compareTo(BigDecimal.ZERO) <= 0) {
                LocalDateTime taskStartTime = latestReleaseTime.isAfter(originalStartTime)
                        ? latestReleaseTime : originalStartTime;
                return Cd15BigRollAgingAllocation.builder()
                        .success(true)
                        .requestedQuantity(requestQty)
                        .allocatedQuantity(requestQty)
                        .originalStartTime(originalStartTime)
                        .taskStartTime(taskStartTime)
                        .latestReleaseTime(latestReleaseTime)
                        .delaySeconds(this.delaySeconds(originalStartTime, taskStartTime))
                        .items(items)
                        .build();
            }
        }
        BigDecimal totalAvailable = requestQty.subtract(remaining);
        log.warn("[斜裁自动排程] GDYY大卷成熟库存不足, bigRollCode={}, requestedQuantity={}, "
                        + "totalAvailableInStocks={}, shortfall={}, stockDetails={}",
                bigRollCode, requestQty, totalAvailable, remaining,
                stocks == null ? "[]" : stocks.stream()
                        .filter(stock -> stock != null && bigRollCode.equals(stock.getBigRollCode()))
                        .map(stock -> String.format("{source=%s, available=%s, allocated=%s, remaining=%s, releaseTime=%s}",
                                stock.getSourceId(), stock.getAvailableQuantity(), stock.getAllocatedQuantity(),
                                stock.getRemainingQuantity(), stock.getReleaseTime()))
                        .collect(Collectors.joining(", ")));
        return this.failure(requestQty, originalStartTime);
    }

    /**
     * 分配并立即扣减占用量。
     */
    public Cd15BigRollAgingAllocation allocate(List<Cd15BigRollAgingStock> stocks,
                                               String bigRollCode,
                                               BigDecimal requestedQuantity,
                                               LocalDateTime originalStartTime) {
        Cd15BigRollAgingAllocation allocation = this.preview(stocks, bigRollCode, requestedQuantity, originalStartTime);
        this.commit(allocation);
        return allocation;
    }

    /**
     * 将试算结果提交到当前滚动资源快照。
     */
    public void commit(Cd15BigRollAgingAllocation allocation) {
        if (allocation == null || !allocation.isSuccess() || allocation.getItems() == null) {
            return;
        }
        allocation.getItems().stream()
                .filter(item -> item.getStock() != null && item.getQuantity() != null)
                .forEach(item -> item.getStock().addAllocatedQuantity(item.getQuantity()));
    }

    private List<Cd15BigRollAgingStock> sortedAvailableStocks(List<Cd15BigRollAgingStock> stocks, String bigRollCode) {
        if (stocks == null || stocks.isEmpty()) {
            return new ArrayList<>();
        }
        return stocks.stream()
                .filter(stock -> this.sameBigRoll(stock, bigRollCode))
                .filter(stock -> stock.getReleaseTime() != null)
                .filter(stock -> stock.getRemainingQuantity().compareTo(BigDecimal.ZERO) > 0)
                .sorted(Comparator.comparing(Cd15BigRollAgingStock::getReleaseTime)
                        .thenComparing(stock -> stock.getStockInTime() == null
                                ? LocalDateTime.MAX : stock.getStockInTime())
                        .thenComparing(stock -> Objects.toString(stock.getSourceId(), "")))
                .collect(Collectors.toList());
    }

    private boolean sameBigRoll(Cd15BigRollAgingStock stock, String bigRollCode) {
        return stock != null && StringUtils.hasText(stock.getBigRollCode())
                && stock.getBigRollCode().equals(bigRollCode);
    }

    private Cd15BigRollAgingAllocation failure(BigDecimal requestedQuantity, LocalDateTime originalStartTime) {
        return Cd15BigRollAgingAllocation.builder()
                .success(false)
                .failureReason(AGING_PERIOD_LIMIT)
                .requestedQuantity(requestedQuantity)
                .allocatedQuantity(BigDecimal.ZERO)
                .originalStartTime(originalStartTime)
                .taskStartTime(originalStartTime)
                .latestReleaseTime(originalStartTime)
                .delaySeconds(0)
                .items(new ArrayList<>())
                .build();
    }

    private int delaySeconds(LocalDateTime originalStartTime, LocalDateTime taskStartTime) {
        long seconds = Duration.between(originalStartTime, taskStartTime).getSeconds();
        return seconds <= 0 ? 0 : Math.toIntExact(seconds);
    }
}