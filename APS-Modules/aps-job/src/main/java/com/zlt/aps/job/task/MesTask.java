package com.zlt.aps.job.task;

import cn.hutool.core.date.DateUtil;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.aps.autoLogin.feign.FeignTokenHelper;
import com.zlt.aps.cx.api.domain.entity.CxMachineOnlineInfo;
import com.zlt.aps.itf.mes.IMesItfService;
import com.zlt.aps.itf.vo.AuxReqSyncDataLogs;
import com.zlt.aps.lh.api.domain.entity.LhMachineOnlineInfo;
import com.zlt.aps.lh.api.service.ILhPrecisionPlanRemoteService;
import com.zlt.aps.mp.api.domain.entity.MdmOutbountOrdersNotScan;
import com.zlt.aps.mp.api.domain.entity.MdmProductStock;
import com.zlt.aps.mp.api.domain.entity.MdmUnqualifiedStock;
import com.zlt.aps.mp.api.domain.entity.RawSpecialMaterialStock;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Date;
import java.util.HashMap;

/**
 * MES接口定时任务
 *
 * @author Chen
 * @since 2025/12/22
 */
@Slf4j
@Component("mesTask")
public class MesTask {

    @Autowired
    private IMesItfService iMesItfService;

    @Autowired
    private ILhPrecisionPlanRemoteService lhPrecisionPlanRemoteService;

    /**
     * 同步成品库存
     */
    @ApiOperation("同步成品库存-默认当前年月")
    public void syncProductStock() {
        FeignTokenHelper.runWithToken(() -> iMesItfService.syncProductStock(new MdmProductStock()));
    }

    /**
     * 同步出库未扫描订单
     */
    @ApiOperation("同步出库未扫描订单-默认当前年月")
    public void syncOutboundOrdersNotScan() {
        FeignTokenHelper.runWithToken(() -> iMesItfService.syncOutbountOrdersNotScan(new MdmOutbountOrdersNotScan()));
    }

    /**
     * 同步不合格库存
     */
    @ApiOperation("同步不合格库存-默认当前日期")
    public void syncUnqualifiedStock() {
        FeignTokenHelper.runWithToken(() -> iMesItfService.syncUnqualifiedStock(new MdmUnqualifiedStock()));
    }

    /**
     * 同步特殊材料库存
     */
    @ApiOperation("同步特殊材料库存-默认当前日期")
    public void syncRawSpecialMaterialStock() {
        FeignTokenHelper.runWithToken(() -> iMesItfService.syncRawSpecialMaterialStock(new RawSpecialMaterialStock()));
    }

    /**
     * 同步成型在机数据
     */
    @ApiOperation("同步成型在机数据")
    public void syncMachineOnlineInfo() {
        FeignTokenHelper.runWithToken(() -> iMesItfService.syncMachineOnlineInfo(new CxMachineOnlineInfo()));
    }

    /**
     * 同步硫化在机数据
     */
    @ApiOperation("同步硫化在机数据")
    public void syncLhMachineOnlineInfo() {
        FeignTokenHelper.runWithToken(() -> iMesItfService.syncLhMachineOnlineInfo(new LhMachineOnlineInfo()));
    }

    /**
     * 临时任务：按指定版本号APS_MES_AH01_20260717060600011抓取硫化在机数据
     * 逻辑参考硫化排程完成量回报按版本号同步（syncLhClassShiftFinishQtyByVersion），调同步硫化在机接口
     * 执行步骤：
     * 1. 从MES中间表按指定版本号查询硫化在机数据（不限日期）
     * 2. 按onlineDate分组，逐组逻辑删除APS旧数据并插入新数据
     */
    @ApiOperation("临时任务-按版本号APS_MES_AH01_20260717060600011抓取硫化在机数据")
    public void syncLhMachineOnlineInfoByVersion() {
        String dataVersion = "APS_MES_AH01_20260717060600011";
        log.info("临时任务-开始按版本号{}抓取硫化在机数据", dataVersion);
        try {
            FeignTokenHelper.runWithToken(() -> {
                AjaxResult result = iMesItfService.syncLhMachineOnlineInfoByVersion(dataVersion);
                log.info("临时任务-按版本号{}抓取硫化在机数据结果：{}", dataVersion, result);
            });
        } catch (Exception e) {
            log.error("临时任务-按版本号{}抓取硫化在机数据异常", dataVersion, e);
        }
        log.info("临时任务-按版本号{}抓取硫化在机数据完成", dataVersion);
    }

