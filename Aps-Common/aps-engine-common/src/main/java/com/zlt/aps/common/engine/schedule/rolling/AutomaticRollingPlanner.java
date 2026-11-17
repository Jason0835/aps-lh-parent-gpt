package com.zlt.aps.common.engine.schedule.rolling;

import com.zlt.aps.common.core.domain.vo.AutomaticRollingDecision;
import com.zlt.aps.common.core.domain.vo.AutomaticRollingDemandWindowResult;
import com.zlt.aps.common.core.utils.AutomaticRollingCalculationUtils;
import com.zlt.aps.common.core.utils.BigDecimalUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 定时自动滚动纯规划器：米数账本、整卷让位、候选沙箱和目标班时间链。
 */
public class AutomaticRollingPlanner {
    private static final Logger LOG = LoggerFactory.getLogger(AutomaticRollingPlanner.class);
    private static final BigDecimal HUNDRED = new BigDecimal("100");
    private static final BigDecimal HOUR_MILLIS = new BigDecimal("3600000");
    /** 最小计划量兜底阶段：下修之后 */
    private static final String MINIMUM_QTY_STAGE_AFTER_DOWN = "AFTER_DOWN";
    /** 最小计划量兜底阶段：上修之后 */
    private static final String MINIMUM_QTY_STAGE_AFTER_UP = "AFTER_UP";

    /**
     * 逐班消耗计算供应班数，负需求由上层阻断。
     *
     * @param available 初始可用量
     * @param demands   下一班起需求
     * @return 窗口内可确认班数
     */
    public static BigDecimal coverage(BigDecimal available, List<BigDecimal> demands) {
        if (available.signum() < 0) {
            return BigDecimal.ZERO;
        }
        BigDecimal count = BigDecimal.ZERO;
        BigDecimal remaining = available;
        for (BigDecimal quantity : demands) {
            BigDecimal demand = quantity == null ? BigDecimal.ZERO : quantity;
            if (demand.signum() < 0) {
                return count;
            }
            if (demand.signum() == 0) {
                count = count.add(BigDecimal.ONE);
            } else if (remaining.compareTo(demand) >= 0) {
                count = count.add(BigDecimal.ONE);
                remaining = remaining.subtract(demand);
            } else {
                return count.add(remaining.divide(demand, 8, RoundingMode.HALF_UP));
            }
        }
        return count;
    }

    /**
     * 计算顺位排序用覆盖班数：以报表六点净库存为基数，从目标班起逐班扣减成型需求。
     *
     * <p>口径与计划分析报表「初始库存覆盖班数」一致，不叠加本班原计划量，
     * 因此可以真实反映「六点库存还够撑几个班」：小于 1 表示六点库存连本班需求都不够，
     * 排序时优先提前。六点库存来源缺失时回退到预计库存口径，避免影响未提供该字段的场景。</p>
     *
     * @param material 物料快照
     * @return 顺位排序用覆盖班数
     */
    private BigDecimal orderCoverage(RollingMaterialSnapshot material) {
        if (material.getSixClockStockQty() == null) {
            return material.getCoverage();
        }
        List<BigDecimal> demands = new ArrayList<>();
        // 目标班需求缺失时不补零，避免零需求被按「已覆盖一班」计数而虚高
        if (material.getTargetDemandQty() != null) {
            demands.add(material.getTargetDemandQty());
        }
        if (material.getFutureDemand() != null) {
            demands.addAll(material.getFutureDemand());
        }
        return coverage(material.getSixClockStockQty(), demands);
    }

    /**
     * 普通任务A-2整卷下修。
     *
     * @param before  原计划
     * @param finish  完成量
     * @param formula 基础目标
     * @param curl    卷长
     * @return 不超过原量且保护完成量的计划
     */
    public static BigDecimal downQty(BigDecimal before, BigDecimal finish, BigDecimal formula, BigDecimal curl) {
        if (before.signum() < 0 || finish.signum() < 0 || before.compareTo(finish) < 0 || !positive(curl)) {
            throw new IllegalArgumentException("INVALID_QUANTITY_OR_CURL");
        }
        return before.min(ceil(formula.max(finish).max(BigDecimal.ZERO), curl));
    }

    /**
     * 按实时缺口整卷让位；扣减后只允许保留“满一卷或最低起排量”或“整行清零”。
     *
     * @param current  当前计划
     * @param maximum  安全可扣上限
     * @param required 实时缺口
     * @param curl     卷长
     * @param minimum  最低保留起排量
     * @return 本次合法整卷扣量；无法整卷扣减但可安全清空整行时返回当前计划
     */
    public static BigDecimal yieldQty(BigDecimal current, BigDecimal maximum, BigDecimal required,
                                      BigDecimal curl, BigDecimal minimum) {
        if (!positive(curl) || maximum.signum() <= 0 || required.signum() <= 0) {
            return BigDecimal.ZERO;
        }
        BigDecimal keep = curl.max(minimum);
        BigDecimal allowed = current.subtract(keep).max(BigDecimal.ZERO);
        BigDecimal limit = maximum.min(allowed);
        BigDecimal reduce = ceil(limit.min(required), curl);
        if (reduce.compareTo(limit) > 0) {
            reduce = limit.divide(curl, 0, RoundingMode.FLOOR).multiply(curl);
        }
        if (reduce.signum() > 0) {
            return reduce;
        }
        // 扣减后剩余量只允许“不低于保留量”或“清零”，扣不出一整卷时改为整行让出；
        // 安全上限不足以覆盖整行时不扣减，避免让位后低于让位方自身保护需求。
        return maximum.compareTo(current) >= 0 ? current : BigDecimal.ZERO;
    }

    /**
     * 判断正数。
     */
    public static boolean positive(BigDecimal quantity) {
        return quantity != null && quantity.signum() > 0;
    }

    /**
     * 严格十进制定点整卷检查。
     */
    public static boolean integral(BigDecimal quantity, BigDecimal curl) {
        return positive(curl) && quantity != null && quantity.remainder(curl).signum() == 0;
    }

    /**
     * 将米数向上补成整卷。
     */
    public static BigDecimal ceil(BigDecimal quantity, BigDecimal curl) {
        return quantity.divide(curl, 0, RoundingMode.CEILING).multiply(curl);
    }

    /**
     * 执行正常滚动及已启用的后置补量，只修改独立快照，不访问数据库或生成工单。
     *
     * @param input 主侧输入及实时账本
     * @return 包含最终任务链和物料终态的同一上下文
     */
    public RollingPlanningInput plan(RollingPlanningInput input) {
        String before = this.signature(input);
        Map<String, Map<String, Object>> initialTaskStates = this.captureTaskStates(input);
        AutomaticRollingReplenishmentPlanner replenishment = new AutomaticRollingReplenishmentPlanner();
        replenishment.prepare(input);
        input.getMaterials().values().forEach(material -> this.decide(input, material));
        input.getMaterials().values().stream()
                .filter(material -> "DOWN".equals(material.getDirection()) && material.getReason() == null)
                .forEach(material -> this.reduce(input, material));
        // 最小计划量兜底紧接在下修之后：先把残量收敛到可开机下限，再进入上修承接轮。
        this.enforceMinimumPlanQty(input, MINIMUM_QTY_STAGE_AFTER_DOWN);
        Comparator<RollingMaterialSnapshot> urgency = Comparator.comparing(RollingMaterialSnapshot::getCoverage)
                .thenComparing(RollingMaterialSnapshot::getMaterialCode);
        List<RollingMaterialSnapshot> up = input.getMaterials().values().stream()
                .filter(material -> "UP".equals(material.getDirection()) && material.getReason() == null)
                .sorted(urgency).collect(Collectors.toList());
        up.stream().filter(material -> material.getBoundMachine() != null)
                .forEach(material -> this.increase(input, material, IncreaseMode.NORMAL));
        up.stream().filter(material -> material.getBoundMachine() == null)
                .forEach(material -> this.increase(input, material, IncreaseMode.NORMAL));
        // 补齐轮：正常承接只拿到部分产能、后续让位又腾出余量的上修规格，按当前真实余量把剩余缺口补一次
        up.stream().filter(material -> material.getReason() == null
                        && this.actualQty(input, material.getMaterialCode()).compareTo(material.getTargetQty()) < 0)
                .forEach(material -> this.increase(input, material, IncreaseMode.TOP_UP));
        // 强排兜底放在最后一轮统一执行：只处理完全没有承接的上修规格，忽略产能上限按目标量补足；
        // 避免先强排的规格提前吃掉产能，导致后续原本能通过让位承接的规格反而落空。
        // 已部分承接的规格由补齐轮处理，不再强排，避免把正常让位后的合理缺口也强行顶到目标量。
        up.stream().filter(material -> "BUSINESS_NO_CAPACITY".equals(material.getReason()))
                .forEach(material -> this.increase(input, material, IncreaseMode.FORCED));
        // 上修各轮结束后再兜底一次：上修新增出来的行在第一轮兜底时计划量还是0会被跳过，
        // 若最终计划量仍为正且低于最小计划量，同样属于无法开机的残量，需在后置补量前收敛。
        // 库存清零轮让出的行计划量仍为0，不会在此被补回。
        this.enforceMinimumPlanQty(input, MINIMUM_QTY_STAGE_AFTER_UP);
        // 产能让位轮：上修各轮结束后，对仍因产能不足没抬到应得量的规格，
        // 从同机台库存富余的规格让出产能再补一次；放在后置补量与填谷轮之前，
        // 让出的产能才能被缺口规格真正吸收。
        this.yieldForCapacityShortage(input);
        // 后置仅使用正常滚动后的真实余量，不再次让位或新建工单。
        replenishment.replenish(input);
        // 余量填谷轮放在所有补量之后：用最后剩余产能给未达需求窗口的已开机规格按班补量。
        this.fillRemainingCapacity(input);
        for (RollingMaterialSnapshot material : input.getMaterials().values()) {
            List<RollingTaskSnapshot> materialRows = this.rows(input, material.getMaterialCode());
            material.setActualQty(materialRows.stream()
                    .map(RollingTaskSnapshot::getPlanQty).reduce(BigDecimal.ZERO, BigDecimal::add));
            if (materialRows.stream().anyMatch(task -> task.getRoundingRule() != null)) {
                material.getEvidence().put("roundingRule", "INTEGER_CEILING");
                material.getEvidence().put("integerCeilingIncreaseQty", materialRows.stream()
                        .map(RollingTaskSnapshot::getRoundingAddedQty).reduce(BigDecimal.ZERO, BigDecimal::add));
            }
            material.setAcceptedQty("UP".equals(material.getDirection())
                    ? material.getActualQty().subtract(material.getInitialPlanQty()).max(BigDecimal.ZERO)
                    : BigDecimal.ZERO);
            material.setGapQty("UP".equals(material.getDirection())
                    ? material.getTargetQty().subtract(material.getActualQty()).max(BigDecimal.ZERO)
                    : BigDecimal.ZERO);
        }
        for (RollingMachineSnapshot machine : input.getMachines().values()) {
            boolean planChanged = machine.getTasks().stream().anyMatch(task ->
                    task.getPlanQty().compareTo(task.getInitialPlanQty()) != 0);
            boolean orderChanged = this.orderingChanged(input, machine);
            if (planChanged || orderChanged) {
                if (input.isShortageFirstEnabled()) {
                    LOG.info("[TM_ROLLING_ORDER] 机台={} 触发重排，计划量变化={}，顺位变化={}",
                            machine.getMachineCode(), planChanged, orderChanged);
                }
                this.retime(input, machine);
            }
        }
        this.recordTaskChanges(input, initialTaskStates);
        input.setPlanChanged(!before.equals(this.signature(input)));
        this.aggregate(input);
        return input;
    }

