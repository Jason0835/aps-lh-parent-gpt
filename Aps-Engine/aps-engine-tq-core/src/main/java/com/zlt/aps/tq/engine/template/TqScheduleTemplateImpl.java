package com.zlt.aps.tq.engine.template;

import com.zlt.aps.tq.engine.context.TqScheduleContext;
import com.zlt.aps.tq.engine.handler.TqBalanceHandler;
import com.zlt.aps.tq.engine.handler.TqDemandQtyCalcHandler;
import com.zlt.aps.tq.engine.handler.TqMachineAssignHandler;
import com.zlt.aps.tq.engine.handler.TqPlanQtyCalcHandler;
import com.zlt.aps.tq.engine.handler.TqPreValidationHandler;
import com.zlt.aps.tq.engine.handler.TqQuotaValidateHandler;
import com.zlt.aps.tq.engine.handler.TqResidualCapacityHandler;
import com.zlt.aps.tq.engine.handler.TqResultPersistHandler;
import com.zlt.aps.tq.engine.handler.TqStopCoordinationHandler;
import com.zlt.aps.tq.engine.handler.TqStockPredictHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * 胎圈排程模板方法实现类。
 *
 * <p>绑定10个Handler到模板方法的10个阶段：</p>
 * <ul>
 *   <li>S1:   TqPreValidationHandler    - 前置校验与数据加载</li>
 *   <li>S2.1: TqStockPredictHandler     - 库存预测（供应时长计算）</li>
 *   <li>S2.2: TqDemandQtyCalcHandler    - 需求量计算（收尾判断）</li>
 *   <li>S2.3: TqPlanQtyCalcHandler      - 计划量计算（6 班滚动 + 备库分摊）</li>
 *   <li>S3:   TqMachineAssignHandler    - 班次排产分配</li>
 *   <li>S4:   TqStopCoordinationHandler - 成型/胎圈停产协调</li>
 *   <li>S5:   TqBalanceHandler         - 班次均衡调整</li>
 *   <li>S5.5: TqQuotaValidateHandler    - 定额校验与顺序重置</li>
 *   <li>S5.6: TqResidualCapacityHandler - 最终剩余产能回填（原 S3.5，移至 S5.5 之后避免被覆盖）</li>
 *   <li>S6:   TqResultPersistHandler    - 结果校验与持久化</li>
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
    private TqStockPredictHandler stockPredictHandler;

    @Resource
    private TqDemandQtyCalcHandler demandQtyCalcHandler;

    @Resource
    private TqPlanQtyCalcHandler planQtyCalcHandler;

    @Resource
    private TqMachineAssignHandler machineAssignHandler;

    @Resource
    private TqResidualCapacityHandler residualCapacityHandler;

    @Resource
    private TqStopCoordinationHandler stopCoordinationHandler;

    @Resource
    private TqBalanceHandler balanceHandler;

    @Resource
    private TqQuotaValidateHandler quotaValidateHandler;

    @Resource
    private TqResultPersistHandler resultPersistHandler;

    @Override
    protected void doPreValidation(TqScheduleContext context) {
        preValidationHandler.handle(context);
    }

    @Override
    protected void doStockPredict(TqScheduleContext context) {
        stockPredictHandler.handle(context);
    }

    @Override
    protected void doDemandQtyCalc(TqScheduleContext context) {
        demandQtyCalcHandler.handle(context);
    }

    @Override
    protected void doPlanQtyCalc(TqScheduleContext context) {
        planQtyCalcHandler.handle(context);
    }

    @Override
    protected void doMachineAssign(TqScheduleContext context) {
        machineAssignHandler.handle(context);
    }

    @Override
    protected void doResidualCapacity(TqScheduleContext context) {
        residualCapacityHandler.handle(context);
    }

    @Override
    protected void doStopCoordination(TqScheduleContext context) {
        stopCoordinationHandler.handle(context);
    }

    @Override
    protected void doBalance(TqScheduleContext context) {
        balanceHandler.handle(context);
    }

    @Override
    protected void doQuotaValidate(TqScheduleContext context) {
        quotaValidateHandler.handle(context);
    }

    @Override
    protected void doResultValidationAndSave(TqScheduleContext context) {
        resultPersistHandler.handle(context);
    }
}
