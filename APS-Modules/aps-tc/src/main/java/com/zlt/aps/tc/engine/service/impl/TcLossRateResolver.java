package com.zlt.aps.tc.engine.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.zlt.aps.tc.api.enums.TcLossMatchLevelEnum;
import com.zlt.aps.tc.engine.domain.TcLossRule;
import com.zlt.aps.tc.engine.domain.TcLossRuleMatchResult;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/** 胎侧损耗率四层匹配解析器。 */
public class TcLossRateResolver {

    /**
     * 按机台+胎侧、胎侧、机台、默认的优先级解析损耗率。
     * @param ruleList 损耗规则列表
     * @param sidewallCode 胎侧编码
     * @param machineCode 机台编码
     * @return 命中结果，未配置时返回 null
     */
    public TcLossRuleMatchResult resolve(List<TcLossRule> ruleList, String sidewallCode, String machineCode) {
        if (CollUtil.isEmpty(ruleList)) {
            return null;
        }
        for (TcLossMatchLevelEnum matchLevel : TcLossMatchLevelEnum.values()) {
            TcLossRuleMatchResult result = this.find(ruleList, sidewallCode, machineCode, matchLevel);
            if (result != null) {
                return result;
            }
        }
        return null;
    }

    private TcLossRuleMatchResult find(List<TcLossRule> ruleList, String sidewallCode, String machineCode,
                                       TcLossMatchLevelEnum matchLevel) {
        String normalizedSidewallCode = normalize(sidewallCode);
        String normalizedMachineCode = normalize(machineCode);
        TcLossRule matchedRule = ruleList.stream().filter(rule -> {
            String ruleSidewallCode = normalize(rule.getSidewallCode());
            String ruleMachineCode = normalize(rule.getMachineCode());
            if (TcLossMatchLevelEnum.MACHINE_SIDEWALL == matchLevel) {
                return Objects.equals(ruleSidewallCode, normalizedSidewallCode)
                        && Objects.equals(ruleMachineCode, normalizedMachineCode);
            }
            if (TcLossMatchLevelEnum.SIDEWALL == matchLevel) {
                return Objects.equals(ruleSidewallCode, normalizedSidewallCode) && ruleMachineCode == null;
            }
            if (TcLossMatchLevelEnum.MACHINE == matchLevel) {
                return ruleSidewallCode == null && Objects.equals(ruleMachineCode, normalizedMachineCode);
            }
            return ruleSidewallCode == null && ruleMachineCode == null;
        }).min(Comparator.comparing(rule -> rule.getPriority() == null ? Integer.MAX_VALUE : rule.getPriority()))
                .orElse(null);
        if (matchedRule == null) {
            return null;
        }
        TcLossRuleMatchResult result = new TcLossRuleMatchResult();
        result.setMatchLevel(matchLevel.getCode());
        result.setLossRate(matchedRule.getLossRate() == null ? BigDecimal.ZERO : matchedRule.getLossRate());
        result.setMatchedRule(matchedRule);
        return result;
    }

    private String normalize(String value) {
        return StrUtil.emptyToNull(StrUtil.trim(value));
    }
}
