package com.zlt.aps.tm.engine.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.zlt.aps.tm.api.enums.TmLossMatchLevelEnum;
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
        for (TmLossMatchLevelEnum matchLevel : new TmLossMatchLevelEnum[]{
                TmLossMatchLevelEnum.MACHINE_TREAD,
                TmLossMatchLevelEnum.TREAD,
                TmLossMatchLevelEnum.MACHINE,
                TmLossMatchLevelEnum.DEFAULT}) {
            TmLossRuleMatchResult result = this.find(ruleList, treadCode, machineCode, matchLevel);
            if (result != null) {
                return result;
            }
        }
        return null;
    }

    private TmLossRuleMatchResult find(List<TmLossRule> ruleList, String treadCode, String machineCode,
                                       TmLossMatchLevelEnum matchLevel) {
        String normalizedTreadCode = normalize(treadCode);
        String normalizedMachineCode = normalize(machineCode);
        TmLossRule matchedRule = ruleList.stream().filter(rule -> {
            String ruleTreadCode = normalize(rule.getTreadCode());
            String ruleMachineCode = normalize(rule.getMachineCode());
            if (TmLossMatchLevelEnum.MACHINE_TREAD == matchLevel) {
                return Objects.equals(ruleTreadCode, normalizedTreadCode)
                        && Objects.equals(ruleMachineCode, normalizedMachineCode);
            }
            if (TmLossMatchLevelEnum.TREAD == matchLevel) {
                return Objects.equals(ruleTreadCode, normalizedTreadCode) && ruleMachineCode == null;
            }
            if (TmLossMatchLevelEnum.MACHINE == matchLevel) {
                return ruleTreadCode == null && Objects.equals(ruleMachineCode, normalizedMachineCode);
            }
            return ruleTreadCode == null && ruleMachineCode == null;
        }).min(Comparator.comparing(rule -> rule.getPriority() == null ? Integer.MAX_VALUE : rule.getPriority()))
                .orElse(null);
        if (matchedRule == null) {
            return null;
        }
        TmLossRuleMatchResult result = new TmLossRuleMatchResult();
        result.setMatchLevel(matchLevel.getCode());
        result.setLossRate(matchedRule.getLossRate() == null ? BigDecimal.ZERO : matchedRule.getLossRate());
        result.setMatchedRule(matchedRule);
        return result;
    }

    private String normalize(String value) {
        return StrUtil.emptyToNull(StrUtil.trim(value));
    }
}
