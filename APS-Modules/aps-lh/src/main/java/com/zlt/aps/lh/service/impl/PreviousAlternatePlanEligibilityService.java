/**
 * Copyright (c) 2008, 智立通（厦门）科技有限公司 All rights reserved。
 */
package com.zlt.aps.lh.service.impl;

import com.zlt.aps.lh.api.domain.dto.SkuScheduleDTO;
import com.zlt.aps.lh.api.enums.SkuScheduleSourceTypeEnum;
import com.zlt.aps.lh.engine.strategy.support.NewSpecCandidatePoolBuilder;
import com.zlt.aps.lh.api.domain.entity.LhMouldChangePlan;
import com.zlt.aps.lh.api.enums.ShiftEnum;
import com.zlt.aps.lh.api.enums.MouldChangeTypeEnum;
import com.zlt.aps.lh.component.EarlyProductionQuantityCalculator;
import com.zlt.aps.lh.context.LhScheduleContext;
import com.zlt.aps.lh.engine.strategy.support.DailyMachineExpansionPlanner;
import com.zlt.aps.lh.engine.strategy.support.DailyNewSpecCandidate;
import com.zlt.aps.lh.engine.strategy.support.PreviousAlternatePlanReleaseEvent;
import com.zlt.aps.lh.service.ILhDailyMouldCalcService;
import com.zlt.aps.lh.util.LhSingleControlMachineUtil;
import com.zlt.aps.lh.util.LhScheduleTimeUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 前日交替计划资格公共服务。
 *
 * <p>统一最近有效批次、精确机台加前物料关系、后物料排产资格和目标机台缺口口径，
 * 供续作降模排序与后续独立复用阶段共同使用。历史关系只提供释放优先级，
 * 不预占机台、模具、月计划、日计划或生产余量。</p>
 */
@Slf4j
@Service
public class PreviousAlternatePlanEligibilityService {

    /**
     * 有效历史动作统一排序：计划日期升序 -> 班次升序 -> 换模类型降序 -> 机台编码升序 -> 左右模升序。
     *
     * <p>班次按编码字符串升序（01夜班 -> 02早班 -> 03中班），与实际生产时间顺序一致；
     * 换模类型降序使更换活字块(02)先于正规换模(01)被冻结和复用；
     * 机台编码加左右模升序保证同日期同班次同类型时多机台按机台+模具侧稳定展开；
     * 最后以记录主键兜底，保持排序稳定。</p>
     */
    private static final Comparator<LhMouldChangePlan> PLAN_ORDER = Comparator
            .comparing(LhMouldChangePlan::getPlanDate, Comparator.nullsLast(Date::compareTo))
            .thenComparing(LhMouldChangePlan::getClassIndex, Comparator.nullsLast(String::compareTo))
            .thenComparing(PreviousAlternatePlanEligibilityService::resolveChangeMouldTypeSortKey,
                    Comparator.nullsLast(Comparator.reverseOrder()))
            .thenComparing(LhMouldChangePlan::getLhMachineCode, Comparator.nullsLast(String::compareTo))
            .thenComparing(LhMouldChangePlan::getLeftRightMould, Comparator.nullsLast(String::compareTo))
            .thenComparing(LhMouldChangePlan::getId, Comparator.nullsLast(Long::compareTo));

    @Resource
    private NewSpecMaterialEligibilityService materialEligibilityService;
    /** 调整需求复用新增内核既有机台机会规则，不改写月计划目标 Map。 */
    @Resource
    private NewSpecCandidatePoolBuilder newSpecCandidatePoolBuilder;
    @Resource
    private ILhDailyMouldCalcService lhDailyMouldCalcService;

