package com.zlt.aps.tc.service;

import com.zlt.aps.tc.api.domain.vo.TcAutoScheduleIssueVo;
import com.zlt.aps.tc.api.domain.vo.TcAutoScheduleRequestVo;
import com.zlt.aps.tc.api.domain.vo.TcAutoScheduleResponseVo;
import com.zlt.aps.tc.domain.TcAutoScheduleTask;

import java.util.Date;
import java.util.List;

/**
 * 胎侧自动排程异步任务状态服务。
 */
public interface TcAutoScheduleTaskService {

    /**
     * 创建等待执行的胎侧自动排程任务。
     *
     * @param request  自动排程请求
     * @param response 初始排程响应
     * @return 任务对象
     */
    TcAutoScheduleTask createPending(TcAutoScheduleRequestVo request, TcAutoScheduleResponseVo response);

    /**
     * 根据任务 ID 查询任务。
     *
     * @param taskId 对外任务 ID
     * @return 任务对象，不存在时返回 null
     */
    TcAutoScheduleTask findByTaskId(String taskId);

    /**
     * 查询指定工厂和排程日期最近一次任务。
     *
     * @param factoryCode  工厂编码
     * @param scheduleDate 排程日期
     * @return 最近任务，不存在时返回 null
     */
    TcAutoScheduleTask findLatest(String factoryCode, Date scheduleDate);

    /**
     * 查询指定工厂和排程日期当前活跃任务。
     *
     * @param factoryCode  工厂编码
     * @param scheduleDate 排程日期
     * @return 活跃任务，不存在时返回 null
     */
    TcAutoScheduleTask findActive(String factoryCode, Date scheduleDate);

    /**
     * 将任务置为执行中。
     *
     * @param taskId 对外任务 ID
     * @return true 表示更新成功
     */
    boolean start(String taskId);

    /**
     * 更新任务进度。
     *
     * @param taskId    对外任务 ID
     * @param progress  进度百分比
     * @param stage     阶段编码
     * @param stageName 阶段名称
     * @return true 表示更新成功
     */
    boolean updateProgress(String taskId, int progress, String stage, String stageName);

    /**
     * 更新本次排程命中参数快照。
     *
     * @param taskId 对外任务 ID
     * @param paramSnapshot 参数快照对象
     * @return true 表示更新成功
     */
    boolean updateParamSnapshot(String taskId, Object paramSnapshot);

    /**
     * 标记任务成功。
     *
     * @param taskId   对外任务 ID
     * @param response 最终响应
     * @param issues   异常明细
     * @return true 表示更新成功
     */
    boolean markSuccess(String taskId, TcAutoScheduleResponseVo response, List<TcAutoScheduleIssueVo> issues);

    /**
     * 标记任务失败。
     *
     * @param taskId       对外任务 ID
     * @param errorMessage 错误摘要
     * @param issues       异常明细
     * @return true 表示更新成功
     */
    boolean markFailed(String taskId, String errorMessage, List<TcAutoScheduleIssueVo> issues);

    /**
     * 转换为前端轮询响应对象。
     *
     * @param task 任务对象
     * @return 自动排程响应
     */
    TcAutoScheduleResponseVo toResponse(TcAutoScheduleTask task);
}
