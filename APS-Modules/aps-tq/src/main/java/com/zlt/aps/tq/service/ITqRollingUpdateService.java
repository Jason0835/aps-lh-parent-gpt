package com.zlt.aps.tq.service;

import com.zlt.aps.tq.api.domain.dto.TqRollingRecalcRequestDTO;
import com.zlt.aps.tq.api.domain.vo.TqRollingRecalcResponseVO;
import com.zlt.aps.tq.engine.vo.RollingUpdateResult;

import java.util.Date;

/**
 * 胎圈排程滚动更新Service接口
 *
 * <p>对齐胎面 ITmRollingUpdateService，提供两类入口：</p>
 * <ul>
 *   <li>{@link #manualRollingUpdate} 手动触发（插单/调量/转机台/删除后），仅同班次内的时间重算和顺序调整</li>
 *   <li>{@link #rollingRecalcAutomatically} 自动触发（定时任务窗口命中），执行库存上下界调量算法</li>
 * </ul>
 *
 * @author APS
 */
public interface ITqRollingUpdateService {

    /**
     * 手动触发滚动更新
     *
     * <p>使用分布式锁保证同一天排程的滚动更新互斥执行。</p>
     *
     * @param triggerType     触发类型：1-插单，2-转机台，3-调量，4-删除
     * @param triggerSourceId 触发源排程记录ID
     * @param scheduleDate    排程日期
     * @param shiftIndex      触发班次索引（1~6）
     * @param machineCode     触发机台编号
     * @param beadCode        触发胎圈代码
     * @return 滚动更新结果
     */
    RollingUpdateResult manualRollingUpdate(String triggerType, Long triggerSourceId,
                                            Date scheduleDate, int shiftIndex,
                                            String machineCode, String beadCode);

    /**
     * 自动滚动重算入口。
     *
     * <p>对齐胎面 TmRollingUpdateServiceImpl.rollingRecalcAutomatically，
     * 由 TqAutoRollingApplicationService 在窗口锁内调用。</p>
     *
     * <p>执行步骤：</p>
     * <ol>
     *   <li>loadRollingContext 加载滚动上下文（复用 TqAutoScheduleDataLoadService 保证与自动排程同口径）</li>
     *   <li>TransactionTemplate.execute 行锁 + 调量算法 + 持久化 + 审计</li>
     *   <li>calculateAdjustments 库存上下界调量算法（参数化阈值）</li>
     *   <li>validateAffectedReleaseStatuses 释放状态校验</li>
     * </ol>
     *
     * @param request 重算请求（工厂、排程日期、库存日期、目标班次、操作人）
     * @return 滚动重算响应（含幂等键、调整统计、跳过摘要）
     */
    TqRollingRecalcResponseVO rollingRecalcAutomatically(TqRollingRecalcRequestDTO request);
}
