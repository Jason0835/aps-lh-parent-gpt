package com.zlt.aps.itf.mes;

import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.aps.cx.api.domain.entity.CxMachineOnlineInfo;
import com.zlt.aps.itf.vo.AuxReqSyncDataLogs;
import com.zlt.aps.itf.vo.MesBrandDict;
import com.zlt.aps.lh.api.domain.entity.LhMachineOnlineInfo;
import com.zlt.aps.mdm.api.domain.entity.MdmMoldAlterPlan;
import com.zlt.aps.mp.api.domain.entity.*;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.text.ParseException;
import java.util.List;

/**
 * MES接口业务
 *
 * @author Chen
 * @since 2025/12/16
 */
@FeignClient(contextId = "IMesItfService", value = ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.itf:/itf}")
public interface IMesItfService {

    /**
     * 同步SKU与模具关系
     *
     * @param mdmSkuMouldRel SKU与模具关系
     * @return 结果
     */
    @ApiOperation("同步SKU与模具关系")
    @PostMapping("/mesItf/syncProductModRelation")
    public AjaxResult syncProductModRelation(@RequestBody MdmSkuMouldRel mdmSkuMouldRel);

    /**
     * 同步模具台账
     *
     * @param modelInfo 模具台账
     * @return 结果
     */
    @ApiOperation("同步模具台账")
    @PostMapping("/mesItf/syncModelInfo")
    public AjaxResult syncModelInfo(@RequestBody MdmModelInfo modelInfo);

    /**
     * 同步成品库存
     *
     * @param productStock 参数
     * @return 结果
     */
    @ApiOperation("同步成品库存")
    @PostMapping("/mesItf/syncProductStock")
    public AjaxResult syncProductStock(@RequestBody MdmProductStock productStock);

    /**
     * 同步不合格库存
     *
     * @param mdmUnqualifiedStock 参数
     * @return 结果
     */
    @ApiOperation("同步不合格库存")
    @PostMapping("/mesItf/syncUnqualifiedStock")
    public AjaxResult syncUnqualifiedStock(@RequestBody MdmUnqualifiedStock mdmUnqualifiedStock);

    /**
     * 同步特殊材料库存
     *
     * @param rawSpecialMaterialStock 参数
     * @return 结果
     */
    @ApiOperation("同步特殊材料库存")
    @PostMapping("/mesItf/syncRawSpecialMaterialStock")
    public AjaxResult syncRawSpecialMaterialStock(@RequestBody RawSpecialMaterialStock rawSpecialMaterialStock);

    /**
     * 同步原材料出库
     *
     * @param syncDataLogs 参数
     * @return 结果
     */
    @ApiOperation("同步原材料出库")
    @PostMapping("/mesItf/syncRawMaterialOutboundRecord")
    public AjaxResult syncRawMaterialOutboundRecord(@RequestBody AuxReqSyncDataLogs syncDataLogs);

    /**
     * 查询实时成品库存
     *
     * @param productStock 参数
     * @return 结果
     */
    @ApiOperation("查询实时成品库存")
    @PostMapping("/mesItf/getProductStock")
    public List<MdmProductStock> getProductStock(@RequestBody MdmProductStock productStock);

    /**
     * 获取不合格库存
     *
     * @param mdmUnqualifiedStock 参数
     * @return 结果
     */
    @ApiOperation("获取不合格库存")
    @PostMapping("/mesItf/getUnqualifiedStock")
    public List<MdmUnqualifiedStock> getUnqualifiedStock(@RequestBody MdmUnqualifiedStock mdmUnqualifiedStock);

    /**
     * 查询特殊材料库存
     *
     * @param rawSpecialMaterialStock 参数
     * @return 结果
     */
    @ApiOperation("同步特殊材料库存")
    @PostMapping("/mesItf/getRawSpecialMaterialStock")
    public List<RawSpecialMaterialStock> getRawSpecialMaterialStock(@RequestBody RawSpecialMaterialStock rawSpecialMaterialStock);

    /**
     * 同步模壳台账信息
     *
     * @param syncDataLogs 参数
     * @return 结果
     */
    @ApiOperation("同步模壳台账信息")
    @PostMapping("/mesItf/syncMoldShell")
    public AjaxResult syncMoldShell(@RequestBody AuxReqSyncDataLogs syncDataLogs);

    /**
     * 下发月计划
     *
     * @param finalResultList 参数
     * @return 结果
     */
    @ApiOperation("下发月计划")
    @PostMapping("/mesItf/issueMonthPlan")
    public AjaxResult issueMonthPlan(@RequestBody List<FactoryMonthPlanProductionFinalResult> finalResultList);

    /**
     * 查询MES品牌字典
     *
     * @return 结果
     */
    @ApiOperation("查询MES品牌字典")
    @PostMapping("/mesItf/selectMesBrandDict")
    public List<MesBrandDict> selectMesBrandDict();

