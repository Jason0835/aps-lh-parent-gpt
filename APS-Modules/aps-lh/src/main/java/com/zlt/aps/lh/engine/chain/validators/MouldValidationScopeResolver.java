package com.zlt.aps.lh.engine.chain.validators;

import com.zlt.aps.lh.api.domain.dto.SkuScheduleDTO;
import com.zlt.aps.lh.api.domain.entity.LhMachineOnlineInfo;
import com.zlt.aps.lh.api.enums.MouldChangeTypeEnum;
import com.zlt.aps.lh.api.enums.ShiftEnum;
import com.zlt.aps.lh.api.enums.TrialStatusEnum;
import com.zlt.aps.lh.component.DayPlanAdjustRequireAssembler;
import com.zlt.aps.lh.component.EarlyProductionQuantityCalculator;
import com.zlt.aps.lh.component.MonthPlanDateResolver;
import com.zlt.aps.lh.component.SkuDecrementChecker;
import com.zlt.aps.lh.component.TargetScheduleQtyResolver;
import com.zlt.aps.lh.context.LhScheduleContext;
import com.zlt.aps.lh.engine.strategy.support.EarlyProductionChecker;
import com.zlt.aps.lh.engine.strategy.support.PendingSkuUnscheduledRule;
import com.zlt.aps.lh.handler.ScheduleAdjustHandler;
import com.zlt.aps.lh.util.LhScheduleTimeUtil;
import com.zlt.aps.lh.util.MachineStatusUtil;
import com.zlt.aps.mp.api.domain.entity.FactoryMonthPlanProductionFinalResult;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 只读解析模具禁用校验所需的本轮需求范围。
 * <p>只筛选需求及日期准入，不预分配机台、模具、胎胚或数量账本；后续实际排产仍执行原资源门禁。</p>
 *
 * @author APS
 * @date 2026/11/02
 */
@Slf4j
@Component
public class MouldValidationScopeResolver {

    /**
     * 复用排程归集的只读硫化余量计算。
     */
    private final ScheduleAdjustHandler scheduleAdjustHandler;

    /**
     * 识别独立日计划调整需求。
     */
    private final DayPlanAdjustRequireAssembler dayPlanAdjustRequireAssembler;

    /**
     * 复用减量清单的年月及产品状态门禁。
     */
    private final SkuDecrementChecker skuDecrementChecker;

    /**
     * 读取胎胚收尾标识，避免零库存物料进入校验范围。
     */
    private final TargetScheduleQtyResolver targetScheduleQtyResolver;

    /**
     * 注入已有只读规则入口，局部解析不另建数量口径。
     *
     * @param scheduleAdjustHandler 硫化余量计算入口
     * @param dayPlanAdjustRequireAssembler 独立日计划调整识别入口
     * @param skuDecrementChecker 减量清单规则
     * @param targetScheduleQtyResolver 收尾库存规则
     */
    public MouldValidationScopeResolver(ScheduleAdjustHandler scheduleAdjustHandler,
                                         DayPlanAdjustRequireAssembler dayPlanAdjustRequireAssembler,
                                         SkuDecrementChecker skuDecrementChecker,
                                         TargetScheduleQtyResolver targetScheduleQtyResolver) {
        this.scheduleAdjustHandler = scheduleAdjustHandler;
        this.dayPlanAdjustRequireAssembler = dayPlanAdjustRequireAssembler;
        this.skuDecrementChecker = skuDecrementChecker;
        this.targetScheduleQtyResolver = targetScheduleQtyResolver;
    }

    /**
     * 收集至少一个产品状态具备本轮需求资格的物料，供禁用模具检查使用。
     *
     * @param context 已完成基础数据加载并固化窗口日期的上下文
     * @return 按月计划顺序去重的需校验物料编码
     */
    public Set<String> resolveMaterialCodes(LhScheduleContext context) {
        LocalDate startDate = context.getScheduleDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        LocalDate endDate = context.getWindowEndDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        Set<String> materialCodes = context.getMonthPlanList().stream()
                .filter(Objects::nonNull)
                .filter(plan -> StringUtils.isNotBlank(plan.getMaterialCode()))
                .filter(plan -> this.hasSchedulingDemand(context, plan, startDate, endDate))
                .map(FactoryMonthPlanProductionFinalResult::getMaterialCode)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Set<String> skippedMaterialCodes = context.getMonthPlanList().stream()
                .filter(Objects::nonNull)
                .map(FactoryMonthPlanProductionFinalResult::getMaterialCode)
                .filter(StringUtils::isNotBlank)
                .filter(materialCode -> !materialCodes.contains(materialCode))
                .collect(Collectors.toCollection(LinkedHashSet::new));
        log.info("禁用模具校验范围, 工厂: {}, 批次: {}, 窗口: {}~{}, 提前生产截止日: {}, 校验物料: {}, 跳过物料: {}",
                context.getFactoryCode(), context.getBatchNo(), startDate, endDate,
                EarlyProductionChecker.resolveEarlyProductionMaxDate(context, endDate),
                materialCodes, skippedMaterialCodes);
        return materialCodes;
    }

