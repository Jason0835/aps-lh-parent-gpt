package com.zlt.aps.tm.engine.strategy;

import com.ruoyi.common.exception.ServiceException;
import com.zlt.aps.common.engine.schedule.ScheduleRuleResult;
import com.zlt.aps.tm.api.enums.TmScheduleErrorCodeEnum;
import com.zlt.aps.tm.engine.domain.TmMachineCandidate;
import com.zlt.aps.tm.engine.domain.TmMachineRuleContext;
import com.zlt.aps.tm.engine.domain.TmTaskDraft;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 胎面默认机台过滤规则链。
 *
 * <p>按启用、剩余产能、口型板、胶料机台关系、定点生产、定点不可生产顺序执行，
 * 任一否决即过滤。方法会修改候选机台的过滤状态和证据，不修改任务链。
 * 通过 {@link Component} 注册为 Spring Bean，由 {@link TmStrategyRegistry} 按编码 "DEFAULT" 收集。</p>
 */
@Component
public class TmDefaultMachineFilterRule implements ITmMachineFilterRule {

    /**
     * 获取规则编码。
     *
     * @return 规则编码
     */
    @Override
    public String getRuleCode() {
        return "DEFAULT";
    }

    /**
     * 执行机台过滤规则。
     *
     * @param candidate 候选机台
     * @param context   机台规则上下文
     * @return 规则执行结果，passed=true 表示通过过滤
     * @throws ServiceException 候选机台或上下文为空时抛出
     */
    @Override
    public ScheduleRuleResult evaluate(TmMachineCandidate candidate, TmMachineRuleContext context) {
        if (candidate == null || context == null) {
            throw new ServiceException(TmScheduleErrorCodeEnum.TM_MACHINE_CANDIDATE_EMPTY.getDefaultMessage());
        }
        TmTaskDraft task = context.getTaskDraft();
        // 1. 机台状态启用
        if (Boolean.FALSE.equals(candidate.getEnabled())) {
            return reject(candidate, "MACHINE_DISABLED", "机台未启用");
        }
        // 2. 剩余产能大于 0
        if (candidate.getRemainCapacity() == null
                || candidate.getRemainCapacity().compareTo(BigDecimal.ZERO) <= 0) {
            return reject(candidate, "NO_REMAIN_CAPACITY", "机台剩余产能不足");
        }
        // 3. 口型板匹配
        if (Boolean.FALSE.equals(candidate.getMouthPlateMatched())) {
            return reject(candidate, "MOUTH_PLATE_NOT_MATCH", "口型板不匹配");
        }
        // 4. 胶料机台关系
        if (Boolean.FALSE.equals(candidate.getGlueMachineMatched())) {
            return reject(candidate, "GLUE_MACHINE_NOT_MATCH", "胶料机台关系不匹配");
        }
        // 5. 选择定点生产机台
        if (Boolean.FALSE.equals(candidate.getFixedMachineSelected())) {
            return reject(candidate, "FIXED_MACHINE_NOT_SELECTED", "未命中选择定点生产机台");
        }
        // 6. 排除定点不可生产机台
        if (Boolean.TRUE.equals(candidate.getFixedMachineExcluded())) {
            return reject(candidate, "FIXED_MACHINE_EXCLUDED", "命中定点不可生产机台");
        }
        candidate.setFiltered(Boolean.FALSE);
        Map<String, Object> evidence = new LinkedHashMap<>();
        evidence.put("task", task == null ? null : task.getBusinessKey());
        evidence.put("machineCode", candidate.getMachineCode());
        candidate.getEvidence().putAll(evidence);
        return ScheduleRuleResult.pass("DEFAULT_PASS", "通过默认过滤规则", evidence);
    }

    /**
     * 标记候选机台被过滤并返回规则结果。
     *
     * @param candidate  候选机台
     * @param reasonCode 原因编码
     * @param reasonDesc 原因描述
     * @return 未通过过滤的规则结果
     */
    private ScheduleRuleResult reject(TmMachineCandidate candidate, String reasonCode, String reasonDesc) {
        candidate.setFiltered(Boolean.TRUE);
        candidate.setFilterReasonCode(reasonCode);
        candidate.setFilterReasonDesc(reasonDesc);
        candidate.getEvidence().put("ruleCode", reasonCode);
        candidate.getEvidence().put("reasonDesc", reasonDesc);
        return ScheduleRuleResult.reject(reasonCode, reasonDesc, candidate.getEvidence());
    }
}
