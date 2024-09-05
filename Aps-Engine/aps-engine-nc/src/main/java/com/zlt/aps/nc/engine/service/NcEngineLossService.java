package com.zlt.aps.nc.engine.service;

import java.util.Map;

/**
 * 损耗率service
 */
public interface NcEngineLossService {

    /**
     * 把损耗率list转成map
     * 获得损耗率（从损耗率表获取对应的损耗率，获取顺序：机台+物料编号 > 机台 >工序参数配置）
     */
    Map<String, Double> getLossRateMap();

    /**
     * 获得损耗率（从损耗率表获取对应的损耗率，获取顺序：机台+物料编号 > 机台 >工序参数配置）
     *
     * @param liningCode  内衬代码
     * @param machineIds 机台id
     * @param paramLossRate 工序参数设置的损耗率
     * @return
     */
    double getLossRate(String liningCode, String machineIds, Map<String, Double> lossMap, double paramLossRate);
}
