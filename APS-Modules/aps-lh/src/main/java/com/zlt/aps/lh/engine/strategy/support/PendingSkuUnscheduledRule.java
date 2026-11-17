package com.zlt.aps.lh.engine.strategy.support;

import com.zlt.aps.lh.api.domain.dto.SkuDailyPlanQuotaDTO;
import com.zlt.aps.lh.api.domain.dto.SkuScheduleDTO;
import com.zlt.aps.lh.api.domain.entity.LhUnscheduledResult;
import com.zlt.aps.lh.api.enums.ConstructionStageEnum;
import com.zlt.aps.lh.api.enums.UnscheduledReasonEnum;
import com.zlt.aps.lh.component.EarlyProductionQuantityCalculator;
import com.zlt.aps.lh.component.EmbryoEndingStockAdmissionPolicy;
import com.zlt.aps.lh.component.MonthPlanDateResolver;
import com.zlt.aps.lh.component.OverdueSurplusAdmissionPolicy;
import com.zlt.aps.lh.context.LhScheduleContext;
import com.zlt.aps.lh.util.LhScheduleTimeUtil;
import com.zlt.aps.lh.util.PriorityTraceLogHelper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.CollectionUtils;

import java.text.MessageFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Objects;

/**
 * 待排SKU前置未排规则。
 *
 * <p>统一处理新增SKU日计划准入、窗口无计划且无欠产余量未排、纯遗留SKU静默出队、
 * 交班停产无胎胚、前段欠产已下机、收尾小余量和仅历史欠产规则，
 * 以及硫化参数SYS0311004=0时新增排产链路试制量试SKU的参数拦截。
 * 换活字块和新增排产必须复用本规则，避免SKU在S4.4被提前消费后绕过S4.5判断。</p>
 *
 * @author APS
 */
@Slf4j
public final class PendingSkuUnscheduledRule {

    /** 非续作SKU在排程及提前生产范围内无日计划的统一未排原因 */
    public static final String DAILY_PLAN_ADMISSION_UNSCHEDULED_REASON =
            "排程窗口及提前生产范围内无日计划量";

    /** 排程窗口内无月计划量且无欠产余量标识的统一未排原因 */
    public static final String NO_OVERDUE_SURPLUS_UNSCHEDULED_REASON =
            "排程窗口内无月计划量且无欠产余量，不参与排产";

    /** 续作试制、量试SKU完整判断范围无日计划量的统一未排原因 */
    public static final String CONTINUOUS_TRIAL_DAILY_PLAN_ADMISSION_UNSCHEDULED_REASON =
            "试制、量试月计划排产量全部为0，跳过排产";

    /** 硫化参数SYS0311004=0时，新增排产链路试制、量试SKU的统一未排原因（续作链路不受该参数影响） */
    public static final String NEW_SPEC_TRIAL_EXCLUSION_UNSCHEDULED_REASON =
            "新增排产试制量试不参与排产（SYS0311004=0）";

    /** 仅历史欠产且最近一次已有完成量的统一未排原因 */
    public static final String HISTORY_SHORTAGE_UNSCHEDULED_REASON =
            "仅历史欠产、后续无月计划，且最近一次（前一次）已有完成量，本次跳过不排";

    /** 历史欠产/收尾遗留阶段下线后的纯遗留SKU出队原因 */
    public static final String LEGACY_ONLY_EXCLUSION_REASON =
            "历史欠产/收尾遗留阶段已下线，当前及未来无原始日计划，不进入本次排程";

    /** 自动排程数据来源 */
    private static final String AUTO_DATA_SOURCE = "0";

    private PendingSkuUnscheduledRule() {
    }

    /**
     * 根据已冻结的MES下机证据登记前段欠产净余量未排。
     * 未来计划不能恢复已完成前段的生产资格，原始余量和资源事实保持不变。
     *
     * @param context 本轮工厂、日期及批次
     * @param sku 原始窗口计划与前段余量来源
     * @param onlineEvidence 物料d1及全部来源机台d2证据，空值表示未证明完成
     * @return 命中时返回净余量未排，否则返回null
     */
    public static LhUnscheduledResult evaluateCompletedOverdueSurplus(
            LhScheduleContext context, SkuScheduleDTO sku, String onlineEvidence) {
        if (!OverdueSurplusAdmissionPolicy.requiresOnlineEvidence(context, sku)
                || StringUtils.isBlank(onlineEvidence)) {
            return null;
        }
        UnscheduledReasonEnum reason = UnscheduledReasonEnum.OVERDUE_SURPLUS_STAGE_COMPLETED;
        String detail = String.format("factoryCode=%s,batchNo=%s,scheduleDate=%s,T=%s,materialCode=%s,"
                        + "productStatus=%s,overdueSurplusFlag=true,originalWindowPlanQty=%d,surplusQty=%d,%s",
                context.getFactoryCode(), context.getBatchNo(),
                LhScheduleTimeUtil.formatDate(context.getScheduleTargetDate()),
                LhScheduleTimeUtil.formatDate(context.getScheduleDate()), sku.getMaterialCode(),
                sku.getProductStatus(), sku.getOriginalWindowPlanQty(), Math.max(0, sku.getSurplusQty()),
                onlineEvidence);
        log.info("前段欠产已下机且窗口无计划，禁止二次上机, {}", detail);
        PriorityTraceLogHelper.appendProcessLog(context, "前段欠产已下机准入拦截", detail);
        LhUnscheduledResult result = buildUnscheduledResult(
                context, sku, sku.getSurplusQty(), reason.getSummary(), reason, detail);
        result.setMonthPlanVersion(sku.getMonthPlanVersion());
        result.setProductionVersion(sku.getProductionVersion());
        return result;
    }

    /**
     * 判断收尾来源是否因无本轮可用胎胚而禁止实机生产。
     * 胎胚库存收尾不依赖交班停产证据，原交班停产规则继续覆盖其余来源。
     *
     * @param context 本轮收尾标识及库存快照
     * @param sku 当前物料及产品状态
     * @return 任一零库存收尾规则命中时返回true
     */
    public static boolean isEndingWithoutStock(LhScheduleContext context, SkuScheduleDTO sku) {
        return isHandoverStoppedWithoutStock(context, sku)
                || EmbryoEndingStockAdmissionPolicy.isEmbryoEndingWithoutStock(context, sku);
    }

