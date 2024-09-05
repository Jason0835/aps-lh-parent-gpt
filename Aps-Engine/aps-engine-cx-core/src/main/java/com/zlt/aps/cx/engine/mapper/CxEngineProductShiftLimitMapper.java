package com.zlt.aps.cx.engine.mapper;

import com.zlt.aps.cx.engine.domain.CxEngineProductDimensionLimit;
import com.zlt.aps.cx.engine.domain.CxEngineProductMachineLimit;
import com.zlt.aps.cx.engine.domain.CxEngineProductStockLimit;

import java.util.List;

/**
 * 可投产硫化班次限定设置数据接口
 */
public interface CxEngineProductShiftLimitMapper {
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
}