    /**
     * 计算方向、完成量保护和固定排序快照。
     *
     * @param input    阈值配置
     * @param material 物料输入
     */
    private void decide(RollingPlanningInput input, RollingMaterialSnapshot material) {
        BigDecimal available = material.getStockQty().add(material.getInitialPlanQty());
        material.setCoverage(coverage(available, material.getFutureDemand()));
        // 顺位排序单独使用六点库存覆盖班数，避免目标班之前的计划量与需求递推影响缺料严重程度的判断
        material.setOrderCoverage(this.orderCoverage(material));
        BigDecimal totalDemand = material.getFutureDemand().stream().filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        material.setWindowCapped(material.isDemandComplete() && available.signum() >= 0 && available.compareTo(totalDemand) >= 0);
        material.setEndingAvailableQty(available.subtract(totalDemand).max(BigDecimal.ZERO));
        AutomaticRollingDecision decision = this.stockDownToZero(input, material);
        boolean stockDownToZero = decision != null;
        if (!stockDownToZero) {
            decision = AutomaticRollingCalculationUtils.decide(material.getStockQty(),
                    material.getInitialPlanQty(), material.getFinishQty(),
                    AutomaticRollingCalculationUtils.accumulateDemand(material.getFutureDemand(), input.getUpThreshold()),
                    AutomaticRollingCalculationUtils.accumulateDemand(material.getFutureDemand(), input.getDownThreshold()),
                    AutomaticRollingCalculationUtils.accumulateDemand(material.getFutureDemand(), input.getDownTarget()));
            if (input.isPostReplenishmentEnabled() && "DOWN".equals(decision.getDirection())
                    && material.getReplenishmentProtectedQty().signum() > 0) {
                BigDecimal protectedQty = material.getReplenishmentProtectedQty();
                BigDecimal effectiveDownDemand = decision.getDownDemandQty().max(material.getStockQty().add(protectedQty));
                Map<String, Object> protection = new LinkedHashMap<>();
                protection.put("originalDownDemandQty", decision.getDownDemandQty());
                protection.put("effectiveDownDemandQty", effectiveDownDemand);
                protection.put("protectedPlanQty", protectedQty);
                material.getEvidence().put("replenishmentDownProtection", protection);
                if (available.compareTo(effectiveDownDemand) <= 0) {
                    decision.setDirection("NONE");
                    decision.setTargetPlanQty(material.getInitialPlanQty());
                } else {
                    decision.setTargetPlanQty(decision.getTargetPlanQty().max(protectedQty));
                }
            }
        }
        material.setDirection(decision.getDirection());
        material.setFormulaQty(decision.getTargetPlanQty());
        material.setTargetQty(decision.getTargetPlanQty());
        material.setUpDemandQty(decision.getUpDemandQty());
        if (material.getInitialPlanQty().signum() < 0 || material.getFinishQty().signum() < 0
                || material.getFinishQty().compareTo(material.getInitialPlanQty()) > 0
                || material.getFutureDemand().stream().anyMatch(qty -> qty != null && qty.signum() < 0)) {
            material.setReason("INVALID_QUANTITY");
        }
        if (!"NONE".equals(material.getDirection()) && material.getBoundMachine() != null) {
            RollingMachineSnapshot machine = input.getMachines().get(material.getBoundMachine());
            if (machine != null && machine.getReason() != null && !"FULL_SHIFT_MAINTENANCE".equals(machine.getReason())
                    && material.getReason() == null) {
                material.setReason(machine.getReason());
            }
        }
        // 不可调或资料异常的任务仍占容量，但不参与让位、数量变更和移动。
        if (material.getReason() != null) {
            this.rows(input, material.getMaterialCode()).forEach(task -> task.setEditable(false));
        }
    }

    /**
     * 库存清零前置轮：预计库存（不含当班计划）已覆盖下修阈值窗口需求时，本班计划直接清零让出产能。
     * 该轮先于上下修阈值公式判定，命中后不再受最低起排量保护、整卷校验及后置补量回补约束。
     *
     * @param input    阈值及需求窗口配置
     * @param material 物料输入
     * @return 清零决策；未命中返回null交回原阈值公式
     */
    private AutomaticRollingDecision stockDownToZero(RollingPlanningInput input, RollingMaterialSnapshot material) {
        // 已存在业务阻断的物料不参与清零，避免证据与实际写入不一致。
        if (material.getReason() != null) {
            this.recordStockDownToZeroSkip(material, "BUSINESS_BLOCKED");
            return null;
        }
        // 当班本来没有计划时无需让出产能，保持原阈值公式结论。
        if (material.getInitialPlanQty().signum() <= 0) {
            this.recordStockDownToZeroSkip(material, "NO_CURRENT_PLAN");
            return null;
        }
        // 下修阈值窗口内前若干班缺少真实需求来源时累计需求会被低估，清零可能造成后续断料，保持原阈值公式行为。
        if (!material.isDownWindowDemandComplete()) {
            this.recordStockDownToZeroSkip(material, "DEMAND_WINDOW_INCOMPLETE");
            return null;
        }
        AutomaticRollingDemandWindowResult downWindow = AutomaticRollingCalculationUtils.accumulateDemand(
                material.getFutureDemand(), input.getDownThreshold());
        if (material.getStockQty().compareTo(downWindow.getDemandQty()) < 0) {
            this.recordStockDownToZeroSkip(material, "STOCK_BELOW_DOWN_DEMAND");
            // 未命中时一并留出阈值需求，便于直接核对库存与阈值窗口的差距。
            material.getEvidence().put("downThresholdDemandQty", downWindow.getDemandQty());
            return null;
        }
        // 完成量已发生不可回退，清零目标不得低于当班已完成量。
        BigDecimal zeroPlanQty = material.getFinishQty().max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
        AutomaticRollingDecision decision = new AutomaticRollingDecision();
        decision.setDirection("DOWN");
        decision.setUpDemandQty(AutomaticRollingCalculationUtils.accumulateDemand(
                material.getFutureDemand(), input.getUpThreshold()).getDemandQty());
        decision.setDownDemandQty(downWindow.getDemandQty());
        decision.setDownTargetDemandQty(AutomaticRollingCalculationUtils.accumulateDemand(
                material.getFutureDemand(), input.getDownTarget()).getDemandQty());
        decision.setFormulaPlanQty(BigDecimal.ZERO);
        decision.setTargetPlanQty(zeroPlanQty);
        material.setStockDownToZero(true);
        Map<String, Object> evidence = new LinkedHashMap<>();
        evidence.put("rule", "STOCK_DOWN_TO_ZERO");
        evidence.put("stockQty", material.getStockQty());
        evidence.put("downThreshold", input.getDownThreshold());
        evidence.put("downThresholdDemandQty", downWindow.getDemandQty());
        evidence.put("demandWindowSize", material.getFutureDemand().size());
        evidence.put("initialPlanQty", material.getInitialPlanQty());
        evidence.put("finishQty", material.getFinishQty());
        evidence.put("targetPlanQty", zeroPlanQty);
        material.getEvidence().put("stockDownToZero", evidence);
        // 清零优先于后置补量保护，同步修正保护证据口径，避免审计仍显示受最低起排量保护。
        Object postReplenishment = material.getEvidence().get("postReplenishment");
        if (postReplenishment instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> protectionNode = (Map<String, Object>) postReplenishment;
            protectionNode.put("protectionScope", "STOCK_DOWN_TO_ZERO");
        }
        return decision;
    }

    /**
     * 记录库存清零轮未命中原因，便于在解释证据中直接定位被哪道门槛挡下。
     *
     * @param material 物料快照
     * @param reason   未命中原因编码
     */
    private void recordStockDownToZeroSkip(RollingMaterialSnapshot material, String reason) {
        material.getEvidence().put("stockDownToZeroSkipReason", reason);
    }

    /**
     * 执行无重复损耗的A-2下修；多行按稳定倒序释放，保留每行完成量。
     *
     * @param input    本轮账本
     * @param material 下修物料
     */
    private void reduce(RollingPlanningInput input, RollingMaterialSnapshot material) {
        List<RollingTaskSnapshot> rows = this.rows(input, material.getMaterialCode());
        BigDecimal desiredReduction = material.getInitialPlanQty().subtract(material.getFormulaQty()).max(BigDecimal.ZERO);
        // 库存清零轮以整行让出产能为目标，不再受整卷、卷长及最低起排量约束。
        boolean stockDownToZero = material.isStockDownToZero();
        for (int index = rows.size() - 1; index >= 0 && desiredReduction.signum() > 0; index--) {
            RollingTaskSnapshot task = rows.get(index);
            if (!task.isEditable() || this.completed(task)) {
                continue;
            }
            if (!stockDownToZero) {
                if (!task.isTailTask() && !positive(task.getCurlLength())) {
                    material.setReason("CURL_LENGTH_UNAVAILABLE");
                    continue;
                }
                if (!material.isWholeRollExempt() && !task.isTailTask() && !integral(task.getInitialPlanQty(), task.getCurlLength())) {
                    material.setReason("DOWN_NON_INTEGRAL_ORIGINAL");
                    continue;
                }
            }
            BigDecimal before = task.getPlanQty();
            BigDecimal base = before.subtract(desiredReduction).max(task.getFinishQty()).max(BigDecimal.ZERO);
            BigDecimal calculatedAfter = stockDownToZero ? base
                    : material.isWholeRollExempt()
                    ? RollingDemandOnlyQuantityPolicy.target(base, material.getMinimumBatch()).min(before)
                    : task.isTailTask() ? base : downQty(before, task.getFinishQty(), base, task.getCurlLength());
            BigDecimal after = this.ceilPlanRowQty(task, calculatedAfter);
            BigDecimal actualReleased = before.subtract(after).max(BigDecimal.ZERO);
            desiredReduction = desiredReduction.subtract(actualReleased).max(BigDecimal.ZERO);
            task.setPlanQty(after);
        }
        material.setTargetQty(rows.stream().map(RollingTaskSnapshot::getPlanQty).reduce(BigDecimal.ZERO, BigDecimal::add));
    }

    /**
     * 最小计划量兜底轮：计划量仍为正但低于SYS0804016时补到该阈值，避免留下无法开机的残量。
     * 计划量为0表示本班不投产（含库存清零轮结果），保持0不补；补齐按行执行且忽略机台产能上限。
     *
     * <p>该轮在下修后、上修后各执行一次：上修新增出来的行在下修后的那轮计划量仍为0会被跳过，
     * 必须在上修各轮结束后再兜底一次，否则会残留 1 米这类无法开机的计划量。</p>
     *
     * @param input 当前账本及最小计划量配置
     * @param stage 兜底阶段标识：AFTER_DOWN（下修后）/ AFTER_UP（上修后）
     */
    private void enforceMinimumPlanQty(RollingPlanningInput input, String stage) {
        BigDecimal minimumQty = input.getMinimumPlanQty();
        if (!positive(minimumQty)) {
            return;
        }
        for (RollingMaterialSnapshot material : input.getMaterials().values()) {
            List<RollingTaskSnapshot> rows = this.rows(input, material.getMaterialCode());
            List<Map<String, Object>> adjustments = new ArrayList<>();
            for (RollingTaskSnapshot task : rows) {
                // 不可编辑行和已完成行不允许调整，保持原计划。
                if (!task.isEditable() || this.completed(task)) {
                    continue;
                }
                BigDecimal before = task.getPlanQty();
                // 仅兜底已开工但不足最小计划量的行，零计划保持零。
                if (before.signum() <= 0 || before.compareTo(minimumQty) >= 0) {
                    continue;
                }
                // 不经过整卷结算，直接按阈值整数米补齐，避免污染原下修行的取整证据。
                BigDecimal after = minimumQty.max(task.getFinishQty()).setScale(0, RoundingMode.CEILING);
                task.setPlanQty(after);
                Map<String, Object> adjustment = new LinkedHashMap<>();
                adjustment.put("stage", stage);
                adjustment.put("rowKey", task.getKey());
                adjustment.put("beforePlanQty", before);
                adjustment.put("afterPlanQty", after);
                adjustments.add(adjustment);
            }
            if (adjustments.isEmpty()) {
                continue;
            }
            // 兜底只会抬高计划量，目标量按「原目标」与「兜底后合计」取大：
            // 上修物料的部分承接缺口不能被行合计覆盖，否则会掩盖未承接量。
            BigDecimal adjustedTotalQty = rows.stream().map(RollingTaskSnapshot::getPlanQty)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal currentTargetQty = material.getTargetQty() == null ? BigDecimal.ZERO : material.getTargetQty();
            material.setTargetQty(currentTargetQty.max(adjustedTotalQty));
            this.appendMinimumPlanQtyEvidence(material, minimumQty, adjustments);
        }
    }

