package com.zlt.aps.common.engine.schedule.engine;

import com.zlt.aps.common.core.utils.BigDecimalUtils;
import com.zlt.aps.common.engine.schedule.constraint.ScheduleConstraintCalculator;
import com.zlt.aps.common.engine.schedule.constraint.SchedulePlanQtyAdjustmentResult;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * 两阶段数量生命周期纯计算器。损耗只在选机后结算，资源承接由领域服务提交。
 * 本类不读数据库、不改任务链、不扣资源，也不裁窄成型收尾窗口。
 */
public final class ScheduleTwoPhaseQuantityCalculator {

    private final ScheduleConstraintCalculator constraintCalculator = new ScheduleConstraintCalculator();

    /**
     * 准备普通或提前任务的损耗前请求；已有领域门禁必须先完成本班重算。
     *
     * @param task               本班草稿
     * @param coverageApplicable 是否适用库存覆盖门禁
     */
    public void prepareNormalRequest(ScheduleTaskDraftModel task, boolean coverageApplicable) {
        task.setExtraCreateTask(Boolean.FALSE);
        BigDecimal stock = this.nonNegative(task.getRollingStockQty());
        BigDecimal currentDemand = this.nonNegative(task.getCurrentShiftDemandQty());
        BigDecimal nextDemand = this.nonNegative(task.getNextShiftDemandQty());
        BigDecimal guardDemand = this.nonNegative(task.getGuardDemandQty());
        boolean covered = coverageApplicable && Boolean.TRUE.equals(task.getStockCoverageCovered());
        // 配置窗口可短于两班；覆盖结论不能替代当前班与下一班的基础保供计算。
        BigDecimal baseRequest = currentDemand.add(nextDemand).subtract(stock).max(BigDecimal.ZERO);
        // 基础库存已覆盖不代表完整备库窗口已覆盖，目标请求必须独立计算。
        BigDecimal targetRequest = currentDemand.add(guardDemand).subtract(stock).max(BigDecimal.ZERO);
        task.setBaseRequestQty(baseRequest);
        task.setTargetRequestQty(targetRequest);
        task.setQuantityStage(ScheduleQuantityStage.PRE_LOSS_REQUEST);
        task.setPreLossPlanQty(baseRequest);
        task.setPreLossValueStage("PRE_LOSS");
        task.setPreLossValueSnapshotKey(task.getInputSnapshotKey());
        task.setPlanQty(baseRequest);
        task.setBaseAssignQty(BigDecimal.ZERO);
        task.setExtraAssignQty(BigDecimal.ZERO);
        if (baseRequest.signum() == 0) {
            task.setBaseCommittedQty(BigDecimal.ZERO);
            task.setBaseRequired(BigDecimal.ZERO);
            task.setBaseRemaining(BigDecimal.ZERO);
        }
        if (covered) {
            this.addReason(task, "STOCK_COVERAGE_COVERED");
        }
    }

    /**
     * 用选中机台的同一组规则形成普通请求的基础和全量候选，不提交资源。
     *
     * @param task             已准备损耗前请求的任务
     * @param lossRatePercent  选中机台损耗百分比
     * @param curlLength       有效卷长
     * @param calculationScale 领域损耗计算精度
     * @return 基础候选的完整原约束证据，全量候选保存在任务上
     * @throws IllegalStateException 任务不是普通损耗前结算模式时抛出
     */
    public SchedulePlanQtyAdjustmentResult calculateNormalCandidates(ScheduleTaskDraftModel task,
                                                                     BigDecimal lossRatePercent, BigDecimal curlLength, int calculationScale) {
        if (task.getSettlementMode() != ScheduleSettlementMode.NORMAL
                || task.getQuantityStage() != ScheduleQuantityStage.PRE_LOSS_REQUEST) {
            throw new IllegalStateException("Two-phase normal candidates require a pre-loss normal request");
        }
        SchedulePlanQtyAdjustmentResult baseResult = this.adjustRequest(task, task.getBaseRequestQty(),
                lossRatePercent, task.getMinStartQty(), curlLength, calculationScale);
        SchedulePlanQtyAdjustmentResult targetResult = this.adjustRequest(task, task.getTargetRequestQty(),
                lossRatePercent, task.getMinStartQty(), curlLength, calculationScale);
        // 先保留真实卷长补卷结果，再将最终米数向上取整；整数结果不要求仍为整卷。
        BigDecimal baseIntegerQty = ScheduleDemandOnlyQuantityPolicy.enabled(task)
                ? baseResult.getFinalPlanQty() : this.normalizeIntegerQty(task, baseResult.getFinalPlanQty());
        if (ScheduleDemandOnlyQuantityPolicy.enabled(task)) {
            task.setIntegerRoundingBeforeQty(baseResult.getPlanQtyAfterMinStart());
            task.setIntegerRoundingAddedQty(baseIntegerQty.subtract(baseResult.getPlanQtyAfterMinStart()));
        }
        baseResult.setFinalPlanQty(baseIntegerQty);
        task.setBaseCommittedQty(baseIntegerQty);
        task.setTargetCommittedQty(targetResult.getFinalPlanQty().setScale(0, RoundingMode.CEILING));
        task.setQuantityStage(ScheduleQuantityStage.POST_LOSS_CANDIDATE);
        return baseResult;
    }