    /**
     * 先锁定前日最近有效批次，再冻结尚未过期的交替关系。
     *
     * @param context 排程上下文
     * @return 机台加前物料对应的有序计划
     */
    public Map<String, List<LhMouldChangePlan>> buildPlanIndex(LhScheduleContext context) {
        if (Objects.isNull(context)
                || CollectionUtils.isEmpty(context.getHistoricalReverseMouldChangePlanList())) {
            return Collections.emptyMap();
        }
        List<LhMouldChangePlan> source = context.getHistoricalReverseMouldChangePlanList();
        LhMouldChangePlan latest = source.stream()
                .filter(Objects::nonNull)
                .filter(plan -> StringUtils.isNotEmpty(plan.getLhResultBatchNo()))
                .max(Comparator.comparing(LhMouldChangePlan::getCreateTime,
                                Comparator.nullsFirst(Date::compareTo))
                        .thenComparing(LhMouldChangePlan::getId,
                                Comparator.nullsFirst(Long::compareTo)))
                .orElse(null);
        if (Objects.isNull(latest)) {
            return Collections.emptyMap();
        }
        Map<String, List<LhMouldChangePlan>> index =
                new LinkedHashMap<String, List<LhMouldChangePlan>>(source.size());
        source.stream()
                .filter(Objects::nonNull)
                .filter(plan -> StringUtils.equals(
                        latest.getLhResultBatchNo(), plan.getLhResultBatchNo()))
                .filter(plan -> StringUtils.isNotEmpty(plan.getLhMachineCode())
                        && StringUtils.isNotEmpty(plan.getBeforeMaterialCode())
                        && StringUtils.isNotEmpty(plan.getAfterMaterialCode()))
                // 过期动作不能再参与强制下机、余量收尾纠偏或指定承接，也不补捞旧批次。
                .filter(plan -> this.isUnexpiredPlan(context, plan))
                .sorted(PLAN_ORDER)
                .forEach(plan -> index.computeIfAbsent(
                        this.buildKey(plan.getLhMachineCode(), plan.getBeforeMaterialCode()),
                        ignored -> new ArrayList<LhMouldChangePlan>(2)).add(plan));
        // 资格快照只保存已过滤的关系；早于资源尝试冻结，失败不能重新获得虚拟机资格。
        context.setEligiblePreviousAlternatePlans(index.values().stream()
                .flatMap(List::stream).collect(Collectors.toList()));
        Stream.of(context.getPreviousAlternateNewSpecCandidates(), context.getContinuousSkuList(),
                context.getTrialVirtualMachineCandidateList()).flatMap(List::stream)
                .forEach(context::registerPreviousAlternateTrialSku);
        return index;
    }

    /**
     * 按实际动作日期排除窗口开始前已经截止的历史交替。
     *
     * @param context 本次固定窗口上下文
     * @param plan 最新历史批次的交替计划
     * @return 动作日期有效且未早于窗口起点时返回true
     */
    private boolean isUnexpiredPlan(LhScheduleContext context, LhMouldChangePlan plan) {
        if (Objects.isNull(plan.getPlanDate()) || Objects.isNull(context.getScheduleDate())) {
            return false;
        }
        boolean expired = LhScheduleTimeUtil.clearTime(plan.getPlanDate())
                .before(LhScheduleTimeUtil.clearTime(context.getScheduleDate()));
        if (expired) {
            log.debug("历史交替动作已过期，不参与本次续作及承接, factoryCode: {}, batchNo: {}, "
                            + "planId: {}, machineCode: {}, beforeMaterial: {}, afterMaterial: {}, "
                            + "planDate: {}, windowStart: {}",
                    context.getFactoryCode(), context.getBatchNo(), plan.getId(), plan.getLhMachineCode(),
                    plan.getBeforeMaterialCode(), plan.getAfterMaterialCode(), plan.getPlanDate(),
                    context.getScheduleDate());
        }
        return !expired;
    }

    /**
     * 获取前日交替计划的稳定排序规则（公共静态口径，供复用链路及降模选机等非 Spring 场景统一使用）。
     *
     * @return 计划日期升序 -> 班次升序 -> 换模类型降序 -> 机台编码升序 -> 左右模升序 -> 主键升序的比较器
     */
    public static Comparator<LhMouldChangePlan> planOrder() {
        return PLAN_ORDER;
    }

    /**
     * 解析换模类型降序排序键。
     *
     * <p>历史计划 CHANGE_MOULD_TYPE 可能存英文逗号分隔的多值，排序时取其中最大类型编码，
     * 保证含更换活字块(02)的组合在降序中优先于纯正规换模(01)。</p>
     *
     * @param plan 前日交替计划
     * @return 多值中最大的类型编码；字段为空时返回null，排序时排在最后
     */
    private static String resolveChangeMouldTypeSortKey(LhMouldChangePlan plan) {
        String changeMouldType = plan.getChangeMouldType();
        if (StringUtils.isEmpty(changeMouldType)) {
            return null;
        }
        return Arrays.stream(changeMouldType.split(","))
                .map(String::trim)
                .filter(StringUtils::isNotEmpty)
                .max(String::compareTo)
                .orElse(null);
    }