    /**
     * 生成零库存收尾禁排说明，保留原硫化需求及历史在机事实。
     * 交班停产沿用原原因，胎胚收尾零库存使用独立原因并保留零量记录。
     *
     * @param context 本轮收尾、库存及批次信息
     * @param sku 当前物料及产品状态来源
     * @return 命中时返回零量未排说明，否则返回null
     */
    public static LhUnscheduledResult evaluateEndingWithoutStock(LhScheduleContext context, SkuScheduleDTO sku) {
        LhUnscheduledResult handoverStopped = evaluateHandoverStoppedWithoutStock(context, sku);
        if (Objects.nonNull(handoverStopped)) {
            return handoverStopped;
        }
        if (!EmbryoEndingStockAdmissionPolicy.isEmbryoEndingWithoutStock(context, sku)) {
            return null;
        }
        UnscheduledReasonEnum reason = UnscheduledReasonEnum.EMBRYO_ENDING_NO_STOCK;
        String detail = MessageFormat.format(
                "工厂={0}，排程日期={1}，T日={2}，批次={3}，物料={4}，状态={5}，胎胚={6}，"
                        + "存在本轮库存记录={7}，本轮库存原值={8}，硫化余量={9}，合法库存生产目标=0；"
                        + "胎胚库存收尾无库存，历史交替身份不豁免",
                context.getFactoryCode(), LhScheduleTimeUtil.formatDate(context.getScheduleTargetDate()),
                LhScheduleTimeUtil.formatDate(context.getScheduleDate()), context.getBatchNo(),
                sku.getMaterialCode(), sku.getProductStatus(), sku.getEmbryoCode(),
                context.getEmbryoRealtimeStockMap().containsKey(sku.getEmbryoCode()),
                context.getEmbryoRealtimeStockMap().get(sku.getEmbryoCode()), Math.max(0, sku.getSurplusQty()));
        log.info("胎胚库存收尾无库存不排产, {}", detail);
        PriorityTraceLogHelper.appendProcessLog(context, "胎胚库存收尾无库存不排产", detail);
        LhUnscheduledResult result = buildUnscheduledResult(context, sku, 0, reason.getSummary(), reason, detail);
        result.setMonthPlanVersion(sku.getMonthPlanVersion());
        result.setProductionVersion(sku.getProductionVersion());
        return result;
    }

    /**
     * 判断已确认交班停产收尾的胎胚是否没有本轮可用库存。
     * 本方法保留交班停产判定；全部零库存收尾准入使用isEndingWithoutStock。
     *
     * @param context 本轮交班停产证据及T日库存快照
     * @param sku 待判断的物料及产品状态来源
     * @return 已确认交班停产且本轮库存缺失或非正时返回true
     */
    public static boolean isHandoverStoppedWithoutStock(LhScheduleContext context, SkuScheduleDTO sku) {
        return Objects.nonNull(sku) && isHandoverStoppedWithoutStock(context, sku.getEmbryoCode());
    }

    /**
     * 按胎胚判断交班停产后的可用库存，供历史前料来源复用相同禁排口径。
     *
     * @param context 本轮交班停产证据及T日库存快照
     * @param embryoCode 实际来源的胎胚编码
     * @return 已确认交班停产且本轮库存缺失或非正时返回true
     */
    public static boolean isHandoverStoppedWithoutStock(LhScheduleContext context, String embryoCode) {
        if (Objects.isNull(context) || StringUtils.isEmpty(embryoCode)
                || !context.getCxHandoverStoppedEndingEmbryoSet().contains(embryoCode)) {
            return false;
        }
        Integer stockQty = context.getEmbryoRealtimeStockMap().get(embryoCode);
        return Objects.isNull(stockQty) || stockQty <= 0;
    }

    /**
     * 为交班已停产且无可用胎胚的来源构造未排说明，不改写需求或资源事实。
     * 合法库存生产目标为0，仍保留未排说明，硫化余量在详情中独立记录。
     *
     * @param context 本轮交班停产证据及库存快照
     * @param sku 待评估的月计划、续作或独立来源
     * @return 命中时返回零库存未排说明，未命中返回null
     */
    public static LhUnscheduledResult evaluateHandoverStoppedWithoutStock(
            LhScheduleContext context, SkuScheduleDTO sku) {
        if (!isHandoverStoppedWithoutStock(context, sku)) {
            return null;
        }
        UnscheduledReasonEnum reason = UnscheduledReasonEnum.HANDOVER_STOPPED_NO_EMBRYO_STOCK;
        String detail = MessageFormat.format(
                "工厂={0}，排程日期={1}，T日={2}，批次={3}，物料={4}，状态={5}，胎胚={6}，"
                        + "存在本轮库存记录={7}，本轮库存={8}，硫化余量={9}，合法库存生产目标=0；交班已停产收尾且无可用胎胚",
                context.getFactoryCode(), LhScheduleTimeUtil.formatDate(context.getScheduleTargetDate()),
                LhScheduleTimeUtil.formatDate(context.getScheduleDate()),
                context.getBatchNo(), sku.getMaterialCode(), sku.getProductStatus(), sku.getEmbryoCode(),
                context.getEmbryoRealtimeStockMap().containsKey(sku.getEmbryoCode()),
                context.getEmbryoRealtimeStockMap().getOrDefault(sku.getEmbryoCode(), 0),
                Math.max(0, sku.getSurplusQty()));
        log.info("交班停产收尾无胎胚不排产, {}", detail);
        PriorityTraceLogHelper.appendProcessLog(context, "交班停产收尾无胎胚不排产", detail);
        LhUnscheduledResult result = buildUnscheduledResult(context, sku, 0,
                reason.getSummary(), reason, detail);
        result.setMonthPlanVersion(sku.getMonthPlanVersion());
        result.setProductionVersion(sku.getProductionVersion());
        return result;
    }

    /**
     * 判断正规新增SKU是否只剩历史欠产或收尾遗留目标。
     *
     * <p>窗口原始DAY_N为0，并且从窗口结束日之后到已加载月计划末日都没有正计划量时，
     * 该SKU不属于正常排产或提前生产范围。调用方应在待排池入口直接移除，不生成未排记录，
     * 也不允许前日T+1交替计划作为无月计划场景的放行依据。</p>
     *
     * <p>试制、量试、续作补偿、换活字块回流等非正规新增来源继续执行各自既有规则；
     * 当前月TOTAL_QTY为0但已注册未来计划候选的SKU必须保留，等待提前生产阶段逐日准入。</p>
     *
     * <p>后半段无计划的单胎胚或非共用胎胚正库存收尾属于有效库存需求，不作为纯遗留目标剔除。</p>
     *
     * @param context 排程上下文
     * @param sku 待评估SKU
     * @return true-只剩历史欠产或收尾遗留目标，应静默移出；false-继续既有准入流程
     */
    public static boolean shouldExcludeLegacyOnlyNewSku(LhScheduleContext context,
                                                         SkuScheduleDTO sku) {
        if (Objects.isNull(context) || Objects.isNull(sku)
                || Objects.isNull(context.getWindowEndDate())
                || !EarlyProductionChecker.isEligibleNewProductionSku(sku)
                || sku.getOriginalWindowPlanQty() > 0
                || EmbryoEndingStockAdmissionPolicy.isEligible(context, sku)
                || context.isFutureOnlyEarlyProductionCandidate(sku)) {
            return false;
        }
        LocalDate windowEndDate = toLocalDate(context.getWindowEndDate());
        return Objects.nonNull(windowEndDate)
                && Objects.isNull(EarlyProductionChecker.resolveFirstFutureOriginalPlanDate(
                context, sku, windowEndDate));
    }

