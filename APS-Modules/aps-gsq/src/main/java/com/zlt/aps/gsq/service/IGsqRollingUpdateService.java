package com.zlt.aps.gsq.service;

import com.zlt.aps.gsq.engine.vo.GsqRollingUpdateResult;

import java.util.Date;

/**
 * 钢丝圈排程滚动更新Service接口
 *
 * <p>提供手动触发与自动定时触发滚动更新的能力，覆盖以下场景：</p>
 * <ul>
 *   <li>插单/调量/转机台/删除后触发同班次内时间重算（manualRollingUpdate）</li>
 *   <li>4类业务场景标准化触发入口（triggerByInsertOrder / triggerByChangeMachine /
 *       triggerByChangeQty / triggerByDelete），自动处理新增/删除任务的顺序±1重算</li>
 *   <li>库存与计划调整算法（adjustPlanByStock），按预计库存与1班/3班需求量关系修正下个班计划</li>
 *   <li>自动定时触发入口（autoRollingUpdate），供定时任务在班次开始前N分钟调用</li>
 *   <li>失败告警与手动补偿触发入口（manualCompensateRolling）</li>
 * </ul>
 *
 * @author APS
 */
public interface IGsqRollingUpdateService {

    /**
     * 手动触发滚动更新（基础入口）
     *
     * <p>分布式锁策略：</p>
     * <ul>
     *   <li>锁Key：GSQ:ROLLING:{排程日期}</li>
     *   <li>waitTime=3s：手动操作允许短暂等待</li>
     *   <li>leaseTime=60s：足够完成单次滚动</li>
     * </ul>
     *
     * @param triggerType     触发类型：1-插单，2-转机台，3-调量，4-删除
     * @param triggerSourceId 触发源排程记录ID
     * @param scheduleDate    排程日期
     * @param shiftIndex      触发班次索引（1~6）
     * @param machineCode     触发机台编号
     * @param steelRingCode   触发钢丝圈代码
     * @return 滚动更新结果
     */
    GsqRollingUpdateResult manualRollingUpdate(String triggerType, Long triggerSourceId,
                                                Date scheduleDate, int shiftIndex,
                                                String machineCode, String steelRingCode);

    /**
     * 插单场景标准化触发入口
     *
     * <p>业务流程：</p>
     * <ol>
     *   <li>按触发源记录的6班次计划量识别受影响班次</li>
     *   <li>对每个受影响班次：将新增任务按排产次序插入任务链指定位置</li>
     *   <li>遍历并更新新增任务所有后续节点的生产顺序（原生产顺序 + 1）</li>
     *   <li>重新计算并更新所有后续节点的预计开始/结束时间</li>
     *   <li>跨班次超时任务自动推迟到下个班</li>
     * </ol>
     *
     * @param triggerSourceId 触发源排程记录ID（即新增的插单记录ID）
     * @param scheduleDate    排程日期
     * @param machineCode     机台编号
     * @param steelRingCode   钢丝圈代码
     * @return 滚动更新结果
     */
    GsqRollingUpdateResult triggerByInsertOrder(Long triggerSourceId, Date scheduleDate,
                                                 String machineCode, String steelRingCode);

    /**
     * 转机台场景标准化触发入口
     *
     * <p>业务流程：原机台和新机台都需要重新计算同班次内时间，原机台视为"删除"，
     * 新机台视为"新增"，分别执行顺序调整与时间重算。</p>
     *
     * @param triggerSourceId 触发源排程记录ID
     * @param scheduleDate    排程日期
     * @param oldMachineCode  原机台编号
     * @param newMachineCode  新机台编号
     * @param steelRingCode   钢丝圈代码
     * @return 滚动更新结果
     */
    GsqRollingUpdateResult triggerByChangeMachine(Long triggerSourceId, Date scheduleDate,
                                                   String oldMachineCode, String newMachineCode,
                                                   String steelRingCode);

