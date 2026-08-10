package com.zlt.aps.lh.engine.strategy.support;

import com.zlt.aps.lh.api.domain.dto.SkuScheduleDTO;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * S4.5 新增排产三天窗口共用的日驱动状态。
 *
 * <p>该状态只维护候选生命周期、延期任务和跨日在机绑定。机台、模具、胎胚、胶囊、
 * 日计划和生产余量仍保存在同一个排程上下文中，不在这里复制或重建平行账本。</p>
 *
 * @author APS
 */
public class DayDrivenScheduleState {

    /**
     * 动态候选使用的 S4.5 全局顺序比较器。
     *
     * <p>存在有效 sortRank 的候选按名次升序；没有全局名次的运行时补偿候选稳定追加在
     * 全部有名次候选之后。相同名次返回相等，依赖 Java 稳定排序保留原登记顺序。</p>
     */
    private static final Comparator<SkuScheduleDTO> GLOBAL_ORDER_COMPARATOR =
            (leftSku, rightSku) -> compareByGlobalOrder(leftSku, rightSku);

    /** S4.5 既有排序完成后的稳定 SKU 顺序 */
    private final List<SkuScheduleDTO> orderedSkuList;
    /** 仍需在本窗口后续业务日参与编排的 SKU，使用对象身份隔离补偿副本 */
    private final Set<SkuScheduleDTO> pendingSkuSet =
            Collections.newSetFromMap(new IdentityHashMap<SkuScheduleDTO, Boolean>());
    /** 已完成业务目标的 SKU */
    private final Set<SkuScheduleDTO> completedSkuSet =
            Collections.newSetFromMap(new IdentityHashMap<SkuScheduleDTO, Boolean>());
    /** 已经写入最终未排的 SKU */
    private final Set<SkuScheduleDTO> finalUnscheduledSkuSet =
            Collections.newSetFromMap(new IdentityHashMap<SkuScheduleDTO, Boolean>());
    /** 当前保留的延期任务，使用 SKU 对象身份避免同物料补偿副本互相覆盖 */
    private final Map<SkuScheduleDTO, DeferredScheduleTask> deferredTaskMap =
            new IdentityHashMap<SkuScheduleDTO, DeferredScheduleTask>();
    /** 机台编码到跨日在机绑定；单控整机的两侧编码会指向同一个绑定对象 */
    private final Map<String, ActiveMachineBinding> activeMachineBindingMap =
            new LinkedHashMap<String, ActiveMachineBinding>();
    /** 当前业务日各 SKU 的执行结果 */
    private final Map<SkuScheduleDTO, SkuDayScheduleOutcome> currentDayOutcomeMap =
            new IdentityHashMap<SkuScheduleDTO, SkuDayScheduleOutcome>();
    /**
     * 尚未形成实际命中的最后一次选机诊断快照。
     *
     * <p>快照跨业务日保留，只在三天窗口最终未排时写一次汇总日志；
     * 中间的换模、首检、产能或 dayN 失败不会直接写选机优先级日志。</p>
     */
    private final Map<SkuScheduleDTO, MachinePriorityTraceSnapshot> pendingMachinePriorityTraceMap =
            new IdentityHashMap<SkuScheduleDTO, MachinePriorityTraceSnapshot>();
    /** 本窗口已经写过实际命中选机日志的 SKU，避免部分成功后再写“最终未命中”误导信息。 */
    private final Set<SkuScheduleDTO> machinePriorityTraceHitSkuSet =
            Collections.newSetFromMap(new IdentityHashMap<SkuScheduleDTO, Boolean>());

    /**
     * 使用已经完成现有业务排序的新增 SKU 列表初始化状态。
     *
     * @param orderedSkuList S4.5 排序后的 SKU 列表
     */
    public DayDrivenScheduleState(List<SkuScheduleDTO> orderedSkuList) {
        List<SkuScheduleDTO> sourceList = orderedSkuList == null
                ? Collections.<SkuScheduleDTO>emptyList() : orderedSkuList;
        this.orderedSkuList = new ArrayList<SkuScheduleDTO>(sourceList);
        this.pendingSkuSet.addAll(sourceList);
    }

    /**
     * 登记排程过程中动态生成的补偿 SKU。
     *
     * <p>动态补偿 SKU 若携带 S4.5 sortRank，则插入对应全局名次；没有有效名次时稳定追加队尾。
     * 已经完成资源分配的候选不会回滚，本方法只调整尚未执行候选及后续业务日的顺序。</p>
     *
     * @param sku 动态补偿 SKU
     */
    public void registerPendingSku(SkuScheduleDTO sku) {
        if (Objects.isNull(sku)) {
            return;
        }
        if (!containsSkuByIdentity(orderedSkuList, sku)) {
            int insertIndex = orderedSkuList.size();
            for (int index = 0; index < orderedSkuList.size(); index++) {
                if (GLOBAL_ORDER_COMPARATOR.compare(sku, orderedSkuList.get(index)) < 0) {
                    insertIndex = index;
                    break;
                }
            }
            orderedSkuList.add(insertIndex, sku);
        }
        pendingSkuSet.add(sku);
    }

