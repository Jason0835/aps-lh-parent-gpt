package com.zlt.aps.lh.service.impl;

import com.zlt.aps.lh.api.domain.dto.SkuDailyPlanQuotaDTO;
import com.zlt.aps.lh.api.domain.dto.SkuScheduleDTO;
import com.zlt.aps.lh.api.domain.entity.LhMouldChangePlan;
import com.zlt.aps.lh.api.enums.MouldChangeTypeEnum;
import com.zlt.aps.lh.api.enums.ScheduleTypeEnum;
import com.zlt.aps.lh.api.enums.ShiftEnum;
import com.zlt.aps.lh.api.enums.SkuScheduleSourceTypeEnum;
import com.zlt.aps.lh.component.MonthPlanDateResolver;
import com.zlt.aps.lh.component.SkuDecrementChecker;
import com.zlt.aps.lh.component.TargetScheduleQtyResolver;
import com.zlt.aps.lh.context.LhScheduleContext;
import com.zlt.aps.lh.engine.strategy.support.PendingSkuUnscheduledRule;
import com.zlt.aps.lh.util.PriorityTraceLogHelper;
import com.zlt.aps.lh.util.LhScheduleTimeUtil;
import com.zlt.aps.lh.util.SkuDailyPlanQuotaUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.Objects;

/**
 * 为历史指定的试制量试余量保留独立真实机台需求。
 * <p>月计划原始日量保持不变，承接额度只写入专用候选的运行账本；
 * 同一物料状态只初始化一次，指定提交、跨日续作和未排收口共用中心剩余量。</p>
 */
@Slf4j
@Service
public class PreviousAlternateTrialDemandService {

    @Resource
    private PreviousAlternatePlanEligibilityService eligibilityService;
    @Resource
    private SkuDecrementChecker skuDecrementChecker;
    @Resource
    private TargetScheduleQtyResolver targetScheduleQtyResolver;

    /**
     * 在S4.3无日计划清理前保留精确历史试验承接来源。
     * @param context 当前批次及最近有效历史计划
     * @param sku 尚未被清零的非续作候选
     * @return 已进入独立指定需求时返回true；其余候选继续原准入规则
     */
    public boolean preserve(LhScheduleContext context, SkuScheduleDTO sku) {
        if (!PendingSkuUnscheduledRule.isTrialOrMassTrialSku(sku)
                || sku.getOriginalWindowPlanQty() > 0 || sku.getSurplusQty() <= 0
                || skuDecrementChecker.isDecrementHit(context, sku)) {
            return false;
        }
        eligibilityService.buildPlanIndex(context);
        LhMouldChangePlan plan = context.getEligiblePreviousAlternatePlans().stream()
                .filter(candidate -> context.matchesPreviousAlternateAfterMaterial(
                        candidate, sku.getMaterialCode(), sku.getProductStatus()))
                .filter(candidate -> MouldChangeTypeEnum.containsAnyCode(candidate.getChangeMouldType(),
                        MouldChangeTypeEnum.REGULAR.getCode(), MouldChangeTypeEnum.TYPE_BLOCK.getCode()))
                .filter(candidate -> StringUtils.equals(ShiftEnum.MORNING_SHIFT.getCode(), candidate.getClassIndex())
                        || StringUtils.equals(ShiftEnum.AFTERNOON_SHIFT.getCode(), candidate.getClassIndex()))
                .filter(candidate -> !LhScheduleTimeUtil.clearTime(candidate.getPlanDate())
                        .after(LhScheduleTimeUtil.clearTime(context.getWindowEndDate())))
                .sorted(PreviousAlternatePlanEligibilityService.planOrder()).findFirst().orElse(null);
        if (Objects.isNull(plan)) {
            return false;
        }
        String skuKey = MonthPlanDateResolver.buildMaterialStatusKey(sku.getMaterialCode(), sku.getProductStatus());
        if (context.getPreviousAlternateTrialDemandList().stream().anyMatch(candidate -> StringUtils.equals(skuKey,
                MonthPlanDateResolver.buildMaterialStatusKey(candidate.getMaterialCode(), candidate.getProductStatus())))) {
            return true;
        }
        int remaining = Math.min(sku.getSurplusQty(),
                targetScheduleQtyResolver.previewProductionRemainingQty(context, sku));
        if (remaining <= 0) {
            return false;
        }
        LocalDate demandDate = plan.getPlanDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        SkuDailyPlanQuotaDTO quota = new SkuDailyPlanQuotaDTO();
        quota.setMaterialCode(sku.getMaterialCode());
        quota.setProductionDate(demandDate);
        // 这是指定承接的有效运行日额度，不能回写月计划DAY_N或originalWindowPlanQty。
        quota.setDayPlanQty(remaining);
        quota.setRemainingQty(remaining);
        sku.setDailyPlanQuotaMap(new LinkedHashMap<>());
        sku.getDailyPlanQuotaMap().put(demandDate, quota);
        SkuDailyPlanQuotaUtil.refreshRollingFields(sku.getDailyPlanQuotaMap());
        sku.setSourceType(SkuScheduleSourceTypeEnum.PREVIOUS_ALTERNATE_TRIAL.getCode());
        sku.setScheduleType(ScheduleTypeEnum.NEW_SPEC.getCode());
        sku.setTargetScheduleQty(remaining);
        sku.setRemainingScheduleQty(remaining);
        sku.setPendingQty(remaining);
        sku.setWindowPlanQty(remaining);
        sku.setWindowRemainingPlanQty(remaining);
        sku.setStrictTargetQty(true);
        context.getPreviousAlternateTrialDemandList().add(sku);
        context.registerPreviousAlternateTrialSku(sku);
        context.getAllSkuScheduleDtoMap().put(skuKey, sku);
        String detail = String.format("工厂=%s, 批次=%s, 计划ID=%s, 指定机台=%s, 后料=%s, 状态=%s, "
                        + "原窗口日计划=%d, 承接日期=%s, 承接余量=%d",
                context.getFactoryCode(), context.getBatchNo(), plan.getId(), plan.getLhMachineCode(),
                sku.getMaterialCode(), sku.getProductStatus(), sku.getOriginalWindowPlanQty(), demandDate, remaining);
        log.info("历史指定试制量试需求保留, {}", detail);
        PriorityTraceLogHelper.appendProcessLog(context, "历史指定试制量试需求保留", detail);
        return true;
    }
}
