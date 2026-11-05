package com.zlt.aps.common.engine.schedule;

import java.time.LocalDate;
import java.util.*;
import java.util.function.Function;

/**
 * 机台班次任务链集合。
 *
 * <p>按机台、排程日期和班次顺序管理多条任务链，用于自动排程、人工调整和局部重算快速定位
 * 需要修改的链表。该类只维护内存态链表，不写数据库。</p>
 *
 * @param <T> 链表节点承载的业务任务对象类型
 */
public class MachineShiftTaskChain<T> {

    /**
     * 机台班次链表集合，key=machineCode|scheduleDate|shiftOrder
     */
    private final Map<String, ScheduleTaskLinkedList<T>> chainMap = new HashMap<>();

    /**
     * 获取或创建指定机台班次链表。
     *
     * @param machineCode  机台编码
     * @param scheduleDate 排程日期
     * @param shiftOrder   班次顺序
     * @return 指定机台班次的任务链
     */
    public ScheduleTaskLinkedList<T> getOrCreate(String machineCode, LocalDate scheduleDate, Integer shiftOrder) {
        String key = buildKey(machineCode, scheduleDate, shiftOrder);
        ScheduleTaskLinkedList<T> chain = chainMap.get(key);
        if (chain == null) {
            chain = new ScheduleTaskLinkedList<>();
            chainMap.put(key, chain);
        }
        return chain;
    }

    /**
     * 获取或创建指定机台全天任务链（不按班次分组）。
     *
     * <p>用于胎圈等"一个机台一个链"的业务场景，链内按班次顺序串联。
     * 与三参数版本的区别：用空 shiftOrder 作为键，避免和胎侧按班次分链冲突。</p>
     *
     * @param machineCode  机台编码
     * @param scheduleDate 排程日期
     * @return 指定机台的全天任务链
     */
    public ScheduleTaskLinkedList<T> getOrCreate(String machineCode, LocalDate scheduleDate) {
        return getOrCreate(machineCode, scheduleDate, null);
    }

    /**
     * 读取已存在的指定机台班次链表。
     *
     * @param machineCode  机台编码
     * @param scheduleDate 排程日期
     * @param shiftOrder   班次顺序
     * @return 已存在任务链；不存在时返回空
     */
    public ScheduleTaskLinkedList<T> get(String machineCode, LocalDate scheduleDate, Integer shiftOrder) {
        return chainMap.get(buildKey(machineCode, scheduleDate, shiftOrder));
    }

    /**
     * 按影响范围返回需要重算的链表。
     *
     * @param scope 影响范围，包含机台集合、排程日期和班次窗口
     * @return 需要重算的任务链列表
     */
    public List<ScheduleTaskLinkedList<T>> listAffectedChains(ImpactScope scope) {
        List<ScheduleTaskLinkedList<T>> result = new ArrayList<>();
        if (scope == null) {
            return result;
        }
        for (Map.Entry<String, ScheduleTaskLinkedList<T>> entry : chainMap.entrySet()) {
            if (scope.matches(entry.getKey())) {
                result.add(entry.getValue());
            }
        }
        return result;
    }

    /**
     * 返回所有已加载的任务链集合。
     *
     * @return 所有任务链的不可修改集合视图
     */
    public java.util.Collection<ScheduleTaskLinkedList<T>> values() {
        return java.util.Collections.unmodifiableCollection(chainMap.values());
    }

    /**
     * 清空所有机台班次任务链。
     *
     * <p>Phase 5 重构新增：用于胎圈自动排程重新构建任务链前清空旧数据，避免重复加载。
     * 该方法只清空 {@link #chainMap}，不涉及节点索引（节点索引由调用方自己清空）。</p>
     */
    public void clear() {
        chainMap.clear();
    }

    /**
     * 复制链表结构并替换节点任务引用，保留班次、顺序和预计起止时间。
     *
     * <p>拷贝按源链顺序逐节点静默追加，不逐节点重排整条链。原实现每次追加都会重新编号整条链，
     * 拷贝数百个节点时是平方级开销，而变更结果在本方法内无人消费；静默追加按链内位置写入序号，
     * 与重排后的最终序号完全一致。</p>
     *
     * @param taskCopier       节点任务复制器
     * @param operationContext 保留参数以兼容既有调用方；拷贝路径不再产生变更结果
     * @param <R>              复制后任务类型
     * @return 仅引用副本任务的新任务链集合
     */
    public <R> MachineShiftTaskChain<R> copy(Function<T, R> taskCopier,
                                             ScheduleOperationContext operationContext) {
        MachineShiftTaskChain<R> copiedGroup = new MachineShiftTaskChain<>();
        for (ScheduleTaskLinkedList<T> sourceChain : this.chainMap.values()) {
            for (ScheduleTaskNode<T> sourceNode : sourceChain.toList()) {
                ScheduleTaskNode<R> copiedNode = new ScheduleTaskNode<>(sourceNode.getTaskId(),
                        sourceNode.getTask() == null ? null : taskCopier.apply(sourceNode.getTask()),
                        sourceNode.getMachineCode(), sourceNode.getScheduleDate(), sourceNode.getShiftCode(),
                        sourceNode.getShiftOrder(), sourceNode.getPlanQty());
                copiedNode.setSequence(sourceNode.getSequence());
                copiedNode.setStartTime(copyDate(sourceNode.getStartTime()));
                copiedNode.setEndTime(copyDate(sourceNode.getEndTime()));
                copiedGroup.getOrCreate(sourceNode.getMachineCode(), sourceNode.getScheduleDate(),
                        sourceNode.getShiftOrder()).appendSilently(copiedNode);
            }
        }
        return copiedGroup;
    }

    /**
     * 复制可变日期，避免工作区重算时间时共享日期实例。
     */
    private Date copyDate(Date source) {
        return source == null ? null : new Date(source.getTime());
    }

    /**
     * 返回已加载任务链数量。
     *
     * @return 任务链数量
     */
    public int size() {
        return chainMap.size();
    }

    /**
     * 构建机台班次链表键。
     *
     * @param machineCode  机台编码
     * @param scheduleDate 排程日期
     * @param shiftOrder   班次顺序
     * @return 链表键
     */
    private String buildKey(String machineCode, LocalDate scheduleDate, Integer shiftOrder) {
        return machineCode + "|" + scheduleDate + "|" + shiftOrder;
    }
}
