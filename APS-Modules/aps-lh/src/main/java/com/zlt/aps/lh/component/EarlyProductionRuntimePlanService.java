package com.zlt.aps.lh.component;

import com.zlt.aps.lh.api.domain.dto.SkuDailyPlanQuotaDTO;
import com.zlt.aps.lh.api.domain.dto.SkuScheduleDTO;
import com.zlt.aps.lh.api.domain.vo.LhShiftConfigVO;
import com.zlt.aps.lh.api.enums.NewSpecFailReasonEnum;
import com.zlt.aps.lh.context.LhScheduleContext;
import com.zlt.aps.lh.engine.strategy.IMouldChangeBalanceStrategy;
import com.zlt.aps.lh.engine.strategy.support.DailyMachineExpansionPlanner;
import com.zlt.aps.lh.engine.strategy.support.DailyNewSpecOrderLogEntry;
import com.zlt.aps.lh.engine.strategy.support.EarlyProductionChecker;
import com.zlt.aps.lh.engine.strategy.support.EarlyProductionDecision;
import com.zlt.aps.lh.engine.strategy.support.EarlyProductionDecisionLogCollector;
import com.zlt.aps.lh.engine.strategy.support.EarlyProductionDecisionLogEntry;
import com.zlt.aps.lh.engine.strategy.support.EarlyProductionLogReason;
import com.zlt.aps.lh.engine.strategy.support.EarlyProductionRuntimePlan;
import com.zlt.aps.lh.engine.strategy.support.MouldResourceAllocationResult;
import com.zlt.aps.lh.engine.strategy.support.NewSpecSelectionRealtimeSnapshot;
import com.zlt.aps.lh.exception.ScheduleErrorCode;
import com.zlt.aps.lh.exception.ScheduleException;
import com.zlt.aps.lh.util.PriorityTraceLogHelper;
import com.zlt.aps.lh.util.SkuDailyPlanQuotaUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 提前生产中心化运行视图服务。
 *
 * <p>该服务统一负责 S4.4 换活字块和 S4.5 新增排产的提前生产准入、未来计划量前移、
 * 硫化余量目标初始化及运行态账本注册。两个排产阶段必须读取同一份运行视图，禁止分别
 * 创建临时 dayN 或重置实际消费账本，避免提前生产后在原计划日重复排产。</p>
 *
 * <p>本服务只修改本批排程内存态，不回写月计划、原始日计划或数据库。</p>
 *
 * @author APS
 */
@Slf4j
@Component
public class EarlyProductionRuntimePlanService {

    /** 提前生产实际消费账本、胎胚有效 SKU 和共用胎胚分配统一入口。 */
    @Resource
    private TargetScheduleQtyResolver targetScheduleQtyResolver;
    /** 结构当天最后班次已排机台数及班内收尾排除的统一统计入口。 */
    @Resource
    private StructureMinMachineRetentionService structureMinMachineRetentionService;
    /** 换模/换活字块统一计数入口，用于冻结提前生产判断时点的实时班次次数。 */
    @Resource
    private IMouldChangeBalanceStrategy mouldChangeBalanceStrategy;

    /**
     * 准备并注册指定业务日的提前生产运行视图。
     *
     * <p>同一 SKU、同一业务日已经激活时直接复用，防止 S4.4 多台候选重复初始化目标量；
     * 业务日变化时仍按现有日驱动规则重新生成当日临时前移账本。</p>
     *
     * @param context 排程上下文
     * @param sku 待提前生产 SKU
     * @param currentDate 实际尝试开产的业务日期
     * @return 提前生产运行视图；不属于提前生产范围时返回 null
     */
    public EarlyProductionRuntimePlan prepareRuntimePlan(
            LhScheduleContext context,
            SkuScheduleDTO sku,
            LocalDate currentDate) {
        if (Objects.isNull(context) || Objects.isNull(sku) || Objects.isNull(currentDate)) {
            return null;
        }
        LocalDate windowStartDate = this.resolveScheduleWindowStartDate(context);
        LocalDate windowEndDate = this.resolveScheduleWindowEndDate(context);
        if (Objects.isNull(windowStartDate) || Objects.isNull(windowEndDate)
                || currentDate.isBefore(windowStartDate) || currentDate.isAfter(windowEndDate)) {
            return null;
        }
        EarlyProductionRuntimePlan runtimePlan = context.getEarlyProductionRuntimePlan(sku);
        if (Objects.nonNull(runtimePlan) && runtimePlan.isActive()
                && currentDate.equals(runtimePlan.getCurrentDate())) {
            log.info("提前生产中心运行视图复用, factoryCode: {}, batchNo: {}, materialCode: {}, "
                            + "currentDate: {}, futurePlanDate: {}, remainingQty: {}",
                    context.getFactoryCode(), context.getBatchNo(), sku.getMaterialCode(),
                    currentDate, runtimePlan.getFuturePlanDate(),
                    targetScheduleQtyResolver.resolveProductionRemainingQty(context, sku));
            return runtimePlan;
        }

        /*
         * S4.4 换活字块提前生产与 S4.5 新规格提前生产统一从提前生产中心申请运行视图。
         * 调用共享准入检查时，只有原始机台数命中结构切换提前，才校验同结构是否存在
         * “有效最早胎胚可供硫化时间”；若同结构已有有效续作排产，则关闭该时间约束。
         * 普通结构提前和结构收尾提前继续执行原有条件。
         * 本服务不复制场景识别、胎胚时间获取或计算逻辑，避免两个入口形成不同口径。
         */
        EarlyProductionDecision decision = this.evaluateEarlyProductionAdmission(
                context, sku, currentDate, windowStartDate, windowEndDate);
        if (Objects.isNull(decision) || !decision.isEarlyProduction()
                || Objects.isNull(decision.getFuturePlanDate())) {
            return this.keepFutureOnlyCandidateInactive(
                    context, sku, currentDate, runtimePlan, decision);
        }

        return this.initializeRuntimePlan(
                context, sku, currentDate, windowStartDate, windowEndDate,
                runtimePlan, decision);
    }

    /**
     * 只读评估指定业务日的提前生产准入，不初始化目标量或临时日计划账本。
     *
     * <p>S4.4 结构切换提前可能在理论开产业务日无计划，但受胎胚时间下限约束后实际
     * 落到有计划日。此时仍需按理论业务日核验全部原提前生产条件，却不应为有计划的
     * 实际生产日创建前移账本，因此提供本只读入口复用与运行视图相同的共享判断器。</p>
     *
     * @param context 排程上下文
     * @param sku 待判断 SKU
     * @param currentDate 需要判断的理论开产业务日
     * @return 共享提前生产准入结论；参数或排程窗口无效时返回明确的不放行结论
     */
    public EarlyProductionDecision evaluateEarlyProductionAdmission(
            LhScheduleContext context,
            SkuScheduleDTO sku,
            LocalDate currentDate) {
        LocalDate windowStartDate = Objects.isNull(context)
                ? null : this.resolveScheduleWindowStartDate(context);
        LocalDate windowEndDate = Objects.isNull(context)
                ? null : this.resolveScheduleWindowEndDate(context);
        return this.evaluateEarlyProductionAdmission(
                context, sku, currentDate, windowStartDate, windowEndDate);
    }