    /**
     * 接受领域特殊结算路径已经完成的合法候选，禁止本类再次计算损耗。
     *
     * @param task            专用收尾、预置、兼容提前等任务
     * @param baseCommitted   原路径的合法基础量
     * @param targetCommitted 原路径的合法全量
     */
    public void acceptCommittedCandidates(ScheduleTaskDraftModel task, BigDecimal baseCommitted,
                                          BigDecimal targetCommitted) {
        task.setBaseCommittedQty(this.nonNegative(baseCommitted));
        task.setTargetCommittedQty(this.nonNegative(targetCommitted));
        task.setQuantityStage(ScheduleQuantityStage.POST_LOSS_CANDIDATE);
    }

    /**
     * 初始化已提交行的实际分量；不能把待承接请求调用为已承接事实。
     *
     * @param task         实际任务行
     * @param assignedQty  已成功承接的数量
     * @param baseRequired 本班合法基础额度
     */
    public void recordBaseAssignment(ScheduleTaskDraftModel task, BigDecimal assignedQty,
                                     BigDecimal baseRequired) {
        BigDecimal actual = this.nonNegative(assignedQty);
        BigDecimal required = this.nonNegative(baseRequired);
        if (actual.compareTo(required) > 0) {
            throw new IllegalArgumentException("Base assignment exceeds its committed quota");
        }
        task.setBaseRequired(required);
        task.setBaseAssignQty(actual);
        task.setExtraAssignQty(BigDecimal.ZERO);
        task.setBaseRemaining(required.subtract(actual));
        task.setPlanQty(actual);
        task.setQuantityStage(ScheduleQuantityStage.ASSIGNED);
        task.setExtraPotentialQty(this.nonNegative(task.getTargetCommittedQty()).subtract(required)
                .max(BigDecimal.ZERO));
        task.setExtraActualQty(BigDecimal.ZERO);
        task.setExtraAllowedQty(BigDecimal.ZERO);
    }

    /**
     * 基础屏障后冻结结构目标，不读取可用产能、工装或候选追加时点。
     *
     * @param task             已承接任务行或未承接诊断草稿
     * @param existingLegalRow 本班存在合法原行
     * @param bindingMatches   当日绑定一致
     * @param timeModeReady    业务时间模式已明确且可进入试算
     */
    public void freezeExtraTarget(ScheduleTaskDraftModel task, boolean existingLegalRow,
                                  boolean bindingMatches, boolean timeModeReady) {
        boolean baseSatisfied = task.getBaseRequired() != null
                && this.nonNegative(task.getBaseAssignQty()).compareTo(task.getBaseRequired()) >= 0;
        boolean eligible = existingLegalRow && baseSatisfied && bindingMatches && timeModeReady
                && task.getExtraPolicy() == ScheduleExtraPolicy.ALLOW;
        task.setExtraEligibleTask(eligible);
        BigDecimal target = eligible ? this.nonNegative(task.getExtraPotentialQty()) : BigDecimal.ZERO;
        task.setExtraTargetQty(target);
        task.setExtraRemaining(target.subtract(this.nonNegative(task.getExtraAssignQty())).max(BigDecimal.ZERO));
        if (!baseSatisfied) {
            this.addReason(task, "BASE_INCOMPLETE");
        }
        if (!existingLegalRow) {
            this.addReason(task, "NO_EXISTING_TASK_PREVENTED_SWITCH");
        }
    }

