package com.zlt.aps.lh.engine.strategy.support;

import com.zlt.aps.lh.api.constant.LhScheduleConstant;
import com.zlt.aps.lh.api.domain.dto.SkuScheduleDTO;
import com.zlt.aps.lh.api.domain.entity.LhMouldChangePlan;
import com.zlt.aps.lh.api.domain.entity.LhRepairCapsule;
import com.zlt.aps.lh.api.domain.entity.LhScheduleResult;
import com.zlt.aps.lh.api.enums.MouldChangeTypeEnum;
import com.zlt.aps.lh.component.MonthPlanDateResolver;
import com.zlt.aps.lh.context.LhScheduleContext;
import com.zlt.aps.lh.util.LhMouldCodeUtil;
import com.zlt.aps.lh.util.LhScheduleTimeUtil;
import com.zlt.aps.lh.util.LhSingleControlMachineUtil;
import com.zlt.aps.mdm.api.domain.entity.LhMachineInfo;
import com.zlt.aps.mdm.api.domain.entity.MdmDevicePlanShut;
import com.zlt.aps.mdm.api.domain.entity.MdmSkuMouldRel;
import com.zlt.aps.mp.api.domain.entity.FactoryMonthPlanProductionFinalResult;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.CollectionUtils;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.function.ToIntFunction;
import java.util.stream.Stream;

/**
 * 续作降模具体机台选择服务。只读取候选和业务快照，不修改计划量、班次、账本或资源占用。
 * <p>是否降模和减少台数由调用方计算；选机结果由原续作流程继续区分保机、豁免生产和真正释放。</p>
 */
public final class ContinuationMachineReducer {