    /**
     * 使用已解析的排程窗口执行共享提前生产准入判断。
     *
     * @param context 排程上下文
     * @param sku 待判断 SKU
     * @param currentDate 当前业务日
     * @param windowStartDate 排程窗口开始业务日
     * @param windowEndDate 排程窗口结束业务日
     * @return 共享提前生产准入结论
     */
    private EarlyProductionDecision evaluateEarlyProductionAdmission(
            LhScheduleContext context,
            SkuScheduleDTO sku,
            LocalDate currentDate,
            LocalDate windowStartDate,
            LocalDate windowEndDate) {
        if (Objects.isNull(context) || Objects.isNull(sku) || Objects.isNull(currentDate)) {
            return EarlyProductionDecision.notEarlyProduction(
                    false, "提前生产准入参数不完整");
        }
        if (Objects.isNull(windowStartDate) || Objects.isNull(windowEndDate)
                || currentDate.isBefore(windowStartDate) || currentDate.isAfter(windowEndDate)) {
            return EarlyProductionDecision.notEarlyProduction(
                    false, "提前生产业务日超出排程窗口");
        }
        EarlyProductionDecision decision = EarlyProductionChecker.checkEarlyProduction(
                context, sku, currentDate, windowStartDate, windowEndDate,
                DailyMachineExpansionPlanner.resolveShortageAddMachineThreshold(context));
        if (Objects.isNull(decision) || !decision.isEarlyProduction()
                || !decision.isAllowed() || Objects.isNull(decision.getFuturePlanDate())) {
            return decision;
        }
        StructureEarlyProductionAdmission admission =
                this.evaluateStructureDailyAdmission(
                        context, sku, currentDate,
                        decision.getFuturePlanDate());
        if (Objects.isNull(admission) || !admission.isAllowed()) {
            String reason = Objects.isNull(admission)
                    ? "结构当天提前生产资格无法生成，禁止提前生产"
                    : admission.getReason();
            return EarlyProductionDecision.earlyProduction(
                    false, decision.getSceneType(), decision.getFuturePlanDate(),
                    decision.getStructurePlanMachineCounts(), reason);
        }
        decision.setReason("结构已取得当天提前生产资格，继续使用当天其他班次剩余资源");
        return decision;
    }

    /**
     * 评估并固化结构当天唯一的提前生产资格。
     *
     * <p>资格只读取当天最后一个班次。首次判断后按“业务日期+结构”保存快照；同结构后续
     * SKU、换活字块、新增候选及跨日在机续排均复用该结论，不再因早班或中班的结构机台数
     * 达到计划值而重新拦截。计划机台数继续复用现有结构切换规则：当前日为0时读取提前
     * 生产来源日，不改变原场景识别和胎胚时间门禁。</p>
     *
     * @param context 排程上下文
     * @param sku 待提前生产SKU
     * @param currentDate 当前业务日期
     * @param futurePlanDate 提前生产来源计划日
     * @return 结构当天提前生产资格
     */
    public StructureEarlyProductionAdmission evaluateStructureDailyAdmission(
            LhScheduleContext context,
            SkuScheduleDTO sku,
            LocalDate currentDate,
            LocalDate futurePlanDate) {
        if (Objects.isNull(context) || Objects.isNull(sku)
                || Objects.isNull(currentDate)
                || StringUtils.isEmpty(sku.getStructureName())) {
            return null;
        }
        StructureEarlyProductionAdmission cachedAdmission =
                context.getStructureEarlyProductionAdmission(
                        currentDate, sku.getStructureName());
        if (Objects.nonNull(cachedAdmission)) {
            return cachedAdmission;
        }
        LhShiftConfigVO admissionShift =
                this.resolveLastBusinessDayShift(context, currentDate);
        StructureEarlyProductionAdmission admission =
                Objects.isNull(admissionShift)
                        ? new StructureEarlyProductionAdmission()
                        : structureMinMachineRetentionService
                        .resolveEarlyProductionMachineStatistics(
                                context, sku.getStructureName(), admissionShift);
        admission.setBusinessDate(currentDate);
        admission.setStructureName(sku.getStructureName());
        int originalCurrentPlanMachineCount =
                context.getStructurePlanMachineCount(
                        currentDate, sku.getStructureName());
        LocalDate planSourceDate = originalCurrentPlanMachineCount > 0
                ? currentDate : futurePlanDate;
        int currentPlanMachineCount =
                EarlyProductionChecker.resolveEffectiveStructurePlanMachineCount(
                        context, sku, currentDate, futurePlanDate);
        admission.setPlanSourceDate(planSourceDate);
        admission.setCurrentPlanMachineCount(currentPlanMachineCount);
        String reason;
        boolean allowed;
        if (Objects.isNull(admissionShift)) {
            allowed = false;
            reason = "当天最后一个班次无法解析，禁止提前生产";
        } else if (currentPlanMachineCount <= 0) {
            allowed = false;
            reason = "结构无有效计划机台数，禁止提前生产";
        } else if (admission.getScheduledStructureCount()
                >= currentPlanMachineCount) {
            allowed = false;
            reason = "当天最后一个班次结构已排硫化机台数已达到或超过计划机台数，禁止提前生产";
        } else {
            allowed = true;
            reason = "当天最后一个班次结构已排硫化机台数未达到计划机台数，取得当天提前生产资格";
        }
        admission.setAllowed(allowed);
        admission.setReason(reason);
        context.registerStructureEarlyProductionAdmission(admission);
        this.logStructureDailyAdmission(
                context, sku, futurePlanDate, admission);
        return context.getStructureEarlyProductionAdmission(
                currentDate, sku.getStructureName());
    }

    /**
     * 解析指定业务日用于提前生产资格判断的最后一个班次。
     *
     * @param context 排程上下文
     * @param businessDate 业务日期
     * @return 当天最后班次；无法解析返回null
     */
    private LhShiftConfigVO resolveLastBusinessDayShift(
            LhScheduleContext context,
            LocalDate businessDate) {
        if (Objects.isNull(context) || Objects.isNull(businessDate)
                || CollectionUtils.isEmpty(context.getScheduleWindowShifts())) {
            return null;
        }
        LhShiftConfigVO lastShift = null;
        for (LhShiftConfigVO shift : context.getScheduleWindowShifts()) {
            if (Objects.isNull(shift) || Objects.isNull(shift.getWorkDate())) {
                continue;
            }
            LocalDate shiftWorkDate = shift.getWorkDate().toInstant()
                    .atZone(ZoneId.systemDefault()).toLocalDate();
            if (businessDate.equals(shiftWorkDate)) {
                lastShift = shift;
            }
        }
        return lastShift;
    }

