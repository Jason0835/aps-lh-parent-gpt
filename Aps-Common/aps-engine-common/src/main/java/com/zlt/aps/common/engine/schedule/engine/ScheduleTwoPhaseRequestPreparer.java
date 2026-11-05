package com.zlt.aps.common.engine.schedule.engine;

import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.aps.common.core.utils.BigDecimalUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 两阶段来源冻结与请求适配，保留原完整guard及有效收尾判定。
 */
public final class ScheduleTwoPhaseRequestPreparer {

    private final ScheduleTwoPhaseQuantityCalculator quantities = new ScheduleTwoPhaseQuantityCalculator();

    /**
     * 在首次计划计算前冻结可信原始输入；重复调用不得把回写planQty误作预置量。
     *
     * @param task         原始或刚聚合完成的草稿
     * @param batchNo      既有运行批次
     * @param factoryCode  工厂
     * @param scheduleDate 当前排程日
     */
    public void freezeInput(ScheduleTaskDraftModel task, String batchNo, String factoryCode,
                            LocalDate scheduleDate) {
        if (task.getInputSnapshotKey() != null) {
            return;
        }
        task.setInputQuantityStage(task.getQuantityStage() == ScheduleQuantityStage.POST_LOSS_PRESET
                ? ScheduleQuantityStage.POST_LOSS_PRESET : ScheduleQuantityStage.PRE_LOSS_REQUEST);
        task.setSourceBusinessKey(task.getBusinessKey());
        List<String> sourceKeys = task.getSourceTaskBusinessKeyList();
        String sourceIdentity = sourceKeys != null && sourceKeys.size() > 1
                ? sourceKeys.stream().filter(Objects::nonNull).distinct().sorted()
                .map(this::part).collect(Collectors.joining())
                : task.getFormingSourceKey();
        if (sourceIdentity == null || sourceIdentity.trim().isEmpty()) {
            sourceIdentity = sourceKeys == null ? null : sourceKeys.stream().filter(Objects::nonNull)
                    .filter(source -> !source.trim().isEmpty()).distinct().sorted()
                    .map(this::part).collect(Collectors.joining());
        }
        if (sourceIdentity == null || sourceIdentity.trim().isEmpty()) {
            sourceIdentity = task.getSourceBusinessKey();
            this.quantities.addReason(task, "UNSOURCEABLE_DEMAND_EVENT");
        }
        if (sourceIdentity == null || sourceIdentity.trim().isEmpty()) {
            throw new ServiceException(I18nUtil.getMessage("ui.schedule.twoPhase.invalidDemandEvent"));
        }
        task.setSourceIdentity(sourceIdentity);
        Integer demandShift = this.resolveDemandShift(task);
        String shiftKey = this.part(scheduleDate) + this.part(demandShift);
        task.setDemandShiftKey(shiftKey);
        task.setOriginDemandShiftKey(shiftKey);
        task.setOriginalSourceShiftKey(this.part(task.getFormingSourceScheduleDate())
                + this.part(task.getSourceShiftOrder()));
        task.setDemandEventKey(this.part(factoryCode) + this.part(task.getProcessCode())
                + this.part(sourceIdentity) + this.part(shiftKey));
        task.setBaseProductionShiftKey(this.part(scheduleDate) + this.part(task.getShiftOrder()));
        task.setActualProductionShiftKey(task.getBaseProductionShiftKey());
        task.setOriginBaseQuotaKey(this.part(batchNo) + this.part(task.getDemandEventKey())
                + this.part(task.getSourceBusinessKey()));
        task.setPhaseBalanceKey(this.part(task.getOriginBaseQuotaKey())
                + this.part(task.getActualProductionShiftKey()));
        task.setCopyPurpose(ScheduleCopyPurpose.BASE_ASSIGN);
        task.setProductionAllocKey(this.part(task.getDemandEventKey())
                + this.part(task.getActualProductionShiftKey()) + this.part(task.getCopyPurpose())
                + this.part(task.getSourceBusinessKey()));
        if (task.getPlanQty() != null && task.getRequestSourceMode() == null) {
            task.setPresetInputPlanQty(task.getPlanQty());
            task.setRequestSourceMode(ScheduleRequestSourceMode.PRESET_PRELOSS);
        }
        if (task.getRequestSourceMode() == ScheduleRequestSourceMode.PRESET_POSTLOSS
                && task.getPresetInputPlanQty() == null) {
            task.setPresetInputPlanQty(task.getPlanQty());
        }
        task.setInputSnapshotKey(this.part(batchNo) + this.part(task.getSourceBusinessKey())
                + this.part(shiftKey) + this.part(task.getPresetInputPlanQty()) + this.part(sourceIdentity)
                + this.part(task.getQuantityStage() == ScheduleQuantityStage.POST_LOSS_PRESET
                ? ScheduleQuantityStage.POST_LOSS_PRESET : ScheduleQuantityStage.PRE_LOSS_REQUEST));
    }

