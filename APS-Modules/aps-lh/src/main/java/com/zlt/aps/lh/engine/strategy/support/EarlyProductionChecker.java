package com.zlt.aps.lh.engine.strategy.support;

import com.zlt.aps.lh.api.constant.LhScheduleConstant;
import com.zlt.aps.lh.api.constant.LhScheduleParamConstant;
import com.zlt.aps.lh.api.domain.dto.SkuDailyPlanQuotaDTO;
import com.zlt.aps.lh.api.domain.dto.SkuScheduleDTO;
import com.zlt.aps.lh.api.enums.ConstructionStageEnum;
import com.zlt.aps.lh.api.enums.ScheduleTypeEnum;
import com.zlt.aps.lh.api.enums.SkuScheduleSourceTypeEnum;
import com.zlt.aps.lh.component.MonthPlanDateResolver;
import com.zlt.aps.lh.context.LhScheduleContext;
import com.zlt.aps.mp.api.domain.entity.FactoryMonthPlanProductionFinalResult;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.CollectionUtils;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * SKU提前生产准入判断器。
 *
 * <p>只判断后续日有计划量的SKU是否允许进入当前日提前生产，不执行选机、换模、
 * 换活字块、胎胚扣减或日计划扣账。S4.4 换活字块提前生产与 S4.5 新增排产共用
 * 本判断器；“结构未形成有效续作排产且存在最早胎胚可供硫化时间”只作为结构切换提前
 * 场景的附加前提，普通结构提前和结构收尾提前继续执行原有准入规则。</p>
 */
@Slf4j
public final class EarlyProductionChecker {

    private EarlyProductionChecker() {
    }

    /**
     * 判断 SKU 是否允许进入当前业务日新增机台判断。
     *
     * @param context 排程上下文
     * @param sku SKU
     * @param currentDate 当前业务日期
     * @param windowEndDate 排程窗口结束日期
     * @param shortageThreshold 欠产增机台阈值
     * @return true-允许继续进入新增机台判断；false-不提前生产，保持原顺延逻辑
     */
    public static boolean canEnterEarlyProductionCheck(LhScheduleContext context,
                                                       SkuScheduleDTO sku,
                                                       LocalDate currentDate,
                                                       LocalDate windowEndDate,
                                                       int shortageThreshold) {
        return checkEarlyProduction(context, sku, currentDate, currentDate,
                windowEndDate, shortageThreshold).isAllowed();
    }

