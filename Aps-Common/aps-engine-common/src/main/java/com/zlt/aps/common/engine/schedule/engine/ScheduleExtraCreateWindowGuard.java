package com.zlt.aps.common.engine.schedule.engine;

/**
 * 按已批准消费班数、成型日历及静态口型白名单筛选新行真实或批准外推需求。
 *
 * <p>停放上下限与生产起止时点门禁已按业务确认下线：本门禁不再校验生产节点时间，
 * 只保留需求窗口范围、成型日历开班与口型静态白名单等需求侧规则；备库数量上限由
 * 目标、产能、工装与成型余量额度在数量计算器中约束。</p>
 */
public final class ScheduleExtraCreateWindowGuard {

    /**
     * 按需求窗口核对全部正需求消费段，不改变原需求或原基础排程的保证窗口。
     * 任一正需求段不合格时整行拒绝，防止冻结净请求含有已被排除的需求。
     *
     * @param request 已偏移窗口、日历及静态口型快照
     * @return 可承接损耗前需求及拒绝证据；未知或非法输入不放行
     */
    public ScheduleExtraCreateWindowProof evaluate(ScheduleExtraCreateWindowRequest request) {
        ScheduleExtraCreateWindowProof proof = new ScheduleExtraCreateWindowProof();
        if (request != null) {
            proof.setCurrentFormingLogicalShiftOrder(request.getCurrentFormingLogicalShiftOrder());
            proof.setGuardShiftCount(request.getGuardShiftCount());
            proof.setMouthPlateCode(request.getMouthPlateCode());
        }
        if (request == null || request.getCurrentFormingLogicalShiftOrder() == null
                || request.getCurrentFormingLogicalShiftOrder() <= 0 || request.getGuardShiftCount() == null
                || request.getGuardShiftCount() < 0 || request.getDemandSlices() == null
                || !Boolean.TRUE.equals(request.getProductionWindowFormingCalendarOpen())) {
            proof.getReasonCodes().add("EXTRA_CREATE_WINDOW_UNPROVEN");
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
        // 净请求按整份窗口冻结，不能仅凭部分合格段的总需求允许整行建库。
        proof.setReady(proof.getEligibleDemandQty().signum() > 0 && proof.getReasonCodes().isEmpty());
        if (!proof.isReady() && proof.getReasonCodes().isEmpty()) {
            proof.getReasonCodes().add("EXTRA_CREATE_NO_REAL_DEMAND");
        }
        return proof;
    }

    /**
     * 检查单一消费段的需求侧资格。
     *
     * @param request 同机同版本窗口
     * @param slice   正需求段
     * @return 拒绝码；通过返回null
     */
    private String exclusionReason(ScheduleExtraCreateWindowRequest request, ScheduleExtraCreateDemandSlice slice) {
        if ((!Boolean.TRUE.equals(slice.getRealDemand()) && !Boolean.TRUE.equals(slice.getApprovedDemand()))
                || !Boolean.TRUE.equals(slice.getCalendarOpen())
                || slice.getFormingLogicalShiftOrder() == null) {
            return "EXTRA_CREATE_WINDOW_UNPROVEN";
        }
        long shift = slice.getFormingLogicalShiftOrder();
        long current = request.getCurrentFormingLogicalShiftOrder();
        if (shift < current || shift > current + request.getGuardShiftCount()) {
            return "EXTRA_CREATE_GUARD_WINDOW_EXCEEDED";
        }
        return null;
    }
}
