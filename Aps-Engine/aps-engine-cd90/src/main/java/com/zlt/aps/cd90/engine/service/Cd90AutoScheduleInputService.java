package com.zlt.aps.cd90.engine.service;

import com.zlt.aps.cd90.engine.model.Cd90AutoScheduleInput;

import java.time.LocalDate;

/**
 * 直裁自动排程输入数据加载边界。
 */
public interface Cd90AutoScheduleInputService {

    /**
     * 加载指定工厂、排程日期和班次所需的第1至5步输入数据。
     * 成型计划覆盖排程日前1天至后3天，施工信息通过胎胚代码批量关联。
     *
     * @param factoryCode 工厂编码
     * @param scheduleDate 排程日期
     * @param classField 直裁班次字段
     * @return 标准化输入数据
     */
    Cd90AutoScheduleInput load(String factoryCode, LocalDate scheduleDate, String classField);
}
