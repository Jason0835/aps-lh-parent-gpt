package com.zlt.aps.cd15.engine.service;

import java.time.LocalDate;

/**
 * 斜裁自动排程关键输入版本指纹服务。
 */
public interface Cd15AutoScheduleInputVersionService {

    /**
     * 生成指定工厂和排程日期的输入版本指纹。
     *
     * @param factoryCode 工厂编码
     * @param scheduleDate 排程日期
     * @return SHA-256 指纹
     */
    String fingerprint(String factoryCode, LocalDate scheduleDate);
}