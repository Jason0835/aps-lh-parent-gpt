package com.zlt.aps.lh.service;


import com.zlt.aps.lh.api.domain.entity.LhScheduleResultLog;
import com.zlt.bill.common.service.IDocService;

import java.util.List;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：ILhScheduleResultLogService.java
 * 描    述：ILhScheduleResultLogService硫化排程结果日志后端接口
 *
 * @author zlt
 * @version 1.0
 * <p>
 * 修改记录：
 * 修改时间：...
 * 修 改 人：zlt
 * 修改内容：...
 * @date 2025-03-27
 */
public interface ILhScheduleResultLogService extends IDocService<LhScheduleResultLog> {


    /**
     * 批量插入
     * @param insertList
     * @return
     */
    int insertList(List<LhScheduleResultLog> insertList);

}
