package com.zlt.aps.lh.component;

import com.zlt.aps.lh.api.domain.entity.LhMachineOnlineInfo;
import com.zlt.aps.lh.util.LhSingleControlMachineUtil;
import org.apache.commons.lang3.StringUtils;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * 按物理机台的每日硫化在机事实解析连续在机时长，不随物料或模具变化重新计时。
 *
 * @author APS
 * @date 2026/11/17
 */
public class LhMachineOnlineDurationResolver {

    /**
     * 强制精度要求的最少连续在机天数。
     */
    public static final int LONG_ONLINE_DAYS = 30;

    /**
     * 汇总截至T日的连续在机已满天数。同日及L/R去重，日期断档停止回溯。
     * 当日缺少在机事实时返回-1；当日首次在机为0天，连续覆盖T-30至T才满30天。
     *
     * @param records 同工厂硫化在机历史，允许为空
     * @param scheduleDate 排程窗口T日
     * @return 物理机台对应的已证实连续天数，未出现的机台由调用方视为未知
     */
    public Map<String, Integer> resolveContinuousOnlineDays(Collection<LhMachineOnlineInfo> records,
                                                          Date scheduleDate) {
        Map<String, Integer> result = new HashMap<>();
        if (records == null || scheduleDate == null) {
            return result;
        }
        LocalDate targetDate = this.toLocalDate(scheduleDate);
        Map<String, Set<LocalDate>> datesByMachine = new HashMap<>();
        for (LhMachineOnlineInfo record : records) {
            if (record == null || record.getOnlineDate() == null || StringUtils.isBlank(record.getLhCode())
                    || StringUtils.isBlank(record.getMaterialCode())) {
                continue;
            }
            LocalDate onlineDate = this.toLocalDate(record.getOnlineDate());
            if (!onlineDate.isAfter(targetDate)) {
                String physicalMachine = LhSingleControlMachineUtil.resolvePhysicalMachineCode(record.getLhCode());
                datesByMachine.computeIfAbsent(physicalMachine, key -> new HashSet<>()).add(onlineDate);
            }
        }
        datesByMachine.forEach((machine, dates) -> {
            int days = -1;
            LocalDate cursor = targetDate;
            while (dates.contains(cursor)) {
                days++;
                cursor = cursor.minusDays(1);
            }
            result.put(machine, days);
        });
        return result;
    }

    /**
     * 判断历史证据是否已覆盖至少30个完整自然日。
     *
     * @param onlineDays 已证实连续天数，负数表示未知
     * @return 连续在机满30天时返回true
     */
    public boolean isLongOnlineMachine(int onlineDays) {
        return onlineDays >= LONG_ONLINE_DAYS;
    }

    /**
     * 使用框架运行时区提取业务日期，兼容数据库日期实现。
     *
     * @param date 非空业务日期
     * @return 对应自然日
     */
    private LocalDate toLocalDate(Date date) {
        return java.time.Instant.ofEpochMilli(date.getTime()).atZone(ZoneId.systemDefault()).toLocalDate();
    }
}