    /**
     * 读取余量收尾纠偏所需的唯一历史关系，不改变降模和正式复用的公共排序。
     * @param context 已加载目标日前一日历史计划的上下文
     * @param materialCode 当前续作前物料
     * @param machineCodes 当前收尾组运行态机台编码
     * @return 最新有效批次内可唯一对应且日期、早中班有效的计划；歧义机台不参与纠偏
     */
    public Map<String, LhMouldChangePlan> resolveRemainingFinishPlans(LhScheduleContext context,
            String materialCode, List<String> machineCodes) {
        Map<String, List<LhMouldChangePlan>> index = this.buildPlanIndex(context);
        Map<String, LhMouldChangePlan> plans = new LinkedHashMap<>(machineCodes.size());
        for (String machineCode : machineCodes) {
            List<LhMouldChangePlan> matches = index.get(this.buildKey(machineCode, materialCode));
            // 同机前料多条计划不能靠ID猜测本次应承接哪一条，保持原收尾结果。
            if (CollectionUtils.isEmpty(matches) || matches.size() != 1) {
                continue;
            }
            LhMouldChangePlan plan = matches.get(0);
            if (Objects.nonNull(plan.getPlanDate())
                    && (ShiftEnum.MORNING_SHIFT.getCode().equals(plan.getClassIndex())
                    || ShiftEnum.AFTERNOON_SHIFT.getCode().equals(plan.getClassIndex()))
                    && MouldChangeTypeEnum.containsAnyCode(plan.getChangeMouldType(),
                    MouldChangeTypeEnum.REGULAR.getCode(), MouldChangeTypeEnum.TYPE_BLOCK.getCode())) {
                plans.put(machineCode, plan);
            }
        }
        return plans;
    }

    /**
     * 解析当前续作 SKU 中存在有效前日交替后料需求的物理机台。
     *
     * @param context 排程上下文
     * @param sourceSku 当前续作 SKU
     * @param businessDateList 本次排程窗口业务日期
     * @return 应优先释放给前日交替后料的物理机台编码
     */
    public Set<String> resolveEligibleReleasePhysicalMachineCodes(
            LhScheduleContext context,
            SkuScheduleDTO sourceSku,
            List<LocalDate> businessDateList) {
        if (Objects.isNull(context) || Objects.isNull(sourceSku)
                || StringUtils.isEmpty(sourceSku.getMaterialCode())
                || CollectionUtils.isEmpty(businessDateList)
                || CollectionUtils.isEmpty(context.getPreviousAlternateNewSpecCandidates())) {
            return Collections.emptySet();
        }
        Map<String, List<LhMouldChangePlan>> planIndex = this.buildPlanIndex(context);
        if (CollectionUtils.isEmpty(planIndex)) {
            return Collections.emptySet();
        }
        Set<String> machineCodeSet = new LinkedHashSet<String>(planIndex.size());
        planIndex.values().stream()
                .flatMap(List::stream)
                .filter(plan -> StringUtils.equals(
                        sourceSku.getMaterialCode(), plan.getBeforeMaterialCode()))
                .filter(plan -> !StringUtils.equals(
                        sourceSku.getMaterialCode(), plan.getAfterMaterialCode()))
                .filter(plan -> this.hasEligibleAfterMaterialDemand(
                        context, plan, businessDateList))
                .map(LhMouldChangePlan::getLhMachineCode)
                .map(LhSingleControlMachineUtil::resolvePhysicalMachineCode)
                .filter(StringUtils::isNotEmpty)
                .forEach(machineCodeSet::add);
        return machineCodeSet;
    }

