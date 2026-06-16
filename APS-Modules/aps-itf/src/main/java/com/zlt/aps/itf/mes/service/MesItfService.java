package com.zlt.aps.itf.mes.service;

import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.aps.cx.api.domain.entity.CxMachineOnlineInfo;
import com.zlt.aps.itf.vo.AuxReqSyncDataLogs;
import com.zlt.aps.itf.vo.MesBrandDict;
import com.zlt.aps.lh.api.domain.entity.LhMachineOnlineInfo;
import com.zlt.aps.mdm.api.domain.entity.MdmMoldAlterPlan;
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
     * @param cxMachineOnlineInfo 参数
     * @return 结果
     */
    AjaxResult syncMachineOnlineInfo(CxMachineOnlineInfo cxMachineOnlineInfo);

    /**
     * 同步硫化在机数据
     *
     * @param lhMachineOnlineInfo 参数
     * @return 结果
     */
    AjaxResult syncLhMachineOnlineInfo(LhMachineOnlineInfo lhMachineOnlineInfo);

    /**
     * 同步设备保养计划
     *
     * @param syncDataLogs 同步参数
     * @return 结果
     */
    AjaxResult syncDevMaintenancePlan(AuxReqSyncDataLogs syncDataLogs);

    /**
     * 仅同步设备保养计划数据，不触发自动生成精度计划逻辑
     *
     * @param syncDataLogs 同步参数（可指定精度类型）
     * @return 同步结果
     */
    AjaxResult syncDevMaintenancePlanOnly(AuxReqSyncDataLogs syncDataLogs);

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
    AjaxResult syncMouldCleanWarn(AuxReqSyncDataLogs syncDataLogs);

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
     * 同步胎面库存
     * 采用更新删除标识模式，而不是先删后插
     *
     * @param syncDataLogs 同步参数
     * @return 结果
     */
    AjaxResult syncTreadStock(AuxReqSyncDataLogs syncDataLogs);

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
     * 按指定版本号同步硫化排程完成量（临时任务）
     * 与原syncLhClassShiftFinishQty的区别：不限日期，按指定版本号查询MES中间表所有日期数据
     * 同步后同样回填排程结果
     *
     * @param dataVersion 指定版本号
     * @return 结果
     */
    AjaxResult syncLhClassShiftFinishQtyByVersion(String dataVersion);

    /**
     * 按上一天最新版本号同步硫化排程完成量（临时任务）
     * 逻辑同syncLhClassShiftFinishQty（抓当天最新版本），但日期条件改为上一天
     *
     * @param syncDataLogs 同步参数
     * @return 结果
     */
    AjaxResult syncLhClassShiftFinishQtyByYesterday(AuxReqSyncDataLogs syncDataLogs);

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
     * 按最新版本号同步硫化排程日完成量（临时任务）
     * 与原syncLhScheDayFinishQty的区别：不限日期（去掉前一天日期条件），取MES中间表最新版本号查询所有日期数据
     *
     * @return 结果
     */
    AjaxResult syncLhScheDayFinishQtyByLatestVersion(String dataVersion);

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

    /**
     * 同步MES硫化精度计划实际执行日期回填数据
     * MES在实际做精度的时间点把实际精度时间回填到中间表通知APS抓取
     *
     * @param syncDataLogs 同步参数
     * @return 结果
     */
    AjaxResult syncLhPrecisionPlanActual(AuxReqSyncDataLogs syncDataLogs);

    /**
     * 下发硫化精度计划到MES
     * 查询计划排程精度日期有值且实际执行日期为空的数据，下发到MES中间表
     *
     * @param factoryCode 分厂编码
     * @return 下发结果
     */
    AjaxResult issueLhPrecisionPlan(String factoryCode);

    /**
     * 同步MES数据并生成硫化精度计划（综合接口）
     * 执行步骤：
     * 1. 同步MES设备保养计划到APS
     * 2. 同步MES硫化精度计划实际执行日期回填数据
     * 3. 将回填MES实际执行日期的数据生成新的下一年度的硫化精度计划
     *
     * @param year 年度
     * @return 执行结果
     */
    AjaxResult syncAndGenerateLhPrecisionPlan(Integer year);

    /**
     * 同步MES数据并生成硫化精度计划（按版本号前缀过滤，综合接口）
     * 执行步骤：
     * 1. 同步MES设备保养计划到APS
     * 2. 同步MES硫化精度计划实际执行日期回填数据
     * 3. 按版本前缀过滤生成硫化精度计划
     * 4. 自动推算下一年度计划
     *
     * @param versionPrefix 版本号前缀（如：APS_MES_AH01）
     * @param year 年度
     * @return 执行结果
     */
    AjaxResult syncAndGenerateLhPrecisionPlanByVersionPrefix(String versionPrefix, Integer year);

    public AjaxResult syncAndFillActualDateByOperYear(String versionPrefix, Integer operYear);

    AjaxResult syncAndGenerateLhPrecisionPlanByVersionPrefixAllVersions(String versionPrefix, Integer year);

    /**
     * 清理并重新同步所有MES历史数据（含今天）
     * 执行步骤：
     * 1. 逻辑删除APS库中今天及今天之前的所有数据（8张表）
     * 2. 从MES库重新抓取每天（含今天）最新版本数据
     * 3. 将MES数据插入到APS库
     *
     * @return 执行结果
     */
    AjaxResult cleanAndResyncAllHistory();

    /**
     * 临时任务：按版本迭代同步模具清洗预警数据并生成清洗计划
     * 执行步骤：
     * 1. 清空APS现有的模具清洗预警和清洗计划表全部数据
     * 2. 从MES获取全部模具清洗预警版本号（升序排列）
     * 3. 从最小版本号开始，先插入APS作为初始数据
     * 4. 逐个版本迭代，对后续版本进行更新和新增
     * 5. 迭代到最新版本后，基于全部预警数据（不限制版本号）生成模具清洗计划
     * 6. 删除的预警也同步生成计划（标记为已删除的计划）
     *
     * @param syncDataLogs 同步参数
     * @return 执行结果
     */
    AjaxResult syncAllVersionsMouldCleanWarnAndGenPlan(AuxReqSyncDataLogs syncDataLogs);

    AjaxResult syncDayFinishQtyToChipStock();

    /**
     * 同步设备计划停机（MES→APS）
     * 支持全量/增量同步，含删除标识处理
     * 停机类型=06（临时性故障）时，MES会分步写入开始时间和结束时间，需按ID更新而非插入
     *
     * @param syncDataLogs 同步参数
     * @return 结果
     */
    AjaxResult syncDevPlanClose(AuxReqSyncDataLogs syncDataLogs);
}