    /**
     * 判断 SKU 是否属于提前生产，并返回准入场景及 T～T+2 结构计划机台数。
     *
     * @param context 排程上下文
     * @param sku SKU
     * @param currentDate 当前业务日期
     * @param windowStartDate 排程窗口 T 日
     * @param windowEndDate 排程窗口结束日期
     * @param shortageThreshold 欠产增机台阈值
     * @return 提前生产结构化判定结果
     */
    public static EarlyProductionDecision checkEarlyProduction(LhScheduleContext context,
                                                               SkuScheduleDTO sku,
                                                               LocalDate currentDate,
                                                               LocalDate windowStartDate,
                                                               LocalDate windowEndDate,
                                                               int shortageThreshold) {
        if (Objects.isNull(context) || Objects.isNull(sku) || Objects.isNull(currentDate)
                || CollectionUtils.isEmpty(sku.getDailyPlanQuotaMap())) {
            return EarlyProductionDecision.notEarlyProduction(true, "非提前生产判定范围");
        }
        int earlyProductionDaysThreshold = resolveEarlyProductionDaysThreshold(context);
        LocalDate earlyProductionMaxDate = resolveEarlyProductionMaxDate(context, windowEndDate);
        boolean continuationAddMachineEarlyProduction =
                isEligibleContinuationAddMachineEarlyProduction(
                        context, sku, currentDate, windowEndDate, earlyProductionMaxDate);
        if (!continuationAddMachineEarlyProduction && !isEligibleNewProductionSku(sku)) {
            return EarlyProductionDecision.notEarlyProduction(false,
                    "非正规新增排产SKU或换活字块回流场景");
        }
        /*
         * 普通提前生产只处理当前日无计划的 SKU；续作增机提前处理的是“当前已有续作机台
         * 已覆盖当天计划、未来首次增机日需要增加物理机台”的独立业务场景，因此不能被
         * 当前日已有计划直接拦截。未来需求来源仍以 firstAddMachineProductionDate 为准。
         */
        if (!continuationAddMachineEarlyProduction && hasCurrentDayPlan(context, sku, currentDate)) {
            return EarlyProductionDecision.notEarlyProduction(true, "当前业务日已有日计划量");
        }
        LocalDate firstFuturePlanDate = continuationAddMachineEarlyProduction
                ? resolveContinuationAddMachineSourcePlanDate(
                        context, sku, currentDate, earlyProductionMaxDate)
                : resolveFirstFuturePlanDate(
                context, sku, currentDate, earlyProductionMaxDate);
        if (continuationAddMachineEarlyProduction
                && Objects.nonNull(firstFuturePlanDate)
                && Objects.nonNull(earlyProductionMaxDate)
                && firstFuturePlanDate.isAfter(earlyProductionMaxDate)) {
            String outOfRangeReason = "续作首次增机日超出提前生产范围";
            logEarlyProductionDecision(context, sku, currentDate, firstFuturePlanDate, 0,
                    context.getStructureScheduledMachineCount(currentDate, sku.getStructureName()),
                    context.getSkuScheduledMachineCount(
                            currentDate, sku.getMaterialCode(), sku.getProductStatus()),
                    shortageThreshold, earlyProductionDaysThreshold,
                    (int) ChronoUnit.DAYS.between(currentDate, firstFuturePlanDate),
                    resolveDayPlanQty(context, sku, firstFuturePlanDate), false,
                    outOfRangeReason);
            return EarlyProductionDecision.notEarlyProduction(false, outOfRangeReason);
        }
        if (Objects.isNull(firstFuturePlanDate)) {
            String noFuturePlanReason = "排程窗口外额外" + earlyProductionDaysThreshold
                    + "天范围内无日计划量";
            logEarlyProductionDecision(context, sku, currentDate, null, 0,
                    context.getStructureScheduledMachineCount(currentDate, sku.getStructureName()),
                    context.getSkuScheduledMachineCount(currentDate, sku.getMaterialCode(), sku.getProductStatus()),
                    shortageThreshold, earlyProductionDaysThreshold, 0, 0, false,
                    noFuturePlanReason);
            return EarlyProductionDecision.notEarlyProduction(false, noFuturePlanReason);
        }
        int futurePlanQty = resolveDayPlanQty(context, sku, firstFuturePlanDate);
        if (continuationAddMachineEarlyProduction && futurePlanQty <= 0) {
            String noAddMachineDayPlanReason = "续作首次增机日无有效原始日计划量";
            logEarlyProductionDecision(context, sku, currentDate, firstFuturePlanDate, 0,
                    context.getStructureScheduledMachineCount(currentDate, sku.getStructureName()),
                    context.getSkuScheduledMachineCount(
                            currentDate, sku.getMaterialCode(), sku.getProductStatus()),
                    shortageThreshold, earlyProductionDaysThreshold,
                    (int) ChronoUnit.DAYS.between(currentDate, firstFuturePlanDate),
                    futurePlanQty, false, noAddMachineDayPlanReason);
            return EarlyProductionDecision.notEarlyProduction(false, noAddMachineDayPlanReason);
        }
        int earlyDays = (int) ChronoUnit.DAYS.between(currentDate, firstFuturePlanDate);
        List<Integer> structurePlanMachineCounts = resolveWindowStructurePlanMachineCounts(
                context, sku.getStructureName(), windowStartDate);
        int currentPlanMachineCount = context.getStructurePlanMachineCount(
                currentDate, sku.getStructureName());
        int futurePlanMachineCount = context.getStructurePlanMachineCount(
                firstFuturePlanDate, sku.getStructureName());
        int planMachineCount = resolveEffectiveStructurePlanMachineCount(
                context, sku, currentDate, firstFuturePlanDate);
        int scheduledStructureCount = context.getStructureScheduledMachineCount(
                currentDate, sku.getStructureName());
        int scheduledSkuCount = context.getSkuScheduledMachineCount(
                currentDate, sku.getMaterialCode(), sku.getProductStatus());
        int threshold = Math.max(0, shortageThreshold);
        /*
         * 结构切换提前必须同时满足：当前日结构计划机台数为0、未来计划日结构计划机台数大于0，
         * 且同结构尚未形成有效续作排产时存在最早胎胚可供硫化时间。这里直接使用原始结构
         * 机台数识别场景，不能依赖最终sceneType反推，避免实际结构切换候选绕过胎胚时间门禁。
         * 普通结构提前和结构收尾提前不进入该分支，完整恢复本次调整前的原准入行为。
         */
        boolean structureSwitchEarlyProduction = isStructureSwitchEarlyProduction(
                context, sku, currentDate, firstFuturePlanDate);
        boolean structureScheduledInContinuation = structureSwitchEarlyProduction
                && NewSpecEmbryoAvailableTimeResolver.isStructureScheduledInCurrentContinuation(
                        context, sku);
        Date earliestEmbryoAvailableTime = structureSwitchEarlyProduction
                && !structureScheduledInContinuation
                ? NewSpecEmbryoAvailableTimeResolver.resolveEarliestAvailableTime(context, sku)
                : null;
        if (structureSwitchEarlyProduction && !structureScheduledInContinuation
                && Objects.isNull(earliestEmbryoAvailableTime)) {
            String missingEmbryoTimeReason =
                    "结构未配置最早胎胚可供硫化时间，禁止提前生产";
            logEarlyProductionDecision(context, sku, currentDate, firstFuturePlanDate,
                    planMachineCount, scheduledStructureCount, scheduledSkuCount,
                    threshold, earlyProductionDaysThreshold, earlyDays, futurePlanQty,
                    false, missingEmbryoTimeReason);
            return EarlyProductionDecision.earlyProduction(
                    false, EarlyProductionDecision.SCENE_STRUCTURE_SWITCH, firstFuturePlanDate,
                    structurePlanMachineCounts, missingEmbryoTimeReason);
        }
        if (structureScheduledInContinuation) {
            log.info("提前生产结构已有续作有效排产，胎胚最早可供时间不生效, factoryCode: {}, "
                            + "currentDate: {}, futurePlanDate: {}, materialCode: {}, structureName: {}",
                    context.getFactoryCode(), currentDate, firstFuturePlanDate,
                    sku.getMaterialCode(), sku.getStructureName());
        }
        if (planMachineCount > 0) {
            /*
             * SKU类型、未来计划日、结构切换和胎胚时间等基础条件通过后，结构机台数统一交给
             * 提前生产中心按“业务日最后一个班次”生成当天唯一资格。资格通过后，当天其他班次
             * 不再根据候选实际开产班次重复校验结构机台数。
             */
            boolean normalStructureEarlyProduction = currentPlanMachineCount > 0;
            logEarlyProductionDecision(context, sku, currentDate, firstFuturePlanDate, planMachineCount,
                    scheduledStructureCount, scheduledSkuCount, threshold,
                    earlyProductionDaysThreshold, earlyDays, futurePlanQty, true,
                    "提前生产进入结构当天最后班次机台数资格判断");
            String sceneType = continuationAddMachineEarlyProduction
                    ? EarlyProductionDecision.SCENE_CONTINUATION_ADD_MACHINE
                    : normalStructureEarlyProduction
                    ? EarlyProductionDecision.SCENE_NORMAL : EarlyProductionDecision.SCENE_STRUCTURE_SWITCH;
            if (currentPlanMachineCount == 0) {
                log.info("提前生产结构切换准入, currentDate: {}, futurePlanDate: {}, structureName: {}, "
                                + "currentPlanMachineCount: {}, futurePlanMachineCount: {}, "
                                + "scheduledStructureCount: {}, result: {}",
                        currentDate, firstFuturePlanDate, sku.getStructureName(),
                        currentPlanMachineCount, futurePlanMachineCount,
                        scheduledStructureCount, true);
            }
            String allowedReason = continuationAddMachineEarlyProduction
                    ? "续作增机提前生产进入结构当天最后班次机台数资格判断"
                    : "提前生产进入结构当天最后班次机台数资格判断";
            return EarlyProductionDecision.earlyProduction(true, sceneType, firstFuturePlanDate,
                    structurePlanMachineCounts, allowedReason);
        }
        /*
         * 历史欠产/收尾遗留阶段下线后，结构没有有效计划机台数时不得再使用历史欠产
         * 或收尾余量强制放行。保留STRUCTURE_ENDING场景编码只用于兼容既有日志和清理节奏。
         */
        boolean allowedByEndingSurplus = false;
        String noPlanMachineReason = "结构无有效计划机台数，禁止提前生产";
        logEarlyProductionDecision(context, sku, currentDate, firstFuturePlanDate, planMachineCount,
                scheduledStructureCount, scheduledSkuCount, threshold, earlyProductionDaysThreshold,
                earlyDays, futurePlanQty, allowedByEndingSurplus, noPlanMachineReason);
        return EarlyProductionDecision.earlyProduction(allowedByEndingSurplus,
                EarlyProductionDecision.SCENE_STRUCTURE_ENDING, firstFuturePlanDate,
                structurePlanMachineCounts, noPlanMachineReason);
    }

