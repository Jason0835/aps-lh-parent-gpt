package com.zlt.aps.cd15.engine.algorithm;

import com.zlt.aps.cd15.api.domain.entity.Cd15Stock;
import com.zlt.aps.cd15.engine.model.Cd15AutoScheduleInput;
import com.zlt.aps.cd15.engine.model.Cd15RollingResourceSnapshot;
import com.zlt.aps.gdyy.api.domain.entity.GdyyStock;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * CD15 逐班滚动资源快照构建器。
 */
@Component
public class Cd15ResourceSnapshotBuilder {

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

        List<GdyyStock> gdyyStocks = input == null || input.getGdyyStocks() == null
                ? Collections.emptyList() : input.getGdyyStocks();
        Map<String, List<GdyyStock>> gdyyStocksByBigRoll = gdyyStocks.stream()
                .filter(item -> item != null && StringUtils.hasText(item.getBigRollCode()))
                .collect(Collectors.groupingBy(item -> item.getBigRollCode().trim(),
                        LinkedHashMap::new, Collectors.toList()));
        return Cd15RollingResourceSnapshot.builder()
                .stockMetersBySteelStrip(stockMetersBySteelStrip)
                .gdyyStocksByBigRoll(gdyyStocksByBigRoll)
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