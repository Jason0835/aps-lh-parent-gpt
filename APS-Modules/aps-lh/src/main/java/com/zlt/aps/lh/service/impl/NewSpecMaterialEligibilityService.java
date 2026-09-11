package com.zlt.aps.lh.service.impl;

import com.zlt.aps.lh.api.domain.dto.SkuDailyPlanQuotaDTO;
import com.zlt.aps.lh.api.domain.dto.SkuScheduleDTO;
import com.zlt.aps.lh.api.domain.entity.LhUnscheduledResult;
import com.zlt.aps.lh.api.enums.SkuScheduleSourceTypeEnum;
import com.zlt.aps.lh.component.EarlyProductionQuantityCalculator;
import com.zlt.aps.lh.component.EarlyProductionRuntimePlanService;
import com.zlt.aps.lh.component.MonthPlanDateResolver;
import com.zlt.aps.lh.component.SkuDecrementChecker;
import com.zlt.aps.lh.component.TargetScheduleQtyResolver;
import com.zlt.aps.lh.context.LhScheduleContext;
import com.zlt.aps.lh.engine.strategy.IEndingJudgmentStrategy;
import com.zlt.aps.lh.engine.strategy.support.DailyCandidateReason;
import com.zlt.aps.lh.engine.strategy.support.DailyMachineExpansionPlanner;
import com.zlt.aps.lh.engine.strategy.support.DailyNewSpecCandidate;
import com.zlt.aps.lh.engine.strategy.support.EarlyProductionChecker;
import com.zlt.aps.lh.engine.strategy.support.EarlyProductionRuntimePlan;
import com.zlt.aps.lh.engine.strategy.support.PendingSkuUnscheduledRule;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.time.LocalDate;
import java.util.Objects;

/**
 * 本次新增物料资格公共服务。
 * <p>只读取日计划、生产余量、产品状态和提前生产中心规则，不查询机台、不比较尺寸或排序。
 * 日驱动候选和前置指定组合共用这些规则，历史关系本身不授予排产资格。</p>
 */
@Service
public class NewSpecMaterialEligibilityService {

    @Resource
    private EarlyProductionRuntimePlanService earlyProductionRuntimePlanService;
    @Resource
    private TargetScheduleQtyResolver targetScheduleQtyResolver;
    @Resource
    private IEndingJudgmentStrategy endingJudgmentStrategy;
    @Resource
    private SkuDecrementChecker skuDecrementChecker;

    /**
     * 读取原始日计划，保持现有月计划与日账本取数口径。
     * @param context 排程上下文
     * @param sku 物料及产品状态
     * @param date 原始计划日期
     * @return 原始日计划量
     */
    public int resolveOriginalDayPlanQty(LhScheduleContext context, SkuScheduleDTO sku, LocalDate date) {
        int dayPlanQty = MonthPlanDateResolver.resolveDayQty(
                context, sku.getMaterialCode(), sku.getProductStatus(), date);
        if (dayPlanQty > 0 || CollectionUtils.isEmpty(sku.getDailyPlanQuotaMap())) {
            return dayPlanQty;
        }
        SkuDailyPlanQuotaDTO quota = sku.getDailyPlanQuotaMap().get(date);
        return Objects.isNull(quota) ? 0 : Math.max(0, quota.getDayPlanQty());
    }

    /**
     * 读取实时日计划余额，不重新初始化已被前序阶段消费的额度。
     * @param sku 物料
     * @param date 实际业务日
     * @return 当日剩余量
     */
    public int resolveDailyRemainingQty(SkuScheduleDTO sku, LocalDate date) {
        if (Objects.isNull(sku) || Objects.isNull(date)
                || CollectionUtils.isEmpty(sku.getDailyPlanQuotaMap())) {
            return 0;
        }
        SkuDailyPlanQuotaDTO quota = sku.getDailyPlanQuotaMap().get(date);
        return Objects.isNull(quota) ? 0 : Math.max(0, quota.getRemainingQty());
    }

    /**
     * 判断续作衍生的增机需求身份，避免补偿副本被当作普通首次新增。
     * @param sku 待排物料
     * @return 是否为续作增机候选
     */
    public boolean isContinuationAddMachineCandidate(SkuScheduleDTO sku) {
        return Objects.nonNull(sku) && sku.isContinuousCompensationSku()
                && SkuScheduleSourceTypeEnum.isContinuationAddMachine(sku.getSourceType());
    }

    /**
     * 复用提前生产中心的只读视图，不把历史日期作为提前生产来源日期。
     * @param context 排程上下文
     * @param sku 待排物料
     * @param date 实际业务日
     * @return 中心计算的提前生产视图
     */
    public EarlyProductionRuntimePlan previewEarlyProduction(
            LhScheduleContext context, SkuScheduleDTO sku, LocalDate date) {
        return earlyProductionRuntimePlanService.previewRuntimePlan(context, sku, date);
    }

