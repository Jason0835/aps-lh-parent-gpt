package com.zlt.aps.common.engine.schedule.rolling;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 定时滚动后置补量纯规划器，原机原行、最低量先尝试、备库随后且严格不新增产能超用。
 */
public class AutomaticRollingReplenishmentPlanner {
    private static final BigDecimal HUNDRED = new BigDecimal("100");
    private static final String MINIMUM_STAGE = "minimumStart";
    private static final String STOCK_STAGE = "stockUp";
    private static final String ONE_CURL_REASON = "INSUFFICIENT_CAPACITY_FOR_ONE_CURL";

    /**
     * 压缩只读来源明细时保留完整两阶段决策，避免大JSON省略时丢失原因和实际前后值。
     *
     * @param evidence       待序列化的证据副本
     * @param maximumSources 本轮允许保留的来源条数
     * @param omittedCounts  统一省略条数记录
     */
    @SuppressWarnings("unchecked")
    public static void compactSourceEvidence(Map<String, Object> evidence, int maximumSources,
                                             Map<String, Integer> omittedCounts) {
        Object value = evidence.get("postReplenishment");
        if (!(value instanceof Map)) {
            return;
        }
        Map<String, Object> node = new LinkedHashMap<>((Map<String, Object>) value);
        Object sourceValue = node.get("targetSource");
        if (sourceValue instanceof Map) {
            Map<String, Object> source = new LinkedHashMap<>((Map<String, Object>) sourceValue);
            Object rowsValue = source.get("sources");
            if (rowsValue instanceof List) {
                List<?> rows = (List<?>) rowsValue;
                int retained = Math.min(Math.max(maximumSources, 0), rows.size());
                source.put("sources", new ArrayList<>(rows.subList(0, retained)));
                if (retained < rows.size()) {
                    source.put("sourcesOmittedCount", rows.size() - retained);
                    omittedCounts.put("postReplenishment.targetSource.sources", rows.size() - retained);
                }
            }
            node.put("targetSource", source);
        }
        evidence.put("postReplenishment", node);
    }

    /**
     * 在主动下修判定前计算本次合法目标，保护随当前来源重算，不保护历史过量计划。
     *
     * @param input 已完成只读装载的滚动快照
     */
    public void prepare(RollingPlanningInput input) {
        if (!input.isPostReplenishmentEnabled()) {
            return;
        }
        for (RollingMaterialSnapshot material : input.getMaterials().values()) {
            RollingTaskSnapshot row = this.targetRow(input, material);
            RollingMachineSnapshot machine = input.getMachines().get(material.getBoundMachine());
            if (row == null || !AutomaticRollingPlanner.positive(row.getCurlLength())
                    || machine == null || this.blockReason(machine, material, row) != null) {
                continue;
            }
            BigDecimal before = this.quantity(input, material);
            boolean totalMode = this.totalMode(row);
            BigDecimal minimum = this.legalTarget(material, this.minimumTarget(material), before, row.getCurlLength(), totalMode);
            BigDecimal reserve = this.stockTarget(material, before, row.getCurlLength(), totalMode);
            material.setReplenishmentProtectedQty(minimum.max(reserve));
            Map<String, Object> node = this.node(material);
            node.put("protectedPlanQty", material.getReplenishmentProtectedQty());
            node.put("protectionScope", "ACTIVE_DOWN_ONLY_NOT_YIELD");
        }
    }