    /**
     * 判断单个物料状态是否存在窗口内、提前生产或独立来源的有效需求。
     *
     * @param context 基础数据上下文
     * @param plan 归集基础月计划
     * @param startDate 窗口开始日
     * @param endDate 窗口结束日
     * @return 存在本轮需求资格时返回 true
     */
    private boolean hasSchedulingDemand(LhScheduleContext context, FactoryMonthPlanProductionFinalResult plan,
                                        LocalDate startDate, LocalDate endDate) {
        LocalDate targetDate = context.getScheduleTargetDate().toInstant()
                .atZone(ZoneId.systemDefault()).toLocalDate();
        FactoryMonthPlanProductionFinalResult targetPlan =
                EarlyProductionQuantityCalculator.resolveTargetMonthPlan(context, plan, targetDate);
        if (StringUtils.isBlank(plan.getStructureName())
                || skuDecrementChecker.isDecrementHit(context, targetPlan.getYear(), targetPlan.getMonth(),
                plan.getMaterialCode(), targetPlan.getProductStatus())) {
            return false;
        }
        SkuScheduleDTO sku = new SkuScheduleDTO();
        sku.setMaterialCode(plan.getMaterialCode());
        sku.setProductStatus(plan.getProductStatus());
        sku.setConstructionStage(targetPlan.getConstructionStage());
        boolean trial = PendingSkuUnscheduledRule.isTrialOrMassTrialSku(sku);
        boolean windowPlan = MonthPlanDateResolver.resolveWindowPlanQty(context, plan.getMaterialCode(),
                plan.getProductStatus(), startDate, endDate) > 0;
        boolean continuation = this.hasContinuationSource(context, plan);
        boolean adjustedDemand = !continuation
                && dayPlanAdjustRequireAssembler.isDayPlanAdjustOnlyDemand(context, plan);
        int currentMonthTotal = EarlyProductionQuantityCalculator.resolveCurrentMonthTotalQty(context, sku, startDate);
        boolean historicalTrial = trial && !continuation && currentMonthTotal > 0
                && this.hasHistoricalAfterSource(context, plan);
        // 历史指定试验余量是独立需求，不读取或改写月计划日量，也不能遗漏其模具校验。
        if (historicalTrial && !windowPlan) {
            return scheduleAdjustHandler.calculatePlanSurplusQty(context, plan) > 0;
        }
        // 试制量试只观察实际窗口；新增还受参与开关约束，MES续作沿用原豁免。
        if (trial && ((!windowPlan && !adjustedDemand) || (!continuation
                && !PendingSkuUnscheduledRule.isTrialMassTrialSchedulingEnabled(context)
                && !historicalTrial))) {
            return false;
        }
        if (continuation) {
            return true;
        }
        if (adjustedDemand) {
            return scheduleAdjustHandler.calculatePlanSurplusQty(context, plan) > 0;
        }
        // 当前月零总量只允许进入未来原始计划候选，不能以当前日量或胎胚库存绕过路由。
        if (trial && currentMonthTotal <= 0) {
            return false;
        }
        LocalDate searchAfterDate = currentMonthTotal > 0 ? startDate.minusDays(1) : startDate;
        LocalDate admissionEndDate = trial ? endDate
                : EarlyProductionChecker.resolveEarlyProductionMaxDate(context, endDate);
        if (Objects.isNull(EarlyProductionChecker.resolveFirstFuturePlanDate(
                context, sku, searchAfterDate, admissionEndDate))) {
            return false;
        }
        if (targetScheduleQtyResolver.isEmbryoStockEndingFlagYes(context, plan.getEmbryoCode())) {
            Integer embryoStock = context.getEmbryoRealtimeStockMap().get(plan.getEmbryoCode());
            return Objects.nonNull(embryoStock) && embryoStock > 0;
        }
        return scheduleAdjustHandler.calculatePlanSurplusQty(context, plan) > 0;
    }

