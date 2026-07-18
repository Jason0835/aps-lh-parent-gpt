package com.zlt.aps.cd15.engine.service;

import com.zlt.aps.cd15.engine.model.Cd15BatchDataCheckResult;

import java.time.LocalDate;

/**
 * 斜裁自动排程批次级数据先行检查服务。
 * <p>
 * 在正式进入自动排程（创建PENDING任务、占用执行锁、触发异步执行器）之前，
 * 同步校验1.2节定义的批次级公共数据是否就绪。检查失败时直接返回结构化错误，
 * 不进入排程算法，不覆盖原排程结果。规格级失败仍由排程算法写入未排结果表。
 * </p>
 */
public interface Cd15AutoScheduleBatchDataValidator {

    /**
     * 执行批次级数据先行检查。
     *
     * @param factoryCode  工厂编码
     * @param scheduleDate 排程日期
     * @return 检查结果，failed=true时中止排程入口
     */
    Cd15BatchDataCheckResult check(String factoryCode, LocalDate scheduleDate);
}
