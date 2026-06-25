package com.zlt.aps.tq.service;

import com.zlt.aps.tq.api.domain.entity.TqDayFinishQty;
import com.zlt.bill.common.service.IDocService;

import java.util.Date;
import java.util.List;

/**
 * 胎圈排程日完成量回报Service接口
 *
 * @author APS Team
 * @since 2026/06/18
 */
public interface ITqDayFinishQtyService extends IDocService<TqDayFinishQty> {

    /**
     * 逻辑删除分厂指定排程日期的旧数据并批量插入新数据（事务性操作）
     *
     * @param factoryCode  分厂编号
     * @param scheduleDate 排程日期
     * @param updateBy     更新者
     * @param insertList   待插入的数据列表
     */
    void logicDeleteAndSaveBatch(String factoryCode, Date scheduleDate, String updateBy, List<TqDayFinishQty> insertList);
}