    /**
     * 记录结构当天提前生产资格及班内收尾排除明细。
     *
     * @param context 排程上下文
     * @param sku 当前SKU
     * @param futurePlanDate 提前生产来源计划日
     * @param admission 结构当天资格快照
     */
    private void logStructureDailyAdmission(
            LhScheduleContext context,
            SkuScheduleDTO sku,
            LocalDate futurePlanDate,
            StructureEarlyProductionAdmission admission) {
        String detail = new StringBuilder(512)
                .append("factoryCode=").append(context.getFactoryCode())
                .append(", batchNo=").append(PriorityTraceLogHelper.safeText(
                        context.getBatchNo()))
                .append(", businessDate=").append(admission.getBusinessDate())
                .append(", futurePlanDate=").append(futurePlanDate)
                .append(", planSourceDate=").append(admission.getPlanSourceDate())
                .append(", materialCode=").append(sku.getMaterialCode())
                .append(", productStatus=").append(PriorityTraceLogHelper.safeText(
                        sku.getProductStatus()))
                .append(", structureName=").append(PriorityTraceLogHelper.safeText(
                        admission.getStructureName()))
                .append(", admissionShiftIndex=").append(
                        admission.getAdmissionShiftIndex())
                .append(", currentPlanMachineCount=").append(
                        admission.getCurrentPlanMachineCount())
                .append(", rawScheduledStructureCount=").append(
                        admission.getRawScheduledPhysicalMachineCodes().size())
                .append(", scheduledStructureCount=").append(
                        admission.getScheduledStructureCount())
                .append(", excludedEndingMachines=").append(
                        admission.getExcludedEndingPhysicalMachineCodes())
                .append(", result=").append(admission.isAllowed())
                .append(", reason=").append(admission.getReason())
                .toString();
        log.info("结构当天SKU提前生产资格, {}", detail);
        PriorityTraceLogHelper.appendProcessLog(
                context, "结构当天SKU提前生产资格", detail);
    }

    /**
     * 根据已形成的提前生产准入结论初始化数量、临时 dayN 和实际消费账本。
     *
     * @param context 排程上下文
     * @param sku SKU
     * @param currentDate 当前业务日
     * @param windowStartDate 排程窗口 T 日
     * @param windowEndDate 排程窗口结束日
     * @param existingRuntimePlan 已有候选视图
     * @param decision 提前生产准入结论
     * @return 初始化后的运行视图
     */
    private EarlyProductionRuntimePlan initializeRuntimePlan(
            LhScheduleContext context,
            SkuScheduleDTO sku,
            LocalDate currentDate,
            LocalDate windowStartDate,
            LocalDate windowEndDate,
            EarlyProductionRuntimePlan existingRuntimePlan,
            EarlyProductionDecision decision) {
        LocalDate futurePlanDate = decision.getFuturePlanDate();
        int earlyDays = (int) ChronoUnit.DAYS.between(currentDate, futurePlanDate);
        LocalDate earlyProductionMaxDate = EarlyProductionChecker.resolveEarlyProductionMaxDate(
                context, windowEndDate);
        EarlyProductionRuntimePlan runtimePlan = Objects.isNull(existingRuntimePlan)
                ? new EarlyProductionRuntimePlan() : existingRuntimePlan;
        this.fillRuntimePlanBaseInfo(
                context, sku, currentDate, windowStartDate,
                futurePlanDate, earlyProductionMaxDate, earlyDays, runtimePlan, decision);
        this.recordDecisionLog(context, sku, currentDate, runtimePlan, decision);
        if (!decision.isAllowed()) {
            log.info("提前生产准入未通过，不注册激活运行视图, factoryCode: {}, batchNo: {}, "
                            + "materialCode: {}, currentDate: {}, futurePlanDate: {}, structureName: {}, reason: {}",
                    context.getFactoryCode(), context.getBatchNo(), sku.getMaterialCode(),
                    currentDate, futurePlanDate, sku.getStructureName(), decision.getReason());
            return runtimePlan;
        }

        Map<LocalDate, SkuDailyPlanQuotaDTO> sourceQuotaMap =
                this.buildSourceQuotaMap(
                        context, sku, currentDate, earlyProductionMaxDate);
        Map<LocalDate, SkuDailyPlanQuotaDTO> shiftedQuotaMap =
                SkuDailyPlanQuotaUtil.buildShiftedEarlyProductionQuotaMap(
                        sourceQuotaMap, currentDate, windowEndDate,
                        futurePlanDate, earlyProductionMaxDate);
        if (CollectionUtils.isEmpty(shiftedQuotaMap)) {
            return runtimePlan;
        }
        /*
         * 历史欠产/收尾遗留阶段下线后，提前生产只前移未来原始计划量。
         * 历史欠产不再追加到临时dayN，也不参与本轮有效目标量计算。
         */
        int historyShortageQty = 0;
        int futureMonthSurplusQty = runtimePlan.isFutureOnlyCandidate()
                ? runtimePlan.getFutureMonthSurplusQty() : Math.max(0, sku.getSurplusQty());
        int effectiveTargetQty = this.resolveEffectiveTargetQty(
                sku, futurePlanDate, futureMonthSurplusQty);
        this.activateRuntimePlan(
                context, sku, currentDate, futurePlanDate, earlyDays,
                runtimePlan, decision, shiftedQuotaMap, historyShortageQty,
                futureMonthSurplusQty, effectiveTargetQty);
        return runtimePlan;
    }

    /**
     * 填充运行视图的准入、日期和原始数量审计字段。
     *
     * @param context 排程上下文
     * @param sku SKU
     * @param currentDate 当前业务日
     * @param windowStartDate 排程窗口 T 日
     * @param futurePlanDate 未来计划日
     * @param earlyProductionMaxDate 本次排程固定的最晚原始计划日期
     * @param earlyDays 实际提前天数
     * @param runtimePlan 运行视图
     * @param decision 准入结论
     */
    private void fillRuntimePlanBaseInfo(
            LhScheduleContext context,
            SkuScheduleDTO sku,
            LocalDate currentDate,
            LocalDate windowStartDate,
            LocalDate futurePlanDate,
            LocalDate earlyProductionMaxDate,
            int earlyDays,
            EarlyProductionRuntimePlan runtimePlan,
            EarlyProductionDecision decision) {
        runtimePlan.setActive(false);
        runtimePlan.setCurrentDate(currentDate);
        runtimePlan.setFuturePlanDate(futurePlanDate);
        runtimePlan.setEarlyProductionMaxDate(earlyProductionMaxDate);
        runtimePlan.setEarlyDays(earlyDays);
        runtimePlan.setEarlyProductionDaysThreshold(
                EarlyProductionChecker.resolveEarlyProductionDaysThreshold(context));
        runtimePlan.setOriginalCurrentDayPlanQty(
                this.resolveOriginalDayPlanQty(context, sku, currentDate));
        runtimePlan.setFutureDayPlanQty(Math.max(0, MonthPlanDateResolver.resolveDayQty(
                context, sku.getMaterialCode(), sku.getProductStatus(), futurePlanDate)));
        // 历史欠产不再迁移到提前生产运行视图，审计字段固定记录为0。
        runtimePlan.setHistoryShortageQty(0);
        runtimePlan.setDecision(decision);
        if (runtimePlan.isFutureOnlyCandidate()) {
            EarlyProductionQuantityCalculator.populateFutureMonthQuantityView(
                    context, sku, windowStartDate, runtimePlan);
        }
    }