    /**
     * 生成超期SKU
     * @param mdmProductStock 参数
     * @return 结果
     */
    @ApiOperation("生成超期SKU")
    @PostMapping("/mesItf/genOverDueSkuByStock")
    public AjaxResult genOverDueSkuByStock(@RequestBody MdmProductStock mdmProductStock) throws ParseException;

    /**
     * 同步成型在机数据
     * @param cxMachineOnlineInfo 参数
     * @return 结果
     */
    @ApiOperation("同步成型在机数据")
    @PostMapping("/mesItf/syncMachineOnlineInfo")
    public AjaxResult syncMachineOnlineInfo(@RequestBody CxMachineOnlineInfo cxMachineOnlineInfo);

    /**
     * 同步硫化在机数据
     * @param lhMachineOnlineInfo 参数
     * @return 结果
     */
    @ApiOperation("同步硫化在机数据")
    @PostMapping("/mesItf/syncLhMachineOnlineInfo")
    public AjaxResult syncLhMachineOnlineInfo(@RequestBody LhMachineOnlineInfo lhMachineOnlineInfo);

    /**
     * 同步设备保养计划
     * @param syncDataLogs 参数
     * @return 结果
     */
    @ApiOperation("同步设备保养计划")
    @PostMapping("/mesItf/syncDevMaintenancePlan")
    public AjaxResult syncDevMaintenancePlan(@RequestBody AuxReqSyncDataLogs syncDataLogs);

    /**
     * 同步胶囊已使用次数
     * @param syncDataLogs 参数
     * @return 结果
     */
    @ApiOperation("同步胶囊已使用次数")
    @PostMapping("/mesItf/syncLhRepairCapsule")
    public AjaxResult syncLhRepairCapsule(@RequestBody AuxReqSyncDataLogs syncDataLogs);

    /**
     * 同步模具清洗预警计划
     * @param syncDataLogs 参数
     * @return 结果
     */
    @ApiOperation("同步模具清洗预警计划")
    @PostMapping("/mesItf/syncMouldCleanWarn")
    public AjaxResult syncMouldCleanWarn(@RequestBody AuxReqSyncDataLogs syncDataLogs);

    /**
     * 同步结构整车胎面配置
     * @param syncDataLogs 参数
     * @return 结果
     */
    @ApiOperation("同步结构整车胎面配置")
    @PostMapping("/mesItf/syncStructureTreadConfig")
    public AjaxResult syncStructureTreadConfig(@RequestBody AuxReqSyncDataLogs syncDataLogs);

    /**
     * 同步生胎库存
     * @param syncDataLogs 参数
     * @return 结果
     */
    @ApiOperation("同步生胎库存")
    @PostMapping("/mesItf/syncMesCxStock")
    public AjaxResult syncMesCxStock(@RequestBody AuxReqSyncDataLogs syncDataLogs);

    /**
     * 同步胎面库存
     * @param syncDataLogs 参数
     * @return 结果
     */
    @ApiOperation("同步胎面库存")
    @PostMapping("/mesItf/syncTreadStock")
    public AjaxResult syncTreadStock(@RequestBody AuxReqSyncDataLogs syncDataLogs);

    /**
     * 同步成型排程完成量
     * @param syncDataLogs 参数
     * @return 结果
     */
    @ApiOperation("同步成型排程完成量")
    @PostMapping("/mesItf/syncCxClassShiftFinishQty")
    public AjaxResult syncCxClassShiftFinishQty(@RequestBody AuxReqSyncDataLogs syncDataLogs);

    /**
     * 同步硫化排程完成量
     * @param syncDataLogs 参数
     * @return 结果
     */
    @ApiOperation("同步硫化排程完成量")
    @PostMapping("/mesItf/syncLhClassShiftFinishQty")
    public AjaxResult syncLhClassShiftFinishQty(@RequestBody AuxReqSyncDataLogs syncDataLogs);

    /**
     * 同步成型排程日完成量
     * @param syncDataLogs 参数
     * @return 结果
     */
    @ApiOperation("同步成型排程日完成量")
    @PostMapping("/mesItf/syncCxScheDayFinishQty")
    public AjaxResult syncCxScheDayFinishQty(@RequestBody AuxReqSyncDataLogs syncDataLogs);

    /**
     * 同步硫化排程日完成量
     * @param syncDataLogs 参数
     * @return 结果
     */
    @ApiOperation("同步硫化排程日完成量")
    @PostMapping("/mesItf/syncLhScheDayFinishQty")
    public AjaxResult syncLhScheDayFinishQty(@RequestBody AuxReqSyncDataLogs syncDataLogs);

    /**
     * 模具交替计划下发到MES
     * @param moldAlterPlanList 模具交替计划列表
     * @return 结果
     */
    @ApiOperation("模具交替计划下发到MES")
    @PostMapping("/mesItf/issueMoldAlterPlan")
    public AjaxResult issueMoldAlterPlan(@RequestBody List<MdmMoldAlterPlan> moldAlterPlanList);

