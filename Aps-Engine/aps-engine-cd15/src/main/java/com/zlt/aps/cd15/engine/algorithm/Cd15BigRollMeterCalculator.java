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
     * 校验计算尺寸必须存在且大于0。
     */
    private void requirePositive(BigDecimal value, String fieldName) {
        if (value == null || value.signum() <= 0) {
            throw new IllegalArgumentException(fieldName + "必须大于0");
        }
    }
}