    /**
     * 为已确定机台的排产构建物料资格；失败仅返回原因，不清理待排物料或写最终未排。
     * @param context 排程上下文
     * @param sku 原始物料对象，补偿副本保持各自身份
     * @param date 本次实际业务日期
     * @return 带正常或提前生产来源的候选；无来源时 lastFailure 为拒绝原因
     */
    public DailyNewSpecCandidate resolveCandidate(
            LhScheduleContext context, SkuScheduleDTO sku, LocalDate date) {
        DailyNewSpecCandidate candidate = new DailyNewSpecCandidate(
                MonthPlanDateResolver.buildMaterialStatusKey(sku.getMaterialCode(), sku.getProductStatus()), sku);
        LhUnscheduledResult trialExclusion = PendingSkuUnscheduledRule.evaluateNewSpecTrialExclusion(context, sku);
        if (Objects.nonNull(trialExclusion) || skuDecrementChecker.isDecrementHit(context, sku)) {
            candidate.setLastFailure(Objects.nonNull(trialExclusion)
                    ? trialExclusion.getUnscheduledReason() : "物料命中减量清单");
            return candidate;
        }
        int originalQty = this.resolveOriginalDayPlanQty(context, sku, date);
        int remainingQty = this.resolveDailyRemainingQty(sku, date);
        candidate.setOriginalDayPlanQty(originalQty);
        candidate.setRealtimeDayPlanRemainingQty(remainingQty);
        boolean continuation = this.isContinuationAddMachineCandidate(sku);
        boolean continuationEarly = EarlyProductionChecker.isEligibleContinuationAddMachineEarlyProduction(
                context, sku, date);
        boolean continuationDue = !continuation || Objects.isNull(sku.getFirstAddMachineProductionDate())
                || !sku.getFirstAddMachineProductionDate().isAfter(date);
        boolean normalDemand = continuation ? continuationDue : originalQty > 0 && remainingQty > 0;
        if (!context.isFutureOnlyEarlyProductionCandidate(sku) && normalDemand && !continuationEarly) {
            candidate.addReason(continuation ? DailyCandidateReason.ADD_MACHINE_REQUIREMENT
                    : DailyCandidateReason.TODAY_PLAN);
            candidate.setTargetPlanDate(date);
        } else if (originalQty <= 0 || continuationEarly || context.isFutureOnlyEarlyProductionCandidate(sku)) {
            EarlyProductionRuntimePlan plan = this.previewEarlyProduction(context, sku, date);
            candidate.setEarlyProductionPreview(plan);
            if (Objects.nonNull(plan) && Objects.nonNull(plan.getDecision())
                    && plan.getDecision().isEarlyProduction() && plan.getDecision().isAllowed()
                    && plan.getEffectiveTargetQty() > 0
                    && (plan.isActive() || !CollectionUtils.isEmpty(plan.getShiftedDailyPlanQuotaMap()))) {
                candidate.addReason(DailyCandidateReason.EARLY_PRODUCTION);
                candidate.setTargetPlanDate(plan.getDecision().getFuturePlanDate());
            }
        }
        if (candidate.getReasons().isEmpty()) {
            candidate.setLastFailure("物料未进入当前业务日正常需求或获准的提前生产范围");
            return candidate;
        }
        LhUnscheduledResult exclusion = this.evaluatePendingRule(context, candidate);
        int productionRemainingQty = Objects.nonNull(candidate.getEarlyProductionPreview())
                ? candidate.getEarlyProductionPreview().getEffectiveTargetQty()
                : targetScheduleQtyResolver.previewProductionRemainingQty(context, sku);
        if (Objects.nonNull(exclusion) || productionRemainingQty <= 0) {
            candidate = new DailyNewSpecCandidate(candidate.getSkuKey(), sku);
            candidate.setLastFailure(Objects.nonNull(exclusion)
                    ? exclusion.getUnscheduledReason() : "物料本次生产余量已耗尽");
        }
        return candidate;
    }

    /**
     * 共用机台无关的前置未排规则，计算过程不修改库存和目标量。
     * @param context 排程上下文
     * @param candidate 当前候选及已批准的提前生产预览
     * @return 既有规则拒绝结果；为空表示通过
     */
    public LhUnscheduledResult evaluatePendingRule(LhScheduleContext context, DailyNewSpecCandidate candidate) {
        SkuScheduleDTO sku = candidate.getSku();
        boolean ending = endingJudgmentStrategy.isCurrentWindowEnding(context, sku)
                || DailyMachineExpansionPlanner.isForceEndingByNoFuturePlan(context, sku);
        boolean embryoEnding = targetScheduleQtyResolver.isEmbryoStockEnding(context, sku);
        int ruleQty = Objects.nonNull(candidate.getEarlyProductionPreview())
                ? Math.max(0, candidate.getEarlyProductionPreview().getEffectiveTargetQty())
                : EarlyProductionQuantityCalculator.resolveSmallEndingRuleQty(context, sku, targetScheduleQtyResolver);
        return PendingSkuUnscheduledRule.evaluate(context, sku, ending, embryoEnding, ruleQty);
    }
}