    /**
     * 激活运行视图并同步 SKU 目标量、实际消费账本和胎胚资源视图。
     *
     * @param context 排程上下文
     * @param sku SKU
     * @param currentDate 当前业务日
     * @param futurePlanDate 未来计划日
     * @param earlyDays 实际提前天数
     * @param runtimePlan 运行视图
     * @param decision 准入结论
     * @param shiftedQuotaMap 临时前移 dayN
     * @param historyShortageQty 当前月历史欠产，阶段下线后固定为0
     * @param futureMonthSurplusQty 硫化余量
     * @param effectiveTargetQty 本轮有效目标量
     */
    private void activateRuntimePlan(
            LhScheduleContext context,
            SkuScheduleDTO sku,
            LocalDate currentDate,
            LocalDate futurePlanDate,
            int earlyDays,
            EarlyProductionRuntimePlan runtimePlan,
            EarlyProductionDecision decision,
            Map<LocalDate, SkuDailyPlanQuotaDTO> shiftedQuotaMap,
            int historyShortageQty,
            int futureMonthSurplusQty,
            int effectiveTargetQty) {
        sku.setMonthlyHistoryShortageQty(historyShortageQty);
        sku.setEffectiveCarryForwardQty(historyShortageQty);
        sku.setTargetScheduleQty(effectiveTargetQty);
        sku.setPendingQty(effectiveTargetQty);
        sku.setRemainingScheduleQty(effectiveTargetQty);
        sku.setWindowPlanQty(this.sumWindowPlanQty(shiftedQuotaMap));
        sku.setWindowRemainingPlanQty(
                SkuDailyPlanQuotaUtil.sumRemainingQty(shiftedQuotaMap));
        targetScheduleQtyResolver.syncProductionRemainingQtyToTarget(
                context, sku, effectiveTargetQty, "提前生产中心运行视图初始化");
        targetScheduleQtyResolver.refreshActiveEmbryoSkuMap(context);
        targetScheduleQtyResolver.refreshAllSharedEmbryoStockAllocations(
                context, "提前生产候选激活");

        runtimePlan.setHistoryShortageQty(historyShortageQty);
        runtimePlan.setEffectiveTargetQty(effectiveTargetQty);
        runtimePlan.setDecision(decision);
        runtimePlan.setShiftedDailyPlanQuotaMap(shiftedQuotaMap);
        runtimePlan.setActive(true);
        context.registerEarlyProductionRuntimePlan(sku, runtimePlan);
        this.logActivatedRuntimePlan(
                context, sku, currentDate, futurePlanDate, earlyDays,
                runtimePlan, shiftedQuotaMap, historyShortageQty,
                futureMonthSurplusQty, effectiveTargetQty);
    }

    /**
     * 记录提前生产运行视图激活后的可对账信息。
     *
     * @param context 排程上下文
     * @param sku SKU
     * @param currentDate 当前业务日
     * @param futurePlanDate 未来计划日
     * @param earlyDays 实际提前天数
     * @param runtimePlan 运行视图
     * @param shiftedQuotaMap 临时前移 dayN
     * @param historyShortageQty 当前月历史欠产
     * @param futureMonthSurplusQty 硫化余量
     * @param effectiveTargetQty 本轮有效目标量
     */
    private void logActivatedRuntimePlan(
            LhScheduleContext context,
            SkuScheduleDTO sku,
            LocalDate currentDate,
            LocalDate futurePlanDate,
            int earlyDays,
            EarlyProductionRuntimePlan runtimePlan,
            Map<LocalDate, SkuDailyPlanQuotaDTO> shiftedQuotaMap,
            int historyShortageQty,
            int futureMonthSurplusQty,
            int effectiveTargetQty) {
        StructureEarlyProductionAdmission admission =
                context.getStructureEarlyProductionAdmission(
                        currentDate, sku.getStructureName());
        log.info("提前生产中心运行视图初始化完成, factoryCode: {}, batchNo: {}, materialCode: {}, "
                        + "currentDate: {}, futurePlanDate: {}, earlyProductionMaxDate: {}, earlyDays: {}, "
                        + "originalCurrentDayPlanQty: {}, futureDayPlanQty: {}, "
                        + "shiftedCurrentDayPlanQty: {}, structureName: {}, "
                        + "currentPlanMachineCount: {}, futurePlanMachineCount: {}, "
                        + "admissionShiftIndex: {}, scheduledStructureCount: {}, "
                        + "excludedEndingMachines: {}, scheduledSkuCount: {}, "
                        + "historyShortageQty: {}, futureMonthSurplusQty: {}, effectiveTargetQty: {}, "
                        + "quotaEntryCount: {}, quotaProjectionEndDate: {}",
                context.getFactoryCode(), context.getBatchNo(), sku.getMaterialCode(),
                currentDate, futurePlanDate, runtimePlan.getEarlyProductionMaxDate(), earlyDays,
                runtimePlan.getOriginalCurrentDayPlanQty(), runtimePlan.getFutureDayPlanQty(),
                this.resolveQuotaDayPlanQty(shiftedQuotaMap, currentDate),
                sku.getStructureName(),
                context.getStructurePlanMachineCount(currentDate, sku.getStructureName()),
                context.getStructurePlanMachineCount(futurePlanDate, sku.getStructureName()),
                Objects.isNull(admission) ? null : admission.getAdmissionShiftIndex(),
                Objects.isNull(admission)
                        ? 0 : admission.getScheduledStructureCount(),
                Objects.isNull(admission)
                        ? java.util.Collections.emptySet()
                        : admission.getExcludedEndingPhysicalMachineCodes(),
                context.getSkuScheduledMachineCount(
                        currentDate, sku.getMaterialCode(), sku.getProductStatus()),
                historyShortageQty, futureMonthSurplusQty, effectiveTargetQty,
                shiftedQuotaMap.size(), SkuDailyPlanQuotaUtil.resolveLastQuotaDate(shiftedQuotaMap));
    }

    /**
     * 保留尚未进入提前生产阈值的 future-only 候选视图。
     *
     * @param context 排程上下文
     * @param sku SKU
     * @param currentDate 当前业务日
     * @param runtimePlan 已有运行视图
     * @param decision 当前准入结论
     * @return 保留后的候选视图；普通 SKU 返回 null
     */
    private EarlyProductionRuntimePlan keepFutureOnlyCandidateInactive(
            LhScheduleContext context,
            SkuScheduleDTO sku,
            LocalDate currentDate,
            EarlyProductionRuntimePlan runtimePlan,
            EarlyProductionDecision decision) {
        if (Objects.isNull(runtimePlan) || !runtimePlan.isFutureOnlyCandidate()) {
            return null;
        }
        runtimePlan.setCurrentDate(currentDate);
        runtimePlan.setActive(false);
        runtimePlan.setDecision(decision);
        runtimePlan.getShiftedDailyPlanQuotaMap().clear();
        context.registerEarlyProductionRuntimePlan(sku, runtimePlan);
        this.recordDecisionLog(context, sku, currentDate, runtimePlan, decision);
        log.info("提前生产候选尚未激活, factoryCode: {}, batchNo: {}, materialCode: {}, "
                        + "currentDate: {}, futurePlanDate: {}, reason: {}",
                context.getFactoryCode(), context.getBatchNo(), sku.getMaterialCode(),
                currentDate, runtimePlan.getFuturePlanDate(),
                Objects.isNull(decision) ? "未形成准入结论" : decision.getReason());
        return runtimePlan;
    }

