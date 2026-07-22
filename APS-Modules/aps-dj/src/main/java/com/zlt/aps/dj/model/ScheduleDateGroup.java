package com.zlt.aps.dj.model;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * 排产日期分组
 * <p>
 * 当插单、调整等操作涉及多个连续班次时，不同班次可能因跨天规则落在不同的排产日期上。
 * 系统按排产日期将班次分组，每组生成一条独立的排产记录。
 * </p>
 *
 * @author zlt
 */
public class ScheduleDateGroup {

    /** 该组的排产日期 */
    private Date scheduleDate;

    /** 该组的首班班次编码 */
    private String scheduleShiftClass;

    /** 该组包含的原始班次位置（1~6） */
    private List<Integer> positions = new ArrayList<>();

    /** 各原始班次位置对应的排产日期 */
    private Map<Integer, Date> positionDates = new java.util.HashMap<>();

    public Date getScheduleDate() {
        return scheduleDate;
    }

    public void setScheduleDate(Date scheduleDate) {
        this.scheduleDate = scheduleDate;
    }

    public String getScheduleShiftClass() {
        return scheduleShiftClass;
    }

    public void setScheduleShiftClass(String scheduleShiftClass) {
        this.scheduleShiftClass = scheduleShiftClass;
    }

    public List<Integer> getPositions() {
        return positions;
    }

    public void setPositions(List<Integer> positions) {
        this.positions = positions;
    }

    public Map<Integer, Date> getPositionDates() {
        return positionDates;
    }

    public void setPositionDates(Map<Integer, Date> positionDates) {
        this.positionDates = positionDates;
    }
}
