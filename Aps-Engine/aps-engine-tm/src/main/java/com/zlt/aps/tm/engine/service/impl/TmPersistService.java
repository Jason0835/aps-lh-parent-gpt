package com.zlt.aps.tm.engine.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.ruoyi.common.exception.ServiceException;
import com.zlt.aps.common.engine.schedule.ScheduleTaskLinkedList;
import com.zlt.aps.common.engine.schedule.ScheduleTaskNode;
import com.zlt.aps.tm.api.constant.TmScheduleConstants;
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
            explain.setPlanGroupKey(task.getPlanGroupKey());
            explain.setGroupSourceCount(task.getGroupSourceCount());
            explain.setSourceRequiredQty(task.getSourceRequiredQty());
            explain.setGroupRequiredQty(task.getGroupRequiredQty());
            explain.setGroupBaseDemandQty(task.getGroupBaseDemandQty());
            explain.setGroupMinStartAdjustQty(task.getGroupMinStartAdjustQty());
            explain.setGroupRoundAdjustQty(task.getGroupRoundAdjustQty());
            explain.setGroupFinalPlanQty(task.getGroupFinalPlanQty());
            explain.setBaseDemandQty(task.getBaseDemandQty() == null ? task.getDemandQty() : task.getBaseDemandQty());
            explain.setLossAddQty(task.getLossAddQty());
            explain.setStockDeductQty(task.getStockDeductQty());
            explain.setToolLimitAdjustQty(task.getToolLimitAdjustQty());
            explain.setMinStartAdjustQty(task.getMinStartAdjustQty());
            explain.setTailRoundAdjustQty(task.getTailRoundAdjustQty());
            explain.setCapacityAdjustQty(task.getCapacityAdjustQty());
            explain.setRequiredQty(resolveRequiredQty(task));
            explain.setFinalPlanQty(task.getPlanQty());
            explain.setCalcFormulaDesc(task.getCalcFormulaDesc());
            explain.setStockQty(task.getSixClockStockQty());
            explain.setPlanStockQty(task.getPlanStockQty());
            explain.setSupplyHours(task.getSupplyHours());
            explain.setCoverageShiftCount(task.getGuardShiftCount());
            explain.setUnplannedReasonCode(task.getUnplannedReasonCode());
            explain.setUnplannedReasonDesc(task.getUnplannedReasonDesc());
            explain.setTaskStatus(resolveTaskStatus(task));
            explain.setTreadCode(task.getTreadCode());
            explain.setGlueCode(task.getGlueCode());
            explain.setBaseGlueCode(task.getBaseGlueCode());
            explain.setMouthPlateCode(task.getMouthPlateCode());
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
        explain.setGenerateMode(TmGenerateModeEnum.ENGINE_FULL.getCode());
        explain.setCurrentStepCode(TmScheduleStepEnum.PERSIST.getCode());
        return explain;
    }

    /**
     * 解析解释表任务状态。
     *
     * @param task 待排任务草稿
     * @return 任务状态；未排任务返回 null，由未排原因字段表达
     */
    private String resolveTaskStatus(TmTaskDraft task) {
        if (task == null || isUnplannedTask(task)) {
            return null;
        }
        if (isNoProductionNeeded(task)) {
            return TmScheduleTaskStatusEnum.NO_PRODUCTION_NEEDED.getCode();
        }
        return TmScheduleTaskStatusEnum.PLANNED.getCode();
    }

    /**
     * 判断任务是否属于未排任务。
     *
     * @param task 待排任务草稿
     * @return true 表示任务需要进入未排语义
     */
    private boolean isUnplannedTask(TmTaskDraft task) {
        if (task == null) {
            return false;
        }
        if (StrUtil.isNotBlank(task.getUnplannedReasonCode())) {
            return true;
        }
        return task.isUnassigned() && task.getPlanQty() != null
                && task.getPlanQty().compareTo(BigDecimal.ZERO) > 0;
    }

    /**
     * 判断任务是否无需生产。
     *
     * @param task 待排任务草稿
     * @return true 表示最终计划量为空或小于等于 0，且不是未排任务
     */
    private boolean isNoProductionNeeded(TmTaskDraft task) {
        return task != null && !isUnplannedTask(task)
                && (task.getPlanQty() == null || task.getPlanQty().compareTo(BigDecimal.ZERO) <= 0);
    }

    /**
     * 解析解释表应排需求量。
     *
     * @param task 待排任务草稿
     * @return 库存抵扣前的当前班成型胎面需求量；旧骨架缺失当前班需求时回退需求量
     */
    private BigDecimal resolveRequiredQty(TmTaskDraft task) {
        if (task == null) {
            return null;
        }
        return task.getCurrentShiftDemandQty() == null ? task.getDemandQty() : task.getCurrentShiftDemandQty();
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
        result.setBaseGlueCode(task.getBaseGlueCode());
        result.setMouthPlateCode(task.getMouthPlateCode());
        result.setTreadShoulderLength(task.getTreadShoulderLength());
        result.setCxRemainQty(task.getTailBalanceQty());
        result.setMaterialCode(task.getMaterialCode());
        result.setMaterialDesc(task.getMaterialDesc());
        result.setEmbryoCode(task.getEmbryoCode());
        result.setMainMaterialDesc(task.getMainMaterialDesc());
        result.setCxMachineCode(task.getCxMachineCode());
        result.setSixClockStockQty(task.getSixClockStockQty());
        result.setCurlRollLength(this.resolveEffectiveCurlRollLength(task));
        result.setReleaseStatus(TmScheduleReleaseStatusEnum.NOT_RELEASED.getCode());
        result.setDataSource(TmScheduleConstants.AUTO_SCHEDULE_DATA_SOURCE);
        applyShiftFields(result, node);
        return result;
    }

    /**
     * 解析任务最终使用的卷曲长度。
     *
     * @param task 待排任务草稿
     * @return 优先返回胎面卷曲配置；配置缺失或无效时返回默认卷曲长度
     */
    private BigDecimal resolveEffectiveCurlRollLength(TmTaskDraft task) {
        if (task.getCurlRollLength() != null && task.getCurlRollLength().compareTo(BigDecimal.ZERO) > 0) {
            return task.getCurlRollLength();
        }
        return task.getDefaultCurlRollLength();
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
        if (shiftOrder == null || shiftOrder < 1 || shiftOrder > TmScheduleConstants.TM_MAX_SHIFT_ORDER) {
            throw new ServiceException(TmScheduleErrorCodeEnum.TM_SHIFT_INVALID.getDefaultMessage() + ":" + shiftOrder);
        }
        result.setFieldValueByFieldName(String.format(TmScheduleConstants.SHIFT_SEQUENCE_FIELD_TEMPLATE, shiftOrder), sequence);
        result.setFieldValueByFieldName(String.format(TmScheduleConstants.SHIFT_PLAN_QTY_FIELD_TEMPLATE, shiftOrder), planQty);
        result.setFieldValueByFieldName(String.format(TmScheduleConstants.SHIFT_START_TIME_FIELD_TEMPLATE, shiftOrder), startTime);
        result.setFieldValueByFieldName(String.format(TmScheduleConstants.SHIFT_END_TIME_FIELD_TEMPLATE, shiftOrder), endTime);
    }
}
