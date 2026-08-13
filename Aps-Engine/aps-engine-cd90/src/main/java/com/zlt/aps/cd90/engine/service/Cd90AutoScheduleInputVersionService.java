package com.zlt.aps.cd90.engine.service;

import java.time.LocalDate;

/** 自动排程关键输入版本指纹服务。 */
public interface Cd90AutoScheduleInputVersionService {

    /** 生成全部关键输入和任务启动时资源快照的统一版本指纹。 */
    String fingerprint(String factoryCode, LocalDate scheduleDate,
                       LocalDate resourceBaselineDate,
                       String resourceBaselineShiftCode);

    /** 生成不包含旧库存的基础输入指纹，供定时滚动叠加目标班次库存。 */
    String fingerprintWithoutStock(String factoryCode, LocalDate scheduleDate,
                                   LocalDate resourceBaselineDate,
                                   String resourceBaselineShiftCode);
}
