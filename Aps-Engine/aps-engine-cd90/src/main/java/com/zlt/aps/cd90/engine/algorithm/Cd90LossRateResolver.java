package com.zlt.aps.cd90.engine.algorithm;

import com.zlt.aps.cd90.engine.model.Cd90LossRateRule;
import com.zlt.aps.cd90.engine.model.Cd90LossRateSelection;
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
public class Cd90LossRateResolver {

    /**
     * 按“帘布加机台、帘布、机台、通用”优先级取得损耗率。
     *
     * @param clothCode 帘布代码
     * @param machineCode 候选机台代码
     * @param rules 损耗率规则
     * @return 最终损耗率及命中层级
     */
    public Cd90LossRateSelection resolve(String clothCode,
                                         String machineCode,
                                         List<Cd90LossRateRule> rules) {
        return resolve(clothCode, machineCode, rules, null);
    }

    /**
     * 按“帘布加机台、帘布、机台、通用”优先级取得损耗率；四层均未命中时使用参数兜底损耗率。
     *
     * @param clothCode 帘布代码
     * @param machineCode 候选机台代码
     * @param rules 损耗率规则
     * @param fallbackLossRatePercent 参数 SYS0701003 配置的通用损耗率兜底（百分比）
     * @return 最终损耗率及命中层级
     */
    public Cd90LossRateSelection resolve(String clothCode,
                                         String machineCode,
                                         List<Cd90LossRateRule> rules,
                                         BigDecimal fallbackLossRatePercent) {
        Cd90LossRateRule rule = find(rules, item -> same(item.getClothCode(), clothCode)
                && same(item.getMachineCode(), machineCode));
        if (rule != null) {
            return selection(rule, "CLOTH_MACHINE");
        }

        rule = find(rules, item -> same(item.getClothCode(), clothCode)
                && !StringUtils.hasText(item.getMachineCode()));
        if (rule != null) {
            return selection(rule, "CLOTH");
        }

        rule = find(rules, item -> !StringUtils.hasText(item.getClothCode())
                && same(item.getMachineCode(), machineCode));
        if (rule != null) {
            return selection(rule, "MACHINE");
        }

        rule = find(rules, item -> !StringUtils.hasText(item.getClothCode())
                && !StringUtils.hasText(item.getMachineCode()));
        if (rule != null) {
            return selection(rule, "GENERAL");
        }
        if (fallbackLossRatePercent != null && fallbackLossRatePercent.signum() >= 0) {
            return Cd90LossRateSelection.builder()
                    .lossRatePercent(fallbackLossRatePercent)
                    .matchedLevel("FALLBACK")
                    .build();
        }
        throw new IllegalArgumentException("未找到候选机台适用的损耗率");
    }

    private Cd90LossRateRule find(List<Cd90LossRateRule> rules, Predicate<Cd90LossRateRule> predicate) {
        if (rules == null) {
            return null;
        }
        return rules.stream().filter(predicate).findFirst().orElse(null);
    }

    private boolean same(String first, String second) {
        return StringUtils.hasText(first) && Objects.equals(first, second);
    }

    private Cd90LossRateSelection selection(Cd90LossRateRule rule, String level) {
        if (rule.getLossRatePercent() == null || rule.getLossRatePercent().signum() < 0) {
            throw new IllegalArgumentException("损耗率不能小于0");
        }
        return Cd90LossRateSelection.builder()
                .lossRatePercent(rule.getLossRatePercent())
                .matchedLevel(level)
                .build();
    }
}