    /**
     * 按原始结构计划机台数识别结构切换提前场景。
     *
     * <p>必须直接比较当前业务日和最早未来计划日的结构计划机台数，不能使用最终
         * {@link EarlyProductionDecision#getSceneType()} 反推。“当前结构无计划、未来结构恢复计划”的
         * 结构切换提前，在同结构没有有效续作排产时受最早胎胚可供硫化时间约束。</p>
     *
     * @param context 排程上下文
     * @param sku 待判断 SKU
     * @param currentDate 当前业务日
     * @param firstFuturePlanDate 阈值内最早未来原始计划日
     * @return true-结构切换提前；false-普通结构提前、结构收尾提前或无有效上下文
     */
    public static boolean isStructureSwitchEarlyProduction(
            LhScheduleContext context,
            SkuScheduleDTO sku,
            LocalDate currentDate,
            LocalDate firstFuturePlanDate) {
        if (Objects.isNull(context) || Objects.isNull(sku)
                || Objects.isNull(currentDate) || Objects.isNull(firstFuturePlanDate)
                || StringUtils.isEmpty(sku.getStructureName())) {
            return false;
        }
        int currentPlanMachineCount = context.getStructurePlanMachineCount(
                currentDate, sku.getStructureName());
        int futurePlanMachineCount = context.getStructurePlanMachineCount(
                firstFuturePlanDate, sku.getStructureName());
        return currentPlanMachineCount == 0 && futurePlanMachineCount > 0;
    }

