package com.zlt.aps.common.engine.schedule.constraint;

import java.math.BigDecimal;

/**
 * 工装账本公共计算适配模板。
 *
 * <p>模板统一使用生产前工装限制和已确认生产量结算两套公式，领域实现只提供任务的总工装数量和有效卷曲长度。
 * 上下文余额、任务状态回写、过程日志和事务边界仍由 TM/TC 服务负责。</p>
 *
 * @param <T> 领域任务类型
 */
public abstract class AbstractToolLedgerCalculator<T> {

    private final ScheduleConstraintCalculator constraintCalculator = new ScheduleConstraintCalculator();

    /**
     * 按生产前工装余额计算允许计划量和工装溢出量。
     *
     * @param task                 当前领域任务
     * @param requestedProductionQty 请求生产量
     * @param releasedDemandQty    当前结算释放的成型需求量
     * @param availableToolQty     结算前可用工装数量；为空表示未启用
     * @return 工装账本结算结果
     */
    public final ScheduleToolLedgerResult settleProductionBeforeRelease(T task,
                                                                         BigDecimal requestedProductionQty,
                                                                         BigDecimal releasedDemandQty,
                                                                         BigDecimal availableToolQty) {
        return this.constraintCalculator.settleProductionBeforeReleaseToolLedger(
                requestedProductionQty, releasedDemandQty, availableToolQty,
                this.getTotalToolQty(task), this.getCurlRollLength(task));
    }

    /**
     * 按指定工装池上限限制自动排程任务计划量并结算生产占用。
     *
     * @param task                  当前领域任务
     * @param requestedProductionQty 请求生产量
     * @param releasedDemandQty     当前结算释放的成型需求量
     * @param availableToolQty      结算前可用工装数量
     * @param totalToolQtyLimit     本次结算使用的有效工装上限
     * @return 工装账本结算结果
     */
    public final ScheduleToolLedgerResult settleProductionBeforeRelease(T task,
                                                                          BigDecimal requestedProductionQty,
                                                                          BigDecimal releasedDemandQty,
                                                                          BigDecimal availableToolQty,
                                                                          BigDecimal totalToolQtyLimit) {
        return this.constraintCalculator.settleProductionBeforeReleaseToolLedger(
                requestedProductionQty, releasedDemandQty, availableToolQty,
                totalToolQtyLimit == null ? this.getTotalToolQty(task) : totalToolQtyLimit,
                this.getCurlRollLength(task));
    }

    /**
     * 按已确认生产量结算工装账本，不再次压缩计划量。
     *
     * @param task                 当前领域任务
     * @param committedProductionQty 已确认生产量
     * @param releasedDemandQty    当前结算释放的成型需求量
     * @param availableToolQty     结算前可用工装数量；为空表示未启用
     * @return 工装账本结算结果
     */
    public final ScheduleToolLedgerResult settleCommitted(T task,
                                                          BigDecimal committedProductionQty,
                                                          BigDecimal releasedDemandQty,
                                                          BigDecimal availableToolQty) {
        return this.constraintCalculator.settleCommittedToolLedger(
                committedProductionQty, releasedDemandQty, availableToolQty,
                this.getTotalToolQty(task), this.getCurlRollLength(task));
    }

    /**
     * 按指定工装池上限结算已确认的自动排程生产量。
     *
     * @param task                   当前领域任务
     * @param committedProductionQty 已确认生产量
     * @param releasedDemandQty      当前结算释放的成型需求量
     * @param availableToolQty       结算前可用工装数量
     * @param totalToolQtyLimit      本次结算使用的有效工装上限
     * @return 工装账本结算结果
     */
    public final ScheduleToolLedgerResult settleCommitted(T task,
                                                           BigDecimal committedProductionQty,
                                                           BigDecimal releasedDemandQty,
                                                           BigDecimal availableToolQty,
                                                           BigDecimal totalToolQtyLimit) {
        return this.constraintCalculator.settleCommittedToolLedger(
                committedProductionQty, releasedDemandQty, availableToolQty,
                totalToolQtyLimit == null ? this.getTotalToolQty(task) : totalToolQtyLimit,
                this.getCurlRollLength(task));
    }

    /**
     * 读取任务总工装数量。
     *
     * @param task 当前领域任务
     * @return 总工装数量
     */
    protected abstract BigDecimal getTotalToolQty(T task);

    /**
     * 读取任务有效卷曲长度。
     *
     * @param task 当前领域任务
     * @return 有效卷曲长度
     */
    protected abstract BigDecimal getCurlRollLength(T task);
}