    /**
     * 评估非续作SKU是否因完整准入范围无日计划量而直接进入未排。
     * <p>判断范围由两部分组成：SKU已初始化的排程窗口计划量覆盖窗口首日至窗口末日；窗口结束日之后
     * 复用提前生产判断器，按硫化参数配置的提前生产天数阈值继续向后查找。
     * 完整范围无正计划量时拒绝本次排产，历史交替关系不授予额外资格。</p>
     * <p>试制、量试SKU为特殊分支：不叠加提前生产天数阈值，仅按排程窗口首日~窗口末日判断日计划量，
     * 窗口内有正计划量即放行；窗口内全为0时退出续作、换活字块和真实机台新增排产并先写未排，
     * 且不检查前日T+1交替计划。调用方可在清理前仅为有硫化余量的试制/量试保留S4.5.3虚拟机台快照，
     * 但不得将其重新放回其他排产阶段。</p>
     * <p>日计划严格沿用物料编码和产品状态复合维度，不以历史交替日期替代本次实际需求日期。</p>
     * <p>本方法只负责判断和构造未排结果，不修改SKU目标量、排产集合、胎胚库存或日计划账本。</p>
     *
     * <p>后半段无计划的单胎胚或非共用胎胚正库存收尾不依赖原始日计划准入。</p>
     *
     * @param context 排程上下文，提供排程窗口、提前生产参数和跨月月计划数据
     * @param sku 待评估的新增SKU
     * @return 命中规则时返回未排结果；未命中返回null
     */
    public static LhUnscheduledResult evaluateDailyPlanAdmission(LhScheduleContext context,
                                                                  SkuScheduleDTO sku) {
        if (Objects.isNull(context) || Objects.isNull(sku)
                || Objects.isNull(context.getWindowEndDate())
                || EmbryoEndingStockAdmissionPolicy.isEligible(context, sku)) {
            return null;
        }
        LocalDate windowStartDate = toLocalDate(context.getScheduleDate());
        LocalDate windowEndDate = toLocalDate(context.getWindowEndDate());
        if (Objects.isNull(windowEndDate)) {
            return null;
        }

        // 试制、量试SKU仅按排程窗口（首日~末日）判断日计划量，不叠加提前生产天数阈值；
        // 其余SKU沿用完整准入范围（窗口+提前生产天数阈值）。
        boolean isTrialSku = isTrialOrMassTrialSku(sku);
        if (isTrialSku) {
            if (hasPositiveDailyPlanInWindow(sku)) {
                return null;
            }
        } else if (hasPositiveDailyPlanInAdmissionRange(context, sku, windowEndDate)) {
            // 窗口内原始日计划和窗口后N天任一有量时，保留SKU继续走现有排产主链。
            return null;
        }

        // 试制、量试不适用提前生产天数阈值，判断截止日即窗口末日；其余SKU复用提前生产参数。
        int earlyProductionDaysThreshold = isTrialSku ? 0
                : EarlyProductionChecker.resolveEarlyProductionDaysThreshold(context);
        LocalDate admissionEndDate = windowEndDate.plusDays(earlyProductionDaysThreshold);
        String detail = String.format("工厂: %s, 批次: %s, 物料: %s, 产品状态: %s, 施工阶段: %s, "
                        + "排程窗口: %s～%s, 窗口原始计划量: %d, 提前生产天数: %d, 准入截止日: %s, "
                        + "原因: %s",
                context.getFactoryCode(), context.getBatchNo(), sku.getMaterialCode(), sku.getProductStatus(),
                sku.getConstructionStage(), windowStartDate, windowEndDate, sku.getOriginalWindowPlanQty(),
                earlyProductionDaysThreshold, admissionEndDate,
                DAILY_PLAN_ADMISSION_UNSCHEDULED_REASON);
        log.info("非续作SKU日计划准入拦截, factoryCode: {}, batchNo: {}, materialCode: {}, "
                        + "productStatus: {}, constructionStage: {}, windowStartDate: {}, windowEndDate: {}, "
                        + "originalWindowPlanQty: {}, earlyProductionDaysThreshold: {}, admissionEndDate: {}, "
                        + "reason: {}",
                context.getFactoryCode(), context.getBatchNo(), sku.getMaterialCode(), sku.getProductStatus(),
                sku.getConstructionStage(), windowStartDate, windowEndDate, sku.getOriginalWindowPlanQty(),
                earlyProductionDaysThreshold, admissionEndDate,
                DAILY_PLAN_ADMISSION_UNSCHEDULED_REASON);
        PriorityTraceLogHelper.appendProcessLog(context, "SKU无计划量不排产", detail);
        LhUnscheduledResult unscheduledResult = buildUnscheduledResult(
                context, sku, 0, DAILY_PLAN_ADMISSION_UNSCHEDULED_REASON,
                UnscheduledReasonEnum.NO_DAILY_PLAN_IN_FULL_RANGE, detail);
        // 月计划版本和生产版本属于本次准入未排结果的对账字段，仅在新增规则命中时回填，
        // 避免扩展公共构造方法后改变既有收尾小余量、仅历史欠产规则的落库内容。
        unscheduledResult.setMonthPlanVersion(sku.getMonthPlanVersion());
        unscheduledResult.setProductionVersion(sku.getProductionVersion());
        return unscheduledResult;
    }

    /**
     * 评估排程窗口内无月计划日计划量且无欠产余量标识的SKU是否直接进入未排。
     * <p>判断范围严格限制为排程窗口首日～窗口末日的原始DAY_N汇总（originalWindowPlanQty），
     * 窗口内无计划量且欠产余量标识为false时，通常列入未排；正规新增候选在固定提前
     * 生产截止日内存在未来原始计划时保留候选资格，后续只能经提前生产中心和资源门禁排产。
     * 不得单凭硫化余量、调整量或账面结转放行，也不得为试制量试扩大观察窗口。</p>
     * <p>SKU尚未完成续作分类时，调用方须使用带显式续作身份的重载，防止在机及历史前料
     * 借新增提前生产例外绕过门禁。停产收尾模式由调用方豁免；本方法只负责判断和
     * 构造未排结果，不修改SKU目标量、排产集合、胎胚库存或日计划账本。</p>
     * <p>后半段无计划且为单胎胚或非共用胎胚、胎胚收尾且本轮库存为正时保留资格，后续按库存硬目标及资源约束排产。</p>
     *
     * @param context 排程上下文
     * @param sku 待评估SKU
     * @return 命中规则时返回未排结果；未命中返回null
     */
    public static LhUnscheduledResult evaluateNoOverdueSurplusAdmission(LhScheduleContext context,
                                                                        SkuScheduleDTO sku) {
        return evaluateNoOverdueSurplusAdmission(context, sku, false);
    }