    /**
     * 准备独立新行的损耗前请求，仅操作试算副本，不修改原始成型来源。
     *
     * @param task             基础请求和当前计划均为0的普通候选副本
     * @param targetRequestQty 目标生产班按实际库存重算的损耗前备库请求
     * @throws IllegalArgumentException 非普通可备库任务、有基础量或已有计划时拒绝
     */
    public void prepareExtraCreateRequest(ScheduleTaskDraftModel task, BigDecimal targetRequestQty) {
        if (task == null || task.getSettlementMode() != ScheduleSettlementMode.NORMAL
                || task.getExtraPolicy() != ScheduleExtraPolicy.ALLOW
                || this.nonNegative(task.getPlanQty()).signum() > 0
                || this.nonNegative(task.getBaseRequestQty()).signum() > 0
                || this.nonNegative(task.getBaseAssignQty()).signum() > 0
                || this.nonNegative(task.getBaseRequired()).signum() > 0
                || this.nonNegative(targetRequestQty).signum() <= 0) {
            throw new IllegalArgumentException("Invalid standalone reserve request");
        }
        task.setExtraCreateTask(Boolean.TRUE);
        task.setBaseRequestQty(BigDecimal.ZERO);
        task.setTargetRequestQty(targetRequestQty);
        task.setQuantityStage(ScheduleQuantityStage.PRE_LOSS_REQUEST);
        task.setBaseRequired(BigDecimal.ZERO);
        task.setBaseAssignQty(BigDecimal.ZERO);
        task.setBaseRemaining(BigDecimal.ZERO);
        task.setExtraAssignQty(BigDecimal.ZERO);
        task.setExtraActualQty(BigDecimal.ZERO);
        task.setExtraAllowedQty(BigDecimal.ZERO);
        task.setExtraTargetQty(BigDecimal.ZERO);
        task.setExtraRemaining(BigDecimal.ZERO);
        task.setExtraEligibleTask(Boolean.FALSE);
    }

    /**
     * 新行使用全量目标的数量证据，不沿用零基础候选的零损耗、零起排说明。
     * 此处只补解释分量，不改变已结算目标，不重复追加损耗或占用资源。
     *
     * @param task 已完成逐机结算的新行 @param curlLength 真实卷长 @param calculationScale 领域计算精度
     */
    public void describeExtraCreateTarget(ScheduleTaskDraftModel task, BigDecimal curlLength, int calculationScale) {
        SchedulePlanQtyAdjustmentResult target = this.adjustRequest(task, task.getTargetRequestQty(),
                task.getResolvedLossRate(), task.getMinStartQty(), curlLength, calculationScale);
        task.setPreLossPlanQty(target.getPreLossPlanQty());
        task.setLossAddQty(target.getLossAddQty());
        task.setMinStartAdjustQty(target.getMinStartAdjustQty());
        task.setTailRoundAdjustQty(target.getRoundAdjustQty());
        task.setIntegerRoundingBeforeQty(target.getFinalPlanQty());
        task.setIntegerRoundingAddedQty(target.getFinalPlanQty().setScale(0, RoundingMode.CEILING)
                .subtract(target.getFinalPlanQty()));
        task.setPlanQtyBeforeToolLimit(task.getTargetCommittedQty());
    }

