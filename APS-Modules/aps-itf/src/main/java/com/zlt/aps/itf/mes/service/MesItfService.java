package com.zlt.aps.itf.mes.service;

import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.aps.itf.vo.AuxReqSyncDataLogs;
import com.zlt.aps.itf.vo.MesBrandDict;
import com.zlt.aps.mp.api.domain.entity.*;

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
     * @return 结果
     * @throws ParseException 异常
     */
    AjaxResult syncProductStock(MdmProductStock productStockMonth) throws ParseException;

    /**
     * 生成超期SKU
     *
     * @param mdmProductStock 参数
     * @return 结果
     */
    AjaxResult genOverDueSkuByStock(MdmProductStock mdmProductStock);

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
     * @return 结果
     * @throws ParseException 异常
     */
    AjaxResult syncUnqualifiedStock(MdmUnqualifiedStock mdmUnqualifiedStock) throws ParseException;

    /**
     * 同步特殊材料库存
     *
     * @param rawSpecialMaterialStock 参数
     * @return 结果
     * @throws ParseException 异常
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
     * @param syncDataLogs 参数
     * @return 结果
     * @throws ParseException 异常
     */
    AjaxResult syncRawMaterialOutboundRecord(AuxReqSyncDataLogs syncDataLogs) throws ParseException;

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

    /**
     * 查询品牌信息
     *
     * @return 结果
     */
    List<MesBrandDict> selectMesBrandDict();

    /**
     * 同步成型在机数据
     *
     * @param mdmCxMachineOnlineInfo 参数
     * @return 结果
     */
    AjaxResult syncMachineOnlineInfo(MdmCxMachineOnlineInfo mdmCxMachineOnlineInfo);

    /**
     * 同步硫化在机数据
     *
     * @param mdmLhMachineOnlineInfo 参数
     * @return 结果
     */
    AjaxResult syncLhMachineOnlineInfo(MdmLhMachineOnlineInfo mdmLhMachineOnlineInfo);

    /**
     * 同步设备保养计划
     *
     * @param syncDataLogs 同步参数
     * @return 结果
     */
    AjaxResult syncDevMaintenancePlan(AuxReqSyncDataLogs syncDataLogs);

    /**
     * 同步胶囊已使用次数
     *
     * @param syncDataLogs 同步参数
     * @return 结果
     */
    AjaxResult syncLhRepairCapsule(AuxReqSyncDataLogs syncDataLogs);

    /**
     * 同步模具清洗预警计划
     *
     * @param syncDataLogs 同步参数
     * @return 结果
     */
    AjaxResult syncMouldCleanPlan(AuxReqSyncDataLogs syncDataLogs);

    /**
     * 同步结构整车胎面配置
     *
     * @param syncDataLogs 同步参数
     * @return 结果
     */
    AjaxResult syncStructureTreadConfig(AuxReqSyncDataLogs syncDataLogs);

    /**
     * 同步生胎库存
     *
     * @param syncDataLogs 同步参数
     * @return 结果
     */
    AjaxResult syncMesCxStock(AuxReqSyncDataLogs syncDataLogs);

    /**
     * 同步成型排程完成量
     * 采用更新删除标识模式，而不是先删后插
     *
     * @param syncDataLogs 同步参数
     * @return 结果
     */
    AjaxResult syncCxClassShiftFinishQty(AuxReqSyncDataLogs syncDataLogs);

    /**
     * 同步硫化排程完成量
     * 采用更新删除标识模式，而不是先删后插
     *
     * @param syncDataLogs 同步参数
     * @return 结果
     */
    AjaxResult syncLhClassShiftFinishQty(AuxReqSyncDataLogs syncDataLogs);

    /**
     * 同步成型排程日完成量
     * 采用更新删除标识模式，而不是先删后插
     *
     * @param syncDataLogs 同步参数
     * @return 结果
     */
    public AjaxResult syncCxScheDayFinishQty(AuxReqSyncDataLogs syncDataLogs);

    /**
     * 同步硫化排程日完成量
     *
     * @param syncDataLogs
     * @return
     */
    public AjaxResult syncLhScheDayFinishQty(AuxReqSyncDataLogs syncDataLogs);

    /**
     * 模具交替计划下发到MES
     *
     * @param moldAlterPlanList
     * @return
     */
    public AjaxResult issueMoldAlterPlan(List<MdmMoldAlterPlan> moldAlterPlanList);

    /**
     * 同步模具交替计划完成回报
     *
     * @param syncDataLogs
     * @return
     */
    public AjaxResult syncMoldAlterPlanFinish(AuxReqSyncDataLogs syncDataLogs);

    /**
     * 同步出库未扫描订单
     *
     * @param outbountOrdersNotScan 参数
     * @return 结果
     */
    AjaxResult syncOutbountOrdersNotScan(MdmOutbountOrdersNotScan outbountOrdersNotScan);

    /**
     * 查询出库未扫描订单
     *
     * @param outbountOrdersNotScan 参数
     * @return 结果
     */
    List<MdmOutbountOrdersNotScan> getOutbountOrdersNotScan(MdmOutbountOrdersNotScan outbountOrdersNotScan);
}