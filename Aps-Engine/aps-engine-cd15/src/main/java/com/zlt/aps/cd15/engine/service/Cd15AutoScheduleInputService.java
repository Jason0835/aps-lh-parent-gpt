package com.zlt.aps.cd15.engine.service;

import com.zlt.aps.cd15.engine.model.Cd15AutoScheduleInput;

import java.time.LocalDate;

/**
 * 斜裁自动排程输入数据加载边界。
 */
public interface Cd15AutoScheduleInputService {

    /**
     * 加载指定工厂、排程日期和班次所需的第1至5步输入数据。
     * 成型计划覆盖排程日前1天至后3天，施工信息通过胎胚代码批量关联。
     *
     * @param factoryCode 工厂编码
     * @param scheduleDate 排程日期
     * @param classField 斜裁结果班次字段
     * @param shiftCode 业务班次编码，用于读取班次基础数据
     * @param agingPeriodHours 大卷静置时长（小时）
     * @return 标准化输入数据
     */
    Cd15AutoScheduleInput load(String factoryCode, LocalDate scheduleDate,
                               String classField, String shiftCode, int agingPeriodHours);
}
