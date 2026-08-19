package com.zlt.aps.tc.engine.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.ruoyi.common.exception.ServiceException;
import com.zlt.aps.common.engine.schedule.ScheduleTaskLinkedList;
import com.zlt.aps.common.engine.schedule.ScheduleTaskNode;
import com.zlt.aps.tc.api.constant.TcScheduleConstants;
import com.zlt.aps.tc.api.domain.entity.TcScheduleResult;
import com.zlt.aps.tc.api.domain.entity.TcScheduleResultExplain;
import com.zlt.aps.tc.api.enums.TcScheduleErrorCodeEnum;
import com.zlt.aps.tc.api.enums.TcScheduleReleaseStatusEnum;
import com.zlt.aps.tc.api.enums.TcScheduleTaskStatusEnum;
import com.zlt.aps.tc.api.enums.TcYesNoEnum;
import com.zlt.aps.tc.engine.domain.TcPersistResult;
import com.zlt.aps.tc.engine.domain.TcScheduleContext;
import com.zlt.aps.tc.engine.domain.TcSnapshotBuildResult;
import com.zlt.aps.tc.engine.domain.TcTaskDraft;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * 胎侧排程落库转换服务。
 *
 * <p>负责把运行态任务链转换为结果实体和解释实体，不直接持有 Mapper，不控制事务。
 * 实际批量写入和事务控制由胎侧模块的 {@link com.zlt.aps.tc.service.impl.TcBizSnapshotAndPersistService}
 * 统一完成。</p>
 */
@Service
public class TcPersistService {

    /**
     * 统计上下文中的待排任务数量，返回落库摘要。
     *
     * @param context 胎侧排程上下文
     * @return 落库摘要
     */
    public TcPersistResult persist(TcScheduleContext context) {
        TcPersistResult result = new TcPersistResult();
        if (context == null || CollUtil.isEmpty(context.getTaskDraftList())) {
            return result;
        }
        long resultCount = context.getTaskDraftList().stream()
                .filter(task -> task != null && !this.isUnplannedTask(task) && !this.isNoProductionNeeded(task))
                .count();
        long unplannedCount = context.getTaskDraftList().stream()
                .filter(this::isUnplannedTask)
                .count();
        result.setResultCount((int) resultCount);
        result.setExplainCount(context.getTaskDraftList().size());
        result.setUnplannedCount((int) unplannedCount);
        return result;
    }

