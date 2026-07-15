package com.zlt.aps.cd15.engine.algorithm;

import com.zlt.aps.cd15.api.domain.entity.Cd15Stock;
import com.zlt.aps.cd15.api.domain.entity.Cd15StorageLaneLimit;
import com.zlt.aps.cd15.engine.model.Cd15AutoScheduleInput;
import com.zlt.aps.cd15.engine.model.Cd15BigRollAgingBuildResult;
import com.zlt.aps.cd15.engine.model.Cd15BigRollAgingStock;
import com.zlt.aps.cd15.engine.model.Cd15RollingResourceSnapshot;
import com.zlt.aps.cd15.engine.model.Cd15StorageLaneState;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * CD15 逐班滚动资源快照构建器。
 */
@Component
@RequiredArgsConstructor
public class Cd15ResourceSnapshotBuilder {

    private final Cd15BigRollAgingStockBuilder bigRollAgingStockBuilder;

    /**
     * 从自动排程输入构建可扣减资源快照。
     *
     * @param input 自动排程输入
     * @return 滚动资源快照
     */
    public Cd15RollingResourceSnapshot build(Cd15AutoScheduleInput input) {
        List<Cd15Stock> cd15Stocks = input == null || input.getStocksAtSix() == null
                ? Collections.emptyList() : input.getStocksAtSix();
        Map<String, BigDecimal> stockMetersBySteelStrip = cd15Stocks.stream()
                .filter(item -> item != null && StringUtils.hasText(item.getMaterialCode()))
                .collect(Collectors.toMap(item -> this.materialCode(item),
                        this::effectiveStockMeters, BigDecimal::add, LinkedHashMap::new));

        Cd15BigRollAgingBuildResult agingBuildResult = bigRollAgingStockBuilder.build(
                input == null ? Collections.emptyList() : input.getGdyyStocks(),
                input == null ? Collections.emptyList() : input.getGdyyPlans(),
                input == null ? 0 : input.getAgingPeriodHours());
        Map<String, List<Cd15BigRollAgingStock>> gdyyAgingStocksByBigRoll = agingBuildResult.getStocks().stream()
                .filter(item -> item != null && StringUtils.hasText(item.getBigRollCode()))
                .collect(Collectors.groupingBy(item -> item.getBigRollCode().trim(),
                        LinkedHashMap::new, Collectors.toList()));
        List<Cd15StorageLaneLimit> storageLanesAtSix = input == null || input.getStorageLanesAtSix() == null
                ? Collections.emptyList() : input.getStorageLanesAtSix();
        List<Cd15StorageLaneState> storageLanes = storageLanesAtSix.stream()
                .filter(item -> item != null && StringUtils.hasText(item.getStorageLaneCode()))
                .map(this::mapStorageLane)
                .collect(Collectors.toList());
        return Cd15RollingResourceSnapshot.builder()
                .stockMetersBySteelStrip(stockMetersBySteelStrip)
                .gdyyAgingStocksByBigRoll(gdyyAgingStocksByBigRoll)
                .storageLanes(storageLanes)
                .dataMissingBigRollCodes(new ArrayList<>(agingBuildResult.getDataMissingBigRollCodes()))
                .build();
    }

    public Cd15StorageLaneState mapStorageLane(Cd15StorageLaneLimit source) {
        int vehicleCount = source.getCarNum() == null ? 0 : source.getCarNum();
        Integer maxCarNum = source.getMaxCarNum();
        if (maxCarNum == null || maxCarNum <= 0) {
            throw new IllegalArgumentException("库排 " + source.getStorageLaneCode() + " 未维护有效最大车数");
        }
        return Cd15StorageLaneState.builder()
                .laneCode(source.getStorageLaneCode())
                .steelStripCode(source.getMaterialCode())
                .vehicleCount(vehicleCount)
                .maxVehicleCount(maxCarNum)
                .build();
    }
    private String materialCode(Cd15Stock stock) {
        return stock.getMaterialCode().trim();
    }

    private BigDecimal effectiveStockMeters(Cd15Stock stock) {
        return this.value(stock.getStockNum())
                .add(this.value(stock.getModifyNum()))
                .subtract(this.value(stock.getBadNum()))
                .max(BigDecimal.ZERO);
    }

    private BigDecimal value(Double value) {
        return value == null ? BigDecimal.ZERO : BigDecimal.valueOf(value);
    }
}