package com.zlt.aps.lh.engine.strategy.support;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/** 多机台余量收尾提交快照，仅保存最终只读衔接复核必需字段，不复制排程上下文。 */
@Data
public class ContinuationEndingAllocationSnapshot implements Serializable {
    private static final long serialVersionUID = 1L;
    /** 来源SKU分组键，防止不同目标或状态的结果混为一组。 */
    private String groupKey;
    /** 已提交的八班实际数量。 */
    private int[] shiftQuantities;
    /** 本行首个生产时间。 */
    private Date productionStartTime;
    /** 单行实际生产结束时间。 */
    private Date productionEndTime;
    /** 单控整机、停产保机及胶囊窗口约束后的物理释放时间。 */
    private Date releaseTime;
    /** 组级模拟次数快照下的预测换模时间，null表示当时无合法预测。 */
    private Date predictedChangeTime;
    /** 预测使用的动作类型，后料尚未选定时为通用交替。 */
    private String predictedActionType;
    /** 预测使用的切换耗时，正式后料动作不同时必须解释差异。 */
    private int predictedDurationHours;
}
