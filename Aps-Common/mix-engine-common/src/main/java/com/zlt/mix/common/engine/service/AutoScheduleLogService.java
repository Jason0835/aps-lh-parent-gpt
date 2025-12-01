package com.zlt.mix.common.engine.service;

/**
 * 自动排程日志Service接口
 * 
 * @author zlt
 * @date 2021-07-16
 */
public interface AutoScheduleLogService {

    /**
     * 新增终炼母炼自动排程日志
     * @param batchNo  批次号
     * @param orderNo  工单号
     * @param logDetail  日志内容
     */
    void insertGlueScheduleLog(String batchNo, String orderNo, String title, String logDetail);

    /**
     * 新增硫磺辅料自动排程日志
     * @param batchNo  批次号
     * @param orderNo  工单号
     * @param logDetail  日志内容
     */
    void insertMaterialScheduleLog(String batchNo, String orderNo, String title, String logDetail);
}