    /**
     * 正常滚动后按机台执行两阶段，所有变更只写快照，解释随原事务落库。
     *
     * @param input 正常下修、上修和让位完成后的实时账本
     */
    public void replenish(RollingPlanningInput input) {
        if (!input.isPostReplenishmentEnabled()) {
            return;
        }
        Map<String, RollingTaskSnapshot> selectedRows = new LinkedHashMap<>();
        Map<String, BigDecimal> bases = new LinkedHashMap<>();
        Map<String, BigDecimal> minimumTargets = new LinkedHashMap<>();
        Set<String> totalModes = new HashSet<>();
        for (RollingMaterialSnapshot material : input.getMaterials().values()) {
            BigDecimal quantity = this.quantity(input, material);
            Map<String, Object> node = this.node(material);
            node.put("normalRollingAfterQty", quantity);
            // 库存清零轮或产能让位整行清零后，后置补量不得再把最低起排量或备库量补回来。
            boolean stockCleared = material.isStockDownToZero();
            boolean capacityYieldCleared = Boolean.TRUE.equals(
                    material.getEvidence().get("capacityYieldCleared"));
            if (stockCleared || capacityYieldCleared) {
                String clearedReason = stockCleared ? "STOCK_DOWN_TO_ZERO" : "CAPACITY_YIELD_CLEARED";
                material.setStockUpTargetQty(BigDecimal.ZERO);
                material.setStockUpAcceptedQty(BigDecimal.ZERO);
                node.put("stockUpSourceKnown", material.isStockUpDemandComplete());
                node.put("stockUpTargetApplicable", false);
                node.put("stockUpTargetKnown", false);
                this.recordStage(material, MINIMUM_STAGE, quantity, quantity, BigDecimal.ZERO, BigDecimal.ZERO,
                        clearedReason, clearedReason, null, null);
                this.recordStage(material, STOCK_STAGE, quantity, quantity, BigDecimal.ZERO, BigDecimal.ZERO,
                        clearedReason, clearedReason, null, null);
                material.setStockUpSkipReason(clearedReason);
                continue;
            }
            RollingTaskSnapshot row = this.targetRow(input, material);
            if (row != null && AutomaticRollingPlanner.positive(row.getCurlLength())) {
                selectedRows.put(material.getMaterialCode(), row);
                bases.put(material.getMaterialCode(), row.getPlanQty());
                boolean totalMode = this.totalMode(row);
                if (totalMode) {
                    totalModes.add(material.getMaterialCode());
                }
                minimumTargets.put(material.getMaterialCode(), this.legalTarget(material, this.minimumTarget(material),
                        quantity, row.getCurlLength(), totalMode));
                material.setStockUpTargetQty(this.stockTarget(material, quantity, row.getCurlLength(), totalMode));
                node.put("quantityMode", material.isWholeRollExempt() ? "DEMAND_ONLY_INTEGER"
                        : totalMode ? "TOTAL_CEILED_CURL" : "PRESERVE_BASE_INCREMENT");
                node.put("baseQty", row.getPlanQty());
                node.put("curlLength", row.getCurlLength());
            }
            material.setStockUpAcceptedQty(BigDecimal.ZERO);
            node.put("stockUpSourceKnown", material.isStockUpDemandComplete());
            node.put("stockUpTargetApplicable", selectedRows.containsKey(material.getMaterialCode()));
            node.put("stockUpTargetKnown", material.isStockUpDemandComplete()
                    && selectedRows.containsKey(material.getMaterialCode()));
        }
        List<RollingMachineSnapshot> machines = input.getMachines().values().stream()
                .sorted(Comparator.comparing(RollingMachineSnapshot::getMachineCode)).collect(Collectors.toList());
        for (RollingMachineSnapshot machine : machines) {
            for (String stage : new String[]{MINIMUM_STAGE, STOCK_STAGE}) {
                // 每阶段使用当时的库存加当前计划重新排序一次，不重排原物理工单顺序。
                List<RollingMaterialSnapshot> materials = input.getMaterials().values().stream()
                        .filter(material -> Objects.equals(machine.getMachineCode(), material.getBoundMachine()))
                        .sorted(Comparator.comparing((RollingMaterialSnapshot material) ->
                                        AutomaticRollingPlanner.coverage(material.getStockQty().add(this.quantity(input, material)),
                                                material.getFutureDemand()))
                                .thenComparing(RollingMaterialSnapshot::getMaterialCode)).collect(Collectors.toList());
                for (RollingMaterialSnapshot material : materials) {
                    String code = material.getMaterialCode();
                    BigDecimal target = STOCK_STAGE.equals(stage) ? material.getStockUpTargetQty()
                            : minimumTargets.getOrDefault(code, material.getMinimumBatch());
                    this.applyStage(input, machine, material, selectedRows.get(code), bases.get(code),
                            totalModes.contains(code), target, stage);
                }
            }
        }
        for (RollingMaterialSnapshot material : input.getMaterials().values()) {
            Map<String, Object> node = this.node(material);
            BigDecimal after = this.quantity(input, material);
            for (String stage : new String[]{MINIMUM_STAGE, STOCK_STAGE}) {
                if (!node.containsKey(stage)) {
                    this.recordStage(material, stage, after, after, BigDecimal.ZERO, BigDecimal.ZERO,
                            "NO_EXISTING_TARGET_SHIFT_PLAN", "NO_EXISTING_TARGET_SHIFT_PLAN", null, null);
                    if (STOCK_STAGE.equals(stage) && material.getStockUpSkipReason() == null) {
                        material.setStockUpSkipReason("NO_EXISTING_TARGET_SHIFT_PLAN");
                    }
                }
            }
            material.setStockUpGapQty(material.isStockUpDemandComplete()
                    ? material.getStockUpTargetQty().subtract(after).max(BigDecimal.ZERO) : BigDecimal.ZERO);
            node.put("afterPlanQty", after);
            node.put("stockUpTargetQty", material.getStockUpTargetQty());
            node.put("stockUpAcceptedQty", material.getStockUpAcceptedQty());
            node.put("stockUpGapQty", material.getStockUpGapQty());
            node.put("stockUpSkipReason", material.getStockUpSkipReason());
            node.put("acceptedQty", after.subtract((BigDecimal) node.get("normalRollingAfterQty")));
            RollingMachineSnapshot machine = input.getMachines().get(material.getBoundMachine());
            if (machine != null) {
                BigDecimal finalCapacity = new AutomaticRollingPlanner().availableQty(input, machine);
                node.put("finalCapacityBalance", finalCapacity);
                if (material.getEvidence().containsKey("capacityBalance")) {
                    node.put("normalRollingCapacityBalance", material.getEvidence().get("capacityBalance"));
                    material.getEvidence().put("capacityBalance", finalCapacity);
                }
            }
            if ("UP".equals(material.getDirection()) && after.compareTo(material.getTargetQty()) >= 0
                    && "BUSINESS_NO_CAPACITY".equals(material.getReason())) {
                material.setReason(null);
            }
        }
    }