    /**
     * 冻结已完成逐机损耗结算的新行目标；外部证明必须在提交前按最新资源复核。
     *
     * @param task          专用新行候选
     * @param noExistingRow 本班同物料不存在有效生产节点
     * @param proofsReady   来源、日历、时效、口型及绑定等领域证明全部有效
     * @throws IllegalArgumentException 用途、阶段或零基础约束不符时拒绝
     */
    public void freezeExtraCreateTarget(ScheduleTaskDraftModel task, boolean noExistingRow,
                                        boolean proofsReady) {
        if (!Boolean.TRUE.equals(task.getExtraCreateTask())
                || task.getQuantityStage() != ScheduleQuantityStage.POST_LOSS_CANDIDATE
                || task.getExtraPolicy() != ScheduleExtraPolicy.ALLOW
                || this.nonNegative(task.getPlanQty()).signum() > 0
                || this.nonNegative(task.getBaseCommittedQty()).signum() > 0
                || this.nonNegative(task.getBaseRequestQty()).signum() > 0
                || this.nonNegative(task.getBaseRequired()).signum() > 0
                || this.nonNegative(task.getBaseAssignQty()).signum() > 0) {
            throw new IllegalArgumentException("Invalid standalone reserve freeze stage");
        }
        boolean eligible = noExistingRow && proofsReady
                && this.nonNegative(task.getTargetCommittedQty()).signum() > 0;
        BigDecimal target = eligible ? task.getTargetCommittedQty() : BigDecimal.ZERO;
        task.setExtraEligibleTask(eligible);
        task.setExtraPotentialQty(target);
        task.setExtraTargetQty(target);
        task.setExtraRemaining(target);
    }

    /**
     * 纯备库正目标不足起排量时先补起排和整卷，再按硬上限选择实际承接量。
     * 目标已完成损耗结算，此处不重复增加损耗；资源限制后不足起排量仍拒绝建行。
     * 已达到起排量的目标沿用原结算值，避免对已取整数米的小数卷长候选重复补卷。
     *
     * @param targetQty   已完成损耗结算的整行目标；正目标不足起排量时允许补量
     * @param curlLength  真实正卷长
     * @param minStartQty 已解析的非负起排量，0沿用不设最低量的领域配置，未知或负数拒绝
     * @param limits      所有资源和时间的整行允许上限
     * @return 最终合法整数米数，不足起排或证明无效返回0
     */
    public BigDecimal calculateExtraCreateAllowedQty(BigDecimal targetQty, BigDecimal curlLength,
                                                     BigDecimal minStartQty, BigDecimal... limits) {
        BigDecimal normalizedTarget = this.nonNegative(targetQty);
        if (normalizedTarget.signum() == 0 || minStartQty == null || minStartQty.signum() < 0
                || this.nonNegative(curlLength).signum() == 0) {
            return BigDecimal.ZERO;
        }
        // 仅补需求侧的正目标，产能、工装和时间上限随后取小，不能因补量而被突破。
        if (normalizedTarget.compareTo(minStartQty) < 0) {
            normalizedTarget = minStartQty.divide(curlLength, 0, RoundingMode.CEILING)
                    .multiply(curlLength).setScale(0, RoundingMode.CEILING);
        }
        BigDecimal allowed = this.calculateAllowedPlanQty(normalizedTarget, curlLength, limits);
        return allowed.compareTo(minStartQty) < 0 ? BigDecimal.ZERO : allowed;
    }

    /**
     * 为一次性成功挂链的新行记账，不允许重复新建或绕过最终起排和整卷校验。
     *
     * @param task       已冻结且通过最新资源复核的新行
     * @param actualQty  实际承接的最终整数米量
     * @param curlLength 候选工艺的真实卷长
     * @throws IllegalArgumentException 用途、阶段、数量或余额不合法时拒绝
     */
    public void recordExtraCreateAssignment(ScheduleTaskDraftModel task, BigDecimal actualQty,
                                            BigDecimal curlLength) {
        BigDecimal actual = this.nonNegative(actualQty);
        BigDecimal legal = this.calculateExtraCreateAllowedQty(task, task.getExtraTargetQty(), curlLength,
                task.getMinStartQty(), task.getExtraAllowedQty(), actual);
        if (!Boolean.TRUE.equals(task.getExtraCreateTask())
                || task.getQuantityStage() != ScheduleQuantityStage.POST_LOSS_CANDIDATE
                || this.nonNegative(task.getPlanQty()).signum() > 0
                || this.nonNegative(task.getBaseAssignQty()).signum() > 0
                || this.nonNegative(task.getExtraAssignQty()).signum() > 0
                || actual.signum() <= 0 || actual.compareTo(legal) != 0) {
            throw new IllegalArgumentException("Invalid standalone reserve assignment");
        }
        this.recordExtraAssignment(task, actual);
        task.setQuantityStage(ScheduleQuantityStage.ASSIGNED);
    }

