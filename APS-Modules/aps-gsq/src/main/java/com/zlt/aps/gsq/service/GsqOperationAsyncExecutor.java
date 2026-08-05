package com.zlt.aps.gsq.service;

/**
 * 钢丝圈人工操作异步执行边界。
 *
 * <p>对齐胎侧 {@code TcOperationAsyncExecutor}，由 {@code GsqScheduleResultServiceImpl}
 * 在4类人工操作（插单/调量/转机台/删除）创建后台任务后派发。</p>
 *
 * <p>异步执行器负责：</p>
 * <ol>
 *   <li>将任务状态从 PENDING -> RUNNING；</li>
 *   <li>反序列化请求快照，恢复操作人上下文；</li>
 *   <li>按任务类型委托 {@link GsqManualScheduleApplicationService}，最终由
 *       {@link com.zlt.aps.gsq.service.impl.GsqManualOperationFacade} 完成锁、行锁、滚动和审计；</li>
 *   <li>更新任务进度并最终标记为 SUCCESS/FAILED。</li>
 * </ol>
 *
 * @author APS
 */
public interface GsqOperationAsyncExecutor {

    /**
     * 异步执行人工操作任务。
     *
     * @param taskId 任务ID
     */
    void execute(String taskId);
}
