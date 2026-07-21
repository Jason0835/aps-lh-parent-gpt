package com.zlt.aps.gsq.engine.service;

/**
 * 钢丝圈排程引擎服务接口
 */
public interface GsqEngineService {

    /**
     * 钢丝圈胶自动排程（6班制新架构入口）
     * @param scheduleDate 排程日期，格式：yyyy-MM-dd
     * @param factoryCode  分厂编码
     */
    void autoGsqSchedule(String scheduleDate, String factoryCode);
}
