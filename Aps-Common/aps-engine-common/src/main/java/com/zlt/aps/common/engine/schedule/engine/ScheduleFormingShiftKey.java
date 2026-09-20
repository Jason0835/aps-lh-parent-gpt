package com.zlt.aps.common.engine.schedule.engine;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.Comparator;

/**
 * 可跨来源排程日期比较的实际成型班次。
 *
 * <p>收尾判定不能只比较 CLASS 序号。该对象把实际成型生产日期和当天班次顺序
 * 作为一个值保存，来源收尾与最终产品收尾统一使用该对象比较。</p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ScheduleFormingShiftKey implements Comparable<ScheduleFormingShiftKey> {

    /**
     * 实际成型生产日期。
     */
    private LocalDate productionDate;

    /**
     * 实际生产日期内的成型班次顺序。
     */
    private Integer shiftOrder;

    /**
     * 比较两个实际成型班次的先后。
     *
     * @param other 另一个成型班次
     * @return 先后比较结果
     */
    @Override
    public int compareTo(ScheduleFormingShiftKey other) {
        if (other == null) {
            return 1;
        }
        int dateCompare = Comparator.nullsFirst(Comparator.<LocalDate>naturalOrder())
                .compare(this.productionDate, other.productionDate);
        if (dateCompare != 0) {
            return dateCompare;
        }
        return Comparator.nullsFirst(Comparator.<Integer>naturalOrder()).compare(this.shiftOrder, other.shiftOrder);
    }
}