    /**
     * 确保当前 SKU 已形成提前生产判断日志明细。
     *
     * <p>该方法只创建或读取轻量日志对象，不重新执行提前生产判断。新增主链在真实进入选机
     * 前调用时，会继续复用中心服务已经形成的准入结论。</p>
     *
     * @param context 排程上下文
     * @param sku 当前 SKU
     * @param businessDate 当前业务日期
     * @return 当前判断日志明细
     */
    public EarlyProductionDecisionLogEntry ensureDecisionLogEntry(
            LhScheduleContext context,
            SkuScheduleDTO sku,
            LocalDate businessDate) {
        if (Objects.isNull(context) || Objects.isNull(sku) || Objects.isNull(businessDate)) {
            return null;
        }
        EarlyProductionRuntimePlan runtimePlan = context.getEarlyProductionRuntimePlan(sku);
        EarlyProductionDecision decision = Objects.isNull(runtimePlan)
                ? null : runtimePlan.getDecision();
        return this.recordDecisionLog(context, sku, businessDate, runtimePlan, decision);
    }

    /**
     * 记录其他提前生产消费者已经得到的共享准入结论。
     *
     * <p>主要供 S4.4 换活字块只读理论日校验使用；调用方传入的判断结果直接复用，
     * 不在日志层再次调用 {@link EarlyProductionChecker}。</p>
     *
     * @param context 排程上下文
     * @param sku 当前 SKU
     * @param businessDate 判断业务日期
     * @param decision 共享准入结果
     * @return 日志明细
     */
    public EarlyProductionDecisionLogEntry recordDecisionSnapshot(
            LhScheduleContext context,
            SkuScheduleDTO sku,
            LocalDate businessDate,
            EarlyProductionDecision decision) {
        EarlyProductionRuntimePlan runtimePlan = Objects.isNull(context)
                ? null : context.getEarlyProductionRuntimePlan(sku);
        return this.recordDecisionLog(context, sku, businessDate, runtimePlan, decision);
    }

    /**
     * 记录真实进入提前生产选机流程时的实时快照。
     *
     * @param context 排程上下文
     * @param sku 当前 SKU
     * @param businessDate 当前业务日期
     * @param orderEntry 现有真实新增选机顺序明细
     * @param candidates 当前正式候选机台
     * @param snapshot 既有选机前实时快照
     * @param attemptShiftIndex 当前尝试班次
     */
    public void recordSelectionSnapshot(
            LhScheduleContext context,
            SkuScheduleDTO sku,
            LocalDate businessDate,
            DailyNewSpecOrderLogEntry orderEntry,
            List<?> candidates,
            NewSpecSelectionRealtimeSnapshot snapshot,
            Integer attemptShiftIndex) {
        EarlyProductionDecisionLogEntry entry =
                this.ensureDecisionLogEntry(context, sku, businessDate);
        if (Objects.isNull(entry)) {
            return;
        }
        if (Objects.nonNull(attemptShiftIndex)
                && Objects.nonNull(entry.getAttemptShiftIndex())
                && !Objects.equals(attemptShiftIndex, entry.getAttemptShiftIndex())) {
            entry = this.createAttemptLogEntry(context, sku, businessDate, entry);
        }
        if (Objects.nonNull(orderEntry)) {
            entry.setActualSelectionOrder(orderEntry.getSelectionOrder());
        }
        entry.setCandidateMachineCount(Objects.isNull(candidates) ? 0 : candidates.size());
        entry.setAttemptShiftIndex(attemptShiftIndex);
        entry.setAttemptScheduledStructureCount(
                this.resolveAttemptScheduledStructureCount(
                        context, sku, businessDate, attemptShiftIndex));
        entry.setAllowedAdvance(true);
        entry.setReasonCode(EarlyProductionLogReason.PENDING.getCode());
        if (Objects.nonNull(snapshot)) {
            entry.setRealtimeShiftTotalPlanQty(snapshot.getRealtimeShiftTotalPlanQty());
            entry.setRealtimeShiftChangeCount(snapshot.getRealtimeShiftChangeCount());
            entry.setRealtimeStructureMachineCount(snapshot.getRealtimeStructureMachineCount());
        }
    }

    /**
     * 记录当前候选机台的模具资源判断。
     *
     * @param context 排程上下文
     * @param sku 当前 SKU
     * @param businessDate 当前业务日期
     * @param allocationResult 既有模具分配结果
     */
    public void recordMouldAllocation(
            LhScheduleContext context,
            SkuScheduleDTO sku,
            LocalDate businessDate,
            MouldResourceAllocationResult allocationResult) {
        EarlyProductionDecisionLogEntry entry =
                this.ensureDecisionLogEntry(context, sku, businessDate);
        if (Objects.isNull(entry) || Objects.isNull(allocationResult)) {
            return;
        }
        entry.setMouldSatisfied(allocationResult.isAllowed());
        entry.setRequiredMouldQty(allocationResult.getRequiredMouldQty());
        entry.setAvailableMouldQty(allocationResult.getAvailableMouldQty());
        entry.setOccupiedMouldQty(allocationResult.getOccupiedMouldQty());
        entry.setRemainingAvailableMouldQty(
                allocationResult.getRemainingAvailableMouldQty());
        if (!allocationResult.isAllowed() && Objects.nonNull(allocationResult.getSkipReason())) {
            entry.setReasonCode(EarlyProductionLogReason.fromFailure(
                    allocationResult.getSkipReason().getDescription(), null).getCode());
            entry.setDetail(allocationResult.getSkipReason().getDescription());
        }
    }

    /**
     * 记录实际形成提前生产结果的成功结论。
     *
     * @param context 排程上下文
     * @param sku 当前 SKU
     * @param businessDate 当前判断业务日期
     * @param actualProductionDate 实际结果业务日期
     * @param shiftIndex 实际生产班次
     * @param machineCode 实际机台
     * @param plannedQty 实际计划量
     * @param remainingCapacity 当前候选剩余产能
     */
    public void recordSuccessfulSchedule(
            LhScheduleContext context,
            SkuScheduleDTO sku,
            LocalDate businessDate,
            LocalDate actualProductionDate,
            Integer shiftIndex,
            String machineCode,
            Integer plannedQty,
            Integer remainingCapacity) {
        EarlyProductionDecisionLogEntry entry =
                this.ensureDecisionLogEntry(context, sku, businessDate);
        if (Objects.isNull(entry)) {
            return;
        }
        if (Objects.nonNull(shiftIndex)
                && Objects.nonNull(entry.getAttemptShiftIndex())
                && !Objects.equals(shiftIndex, entry.getAttemptShiftIndex())) {
            entry = this.createAttemptLogEntry(context, sku, businessDate, entry);
        }
        entry.setActualProductionDate(actualProductionDate);
        entry.setAttemptShiftIndex(shiftIndex);
        entry.setActualMachineCode(machineCode);
        entry.setPlannedQty(plannedQty);
        entry.setRemainingCapacity(remainingCapacity);
        entry.setAllowedAdvance(true);
        entry.setMouldSatisfied(true);
        entry.setActualScheduled(true);
        entry.setReasonCode(EarlyProductionLogReason.SUCCESS.getCode());
        entry.setDetail("提前生产结果已提交");
    }

