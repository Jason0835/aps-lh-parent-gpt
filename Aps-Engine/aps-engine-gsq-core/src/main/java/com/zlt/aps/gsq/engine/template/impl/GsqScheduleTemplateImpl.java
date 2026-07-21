package com.zlt.aps.gsq.engine.template.impl;

import com.zlt.aps.gsq.engine.context.GsqScheduleContext;
import com.zlt.aps.gsq.engine.handler.GsqBalanceHandler;
import com.zlt.aps.gsq.engine.handler.GsqDemandCalcHandler;
import com.zlt.aps.gsq.engine.handler.GsqMachineAssignHandler;
import com.zlt.aps.gsq.engine.handler.GsqPreValidationHandler;
import com.zlt.aps.gsq.engine.handler.GsqQuotaValidateHandler;
import com.zlt.aps.gsq.engine.handler.GsqResidualCapacityHandler;
import com.zlt.aps.gsq.engine.handler.GsqResultValidationHandler;
import com.zlt.aps.gsq.engine.handler.GsqStopCoordinationHandler;
import com.zlt.aps.gsq.engine.template.AbsGsqScheduleTemplate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * 钢丝圈排程模板方法实现。
 *
 * <p>绑定8个Handler，按顺序执行 S1 → S2 → S3 → S3.5 → S4 → S5 → S5.5 → S6。</p>
 *
 * @author APS
 */
@Slf4j
@Component
public class GsqScheduleTemplateImpl extends AbsGsqScheduleTemplate {

    @Resource
    private GsqPreValidationHandler preValidationHandler;

    @Resource
    private GsqDemandCalcHandler demandCalcHandler;

    @Resource
    private GsqMachineAssignHandler machineAssignHandler;

    @Resource
    private GsqResidualCapacityHandler residualCapacityHandler;

    @Resource
    private GsqStopCoordinationHandler stopCoordinationHandler;

    @Resource
    private GsqBalanceHandler balanceHandler;

    @Resource
    private GsqQuotaValidateHandler quotaValidateHandler;

    @Resource
    private GsqResultValidationHandler resultValidationHandler;

    @Override
    protected void doPreValidation(GsqScheduleContext context) {
        preValidationHandler.handle(context);
    }

    @Override
    protected void doDemandCalc(GsqScheduleContext context) {
        demandCalcHandler.handle(context);
    }

    @Override
    protected void doMachineAssign(GsqScheduleContext context) {
        machineAssignHandler.handle(context);
    }

    @Override
    protected void doResidualCapacity(GsqScheduleContext context) {
        residualCapacityHandler.handle(context);
    }

    @Override
    protected void doStopCoordination(GsqScheduleContext context) {
        stopCoordinationHandler.handle(context);
    }

    @Override
    protected void doBalance(GsqScheduleContext context) {
        balanceHandler.handle(context);
    }

    @Override
    protected void doQuotaValidate(GsqScheduleContext context) {
        quotaValidateHandler.handle(context);
    }

    @Override
    protected void doResultValidationAndSave(GsqScheduleContext context) {
        resultValidationHandler.handle(context);
    }
}
