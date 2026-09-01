package com.zlt.aps.lh.engine.strategy.support;

import com.zlt.aps.lh.api.domain.dto.SkuScheduleDTO;

import java.time.LocalDate;
import java.util.EnumSet;
import java.util.Objects;

/**
 * 新增排产单个业务日的 SKU 候选。
 *
 * <p>该对象只承载日编排元数据，不复制 SKU 的模具、胎胚、目标量、日计划账本等运行态数据。
 * 所有资源计算仍直接读取 {@link SkuScheduleDTO} 和排程上下文，保证全窗口只有一份权威状态。</p>
 *
 * @author APS
 */
public class DailyNewSpecCandidate {

    /** 物料编码和产品状态组成的业务键，仅用于日志和候选去重诊断 */
    private final String skuKey;
    /** 现有新增排产 SKU 对象，保留 S4.5 排序和全部运行态账本 */
    private final SkuScheduleDTO sku;
    /** 当前业务日命中的全部候选来源 */
    private final EnumSet<DailyCandidateReason> reasons =
            EnumSet.noneOf(DailyCandidateReason.class);
    /** 提前生产候选在正式选机前形成的只读运行视图预览 */
    private EarlyProductionRuntimePlan earlyProductionPreview;
    /** 本次准入实际消费或提前拉取的计划日期 */
    private LocalDate targetPlanDate;
    /** 构建候选时读取的当前日实时日计划余额 */
    private int realtimeDayPlanRemainingQty;
    /** 当前 SKU 是否已经存在跨日在机绑定 */
    private boolean boundOnMachine;
    /** 当前业务日是否被机台无关的业务门禁阻止进入资源竞争 */
    private boolean machineCompetitionBlocked;
    /** 跨业务日和阶段持续复用的轻量候选运行态 */
    private final NewSpecCandidateRuntimeState runtimeState;

    /**
     * 创建单个业务日候选。
     *
     * @param skuKey 物料和产品状态业务键
     * @param sku 现有新增排产 SKU
     */
    public DailyNewSpecCandidate(String skuKey, SkuScheduleDTO sku) {
        this(skuKey, sku, new NewSpecCandidateRuntimeState());
    }

    /**
     * 创建复用窗口级运行态的业务日候选。
     *
     * @param skuKey 物料和产品状态业务键
     * @param sku 现有新增排产SKU
     * @param runtimeState 窗口级轻量候选运行态
     */
    public DailyNewSpecCandidate(String skuKey,
                                 SkuScheduleDTO sku,
                                 NewSpecCandidateRuntimeState runtimeState) {
        this.skuKey = skuKey;
        this.sku = Objects.requireNonNull(sku, "新增排产SKU不能为空");
        this.runtimeState = Objects.requireNonNull(runtimeState, "新增排产候选运行态不能为空");
    }

    /**
     * 增加候选来源。
     *
     * @param reason 候选来源
     */
    public void addReason(DailyCandidateReason reason) {
        if (Objects.nonNull(reason)) {
            reasons.add(reason);
        }
    }

    /**
     * 判断是否命中指定候选来源。
     *
     * @param reason 候选来源
     * @return true-已命中；false-未命中
     */
    public boolean hasReason(DailyCandidateReason reason) {
        return Objects.nonNull(reason) && reasons.contains(reason);
    }

    public String getSkuKey() {
        return skuKey;
    }

    public SkuScheduleDTO getSku() {
        return sku;
    }

    public EnumSet<DailyCandidateReason> getReasons() {
        return EnumSet.copyOf(reasons);
    }

    public EarlyProductionRuntimePlan getEarlyProductionPreview() {
        return earlyProductionPreview;
    }

    public void setEarlyProductionPreview(EarlyProductionRuntimePlan earlyProductionPreview) {
        this.earlyProductionPreview = earlyProductionPreview;
    }

    public LocalDate getTargetPlanDate() {
        return targetPlanDate;
    }

    public void setTargetPlanDate(LocalDate targetPlanDate) {
        this.targetPlanDate = targetPlanDate;
    }

    public int getRealtimeDayPlanRemainingQty() {
        return realtimeDayPlanRemainingQty;
    }

    public void setRealtimeDayPlanRemainingQty(int realtimeDayPlanRemainingQty) {
        this.realtimeDayPlanRemainingQty = Math.max(0, realtimeDayPlanRemainingQty);
    }

    public boolean isBoundOnMachine() {
        return boundOnMachine;
    }

    public void setBoundOnMachine(boolean boundOnMachine) {
        this.boundOnMachine = boundOnMachine;
    }

    public boolean isMachineCompetitionBlocked() {
        return machineCompetitionBlocked;
    }

    public void blockMachineCompetition(String reason) {
        this.machineCompetitionBlocked = true;
        this.setLastFailure(reason);
    }

    public LocalDate getPoolDate() {
        return runtimeState.getOriginalPoolDate();
    }

    public void setPoolDate(LocalDate poolDate) {
        runtimeState.initializeOriginalPoolDate(poolDate);
    }

    public int getRemainingMachineCount() {
        return runtimeState.getRemainingMachineCount();
    }

    public void setRemainingMachineCount(int remainingMachineCount) {
        runtimeState.setRemainingMachineCount(remainingMachineCount);
    }

    /**
     * 用中心目标机台数和正式已排机台数刷新剩余机会。
     *
     * <p>已消费机会不会因统计索引延迟刷新而被重新补回；中心目标数后续真实增加时，
     * 只追加新增差额，再用正式已排缺口收口。</p>
     *
     * @param requiredMachineCount 当前中心目标物理机台数
     * @param scheduledMachineCount 当前正式已排物理机台数
     * @param currentDate 当前竞争业务日
     */
    public void reconcileRemainingMachineCount(int requiredMachineCount,
                                               int scheduledMachineCount,
                                               LocalDate currentDate) {
        runtimeState.reconcileRemainingMachineCount(
                requiredMachineCount, scheduledMachineCount, currentDate);
    }

    /**
     * 消费一次物理机台机会。
     *
     * <p>正规单控 L/R 两条结果属于同一个物理机台组，本方法只允许调用一次。</p>
     */
    public void consumeMachineOpportunity() {
        runtimeState.consumeMachineOpportunity();
    }

    public boolean isStrictEndingClearance() {
        return runtimeState.isStrictEndingClearance();
    }

    public void setStrictEndingClearance(boolean strictEndingClearance) {
        runtimeState.setStrictEndingClearance(strictEndingClearance);
    }

    public boolean isSpecialSku() {
        return runtimeState.isSpecialSku();
    }

    public void setSpecialSku(boolean specialSku) {
        runtimeState.setSpecialSku(specialSku);
    }

    public boolean isSpecialSkuClassified() {
        return runtimeState.isSpecialSkuClassified();
    }

    public String getLastFailure() {
        return runtimeState.getLastFailure();
    }

    public void setLastFailure(String lastFailure) {
        runtimeState.setLastFailure(lastFailure);
    }

    public NewSpecCandidateRuntimeState getRuntimeState() {
        return runtimeState;
    }
}
