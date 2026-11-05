package com.zlt.aps.common.engine.schedule.engine;

import com.zlt.aps.common.core.utils.BigDecimalUtils;

import java.math.BigDecimal;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 两阶段跨班基础额度账本。后班原生缺口先吸收同源残量，不把两份目标相加。
 * 物理行只提交一次；来源归属单独记录，供解释分摊，不改变任务链数量。
 *
 * @param <T> 领域任务
 */
public final class ScheduleTwoPhaseCarryLedger<T extends ScheduleTaskDraftModel> {

    /**
     * 尚未承接的原基础额度，按进入账本顺序稳定分配。
     */
    private final Map<String, ScheduleBaseCarryQuota<T>> quotas = new LinkedHashMap<>();
    /**
     * 实际行业务键对应的各原计划组基础归属。
     */
    private final Map<String, Map<String, BigDecimal>> baseOwnership = new LinkedHashMap<>();

    /**
     * 复制账本并把配额任务引用重连到工作区任务副本。
     *
     * @param taskCopier 任务复制器
     * @return 不与原账本共享可变容器的新账本
     */
    public ScheduleTwoPhaseCarryLedger<T> copy(Function<T, T> taskCopier) {
        ScheduleTwoPhaseCarryLedger<T> copy = new ScheduleTwoPhaseCarryLedger<>();
        this.quotas.forEach((key, sourceQuota) -> {
            ScheduleBaseCarryQuota<T> quota = new ScheduleBaseCarryQuota<>();
            quota.setSourceTask(sourceQuota.getSourceTask() == null ? null : taskCopier.apply(sourceQuota.getSourceTask()));
            quota.setDiagnosticTask(sourceQuota.getDiagnosticTask() == null ? null
                    : taskCopier.apply(sourceQuota.getDiagnosticTask()));
            quota.setOriginBaseQuotaKey(sourceQuota.getOriginBaseQuotaKey());
            quota.setPlanGroupKey(sourceQuota.getPlanGroupKey());
            quota.setSourceProductionShift(sourceQuota.getSourceProductionShift());
            quota.setSourceIdentities(sourceQuota.getSourceIdentities() == null ? Collections.emptySet()
                    : new LinkedHashSet<>(sourceQuota.getSourceIdentities()));
            quota.setRemainingQty(sourceQuota.getRemainingQty());
            quota.setCommitted(sourceQuota.isCommitted());
            copy.quotas.put(key, quota);
        });
        this.baseOwnership.forEach((businessKey, ownerMap) ->
                copy.baseOwnership.put(businessKey, new LinkedHashMap<>(ownerMap)));
        return copy;
    }

    /**
     * 找到允许在当前班承接且来源可证明重叠的残量。
     *
     * @param task             后班原生任务
     * @param sourceIdentities 后班完整来源集合
     * @return 可吸收的原额度
     */
    public List<ScheduleBaseCarryQuota<T>> overlappingQuotas(T task, Set<String> sourceIdentities) {
        return this.quotas.values().stream().filter(quota -> quota.getRemainingQty().signum() > 0)
                .filter(ScheduleBaseCarryQuota::isCommitted)
                .filter(quota -> quota.getSourceProductionShift() < task.getShiftOrder())
                .filter(quota -> Objects.equals(quota.getSourceTask().getProcessCode(), task.getProcessCode()))
                .filter(quota -> Objects.equals(quota.getSourceTask().getGlueCode(), task.getGlueCode())
                        && Objects.equals(quota.getSourceTask().getMouthPlateCode(), task.getMouthPlateCode()))
                .filter(quota -> sourceIdentities != null && quota.getSourceIdentities() != null
                        && quota.getSourceIdentities().stream().anyMatch(sourceIdentities::contains))
                .collect(Collectors.toList());
    }

    /**
     * 登记后班实际基础量并吸收同源残量，幂等身份为当前物理行业务键。
     *
     * @param task             本班实际生产行
     * @param actualQty        已经通过资源检查的实际基础量
     * @param sourceIdentities 完整来源身份
     * @return 当前班自己拥有的基础缺口，不包含仍由前班原额度拥有的残量
     */
    public BigDecimal assignNativeBase(T task, BigDecimal actualQty, Set<String> sourceIdentities) {
        if (this.baseOwnership.containsKey(task.getBusinessKey())) {
            return this.ownRemaining(task, sourceIdentities);
        }
        BigDecimal nativeOwned = BigDecimalUtils.valueOf(actualQty);
        Map<String, BigDecimal> owners = new LinkedHashMap<>();
        for (ScheduleBaseCarryQuota<T> quota : this.overlappingQuotas(task, sourceIdentities)) {
            BigDecimal absorbed = nativeOwned.min(quota.getRemainingQty());
            if (absorbed.signum() > 0) {
                owners.merge(quota.getPlanGroupKey(), absorbed, BigDecimal::add);
                this.reduceQuota(quota, absorbed);
                nativeOwned = nativeOwned.subtract(absorbed);
            }
        }
        if (nativeOwned.signum() > 0) {
            owners.merge(task.getPlanGroupKey(), nativeOwned, BigDecimal::add);
        }
        this.baseOwnership.put(task.getBusinessKey(), owners);
        return this.ownRemaining(task, sourceIdentities);
    }

