package com.zlt.aps.cd90.engine.service;

import java.time.LocalDate;

/** 自动排程关键输入版本指纹服务。 */
public interface Cd90AutoScheduleInputVersionService {

    /** 生成成型计划、6点库存和库排资源的统一版本指纹。 */
    String fingerprint(String factoryCode, LocalDate scheduleDate);
}