    /**
     * 根据统一目标 Map 和已提交物理机台数刷新候选需求份数。
     *
     * @param context 排程上下文
     * @param businessDate 实际业务日
     * @param candidate 当前新增候选
     */
    void fillMachineDemand(LhScheduleContext context,
                           LocalDate businessDate,
                           DailyNewSpecCandidate candidate) {
        SkuScheduleDTO sku = candidate.getSku();
        if (StringUtils.equals(SkuScheduleSourceTypeEnum.PREVIOUS_ALTERNATE_TRIAL.getCode(), sku.getSourceType())) {
            // 独立承接需求没有月计划目标台数，只允许一台指定物理机消费共享的严格余量。
            candidate.setTargetMachineCount(1);
            candidate.setScheduledMachineCount(DailyMachineExpansionPlanner.countCommittedMachineDemand(
                    context, sku, businessDate));
            return;
        }
        if (StringUtils.equals(SkuScheduleSourceTypeEnum.DAY_PLAN_ADJUST.getCode(), sku.getSourceType())) {
            // 调整需求无原始 dayN 时，沿用 S4.5.2 的合法首次尝试机会及已提交机台计数。
            newSpecCandidatePoolBuilder.refreshRemainingMachineCount(context, businessDate, candidate);
            return;
        }
        LocalDate requiredDate = EarlyProductionQuantityCalculator.resolveRequiredMachineCountDate(
                context, sku, candidate.getEarlyProductionPreview(), businessDate);
        candidate.setTargetMachineCount(this.lhDailyMouldCalcService.getRequiredMachineCount(
                context, sku.getMaterialCode(), sku.getProductStatus(), requiredDate));
        int scheduledMachineCount = DailyMachineExpansionPlanner.countCommittedMachineDemand(
                context, sku, businessDate);
        Set<String> forcedReleasedMachineCodeSet =
                context.resolveForcedReleasedPhysicalMachineCodes(sku);
        if (!CollectionUtils.isEmpty(forcedReleasedMachineCodeSet)) {
            int activeMachineCount = context.getSkuScheduledMachineCountExcluding(
                    businessDate, sku.getMaterialCode(), sku.getProductStatus(),
                    forcedReleasedMachineCodeSet);
            candidate.setTargetMachineCount(Math.max(candidate.getTargetMachineCount(),
                    activeMachineCount + Math.max(1, sku.getContinuationShortageMachineCount())));
        }
        candidate.setScheduledMachineCount(scheduledMachineCount);
        candidate.setTargetMachineCount(context.resolvePreviousAlternateMachineCount(
                sku, businessDate, candidate.getTargetMachineCount()));
        PreviousAlternatePlanReleaseEvent event = context.getActivePreviousAlternateEvent();
        if (Objects.nonNull(event) && event.isBeforeMaterialNotScheduled()
                && context.isPreviousAlternateAction(sku, event.getMachineCode())
                && StringUtils.equals(event.getMaterialCode(), sku.getMaterialCode())
                && StringUtils.equals(event.getProductStatus(), sku.getProductStatus())) {
            // 后料本来就在该机续作，历史动作只重建这台原承载，不因T日dayN为0等待次日增机。
            // 仅作用于当前指定动作，不修改统一目标Map，也不给普通新增候选增加份数。
            candidate.setTargetMachineCount(Math.max(candidate.getTargetMachineCount(), scheduledMachineCount + 1));
        }
    }

    /**
     * 判断历史后物料在窗口内是否仍有可消费的有效机台需求。
     *
     * @param context 排程上下文
     * @param plan 历史交替计划，提供后物料及对应产品状态
     * @param businessDateList 本次排程窗口业务日期
     * @return 是否存在有效资格且目标机台仍有缺口
     */
    private boolean hasEligibleAfterMaterialDemand(LhScheduleContext context,
                                                   LhMouldChangePlan plan,
                                                   List<LocalDate> businessDateList) {
        List<SkuScheduleDTO> candidateSkuList = context.getPreviousAlternateNewSpecCandidates().stream()
                .filter(Objects::nonNull)
                .filter(sku -> context.matchesPreviousAlternateAfterMaterial(
                        plan, sku.getMaterialCode(), sku.getProductStatus()))
                .collect(Collectors.toList());
        for (SkuScheduleDTO sku : candidateSkuList) {
            for (LocalDate businessDate : businessDateList) {
                DailyNewSpecCandidate candidate = this.materialEligibilityService.resolvePreviousAlternateCandidate(
                        context, sku, businessDate, plan, plan.getLhMachineCode());
                if (candidate.getReasons().isEmpty()) {
                    continue;
                }
                this.fillMachineDemand(context, businessDate, candidate);
                if (candidate.getTargetMachineCount() > candidate.getScheduledMachineCount()) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * 构建精确运行态机台与前物料键，不合并左右侧历史关系。
     *
     * @param machineCode 机台编码
     * @param beforeMaterial 前物料
     * @return 历史关系索引键
     */
    String buildKey(String machineCode, String beforeMaterial) {
        return new StringBuilder(64)
                .append(machineCode)
                .append('|')
                .append(beforeMaterial)
                .toString();
    }
}
