package com.zlt.aps.lh.engine.strategy.support;

import com.zlt.aps.lh.api.domain.vo.LhShiftConfigVO;

import java.util.Date;
import java.util.Comparator;
import java.util.Objects;

/**
 * 新增 SKU 候选机台的历史班次剩余产能画像。
 *
 * <p>该对象只描述当前 SKU、当前机台在一次选机回合中的历史剩余产能来源，
 * 不写入机台运行态。标准S4.5在既有目标缺口和匹配等级之前比较来源班次；固定指令、
 * 候选分层、过程日志和正式选机共用同一对象，避免重新读取已经变化的实时排程结果。</p>
 *
 * @author APS
 */
public class HistoricalResidualCapacityInfo {

    /** 剩余产能来源班次。 */
    private final LhShiftConfigVO sourceShift;

    /** 来源班次内首个可连续完成一模的时间。 */
    private final Date availableStartTime;

    /** 来源班次经过开停产管控后的有效结束时间。 */
    private final Date availableEndTime;

    /** 扣减停机、清洗、精度和维修后的净可生产秒数。 */
    private final long netProductiveSeconds;

    /** 按当前 SKU、机台模数和班产折算的剩余可排量。 */
    private final int residualCapacityQty;

    /** 来源班次优先级，数值越小表示来源班次越早。 */
    private final int priorityLevel;

    /**
     * 创建历史班次剩余产能画像。
     *
     * @param sourceShift 剩余产能来源班次
     * @param availableStartTime 首个可连续生产时间
     * @param availableEndTime 有效生产结束时间
     * @param netProductiveSeconds 净可生产秒数
     * @param residualCapacityQty 剩余可排量
     * @param priorityLevel 来源班次优先级
     */
    public HistoricalResidualCapacityInfo(
            LhShiftConfigVO sourceShift,
            Date availableStartTime,
            Date availableEndTime,
            long netProductiveSeconds,
            int residualCapacityQty,
            int priorityLevel) {
        this.sourceShift = sourceShift;
        this.availableStartTime = availableStartTime;
        this.availableEndTime = availableEndTime;
        this.netProductiveSeconds = netProductiveSeconds;
        this.residualCapacityQty = residualCapacityQty;
        this.priorityLevel = priorityLevel;
    }

    /**
     * 共用历史剩余资源来源比较，历史来源优先，同为历史来源时按绝对班次开始时间升序。
     *
     * @param left 左侧冻结的来源，空表示当前资源
     * @param right 右侧冻结的来源，空表示当前资源
     * @return 负数表示左侧优先，零表示继续比较后续规则
     */
    public static int compareSourcePriority(HistoricalResidualCapacityInfo left,
            HistoricalResidualCapacityInfo right) {
        if (Objects.nonNull(left) != Objects.nonNull(right)) {
            return Objects.nonNull(left) ? -1 : 1;
        }
        if (Objects.isNull(left)) {
            return 0;
        }
        return Comparator.nullsLast(Date::compareTo).compare(left.getSourceStartTime(), right.getSourceStartTime());
    }

    /**
     * 读取冻结来源班次的绝对开始时间，供排序与审计共用。
     *
     * @return 来源开始时间，来源班次缺失时为空
     */
    public Date getSourceStartTime() {
        return Objects.isNull(sourceShift) ? null : sourceShift.getShiftStartDateTime();
    }

    public LhShiftConfigVO getSourceShift() {
        return sourceShift;
    }

    public Date getAvailableStartTime() {
        return availableStartTime;
    }

    public Date getAvailableEndTime() {
        return availableEndTime;
    }

    public long getNetProductiveSeconds() {
        return netProductiveSeconds;
    }

    public int getResidualCapacityQty() {
        return residualCapacityQty;
    }

    public int getPriorityLevel() {
        return priorityLevel;
    }
}