    /**
     * 登记独立carry片段，只减少同一原额度，绝不创建第二份基础目标。
     *
     * @param task      已成功提交的carry行
     * @param actualQty 实际承接量
     */
    public void assignCarryBase(T task, BigDecimal actualQty) {
        if (this.baseOwnership.containsKey(task.getBusinessKey())) {
            return;
        }
        ScheduleBaseCarryQuota<T> quota = this.quotas.get(task.getOriginBaseQuotaKey());
        BigDecimal actual = BigDecimalUtils.valueOf(actualQty);
        boolean minimumSurplus = quota != null && ScheduleDemandOnlyQuantityPolicy.enabled(task)
                && actual.compareTo(task.getDemandOnlyMinimumQty()) == 0
                && quota.getRemainingQty().signum() > 0;
        if (quota == null || (actual.compareTo(quota.getRemainingQty()) > 0 && !minimumSurplus)) {
            throw new IllegalStateException("Base carry has no matching remaining quota");
        }
        // 最低量额外产出只消耗一次原需求余额，实产全部归属原来源并进入库存。
        this.reduceQuota(quota, actual.min(quota.getRemainingQty()));
        Map<String, BigDecimal> owners = new LinkedHashMap<>();
        owners.put(quota.getPlanGroupKey(), actual);
        this.baseOwnership.put(task.getBusinessKey(), owners);
    }

    /**
     * 只登记当前班自身的新缺口，已在旧额度中的部分不再次入账。
     *
     * @param source           来源任务
     * @param diagnostic       当前班自身缺口片段
     * @param ownRemaining     当前班自己拥有的缺口
     * @param sourceIdentities 完整来源身份
     */
    public void stageQuota(T source, T diagnostic, BigDecimal ownRemaining, Set<String> sourceIdentities) {
        if (BigDecimalUtils.valueOf(ownRemaining).signum() <= 0
                || this.quotas.containsKey(source.getOriginBaseQuotaKey())) {
            return;
        }
        ScheduleBaseCarryQuota<T> quota = new ScheduleBaseCarryQuota<>();
        quota.setSourceTask(source);
        quota.setDiagnosticTask(diagnostic);
        quota.setOriginBaseQuotaKey(source.getOriginBaseQuotaKey());
        quota.setPlanGroupKey(source.getPlanGroupKey());
        quota.setSourceProductionShift(source.getShiftOrder());
        quota.setSourceIdentities(sourceIdentities == null ? Collections.emptySet()
                : new LinkedHashSet<>(sourceIdentities));
        quota.setRemainingQty(ownRemaining);
        quota.setCommitted(true);
        this.quotas.put(quota.getOriginBaseQuotaKey(), quota);
    }

    /**
     * 未选机任务仅登记损耗前意图，不把请求冒充post-loss配额。
     *
     * @param source           原请求
     * @param diagnostic       唯一未排请求片段
     * @param requestQty       尚未结算损耗的请求量
     * @param sourceIdentities 来源集合
     */
    public void stageRequestQuota(T source, T diagnostic, BigDecimal requestQty, Set<String> sourceIdentities) {
        if (this.quotas.containsKey(source.getOriginBaseQuotaKey())) {
            return;
        }
        // 不混减pre-loss与post-loss；同源差量在选机后统一换成post-loss再扣旧额度。
        diagnostic.setPlanQty(BigDecimalUtils.valueOf(requestQty));
        this.stageQuota(source, diagnostic, BigDecimalUtils.valueOf(requestQty), sourceIdentities);
        ScheduleBaseCarryQuota<T> quota = this.quotas.get(source.getOriginBaseQuotaKey());
        if (quota != null) {
            quota.setCommitted(false);
        }
    }

    /**
     * 未来选机后才把原请求转为合法post-loss配额；不提交任何生产或工装。
     *
     * @param carry 已选机形成合法候选的carry意图
     */
    public void commitRequestQuota(T carry) {
        ScheduleBaseCarryQuota<T> quota = this.quotas.get(carry.getOriginBaseQuotaKey());
        if (quota == null || quota.isCommitted()) {
            return;
        }
        quota.setRemainingQty(BigDecimalUtils.valueOf(carry.getBaseRequired()));
        quota.setCommitted(true);
        T original = quota.getSourceTask();
        original.setBaseCommittedQty(carry.getBaseCommittedQty());
        original.setBaseRequired(carry.getBaseRequired());
        original.setBaseRemaining(carry.getBaseRequired());
        original.setSettlementMachineCode(carry.getSettlementMachineCode());
        original.setResolvedLossRate(carry.getResolvedLossRate());
        original.setTailPlanCandidateQty(carry.getTailPlanCandidateQty());
        original.setTailLimitCandidateQty(carry.getTailLimitCandidateQty());
        quota.getDiagnosticTask().setPlanQty(quota.getRemainingQty());
        quota.getDiagnosticTask().setQuantityStage(ScheduleQuantityStage.POST_LOSS_CANDIDATE);
    }

