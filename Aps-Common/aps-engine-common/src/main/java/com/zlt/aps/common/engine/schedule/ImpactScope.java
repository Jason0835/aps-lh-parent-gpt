package com.zlt.aps.common.engine.schedule;

import lombok.Data;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

/**
 * 排程局部重算影响范围。
 *
 * <p>用于描述从某个机台、日期和班次开始，哪些机台班次链需要被重新计算。当前对象只做范围匹配，
 * 不直接修改任务链。</p>
 */
@Data
public class ImpactScope {

    /** 受影响机台编码集合；为空表示不限机台 */
    private Set<String> machineCodes = new HashSet<>();

    /** 排程日期；为空表示不限日期 */
    private LocalDate scheduleDate;

    /** 起始班次顺序；为空表示不限下限 */
    private Integer startShiftOrder;

    /** 结束班次顺序；为空表示不限上限 */
    private Integer endShiftOrder;

    /**
     * 判断链表键是否落在影响范围内。
     *
     * @param chainKey 机台班次链表键，格式为 machineCode|scheduleDate|shiftOrder
     * @return true 表示该链表受影响
     */
    public boolean matches(String chainKey) {
        if (chainKey == null) {
            return false;
        }
        String[] parts = chainKey.split("\\|", -1);
        if (parts.length != 3) {
            return false;
        }
        String machineCode = parts[0];
        LocalDate date = "null".equals(parts[1]) ? null : LocalDate.parse(parts[1]);
        Integer shiftOrder = "null".equals(parts[2]) ? null : Integer.valueOf(parts[2]);
        if (!machineCodes.isEmpty() && !machineCodes.contains(machineCode)) {
            return false;
        }
        if (scheduleDate != null && !scheduleDate.equals(date)) {
            return false;
        }
        if (startShiftOrder != null && (shiftOrder == null || shiftOrder < startShiftOrder)) {
            return false;
        }
        return endShiftOrder == null || (shiftOrder != null && shiftOrder <= endShiftOrder);
    }

    public void setMachineCodes(Set<String> machineCodes) {
        this.machineCodes = machineCodes == null ? new HashSet<>() : machineCodes;
    }
}
