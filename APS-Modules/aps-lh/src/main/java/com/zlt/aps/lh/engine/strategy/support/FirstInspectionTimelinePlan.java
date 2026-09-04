package com.zlt.aps.lh.engine.strategy.support;

import com.zlt.aps.lh.api.domain.vo.LhShiftConfigVO;
import com.zlt.aps.lh.util.FirstInspectionTimingMode;
import org.apache.commons.lang3.StringUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.util.List;
import java.util.Objects;

/**
 * 新增排产冻结首检时间轴。
 *
 * <p>候选预演和正式提交使用同一对象。对象同时给出首检区间、生产占用起点、
 * 正式生产起点、计数班次和指纹，禁止调用方在提交阶段重新推导。</p>
 */
public final class FirstInspectionTimelinePlan {

    /** 首检分摊计划，保留既有数量、容量和跨班分摊结果。 */
    private final FirstInspectionAllocationPlan allocationPlan;

    /** 集中解析出的时间模式。 */
    private final FirstInspectionTimingMode timingMode;

    /** 模式选择原因。 */
    private final String modeReason;

    /** 切换开始时间。 */
    private final Date changeoverStartTime;

    /** 切换完成时间。 */
    private final Date changeoverEndTime;

    /** 生产就绪或硬性生产门禁时间。 */
    private final Date productionReadyTime;

    /** 正式生产开始时间。 */
    private final Date formalProductionStartTime;

    /** 正式生产所属班次。 */
    private final LhShiftConfigVO formalProductionShift;

    /** 稳定时间轴指纹。 */
    private final String timelineFingerprint;

    private FirstInspectionTimelinePlan(FirstInspectionAllocationPlan allocationPlan,
                                        FirstInspectionTimingMode timingMode,
                                        String modeReason,
                                        Date changeoverStartTime,
                                        Date changeoverEndTime,
                                        Date productionReadyTime,
                                        Date formalProductionStartTime,
                                        LhShiftConfigVO formalProductionShift) {
        this.allocationPlan = allocationPlan;
        this.timingMode = Objects.requireNonNull(timingMode, "timingMode不能为空");
        this.modeReason = StringUtils.defaultString(modeReason);
        this.changeoverStartTime = changeoverStartTime;
        this.changeoverEndTime = changeoverEndTime;
        this.productionReadyTime = productionReadyTime;
        this.formalProductionStartTime = formalProductionStartTime;
        this.formalProductionShift = formalProductionShift;
        this.timelineFingerprint = buildFingerprint();
    }

    /**
     * 创建首检时间轴。
     *
     * @param allocationPlan 首检分摊计划
     * @param timingMode 时间模式
     * @param modeReason 模式原因
     * @param changeoverStartTime 切换开始时间
     * @param changeoverEndTime 切换完成时间
     * @param productionReadyTime 生产就绪时间
     * @param formalProductionStartTime 正式生产开始时间
     * @param formalProductionShift 正式生产班次
     * @return 不可变时间轴
     */
    public static FirstInspectionTimelinePlan of(FirstInspectionAllocationPlan allocationPlan,
                                                 FirstInspectionTimingMode timingMode,
                                                 String modeReason,
                                                 Date changeoverStartTime,
                                                 Date changeoverEndTime,
                                                 Date productionReadyTime,
                                                 Date formalProductionStartTime,
                                                 LhShiftConfigVO formalProductionShift) {
        return new FirstInspectionTimelinePlan(
                allocationPlan, timingMode, modeReason, changeoverStartTime, changeoverEndTime,
                productionReadyTime, formalProductionStartTime, formalProductionShift);
    }

    public FirstInspectionAllocationPlan getAllocationPlan() {
        return allocationPlan;
    }

    public FirstInspectionTimingMode getTimingMode() {
        return timingMode;
    }

    public String getModeReason() {
        return modeReason;
    }

    public Date getChangeoverStartTime() {
        return changeoverStartTime;
    }

    public Date getChangeoverEndTime() {
        return changeoverEndTime;
    }

    public Date getProductionReadyTime() {
        return productionReadyTime;
    }

    public Date getFormalProductionStartTime() {
        return formalProductionStartTime;
    }

    public LhShiftConfigVO getFormalProductionShift() {
        return formalProductionShift;
    }

