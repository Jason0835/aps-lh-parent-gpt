package com.zlt.aps.cd15.engine.service;

import java.time.LocalDate;

/** 自动排程关键输入版本指纹服务。 */
public interface Cd15AutoScheduleInputVersionService {

    /**
     * 生成成型计划、6点库存和任务启动时当前班次库排资源的统一版本指纹。
     *
     * @param factoryCode 工厂编码
     * @param scheduleDate 排程日期
     * @param resourceBaselineDate 当前资源班次快照日期
     * @param resourceBaselineShiftCode 当前资源班次编码
     * @return 输入版本指纹
     */
    String fingerprint(String factoryCode, LocalDate scheduleDate,
                       LocalDate resourceBaselineDate,
                       String resourceBaselineShiftCode);
}