    /**
     * 累加最小计划量兜底证据：同一物料可能在下修后、上修后两轮都被兜底，需保留两轮明细。
     *
     * @param material    物料快照
     * @param minimumQty  最小计划量阈值
     * @param adjustments 本次兜底的行级调整明细
     */
    @SuppressWarnings("unchecked")
    private void appendMinimumPlanQtyEvidence(RollingMaterialSnapshot material, BigDecimal minimumQty,
                                              List<Map<String, Object>> adjustments) {
        Object current = material.getEvidence().get("minimumPlanQtyAdjust");
        if (current instanceof Map) {
            Object existRows = ((Map<String, Object>) current).get("rows");
            if (existRows instanceof List) {
                ((List<Map<String, Object>>) existRows).addAll(adjustments);
                return;
            }
        }
        Map<String, Object> evidence = new LinkedHashMap<>();
        evidence.put("minimumPlanQty", minimumQty);
        evidence.put("rows", new ArrayList<>(adjustments));
        material.getEvidence().put("minimumPlanQtyAdjust", evidence);
    }

    /**
     * 产能让位轮：上修各轮结束后，对仍因产能不足没有达到应得量的上修规格，
     * 从同一机台上库存富余的规格让出产能，再补一次。
     *
     * <p>让位方需同时满足：非收尾行、可编辑、完成量为0、方向不是UP。
     * 窗口需求取「本班 + 上修阈值班数」的累计需求（SYS0804005 默认1，即前两班）。
     * 六点库存已覆盖窗口需求时整行清零，该班不做这个规格，不受最小开机量限制；
     * 六点库存加计划量能覆盖时，下修到 max(最低起排量, 窗口需求 − 六点库存) 并按整卷向上取整。
     * 六点库存缺失按0参与判断，用于覆盖「本身计划量很高、但需求已发生变化」的规格。
     * 让位与补量都只在本机台内进行。</p>
     *
     * @param input 本轮规划结果
     */
    private void yieldForCapacityShortage(RollingPlanningInput input) {
        BigDecimal windowShiftCount = BigDecimal.ONE.add(input.getUpThreshold());
        List<RollingMaterialSnapshot> shortages = input.getMaterials().values().stream()
                .filter(material -> "UP".equals(material.getDirection()))
                .filter(material -> material.getBoundMachine() != null)
                .filter(material -> this.actualQty(input, material.getMaterialCode())
                        .compareTo(material.getTargetQty()) < 0)
                .sorted(Comparator.comparing(RollingMaterialSnapshot::getCoverage)
                        .thenComparing(RollingMaterialSnapshot::getMaterialCode))
                .collect(Collectors.toList());
        if (shortages.isEmpty()) {
            return;
        }
        for (RollingMaterialSnapshot shortage : shortages) {
            RollingMachineSnapshot machine = input.getMachines().get(shortage.getBoundMachine());
            if (machine == null) {
                continue;
            }
            BigDecimal released = this.yieldFromMachine(input, machine, shortage.getMaterialCode(),
                    windowShiftCount);
            if (released.signum() <= 0) {
                continue;
            }
            Map<String, Object> gain = new LinkedHashMap<>();
            gain.put("releasedQty", released);
            gain.put("windowShiftCount", windowShiftCount);
            gain.put("actualBeforeQty", this.actualQty(input, shortage.getMaterialCode()));
            // 让出的产能立即补给缺口规格，仍受实时余量约束
            this.increase(input, shortage, IncreaseMode.TOP_UP);
            gain.put("actualAfterQty", this.actualQty(input, shortage.getMaterialCode()));
            shortage.getEvidence().put("capacityYieldGain", gain);
        }
    }

    /**
     * 在指定机台上为缺口规格让出产能。
     *
     * @param input            本轮规划结果
     * @param machine          缺口规格所在机台
     * @param shortageCode     缺口规格编码
     * @param windowShiftCount 窗口班数
     * @return 实际让出的计划量合计
     */
    private BigDecimal yieldFromMachine(RollingPlanningInput input, RollingMachineSnapshot machine,
                                        String shortageCode, BigDecimal windowShiftCount) {
        List<RollingTaskSnapshot> donors = machine.getTasks().stream()
                .filter(task -> !shortageCode.equals(task.getMaterialCode()))
                .filter(task -> task.isEditable() && !this.completed(task) && !task.isTailTask())
                .filter(task -> task.getPlanQty().signum() > 0)
                .sorted(Comparator.comparing((RollingTaskSnapshot task) ->
                                input.getMaterials().get(task.getMaterialCode()).getCoverage()).reversed()
                        .thenComparing(RollingTaskSnapshot::getMaterialCode))
                .collect(Collectors.toList());
        BigDecimal released = BigDecimal.ZERO;
        for (RollingTaskSnapshot task : donors) {
            RollingMaterialSnapshot donor = input.getMaterials().get(task.getMaterialCode());
            // 让位方不包含上修方向，避免把缺口在需求上升的规格之间转移
            if (donor == null || "UP".equals(donor.getDirection())) {
                continue;
            }
            BigDecimal reduce = this.capacityYieldQty(donor, task, windowShiftCount);
            if (reduce.signum() <= 0) {
                continue;
            }
            BigDecimal before = task.getPlanQty();
            // 让位扣减同样必须落整数米：保留量是整卷倍数（卷长多为小数），直接相减会得到小数，
            // 落库前的整数校验会因此抛异常并整笔回滚。
            BigDecimal after = this.ceilPlanRowQty(task, before.subtract(reduce));
            BigDecimal actualReduce = before.subtract(after).max(BigDecimal.ZERO);
            if (actualReduce.signum() <= 0) {
                continue;
            }
            task.setPlanQty(after);
            // 释放量按取整后的真实行量差结算，避免机台余额多算不足1米的取整差额
            released = released.add(actualReduce);
            // 整行清零让位：该班不做这个规格，后置补量不得再补回最低起排量
            if (actualReduce.compareTo(before) >= 0) {
                donor.getEvidence().put("capacityYieldCleared", true);
            }
            Map<String, Object> detail = new LinkedHashMap<>();
            detail.put("materialCode", donor.getMaterialCode());
            detail.put("rule", this.capacityYieldRule(donor, windowShiftCount));
            detail.put("sixClockStockQty", BigDecimalUtils.valueOf(donor.getSixClockStockQty()));
            detail.put("windowDemandQty", this.windowDemandQty(donor, windowShiftCount));
            detail.put("beforePlanQty", before);
            detail.put("afterPlanQty", task.getPlanQty());
            // 记录整卷口径的请求扣减量与取整后的实际释放量，便于核对不足1米的取整差额
            detail.put("requestedReduceQty", reduce);
            detail.put("releasedQty", actualReduce);
            detail.put("minimumBatch", donor.getMinimumBatch());
            detail.put("curlLength", task.getCurlLength());
            detail.put("forShortageCode", shortageCode);
            this.appendCapacityYieldEvidence(donor, detail);
        }
        return released;
    }

    /**
     * 计算让位方可让出的计划量。
     *
     * @param donor            让位物料
     * @param task             让位行
     * @param windowShiftCount 窗口班数
     * @return 可让出量；不满足让位条件时返回0
     */
    private BigDecimal capacityYieldQty(RollingMaterialSnapshot donor, RollingTaskSnapshot task,
                                        BigDecimal windowShiftCount) {
        BigDecimal planQty = task.getPlanQty();
        BigDecimal windowDemandQty = this.windowDemandQty(donor, windowShiftCount);
        // 六点库存缺失按0参与判断，用于覆盖「本身计划量很高、但需求已发生变化」的规格
        BigDecimal sixClockStockQty = BigDecimalUtils.valueOf(donor.getSixClockStockQty());
        // 六点库存已覆盖窗口需求：整行清零，该班不做这个规格，不受最小开机量限制
        if (sixClockStockQty.compareTo(windowDemandQty) >= 0) {
            return planQty;
        }
        // 六点库存加计划量仍覆盖不了窗口需求：不让位
        if (sixClockStockQty.add(planQty).compareTo(windowDemandQty) < 0) {
            return BigDecimal.ZERO;
        }
        BigDecimal minimumBatch = donor.getMinimumBatch() == null ? BigDecimal.ZERO : donor.getMinimumBatch();
        BigDecimal keepQty = windowDemandQty.subtract(sixClockStockQty).max(minimumBatch);
        if (!positive(task.getCurlLength())) {
            // 卷长缺失时按整数米保留，避免让位后留下小数计划量
            return planQty.subtract(keepQty.setScale(0, RoundingMode.CEILING)).max(BigDecimal.ZERO);
        }
        // 保留量按整卷向上取整，让位后的剩余计划量必须是整卷
        BigDecimal keep = ceil(keepQty, task.getCurlLength());
        return planQty.subtract(keep).max(BigDecimal.ZERO);
    }

    /**
     * 取让位窗口的累计需求量：本班 + 上修阈值班数。
     *
     * @param material         物料快照
     * @param windowShiftCount 窗口班数
     * @return 窗口累计需求
     */
    private BigDecimal windowDemandQty(RollingMaterialSnapshot material, BigDecimal windowShiftCount) {
        return AutomaticRollingCalculationUtils.accumulateDemand(material.getFutureDemand(), windowShiftCount)
                .getDemandQty();
    }

    /**
     * 判定让位规则编码，便于在解释证据中区分整行清零与下修两种口径。
     *
     * @param donor            让位物料
     * @param windowShiftCount 窗口班数
     * @return 规则编码
     */
    private String capacityYieldRule(RollingMaterialSnapshot donor, BigDecimal windowShiftCount) {
        BigDecimal sixClockStockQty = BigDecimalUtils.valueOf(donor.getSixClockStockQty());
        return sixClockStockQty.compareTo(this.windowDemandQty(donor, windowShiftCount)) >= 0
                ? "STOCK_COVERS_WINDOW" : "DOWN_TO_WINDOW_DEMAND";
    }

    /**
     * 累加让位证据：同一规格可能为多个缺口规格让位，需保留每次让位的明细。
     *
     * @param donor  让位物料
     * @param detail 本次让位明细
     */
    @SuppressWarnings("unchecked")
    private void appendCapacityYieldEvidence(RollingMaterialSnapshot donor, Map<String, Object> detail) {
        Object current = donor.getEvidence().get("capacityYield");
        if (current instanceof List) {
            ((List<Map<String, Object>>) current).add(detail);
            return;
        }
        List<Map<String, Object>> details = new ArrayList<>();
        details.add(detail);
        donor.getEvidence().put("capacityYield", details);
    }

    /**
     * 余量填谷轮：前面各轮补完后仍有剩余产能时，对已开机且供应班数未达需求窗口的规格按班补量。
     * 每轮按供应班数升序逐规格补一个班的需求量，一轮结束后重新排序重复，
     * 直到机台没有剩余产能或没有可补量规格为止。
     *
     * @param input 当前账本
     */
    private void fillRemainingCapacity(RollingPlanningInput input) {
        if (!input.isRemainingCapacityFillEnabled()) {
            return;
        }
        // 最长需求窗口决定理论最大轮次，额外留一轮兜底，避免整卷取整推进缓慢时反复空转。
        int maximumRound = input.getMaterials().values().stream()
                .mapToInt(material -> material.getFutureDemand().size()).max().orElse(0) + 1;
        for (int round = 0; round < maximumRound; round++) {
            List<RollingMaterialSnapshot> candidates = input.getMaterials().values().stream()
                    .filter(material -> this.fillable(input, material))
                    .sorted(Comparator.comparing((RollingMaterialSnapshot material) -> this.liveCoverage(input, material))
                            .thenComparing(RollingMaterialSnapshot::getMaterialCode))
                    .collect(Collectors.toList());
            if (candidates.isEmpty()) {
                return;
            }
            boolean filled = false;
            for (RollingMaterialSnapshot material : candidates) {
                if (this.fillOneShift(input, material)) {
                    filled = true;
                }
            }
            if (!filled) {
                return;
            }
        }
    }

    /**
     * 判断规格是否参与余量填谷。
     *
     * @param input    当前账本
     * @param material 物料快照
     * @return 可补量返回true
     */
    private boolean fillable(RollingPlanningInput input, RollingMaterialSnapshot material) {
        // 业务阻断、未绑定机台、纯试制量试、收尾及已触发下修的规格不参与填谷，避免下修后又被补回。
        if (material.getReason() != null || material.getBoundMachine() == null
                || material.isWholeRollExempt() || "DOWN".equals(material.getDirection())) {
            return false;
        }
        RollingTaskSnapshot template = material.getTemplate();
        if (template == null || template.isTailTask()) {
            return false;
        }
        // 只给已开机规格填谷，计划量为0的规格保持本班不投产。
        if (this.actualQty(input, material.getMaterialCode()).signum() <= 0) {
            return false;
        }
        // 供应班数已达到下修阈值或需求窗口长度时不再补量，防止触发下一轮下修震荡。
        return this.liveCoverage(input, material).compareTo(this.fillCoverageLimit(input, material)) < 0;
    }