    /**
     * 记录当前业务日提前生产未形成有效结果的结论。
     *
     * @param context 排程上下文
     * @param sku 当前 SKU
     * @param businessDate 当前业务日期
     * @param detail 既有业务失败明细
     * @param failReason 既有新增排产失败原因
     * @param candidateMachineCount 当前候选机台数量
     * @param attemptShiftIndex 当前尝试班次
     */
    public void recordFailedSchedule(
            LhScheduleContext context,
            SkuScheduleDTO sku,
            LocalDate businessDate,
            String detail,
            NewSpecFailReasonEnum failReason,
            Integer candidateMachineCount,
            Integer attemptShiftIndex) {
        EarlyProductionDecisionLogEntry entry =
                this.ensureDecisionLogEntry(context, sku, businessDate);
        if (Objects.isNull(entry)) {
            return;
        }
        String effectiveDetail = StringUtils.isEmpty(detail)
                ? Objects.isNull(failReason) ? "提前生产未形成有效结果" : failReason.getDescription()
                : detail;
        if (Objects.nonNull(attemptShiftIndex)
                && Objects.nonNull(entry.getAttemptShiftIndex())
                && !Objects.equals(attemptShiftIndex, entry.getAttemptShiftIndex())) {
            entry = this.createAttemptLogEntry(context, sku, businessDate, entry);
        }
        entry.setCandidateMachineCount(candidateMachineCount);
        entry.setAttemptShiftIndex(attemptShiftIndex);
        entry.setAttemptScheduledStructureCount(
                this.resolveAttemptScheduledStructureCount(
                        context, sku, businessDate, attemptShiftIndex));
        entry.setAllowedAdvance(Boolean.TRUE.equals(entry.getAllowedAdvance()));
        entry.setActualScheduled(false);
        entry.setReasonCode(EarlyProductionLogReason.fromFailure(
                effectiveDetail, failReason).getCode());
        entry.setDetail(effectiveDetail);
    }

    /**
     * 构建提前生产判断日志明细并写入业务日采集器。
     *
     * @param context 排程上下文
     * @param sku 当前 SKU
     * @param businessDate 当前业务日期
     * @param runtimePlan 当前运行视图
     * @param decision 既有提前生产判断结果
     * @return 日志明细
     */
    private EarlyProductionDecisionLogEntry recordDecisionLog(
            LhScheduleContext context,
            SkuScheduleDTO sku,
            LocalDate businessDate,
            EarlyProductionRuntimePlan runtimePlan,
            EarlyProductionDecision decision) {
        if (Objects.isNull(context) || Objects.isNull(sku) || Objects.isNull(businessDate)) {
            return null;
        }
        if (Objects.nonNull(runtimePlan)
                && Objects.nonNull(runtimePlan.getDecisionLogEntry())
                && businessDate.equals(runtimePlan.getDecisionLogEntry().getBusinessDate())
                && !Boolean.TRUE.equals(runtimePlan.getDecisionLogEntry().getActualScheduled())) {
            return runtimePlan.getDecisionLogEntry();
        }
        LocalDate sourcePlanDate = Objects.isNull(decision)
                ? null : decision.getFuturePlanDate();
        StructureEarlyProductionAdmission admission =
                StringUtils.isEmpty(sku.getStructureName())
                        ? null : context.getStructureEarlyProductionAdmission(
                        businessDate, sku.getStructureName());
        EarlyProductionDecisionLogCollector collector =
                context.getOrCreateEarlyProductionDecisionLogCollector(
                        businessDate, this.resolveDateOffset(context, businessDate));
        EarlyProductionDecisionLogEntry existingEntry = Objects.isNull(collector)
                ? null : Objects.isNull(sourcePlanDate)
                ? collector.findLatest(sku.getMaterialCode(), sku.getProductStatus())
                : collector.findLatest(
                sku.getMaterialCode(), sku.getProductStatus(), sourcePlanDate);
        if (Objects.nonNull(existingEntry)
                && !Boolean.TRUE.equals(existingEntry.getActualScheduled())) {
            if (Objects.nonNull(runtimePlan)) {
                runtimePlan.setDecisionLogEntry(existingEntry);
            }
            return existingEntry;
        }
        EarlyProductionDecisionLogEntry entry = new EarlyProductionDecisionLogEntry();
        entry.setBusinessDate(businessDate);
        entry.setSourcePlanDate(sourcePlanDate);
        entry.setMaterialCode(sku.getMaterialCode());
        entry.setProductStatus(sku.getProductStatus());
        entry.setStructureName(sku.getStructureName());
        entry.setPhase("提前生产阶段");
        entry.setSource(sku.getSourceType());
        entry.setCurrentDatePlanMachineCount(
                context.getStructurePlanMachineCount(businessDate, sku.getStructureName()));
        entry.setEffectivePlanMachineCount(Objects.isNull(admission)
                ? this.resolveEffectivePlanMachineCount(context, sku, sourcePlanDate, businessDate)
                : admission.getCurrentPlanMachineCount());
        entry.setStructurePlanMachineCountRange(
                this.resolveStructurePlanMachineCountRange(context, sku, businessDate, decision));
        /*
         * 提前生产准入拒绝不会进入新增选机主链。必须在创建每条判断明细时冻结实时快照，
         * 使准入拒绝、候选失败和最终排产三类明细均可展示相同口径的机台数和切换次数。
         * 后续真实选机仍会复用既有逻辑回填该次选机前的最新快照。
         */
        this.fillDecisionRealtimeSnapshot(context, sku, entry);
        entry.setAdmissionShiftIndex(Objects.isNull(admission)
                ? null : admission.getAdmissionShiftIndex());
        entry.setScheduledStructureCount(Objects.isNull(admission)
                ? context.getStructureScheduledMachineCount(businessDate, sku.getStructureName())
                : admission.getScheduledStructureCount());
        entry.setStructureAdmission(Objects.isNull(admission)
                ? null : admission.isAllowed());
        entry.setAllowedAdvance(Objects.nonNull(decision)
                && decision.isEarlyProduction() && decision.isAllowed());
        entry.setActualScheduled(false);
        entry.setReasonCode(EarlyProductionLogReason.fromDecision(decision, admission).getCode());
        entry.setDetail(Objects.isNull(decision) ? "未形成提前生产判断结果" : decision.getReason());
        if (Objects.nonNull(collector)) {
            collector.record(entry);
        }
        if (Objects.nonNull(runtimePlan)) {
            runtimePlan.setDecisionLogEntry(entry);
        }
        return entry;
    }

