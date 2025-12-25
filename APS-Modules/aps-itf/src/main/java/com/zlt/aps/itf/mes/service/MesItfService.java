package com.zlt.aps.itf.mes.service;

import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.aps.itf.vo.AuxReqSyncDataLogs;
import com.zlt.aps.monthplan.api.domain.entity.*;

import java.text.ParseException;
import java.util.List;

/**
 * @author Chen
 * @since 2025/12/16
 */
public interface MesItfService {

    /**
     * 同步产品模型关系
     *
     * @param mdmSkuMouldRel SKU与模具关系
     * @return 结果
     */
    AjaxResult syncProductModRelation(AuxReqSyncDataLogs mdmSkuMouldRel);

    /**
     * 同步模具台账
     *
     * @param modelInfo 模具台账
     * @return 结果
     */
    AjaxResult syncModelInfo(AuxReqSyncDataLogs modelInfo);

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
     * @param mdmSkuMouldRel SKU与模具关系
     * @return 结果
     */
    List<MdmSkuMouldRel> getMdmSkuMouldRelList(MdmSkuMouldRel mdmSkuMouldRel);

    /**
     * 同步成品库存
     *
     * @param productStockMonth 参数
     * @throws ParseException 异常
     * @return 结果
     */
    AjaxResult syncProductStock(MdmProductStock productStockMonth) throws ParseException;

    /**
     * 获取成品库存
     *
     * @param productStockMonth 参数
     * @return 结果
     */
    List<MdmProductStock> getProductStock(MdmProductStock productStockMonth);

    /**
     * 获取不合格库存
     *
     * @param mdmUnqualifiedStock 参数
     * @return 结果
     */
    List<MdmUnqualifiedStock> getUnqualifiedStock(MdmUnqualifiedStock mdmUnqualifiedStock);

    /**
     * 同步不合格库存
     *
     * @param mdmUnqualifiedStock 参数
     * @throws ParseException 异常
     * @return 结果
     */
    AjaxResult syncUnqualifiedStock(MdmUnqualifiedStock mdmUnqualifiedStock) throws ParseException;

    /**
     * 同步特殊材料库存
     *
     * @param rawSpecialMaterialStock 参数
     * @throws ParseException 异常
     * @return 结果
     */
    AjaxResult syncRawSpecialMaterialStock(RawSpecialMaterialStock rawSpecialMaterialStock) throws ParseException;

    /**
     * 查询特殊材料库存
     *
     * @param rawSpecialMaterialStock 参数
     * @return 结果
     */
    List<RawSpecialMaterialStock> getRawSpecialMaterialStock(RawSpecialMaterialStock rawSpecialMaterialStock);

    /**
     * 同步原材料出库
     *
     * @param materialOutboundRecord 参数
     * @throws ParseException 异常
     * @return 结果
     */
    AjaxResult syncRawMaterialOutboundRecord(RawMaterialOutboundRecord materialOutboundRecord) throws ParseException;

    /**
     * 同步成品物料信息
     *
     * @param syncDataLogs 参数
     * @return 结果
     */
    AjaxResult syncMaterial(AuxReqSyncDataLogs syncDataLogs);

    /**
     * 同步模壳台账信息
     *
     * @param syncDataLogs 参数
     * @return 结果
     */
    AjaxResult syncMoldShell(AuxReqSyncDataLogs syncDataLogs);
}