    /**
     * 同步设备保养计划并按精度类型分发写入对应的精度计划表（现逻辑）
     * 现逻辑：MES全权决定计划时间和实际完成时间，APS只做同步+分发，不再回填/生成下一次精度计划。
     * 原 syncDevMaintenancePlan 方法（含回填+生成）已 @Deprecated 备份保留。
     */
    @ApiOperation("同步设备保养计划并分发写入精度计划表")
    public void syncDevMaintenancePlan() {
        FeignTokenHelper.runWithToken(() -> iMesItfService.syncAndDispatchDevMaintenancePlan(new AuxReqSyncDataLogs()));
    }

    /**
     * 同步模具清洗预警计划
     */
    @ApiOperation("同步模具清洗预警计划")
    public void syncMouldCleanWarn() {
        FeignTokenHelper.runWithToken(() -> iMesItfService.syncMouldCleanWarn(new AuxReqSyncDataLogs()));
    }

    /**
     * 同步胶囊已使用次数
     */
    @ApiOperation("同步胶囊已使用次数")
    public void syncLhRepairCapsule() {
        FeignTokenHelper.runWithToken(() -> iMesItfService.syncLhRepairCapsule(new AuxReqSyncDataLogs()));
    }

    /**
     * 同步生胎库存
     */
    @ApiOperation("同步生胎库存")
    public void syncMesCxStock() {
        FeignTokenHelper.runWithToken(() -> {
            AuxReqSyncDataLogs syncDataLogs = new AuxReqSyncDataLogs();
            HashMap<String, Object> queryParams = new HashMap<>();
            queryParams.put("stockDate", DateUtil.format(new Date(), "yyyy-MM-dd"));
            syncDataLogs.setQueryParams(queryParams);
            iMesItfService.syncMesCxStock(syncDataLogs);
        });
    }

    /**
     * 同步胎圈库存
     */
    @ApiOperation("同步胎圈库存")
    public void syncMesTqStock() {
        FeignTokenHelper.runWithToken(() -> iMesItfService.syncMesTqStock(new AuxReqSyncDataLogs()));
    }

    /**
     * 同步钢丝圈库存
     */
    @ApiOperation("同步钢丝圈库存")
    public void syncMesGsqStock() {
        FeignTokenHelper.runWithToken(() -> iMesItfService.syncMesGsqStock(new AuxReqSyncDataLogs()));
    }

    /**
     * 同步结构整车胎面配置
     */
    @ApiOperation("同步结构整车胎面配置")
    public void syncStructureTreadConfig() {
        FeignTokenHelper.runWithToken(() -> iMesItfService.syncStructureTreadConfig(new AuxReqSyncDataLogs()));
    }

    /**
     * 同步成型排程完成量
     */
    @ApiOperation("同步成型排程完成量")
    public void syncCxClassShiftFinishQty() {
        FeignTokenHelper.runWithToken(() -> iMesItfService.syncCxClassShiftFinishQty(new AuxReqSyncDataLogs()));
    }

    /**
     * 同步硫化排程完成量
     */
    @ApiOperation("同步硫化排程完成量")
    public void syncLhClassShiftFinishQty() {
        FeignTokenHelper.runWithToken(() -> iMesItfService.syncLhClassShiftFinishQty(new AuxReqSyncDataLogs()));
    }

