package com.zlt.aps.lh.engine.strategy.support;

import com.zlt.aps.lh.api.domain.vo.LhShiftConfigVO;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

/**
 * 一次换模或换活字块首检的完整时间分摊计划。
 *
 * <p>首检结束时间等于切换结束时间，开始时间按首检数量和小时产量向前倒推。
 * 计划一旦通过预演，正式排产直接复用其时间、班次和数量，不再二次推导。</p>
 *
 * @author APS
 */
public class FirstInspectionAllocationPlan {

    /** 是否通过时间覆盖、配置和班次容量校验。 */
    private final boolean valid;

    /** 无效原因；有效计划为空。 */
    private final String invalidReason;

    /** 当前事件在计数班次中的首检顺序。 */
    private final int sequence;

    /** 本次首检总条数。 */
    private final int inspectionQty;

    /** 按班产和有效班时长向下取整后的小时产量。 */
    private final int hourlyOutput;

    /** 首检生产时长，按秒向上取整。 */
    private final long inspectionDurationSeconds;

    /** 首检真实开始时间（含）。 */
    private final Date inspectionStartTime;

    /** 首检真实结束时间（不含），等于换模或换活字块结束时间。 */
    private final Date inspectionEndTime;

    /** 沿用项目既有“同班次前2台”计数语义的事件计数班次。 */
    private final LhShiftConfigVO countingShift;

    /** 按真实时间重叠形成的各班次首检分摊。 */
    private final List<FirstInspectionShiftAllocation> shiftAllocations;

    private FirstInspectionAllocationPlan(boolean valid,
                                          String invalidReason,
                                          int sequence,
                                          int inspectionQty,
                                          int hourlyOutput,
                                          long inspectionDurationSeconds,
                                          Date inspectionStartTime,
                                          Date inspectionEndTime,
                                          LhShiftConfigVO countingShift,
                                          List<FirstInspectionShiftAllocation> shiftAllocations) {
        this.valid = valid;
        this.invalidReason = invalidReason;
        this.sequence = sequence;
        this.inspectionQty = inspectionQty;
        this.hourlyOutput = hourlyOutput;
        this.inspectionDurationSeconds = inspectionDurationSeconds;
        this.inspectionStartTime = inspectionStartTime;
        this.inspectionEndTime = inspectionEndTime;
        this.countingShift = countingShift;
        this.shiftAllocations = Collections.unmodifiableList(
                new ArrayList<FirstInspectionShiftAllocation>(shiftAllocations));
    }

    /**
     * 创建有效首检分摊计划。
     *
     * @param sequence 当前事件在计数班次内的序号
     * @param inspectionQty 本次首检总条数
     * @param hourlyOutput 按班产和有效班时长向下取整后的小时产量
     * @param inspectionDurationSeconds 首检真实生产时长（秒）
     * @param inspectionStartTime 首检区间开始时间（含）
     * @param inspectionEndTime 首检区间结束时间（不含）
     * @param countingShift 沿用既有计数语义取得的计数班次
     * @param allocations 按真实时间重叠形成的班次分摊明细
     * @return 只读的有效首检分摊计划
     */
    public static FirstInspectionAllocationPlan valid(int sequence,
                                                      int inspectionQty,
                                                      int hourlyOutput,
                                                      long inspectionDurationSeconds,
                                                      Date inspectionStartTime,
                                                      Date inspectionEndTime,
                                                      LhShiftConfigVO countingShift,
                                                      List<FirstInspectionShiftAllocation> allocations) {
        return new FirstInspectionAllocationPlan(
                true, null, sequence, inspectionQty, hourlyOutput, inspectionDurationSeconds,
                inspectionStartTime, inspectionEndTime, countingShift, allocations);
    }

    /**
     * 创建未通过校验的首检计划。
     *
     * @param invalidReason 未通过时间覆盖、配置或产能校验的明确原因
     * @param countingShift 已解析出的计数班次；无法解析时为 null
     * @param inspectionEndTime 本次切换完成时间
     * @return 不可提交的首检分摊计划
     */
    public static FirstInspectionAllocationPlan invalid(String invalidReason,
                                                        LhShiftConfigVO countingShift,
                                                        Date inspectionEndTime) {
        return new FirstInspectionAllocationPlan(
                false, invalidReason, 0, 0, 0, 0L, null, inspectionEndTime,
                countingShift, Collections.<FirstInspectionShiftAllocation>emptyList());
    }

    public boolean isValid() {
        return valid;
    }

    public String getInvalidReason() {
        return invalidReason;
    }

    public int getSequence() {
        return sequence;
    }

    public int getInspectionQty() {
        return inspectionQty;
    }

    public int getHourlyOutput() {
        return hourlyOutput;
    }

    public long getInspectionDurationSeconds() {
        return inspectionDurationSeconds;
    }

    public Date getInspectionStartTime() {
        return inspectionStartTime;
    }

    public Date getInspectionEndTime() {
        return inspectionEndTime;
    }

    public LhShiftConfigVO getCountingShift() {
        return countingShift;
    }

    public List<FirstInspectionShiftAllocation> getShiftAllocations() {
        return shiftAllocations;
    }
}
