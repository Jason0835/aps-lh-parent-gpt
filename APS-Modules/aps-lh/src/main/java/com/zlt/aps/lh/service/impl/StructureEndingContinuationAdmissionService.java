package com.zlt.aps.lh.service.impl;

import cn.hutool.core.bean.BeanUtil;

import com.zlt.aps.lh.api.domain.dto.SkuScheduleDTO;
import com.zlt.aps.lh.api.domain.entity.LhMouldChangePlan;
import com.zlt.aps.lh.api.domain.vo.LhShiftConfigVO;
import com.zlt.aps.lh.context.LhScheduleContext;
import com.zlt.aps.lh.engine.strategy.support.DailyNewSpecCandidate;
import com.zlt.aps.lh.engine.strategy.support.DayScheduleContext;
import com.zlt.aps.lh.engine.strategy.support.PreviousAlternateAdditionalMachineDemand;
import com.zlt.aps.lh.engine.strategy.support.PreviousAlternateAdditionalMachinePolicy;
import com.zlt.aps.lh.engine.strategy.support.PreviousAlternatePlanReleaseEvent;
import com.zlt.aps.lh.engine.strategy.support.StructureEndingContinuationState;
import com.zlt.aps.lh.util.LhScheduleTimeUtil;
import com.zlt.aps.lh.util.LhSingleControlMachineUtil;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 截短原机之前只读冻结有效历史增机资格，复用原日计划与指定增机判断。
 *
 * @author Codex
 * @date 2026/10/29
 */
@Service
public class StructureEndingContinuationAdmissionService {
    /**
     * 原新增及指定候选业务资格。
     */
    @Resource
    private NewSpecMaterialEligibilityService materialEligibilityService;
    /**
     * 原指定关系的目标与已排机台统计。
     */
    @Resource
    private PreviousAlternatePlanEligibilityService historicalEligibilityService;

    /**
     * 保存尚未限量时允许增加的历史机台，不提前应用目标量或登记正式消费。
     *
     * @param context 续作分组前的原始结果与需求
     * @param state 已定位结构收尾和原机身份的待冻结状态
     */
    public void freezeHistoricalEligibility(LhScheduleContext context, StructureEndingContinuationState state) {
        SkuScheduleDTO source = state.getSourceSku();
        if (state.isOriginalRemainderEnding()) {
            // 原收尾分组本来就会建立严格目标，前置只读预判不能借尚未登记的时序取得额外例外。
            source = BeanUtil.copyProperties(source, SkuScheduleDTO.class);
            source.setStrictTargetQty(true);
        }
        LinkedHashMap<LocalDate, List<LhShiftConfigVO>> days = LhScheduleTimeUtil.groupByWorkDate(
                LhScheduleTimeUtil.getScheduleShifts(context, context.getScheduleDate()));
        PreviousAlternatePlanReleaseEvent previous = context.getActivePreviousAlternateEvent();
        try {
            for (LhMouldChangePlan plan : context.getEligiblePreviousAlternatePlans()) {
                if (!context.matchesPreviousAlternateAfterMaterial(plan,
                        source.getMaterialCode(), source.getProductStatus())
                        || state.getOriginalPhysicalMachines().contains(
                        LhSingleControlMachineUtil.resolvePhysicalMachineCode(plan.getLhMachineCode()))) {
                    continue;
                }
                PreviousAlternatePlanReleaseEvent event = context.getPreviousAlternateReleaseEventMap()
                        .get(plan.getLhMachineCode());
                if (event == null || event.getPlan() != plan) {
                    event = new PreviousAlternatePlanReleaseEvent();
                    event.setPlan(plan);
                    event.setMachineCode(plan.getLhMachineCode());
                    event.setMaterialCode(plan.getBeforeMaterialCode());
                }
                context.setActivePreviousAlternateEvent(event);
                for (Map.Entry<LocalDate, List<LhShiftConfigVO>> entry : days.entrySet()) {
                    DayScheduleContext day = new DayScheduleContext(entry.getKey(), entry.getValue(), false, false);
                    DailyNewSpecCandidate candidate = materialEligibilityService.previewPreviousAlternateCandidate(
                            context, source, entry.getKey(), plan, plan.getLhMachineCode());
                    historicalEligibilityService.fillMachineDemand(context, entry.getKey(), candidate);
                    PreviousAlternateAdditionalMachineDemand additional =
                            PreviousAlternateAdditionalMachinePolicy.preview(context, day, event, source, candidate);
                    if (additional != null) {
                        state.getHistoricalPlanIds().add(plan.getId());
                        state.getHistoricalAdditionalDemands().put(plan.getId(), additional);
                        break;
                    }
                    if (!candidate.getReasons().isEmpty()
                            && candidate.getTargetMachineCount() > candidate.getScheduledMachineCount()) {
                        state.getHistoricalPlanIds().add(plan.getId());
                        break;
                    }
                }
            }
        } finally {
            context.setActivePreviousAlternateEvent(previous);
        }
    }
}