    /**
     * 按已确认的续作身份评估窗口无计划且无欠产余量的统一未排准入。
     * <p>真实MES在机及已证明历史前料均不适用新增提前生产例外。仅构造未排结果，
     * 不清理MES、模具事实，不消费生产、日计划或胎胚账本。</p>
     *
     * @param context 排程上下文
     * @param sku 待评估SKU
     * @param continuationCandidate 是否为归集阶段已确认的在机或历史前料续作候选
     * @return 命中规则时返回未排结果；窗口有计划、存在欠产余量或合法新增提前候选时返回null
     */
    public static LhUnscheduledResult evaluateNoOverdueSurplusAdmission(
            LhScheduleContext context, SkuScheduleDTO sku, boolean continuationCandidate) {
        if (Objects.isNull(context) || Objects.isNull(sku)
                || sku.getOriginalWindowPlanQty() > 0
                || sku.isOverdueSurplusFlag()
                || EmbryoEndingStockAdmissionPolicy.isEligible(context, sku)
                || (!continuationCandidate && hasEligibleFutureOriginalPlan(context, sku))) {
            return null;
        }
        LocalDate windowStartDate = toLocalDate(context.getScheduleDate());
        LocalDate windowEndDate = toLocalDate(context.getWindowEndDate());
        int surplusQty = Math.max(0, sku.getSurplusQty());
        String detail = String.format("工厂: %s, 批次: %s, 物料: %s, 产品状态: %s, 施工阶段: %s, "
                        + "排程窗口: %s～%s, 窗口原始计划量: %d, 硫化余量: %d, 欠产余量标识: %s, "
                        + "原因: %s",
                context.getFactoryCode(), context.getBatchNo(), sku.getMaterialCode(), sku.getProductStatus(),
                sku.getConstructionStage(), windowStartDate, windowEndDate, sku.getOriginalWindowPlanQty(),
                surplusQty, sku.isOverdueSurplusFlag(), NO_OVERDUE_SURPLUS_UNSCHEDULED_REASON);
        log.info("窗口无计划且无欠产余量SKU未排准入拦截, factoryCode: {}, batchNo: {}, materialCode: {}, "
                        + "productStatus: {}, constructionStage: {}, windowStartDate: {}, windowEndDate: {}, "
                        + "originalWindowPlanQty: {}, surplusQty: {}, reason: {}",
                context.getFactoryCode(), context.getBatchNo(), sku.getMaterialCode(), sku.getProductStatus(),
                sku.getConstructionStage(), windowStartDate, windowEndDate, sku.getOriginalWindowPlanQty(),
                surplusQty, NO_OVERDUE_SURPLUS_UNSCHEDULED_REASON);
        PriorityTraceLogHelper.appendProcessLog(context, "窗口无计划且无欠产余量不排产", detail);
        LhUnscheduledResult unscheduledResult = buildUnscheduledResult(
                context, sku, 0, NO_OVERDUE_SURPLUS_UNSCHEDULED_REASON,
                UnscheduledReasonEnum.NO_OVERDUE_SURPLUS_NO_PLAN, detail);
        // 月计划版本和生产版本属于本次准入未排结果的对账字段，与日计划准入拦截口径保持一致。
        unscheduledResult.setMonthPlanVersion(sku.getMonthPlanVersion());
        unscheduledResult.setProductionVersion(sku.getProductionVersion());
        return unscheduledResult;
    }

    /**
     * 判断窗口外正规新增计划是否处于本次允许的提前生产范围。
     *
     * @param context 排程上下文
     * @param sku 待评估SKU
     * @return 存在允许范围内的未来原始计划时返回true，仅保留候选，不授予资源
     */
    private static boolean hasEligibleFutureOriginalPlan(LhScheduleContext context, SkuScheduleDTO sku) {
        if (!EarlyProductionChecker.isEligibleNewProductionSku(sku)
                || Objects.isNull(context.getWindowEndDate())) {
            return false;
        }
        return Objects.nonNull(EarlyProductionChecker.resolveFirstFuturePlanDate(
                context, sku, toLocalDate(context.getWindowEndDate())));
    }

    /**
     * 评估续作试制、量试SKU是否因排程窗口内无日计划量且无硫化余量而直接进入未排。
     * <p>本规则仅按施工阶段识别试制和量试，判断范围严格限制为排程窗口首日~窗口末日，
     * 不叠加提前生产天数阈值。已识别为续作且欠产余量标识为true（余量由上月欠产构成）的SKU
     * 优先保留续作身份，由续作收尾目标量和实际消费账本控制实际排产量；窗口内无计划但余量
     * 来自窗口外未来计划等非欠产来源（欠产余量标识为false）时不放行，直接进入未排；
     * 前日T+1交替计划仍不得放行无余量的全窗口为零SKU。</p>
     * <p>本方法只负责判断和构造未排结果，不修改续作列表、机台或日计划账本。</p>
     *
     * <p>后半段无计划的单胎胚或非共用胎胚正库存收尾保留续作资格，试制量试独立规则继续生效。</p>
     *
     * @param context 排程上下文，提供排程窗口和跨月月计划数据
     * @param sku 已识别为续作的SKU
     * @return 命中规则时返回未排结果；非试制量试、窗口内有计划或仍有硫化余量时返回null
     */
    public static LhUnscheduledResult evaluateContinuousTrialDailyPlanAdmission(
            LhScheduleContext context, SkuScheduleDTO sku) {
        if (Objects.isNull(context) || Objects.isNull(sku)
                || !isTrialOrMassTrialSku(sku)
                || Objects.isNull(context.getWindowEndDate())
                || EmbryoEndingStockAdmissionPolicy.isEligible(context, sku)) {
            return null;
        }
        LocalDate windowStartDate = toLocalDate(context.getScheduleDate());
        LocalDate windowEndDate = toLocalDate(context.getWindowEndDate());
        // 试制、量试续作仅按排程窗口（首日~末日）判断，不向后叠加提前生产天数阈值。
        if (Objects.isNull(windowEndDate)
                || hasPositiveDailyPlanInWindow(sku)) {
            return null;
        }
        int surplusQty = Math.max(0, sku.getSurplusQty());
        if (surplusQty > 0 && sku.isOverdueSurplusFlag()) {
            String retainDetail = String.format("工厂: %s, 批次: %s, 物料: %s, 产品状态: %s, 施工阶段: %s, "
                            + "续作机台: %s, 排程窗口: %s～%s, 窗口原始计划量: %d, 硫化余量: %d, "
                            + "欠产余量标识: %s, 决策: 保留续作并按欠产余量严格排产",
                    context.getFactoryCode(), context.getBatchNo(), sku.getMaterialCode(), sku.getProductStatus(),
                    sku.getConstructionStage(), sku.getContinuousMachineCode(), windowStartDate, windowEndDate,
                    sku.getOriginalWindowPlanQty(), surplusQty, sku.isOverdueSurplusFlag());
            log.info("续作试制量试SKU欠产余量优先放行, {}", retainDetail);
            PriorityTraceLogHelper.appendProcessLog(context, "续作试制量试欠产余量优先放行", retainDetail);
            return null;
        }

        // 无硫化余量时，试制、量试不适用提前生产天数阈值，判断截止日即窗口末日。
        int earlyProductionDaysThreshold = 0;
        LocalDate admissionEndDate = windowEndDate;
        String detail = String.format("工厂: %s, 批次: %s, 物料: %s, 产品状态: %s, 施工阶段: %s, "
                        + "续作机台: %s, 排程窗口: %s～%s, 窗口原始计划量: %d, 提前生产天数: %d, "
                        + "准入截止日: %s, 原因: %s",
                context.getFactoryCode(), context.getBatchNo(), sku.getMaterialCode(), sku.getProductStatus(),
                sku.getConstructionStage(), sku.getContinuousMachineCode(), windowStartDate, windowEndDate,
                sku.getOriginalWindowPlanQty(), earlyProductionDaysThreshold, admissionEndDate,
                CONTINUOUS_TRIAL_DAILY_PLAN_ADMISSION_UNSCHEDULED_REASON);
        log.info("续作试制量试SKU日计划准入拦截, factoryCode: {}, batchNo: {}, materialCode: {}, "
                        + "productStatus: {}, constructionStage: {}, continuousMachineCode: {}, windowStartDate: {}, "
                        + "windowEndDate: {}, originalWindowPlanQty: {}, earlyProductionDaysThreshold: {}, "
                        + "admissionEndDate: {}, reason: {}",
                context.getFactoryCode(), context.getBatchNo(), sku.getMaterialCode(), sku.getProductStatus(),
                sku.getConstructionStage(), sku.getContinuousMachineCode(), windowStartDate, windowEndDate,
                sku.getOriginalWindowPlanQty(), earlyProductionDaysThreshold, admissionEndDate,
                CONTINUOUS_TRIAL_DAILY_PLAN_ADMISSION_UNSCHEDULED_REASON);
        PriorityTraceLogHelper.appendProcessLog(context, "续作试制量试无计划量不排产", detail);
        LhUnscheduledResult unscheduledResult = buildUnscheduledResult(
                context, sku, 0, CONTINUOUS_TRIAL_DAILY_PLAN_ADMISSION_UNSCHEDULED_REASON,
                UnscheduledReasonEnum.TRIAL_MASS_TRIAL_BLOCKED, detail);
        unscheduledResult.setMonthPlanVersion(sku.getMonthPlanVersion());
        unscheduledResult.setProductionVersion(sku.getProductionVersion());
        return unscheduledResult;
    }

