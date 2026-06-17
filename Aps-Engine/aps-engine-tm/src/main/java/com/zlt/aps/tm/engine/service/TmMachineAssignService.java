package com.zlt.aps.tm.engine.service;

import cn.hutool.core.collection.CollUtil;
import com.zlt.aps.tm.api.enums.TmUnplannedReasonEnum;
import com.zlt.aps.tm.engine.domain.TmMachineCandidate;
import com.zlt.aps.tm.engine.domain.TmScheduleContext;
import com.zlt.aps.tm.engine.domain.TmTaskDraft;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 胎面机台分配默认步骤服务。
 *
 * <p>当前只处理已经预置机台编码的任务，将其追加到对应机台一班任务链；
 * 未预置机台的任务标记为无可用机台，不执行未确认的过滤和评分算法。</p>
 */
@Service
public class TmMachineAssignService implements ITmMachineAssignService {

    private final TmTaskChainScheduleService taskChainScheduleService;

    /**
     * 创建默认机台分配步骤服务。
     *
     * @param taskChainScheduleService 任务链排程服务
     */
    public TmMachineAssignService(TmTaskChainScheduleService taskChainScheduleService) {
        this.taskChainScheduleService = taskChainScheduleService;
    }

    @Override
    public void assign(TmScheduleContext context) {
        if (context == null) {
            throw new IllegalArgumentException("胎面排程上下文不能为空");
        }
        if (CollUtil.isEmpty(context.getTaskDraftList())) {
            return;
        }
        for (TmTaskDraft task : context.getTaskDraftList()) {
            if (task.isUnassigned()) {
                task.setUnplannedReasonCode(TmUnplannedReasonEnum.NO_AVAILABLE_MACHINE.getCode());
                task.setUnplannedReasonDesc(TmUnplannedReasonEnum.NO_AVAILABLE_MACHINE.getDesc());
                continue;
            }
            TmMachineCandidate candidate = new TmMachineCandidate();
            candidate.setMachineCode(task.getMachineCode());
            taskChainScheduleService.appendAutoTask(task, candidate, context);
        }
    }

    /**
     * 使用调用方准备好的任务列表执行机台分配。
     *
     * <p>该方法用于场景测试或上层已完成数据加载的入口，先把任务列表放入上下文，
     * 再复用默认分配逻辑建立任务链。方法会修改上下文中的任务列表和任务链。</p>
     *
     * @param context  胎面排程上下文
     * @param taskList 已准备好的任务草稿列表
     * @throws IllegalArgumentException 上下文为空时抛出
     */
    public void assignPrepared(TmScheduleContext context, List<TmTaskDraft> taskList) {
        if (context == null) {
            throw new IllegalArgumentException("胎面排程上下文不能为空");
        }
        context.setTaskDraftList(taskList);
        assign(context);
    }
}
