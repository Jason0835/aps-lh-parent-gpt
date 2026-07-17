package com.zlt.aps.tc.service;

import com.zlt.aps.tc.api.domain.entity.TcDayFinishQty;
import com.zlt.bill.common.service.IDocService;

import java.util.Date;
import java.util.List;

/**
 * 胎侧日完成量MES快照服务。
 */
public interface ITcDayFinishQtyService extends IDocService<TcDayFinishQty> {

    /**
     * 失效旧快照并批量保存MES最新日完成量。
     *
     * @param factoryCode 工厂编码
     * @param scheduleDate MES业务日期
     * @param updateBy 更新人
     * @param insertList 日完成量列表
     */
    void logicDeleteAndSaveBatch(String factoryCode, Date scheduleDate, String updateBy,
                                 List<TcDayFinishQty> insertList);
}
