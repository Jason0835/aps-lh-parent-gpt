package com.zlt.aps.itf.mes;

import com.zlt.aps.cd15.api.domain.entity.Cd15ScheduleResultIssue;
import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.aps.cx.api.domain.entity.CxMachineOnlineInfo;
import com.zlt.aps.cx.api.domain.entity.CxStock;
import com.zlt.aps.itf.vo.AuxReqSyncDataLogs;
import com.zlt.aps.itf.vo.MesBrandDict;
import com.zlt.aps.lh.api.domain.entity.LhMachineOnlineInfo;
import com.zlt.aps.mdm.api.domain.entity.MdmMoldAlterPlan;
import com.zlt.aps.mp.api.domain.entity.*;
import com.zlt.aps.gsq.api.domain.entity.GsqScheduleResultIssue;
import com.zlt.aps.tc.api.domain.entity.TcScheduleResultIssue;
import com.zlt.aps.tc.api.domain.vo.TcReleaseFeedbackVo;
import com.zlt.aps.tq.api.domain.entity.TqScheduleResultIssue;
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
     * 按指定版本号同步硫化在机数据（临时任务）
     * 与原syncLhMachineOnlineInfo的区别：不限日期，按指定版本号查询MES中间表所有日期数据
     * 同步逻辑与硫化排程完成量回报按版本号同步一致：按onlineDate分组后逐组调用逻辑删除+插入
     *
     * @param dataVersion 指定版本号
     * @return 结果
     */
    @ApiOperation("按指定版本号同步硫化在机数据（临时任务）")
    @PostMapping("/mesItf/syncLhMachineOnlineInfoByVersion")
    public AjaxResult syncLhMachineOnlineInfoByVersion(@RequestParam("dataVersion") String dataVersion);

    /**
     * 同步设备保养计划
     * @param syncDataLogs 参数
     * @return 结果
     */
    @ApiOperation("同步设备保养计划")
    @PostMapping("/mesItf/syncDevMaintenancePlan")
    public AjaxResult syncDevMaintenancePlan(@RequestBody AuxReqSyncDataLogs syncDataLogs);

    /**
     * 仅同步设备保养计划数据，不触发自动生成精度计划逻辑
     * 用于临时任务场景：先同步数据，再按自定义逻辑生成精度计划
     *
     * @param syncDataLogs 同步参数（可指定精度类型）
     * @return 同步结果
     */
    @ApiOperation("仅同步设备保养计划数据（不触发生成精度计划）")
    @PostMapping("/mesItf/syncDevMaintenancePlanOnly")
    public AjaxResult syncDevMaintenancePlanOnly(@RequestBody AuxReqSyncDataLogs syncDataLogs);

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
     * 同步直裁库存（从 MES 中间表 T_MES_CD90_STOCK 同步到 t_cd90_stock）
     * @param syncDataLogs 参数（可传 factoryCode；queryParams.shiftCode 可覆盖自动推断班次）
     * @return 结果
     */
    @ApiOperation("同步直裁库存")
    @PostMapping("/mesItf/syncMesCd90Stock")
    public AjaxResult syncMesCd90Stock(@RequestBody AuxReqSyncDataLogs syncDataLogs);

    /**
     * 实时查询MES生胎库存（不写入APS本地表，仅供成型排程实时调用）
     * @param syncDataLogs 参数（可传factoryCode过滤分厂）
     * @return 生胎库存列表
     */
    @ApiOperation("实时查询MES生胎库存")
    @PostMapping("/mesItf/getCxStock")
    public List<CxStock> getCxStock(@RequestBody AuxReqSyncDataLogs syncDataLogs);

    /**
     * 同步胎面库存
     * @param syncDataLogs 参数
     * @return 结果
     */
    @ApiOperation("同步胎面库存")
    @PostMapping("/mesItf/syncTreadStock")
    public AjaxResult syncTreadStock(@RequestBody AuxReqSyncDataLogs syncDataLogs);

    /**
     * 同步胎圈库存
     * @param syncDataLogs 参数
     * @return 结果
     */
    @ApiOperation("同步胎圈库存")
    @PostMapping("/mesItf/syncMesTqStock")
    public AjaxResult syncMesTqStock(@RequestBody AuxReqSyncDataLogs syncDataLogs);

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
     * 胎圈排程结果下发到MES
     * 业务规则：
     * 1. D日（今天）：更新中班数据（胎圈1班→MES中班），夜班早班已过不下发
     * 2. D+1日（明天）：更新夜早中3班数据（胎圈2/3/4班→MES夜/早/中班）
     * 3. D+2日（后天）：先删后插夜早2班数据（胎圈5/6班→MES夜/早班），中班尚未排产不下发
     * 胎圈6班覆盖成型3~8班，CX_CLASS3~8_PLAN全量传递
     *
     * @param tqScheduleResultIssueList 胎圈排程结果列表（已按3天拆分）
     * @return 结果
     */
    @ApiOperation("胎圈排程结果下发到MES")
    @PostMapping("/mesItf/issueTqScheduleResult")
    public AjaxResult issueTqScheduleResult(@RequestBody List<TqScheduleResultIssue> tqScheduleResultIssueList);

    /**
     * 钢丝圈排程结果下发到MES
     * 业务规则：
     * 1. D日（今天）：更新中班数据（钢丝圈1班→MES中班），夜班早班已过不下发
     * 2. D+1日（明天）：更新夜早中3班数据（钢丝圈2/3/4班→MES夜/早/中班）
     * 3. D+2日（后天）：先删后插夜早2班数据（钢丝圈5/6班→MES夜/早班），中班尚未排产不下发
     * 钢丝圈6班覆盖胎圈1~6班，TQ_CLASS1~6_PLAN全量传递
     *
     * @param gsqScheduleResultIssueList 钢丝圈排程结果列表（已按3天拆分）
     * @return 结果
     */
    @ApiOperation("钢丝圈排程结果下发到MES")
    @PostMapping("/mesItf/issueGsqScheduleResult")
    public AjaxResult issueGsqScheduleResult(@RequestBody List<GsqScheduleResultIssue> gsqScheduleResultIssueList);

    /**
     * 直裁排程结果下发到MES
     * 业务规则：
     * 1. 班次配置由 t_cd90_shift_config 启用项决定，默认 CLASS1~CLASS6
     * 2. 每条 issue 携带 classField / scheduleDay / dayShiftOrder / shiftName，由 MES 侧按班次槽位映射
     * 3. APS 侧按 SCHEDULE_DAY 推导排班日期，day1=T-1，day2=T，day3=T+1
     *
     * @param cd90ScheduleResultIssueList 直裁排程结果列表（已按班次展开）
     * @return 结果
     */
    @ApiOperation("直裁排程结果下发到MES")
    @PostMapping("/mesItf/issueCd90ScheduleResult")
    public AjaxResult issueCd90ScheduleResult(@RequestBody List<Cd90ScheduleResultIssue> cd90ScheduleResultIssueList);
    /**
     * 斜裁排程结果下发到 MES。
     *
     * @param issueList 按班次展开的斜裁排程结果
     * @return 下发结果
     */
    @ApiOperation("斜裁排程结果下发到MES")
    @PostMapping("/mesItf/issueCd15ScheduleResult")
    AjaxResult issueCd15ScheduleResult(
            @RequestBody List<Cd15ScheduleResultIssue> issueList);


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
     * 同步MES数据并生成硫化精度计划（按版本号前缀过滤，不限最大版本号，综合接口）
     * 与syncAndGenerateLhPrecisionPlanByVersionPrefix的区别：不限制最大版本号，版本前缀匹配的所有版本数据都参与生成
     *
     * @param versionPrefix 版本号前缀（如：APS_MES_AH01）
     * @param year 年度
     * @return 执行结果
     */
    @ApiOperation("同步MES数据并生成硫化精度计划（按版本号前缀过滤，不限最大版本号，综合接口）")
    @PostMapping("/mesItf/syncAndGenerateLhPrecisionPlanByVersionPrefixAllVersions")
    AjaxResult syncAndGenerateLhPrecisionPlanByVersionPrefixAllVersions(@RequestParam("versionPrefix") String versionPrefix,
                                                                         @RequestParam("year") Integer year);

    /**
     * 临时任务：同步MES设备保养计划并按计划时间年份回填实际执行日期+生成下一年度精度计划
     * 只取指定年份有实际执行时间的数据来回填对应年份的精度计划，并推算生成下一年度计划
     *
     * @param versionPrefix 版本号前缀（如：APS_MES_AH01）
     * @param operYear 计划时间所在年份（如：2026，只取operTime在该年份的数据）
     * @return 执行结果
     */
    @ApiOperation("临时任务-按计划时间年份同步回填实际日期并生成下一年度精度计划")
    @PostMapping("/mesItf/syncAndFillActualDateByOperYear")
    AjaxResult syncAndFillActualDateByOperYear(@RequestParam("versionPrefix") String versionPrefix,
                                                @RequestParam("operYear") Integer operYear);

    /**
     * 清理并重新同步所有MES历史数据
     * 执行步骤：
     * 1. 逻辑删除APS库中今天之前的所有数据（6张表）
     * 2. 从MES库重新抓取今天之前每天最新版本数据
     * 3. 将MES数据插入到APS库
     *
     * @return 执行结果
     */
    @ApiOperation("清理并重新同步所有MES历史数据（含今天）")
    @PostMapping("/mesItf/cleanAndResyncAllHistory")
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
    @ApiOperation("临时任务-按版本迭代同步模具清洗预警并生成清洗计划")
    @PostMapping("/mesItf/syncAllVersionsMouldCleanWarnAndGenPlan")
    AjaxResult syncAllVersionsMouldCleanWarnAndGenPlan(@RequestBody AuxReqSyncDataLogs syncDataLogs);

    @ApiOperation("硫化日完成量回填芯片库存")
    @PostMapping("/mesItf/syncDayFinishQtyToChipStock")
    AjaxResult syncDayFinishQtyToChipStock();

    @ApiOperation("按指定版本号同步硫化排程完成量（临时任务）")
    @PostMapping("/mesItf/syncLhClassShiftFinishQtyByVersion")
    public AjaxResult syncLhClassShiftFinishQtyByVersion(@RequestParam("dataVersion") String dataVersion);

    /**
     * 按最新版本号同步硫化排程日完成量（临时任务，不限日期）
     * @return 结果
     */
    @ApiOperation("按最新版本号同步硫化排程日完成量（临时任务，不限日期）")
    @PostMapping("/mesItf/syncLhScheDayFinishQtyByLatestVersion")
    public AjaxResult syncLhScheDayFinishQtyByLatestVersion(@RequestParam("dataVersion") String dataVersion);

    @ApiOperation("按上一天最新版本号同步硫化排程完成量（临时任务）")
    @PostMapping("/mesItf/syncLhClassShiftFinishQtyByYesterday")
    public AjaxResult syncLhClassShiftFinishQtyByYesterday(@RequestBody AuxReqSyncDataLogs syncDataLogs);

    /**
     * 同步设备停机计划
     * @param syncDataLogs 参数
     * @return 结果
     */
    @ApiOperation("同步设备停机计划")
    @PostMapping("/mesItf/syncDevPlanClose")
    public AjaxResult syncDevPlanClose(@RequestBody AuxReqSyncDataLogs syncDataLogs);

    /**
     * 同步胎侧库存。
     *
     * @param syncDataLogs 同步参数
     * @return 同步结果
     */
    @ApiOperation("同步胎侧库存")
    @PostMapping("/mesItf/syncSidewallStock")
    AjaxResult syncSidewallStock(@RequestBody AuxReqSyncDataLogs syncDataLogs);

    /**
     * 同步胎侧班次完成量并回写排程结果。
     *
     * @param syncDataLogs 同步参数
     * @return 同步结果
     */
    @ApiOperation("同步胎侧班次完成量")
    @PostMapping("/mesItf/syncTcClassShiftFinishQty")
    AjaxResult syncTcClassShiftFinishQty(@RequestBody AuxReqSyncDataLogs syncDataLogs);

    /**
     * 同步胎侧日完成量。
     *
     * @param syncDataLogs 同步参数
     * @return 同步结果
     */
    @ApiOperation("同步胎侧日完成量")
    @PostMapping("/mesItf/syncTcScheDayFinishQty")
    AjaxResult syncTcScheDayFinishQty(@RequestBody AuxReqSyncDataLogs syncDataLogs);

    /**
     * 下发胎侧排程结果。
     *
     * @param issueList 已按MES业务日期拆分的结果
     * @return 下发结果
     */
    @ApiOperation("胎侧排程结果下发到MES")
    @PostMapping("/mesItf/issueTcScheduleResult")
    AjaxResult issueTcScheduleResult(@RequestBody List<TcScheduleResultIssue> issueList);

    /**
     * 查询胎侧排程发布处理状态。
     *
     * @param dataVersion 发布数据版本
     * @return MES反馈
     */
    @ApiOperation("查询胎侧排程发布处理状态")
    @PostMapping("/mesItf/queryTcScheduleIssueStatus")
    TcReleaseFeedbackVo queryTcScheduleIssueStatus(@RequestParam("dataVersion") String dataVersion);
}
