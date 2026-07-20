package com.zlt.aps.gsq.engine.strategy;

import com.zlt.aps.gsq.api.domain.entity.GsqMachineInfo;
import com.zlt.aps.gsq.engine.context.GsqScheduleContext;
import com.zlt.aps.gsq.engine.vo.GsqScheduleResultVo;

import java.util.List;

/**
 * 钢丝圈机台过滤策略接口。
 *
 * <p>策略链按以下顺序执行（顺序不可调换）：</p>
 * <ol>
 *   <li>SpecifyMachineFilter - 定点机台过滤</li>
 *   <li>InchRangeFilter - 寸口范围过滤</li>
 *   <li>WireDiameterFilter - 钢丝直径过滤（钢丝圈独有）</li>
 *   <li>ProductionLineFilter - 产线固定规则过滤（钢丝圈独有）</li>
 *   <li>MaintenanceFilter - 检修计划过滤</li>
 *   <li>ClassAvailabilityFilter - 班次可用状态过滤</li>
 * </ol>
 *
 * @author APS
 */
public interface IMachineFilterStrategy {

    /**
     * 过滤机台列表，返回符合当前策略的机台子集。
     *
     * @param machines    待过滤的机台列表
     * @param scheduleVo  当前排程记录（用于读取钢丝圈代码、英寸、班次等）
     * @param context     排程上下文（用于读取检修计划、产线规则等全局数据）
     * @return 过滤后的机台列表
     */
    List<GsqMachineInfo> filter(List<GsqMachineInfo> machines,
                                 GsqScheduleResultVo scheduleVo,
                                 GsqScheduleContext context);

    /**
     * 获取策略执行顺序，数字越小越先执行。
     *
     * @return 执行顺序
     */
    int getOrder();

    /**
     * 获取策略名称，用于日志输出。
     *
     * @return 策略名称
     */
    String getName();
}