    /**
     * 将运行中生成的补偿 SKU 按 S4.5 全局顺序稳定合并到当前剩余工作队列。
     *
     * <p>该方法只处理“下一轮尚未执行”的队列：有名次补偿按 sortRank 与剩余候选竞争，
     * 无名次补偿排在全部有名次候选之后；相同名次和多个无名次候选均保持生成顺序。</p>
     *
     * @param pendingSkuList 当前轮尚未执行的工作队列
     * @param additionalSkuList 本轮动态生成的补偿 SKU
     */
    public void mergePendingSkuListByGlobalOrder(List<SkuScheduleDTO> pendingSkuList,
                                                 List<SkuScheduleDTO> additionalSkuList) {
        if (Objects.isNull(pendingSkuList) || Objects.isNull(additionalSkuList)
                || additionalSkuList.isEmpty()) {
            return;
        }
        for (SkuScheduleDTO additionalSku : additionalSkuList) {
            if (Objects.isNull(additionalSku)) {
                continue;
            }
            this.registerPendingSku(additionalSku);
            if (!containsSkuByIdentity(pendingSkuList, additionalSku)) {
                pendingSkuList.add(additionalSku);
            }
        }
        Collections.sort(pendingSkuList, GLOBAL_ORDER_COMPARATOR);
    }

    /**
     * 开始新的业务日，清空只在当日有效的执行结果。
     */
    public void beginDay() {
        currentDayOutcomeMap.clear();
    }

    /**
     * 标记 SKU 延期到下一业务日。
     *
     * @param task 延期任务
     */
    public void defer(DeferredScheduleTask task) {
        if (Objects.isNull(task)) {
            return;
        }
        SkuScheduleDTO sku = task.getSku();
        pendingSkuSet.add(sku);
        deferredTaskMap.put(sku, task);
        currentDayOutcomeMap.put(sku, SkuDayScheduleOutcome.DEFER_TO_NEXT_DAY);
    }

    /**
     * 清理当前 SKU 的历史延期记录。
     *
     * @param sku SKU
     */
    public void clearDeferredTask(SkuScheduleDTO sku) {
        deferredTaskMap.remove(sku);
    }

    /**
     * 标记 SKU 已完成，不再进入后续日候选池。
     *
     * @param sku SKU
     */
    public void complete(SkuScheduleDTO sku) {
        pendingSkuSet.remove(sku);
        completedSkuSet.add(sku);
        deferredTaskMap.remove(sku);
        pendingMachinePriorityTraceMap.remove(sku);
        currentDayOutcomeMap.put(sku, SkuDayScheduleOutcome.COMPLETED);
    }

    /**
     * 标记 SKU 已写最终未排，不再进入后续日候选池。
     *
     * @param sku SKU
     */
    public void finalizeUnscheduled(SkuScheduleDTO sku) {
        pendingSkuSet.remove(sku);
        finalUnscheduledSkuSet.add(sku);
        deferredTaskMap.remove(sku);
        pendingMachinePriorityTraceMap.remove(sku);
        currentDayOutcomeMap.put(sku, SkuDayScheduleOutcome.FINAL_UNSCHEDULED);
    }

    /**
     * 记录当前业务日已经形成有效排产并需要跨日延续。
     *
     * @param sku SKU
     */
    public void markScheduledAndCarryOver(SkuScheduleDTO sku) {
        pendingSkuSet.add(sku);
        deferredTaskMap.remove(sku);
        currentDayOutcomeMap.put(sku, SkuDayScheduleOutcome.SCHEDULED_AND_CARRY_OVER);
    }

    /**
     * 保存当前 SKU 最后一次尚未命中的选机诊断快照。
     *
     * <p>同一 SKU 已经形成过实际命中时，不再保留后续“无需继续扩机”或失败尝试，
     * 防止窗口收口时把部分成功 SKU 错记成完全未命中。</p>
     *
     * @param sku 当前 SKU
     * @param traceSnapshot 当前选机时点的只读诊断快照
     */
    public void rememberPendingMachinePriorityTrace(
            SkuScheduleDTO sku,
            MachinePriorityTraceSnapshot traceSnapshot) {
        if (Objects.isNull(sku) || Objects.isNull(traceSnapshot)
                || machinePriorityTraceHitSkuSet.contains(sku)) {
            return;
        }
        pendingMachinePriorityTraceMap.put(sku, traceSnapshot);
    }