    /**
     * 原领域策略完成覆盖/收尾/开产判定后，准备本班首轮请求，不提前计算损耗。
     *
     * @param task          领域已计算的任务
     * @param effectiveTail 原领域isTailTask结果
     * @param tailLimit     原领域完整收尾窗口解析出的上限
     */
    public void prepare(ScheduleTaskDraftModel task, boolean effectiveTail, BigDecimal tailLimit) {
        if (task.getSettlementMode() == ScheduleSettlementMode.RETAINED_FACT
                || task.getRequestSourceMode() == ScheduleRequestSourceMode.RETAINED_FACT) {
            BigDecimal factQty = BigDecimalUtils.valueOf(task.getPlanQty());
            task.setRequestSourceMode(ScheduleRequestSourceMode.RETAINED_FACT);
            task.setSettlementMode(ScheduleSettlementMode.RETAINED_FACT);
            task.setExtraPolicy(ScheduleExtraPolicy.FORBID_RETAINED_FACT);
            this.quantities.acceptCommittedCandidates(task, factQty, factQty);
            this.quantities.recordBaseAssignment(task, factQty, factQty);
            this.quantities.addReason(task, ScheduleExtraPolicy.FORBID_RETAINED_FACT.name());
            return;
        }
        if (task.getCopyPurpose() == ScheduleCopyPurpose.BASE_CARRY) {
            // 顺延任务按目标班禁排，冻结的待承接量不能因此丢失。
            if (Boolean.TRUE.equals(task.getPostTailShiftBlocked())) {
                task.setDispatchBlocked(true);
                task.setExtraPolicy(ScheduleExtraPolicy.FORBID_BLOCKED);
                this.quantities.addReason(task, ScheduleExtraPolicy.FORBID_BLOCKED.name());
                return;
            }
            task.setDispatchBlocked(false);
            task.setExtraPolicy(ScheduleExtraPolicy.FORBID_BASE_CARRY);
            if (task.getSettlementMode() == ScheduleSettlementMode.POST_LOSS_CARRY) {
                task.setQuantityStage(ScheduleQuantityStage.POST_LOSS_CARRY);
            } else {
                // 损耗前基础carry必须保留复制时冻结的请求，不能被本班N2/Nall重新覆盖。
                if (task.getSettlementMode() == null) {
                    task.setSettlementMode(ScheduleSettlementMode.NORMAL);
                }
                if (task.getBaseRequestQty() == null) {
                    task.setBaseRequestQty(BigDecimalUtils.valueOf(task.getPlanQty()));
                }
                if (task.getTargetRequestQty() == null) {
                    task.setTargetRequestQty(task.getBaseRequestQty());
                }
                task.setPreLossPlanQty(task.getBaseRequestQty());
                task.setPlanQty(task.getBaseRequestQty());
                task.setQuantityStage(ScheduleQuantityStage.PRE_LOSS_REQUEST);
            }
            this.quantities.addReason(task, ScheduleExtraPolicy.FORBID_BASE_CARRY.name());
            return;
        }
        boolean shutdown = Boolean.TRUE.equals(task.getFormingShutdownCloseOutFlag());
        boolean blocked = Boolean.TRUE.equals(task.getTwoPhaseDuplicateTask())
                || Boolean.TRUE.equals(task.getPostTailShiftBlocked());
        boolean newSpec = task.getCommonNewSpecInfo() != null && task.getCommonNewSpecInfo().isNewSpecHit();
        boolean experiment = task.getCommonExperimentSpecInfo() != null
                && task.getCommonExperimentSpecInfo().isExperimentSpecHit();
        boolean preset = task.getRequestSourceMode() == ScheduleRequestSourceMode.PRESET_PRELOSS
                || task.getRequestSourceMode() == ScheduleRequestSourceMode.PRESET_POSTLOSS;
        boolean postLossPreset = task.getRequestSourceMode() == ScheduleRequestSourceMode.PRESET_POSTLOSS;
        List<ScheduleExtraPolicy> forbids = new ArrayList<>();
        task.setDispatchBlocked(blocked);
        if (blocked) {
            task.setRequestSourceMode(ScheduleRequestSourceMode.BLOCKED);
            forbids.add(ScheduleExtraPolicy.FORBID_BLOCKED);
        } else if (!preset) {
            task.setRequestSourceMode(Boolean.TRUE.equals(task.getFutureShutdownSupplementTask())
                    ? ScheduleRequestSourceMode.FUTURE_SUPPLEMENT : experiment
                    ? ScheduleRequestSourceMode.EXPERIMENT : newSpec ? ScheduleRequestSourceMode.NEW_SPEC
                    : Boolean.TRUE.equals(task.getTwoShiftLeadTask()) ? ScheduleRequestSourceMode.LEAD
                    : ScheduleRequestSourceMode.NORMAL);
        }
        if (shutdown) {
            forbids.add(ScheduleExtraPolicy.FORBID_SHUTDOWN);
        }
        if (effectiveTail) {
            forbids.add(ScheduleExtraPolicy.FORBID_TAIL);
        } else if ("1".equals(task.getTailFlag())) {
            forbids.add(ScheduleExtraPolicy.FORBID_RAW_TAIL);
        }
        if (preset) {
            forbids.add(postLossPreset ? ScheduleExtraPolicy.FORBID_POSTLOSS_PRESET
                    : ScheduleExtraPolicy.FORBID_PRESET);
        }
        if (Boolean.TRUE.equals(task.getFutureShutdownSupplementTask())) {
            forbids.add(ScheduleExtraPolicy.FORBID_FUTURE_SUPPLEMENT);
        }
        task.setExtraPolicy(ScheduleExtraPolicy.primary(forbids));
        forbids.forEach(policy -> this.quantities.addReason(task, policy.name()));
        if (blocked) {
            task.setQuantityStage(null);
            task.setBaseRequestQty(BigDecimal.ZERO);
            task.setTargetRequestQty(BigDecimal.ZERO);
            return;
        }
        task.setSettlementMode(postLossPreset ? ScheduleSettlementMode.POST_LOSS_PRESET
                : effectiveTail ? ScheduleSettlementMode.TAIL_DUAL_INPUT : ScheduleSettlementMode.NORMAL);
        if (postLossPreset) {
            task.setBaseRequestQty(null);
            task.setTargetRequestQty(null);
            task.setPlanQty(task.getPresetInputPlanQty());
            task.setQuantityStage(ScheduleQuantityStage.POST_LOSS_PRESET);
            return;
        }
        if (effectiveTail) {
            task.setTailPlanBaseRequestQty(task.getBaseDemandQty());
            task.setTailLimitRequestQty(tailLimit);
            task.setQuantityStage(ScheduleQuantityStage.PRE_LOSS_REQUEST);
            return;
        }
        if (preset || Boolean.TRUE.equals(task.getFutureShutdownSupplementTask())) {
            BigDecimal request = preset ? this.presetPreLoss(task)
                    : BigDecimalUtils.valueOf(task.getPreLossPlanQty());
            task.setBaseRequestQty(request);
            task.setTargetRequestQty(request);
            task.setPreLossPlanQty(request);
            task.setPlanQty(request);
            task.setQuantityStage(ScheduleQuantityStage.PRE_LOSS_REQUEST);
            return;
        }
        // 规格标志只标识需求来源；新规格和实验规格也必须先满足基础需求，再竞争备库资源。
        this.quantities.prepareNormalRequest(task, !shutdown);
    }

