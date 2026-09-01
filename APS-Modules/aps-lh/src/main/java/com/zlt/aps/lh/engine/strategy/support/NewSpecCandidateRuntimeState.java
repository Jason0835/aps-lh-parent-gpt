package com.zlt.aps.lh.engine.strategy.support;

import java.time.LocalDate;

/**
 * 新增排产候选在T～T+2窗口内持续生效的轻量运行态。
 *
 * <p>候选按业务日和阶段会重复构建 {@link DailyNewSpecCandidate} 视图，但原始日期池、
 * 剩余物理机台机会、特殊SKU分类和最后失败原因必须跨日保留。该对象只保存编排元数据，
 * 不复制SKU、日计划、胎胚、模具或结果账本。</p>
 *
 * @author APS
 */
public class NewSpecCandidateRuntimeState {

    /** SKU首次进入可执行候选池时确定的原始日期池 */
    private LocalDate originalPoolDate;
    /** 当前日期池内尚未消费的物理机台机会数 */
    private int remainingMachineCount = 1;
    /** 上次对账读取的目标物理机台数 */
    private int lastRequiredMachineCount;
    /** 是否已经完成首次机台数对账 */
    private boolean machineCountInitialized;
    /** 剩余机台机会最近一次对账业务日 */
    private LocalDate machineCountReconciliationDate;
    /** 严格收尾尚有真实余量时持续补充机台机会，不受dayN目标台数截断 */
    private boolean strictEndingClearance;
    /** 是否命中特殊SKU置换阶段分类 */
    private boolean specialSku;
    /** 特殊SKU是否已经按中心排序口径完成分类 */
    private boolean specialSkuClassified;
    /** 最近一次Machine×SKU试算失败原因 */
    private String lastFailure;

    public LocalDate getOriginalPoolDate() {
        return originalPoolDate;
    }

    /**
     * 仅在首次形成日期池时登记来源日期，后续延期、部分成功和跨日在机均不得覆盖。
     *
     * @param poolDate 首次来源日期
     */
    public void initializeOriginalPoolDate(LocalDate poolDate) {
        if (originalPoolDate == null && poolDate != null) {
            originalPoolDate = poolDate;
        }
    }

    public int getRemainingMachineCount() {
        return remainingMachineCount;
    }

    public void setRemainingMachineCount(int remainingMachineCount) {
        this.remainingMachineCount = Math.max(0, remainingMachineCount);
    }

    /**
     * 用中心目标机台数和正式已排机台数刷新剩余机会。
     *
     * @param requiredMachineCount 当前目标物理机台数
     * @param scheduledMachineCount 当前正式已排物理机台数
     */
    public void reconcileRemainingMachineCount(int requiredMachineCount,
                                               int scheduledMachineCount,
                                               LocalDate currentDate) {
        int normalizedRequired = Math.max(0, requiredMachineCount);
        int calculatedRemaining = Math.max(
                0, normalizedRequired - Math.max(0, scheduledMachineCount));
        if (!machineCountInitialized
                || (currentDate != null
                && !currentDate.equals(machineCountReconciliationDate))) {
            remainingMachineCount = calculatedRemaining;
            lastRequiredMachineCount = normalizedRequired;
            machineCountInitialized = true;
            machineCountReconciliationDate = currentDate;
            return;
        }
        int requiredIncrease = Math.max(0, normalizedRequired - lastRequiredMachineCount);
        remainingMachineCount = Math.min(
                calculatedRemaining, remainingMachineCount + requiredIncrease);
        lastRequiredMachineCount = normalizedRequired;
        machineCountReconciliationDate = currentDate;
    }

    /** 消费一次物理机台机会，单控L/R整机只调用一次。 */
    public void consumeMachineOpportunity() {
        remainingMachineCount = Math.max(0, remainingMachineCount - 1);
    }

    public boolean isStrictEndingClearance() {
        return strictEndingClearance;
    }

    public void setStrictEndingClearance(boolean strictEndingClearance) {
        this.strictEndingClearance = strictEndingClearance;
    }

    public boolean isSpecialSku() {
        return specialSku;
    }

    public void setSpecialSku(boolean specialSku) {
        this.specialSku = specialSku;
        this.specialSkuClassified = true;
    }

    public boolean isSpecialSkuClassified() {
        return specialSkuClassified;
    }

    public String getLastFailure() {
        return lastFailure;
    }

    public void setLastFailure(String lastFailure) {
        this.lastFailure = lastFailure;
    }
}
