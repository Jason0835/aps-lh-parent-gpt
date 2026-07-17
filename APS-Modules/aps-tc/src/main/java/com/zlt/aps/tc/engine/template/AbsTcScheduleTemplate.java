package com.zlt.aps.tc.engine.template;

import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.aps.tc.api.domain.vo.TcAutoScheduleResponseVo;
import com.zlt.aps.tc.api.enums.TcScheduleErrorCodeEnum;
import com.zlt.aps.tc.engine.domain.TcScheduleContext;

/**
 * 胎侧自动排程模板抽象类。
 *
 * <p>固定第16章定义的自动排程主流程，子类或步骤服务只实现具体业务步骤，不改变主流程顺序。
 * 该模板会按顺序调用初始化、库存预测、计划计算、任务排序、机台分配、解释快照和落库。</p>
 */
public abstract class AbsTcScheduleTemplate {

    /**
     * 执行胎侧自动排程模板流程。
     *
     * @param context 胎侧排程上下文，必须由调用方传入排程日期和操作人，步骤服务会补充运行态数据
     * @return 自动排程响应对象
     * @throws ServiceException 上下文为空时抛出
     */
    public TcAutoScheduleResponseVo execute(TcScheduleContext context) {
        if (context == null) {
            throw new ServiceException(TcScheduleErrorCodeEnum.TC_CONTEXT_EMPTY.getDefaultMessage());
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
    protected abstract void doBootstrap(TcScheduleContext context);

    /**
     * 计算预计库存
     * @param context 上下文
     */
    protected abstract void doInventoryPredict(TcScheduleContext context);

    /**
     * 需求量和计划量计算
     * @param context 上下文
     */
    protected abstract void doDemandAndPlanCalc(TcScheduleContext context);

    /**
     * 待排任务排序
     * @param context 上下文
     */
    protected abstract void doTaskSort(TcScheduleContext context);

    /**
     * 机台分配
     * @param context 上下文
     */
    protected abstract void doMachineAssign(TcScheduleContext context);

    /**
     * 执行解释快照构建和落库
     * @param context 上下文
     */
    protected abstract void doSnapshotAndPersist(TcScheduleContext context);

    /**
     * 构建排程响应。
     *
     * @param context 胎侧排程上下文
     * @return 自动排程响应
     */
    protected TcAutoScheduleResponseVo buildResponse(TcScheduleContext context) {
        TcAutoScheduleResponseVo responseVo = new TcAutoScheduleResponseVo();
        responseVo.setSuccess(Boolean.TRUE);
        responseVo.setBatchNo(context.getBatchNo());
        responseVo.setTraceId(context.getTraceId());
        responseVo.setResultCount(context.getTaskDraftList().size());
        responseVo.setUnplannedCount(0);
        responseVo.setMessage(I18nUtil.getMessage("ui.tc.schedule.skeletonFinished"));
        return responseVo;
    }
}