    /**
     * 将任务链转换为排程结果实体列表。
     *
     * @param chain   任务链
     * @param context 胎侧排程上下文
     * @return 排程结果实体列表
     */
    public List<TcScheduleResult> convertChainToResult(ScheduleTaskLinkedList<TcTaskDraft> chain,
                                                       TcScheduleContext context) {
        List<TcScheduleResult> resultList = new ArrayList<>();
        if (chain == null) {
            return resultList;
        }
        for (ScheduleTaskNode<TcTaskDraft> node : chain.toList()) {
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
    public TcScheduleResultExplain convertExplain(TcTaskDraft task, TcSnapshotBuildResult snapshot) {
        return convertExplain(task, snapshot, null);
    }

    /**
     * 转换解释实体。
     *
     * @param task     待排任务草稿
     * @param snapshot 解释快照
     * @param context  胎侧排程上下文，用于补充批次和追踪标识
     * @return 排程结果解释实体
     */
    public TcScheduleResultExplain convertExplain(TcTaskDraft task, TcSnapshotBuildResult snapshot,
                                                  TcScheduleContext context) {
        TcScheduleResultExplain explain = new TcScheduleResultExplain();
        if (context != null) {
            explain.setFactoryCode(context.getFactoryCode());
            explain.setBatchNo(context.getBatchNo());
            explain.setTraceId(context.getTraceId());
            explain.setScheduleDate(context.getScheduleDate());
        }
        if (task != null) {
            explain.setTaskBusinessKey(task.getBusinessKey());
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
            explain.setStockDeductQty(task.getStockDeductQty());
            explain.setFinalPlanQty(task.getPlanQty());
            explain.setToolLedgerOrder(task.getToolLedgerOrder());
            explain.setAvailableToolQty(task.getAvailableToolQty());
            explain.setToolUsedQty(task.getToolUsedQty());
            explain.setRemainingToolQty(task.getRemainingToolQty());
            explain.setPlanQtyBreakdownJson(this.buildPlanQtyBreakdownJson(task));
            explain.setStockQty(task.getSixClockStockQty());
            explain.setPlanStockQty(task.getPlanStockQty());
            explain.setSupplyHours(task.getSupplyHours());
            explain.setCoverageShiftCount(task.getGuardShiftCount());
            explain.setUnplannedReasonCode(task.getUnplannedReasonCode());
            explain.setTaskStatus(resolveTaskStatus(task));
            explain.setSidewallCode(task.getSidewallCode());
            explain.setGlueCode(task.getGlueCode());
            explain.setBaseGlueCode(task.getBaseGlueCode());
            explain.setMouthPlateCode(task.getMouthPlateCode());
            explain.setSelectedMachineCode(task.getMachineCode());
        }
        if (snapshot != null) {
            explain.setRuleHitJson(snapshot.getRuleHitJson());
            explain.setCandidateMachineJson(snapshot.getCandidateMachineJson());
            explain.setSelectedMachineScore(snapshot.getSelectedMachineScore());
            explain.setAssignStatus(snapshot.getAssignStatus());
            explain.setUnplannedEvidenceJson(snapshot.getUnplannedEvidenceJson());
            explain.setIssueLevel(snapshot.getIssueLevel());
            explain.setIssueJson(snapshot.getIssueJson());
        }
        explain.setManualLockedFlag(TcYesNoEnum.NO.getCode());
        explain.setSequenceLockFlag(TcYesNoEnum.NO.getCode());
        explain.setForceChangeFlag(TcYesNoEnum.NO.getCode());
        return explain;
    }

    /**
     * 构建带版本号的计划量分解 JSON。
     *
     * @param task 待排任务
     * @return 计划量分解 JSON
     */
    private String buildPlanQtyBreakdownJson(TcTaskDraft task) {
        JSONObject result = new JSONObject();
        result.set("schemaVersion", "1");
        JSONArray itemArray = new JSONArray();
        BigDecimal requiredQty = this.resolveRequiredQty(task);
        BigDecimal baseDemandQty = nvl(task.getBaseDemandQty());
        BigDecimal preLossPlanQty = this.nvl(task.getPreLossPlanQty());
        BigDecimal afterLossQty = preLossPlanQty.add(this.nvl(task.getLossAddQty()));
        BigDecimal afterMinStartQty = afterLossQty.add(this.nvl(task.getMinStartAdjustQty()));
        BigDecimal afterRoundQty = afterMinStartQty.add(this.nvl(task.getTailRoundAdjustQty()));
        this.addBreakdownItem(itemArray, "REQUIRED", "基础需求与库存抵扣",
                requiredQty, baseDemandQty, baseDemandQty.subtract(nvl(requiredQty)),
                "INVENTORY", "max(当前班需求,保证范围需求)-滚动库存");
        this.addBreakdownItem(itemArray, "LAST_SHIFT_SUPPLY", "上班供应抵扣",
                null, null, BigDecimal.ZERO, "SHIFT_CHAIN", "当前版本无独立分量");
        this.addBreakdownItem(itemArray, "MONTH_SURPLUS_DEDUCT", "月结余抵扣",
                null, null, BigDecimal.ZERO, "MONTH_PLAN", "当前版本无独立分量");
        if (this.isTailTask(task)) {
            this.addBreakdownItem(itemArray, "TAIL_ROUND", "收尾基础量调整",
                    baseDemandQty, preLossPlanQty, this.nvl(task.getTailRoundAdjustQty()),
                    "SIDEWALL_LENGTH", task.getCalcFormulaDesc());
            this.addBreakdownItem(itemArray, "LOSS_ADD", "损耗补偿",
                    preLossPlanQty, afterLossQty, this.nvl(task.getLossAddQty()),
                    task.getLossMatchSource(), "损耗前计划量×损耗率");
            this.addBreakdownItem(itemArray, "MIN_START", "收尾跳过最小起排",
                    afterLossQty, afterLossQty, this.nvl(task.getMinStartAdjustQty()),
                    TcScheduleConstants.PARAM_MIN_START_QTY, "收尾任务不执行最小起排");
        } else {
            this.addBreakdownItem(itemArray, "LOSS_ADD", "损耗补偿",
                    preLossPlanQty, afterLossQty, this.nvl(task.getLossAddQty()),
                    task.getLossMatchSource(), "损耗前计划量×损耗率");
            this.addBreakdownItem(itemArray, "MIN_START", "最小起排调整",
                    afterLossQty, afterMinStartQty, this.nvl(task.getMinStartAdjustQty()),
                    TcScheduleConstants.PARAM_MIN_START_QTY, "损耗后正计划量不足最小起排时补齐");
            this.addBreakdownItem(itemArray, "TAIL_ROUND", "卷曲取整",
                    afterMinStartQty, afterRoundQty, this.nvl(task.getTailRoundAdjustQty()),
                    "CURL_LENGTH", task.getCalcFormulaDesc());
        }
        this.addBreakdownItem(itemArray, "TOOL_LIMIT", "工装限制",
                task.getPlanQtyBeforeToolLimit(), nvl(task.getPlanQtyBeforeToolLimit())
                        .add(nvl(task.getToolLimitAdjustQty())), nvl(task.getToolLimitAdjustQty()),
                TcScheduleConstants.PARAM_TOOL_TOTAL_QTY, "工厂级可用工装池限制");
        this.addBreakdownItem(itemArray, "CAPACITY", "机台产能调整",
                null, task.getPlanQty(), nvl(task.getCapacityAdjustQty()),
                "T_TC_MACHINE_INFO.MAX_CAPACITY", "机台最大班产限制与顺延");
        result.set("items", itemArray);
        return JSONUtil.toJsonPrettyStr(result);
    }

    /**
     * 追加计划量分解明细。
     *
     * @param itemArray 明细数组
     * @param code 分量编码
     * @param name 分量名称
     * @param before 调整前数量
     * @param after 调整后数量
     * @param delta 调整量
     * @param source 来源
     * @param formula 公式说明
     */
    private void addBreakdownItem(JSONArray itemArray, String code, String name, BigDecimal before,
                                  BigDecimal after, BigDecimal delta, String source, String formula) {
        JSONObject item = new JSONObject();
        item.set("code", code);
        item.set("name", name);
        item.set("before", before);
        item.set("after", after);
        item.set("delta", delta);
        item.set("source", source);
        item.set("formula", formula);
        itemArray.add(item);
    }

    /**
     * 解析解释表任务状态。
     *
     * @param task 待排任务草稿
     * @return 任务状态；未排任务返回 null，由未排原因字段表达
     */
    private String resolveTaskStatus(TcTaskDraft task) {
        if (task == null || isUnplannedTask(task)) {
            return null;
        }
        if (isNoProductionNeeded(task)) {
            return TcScheduleTaskStatusEnum.NO_PRODUCTION_NEEDED.getCode();
        }
        return TcScheduleTaskStatusEnum.PLANNED.getCode();
    }

    /**
     * 判断任务是否属于未排任务。
     *
     * @param task 待排任务草稿
     * @return true 表示任务需要进入未排语义
     */
    private boolean isUnplannedTask(TcTaskDraft task) {
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
    private boolean isNoProductionNeeded(TcTaskDraft task) {
        return task != null && !isUnplannedTask(task)
                && (task.getPlanQty() == null || task.getPlanQty().compareTo(BigDecimal.ZERO) <= 0);
    }

    /**
     * 解析解释表应排需求量。
     *
     * @param task 待排任务草稿
     * @return 库存抵扣前的当前班成型胎侧需求量；旧骨架缺失当前班需求时回退需求量
     */
    private BigDecimal resolveRequiredQty(TcTaskDraft task) {
        if (task == null) {
            return null;
        }
        if (Boolean.TRUE.equals(task.getFormingShutdownCloseOutFlag())) {
            return task.getFormingShutdownCloseOutDemandQty();
        }
        return task.getCurrentShiftDemandQty() == null ? task.getDemandQty() : task.getCurrentShiftDemandQty();
    }

    /**
     * 空数量转零。
     *
     * @param value 原数量
     * @return 非空数量
     */
    private BigDecimal nvl(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    /**
     * 判断任务是否使用收尾基础量口径。
     *
     * @param task 待排任务
     * @return 成型连续停产收尾，或普通收尾标识、收尾余量和胎侧长度均有效时返回true
     */
    private boolean isTailTask(TcTaskDraft task) {
        return task != null
                && (Boolean.TRUE.equals(task.getFormingShutdownCloseOutFlag())
                || (TcYesNoEnum.YES.getCode().equals(task.getTailFlag())
                && this.nvl(task.getTailBalanceQty()).compareTo(BigDecimal.ZERO) > 0
                && this.nvl(task.getSidewallLength()).compareTo(BigDecimal.ZERO) > 0));
    }

    private TcScheduleResult convertNodeToResult(ScheduleTaskNode<TcTaskDraft> node, TcScheduleContext context) {
        TcTaskDraft task = node.getTask();
        TcScheduleResult result = new TcScheduleResult();
        result.setFactoryCode(context == null ? null : context.getFactoryCode());
        result.setBatchNo(context == null ? null : context.getBatchNo());
        result.setScheduleDate(context == null ? null : context.getScheduleDate());
        result.setMachineCode(node.getMachineCode());
        result.setSidewallCode(task.getSidewallCode());
        result.setConstructionVersion(task.getConstructionVersion());
        result.setSidewallCraft(task.getSidewallCraft());
        result.setSidewallLength(task.getSidewallLength());
        result.setSidewallWeight(task.getSidewallWeight());
        result.setSidewallWearpRubberWeight(task.getSidewallWearpRubberWeight());
        result.setGlueCode(task.getGlueCode());
        result.setBaseGlueCode(task.getBaseGlueCode());
        result.setMouthPlateCode(task.getMouthPlateCode());
        result.setReleaseStatus(TcScheduleReleaseStatusEnum.NOT_RELEASED.getCode());
        result.setDataSource(TcScheduleConstants.AUTO_SCHEDULE_DATA_SOURCE);
        result.setTailFlag(task.getTailFlag());
        result.setMonthSurplusQty(task.getMonthSurplusQty());
        result.setStockQty(task.getSixClockStockQty());
        applyShiftFields(result, node);
        return result;
    }

    /**
     * 按任务班次写入未排任务的班次字段。
     *
     * @param result 排程结果
     * @param task   未排任务
     */
    private void applyTaskShiftFields(TcScheduleResult result, TcTaskDraft task) {
        Integer shiftOrder = task.getShiftOrder() == null ? 1 : task.getShiftOrder();
        applyShiftFields(result, shiftOrder, 1, task.getPlanQty(), null, null);
    }

    /**
     * 按任务链节点班次写入排程结果字段。
     *
     * @param result 排程结果
     * @param node   任务链节点
     */
    private void applyShiftFields(TcScheduleResult result, ScheduleTaskNode<TcTaskDraft> node) {
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
    private void applyShiftFields(TcScheduleResult result, Integer shiftOrder, Integer sequence, BigDecimal planQty,
                                  Date startTime, Date endTime) {
        if (shiftOrder == null || shiftOrder < 1 || shiftOrder > TcScheduleConstants.TC_MAX_SHIFT_ORDER) {
            throw new ServiceException(TcScheduleErrorCodeEnum.TC_SHIFT_INVALID.getDefaultMessage() + ":" + shiftOrder);
        }
        result.setFieldValueByFieldName(String.format(TcScheduleConstants.SHIFT_SEQUENCE_FIELD_TEMPLATE, shiftOrder), sequence);
        result.setFieldValueByFieldName(String.format(TcScheduleConstants.SHIFT_PLAN_QTY_FIELD_TEMPLATE, shiftOrder), planQty);
        result.setFieldValueByFieldName(String.format(TcScheduleConstants.SHIFT_START_TIME_FIELD_TEMPLATE, shiftOrder), startTime);
        result.setFieldValueByFieldName(String.format(TcScheduleConstants.SHIFT_END_TIME_FIELD_TEMPLATE, shiftOrder), endTime);
    }
}
