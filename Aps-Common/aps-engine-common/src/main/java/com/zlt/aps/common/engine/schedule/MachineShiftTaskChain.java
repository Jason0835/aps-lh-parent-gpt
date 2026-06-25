package com.zlt.aps.common.engine.schedule;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
