package com.zlt.aps.common.engine.schedule;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * TM/TC 最终排程汇总公共模板。
 *
 * <p>固定已排正计划量按机台、产品、胎胚和六班聚合的顺序；领域实现只提供字段读取，
 * 是否纳入汇总由领域服务保留未排判定。</p>
 *
 * @param <T> 领域任务类型
 */
public abstract class AbstractFinalScheduleSummaryCalculator<T> {

    /** 分组键分隔符，避免常规编码拼接冲突。 */
    private static final String GROUP_SEPARATOR = "\u0001";

    /**
     * 汇总任务计划量。
     *
     * @param tasks 待汇总任务
     * @param includePredicate 领域纳入条件
     * @return 按稳定分组键排序的汇总结果
     */
    public final List<ScheduleFinalQuantitySummaryItem> calculate(Collection<T> tasks,
                                                                    Predicate<T> includePredicate) {
        if (tasks == null || tasks.isEmpty()) {
            return Collections.emptyList();
        }
        Map<String, ScheduleFinalQuantitySummaryItem> summaryMap = new TreeMap<>();
        tasks.stream()
                .filter(task -> task != null && (includePredicate == null || includePredicate.test(task)))
                .forEach(task -> {
                    String machineCode = this.normalizeCode(this.getMachineCode(task));
                    String productCode = this.normalizeCode(this.getProductCode(task));
                    String embryoCode = this.normalizeEmbryoCode(this.getEmbryoCode(task));
                    String groupKey = machineCode + GROUP_SEPARATOR + productCode + GROUP_SEPARATOR + embryoCode;
                    ScheduleFinalQuantitySummaryItem summaryItem = summaryMap.computeIfAbsent(groupKey,
                            key -> new ScheduleFinalQuantitySummaryItem(machineCode, productCode, embryoCode));
                    summaryItem.add(this.getShiftOrder(task), this.getPlanQty(task));
                });
        return summaryMap.values().stream().collect(Collectors.toList());
    }

    /**
     * 读取机台编码。
     *
     * @param task 领域任务
     * @return 机台编码
     */
    protected abstract String getMachineCode(T task);

    /**
     * 读取产品编码。
     *
     * @param task 领域任务
     * @return 产品编码
     */
    protected abstract String getProductCode(T task);

    /**
     * 读取胎胚编码。
     *
     * @param task 领域任务
     * @return 胎胚编码
     */
    protected abstract String getEmbryoCode(T task);

    /**
     * 读取班次顺序。
     *
     * @param task 领域任务
     * @return 班次顺序
     */
    protected abstract Integer getShiftOrder(T task);

    /**
     * 读取计划量。
     *
     * @param task 领域任务
     * @return 计划量
     */
    protected abstract BigDecimal getPlanQty(T task);

    /**
     * 归一化普通编码。
     *
     * @param value 原始编码
     * @return 非空编码
     */
    private String normalizeCode(String value) {
        return String.valueOf(value);
    }

    /**
     * 归一化胎胚编码，保持现有日志的缺省文本。
     *
     * @param value 原始胎胚编码
     * @return 非空胎胚编码
     */
    private String normalizeEmbryoCode(String value) {
        return value == null || value.trim().isEmpty() ? "未提供" : value;
    }
}
