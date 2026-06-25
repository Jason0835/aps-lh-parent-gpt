package com.zlt.aps.cd90.engine.algorithm;

import com.zlt.aps.cd90.engine.model.Cd90AutoScheduleParameters;
import com.zlt.aps.cd90.engine.model.Cd90SpecialBigRollDecision;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

/** 特殊大卷策略入口，隔离“上机即耗尽”等未定业务规则。 */
@Component
public class Cd90SpecialBigRollPolicy {

    /**
     * 根据参数判断当前大卷是否属于上机后按耗尽处理的特殊大卷。
     *
     * @param bigRollCode 大卷代码
     * @param parameters 自动排程参数快照
     * @return 特殊大卷策略决策；未命中时返回空策略，不影响现有排程逻辑
     */
    public Cd90SpecialBigRollDecision decide(String bigRollCode,
                                             Cd90AutoScheduleParameters parameters) {
        if (!StringUtils.hasText(bigRollCode) || parameters == null) {
            return normalDecision();
        }
        String normalized = bigRollCode.trim();
        boolean special = safe(parameters.getSpecialRollUseUpCodes()).stream()
                .filter(StringUtils::hasText)
                .map(String::trim)
                .anyMatch(normalized::equals);
        if (!special) {
            return normalDecision();
        }
        return Cd90SpecialBigRollDecision.builder()
                .special(true)
                .consumeAfterMounted(true)
                .lookaheadShifts(Math.max(0, parameters.getSpecialRollLookaheadShifts()))
                .extraStockLimit(nonNegative(parameters.getSpecialRollExtraStockLimit()))
                .build();
    }

    private Cd90SpecialBigRollDecision normalDecision() {
        return Cd90SpecialBigRollDecision.builder()
                .special(false)
                .consumeAfterMounted(false)
                .lookaheadShifts(0)
                .extraStockLimit(BigDecimal.ZERO)
                .build();
    }

    private BigDecimal nonNegative(BigDecimal value) {
        return value == null || value.signum() < 0 ? BigDecimal.ZERO : value;
    }

    private <T> List<T> safe(List<T> values) {
        return values == null ? Collections.emptyList() : values;
    }
}