    /**
     * 同步成型排程日完成量
     */
    @ApiOperation("同步成型排程日完成量")
    public void syncCxScheDayFinishQty() {
        FeignTokenHelper.runWithToken(() -> iMesItfService.syncCxScheDayFinishQty(new AuxReqSyncDataLogs()));
    }

    /**
     * 同步硫化排程日完成量
     */
    @ApiOperation("同步硫化排程日完成量")
    public void syncLhScheDayFinishQty() {
        FeignTokenHelper.runWithToken(() -> iMesItfService.syncLhScheDayFinishQty(new AuxReqSyncDataLogs()));
    }

    /**
     * 同步胎圈排程日完成量
     */
    @ApiOperation("同步胎圈排程日完成量")
    public void syncTqScheDayFinishQty() {
        FeignTokenHelper.runWithToken(() -> iMesItfService.syncTqScheDayFinishQty(new AuxReqSyncDataLogs()));
    }

    /**
     * 同步钢丝圈排程日完成量
     */
    @ApiOperation("同步钢丝圈排程日完成量")
    public void syncGsqScheDayFinishQty() {
        FeignTokenHelper.runWithToken(() -> iMesItfService.syncGsqScheDayFinishQty(new AuxReqSyncDataLogs()));
    }

    /**
     * 同步模具交替计划完成回报
     */
    @ApiOperation("同步模具交替计划完成回报")
    public void syncMoldAlterPlanFinish() {
        FeignTokenHelper.runWithToken(() -> iMesItfService.syncMoldAlterPlanFinish(new AuxReqSyncDataLogs()));
    }

    /**
     * 同步MES硫化精度计划实际执行日期回填数据
     * 已停用：现逻辑APS不再回填实际执行日期、不再生成下一次精度计划。
     * 现逻辑改由 syncDevMaintenancePlan 调 syncAndDispatchDevMaintenancePlan 完成同步+分发。
     * 方法签名保留以便调度配置不动，如需恢复原逻辑可调用 iMesItfService.syncLhPrecisionPlanActual。
     */
    @ApiOperation("同步MES硫化精度计划实际执行日期回填数据（已停用）")
    public void syncLhPrecisionPlanActual() {
        log.info("定时任务syncLhPrecisionPlanActual已停用：现逻辑APS不再回填实际执行日期、不再生成下一次精度计划");
    }

    /**
     * 临时任务：清理并重新同步所有MES历史数据（含今天）
     * 执行步骤：
     * 1. 逻辑删除APS库中今天及今天之前的所有数据（8张表）
     * 2. 从MES库重新抓取每天（含今天）最新版本数据
     * 3. 将MES数据插入到APS库
     * 涉及表：成型在机、硫化在机、胶囊已使用次数、生胎库存、成型排程完成量、成型排程日完成量、硫化排程完成量、硫化排程日完成量
     */
    @ApiOperation("临时任务-清理并重新同步所有MES历史数据（含今天）")
    public void cleanAllHistoryDuplicate() {
        log.info("临时任务-开始清理并重新同步所有MES历史数据（含今天）");
        try {
            FeignTokenHelper.runWithToken(() -> {
                com.ruoyi.common.core.web.domain.AjaxResult result = iMesItfService.cleanAndResyncAllHistory();
                log.info("临时任务-清理并重新同步所有MES历史数据（含今天）结果：{}", result);
            });
        } catch (Exception e) {
            log.error("临时任务-清理并重新同步所有MES历史数据异常", e);
        }
        log.info("临时任务-清理并重新同步所有MES历史数据（含今天）完成");
    }

