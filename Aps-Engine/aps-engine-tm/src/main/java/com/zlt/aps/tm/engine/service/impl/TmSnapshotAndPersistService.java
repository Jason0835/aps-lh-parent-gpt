package com.zlt.aps.tm.engine.service.impl;

import cn.hutool.core.collection.CollUtil;
import com.ruoyi.common.exception.ServiceException;
import com.zlt.aps.tm.api.enums.TmScheduleErrorCodeEnum;
import com.zlt.aps.tm.engine.domain.TmPersistResult;
import com.zlt.aps.tm.engine.domain.TmScheduleContext;
import com.zlt.aps.tm.engine.domain.TmSnapshotBuildResult;
import com.zlt.aps.tm.engine.domain.TmTaskDraft;
import com.zlt.aps.tm.engine.service.ITmSnapshotAndPersistService;
import org.springframework.stereotype.Service;

/**
 * 胎面解释快照和落库默认步骤服务。
 *
 * <p>负责为上下文中的任务生成解释快照，并调用落库转换服务形成汇总结果。
 * 当前不直接接 Mapper，不开启事务。</p>
 */
@Service
public class TmSnapshotAndPersistService implements ITmSnapshotAndPersistService {

    private final TmSnapshotBuildService snapshotBuildService;

    private final TmPersistService persistService;

    /**
     * 创建解释快照和落库默认步骤服务。
     *
     * @param snapshotBuildService 解释快照构建服务
     * @param persistService       落库转换服务
     */
    public TmSnapshotAndPersistService(TmSnapshotBuildService snapshotBuildService, TmPersistService persistService) {
        this.snapshotBuildService = snapshotBuildService;
        this.persistService = persistService;
    }

    @Override
    public void snapshotAndPersist(TmScheduleContext context) {
        if (context == null) {
            throw new ServiceException(TmScheduleErrorCodeEnum.TM_CONTEXT_EMPTY.getDefaultMessage());
        }
        if (CollUtil.isNotEmpty(context.getTaskDraftList())) {
            for (TmTaskDraft task : context.getTaskDraftList()) {
                TmSnapshotBuildResult snapshot = snapshotBuildService.buildTaskExplain(task, context);
                context.getSnapshotMap().put(task.getBusinessKey(), snapshot);
            }
        }
        TmPersistResult persistResult = persistService.persist(context);
        context.setPersistResult(persistResult);
    }
}