    /**
     * 评估硫化参数SYS0311004=0时，新增排产链路的试制、量试SKU是否直接进入未排。
     * <p>该参数仅控制新增排产：参数=0时施工阶段为试制（01）、量试（02）的SKU不得通过
     * 换活字块和普通新增选机排产；有效前日指定交替由物料资格服务单独校验组合并豁免本项；
     * 续作排产不受该参数影响，续作试制量试及同物料多状态续作切换照常参与。</p>
     * <p>续作加机台补偿SKU由S4.4从已排续作SKU复制生成，属于续作衍生的产能承接，
     * 不视为新增排产试制量试，本方法对其放行。</p>
     * <p>本方法只负责判断和构造未排结果，不修改SKU目标量、排产集合、胎胚库存或日计划账本。</p>
     *
     * @param context 排程上下文，提供硫化参数配置
     * @param sku 待评估的新增待排SKU
     * @return 命中拦截时返回未排结果；参数=1、非试制量试或续作补偿SKU返回null
     */
    public static LhUnscheduledResult evaluateNewSpecTrialExclusion(LhScheduleContext context,
                                                                    SkuScheduleDTO sku) {
        if (Objects.isNull(context) || Objects.isNull(sku)
                || isTrialMassTrialSchedulingEnabled(context)
                || sku.isContinuousCompensationSku()
                || !isTrialOrMassTrialSku(sku)) {
            return null;
        }
        String detail = String.format("工厂: %s, 批次: %s, 物料: %s, 产品状态: %s, 施工阶段: %s, "
                        + "排程类型: %s, 窗口原始计划量: %d, 原因: %s",
                context.getFactoryCode(), context.getBatchNo(), sku.getMaterialCode(), sku.getProductStatus(),
                sku.getConstructionStage(), sku.getScheduleType(), sku.getOriginalWindowPlanQty(),
                NEW_SPEC_TRIAL_EXCLUSION_UNSCHEDULED_REASON);
        log.info("新增排产试制量试SKU参数拦截, factoryCode: {}, batchNo: {}, materialCode: {}, "
                        + "productStatus: {}, constructionStage: {}, scheduleType: {}, "
                        + "originalWindowPlanQty: {}, reason: {}",
                context.getFactoryCode(), context.getBatchNo(), sku.getMaterialCode(), sku.getProductStatus(),
                sku.getConstructionStage(), sku.getScheduleType(), sku.getOriginalWindowPlanQty(),
                NEW_SPEC_TRIAL_EXCLUSION_UNSCHEDULED_REASON);
        PriorityTraceLogHelper.appendProcessLog(context, "新增排产试制量试不排产", detail);
        LhUnscheduledResult unscheduledResult = buildUnscheduledResult(
                context, sku, 0, NEW_SPEC_TRIAL_EXCLUSION_UNSCHEDULED_REASON,
                UnscheduledReasonEnum.TRIAL_MASS_TRIAL_BLOCKED, detail);
        unscheduledResult.setMonthPlanVersion(sku.getMonthPlanVersion());
        unscheduledResult.setProductionVersion(sku.getProductionVersion());
        return unscheduledResult;
    }

    /**
     * 判断试制量试参与排产开关是否开启（SYS0311004=1）。
     * <p>参数配置或上下文缺失时按默认值0（关闭）处理，与参数解析默认口径保持一致。</p>
     *
     * @param context 排程上下文
     * @return true-开关开启，新增排产允许试制量试按日计划准入参与；false-开关关闭，新增排产拦截试制量试
     */
    public static boolean isTrialMassTrialSchedulingEnabled(LhScheduleContext context) {
        return Objects.nonNull(context)
                && Objects.nonNull(context.getScheduleConfig())
                && context.getScheduleConfig().isTrialMassTrialSchedulingEnabled();
    }

    /**
     * 判断完整准入范围内是否存在正日计划量。
     * <p>窗口内使用SKU初始化时按月计划DAY_N汇总的原始计划量；窗口结束后
     * 继续复用提前生产判断器，按物料、产品状态和实际年月读取后续DAY_N。</p>
     *
     * @param context 排程上下文
     * @param sku 待判断SKU
     * @param windowEndDate 排程窗口结束日
     * @return true-范围内至少一天有正日计划量；false-全范围无正日计划量
     */
    private static boolean hasPositiveDailyPlanInAdmissionRange(LhScheduleContext context,
                                                                 SkuScheduleDTO sku,
                                                                 LocalDate windowEndDate) {
        if (sku.getWindowPlanQty() > 0) {
            return true;
        }
        // 以窗口最后一天为基准后看N天，复用提前生产参数、跨月取数和产品状态口径。
        return Objects.nonNull(EarlyProductionChecker.resolveFirstFuturePlanDate(
                context, sku, windowEndDate));
    }

