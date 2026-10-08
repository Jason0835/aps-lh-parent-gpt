package com.zlt.aps.common.engine.schedule.engine;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 收尾后目标生产班次判定结果。
 *
 * <p>该结果是纯规则计算结果，供 TM/TC 领域服务写回任务运行态并生成解释证据。</p>
 */
@Data
public class SchedulePostTailDecision {

    /**
     * 是否已确认实际收尾。
     */
    private Boolean closeOutConfirmed;

    /**
     * 实际收尾逻辑班次 C。
     */
    private Integer actualCloseOutShiftOrder;

    /**
     * 当前目标生产逻辑班次 S。
     */
    private Integer targetShiftOrder;

    /**
     * S-C 的班次数差。
     */
    private Integer intervalShiftCount;

    /**
     * 收尾后第 N 个允许排产班次。
     */
    private Integer recoveryShiftOrder;

    /**
     * 是否禁止当前目标班次排产。
     */
    private Boolean blocked;

    /**
     * 是否允许沿用普通计划量计算。
     */
    private Boolean allowNormalPlan;

    /**
     * 判定原因。
     */
    private String decisionReason;

    /**
     * 被跳过的目标班次。
     */
    private List<Integer> skippedTargetShiftOrders = new ArrayList<>();

    /**
     * 设置跳过班次并复制容器。
     *
     * @param shiftOrders 跳过班次
     */
    public void setSkippedTargetShiftOrders(List<Integer> shiftOrders) {
        this.skippedTargetShiftOrders = shiftOrders == null
                ? new ArrayList<>() : new ArrayList<>(shiftOrders);
    }
}
