package com.zlt.aps.itf.mes.mapper;

import com.baomidou.dynamic.datasource.annotation.DS;
import com.zlt.aps.itf.constant.DataSource;
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
     * 按指定版本号查询硫化排程完成量同步数据（不限日期），用于临时任务
     *
     * @param syncDataLogs 参数（dataVersion必传）
     * @return 列表
     */
    List<LhScheFinishQty> selectLhClassShiftFinishQtyByVersion(AuxReqSyncDataLogs syncDataLogs);

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
     * 查询模具交替计划完成回报同步数据
     *
     * @param syncDataLogs 参数
     * @return 列表
     */
    List<LhMoldAlterPlanFinish> selectMoldAlterPlanFinishList(AuxReqSyncDataLogs syncDataLogs);

    /**
     * 查询MES设备保养计划中硫化精度实际执行日期回填数据
     * 从DEV_MAINTENANCE_PLAN表获取FIRST_WASH_TIME不为空的硫化精度记录
     *
     * @param syncDataLogs 参数
     * @return 列表
     */
    List<DevMaintenancePlan> selectLhPrecisionPlanActualList(AuxReqSyncDataLogs syncDataLogs);

    /**
     * 查询MES中间表设备保养计划指定精度类型的最大版本号
     * 版本号格式如：APS_MES_AH01_20260510120430003，字符串MAX比较即可获取最新版本
     *
     * @param precisionType 精度类型（如：硫化精度）
     * @return 最大版本号，无数据时返回null
     */
    String selectMaxDataVersionFromMes(@Param("precisionType") String precisionType);

    /**
     * 查询MES中间表模具清洗预警计划的最大版本号
     * 只取最新版本的数据进行同步
     *
     * @param factoryCode 分厂编码（可选）
     * @return 最大版本号，无数据时返回null
     */
    String selectMaxDataVersionFromMouldCleanPlan(@Param("factoryCode") String factoryCode);

    /**
     * 查询MES硫化排程日完成量中间表的最大版本号，用于临时任务
     *
     * @param factoryCode 分厂编码（可选）
     * @return 最大版本号，无数据时返回null
     */
    String selectMaxDataVersionFromLhDayFinishQty(@Param("factoryCode") String factoryCode);

    /**
     * 查询成型在机历史同步数据（今天之前每天最新版本）
     * 按日期+成型机台分组，取每天每个机台的MAX(DATA_VERSION)
     *
     * @param cxMachineOnlineInfo 参数（factoryCode可选）
     * @return 列表
     */
    List<CxMachineOnlineInfo> selectCxMachineOnlineHistorySyncList(CxMachineOnlineInfo cxMachineOnlineInfo);

    /**
     * 查询硫化在机历史同步数据（今天之前每天最新版本）
     * 按日期+硫化机台分组，取每天每个机台的MAX(DATA_VERSION)
     *
     * @param lhMachineOnlineInfo 参数（factoryCode可选）
     * @return 列表
     */
    List<LhMachineOnlineInfo> selectLhMachineOnlineHistorySyncList(LhMachineOnlineInfo lhMachineOnlineInfo);

    /**
     * 查询胶囊已使用次数历史同步数据（今天之前每天最新版本）
     * 按日期+硫化机台分组，取每天每个机台的MAX(DATA_VERSION)
     *
     * @param syncDataLogs 参数
     * @return 列表
     */
    List<LhRepairCapsuleVo> selectLhRepairCapsuleHistoryList(AuxReqSyncDataLogs syncDataLogs);

    /**
     * 查询生胎库存历史同步数据（今天之前的数据）
     *
     * @param syncDataLogs 参数
     * @return 列表
     */
    List<CxMesStock> selectMesCxStockHistoryList(AuxReqSyncDataLogs syncDataLogs);

    /**
     * 查询硫化排程完成量历史同步数据（今天之前每天最新版本）
     * 按日期+硫化机台+订单号分组，取每天每组的MAX(DATA_VERSION)
     *
     * @param syncDataLogs 参数
     * @return 列表
     */
    List<LhScheFinishQty> selectLhClassShiftFinishQtyHistoryList(AuxReqSyncDataLogs syncDataLogs);

    /**
     * 查询硫化排程日完成量历史同步数据（今天之前每天最新版本）
     * 按日期+分厂分组，取每天每组的MAX(DATA_VERSION)
     *
     * @param syncDataLogs 参数
     * @return 列表
     */
    List<LhDayFinishQty> selectLhScheDayFinishQtyHistoryList(AuxReqSyncDataLogs syncDataLogs);

    /**
     * 查询成型排程完成量历史同步数据（今天之前每天最新版本）
     * 按日期+成型机台+订单号分组，取每天每组的MAX(DATA_VERSION)
     *
     * @param syncDataLogs 参数
     * @return 列表
     */
    List<CxScheFinishQty> selectCxClassShiftFinishQtyHistoryList(AuxReqSyncDataLogs syncDataLogs);

    /**
     * 查询成型排程日完成量历史同步数据（今天之前每天最新版本）
     * 按日期+分厂+胚胎编码+施工版本分组，取每天每组的MAX(DATA_VERSION)
     *
     * @param syncDataLogs 参数
     * @return 列表
     */
    List<CxDayFinishQty> selectCxScheDayFinishQtyHistoryList(AuxReqSyncDataLogs syncDataLogs);

    /**
     * 查询MES中间表模具清洗预警计划的所有版本号（升序排列）
     * 用于临时任务按版本迭代同步全部预警数据
     *
     * @param factoryCode 分厂编码（可选）
     * @return 版本号列表，按升序排列
     */
    List<String> selectAllDataVersionsFromMouldCleanPlan(@Param("factoryCode") String factoryCode);
}
