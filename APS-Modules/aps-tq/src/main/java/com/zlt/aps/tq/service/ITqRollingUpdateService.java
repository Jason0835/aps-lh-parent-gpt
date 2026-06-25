package com.zlt.aps.tq.service;

import com.zlt.aps.tq.engine.vo.RollingUpdateResult;

import java.util.Date;

/**
 * 胎圈排程滚动更新Service接口
 *
 * <p>MVP阶段范围：</p>
 * <ul>
 *   <li>仅手动触发（插单/调量/转机台/删除后）</li>
 *   <li>仅同班次内的时间重算和顺序调整</li>
 *   <li>不实现跨班次推迟</li>
 *   <li>日志仅记录主表</li>
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
}
