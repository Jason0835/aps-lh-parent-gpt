package com.zlt.aps.tq.service;

import com.zlt.aps.tq.api.domain.entity.TqWarningRecord;
import com.zlt.bill.common.service.IDocService;

import java.util.List;

/**
 * 胎圈排程预警记录Service接口
 *
 * <p>提供预警记录的增删改查、批量保存和处理状态更新功能。</p>
 *
 * @author APS
 */
public interface ITqWarningRecordService extends IDocService<TqWarningRecord> {

    /**
     * 批量保存预警记录
     *
     * @param warningRecords 预警记录列表
     * @return 保存成功的记录数
     */
    int saveBatchWarningRecords(List<TqWarningRecord> warningRecords);

    /**
     * 处理预警记录（更新处理状态、处理人和处理意见）
     *
     * @param id       预警记录ID
     * @param handler  处理人
     * @param opinion  处理意见
     * @return 操作结果
     */
    int handleWarning(Long id, String handler, String opinion);
}
