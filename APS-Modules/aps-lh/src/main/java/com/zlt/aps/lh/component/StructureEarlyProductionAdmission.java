package com.zlt.aps.lh.component;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * 结构当天提前生产资格快照。
 *
 * <p>同一业务日、同一结构只生成一次。快照固定记录当天最后一个班次的结构计划机台数和
 * 实际已排物理机台数；一旦取得资格，当天其他班次只继续执行产能、模具、胎胚、首检和
 * 选机等既有约束，不再重复判断结构机台数。</p>
 *
 * @author APS
 */
@Data
public class StructureEarlyProductionAdmission implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 资格所属业务日期 */
    private LocalDate businessDate;
    /** 产品结构名称 */
    private String structureName;
    /** 生效结构计划机台数的来源日期 */
    private LocalDate planSourceDate;
    /** 当天用于资格判断的最后一个班次索引 */
    private int admissionShiftIndex;
    /** 生效结构计划硫化机台数 */
    private int currentPlanMachineCount;
    /** 参与最后班次生产或业务停机占用的物理机台 */
    private Set<String> rawScheduledPhysicalMachineCodes = new LinkedHashSet<String>(8);
    /** 按统一在机口径计入最后班次的物理机台 */
    private Set<String> scheduledPhysicalMachineCodes = new LinkedHashSet<String>(8);
    /** 结构已排硫化机台数 */
    private int scheduledStructureCount;
    /** 是否取得当天提前生产资格 */
    private boolean allowed;
    /** 资格判断原因 */
    private String reason;
}
