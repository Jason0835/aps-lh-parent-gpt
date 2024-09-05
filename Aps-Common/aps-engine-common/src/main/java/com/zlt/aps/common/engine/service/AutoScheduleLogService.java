package com.zlt.aps.common.engine.service;

import com.zlt.aps.common.engine.domain.AutoScheduleLog;

/**
 * 自动排程日志Service接口
 * 
 * @author zlt
 * @date 2021-07-16
 */
public interface AutoScheduleLogService {

    /**
     * 新增硫化自动排程日志
     * @param batchNo  批次号
     * @param orderNo  工单号
     * @param logDetail  日志内容
     */
    void insertLhScheduleLog(String batchNo, String orderNo, String title, String logDetail);

    /**
     * 新增成型自动排程日志
     * @param batchNo  批次号
     * @param orderNo  工单号
     * @param logDetail  日志内容
     */
    void insertCxScheduleLog(String batchNo, String orderNo, String title, String logDetail);

    /**
     * 新增胎面自动排程日志
     * @param batchNo  批次号
     * @param orderNo  工单号
     * @param logDetail  日志内容
     */
    void insertTmScheduleLog(String batchNo, String orderNo, String title, String logDetail);

    /**
     * 新增胎侧自动排程日志
     * @param batchNo  批次号
     * @param orderNo  工单号
     * @param logDetail  日志内容
     */
    void insertTcScheduleLog(String batchNo, String orderNo, String title, String logDetail);

    /**
     * 新增内衬自动排程日志
     * @param batchNo  批次号
     * @param orderNo  工单号
     * @param logDetail  日志内容
     */
    void insertNcScheduleLog(String batchNo, String orderNo, String title, String logDetail);

    /**
     * 新增胎圈自动排程日志
     * @param batchNo  批次号
     * @param orderNo  工单号
     * @param logDetail  日志内容
     */
    void insertTqScheduleLog(String batchNo, String orderNo, String title, String logDetail);

    /**
     * 新增钢丝圈自动排程日志
     * @param batchNo  批次号
     * @param orderNo  工单号
     * @param logDetail  日志内容
     */
    void insertGsqScheduleLog(String batchNo, String orderNo, String title, String logDetail);

    /**
     * 新增15度裁断自动排程日志
     * @param batchNo  批次号
     * @param orderNo  工单号
     * @param logDetail  日志内容
     */
    void insertCd15ScheduleLog(String batchNo, String orderNo, String title, String logDetail);

    /**
     * 新增90度裁断自动排程日志
     * @param batchNo  批次号
     * @param orderNo  工单号
     * @param logDetail  日志内容
     */
    void insertCd90ScheduleLog(String batchNo, String orderNo, String title, String logDetail);

    /**
     * 新增钢带压延自动排程日志
     * @param batchNo  批次号
     * @param orderNo  工单号
     * @param logDetail  日志内容
     */
    void insertGdyyScheduleLog(String batchNo, String orderNo, String title, String logDetail);

    /**
     * 新增纤维压延自动排程日志
     * @param batchNo  批次号
     * @param orderNo  工单号
     * @param logDetail  日志内容
     */
    void insertXwyyScheduleLog(String batchNo, String orderNo, String title, String logDetail);

    /**
     * 新增自动排程日志
     * @param procedureCode 工序类型：0-硫化、1-成型、2-胎面、3-胎侧、4-内衬、5-胎圈、6-钢丝圈、7-15度裁断、8-90裁断、9-90度裁断、10-纤维压延
     * @param batchNo  批次号
     * @param orderNo  工单号
     * @param title  标题
     * @param title  标题
     * @param logDetail  日志内容
     */
     void insertScheduleLog(String procedureCode, String batchNo, String orderNo, String title, String logDetail);


//    /**
//     * 新增自动排程日志
//     * @param procedureCode 工序类型：0-硫化、1-成型、2-胎面、3-胎侧、4-内衬、5-胎圈、6-钢丝圈、7-15度裁断、8-90裁断、9-90度裁断、10-纤维压延
//     * @param batchNo  批次号
//     * @param orderNo  工单号
//     * @param logDetail  日志内容
//     */
//    void insertScheduleLog(String procedureCode, String batchNo, String orderNo, String title, String logDetail);

//    /**
//     * 新增自动排程日志
//     *
//     * @param autoScheduleLog 自动排程日志
//     * @return 结果
//     */
//    void insertScheduleLog(AutoScheduleLog autoScheduleLog);
}
