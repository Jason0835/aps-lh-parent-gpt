package com.zlt.aps.tc.engine.service;

import com.zlt.aps.tc.engine.domain.TcScheduleContext;

/**
 * 胎侧排程初始化步骤服务。
 *
 * <p>负责生成批次号、追踪号并加载参数与基础资料。骨架阶段只定义契约，具体数据加载由后续实现补充。</p>
 */
public interface ITcPlanBootstrapService {

    /**
     * 执行初始化步骤。
     *
     * @param context 胎侧排程上下文，方法会按实现补充批次、参数和基础资料
     */
    void bootstrap(TcScheduleContext context);
}
