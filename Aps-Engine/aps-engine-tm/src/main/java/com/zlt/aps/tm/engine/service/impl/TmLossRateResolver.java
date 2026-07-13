package com.zlt.aps.tm.engine.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.zlt.aps.tm.engine.domain.TmLossRule;
import com.zlt.aps.tm.engine.domain.TmLossRuleMatchResult;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/** 胎面损耗率四层匹配解析器。 */
public class TmLossRateResolver {

    /**
     * 按机台+胎面、胎面、机台、默认的优先级解析损耗率。
     * @param ruleList 损耗规则列表
     * @param treadCode 胎面编码
     * @param machineCode 机台编码
     * @return 命中结果，未配置时返回 null
     */
    public TmLossRuleMatchResult resolve(List<TmLossRule> ruleList, String treadCode, String machineCode) {
        if (CollUtil.isEmpty(ruleList)) {
            return null;
        }
        for (String matchLevel : new String[]{"MACHINE_TREAD", "TREAD", "MACHINE", "DEFAULT"}) {
            TmLossRuleMatchResult result = this.find(ruleList, treadCode, machineCode, matchLevel);
            if (result != null) {
                return result;
            }
        }
        return null;
    }

    private TmLossRuleMatchResult find(List<TmLossRule> ruleList, String treadCode, String machineCode,
                                       String matchLevel) {
        String normalizedTreadCode = normalize(treadCode);
        String normalizedMachineCode = normalize(machineCode);
        TmLossRule matchedRule = ruleList.stream().filter(rule -> {
            String ruleTreadCode = normalize(rule.getTreadCode());
            String ruleMachineCode = normalize(rule.getMachineCode());
            if ("MACHINE_TREAD".equals(matchLevel)) {
                return Objects.equals(ruleTreadCode, normalizedTreadCode)
                        && Objects.equals(ruleMachineCode, normalizedMachineCode);
            }
            if ("TREAD".equals(matchLevel)) {
                return Objects.equals(ruleTreadCode, normalizedTreadCode) && ruleMachineCode == null;
            }
            if ("MACHINE".equals(matchLevel)) {
                return ruleTreadCode == null && Objects.equals(ruleMachineCode, normalizedMachineCode);
            }
            return ruleTreadCode == null && ruleMachineCode == null;
        }).min(Comparator.comparing(rule -> rule.getPriority() == null ? Integer.MAX_VALUE : rule.getPriority()))
                .orElse(null);
        if (matchedRule == null) {
            return null;
        }
        TmLossRuleMatchResult result = new TmLossRuleMatchResult();
        result.setMatchLevel(matchLevel);
        result.setLossRate(matchedRule.getLossRate() == null ? BigDecimal.ZERO : matchedRule.getLossRate());
        result.setMatchedRule(matchedRule);
        return result;
    }

    private String normalize(String value) {
        return StrUtil.emptyToNull(StrUtil.trim(value));
    }
}