    /**
     * 判断排程窗口内（排程首日~窗口末日）是否存在正日计划量。
     * <p>仅用于试制、量试SKU的准入判断：不向后叠加提前生产天数阈值，严格以排程窗口为判断边界。
     * 读取S4.3归集时固化的原始DAY_N汇总（originalWindowPlanQty），不读取可能被收尾目标量
     * 同步抬高的windowPlanQty，确保只用月计划原始日计划量放行。</p>
     *
     * @param sku 待判断SKU
     * @return true-窗口内至少一天有正日计划量；false-窗口内全为0或无SKU
     */
    private static boolean hasPositiveDailyPlanInWindow(SkuScheduleDTO sku) {
        return Objects.nonNull(sku) && sku.getOriginalWindowPlanQty() > 0;
    }

    /**
     * 判断SKU施工阶段是否为试制或量试。
     *
     * @param sku SKU排程信息
     * @return true-试制或量试；false-其他施工阶段
     */
    public static boolean isTrialOrMassTrialSku(SkuScheduleDTO sku) {
        return Objects.nonNull(sku)
                && (StringUtils.equals(ConstructionStageEnum.TRIAL.getCode(), sku.getConstructionStage())
                || StringUtils.equals(ConstructionStageEnum.MASS_TRIAL.getCode(), sku.getConstructionStage()));
    }

    /**
     * 将日期转换为排程业务日。
     *
     * @param date 日期
     * @return 本地业务日；日期为空时返回null
     */
    private static LocalDate toLocalDate(java.util.Date date) {
        if (Objects.isNull(date)) {
            return null;
        }
        return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
    }

    /**
     * 按优先级评估新增待排SKU是否需要直接进入未排。
     *
     * @param context 排程上下文
     * @param sku 新增待排SKU
     * @param smallEndingRuleEnding 是否按收尾小余量规则视为收尾
     * @param embryoStockEndingTargetApplied 是否已命中成型胎胚库存收尾目标
     * @return 命中规则时返回未排结果；未命中返回null
     */
    public static LhUnscheduledResult evaluate(LhScheduleContext context,
                                               SkuScheduleDTO sku,
                                               boolean smallEndingRuleEnding,
                                               boolean embryoStockEndingTargetApplied) {
        int genericSurplusQty = Objects.isNull(sku)
                ? 0 : Math.max(0, sku.getSurplusQty());
        return evaluate(context, sku, smallEndingRuleEnding,
                embryoStockEndingTargetApplied, genericSurplusQty);
    }

    /**
     * 按优先级评估新增待排SKU是否需要直接进入未排，并允许调用方传入当前运行场景的规则余量。
     *
     * <p>该重载仅替换“收尾小余量”规则的数量输入，不改变仅历史欠产规则使用通用硫化余量的
     * 既有口径。提前生产中心运行视图必须传入实际消费账本剩余量；其他调用继续使用原重载，
     * 从而保持续作、换活字块和普通新增排产行为不变。</p>
     *
     * @param context 排程上下文
     * @param sku 新增待排SKU
     * @param smallEndingRuleEnding 是否按收尾小余量规则视为收尾
     * @param embryoStockEndingTargetApplied 是否已命中成型胎胚库存收尾目标
     * @param smallEndingRuleQty 收尾小余量规则本轮应使用的有效余量
     * @return 命中规则时返回未排结果；未命中返回null
     */
    public static LhUnscheduledResult evaluate(LhScheduleContext context,
                                               SkuScheduleDTO sku,
                                               boolean smallEndingRuleEnding,
                                               boolean embryoStockEndingTargetApplied,
                                               int smallEndingRuleQty) {
        return evaluate(context, sku, smallEndingRuleEnding, embryoStockEndingTargetApplied,
                smallEndingRuleQty, Objects.isNull(sku) ? 0 : sku.getMonthlyHistoryShortageQty());
    }

    /**
     * 统一按小余量优先、历史欠产其次评估真实未排，支持出队入口显式提供历史证据。
     * <p>只替换历史来源证据，不改写SKU或传导缓存；小余量的收尾、胎胚硬目标、比例和前次累计计划覆盖历史余量条件完整保留。</p>
     *
     * @param context 排程上下文
     * @param sku 待评估SKU
     * @param smallEndingRuleEnding 是否满足本轮收尾判断
     * @param embryoStockEndingTargetApplied 是否已经应用胎胚库存硬目标
     * @param smallEndingRuleQty 小余量规则使用的有效余量
     * @param monthlyHistoryShortageQty 调用方已确认的本月历史欠产来源
     * @return 首个真实命中的未排结果；交班停产、小余量和历史欠产均未命中时返回null
     */
    public static LhUnscheduledResult evaluate(LhScheduleContext context,
                                               SkuScheduleDTO sku,
                                               boolean smallEndingRuleEnding,
                                               boolean embryoStockEndingTargetApplied,
                                               int smallEndingRuleQty,
                                               int monthlyHistoryShortageQty) {
        if (Objects.isNull(context) || Objects.isNull(sku)) {
            return null;
        }
        LhUnscheduledResult endingWithoutStock = evaluateEndingWithoutStock(context, sku);
        if (Objects.nonNull(endingWithoutStock)) {
            return endingWithoutStock;
        }
        int genericSurplusQty = Math.max(0, sku.getSurplusQty());
        int resolvedSmallEndingRuleQty = Math.max(0, smallEndingRuleQty);
        boolean runtimeRemainingQtyApplied =
                EarlyProductionQuantityCalculator.shouldUseRuntimeRemainingQtyForSmallEnding(
                        context, sku);
        String quantitySource = runtimeRemainingQtyApplied
                ? "EARLY_PRODUCTION_RUNTIME_REMAINING" : "GENERIC_SURPLUS";
        int toleranceQty = SmallEndingSurplusSkipRule.resolveToleranceQty(context);
        if (!embryoStockEndingTargetApplied && smallEndingRuleEnding) {
            log.info("待排SKU收尾小余量规则数量确认, materialCode: {}, genericSurplusQty: {}, "
                            + "smallEndingRuleQty: {}, quantitySource: {}, toleranceQty: {}",
                    sku.getMaterialCode(), genericSurplusQty, resolvedSmallEndingRuleQty,
                    quantitySource, toleranceQty);
        }
        if (!embryoStockEndingTargetApplied
                && smallEndingRuleEnding
                && resolvedSmallEndingRuleQty <= toleranceQty) {
            long previousPlannedQty = SmallEndingSurplusSkipRule.resolvePreviousPlannedQty(
                    context, sku);
            boolean smallEndingSkipped = SmallEndingSurplusSkipRule.shouldSkip(
                    context, sku, smallEndingRuleEnding, resolvedSmallEndingRuleQty);
            log.info("待排SKU收尾小余量规则判断, materialCode: {}, genericSurplusQty: {}, "
                            + "smallEndingRuleQty: {}, quantitySource: {}, toleranceQty: {}, "
                            + "previousPlannedQty: {}, previousSurplusQty: {}, skipped: {}",
                    sku.getMaterialCode(), genericSurplusQty, resolvedSmallEndingRuleQty,
                    quantitySource, toleranceQty, previousPlannedQty,
                    SmallEndingSurplusSkipRule.resolvePreviousSurplusQty(context, sku), smallEndingSkipped);
            if (smallEndingSkipped) {
                String detail = new StringBuilder(256)
                        .append("materialCode=").append(sku.getMaterialCode())
                        .append(", productStatus=").append(sku.getProductStatus())
                        .append(", actualQty=").append(resolvedSmallEndingRuleQty)
                        .append(", toleranceQty=").append(toleranceQty)
                        .append(", previousPlannedQty=").append(previousPlannedQty)
                        .append(", previousSurplusQty=")
                        .append(SmallEndingSurplusSkipRule.resolvePreviousSurplusQty(context, sku))
                        .toString();
                return buildUnscheduledResult(context, sku, resolvedSmallEndingRuleQty,
                        SmallEndingSurplusSkipRule.UNSCHEDULED_REASON,
                        UnscheduledReasonEnum.SMALL_ENDING_SURPLUS, detail);
            }
        }
        return evaluateHistoryShortage(context, sku, monthlyHistoryShortageQty);
    }

