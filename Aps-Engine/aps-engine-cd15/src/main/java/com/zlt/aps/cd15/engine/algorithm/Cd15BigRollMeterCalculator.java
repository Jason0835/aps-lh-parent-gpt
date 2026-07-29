package com.zlt.aps.cd15.engine.algorithm;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 根据卷曲总长度和施工尺寸计算大卷消耗米数。
 */
@Slf4j
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
     * @param unitConsumeMillimeter 单条钢带长度，单位毫米/条；大卷幅宽缺失时作为有效幅宽
     * @param craftWidthMillimeter 斜裁宽度，单位毫米
     * @param cordWidthMillimeter 大卷幅宽，单位毫米
     * @return GDYY大卷占用量，单位米
     */
    public BigDecimal calculateForPlanQuantity(BigDecimal planQuantity,
                                               BigDecimal unitConsumeMillimeter,
                                               BigDecimal craftWidthMillimeter,
                                               BigDecimal cordWidthMillimeter) {
        return this.calculateForPlanQuantity(
                planQuantity, unitConsumeMillimeter, craftWidthMillimeter,
                cordWidthMillimeter, null, null);
    }

    /**
     * 根据斜裁计划量换算GDYY大卷占用量，并在非法计划量时输出材料上下文。
     *
     * @param planQuantity 斜裁计划量，单位米
     * @param unitConsumeMillimeter 单条钢带长度，单位毫米/条；大卷幅宽缺失时作为有效幅宽
     * @param craftWidthMillimeter 斜裁宽度，单位毫米
     * @param cordWidthMillimeter 大卷幅宽，单位毫米
     * @param steelStripCode 钢带代码
     * @param bigRollCode 大卷编号
     * @return GDYY大卷占用量，单位米
     */
    public BigDecimal calculateForPlanQuantity(BigDecimal planQuantity,
                                               BigDecimal unitConsumeMillimeter,
                                               BigDecimal craftWidthMillimeter,
                                               BigDecimal cordWidthMillimeter,
                                               String steelStripCode,
                                               String bigRollCode) {
        this.requirePositivePlanQuantity(
                planQuantity, unitConsumeMillimeter, craftWidthMillimeter,
                cordWidthMillimeter, steelStripCode, bigRollCode);
        requirePositive(unitConsumeMillimeter, "单耗");
        requirePositive(craftWidthMillimeter, "斜裁宽度");
        BigDecimal effectiveCordWidth = cordWidthMillimeter == null
                || cordWidthMillimeter.signum() <= 0
                ? unitConsumeMillimeter : cordWidthMillimeter;
        return calculate(planQuantity, craftWidthMillimeter, effectiveCordWidth);
    }

    /** 非正计划量同时记录钢带、大卷和施工尺寸，便于定位上游归零原因。 */
    private void requirePositivePlanQuantity(
            BigDecimal planQuantity,
            BigDecimal unitConsumeMillimeter,
            BigDecimal craftWidthMillimeter,
            BigDecimal cordWidthMillimeter,
            String steelStripCode,
            String bigRollCode) {
        if (planQuantity != null && planQuantity.signum() > 0) {
            return;
        }
        log.error("[斜裁大卷消耗] 斜裁计划量非正, steelStripCode={}, bigRollCode={}, "
                        + "planQuantity={}, unitConsumeMillimeter={}, "
                        + "craftWidthMillimeter={}, cordWidthMillimeter={}",
                steelStripCode, bigRollCode, planQuantity,
                unitConsumeMillimeter, craftWidthMillimeter,
                cordWidthMillimeter);
        throw new IllegalArgumentException(
                "斜裁计划量必须大于0, steelStripCode=" + steelStripCode
                        + ", bigRollCode=" + bigRollCode);
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
