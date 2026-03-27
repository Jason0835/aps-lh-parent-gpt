package com.zlt.aps.itf.mes.mapper;

import com.baomidou.dynamic.datasource.annotation.DS;
import com.zlt.aps.itf.constant.DataSource;
import com.zlt.aps.itf.vo.AuxReqSyncDataLogs;
import com.zlt.aps.mp.api.domain.entity.*;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * MES接口Mapper
 *
 * @author Chen
 * @since 2025/12/16
 */
@DS(DataSource.MES)
@Mapper
public interface MesItfMapper {

    /**
     * 查询SKU与模具关系列表
     *
     * @param mdmSkuMouldRel 查询参数
     * @return 结果
     */
    List<MdmSkuMouldRel> selectSkuMouldRelList(MdmSkuMouldRel mdmSkuMouldRel);

    /**
     * 模具台账列表
     *
     * @param modelInfo 模具台账
     * @return 列表
     */
    List<MdmModelInfo> selectModelInfoList(MdmModelInfo modelInfo);

    /**
     * 获取成品物料信息
     *
     * @param syncDataLogs 参数
     * @return 结果
     */
    List<MdmMaterialInfo> selectMaterialList(AuxReqSyncDataLogs syncDataLogs);

    /**
     * 获取模壳台账信息
     *
     * @param syncDataLogs 参数
     * @return 结果
     */
    List<MdmMouldShellInfo> selectMoldShellList(AuxReqSyncDataLogs syncDataLogs);

    /**
     * 获取原材料出库记录列表
     *
     * @param syncDataLogs 同步数据日志
     * @return 列表
     */
    List<RawMaterialOutboundRecord> syncRawMaterialOutboundRecord(AuxReqSyncDataLogs syncDataLogs);

    /**
     * 查询成型在机同步数据
     *
     * @param mdmCxMachineOnlineInfo 参数
     * @return 列表
     */
    List<MdmCxMachineOnlineInfo> selectCxMachineOnlineSyncList(MdmCxMachineOnlineInfo mdmCxMachineOnlineInfo);

    /**
     * 查询硫化在机同步数据
     *
     * @param mdmLhMachineOnlineInfo 参数
     * @return 列表
     */
    List<MdmLhMachineOnlineInfo> selectLhMachineOnlineSyncList(MdmLhMachineOnlineInfo mdmLhMachineOnlineInfo);

    /**
     * 查询设备保养计划同步数据
     *
     * @param syncDataLogs 参数
     * @return 列表
     */
    List<MdmDevMaintenancePlan> selectDevMaintenancePlanList(AuxReqSyncDataLogs syncDataLogs);

    /**
     * 查询胶囊已使用次数同步数据
     *
     * @param syncDataLogs 参数
     * @return 列表
     */
    List<MdmLhRepairCapsule> selectLhRepairCapsuleList(AuxReqSyncDataLogs syncDataLogs);

    /**
     * 查询模具清洗预警计划同步数据
     *
     * @param syncDataLogs 参数
     * @return 列表
     */
    List<MdmMouldCleanPlan> selectMouldCleanPlanList(AuxReqSyncDataLogs syncDataLogs);

    /**
     * 查询结构整车胎面配置同步数据
     *
     * @param syncDataLogs 参数
     * @return 列表
     */
    List<MdmStructureTreadConfig> selectStructureTreadConfigList(AuxReqSyncDataLogs syncDataLogs);

    /**
     * 查询生胎库存同步数据
     *
     * @param syncDataLogs 参数
     * @return 列表
     */
    List<MdmMesCxStock> selectMesCxStockList(AuxReqSyncDataLogs syncDataLogs);

    /**
     * 查询成型排程完成量同步数据
     *
     * @param syncDataLogs 参数
     * @return 列表
     */
    List<MdmCxScheFinishQty> selectCxClassShiftFinishQtyList(AuxReqSyncDataLogs syncDataLogs);

    /**
     * 查询硫化排程完成量同步数据
     *
     * @param syncDataLogs 参数
     * @return 列表
     */
    List<MdmLhScheFinishQty> selectLhClassShiftFinishQtyList(AuxReqSyncDataLogs syncDataLogs);

    /**
     * 查询成型排程日完成量同步数据
     *
     * @param syncDataLogs 参数
     * @return 列表
     */
    List<MdmCxScheDayFinishQty> selectCxScheDayFinishQtyList(AuxReqSyncDataLogs syncDataLogs);

    /**
     * 查询硫化排程日完成量同步数据
     *
     * @param syncDataLogs 参数
     * @return 列表
     */
    List<MdmLhScheDayFinishQty> selectLhScheDayFinishQtyList(AuxReqSyncDataLogs syncDataLogs);
}
