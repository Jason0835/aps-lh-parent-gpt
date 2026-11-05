package com.zlt.aps.lh.engine.strategy.support;

import lombok.Getter;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 精确历史任务在当前资源时刻选定的执行动作与完整目标模具组。
 * 原装机事实仍保存在事件的初始动作中，本决定随候选失败恢复、随成功组合重放。
 *
 * @author APS
 * @date 2026/11/05
 */
@Getter
public final class PreviousAlternateExecutionPlan implements Serializable {
    /**
     * 序列化版本。
     */
    private static final long serialVersionUID = 1L;
    /**
     * 本次执行动作，保留原前料及原装机事实。
     */
    private final PreviousAlternateActionPlan action;
    /**
     * 本次正规换模选定的完整目标模具组，按现有分配优先序保存。
     */
    private final List<String> mouldCodes;
    /**
     * 本次完整组通过资源预检的最早准备时刻，单位毫秒。
     */
    private final long resourceReadyTime;

    /**
     * 冻结已通过资源预检的替代组合，后续仍需完整提案验证。
     *
     * @param action 实际执行动作
     * @param mouldCodes 已选择的完整目标组
     * @param resourceReadyTime 通过预检的资源时刻
     */
    public PreviousAlternateExecutionPlan(PreviousAlternateActionPlan action,
            List<String> mouldCodes, long resourceReadyTime) {
        this.action = action;
        this.mouldCodes = Collections.unmodifiableList(new ArrayList<>(mouldCodes));
        this.resourceReadyTime = resourceReadyTime;
    }
}