    /**
     * 对应原额度是否仍处于损耗前；候选试算失败不能改变这个事实。
     *
     * @param task 待承接片段
     * @return 是否需要在成功承接时确认候选
     */
    public boolean isRequestQuota(T task) {
        ScheduleBaseCarryQuota<T> quota = this.quotas.get(task.getOriginBaseQuotaKey());
        return quota != null && !quota.isCommitted();
    }

    /**
     * 同源新请求结算后，仅保留超过旧含损耗残量的基础额度；必须在资源承接前调用。
     *
     * @param task 已选机、未提交资源的损耗前carry候选
     * @return 本请求自己拥有的post-loss额度
     */
    public BigDecimal resolveRequestRequired(T task) {
        ScheduleBaseCarryQuota<T> quota = this.quotas.get(task.getOriginBaseQuotaKey());
        if (quota == null || quota.isCommitted()) {
            return BigDecimalUtils.valueOf(task.getBaseRequired());
        }
        BigDecimal oldRemaining = this.overlappingQuotas(task, quota.getSourceIdentities()).stream()
                .filter(old -> old.getSourceProductionShift() < quota.getSourceProductionShift())
                .map(ScheduleBaseCarryQuota::getRemainingQty).reduce(BigDecimal.ZERO, BigDecimal::add);
        return BigDecimalUtils.valueOf(task.getBaseRequired()).subtract(oldRemaining).max(BigDecimal.ZERO);
    }

    /**
     * 获取尚待具体机台结算且可证明同源的pre-loss意图。
     *
     * @param task             后班原生任务
     * @param sourceIdentities 完整来源集合
     * @return 不参与post-loss直接吸收的原请求
     */
    public List<ScheduleBaseCarryQuota<T>> matchingRequests(T task, Set<String> sourceIdentities) {
        return this.quotas.values().stream().filter(quota -> !quota.isCommitted())
                .filter(quota -> quota.getSourceProductionShift() < task.getShiftOrder())
                .filter(quota -> Objects.equals(quota.getSourceTask().getProcessCode(), task.getProcessCode()))
                .filter(quota -> Objects.equals(quota.getSourceTask().getGlueCode(), task.getGlueCode())
                        && Objects.equals(quota.getSourceTask().getMouthPlateCode(), task.getMouthPlateCode()))
                .filter(quota -> sourceIdentities != null
                        && quota.getSourceIdentities().stream().anyMatch(sourceIdentities::contains))
                .collect(Collectors.toList());
    }

    /**
     * 没有本班同物料原生任务时，返回可创建基础carry意图的原额度。
     * 有原生行即由其重算与吸收，不额外生产相同残量。
     *
     * @param shiftOrder  当前班次
     * @param nativeTasks 本班原生任务（可以为空）
     * @return 不提前扣任何产能的待承接意图
     */
    public List<ScheduleBaseCarryQuota<T>> standaloneIntents(Integer shiftOrder, List<T> nativeTasks) {
        Set<String> products = nativeTasks.stream().map(ScheduleTaskDraftModel::getProcessCode)
                .collect(Collectors.toSet());
        return this.quotas.values().stream().filter(quota -> quota.getRemainingQty().signum() > 0)
                .filter(quota -> quota.getSourceProductionShift() < shiftOrder)
                .filter(quota -> !products.contains(quota.getSourceTask().getProcessCode()))
                .collect(Collectors.toCollection(ArrayList::new));
    }

    /**
     * 查询物理基础行在各原计划组的归属；返回独立副本。
     *
     * @param businessKey 实际行业务键
     * @return 归属数量
     */
    public Map<String, BigDecimal> ownersOf(String businessKey) {
        return new LinkedHashMap<>(this.baseOwnership.getOrDefault(businessKey, Collections.emptyMap()));
    }

    /**
     * 计算本班未承接量扣除已有同源配额后的新缺口。
     *
     * @param task             本班原生任务
     * @param sourceIdentities 来源集合
     * @return 不重复登记的本班自身缺口
     */
    private BigDecimal ownRemaining(T task, Set<String> sourceIdentities) {
        BigDecimal pending = this.overlappingQuotas(task, sourceIdentities).stream()
                .map(ScheduleBaseCarryQuota::getRemainingQty).reduce(BigDecimal.ZERO, BigDecimal::add);
        return BigDecimalUtils.valueOf(task.getBaseRemaining()).subtract(pending).max(BigDecimal.ZERO);
    }

    /**
     * 同步扣减额度与唯一未排片段；来源原班缺口字段保持历史冻结，不解除其extra禁入。
     *
     * @param quota    原额度
     * @param assigned 本次已确认承接量
     */
    private void reduceQuota(ScheduleBaseCarryQuota<T> quota, BigDecimal assigned) {
        quota.setRemainingQty(quota.getRemainingQty().subtract(assigned));
        if (quota.getDiagnosticTask() != null) {
            quota.getDiagnosticTask().setPlanQty(quota.getRemainingQty());
        }
    }
}