    /**
     * 计算余量填谷允许达到的最高供应班数上限。
     *
     * @param input    当前账本
     * @param material 物料快照
     * @return 供应班数上限
     */
    private BigDecimal fillCoverageLimit(RollingPlanningInput input, RollingMaterialSnapshot material) {
        BigDecimal windowLimit = BigDecimalUtils.valueOf(material.getFutureDemand().size());
        return positive(input.getDownThreshold()) ? input.getDownThreshold().min(windowLimit) : windowLimit;
    }

    /**
     * 按实时计划量重算可供应班数，供填谷排序和窗口判断使用。
     *
     * @param input    当前账本
     * @param material 物料快照
     * @return 实时可供应班数
     */
    private BigDecimal liveCoverage(RollingPlanningInput input, RollingMaterialSnapshot material) {
        return coverage(material.getStockQty().add(this.actualQty(input, material.getMaterialCode())),
                material.getFutureDemand());
    }

    /**
     * 取下一个尚未覆盖班次的需求量，作为一次填谷的量。
     *
     * @param input    当前账本
     * @param material 物料快照
     * @return 单班需求量；窗口已覆盖或需求缺失时返回0
     */
    private BigDecimal nextShiftDemand(RollingPlanningInput input, RollingMaterialSnapshot material) {
        List<BigDecimal> demands = material.getFutureDemand();
        if (demands == null || demands.isEmpty()) {
            return BigDecimal.ZERO;
        }
        int index = this.liveCoverage(input, material).max(BigDecimal.ZERO)
                .setScale(0, RoundingMode.FLOOR).intValue();
        return index >= demands.size() ? BigDecimal.ZERO : BigDecimalUtils.valueOf(demands.get(index));
    }

    /**
     * 给单个规格补一个班的量：按损耗率和整卷向上取整，余量不足一个补量单位时不补。
     *
     * @param input    当前账本
     * @param material 物料快照
     * @return 实际补量返回true
     */
    @SuppressWarnings("unchecked")
    private boolean fillOneShift(RollingPlanningInput input, RollingMaterialSnapshot material) {
        RollingMachineSnapshot machine = input.getMachines().get(material.getBoundMachine());
        if (machine == null || machine.getReason() != null) {
            return false;
        }
        BigDecimal available = this.availableQty(input, machine);
        if (available.signum() <= 0) {
            return false;
        }
        // 只在本机台已有可编辑未完成行上追加，不新建行也不改其他机台。
        RollingTaskSnapshot targetRow = this.rows(input, material.getMaterialCode()).stream()
                .filter(task -> task.isEditable() && !this.completed(task)
                        && machine.getMachineCode().equals(task.getMachineCode()))
                .min(Comparator.comparing(RollingTaskSnapshot::getSequence, Comparator.nullsLast(Integer::compareTo))
                        .thenComparing(RollingTaskSnapshot::getKey, Comparator.nullsLast(String::compareTo))).orElse(null);
        if (targetRow == null) {
            return false;
        }
        BigDecimal oneShiftQty = this.nextShiftDemand(input, material);
        if (!positive(oneShiftQty)) {
            return false;
        }
        BigDecimal coverageBefore = this.liveCoverage(input, material);
        BigDecimal lossRate = material.getCandidates().stream()
                .filter(candidate -> machine.getMachineCode().equals(candidate.getMachineCode()))
                .map(RollingCandidateSnapshot::getLossRate).filter(Objects::nonNull)
                .findFirst().orElse(BigDecimal.ZERO);
        BigDecimal demandQty = oneShiftQty.multiply(BigDecimal.ONE.add(
                lossRate.divide(HUNDRED, 8, RoundingMode.HALF_UP)));
        // 与上修承接一致按整卷向上取整，卷长缺失时退回整数米向上取整。
        BigDecimal addQty = positive(targetRow.getCurlLength()) ? ceil(demandQty, targetRow.getCurlLength())
                : demandQty.setScale(0, RoundingMode.CEILING);
        if (addQty.signum() <= 0) {
            return false;
        }
        BigDecimal before = targetRow.getPlanQty();
        // 计划量必须落整数米：历史小数行追加整卷量后统一向上取整，避免持久化前整数校验失败。
        BigDecimal after = this.ceilIntegerQty(before.add(addQty));
        BigDecimal actualAddQty = after.subtract(before);
        if (actualAddQty.signum() <= 0 || actualAddQty.compareTo(available) > 0) {
            return false;
        }
        targetRow.setPlanQty(after);
        BigDecimal coverageAfter = this.liveCoverage(input, material);
        if (positive(input.getDownThreshold()) && coverageAfter.compareTo(input.getDownThreshold()) > 0) {
            targetRow.setPlanQty(before);
            return false;
        }
        Map<String, Object> record = new LinkedHashMap<>();
        record.put("rowKey", targetRow.getKey());
        record.put("machineCode", machine.getMachineCode());
        record.put("oneShiftDemandQty", oneShiftQty);
        record.put("lossRate", lossRate);
        record.put("roundAddQty", addQty);
        record.put("addQty", actualAddQty);
        record.put("beforePlanQty", before);
        record.put("afterPlanQty", after);
        record.put("coverageBefore", coverageBefore);
        record.put("coverageAfter", coverageAfter);
        Object existing = material.getEvidence().get("remainingCapacityFill");
        List<Map<String, Object>> records = existing instanceof List
                ? (List<Map<String, Object>>) existing : new ArrayList<>();
        records.add(record);
        material.getEvidence().put("remainingCapacityFill", records);
        return true;
    }

    /**
     * 上修承接轮次：正常承接/让位、按剩余缺口补齐、忽略产能上限强排。
     */
    private enum IncreaseMode {
        /**
         * 正常轮：直接足额承接，不足时让位后部分承接。
         */
        NORMAL,
        /**
         * 补齐轮：只按当前真实余量补齐剩余缺口，不再触发让位。
         */
        TOP_UP,
        /**
         * 强排轮：忽略机台产能上限，按需求足额补足。
         */
        FORCED
    }

    /**
     * 原机优先或按评分选择唯一机台；失败试算不合并任何扣减。
     *
     * @param input    主账本
     * @param material 紧急物料
     * @param mode     承接轮次
     */
    private void increase(RollingPlanningInput input, RollingMaterialSnapshot material, IncreaseMode mode) {
        if (input.getCandidateRefresh() != null) {
            input.getCandidateRefresh().accept(material, input);
        }
        List<RollingCandidateSnapshot> candidates = material.getCandidates().stream()
                .filter(candidate -> material.getBoundMachine() == null
                        || material.getBoundMachine().equals(candidate.getMachineCode()))
                .sorted(Comparator.comparing(RollingCandidateSnapshot::getScore).reversed()
                        .thenComparing(RollingCandidateSnapshot::getMachineCode)).collect(Collectors.toList());
        if (material.getTemplate() == null) {
            material.setReason("SKIPPED_DUE_TO_NO_BOM_OR_CONSTRUCTION");
            return;
        }
        boolean validCandidate = false;
        boolean topUp = mode == IncreaseMode.TOP_UP;
        boolean force = mode == IncreaseMode.FORCED;
        // 正常轮：阶段0直接足额承接、阶段1让位和部分承接；
        // 补齐轮与强排轮各只执行一个收尾阶段，不再重复直接承接和让位。
        int firstPhase = mode == IncreaseMode.NORMAL ? 0 : 2;
        int lastPhase = mode == IncreaseMode.NORMAL ? 1 : 2;
        for (int phase = firstPhase; phase <= lastPhase; phase++) {
            boolean allowYield = phase == 1;
            for (RollingCandidateSnapshot candidate : candidates) {
                RollingMachineSnapshot machine = input.getMachines().get(candidate.getMachineCode());
                // 已超容冻结的机台在直接承接阶段必然无余量，留到让位阶段先在本机腾出余量后再试算；
                // 强排阶段忽略产能限制、补齐阶段按实时余量试算，两者都允许超容机台参与。
                if (candidate.getReason() != null || machine == null || machine.getReason() != null
                        || (machine.isAllocationFrozen() && !allowYield && !force && !topUp)) {
                    continue;
                }
                validCandidate = true;
                BigDecimal target = material.getFormulaQty().multiply(BigDecimal.ONE.add(
                        candidate.getLossRate().divide(HUNDRED, 8, RoundingMode.HALF_UP)));
                RollingTaskSnapshot template = material.getTemplate();
                if (material.isWholeRollExempt()) {
                    target = RollingDemandOnlyQuantityPolicy.target(target, material.getMinimumBatch());
                } else if (!template.isTailTask()) {
                    if (!positive(template.getCurlLength())) {
                        material.setReason("CURL_LENGTH_UNAVAILABLE");
                        return;
                    }
                    if (material.getFinishQty().signum() > 0
                            || !integral(material.getInitialPlanQty(), template.getCurlLength())) {
                        target = material.getInitialPlanQty().add(ceil(target.subtract(material.getInitialPlanQty())
                                .max(BigDecimal.ZERO), template.getCurlLength()));
                    } else {
                        target = ceil(target, template.getCurlLength());
                    }
                }
                // 目标量不低于起排量：否则正常轮因产能不足只承接部分时，
                // 补齐轮会按“实际量 >= 目标量”误判为已达成，起排量永远补不上。
                material.setTargetQty(target.max(material.getInitialPlanQty())
                        .max(this.startFloorQty(material, template)));
                if (this.trial(input, material, candidate, machine, allowYield, topUp, force)) {
                    material.setReason(null);
                    return;
                }
            }
        }
        // 只有本轮完全没有承接的规格才回写未承接原因；已部分承接的规格保留原因为空，缺口由补齐/强排轮和缺口字段体现
        if (material.getReason() == null && this.actualQty(input, material.getMaterialCode())
                .compareTo(material.getInitialPlanQty()) <= 0) {
            boolean allData = !candidates.isEmpty() && candidates.stream().allMatch(candidate ->
                    "CAPACITY_CONFIG_MISSING".equals(candidate.getReason()));
            material.setReason(allData ? "MACHINE_DATA_MISSING"
                    : validCandidate ? "BUSINESS_NO_CAPACITY" : "NO_ELIGIBLE_MACHINE");
        }
    }

