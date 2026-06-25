package com.zlt.aps.tm.autoplan;

import lombok.Data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * 胎面自动排程 JSON 场景期望结果。
 *
 * <p>用于描述自动排程响应、排程结果和解释信息。</p>
 */
@Data
public class TmAutoPlanExpectedResult {

    /** 是否期望完整入口执行成功 */
    private Boolean success = Boolean.TRUE;

    /** 是否期望要求覆盖确认 */
    private Boolean confirmRequired;

    /** 期望响应消息包含文本 */
    private String messageContains;

    /** 期望拒绝消息包含文本 */
    private String rejectMessageContains;

    /** 期望结果表写入数量 */
    private Integer resultCount;

    /** 期望已排数量 */
    private Integer assignedCount;

    /** 期望未排数量 */
    private Integer unassignedCount;

    /** 期望解释表写入数量 */
    private Integer explainCount;

    /** 期望落库错误数量 */
    private Integer errorCount;

    /** 期望排程结果明细 */
    private List<ExpectedScheduleResult> expectedResults = new ArrayList<>();

    /** 期望解释信息明细 */
    private List<ExpectedExplain> expectedExplains = new ArrayList<>();

    /**
     * 排程结果期望。
     */
    @Data
    public static class ExpectedScheduleResult {

        /** 订单号 */
        private String orderNo;

        /** 胎面编码 */
        private String treadCode;

        /** 机台编码，未排任务允许为空 */
        private String machineCode;

        /** 班次顺序 */
        private Integer shiftOrder;

        /** 班内顺序 */
        private Integer sequence;

        /** 计划量 */
        private BigDecimal planQty;
    }

    /**
     * 解释信息期望。
     */
    @Data
    public static class ExpectedExplain {

        /** 订单号 */
        private String orderNo;

        /** 胎面编码 */
        private String treadCode;

        /** 最终计划量 */
        private BigDecimal finalPlanQty;

        /** 未排原因编码 */
        private String unplannedReasonCode;

        /** 规则命中 JSON 需包含文本 */
        private String ruleHitContains;

        /** 候选机台 JSON 需包含文本 */
        private String candidateMachineContains;

        /** 最终选机说明需包含文本 */
        private String machineSelectReasonContains;

        /** 分配状态 */
        private String assignStatus;

        /** 选中机台评分 */
        private BigDecimal selectedMachineScore;
    }
}