    /**
     * 在整行目标及资源上限内选择最大整卷候选，再将候选向上取整到整数米。
     *
     * @param targetQty 允许承接的整行目标米数
     * @param curlLength 有效真实卷长
     * @param limits 独立资源或时间返回的整行数量上限；null表示该维度不限
     * @return 不突破目标及资源的最大整数米候选；卷长无效返回0
     */
    public BigDecimal calculateAllowedPlanQty(BigDecimal targetQty, BigDecimal curlLength,
                                              BigDecimal... limits) {
        if (this.nonNegative(curlLength).signum() == 0) {
            return BigDecimal.ZERO;
        }
        BigDecimal allowed = this.nonNegative(targetQty);
        if (limits != null) {
            allowed = Arrays.stream(limits).filter(java.util.Objects::nonNull)
                    .map(this::nonNegative).reduce(allowed, BigDecimal::min);
        }
        // ceil(卷数×卷长) <= 上限，等价于卷数×卷长 <= floor(上限)。
        // 先收紧整数上限可一次求出最大卷数，避免向上补出的不足1米突破资源。
        BigDecimal integerLimit = allowed.setScale(0, RoundingMode.FLOOR);
        return integerLimit.divide(curlLength, 0, RoundingMode.FLOOR).multiply(curlLength)
                .setScale(0, RoundingMode.CEILING);
    }

    /**
     * 按整行总量选择备库追加量，避免基础段和多次追加分别补整数造成重复增量。
     *
     * @param existingQty 原行已经承接的整数米数
     * @param remainingTarget 尚未承接的备库目标米数
     * @param curlLength 有效真实卷长
     * @param limits 独立资源或时间允许增加的米数；null表示该维度不限
     * @return 最大合法新整行量减去原行量，无法增加时返回0
     */
    public BigDecimal calculateAllowedExtraForRow(BigDecimal existingQty, BigDecimal remainingTarget,
                                                  BigDecimal curlLength, BigDecimal... limits) {
        BigDecimal allowedExtra = this.nonNegative(remainingTarget);
        if (limits != null) {
            allowedExtra = Arrays.stream(limits).filter(java.util.Objects::nonNull)
                    .map(this::nonNegative).reduce(allowedExtra, BigDecimal::min);
        }
        BigDecimal existing = this.nonNegative(existingQty);
        BigDecimal total = this.calculateAllowedPlanQty(existing.add(allowedExtra), curlLength);
        return total.subtract(existing).max(BigDecimal.ZERO);
    }

    /**
     * 按用户确认的收尾整数米规则向上归一，新增量在资源扣减前明确记账。
     * 原小数收尾候选保留，最终整数执行量允许增加不足1米；零不新增生产。
     *
     * @param task         收尾任务或其合法post-loss片段
     * @param legalTailQty 已完成原损耗和收尾上限取小的数量
     * @return 最终整数米候选
     */
    public BigDecimal normalizeTailIntegerQty(ScheduleTaskDraftModel task, BigDecimal legalTailQty) {
        return this.normalizeIntegerQty(task, legalTailQty);
    }

    /**
     * 保存补整卷后的原数量与整数补量，供普通任务和收尾任务共用解释字段。
     *
     * @param task         当前任务
     * @param wholeRollQty 已完成原业务规则的候选数量
     * @return 向上取整后的整数米数；零保持为零
     */
    private BigDecimal normalizeIntegerQty(ScheduleTaskDraftModel task, BigDecimal wholeRollQty) {
        BigDecimal before = this.nonNegative(wholeRollQty);
        BigDecimal normalized = before.setScale(0, RoundingMode.CEILING);
        task.setIntegerRoundingBeforeQty(before);
        task.setIntegerRoundingAddedQty(normalized.subtract(before));
        return normalized;
    }

