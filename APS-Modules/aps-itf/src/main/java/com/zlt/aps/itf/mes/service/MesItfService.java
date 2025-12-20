package com.zlt.aps.itf.mes.service;

import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.aps.monthplan.api.domain.entity.MdmModelInfo;
import com.zlt.aps.monthplan.api.domain.entity.MdmSkuMouldRel;
import com.zlt.aps.monthplan.api.domain.entity.ProductStockMonth;

import java.util.List;

/**
 * @author Chen
 * @since 2025/12/16
 */
public interface MesItfService {

    /**
     * 同步产品模型关系
     *
     * @param mdmSkuMouldRel SAP与模具关系
     * @return 结果
     */
    AjaxResult syncProductModRelation(MdmSkuMouldRel mdmSkuMouldRel);

    /**
     * 同步模具台账
     *
     * @param modelInfo 模具台账
     * @return 结果
     */
    AjaxResult syncModelInfo(MdmModelInfo modelInfo);

    /**
     * 获取模具台账List
     *
     * @param modelInfo 模具台账
     * @return 结果
     */
    List<MdmModelInfo> getMdmModelInfoList(MdmModelInfo modelInfo);

    /**
     * 获取AP与模具关系
     *
     * @param mdmSkuMouldRel SAP与模具关系
     * @return 结果
     */
    List<MdmSkuMouldRel> getMdmSkuMouldRelList(MdmSkuMouldRel mdmSkuMouldRel);

    /**
     * 同步成品库存
     *
     * @param productStockMonth 参数
     * @return 结果
     */
    List<ProductStockMonth> getProductStock(ProductStockMonth productStockMonth);

    /**
     * 同步不合格库存
     *
     * @param productStockMonth 参数
     * @return 结果
     */
    List<ProductStockMonth> syncUnqualifiedStock(ProductStockMonth productStockMonth);
}