    /**
     * 获取排程窗口 T～T+2 的结构计划硫化机台数。
     *
     * @param context 排程上下文
     * @param structureName 产品结构
     * @param windowStartDate 排程窗口 T 日
     * @return 固定三个业务日的结构计划机台数
     */
    private static List<Integer> resolveWindowStructurePlanMachineCounts(LhScheduleContext context,
                                                                         String structureName,
                                                                         LocalDate windowStartDate) {
        List<Integer> machineCounts = new ArrayList<Integer>(3);
        if (Objects.isNull(context) || Objects.isNull(windowStartDate)
                || StringUtils.isEmpty(structureName)) {
            return machineCounts;
        }
        for (int dayOffset = 0; dayOffset < 3; dayOffset++) {
            machineCounts.add(context.getStructurePlanMachineCount(
                    windowStartDate.plusDays(dayOffset), structureName));
        }
        return machineCounts;
    }

    /**
     * 判断是否命中结构已收尾但 SKU 余量较大的强制加机台条件。
     * <p>历史欠产/收尾遗留阶段下线后，主提前生产准入不再调用本方法；暂时保留供后续
     * 关联代码统一清理。</p>
     *
     * @param context 排程上下文
     * @param sku SKU
     * @param currentDate 当前业务日期
     * @param firstFuturePlanDate 下一业务日计划日
     * @return true-命中结构收尾大余量；false-未命中
     */
    public static boolean isEndingStructureLargeSurplus(LhScheduleContext context,
                                                        SkuScheduleDTO sku,
                                                        LocalDate currentDate,
                                                        LocalDate firstFuturePlanDate) {
        // 历史欠产/收尾遗留阶段已下线，残留调用统一不得触发强制扩机。
        return false;
    }

    /**
     * 解析当前日后、指定固定截止日前最早有 dayN 日计划量的日期。
     * <p>该兼容入口由调用方显式传入固定截止日期，不再忽略 windowEndDate。</p>
     *
     * @param sku SKU
     * @param currentDate 当前业务日期
     * @param windowEndDate 提前生产固定截止日期
     * @return 最早未来计划日；阈值内无计划返回 null
     */
    public static LocalDate resolveFirstFuturePlanDate(SkuScheduleDTO sku,
                                                       LocalDate currentDate,
                                                       LocalDate windowEndDate) {
        return resolveFirstFuturePlanDate(null, sku, currentDate, windowEndDate);
    }

    /**
     * 解析当前日后、本次排程固定提前生产截止日前最早有 dayN 日计划量的日期。
     * <p>优先读取 SKU 运行态账本；账本未覆盖未来日期时，再按日期所属真实年月从上下文月计划读取，
     * 避免跨月、跨年提前生产误用 day32 或排程目标月。</p>
     *
     * @param context 排程上下文
     * @param sku SKU
     * @param currentDate 当前业务日期
     * @return 最早未来计划日；阈值内无计划返回 null
     */
    public static LocalDate resolveFirstFuturePlanDate(LhScheduleContext context,
                                                       SkuScheduleDTO sku,
                                                       LocalDate currentDate) {
        LocalDate earlyProductionMaxDate = resolveEarlyProductionMaxDate(context, null);
        return resolveFirstFuturePlanDate(
                context, sku, currentDate, earlyProductionMaxDate);
    }

    /**
     * 在指定闭区间结束日前查找最早未来正计划日。
     *
     * @param context 排程上下文
     * @param sku SKU
     * @param currentDate 当前业务日期，不包含当天
     * @param earlyProductionMaxDate 本次排程固定的最晚原始计划日期
     * @return 最早未来计划日；范围内无计划返回 null
     */
    public static LocalDate resolveFirstFuturePlanDate(LhScheduleContext context,
                                                       SkuScheduleDTO sku,
                                                       LocalDate currentDate,
                                                       LocalDate earlyProductionMaxDate) {
        if (Objects.isNull(sku) || Objects.isNull(currentDate)) {
            return null;
        }
        if (Objects.isNull(earlyProductionMaxDate)
                || !earlyProductionMaxDate.isAfter(currentDate)) {
            return null;
        }
        LocalDate date = currentDate.plusDays(1);
        while (!date.isAfter(earlyProductionMaxDate)) {
            if (resolveDayPlanQty(context, sku, date) > 0) {
                return date;
            }
            date = date.plusDays(1);
        }
        return null;
    }

    /**
     * 解析本次排程固定的提前生产最晚原始计划日期。
     * <p>正式排程优先读取上下文初始化时固化的日期；测试、历史内部调用未设置时，
     * 再按窗口结束日加参数天数计算，语义仍保持一致。</p>
     *
     * @param context 排程上下文
     * @param windowEndDate 调用方已经解析的排程窗口结束日
     * @return 固定截止日；缺少窗口信息时返回 null
     */
    public static LocalDate resolveEarlyProductionMaxDate(LhScheduleContext context,
                                                           LocalDate windowEndDate) {
        if (Objects.nonNull(context) && Objects.nonNull(context.getEarlyProductionMaxDate())) {
            return context.getEarlyProductionMaxDate().toInstant()
                    .atZone(ZoneId.systemDefault()).toLocalDate();
        }
        LocalDate effectiveWindowEndDate = windowEndDate;
        if (Objects.isNull(effectiveWindowEndDate)
                && Objects.nonNull(context) && Objects.nonNull(context.getWindowEndDate())) {
            effectiveWindowEndDate = context.getWindowEndDate().toInstant()
                    .atZone(ZoneId.systemDefault()).toLocalDate();
        }
        if (Objects.isNull(effectiveWindowEndDate)) {
            return null;
        }
        return effectiveWindowEndDate.plusDays(
                resolveEarlyProductionDaysThreshold(context));
    }