    /**
     * 标记当前 SKU 已经写入实际命中选机日志。
     *
     * @param sku 当前 SKU
     */
    public void markMachinePriorityTraceHit(SkuScheduleDTO sku) {
        if (Objects.isNull(sku)) {
            return;
        }
        machinePriorityTraceHitSkuSet.add(sku);
        pendingMachinePriorityTraceMap.remove(sku);
    }

    /**
     * 获取三天窗口内最后一次尚未命中的选机诊断快照。
     *
     * @param sku 当前 SKU
     * @return 最后一次诊断快照；没有进入选机或已经实际命中时为空
     */
    public MachinePriorityTraceSnapshot getPendingMachinePriorityTrace(SkuScheduleDTO sku) {
        return pendingMachinePriorityTraceMap.get(sku);
    }

    /**
     * 清理不会形成新增实际命中的中间诊断快照。
     *
     * <p>例如已有同物料结果已满足 dayN、当前新增候选无需再扩机时，
     * 该候选既不是失败，也不是新增命中，不应在最终窗口写选机日志。</p>
     *
     * @param sku 当前 SKU
     */
    public void clearPendingMachinePriorityTrace(SkuScheduleDTO sku) {
        pendingMachinePriorityTraceMap.remove(sku);
    }

    /**
     * 记录当前业务日未形成新增量，但 SKU 仍保留后续资格。
     *
     * @param sku SKU
     */
    public void markNoProgressToday(SkuScheduleDTO sku) {
        pendingSkuSet.add(sku);
        currentDayOutcomeMap.put(sku, SkuDayScheduleOutcome.NO_PROGRESS_TODAY);
    }

    /**
     * 登记或替换机台跨日在机绑定。
     *
     * <p>新 SKU 真正形成有效结果后才调用。若机台原先存在其他绑定，说明现有主链已经完成换产，
     * 旧绑定随真实机台状态一起被替换。</p>
     *
     * @param binding 新在机绑定
     */
    public void registerBinding(ActiveMachineBinding binding) {
        if (Objects.isNull(binding) || binding.getMachineCode() == null) {
            return;
        }
        removeBindingByMachineCode(binding.getMachineCode());
        if (binding.getPairMachineCode() != null) {
            removeBindingByMachineCode(binding.getPairMachineCode());
        }
        activeMachineBindingMap.put(binding.getMachineCode(), binding);
        if (binding.getPairMachineCode() != null) {
            activeMachineBindingMap.put(binding.getPairMachineCode(), binding);
        }
    }

    /**
     * 按机台编码移除整组跨日在机绑定。
     *
     * @param machineCode 主机台或配对侧机台编码
     */
    public void removeBindingByMachineCode(String machineCode) {
        ActiveMachineBinding binding = activeMachineBindingMap.get(machineCode);
        if (Objects.nonNull(binding)) {
            removeBinding(binding);
        }
    }

    /**
     * 移除指定绑定及其主副机台索引。
     *
     * @param binding 在机绑定
     */
    public void removeBinding(ActiveMachineBinding binding) {
        if (Objects.isNull(binding)) {
            return;
        }
        activeMachineBindingMap.entrySet().removeIf(entry -> entry.getValue() == binding);
    }

    /**
     * 移除指定 SKU 的全部跨日在机绑定。
     *
     * @param sku SKU
     */
    public void removeBindingsBySku(SkuScheduleDTO sku) {
        if (Objects.isNull(sku)) {
            return;
        }
        List<ActiveMachineBinding> bindingList = findBindingsBySku(sku);
        for (ActiveMachineBinding binding : bindingList) {
            removeBinding(binding);
        }
    }

    /**
     * 获取去重后的全部在机绑定，保持首次登记顺序。
     *
     * @return 在机绑定列表
     */
    public List<ActiveMachineBinding> getActiveBindings() {
        return new ArrayList<ActiveMachineBinding>(
                new LinkedHashSet<ActiveMachineBinding>(activeMachineBindingMap.values()));
    }

    /**
     * 判断 SKU 是否已经存在跨日在机绑定。
     *
     * @param sku SKU
     * @return true-至少存在一台绑定机台
     */
    public boolean hasActiveBinding(SkuScheduleDTO sku) {
        if (Objects.isNull(sku)) {
            return false;
        }
        for (ActiveMachineBinding binding : activeMachineBindingMap.values()) {
            if (binding.getSku() == sku) {
                return true;
            }
        }
        return false;
    }