    public Date getProductionOccupationStartTime() {
        if (hasInspection()) {
            return allocationPlan.getInspectionStartTime();
        }
        return formalProductionStartTime;
    }

    public LhShiftConfigVO getProductionOccupationShift() {
        if (hasInspection()) {
            List<FirstInspectionShiftAllocation> allocations = allocationPlan.getShiftAllocations();
            if (!allocations.isEmpty() && Objects.nonNull(allocations.get(0))) {
                return allocations.get(0).getShift();
            }
        }
        return formalProductionShift;
    }

    public Date getInspectionCountDate() {
        LhShiftConfigVO countShift = getInspectionCountShift();
        return Objects.nonNull(countShift) ? countShift.getWorkDate() : null;
    }

    public LhShiftConfigVO getInspectionCountShift() {
        if (!hasInspection()) {
            return null;
        }
        List<FirstInspectionShiftAllocation> allocations = allocationPlan.getShiftAllocations();
        return allocations.isEmpty() || Objects.isNull(allocations.get(0))
                ? allocationPlan.getCountingShift() : allocations.get(0).getShift();
    }

    public String getTimelineFingerprint() {
        return timelineFingerprint;
    }

    /**
     * 判断时间轴是否对应指定切换区间和模式。
     *
     * @param expectedStartTime 期望切换开始
     * @param expectedEndTime 期望切换完成
     * @param expectedMode 期望时间模式
     * @return true-同一冻结时间轴
     */
    public boolean matches(Date expectedStartTime,
                           Date expectedEndTime,
                           FirstInspectionTimingMode expectedMode) {
        return Objects.equals(expectedMode, timingMode)
                && Objects.equals(expectedStartTime, changeoverStartTime)
                && Objects.equals(expectedEndTime, changeoverEndTime);
    }

    private boolean hasInspection() {
        return Objects.nonNull(allocationPlan)
                && allocationPlan.isValid()
                && allocationPlan.getInspectionQty() > 0;
    }

    /**
     * 使用关键时间、数量、班次和分摊明细生成稳定指纹。
     *
     * @return 64位十六进制指纹
     */
    private String buildFingerprint() {
        StringBuilder source = new StringBuilder(512);
        appendValue(source, timingMode);
        appendValue(source, changeoverStartTime);
        appendValue(source, changeoverEndTime);
        appendValue(source, productionReadyTime);
        appendValue(source, formalProductionStartTime);
        appendValue(source, formalProductionShift);
        if (hasInspection()) {
            appendValue(source, allocationPlan.getInspectionQty());
            appendValue(source, allocationPlan.getHourlyOutput());
            appendValue(source, allocationPlan.getInspectionDurationSeconds());
            appendValue(source, allocationPlan.getInspectionStartTime());
            appendValue(source, allocationPlan.getInspectionEndTime());
            appendValue(source, allocationPlan.getCountingShift());
            allocationPlan.getShiftAllocations().forEach(allocation -> {
                appendValue(source, allocation.getShift());
                appendValue(source, allocation.getOverlapStartTime());
                appendValue(source, allocation.getOverlapEndTime());
                appendValue(source, allocation.getQuantity());
            });
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(source.toString().getBytes(StandardCharsets.UTF_8));
            StringBuilder fingerprint = new StringBuilder(bytes.length * 2);
            for (byte value : bytes) {
                fingerprint.append(String.format("%02x", value));
            }
            return fingerprint.toString();
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("JVM缺少SHA-256算法，无法生成首检时间轴指纹", exception);
        }
    }

    private static void appendValue(StringBuilder source, Object value) {
        if (Objects.isNull(value)) {
            source.append("|null");
            return;
        }
        if (value instanceof Date) {
            source.append('|').append(((Date) value).getTime());
            return;
        }
        if (value instanceof LhShiftConfigVO) {
            LhShiftConfigVO shift = (LhShiftConfigVO) value;
            source.append("|shift:").append(shift.getShiftIndex())
                    .append(':').append(shift.getShiftStartDateTime() == null
                    ? null : shift.getShiftStartDateTime().getTime())
                    .append(':').append(shift.getShiftEndDateTime() == null
                    ? null : shift.getShiftEndDateTime().getTime());
            return;
        }
        source.append('|').append(value);
    }
}