    /**
     * 解析当前日之后最早存在的原始未来计划日，不受提前生产天数阈值限制。
     *
     * <p>该方法只回答“是否存在未来计划”，用于区分应静默出队的纯历史遗留任务
     * 与尚未进入提前生产窗口的未来计划任务。提前生产正式准入仍必须调用
     * {@link #resolveFirstFuturePlanDate(LhScheduleContext, SkuScheduleDTO, LocalDate)}，
     * 两个判断范围不得混用。</p>
     *
     * @param context 排程上下文
     * @param sku SKU
     * @param currentDate 当前业务日期
     * @return 最早原始未来计划日；已加载计划范围内无未来计划时返回 null
     */
    public static LocalDate resolveFirstFutureOriginalPlanDate(LhScheduleContext context,
                                                               SkuScheduleDTO sku,
                                                               LocalDate currentDate) {
        if (Objects.isNull(sku) || Objects.isNull(currentDate)) {
            return null;
        }
        LocalDate scanEndDate = currentDate.withDayOfMonth(currentDate.lengthOfMonth());
        if (!CollectionUtils.isEmpty(sku.getDailyPlanQuotaMap())) {
            for (LocalDate quotaDate : sku.getDailyPlanQuotaMap().keySet()) {
                if (Objects.nonNull(quotaDate) && quotaDate.isAfter(scanEndDate)) {
                    scanEndDate = quotaDate;
                }
            }
        }
        if (Objects.nonNull(context)) {
            List<FactoryMonthPlanProductionFinalResult> originalPlanList =
                    CollectionUtils.isEmpty(context.getLoadedMonthPlanList())
                            ? context.getMonthPlanList() : context.getLoadedMonthPlanList();
            if (!CollectionUtils.isEmpty(originalPlanList)) {
                String productStatus = StringUtils.trimToEmpty(sku.getProductStatus());
                for (FactoryMonthPlanProductionFinalResult plan : originalPlanList) {
                    if (Objects.isNull(plan)
                            || !StringUtils.equals(sku.getMaterialCode(), plan.getMaterialCode())
                            || (StringUtils.isNotEmpty(productStatus)
                            && !StringUtils.equals(productStatus,
                            StringUtils.trimToEmpty(plan.getProductStatus())))
                            || Objects.isNull(plan.getYear()) || plan.getYear() <= 0
                            || Objects.isNull(plan.getMonth())
                            || plan.getMonth() < 1 || plan.getMonth() > 12) {
                        continue;
                    }
                    LocalDate planMonthEndDate = YearMonth.of(
                            plan.getYear(), plan.getMonth()).atEndOfMonth();
                    if (planMonthEndDate.isAfter(scanEndDate)) {
                        scanEndDate = planMonthEndDate;
                    }
                }
            }
        }
        LocalDate planDate = currentDate.plusDays(1);
        while (!planDate.isAfter(scanEndDate)) {
            int quotaPlanQty = 0;
            if (!CollectionUtils.isEmpty(sku.getDailyPlanQuotaMap())
                    && sku.getDailyPlanQuotaMap().containsKey(planDate)) {
                SkuDailyPlanQuotaDTO quota = sku.getDailyPlanQuotaMap().get(planDate);
                quotaPlanQty = Objects.isNull(quota) ? 0 : Math.max(0, quota.getDayPlanQty());
            }
            int originalMonthPlanQty = Objects.isNull(context) ? 0
                    : Math.max(0, MonthPlanDateResolver.resolveDayQty(
                    context, sku.getMaterialCode(), sku.getProductStatus(), planDate));
            if (quotaPlanQty > 0 || originalMonthPlanQty > 0) {
                return planDate;
            }
            planDate = planDate.plusDays(1);
        }
        return null;
    }

    /**
     * 判断当前日是否已有 dayN 日计划量。
     *
     * @param context 排程上下文
     * @param sku SKU
     * @param currentDate 当前业务日期
     * @return true-当前日有计划；false-当前日无计划
     */
    private static boolean hasCurrentDayPlan(LhScheduleContext context, SkuScheduleDTO sku, LocalDate currentDate) {
        return resolveDayPlanQty(context, sku, currentDate) > 0;
    }

