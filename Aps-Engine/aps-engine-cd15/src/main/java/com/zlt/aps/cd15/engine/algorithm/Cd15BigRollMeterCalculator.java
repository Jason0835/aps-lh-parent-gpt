package com.zlt.aps.cd15.engine.algorithm;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 根据卷曲总长度和施工尺寸计算大卷消耗米数。
 */
@Component
public class Cd15BigRollMeterCalculator {

    /**
     * 计算大卷消耗米数。
     *
     * @param curlLength 卷曲总长度，即N个单片斜裁长度的累计值
     * @param craftWidth 当前层位单片斜裁宽度
     * @param cordWidth 大卷幅宽，同时作为单片斜裁长度
     * @return 大卷消耗米数
     */
    public BigDecimal calculate(BigDecimal curlLength,
                                BigDecimal craftWidth,
                                BigDecimal cordWidth) {
        requirePositive(curlLength, "卷曲总长度");
        requirePositive(craftWidth, "斜裁宽度");
        requirePositive(cordWidth, "大卷幅宽");
        BigDecimal result = curlLength.multiply(craftWidth)
                .divide(cordWidth, 10, RoundingMode.HALF_UP)
                .stripTrailingZeros();
        return result.scale() < 0 ? result.setScale(0) : result;
    }

    /**
     * 根据斜裁计划量换算GDYY大卷占用量。
     *
     * @param planQuantity 斜裁计划量，单位米
     * @param unitConsumeMillimeter 单耗，单位毫米/条
     * @param craftWidthMillimeter 斜裁宽度，单位毫米
     * @param cordWidthMillimeter 大卷幅宽，单位毫米
     * @return GDYY大卷占用量，单位米
     */
    public BigDecimal calculateForPlanQuantity(BigDecimal planQuantity,
                                               BigDecimal unitConsumeMillimeter,
                                               BigDecimal craftWidthMillimeter,
                                               BigDecimal cordWidthMillimeter) {
        requirePositive(planQuantity, "斜裁计划量");
        requirePositive(unitConsumeMillimeter, "单耗");
        requirePositive(craftWidthMillimeter, "斜裁宽度");
        if (cordWidthMillimeter == null || cordWidthMillimeter.signum() <= 0) {
            return normalize(planQuantity);
        }
        BigDecimal totalCutLength = planQuantity.multiply(unitConsumeMillimeter)
                .divide(craftWidthMillimeter, 10, RoundingMode.HALF_UP);
        return calculate(totalCutLength, craftWidthMillimeter, cordWidthMillimeter);
    }

    private BigDecimal normalize(BigDecimal value) {
        BigDecimal normalized = value.stripTrailingZeros();
        return normalized.scale() < 0 ? normalized.setScale(0) : normalized;
    }

    /**
     * 校验计算尺寸必须存在且大于0。
     */
    private void requirePositive(BigDecimal value, String fieldName) {
        if (value == null || value.signum() <= 0) {
            throw new IllegalArgumentException(fieldName + "必须大于0");
        }
    }
}
