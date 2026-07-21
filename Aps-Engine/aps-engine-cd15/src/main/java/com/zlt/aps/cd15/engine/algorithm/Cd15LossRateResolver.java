package com.zlt.aps.cd15.engine.algorithm;

import com.zlt.aps.cd15.engine.model.Cd15LossRateRule;
import com.zlt.aps.cd15.engine.model.Cd15LossRateSelection;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;

/**
 * 候选机台最终损耗率解析器。
 */
@Component
public class Cd15LossRateResolver {

    /**
     * 按“钢带加机台、钢带、机台、通用”优先级取得损耗率。
     *
     * @param steelStripCode 钢带代码
     * @param machineCode 候选机台代码
     * @param rules 损耗率规则
     * @return 最终损耗率及命中层级
     */
    public Cd15LossRateSelection resolve(String steelStripCode,
                                         String machineCode,
                                         List<Cd15LossRateRule> rules) {
        return resolve(steelStripCode, machineCode, rules, null);
    }

    /**
     * 按“钢带加机台、钢带、机台、通用”优先级取得损耗率；四层均未命中时使用参数兜底损耗率。
     *
     * @param steelStripCode 钢带代码
     * @param machineCode 候选机台代码
     * @param rules 损耗率规则
     * @param fallbackLossRatePercent 参数 SYS0601003 配置的通用损耗率兜底（百分比）
     * @return 最终损耗率及命中层级
     */
    public Cd15LossRateSelection resolve(String steelStripCode,
                                         String machineCode,
                                         List<Cd15LossRateRule> rules,
                                         BigDecimal fallbackLossRatePercent) {
        Cd15LossRateRule rule = find(rules, item -> same(item.getSteelStripCode(), steelStripCode)
                && same(item.getMachineCode(), machineCode));
        if (rule != null) {
            return selection(rule, "STEEL_STRIP_MACHINE");
        }

        rule = find(rules, item -> same(item.getSteelStripCode(), steelStripCode)
                && !StringUtils.hasText(item.getMachineCode()));
        if (rule != null) {
            return selection(rule, "STEEL_STRIP");
        }

        rule = find(rules, item -> !StringUtils.hasText(item.getSteelStripCode())
                && same(item.getMachineCode(), machineCode));
        if (rule != null) {
            return selection(rule, "MACHINE");
        }

        rule = find(rules, item -> !StringUtils.hasText(item.getSteelStripCode())
                && !StringUtils.hasText(item.getMachineCode()));
        if (rule != null) {
            return selection(rule, "GENERAL");
        }
        if (fallbackLossRatePercent != null && fallbackLossRatePercent.signum() >= 0) {
            return Cd15LossRateSelection.builder()
                    .lossRatePercent(fallbackLossRatePercent)
                    .matchedLevel("FALLBACK")
                    .build();
        }
        throw new IllegalArgumentException("未找到候选机台适用的损耗率");
    }

    private Cd15LossRateRule find(List<Cd15LossRateRule> rules, Predicate<Cd15LossRateRule> predicate) {
        if (rules == null) {
            return null;
        }
        return rules.stream().filter(predicate).findFirst().orElse(null);
    }

    private boolean same(String first, String second) {
        return StringUtils.hasText(first) && Objects.equals(first, second);
    }

    private Cd15LossRateSelection selection(Cd15LossRateRule rule, String level) {
        if (rule.getLossRatePercent() == null || rule.getLossRatePercent().signum() < 0) {
            throw new IllegalArgumentException("损耗率不能小于0");
        }
        return Cd15LossRateSelection.builder()
                .lossRatePercent(rule.getLossRatePercent())
                .matchedLevel(level)
                .build();
    }
}
