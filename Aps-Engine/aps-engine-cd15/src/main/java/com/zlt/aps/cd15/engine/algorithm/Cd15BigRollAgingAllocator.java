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
 * 大卷静置成熟流水分配器。
 *
 * <p>大卷钢带入库后需经过一定时间的自然静置（成熟）方可上机使用。
 * 每卷大卷按入库时间依次释放（releaseTime），分配器按释放时间顺序逐卷分配用量，
 * 若最晚释放时间晚于机台原开工时间，则产生延迟。</p>
 */
@Component
@Slf4j
public class Cd15BigRollAgingAllocator {

    /** 大卷时效不足限制标识 */
    public static final String AGING_PERIOD_LIMIT = "AGING_PERIOD_LIMIT";

    /**
     * 试算本次任务需要等待到哪个时间开裁，不修改库存流水占用量。
     *
     * @param stocks 大卷成熟流水
     * @param bigRollCode 帘线大卷编码
     * @param requestedQuantity 本次任务米数
     * @param originalStartTime 机台原预计可上机时间
     * @return 分配试算结果；库存不足时返回当前最大可供量，完全无可用流水时返回失败
     */
    public Cd15BigRollAgingAllocation preview(List<Cd15BigRollAgingStock> stocks, String bigRollCode,
            BigDecimal requestedQuantity, LocalDateTime originalStartTime) {
        BigDecimal requestQty = requestedQuantity == null ? BigDecimal.ZERO : requestedQuantity;
        // 需求量为零或无原始开工时间时直接返回失败
        if (requestQty.compareTo(BigDecimal.ZERO) <= 0 || originalStartTime == null) {
            return failure(requestQty, originalStartTime);
        }

        List<Cd15BigRollAgingAllocationItem> items = new ArrayList<>();
        BigDecimal remaining = requestQty;
        LocalDateTime latestReleaseTime = originalStartTime;
        // 按释放时间升序遍历可用大卷，逐卷分配直至满足需求
        for (Cd15BigRollAgingStock stock : sortedAvailableStocks(stocks, bigRollCode)) {
            BigDecimal stockRemaining = stock.getRemainingQuantity();
            if (stockRemaining.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            BigDecimal used = stockRemaining.min(remaining);
            items.add(Cd15BigRollAgingAllocationItem.builder().stock(stock).quantity(used).build());
            remaining = remaining.subtract(used);
            // 记录已分配大卷中最晚的释放时间，决定任务实际可开工时间
            if (stock.getReleaseTime().isAfter(latestReleaseTime)) {
                latestReleaseTime = stock.getReleaseTime();
            }
            // 需求已满足：以最晚释放时间与原始开工时间中较晚者作为实际开工时间
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
                        .delaySeconds(delaySeconds(originalStartTime, taskStartTime))
                        .items(items)
                        .build();
            }
        }
        // 遍历完仍未满足需求时，将已选流水作为部分可供量返回。
        // 试算只提供上限且不扣减库存；正式提交仍要求完整覆盖最终提交量。
        BigDecimal totalAvailable = requestQty.subtract(remaining);
        log.warn("[斜裁自动排程] 大卷静置库存不足, bigRollCode={}, requestedQuantity={}, "
                        + "totalAvailableInStocks={}, shortfall={}, stockDetails={}",
                bigRollCode, requestQty, totalAvailable, remaining,
                stocks == null ? "[]" : stocks.stream()
                        .filter(s -> s != null && bigRollCode.equals(s.getBigRollCode()))
                        .map(s -> String.format("{source=%s, available=%s, allocated=%s, remaining=%s, releaseTime=%s}",
                                s.getSourceId(), s.getAvailableQuantity(), s.getAllocatedQuantity(),
                                s.getRemainingQuantity(), s.getReleaseTime()))
                        .collect(java.util.stream.Collectors.joining(", ")));
        if (totalAvailable.compareTo(BigDecimal.ZERO) <= 0) {
            return failure(requestQty, originalStartTime);
        }
        LocalDateTime taskStartTime = latestReleaseTime.isAfter(originalStartTime)
                ? latestReleaseTime : originalStartTime;
        return Cd15BigRollAgingAllocation.builder()
                .success(true)
                .requestedQuantity(requestQty)
                .allocatedQuantity(totalAvailable)
                .originalStartTime(originalStartTime)
                .taskStartTime(taskStartTime)
                .latestReleaseTime(latestReleaseTime)
                .delaySeconds(delaySeconds(originalStartTime, taskStartTime))
                .items(items)
                .build();
    }

    /**
     * 分配并立即扣减占用量，用于正式提交排产位后的资源快照更新。
     */
    public Cd15BigRollAgingAllocation allocate(List<Cd15BigRollAgingStock> stocks, String bigRollCode,
            BigDecimal requestedQuantity, LocalDateTime originalStartTime) {
        // 先试算确定分配方案，再提交扣减
        Cd15BigRollAgingAllocation allocation = preview(stocks, bigRollCode, requestedQuantity, originalStartTime);
        BigDecimal requestQty = requestedQuantity == null ? BigDecimal.ZERO : requestedQuantity;
        if (!allocation.isSuccess() || allocation.getAllocatedQuantity().compareTo(requestQty) < 0) {
            // 正式提交禁止部分扣减，资源副本不足时返回失败且不修改库存。
            return failure(requestQty, originalStartTime);
        }
        commit(allocation);
        return allocation;
    }

    /**
     * 将试算结果提交到当前滚动资源快照，扣减各卷已分配量。
     */
    public void commit(Cd15BigRollAgingAllocation allocation) {
        if (allocation == null || !allocation.isSuccess() || allocation.getItems() == null) {
            return;
        }
        allocation.getItems().stream()
                .filter(item -> item.getStock() != null && item.getQuantity() != null)
                .forEach(item -> item.getStock().addAllocatedQuantity(item.getQuantity()));
    }

    /**
     * 筛选指定钢带规格下所有可用的库存流水，按释放时间升序排列。
     * 释放时间相同时按入库时间排序，仍相同按来源ID排序保证确定性。
     */
    private List<Cd15BigRollAgingStock> sortedAvailableStocks(List<Cd15BigRollAgingStock> stocks, String bigRollCode) {
        if (stocks == null || stocks.isEmpty()) {
            return new ArrayList<>();
        }
        return stocks.stream()
                .filter(stock -> sameBigRoll(stock, bigRollCode))
                .filter(stock -> stock.getReleaseTime() != null)
                .filter(stock -> stock.getRemainingQuantity().compareTo(BigDecimal.ZERO) > 0)
                .sorted(Comparator.comparing(Cd15BigRollAgingStock::getReleaseTime)
                        .thenComparing(stock -> stock.getStockInTime() == null
                                ? LocalDateTime.MAX : stock.getStockInTime())
                        .thenComparing(stock -> Objects.toString(stock.getSourceId(), "")))
                .collect(Collectors.toList());
    }

    /** 判断库存流水是否属于指定钢带规格 */
    private boolean sameBigRoll(Cd15BigRollAgingStock stock, String bigRollCode) {
        return stock != null && StringUtils.hasText(stock.getBigRollCode())
                && stock.getBigRollCode().equals(bigRollCode);
    }

    /** 构建分配失败结果：已分配量为零，延迟秒数为零 */
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

    /** 计算实际开工时间相对于原始开工时间的延迟秒数 */
    private int delaySeconds(LocalDateTime originalStartTime, LocalDateTime taskStartTime) {
        long seconds = Duration.between(originalStartTime, taskStartTime).getSeconds();
        return seconds <= 0 ? 0 : Math.toIntExact(seconds);
    }
}
