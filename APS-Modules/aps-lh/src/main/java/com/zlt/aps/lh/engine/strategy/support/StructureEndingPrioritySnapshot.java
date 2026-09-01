package com.zlt.aps.lh.engine.strategy.support;

import java.time.LocalDate;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * 结构 N 天内收尾优先级只读快照。
 *
 * <p>快照只保存结构维度的最大结束日、包含首尾距离和命中结果，不保存 SKU 深拷贝，
 * 排序器和特殊 SKU 分类器按结构名称共同读取，避免大候选集重复构造对象身份映射。</p>
 *
 * @author APS
 */
public final class StructureEndingPrioritySnapshot {

    /** 排程窗口 T 日。 */
    private final LocalDate scheduleDate;
    /** SYS0304002 结构收尾阈值。 */
    private final int thresholdDays;
    /** 结构最大 END_DAY。 */
    private final Map<String, LocalDate> maximumEndingDateMap;
    /** 结构最大 END_DAY 与 T 日的包含首尾距离。 */
    private final Map<String, Integer> inclusiveDistanceDaysMap;

    public StructureEndingPrioritySnapshot(
            LocalDate scheduleDate,
            int thresholdDays,
            Map<String, LocalDate> maximumEndingDateMap,
            Map<String, Integer> inclusiveDistanceDaysMap) {
        this.scheduleDate = scheduleDate;
        this.thresholdDays = thresholdDays;
        this.maximumEndingDateMap = Collections.unmodifiableMap(
                new LinkedHashMap<String, LocalDate>(maximumEndingDateMap));
        this.inclusiveDistanceDaysMap = Collections.unmodifiableMap(
                new LinkedHashMap<String, Integer>(inclusiveDistanceDaysMap));
    }

    /**
     * 判断结构是否命中现有排序使用的 N 天内收尾层级。
     *
     * @param structureName 结构名称
     * @return true-命中；false-未命中
     */
    public boolean isPriorityStructure(String structureName) {
        int distanceDays = resolveInclusiveDistanceDays(structureName);
        return distanceDays > 0 && distanceDays < thresholdDays;
    }

    public LocalDate resolveMaximumEndingDate(String structureName) {
        return maximumEndingDateMap.get(structureName);
    }

    public int resolveInclusiveDistanceDays(String structureName) {
        Integer distanceDays = inclusiveDistanceDaysMap.get(structureName);
        return Objects.isNull(distanceDays) ? -1 : distanceDays;
    }

    public LocalDate getScheduleDate() {
        return scheduleDate;
    }

    public int getThresholdDays() {
        return thresholdDays;
    }
}
