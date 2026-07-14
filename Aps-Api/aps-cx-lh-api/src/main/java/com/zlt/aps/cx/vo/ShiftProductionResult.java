package com.zlt.aps.cx.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Set;

/**
 * 班次级排产结果 — 汇总为 CxScheduleResult 前的最小持久化单元。
 *
 * <p>一条记录 = 某机台、某班次、某任务的一次计划量（条）及计划起止时间。
 * {@link #sourceTask} 保留原始 DailyEmbryoTask，供合并阶段读取硫化机台数等字段。
 *
 * @author APS Team
 */
@Data
public class ShiftProductionResult {
    /** 机台编码 */
    private String machineCode;
    /** 班次编码 */
    private String shiftCode;
    /** 班次名称 */
    private String shiftName;
    /** 胎胚编码 */
    private String embryoCode;
    /** 物料编号（成品物料编码） */
    private String materialCode;
    /** 物料描述 */
    private String materialDesc;
    /** 主物料描述（胎胚描述） */
    private String mainMaterialDesc;
    /** 结构名称 */
    private String structureName;
    /** 排产数量（条） */
    private Integer quantity;
    /** 车次号（班次内第几车） */
    private String tripNo;
    /** 本车次容量（整车条数） */
    private Integer tripCapacity;
    /** 库存可供硫化时长（小时） */
    private BigDecimal stockHours;
    /** 顺位（班次内排序） */
    private Integer sequence;
    /** 计划开始时间 */
    private LocalDateTime planStartTime;
    /** 计划结束时间 */
    private LocalDateTime planEndTime;
    /** 是否试制任务 */
    private Boolean isTrialTask;
    /** 是否收尾任务 */
    private Boolean isEndingTask;
    /** 是否续作任务 */
    private Boolean isContinueTask;
    /** 该班次分配的车数 */
    private Integer carsForShift;
    /** 机台小时产能（条/小时） */
    private Integer hourCapacity;
    /** 是否收尾最后一批（不补整车） */
    private Boolean isLastEndingBatch;
    /** 来源任务（用于均衡计算：获取硫化机数 vulcanizeMachineCount） */
    private DailyEmbryoTask sourceTask;
    /** 是否结束生产（反推需求-库存<=0，无需再排产） */
    private Boolean isEndProduction;
    /** 施工阶段（00 无工艺 01 试制 02 量试 03 正式），来自硫化任务 */
    private String constructionStage;
    /** 合并的所有物料编码（用于判断是否全部收尾） */
    private Set<String> allMaterialCodes;
    /** 收尾物料编码集合（部分收尾时用于精确标记） */
    private Set<String> endingMaterialCodes;
}