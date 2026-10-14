package com.zlt.aps.lh.engine.strategy.support;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.Date;

/**
 * 按时间下机的规则节点和最终物理释放事实；后料动作仍需独立校验。
 */
@Data
public class OffMachineDecision implements Serializable {
    /**
     * 运行态决策序列化版本。
     */
    private static final long serialVersionUID = 1L;

    /**
     * 实际释放所属业务日；纯规则预览时为候选业务日。
     */
    private LocalDate planDate;
    /**
     * 实际释放班次类型，可为夜班；不能用作后料换模次数归属。
     */
    private String shift;
    /**
     * 中位法及候选校验选出的规则节点；无合法节点时为空。
     */
    private Date ruleOffMachineTime;
    /**
     * 最终逐班正量对应的真实完成时刻，双侧取较晚值；零量时为空。
     */
    private Date productionEndTime;
    /**
     * 最终前料的物理机台合计量，预演不消费正式数量账本。
     */
    private Integer scheduledQty;
    /**
     * 实际物理释放时刻；纯规则预览时表示候选节点。
     */
    private Date offMachineTime;
    /**
     * 前料占用截止；提交后与物理释放时刻一致。
     */
    private Date occupancyEndTime;
    /**
     * 命中的规则及实际释放原因。
     */
    private String reason;
}
