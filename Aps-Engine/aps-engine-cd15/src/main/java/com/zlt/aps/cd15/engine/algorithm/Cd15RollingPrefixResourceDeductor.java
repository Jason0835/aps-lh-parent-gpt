package com.zlt.aps.cd15.engine.algorithm;

import com.zlt.aps.cd15.engine.model.Cd15AutoScheduleInput;
import com.zlt.aps.cd15.engine.model.Cd15BigRollAgingStock;
import com.zlt.aps.cd15.engine.model.Cd15RollingPrefixResourceUsage;
import com.zlt.aps.cd15.engine.model.Cd15RollingResourceSnapshot;
import com.zlt.aps.cd15.engine.model.Cd15StorageLaneState;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** 定时滚动重排前扣减目标班次之前已保留结果占用的资源。 */
@Component
public class Cd15RollingPrefixResourceDeductor {

    public void deductPrefixResources(Cd15AutoScheduleInput input, Cd15RollingResourceSnapshot snapshot) {
        if (input == null || snapshot == null || input.getPrefixResourceUsages() == null) {
            return;
        }
        input.getPrefixResourceUsages().stream()
                .filter(Objects::nonNull)
                .forEach(usage -> {
                    this.deductSteelStrip(snapshot, usage);
                    this.deductBigRoll(snapshot, usage);
                    this.occupyStorageLane(snapshot, usage);
                });
    }

    private void deductSteelStrip(Cd15RollingResourceSnapshot snapshot, Cd15RollingPrefixResourceUsage usage) {
        if (!StringUtils.hasText(usage.getSteelStripCode()) || !this.positive(usage.getSteelStripConsumeMeters())
                || snapshot.getStockMetersBySteelStrip() == null) {
            return;
        }
        snapshot.getStockMetersBySteelStrip().compute(usage.getSteelStripCode().trim(),
                (key, oldValue) -> this.value(oldValue).subtract(usage.getSteelStripConsumeMeters()).max(BigDecimal.ZERO));
    }

    private void deductBigRoll(Cd15RollingResourceSnapshot snapshot, Cd15RollingPrefixResourceUsage usage) {
        if (!StringUtils.hasText(usage.getBigRollCode()) || !this.positive(usage.getBigRollConsumeMeters())) {
            return;
        }
        Map<String, List<Cd15BigRollAgingStock>> stocksByBigRoll = snapshot.getGdyyAgingStocksByBigRoll() == null
                ? Collections.emptyMap() : snapshot.getGdyyAgingStocksByBigRoll();
        List<Cd15BigRollAgingStock> stocks = stocksByBigRoll.getOrDefault(usage.getBigRollCode().trim(), Collections.emptyList());
        BigDecimal[] remaining = new BigDecimal[] { usage.getBigRollConsumeMeters() };
        stocks.stream()
                .filter(Objects::nonNull)
                .filter(stock -> stock.getRemainingQuantity().compareTo(BigDecimal.ZERO) > 0)
                .sorted(Comparator.comparing((Cd15BigRollAgingStock stock) -> this.time(stock.getReleaseTime()))
                        .thenComparing(stock -> this.time(stock.getStockInTime()))
                        .thenComparing(stock -> Objects.toString(stock.getSourceId(), "")))
                .forEach(stock -> {
                    if (remaining[0].compareTo(BigDecimal.ZERO) > 0) {
                        BigDecimal used = stock.getRemainingQuantity().min(remaining[0]);
                        stock.addAllocatedQuantity(used);
                        remaining[0] = remaining[0].subtract(used);
                    }
                });
    }

    private void occupyStorageLane(Cd15RollingResourceSnapshot snapshot, Cd15RollingPrefixResourceUsage usage) {
        if (!StringUtils.hasText(usage.getStorageLaneCode()) || usage.getAllocatedCartCount() == null
                || usage.getAllocatedCartCount() <= 0 || snapshot.getStorageLanes() == null) {
            return;
        }
        snapshot.getStorageLanes().stream()
                .filter(Objects::nonNull)
                .filter(lane -> usage.getStorageLaneCode().trim().equals(lane.getLaneCode()))
                .findFirst()
                .ifPresent(lane -> {
                    if (StringUtils.hasText(usage.getSteelStripCode()) && !StringUtils.hasText(lane.getSteelStripCode())) {
                        lane.setSteelStripCode(usage.getSteelStripCode().trim());
                    }
                    lane.setVehicleCount(Math.min(lane.getMaxVehicleCount(),
                            lane.getVehicleCount() + usage.getAllocatedCartCount()));
                });
    }

    private boolean positive(BigDecimal value) {
        return value != null && value.signum() > 0;
    }

    private BigDecimal value(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private LocalDateTime time(LocalDateTime value) {
        return value == null ? LocalDateTime.MAX : value;
    }
}