    /**
     * 解析消费归属班；生产前移不能把消费同时前移。
     *
     * @param task 原需求任务
     * @return 当前排程轴上的活动消费班
     */
    public Integer resolveDemandShift(ScheduleTaskDraftModel task) {
        if (Boolean.TRUE.equals(task.getFutureShutdownSupplementTask())) {
            return task.getShiftOrder();
        }
        if (task.getCommonExperimentSpecInfo() != null
                && task.getCommonExperimentSpecInfo().isExperimentSpecHit()) {
            Integer normalTarget = task.getCommonExperimentSpecInfo().getNormalTargetShift();
            if (normalTarget == null) {
                throw new ServiceException(I18nUtil.getMessage("ui.schedule.twoPhase.invalidDemandEvent"));
            }
            return normalTarget;
        }
        if (task.getCommonNewSpecInfo() != null && task.getCommonNewSpecInfo().isNewSpecHit()) {
            Integer demandShift = task.getCommonNewSpecInfo().getDemandShift();
            if (demandShift != null) {
                return demandShift;
            }
            if (task.getCommonNewSpecInfo().getNormalTargetShift() != null) {
                return task.getCommonNewSpecInfo().getNormalTargetShift();
            }
            throw new ServiceException(I18nUtil.getMessage("ui.schedule.twoPhase.invalidDemandEvent"));
        }
        return task.getShiftOrder();
    }