    /**
     * 在固定行结算一阶段；k=0或整数化超资源时不修改行，不向其他机台借产能。
     *
     * @param input     共享米数账本
     * @param machine   原机台
     * @param material  本阶段物料
     * @param row       唯一选中的原有正计划行
     * @param baseQty   两阶段共同使用的追加前基数
     * @param totalMode 是否可按最终总量整卷
     * @param target    合法目标总计划
     * @param stage     阶段名称
     */
    private void applyStage(RollingPlanningInput input, RollingMachineSnapshot machine,
                            RollingMaterialSnapshot material, RollingTaskSnapshot row, BigDecimal baseQty,
                            boolean totalMode, BigDecimal target, String stage) {
        BigDecimal before = this.quantity(input, material);
        BigDecimal capacity = new AutomaticRollingPlanner().availableQty(input, machine);
        String reason = this.blockReason(machine, material, row);
        if (reason == null && STOCK_STAGE.equals(stage) && !material.isStockUpDemandComplete()) {
            reason = material.getStockUpSkipReason() == null ? "STOCK_UP_DEMAND_SOURCE_MISSING" : material.getStockUpSkipReason();
        }
        BigDecimal gap = target.subtract(before).max(BigDecimal.ZERO);
        if (reason == null && gap.signum() == 0) {
            reason = "TARGET_ALREADY_SATISFIED";
        }
        if (reason == null && capacity.signum() <= 0) {
            reason = "NO_REMAINING_CAPACITY";
        }
        BigDecimal accepted = BigDecimal.ZERO;
        BigDecimal rolls = BigDecimal.ZERO;
        BigDecimal raw = row == null ? BigDecimal.ZERO : row.getPlanQty();
        if (reason == null) {
            BigDecimal others = before.subtract(row.getPlanQty());
            BigDecimal effectiveCapacity = material.isWholeRollExempt() && MINIMUM_STAGE.equals(stage)
                    && capacity.signum() > 0
                    ? capacity.max(material.getMinimumBatch().subtract(row.getPlanQty())) : capacity;
            BigDecimal limit = target.subtract(others).min(row.getPlanQty().add(effectiveCapacity))
                    .setScale(0, RoundingMode.FLOOR);
            if (material.isWholeRollExempt()) {
                raw = limit;
                BigDecimal next = limit.signum() > 0 && limit.compareTo(material.getMinimumBatch()) < 0
                        ? row.getPlanQty() : limit;
                accepted = next.subtract(row.getPlanQty()).max(BigDecimal.ZERO);
                if (accepted.signum() > 0) {
                    row.setPlanQty(next);
                    row.setRoundingBeforeQty(raw);
                    row.setRoundingAddedQty(BigDecimal.ZERO);
                    row.setRoundingRule("INTEGER_CEILING");
                } else {
                    reason = "NO_LEGAL_INTEGER_CAPACITY";
                }
            } else {
                rolls = (totalMode ? limit : limit.subtract(baseQty)).max(BigDecimal.ZERO)
                        .divide(row.getCurlLength(), 0, RoundingMode.FLOOR);
                raw = rolls.multiply(row.getCurlLength()).add(totalMode ? BigDecimal.ZERO : baseQty);
                BigDecimal next = raw.setScale(0, RoundingMode.CEILING);
                accepted = next.subtract(row.getPlanQty()).max(BigDecimal.ZERO);
                if (rolls.signum() <= 0 || accepted.signum() <= 0 || next.compareTo(limit) > 0
                        || accepted.compareTo(capacity) > 0) {
                    accepted = BigDecimal.ZERO;
                    reason = ONE_CURL_REASON;
                } else {
                    row.setPlanQty(next);
                    row.setRoundingBeforeQty(raw);
                    row.setRoundingAddedQty(next.subtract(raw));
                    row.setRoundingRule("INTEGER_CEILING");
                }
            }
        }
        BigDecimal after = before.add(accepted);
        BigDecimal remaining = target.subtract(after).max(BigDecimal.ZERO);
        BigDecimal afterCapacity = new AutomaticRollingPlanner().availableQty(input, machine);
        String unfilled = remaining.signum() == 0 ? null : reason != null ? reason
                : afterCapacity.signum() <= 0 ? "NO_REMAINING_CAPACITY" : ONE_CURL_REASON;
        Map<String, Object> detail = this.recordStage(material, stage, before, target, accepted, remaining,
                reason, unfilled, capacity, afterCapacity);
        detail.put("candidateRollCount", rolls);
        detail.put("roundingBeforeQty", raw);
        detail.put("coverage", AutomaticRollingPlanner.coverage(material.getStockQty().add(before), material.getFutureDemand()));
        if (STOCK_STAGE.equals(stage)) {
            material.setStockUpAcceptedQty(accepted);
            material.setStockUpGapQty(remaining);
            material.setStockUpSkipReason("TARGET_ALREADY_SATISFIED".equals(reason) ? null
                    : unfilled != null ? unfilled : reason);
        }
    }

