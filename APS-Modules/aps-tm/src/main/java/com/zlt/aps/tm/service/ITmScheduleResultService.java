package com.zlt.aps.tm.service;

import com.zlt.aps.tm.api.domain.dto.TmRollingRecalcRequestDTO;
import com.zlt.aps.tm.api.domain.entity.TmScheduleResult;
import com.zlt.aps.tm.api.domain.vo.TmAutoScheduleRequestVo;
import com.zlt.aps.tm.api.domain.vo.TmAutoScheduleResponseVo;
import com.zlt.aps.tm.api.domain.vo.TmRollingRecalcResponseVO;
import com.zlt.aps.tm.api.domain.vo.TmScheduleShiftDateVO;
import com.zlt.bill.common.service.IDocService;

import java.util.Date;
import java.util.List;

/**
 * 胎面排程结果表 服务接口
 */
public interface ITmScheduleResultService extends IDocService<TmScheduleResult> {

    /**
     * 修改胎面排程结果
     * @param scheduleResult 胎面排程结果
     * @return 结果
     */
    int updateTmScheduleResult(TmScheduleResult scheduleResult);

    /**
     * 根据id查询当前日期发布状态为"发布中"或"超时失败"的记录数
     * @param ids id数组
     * @return 符合条件的记录数
     */
    int isReleasingOrTimeoutByIds(Long[] ids);

    /**
     * 记录调度员操作日志
     * @param operType 操作类型：0--转机台、1--调量
     * @param newSchedule 操作后的排程数据
     */
    void insetDispatcherLog(String operType, TmScheduleResult newSchedule);

    /**
     * 按工厂和排程日期逻辑删除当前有效批次数据
     * @param factoryCode 工厂编号
     * @param scheduleDate 排程日期
     */
    void logicDeleteByFactoryCodeAndScheduleDate(String factoryCode, Date scheduleDate);

    /**
     * 校验胎面自动排程请求是否满足结构闭环执行条件。
     *
     * @param request 自动排程请求
     * @return 自动排程响应，包含批次、追踪号和校验消息
     * @throws com.ruoyi.common.exception.ServiceException 请求缺少工厂或排程日期时抛出
     */
    TmAutoScheduleResponseVo validateTmAutoPlan(TmAutoScheduleRequestVo request);

    /**
     * 校验胎面自动排程请求。
     *
     * <p>兼容旧 Java 调用名，后续新代码请使用 {@link #validateTmAutoPlan(TmAutoScheduleRequestVo)}。</p>
     *
     * @param request 自动排程请求
     * @return 自动排程校验响应
     */
    @Deprecated
    default TmAutoScheduleResponseVo validateAutoPlan(TmAutoScheduleRequestVo request) {
        return validateTmAutoPlan(request);
    }

    /**
     * 执行胎面自动排程结构闭环。
     *
     * <p>当前方法只串联请求校验和结果汇总，不执行未确认的完整算法、不覆盖旧批次。</p>
     *
     * @param request 自动排程请求
     * @return 自动排程响应
     * @throws com.ruoyi.common.exception.ServiceException 请求非法时抛出
     */
    TmAutoScheduleResponseVo tmAutoPlan(TmAutoScheduleRequestVo request);

    /**
     * 执行已提交的胎面自动排程后台任务。
     *
     * @param taskId 自动排程任务 ID
     * @return 自动排程最终响应
     */
    TmAutoScheduleResponseVo executeTmAutoPlanTask(String taskId);
    /**
     * 清理胎面自动排程 Redis 基础资料缓存。
     *
     * @param factoryCode 工厂编码，为空时清理全部胎面自动排程缓存
     * @param scheduleDate 排程日期，和工厂同时传入时清理该日期相关缓存
     * @return 实际删除的 Redis key 数量
     */
    long clearAutoPlanRedisCache(String factoryCode, Date scheduleDate);


