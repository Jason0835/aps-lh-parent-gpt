package com.zlt.aps.job.task;

import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.aps.autoLogin.feign.FeignTokenHelper;
import com.zlt.aps.cx.api.domain.entity.CxMachineOnlineInfo;
import com.zlt.aps.itf.mes.IMesItfService;
import com.zlt.aps.itf.vo.AuxReqSyncDataLogs;
import com.zlt.aps.lh.api.domain.entity.LhMachineOnlineInfo;
import com.zlt.aps.lh.api.service.ILhPrecisionPlanRemoteService;
import com.zlt.aps.mp.api.domain.entity.*;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

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
     * 同步设备保养计划
     */
    @ApiOperation("同步设备保养计划")
    public void syncDevMaintenancePlan() {
        FeignTokenHelper.runWithToken(() -> iMesItfService.syncDevMaintenancePlan(new AuxReqSyncDataLogs()));
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
        FeignTokenHelper.runWithToken(() -> iMesItfService.syncMesCxStock(new AuxReqSyncDataLogs()));
    }

    /**
     * 同步直裁库存（从 MES 中间表 T_MES_CD90_STOCK 同步到 t_cd90_stock）
     */
    @ApiOperation("同步直裁库存")
    public void syncMesCd90Stock() {
        FeignTokenHelper.runWithToken(() -> iMesItfService.syncMesCd90Stock(new AuxReqSyncDataLogs()));
    }

    /**
     * 同步胎圈库存
     */
    @ApiOperation("同步胎圈库存")
    public void syncMesTqStock() {
        FeignTokenHelper.runWithToken(() -> iMesItfService.syncMesTqStock(new AuxReqSyncDataLogs()));
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
     * 同步模具交替计划完成回报
     */
    @ApiOperation("同步模具交替计划完成回报")
    public void syncMoldAlterPlanFinish() {
        FeignTokenHelper.runWithToken(() -> iMesItfService.syncMoldAlterPlanFinish(new AuxReqSyncDataLogs()));
    }

    /**
     * 同步MES硫化精度计划实际执行日期回填数据
     */
    @ApiOperation("同步MES硫化精度计划实际执行日期回填数据")
    public void syncLhPrecisionPlanActual() {
        FeignTokenHelper.runWithToken(() -> iMesItfService.syncLhPrecisionPlanActual(new AuxReqSyncDataLogs()));
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
     * 执行步骤：
     * 1. 同步MES设备保养计划到APS（仅硫化精度）
     * 2. 同步MES硫化精度计划实际执行日期回填数据
     * 3. 只处理版本号前缀为APS_MES_AH01且类型为硫化精度的数据，生成新的硫化精度计划
     * 4. 新的硫化精度计划只有计划时间（planDate），实际时间（actualDate）为空，等待MES回填
     * 5. 自动推算下一年度硫化精度计划
     */
    @ApiOperation("临时任务-按版本前缀APS_MES_AH01抓取MES硫化精度数据并生成计划")
    public void syncAndGenerateLhPrecisionPlanByVersionPrefix() {
        log.info("临时任务-开始按版本前缀APS_MES_AH01抓取MES硫化精度数据并生成计划");
        try {
            FeignTokenHelper.runWithToken(() -> {
                Integer currentYear = LocalDate.now().getYear();
                AjaxResult result = iMesItfService.syncAndGenerateLhPrecisionPlanByVersionPrefix("APS_MES_AH01", currentYear);
                log.info("临时任务-按版本前缀APS_MES_AH01抓取MES硫化精度数据并生成计划结果：{}", result);
            });
        } catch (Exception e) {
            log.error("临时任务-按版本前缀APS_MES_AH01抓取MES硫化精度数据并生成计划异常", e);
        }
        log.info("临时任务-按版本前缀APS_MES_AH01抓取MES硫化精度数据并生成计划完成");
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
     * 与临时任务syncAndGenerateLhPrecisionPlanByVersionPrefix的区别：
     * 不限制最大版本号，版本前缀为APS_MES_AH01的所有版本数据都参与生成
     * 执行步骤：
     * 1. 同步MES设备保养计划到APS（仅硫化精度）
     * 2. 同步MES硫化精度计划实际执行日期回填数据
     * 3. 按版本前缀APS_MES_AH01过滤，不限最大版本号，生成硫化精度计划
     * 4. 自动推算下一年度硫化精度计划
     */
    @ApiOperation("定时任务-按版本前缀APS_MES_AH01同步MES硫化精度数据并生成计划（不限最大版本号）")
    public void syncAndGenerateLhPrecisionPlanByAh01() {
        log.info("定时任务-开始按版本前缀APS_MES_AH01同步MES硫化精度数据并生成计划（不限最大版本号）");
        try {
            FeignTokenHelper.runWithToken(() -> {
                Integer currentYear = LocalDate.now().getYear();
                AjaxResult result = iMesItfService.syncAndGenerateLhPrecisionPlanByVersionPrefixAllVersions("APS_MES_AH01", currentYear);
                log.info("定时任务执行结果：{}", result);
            });
        } catch (Exception e) {
            log.error("定时任务-按版本前缀APS_MES_AH01同步MES硫化精度数据并生成计划异常", e);
        }
        log.info("定时任务-按版本前缀APS_MES_AH01同步MES硫化精度数据并生成计划完成");
    }

    /**
     * 临时任务：按计划时间年份过滤，从MES同步硫化精度数据并生成目标年度计划
     * 取最新版本+版本前缀APS_MES_AH01+计划时间在25年的硫化精度数据，生成26年精度计划
     * 用于MES全量版本数据中只取特定年份的数据，避免跨年数据干扰
     * 执行步骤：
     * 1. 同步MES设备保养计划到APS（仅硫化精度）
     * 2. 按计划时间年份25年过滤，生成26年硫化精度计划
     */
    @ApiOperation("临时任务-按计划时间年份25年过滤生成26年硫化精度计划")
    public void generateLhPrecisionPlanByOperYear2025() {
        log.info("临时任务-开始按计划时间年份25年过滤生成26年硫化精度计划");
        try {
            FeignTokenHelper.runWithToken(() -> {
                // 先同步MES设备保养计划到APS（仅同步，不触发生成精度计划）
                AuxReqSyncDataLogs lhSyncParam = new AuxReqSyncDataLogs();
                lhSyncParam.setPrecisionType("硫化精度");
                AjaxResult syncResult = iMesItfService.syncDevMaintenancePlanOnly(lhSyncParam);
                log.info("同步设备保养计划结果：{}", syncResult.get("msg"));

                // 按计划时间年份25年过滤，生成26年硫化精度计划
                AjaxResult result = lhPrecisionPlanRemoteService.generatePlansFromMesByOperYear("APS_MES_AH01", 2025, 2026);
                log.info("临时任务执行结果：{}", result);
            });
        } catch (Exception e) {
            log.error("临时任务-按计划时间年份25年过滤生成26年硫化精度计划异常", e);
        }
        log.info("临时任务-按计划时间年份25年过滤生成26年硫化精度计划完成");
    }

    /**
     * 临时任务：同步MES 26年硫化精度数据，回填26年精度计划的实际执行日期，并生成27年精度计划
     * 执行步骤：
     * 1. 同步MES设备保养计划到APS（仅硫化精度），确保26年数据在本地表中
     * 2. 从APS本地表查版本前缀APS_MES_AH01+计划时间在26年+有实际执行时间的硫化精度数据
     * 3. 用查到的数据回填26年精度计划的实际执行日期
     * 4. 基于实际执行日期推算生成27年精度计划
     */
    @ApiOperation("临时任务-同步26年数据回填26年精度计划实际日期并生成27年计划")
    public void syncAndFillActualDateByOperYear2026() {
        log.info("临时任务-开始同步26年数据回填26年精度计划实际日期并生成27年计划");
        try {
            FeignTokenHelper.runWithToken(() -> {
                AjaxResult result = iMesItfService.syncAndFillActualDateByOperYear("APS_MES_AH01", 2026);
                log.info("临时任务执行结果：{}", result);
            });
        } catch (Exception e) {
            log.error("临时任务-同步26年数据回填26年精度计划实际日期并生成27年计划异常", e);
        }
        log.info("临时任务-同步26年数据回填26年精度计划实际日期并生成27年计划完成");
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
        String dataVersion = "APS_MES_AH01_20260630174000089";
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
        String dataVersion = "APS_MES_AH01_20260704160046006";
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