    /**
     * 在既定减少台数内先选历史交替命中机台，再按原正常规则补足。
     * @param context 已初始化的排程上下文
     * @param sourceSku 当前续作SKU
     * @param candidates 已完成业务准入和保护过滤的候选，不跨SKU扩展候选
     * @param reduceCount 本轮需要减少的物理机台数
     * @param excludedMachineCodes 本轮已经选中的物理机台编码
     * @return 有序选择结果及原因；不执行实际释放
     */
    public ReductionResult select(LhScheduleContext context, SkuScheduleDTO sourceSku,
                                  List<LhScheduleResult> candidates, int reduceCount,
                                  Set<String> excludedMachineCodes) {
        if (reduceCount <= 0 || CollectionUtils.isEmpty(candidates)) {
            return new ReductionResult(Collections.emptyList());
        }
        // 只在传入的同SKU候选内归组，不能把另一SKU的单控侧带入本轮。
        Map<String, List<LhScheduleResult>> groups = candidates.stream()
                .filter(Objects::nonNull)
                .filter(result -> StringUtils.isNotEmpty(result.getLhMachineCode()))
                .filter(result -> !excludedMachineCodes.contains(
                        LhSingleControlMachineUtil.resolvePhysicalMachineCode(result.getLhMachineCode())))
                .collect(Collectors.groupingBy(result -> LhSingleControlMachineUtil.resolvePhysicalMachineCode(
                        result.getLhMachineCode()), LinkedHashMap::new, Collectors.toList()));
        // 决策内部的侧别顺序沿用原下机序，保证后续从尾部选择保机首侧及胎胚计算输入不漂移。
        Comparator<LhScheduleResult> sideOrder = this.buildRemoveComparator(context, sourceSku);
        groups.values().forEach(sides -> sides.sort(sideOrder));
        Map<String, Integer> sharedCounts = this.buildFuturePlanMouldSharedSkuCountMap(context, sourceSku);
        boolean dimensionEnabled = this.shouldEnableContinuationReduceDimensionPriority(context, sourceSku);
        Comparator<LhScheduleResult> keepOrder = this.buildKeepComparator(
                context, sourceSku, result -> this.resolveCapsuleUsageCount(context, result));
        // 原主流程先选保留再取差集；反向使用同一保留序，连尺寸缺失时的选择行为也保持一致。
        List<LhScheduleResult> representatives = groups.values().stream()
                .map(sides -> sides.stream().min(keepOrder).get()).collect(Collectors.toList());
        Map<String, LhMouldChangePlan> history = this.resolvePreviousAlternatePlans(context);
        List<LhScheduleResult> historicalCandidates = representatives.stream()
                .filter(candidate -> history.containsKey(LhSingleControlMachineUtil.resolvePhysicalMachineCode(candidate.getLhMachineCode())))
                .sorted(keepOrder.reversed()).collect(Collectors.toList());
        List<MachineReductionDecision> decisions = new ArrayList<>(Math.min(reduceCount, groups.size()));
        Set<String> selected = new LinkedHashSet<>(groups.size());
        for (LhScheduleResult candidate : historicalCandidates) {
            String machineCode = LhSingleControlMachineUtil.resolvePhysicalMachineCode(candidate.getLhMachineCode());
            LhMouldChangePlan plan = history.get(machineCode);
            String detail = String.format("前日批次=%s, 计划日期=%s, 计划ID=%s",
                    plan.getLhResultBatchNo(), plan.getPlanDate(), plan.getId());
            decisions.add(new MachineReductionDecision(machineCode, ReductionReason.PREVIOUS_ALTERNATE_PLAN,
                    detail, decisions.size() + 1, groups.get(machineCode)));
            selected.add(machineCode);
            if (decisions.size() == reduceCount) {
                return new ReductionResult(decisions);
            }
        }
        // 第一阶段已选机台不再参与正常排序和补足。
        List<LhScheduleResult> remaining = representatives.stream().filter(candidate -> !selected.contains(
                LhSingleControlMachineUtil.resolvePhysicalMachineCode(candidate.getLhMachineCode())))
                .sorted(keepOrder.reversed()).collect(Collectors.toList());
        for (int index = 0; index < remaining.size() && decisions.size() < reduceCount; index++) {
            LhScheduleResult candidate = remaining.get(index);
            LhScheduleResult next = index + 1 < remaining.size() ? remaining.get(index + 1) : null;
            String machineCode = LhSingleControlMachineUtil.resolvePhysicalMachineCode(candidate.getLhMachineCode());
            ReductionReason reason = this.resolveReason(context, candidate, next, sharedCounts, dimensionEnabled);
            String detail = String.format("正常下机排序, 前缀保护=%s, 模具共用数=%s, 尺寸优先启用=%s, "
                            + "尺寸=%s, 有效清洗=%s, 胶囊最大次数=%s, 对比下序机台=%s",
                    this.isPriorityContinuationMachine(context, candidate),
                    this.resolveMachineMouldSharedSkuCount(context, candidate, sharedCounts), dimensionEnabled,
                    this.resolveMachineDimensionSize(context, candidate), this.hasValidCleaningPlanForMachine(context, candidate),
                    this.resolveCapsuleUsageCount(context, candidate), Objects.nonNull(next) ? next.getLhMachineCode() : "无剩余候选");
            decisions.add(new MachineReductionDecision(machineCode, reason, detail,
                    decisions.size() + 1, groups.get(machineCode)));
        }
        return new ReductionResult(decisions);
    }