    /**
     * 识别窗口内精确的历史指定试制量试来源，供开关豁免及无日计划余量承接使用。
     *
     * @param context 基础输入上下文
     * @param plan 待校验物料状态
     * @return 存在窗口内精确历史后料来源时返回 true
     */
    private boolean hasHistoricalAfterSource(LhScheduleContext context, FactoryMonthPlanProductionFinalResult plan) {
        return context.getHistoricalReverseMouldChangePlanList().stream()
                .filter(Objects::nonNull)
                .filter(history -> Objects.nonNull(history.getPlanDate()))
                .filter(history -> MouldChangeTypeEnum.containsAnyCode(history.getChangeMouldType(),
                        MouldChangeTypeEnum.REGULAR.getCode(), MouldChangeTypeEnum.TYPE_BLOCK.getCode()))
                .filter(history -> ShiftEnum.MORNING_SHIFT.getCode().equals(history.getClassIndex())
                        || ShiftEnum.AFTERNOON_SHIFT.getCode().equals(history.getClassIndex()))
                .filter(history -> !LhScheduleTimeUtil.clearTime(history.getPlanDate())
                        .before(LhScheduleTimeUtil.clearTime(context.getScheduleDate()))
                        && !LhScheduleTimeUtil.clearTime(history.getPlanDate())
                        .after(LhScheduleTimeUtil.clearTime(context.getWindowEndDate())))
                .filter(history -> this.hasEnabledMachine(context, history.getLhMachineCode()))
                .anyMatch(history -> StringUtils.equals(plan.getMaterialCode(), history.getAfterMaterialCode())
                        && StringUtils.equals(plan.getProductStatus(), history.getProductStatus()));
    }

    /**
     * 保留精确MES在机及T日历史前料来源，不以月计划零量误排除合法续作。
     * <p>历史来源仅确认需求身份，完整装机组、释放时间及实际可排量仍由续作主链确认。</p>
     *
     * @param context 已加载MES和历史输入的上下文
     * @param plan 当前物料状态月计划
     * @return 存在续作需求来源时返回 true
     */
    private boolean hasContinuationSource(LhScheduleContext context, FactoryMonthPlanProductionFinalResult plan) {
        boolean online = context.getMachineOnlineInfoMap().entrySet().stream()
                .filter(entry -> this.hasEnabledMachine(context, entry.getKey()))
                .map(java.util.Map.Entry::getValue)
                .filter(Objects::nonNull)
                .anyMatch(source -> this.matchesOnline(plan, source));
        if (online) {
            return true;
        }
        return context.getHistoricalReverseMouldChangePlanList().stream()
                .filter(Objects::nonNull)
                .filter(history -> Objects.nonNull(history.getPlanDate())
                        && LhScheduleTimeUtil.isSameDay(history.getPlanDate(), context.getScheduleDate()))
                .filter(history -> ShiftEnum.MORNING_SHIFT.getCode().equals(history.getClassIndex())
                        || ShiftEnum.AFTERNOON_SHIFT.getCode().equals(history.getClassIndex()))
                .filter(history -> MouldChangeTypeEnum.containsAnyCode(history.getChangeMouldType(),
                        MouldChangeTypeEnum.REGULAR.getCode(), MouldChangeTypeEnum.TYPE_BLOCK.getCode()))
                .filter(history -> StringUtils.equals(plan.getMaterialCode(), history.getBeforeMaterialCode()))
                .filter(history -> !StringUtils.equals(history.getBeforeMaterialCode(), history.getAfterMaterialCode()))
                .filter(history -> this.hasEnabledMachine(context, history.getLhMachineCode()))
                .filter(history -> Objects.nonNull(context.getMachineOnlineInfoMap().get(history.getLhMachineCode())))
                .filter(history -> StringUtils.equals(history.getAfterMaterialCode(),
                        context.getMachineOnlineInfoMap().get(history.getLhMachineCode()).getMaterialCode()))
                .filter(history -> StringUtils.equals(history.getProductStatus(), StringUtils.defaultIfBlank(
                        context.getMachineOnlineInfoMap().get(history.getLhMachineCode()).getProductStatus(),
                        TrialStatusEnum.FORMAL.getCode())))
                .anyMatch(history -> context.getPreviousScheduleResultList().stream()
                        .anyMatch(result -> StringUtils.equals(history.getLhMachineCode(), result.getLhMachineCode())
                                && StringUtils.equals(plan.getMaterialCode(), result.getMaterialCode())
                                && StringUtils.equals(plan.getProductStatus(), result.getProductStatus())));
    }

    /**
     * 确认续作来源对应本工厂已启用的物理机台。
     *
     * @param context 基础数据上下文
     * @param machineCode 物理机台编码
     * @return 机台存在且启用时返回 true
     */
    private boolean hasEnabledMachine(LhScheduleContext context, String machineCode) {
        return Objects.nonNull(context.getMachineInfoMap().get(machineCode))
                && MachineStatusUtil.isEnabled(context.getMachineInfoMap().get(machineCode).getStatus());
    }

    /**
     * 按物料和产品状态精确匹配MES来源，空MES状态沿用正规状态口径。
     *
     * @param plan 当前月计划
     * @param source MES在机记录
     * @return 精确匹配时返回 true
     */
    private boolean matchesOnline(FactoryMonthPlanProductionFinalResult plan, LhMachineOnlineInfo source) {
        return StringUtils.equals(plan.getMaterialCode(), source.getMaterialCode())
                && StringUtils.equals(plan.getProductStatus(),
                StringUtils.defaultIfBlank(source.getProductStatus(), TrialStatusEnum.FORMAL.getCode()));
    }
}