    /**
     * 保存阶段前后值及独立的跳过/未补足原因，不改原保供gap或reason。
     *
     * @param material       物料快照
     * @param stage          阶段名
     * @param before         阶段前总计划
     * @param target         阶段目标总计划
     * @param accepted       实际追加量
     * @param gap            最终缺口
     * @param skipped        跳过原因
     * @param unfilled       未补足原因
     * @param capacityBefore 补量前余额，无机台时为空
     * @param capacityAfter  补量后余额，无机台时为空
     * @return 供追加卷数和排序证据的阶段节点
     */
    private Map<String, Object> recordStage(RollingMaterialSnapshot material, String stage, BigDecimal before,
                                            BigDecimal target, BigDecimal accepted, BigDecimal gap,
                                            String skipped, String unfilled, BigDecimal capacityBefore,
                                            BigDecimal capacityAfter) {
        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("beforePlanQty", before);
        detail.put("targetPlanQty", target);
        detail.put("afterPlanQty", before.add(accepted));
        detail.put("acceptedQty", accepted);
        detail.put("gapQty", gap);
        detail.put("skipReason", skipped);
        detail.put("unfilledReason", unfilled);
        detail.put("capacityBefore", capacityBefore);
        detail.put("capacityAfter", capacityAfter);
        this.node(material).put(stage, detail);
        return detail;
    }

