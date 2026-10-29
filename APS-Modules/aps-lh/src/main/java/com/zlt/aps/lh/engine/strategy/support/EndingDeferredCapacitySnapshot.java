package com.zlt.aps.lh.engine.strategy.support;

import com.zlt.aps.lh.context.LhScheduleContext;
import lombok.Data;
import lombok.ToString;
import java.util.Date;

/**
 * 延期评估专用的隔离产能快照，不挂接正式生产结果、数量账本或持久化流程。
 *
 * @author APS
 * @date 2026/10/27
 */
@Data
public class EndingDeferredCapacitySnapshot {
    /**
     * 正式窗口最后一个班次的结束时刻，延期从该边界开始。
     */
    private Date windowEndTime;
    /**
     * 按工厂班次连续延长X个业务日后的截止时刻，截止点不包含在产能区间内。
     */
    private Date deferredEndTime;
    /**
     * 只用于公共产能公式的隔离上下文，全部可变运行态与正式上下文分离。
     */
    @ToString.Exclude
    private LhScheduleContext capacityContext;
}
