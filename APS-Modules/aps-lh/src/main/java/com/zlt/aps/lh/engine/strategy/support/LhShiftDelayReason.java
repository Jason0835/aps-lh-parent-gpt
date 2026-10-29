package com.zlt.aps.lh.engine.strategy.support;

import java.util.Objects;

/**
 * 班次延后备注的稳定原因编码，仅用于观测，不参与排程决策。
 *
 * @author APS
 * @date 2026/10/23
 */
public enum LhShiftDelayReason {

    /**
     * 实际换模或换活字块额度不足。
     */
    CHANGEOVER_QUOTA("超换模/换活字块次数上限"),
    /**
     * 普通结构计划的有效物理机台配额不足。
     */
    STRUCTURE_MACHINE_QUOTA("超结构机台数"),
    /**
     * 结构切换等结果期间的首班或第二班新增机台名额不足。
     */
    STRUCTURE_SWITCH_WAIT_RESULT_QUOTA("结构切换等结果限制新增开机"),
    /**
     * 结构切换不等结果时的首班新增机台名额不足。
     */
    STRUCTURE_SWITCH_FIRST_SHIFT_QUOTA("结构切换首班限制新增开机"),
    /**
     * 已确认可用模具数量不足。
     */
    MOULD_SHORTAGE("模具不足");

    /**
     * 成型供胚来源明确要求等待结果的标识。
     */
    private static final Integer WAIT_NOTIFY = 1;

    /**
     * 追加到既有原因分析的完整备注项。
     */
    private final String analysis;

    /**
     * 创建固定原因项。
     *
     * @param analysis 班次备注文字
     */
    LhShiftDelayReason(String analysis) {
        this.analysis = analysis;
    }

    /**
     * 取得原因分析的追加文字。
     *
     * @return 固定完整备注项
     */
    public String getAnalysis() {
        return analysis;
    }

    /**
     * 按原供胚来源区分临时限台备注，避免与普通结构计划配额混淆。
     *
     * @param waitNotify 等结果标识，空值沿用原规则按不等结果解释
     * @return 仅用于备注的结构切换限台原因
     */
    public static LhShiftDelayReason forStructureSwitch(Integer waitNotify) {
        return Objects.equals(WAIT_NOTIFY, waitNotify)
                ? STRUCTURE_SWITCH_WAIT_RESULT_QUOTA : STRUCTURE_SWITCH_FIRST_SHIFT_QUOTA;
    }
}
