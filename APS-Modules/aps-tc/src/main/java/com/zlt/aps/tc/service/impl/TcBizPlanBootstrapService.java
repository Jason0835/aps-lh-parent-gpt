package com.zlt.aps.tc.service.impl;

import com.zlt.aps.tc.engine.domain.TcScheduleContext;
import com.zlt.aps.tc.engine.service.ITcPlanBootstrapService;
import com.zlt.aps.tc.engine.service.impl.TcPlanBootstrapService;
import com.zlt.aps.tc.service.loader.TcAutoScheduleDataLoadService;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

/**
 * 胎侧自动排程业务初始化步骤服务。
 *
 * <p>负责在模板初始化阶段完成基础上下文校验、批次追踪信息初始化，并加载自动排程所需业务数据。</p>
 */
@Primary
@Service
public class TcBizPlanBootstrapService implements ITcPlanBootstrapService {

    private final TcPlanBootstrapService planBootstrapService;

    private final TcAutoScheduleDataLoadService dataLoadService;

    private final com.zlt.aps.tc.service.TcAutoScheduleTaskService autoScheduleTaskService;

    /**
     * 创建胎侧自动排程业务初始化步骤服务。
     *
     * @param planBootstrapService 引擎默认初始化服务，用于基础校验和批次追踪信息初始化
     * @param dataLoadService      胎侧自动排程数据加载服务
     */
    public TcBizPlanBootstrapService(TcPlanBootstrapService planBootstrapService,
                                     TcAutoScheduleDataLoadService dataLoadService,
                                     com.zlt.aps.tc.service.TcAutoScheduleTaskService autoScheduleTaskService) {
        this.planBootstrapService = planBootstrapService;
        this.dataLoadService = dataLoadService;
        this.autoScheduleTaskService = autoScheduleTaskService;
    }

    /**
     * 执行业务初始化。
     *
     * @param context 胎侧排程上下文，需包含工厂、排程日期和操作人
     */
    @Override
    public void bootstrap(TcScheduleContext context) {
        planBootstrapService.bootstrap(context);
        dataLoadService.loadAllData(context);
        if (context.getTaskId() != null) {
            autoScheduleTaskService.updateParamSnapshot(context.getTaskId(), context.getParamMap());
        }
    }
}