    /**
     * 实时汇总物料在各机台上的计划量。
     *
     * @param input        主账本
     * @param materialCode 物料编码
     * @return 实时计划量合计
     */
    private BigDecimal actualQty(RollingPlanningInput input, String materialCode) {
        return this.rows(input, materialCode).stream().map(RollingTaskSnapshot::getPlanQty)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * 在独立机台任务副本中试算，承接有效后一次合并。
     *
     * @param input      主账本
     * @param material   上修物料
     * @param candidate  已评分候选
     * @param machine    当前机台
     * @param allowYield 是否允许让位及部分承接
     * @param topUp      是否补齐轮：只补剩余缺口，不触发让位
     * @param force      是否强排：忽略机台产能上限，按需求直接补足
     * @return 是否接受该候选
     */
    private boolean trial(RollingPlanningInput input, RollingMaterialSnapshot material,
                          RollingCandidateSnapshot candidate, RollingMachineSnapshot machine, boolean allowYield,
                          boolean topUp, boolean force) {
        List<RollingTaskSnapshot> sandbox = machine.getTasks().stream()
                .map(RollingTaskSnapshot::copy).collect(Collectors.toList());
        RollingTaskSnapshot targetRow = sandbox.stream()
                .filter(task -> material.getMaterialCode().equals(task.getMaterialCode()) && task.isEditable()
                        && !this.completed(task)).findFirst().orElse(null);
        boolean inserted = false;
        if (targetRow == null) {
            if (sandbox.stream().anyMatch(task -> material.getMaterialCode().equals(task.getMaterialCode())
                    && (task.getInitialPlanQty().signum() > 0 || task.getFinishQty().signum() > 0))) {
                material.setReason("WORK_ORDER_COMPLETED");
                return false;
            }
            // 已有空班行不可修改时，不能另建一行绕过发布门禁。
            if (sandbox.stream().anyMatch(task -> material.getMaterialCode().equals(task.getMaterialCode())
                    && !task.isEditable())) {
                material.setReason("RELEASE_STATUS_BLOCKED");
                return false;
            }
            targetRow = material.getTemplate().copy();
            targetRow.setKey("NEW:" + material.getMaterialCode());
            targetRow.setResultId(null);
            targetRow.setInitialPlanQty(BigDecimal.ZERO);
            targetRow.setPlanQty(BigDecimal.ZERO);
            targetRow.setFinishQty(BigDecimal.ZERO);
            targetRow.setMachineCode(machine.getMachineCode());
            targetRow.setSequence(Integer.MAX_VALUE);
            sandbox.add(targetRow);
            inserted = true;
        }
        targetRow.setSpeed(candidate.getSpeed());
        targetRow.setSpeedSource(candidate.getSpeedSource());
        BigDecimal need = material.getTargetQty().subtract(material.getInitialPlanQty()).max(BigDecimal.ZERO);
        if (need.signum() <= 0) {
            return false;
        }
        BigDecimal originalTarget = targetRow.getPlanQty();
        // 非收尾规格按最低起排量整批承接：行总量需先补齐到起排量，避免排出几十米的残行
        BigDecimal startFloor = this.startFloorQty(material, targetRow);
        BigDecimal floorAdd = startFloor.subtract(originalTarget).max(BigDecimal.ZERO);
        // 补齐轮与强排轮按“目标量减本机台实时行量”的剩余缺口补量，避免把全量新增量重复叠加一次
        BigDecimal currentQty = sandbox.stream()
                .filter(task -> material.getMaterialCode().equals(task.getMaterialCode()))
                .map(RollingTaskSnapshot::getPlanQty).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal requirement = (topUp || force)
                ? material.getTargetQty().subtract(currentQty).max(BigDecimal.ZERO).max(floorAdd)
                : need.max(floorAdd);
        if (requirement.signum() <= 0) {
            return false;
        }
        // 将目标行暂设为有量，用完整新链求切换差；容量占用仍使用加量前基数。
        targetRow.setPlanQty(originalTarget.add(requirement));
        BigDecimal newSwitch = this.switchLoss(input, machine, this.order(input, sandbox));
        targetRow.setPlanQty(originalTarget);
        BigDecimal occupied = sandbox.stream().map(RollingTaskSnapshot::getPlanQty)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal capacityLimit = machine.getCapacityQty();
        BigDecimal realAvailable = capacityLimit.subtract(occupied).subtract(newSwitch);
        if (force && !material.isWholeRollExempt()) {
            // 强排兜底忽略机台产能上限：产能上限只是预估值，按需求直接补足，超容部分由落库后冻结机台承接
            realAvailable = requirement;
        }
        Map<String, BigDecimal> yields = new LinkedHashMap<>();
        List<Map<String, Object>> donorTrials = new ArrayList<>();
        // 记录让位范围诊断信息，用于区分“未找到让位方”和“让位方扣量为零”两类原因
        Map<String, Object> yieldScope = new LinkedHashMap<>();
        yieldScope.put("allowYield", allowYield);
        yieldScope.put("topUp", topUp);
        yieldScope.put("forced", force);
        yieldScope.put("capacityLimitQty", capacityLimit);
        yieldScope.put("capacityIgnored", force && !material.isWholeRollExempt());
        yieldScope.put("needQty", need);
        yieldScope.put("minimumStartFloorQty", startFloor);
        yieldScope.put("minimumStartAddQty", floorAdd);
        yieldScope.put("requiredQty", requirement);
        yieldScope.put("realAvailableQty", realAvailable);
        yieldScope.put("allocationFrozen", machine.isAllocationFrozen());
        yieldScope.put("targetCurlLength", targetRow.getCurlLength());
        yieldScope.put("targetMinimumBatch", material.getMinimumBatch());
        if (allowYield && realAvailable.compareTo(requirement) < 0) {
            List<RollingTaskSnapshot> donors = sandbox.stream().filter(task ->
                            !material.getMaterialCode().equals(task.getMaterialCode()) && this.canYield(input, task, sandbox))
                    .sorted(Comparator.comparing((RollingTaskSnapshot task) ->
                                    input.getMaterials().get(task.getMaterialCode()).getCoverage()).reversed()
                            .thenComparing(RollingTaskSnapshot::getMaterialCode)
                            .thenComparing(RollingTaskSnapshot::getKey)).collect(Collectors.toList());
            yieldScope.put("donorCandidateCount", donors.size());
            for (RollingTaskSnapshot donor : donors) {
                BigDecimal gap = requirement.subtract(realAvailable).max(BigDecimal.ZERO);
                if (gap.signum() <= 0) {
                    break;
                }
                RollingMaterialSnapshot donorMaterial = input.getMaterials().get(donor.getMaterialCode());
                BigDecimal donorTotal = sandbox.stream().filter(task -> donor.getMaterialCode().equals(task.getMaterialCode()))
                        .map(RollingTaskSnapshot::getPlanQty).reduce(BigDecimal.ZERO, BigDecimal::add);
                BigDecimal safety = donorMaterial.getStockQty().add(donorTotal)
                        .subtract(donorMaterial.getUpDemandQty()).max(BigDecimal.ZERO);
                // 上修物料让位后不得低于自身目标，避免把缺口转移给原本足量的规格
                if ("UP".equals(donorMaterial.getDirection())) {
                    safety = safety.min(donorTotal.subtract(donorMaterial.getTargetQty()).max(BigDecimal.ZERO));
                }
                BigDecimal maximum = donor.getPlanQty().subtract(donor.getFinishQty()).min(safety).max(BigDecimal.ZERO);
                BigDecimal reduce = donorMaterial.isWholeRollExempt()
                        ? RollingDemandOnlyQuantityPolicy.yieldQty(donor.getPlanQty(), maximum, gap,
                        donorMaterial.getMinimumBatch())
                        : yieldQty(donor.getPlanQty(), maximum, gap, donor.getCurlLength(), donorMaterial.getMinimumBatch());
                Map<String, Object> donorTrial = new LinkedHashMap<>();
                donorTrial.put("materialCode", donor.getMaterialCode());
                donorTrial.put("donorDirection", donorMaterial.getDirection());
                donorTrial.put("donorCoverage", coverage(donorMaterial.getStockQty().add(donorTotal),
                        donorMaterial.getFutureDemand()));
                donorTrial.put("requiredQty", gap);
                donorTrial.put("maxReducibleQty", maximum);
                donorTrial.put("donorCurlLength", donor.getCurlLength());
                donorTrial.put("donorMinimumBatch", donorMaterial.getMinimumBatch());
                donorTrial.put("candidateReduceQty", reduce);
                // 让位量覆盖整行时标记为整行清零让位，用于区分整卷扣减与整行让出
                donorTrial.put("wholeRowReleased", reduce.signum() > 0
                        && reduce.compareTo(donor.getPlanQty()) >= 0);
                if (reduce.signum() > 0) {
                    BigDecimal before = donor.getPlanQty();
                    BigDecimal calculatedAfter = before.subtract(reduce);
                    BigDecimal after = this.ceilIntegerQty(calculatedAfter);
                    BigDecimal actualReleased = before.subtract(after).max(BigDecimal.ZERO);
                    donorTrial.put("actualReleasedQty", actualReleased);
                    donorTrial.put("roundedPlanQty", after);
                    donorTrial.put("roundingAddedQty", after.subtract(calculatedAfter).max(BigDecimal.ZERO));
                    donorTrial.put("roundingRule", "INTEGER_CEILING");
                    if (actualReleased.signum() > 0) {
                        after = this.ceilPlanRowQty(donor, calculatedAfter);
                        donor.setPlanQty(after);
                        yields.merge(donor.getMaterialCode(), actualReleased, BigDecimal::add);
                        realAvailable = realAvailable.add(actualReleased);
                        donorTrial.put("adoptedInSandbox", true);
                    } else {
                        donorTrial.put("adoptedInSandbox", false);
                    }
                } else {
                    donorTrial.put("actualReleasedQty", BigDecimal.ZERO);
                    donorTrial.put("adoptedInSandbox", false);
                }
                donorTrials.add(donorTrial);
            }
        } else {
            yieldScope.put("donorCandidateCount", 0);
        }
        yieldScope.put("realAvailableAfterYieldQty", realAvailable);
        BigDecimal raw = realAvailable.min(requirement);
        BigDecimal minimum;
        if (material.isWholeRollExempt()) {
            minimum = floorAdd.signum() > 0 ? floorAdd : BigDecimal.ZERO;
            // 先把原行量加可承接量收紧到整数米，防止后续CEILING把40.2米补成41米透支产能。
            // 按整行总量取整，兼容已有小数历史行；原行达到最低量后不对每次增量再设1米门槛。
            BigDecimal integerRowLimit = originalTarget.add(raw.max(BigDecimal.ZERO))
                    .setScale(0, RoundingMode.FLOOR);
            BigDecimal integerAdditionalQty = integerRowLimit.subtract(originalTarget).max(BigDecimal.ZERO);
            yieldScope.put("capacityLimitedBeforeIntegerQty", raw);
            yieldScope.put("integerRowLimitQty", integerRowLimit);
            // 正剩余产能不足最低量仍只补到最低行总量，不扩大为普通的整数向上超用。
            raw = raw.signum() > 0 && floorAdd.signum() > 0
                    ? integerAdditionalQty.max(floorAdd) : integerAdditionalQty;
        } else if (targetRow.isTailTask()) {
            minimum = BigDecimal.ZERO;
        } else if (topUp || (force && originalTarget.signum() > 0)) {
            // 补齐轮和“已有计划行但本轮完全未承接”的强排轮只处理剩余缺口，缺口常因整数向上取整而小于一卷，
            // 不能再按整卷卡门槛；门槛降到1米，最终由整数向上取整落到整米
            minimum = BigDecimal.ONE;
        } else if (floorAdd.signum() > 0) {
            // 非收尾规格必须先补齐到最低起排量，不足该量不做几十米的残行承接
            minimum = targetRow.getCurlLength().max(floorAdd.min(requirement));
        } else {
            minimum = targetRow.getCurlLength();
            if (inserted) {
                // 普通新建行可足额承接小目标，不为满足最低批量额外增产；部分承接仍须先过真实数量门槛。
                minimum = material.getMinimumBatch().min(need).max(targetRow.getCurlLength());
            }
        }
        boolean adopted = raw.signum() > 0 && raw.compareTo(minimum) >= 0
                && (material.isWholeRollExempt() || allowYield || topUp || raw.compareTo(requirement) >= 0);
        if (!adopted) {
            this.recordTrial(material, machine, allowYield, topUp, force, raw, minimum, false, donorTrials, yieldScope);
            return false;
        }
        this.recordTrial(material, machine, allowYield, topUp, force, raw, minimum, true, donorTrials, yieldScope);
        BigDecimal accepted;
        if (material.isWholeRollExempt()) {
            accepted = raw.min(requirement);
        } else if (topUp && !targetRow.isTailTask()) {
            // 补齐轮按剩余缺口补量：缺口不足一卷时直接补足剩余量（不再按整卷向上补，避免超过目标量）；
            // 缺口超过一卷时仍按整卷向上承接，且不超过目标量
            accepted = requirement.compareTo(targetRow.getCurlLength()) < 0
                    ? raw
                    : ceil(raw, targetRow.getCurlLength()).min(requirement);
        } else {
            accepted = targetRow.isTailTask() ? raw
                    : ceil(raw, targetRow.getCurlLength()).max(floorAdd).min(requirement);
        }
        BigDecimal calculatedPlanQty = originalTarget.add(accepted);
        BigDecimal finalPlanQty = this.ceilPlanRowQty(targetRow, calculatedPlanQty);
        targetRow.setPlanQty(finalPlanQty);
        machine.setTasks(sandbox);
        BigDecimal balance = machine.getCapacityQty().subtract(sandbox.stream()
                        .map(RollingTaskSnapshot::getPlanQty).reduce(BigDecimal.ZERO, BigDecimal::add))
                .subtract(this.switchLoss(input, machine, this.order(input, sandbox)));
        machine.setAllocationFrozen(balance.signum() < 0);
        material.setBoundMachine(machine.getMachineCode());
        material.getEvidence().put("roundingExtraQty", accepted.subtract(raw).max(BigDecimal.ZERO));
        material.getEvidence().put("integerCeilingIncreaseQty", targetRow.getRoundingAddedQty());
        material.getEvidence().put("roundingRule", "INTEGER_CEILING");
        material.getEvidence().put("roundingOverrunQty", balance.negate().max(BigDecimal.ZERO));
        // 强排/补齐标记：便于落库日志区分“正常承接”“补齐缺口”和“忽略预估产能承接”
        material.getEvidence().put("forcedSchedule", force);
        material.getEvidence().put("topUpSchedule", topUp);
        material.getEvidence().put("capacityIgnored", force && !material.isWholeRollExempt());
        material.getEvidence().put("capacityBalance", balance);
        material.getEvidence().put("maintenanceHours", machine.getMaintenanceHours());
        material.getEvidence().put("maintenanceDeduct", machine.getMaintenanceDeduct());
        material.getEvidence().put("selectedMachine", machine.getMachineCode());
        material.getEvidence().put("acceptedYieldByMaterial", new LinkedHashMap<>(yields));
        yields.forEach((code, quantity) -> input.getMaterials().get(code).setYieldedQty(
                input.getMaterials().get(code).getYieldedQty().add(quantity)));
        return true;
    }

    /**
     * 保存精简试算证据，未采用候选的扣量不进入实际让位汇总。
     *
     * @param material    上修物料
     * @param machine     候选机台
     * @param allowYield  是否允许让位
     * @param topUp       是否补齐轮
     * @param force       是否强排
     * @param raw         取整前真实承接米数
     * @param minimum     起排门槛
     * @param adopted     是否采用
     * @param donorTrials 候选内试算，不表示最终提交
     * @param yieldScope  让位范围诊断
     */
    @SuppressWarnings("unchecked")
    private void recordTrial(RollingMaterialSnapshot material, RollingMachineSnapshot machine, boolean allowYield,
                             boolean topUp, boolean force, BigDecimal raw, BigDecimal minimum, boolean adopted,
                             List<Map<String, Object>> donorTrials, Map<String, Object> yieldScope) {
        Map<String, Object> trial = new LinkedHashMap<>();
        trial.put("machineCode", machine.getMachineCode());
        trial.put("phase", force ? "FORCED_OVER_CAPACITY"
                : topUp ? "TOP_UP" : allowYield ? "YIELD_OR_PARTIAL" : "DIRECT_FULL");
        trial.put("rawAcceptedQty", raw);
        trial.put("acceptedMinimum", minimum);
        trial.put("adopted", adopted);
        trial.put("yieldScope", yieldScope);
        trial.put("donorTrials", donorTrials);
        List<Map<String, Object>> trials = (List<Map<String, Object>>) material.getEvidence()
                .computeIfAbsent("candidateTrials", ignored -> new ArrayList<Map<String, Object>>());
        trials.add(trial);
    }

    /**
     * 校验让位准入；按实时可供应班数判断，只有已覆盖上修阈值窗口且仍有余量的规格才允许让出产能。
     *
     * @param input   当前上下文
     * @param task    常规候选任务
     * @param sandbox 当前机台试算任务副本
     * @return 是否允许进入让位池
     */
    private boolean canYield(RollingPlanningInput input, RollingTaskSnapshot task, List<RollingTaskSnapshot> sandbox) {
        RollingMaterialSnapshot material = input.getMaterials().get(task.getMaterialCode());
        // 历史非整卷、卷长缺失等只表示本轮不能下修，不影响按整卷让出多余产能；数据异常原因已在decide中置为不可编辑
        if (material == null || !material.isDemandComplete() || !task.isEditable()
                || task.getFinishQty().signum() != 0 || (task.isTailTask() && !material.isWholeRollExempt())) {
            return false;
        }
        // 不再限制非上修方向；上修物料在本轮补量后若实时可供应班数仍超过上修阈值班数，也允许让出多余产能
        BigDecimal donorTotal = sandbox.stream()
                .filter(row -> Objects.equals(material.getMaterialCode(), row.getMaterialCode()))
                .map(RollingTaskSnapshot::getPlanQty).reduce(BigDecimal.ZERO, BigDecimal::add);
        return coverage(material.getStockQty().add(donorTotal), material.getFutureDemand())
                .compareTo(input.getUpThreshold()) > 0;
    }

    /**
     * 计算非收尾规格本轮的最低起排数量；收尾规格或无本轮需求时不设起排下限。
     *
     * @param material  目标物料
     * @param targetRow 目标班计划行
     * @return 行总量下限，未启用时为0
     */
    private BigDecimal startFloorQty(RollingMaterialSnapshot material, RollingTaskSnapshot targetRow) {
        if (material.isWholeRollExempt()) {
            return this.hasDemand(material) ? material.getMinimumBatch() : BigDecimal.ZERO;
        }
        if (!this.hasDemand(material)) {
            return BigDecimal.ZERO;
        }
        BigDecimal minimumBatch = material.getMinimumBatch() == null ? BigDecimal.ZERO : material.getMinimumBatch();
        BigDecimal floorQty = minimumBatch.max(targetRow.getCurlLength());
        // 收尾行默认只排需求余量、不按起排量整批承接；但滚动窗口内需求合计仍超过起排量时，
        // 说明该规格后续还要持续生产，按尾料口径只排零头会留下无法开机的残量，此时仍按起排量承接。
        if (targetRow.isTailTask() && !this.demandExceedsFloor(material, floorQty)) {
            return BigDecimal.ZERO;
        }
        return floorQty;
    }

    /**
     * 判断滚动窗口内的需求合计是否超过起排量。
     *
     * <p>收尾标记按胎胚月计划余量判定（余量不足八班成型计划量即置为收尾），与逐班需求是两个口径；
     * 存在月计划余量偏少、但成型仍在持续要货的规格，这类规格在滚动里若按尾料处理，
     * 只会排出一个远低于起排量的零头，导致后续无法开机。</p>
     *
     * @param material 物料快照
     * @param floorQty 起排量下限（最低起排量与卷长的较大值）
     * @return 窗口需求合计超过起排量时返回true
     */
    private boolean demandExceedsFloor(RollingMaterialSnapshot material, BigDecimal floorQty) {
        if (floorQty.signum() <= 0 || material.getFutureDemand() == null) {
            return false;
        }
        BigDecimal windowDemandQty = material.getFutureDemand().stream()
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return windowDemandQty.compareTo(floorQty) > 0;
    }

    /**
     * 判断本轮是否存在真实需求；无需求的库存回正规格不按最低起排量增排。
     *
     * @param material 目标物料
     * @return 上修阈值需求或备库需求大于零时为true
     */
    private boolean hasDemand(RollingMaterialSnapshot material) {
        if (material.getUpDemandQty() != null && material.getUpDemandQty().signum() > 0) {
            return true;
        }
        return material.isStockUpDemandComplete() && material.getStockUpDemandQty() != null
                && material.getStockUpDemandQty().signum() > 0;
    }

    /**
     * 保存规划前机台任务的数量、顺序和预计时间，供结果解释记录变更前后值。
     *
     * @param input 本轮自动滚动输入
     * @return 按机台及任务键索引的规划前状态
     */
    private Map<String, Map<String, Object>> captureTaskStates(RollingPlanningInput input) {
        Map<String, Map<String, Object>> states = new LinkedHashMap<>();
        input.getMachines().values().forEach(machine -> machine.getTasks().forEach(task -> {
            Map<String, Object> state = new LinkedHashMap<>();
            state.put("resultId", task.getResultId());
            state.put("materialCode", task.getMaterialCode());
            state.put("machineCode", task.getMachineCode());
            state.put("planQty", task.getPlanQty());
            state.put("sequence", task.getSequence());
            state.put("startTime", task.getStartTime());
            state.put("endTime", task.getEndTime());
            states.put(this.taskStateKey(machine.getMachineCode(), task), state);
        }));
        return states;
    }

    /**
     * 汇总各任务发生变化的原值和新值，写入对应物料的结构化解释。
     *
     * @param input             本轮自动滚动输入
     * @param initialTaskStates 规划前任务状态
     */
    @SuppressWarnings("unchecked")
    private void recordTaskChanges(RollingPlanningInput input, Map<String, Map<String, Object>> initialTaskStates) {
        input.getMachines().values().forEach(machine -> machine.getTasks().forEach(task -> {
            Map<String, Object> initial = initialTaskStates.get(this.taskStateKey(machine.getMachineCode(), task));
            if (initial == null) {
                initial = new LinkedHashMap<>();
                initial.put("resultId", null);
                initial.put("materialCode", task.getMaterialCode());
                initial.put("machineCode", machine.getMachineCode());
                initial.put("planQty", task.getInitialPlanQty());
                initial.put("sequence", null);
                initial.put("startTime", null);
                initial.put("endTime", null);
            }
            if (!this.taskStateChanged(initial, task)) {
                return;
            }
            RollingMaterialSnapshot material = input.getMaterials().get(task.getMaterialCode());
            if (material == null) {
                return;
            }
            List<Map<String, Object>> changes = (List<Map<String, Object>>) material.getEvidence()
                    .computeIfAbsent("resultChanges", ignored -> new ArrayList<Map<String, Object>>());
            Map<String, Object> change = new LinkedHashMap<>();
            change.put("resultId", task.getResultId());
            change.put("taskKey", task.getKey());
            change.put("materialCode", task.getMaterialCode());
            change.put("machineCode", task.getMachineCode());
            change.put("before", initial);
            change.put("after", this.taskState(task));
            change.put("formulaPlanQty", material.getFormulaQty());
            if (task.getRoundingRule() != null) {
                change.put("roundingBeforeQty", task.getRoundingBeforeQty());
                change.put("roundedPlanQty", task.getPlanQty());
                change.put("roundingAddedQty", task.getRoundingAddedQty());
                change.put("roundingRule", task.getRoundingRule());
            }
            changes.add(change);
        }));
    }

    /**
     * 判断数量、顺序或时间是否与规划前不同。
     *
     * @param initial 规划前任务状态
     * @param task    规划后任务
     * @return 任一持久化字段改变时返回 true
     */
    private boolean taskStateChanged(Map<String, Object> initial, RollingTaskSnapshot task) {
        BigDecimal beforeQty = (BigDecimal) initial.get("planQty");
        boolean quantityChanged = beforeQty == null ? task.getPlanQty() != null
                : task.getPlanQty() == null || beforeQty.compareTo(task.getPlanQty()) != 0;
        return quantityChanged || !Objects.equals(initial.get("sequence"), task.getSequence())
                || !Objects.equals(initial.get("startTime"), task.getStartTime())
                || !Objects.equals(initial.get("endTime"), task.getEndTime());
    }

    /**
     * 构造规划后任务持久化字段快照。
     *
     * @param task 规划后任务
     * @return 数量、顺序及预计时间快照
     */
    private Map<String, Object> taskState(RollingTaskSnapshot task) {
        Map<String, Object> state = new LinkedHashMap<>();
        state.put("resultId", task.getResultId());
        state.put("materialCode", task.getMaterialCode());
        state.put("machineCode", task.getMachineCode());
        state.put("planQty", task.getPlanQty());
        state.put("sequence", task.getSequence());
        state.put("startTime", task.getStartTime());
        state.put("endTime", task.getEndTime());
        return state;
    }

    /**
     * 生成规划内稳定的任务状态索引。
     *
     * @param machineCode 机台编码
     * @param task        机台任务
     * @return 包含机台和任务键的唯一索引
     */
    private String taskStateKey(String machineCode, RollingTaskSnapshot task) {
        return String.valueOf(machineCode) + "::" + String.valueOf(task.getKey());
    }

    /**
     * 冻结前缀关系，紧急任务移动到最后冻结位置后。
     *
     * @param input 排序快照
     * @param tasks 机台链副本
     * @return 新任务顺序，不修改原集合
     */
    private List<RollingTaskSnapshot> order(RollingPlanningInput input, List<RollingTaskSnapshot> tasks) {
        List<RollingTaskSnapshot> ordered = tasks.stream().filter(task -> task.getPlanQty().signum() > 0)
                .sorted(Comparator.comparing(RollingTaskSnapshot::getSequence, Comparator.nullsLast(Integer::compareTo))
                        .thenComparing(RollingTaskSnapshot::getKey)).collect(Collectors.toList());
        if (input.isShortageFirstEnabled()) {
            return this.orderBySegment(input, ordered);
        }
        int frozenEnd = -1;
        for (int index = 0; index < ordered.size(); index++) {
            if (ordered.get(index).getFinishQty().signum() > 0 || !ordered.get(index).isEditable()) {
                frozenEnd = index;
            }
        }
        List<RollingTaskSnapshot> result = new ArrayList<>(ordered.subList(0, frozenEnd + 1));
        List<RollingTaskSnapshot> suffix = ordered.subList(frozenEnd + 1, ordered.size());
        Comparator<RollingTaskSnapshot> urgency = Comparator.comparing((RollingTaskSnapshot task) ->
                        input.getMaterials().get(task.getMaterialCode()).getCoverage()).thenComparing(RollingTaskSnapshot::getMaterialCode)
                .thenComparing(RollingTaskSnapshot::getKey);
        result.addAll(suffix.stream().filter(task -> this.urgent(input, task)).sorted(urgency).collect(Collectors.toList()));
        result.addAll(suffix.stream().filter(task -> !this.urgent(input, task)).collect(Collectors.toList()));
        return result;
    }

    /**
     * 按冻结行分段重排：冻结行（已生产或不可编辑）保持原位置，其余可编辑行在段内把缺料规格提前。
     *
     * <p>胎面缺料规格提前启用时使用：本班第一个任务为续作规格（上一班顺延或已开工），固定排最前；
     * 段内按「缺料规格（六点库存覆盖班数升序）→ 其余规格原顺序」排列，冻结行顺序号不变，
     * 避免改动已生产或已发布的行；新增行的空顺序号由顺序号回填阶段统一分配，不影响段内重排。</p>
     *
     * @param input   排序快照
     * @param ordered 按原顺序排列的正计划量任务
     * @return 分段重排后的任务顺序
     */
    private List<RollingTaskSnapshot> orderBySegment(RollingPlanningInput input, List<RollingTaskSnapshot> ordered) {
        List<RollingTaskSnapshot> result = new ArrayList<>(ordered.size());
        List<RollingTaskSnapshot> segment = new ArrayList<>();
        int startIndex = 0;
        // 本班第一个任务视为续作规格，保持最前，缺料规格只能排在它之后
        if (!ordered.isEmpty() && !this.frozen(ordered.get(0))) {
            result.add(ordered.get(0));
            startIndex = 1;
        }
        for (int index = startIndex; index < ordered.size(); index++) {
            RollingTaskSnapshot task = ordered.get(index);
            if (this.frozen(task)) {
                this.appendOrderedSegment(input, result, segment);
                result.add(task);
            } else {
                segment.add(task);
            }
        }
        this.appendOrderedSegment(input, result, segment);
        return result;
    }

    /**
     * 把可编辑段按「缺料规格优先」顺序追加到结果，并清空段缓存。
     *
     * <p>新增行的顺序号为 null 或 {@link Integer#MAX_VALUE} 时不影响段内重排：
     * 顺序号在 {@code assignSegmentSlots} 中统一回填，空值和占位值改用未被占用的最小正整数。</p>
     *
     * @param input   排序快照
     * @param result  重排结果
     * @param segment 当前可编辑段
     */
    private void appendOrderedSegment(RollingPlanningInput input, List<RollingTaskSnapshot> result,
                                      List<RollingTaskSnapshot> segment) {
        if (segment.isEmpty()) {
            return;
        }
        // 缺料严重程度按六点库存覆盖班数升序：六点库存连本班需求都不够的规格排最前
        Comparator<RollingTaskSnapshot> urgency = Comparator.comparing((RollingTaskSnapshot task) ->
                        BigDecimalUtils.valueOf(this.orderCoverageOf(input, task.getMaterialCode())))
                .thenComparing(RollingTaskSnapshot::getMaterialCode)
                .thenComparing(RollingTaskSnapshot::getKey);
        result.addAll(segment.stream().filter(task -> this.urgent(input, task)).sorted(urgency)
                .collect(Collectors.toList()));
        result.addAll(segment.stream().filter(task -> !this.urgent(input, task)).collect(Collectors.toList()));
        segment.clear();
    }

    /**
     * 判断任务是否为冻结行：已生产（完成量大于0）或不可编辑。
     *
     * @param task 任务
     * @return true 表示该行不能移动
     */
    private boolean frozen(RollingTaskSnapshot task) {
        return task.getFinishQty().signum() > 0 || !task.isEditable();
    }

    /**
     * 输出机台班次重排明细：原顺序、重排后顺序以及每行的缺料方向和紧急判定。
     *
     * <p>用于定位「缺料规格没有提前」：若某行 dir=UP 但 urgent=false，说明紧急判定没命中；
     * 若 urgent=true 但新顺序未变，说明分段或顺序号回填有问题；若 frozen=true，说明该行被锁定。</p>
     *
     * @param input   排序快照
     * @param machine 机台快照
     * @param ordered 重排后的任务顺序
     */
    private void logOrderDetail(RollingPlanningInput input, RollingMachineSnapshot machine,
                                List<RollingTaskSnapshot> ordered) {
        String before = machine.getTasks().stream()
                .filter(task -> task.getPlanQty().signum() > 0)
                .sorted(Comparator.comparing(RollingTaskSnapshot::getSequence,
                                Comparator.nullsLast(Integer::compareTo))
                        .thenComparing(RollingTaskSnapshot::getKey))
                .map(task -> task.getMaterialCode() + "#" + task.getSequence())
                .collect(Collectors.joining(","));
        String after = ordered.stream()
                .map(task -> task.getMaterialCode() + "#" + task.getSequence()
                        + "(dir=" + this.directionOf(input, task.getMaterialCode())
                        + ",urgent=" + this.urgent(input, task)
                        + ",frozen=" + this.frozen(task)
                        + ",cov=" + this.orderCoverageOf(input, task.getMaterialCode()) + ")")
                .collect(Collectors.joining(","));
        LOG.info("[TM_ROLLING_ORDER] 机台={} 重排明细，开关={}，原顺序={}，新顺序={}", machine.getMachineCode(),
                input.isShortageFirstEnabled(), before, after);
    }

    /**
     * 读取物料上修方向，供重排明细日志标注。
     *
     * @param input 排序快照
     * @param code  物料编码
     * @return 方向文本，物料缺失返回 MISSING
     */
    private String directionOf(RollingPlanningInput input, String code) {
        RollingMaterialSnapshot material = input.getMaterials().get(code);
        return material == null ? "MISSING" : String.valueOf(material.getDirection());
    }

    /**
     * 读取物料顺位排序用覆盖班数，供重排明细日志标注。
     *
     * @param input 排序快照
     * @param code  物料编码
     * @return 排序用覆盖班数，物料缺失返回 null
     */
    private BigDecimal orderCoverageOf(RollingPlanningInput input, String code) {
        RollingMaterialSnapshot material = input.getMaterials().get(code);
        return material == null ? null : material.getOrderCoverage();
    }

    /**
     * 判断机台班次重排后的顺序是否与当前顺序不同。
     *
     * <p>缺料规格提前后即使计划量没有变化，也需要重新推算顺位和预计时间；
     * 计划量全部未变化时用本方法判断该机台是否仍需重排。</p>
     *
     * @param input   排序快照
     * @param machine 机台快照
     * @return true 表示重排后顺序发生变化
     */
    private boolean orderingChanged(RollingPlanningInput input, RollingMachineSnapshot machine) {
        List<RollingTaskSnapshot> ordered = this.order(input, machine.getTasks());
        List<RollingTaskSnapshot> current = machine.getTasks().stream()
                .filter(task -> task.getPlanQty().signum() > 0)
                .sorted(Comparator.comparing(RollingTaskSnapshot::getSequence,
                                Comparator.nullsLast(Integer::compareTo))
                        .thenComparing(RollingTaskSnapshot::getKey))
                .collect(Collectors.toList());
        if (ordered.size() != current.size()) {
            return true;
        }
        for (int index = 0; index < ordered.size(); index++) {
            if (!Objects.equals(ordered.get(index).getKey(), current.get(index).getKey())) {
                return true;
            }
        }
        return false;
    }

    /**
     * 判断待生产任务是否属于本轮上修紧急区。
     *
     * <p>缺料规格顺位提前启用时（胎面定时滚动），上修方向的规格整规格参与提前，不要求本行计划量增加，
     * 被产能或资料阻断的缺料规格也提前——排序只影响顺位和预计时间，不改变计划量。
     * 未启用时保持原行级口径：仅无阻断原因且本行计划量增加的任务参与提前。</p>
     */
    private boolean urgent(RollingPlanningInput input, RollingTaskSnapshot task) {
        RollingMaterialSnapshot material = input.getMaterials().get(task.getMaterialCode());
        if (material == null || !"UP".equals(material.getDirection())) {
            return false;
        }
        if (input.isShortageFirstEnabled()) {
            return true;
        }
        return material.getReason() == null && task.getPlanQty().compareTo(task.getInitialPlanQty()) > 0;
    }

    /**
     * 按冻结行分段回填顺序号。
     *
     * <p>冻结行顺序号保持不变，可编辑段内按重排后的先后顺序复用该段原有的顺序号槽位，
     * 既保证段内顺位提前，又不会与冻结行或其它段的顺序号冲突；新增行的占位顺序号
     * {@link Integer#MAX_VALUE} 不参与复用，改用未被占用的最小正整数。</p>
     *
     * @param ordered 分段重排后的任务顺序
     */
    private void assignSegmentSequences(List<RollingTaskSnapshot> ordered) {
        Set<Integer> usedSequences = ordered.stream().map(RollingTaskSnapshot::getSequence)
                .filter(Objects::nonNull)
                .filter(sequence -> sequence < Integer.MAX_VALUE)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        List<RollingTaskSnapshot> segment = new ArrayList<>();
        for (RollingTaskSnapshot task : ordered) {
            if (this.frozen(task)) {
                this.assignSegmentSlots(segment, usedSequences);
                segment.clear();
            } else {
                segment.add(task);
            }
        }
        this.assignSegmentSlots(segment, usedSequences);
    }

    /**
     * 用段内原有顺序号槽位回填段内任务顺序号。
     *
     * @param segment       同一可编辑段内的任务，已按目标顺序排列
     * @param usedSequences 当前机台班次已占用的顺序号，回填过程中同步追加新分配的号码
     */
    private void assignSegmentSlots(List<RollingTaskSnapshot> segment, Set<Integer> usedSequences) {
        if (segment.isEmpty()) {
            return;
        }
        List<Integer> slots = segment.stream().map(RollingTaskSnapshot::getSequence)
                .filter(Objects::nonNull)
                .filter(sequence -> sequence < Integer.MAX_VALUE)
                .collect(Collectors.toCollection(ArrayList::new));
        Collections.sort(slots);
        int candidate = 1;
        while (slots.size() < segment.size()) {
            if (!usedSequences.contains(candidate)) {
                slots.add(candidate);
                usedSequences.add(candidate);
            }
            candidate++;
        }
        Collections.sort(slots);
        for (int index = 0; index < segment.size(); index++) {
            segment.get(index).setSequence(slots.get(index));
        }
    }

    /**
     * 计算完整相邻边独立换胶米数损失，不把切换分钟换算成米。
     */
    private BigDecimal switchLoss(RollingPlanningInput input, RollingMachineSnapshot machine, List<RollingTaskSnapshot> tasks) {
        BigDecimal loss = BigDecimal.ZERO;
        String previous = machine.getPreviousGlue();
        for (RollingTaskSnapshot task : tasks) {
            if (this.changed(previous, task.getGlueCode())) {
                loss = loss.add(input.getGlueLossQty());
            }
            previous = task.getGlueCode();
        }
        return loss;
    }

    /**
     * 数量变更机台重排并线性推算时间，完成量不重复计时。
     */
    private void retime(RollingPlanningInput input, RollingMachineSnapshot machine) {
        List<RollingTaskSnapshot> ordered = this.order(input, machine.getTasks());
        if (input.isShortageFirstEnabled()) {
            this.logOrderDetail(input, machine, ordered);
        }
        int frozenEnd = -1;
        for (int index = 0; index < ordered.size(); index++) {
            RollingTaskSnapshot task = ordered.get(index);
            if (task.getFinishQty().signum() > 0 || !task.isEditable()) {
                frozenEnd = index;
            }
        }
        if (frozenEnd > 0) {
            for (int index = 0; index < frozenEnd; index++) {
                if (ordered.get(index).getFinishQty().signum() == 0 && ordered.get(index).isEditable()) {
                    for (RollingTaskSnapshot task : ordered) {
                        if (task.getPlanQty().compareTo(task.getInitialPlanQty()) != 0) {
                            input.getMaterials().get(task.getMaterialCode()).getEvidence()
                                    .put("lastFrozenBoundary", ordered.get(frozenEnd).getKey());
                            input.getMaterials().get(task.getMaterialCode()).getEvidence()
                                    .put("frozenTasksNotContinuousPrefix", true);
                        }
                    }
                    break;
                }
            }
        }
        long cursor = input.getShiftStart();
        String previousGlue = machine.getPreviousGlue();
        String previousMouth = machine.getPreviousMouth();
        String previousMaterial = machine.getPreviousMaterial();
        int sequence = 0;
        boolean timingBlocked = false;
        if (input.isShortageFirstEnabled()) {
            // 分段重排后按段内原有顺序号槽位回填，冻结行顺序号保持不变，避免改动已生产或已发布的行
            this.assignSegmentSequences(ordered);
        }
        for (int index = 0; index < ordered.size(); index++) {
            RollingTaskSnapshot task = ordered.get(index);
            task.setTimeUnavailableReason(null);
            RollingMaterialSnapshot material = input.getMaterials().get(task.getMaterialCode());
            if (material != null && material.getEvidence().get("timeUnavailableTasks") instanceof List) {
                ((List<?>) material.getEvidence().get("timeUnavailableTasks")).removeIf(row -> row instanceof Map
                        && task.getKey().equals(((Map<?, ?>) row).get("taskKey")));
            }
            boolean completed = this.completed(task);
            boolean inProgress = task.getFinishQty().signum() > 0 && !completed && task.isEditable();
            boolean frozen = completed || !task.isEditable();
            if (!input.isShortageFirstEnabled()) {
                if (index <= frozenEnd) {
                    sequence = Math.max(sequence, task.getSequence() == null ? 0 : task.getSequence());
                } else {
                    task.setSequence(++sequence);
                }
            }
            if (inProgress) {
                Long remainingDuration = this.duration(task.getPlanQty().subtract(task.getFinishQty()), task.getSpeed());
                if (remainingDuration == null) {
                    task.setEndTime(null);
                    this.markTimeUnavailable(input, task, "MACHINE_SPEED_MISSING");
                    timingBlocked = true;
                } else {
                    task.setEndTime(Math.max(input.getProgressTime(), input.getShiftStart()) + remainingDuration);
                    cursor = task.getEndTime();
                    timingBlocked = false;
                }
            } else if (frozen) {
                if (completed && task.getEndTime() != null) {
                    // 已完成任务的结束时刻是事实锚点，可恢复后续待生产任务的预计时间。
                    cursor = Math.max(cursor, task.getEndTime());
                    timingBlocked = false;
                } else {
                    // 未完成的冻结任务可能只有旧预计结束时间，不能把它当作新时间链的可靠锚点。
                    timingBlocked = true;
                }
            } else if (timingBlocked) {
                task.setStartTime(null);
                task.setEndTime(null);
                String predecessorTaskKey = index > 0 ? ordered.get(index - 1).getKey() : null;
                this.markTimeUnavailable(input, task, "PREDECESSOR_TIME_UNKNOWN", predecessorTaskKey);
            } else {
                BigDecimal switchMinutes = this.changed(previousGlue, task.getGlueCode())
                        ? input.getGlueMinutes() : BigDecimal.ZERO;
                if (this.changed(previousMaterial, task.getMaterialCode()) || this.changed(previousMouth, task.getMouthPlateCode())) {
                    switchMinutes = switchMinutes.add(input.getSpecMinutes());
                }
                long switchMillis = switchMinutes.multiply(new BigDecimal("60"))
                        .setScale(0, RoundingMode.CEILING).longValueExact() * 1000L;
                Long taskDuration = this.duration(task.getPlanQty(), task.getSpeed());
                if (taskDuration == null) {
                    task.setStartTime(null);
                    task.setEndTime(null);
                    this.markTimeUnavailable(input, task, "MACHINE_SPEED_MISSING");
                    timingBlocked = true;
                } else {
                    cursor += switchMillis;
                    task.setStartTime(cursor);
                    cursor += taskDuration;
                    task.setEndTime(cursor);
                }
            }
            previousGlue = task.getGlueCode();
            previousMouth = task.getMouthPlateCode();
            previousMaterial = task.getMaterialCode();
        }
        machine.getTasks().stream().filter(task -> task.getPlanQty().signum() == 0 && task.isEditable()
                && task.getInitialPlanQty().signum() > 0).forEach(task -> {
            task.setSequence(null);
            task.setStartTime(null);
            task.setEndTime(null);
        });
    }

    /**
     * 按有效标定速度推算毫秒，调用前已完成机台链资料校验。
     */
    private Long duration(BigDecimal quantity, BigDecimal speed) {
        if (!positive(speed)) {
            return null;
        }
        return quantity.multiply(new BigDecimal("3600")).divide(speed, 0, RoundingMode.CEILING).longValueExact() * 1000L;
    }

    /**
     * 为预计时间无法计算的任务保存非阻断解释。
     *
     * @param input  当前滚动规划输入
     * @param task   目标任务快照
     * @param reason 时间不可计算原因
     */
    private void markTimeUnavailable(RollingPlanningInput input, RollingTaskSnapshot task, String reason) {
        this.markTimeUnavailable(input, task, reason, null);
    }

    /**
     * 为预计时间无法计算的任务保存非阻断解释。
     *
     * @param input              当前滚动规划输入
     * @param task               目标任务快照
     * @param reason             时间不可计算原因
     * @param predecessorTaskKey 时间链前置任务键
     */
    private void markTimeUnavailable(RollingPlanningInput input, RollingTaskSnapshot task, String reason,
                                     String predecessorTaskKey) {
        task.setTimeUnavailableReason(reason);
        RollingMaterialSnapshot material = input.getMaterials().get(task.getMaterialCode());
        if (material != null) {
            List<Map<String, Object>> unavailable = (List<Map<String, Object>>) material.getEvidence()
                    .computeIfAbsent("timeUnavailableTasks", ignored -> new ArrayList<Map<String, Object>>());
            if (unavailable.stream().noneMatch(row -> task.getKey().equals(row.get("taskKey")))) {
                Map<String, Object> detail = new LinkedHashMap<>();
                detail.put("taskKey", task.getKey());
                detail.put("resultId", task.getResultId());
                detail.put("machineCode", task.getMachineCode());
                detail.put("planQty", task.getPlanQty());
                detail.put("finishQty", task.getFinishQty());
                detail.put("speed", task.getSpeed());
                detail.put("speedSource", task.getSpeedSource());
                detail.put("reasonCode", reason);
                detail.put("predecessorTaskKey", predecessorTaskKey);
                unavailable.add(detail);
            }
        }
    }

    /**
     * 汇总业务终态，与技术异常区分。
     */
    private void aggregate(RollingPlanningInput input) {
        Collection<RollingMaterialSnapshot> materials = input.getMaterials().values();
        boolean unresolved = materials.stream().anyMatch(material -> material.getGapQty().signum() > 0 || material.getReason() != null);
        if (input.isPlanChanged()) {
            input.setStatus(unresolved ? "PARTIAL_SUCCESS" : "SUCCESS");
        } else if (!unresolved) {
            input.setStatus("SKIPPED");
        } else {
            boolean capacity = materials.stream().anyMatch(material -> "BUSINESS_NO_CAPACITY".equals(material.getReason())
                    || "NO_ELIGIBLE_MACHINE".equals(material.getReason()) || "WORK_ORDER_COMPLETED".equals(material.getReason()));
            input.setStatus(capacity ? "FAILED" : "DATA_BLOCKED");
        }
    }

    /**
     * 读取评分所需实时米数余额，保留负值。
     *
     * @param input   当前账本
     * @param machine 目标机台
     * @return 有符号米数余额
     */
    public BigDecimal availableQty(RollingPlanningInput input, RollingMachineSnapshot machine) {
        return machine.getCapacityQty().subtract(machine.getTasks().stream().map(RollingTaskSnapshot::getPlanQty)
                .reduce(BigDecimal.ZERO, BigDecimal::add)).subtract(this.switchLoss(input, machine, this.order(input, machine.getTasks())));
    }

    /**
     * 用独立副本预览紧急任务插入位置的前驱，供领域评分复用。
     *
     * @param input    当前账本
     * @param machine  目标机台
     * @param material 上修物料
     * @return 前驱任务，班首时为空
     */
    public RollingTaskSnapshot insertionPredecessor(RollingPlanningInput input, RollingMachineSnapshot machine,
                                                    RollingMaterialSnapshot material) {
        List<RollingTaskSnapshot> tasks = machine.getTasks().stream().map(RollingTaskSnapshot::copy).collect(Collectors.toList());
        RollingTaskSnapshot target = tasks.stream().filter(task -> material.getMaterialCode().equals(task.getMaterialCode())
                && task.isEditable() && !this.completed(task)).findFirst().orElse(null);
        if (target == null) {
            target = material.getTemplate().copy();
            target.setKey("NEW:" + material.getMaterialCode());
            target.setSequence(Integer.MAX_VALUE);
            tasks.add(target);
        }
        target.setPlanQty(target.getInitialPlanQty().add(BigDecimal.ONE));
        String targetKey = target.getKey();
        List<RollingTaskSnapshot> ordered = this.order(input, tasks);
        for (int index = 0; index < ordered.size(); index++) {
            if (targetKey.equals(ordered.get(index).getKey())) {
                return index == 0 ? null : ordered.get(index - 1);
            }
        }
        return null;
    }

    /**
     * 返回物料实时行，避免沙箱合并后持有过期引用。
     */
    private List<RollingTaskSnapshot> rows(RollingPlanningInput input, String materialCode) {
        return input.getMachines().values().stream().flatMap(machine -> machine.getTasks().stream())
                .filter(task -> materialCode.equals(task.getMaterialCode()))
                .sorted(Comparator.comparing(RollingTaskSnapshot::getKey)).collect(Collectors.toList());
    }

    /**
     * 用于识别本轮实际数量、顺序及时间变化。
     */
    private String signature(RollingPlanningInput input) {
        return input.getMachines().values().stream().flatMap(machine -> machine.getTasks().stream())
                .sorted(Comparator.comparing(RollingTaskSnapshot::getKey))
                .map(task -> Arrays.asList(task.getKey(), task.getPlanQty(), task.getSequence(),
                        task.getStartTime(), task.getEndTime()).toString()).collect(Collectors.joining("|"));
    }

    /**
     * 已完成以本轮初始计划为准，避免上修后重新解释完成状态。
     */
    private boolean completed(RollingTaskSnapshot task) {
        return task.getInitialPlanQty().signum() > 0 && task.getFinishQty().compareTo(task.getInitialPlanQty()) == 0;
    }

    /**
     * 非空工艺值发生变化时才计切换。
     */
    private boolean changed(String previous, String current) {
        return previous != null && !previous.trim().isEmpty() && current != null && !current.trim().isEmpty()
                && !previous.equals(current);
    }

    /**
     * 将已完成需求、损耗及卷长结算后的行计划量向上取整为整数米。
     *
     * @param task          待更新任务行，用于记录取整审计字段
     * @param calculatedQty 现有业务公式计算出的行计划量
     * @return 向上取整后的整数计划量；负数原样保留供既有校验识别
     */
    private BigDecimal ceilPlanRowQty(RollingTaskSnapshot task, BigDecimal calculatedQty) {
        task.setRoundingBeforeQty(calculatedQty);
        task.setRoundingRule("INTEGER_CEILING");
        BigDecimal finalQty = this.ceilIntegerQty(calculatedQty);
        task.setRoundingAddedQty(finalQty.subtract(calculatedQty).max(BigDecimal.ZERO));
        return finalQty;
    }

    /**
     * 对候选数量执行数学向上取整，负数保留给既有数量校验处理。
     *
     * @param calculatedQty 现有业务公式计算出的行数量
     * @return 不小于输入值的整数数量
     */
    private BigDecimal ceilIntegerQty(BigDecimal calculatedQty) {
        return calculatedQty.signum() < 0
                ? calculatedQty : calculatedQty.setScale(0, RoundingMode.CEILING);
    }
}
