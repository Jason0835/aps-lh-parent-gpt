package com.zlt.aps.tm.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.zlt.aps.tm.api.domain.entity.TmScheduleResult;
import com.zlt.aps.tm.api.domain.entity.TmScheduleResultExplain;
import com.zlt.aps.tm.engine.domain.TmPersistResult;
import com.zlt.aps.tm.engine.domain.TmScheduleContext;
import com.zlt.aps.tm.engine.domain.TmSnapshotBuildResult;
import com.zlt.aps.tm.engine.domain.TmTaskDraft;
import com.zlt.aps.tm.engine.service.ITmSnapshotAndPersistService;
import com.zlt.aps.tm.engine.service.TmPersistService;
import com.zlt.aps.tm.engine.service.TmSnapshotBuildService;
import com.zlt.aps.tm.mapper.TmScheduleResultExplainMapper;
import com.zlt.aps.tm.mapper.TmScheduleResultMapper;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 胎面自动排程业务快照和落库步骤服务。
 *
 * <p>负责在模板快照阶段生成解释快照，将任务链转换为排程结果并写入排程结果与解释表。</p>
 */
@Primary
@Service
public class TmBizSnapshotAndPersistService implements ITmSnapshotAndPersistService {

    private final TmSnapshotBuildService snapshotBuildService;

    private final TmPersistService persistService;

    private final TmScheduleResultMapper scheduleResultMapper;

    private final TmScheduleResultExplainMapper scheduleResultExplainMapper;

    /**
     * 创建胎面自动排程业务快照和落库步骤服务。
     *
     * @param snapshotBuildService       解释快照构建服务
     * @param persistService             落库实体转换服务
     * @param scheduleResultMapper       胎面排程结果 Mapper
     * @param scheduleResultExplainMapper 胎面排程解释 Mapper
     */
    public TmBizSnapshotAndPersistService(TmSnapshotBuildService snapshotBuildService,
                                          TmPersistService persistService,
                                          TmScheduleResultMapper scheduleResultMapper,
                                          TmScheduleResultExplainMapper scheduleResultExplainMapper) {
        this.snapshotBuildService = snapshotBuildService;
        this.persistService = persistService;
        this.scheduleResultMapper = scheduleResultMapper;
        this.scheduleResultExplainMapper = scheduleResultExplainMapper;
    }

    /**
     * 执行解释快照构建和实际落库。
     *
     * @param context 胎面排程上下文，需包含已完成机台分配的任务链
     */
    @Override
    public void snapshotAndPersist(TmScheduleContext context) {
        if (context == null) {
            throw new IllegalArgumentException("胎面排程上下文不能为空");
        }
        buildSnapshot(context);
        context.setPersistResult(persistScheduleContext(context));
    }

    /**
     * 构建所有待排任务的解释快照。
     *
     * @param context 胎面排程上下文
     */
    private void buildSnapshot(TmScheduleContext context) {
        if (CollUtil.isEmpty(context.getTaskDraftList())) {
            return;
        }
        for (TmTaskDraft task : context.getTaskDraftList()) {
            TmSnapshotBuildResult snapshot = snapshotBuildService.buildTaskExplain(task, context);
            context.getSnapshotMap().put(task.getBusinessKey(), snapshot);
        }
    }

    /**
     * 将自动排程上下文中的任务结果和解释写入数据库。
     *
     * @param context 自动排程上下文
     * @return 落库汇总
     */
    private TmPersistResult persistScheduleContext(TmScheduleContext context) {
        TmPersistResult persistResult = new TmPersistResult();
        if (CollUtil.isEmpty(context.getTaskDraftList())) {
            return persistResult;
        }
        List<TmScheduleResult> resultList = new ArrayList<>();
        for (TmTaskDraft taskDraft : context.getTaskDraftList()) {
            if (taskDraft.isUnassigned() || StrUtil.isNotBlank(taskDraft.getUnplannedReasonCode())) {
                resultList.add(persistService.convertUnplanned(taskDraft, context));
            }
        }
        context.getTaskChainGroup().values().forEach(chain -> resultList.addAll(persistService.convertChainToResult(chain, context)));
        if (CollUtil.isEmpty(resultList)) {
            return persistResult;
        }
        int resultCount = 0;
        int explainCount = 0;
        int unplannedCount = 0;
        for (TmScheduleResult result : resultList) {
            scheduleResultMapper.insert(result);
            resultCount++;
            if (StrUtil.isBlank(result.getMachineCode())) {
                unplannedCount++;
            }
        }
        for (TmTaskDraft taskDraft : context.getTaskDraftList()) {
            TmSnapshotBuildResult snapshot = context.getSnapshotMap().get(taskDraft.getBusinessKey());
            TmScheduleResultExplain explain = persistService.convertExplain(taskDraft, snapshot, context);
            scheduleResultExplainMapper.insert(explain);
            explainCount++;
        }
        persistResult.setResultCount(resultCount);
        persistResult.setExplainCount(explainCount);
        persistResult.setUnplannedCount(unplannedCount);
        return persistResult;
    }
}
