package com.zlt.aps.tq.service;

import com.zlt.aps.tq.api.domain.entity.TqShiftStock;
import com.zlt.bill.common.service.IDocService;

import java.util.Date;
import java.util.List;

/**
 * 胎圈自动滚动班次库存服务。
 *
 * <p>对齐胎面 ITmShiftStockService，提供班次库存快照替换能力，
 * 供 aps-itf 的 syncBeadShiftStock 通过 Feign 远程调用。</p>
 *
 * @author APS
 */
public interface ITqShiftStockService extends IDocService<TqShiftStock> {

    /**
     * 替换指定工厂、物理日和班序的库存快照。
     *
     * <p>对齐胎面 TmShiftStockServiceImpl.replaceShiftStock，
     * 先逻辑删除旧快照（IS_DELETE=1），再批量插入新快照（IS_DELETE=0），
     * 空集合也会先失效旧快照。</p>
     *
     * @param factoryCode 工厂编码
     * @param stockDate MES库存物理日期
     * @param shiftOrder 班次顺序（1~6）
     * @param updateBy 更新人
     * @param stockList 新库存快照
     * @throws com.ruoyi.common.exception.ServiceException 工厂、日期或班次非法时抛出
     */
    void replaceShiftStock(String factoryCode, Date stockDate, Integer shiftOrder,
                           String updateBy, List<TqShiftStock> stockList);
}
