package com.zlt.aps.lh.context;

import lombok.Getter;

import java.time.LocalDate;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/** 本次排程的历史限额快照；派生上下文共享同一对象，不重新解释原窗口。 */
@Getter
public final class MouldChangeQuotaSnapshot {
    /** 快照所属工厂。 */
    private final String factoryCode;
    /** 快照所属本次排程批次。 */
    private final String batchNo;
    /** 本次原窗口起点T。 */
    private final LocalDate originalStartDate;
    /** 历史交替计划保存日期，取业务目标日前一日。 */
    private final LocalDate sourceScheduleDate;
    /** 初始化时的基础参数限额。 */
    private final MouldChangeQuotaLimits baseLimits;
    /** 按计划日期统计的有效源记录次数，尚未合并L/R。 */
    private final Map<String, MouldChangeQuotaLimits> rawCounts;
    /** 按计划日期合并L/R后的历史次数。 */
    private final Map<String, MouldChangeQuotaLimits> historicalCounts;
    /** 原T、T+1按三个维度分别取大的有效限额。 */
    private final Map<String, MouldChangeQuotaLimits> effectiveLimits;

    /**
     * 创建只读快照，复制集合以隔离统计阶段的可变容器。
     * @param factoryCode 工厂
     * @param batchNo 本次批次
     * @param originalStartDate 原T日
     * @param sourceScheduleDate 历史保存日期
     * @param baseLimits 基础限额
     * @param rawCounts 有效源记录次数
     * @param historicalCounts 历史合并次数
     * @param effectiveLimits 有效限额
     */
    public MouldChangeQuotaSnapshot(String factoryCode, String batchNo, LocalDate originalStartDate,
            LocalDate sourceScheduleDate, MouldChangeQuotaLimits baseLimits,
            Map<String, MouldChangeQuotaLimits> rawCounts,
            Map<String, MouldChangeQuotaLimits> historicalCounts,
            Map<String, MouldChangeQuotaLimits> effectiveLimits) {
        this.factoryCode = factoryCode;
        this.batchNo = batchNo;
        this.originalStartDate = originalStartDate;
        this.sourceScheduleDate = sourceScheduleDate;
        this.baseLimits = baseLimits;
        this.rawCounts = Collections.unmodifiableMap(new LinkedHashMap<>(rawCounts));
        this.historicalCounts = Collections.unmodifiableMap(new LinkedHashMap<>(historicalCounts));
        this.effectiveLimits = Collections.unmodifiableMap(new LinkedHashMap<>(effectiveLimits));
    }
}