    /**
     * 先锁定已加载历史的最近有效批次，再筛选窗口T/T+1，不能从更旧批次补捞。
     * @param context 已按业务目标日前一天加载的历史快照
     * @return 物理机台对应命中的一条交替计划，仅用于选机解释
     */
    public Map<String, LhMouldChangePlan> resolvePreviousAlternatePlans(LhScheduleContext context) {
        if (Objects.isNull(context) || Objects.isNull(context.getScheduleDate())
                || CollectionUtils.isEmpty(context.getHistoricalReverseMouldChangePlanList())) {
            return Collections.emptyMap();
        }
        List<LhMouldChangePlan> source = context.getHistoricalReverseMouldChangePlanList();
        LhMouldChangePlan latest = source.stream().filter(Objects::nonNull)
                .filter(plan -> StringUtils.isNotEmpty(plan.getLhResultBatchNo()))
                .max(Comparator.comparing(LhMouldChangePlan::getCreateTime, Comparator.nullsFirst(Date::compareTo))
                        .thenComparing(LhMouldChangePlan::getId, Comparator.nullsFirst(Long::compareTo))).orElse(null);
        if (Objects.isNull(latest)) {
            return Collections.emptyMap();
        }
        LocalDate firstDate = context.getScheduleDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        Map<String, LhMouldChangePlan> plans = new LinkedHashMap<>(source.size());
        source.stream().filter(Objects::nonNull)
                .filter(plan -> StringUtils.equals(latest.getLhResultBatchNo(), plan.getLhResultBatchNo()))
                .filter(plan -> StringUtils.isNotEmpty(plan.getLhMachineCode()) && Objects.nonNull(plan.getPlanDate()))
                .filter(plan -> MouldChangeTypeEnum.containsAnyCode(plan.getChangeMouldType(),
                        MouldChangeTypeEnum.REGULAR.getCode(), MouldChangeTypeEnum.TYPE_BLOCK.getCode()))
                .filter(plan -> {
                    LocalDate planDate = plan.getPlanDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
                    return firstDate.equals(planDate) || firstDate.plusDays(1).equals(planDate);
                })
                .sorted(Comparator.comparing(LhMouldChangePlan::getPlanDate)
                        .thenComparing(LhMouldChangePlan::getId, Comparator.nullsLast(Long::compareTo)))
                .forEach(plan -> plans.putIfAbsent(
                        LhSingleControlMachineUtil.resolvePhysicalMachineCode(plan.getLhMachineCode()), plan));
        return plans;
    }

    /**
     * 构建不含历史交替偏好的正常保留排序，供选机和既有排量顺序共同使用。
     * @param context 排程上下文
     * @param sourceSku 当前SKU
     * @return 正常保留比较器
     */
    public Comparator<LhScheduleResult> buildKeepComparator(LhScheduleContext context, SkuScheduleDTO sourceSku) {
        return this.buildKeepComparator(context, sourceSku, result -> this.resolveCapsuleRecordUsageCount(context, result));
    }

