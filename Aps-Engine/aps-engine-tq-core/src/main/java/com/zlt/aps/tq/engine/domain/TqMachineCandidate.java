package com.zlt.aps.tq.engine.domain;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 胎圈候选机台追踪对象。
 *
 * <p>Phase 3 重构新增：对齐胎侧 {@code TcMachineCandidate}，用于在 S3 机台分配阶段记录每个候选机台的
 * 过滤状态、过滤原因、剩余产能和选中评分。该对象不修改机台基础数据，只承载分配过程的运行态信息。</p>
 *
 * <p>与胎侧的差异：</p>
 * <ul>
 *   <li>胎侧有独立的评分策略接口 {@code ITcMachineScoreStrategy}，评分维度复杂（定点/口型板/胶料/工装/任务链连续性）</li>
 *   <li>胎圈业务相对简单（一个规格一个机台，无胶料切换），评分仅按排序优先级（已排同规格 → 剩余产能升序 → 机台编号）</li>
 *   <li>胎侧的 {@code maxCapacity/machineSpeed/maintenanceHours} 等机台能力字段在胎圈对应 {@code TqMachineInfo.quota}</li>
 * </ul>
 *
 * <p>使用方式：</p>
 * <ol>
 *   <li>S3 阶段 {@code TqMachineAssignHandler.searchOptionalMachineList} 创建候选列表</li>
 *   <li>策略链过滤时，被过滤的机台标记 {@code filtered=true} 并记录过滤原因</li>
 *   <li>通过过滤的机台按排序优先级评分，选中机台标记 {@code selected=true}</li>
 *   <li>候选列表写入 {@code context.candidateTraceMap}，供 Phase 4 解释快照使用</li>
 * </ol>
 *
 * @author APS
 */
@Data
public class TqMachineCandidate {

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

    /** 剩余定额（同 remainCapacity，保留字段用于与胎侧对齐） */
    private BigDecimal remainQuota;

    /** 评分（数值越大优先级越高，按排序优先级计算） */
    private BigDecimal score;

    /** 评分结果描述（人类可读，用于解释输出） */
    private String scoreResult;

    /** 是否被选中（最终分配的机台） */
    private boolean selected;

    /** 排名（在候选列表中的位置，1 表示首选） */
    private int rank;

    // ========== Phase 5 重构新增：任务链评分上下文 ==========

    /**
     * 当前机台任务链末尾节点对应的胎圈规格（前置规格）。
     *
     * <p>Phase 5 重构新增：用于评分时判断是否需要规格切换。空表示机台当前无任务（首班次），
     * 不需要切换；非空且与当前待排规格不同时，需要计算切换时长并扣分。</p>
     *
     * <p>由 {@code TqMachineAssignHandler.searchOptionalMachineList} 在构建候选列表时
     * 从 {@code context.taskChainGroup} 末尾节点读取填充。</p>
     */
    private String lastBeadCode;

    /**
     * 当前机台任务链末尾节点对应的班次顺序（前置班次）。
     *
     * <p>Phase 5 重构新增：用于判断当前待排班次与前置班次是否同班。
     * 同班次内切换规格用规格切换时长（短），跨班次切换用英寸切换时长（长），
     * 评分时同班次切换的扣分应更重，因为占用本班生产时间。</p>
     *
     * <p>空表示机台当前无任务；非空时为 1~6 的班次索引。</p>
     */
    private Integer lastClassIndex;

    /**
     * 当前机台任务链末尾节点对应的生产顺序（前置生产顺序）。
     *
     * <p>Phase 5 重构新增：用于判断机台在本班次是否已有任务。同一班次内已有任务时，
     * 追加新规格需要更长的切换时间，评分应考虑这一因素。</p>
     */
    private Integer lastProduceOrder;

    /**
     * 规格切换时长（小时），0 表示无切换。
     *
     * <p>Phase 5 重构新增：由前置规格和当前规格是否一致、是否同班次决定。
     * 该字段只读，由 {@code TqMachineAssignHandler} 在评分前计算填充，
     * 评分策略直接读取该值用于扣分计算。</p>
     */
    private double switchTime;

    /**
     * 规格切换连续性得分（0~100，数值越大连续性越好）。
     *
     * <p>Phase 5 重构新增：评分策略基于 {@link #lastBeadCode} 和当前待排规格是否一致计算：</p>
     * <ul>
     *   <li>规格一致：100 分（无切换损耗）</li>
     *   <li>同班次不同规格：60 分（规格切换占用本班生产时间）</li>
     *   <li>跨班次不同规格：80 分（规格切换占用班次切换时间，影响较小）</li>
     *   <li>机台无任务（首班次）：90 分（无需切换，但缺乏连续性优势）</li>
     * </ul>
     */
    private BigDecimal continuityScore;

    /**
     * 任务链末尾节点引用（用于评分时直接访问末尾节点的附加属性）。
     *
     * <p>Phase 5 重构新增：避免重复查找任务链末尾节点。{@code TqMachineAssignHandler.searchOptionalMachineList}
     * 在构建候选列表时一次性查找并填充，评分策略直接读取该引用。</p>
     *
     * <p>注意：该字段不参与序列化，避免循环引用导致 JSON 输出异常。</p>
     */
    private transient com.zlt.aps.common.engine.schedule.ScheduleTaskNode<com.zlt.aps.tq.engine.vo.TqTaskNode> lastChainNode;
}
