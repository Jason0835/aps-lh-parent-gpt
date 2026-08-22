package com.zlt.aps.tm.engine.service.impl;

import cn.hutool.core.util.StrUtil;
import com.zlt.aps.tm.engine.domain.TmTaskDraft;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * 胎面机台分配任务固定进入顺序选择器。
 *
 * <p>该类只负责以基础排序序号和业务键确定稳定顺序，不读取上下文、不写规则证据，
 * 机台分配服务继续负责分配时机、日志和异常传播。</p>
 */
final class TmMachineAssignmentTaskOrderSelector {

    /**
     * 原地按机台分配固定顺序排序任务。
     *
     * @param taskList 待排序任务列表
     */
    void sort(List<TmTaskDraft> taskList) {
        if (taskList == null) {
            return;
        }
        taskList.sort(this::compare);
    }

    /**
     * 从已排序任务队列读取首个待分配任务。
     *
     * @param taskList 已按固定顺序排序的任务列表
     * @return 排序最靠前的任务
     * @throws IllegalArgumentException 任务列表为空时抛出
     */
    TmTaskDraft selectFirst(List<TmTaskDraft> taskList) {
        if (taskList == null || taskList.isEmpty() || taskList.get(0) == null) {
            throw new IllegalArgumentException("机台分配任务列表不能为空");
        }
        return taskList.get(0);
    }

    /**
     * 从未排序任务中按固定规则选择首个任务，供兼容调用和单元测试使用。
     *
     * @param taskList 待选择任务列表
     * @return 排序最靠前的任务
     * @throws IllegalArgumentException 任务列表为空时抛出
     */
    TmTaskDraft selectMinimum(List<TmTaskDraft> taskList) {
        if (taskList == null) {
            throw new IllegalArgumentException("机台分配任务列表不能为空");
        }
        return taskList.stream()
                .filter(Objects::nonNull)
                .min(this::compare)
                .orElseThrow(() -> new IllegalArgumentException("机台分配任务列表不能为空"));
    }

    /**
     * 比较机台分配任务的固定进入顺序。
     *
     * @param firstTask 第一个任务
     * @param secondTask 第二个任务
     * @return 基础排序序号及业务键的比较结果
     */
    private int compare(TmTaskDraft firstTask, TmTaskDraft secondTask) {
        return Comparator.comparing(TmTaskDraft::getBaseSortIndex,
                        Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(task -> StrUtil.blankToDefault(task.getBusinessKey(), ""))
                .compare(firstTask, secondTask);
    }
}
