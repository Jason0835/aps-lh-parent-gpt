package com.zlt.aps.tq.engine.strategy;

import com.zlt.aps.tq.api.domain.entity.TqMachineInfo;
import com.zlt.aps.tq.engine.context.TqScheduleContext;
import com.zlt.aps.tq.engine.vo.TqScheduleResultVo;

import java.util.List;

/**
 * 机台过滤策略接口。
 *
 * <p>在S3（机台分配与排序）阶段，通过策略链模式对候选机台进行多维度过滤。
 * 每个策略实现一种过滤维度，按优先级顺序执行。</p>
 *
 * <p>当前实现的策略链顺序：</p>
 * <ol>
 *   <li>SpecifyMachineFilter - 定点机台过滤（限制作业优先）</li>
 *   <li>MouthPlateFilter - 口型板过滤</li>
 *   <li>InchRangeFilter - 寸口范围过滤</li>
 *   <li>MaintenanceFilter - 维修计划过滤</li>
 * </ol>
 *
 * @author APS
 */
public interface IMachineFilterStrategy {

    /**
     * 过滤候选机台列表。
     *
     * <p>从全部机台列表中，根据当前排程记录和上下文数据，
     * 过滤出符合条件的机台子集。</p>
     *
     * @param candidateMachines 当前候选机台列表（可能已被上游策略过滤过）
     * @param scheduleVo        当前排程记录
     * @param context           排程上下文
     * @return 过滤后的机台列表
     */
    List<TqMachineInfo> filter(List<TqMachineInfo> candidateMachines, TqScheduleResultVo scheduleVo, TqScheduleContext context);

    /**
     * 获取策略优先级（数值越小优先级越高）。
     *
     * @return 优先级数值
     */
    int getOrder();

    /**
     * 获取策略名称，用于日志输出。
     *
     * @return 策略名称
     */
    String getStrategyName();
}