    /**
     * 临时任务：按版本前缀APS_MES_AH01抓取MES硫化精度数据并生成硫化精度计划
     * 已停用：现逻辑APS不再回填实际执行日期、不再生成下一次精度计划。
     * 现逻辑改由 syncDevMaintenancePlan 调 syncAndDispatchDevMaintenancePlan 完成同步+分发。
     * 方法签名保留以便调度配置不动，如需恢复原逻辑可调用 iMesItfService.syncAndGenerateLhPrecisionPlanByVersionPrefix。
     */
    @ApiOperation("临时任务-按版本前缀APS_MES_AH01抓取MES硫化精度数据并生成计划（已停用）")
    public void syncAndGenerateLhPrecisionPlanByVersionPrefix() {
        log.info("临时任务syncAndGenerateLhPrecisionPlanByVersionPrefix已停用：现逻辑APS不再回填/生成精度计划");
    }

    /**
     * 临时任务：按版本迭代同步模具清洗预警数据并生成清洗计划
     * 执行步骤：
     * 1. 清空APS现有的模具清洗预警和清洗计划表全部数据
     * 2. 从MES获取全部模具清洗预警版本号（升序排列）
     * 3. 从最小版本号开始，先插入APS作为初始数据
     * 4. 逐个版本迭代，对后续版本进行更新和新增
     * 5. 迭代到最新版本后，基于全部预警数据（不限制版本号）生成模具清洗计划
     * 6. 删除的预警也同步生成计划（标记为已删除的计划）
     */
    @ApiOperation("临时任务-按版本迭代同步模具清洗预警并生成清洗计划")
    public void syncAllVersionsMouldCleanWarnAndGenPlan() {
        log.info("临时任务-开始按版本迭代同步模具清洗预警并生成清洗计划");
        try {
            FeignTokenHelper.runWithToken(() -> {
                AjaxResult result = iMesItfService.syncAllVersionsMouldCleanWarnAndGenPlan(new AuxReqSyncDataLogs());
                log.info("临时任务-按版本迭代同步模具清洗预警并生成清洗计划结果：{}", result);
            });
        } catch (Exception e) {
            log.error("临时任务-按版本迭代同步模具清洗预警并生成清洗计划异常", e);
        }
        log.info("临时任务-按版本迭代同步模具清洗预警并生成清洗计划完成");
    }

    @ApiOperation("硫化日完成量回填芯片库存")
    public void syncDayFinishQtyToChipStock() {
        log.info("硫化日完成量回填芯片库存-定时任务开始执行");
        try {
            FeignTokenHelper.runWithToken(() -> {
                AjaxResult result = iMesItfService.syncDayFinishQtyToChipStock();
                log.info("硫化日完成量回填芯片库存-定时任务结果：{}", result);
            });
        } catch (Exception e) {
            log.error("硫化日完成量回填芯片库存-定时任务异常", e);
        }
        log.info("硫化日完成量回填芯片库存-定时任务执行完成");
    }

    /**
     * 定时任务：从MES同步版本前缀为APS_MES_AH01的硫化精度数据并生成硫化精度计划
     * 现逻辑：MES全权决定计划时间和实际完成时间，APS只做同步+分发，不再回填/生成下一次精度计划。
     * 原 syncAndGenerateLhPrecisionPlanByVersionPrefixAllVersions 方法已 @Deprecated 备份保留。
     * 现改为调用 syncAndDispatchDevMaintenancePlan 完成同步+分发。
     */
    @ApiOperation("定时任务-按版本前缀APS_MES_AH01同步MES硫化精度数据并分发写入计划表")
    public void syncAndGenerateLhPrecisionPlanByAh01() {
        log.info("定时任务-开始按版本前缀APS_MES_AH01同步MES硫化精度数据并分发写入计划表");
        try {
            FeignTokenHelper.runWithToken(() -> {
                AjaxResult result = iMesItfService.syncAndDispatchDevMaintenancePlan(new AuxReqSyncDataLogs());
                log.info("定时任务执行结果：{}", result);
            });
        } catch (Exception e) {
            log.error("定时任务-按版本前缀APS_MES_AH01同步MES硫化精度数据并分发写入计划表异常", e);
        }
        log.info("定时任务-按版本前缀APS_MES_AH01同步MES硫化精度数据并分发写入计划表完成");
    }

