package com.zlt.aps.tm.engine.service;

import com.zlt.aps.tm.engine.domain.TmScheduleContext;

/**
 * 胎面机台分配步骤服务。
 *
 * <p>负责调用机台过滤规则和评分策略，把任务插入机台班次任务链。骨架阶段不落具体业务规则。</p>
 */
public interface ITmMachineAssignService {

    /**
     * 执行机台分配。
     *
     * @param context 胎面排程上下文，方法会按实现修改任务链
     */
    void assign(TmScheduleContext context);
}
