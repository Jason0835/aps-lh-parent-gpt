package com.zlt.aps.tq.service;

import com.zlt.aps.tq.api.domain.entity.TqRollingLog;
import com.zlt.bill.common.service.IDocService;

/**
 * 胎圈排程滚动更新日志Service接口
 *
 * @author APS
 */
public interface ITqRollingLogService extends IDocService<TqRollingLog> {

    /**
     * 根据批次号查询滚动更新日志
     *
     * @param batchNo 滚动批次号
     * @return 滚动更新日志
     */
    TqRollingLog selectByBatchNo(String batchNo);
}
