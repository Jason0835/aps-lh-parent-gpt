package com.zlt.aps.tq.engine.template;

import com.zlt.aps.tq.engine.context.TqScheduleContext;
import com.zlt.aps.tq.engine.handler.TqDemandCalcHandler;
import com.zlt.aps.tq.engine.handler.TqMachineAssignHandler;
import com.zlt.aps.tq.engine.handler.TqPreValidationHandler;
import com.zlt.aps.tq.engine.handler.TqResultPersistHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * 胎圈排程模板方法实现类。
 *
 * <p>绑定4个Handler到模板方法的4个阶段：</p>
 * <ul>
 *   <li>S1: TqPreValidationHandler - 前置校验与数据加载</li>
 *   <li>S2: TqDemandCalcHandler - 需求计算与均衡</li>
 *   <li>S3: TqMachineAssignHandler - 机台分配与排序</li>
 *   <li>S4: TqResultPersistHandler - 结果校验与持久化</li>
 * </ul>
 *
 * @author APS
 */
@Slf4j
@Component
public class TqScheduleTemplateImpl extends AbsTqScheduleTemplate {

    @Resource
    private TqPreValidationHandler preValidationHandler;

    @Resource
    private TqDemandCalcHandler demandCalcHandler;

    @Resource
    private TqMachineAssignHandler machineAssignHandler;

    @Resource
    private TqResultPersistHandler resultPersistHandler;

    @Override
    protected void doPreValidation(TqScheduleContext context) {
        preValidationHandler.handle(context);
    }

    @Override
    protected void doDemandCalcAndBalance(TqScheduleContext context) {
        demandCalcHandler.handle(context);
    }

    @Override
    protected void doMachineAssignAndSort(TqScheduleContext context) {
        machineAssignHandler.handle(context);
    }

    @Override
    protected void doResultValidationAndSave(TqScheduleContext context) {
        resultPersistHandler.handle(context);
    }
}
