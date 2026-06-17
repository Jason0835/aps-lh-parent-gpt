package com.zlt.aps.tm.engine.service;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.zlt.aps.common.engine.schedule.ScheduleTaskLinkedList;
import com.zlt.aps.common.engine.schedule.ScheduleTaskNode;
import com.zlt.aps.tm.api.domain.entity.TmScheduleResult;
import com.zlt.aps.tm.api.domain.entity.TmScheduleResultExplain;
import com.zlt.aps.tm.api.enums.TmScheduleReleaseStatusEnum;
import com.zlt.aps.tm.api.enums.TmScheduleStepEnum;
import com.zlt.aps.tm.api.enums.TmScheduleTaskStatusEnum;
import com.zlt.aps.tm.engine.domain.TmPersistResult;
import com.zlt.aps.tm.engine.domain.TmScheduleContext;
import com.zlt.aps.tm.engine.domain.TmSnapshotBuildResult;
import com.zlt.aps.tm.engine.domain.TmTaskDraft;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 胎面排程落库转换服务。
 *
 * <p>当前骨架只负责把运行态任务链转换为结果实体和解释实体，不直接持有 Mapper，不控制事务。
 * 实际批量写入由 `APS-Modules/aps-tm` 业务入口接入后完成。</p>
 */
@Service
public class TmPersistService {

    /**
     * 统计上下文中的待排任务数量，返回落库摘要。
     *
     * @param context 胎面排程上下文
     * @return 落库摘要
     */
    public TmPersistResult persist(TmScheduleContext context) {
        TmPersistResult result = new TmPersistResult();
        if (context == null || CollUtil.isEmpty(context.getTaskDraftList())) {
            return result;
        }
        result.setResultCount(context.getTaskDraftList().size());
        result.setExplainCount(result.getResultCount());
        int unplannedCount = 0;
        for (TmTaskDraft task : context.getTaskDraftList()) {
            if (task.isUnassigned() || StrUtil.isNotBlank(task.getUnplannedReasonCode())) {
                unplannedCount++;
            }
        }
        result.setUnplannedCount(unplannedCount);
        return result;
    }

    /**
     * 将任务链转换为排程结果实体列表。
     *
     * @param chain   任务链
     * @param context 胎面排程上下文
     * @return 排程结果实体列表
     */
    public List<TmScheduleResult> convertChainToResult(ScheduleTaskLinkedList<TmTaskDraft> chain,
                                                       TmScheduleContext context) {
        List<TmScheduleResult> resultList = new ArrayList<>();
        if (chain == null) {
            return resultList;
        }
        for (ScheduleTaskNode<TmTaskDraft> node : chain.toList()) {
            resultList.add(convertNodeToResult(node, context));
        }
        return resultList;
    }

    /**
     * 转换解释实体。
     *
     * @param task     待排任务草稿
     * @param snapshot 解释快照
     * @return 排程结果解释实体
     */
    public TmScheduleResultExplain convertExplain(TmTaskDraft task, TmSnapshotBuildResult snapshot) {
        return convertExplain(task, snapshot, null);
    }

    /**
     * 转换解释实体。
     *
     * @param task     待排任务草稿
     * @param snapshot 解释快照
     * @param context  胎面排程上下文，用于补充批次和追踪标识
     * @return 排程结果解释实体
     */
    public TmScheduleResultExplain convertExplain(TmTaskDraft task, TmSnapshotBuildResult snapshot,
                                                  TmScheduleContext context) {
        TmScheduleResultExplain explain = new TmScheduleResultExplain();
        if (context != null) {
            explain.setFactoryCode(context.getFactoryCode());
            explain.setBatchNo(context.getBatchNo());
            explain.setTraceId(context.getTraceId());
        }
        if (task != null) {
            explain.setBaseDemandQty(task.getDemandQty());
            explain.setRequiredQty(task.getDemandQty());
            explain.setFinalPlanQty(task.getPlanQty());
            explain.setUnplannedReasonCode(task.getUnplannedReasonCode());
            explain.setUnplannedReasonDesc(task.getUnplannedReasonDesc());
            explain.setTaskStatus(task.isUnassigned() ? null : TmScheduleTaskStatusEnum.PLANNED.getCode());
            explain.setResultStatus(task.isUnassigned()
                    ? TmScheduleReleaseStatusEnum.NOT_RELEASED.getCode()
                    : TmScheduleReleaseStatusEnum.WAIT_RELEASE.getCode());
        }
        if (snapshot != null) {
            explain.setRuleHitJson(snapshot.getRuleHitJson());
            explain.setCandidateMachineJson(snapshot.getCandidateMachineJson());
            explain.setUnplannedEvidenceJson(snapshot.getUnplannedEvidenceJson());
            explain.setSysAnalysis(snapshot.getSysAnalysis());
        }
        explain.setGenerateMode("ENGINE_SKELETON");
        explain.setCurrentStepCode(TmScheduleStepEnum.PERSIST.getCode());
        return explain;
    }

    /**
     * 写入未排任务入口。
     *
     * <p>当前骨架版本只做参数校验和实体转换，实际批量写入由 aps-tm 业务入口接入后完成。</p>
     *
     * @param task     未排任务
     * @param snapshot 解释快照
     * @param context  胎面排程上下文
     */
    public void persistUnplanned(TmTaskDraft task, TmSnapshotBuildResult snapshot, TmScheduleContext context) {
        if (task == null) {
            throw new IllegalArgumentException("未排任务不能为空");
        }
        if (context == null) {
            throw new IllegalArgumentException("胎面排程上下文不能为空");
        }
        convertUnplanned(task, context);
        convertExplain(task, snapshot, context);
    }

