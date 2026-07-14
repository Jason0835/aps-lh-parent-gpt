package com.zlt.aps.cx.vo;

import lombok.Data;

/**
 * 均衡轮次配置 - 封装不同轮次之间的行为差异，实现配置驱动的策略分支。
 *
 * <p><b>设计目标</b>：通过配置对象控制 DFS 及辅助方法的行为分支，
 * 避免在 DFS 递归签名中堆积布尔参数，同时支持未来扩展更多轮次。
 *
 * <p><b>轮次差异</b>：
 * <table>
 *   <tr><th>配置项</th><th>第一轮（严格）</th><th>第二轮（宽松）</th></tr>
 *   <tr><td>enforceCapacityLimit</td><td>true（强制maxCapacity管控）</td><td>false（解除容量上限）</td></tr>
 *   <tr><td>enforceReservedHistory</td><td>=forceKeepHistory（按参数）</td><td>false（不强制保底预留）</td></tr>
 *   <tr><td>boostHistoryPreference</td><td>false（历史偏好为排序④级）</td><td>true（启用第二轮排序策略：历史优先+同胎胚集中+均衡例外）</td></tr>
 *   <tr><td>capacitySufficientOverride</td><td>null（按实际供需计算）</td><td>Boolean.TRUE（强制产能充足策略）</td></tr>
 *   <tr><td>enforceContinuePreload</td><td>true（续作预扣占用种类槽+负荷）</td><td>false（不预扣，释放种类槽供新胎胚使用）</td></tr>
 * </table>
 *
 * @author APS Team
 */
@Data
public class BalancingRoundConfig {
    /** 轮次名称（日志标识，如"第一轮-严格均衡"/"第二轮-宽松均衡"） */
    private String roundName;
    /**
     * 是否强制容量上限管控（maxCapacity）。
     * <p>第一轮=true：机台 currentLoad 超过 maxCapacity 时拒绝分配；
     * 第二轮=false：解除容量限制，允许超载分配，确保所有任务都能上机。
     */
    private boolean enforceCapacityLimit;
    /**
     * 是否执行保底预留（reservedHistoryTasks，SYS04070003=Y 时的 DFS 前预留）。
     * <p>第一轮=forceKeepHistory 参数值；第二轮=false：不强制预留。
     */
    private boolean enforceReservedHistory;
    /**
     * 是否启用第二轮排序策略（历史胎胚优先 + 同胎胚任务集中 + 均衡性优先例外）。
     * <p>第一轮=false：历史偏好为排序第④优先级；
     * 第二轮=true：启用三项排序原则--
     * <ul>
     *   <li>① 历史胎胚优先：历史机台绝对优先，保障生产连贯性</li>
     *   <li>② 同胎胚任务集中：已有该胎胚的机台优先集中，提高处理效率</li>
     *   <li>③ 均衡性优先例外：当集中会导致负荷差超阈值时，不集中而按均衡拆分</li>
     * </ul>
     */
    private boolean boostHistoryPreference;
    /**
     * 产能充足标志覆盖值（null=按实际供需计算，非null=强制使用该值）。
     * <p>第二轮强制为 true（容量无限），使用均衡优先的排序策略。
     */
    private Boolean capacitySufficientOverride;
    /**
     * 是否执行续作预扣（continueLoadMap/continueTypeMap/continueLhMachineCodeMap）。
     * <p>第一轮=true：机台初始化时预扣续作已占的容量和种类槽，反映实际机台占用状态；
     * 第二轮=false：不预扣，释放种类槽供新胎胚使用，避免 maxTypes 约束阻止分配。
     * <p><b>原因</b>：续作预扣设置的 currentTypes 在第二轮仍受 maxTypes 约束，
     * 若机台续作已占满 maxTypes 个种类槽，即使解除容量上限，新胎胚仍无法上机，
     * 违背"确保所有任务都能被成功分配"的核心目标。
     */
    private boolean enforceContinuePreload;

    /**
     * 目标胎胚种类数（ceil(总种类数/机台数)），引导 DFS 优先按此值均衡分配种类。
     * <p>在 sortCandidatesForDfs 中，新种类分配时未达目标值的机台优先；
     * 在 calculateBalancingScore 中，超出目标值的机台施加额外惩罚。
     * <p>0 表示不启用目标种类引导（保持原逻辑）。
     */
    private int targetTypesPerMachine;

    /**
     * 是否将 targetTypesPerMachine 作为 maxTypes 硬约束（第一轮=true，第二轮=false）。
     * <p>true 时，MachineState 初始化阶段将 maxTypes cap 为 min(原始maxTypes, targetTypesPerMachine)，
     * 强制 DFS 先尝试 3,3,3,3 均衡分布；若无法获得完整解则自动进入第二轮恢复原始 maxTypes。
     * <p>false 时，使用机台原始 maxTypes，不做限制。
     */
    private boolean useTargetAsMaxTypes;
}
