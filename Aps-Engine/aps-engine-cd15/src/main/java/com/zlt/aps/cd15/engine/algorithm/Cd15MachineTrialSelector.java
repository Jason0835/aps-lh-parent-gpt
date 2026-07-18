package com.zlt.aps.cd15.engine.algorithm;

import com.zlt.aps.cd15.engine.model.Cd15MachineTrial;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 候选机台试算方案选择器。
 */
@Component
public class Cd15MachineTrialSelector {

    /**
     * 按自动排程业务优先级选择最佳候选机台方案。
     *
     * @param trials 候选机台试算结果
     * @return 最佳方案；无可排方案时返回null
     */
    public Cd15MachineTrial select(List<Cd15MachineTrial> trials) {
        if (trials == null) {
            return null;
        }
        Cd15MachineTrial positive = trials.stream()
                .filter(item -> item.getFinalSchedulableQuantity() != null
                        && item.getFinalSchedulableQuantity().signum() > 0)
                .min(this::compare)
                .orElse(null);
        if (positive != null) {
            return positive;
        }
        return trials.stream()
                .filter(item -> item.getLimitReason() != null)
                .min(this::compareLimitReason)
                .orElse(null);
    }
    private int compareLimitReason(Cd15MachineTrial first, Cd15MachineTrial second) {
        int result = Integer.compare(limitPriority(first.getLimitReason()), limitPriority(second.getLimitReason()));
        if (result != 0) {
            return result;
        }
        result = Integer.compare(first.getPriorityOrder(), second.getPriorityOrder());
        if (result != 0) {
            return result;
        }
        if (first.getMachineCode() == null) {
            return second.getMachineCode() == null ? 0 : 1;
        }
        if (second.getMachineCode() == null) {
            return -1;
        }
        return first.getMachineCode().compareTo(second.getMachineCode());
    }

    private int limitPriority(String limitReason) {
        if ("TOOLING_LIMIT".equals(limitReason)) {
            return 0;
        }
        if ("CAPACITY_LIMIT".equals(limitReason)) {
            return 1;
        }
        return 2;
    }
    private int compare(Cd15MachineTrial first, Cd15MachineTrial second) {
        int result = Boolean.compare(second.isHistoryMachine(), first.isHistoryMachine());
        if (result != 0) {
            return result;
        }
        result = Boolean.compare(second.isFullyAccommodated(), first.isFullyAccommodated());
        if (result != 0) {
            return result;
        }
        if (!first.isFullyAccommodated()) {
            result = second.getFinalSchedulableQuantity().compareTo(first.getFinalSchedulableQuantity());
            if (result != 0) {
                return result;
            }
        }
        result = Boolean.compare(second.isPreferredMachine(), first.isPreferredMachine());
        if (result != 0) {
            return result;
        }
        result = Integer.compare(first.getPriorityOrder(), second.getPriorityOrder());
        if (result != 0) {
            return result;
        }
        result = Boolean.compare(second.isSameTailSpec(), first.isSameTailSpec());
        if (result != 0) {
            return result;
        }
        result = Integer.compare(first.getRemainingSeconds(), second.getRemainingSeconds());
        if (result != 0) {
            return result;
        }
        if (first.getMachineCode() == null) {
            return second.getMachineCode() == null ? 0 : 1;
        }
        if (second.getMachineCode() == null) {
            return -1;
        }
        return first.getMachineCode().compareTo(second.getMachineCode());
    }
}
