package com.zlt.aps.job.task;

import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.aps.autoLogin.feign.FeignTokenHelper;
import com.zlt.aps.cx.api.domain.entity.CxMachineOnlineInfo;
import com.zlt.aps.itf.mes.IMesItfService;
import com.zlt.aps.itf.vo.AuxReqSyncDataLogs;
import com.zlt.aps.lh.api.domain.entity.LhMachineOnlineInfo;
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
}
