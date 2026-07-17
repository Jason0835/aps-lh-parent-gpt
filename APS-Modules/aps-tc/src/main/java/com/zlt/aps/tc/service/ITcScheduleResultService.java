package com.zlt.aps.tc.service;

import com.zlt.aps.tc.api.domain.entity.TcScheduleResult;
import com.zlt.aps.tc.api.domain.vo.TcAutoScheduleRequestVo;
import com.zlt.aps.tc.api.domain.vo.TcAutoScheduleResponseVo;
import com.zlt.bill.common.service.IDocService;

import java.util.Date;
import java.util.List;

/**
 * 胎侧自动排程结果服务接口。
 */
public interface ITcScheduleResultService extends IDocService<TcScheduleResult> {

    /**
     * 校验自动排程请求和旧结果覆盖条件。
     *
     * @param request 自动排程请求
     * @return 校验响应
     * @throws com.ruoyi.common.exception.ServiceException 请求非法或存在不可覆盖结果时抛出
     */
    TcAutoScheduleResponseVo validateAutoPlan(TcAutoScheduleRequestVo request);

    /**
     * 创建胎侧自动排程异步任务。
     *
     * @param request 自动排程请求
     * @return 待执行任务响应
     * @throws com.ruoyi.common.exception.ServiceException 请求非法或未确认覆盖时抛出
     */
    TcAutoScheduleResponseVo autoPlan(TcAutoScheduleRequestVo request);

    /**
     * 执行已提交的胎侧自动排程任务。
     *
     * @param taskId 对外任务编号
     * @return 最终排程响应
     * @throws com.ruoyi.common.exception.ServiceException 任务不存在或排程失败时抛出
     */
    TcAutoScheduleResponseVo executeTcAutoPlanTask(String taskId);

    /**
     * 查询自动排程任务。
     *
     * @param taskId 对外任务编号
     * @return 任务响应
     * @throws com.ruoyi.common.exception.ServiceException 任务不存在时抛出
     */
    TcAutoScheduleResponseVo getAutoPlanTask(String taskId);

    /**
     * 查询指定工厂和排程日期最近一次任务。
     *
     * @param factoryCode 工厂编码
     * @param scheduleDate 排程日期
     * @return 最近任务响应
     * @throws com.ruoyi.common.exception.ServiceException 参数非法或任务不存在时抛出
     */
    TcAutoScheduleResponseVo getLatestAutoPlanTask(String factoryCode, Date scheduleDate);

    /**
     * 清理胎侧自动排程基础资料缓存。
     *
     * @param factoryCode 工厂编码，为空时清理全部胎侧缓存
     * @param scheduleDate 排程日期
     * @return 删除的缓存键数量
     */
    long clearAutoPlanRedisCache(String factoryCode, Date scheduleDate);

    /**
     * 查询胎侧排程结果只读列表。
     *
     * @param query 查询条件
     * @return 排程结果列表
     */
    List<TcScheduleResult> listResult(TcScheduleResult query);
}