    /**
     * 调量场景标准化触发入口
     *
     * <p>业务流程：调量不改变任务顺序，仅按调整后的计划量重新计算所有后续节点的
     * 预计开始/结束时间，并处理跨班次超时任务。</p>
     *
     * @param triggerSourceId 触发源排程记录ID
     * @param scheduleDate    排程日期
     * @param machineCode     机台编号
     * @param steelRingCode   钢丝圈代码
     * @return 滚动更新结果
     */
    GsqRollingUpdateResult triggerByChangeQty(Long triggerSourceId, Date scheduleDate,
                                               String machineCode, String steelRingCode);

    /**
     * 删除场景标准化触发入口
     *
     * <p>业务流程：</p>
     * <ol>
     *   <li>从任务链中移除待删除任务</li>
     *   <li>遍历并更新删除任务所有后续节点的生产顺序（原生产顺序 - 1）</li>
     *   <li>重新计算并更新所有后续节点的预计开始/结束时间</li>
     *   <li>跨班次超时任务自动推迟到下个班</li>
     * </ol>
     *
     * @param triggerSourceId 触发源排程记录ID（即被逻辑删除的记录ID）
     * @param scheduleDate    排程日期
     * @param machineCode     机台编号
     * @param steelRingCode   钢丝圈代码
     * @return 滚动更新结果
     */
    GsqRollingUpdateResult triggerByDelete(Long triggerSourceId, Date scheduleDate,
                                            String machineCode, String steelRingCode);

    /**
     * 库存与计划调整算法
     *
     * <p>精确实现库存与计划调整计算公式：</p>
     * <ul>
     *   <li>条件A（库存不足）：当 预计库存 + 下个班原计划 &lt; 一个班需求量 时，
     *       将下个班计划修正为 (一个班需求量 - 预计库存)</li>
     *   <li>条件B（库存积压）：当 预计库存 + 下个班原计划 &gt; 一个班需求量，
     *       且超出 3个班库存阈值（可配置参数 GSQ_ROLLING_STOCK_THRESHOLD_CLASSES，默认3） 时，
     *       将下个班计划修正为 (3个班需求量 - 预计库存)</li>
     *   <li>预计库存来源：T_GSQ_STOCK 当日库存 + 当日至下个班开始前的累计净增量</li>
     *   <li>需求量来源：TQ_CLASS1~6_PLAN 字段（对应胎圈班次消耗量）</li>
     * </ul>
     *
     * @param scheduleDate     排程日期
     * @param steelRingCode    钢丝圈代码
     * @param targetShiftIndex 目标班次索引（1~6），即待修正的"下个班"
     * @return 滚动更新结果（含调整前后的库存与计划量）
     */
    GsqRollingUpdateResult adjustPlanByStock(Date scheduleDate, String steelRingCode, int targetShiftIndex);

    /**
     * 自动定时触发滚动更新（供定时任务调用）
     *
     * <p>触发时机：每个生产班次开始前30分钟（可配置 GSQ_ROLLING_AUTO_TRIGGER_LEAD_MINUTES）。
     * 自动触发会先刷新本地库存缓存（调用MES同步接口），再对目标班次的全部机台执行滚动更新，
     * 并应用库存与计划调整算法。</p>
     *
     * <p>失败处理：自动触发失败时记录失败日志并发送系统告警，支持手动补偿触发。</p>
     *
     * @param scheduleDate 排程日期
     * @param shiftIndex   目标班次索引（1~6）
     * @param factoryCode  分厂编码
     * @return 滚动更新结果
     */
    GsqRollingUpdateResult autoRollingUpdate(Date scheduleDate, int shiftIndex, String factoryCode);

    /**
     * 手动补偿触发（自动触发失败后的人工补偿入口）
     *
     * <p>与 autoRollingUpdate 等价，但触发类型标记为"手动补偿"，跳过MES库存同步步骤
     * （假设库存已被自动任务刷新或人工确认）。</p>
     *
     * @param scheduleDate 排程日期
     * @param shiftIndex   目标班次索引（1~6）
     * @param factoryCode  分厂编码
     * @return 滚动更新结果
     */
    GsqRollingUpdateResult manualCompensateRolling(Date scheduleDate, int shiftIndex, String factoryCode);
}