    /**
     * 原需求事件的真实消费；lead/carry副本不新增消费。
     *
     * @param task 原需求任务
     * @return 本事件候选消费量
     */
    public BigDecimal consumptionQty(ScheduleTaskDraftModel task) {
        if (Boolean.TRUE.equals(task.getTwoShiftLeadTask())
                || task.getRequestSourceMode() == ScheduleRequestSourceMode.RETAINED_FACT
                || task.getCopyPurpose() == ScheduleCopyPurpose.BASE_CARRY
                || task.getCopyPurpose() == ScheduleCopyPurpose.PROBE
                || task.getCopyPurpose() == ScheduleCopyPurpose.UNPLANNED_DIAGNOSTIC) {
            return BigDecimal.ZERO;
        }
        return BigDecimalUtils.valueOf(Boolean.TRUE.equals(task.getFormingShutdownCloseOutFlag())
                ? task.getFormingShutdownCloseOutDemandQty() : task.getCurrentShiftDemandQty());
    }

    /**
     * 基础carry只换生产分配身份，不改变活动需求事件或原配额身份。
     *
     * @param task                独立carry意图
     * @param date                当前排程日期
     * @param shiftOrder          新实际生产班
     * @param sourceProductionKey 来源当前生产分配键
     * @param allocationIdentity  稳定分片身份
     */
    public void relocateBaseCarry(ScheduleTaskDraftModel task, LocalDate date, Integer shiftOrder,
                                  String sourceProductionKey, String allocationIdentity) {
        task.setOriginProductionAllocKey(sourceProductionKey);
        task.setActualProductionShiftKey(this.part(date) + this.part(shiftOrder));
        task.setProductionAllocKey(this.part(task.getDemandEventKey()) + this.part(task.getActualProductionShiftKey())
                + this.part(ScheduleCopyPurpose.BASE_CARRY) + this.part(allocationIdentity));
        task.setPhaseBalanceKey(this.part(task.getOriginBaseQuotaKey()) + this.part(task.getActualProductionShiftKey()));
        task.setParentInputSnapshotKey(task.getInputSnapshotKey());
        task.setInputQuantityStage(task.getSettlementMode() == ScheduleSettlementMode.POST_LOSS_CARRY
                ? ScheduleQuantityStage.POST_LOSS_CARRY : ScheduleQuantityStage.PRE_LOSS_REQUEST);
        task.setInputSnapshotKey(this.part(task.getParentInputSnapshotKey())
                + this.part(task.getInputQuantityStage()) + this.part(task.getPlanQty())
                + this.part(task.getProductionAllocKey()));
    }

    /**
     * 仅PRESET采用严格快照替换证明；普通请求不经过这里。
     *
     * @param task 可信预置来源
     * @return 冻结的损耗前输入
     */
    private BigDecimal presetPreLoss(ScheduleTaskDraftModel task) {
        if ("PRE_LOSS".equals(task.getPreLossValueStage())
                && task.getInputSnapshotKey() != null
                && task.getInputSnapshotKey().equals(task.getPreLossValueSnapshotKey())) {
            return BigDecimalUtils.valueOf(task.getPreLossPlanQty());
        }
        return BigDecimalUtils.valueOf(task.getPresetInputPlanQty());
    }

    /**
     * 长度前缀规范编码，NULL与0分离，避免分隔符或hashCode碰撞。
     *
     * @param value 身份分段
     * @return 稳定、无歧义编码
     */
    private String part(Object value) {
        if (value == null) {
            return "-1:";
        }
        String text = value instanceof BigDecimal ? ((BigDecimal) value).stripTrailingZeros().toPlainString()
                : value.toString();
        return text.length() + ":" + text;
    }
}
