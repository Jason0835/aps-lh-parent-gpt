package com.zlt.aps.tm.engine.strategy;

import com.zlt.aps.tm.engine.domain.TmMachineCandidate;
import com.zlt.aps.tm.engine.domain.TmTaskDraft;

import java.math.BigDecimal;

/**
 * 胎面默认机台过滤规则链。
 *
 * <p>按启用、剩余产能、口型板、胶料机台关系、定点生产、定点不可生产顺序执行，
 * 任一否决即过滤。方法会修改候选机台的过滤状态和证据，不修改任务链。</p>
 */
public class TmDefaultMachineFilterRule {

    /**
     * 执行机台过滤。
     *
     * @param task      胎面任务草稿
     * @param candidate 候选机台
     * @return true 表示通过过滤，false 表示被过滤
     * @throws IllegalArgumentException 任务或候选机台为空时抛出
     */
    public boolean filter(TmTaskDraft task, TmMachineCandidate candidate) {
        if (task == null || candidate == null) {
            throw new IllegalArgumentException("机台过滤入参不能为空");
        }
        if (Boolean.FALSE.equals(candidate.getEnabled())) {
            return reject(candidate, "MACHINE_DISABLED", "机台未启用");
        }
        if (candidate.getRemainCapacity() == null || candidate.getRemainCapacity().compareTo(BigDecimal.ZERO) <= 0) {
            return reject(candidate, "NO_REMAIN_CAPACITY", "机台剩余产能不足");
        }
        if (Boolean.FALSE.equals(candidate.getMouthPlateMatched())) {
            return reject(candidate, "MOUTH_PLATE_NOT_MATCH", "口型板不匹配");
        }
        if (Boolean.FALSE.equals(candidate.getGlueMachineMatched())) {
            return reject(candidate, "GLUE_MACHINE_NOT_MATCH", "胶料机台关系不匹配");
        }
        if (Boolean.FALSE.equals(candidate.getFixedMachineSelected())) {
            return reject(candidate, "FIXED_MACHINE_NOT_SELECTED", "未命中选择定点生产机台");
        }
        if (Boolean.TRUE.equals(candidate.getFixedMachineExcluded())) {
            return reject(candidate, "FIXED_MACHINE_EXCLUDED", "命中定点不可生产机台");
        }
        candidate.setFiltered(Boolean.FALSE);
        candidate.getEvidence().put("task", task.getBusinessKey());
        candidate.getEvidence().put("machineCode", candidate.getMachineCode());
        return true;
    }

    /**
     * 标记候选机台被过滤。
     *
     * @param candidate  候选机台
     * @param reasonCode 原因编码
     * @param reasonDesc 原因描述
     * @return false，表示未通过过滤
     */
    private boolean reject(TmMachineCandidate candidate, String reasonCode, String reasonDesc) {
        candidate.setFiltered(Boolean.TRUE);
        candidate.setFilterReasonCode(reasonCode);
        candidate.setFilterReasonDesc(reasonDesc);
        candidate.getEvidence().put("ruleCode", reasonCode);
        candidate.getEvidence().put("reasonDesc", reasonDesc);
        return false;
    }
}