    /**
     * 判断仅历史欠产的净余量是否应停止二次上机，并构造真实正量未排。
     * <p>供既有待排规则和无日计划出队入口共用，不执行收尾小余量判断，也不修改排产账本。</p>
     *
     * @param context 排程上下文，提供逐日完成量和原始月计划
     * @param sku 待判断的SKU，使用当前净硫化余量
     * @return 命中历史欠产规则时返回真实未排；未命中时返回null
     */
    public static LhUnscheduledResult evaluateHistoryShortage(LhScheduleContext context,
                                                              SkuScheduleDTO sku) {
        return evaluateHistoryShortage(context, sku,
                Objects.isNull(sku) ? 0 : sku.getMonthlyHistoryShortageQty());
    }

    /**
     * 使用调用方确认的历史欠产证据判断舍弃，保持传导缓存和SKU账本不变。
     *
     * @param context 排程上下文
     * @param sku 待判断SKU，舍弃数量仍取净硫化余量
     * @param monthlyHistoryShortageQty 原始月计划与逐日完成量核对的本月历史欠产
     * @return 命中时返回真实未排，证据或规则不满足时返回null
     */
    public static LhUnscheduledResult evaluateHistoryShortage(LhScheduleContext context,
                                                              SkuScheduleDTO sku,
                                                              int monthlyHistoryShortageQty) {
        if (Objects.isNull(context) || Objects.isNull(sku)
                || (monthlyHistoryShortageQty <= 0 && sku.getEffectiveLastMonthOverdueQty() <= 0)) {
            return null;
        }
        Integer latestPreviousFinishedQty = resolveLatestPreviousFinishedQty(
                context, sku.getMaterialCode(), sku.getProductStatus());
        if (!shouldSkipHistoryShortageDemand(context, sku, latestPreviousFinishedQty)) {
            return null;
        }
        int historyShortageRemainingQty = Math.max(0, sku.getSurplusQty());
        log.info("待排SKU仅历史欠产规则命中, factoryCode: {}, batchNo: {}, materialCode: {}, monthlyHistoryShortageQty: {}, "
                        + "effectiveLastMonthOverdueQty: {}, historyShortageRemainingQty: {}, scheduleDate: {}, "
                        + "latestPreviousFinishedQty: {}, reason: {}",
                context.getFactoryCode(), context.getBatchNo(), sku.getMaterialCode(),
                monthlyHistoryShortageQty, sku.getEffectiveLastMonthOverdueQty(),
                historyShortageRemainingQty, context.getScheduleDate(), latestPreviousFinishedQty,
                HISTORY_SHORTAGE_UNSCHEDULED_REASON);
        String detail = new StringBuilder(256)
                .append("materialCode=").append(sku.getMaterialCode())
                .append(", productStatus=").append(sku.getProductStatus())
                .append(", monthlyHistoryShortageQty=").append(monthlyHistoryShortageQty)
                .append(", effectiveLastMonthOverdueQty=").append(sku.getEffectiveLastMonthOverdueQty())
                .append(", remainingQty=").append(historyShortageRemainingQty)
                .append(", latestPreviousFinishedQty=").append(latestPreviousFinishedQty)
                .toString();
        return buildUnscheduledResult(context, sku, historyShortageRemainingQty,
                HISTORY_SHORTAGE_UNSCHEDULED_REASON,
                UnscheduledReasonEnum.HISTORY_SHORTAGE_SKIPPED, detail);
    }

    /**
     * 判断SKU是否命中仅历史欠产跳过规则。
     *
     * @param context 排程上下文
     * @param sku 新增待排SKU
     * @param latestPreviousFinishedQty 当前月最近一次非空日完成量
     * @return true-命中仅历史欠产规则；false-不命中
     */
    public static boolean shouldSkipHistoryShortage(LhScheduleContext context,
                                                    SkuScheduleDTO sku,
                                                    Integer latestPreviousFinishedQty) {
        return resolveHistoryShortageRemainingQty(sku) > 0
                && shouldSkipHistoryShortageDemand(context, sku, latestPreviousFinishedQty);
    }

    /**
     * 核对已确认存在历史欠产来源的SKU是否满足停止二次上机条件。
     *
     * @param context 排程上下文
     * @param sku 已确认历史欠产来源的SKU
     * @param latestPreviousFinishedQty 最近一次非空完成量
     * @return 净余量、无后续计划和最近完成证据均满足时返回true
     */
    private static boolean shouldSkipHistoryShortageDemand(LhScheduleContext context,
                                                           SkuScheduleDTO sku,
                                                           Integer latestPreviousFinishedQty) {
        if (Objects.isNull(context) || Objects.isNull(sku) || sku.isContinuousCompensationSku()) {
            return false;
        }
        // 本月历史欠产和有效上月欠产都属于历史欠产来源，统一按当前净硫化余量判断是否仍需补排。
        if (sku.getSurplusQty() <= 0 || !isWindowDayPlanEmpty(sku)) {
            return false;
        }
        if (resolveCurrentMonthPlanQtyFromScheduleDate(context, sku) > 0) {
            return false;
        }
        // 最近一次完成量为0是有效数据，但不能触发跳过，也不得继续向更早日期回溯。
        return Objects.nonNull(latestPreviousFinishedQty) && latestPreviousFinishedQty > 0;
    }

