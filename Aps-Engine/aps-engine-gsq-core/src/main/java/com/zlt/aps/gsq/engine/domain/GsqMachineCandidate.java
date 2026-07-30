package com.zlt.aps.gsq.engine.domain;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 钢丝圈候选机台追踪对象。
 *
 * <p>Phase 3 重构新增：对齐胎圈 {@code TqMachineCandidate}，用于在 S3 机台分配阶段记录每个候选机台的
 * 过滤状态、过滤原因、剩余产能和选中评分。该对象不修改机台基础数据，只承载分配过程的运行态信息。</p>
 *
 * <p>与胎圈的差异：</p>
 * <ul>
 *   <li>胎圈评分维度：已排同规格优先 → 剩余产能升序 → 机台编号</li>
 *   <li>钢丝圈评分维度：任务链最短优先（负载均衡） → 机台编号</li>
 *   <li>胎圈有定点机台映射（plannedMachineMap），钢丝圈有定点/产线/钢丝直径等更多过滤维度</li>
 * </ul>
 *
 * <p>使用方式：</p>
 * <ol>
 *   <li>S3 阶段 {@code GsqMachineAssignHandler} 创建候选列表</li>
 *   <li>策略链过滤时，被过滤的机台标记 {@code filtered=true} 并记录过滤原因</li>
 *   <li>通过过滤的机台按任务链长度评分，选中机台标记 {@code selected=true}</li>
 *   <li>候选列表写入 {@code context.candidateTraceMap}，供解释快照使用</li>
 * </ol>
 *
 * @author APS
 */
@Data
public class GsqMachineCandidate {

    /** 机台编码 */
    private String machineCode;

    /** 机台名称（便于日志和解释输出） */
    private String machineName;

    /** 是否被过滤 */
    private boolean filtered;

    /** 过滤策略名（被哪个策略过滤） */
    private String filterStrategy;

    /** 过滤原因编码 */
    private String filterReasonCode;

    /** 过滤原因描述 */
    private String filterReasonDesc;

    /** 过滤证据（含策略输入参数和判断结果） */
    private Object filterEvidence;

    /** 剩余产能（机台定额 - 已排产量，单位：条） */
    private BigDecimal remainCapacity;

    /** 任务链长度（该机台当前已排任务数，用于负载均衡评分） */
    private int taskChainSize;

    /** 评分（数值越大优先级越高） */
    private BigDecimal score;

    /** 评分结果描述（人类可读，用于解释输出） */
    private String scoreResult;

    /** 是否被选中（最终分配的机台） */
    private boolean selected;

    /** 排名（在候选列表中的位置，1 表示首选） */
    private int rank;
}
