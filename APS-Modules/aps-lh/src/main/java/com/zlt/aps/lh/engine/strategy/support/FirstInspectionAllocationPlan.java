package com.zlt.aps.lh.engine.strategy.support;

import com.zlt.aps.lh.api.domain.vo.LhShiftConfigVO;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Objects;

/**
 * 一次换模或换活字块首检的完整时间分摊计划。
 *
 * <p>计划同时支持共享入口的切换结束前倒推语义，以及新增排产“首检即开产”的生产
 * 就绪后正向语义。计划一旦通过预演，正式排产直接复用其时间、班次和数量，不再二次推导。</p>
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

    /** 按班产和有效班时长精确折算的小时产量。 */
    private final BigDecimal hourlyOutput;

    /** 首检生产时长，按秒向上取整。 */
    private final long inspectionDurationSeconds;

    /** 首检真实开始时间（含）。 */
    private final Date inspectionStartTime;

    /** 首检真实结束时间（不含）。 */
    private final Date inspectionEndTime;

    /** 沿用项目既有“同班次前2台”计数语义的事件计数班次。 */
    private final LhShiftConfigVO countingShift;

    /** 按真实时间重叠形成的各班次首检分摊。 */
    private final List<FirstInspectionShiftAllocation> shiftAllocations;

    /**
     * 首检条数的计量明细；真实资源占用仍读取原时间分摊。
     */
    private final List<FirstInspectionShiftAllocation> quantityAllocations;

    /**
     * 是否按实际切换的单班计量规则构建，独立设备计划保持原规则。
     */
    private final boolean singleShiftQuantity;

    /** 首检已包含在精度及预热完整时长内，不再另排首检量或追加时长。 */
    private final boolean includedInMaintenance;

    /** 公共设备首检所属的有效精度计划主键；普通换模首检为空。 */
    private final Long equipmentPlanId;

    private FirstInspectionAllocationPlan(boolean valid,
                                          String invalidReason,
                                          int sequence,
                                          int inspectionQty,
                                          BigDecimal hourlyOutput,
                                          long inspectionDurationSeconds,
                                          Date inspectionStartTime,
                                          Date inspectionEndTime,
                                          LhShiftConfigVO countingShift,
                                          List<FirstInspectionShiftAllocation> shiftAllocations,
                                          boolean includedInMaintenance) {
        this(valid, invalidReason, sequence, inspectionQty, hourlyOutput, inspectionDurationSeconds,
                inspectionStartTime, inspectionEndTime, countingShift, shiftAllocations, includedInMaintenance, null);
    }

    private FirstInspectionAllocationPlan(boolean valid, String invalidReason, int sequence, int inspectionQty,
            BigDecimal hourlyOutput, long inspectionDurationSeconds, Date inspectionStartTime,
            Date inspectionEndTime, LhShiftConfigVO countingShift,
            List<FirstInspectionShiftAllocation> shiftAllocations, boolean includedInMaintenance, Long equipmentPlanId) {
        this(valid, invalidReason, sequence, inspectionQty, hourlyOutput, inspectionDurationSeconds,
                inspectionStartTime, inspectionEndTime, countingShift, shiftAllocations,
                includedInMaintenance, equipmentPlanId, shiftAllocations);
    }

    /**
     * 冻结真实首检占用及独立的计划条数归属。
     *
     * @param valid 计划是否有效
     * @param invalidReason 无效原因
     * @param sequence 当班首检顺序
     * @param inspectionQty 首检总条数
     * @param hourlyOutput 小时产量
     * @param inspectionDurationSeconds 首检实际秒数
     * @param inspectionStartTime 实际开始时间
     * @param inspectionEndTime 实际结束时间
     * @param countingShift 首检计数班次
     * @param shiftAllocations 真实时间占用明细
     * @param includedInMaintenance 是否包含在维护时长内
     * @param equipmentPlanId 独立设备计划主键
     * @param quantityAllocations 计划条数归属明细
     */
    private FirstInspectionAllocationPlan(boolean valid, String invalidReason, int sequence, int inspectionQty,
            BigDecimal hourlyOutput, long inspectionDurationSeconds, Date inspectionStartTime, Date inspectionEndTime,
            LhShiftConfigVO countingShift, List<FirstInspectionShiftAllocation> shiftAllocations,
            boolean includedInMaintenance, Long equipmentPlanId,
            List<FirstInspectionShiftAllocation> quantityAllocations) {
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
        this.quantityAllocations = Collections.unmodifiableList(new ArrayList<>(quantityAllocations));
        // 默认构造共用同一明细；单班归集显式传入独立数量明细，资源明细始终保留。
        this.singleShiftQuantity = quantityAllocations != shiftAllocations;
        this.includedInMaintenance = includedInMaintenance;
        this.equipmentPlanId = equipmentPlanId;
    }

    /** @param planId 有效判定返回的主键 @return 带精度首检归属的新计划，不改变原计划 */
    public FirstInspectionAllocationPlan withEquipmentPlanId(Long planId) {
        return new FirstInspectionAllocationPlan(valid, invalidReason, sequence, inspectionQty, hourlyOutput,
                inspectionDurationSeconds, inspectionStartTime, inspectionEndTime, countingShift,
                shiftAllocations, false, planId);
    }

    /** @return 精度首检主键；普通生产首检为空 */
    public Long getEquipmentPlanId() {
        return equipmentPlanId;
    }

    /**
     * 创建有效首检分摊计划。
     *
     * @param sequence 当前事件在计数班次内的序号
     * @param inspectionQty 本次首检总条数
     * @param hourlyOutput 按班产和有效班时长精确折算的小时产量
     * @param inspectionDurationSeconds 首检真实生产时长（秒）
     * @param inspectionStartTime 首检区间开始时间（含）
     * @param inspectionEndTime 首检区间结束时间（不含）
     * @param countingShift 沿用既有计数语义取得的计数班次
     * @param allocations 按真实时间重叠形成的班次分摊明细
     * @return 只读的有效首检分摊计划
     */
    public static FirstInspectionAllocationPlan valid(int sequence,
                                                      int inspectionQty,
                                                      BigDecimal hourlyOutput,
                                                      long inspectionDurationSeconds,
                                                      Date inspectionStartTime,
                                                      Date inspectionEndTime,
                                                      LhShiftConfigVO countingShift,
                                                      List<FirstInspectionShiftAllocation> allocations) {
        return new FirstInspectionAllocationPlan(
                true, null, sequence, inspectionQty, hourlyOutput, inspectionDurationSeconds,
                inspectionStartTime, inspectionEndTime, countingShift, allocations, false);
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
                false, invalidReason, 0, 0, BigDecimal.ZERO, 0L, null, inspectionEndTime,
                countingShift, Collections.<FirstInspectionShiftAllocation>emptyList(), false);
    }

    /**
     * 创建精度内已完成首检的计划，保留首检资源计数但不虚构精度期间的生产量。
     * @param sequence 当前计数班次首检顺序
     * @param readyTime 换模与精度完整时长取最大值后的生产就绪时间
     * @param countingShift 就绪时刻所在班次
     * @return 首检包含在精度内的只读计划
     */
    public static FirstInspectionAllocationPlan includedInMaintenance(int sequence, Date readyTime,
                                                                      LhShiftConfigVO countingShift) {
        if (Objects.isNull(readyTime) || Objects.isNull(countingShift)) {
            return invalid("精度并行首检完成时间未命中排程班次", countingShift, readyTime);
        }
        return new FirstInspectionAllocationPlan(true, null, sequence, 0, BigDecimal.ZERO, 0L,
                readyTime, readyTime, countingShift, Collections.<FirstInspectionShiftAllocation>emptyList(), true);
    }

    /** @return 首检是否已包含在精度完整时长中 */
    public boolean isIncludedInMaintenance() {
        return includedInMaintenance;
    }

    /**
     * 是否已有计件首检时间计划。故障可使产量归零，但不能重新触发旧首检或改变原时间安排。
     * @return 有效且原计件时长大于零
     */
    public boolean hasQuantityTimeline() {
        return valid && inspectionDurationSeconds > 0L;
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

    public BigDecimal getHourlyOutput() {
        return hourlyOutput;
    }

    /**
     * 判断完整首检是否在指定可写班次内形成正计划量。
     * 首检结束允许等于班末；无效、不完整或跨出可写窗口的计划不能独立支撑开产。
     * @param shifts 当前允许写入的班次
     * @return 是否存在完整且有正量的首检
     */
    public boolean hasPositiveQuantityInShifts(List<LhShiftConfigVO> shifts) {
        if (!valid || inspectionQty <= 0 || CollectionUtils.isEmpty(shifts)
                || CollectionUtils.isEmpty(quantityAllocations)) {
            return false;
        }
        return quantityAllocations.stream().allMatch(allocation ->
                Objects.nonNull(allocation) && shifts.stream().anyMatch(shift ->
                        Objects.nonNull(shift)
                                && Objects.equals(shift.getShiftIndex(), allocation.getShift().getShiftIndex())
                                && !allocation.getOverlapStartTime().before(shift.getShiftStartDateTime())
                                && !allocation.getOverlapEndTime().after(shift.getShiftEndDateTime())))
                && quantityAllocations.stream().mapToInt(FirstInspectionShiftAllocation::getQuantity).sum()
                == inspectionQty;
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

    /**
     * 获取用于落量、总量限制和账本回裁的首检明细，不能用于推断真实资源占用。
     *
     * @return 只读的计划条数归属明细
     */
    public List<FirstInspectionShiftAllocation> getQuantityAllocations() {
        return quantityAllocations;
    }

    /**
     * 判断是否启用实际切换的单班计量规则。
     *
     * @return 独立计量与真实占用时返回真
     */
    public boolean isSingleShiftQuantity() {
        return singleShiftQuantity;
    }

    /**
     * 判断指定班次是否保留真实首检占用，不使用归集后的计划条数推断。
     *
     * @param shiftIndex 班次索引
     * @return 实际首检在该班形成产出时返回真
     */
    public boolean hasActualOccupation(int shiftIndex) {
        return valid && shiftAllocations.stream().anyMatch(allocation -> allocation.getQuantity() > 0
                && Objects.equals(allocation.getShift().getShiftIndex(), shiftIndex));
    }

    /**
     * 将完整首检条数归集到指定计量班次，参数计数、真实区间与逐班占用保持不变。
     *
     * @param attributionShift 独立于原参数计数的计划条数归属班次
     * @return 保留真实占用、仅在一个班次记量的首检计划
     */
    public FirstInspectionAllocationPlan withSingleShiftQuantity(LhShiftConfigVO attributionShift) {
        if (!valid || Objects.isNull(attributionShift)) {
            return this;
        }
        Date start = inspectionStartTime.before(attributionShift.getShiftStartDateTime())
                ? attributionShift.getShiftStartDateTime() : inspectionStartTime;
        Date end = inspectionEndTime.after(attributionShift.getShiftEndDateTime())
                ? attributionShift.getShiftEndDateTime() : inspectionEndTime;
        FirstInspectionShiftAllocation quantity = new FirstInspectionShiftAllocation(attributionShift,
                start, end, Math.max(0L, end.getTime() - start.getTime()), 0L, inspectionQty, inspectionQty);
        return new FirstInspectionAllocationPlan(valid, invalidReason, sequence, inspectionQty, hourlyOutput,
                inspectionDurationSeconds, inspectionStartTime, inspectionEndTime, countingShift, shiftAllocations,
                includedInMaintenance, equipmentPlanId, Collections.singletonList(quantity));
    }
}
