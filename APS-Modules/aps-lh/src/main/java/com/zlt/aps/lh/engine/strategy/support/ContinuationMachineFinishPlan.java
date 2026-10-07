package com.zlt.aps.lh.engine.strategy.support;

import lombok.Data;
import java.util.Date;

/** 一台物理机台的余量收尾计算结果，零量释放不伪造生产班次。 */
@Data
public class ContinuationMachineFinishPlan {
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
    /** 历史交替终末余量的完成或退出事实；其他节点场景不设置。 */
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
