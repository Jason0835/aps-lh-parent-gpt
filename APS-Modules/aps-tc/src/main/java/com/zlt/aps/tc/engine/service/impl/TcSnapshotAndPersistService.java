package com.zlt.aps.tc.engine.service.impl;

import cn.hutool.core.collection.CollUtil;
import com.ruoyi.common.exception.ServiceException;
import com.zlt.aps.tc.api.enums.TcScheduleErrorCodeEnum;
import com.zlt.aps.tc.engine.domain.TcPersistResult;
import com.zlt.aps.tc.engine.domain.TcScheduleContext;
import com.zlt.aps.tc.engine.domain.TcSnapshotBuildResult;
import com.zlt.aps.tc.engine.domain.TcTaskDraft;
import com.zlt.aps.tc.engine.service.ITcSnapshotAndPersistService;
import org.springframework.stereotype.Service;

/**
 * 胎侧解释快照和落库默认步骤服务。
 *
 * <p>负责为上下文中的任务生成解释快照，并调用落库转换服务形成汇总结果。
 * 当前不直接接 Mapper，不开启事务。</p>
 */
@Service
public class TcSnapshotAndPersistService implements ITcSnapshotAndPersistService {

    private final TcSnapshotBuildService snapshotBuildService;

    private final TcPersistService persistService;

    /**
     * 创建解释快照和落库默认步骤服务。
     *
     * @param snapshotBuildService 解释快照构建服务
     * @param persistService       落库转换服务
     */
    public TcSnapshotAndPersistService(TcSnapshotBuildService snapshotBuildService, TcPersistService persistService) {
        this.snapshotBuildService = snapshotBuildService;
        this.persistService = persistService;
    }

    @Override
    public void snapshotAndPersist(TcScheduleContext context) {
        if (context == null) {
            throw new ServiceException(TcScheduleErrorCodeEnum.TC_CONTEXT_EMPTY.getDefaultMessage());
        }
        if (CollUtil.isNotEmpty(context.getTaskDraftList())) {
            for (TcTaskDraft task : context.getTaskDraftList()) {
                TcSnapshotBuildResult snapshot = snapshotBuildService.buildTaskExplain(task, context);
                context.getSnapshotMap().put(task.getBusinessKey(), snapshot);
            }
        }
        TcPersistResult persistResult = persistService.persist(context);
        context.setPersistResult(persistResult);
    }
}
