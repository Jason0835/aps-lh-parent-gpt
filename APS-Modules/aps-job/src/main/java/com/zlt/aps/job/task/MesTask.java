package com.zlt.aps.job.task;

import com.zlt.aps.itf.mes.IMesItfService;
import com.zlt.aps.itf.vo.AuxReqSyncDataLogs;
import com.zlt.aps.mp.api.domain.entity.*;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * MES接口定时任务
 *
 * @author Chen
 * @since 2025/12/22
 */
@Component("mesTask")
public class MesTask {

    @Autowired
    private IMesItfService iMesItfService;

    /**
     * 同步成品库存
     */
    @ApiOperation("同步成品库存-默认当前年月")
    public void syncProductStock() {
        iMesItfService.syncProductStock(new MdmProductStock());
    }

    /**
     * 同步出库未扫描订单
     */
    @ApiOperation("同步出库未扫描订单-默认当前年月")
    public void syncOutboundOrdersNotScan() {
        iMesItfService.syncOutbountOrdersNotScan(new MdmOutbountOrdersNotScan());
    }

    /**
     * 同步不合格库存
     */
    @ApiOperation("同步不合格库存-默认当前日期")
    public void syncUnqualifiedStock() {
        iMesItfService.syncUnqualifiedStock(new MdmUnqualifiedStock());
    }

    /**
     * 同步特殊材料库存
     */
    @ApiOperation("同步特殊材料库存-默认当前日期")
    public void syncRawSpecialMaterialStock() {
        iMesItfService.syncRawSpecialMaterialStock(new RawSpecialMaterialStock());
    }

    /**
     * 同步成型在机数据
     */
    @ApiOperation("同步成型在机数据")
    public void syncMachineOnlineInfo() {
        iMesItfService.syncMachineOnlineInfo(new MdmCxMachineOnlineInfo());
    }

    /**
     * 同步硫化在机数据
     */
    @ApiOperation("同步硫化在机数据")
    public void syncLhMachineOnlineInfo() {
        iMesItfService.syncLhMachineOnlineInfo(new MdmLhMachineOnlineInfo());
    }

    /**
     * 同步设备保养计划
     */
    @ApiOperation("同步设备保养计划")
    public void syncDevMaintenancePlan() {
        iMesItfService.syncDevMaintenancePlan(new AuxReqSyncDataLogs());
    }

    /**
     * 同步模具清洗预警计划
     */
    @ApiOperation("同步模具清洗预警计划")
    public void syncMouldCleanPlan() {
        iMesItfService.syncMouldCleanPlan(new AuxReqSyncDataLogs());
    }

    /**
     * 同步结构整车胎面配置
     */
    @ApiOperation("同步结构整车胎面配置")
    public void syncStructureTreadConfig() {
        iMesItfService.syncStructureTreadConfig(new AuxReqSyncDataLogs());
    }

    /**
     * 同步成型排程完成量
     */
    @ApiOperation("同步成型排程完成量")
    public void syncCxClassShiftFinishQty() {
        iMesItfService.syncCxClassShiftFinishQty(new AuxReqSyncDataLogs());
    }

    /**
     * 同步硫化排程完成量
     */
    @ApiOperation("同步硫化排程完成量")
    public void syncLhClassShiftFinishQty() {
        iMesItfService.syncLhClassShiftFinishQty(new AuxReqSyncDataLogs());
    }

    /**
     * 同步成型排程日完成量
     */
    @ApiOperation("同步成型排程日完成量")
    public void syncCxScheDayFinishQty() {
        iMesItfService.syncCxScheDayFinishQty(new AuxReqSyncDataLogs());
    }

    /**
     * 同步硫化排程日完成量
     */
    @ApiOperation("同步硫化排程日完成量")
    public void syncLhScheDayFinishQty() {
        iMesItfService.syncLhScheDayFinishQty(new AuxReqSyncDataLogs());
    }

    /**
     * 同步模具交替计划完成回报
     */
    @ApiOperation("同步模具交替计划完成回报")
    public void syncMoldAlterPlanFinish() {
        iMesItfService.syncMoldAlterPlanFinish(new AuxReqSyncDataLogs());
    }
}
