package com.zlt.aps.cd90.service;

import com.zlt.aps.cd90.api.domain.entity.Cd90ShiftStock;

import java.util.Date;
import java.util.List;

/**
 * 直裁自动滚动班次库存写入服务。
 */
public interface ICd90ShiftStockService {

    /** 替换指定交班范围的库存快照，空集合表示清空。 */
    void replaceShiftStock(String factoryCode, Date stockDate, String shiftCode,
                           Date shiftStartTime, String updateBy, List<Cd90ShiftStock> stockList);
}
