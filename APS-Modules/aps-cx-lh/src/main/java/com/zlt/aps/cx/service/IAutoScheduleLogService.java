package com.zlt.aps.cx.service;


import com.zlt.aps.cxlh.cx.api.domain.entity.AutoScheduleLog;

import java.util.List;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：IAutoScheduleLogService.java
 * 描    述：IAutoScheduleLogService成型自动排程日志后端接口
 *@author zlt
 *@date 2025-03-07
 *@version 1.0
 *
 *  修改记录：
 *     修改时间：...
 *     修 改 人：zlt
 *     修改内容：...
 */
public interface IAutoScheduleLogService{

    /**
     * 列表查询
     */
    List<AutoScheduleLog> selectList(AutoScheduleLog queryVO);

    /**
     * 新增自动排程日志
     * @param batchNo  批次号
     * @param orderNo  工单号
     * @param logDetail  日志内容
     */
    void insertLhScheduleLog(String procedureCode,String batchNo, String orderNo, String title, String logDetail);
}
