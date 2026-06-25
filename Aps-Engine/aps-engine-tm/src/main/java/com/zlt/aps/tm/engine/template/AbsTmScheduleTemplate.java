package com.zlt.aps.tm.engine.template;

import com.ruoyi.common.exception.ServiceException;
import com.zlt.aps.tm.api.domain.vo.TmAutoScheduleResponseVo;
import com.zlt.aps.tm.api.enums.TmScheduleErrorCodeEnum;
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
     * @throws ServiceException 上下文为空时抛出
     */
    public TmAutoScheduleResponseVo execute(TmScheduleContext context) {
        if (context == null) {
            throw new ServiceException(TmScheduleErrorCodeEnum.TM_CONTEXT_EMPTY.getDefaultMessage());
        }
        doBootstrap(context);
        doInventoryPredict(context);
        doDemandAndPlanCalc(context);
        doTaskSort(context);
        doMachineAssign(context);
        doSnapshotAndPersist(context);
        return buildResponse(context);
    }

    /**
     * 初始化
     * @param context 上下文
     */
    protected abstract void doBootstrap(TmScheduleContext context);

    /**
     * 计算预计库存
     * @param context 上下文
     */
    protected abstract void doInventoryPredict(TmScheduleContext context);

    /**
     * 需求量和计划量计算
     * @param context 上下文
     */
    protected abstract void doDemandAndPlanCalc(TmScheduleContext context);

    /**
     * 待排任务排序
     * @param context 上下文
     */
    protected abstract void doTaskSort(TmScheduleContext context);

    /**
     * 机台分配
     * @param context 上下文
     */
    protected abstract void doMachineAssign(TmScheduleContext context);

    /**
     * 执行解释快照构建和落库
     * @param context 上下文
     */
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
