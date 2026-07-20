package com.zlt.aps.gsq.engine.service;

import com.zlt.aps.gsq.api.domain.entity.GsqMachineInfo;
import com.zlt.aps.gsq.engine.context.GsqScheduleContext;
import com.zlt.aps.gsq.engine.strategy.IMachineFilterStrategy;
import com.zlt.aps.gsq.engine.vo.GsqScheduleResultVo;

import java.util.List;

/**
 * 钢丝圈机台过滤策略链Service。
 *
 * <p>按策略的order顺序串联执行所有过滤策略。</p>
 *
 * @author APS
 */
public interface IGsqMachineFilterChainService {

    /**
     * 按顺序执行所有过滤策略，返回最终可用的机台列表。
     *
     * @param machines    全部机台列表
     * @param scheduleVo  当前排程记录
     * @param context     排程上下文
     * @return 过滤后的可用机台列表
     */
    List<GsqMachineInfo> filter(List<GsqMachineInfo> machines,
                                 GsqScheduleResultVo scheduleVo,
                                 GsqScheduleContext context);

    /**
     * 注册策略到策略链。
     *
     * @param strategy 策略实现
     */
    void registerStrategy(IMachineFilterStrategy strategy);
}