    /**
     * 按已确认的MIN_WITH_RISK生成基础额度，风险独立保留而不抬高全量上限。
     *
     * @param task 已应用领域封顶的候选
     * @return 本班合法基础额度
     */
    public BigDecimal resolveBaseRequired(ScheduleTaskDraftModel task) {
        BigDecimal base = this.nonNegative(task.getBaseCommittedQty());
        BigDecimal target = this.nonNegative(task.getTargetCommittedQty());
        if (target.compareTo(base) < 0) {
            this.addReason(task, "TARGET_BELOW_TWO_SHIFT_REQUIREMENT");
        }
        BigDecimal required = base.min(target);
        task.setBaseRequired(required);
        return required;
    }

    /**
     * 已确认post-loss输入只构造证据视图，不调用损耗、最小起排或收尾取整。
     *
     * @param committedQty 有来源证明的已结算量
     * @return 数量保持不变的结算视图
     */
    public SchedulePlanQtyAdjustmentResult postLossView(BigDecimal committedQty) {
        BigDecimal quantity = this.nonNegative(committedQty);
        SchedulePlanQtyAdjustmentResult result = new SchedulePlanQtyAdjustmentResult();
        result.setPreLossPlanQty(quantity);
        result.setLossAddQty(BigDecimal.ZERO);
        result.setPlanQtyAfterLoss(quantity);
        result.setMinStartAdjustQty(BigDecimal.ZERO);
        result.setPlanQtyAfterMinStart(quantity);
        result.setRoundAdjustQty(BigDecimal.ZERO);
        result.setFinalPlanQty(quantity);
        return result;
    }

    /**
     * 成功原子提交后登记追加实际量；调用者须先完成所有资源和时间校验。
     *
     * @param task        当前合法原行
     * @param extraActual 本次成功追加量
     * @throws IllegalArgumentException 追加超过冻结目标余额时抛出
     */
    public void recordExtraAssignment(ScheduleTaskDraftModel task, BigDecimal extraActual) {
        BigDecimal actual = this.nonNegative(extraActual);
        if (!Boolean.TRUE.equals(task.getExtraEligibleTask())
                || actual.compareTo(this.nonNegative(task.getExtraRemaining())) > 0
                || actual.compareTo(this.nonNegative(task.getExtraAllowedQty())) > 0) {
            throw new IllegalArgumentException("Extra assignment exceeds its allowed target balance");
        }
        task.setExtraActualQty(actual);
        task.setExtraAssignQty(this.nonNegative(task.getExtraAssignQty()).add(actual));
        task.setPlanQty(this.nonNegative(task.getBaseAssignQty()).add(task.getExtraAssignQty()));
        task.setExtraRemaining(this.nonNegative(task.getExtraTargetQty()).subtract(task.getExtraAssignQty())
                .max(BigDecimal.ZERO));
    }

    /**
     * 保存完整原因集合，调用者可独立解析展示主因。
     *
     * @param task   当前任务
     * @param reason 稳定内部原因码
     */
    public void addReason(ScheduleTaskDraftModel task, String reason) {
        Set<String> reasons = task.getTwoPhaseReasonCodes() == null
                ? new LinkedHashSet<>() : new LinkedHashSet<>(task.getTwoPhaseReasonCodes());
        reasons.add(reason);
        task.setTwoPhaseReasonCodes(reasons);
    }

    /**
     * 零请求仍调用原计算器的零输入路径，防止最小起排量凭空制造生产。
     *
     * @param request         损耗前净请求
     * @param lossRatePercent 损耗百分比
     * @param minStart        最小起排量
     * @param curlLength      卷长
     * @param scale           损耗精度
     * @return 原约束计算器结果
     */
    private SchedulePlanQtyAdjustmentResult adjustPositiveRequest(BigDecimal request,
                                                                  BigDecimal lossRatePercent, BigDecimal minStart, BigDecimal curlLength, int scale) {
        BigDecimal normalized = this.nonNegative(request);
        return this.constraintCalculator.calculatePlanQtyAfterLoss(normalized, lossRatePercent,
                minStart, curlLength, normalized.signum() == 0, scale);
    }