    /**
     * 同步模具交替计划完成回报
     * @param syncDataLogs 同步参数
     * @return 结果
     */
    @ApiOperation("同步模具交替计划完成回报")
    @PostMapping("/mesItf/syncMoldAlterPlanFinish")
    public AjaxResult syncMoldAlterPlanFinish(@RequestBody AuxReqSyncDataLogs syncDataLogs);

    /**
     * 成型排程结果下发到MES
     * 业务规则：
     * 1. 更新当天的2班（早中班，即二班和三班）- 清空一班数据
     * 2. 更新明天的3班（早中晚班，即一班、二班和三班）
     * 3. 下发后天的3班（早中晚班，即一班、二班和三班）

     * @param cxScheduleResultIssueList 成型排程结果列表
     * @return 结果
     */
    @ApiOperation("成型排程结果下发到MES")
    @PostMapping("/mesItf/issueCxScheduleResult")
    public AjaxResult issueCxScheduleResult(@RequestBody List<com.zlt.aps.mp.api.domain.entity.CxScheduleResultIssue> cxScheduleResultIssueList);

    /**
     * 硫化排程结果下发到MES
     * 业务规则：
     * 1. 更新窗口首日的2班（早中班）- 清空一班数据
     * 2. 更新窗口次日的3班（夜早中班）
     * 3. 下发排程日期当天的3班（夜早中班）
     * 日期从下发数据中推导，不再依赖LocalDate.now()
     *
     * @param lhScheduleResultIssueList 硫化排程结果列表
     * @return 结果
     */
    @ApiOperation("硫化排程结果下发到MES")
    @PostMapping("/mesItf/issueLhScheduleResult")
    public AjaxResult issueLhScheduleResult(@RequestBody List<com.zlt.aps.mp.api.domain.entity.LhScheduleResultIssue> lhScheduleResultIssueList);

    /**
     * 同步出库未扫描订单
     *
     * @param outbountOrdersNotScan 参数
     * @return 结果
     */
    @ApiOperation("同步出库未扫描订单")
    @PostMapping("/mesItf/syncOutbountOrdersNotScan")
    public AjaxResult syncOutbountOrdersNotScan(@RequestBody MdmOutbountOrdersNotScan outbountOrdersNotScan);

    /**
     * 查询出库未扫描订单
     *
     * @param outbountOrdersNotScan 参数
     * @return 结果
     */
    @ApiOperation("查询出库未扫描订单")
    @PostMapping("/mesItf/getOutbountOrdersNotScan")
    public List<MdmOutbountOrdersNotScan> getOutbountOrdersNotScan(@RequestBody MdmOutbountOrdersNotScan outbountOrdersNotScan);

    /**
     * 同步MES硫化精度计划实际执行日期回填数据
     *
     * @param syncDataLogs 参数
     * @return 结果
     */
    @ApiOperation("同步MES硫化精度计划实际执行日期回填数据")
    @PostMapping("/mesItf/syncLhPrecisionPlanActual")
    public AjaxResult syncLhPrecisionPlanActual(@RequestBody AuxReqSyncDataLogs syncDataLogs);

    /**
     * 下发硫化精度计划到MES
     * 查询计划排程精度日期有值且实际执行日期为空的数据，下发到MES中间表
     *
     * @param factoryCode 分厂编码
     * @return 下发结果
     */
    @ApiOperation("下发硫化精度计划到MES")
    @PostMapping("/mesItf/issueLhPrecisionPlan")
    public AjaxResult issueLhPrecisionPlan(@RequestParam("factoryCode") String factoryCode);

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
    @ApiOperation("同步MES数据并生成硫化精度计划（综合接口）")
    @PostMapping("/mesItf/syncAndGenerateLhPrecisionPlan")
    AjaxResult syncAndGenerateLhPrecisionPlan(@RequestParam("year") Integer year);

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
    @ApiOperation("同步MES数据并生成硫化精度计划（按版本号前缀过滤，综合接口）")
    @PostMapping("/mesItf/syncAndGenerateLhPrecisionPlanByVersionPrefix")
    AjaxResult syncAndGenerateLhPrecisionPlanByVersionPrefix(@RequestParam("versionPrefix") String versionPrefix,
                                                              @RequestParam("year") Integer year);

    /**
     * 清理并重新同步所有MES历史数据
     * 执行步骤：
     * 1. 逻辑删除APS库中今天之前的所有数据（6张表）
     * 2. 从MES库重新抓取今天之前每天最新版本数据
     * 3. 将MES数据插入到APS库
     *
     * @return 执行结果
     */
    @ApiOperation("清理并重新同步所有MES历史数据")
    @PostMapping("/mesItf/cleanAndResyncAllHistory")
    AjaxResult cleanAndResyncAllHistory();
}
