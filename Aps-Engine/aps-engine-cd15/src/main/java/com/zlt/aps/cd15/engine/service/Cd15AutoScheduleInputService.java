package com.zlt.aps.cd15.engine.service;

import com.zlt.aps.cd15.engine.model.Cd15AutoScheduleInput;

import java.time.LocalDate;

/**
 * 斜裁自动排程输入数据加载服务。
 */
public interface Cd15AutoScheduleInputService {

    /**
     * 加载完整自动排程输入快照，资源基线取启用配置中的首个班次。
     *
     * @param factoryCode 工厂编码
     * @param scheduleDate 排程日期
     * @param agingPeriodHours 大卷成熟期小时数
     * @return 自动排程输入快照
     */
    Cd15AutoScheduleInput load(String factoryCode, LocalDate scheduleDate, int agingPeriodHours);

    /**
     * 加载指定排程日期和班次的滚动排程输入快照。
     *
     * @param factoryCode 工厂编码
     * @param scheduleDate 排程日期
     * @param classField 结果班次字段
     * @param shiftCode 业务班次编码
     * @param agingPeriodHours 大卷成熟期小时数
     * @return 自动排程输入快照
     */
    Cd15AutoScheduleInput load(String factoryCode, LocalDate scheduleDate,
                               String classField, String shiftCode, int agingPeriodHours);
}