    /**
     * 根据显式策略分别结算普通整卷需求与免整卷需求。
     *
     * @param task     来源已判定的任务
     * @param request  净需求
     * @param lossRate 机台损耗率
     * @param minimum  普通最低量
     * @param curl     真实卷长
     * @param scale    计算精度
     * @return 数量阶段证据
     */
    private SchedulePlanQtyAdjustmentResult adjustRequest(ScheduleTaskDraftModel task, BigDecimal request,
                                                          BigDecimal lossRate, BigDecimal minimum, BigDecimal curl, int scale) {
        return ScheduleDemandOnlyQuantityPolicy.enabled(task)
                ? ScheduleDemandOnlyQuantityPolicy.settle(task, request, lossRate, scale)
                : this.adjustPositiveRequest(request, lossRate, minimum, curl, scale);
    }

    /**
     * 按领域显式策略选择整卷或免整卷的硬上限承接量。
     *
     * @param task   运行态任务
     * @param target 目标总量
     * @param curl   真实卷长
     * @param limits 硬上限
     * @return 最大合法承接量
     */
    public BigDecimal calculateAllowedPlanQty(ScheduleTaskDraftModel task, BigDecimal target,
                                              BigDecimal curl, BigDecimal... limits) {
        return ScheduleDemandOnlyQuantityPolicy.enabled(task)
                ? ScheduleDemandOnlyQuantityPolicy.allowed(task, target, limits)
                : this.calculateAllowedPlanQty(target, curl, limits);
    }

    /**
     * 独立新行按领域最低量检查，不以整卷规则拒绝按需生产任务。
     *
     * @param task    运行态任务
     * @param target  目标总量
     * @param curl    真实卷长
     * @param minimum 普通最低量
     * @param limits  硬上限
     * @return 合法新行量
     */
    public BigDecimal calculateExtraCreateAllowedQty(ScheduleTaskDraftModel task, BigDecimal target,
                                                     BigDecimal curl, BigDecimal minimum, BigDecimal... limits) {
        if (ScheduleDemandOnlyQuantityPolicy.enabled(task)) {
            return ScheduleDemandOnlyQuantityPolicy.allowed(task, target, limits);
        }
        // 冻结额度已包含开产封顶等领域门禁；目标补量不能重新突破已确认的额度。
        BigDecimal[] effectiveLimits = limits == null ? new BigDecimal[1]
                : Arrays.copyOf(limits, limits.length + 1);
        effectiveLimits[effectiveLimits.length - 1] = task.getExtraTargetQty();
        return this.calculateExtraCreateAllowedQty(target, curl, minimum, effectiveLimits);
    }

    /**
     * 已有行追加按最终总量选择整数米，不重复给增量补最低量。
     *
     * @param task      已有任务
     * @param existing  已有量
     * @param remaining 剩余目标
     * @param curl      真实卷长
     * @param limits    可追加硬上限
     * @return 允许新增量
     */
    public BigDecimal calculateAllowedExtraForRow(ScheduleTaskDraftModel task, BigDecimal existing,
                                                  BigDecimal remaining, BigDecimal curl, BigDecimal... limits) {
        if (!ScheduleDemandOnlyQuantityPolicy.enabled(task)) {
            return this.calculateAllowedExtraForRow(existing, remaining, curl, limits);
        }
        BigDecimal allowed = this.nonNegative(remaining);
        if (limits != null) {
            allowed = Arrays.stream(limits).filter(java.util.Objects::nonNull)
                    .map(this::nonNegative).reduce(allowed, BigDecimal::min);
        }
        return ScheduleDemandOnlyQuantityPolicy.allowed(task, this.nonNegative(existing).add(allowed))
                .subtract(this.nonNegative(existing)).max(BigDecimal.ZERO);
    }

    /**
     * 统一空值与负值归零，不使用浮点换算。
     * @param value 原数量
     * @return 非负数量
     */
    private BigDecimal nonNegative(BigDecimal value) {
        return BigDecimalUtils.valueOf(value).max(BigDecimal.ZERO);
    }
}