    /**
     * 解析仅历史欠产形成的当前净余量。
     * <p>本月历史欠产或有效上月欠产任一为正时，说明当前余量存在历史欠产来源；未排数量必须使用
     * 已扣除本月累计完成量后的净硫化余量，不能直接使用原始上月欠产量，避免未排数量虚增。</p>
     *
     * @param sku 新增待排SKU
     * @return 仅历史欠产净余量；不存在正向历史欠产来源时返回0
     */
    private static int resolveHistoryShortageRemainingQty(SkuScheduleDTO sku) {
        if (Objects.isNull(sku)) {
            return 0;
        }
        boolean hasHistoryShortageSource = Math.max(0, sku.getMonthlyHistoryShortageQty()) > 0
                || Math.max(0, sku.getEffectiveLastMonthOverdueQty()) > 0;
        return hasHistoryShortageSource ? Math.max(0, sku.getSurplusQty()) : 0;
    }

    /**
     * 解析当前月月初至排程日T-1最近一次非空日完成量。
     *
     * @param context 排程上下文
     * @param materialCode 物料编码
     * @param productStatus 产品状态
     * @return 最近一次非空日完成量；当前月无有效记录时返回null
     */
    public static Integer resolveLatestPreviousFinishedQty(LhScheduleContext context,
                                                           String materialCode,
                                                           String productStatus) {
        if (Objects.isNull(context) || Objects.isNull(context.getScheduleDate())
                || StringUtils.isEmpty(materialCode)
                || CollectionUtils.isEmpty(context.getMaterialMonthDailyFinishedQtyMap())) {
            return null;
        }
        LocalDate scheduleDate = context.getScheduleDate().toInstant()
                .atZone(ZoneId.systemDefault()).toLocalDate();
        LocalDate monthStart = scheduleDate.withDayOfMonth(1);
        for (LocalDate finishDate = scheduleDate.minusDays(1);
             !finishDate.isBefore(monthStart); finishDate = finishDate.minusDays(1)) {
            Integer finishedQty = resolveDailyFinishedQty(context, materialCode, productStatus, finishDate);
            if (Objects.nonNull(finishedQty)) {
                return Math.max(finishedQty, 0);
            }
        }
        return null;
    }

    /**
     * 判断当前排程窗口dayN月计划量是否全为0。
     *
     * @param sku 新增待排SKU
     * @return true-窗口内dayN均为0；false-仍有计划或账本缺失
     */
    private static boolean isWindowDayPlanEmpty(SkuScheduleDTO sku) {
        if (Objects.isNull(sku) || CollectionUtils.isEmpty(sku.getDailyPlanQuotaMap())) {
            return false;
        }
        for (SkuDailyPlanQuotaDTO quota : sku.getDailyPlanQuotaMap().values()) {
            if (Objects.nonNull(quota) && Math.max(0, quota.getDayPlanQty()) > 0) {
                return false;
            }
        }
        return true;
    }

    /**
     * 统计当前排程月从排程日T到月底的日计划量。
     *
     * @param context 排程上下文
     * @param sku 新增待排SKU
     * @return 当前排程月T至月底日计划总量
     */
    private static int resolveCurrentMonthPlanQtyFromScheduleDate(LhScheduleContext context, SkuScheduleDTO sku) {
        if (Objects.isNull(context) || Objects.isNull(sku) || Objects.isNull(context.getScheduleDate())
                || StringUtils.isEmpty(sku.getMaterialCode())) {
            return 0;
        }
        LocalDate scheduleDate = context.getScheduleDate().toInstant()
                .atZone(ZoneId.systemDefault()).toLocalDate();
        LocalDate monthEndDate = scheduleDate.withDayOfMonth(scheduleDate.lengthOfMonth());
        int totalQty = 0;
        for (LocalDate cursor = scheduleDate; !cursor.isAfter(monthEndDate); cursor = cursor.plusDays(1)) {
            totalQty += Math.max(0, MonthPlanDateResolver.resolveDayQty(
                    context, sku.getMaterialCode(), sku.getProductStatus(), cursor));
        }
        return totalQty;
    }

    /**
     * 按物料、产品状态和完成日期读取逐日完成量。
     *
     * @param context 排程上下文
     * @param materialCode 物料编码
     * @param productStatus 产品状态
     * @param finishDate 完成日期
     * @return 日完成量；无记录返回null
     */
    private static Integer resolveDailyFinishedQty(LhScheduleContext context,
                                                   String materialCode,
                                                   String productStatus,
                                                   LocalDate finishDate) {
        String materialStatusKey = MonthPlanDateResolver.buildMaterialStatusKey(materialCode, productStatus);
        Integer finishedQty = context.getMaterialMonthDailyFinishedQtyMap()
                .get(materialStatusKey + "_" + finishDate);
        if (Objects.nonNull(finishedQty) || StringUtils.equals(materialStatusKey, materialCode)) {
            return finishedQty;
        }
        return context.getMaterialMonthDailyFinishedQtyMap().get(materialCode + "_" + finishDate);
    }

    /**
     * 构建规则命中的未排结果。
     *
     * @param context 排程上下文
     * @param sku 新增待排SKU
     * @param unscheduledQty 未排数量
     * @param reason 未排原因
     * @return 未排结果
     */
    private static LhUnscheduledResult buildUnscheduledResult(
            LhScheduleContext context,
            SkuScheduleDTO sku,
            int unscheduledQty,
            String reason,
            UnscheduledReasonEnum reasonCode,
            String reasonDetail) {
        LhUnscheduledResult unscheduled = new LhUnscheduledResult();
        unscheduled.setFactoryCode(context.getFactoryCode());
        unscheduled.setBatchNo(context.getBatchNo());
        unscheduled.setMaterialCode(sku.getMaterialCode());
        unscheduled.setProductStatus(sku.getProductStatus());
        unscheduled.setMaterialDesc(sku.getMaterialDesc());
        unscheduled.setScheduleDate(context.getScheduleTargetDate());
        unscheduled.setUnscheduledReason(reason);
        unscheduled.setUnscheduledReasonCode(reasonCode.getCode());
        unscheduled.setUnscheduledReasonStage(reasonCode.getStage());
        unscheduled.setUnscheduledReasonDetail(reasonDetail);
        unscheduled.setUnscheduledQty(Math.max(0, unscheduledQty));
        unscheduled.setStructureName(sku.getStructureName());
        unscheduled.setMainMaterialDesc(sku.getMainMaterialDesc());
        unscheduled.setSpecCode(sku.getSpecCode());
        unscheduled.setEmbryoCode(sku.getEmbryoCode());
        unscheduled.setMouldQty(sku.getMouldQty());
        unscheduled.setDataSource(AUTO_DATA_SOURCE);
        unscheduled.setIsDelete(0);
        return unscheduled;
    }
}
