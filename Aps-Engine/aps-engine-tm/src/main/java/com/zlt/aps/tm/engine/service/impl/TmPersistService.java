package com.zlt.aps.tm.engine.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.ruoyi.common.exception.ServiceException;
import com.zlt.aps.common.engine.schedule.ScheduleTaskLinkedList;
import com.zlt.aps.common.engine.schedule.ScheduleTaskNode;
import com.zlt.aps.tm.api.domain.entity.TmScheduleResult;
import com.zlt.aps.tm.api.domain.entity.TmScheduleResultExplain;
import com.zlt.aps.tm.api.enums.*;
import com.zlt.aps.tm.engine.domain.TmPersistResult;
import com.zlt.aps.tm.engine.domain.TmScheduleContext;
import com.zlt.aps.tm.engine.domain.TmSnapshotBuildResult;
import com.zlt.aps.tm.engine.domain.TmTaskDraft;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
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
            explain.setTaskBusinessKey(task.getBusinessKey());
            explain.setTaskOrderNo(task.getOrderNo());
            explain.setSourceOrderNos(task.getSourceOrderNos());
            explain.setShiftOrder(task.getShiftOrder());
            explain.setBaseDemandQty(task.getBaseDemandQty() == null ? task.getDemandQty() : task.getBaseDemandQty());
            explain.setLossAddQty(task.getLossAddQty());
            explain.setStockDeductQty(task.getStockDeductQty());
            explain.setToolLimitAdjustQty(task.getToolLimitAdjustQty());
            explain.setMinStartAdjustQty(task.getMinStartAdjustQty());
            explain.setTailRoundAdjustQty(task.getTailRoundAdjustQty());
            explain.setCapacityAdjustQty(task.getCapacityAdjustQty());
            explain.setRequiredQty(task.getDemandQty());
            explain.setFinalPlanQty(task.getPlanQty());
            explain.setCalcFormulaDesc(task.getCalcFormulaDesc());
            explain.setStockQty(task.getSixClockStockQty());
            explain.setPlanStockQty(task.getPlanStockQty());
            explain.setSupplyHours(task.getSupplyHours());
            explain.setCoverageShiftCount(task.getGuardShiftCount());
            explain.setUnplannedReasonCode(task.getUnplannedReasonCode());
            explain.setUnplannedReasonDesc(task.getUnplannedReasonDesc());
            explain.setTaskStatus(task.isUnassigned() ? null : TmScheduleTaskStatusEnum.PLANNED.getCode());
            explain.setResultStatus(TmScheduleReleaseStatusEnum.NOT_RELEASED.getCode());
        }
        if (snapshot != null) {
            explain.setRuleHitJson(snapshot.getRuleHitJson());
            explain.setCandidateMachineJson(snapshot.getCandidateMachineJson());
            explain.setSelectedMachineScore(snapshot.getSelectedMachineScore());
            explain.setMachineSelectReason(snapshot.getMachineSelectReason());
            explain.setAssignStatus(snapshot.getAssignStatus());
            explain.setUnplannedEvidenceJson(snapshot.getUnplannedEvidenceJson());
            explain.setSysAnalysis(snapshot.getSysAnalysis());
        }
        explain.setGenerateMode(TmGenerateModeEnum.ENGINE_SKELETON.getCode());
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
            throw new ServiceException(TmScheduleErrorCodeEnum.TM_TASK_NOT_FOUND.getDefaultMessage());
        }
        if (context == null) {
            throw new ServiceException(TmScheduleErrorCodeEnum.TM_CONTEXT_EMPTY.getDefaultMessage());
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
        result.setMachineCode(node.getMachineCode());
        result.setTreadCode(task.getTreadCode());
        result.setGlueCode(task.getGlueCode());
        result.setMouthPlateCode(task.getMouthPlateCode());
        result.setReleaseStatus(TmScheduleReleaseStatusEnum.NOT_RELEASED.getCode());
        result.setDataSource("AUTO");
        applyShiftFields(result, node);
        return result;
    }

    /**
     * 按任务班次写入未排任务的班次字段。
     *
     * @param result 排程结果
     * @param task   未排任务
     */
    private void applyTaskShiftFields(TmScheduleResult result, TmTaskDraft task) {
        Integer shiftOrder = task.getShiftOrder() == null ? 1 : task.getShiftOrder();
        applyShiftFields(result, shiftOrder, 1, task.getPlanQty(), null, null);
    }

    /**
     * 按任务链节点班次写入排程结果字段。
     *
     * @param result 排程结果
     * @param node   任务链节点
     */
    private void applyShiftFields(TmScheduleResult result, ScheduleTaskNode<TmTaskDraft> node) {
        applyShiftFields(result, node.getShiftOrder(), node.getSequence(), node.getPlanQty(), node.getStartTime(), node.getEndTime());
    }

    /**
     * 按班次顺序统一写入排程结果的横向班次字段。
     *
     * @param result     排程结果
     * @param shiftOrder 班次顺序，支持 1-6
     * @param sequence   班内顺序
     * @param planQty    计划量
     * @param startTime  开始时间，可为空
     * @param endTime    结束时间，可为空
     */
    private void applyShiftFields(TmScheduleResult result, Integer shiftOrder, Integer sequence, BigDecimal planQty,
                                  Date startTime, Date endTime) {
        if (Integer.valueOf(1).equals(shiftOrder)) {
            result.setClass1Sequence(sequence);
            result.setClass1PlanQty(planQty);
            result.setClass1StartTime(startTime);
            result.setClass1EndTime(endTime);
            return;
        }
        if (Integer.valueOf(2).equals(shiftOrder)) {
            result.setClass2Sequence(sequence);
            result.setClass2PlanQty(planQty);
            result.setClass2StartTime(startTime);
            result.setClass2EndTime(endTime);
            return;
        }
        if (Integer.valueOf(3).equals(shiftOrder)) {
            result.setClass3Sequence(sequence);
            result.setClass3PlanQty(planQty);
            result.setClass3StartTime(startTime);
            result.setClass3EndTime(endTime);
            return;
        }
        if (Integer.valueOf(4).equals(shiftOrder)) {
            result.setClass4Sequence(sequence);
            result.setClass4PlanQty(planQty);
            result.setClass4StartTime(startTime);
            result.setClass4EndTime(endTime);
            return;
        }
        if (Integer.valueOf(5).equals(shiftOrder)) {
            result.setClass5Sequence(sequence);
            result.setClass5PlanQty(planQty);
            result.setClass5StartTime(startTime);
            result.setClass5EndTime(endTime);
            return;
        }
        if (Integer.valueOf(6).equals(shiftOrder)) {
            result.setClass6Sequence(sequence);
            result.setClass6PlanQty(planQty);
            result.setClass6StartTime(startTime);
            result.setClass6EndTime(endTime);
            return;
        }
        throw new ServiceException(TmScheduleErrorCodeEnum.TM_SHIFT_INVALID.getDefaultMessage() + ":" + shiftOrder);
    }
}
