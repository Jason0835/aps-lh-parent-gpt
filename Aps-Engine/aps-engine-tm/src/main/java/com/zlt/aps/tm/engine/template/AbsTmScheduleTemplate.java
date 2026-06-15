package com.zlt.aps.tm.engine.template;

import com.zlt.aps.tm.api.domain.vo.TmAutoScheduleResponseVo;
import com.zlt.aps.tm.engine.domain.TmScheduleContext;

/**
 * 胎面自动排程模板抽象类。
 *
 * <p>固定第16章定义的自动排程主流程，子类或步骤服务只实现具体业务步骤，不改变主流程顺序。
 * 该模板会按顺序调用初始化、库存预测、计划计算、任务排序、机台分配、解释快照和落库。</p>
 */
public abstract class AbsTmScheduleTemplate {

    /**
     * 执行胎面自动排程模板流程。
     *
     * @param context 胎面排程上下文，必须由调用方传入排程日期和操作人，步骤服务会补充运行态数据
     * @return 自动排程响应对象
     * @throws IllegalArgumentException 上下文为空时抛出
     */
    public TmAutoScheduleResponseVo execute(TmScheduleContext context) {
        if (context == null) {
            throw new IllegalArgumentException("胎面排程上下文不能为空");
        }
        doBootstrap(context);
        doInventoryPredict(context);
        doDemandAndPlanCalc(context);
        doTaskSort(context);
        doMachineAssign(context);
        doSnapshotAndPersist(context);
        return buildResponse(context);
    }

    protected abstract void doBootstrap(TmScheduleContext context);

    protected abstract void doInventoryPredict(TmScheduleContext context);

    protected abstract void doDemandAndPlanCalc(TmScheduleContext context);

    protected abstract void doTaskSort(TmScheduleContext context);

    protected abstract void doMachineAssign(TmScheduleContext context);

    protected abstract void doSnapshotAndPersist(TmScheduleContext context);

    /**
     * 构建排程响应。
     *
     * @param context 胎面排程上下文
     * @return 自动排程响应
     */
    protected TmAutoScheduleResponseVo buildResponse(TmScheduleContext context) {
        TmAutoScheduleResponseVo responseVo = new TmAutoScheduleResponseVo();
        responseVo.setSuccess(Boolean.TRUE);
        responseVo.setBatchNo(context.getBatchNo());
        responseVo.setTraceId(context.getTraceId());
        responseVo.setResultCount(context.getTaskDraftList().size());
        responseVo.setUnplannedCount(0);
        responseVo.setMessage("胎面排程骨架执行完成");
        return responseVo;
    }
}