    /**
     * 复用正常排序键；仅具体整机选择使用物理机台胶囊最大值，既有排量顺序保持原记录口径。
     * @param context 排程上下文
     * @param sourceSku 当前SKU
     * @param capsuleUsage 胶囊次数只读取值方法
     * @return 正常保留顺序
     */
    private Comparator<LhScheduleResult> buildKeepComparator(LhScheduleContext context, SkuScheduleDTO sourceSku,
                                                             ToIntFunction<LhScheduleResult> capsuleUsage) {
        Map<String, Integer> sharedCounts = this.buildFuturePlanMouldSharedSkuCountMap(context, sourceSku);
        boolean dimensionEnabled = this.shouldEnableContinuationReduceDimensionPriority(context, sourceSku);
        return Comparator.comparingInt((LhScheduleResult result) -> this.isPriorityContinuationMachine(context, result) ? 0 : 1)
                .thenComparingInt(result -> this.resolveMachineMouldSharedSkuCount(context, result, sharedCounts))
                .thenComparing(result -> dimensionEnabled ? this.resolveMachineDimensionSize(context, result) : BigDecimal.ZERO,
                        Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparingInt(result -> this.hasValidCleaningPlanForMachine(context, result) ? 1 : 0)
                .thenComparingInt(result -> -capsuleUsage.applyAsInt(result))
                .thenComparing(result -> StringUtils.defaultString(result.getLhMachineCode()));
    }

    /**
     * 原下机顺序仅用于已确定集合的排量/载体顺序，不再承担历史交替筛选。
     * @param context 排程上下文
     * @param sourceSku 当前SKU
     * @return 正常下机比较器
     */
    public Comparator<LhScheduleResult> buildRemoveComparator(LhScheduleContext context, SkuScheduleDTO sourceSku) {
        Map<String, Integer> sharedCounts = this.buildFuturePlanMouldSharedSkuCountMap(context, sourceSku);
        boolean dimensionEnabled = this.shouldEnableContinuationReduceDimensionPriority(context, sourceSku);
        return Comparator.comparingInt((LhScheduleResult result) -> this.isPriorityContinuationMachine(context, result) ? 1 : 0)
                .thenComparingInt(result -> -this.resolveMachineMouldSharedSkuCount(context, result, sharedCounts))
                .thenComparing(result -> dimensionEnabled ? this.resolveMachineDimensionSize(context, result) : BigDecimal.ZERO,
                        Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparingInt(result -> this.hasValidCleaningPlanForMachine(context, result) ? 0 : 1)
                .thenComparingInt(result -> this.resolveCapsuleRecordUsageCount(context, result))
                .thenComparing(Comparator.comparing((LhScheduleResult result) ->
                        StringUtils.defaultString(result.getLhMachineCode())).reversed());
    }

    /**
     * 解释与下序候选相比第一个不同的排序因素；最后一台记录为确定性编码收口。
     * @param context 排程上下文
     * @param candidate 当前选择
     * @param next 下序候选
     * @param sharedCounts 模具共用性
     * @param dimensionEnabled 是否启用尺寸优先
     * @return 排序原因
     */
    private ReductionReason resolveReason(LhScheduleContext context, LhScheduleResult candidate,
                                           LhScheduleResult next, Map<String, Integer> sharedCounts,
                                           boolean dimensionEnabled) {
        if (Objects.isNull(next)) {
            return ReductionReason.MACHINE_CODE_FALLBACK;
        }
        if (this.isPriorityContinuationMachine(context, candidate) != this.isPriorityContinuationMachine(context, next)) {
            return ReductionReason.CONTINUATION_PREFIX;
        }
        if (this.resolveMachineMouldSharedSkuCount(context, candidate, sharedCounts)
                != this.resolveMachineMouldSharedSkuCount(context, next, sharedCounts)) {
            return ReductionReason.MOULD_COMMONALITY;
        }
        if (dimensionEnabled && Comparator.<BigDecimal>nullsLast(BigDecimal::compareTo).compare(
                this.resolveMachineDimensionSize(context, candidate), this.resolveMachineDimensionSize(context, next)) != 0) {
            return ReductionReason.FUTURE_NO_PLAN_LARGE_MACHINE;
        }
        if (this.hasValidCleaningPlanForMachine(context, candidate) != this.hasValidCleaningPlanForMachine(context, next)) {
            return ReductionReason.CLEANING_PLAN;
        }
        if (this.resolveCapsuleUsageCount(context, candidate) != this.resolveCapsuleUsageCount(context, next)) {
            return ReductionReason.CAPSULE_USAGE;
        }
        return ReductionReason.MACHINE_CODE_FALLBACK;
    }

    /**
     * 判断是否启用续作降模大尺寸机台优先下机。
     * <p>仅检查排程窗口结束日后的 5 个自然日，即标准三天窗口下的 T+3～T+7；
     * 使用专用月计划索引，任意一天存在正计划量都保持原下机排序。</p>
     *
     * @param context 排程上下文
     * @param sourceSku 当前续作SKU
     * @return true-未来5天无正计划，启用尺寸排序；false-保持原排序
     */
    public boolean shouldEnableContinuationReduceDimensionPriority(
            LhScheduleContext context,
            SkuScheduleDTO sourceSku) {
        if (Objects.isNull(context) || Objects.isNull(context.getScheduleDate()) || Objects.isNull(sourceSku)
                || StringUtils.isEmpty(sourceSku.getMaterialCode())) {
            return false;
        }
        LocalDate futurePlanStartDate = this.resolveContinuationReduceDimensionFuturePlanStartDate(context);
        LocalDate futurePlanEndDate = futurePlanStartDate.plusDays(
                LhScheduleConstant.CONTINUATION_REDUCE_DIMENSION_FUTURE_PLAN_DAYS - 1L);
        int futurePlanQty = this.resolveContinuationReduceDimensionFuturePlanQty(
                context, sourceSku, futurePlanStartDate, futurePlanEndDate);
        return futurePlanQty <= 0;
    }

    /**
     * 解析续作降模尺寸排序未来计划开始日。
     *
     * @param context 排程上下文
     * @return 排程窗口结束日的下一天，即T+3
     */
    public LocalDate resolveContinuationReduceDimensionFuturePlanStartDate(LhScheduleContext context) {
        LocalDate windowEndDate = Objects.nonNull(context.getWindowEndDate())
                ? context.getWindowEndDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate()
                : context.getScheduleDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate()
                .plusDays(LhScheduleConstant.SCHEDULE_DAYS - 1L);
        return windowEndDate.plusDays(1);
    }

    /**
     * 汇总续作降模尺寸排序专用未来日计划量。
     *
     * @param context 排程上下文
     * @param sourceSku 当前续作SKU
     * @param futurePlanStartDate 未来计划开始日T+3
     * @param futurePlanEndDate 未来计划结束日T+7
     * @return T+3～T+7原始日计划量合计
     */
    public int resolveContinuationReduceDimensionFuturePlanQty(
            LhScheduleContext context,
            SkuScheduleDTO sourceSku,
            LocalDate futurePlanStartDate,
            LocalDate futurePlanEndDate) {
        return MonthPlanDateResolver.resolveWindowPlanQty(
                context.getContinuationReduceDimensionMonthPlanByMaterialMonthMap(),
                sourceSku.getMaterialCode(), sourceSku.getProductStatus(),
                futurePlanStartDate, futurePlanEndDate);
    }

    /**
     * 解析续作结果对应硫化机台的数值尺寸。
     * <p>优先按结果机台编码读取，单控L/R未命中时再按物理机台编码读取；当前有效机台主数据
     * DIMENSION_SIZE 使用纯数字，统一转为BigDecimal后参与排序，避免字符串字典序错误。</p>
     *
     * @param context 排程上下文
     * @param result 续作结果
     * @return 数值尺寸；机台或尺寸数据缺失时返回null并排在尺寸比较末尾
     */
    public BigDecimal resolveMachineDimensionSize(LhScheduleContext context, LhScheduleResult result) {
        if (Objects.isNull(context) || Objects.isNull(result) || StringUtils.isEmpty(result.getLhMachineCode())
                || CollectionUtils.isEmpty(context.getMachineInfoMap())) {
            return null;
        }
        String machineCode = result.getLhMachineCode();
        LhMachineInfo machineInfo = context.getMachineInfoMap().get(machineCode);
        if (Objects.isNull(machineInfo)) {
            machineInfo = context.getMachineInfoMap().get(
                    LhSingleControlMachineUtil.resolvePhysicalMachineCode(machineCode));
        }
        if (Objects.isNull(machineInfo) || StringUtils.isEmpty(machineInfo.getDimensionSize())) {
            return null;
        }
        return new BigDecimal(StringUtils.trim(machineInfo.getDimensionSize()));
    }

    /**
     * 判断续作机台是否命中优先保留前缀。
     *
     * @param context 排程上下文
     * @param result 续作排程结果
     * @return true-命中参数配置前缀；false-按原降模优先级排序
     */
    public boolean isPriorityContinuationMachine(LhScheduleContext context, LhScheduleResult result) {
        return Objects.nonNull(context)
                && Objects.nonNull(context.getScheduleConfig())
                && Objects.nonNull(result)
                && context.getScheduleConfig().isPriorityContinuationMachine(result.getLhMachineCode());
    }

    /**
     * 构建续作降模使用的未来计划模具共用性映射。
     *
     * <p>逐个检查模具关系中的其他关联 SKU，只要该 SKU 从排程日期 T 日至当月月底的原始定稿
     * 日计划合计大于 0，才允许纳入模具共用性。当前续作 SKU 本身无论是否有未来计划均不计数，
     * 因为本排序要衡量的是释放模具后可承接其他 SKU 的复用价值。该方法只读取上下文已经加载的
     * 月计划，不重复查询数据库，也不读取已扣减日额度或实际排产量。T 日为月末时开始、结束日期
     * 相同，因此只检查 T 日当天。</p>
     *
     * @param context 排程上下文
     * @param sourceSku 当前续作SKU
     * @return 模具号到“未来有计划的其他关联 SKU 数量”的映射
     */
    public Map<String, Integer> buildFuturePlanMouldSharedSkuCountMap(LhScheduleContext context,
                                                                       SkuScheduleDTO sourceSku) {
        if (Objects.isNull(context) || Objects.isNull(context.getScheduleDate()) || Objects.isNull(sourceSku)
                || StringUtils.isEmpty(sourceSku.getMaterialCode())
                || CollectionUtils.isEmpty(context.getSkuMouldRelMap())) {
            return Collections.emptyMap();
        }
        LocalDate scheduleDate = context.getScheduleDate().toInstant()
                .atZone(ZoneId.systemDefault()).toLocalDate();
        LocalDate monthEndDate = scheduleDate.withDayOfMonth(scheduleDate.lengthOfMonth());
        Set<String> futurePlanMaterialCodeSet = this.resolveFuturePlanRelatedMaterialCodeSet(
                context, sourceSku, scheduleDate, monthEndDate);
        return LhMouldCodeUtil.buildMouldSharedSkuCountMap(context, futurePlanMaterialCodeSet);
    }

    /**
     * 解析从 T 日至月底仍有正计划量的其他关联 SKU。
     *
     * @param context 排程上下文
     * @param sourceSku 当前续作 SKU；该 SKU 本身必须从结果中排除
     * @param scheduleDate 排程日期 T 日
     * @param monthEndDate T 日所在月的最后一天
     * @return 按模具关系上下文顺序去重后的未来计划关联 SKU 集合
     */
    public Set<String> resolveFuturePlanRelatedMaterialCodeSet(LhScheduleContext context,
                                                                SkuScheduleDTO sourceSku,
                                                                LocalDate scheduleDate,
                                                                LocalDate monthEndDate) {
        if (Objects.isNull(context) || Objects.isNull(sourceSku)
                || StringUtils.isEmpty(sourceSku.getMaterialCode())
                || Objects.isNull(scheduleDate) || Objects.isNull(monthEndDate)
                || CollectionUtils.isEmpty(context.getSkuMouldRelMap())) {
            return Collections.emptySet();
        }
        Set<String> sourceSkuMouldCodeSet = this.resolveSourceSkuMouldCodeSet(context, sourceSku.getMaterialCode());
        if (CollectionUtils.isEmpty(sourceSkuMouldCodeSet)) {
            return Collections.emptySet();
        }
        Set<String> futurePlanMaterialCodeSet = new LinkedHashSet<String>(context.getSkuMouldRelMap().size());
        for (Map.Entry<String, List<MdmSkuMouldRel>> entry : context.getSkuMouldRelMap().entrySet()) {
            String relatedMaterialCode = entry.getKey();
            if (StringUtils.isEmpty(relatedMaterialCode)
                    || StringUtils.equals(sourceSku.getMaterialCode(), relatedMaterialCode)
                    || !this.hasSharedMouldCode(entry.getValue(), sourceSkuMouldCodeSet)) {
                continue;
            }
            // 模具关系不带产品状态，因此同一关联物料的任一产品状态存在正计划量即视为未来有计划。
            if (this.hasRelatedSkuFuturePlan(context, relatedMaterialCode, scheduleDate, monthEndDate)) {
                futurePlanMaterialCodeSet.add(relatedMaterialCode);
            }
        }
        return futurePlanMaterialCodeSet;
    }

    /**
     * 解析当前续作 SKU 的全部关联模具号。
     *
     * <p>本集合只用于限定“其他关联 SKU”的搜索范围，不参与在机模具数量计算；机台最终共用性仍由
     * {@link LhMouldCodeUtil#resolveMachineMouldSharedSkuCount(LhScheduleContext, String, Map)}
     * 按各机台实际在机模具计算。</p>
     *
     * @param context 排程上下文
     * @param sourceMaterialCode 当前续作 SKU 编码
     * @return 去空、去重后的当前 SKU 关联模具号集合
     */
    private Set<String> resolveSourceSkuMouldCodeSet(LhScheduleContext context, String sourceMaterialCode) {
        List<MdmSkuMouldRel> sourceSkuMouldRelList = context.getSkuMouldRelMap().get(sourceMaterialCode);
        if (CollectionUtils.isEmpty(sourceSkuMouldRelList)) {
            return Collections.emptySet();
        }
        Set<String> sourceSkuMouldCodeSet = new LinkedHashSet<String>(sourceSkuMouldRelList.size());
        for (MdmSkuMouldRel rel : sourceSkuMouldRelList) {
            String mouldCode = Objects.isNull(rel) ? null : StringUtils.trim(rel.getMouldCode());
            if (StringUtils.isNotEmpty(mouldCode)) {
                sourceSkuMouldCodeSet.add(mouldCode);
            }
        }
        return sourceSkuMouldCodeSet;
    }

    /**
     * 判断候选 SKU 是否与当前续作 SKU 共用至少一个模具。
     *
     * @param relatedSkuMouldRelList 候选关联 SKU 的模具关系
     * @param sourceSkuMouldCodeSet 当前续作 SKU 的模具号集合
     * @return true-至少共用一个模具；false-没有共用模具或关系数据缺失
     */
    private boolean hasSharedMouldCode(List<MdmSkuMouldRel> relatedSkuMouldRelList,
                                       Set<String> sourceSkuMouldCodeSet) {
        if (CollectionUtils.isEmpty(relatedSkuMouldRelList) || CollectionUtils.isEmpty(sourceSkuMouldCodeSet)) {
            return false;
        }
        for (MdmSkuMouldRel rel : relatedSkuMouldRelList) {
            String mouldCode = Objects.isNull(rel) ? null : StringUtils.trim(rel.getMouldCode());
            if (StringUtils.isNotEmpty(mouldCode) && sourceSkuMouldCodeSet.contains(mouldCode)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 判断关联 SKU 的任一产品状态在指定窗口内是否存在正日计划量。
     *
     * <p>模具关系只有物料编码，没有产品状态；月计划却可能按产品状态拆成多条记录。因此先从本次
     * 已加载月计划提取该物料在 T 日所在月份的产品状态，再逐状态复用
     * {@link MonthPlanDateResolver#resolveWindowPlanQty(LhScheduleContext, String, String, LocalDate, LocalDate)}
     * 读取原始 DAY_n。任一状态合计大于 0 即命中，避免只取第一条月计划而漏掉其他状态。</p>
     *
     * @param context 排程上下文
     * @param materialCode 关联 SKU 编码
     * @param startDate 检查开始日期，即排程日期 T 日
     * @param endDate 检查结束日期，即 T 日所在月月底
     * @return true-任一产品状态存在正日计划量；false-全部无计划或数据缺失
     */
    private boolean hasRelatedSkuFuturePlan(LhScheduleContext context,
                                            String materialCode,
                                            LocalDate startDate,
                                            LocalDate endDate) {
        List<FactoryMonthPlanProductionFinalResult> loadedMonthPlanList =
                !CollectionUtils.isEmpty(context.getLoadedMonthPlanList())
                        ? context.getLoadedMonthPlanList() : context.getMonthPlanList();
        if (CollectionUtils.isEmpty(loadedMonthPlanList)) {
            return false;
        }
        Set<String> checkedProductStatusSet = new HashSet<String>(4);
        for (FactoryMonthPlanProductionFinalResult plan : loadedMonthPlanList) {
            if (Objects.isNull(plan) || !StringUtils.equals(materialCode, plan.getMaterialCode())) {
                continue;
            }
            // 年月为空是项目既有测试/兼容数据口径；年月有值时必须与 T 日所在月份一致。
            if ((Objects.nonNull(plan.getYear()) && !Objects.equals(plan.getYear(), startDate.getYear()))
                    || (Objects.nonNull(plan.getMonth())
                    && !Objects.equals(plan.getMonth(), startDate.getMonthValue()))) {
                continue;
            }
            String productStatus = StringUtils.trimToEmpty(plan.getProductStatus());
            if (!checkedProductStatusSet.add(productStatus)) {
                continue;
            }
            int futurePlanQty = MonthPlanDateResolver.resolveWindowPlanQty(
                    context, materialCode, productStatus, startDate, endDate);
            if (futurePlanQty > 0) {
                return true;
            }
        }
        return false;
    }

    /**
     * 判断续作候选机台是否命中本次已加载的有效清洗计划。
     * <p>只读取初始化阶段保存的原始清洗候选快照，不重复查询数据库，也不依赖候选最终是否因每日上限、
     * 班次或三天内收尾规则生成实际清洗窗口。单控机台复用物理机台编码匹配，保证 K1501 与
     * K1501L/K1501R 按既有左右侧联动口径一致命中。</p>
     *
     * @param context 排程上下文
     * @param result 续作机台结果
     * @return true-存在计划开始时间不早于T日的清洗候选；false-不存在
     */
    public boolean hasValidCleaningPlanForMachine(LhScheduleContext context, LhScheduleResult result) {
        if (Objects.isNull(context) || Objects.isNull(context.getScheduleDate()) || Objects.isNull(result)
                || StringUtils.isEmpty(result.getLhMachineCode())
                || CollectionUtils.isEmpty(context.getLoadedCleaningPlanShutList())) {
            return false;
        }
        Date scheduleStartTime = LhScheduleTimeUtil.clearTime(context.getScheduleDate());
        String resultPhysicalMachineCode = LhSingleControlMachineUtil.resolvePhysicalMachineCode(
                result.getLhMachineCode());
        if (StringUtils.isEmpty(resultPhysicalMachineCode)) {
            return false;
        }
        for (MdmDevicePlanShut cleaningPlan : context.getLoadedCleaningPlanShutList()) {
            if (Objects.isNull(cleaningPlan) || Objects.isNull(cleaningPlan.getBeginDate())
                    || cleaningPlan.getBeginDate().before(scheduleStartTime)) {
                continue;
            }
            String planPhysicalMachineCode = LhSingleControlMachineUtil.resolvePhysicalMachineCode(
                    cleaningPlan.getMachineCode());
            if (StringUtils.equals(resultPhysicalMachineCode, planPhysicalMachineCode)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 解析结果机台在机模具共用性数量。
     *
     * @param context 排程上下文
     * @param result 续作结果
     * @param mouldSharedSkuCountMap 模具号到关联 SKU 数量的映射
     * @return 在机模具共用性数量
     */
    public int resolveMachineMouldSharedSkuCount(LhScheduleContext context,
                                                  LhScheduleResult result,
                                                  Map<String, Integer> mouldSharedSkuCountMap) {
        if (Objects.isNull(result) || StringUtils.isEmpty(result.getLhMachineCode())) {
            return 0;
        }
        return LhMouldCodeUtil.resolveMachineMouldSharedSkuCount(
                context, result.getLhMachineCode(), mouldSharedSkuCountMap);
    }

    /**
     * 解析结果机台胶囊最大使用次数。
     *
     * @param context 排程上下文
     * @param result 续作结果
     * @return 胶囊最大使用次数
     */
    public int resolveCapsuleUsageCount(LhScheduleContext context, LhScheduleResult result) {
        if (Objects.isNull(context) || Objects.isNull(result) || StringUtils.isEmpty(result.getLhMachineCode())) {
            return 0;
        }
        String physicalCode = LhSingleControlMachineUtil.resolvePhysicalMachineCode(result.getLhMachineCode());
        return Stream.of(physicalCode, LhSingleControlMachineUtil.resolveLeftMachineCode(physicalCode),
                        LhSingleControlMachineUtil.resolveRightMachineCode(physicalCode))
                .map(context.getCapsuleUsageMap()::get).filter(Objects::nonNull)
                .mapToInt(this::resolveCapsuleRecordMaximum).max().orElse(0);
    }
    /**
     * 原排量及专用载体保留按精确机台记录读取胶囊次数，不扩大本次整机选机影响范围。
     * @param context 排程上下文
     * @param result 续作结果
     * @return 该记录左右模最大次数，缺失仍沿用原0口径
     */
    public int resolveCapsuleRecordUsageCount(LhScheduleContext context, LhScheduleResult result) {
        if (Objects.isNull(context) || Objects.isNull(result) || StringUtils.isEmpty(result.getLhMachineCode())) {
            return 0;
        }
        LhRepairCapsule capsule = context.getCapsuleUsageMap().get(result.getLhMachineCode());
        return Objects.isNull(capsule) ? 0 : this.resolveCapsuleRecordMaximum(capsule);
    }

    /** @param capsule 已加载胶囊记录 @return 原左右模最大使用次数 */
    private int resolveCapsuleRecordMaximum(LhRepairCapsule capsule) {
        int left = Objects.isNull(capsule.getReplaceCapsuleCount()) ? 0 : Math.max(0, capsule.getReplaceCapsuleCount());
        int right = Objects.isNull(capsule.getReplaceCapsuleCount2()) ? 0 : Math.max(0, capsule.getReplaceCapsuleCount2());
        return Math.max(left, right);
    }
}