    /**
     * 将未排任务转换为排程结果实体。
     *
     * <p>未排任务仍写入 T_TM_SCHEDULE_RESULT，机台编码为空，各班次字段根据任务所属班次写入。
     * 当前骨架版本只支持单班次写入（shiftOrder=1 对应 class1），后续如需多班支持可扩展。</p>
     *
     * @param task    未排任务草稿
     * @param context 胎面排程上下文
     * @return 排程结果实体，机台编码为空
     */
    public TmScheduleResult convertUnplanned(TmTaskDraft task, TmScheduleContext context) {
        TmScheduleResult result = new TmScheduleResult();
        result.setFactoryCode(context == null ? null : context.getFactoryCode());
        result.setBatchNo(context == null ? null : context.getBatchNo());
        result.setScheduleDate(context == null ? null : context.getScheduleDate());
        if (task != null) {
            result.setOrderNo(task.getOrderNo());
            result.setTreadCode(task.getTreadCode());
            result.setGlueCode(task.getGlueCode());
            result.setMouthPlateCode(task.getMouthPlateCode());
            result.setMachineCode(null);
            applyTaskShiftFields(result, task);
        }
        result.setReleaseStatus(TmScheduleReleaseStatusEnum.NOT_RELEASED.getCode());
        result.setDataSource("AUTO");
        return result;
    }

    private TmScheduleResult convertNodeToResult(ScheduleTaskNode<TmTaskDraft> node, TmScheduleContext context) {
        TmTaskDraft task = node.getTask();
        TmScheduleResult result = new TmScheduleResult();
        result.setFactoryCode(context == null ? null : context.getFactoryCode());
        result.setBatchNo(context == null ? null : context.getBatchNo());
        result.setScheduleDate(context == null ? null : context.getScheduleDate());
        result.setOrderNo(task.getOrderNo());
        result.setMachineCode(node.getMachineCode());
        result.setTreadCode(task.getTreadCode());
        result.setGlueCode(task.getGlueCode());
        result.setMouthPlateCode(task.getMouthPlateCode());
        result.setReleaseStatus(TmScheduleReleaseStatusEnum.WAIT_RELEASE.getCode());
        result.setDataSource("AUTO");
        applyShiftFields(result, node);
        return result;
    }

    private void applyTaskShiftFields(TmScheduleResult result, TmTaskDraft task) {
        Integer shiftOrder = task.getShiftOrder() == null ? 1 : task.getShiftOrder();
        if (Integer.valueOf(1).equals(shiftOrder)) {
            result.setClass1Sequence(1);
            result.setClass1PlanQty(task.getPlanQty());
            return;
        }
        if (Integer.valueOf(2).equals(shiftOrder)) {
            result.setClass2Sequence(1);
            result.setClass2PlanQty(task.getPlanQty());
            return;
        }
        if (Integer.valueOf(3).equals(shiftOrder)) {
            result.setClass3Sequence(1);
            result.setClass3PlanQty(task.getPlanQty());
            return;
        }
        if (Integer.valueOf(4).equals(shiftOrder)) {
            result.setClass4Sequence(1);
            result.setClass4PlanQty(task.getPlanQty());
            return;
        }
        if (Integer.valueOf(5).equals(shiftOrder)) {
            result.setClass5Sequence(1);
            result.setClass5PlanQty(task.getPlanQty());
            return;
        }
        if (Integer.valueOf(6).equals(shiftOrder)) {
            result.setClass6Sequence(1);
            result.setClass6PlanQty(task.getPlanQty());
            return;
        }
        throw new IllegalArgumentException("不支持的胎面排程班次顺序:" + shiftOrder);
    }

    private void applyShiftFields(TmScheduleResult result, ScheduleTaskNode<TmTaskDraft> node) {
        Integer shiftOrder = node.getShiftOrder();
        if (Integer.valueOf(1).equals(shiftOrder)) {
            result.setClass1Sequence(node.getSequence());
            result.setClass1PlanQty(node.getPlanQty());
            result.setClass1StartTime(node.getStartTime());
            result.setClass1EndTime(node.getEndTime());
            return;
        }
        if (Integer.valueOf(2).equals(shiftOrder)) {
            result.setClass2Sequence(node.getSequence());
            result.setClass2PlanQty(node.getPlanQty());
            result.setClass2StartTime(node.getStartTime());
            result.setClass2EndTime(node.getEndTime());
            return;
        }
        if (Integer.valueOf(3).equals(shiftOrder)) {
            result.setClass3Sequence(node.getSequence());
            result.setClass3PlanQty(node.getPlanQty());
            result.setClass3StartTime(node.getStartTime());
            result.setClass3EndTime(node.getEndTime());
            return;
        }
        if (Integer.valueOf(4).equals(shiftOrder)) {
            result.setClass4Sequence(node.getSequence());
            result.setClass4PlanQty(node.getPlanQty());
            result.setClass4StartTime(node.getStartTime());
            result.setClass4EndTime(node.getEndTime());
            return;
        }
        if (Integer.valueOf(5).equals(shiftOrder)) {
            result.setClass5Sequence(node.getSequence());
            result.setClass5PlanQty(node.getPlanQty());
            result.setClass5StartTime(node.getStartTime());
            result.setClass5EndTime(node.getEndTime());
            return;
        }
        if (Integer.valueOf(6).equals(shiftOrder)) {
            result.setClass6Sequence(node.getSequence());
            result.setClass6PlanQty(node.getPlanQty());
            result.setClass6StartTime(node.getStartTime());
            result.setClass6EndTime(node.getEndTime());
            return;
        }
        throw new IllegalArgumentException("不支持的胎面排程班次顺序:" + shiftOrder);
    }
}