    /**
     * 冻结提前生产判断创建时点的实时班次快照。
     *
     * <p>该方法仅复用现有 {@link NewSpecSelectionRealtimeSnapshot} 的只读统计，不写入排程
     * 结果、结构机台账本或换模次数。它保证未进入真实选机的前置拒绝明细也有完整 c1～c8
     * 统计；真实选机明细随后由 {@link #recordSelectionSnapshot} 覆盖为同一选机回合的快照。</p>
     *
     * @param context 排程上下文
     * @param sku 当前 SKU
     * @param entry 当前提前生产判断日志明细
     */
    private void fillDecisionRealtimeSnapshot(
            LhScheduleContext context,
            SkuScheduleDTO sku,
            EarlyProductionDecisionLogEntry entry) {
        NewSpecSelectionRealtimeSnapshot snapshot = NewSpecSelectionRealtimeSnapshot.capture(
                context, sku, null, null, mouldChangeBalanceStrategy,
                this.resolveDateOffset(context, entry.getBusinessDate()));
        entry.setRealtimeShiftTotalPlanQty(snapshot.getRealtimeShiftTotalPlanQty());
        entry.setRealtimeShiftChangeCount(snapshot.getRealtimeShiftChangeCount());
        entry.setRealtimeStructureMachineCount(snapshot.getRealtimeStructureMachineCount());
    }

    /**
     * 复用既有提前生产判定中的窗口结构计划机台数文本。
     *
     * <p>优先使用 Checker 已经生成的 T～T+2 结果；只有共享判定未携带该结果时，才从
     * 上下文已加载的结构计划 Map 读取，避免新增数据库查询或重复执行提前生产判断。</p>
     *
     * @param context 排程上下文
     * @param sku 当前 SKU
     * @param businessDate 当前业务日期
     * @param decision 既有提前生产判断结果
     * @return 计划硫化机台数范围文本
     */
    private String resolveStructurePlanMachineCountRange(
            LhScheduleContext context,
            SkuScheduleDTO sku,
            LocalDate businessDate,
            EarlyProductionDecision decision) {
        Collection<Integer> decisionMachineCounts = Objects.isNull(decision)
                ? null : decision.getStructurePlanMachineCounts();
        if (CollectionUtils.isEmpty(decisionMachineCounts)) {
            LocalDate windowStartDate = this.resolveScheduleWindowStartDate(context);
            if (Objects.isNull(windowStartDate)) {
                return null;
            }
            StringBuilder fallbackRangeBuilder = new StringBuilder(32);
            for (int dayOffset = 0; dayOffset < 3; dayOffset++) {
                if (dayOffset > 0) {
                    fallbackRangeBuilder.append(',');
                }
                fallbackRangeBuilder.append(this.resolvePlanDateLabel(dayOffset))
                        .append('=')
                        .append(context.getStructurePlanMachineCount(
                                windowStartDate.plusDays(dayOffset), sku.getStructureName()));
            }
            return fallbackRangeBuilder.toString();
        }
        StringBuilder rangeBuilder = new StringBuilder(32);
        int dayOffset = 0;
        for (Integer machineCount : decisionMachineCounts) {
            if (dayOffset > 0) {
                rangeBuilder.append(',');
            }
            rangeBuilder.append(this.resolvePlanDateLabel(dayOffset))
                    .append('=')
                    .append(Objects.isNull(machineCount) ? 0 : machineCount);
            dayOffset++;
        }
        return rangeBuilder.toString();
    }

    /** 获取结构计划机台数范围中的业务日标签。 */
    private String resolvePlanDateLabel(int dayOffset) {
        return dayOffset == 0 ? "T" : "T+" + dayOffset;
    }

    /**
     * 基于上一条准入快照创建新的实际班次尝试明细。
     *
     * @param context 排程上下文
     * @param sku 当前 SKU
     * @param businessDate 当前业务日期
     * @param previousEntry 上一条尝试明细
     * @return 新的尝试明细
     */
    private EarlyProductionDecisionLogEntry createAttemptLogEntry(
            LhScheduleContext context,
            SkuScheduleDTO sku,
            LocalDate businessDate,
            EarlyProductionDecisionLogEntry previousEntry) {
        EarlyProductionDecisionLogEntry entry = previousEntry.copyForAttempt();
        EarlyProductionDecisionLogCollector collector =
                context.getOrCreateEarlyProductionDecisionLogCollector(
                        businessDate, this.resolveDateOffset(context, businessDate));
        if (Objects.nonNull(collector)) {
            collector.record(entry);
        }
        EarlyProductionRuntimePlan runtimePlan = context.getEarlyProductionRuntimePlan(sku);
        if (Objects.nonNull(runtimePlan)) {
            runtimePlan.setDecisionLogEntry(entry);
        }
        return entry;
    }

    /** 解析结构切换场景下现有准入使用的有效结构计划机台数。 */
    private int resolveEffectivePlanMachineCount(
            LhScheduleContext context,
            SkuScheduleDTO sku,
            LocalDate sourcePlanDate,
            LocalDate businessDate) {
        if (Objects.isNull(sourcePlanDate)) {
            return 0;
        }
        return EarlyProductionChecker.resolveEffectiveStructurePlanMachineCount(
                context, sku, businessDate, sourcePlanDate);
    }

    /** 解析当前候选实际尝试班次的结构已排物理机台数。 */
    private int resolveAttemptScheduledStructureCount(
            LhScheduleContext context,
            SkuScheduleDTO sku,
            LocalDate businessDate,
            Integer shiftIndex) {
        return Objects.isNull(shiftIndex) ? 0
                : context.getStructureScheduledMachineCount(
                businessDate, shiftIndex, sku.getStructureName());
    }

    /** 解析业务日相对窗口 T 日的偏移。 */
    private int resolveDateOffset(LhScheduleContext context, LocalDate businessDate) {
        LocalDate windowStartDate = this.resolveScheduleWindowStartDate(context);
        return Objects.isNull(windowStartDate) ? 0
                : Math.max(0, (int) ChronoUnit.DAYS.between(windowStartDate, businessDate));
    }