    /**
     * 获取最终备库目标；预计库存只抵扣一次，机台损耗只结算目标总量一次。
     *
     * @param material  已装载的未来需求与库存
     * @param current   基数，历史模式只取其卷格余数，不保护过量计划
     * @param curl      有效卷长
     * @param totalMode 是否按总量整卷
     * @return 合法目标总计划，未知来源返回0并由专用原因解释
     */
    private BigDecimal stockTarget(RollingMaterialSnapshot material, BigDecimal current, BigDecimal curl, boolean totalMode) {
        if (!material.isStockUpDemandComplete()) {
            return BigDecimal.ZERO;
        }
        BigDecimal loss = material.getCandidates().stream()
                .filter(candidate -> Objects.equals(candidate.getMachineCode(), material.getBoundMachine()))
                .map(RollingCandidateSnapshot::getLossRate).filter(Objects::nonNull).findFirst().orElse(BigDecimal.ZERO);
        BigDecimal requested = material.getStockUpDemandQty().subtract(material.getStockQty()).max(BigDecimal.ZERO);
        requested = requested.multiply(BigDecimal.ONE.add(loss.divide(HUNDRED, 8, RoundingMode.HALF_UP)));
        return this.legalTarget(material, requested, current, curl, totalMode);
    }

    /**
     * 将目标落入合法卷格；历史模式按卷格余数结算，避免把当前过量计划当保护下限。
     *
     * @param material 物料来源策略
     * @param desired   原始目标总量
     * @param base      当前基数
     * @param curl      正卷长
     * @param totalMode 总量或保留基数模式
     * @return 整卷后向上整数米的目标
     */
    private BigDecimal legalTarget(RollingMaterialSnapshot material, BigDecimal desired, BigDecimal base,
                                   BigDecimal curl, boolean totalMode) {
        if (material.isWholeRollExempt()) {
            return RollingDemandOnlyQuantityPolicy.target(desired, material.getMinimumBatch());
        }
        if (desired.signum() <= 0) {
            return BigDecimal.ZERO;
        }
        BigDecimal offset = totalMode ? BigDecimal.ZERO : base.remainder(curl);
        return desired.subtract(offset).max(BigDecimal.ZERO).divide(curl, 0, RoundingMode.CEILING)
                .multiply(curl).add(offset).setScale(0, RoundingMode.CEILING);
    }

    /**
     * 判断整数行是否与整卷向上整数兼容，不使用1米容差，不推断真实来源。
     *
     * @param row 待追加行
     * @return 未开工且数学兼容时使用总量模式
     */
    private boolean totalMode(RollingTaskSnapshot row) {
        BigDecimal quantity = row.getPlanQty();
        BigDecimal rolls = quantity.divide(row.getCurlLength(), 0, RoundingMode.FLOOR);
        return row.getFinishQty().signum() == 0 && quantity.stripTrailingZeros().scale() <= 0 && rolls.signum() > 0
                && rolls.multiply(row.getCurlLength()).setScale(0, RoundingMode.CEILING).compareTo(quantity) == 0;
    }

    /**
     * 纯试制量试无净缺口时不以最低量凭空制造生产；普通物料保持原行为。
     *
     * @param material 当前物料及真实需求库存
     * @return 本次最低量保护目标
     */
    private BigDecimal minimumTarget(RollingMaterialSnapshot material) {
        if (material.isWholeRollExempt()
                && material.getUpDemandQty().max(material.getStockUpDemandQty())
                .subtract(material.getStockQty()).signum() <= 0) {
            return BigDecimal.ZERO;
        }
        return material.getMinimumBatch();
    }

