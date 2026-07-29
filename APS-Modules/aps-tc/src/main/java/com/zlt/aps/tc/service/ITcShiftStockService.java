package com.zlt.aps.tc.service;

import com.zlt.aps.tc.api.domain.entity.TcShiftStock;
import com.zlt.bill.common.service.IDocService;

import java.util.Date;
import java.util.List;

/**
 * 胎侧自动滚动班次库存服务。
 */
public interface ITcShiftStockService extends IDocService<TcShiftStock> {

    /**
     * 替换指定工厂、物理日和班序的库存快照。
     *
     * @param factoryCode 工厂编码
     * @param stockDate MES库存物理日期
     * @param shiftOrder 班次顺序
     * @param updateBy 更新人
     * @param stockList 新库存快照，空集合表示清空快照
     * @throws com.ruoyi.common.exception.ServiceException 参数非法时抛出
     */
    void replaceShiftStock(String factoryCode, Date stockDate, Integer shiftOrder,
                           String updateBy, List<TcShiftStock> stockList);
}