    /**
     * 判断 SKU 是否属于提前生产允许的正规新增排产范围。
     *
     * <p>施工阶段 01/02 明确排除；续作补偿和换活字块回流只允许等待其原计划日期，
     * 不得通过提前生产主动拉取未来 SKU。空来源或 NORMAL_NEW_SPEC 均视为普通新增来源，
     * 兼容当前项目尚未统一回写来源编码的历史数据。</p>
     *
     * @param sku SKU
     * @return true-可进入提前生产判断；false-不适用
     */
    public static boolean isEligibleNewProductionSku(SkuScheduleDTO sku) {
        if (Objects.isNull(sku) || sku.isContinuousCompensationSku()) {
            return false;
        }
        if (StringUtils.isNotEmpty(sku.getScheduleType())
                && !StringUtils.equals(
                ScheduleTypeEnum.NEW_SPEC.getCode(), sku.getScheduleType())) {
            return false;
        }
        if (StringUtils.equals(ConstructionStageEnum.TRIAL.getCode(), sku.getConstructionStage())
                || StringUtils.equals(
                ConstructionStageEnum.MASS_TRIAL.getCode(), sku.getConstructionStage())) {
            return false;
        }
        return !StringUtils.equals(
                SkuScheduleSourceTypeEnum.TYPE_BLOCK_TO_NEW_SPEC.getCode(), sku.getSourceType())
                && !StringUtils.equals(
                SkuScheduleSourceTypeEnum.CONTINUATION_ADD_MACHINE.getCode(), sku.getSourceType());
    }

    /**
     * 判断是否为允许进入提前生产中心的正规续作增机补偿 SKU。
     *
     * <p>该场景必须已经由 S4.4 续作中心识别出未来首次增机日，并复制为 S4.5 新增链路候选。
     * 直接续作结果、普通新增、换活字块回流、试制和量试均不进入本分支。</p>
     *
     * @param sku 待判断 SKU
     * @param currentDate 当前业务日
     * @return true-可按首次增机日判断提前生产；false-保持原入口语义
     */
    public static boolean isEligibleContinuationAddMachineEarlyProduction(
            SkuScheduleDTO sku,
            LocalDate currentDate) {
        if (Objects.isNull(sku) || Objects.isNull(currentDate)
                || !sku.isContinuousCompensationSku()
                || !StringUtils.equals(
                SkuScheduleSourceTypeEnum.CONTINUATION_ADD_MACHINE.getCode(), sku.getSourceType())
                || Objects.isNull(sku.getFirstAddMachineProductionDate())
                || sku.getFirstAddMachineProductionDate().isBefore(currentDate)) {
            return false;
        }
        if (StringUtils.isNotEmpty(sku.getScheduleType())
                && !StringUtils.equals(
                ScheduleTypeEnum.NEW_SPEC.getCode(), sku.getScheduleType())) {
            return false;
        }
        return !StringUtils.equals(
                ConstructionStageEnum.TRIAL.getCode(), sku.getConstructionStage())
                && !StringUtils.equals(
                ConstructionStageEnum.MASS_TRIAL.getCode(), sku.getConstructionStage());
    }

    /**
     * 判断续作增机补偿是否允许在排程窗口最后业务日使用窗口外计划额度。
     *
     * <p>窗口内首次增机日继续由正常资源阶段处理，禁止再次向前提前；只有首次增机日
     * 位于窗口最后日或窗口外参数范围内时，才在窗口最后日进入共享提前生产中心。</p>
     *
     * @param context 排程上下文
     * @param sku 待判断SKU
     * @param currentDate 当前业务日
     * @return true-允许进入续作增机提前生产；false-保持原日期门禁
     */
    public static boolean isEligibleContinuationAddMachineEarlyProduction(
            LhScheduleContext context,
            SkuScheduleDTO sku,
            LocalDate currentDate) {
        LocalDate windowEndDate = Objects.isNull(context) || Objects.isNull(context.getWindowEndDate())
                ? null : context.getWindowEndDate().toInstant()
                .atZone(ZoneId.systemDefault()).toLocalDate();
        LocalDate earlyProductionMaxDate = resolveEarlyProductionMaxDate(context, windowEndDate);
        return isEligibleContinuationAddMachineEarlyProduction(
                context, sku, currentDate, windowEndDate, earlyProductionMaxDate);
    }

    private static boolean isEligibleContinuationAddMachineEarlyProduction(
            LhScheduleContext context,
            SkuScheduleDTO sku,
            LocalDate currentDate,
            LocalDate windowEndDate,
            LocalDate earlyProductionMaxDate) {
        if (!isEligibleContinuationAddMachineEarlyProduction(sku, currentDate)
                || Objects.isNull(context) || Objects.isNull(windowEndDate)
                || Objects.isNull(earlyProductionMaxDate)
                || !currentDate.equals(windowEndDate)) {
            return false;
        }
        return !sku.getFirstAddMachineProductionDate().isAfter(earlyProductionMaxDate);
    }

    /**
     * 解析续作增机提前实际借用的原始计划来源日。
     *
     * <p>首次增机日在窗口外时直接使用该日；首次增机日等于窗口最后日时，说明统一
     * 目标机台数Map已基于后续高计划提前给出增机日期，此时从下一正计划日借用额度，
     * 避免再次把窗口内较小日计划当成新增机台生产目标。</p>
     */
    private static LocalDate resolveContinuationAddMachineSourcePlanDate(
            LhScheduleContext context,
            SkuScheduleDTO sku,
            LocalDate currentDate,
            LocalDate earlyProductionMaxDate) {
        LocalDate firstAddMachineDate = sku.getFirstAddMachineProductionDate();
        if (Objects.nonNull(firstAddMachineDate) && firstAddMachineDate.isAfter(currentDate)) {
            return firstAddMachineDate;
        }
        return resolveFirstFuturePlanDate(
                context, sku, currentDate, earlyProductionMaxDate);
    }

