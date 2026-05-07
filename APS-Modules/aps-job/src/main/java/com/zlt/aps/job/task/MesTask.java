package com.zlt.aps.job.task;

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
}
