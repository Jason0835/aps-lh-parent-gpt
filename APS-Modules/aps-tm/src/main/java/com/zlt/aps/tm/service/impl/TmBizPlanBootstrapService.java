package com.zlt.aps.tm.service.impl;

import com.zlt.aps.tm.engine.domain.TmScheduleContext;
import com.zlt.aps.tm.engine.service.ITmPlanBootstrapService;
import com.zlt.aps.tm.engine.service.TmPlanBootstrapService;
import com.zlt.aps.tm.service.TmAutoScheduleDataLoadService;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

/**
 * 胎面自动排程业务初始化步骤服务。
 *
 * <p>负责在模板初始化阶段完成基础上下文校验、批次追踪信息初始化，并加载自动排程所需业务数据。</p>
 */
@Primary
@Service
public class TmBizPlanBootstrapService implements ITmPlanBootstrapService {

    private final TmPlanBootstrapService planBootstrapService;

    private final TmAutoScheduleDataLoadService dataLoadService;

    /**
     * 创建胎面自动排程业务初始化步骤服务。
     *
     * @param planBootstrapService 引擎默认初始化服务，用于基础校验和批次追踪信息初始化
     * @param dataLoadService      胎面自动排程数据加载服务
     */
    public TmBizPlanBootstrapService(TmPlanBootstrapService planBootstrapService,
                                     TmAutoScheduleDataLoadService dataLoadService) {
        this.planBootstrapService = planBootstrapService;
        this.dataLoadService = dataLoadService;
    }

    /**
     * 执行业务初始化。
     *
     * @param context 胎面排程上下文，需包含工厂、排程日期和操作人
     */
    @Override
    public void bootstrap(TmScheduleContext context) {
        planBootstrapService.bootstrap(context);
        dataLoadService.loadAllData(context);
    }
}