    /**
     * 临时任务：按指定版本号同步设备保养计划并分发写入精度计划表
     * 与原syncDevMaintenancePlan的区别：不查最大版本号，直接按指定版本号查询MES中间表所有数据
     * 同步后按版本号从APS本地表精确查询本次同步数据，按精度类型分发到lh/cx模块精度计划表
     * 执行步骤：
     * 1. 从MES中间表按指定版本号查询设备保养计划数据（不限精度类型、不限日期）
     * 2. upsert到APS本地表T_MDM_DEV_MAINTENANCE_PLAN
     * 3. 按版本号查询本次同步的数据，按PRECISION_TYPE分发：
     *    - "硫化精度" → 调lh模块写入T_LH_PRECISION_PLAN
     *    - "成型精度15天"/"成型精度60天" → 调cx模块写入T_CX_PRECISION_PLAN
     */
    @ApiOperation("临时任务-按指定版本号同步设备保养计划并分发写入精度计划表")
    public void syncAndDispatchDevMaintenancePlanByVersion() {
        String dataVersion = "APS_MES_AH01_20260812000000001"; // TODO: 替换为实际版本号
        log.info("临时任务-开始按版本号{}同步设备保养计划并分发写入精度计划表", dataVersion);
        try {
            FeignTokenHelper.runWithToken(() -> {
                AjaxResult result = iMesItfService.syncAndDispatchDevMaintenancePlanByVersion(dataVersion);
                log.info("临时任务-按版本号{}同步分发结果：{}", dataVersion, result);
            });
        } catch (Exception e) {
            log.error("临时任务-按版本号{}同步设备保养计划并分发写入精度计划表异常", dataVersion, e);
        }
        log.info("临时任务-按版本号{}同步设备保养计划并分发写入精度计划表完成", dataVersion);
    }

    /**
     * 临时任务：按计划时间年份过滤，从MES同步硫化精度数据并生成目标年度计划
     * 已停用：现逻辑APS不再回填实际执行日期、不再生成下一次精度计划。
     * 现逻辑改由 syncDevMaintenancePlan 调 syncAndDispatchDevMaintenancePlan 完成同步+分发。
     * 方法签名保留以便调度配置不动，如需恢复原逻辑可调用 lhPrecisionPlanRemoteService.generatePlansFromMesByOperYear。
     */
    @ApiOperation("临时任务-按计划时间年份25年过滤生成26年硫化精度计划（已停用）")
    public void generateLhPrecisionPlanByOperYear2025() {
        log.info("临时任务generateLhPrecisionPlanByOperYear2025已停用：现逻辑APS不再回填/生成精度计划");
    }

    /**
     * 临时任务：同步MES 26年硫化精度数据，回填26年精度计划的实际执行日期，并生成27年精度计划
     * 已停用：现逻辑APS不再回填实际执行日期、不再生成下一次精度计划。
     * 现逻辑改由 syncDevMaintenancePlan 调 syncAndDispatchDevMaintenancePlan 完成同步+分发。
     * 方法签名保留以便调度配置不动，如需恢复原逻辑可调用 iMesItfService.syncAndFillActualDateByOperYear。
     */
    @ApiOperation("临时任务-同步26年数据回填26年精度计划实际日期并生成27年计划（已停用）")
    public void syncAndFillActualDateByOperYear2026() {
        log.info("临时任务syncAndFillActualDateByOperYear2026已停用：现逻辑APS不再回填/生成精度计划");
    }

