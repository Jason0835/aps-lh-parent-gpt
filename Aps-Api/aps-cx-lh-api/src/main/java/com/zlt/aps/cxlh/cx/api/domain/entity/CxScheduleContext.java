package com.zlt.aps.cxlh.cx.api.domain.entity;

import lombok.Data;

import java.math.BigDecimal;
import java.util.*;

/**
 * 排程上下文 - 贯穿整个排程流程的上下文数据
 */
@Data
public class CxScheduleContext {

    // ==================== 基础信息 ====================
    /** 排程日期 */
    private Date scheduleDate;
    /** 班次编码 */
    private String shiftCode;
    /** 版本号 */
    private String versionNo;

    // ==================== 初始化数据 ====================
    /** 可用机台列表 */
    private List<CxMachine> availableMachines;
    /** 硫化排产列表 */
    private List<LhScheduleResult> lhScheduleList;
    /** 前日成型排产 */
    private List<CxScheduleResult> prevCxScheduleList;
    /** BOM示方书信息 */
    private List<MdmBomInfo> bomInfoList;
    /** 施工信息 */
    private List<MdmConstructionInfo> constructionList;
    /** 胎面配置 */
    private List<TreadConfig> treadConfigList;
    /** 工作日历 */
    private List<WorkCalendar> workCalendarList;
    /** 计划停机列表 */
    private List<PlannedStop> plannedStopList;
    /** 胎胚库存 */
    private Map<String, BigDecimal> inventoryMap;
    /** 本月已完成量 */
    private Map<String, BigDecimal> completedQtyMap;

    // ==================== 任务分类结果 ====================
    /** 续作任务列表 */
    private List<ScheduleTask> continueTasks;
    /** 新增任务列表 */
    private List<ScheduleTask> newTasks;
    /** 结构余量映射 */
    private Map<String, BigDecimal> surplusMap;
    /** 按结构分组 */
    private Map<String, List<ScheduleTask>> structureGroup;

    // ==================== 排程结果 ====================
    /** 排程结果列表 */
    private List<CxScheduleResult> scheduleList;
    /** 排程明细列表 */
    private List<CxScheduleDetail> scheduleDetailList;

    // ==================== 统计信息 ====================
    /** 总任务数 */
    private int totalTasks;
    /** 已完成任务数 */
    private int completedTasks;
    /** 各班次总计划量 */
    private Map<String, BigDecimal> shiftTotalQtyMap;

    public CxScheduleContext() {
        this.availableMachines = new ArrayList<>();
        this.lhScheduleList = new ArrayList<>();
        this.prevCxScheduleList = new ArrayList<>();
        this.bomInfoList = new ArrayList<>();
        this.constructionList = new ArrayList<>();
        this.treadConfigList = new ArrayList<>();
        this.workCalendarList = new ArrayList<>();
        this.plannedStopList = new ArrayList<>();
        this.inventoryMap = new HashMap<>();
        this.completedQtyMap = new HashMap<>();
        this.continueTasks = new ArrayList<>();
        this.newTasks = new ArrayList<>();
        this.surplusMap = new HashMap<>();
        this.structureGroup = new HashMap<>();
        this.scheduleList = new ArrayList<>();
        this.scheduleDetailList = new ArrayList<>();
        this.shiftTotalQtyMap = new HashMap<>();
    }
}
