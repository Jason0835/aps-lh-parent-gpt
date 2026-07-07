package com.zlt.aps.cd15.engine.service;

import com.zlt.aps.cd15.engine.model.Cd15BatchDataCheckResult;

import java.time.LocalDate;

/**
 * 斜裁自动排程批次级数据先行检查。
 */
public interface Cd15AutoScheduleBatchDataValidator {

    /**
     * 检查排程入口公共数据是否完整。
     *
     * @param factoryCode 工厂编码
     * @param scheduleDate 排程日期
     * @return 检查结果
     */
    Cd15BatchDataCheckResult check(String factoryCode, LocalDate scheduleDate);
}