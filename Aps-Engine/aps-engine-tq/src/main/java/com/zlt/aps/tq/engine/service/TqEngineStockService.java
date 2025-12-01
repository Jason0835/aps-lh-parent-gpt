package com.zlt.aps.tq.engine.service;

import java.util.Map;

/**
 * 库存service
 */
public interface TqEngineStockService {

    /**
     * 计算胎面隔天7点预计库存
     * @param batchNo 排程批次号
     * @param scheduleDate 排程日期
     * @param stockLossRate 库存损耗率
     * @return
     */
    Map<String, Double> getPlanStockMap(String batchNo, String scheduleDate, Double stockLossRate);
    
    /**
     * 计算胎面夜班预计库存
     * @param batchNo 排程批次号
     * @param scheduleDate 排程日期
     * @param stockLossRate 库存损耗率
     * @return
     */
    Map<String, Double> getNightStockMap(String batchNo, String scheduleDate, Double stockLossRate);
}
