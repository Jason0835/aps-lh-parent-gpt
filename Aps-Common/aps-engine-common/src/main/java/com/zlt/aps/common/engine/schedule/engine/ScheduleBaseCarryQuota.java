package com.zlt.aps.common.engine.schedule.engine;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Set;

/**
 * 已选机结算的基础残量配额；待承接不是实际生产事实。
 */
@Data
public class ScheduleBaseCarryQuota<T extends ScheduleTaskDraftModel> {
    /**
     * 来源基础任务，保留原需求事件与数量证明。
     */
    private T sourceTask;
    /**
     * 唯一基础未排片段，后班承接后只扣减该片段。
     */
    private T diagnosticTask;
    /**
     * 原基础额度身份。
     */
    private String originBaseQuotaKey;
    /**
     * 来源归属计划组。
     */
    private String planGroupKey;
    /**
     * 首次未承接的实际生产班。
     */
    private Integer sourceProductionShift;
    /**
     * 来源成型身份集合；不靠planGroupKey字符串猜测需求重叠。
     */
    private Set<String> sourceIdentities;
    /**
     * 尚未承接的已结算基础量。
     */
    private BigDecimal remainingQty;
    /**
     * 是否已在具体机台下完成损耗/封顶结算；false时余额仍是pre-loss请求。
     */
    private boolean committed;
}