    /**
     * 临时任务：按上一天最新版本号抓取硫化排程完成量回报数据
     * 逻辑同抓当天最新版本（syncLhClassShiftFinishQty），但日期条件改为上一天
     * 执行步骤：
     * 1. 从MES中间表查询上一天（SCHEDULE_DATE = DATEADD(DAY, -1, GETDATE())）的硫化排程完成量数据
     * 2. 按排程日期+硫化机台+订单号分组取MAX(DATA_VERSION)，获取上一天最新版本数据
     * 3. 逻辑删除APS旧数据并插入新数据
     * 4. 回填硫化排程结果表各班次完成量
     */
    @ApiOperation("临时任务-按上一天最新版本号抓取硫化排程完成量回报")
    public void syncLhClassShiftFinishQtyByYesterday() {
        log.info("临时任务-开始按上一天最新版本号抓取硫化排程完成量回报数据");
        try {
            FeignTokenHelper.runWithToken(() -> {
                AjaxResult result = iMesItfService.syncLhClassShiftFinishQtyByYesterday(new AuxReqSyncDataLogs());
                log.info("临时任务-按上一天最新版本号抓取硫化排程完成量回报结果：{}", result);
            });
        } catch (Exception e) {
            log.error("临时任务-按上一天最新版本号抓取硫化排程完成量回报异常", e);
        }
        log.info("临时任务-按上一天最新版本号抓取硫化排程完成量回报完成");
    }

    /**
     * 临时任务：按指定版本号APS_MES_AH01_20260622174000009抓取硫化排程完成量回报数据
     * 执行步骤：
     * 1. 从MES中间表按指定版本号查询硫化排程完成量数据（不限日期）
     * 2. 按排程日期分组，逐组逻辑删除APS旧数据并插入新数据
     * 3. 回填硫化排程结果表各班次完成量
     */
    @ApiOperation("临时任务-按版本号APS_MES_AH01_20260622174000009抓取硫化排程完成量回报")
    public void syncLhClassShiftFinishQtyByVersion() {
        String dataVersion = "APS_MES_AH01_20260713143800006";
        log.info("临时任务-开始按版本号{}抓取硫化排程完成量回报数据", dataVersion);
        try {
            FeignTokenHelper.runWithToken(() -> {
                AjaxResult result = iMesItfService.syncLhClassShiftFinishQtyByVersion(dataVersion);
                log.info("临时任务-按版本号{}抓取硫化排程完成量回报结果：{}", dataVersion, result);
            });
        } catch (Exception e) {
            log.error("临时任务-按版本号{}抓取硫化排程完成量回报异常", dataVersion, e);
        }
        log.info("临时任务-按版本号{}抓取硫化排程完成量回报完成", dataVersion);
    }

    /**
     * 临时任务：按最新版本号抓取硫化排程日完成量回报数据（不限日期）
     * 执行步骤：
     * 1. 从MES中间表查询硫化排程日完成量的最大版本号
     * 2. 按最新版本号查询所有日期的日完成量数据（不限日期，去掉原逻辑的前一天日期条件）
     * 3. 按完成日期分组，逐组逻辑删除APS旧数据并插入新数据
     * 4. 更新月计划监控表
     * 5. 增量更新芯片库存完成量
     */
    @ApiOperation("临时任务-按最新版本号抓取硫化排程日完成量回报（不限日期）")
    public void syncLhScheDayFinishQtyByLatestVersion() {
        String dataVersion = "APS_MES_AH01_20260721013000008";
        log.info("临时任务-开始按最新版本号抓取硫化排程日完成量回报（不限日期）");
        try {
            FeignTokenHelper.runWithToken(() -> {
                AjaxResult result = iMesItfService.syncLhScheDayFinishQtyByLatestVersion(dataVersion);
                log.info("临时任务-按最新版本号抓取硫化排程日完成量回报结果：{}", result);
            });
        } catch (Exception e) {
            log.error("临时任务-按最新版本号抓取硫化排程日完成量回报异常", e);
        }
        log.info("临时任务-按最新版本号抓取硫化排程日完成量回报完成");
    }

    /**
     * 同步设备停机计划
     */
    @ApiOperation("同步设备停机计划")
    public void syncDevPlanClose() {
        FeignTokenHelper.runWithToken(() -> iMesItfService.syncDevPlanClose(new AuxReqSyncDataLogs()));
    }
}
