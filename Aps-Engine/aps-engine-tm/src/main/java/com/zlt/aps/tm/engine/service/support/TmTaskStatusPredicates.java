package com.zlt.aps.tm.engine.service.support;

import cn.hutool.core.util.StrUtil;
import com.zlt.aps.tm.engine.domain.TmTaskDraft;

import java.math.BigDecimal;

/**
 * 胎面任务状态判定工具。
 *
 * <p>集中落库转换与解释快照使用的状态口径，避免同一任务被两个出口判定为不同状态。</p>
 */
public final class TmTaskStatusPredicates {

    /**
     * 工具类不允许实例化。
     */
    private TmTaskStatusPredicates() {
    }

    /**
     * 判断任务是否需要按未排语义处理。
     *
     * @param task 待判定任务
     * @return true 表示任务需要进入未排语义
     */
    public static boolean isUnplannedTask(TmTaskDraft task) {
        if (task == null) {
            return false;
        }
        if (StrUtil.isNotBlank(task.getUnplannedReasonCode())) {
            return true;
        }
        return task.isUnassigned() && task.getPlanQty() != null
                && task.getPlanQty().compareTo(BigDecimal.ZERO) > 0;
    }

    /**
     * 判断任务是否无需生产。
     *
     * @param task 待判定任务
     * @return true 表示最终计划量为空或小于等于 0，且不是未排任务
     */
    public static boolean isNoProductionNeeded(TmTaskDraft task) {
        return task != null && !isUnplannedTask(task)
                && (task.getPlanQty() == null || task.getPlanQty().compareTo(BigDecimal.ZERO) <= 0);
    }
}
