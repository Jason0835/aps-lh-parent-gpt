package com.zlt.aps.itf.mes.service;

import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.aps.cx.api.domain.entity.CxMachineOnlineInfo;
import com.zlt.aps.cx.api.domain.entity.CxStock;
import com.zlt.aps.itf.vo.AuxReqSyncDataLogs;
import com.zlt.aps.itf.vo.MesBrandDict;
import com.zlt.aps.itf.vo.MesShiftStockSyncRequest;
import com.zlt.aps.lh.api.domain.entity.LhMachineOnlineInfo;
import com.zlt.aps.mdm.api.domain.entity.MdmMoldAlterPlan;
import com.zlt.aps.mp.api.domain.entity.*;
import com.zlt.aps.tm.api.domain.entity.TmScheduleResultIssue;

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
     * 按指定版本号同步硫化在机数据（临时任务）
     * 与原syncLhMachineOnlineInfo的区别：不限日期，按指定版本号查询MES中间表所有日期数据
     * 同步后按onlineDate分组逐组调用逻辑删除+插入，无需回填排程结果
     *
     * @param dataVersion 指定版本号
     * @return 结果
     */
    AjaxResult syncLhMachineOnlineInfoByVersion(String dataVersion);

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
     * 同步设备保养计划并按精度类型分发写入对应的精度计划表（现逻辑）
     * 现逻辑：MES全权决定计划时间(OPER_TIME)和实际完成时间(FIRST_WASH_TIME)，
     * APS侧不再回填实际日期、不再生成下一次精度计划。
     * 执行步骤：
     * 1. 同步MES设备保养计划数据到T_MDM_DEV_MAINTENANCE_PLAN
     * 2. 按PRECISION_TYPE分发：
     *    - "硫化精度" → 调用lh模块dispatchFromMaintenancePlan写入T_LH_PRECISION_PLAN
     *    - "成型精度15天"/"成型精度60天" → 调用cx模块dispatchFromMaintenancePlan写入T_CX_PRECISION_PLAN
     * 3. 分发时根据MES字段值直接计算派生字段
     *
     * @param syncDataLogs 同步参数（可指定精度类型）
     * @return 同步+分发结果
     */
    AjaxResult syncAndDispatchDevMaintenancePlan(AuxReqSyncDataLogs syncDataLogs);

    /**
     * 按指定版本号同步设备保养计划并分发写入精度计划表（临时任务）
     * 与原syncAndDispatchDevMaintenancePlan的区别：
     * 1. 不查最大版本号，直接使用传入的dataVersion查询MES中间表
     * 2. 不限精度类型，同步指定版本下的全部精度类型数据
     * 3. 分发时按dataVersion精确查询APS本地表，仅分发本次同步的数据（避免历史数据重复分发）
     *
     * @param dataVersion 指定版本号
     * @return 同步+分发结果
     */
    AjaxResult syncAndDispatchDevMaintenancePlanByVersion(String dataVersion);

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
     * 实时查询MES生胎库存（不写入APS本地表，仅供成型排程实时调用）
     *
     * @param syncDataLogs 查询参数（可传factoryCode过滤分厂）
     * @return 生胎库存列表
     */
    List<CxStock> getCxStock(AuxReqSyncDataLogs syncDataLogs);

    /**
     * 同步胎面库存
     * 采用更新删除标识模式，而不是先删后插
     *
     * @param syncDataLogs 同步参数
     * @return 结果
     */
    AjaxResult syncTmStock(AuxReqSyncDataLogs syncDataLogs);

    /**
     * 同步胎圈库存
     * T_TQ_STOCK：采用逻辑删除+插入方案
     *   步骤1：逻辑删除当天库存日期的所有数据（IS_DELETE置为1）
     *   步骤2：将MES最新库存数据批量插入（新记录，IS_DELETE=0）
     *   历史数据保留，只删当天库存日期的数据
     *
     * @param syncDataLogs 同步参数
     * @return 结果
     */
    AjaxResult syncMesTqStock(AuxReqSyncDataLogs syncDataLogs);

    /**
     * 同步钢丝圈库存
     * T_GSQ_STOCK：采用逻辑删除+插入方案
     *   步骤1：逻辑删除当天库存日期的所有数据（IS_DELETE置为1）
     *   步骤2：将MES最新库存数据批量插入（新记录，IS_DELETE=0）
     *   历史数据保留，只删当天库存日期的数据
     *
     * @param syncDataLogs 同步参数
     * @return 结果
     */
    AjaxResult syncMesGsqStock(AuxReqSyncDataLogs syncDataLogs);

    /**
     * 同步MES钢丝圈缠绕盘三表数据（缠绕盘清单/规格关系/机台关系）
     * 三表均按业务键取MES DATA_VERSION最大版本行，一次Feign调用在钢丝圈微服务侧单事务落库：
     *   步骤1：查询MES_WIRE_DISC_INFO缠绕盘清单（按MOUTH_PLAT_CODE取最大版本）
     *   步骤2：查询MES_WIRE_DISC_SPEC_MAPPING规格关系（按WIRE_DISC_CODE+STEEL_RING_CODE取最大版本）
     *   步骤3：查询MES_WIRE_DISC_MACHINE_MAPPING机台关系（按WIRE_DISC_CODE+MACHINE_CODE取最大版本）
     *   步骤4：聚合后远程调用gsqMesSyncRemoteService.syncTwiningDisc落库
     *          （主表UPSERT保留手工字段、MES失效数据清理级联、子表整体替换、机台关系UPSERT）
     *
     * @param syncDataLogs 同步参数（可传factoryCode过滤分厂）
     * @return 结果
     */
    AjaxResult syncMesTwiningDisc(AuxReqSyncDataLogs syncDataLogs);

    /**
     * 同步胎圈排程完成量
     * T_TQ_SCHE_FINISH_QTY：采用逻辑删除+插入方案
     *   步骤1：逻辑删除当天排程日期的所有数据（IS_DELETE置为1）
     *   步骤2：将MES最新排程完成量数据批量插入（新记录，IS_DELETE=0）
     *   步骤3：回写胎圈排程结果表各班次完成量
     *
     * @param syncDataLogs 同步参数
     * @return 结果
     */
    AjaxResult syncTqClassShiftFinishQty(AuxReqSyncDataLogs syncDataLogs);

    /**
     * 同步胎圈排程日完成量
     * T_TQ_DAY_FINISH_QTY：采用逻辑删除+插入方案
     *   步骤1：逻辑删除当天排程日期的所有数据（IS_DELETE置为1）
     *   步骤2：将MES最新排程日完成量数据批量插入（新记录，IS_DELETE=0）
     *
     * @param syncDataLogs 同步参数
     * @return 结果
     */
    AjaxResult syncTqScheDayFinishQty(AuxReqSyncDataLogs syncDataLogs);

    /**
     * 同步钢丝圈排程日完成量
     * T_GSQ_DAY_FINISH_QTY：采用逻辑删除+插入方案
     *   步骤1：逻辑删除当天排程日期的所有数据（IS_DELETE置为1）
     *   步骤2：将MES最新排程日完成量数据批量插入（新记录，IS_DELETE=0）
     *
     * @param syncDataLogs 同步参数
     * @return 结果
     */
    AjaxResult syncGsqScheDayFinishQty(AuxReqSyncDataLogs syncDataLogs);

    /**
     * 同步钢丝圈排程完成量
     * T_GSQ_SCHE_FINISH_QTY：采用逻辑删除+插入方案
     *   步骤1：逻辑删除当天排程日期的所有数据（IS_DELETE置为1）
     *   步骤2：将MES最新排程完成量数据批量插入（新记录，IS_DELETE=0）
     *   步骤3：回写钢丝圈排程结果表各班次完成量
     *
     * @param syncDataLogs 同步参数
     * @return 结果
     */
    AjaxResult syncGsqClassShiftFinishQty(AuxReqSyncDataLogs syncDataLogs);

    /**
     * 按上一天最新版本号同步钢丝圈排程完成量（临时任务）
     * 逻辑同 syncGsqClassShiftFinishQty（抓当天最新版本），但日期条件改为上一天
     *
     * @param syncDataLogs 同步参数
     * @return 结果
     */
    AjaxResult syncGsqClassShiftFinishQtyByYesterday(AuxReqSyncDataLogs syncDataLogs);

    /**
     * 按指定版本号同步钢丝圈排程完成量（临时任务）
     * 与原 syncGsqClassShiftFinishQty 的区别：不限日期，按指定版本号查询MES中间表所有日期数据
     * 同步后同样回写排程结果
     * 由于指定版本可能包含多个排程日期的数据，按排程日期分组后逐组调用逻辑删除+插入
     *
     * @param dataVersion 指定版本号
     * @return 结果
     */
    AjaxResult syncGsqClassShiftFinishQtyByVersion(String dataVersion);

    /**
     * 同步胎面排程完成量
     * T_TM_SCHE_FINISH_QTY：采用逻辑删除+插入方案
     *   步骤1：逻辑删除当天排程日期的所有数据（IS_DELETE置为1）
     *   步骤2：将MES最新排程完成量数据批量插入（新记录，IS_DELETE=0）
     *   步骤3：回写胎面排程结果表各班次完成量
     *
     * @param syncDataLogs 同步参数
     * @return 结果
     */
    AjaxResult syncTmClassShiftFinishQty(AuxReqSyncDataLogs syncDataLogs);

    /**
     * 同步胎面排程日完成量
     * T_TM_DAY_FINISH_QTY（复用表名，1-total 结构）：采用逻辑删除+插入方案
     *   步骤1：逻辑删除当天排程日期的所有数据（IS_DELETE置为1）
     *   步骤2：将MES最新排程日完成量数据批量插入（新记录，IS_DELETE=0）
     *   注意：该表 mps 以 2 班结构复用，逻辑删除会覆盖 mps 数据（已知冲突风险）
     *
     * @param syncDataLogs 同步参数
     * @return 结果
     */
    AjaxResult syncTmScheDayFinishQty(AuxReqSyncDataLogs syncDataLogs);

    /**
     * 胎面排程结果下发到MES
     * 业务规则（与胎圈一致）：
     * 1. D日（今天）：更新中班数据，夜班早班已过不下发
     * 2. D+1日（明天）：更新夜早中3班数据
     * 3. D+2日（后天）：先删后插夜早2班数据，中班尚未排产不下发
     *
     * @param tmScheduleResultIssueList 胎面排程结果下发列表（已按3天拆分）
     * @return 结果
     */
    AjaxResult issueTmScheduleResult(List<TmScheduleResultIssue> tmScheduleResultIssueList);

    /**
     * 钢丝圈排程结果下发到MES
     * 业务规则（与胎圈一致）：
     * 1. D日（今天）：更新中班数据（钢丝圈1班→MES中班），夜班早班已过不下发
     * 2. D+1日（明天）：更新夜早中3班数据（钢丝圈2/3/4班→MES夜/早/中班）
     * 3. D+2日（后天）：先删后插夜早2班数据（钢丝圈5/6班→MES夜/早班），中班尚未排产不下发
     * TQ_CLASS1~6_PLAN 全量传递到每条记录
     *
     * @param gsqScheduleResultIssueList 钢丝圈排程结果下发列表（已按3天拆分）
     * @return 下发结果（data 字段携带 mesStatus：IS_RELEASE/FAILURE_RELEASE/TIMEOUT_FAILURE）
     */
//    AjaxResult issueGsqScheduleResult(List<GsqScheduleResultIssue> gsqScheduleResultIssueList);

    /**
     * 同步胎面自动滚动班次库存。
     *
     * @param request 工厂、物理库存日和班序
     * @return 同步结果
     */
    AjaxResult syncTmShiftStock(MesShiftStockSyncRequest request);

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

    public AjaxResult syncBeadShiftStock(MesShiftStockSyncRequest request);
}
