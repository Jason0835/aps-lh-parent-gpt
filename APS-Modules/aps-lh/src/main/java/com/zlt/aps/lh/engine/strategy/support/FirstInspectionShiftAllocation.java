package com.zlt.aps.lh.engine.strategy.support;

import com.zlt.aps.lh.api.domain.vo.LhShiftConfigVO;

import java.util.Date;

/**
 * 一次换模或换活字块首检在单个班次中的时间及数量分摊。
 *
 * <p>该对象同时供候选预演、正式结果写入、班次产能扣减和过程日志使用，避免各调用方
 * 再次按“切换完成所在班次”自行计算。时间区间始终使用 {@code [start, end)}。</p>
 *
 * @author APS
 */
public class FirstInspectionShiftAllocation {

    /** 实际覆盖的班次配置。 */
    private final LhShiftConfigVO shift;

    /** 首检区间与当前班次重叠的开始时间（含）。 */
    private final Date overlapStartTime;

    /** 首检区间与当前班次重叠的结束时间（不含）。 */
    private final Date overlapEndTime;

    /** 当前班次真实重叠毫秒数。 */
    private final long overlapMillis;

    /** 按重叠时长折算后的小数余数，用于守恒补差排序。 */
    private final long fractionalRemainder;

    /** 当前班次允许首检占用的最大数量。 */
    private final int capacityLimit;

    /** 当前班次最终分摊的首检数量。 */
    private int quantity;

    /**
     * 构造单班首检分摊。
     *
     * @param shift 班次配置
     * @param overlapStartTime 重叠开始时间
     * @param overlapEndTime 重叠结束时间
     * @param overlapMillis 重叠毫秒数
     * @param fractionalRemainder 折算小数余数
     * @param capacityLimit 班次首检容量上限
     * @param quantity 初始分摊数量
     */
    public FirstInspectionShiftAllocation(LhShiftConfigVO shift,
                                          Date overlapStartTime,
                                          Date overlapEndTime,
                                          long overlapMillis,
                                          long fractionalRemainder,
                                          int capacityLimit,
                                          int quantity) {
        this.shift = shift;
        this.overlapStartTime = overlapStartTime;
        this.overlapEndTime = overlapEndTime;
        this.overlapMillis = overlapMillis;
        this.fractionalRemainder = fractionalRemainder;
        this.capacityLimit = capacityLimit;
        this.quantity = quantity;
    }

    /**
     * 按小数余数顺序补偿一条取整尾差。
     *
     * <p>尾差归属必须只由真实时间占比决定，不能因为某班容量不足而改分给另一个班次。
     * 补差完成后由公共分摊工具统一校验容量；超限时整次准备时间轴后移。</p>
     */
    public void increaseQuantity() {
        quantity++;
    }

    public LhShiftConfigVO getShift() {
        return shift;
    }

    public Date getOverlapStartTime() {
        return overlapStartTime;
    }

    public Date getOverlapEndTime() {
        return overlapEndTime;
    }

    public long getOverlapMillis() {
        return overlapMillis;
    }

    public long getFractionalRemainder() {
        return fractionalRemainder;
    }

    public int getCapacityLimit() {
        return capacityLimit;
    }

    public int getQuantity() {
        return quantity;
    }
}