    /**
     * 获取指定 SKU 的去重在机绑定。
     *
     * @param sku SKU
     * @return 在机绑定列表
     */
    public List<ActiveMachineBinding> findBindingsBySku(SkuScheduleDTO sku) {
        List<ActiveMachineBinding> bindingList = new ArrayList<ActiveMachineBinding>(2);
        Set<ActiveMachineBinding> visitedSet =
                Collections.newSetFromMap(new IdentityHashMap<ActiveMachineBinding, Boolean>());
        for (ActiveMachineBinding binding : activeMachineBindingMap.values()) {
            if (binding.getSku() == sku && visitedSet.add(binding)) {
                bindingList.add(binding);
            }
        }
        return bindingList;
    }

    /**
     * 获取指定 SKU 已经形成有效结果的绑定机台编码。
     *
     * <p>普通机台返回主机台编码；单控整机同时返回左右两侧编码。调用方使用该集合把已在机机台
     * 从“新选机”候选中剔除，保证同一绑定只能通过延续阶段追加班次，不能再次换模并生成新结果。</p>
     *
     * @param sku SKU
     * @return 按绑定登记顺序返回的机台编码集合
     */
    public Set<String> getBoundMachineCodesBySku(SkuScheduleDTO sku) {
        Set<String> machineCodeSet = new LinkedHashSet<String>();
        for (ActiveMachineBinding binding : findBindingsBySku(sku)) {
            if (binding.getMachineCode() != null) {
                machineCodeSet.add(binding.getMachineCode());
            }
            if (binding.getPairMachineCode() != null) {
                machineCodeSet.add(binding.getPairMachineCode());
            }
        }
        return machineCodeSet;
    }

    /**
     * 判断指定 SKU 是否仍保留非收尾的物理在机绑定。
     *
     * <p>非收尾结果达到当前日目标只表示本日账本已满足，不表示机台已经下机。该状态供日终
     * 收口和下一业务日延续阶段共同使用，避免把同一物料在原机台再次当作新增换产。</p>
     *
     * @param sku SKU
     * @return true-至少存在一个非收尾在机绑定
     */
    public boolean hasNonEndingBinding(SkuScheduleDTO sku) {
        for (ActiveMachineBinding binding : findBindingsBySku(sku)) {
            if (!binding.isEndingTarget()) {
                return true;
            }
        }
        return false;
    }

    public List<SkuScheduleDTO> getOrderedSkuList() {
        return Collections.unmodifiableList(orderedSkuList);
    }

    public boolean isPending(SkuScheduleDTO sku) {
        return pendingSkuSet.contains(sku);
    }

    public boolean isCompleted(SkuScheduleDTO sku) {
        return completedSkuSet.contains(sku);
    }

    public boolean isFinalUnscheduled(SkuScheduleDTO sku) {
        return finalUnscheduledSkuSet.contains(sku);
    }

    public DeferredScheduleTask getDeferredTask(SkuScheduleDTO sku) {
        return deferredTaskMap.get(sku);
    }

    public SkuDayScheduleOutcome getCurrentDayOutcome(SkuScheduleDTO sku) {
        return currentDayOutcomeMap.get(sku);
    }

    public List<SkuScheduleDTO> getPendingSkuListInOriginalOrder() {
        List<SkuScheduleDTO> resultList = new ArrayList<SkuScheduleDTO>(pendingSkuSet.size());
        for (SkuScheduleDTO sku : orderedSkuList) {
            if (pendingSkuSet.contains(sku)) {
                resultList.add(sku);
            }
        }
        return resultList;
    }

    /**
     * 获取当前延期任务数。
     *
     * @return 延期任务数
     */
    public int getDeferredTaskCount() {
        return deferredTaskMap.size();
    }

    private boolean containsSkuByIdentity(List<SkuScheduleDTO> skuList, SkuScheduleDTO targetSku) {
        for (SkuScheduleDTO sku : skuList) {
            if (sku == targetSku) {
                return true;
            }
        }
        return false;
    }

    /**
     * 比较两个候选的 S4.5 全局名次。
     *
     * @param leftSku 左侧候选
     * @param rightSku 右侧候选
     * @return 负数-左侧优先；正数-右侧优先；0-保持原相对顺序
     */
    private static int compareByGlobalOrder(SkuScheduleDTO leftSku, SkuScheduleDTO rightSku) {
        if (leftSku == rightSku) {
            return 0;
        }
        if (Objects.isNull(leftSku)) {
            return 1;
        }
        if (Objects.isNull(rightSku)) {
            return -1;
        }
        int leftRank = leftSku.getSortRank();
        int rightRank = rightSku.getSortRank();
        boolean leftRanked = leftRank > 0;
        boolean rightRanked = rightRank > 0;
        if (leftRanked && rightRanked) {
            return Integer.compare(leftRank, rightRank);
        }
        if (leftRanked) {
            return -1;
        }
        if (rightRanked) {
            return 1;
        }
        return 0;
    }
}
