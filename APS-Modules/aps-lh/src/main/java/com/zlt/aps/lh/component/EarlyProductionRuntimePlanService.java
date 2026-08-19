package com.zlt.aps.lh.component;

import com.zlt.aps.lh.api.domain.dto.SkuDailyPlanQuotaDTO;
import com.zlt.aps.lh.api.domain.dto.SkuScheduleDTO;
import com.zlt.aps.lh.api.domain.vo.LhShiftConfigVO;
import com.zlt.aps.lh.context.LhScheduleContext;
import com.zlt.aps.lh.engine.strategy.support.DailyMachineExpansionPlanner;
import com.zlt.aps.lh.engine.strategy.support.EarlyProductionChecker;
import com.zlt.aps.lh.engine.strategy.support.EarlyProductionDecision;
import com.zlt.aps.lh.engine.strategy.support.EarlyProductionRuntimePlan;
import com.zlt.aps.lh.exception.ScheduleErrorCode;
import com.zlt.aps.lh.exception.ScheduleException;
import com.zlt.aps.lh.util.SkuDailyPlanQuotaUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
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
        return EarlyProductionChecker.checkEarlyProduction(
                context, sku, currentDate, windowStartDate, windowEndDate,
                DailyMachineExpansionPlanner.resolveShortageAddMachineThreshold(context));
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
        EarlyProductionRuntimePlan runtimePlan = Objects.isNull(existingRuntimePlan)
                ? new EarlyProductionRuntimePlan() : existingRuntimePlan;
        this.fillRuntimePlanBaseInfo(
                context, sku, currentDate, windowStartDate,
                futurePlanDate, earlyDays, runtimePlan, decision);
        if (!decision.isAllowed()) {
            log.info("提前生产准入未通过，不注册激活运行视图, factoryCode: {}, batchNo: {}, "
                            + "materialCode: {}, currentDate: {}, futurePlanDate: {}, structureName: {}, reason: {}",
                    context.getFactoryCode(), context.getBatchNo(), sku.getMaterialCode(),
                    currentDate, futurePlanDate, sku.getStructureName(), decision.getReason());
            return runtimePlan;
        }

        Map<LocalDate, SkuDailyPlanQuotaDTO> sourceQuotaMap =
                this.buildSourceQuotaMap(
                        context, sku, currentDate, windowEndDate, earlyDays);
        Map<LocalDate, SkuDailyPlanQuotaDTO> shiftedQuotaMap =
                SkuDailyPlanQuotaUtil.buildShiftedEarlyProductionQuotaMap(
                        sourceQuotaMap, currentDate, windowEndDate, futurePlanDate);
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
            int earlyDays,
            EarlyProductionRuntimePlan runtimePlan,
            EarlyProductionDecision decision) {
        runtimePlan.setActive(false);
        runtimePlan.setCurrentDate(currentDate);
        runtimePlan.setFuturePlanDate(futurePlanDate);
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
        log.info("提前生产中心运行视图初始化完成, factoryCode: {}, batchNo: {}, materialCode: {}, "
                        + "currentDate: {}, futurePlanDate: {}, earlyDays: {}, "
                        + "originalCurrentDayPlanQty: {}, futureDayPlanQty: {}, "
                        + "shiftedCurrentDayPlanQty: {}, structureName: {}, "
                        + "currentPlanMachineCount: {}, futurePlanMachineCount: {}, "
                        + "scheduledStructureCount: {}, scheduledSkuCount: {}, "
                        + "historyShortageQty: {}, futureMonthSurplusQty: {}, effectiveTargetQty: {}",
                context.getFactoryCode(), context.getBatchNo(), sku.getMaterialCode(),
                currentDate, futurePlanDate, earlyDays,
                runtimePlan.getOriginalCurrentDayPlanQty(), runtimePlan.getFutureDayPlanQty(),
                this.resolveQuotaDayPlanQty(shiftedQuotaMap, currentDate),
                sku.getStructureName(),
                context.getStructurePlanMachineCount(currentDate, sku.getStructureName()),
                context.getStructurePlanMachineCount(futurePlanDate, sku.getStructureName()),
                context.getStructureScheduledMachineCount(currentDate, sku.getStructureName()),
                context.getSkuScheduledMachineCount(
                        currentDate, sku.getMaterialCode(), sku.getProductStatus()),
                historyShortageQty, futureMonthSurplusQty, effectiveTargetQty);
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
        log.info("提前生产候选尚未激活, factoryCode: {}, batchNo: {}, materialCode: {}, "
                        + "currentDate: {}, futurePlanDate: {}, reason: {}",
                context.getFactoryCode(), context.getBatchNo(), sku.getMaterialCode(),
                currentDate, runtimePlan.getFuturePlanDate(),
                Objects.isNull(decision) ? "未形成准入结论" : decision.getReason());
        return runtimePlan;
    }

    /**
     * 构造当前业务日至窗口结束日加提前天数的原始计划读取视图。
     *
     * @param context 排程上下文
     * @param sku SKU
     * @param currentDate 当前业务日
     * @param windowEndDate 窗口结束业务日
     * @param earlyDays 实际提前天数
     * @return 原始计划临时读取视图
     */
    private Map<LocalDate, SkuDailyPlanQuotaDTO> buildSourceQuotaMap(
            LhScheduleContext context,
            SkuScheduleDTO sku,
            LocalDate currentDate,
            LocalDate windowEndDate,
            int earlyDays) {
        LocalDate sourceEndDate = windowEndDate.plusDays(Math.max(0, earlyDays));
        int initialCapacity = Math.max(
                4, (int) ChronoUnit.DAYS.between(currentDate, sourceEndDate) + 1);
        Map<LocalDate, SkuDailyPlanQuotaDTO> sourceQuotaMap =
                new LinkedHashMap<LocalDate, SkuDailyPlanQuotaDTO>(initialCapacity);
        LocalDate planDate = currentDate;
        while (!planDate.isAfter(sourceEndDate)) {
            int dayPlanQty = Math.max(0, MonthPlanDateResolver.resolveDayQty(
                    context, sku.getMaterialCode(), sku.getProductStatus(), planDate));
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
