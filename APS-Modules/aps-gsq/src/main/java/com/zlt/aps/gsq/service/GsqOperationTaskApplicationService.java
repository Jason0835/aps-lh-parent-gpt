package com.zlt.aps.gsq.service;

import com.zlt.aps.gsq.api.domain.entity.GsqScheduleResult;
import com.zlt.aps.gsq.api.domain.vo.GsqInsertTaskRequestVo;
import com.zlt.aps.gsq.api.domain.vo.GsqOperationTaskVo;

import java.util.Date;
import java.util.List;

/**
 * 钢丝圈人工操作任务应用服务。
 *
 * <p>对齐胎侧 {@code TcOperationTaskApplicationService}，作为 Controller 与
 * {@link GsqManualScheduleApplicationService} 之间的桥接层，负责：</p>
 * <ul>
 *   <li>请求基础校验（必填项、批量范围一致性）；</li>
 *   <li>构造 {@link com.zlt.aps.gsq.api.domain.vo.GsqOperationRequestSnapshot} 请求快照；</li>
 *   <li>调用 {@link GsqBackgroundTaskService#createOperationPending} 持久化任务；</li>
 *   <li>触发 {@link GsqOperationAsyncExecutor#execute} 异步执行；</li>
 *   <li>返回 {@link GsqOperationTaskVo} 供前端轮询。</li>
 * </ul>
 *
 * <p>该层不直接持有数据库事务或分布式锁，与 {@link GsqManualScheduleApplicationService}
 * 职责区分清晰：本服务面向 Controller 提供任务编排能力，
 * {@link GsqManualScheduleApplicationService} 面向异步执行器提供业务编排能力。</p>
 *
 * @author APS
 */
public interface GsqOperationTaskApplicationService {

    /**
     * 提交人工插单任务。
     *
     * @param request 插单请求
     * @return 初始任务
     */
    GsqOperationTaskVo submitInsert(GsqInsertTaskRequestVo request);

    /**
     * 提交批量调量任务。
     *
     * @param requestList 调量请求列表
     * @return 初始任务
     */
    GsqOperationTaskVo submitChangeQty(List<GsqScheduleResult> requestList);

    /**
     * 提交批量转机台任务。
     *
     * @param requestList 转机台请求列表
     * @return 初始任务
     */
    GsqOperationTaskVo submitChangeMachine(List<GsqScheduleResult> requestList);

    /**
     * 提交批量删除任务。
     *
     * @param resultIdList 排程结果 ID 列表
     * @return 初始任务
     */
    GsqOperationTaskVo submitDelete(List<Long> resultIdList);

    /**
     * 查询指定人工任务。
     *
     * @param taskId 任务编号
     * @return 任务响应
     */
    GsqOperationTaskVo getTask(String taskId);

    /**
     * 查询最近一次人工操作任务。
     *
     * @param factoryCode  工厂编码
     * @param scheduleDate 排程日期
     * @return 最近任务，不存在返回 null
     */
    GsqOperationTaskVo getLatestTask(String factoryCode, Date scheduleDate);
}
