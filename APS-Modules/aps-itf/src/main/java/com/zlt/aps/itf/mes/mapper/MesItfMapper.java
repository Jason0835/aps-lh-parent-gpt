package com.zlt.aps.itf.mes.mapper;

import com.baomidou.dynamic.datasource.annotation.DS;
import com.zlt.aps.itf.constant.DataSource;
import com.zlt.aps.itf.mes.vo.MoldAlterPlanIssue;
import com.zlt.aps.itf.vo.*;
import com.zlt.aps.mp.api.domain.entity.*;
    import com.zlt.aps.cx.api.domain.entity.CxMachineOnlineInfo;
import com.zlt.aps.cx.api.domain.entity.CxMesStock;
import com.zlt.aps.cx.api.domain.entity.CxScheFinishQty;
import com.zlt.aps.cx.api.domain.entity.CxDayFinishQty;
import com.zlt.aps.lh.api.domain.entity.LhMachineOnlineInfo;
import com.zlt.aps.lh.api.domain.entity.LhScheFinishQty;
import com.zlt.aps.lh.api.domain.entity.LhDayFinishQty;
import com.zlt.aps.lh.api.domain.entity.LhMoldAlterPlanFinish;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

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
     * @param cxMachineOnlineInfo 参数
     * @return 列表
     */
    List<CxMachineOnlineInfo> selectCxMachineOnlineSyncList(CxMachineOnlineInfo cxMachineOnlineInfo);

    /**
     * 查询硫化在机同步数据
     *
     * @param lhMachineOnlineInfo 参数
     * @return 列表
     */
    List<LhMachineOnlineInfo> selectLhMachineOnlineSyncList(LhMachineOnlineInfo lhMachineOnlineInfo);

    /**
     * 查询设备保养计划同步数据
     *
     * @param syncDataLogs 参数
     * @return 列表
     */
    List<DevMaintenancePlan> selectDevMaintenancePlanList(AuxReqSyncDataLogs syncDataLogs);

    /**
     * 查询胶囊已使用次数同步数据
     *
     * @param syncDataLogs 参数
     * @return 列表
     */
    List<LhRepairCapsuleVo> selectLhRepairCapsuleList(AuxReqSyncDataLogs syncDataLogs);

    /**
     * 查询模具清洗预警计划同步数据
     *
     * @param syncDataLogs 参数
     * @return 列表
     */
    List<MouldCleanPlanVo> selectMouldCleanPlanList(AuxReqSyncDataLogs syncDataLogs);

    /**
     * 查询结构整车胎面配置同步数据
     *
     * @param syncDataLogs 参数
     * @return 列表
     */
    List<StructureTreadConfigVo> selectStructureTreadConfigList(AuxReqSyncDataLogs syncDataLogs);

    /**
     * 查询生胎库存同步数据
     *
     * @param syncDataLogs 参数
     * @return 列表
     */
    List<CxMesStock> selectMesCxStockList(AuxReqSyncDataLogs syncDataLogs);

    /**
     * 查询胎面库存同步数据
     *
     * @param syncDataLogs 参数
     * @return 列表
     */
    List<MdmTreadStock> selectTreadStockList(AuxReqSyncDataLogs syncDataLogs);

    /**
     * 查询成型排程完成量同步数据
     *
     * @param syncDataLogs 参数
     * @return 列表
     */
    List<CxScheFinishQty> selectCxClassShiftFinishQtyList(AuxReqSyncDataLogs syncDataLogs);

    /**
     * 查询硫化排程完成量同步数据
     *
     * @param syncDataLogs 参数
     * @return 列表
     */
    List<LhScheFinishQty> selectLhClassShiftFinishQtyList(AuxReqSyncDataLogs syncDataLogs);

    /**
     * 查询成型排程日完成量同步数据
     *
     * @param syncDataLogs 参数
     * @return 列表
     */
    List<CxDayFinishQty> selectCxScheDayFinishQtyList(AuxReqSyncDataLogs syncDataLogs);

    /**
     * 查询硫化排程日完成量同步数据
     *
     * @param syncDataLogs 参数
     * @return 列表
     */
    List<LhDayFinishQty> selectLhScheDayFinishQtyList(AuxReqSyncDataLogs syncDataLogs);

    /**
     * 插入模具交替计划到中间表
     *
     * @param moldAlterPlanList 模具交替计划列表
     * @return 插入数量
     */
    int insertMoldAlterPlanList(@Param("list") List<MoldAlterPlanIssue> moldAlterPlanList);

    /**
     * 查询模具交替计划完成回报同步数据
     *
     * @param syncDataLogs 参数
     * @return 列表
     */
    List<LhMoldAlterPlanFinish> selectMoldAlterPlanFinishList(AuxReqSyncDataLogs syncDataLogs);
}