    /**
     * 查找原机允许追加的正计划行，按稳定顺序选择，不新建、不恢复清零或完成工单。
     *
     * @param input    当前全部任务
     * @param material 当前物料
     * @return 可用原行，查不到返回空
     */
    private RollingTaskSnapshot targetRow(RollingPlanningInput input, RollingMaterialSnapshot material) {
        return input.getMachines().values().stream().flatMap(machine -> machine.getTasks().stream())
                .filter(row -> Objects.equals(material.getMaterialCode(), row.getMaterialCode())
                        && Objects.equals(material.getBoundMachine(), row.getMachineCode()) && row.isEditable()
                        && (!row.isTailTask() || material.isWholeRollExempt()) && row.getPlanQty().signum() > 0
                        && !(row.getInitialPlanQty().signum() > 0
                        && row.getFinishQty().compareTo(row.getInitialPlanQty()) >= 0))
                .sorted(Comparator.comparing(RollingTaskSnapshot::getSequence, Comparator.nullsLast(Integer::compareTo))
                        .thenComparing(RollingTaskSnapshot::getKey)).findFirst().orElse(null);
    }

    /**
     * 检查追加资格，原业务阻断不绕过；暂时容量不足可由当前真实余额重新试算。
     *
     * @param machine  原机
     * @param material 物料
     * @param row      原行
     * @return 阻断原因或空
     */
    private String blockReason(RollingMachineSnapshot machine, RollingMaterialSnapshot material, RollingTaskSnapshot row) {
        if (material.getReason() != null && !"BUSINESS_NO_CAPACITY".equals(material.getReason())) {
            return material.getReason();
        }
        if (row == null) {
            if (material.getTemplate() != null && material.getTemplate().isTailTask()) {
                return "TAIL_TASK_EXCLUDED";
            }
            if (material.getInitialPlanQty().signum() > 0
                    && material.getFinishQty().compareTo(material.getInitialPlanQty()) >= 0) {
                return "WORK_ORDER_COMPLETED";
            }
            if (material.getTemplate() != null && !material.getTemplate().isEditable()) {
                return "RELEASE_STATUS_BLOCKED";
            }
            if (material.getTemplate() != null
                    && !AutomaticRollingPlanner.positive(material.getTemplate().getCurlLength())) {
                return "CURL_LENGTH_UNAVAILABLE";
            }
            return "NO_EDITABLE_NORMAL_TARGET_SHIFT_PLAN";
        }
        if (!AutomaticRollingPlanner.positive(row.getCurlLength())) {
            return "CURL_LENGTH_UNAVAILABLE";
        }
        if (machine.getReason() != null) {
            return machine.getReason();
        }
        if (machine.isAllocationFrozen()) {
            return "MACHINE_ALLOCATION_FROZEN";
        }
        return material.getCandidates().stream()
                .filter(candidate -> Objects.equals(machine.getMachineCode(), candidate.getMachineCode()))
                .map(RollingCandidateSnapshot::getReason).filter(Objects::nonNull).findFirst().orElse(null);
    }

    /**
     * 汇总物料当前实际量，包含已完成量，不以完成量抵扣容量。
     *
     * @param input    当前账本
     * @param material 物料
     * @return 当前目标班合计
     */
    private BigDecimal quantity(RollingPlanningInput input, RollingMaterialSnapshot material) {
        return input.getMachines().values().stream().flatMap(machine -> machine.getTasks().stream())
                .filter(row -> Objects.equals(material.getMaterialCode(), row.getMaterialCode()))
                .map(RollingTaskSnapshot::getPlanQty).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * 获取同一解释节点，保留只读装载阶段写入的来源证据。
     *
     * @param material 当前物料
     * @return 两阶段共用解释节点
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> node(RollingMaterialSnapshot material) {
        return (Map<String, Object>) material.getEvidence().computeIfAbsent("postReplenishment", ignored -> new LinkedHashMap<>());
    }
}