    /**
     * 解析当前业务日应使用的本月前日累计欠产。
     *
     * @param context 排程上下文
     * @param sku SKU
     * @param currentDate 当前业务日期
     * @return 动态历史欠产量
     */
    public static int resolveHistoryShortageQty(LhScheduleContext context,
                                                SkuScheduleDTO sku,
                                                LocalDate currentDate) {
        if (Objects.isNull(sku)) {
            return 0;
        }
        if (Objects.nonNull(context) && Objects.nonNull(currentDate)) {
            int cachedShortageQty = context.getMonthlyHistoryShortageQty(
                    currentDate, sku.getMaterialCode(), sku.getProductStatus());
            Map<LocalDate, Map<String, Integer>> shortageMap =
                    context.getMonthlyHistoryShortageQtyMap();
            if (!CollectionUtils.isEmpty(shortageMap) && shortageMap.containsKey(currentDate)) {
                return Math.max(0, cachedShortageQty);
            }
        }
        return Math.max(0, sku.getMonthlyHistoryShortageQty());
    }

    /**
     * 解析指定日期的 SKU 原始 dayN 日计划量。
     *
     * @param context 排程上下文
     * @param sku SKU
     * @param productionDate 计划日期
     * @return 日计划量
     */
    private static int resolveDayPlanQty(LhScheduleContext context, SkuScheduleDTO sku, LocalDate productionDate) {
        if (Objects.isNull(sku) || Objects.isNull(productionDate)) {
            return 0;
        }
        Map<LocalDate, SkuDailyPlanQuotaDTO> quotaMap = sku.getDailyPlanQuotaMap();
        if (!CollectionUtils.isEmpty(quotaMap) && quotaMap.containsKey(productionDate)) {
            SkuDailyPlanQuotaDTO quota = quotaMap.get(productionDate);
            return Objects.isNull(quota) ? 0 : Math.max(0, quota.getDayPlanQty());
        }
        if (Objects.isNull(context)) {
            return 0;
        }
        return Math.max(0, MonthPlanDateResolver.resolveDayQty(
                context, sku.getMaterialCode(), sku.getProductStatus(), productionDate));
    }

    /**
     * 解析 SKU 提前生产窗口外额外拉取天数。
     *
     * @param context 排程上下文
     * @return 窗口外额外拉取天数，范围1～31
     */
    public static int resolveEarlyProductionDaysThreshold(LhScheduleContext context) {
        int threshold;
        if (Objects.nonNull(context) && Objects.nonNull(context.getScheduleConfig())) {
            threshold = context.getScheduleConfig().getEarlyProductionDaysThreshold();
        } else if (Objects.nonNull(context)) {
            threshold = context.getParamIntValue(LhScheduleParamConstant.EARLY_PRODUCTION_DAYS_THRESHOLD,
                    LhScheduleConstant.DEFAULT_EARLY_PRODUCTION_DAYS_THRESHOLD);
        } else {
            threshold = LhScheduleConstant.DEFAULT_EARLY_PRODUCTION_DAYS_THRESHOLD;
        }
        if (threshold <= 0) {
            return LhScheduleConstant.DEFAULT_EARLY_PRODUCTION_DAYS_THRESHOLD;
        }
        return Math.min(threshold, LhScheduleConstant.MAX_EARLY_PRODUCTION_DAYS_THRESHOLD);
    }

    /**
     * 判断日计划额度是否有原始 dayN 计划量。
     *
     * @param quota 日计划额度
     * @return true-有 dayN 计划量；false-无 dayN 计划量
     */
    private static boolean hasDayPlan(SkuDailyPlanQuotaDTO quota) {
        return Objects.nonNull(quota) && Math.max(0, quota.getDayPlanQty()) > 0;
    }

    /**
     * 获取结构计划机台数；当前日为0时，按结构切换规则只改取下一业务日。
     *
     * @param context 排程上下文
     * @param sku SKU
     * @param currentDate 当前业务日期
     * @param firstFuturePlanDate 下一业务日计划日
     * @return 用于准入判断的结构计划机台数
     */
    public static int resolveEffectiveStructurePlanMachineCount(LhScheduleContext context,
                                                                SkuScheduleDTO sku,
                                                                LocalDate currentDate,
                                                                LocalDate firstFuturePlanDate) {
        if (Objects.isNull(context) || Objects.isNull(sku) || StringUtils.isEmpty(sku.getStructureName())) {
            return 0;
        }
        int currentPlanMachineCount = context.getStructurePlanMachineCount(currentDate, sku.getStructureName());
        if (currentPlanMachineCount > 0) {
            return currentPlanMachineCount;
        }
        int futurePlanMachineCount = context.getStructurePlanMachineCount(
                firstFuturePlanDate, sku.getStructureName());
        if (futurePlanMachineCount > 0) {
            log.info("提前生产结构切换判断, factoryCode: {}, currentDate: {}, futurePlanDate: {}, "
                            + "structureName: {}, currentPlanMachineCount: {}, futurePlanMachineCount: {}",
                    context.getFactoryCode(), currentDate, firstFuturePlanDate, sku.getStructureName(),
                    currentPlanMachineCount, futurePlanMachineCount);
        }
        return futurePlanMachineCount;
    }