    /**
     * 构造当前业务日至固定提前生产截止日的原始计划读取视图。
     *
     * @param context 排程上下文
     * @param sku SKU
     * @param currentDate 当前业务日
     * @param earlyProductionMaxDate 本次排程固定的最晚原始计划日期
     * @return 原始计划临时读取视图
     */
    private Map<LocalDate, SkuDailyPlanQuotaDTO> buildSourceQuotaMap(
            LhScheduleContext context,
            SkuScheduleDTO sku,
            LocalDate currentDate,
            LocalDate earlyProductionMaxDate) {
        LocalDate sourceEndDate = Objects.isNull(earlyProductionMaxDate)
                ? currentDate : earlyProductionMaxDate;
        int initialCapacity = Math.max(4, Math.min(
                8, (int) ChronoUnit.DAYS.between(currentDate, sourceEndDate) + 1));
        Map<LocalDate, SkuDailyPlanQuotaDTO> sourceQuotaMap =
                new LinkedHashMap<LocalDate, SkuDailyPlanQuotaDTO>(initialCapacity);
        LocalDate planDate = currentDate;
        while (!planDate.isAfter(sourceEndDate)) {
            int dayPlanQty = Math.max(0, MonthPlanDateResolver.resolveDayQty(
                    context, sku.getMaterialCode(), sku.getProductStatus(), planDate));
            if (dayPlanQty <= 0) {
                planDate = planDate.plusDays(1);
                continue;
            }
            SkuDailyPlanQuotaDTO quota = new SkuDailyPlanQuotaDTO();
            quota.setMaterialCode(sku.getMaterialCode());
            quota.setProductionDate(planDate);
            quota.setDayPlanQty(dayPlanQty);
            quota.setRemainingQty(dayPlanQty);
            sourceQuotaMap.put(planDate, quota);
            planDate = planDate.plusDays(1);
        }
        SkuDailyPlanQuotaUtil.refreshRollingFields(sourceQuotaMap);
        return sourceQuotaMap;
    }

    /**
     * 把当前月历史欠产追加到临时前移账本首日。
     *
     * @param shiftedQuotaMap 临时前移账本
     * @param currentDate 当前业务日
     * @param historyShortageQty 当前月前日累计欠产
     */
    private void appendHistoryShortage(
            Map<LocalDate, SkuDailyPlanQuotaDTO> shiftedQuotaMap,
            LocalDate currentDate,
            int historyShortageQty) {
        if (historyShortageQty <= 0 || CollectionUtils.isEmpty(shiftedQuotaMap)) {
            return;
        }
        SkuDailyPlanQuotaDTO currentQuota = shiftedQuotaMap.get(currentDate);
        if (Objects.isNull(currentQuota)) {
            return;
        }
        long remainingQty =
                (long) Math.max(0, currentQuota.getRemainingQty()) + historyShortageQty;
        if (remainingQty > Integer.MAX_VALUE) {
            throw new ScheduleException(
                    ScheduleErrorCode.SURPLUS_CALCULATION_ERROR,
                    "提前生产临时日计划追加历史欠产后超出整数范围");
        }
        currentQuota.setRemainingQty((int) remainingQty);
        SkuDailyPlanQuotaUtil.refreshRollingFields(shiftedQuotaMap);
    }

    /**
     * 计算提前生产未来计划有效目标量。
     *
     * @param sku SKU
     * @param futurePlanDate 未来计划日
     * @param futureMonthSurplusQty 未来计划段或当前月硫化余量
     * @return 非负有效目标量
     */
    private int resolveEffectiveTargetQty(
            SkuScheduleDTO sku,
            LocalDate futurePlanDate,
            int futureMonthSurplusQty) {
        int effectiveTargetQty = Math.max(0, futureMonthSurplusQty);
        log.debug("提前生产目标量仅使用未来计划余量, materialCode: {}, futurePlanDate: {}, "
                        + "futureMonthSurplusQty: {}, effectiveTargetQty: {}",
                Objects.isNull(sku) ? null : sku.getMaterialCode(), futurePlanDate,
                futureMonthSurplusQty, effectiveTargetQty);
        return effectiveTargetQty;
    }

    /**
     * 汇总临时前移账本的原始日计划量。
     *
     * @param quotaMap 临时前移账本
     * @return 非负窗口计划量
     */
    private int sumWindowPlanQty(Map<LocalDate, SkuDailyPlanQuotaDTO> quotaMap) {
        if (CollectionUtils.isEmpty(quotaMap)) {
            return 0;
        }
        int planQty = 0;
        for (SkuDailyPlanQuotaDTO quota : quotaMap.values()) {
            if (Objects.nonNull(quota)) {
                planQty += Math.max(0, quota.getDayPlanQty());
            }
        }
        return Math.max(0, planQty);
    }

    /**
     * 读取原始月计划 dayN；月计划未返回正值时兼容 SKU 原始运行态账本。
     *
     * @param context 排程上下文
     * @param sku SKU
     * @param productionDate 业务日
     * @return 原始日计划量
     */
    private int resolveOriginalDayPlanQty(
            LhScheduleContext context,
            SkuScheduleDTO sku,
            LocalDate productionDate) {
        int dayPlanQty = Math.max(0, MonthPlanDateResolver.resolveDayQty(
                context, sku.getMaterialCode(), sku.getProductStatus(), productionDate));
        if (dayPlanQty > 0 || CollectionUtils.isEmpty(sku.getDailyPlanQuotaMap())) {
            return dayPlanQty;
        }
        SkuDailyPlanQuotaDTO quota = sku.getDailyPlanQuotaMap().get(productionDate);
        return Objects.isNull(quota) ? 0 : Math.max(0, quota.getDayPlanQty());
    }

    /**
     * 读取临时账本指定日期的日计划量。
     *
     * @param quotaMap 临时前移账本
     * @param productionDate 业务日
     * @return 非负日计划量
     */
    private int resolveQuotaDayPlanQty(
            Map<LocalDate, SkuDailyPlanQuotaDTO> quotaMap,
            LocalDate productionDate) {
        if (CollectionUtils.isEmpty(quotaMap) || Objects.isNull(productionDate)) {
            return 0;
        }
        SkuDailyPlanQuotaDTO quota = quotaMap.get(productionDate);
        return Objects.isNull(quota) ? 0 : Math.max(0, quota.getDayPlanQty());
    }

    /**
     * 解析排程窗口 T 日。
     *
     * @param context 排程上下文
     * @return 排程窗口开始业务日
     */
    private LocalDate resolveScheduleWindowStartDate(LhScheduleContext context) {
        if (!CollectionUtils.isEmpty(context.getScheduleWindowShifts())) {
            for (LhShiftConfigVO shift : context.getScheduleWindowShifts()) {
                if (Objects.nonNull(shift) && Objects.nonNull(shift.getWorkDate())) {
                    return shift.getWorkDate().toInstant()
                            .atZone(ZoneId.systemDefault()).toLocalDate();
                }
            }
        }
        return Objects.isNull(context.getScheduleDate()) ? null
                : context.getScheduleDate().toInstant()
                .atZone(ZoneId.systemDefault()).toLocalDate();
    }

    /**
     * 解析排程窗口结束业务日。
     *
     * @param context 排程上下文
     * @return 排程窗口结束业务日
     */
    private LocalDate resolveScheduleWindowEndDate(LhScheduleContext context) {
        if (!CollectionUtils.isEmpty(context.getScheduleWindowShifts())) {
            for (int index = context.getScheduleWindowShifts().size() - 1;
                 index >= 0; index--) {
                LhShiftConfigVO shift = context.getScheduleWindowShifts().get(index);
                if (Objects.nonNull(shift) && Objects.nonNull(shift.getWorkDate())) {
                    return shift.getWorkDate().toInstant()
                            .atZone(ZoneId.systemDefault()).toLocalDate();
                }
            }
        }
        return Objects.isNull(context.getWindowEndDate()) ? null
                : context.getWindowEndDate().toInstant()
                .atZone(ZoneId.systemDefault()).toLocalDate();
    }
}
