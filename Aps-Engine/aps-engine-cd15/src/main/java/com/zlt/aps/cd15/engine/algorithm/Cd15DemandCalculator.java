package com.zlt.aps.cd15.engine.algorithm;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * CD15 单班自然需求换算。
 */
@Component
public class Cd15DemandCalculator {

    private static final BigDecimal MILLIMETER_PER_METER = BigDecimal.valueOf(1000L);

    /**
     * 片数 = 胎胚自然需求 * 单耗(mm/条) / 1000 / 卷曲长度(m)，向上取整。
     */
    public BigDecimal calculatePieceCount(BigDecimal naturalDemandQty,
                                          BigDecimal unitConsumeMillimeter,
                                          BigDecimal curlLength) {
        if (!this.isPositive(naturalDemandQty) || !this.isPositive(unitConsumeMillimeter)
                || !this.isPositive(curlLength)) {
            return BigDecimal.ZERO;
        }
        BigDecimal consumeMeters = naturalDemandQty.multiply(unitConsumeMillimeter)
                .divide(MILLIMETER_PER_METER, 8, RoundingMode.HALF_UP);
        return consumeMeters.divide(curlLength, 0, RoundingMode.CEILING);
    }

    /**
     * 毛需求米数 = 片数 * 斜裁有效宽度(mm) / 1000。
     */
    public BigDecimal calculateRawDemandMeters(BigDecimal pieceCount, BigDecimal craftWidthMillimeter) {
        if (!this.isPositive(pieceCount) || !this.isPositive(craftWidthMillimeter)) {
            return BigDecimal.ZERO;
        }
        return pieceCount.multiply(craftWidthMillimeter).divide(MILLIMETER_PER_METER, 4, RoundingMode.HALF_UP);
    }

    /**
     * 净需求米数 = 毛需求米数 - 6点库存米数，最低为0。
     */
    public BigDecimal calculateNetDemandMeters(BigDecimal pieceCount,
                                               BigDecimal craftWidthMillimeter,
                                               BigDecimal stockMetersAtSix) {
        BigDecimal rawDemand = this.calculateRawDemandMeters(pieceCount, craftWidthMillimeter);
        BigDecimal stock = stockMetersAtSix == null ? BigDecimal.ZERO : stockMetersAtSix;
        BigDecimal netDemand = rawDemand.subtract(stock);
        return netDemand.signum() < 0 ? BigDecimal.ZERO : netDemand;
    }

    /**
     * 含损耗需求 = 净需求 * (1 + 损耗率 / 100)。
     */
    public BigDecimal calculateDemandWithLoss(BigDecimal netDemandMeters,
                                              BigDecimal lossRatePercent) {
        BigDecimal netDemand = netDemandMeters == null ? BigDecimal.ZERO : netDemandMeters;
        if (netDemand.signum() <= 0) {
            return BigDecimal.ZERO;
        }
        if (lossRatePercent == null || lossRatePercent.signum() < 0) {
            throw new IllegalArgumentException("损耗率不能为空或小于0");
        }
        return netDemand.multiply(BigDecimal.ONE.add(lossRatePercent
                .divide(BigDecimal.valueOf(100), 8, RoundingMode.HALF_UP)))
                .setScale(4, RoundingMode.HALF_UP);
    }
    private boolean isPositive(BigDecimal value) {
        return value != null && value.signum() > 0;
    }
}