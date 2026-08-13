package com.zlt.aps.cd15.service;

import com.zlt.aps.cd15.api.domain.entity.Cd15ShiftStock;

import java.util.Date;
import java.util.List;

/**
 * 斜裁自动滚动班次库存写入服务。
 */
public interface ICd15ShiftStockService {

    /** 替换指定交班范围的库存快照，空集合表示清空。 */
    void replaceShiftStock(String factoryCode, Date stockDate, String shiftCode,
                           Date shiftStartTime, String updateBy, List<Cd15ShiftStock> stockList);
}
