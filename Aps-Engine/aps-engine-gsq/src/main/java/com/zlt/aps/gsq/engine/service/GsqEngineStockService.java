package com.zlt.aps.gsq.engine.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 库存service
 */
public interface GsqEngineStockService {

    /**
     * 计算胎面16点预计库存
     * @param batchNo 排程批次号
     * @param scheduleDate 排程日期
     * @param stockLossRate 库存损耗率
     * @return
     */
    Map<String, Double> getPlanStockMap(String batchNo, String scheduleDate, Double stockLossRate);

    /**
     * 取预生产库存倍数Map
     * @param steelRingCodeList 要查询的steelRingCode列表
     * @param reserveStockRate 预生产库存倍数
     * @return 结果
     */
    Map<String, BigDecimal> getReserveStockMap(List<String> steelRingCodeList, Double reserveStockRate);
}