    /**
     * 执行胎面自动排程。
     *
     * <p>兼容旧 Java 调用名，后续新代码请使用 {@link #tmAutoPlan(TmAutoScheduleRequestVo)}。</p>
     *
     * @param request 自动排程请求
     * @return 自动排程响应
     */
    @Deprecated
    default TmAutoScheduleResponseVo autoPlan(TmAutoScheduleRequestVo request) {
        return tmAutoPlan(request);
    }

    /**
     * 查询胎面排程看板数据。
     *
     * @param query 查询条件
     * @return 看板结果列表
     */
    List<TmScheduleResult> listBoard(TmScheduleResult query);

    /**
     * 插入人工插单排程结果。
     *
     * @param scheduleResult 插单结果
     * @return 写入行数
     * @throws com.ruoyi.common.exception.ServiceException 必填字段缺失时抛出
     */
    int insertTask(TmScheduleResult scheduleResult);

    /**
     * 批量删除未发布排程结果并滚动重排受影响机台。
     *
     * @param ids 排程结果 ID
     * @return 删除行数
     * @throws com.ruoyi.common.exception.ServiceException ID 为空、记录缺失或包含非未发布状态时抛出
     */
    int removeScheduleResults(List<Long> ids);

    /**
     * 调整排程计划量。
     *
     * @param scheduleResult 调量后的排程结果
     * @return 更新行数
     * @throws com.ruoyi.common.exception.ServiceException 记录不存在或处于不可调整状态时抛出
     */
    int changeQty(TmScheduleResult scheduleResult);

    /**
     * 调整排程机台。
     *
     * @param scheduleResult 转机台后的排程结果
     * @return 更新行数
     * @throws com.ruoyi.common.exception.ServiceException 记录不存在或处于不可调整状态时抛出
     */
    int changeMachine(TmScheduleResult scheduleResult);

    /**
     * 在单个事务中批量调整排程机台。
     *
     * @param machineCode 目标机台编码
     * @param scheduleResultList 待转机的排程结果
     * @return 更新行数
     * @throws com.ruoyi.common.exception.ServiceException 任一记录校验或转机失败时抛出并整批回滚
     */
    int batchChangeMachine(String machineCode, List<TmScheduleResult> scheduleResultList);

    /**
     * 手动触发自动滚动重算。
     *
     * @param request 工厂、日期和目标班次
     * @return 滚动重算统计
     * @throws com.ruoyi.common.exception.ServiceException 参数、状态、锁或事务失败时抛出
     */
    TmRollingRecalcResponseVO rollingRecalc(TmRollingRecalcRequestDTO request);

    /**
     * 单步撤销最近一次人工操作（插单/调量/转机台）。
     *
     * @param dispatcherLogId 调度日志 ID
     * @return 恢复的记录数
     * @throws com.ruoyi.common.exception.ServiceException 日志不存在、已撤销或目标记录发布中时抛出
     */
    int undoLastOperation(Long dispatcherLogId);

    /**
     * 校验排程结果是否允许发布。
     *
     * @param ids 排程结果ID列表
     * @return true 表示可发布
     * @throws com.ruoyi.common.exception.ServiceException 参数非法或存在发布中/超时失败记录时抛出
     */
    boolean publishValidate(List<Long> ids);

    /**
     * 将排程结果标记为待发布。
     *
     * @param ids 排程结果ID列表
     * @return 更新行数
     * @throws com.ruoyi.common.exception.ServiceException 参数非法或记录不可发布时抛出
     */
    int publish(List<Long> ids);

    /**
     * 更改排程结果发布状态
     *
     * @param ids 排程结果ID列表，逗号分隔
     * @param releaseStatus 发布状态
     * @return 更新行数
     */
    int changeReleaseStatus(String ids, String releaseStatus);

    /**
     * 胎面排程班次日期列表
     * 根据排程日期构建6个班次的日期展示列表
     *
     * @param scheduleDate 排程日期
     * @return 班次日期列表
     */
    List<TmScheduleShiftDateVO> listScheduleShiftDates(Date scheduleDate);
}
