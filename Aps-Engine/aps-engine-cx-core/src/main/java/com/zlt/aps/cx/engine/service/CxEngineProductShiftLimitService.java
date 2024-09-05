package com.zlt.aps.cx.engine.service;

import com.zlt.aps.cx.engine.domain.CxEngineProductDimensionLimit;
import com.zlt.aps.cx.engine.domain.CxEngineProductMachineLimit;
import com.zlt.aps.cx.engine.domain.CxEngineProductStockLimit;
import com.zlt.aps.cx.engine.exception.CxScheduleEngineException;

import java.util.List;
import java.util.Map;


/**
 * 可硫化班次投产限定
 */
public interface CxEngineProductShiftLimitService
{
    /**
     *  胎胚类型库存限制设定
     * @param cxEngineProductStockLimit
     * @return
     */
    List<CxEngineProductStockLimit> selectCxProductShiftStockLimitList(CxEngineProductStockLimit cxEngineProductStockLimit);

    /**
     * 同寸口一班平均可硫化班次设定可投产硫化班数
     * @param cxEngineProductDimensionLimit
     * @return
     */
    List<CxEngineProductDimensionLimit> selectCxEngineProductDimensionLimitList(CxEngineProductDimensionLimit cxEngineProductDimensionLimit);

    /**
     * 同机台一班可硫化班次设定可投产硫化班产数限定
     * @param cxEngineProductMachineLimit
     * @return
     */
    List<CxEngineProductMachineLimit> selectCxEngineProductMachineLimitList(CxEngineProductMachineLimit cxEngineProductMachineLimit);

    /**
     * 第一轮根据胎胚类型可投产设定调整班数
     * @param key 轮胎类型
     * @param embryoCodeTypeTotalMap
     * @return
     */
    public Double adjustLhShiftCountByStock(String key, Map<String, Integer> embryoCodeTypeTotalMap, Map<String, String> cxParams, List<CxEngineProductStockLimit> productStockLimits, StringBuilder logDetail) throws CxScheduleEngineException;

    /**
     * 根据寸口的一班可硫化班次进行数据获取
     * @param dimension
     * @param dimensionAvgLhShift
     * @return
     */
    public Double adjustLhShiftCountByDimension(Double dimension, Double dimensionAvgLhShift, List<CxEngineProductDimensionLimit> productDimensionLimits, StringBuilder logDetail);

    /**
     * 根据同机台一班可硫化班次进行数据获取
     * @param avgAvailableLhShift
     * @param productMachineLimits
     * @return
     */
    public Double adjustLhShiftCountByMachine(Double avgAvailableLhShift, List<CxEngineProductMachineLimit> productMachineLimits, StringBuilder logDetail);


}
