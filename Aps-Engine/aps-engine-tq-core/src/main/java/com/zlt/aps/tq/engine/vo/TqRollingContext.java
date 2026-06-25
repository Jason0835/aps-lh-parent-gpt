package com.zlt.aps.tq.engine.vo;

import lombok.Data;

import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

/**
 * 胎圈排程滚动更新上下文
 *
 * <p>单次滚动更新请求内复用的上下文对象，包含：</p>
 * <ul>
 *   <li>触发信息：触发类型、触发源、排程日期、班次</li>
 *   <li>任务链：按机台编号分组，每个机台维护一个任务链</li>
 *   <li>缓存：生产速度缓存、参数缓存（避免重复查DB）</li>
 *   <li>变更明细：本次滚动产生的所有变更</li>
 * </ul>
 *
 * @author APS
 */
@Data
public class TqRollingContext {

    /** 触发类型：0-自动定时，1-插单，2-转机台，3-调量，4-删除 */
    private String triggerType;

    /** 触发源排程记录ID（手动操作时） */
    private Long triggerSourceId;

    /** 排程日期 */
    private Date scheduleDate;

    /** 触发班次索引（1~6） */
    private int shiftIndex;

    /** 触发机台编号 */
    private String machineCode;

    /** 触发胎圈代码 */
    private String beadCode;

    /** 厂别 */
    private String factoryCode;

    /** 分公司编码 */
    private String companyCode;

    /** 滚动批次号（每次滚动唯一） */
    private String batchNo;

    /** 关联的日志主表ID（用于明细记录关联） */
    private Long rollingLogId;

    /** 滚动前预计库存 */
    private double beforeStockQty;

    /** 滚动后预计库存 */
    private double afterStockQty;

    /** 调整原因 */
    private String adjustReason;

    /** 班次开始时间（用于计算预计开始时间） */
    private Date shiftStartTime;

    /** 班次结束时间（用于判断任务是否超时） */
    private Date shiftEndTime;

    /** 单班时长（小时） */
    private double shiftHours;

    /**
     * 任务链：key=机台编号，value=该机台的任务链（按生产顺序排序）
     * <p>MVP阶段：仅维护触发班次内的任务链</p>
     */
    private Map<String, LinkedList<TqRollingTaskNode>> taskChainMap = new HashMap<>();

    /**
     * 生产速度缓存：key=machineCode:beadCode，value=速度（个/小时）
     * <p>避免单次滚动更新内重复查询 T_TQ_MACHINE_SPEC_SPEED</p>
     */
    private Map<String, Double> speedCache = new HashMap<>();

    /** 影响的排程记录数 */
    private int affectedCount;

    /** 是否有变更（用于判断是否需要持久化） */
    private boolean hasChange;
}
