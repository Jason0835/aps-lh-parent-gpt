package com.zlt.aps.common.engine.schedule.engine;

import com.zlt.aps.common.core.utils.BigDecimalUtils;

import java.math.BigDecimal;

/**
 * 按已批准消费班数、最大停放及静态口型白名单筛选新行真实或批准外推需求。
 */
public final class ScheduleExtraCreateWindowGuard {

    /**
     * 解析工厂级最大停放配置；重复、非数字和负数均未知，禁止误当0关闭。
     *
     * @param parameter 当前工厂已冻结并填入默认值的参数，不可为空
     * @return 非负小时；非法返回null，由窗口门禁拒绝
     */
    public static BigDecimal resolveMaximumStandingHours(ScheduleParamValueModel parameter) {
        if (parameter.getConfiguredValues() != null && parameter.getConfiguredValues().size() > 1) {
            return null;
        }
        String configured = parameter.getEffectiveValue();
        try {
            BigDecimal hours = BigDecimalUtils.valueOf(configured.trim());
            return hours.signum() < 0 ? null : hours;
        } catch (NumberFormatException invalidConfiguration) {
            // 返回未知，不将坏配置吞成“无限期”；领域事件保留原始值并说明拒绝原因。
            return null;
        }
    }

    /**
     * 按真实时点过滤消费段，不改变原需求或原基础排程的保证窗口。
     * 最大停放按生产开始至该段消费结束，最小停放按生产完成至消费开始，边界相等允许。
     *
     * @param request 逐机试排后的已偏移窗口、日历及静态口型快照
     * @return 可承接损耗前需求及拒绝证据；未知或非法输入不放行
     */
    public ScheduleExtraCreateWindowProof evaluate(ScheduleExtraCreateWindowRequest request) {
        ScheduleExtraCreateWindowProof proof = new ScheduleExtraCreateWindowProof();
        if (request != null) {
            proof.setCurrentFormingLogicalShiftOrder(request.getCurrentFormingLogicalShiftOrder());
            proof.setGuardShiftCount(request.getGuardShiftCount());
            proof.setMinimumStandingHours(request.getMinimumStandingHours());
            proof.setMaximumStandingHours(request.getMaximumStandingHours());
            proof.setMaximumStandingEnabled(request.getMaximumStandingHours() != null
                    && request.getMaximumStandingHours().signum() > 0);
            proof.setProductionStartTime(request.getProductionStartTime());
            proof.setProductionEndTime(request.getProductionEndTime());
            proof.setMouthPlateCode(request.getMouthPlateCode());
        }
        if (request == null || request.getCurrentFormingLogicalShiftOrder() == null
                || request.getCurrentFormingLogicalShiftOrder() <= 0 || request.getGuardShiftCount() == null
                || request.getGuardShiftCount() < 0 || request.getProductionStartTime() == null
                || request.getProductionEndTime() == null
                || request.getProductionEndTime() < request.getProductionStartTime()
                || request.getMinimumStandingHours() == null || request.getMinimumStandingHours().signum() < 0
                || request.getMaximumStandingHours() == null || request.getMaximumStandingHours().signum() < 0
                || request.getDemandSlices() == null
                || !Boolean.TRUE.equals(request.getProductionWindowFormingCalendarOpen())) {
            proof.getReasonCodes().add("EXTRA_CREATE_TIME_WINDOW_UNPROVEN");
            return proof;
        }
        if (request.getMouthPlateCode() == null || request.getMouthPlateCode().trim().isEmpty()
                || request.getAllowedMouthPlateCodes() == null
                || !request.getAllowedMouthPlateCodes().contains(request.getMouthPlateCode())) {
            proof.getReasonCodes().add("EXTRA_CREATE_MOUTH_NOT_ALLOWED");
            return proof;
        }
        for (ScheduleExtraCreateDemandSlice slice : request.getDemandSlices()) {
            if (slice == null || slice.getDemandQty() == null || slice.getDemandQty().signum() <= 0) {
                continue;
            }
            String reason = this.exclusionReason(request, slice);
            if (reason == null) {
                proof.setEligibleDemandQty(proof.getEligibleDemandQty().add(slice.getDemandQty()));
                proof.getEligibleFormingShiftOrders().add(slice.getFormingLogicalShiftOrder());
            } else {
                proof.setExcludedDemandQty(proof.getExcludedDemandQty().add(slice.getDemandQty()));
                proof.getReasonCodes().add(reason);
            }
        }
        proof.setReady(proof.getEligibleDemandQty().signum() > 0);
        if (!proof.isReady() && proof.getReasonCodes().isEmpty()) {
            proof.getReasonCodes().add("EXTRA_CREATE_NO_REAL_DEMAND");
        }
        return proof;
    }

    /**
     * 检查单一消费段；小时转毫秒使用BigDecimal避免小数小时截断和long乘法溢出。
     *
     * @param request 同机同版本窗口
     * @param slice   正需求段
     * @return 拒绝码；通过返回null
     */
    private String exclusionReason(ScheduleExtraCreateWindowRequest request, ScheduleExtraCreateDemandSlice slice) {
        if ((!Boolean.TRUE.equals(slice.getRealDemand()) && !Boolean.TRUE.equals(slice.getApprovedDemand()))
                || !Boolean.TRUE.equals(slice.getCalendarOpen())
                || slice.getFormingLogicalShiftOrder() == null || slice.getConsumptionStartTime() == null) {
            return "EXTRA_CREATE_TIME_WINDOW_UNPROVEN";
        }
        if ((slice.getConsumptionEndTime() != null
                && slice.getConsumptionEndTime() < slice.getConsumptionStartTime())
                || (request.getMaximumStandingHours().signum() > 0 && slice.getConsumptionEndTime() == null)) {
            return "EXTRA_CREATE_TIME_WINDOW_UNPROVEN";
        }
        long shift = slice.getFormingLogicalShiftOrder();
        long current = request.getCurrentFormingLogicalShiftOrder();
        if (shift < current || shift > current + request.getGuardShiftCount()) {
            return "EXTRA_CREATE_GUARD_WINDOW_EXCEEDED";
        }
        BigDecimal consumption = BigDecimalUtils.valueOf(slice.getConsumptionStartTime());
        BigDecimal minimumWait = request.getMinimumStandingHours().multiply(BigDecimalUtils.valueOf(3600000));
        if (consumption.subtract(BigDecimalUtils.valueOf(request.getProductionEndTime())).compareTo(minimumWait) < 0) {
            return "EXTRA_CREATE_MINIMUM_STANDING_NOT_MET";
        }
        BigDecimal maximumWait = request.getMaximumStandingHours().multiply(BigDecimalUtils.valueOf(3600000));
        if (request.getMaximumStandingHours().signum() > 0
                && BigDecimalUtils.valueOf(slice.getConsumptionEndTime())
                .subtract(BigDecimalUtils.valueOf(request.getProductionStartTime())).compareTo(maximumWait) > 0) {
            return "EXTRA_CREATE_MAXIMUM_STANDING_EXCEEDED";
        }
        return null;
    }
}