    /**
     * 按目标班次和候选物理机台校验提前生产是否可以使用当前机台。
     *
     * <p>候选机台已经计入同结构、同班次时允许复用；否则只有当前实时物理机台数严格小于
     * 有效计划机台数时才允许新增。单控L/R由结构在机索引统一按物理整机去重。</p>
     *
     * @param context 排程上下文
     * @param sku 当前SKU
     * @param currentDate 当前业务日
     * @param futurePlanDate 提前生产来源计划日
     * @param shiftIndex 提案正式目标班次
     * @param machineCode 候选运行态机台编码
     * @return true-允许复用或新增；false-结构机台数已达上限
     */
    public static boolean canUseMachineForEarlyProduction(
            LhScheduleContext context,
            SkuScheduleDTO sku,
            LocalDate currentDate,
            LocalDate futurePlanDate,
            int shiftIndex,
            String machineCode) {
        if (Objects.isNull(context) || Objects.isNull(sku)
                || StringUtils.isEmpty(sku.getStructureName())
                || Objects.isNull(context.getStructureShiftInMachineIndex())
                || shiftIndex <= 0 || StringUtils.isEmpty(machineCode)) {
            return false;
        }
        int planMachineCount = resolveEffectiveStructurePlanMachineCount(
                context, sku, currentDate, futurePlanDate);
        if (planMachineCount <= 0) {
            return false;
        }
        if (context.getStructureShiftInMachineIndex().containsPhysicalMachine(
                sku.getStructureName(), shiftIndex, machineCode)) {
            return true;
        }
        return context.getStructureShiftInMachineIndex().resolveInMachineCount(
                sku.getStructureName(), shiftIndex) < planMachineCount;
    }

    /**
     * 输出提前生产准入判断日志。
     *
     * @param context 排程上下文
     * @param sku SKU
     * @param currentDate 当前业务日期
     * @param futurePlanDate 后续计划日
     * @param planMachineCount 计划机台数
     * @param scheduledStructureCount 结构已排机台数
     * @param scheduledSkuCount SKU已排机台数
     * @param threshold 欠产阈值
     * @param earlyProductionDaysThreshold 提前生产天数阈值
     * @param earlyDays 实际提前自然日
     * @param futurePlanQty 未来计划日原始日计划量
     * @param allowed 是否允许进入新增判断
     * @param reason 原因
     */
    private static void logEarlyProductionDecision(LhScheduleContext context,
                                                   SkuScheduleDTO sku,
                                                   LocalDate currentDate,
                                                   LocalDate futurePlanDate,
                                                   int planMachineCount,
                                                   int scheduledStructureCount,
                                                   int scheduledSkuCount,
                                                   int threshold,
                                                   int earlyProductionDaysThreshold,
                                                   int earlyDays,
                                                   int futurePlanQty,
                                                   boolean allowed,
                                                   String reason) {
        Date configuredEarliestEmbryoAvailableTime =
                NewSpecEmbryoAvailableTimeResolver.resolveEarliestAvailableTime(context, sku);
        boolean structureScheduledInContinuation =
                NewSpecEmbryoAvailableTimeResolver.isStructureScheduledInCurrentContinuation(context, sku);
        Date effectiveEarliestEmbryoAvailableTime = structureScheduledInContinuation
                ? null : configuredEarliestEmbryoAvailableTime;
        LocalDate earlyProductionMaxDate = resolveEarlyProductionMaxDate(context, null);
        log.info("提前生产准入判断, factoryCode: {}, batchNo: {}, currentDate: {}, futurePlanDate: {}, "
                        + "earlyProductionMaxDate: {}, "
                        + "materialCode: {}, "
                        + "structureName: {}, historyShortageQty: {}, threshold: {}, planMachineCount: {}, "
                        + "scheduledStructureCount: {}, scheduledSkuCount: {}, dailyQty: {}, "
                        + "earlyProductionDaysThreshold: {}, earlyDays: {}, futurePlanQty: {}, "
                        + "earliestEmbryoAvailableTime: {}, effectiveEarliestEmbryoAvailableTime: {}, "
                        + "structureScheduledInContinuation: {}, result: {}, reason: {}",
                context.getFactoryCode(), context.getBatchNo(), currentDate, futurePlanDate,
                earlyProductionMaxDate,
                sku.getMaterialCode(),
                sku.getStructureName(), resolveHistoryShortageQty(context, sku, currentDate), threshold,
                planMachineCount, scheduledStructureCount, scheduledSkuCount,
                Math.max(0, sku.getDailyCapacity()), earlyProductionDaysThreshold, earlyDays,
                futurePlanQty, configuredEarliestEmbryoAvailableTime,
                effectiveEarliestEmbryoAvailableTime, structureScheduledInContinuation,
                allowed, reason);
    }
}
