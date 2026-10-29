package com.zlt.aps.lh.engine.strategy.support;

import lombok.Data;
import java.util.Date;

/** 一台物理机台的余量收尾计算结果，零量释放不伪造生产班次。 */
@Data
public class ContinuationMachineFinishPlan {
    /**
     * 精确历史指定交替前缀，独立于普通错峰收尾节点。
     */
    public static final String HISTORICAL_CUTOVER_RULE = "历史指定交替前缀";
    /** 物理机台合计计划量。 */
    private int planQty;
    /** 最后正产量班次，零量时为空。 */
    private Integer finishShiftIndex;
    /** 实际生产结束时刻，零量时为空。 */
    private Date finishTime;
    /** 物理机台可交接时刻。 */
    private Date offlineTime;
    /** 本场景要求的收尾截止，承接余量的自由机台仅受窗口限制。 */
    private Date finishDeadline;
    /** 分配前确定释放角色的唯一历史计划，空值表示原有分配路径。 */
    private Long historicalFinishPlanId;
    /** 释放角色固定的物理机台，禁止后置槽位交换改变该身份。 */
    private String finishRoleMachineCode;
    /** 本次释放节点采用的业务规则。 */
    private String finishRule;
    /**
     * T日节点角色分配前的业务输入，供保存前独立核对。
     */
    private ContinuationTDayFinishConstraint tDayFinishConstraint;
    /**
     * 跨日两机大余量分配前的公式、物理角色及夜班优先约束。
     */
    private ContinuationNightFinishConstraint nightFinishConstraint;
    /**
     * 普通及历史交替续作余量统一的完成或退出事实，窗口未完成时不得提前释放机台。
     */
    private FinishState finishState;

    /** 区分物料已完成、机台强制退出和窗口内仍未完成，禁止把生产停止直接当作余量收尾。 */
    public enum FinishState {
        /** 本组合法余量已经全部分配完成。 */
        SURPLUS_COMPLETED,
        /** 本机达到已冻结的释放节点、交替或其他物理硬截止。 */
        FORCED_RELEASE,
        /** 本组余量未清完，本机继续占用到窗口末端。 */
        WINDOW_UNFINISHED
    }
}
