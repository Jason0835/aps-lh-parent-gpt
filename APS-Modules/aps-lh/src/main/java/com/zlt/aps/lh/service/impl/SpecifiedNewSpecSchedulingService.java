package com.zlt.aps.lh.service.impl;

import com.zlt.aps.lh.api.domain.dto.SkuScheduleDTO;
import com.zlt.aps.lh.api.domain.entity.LhScheduleResult;
import com.zlt.aps.lh.api.enums.ScheduleTypeEnum;
import com.zlt.aps.lh.component.MonthPlanDateResolver;
import com.zlt.aps.lh.context.LhScheduleContext;
import com.zlt.aps.lh.engine.factory.ScheduleStrategyFactory;
import com.zlt.aps.lh.engine.strategy.ICapacityCalculateStrategy;
import com.zlt.aps.lh.engine.strategy.IFirstInspectionBalanceStrategy;
import com.zlt.aps.lh.engine.strategy.IMachineMatchStrategy;
import com.zlt.aps.lh.engine.strategy.IMouldChangeBalanceStrategy;
import com.zlt.aps.lh.engine.strategy.IProductionStrategy;
import com.zlt.aps.lh.engine.strategy.impl.NewSpecProductionStrategy;
import com.zlt.aps.lh.engine.strategy.support.DailyNewSpecCandidate;
import com.zlt.aps.lh.engine.strategy.support.DayScheduleContext;
import com.zlt.aps.lh.engine.strategy.support.EarlyProductionRuntimePlan;
import com.zlt.aps.lh.engine.strategy.support.ScheduleSubstitutionDirective;
import com.zlt.aps.lh.util.ShiftFieldUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * 联动置换复用新增规格主链的隔离调度服务。
 *
 * <p>调用方只传入一个 A 或 B 及临时指令。本服务临时隔离 {@code newSpecSkuList}，继续调用
 * 原有选机、模具、换模均衡、晚班禁换模、首检、停机和产能链；完成后恢复原待排列表并清理
 * 临时指令。预演和正式提交使用同一个入口，避免形成两套排程规则。</p>
 *
 * @author APS
 */
@Service
public class SpecifiedNewSpecSchedulingService {

    @Resource
    private ScheduleStrategyFactory strategyFactory;

    /**
     * 对单个置换 SKU 执行一次隔离新增排产。
     *
     * @param context 排程上下文
     * @param sku 物料 A 或 B 的迁移副本
     * @param directive 本次临时排产指令
     * @return 本次新生成的正计划量结果，单控整机可能包含 L/R 两条
     */
    public List<LhScheduleResult> schedule(
            LhScheduleContext context,
            SkuScheduleDTO sku,
            ScheduleSubstitutionDirective directive) {
        return this.schedule(context, sku, directive, null, null);
    }

    /**
     * 对当日未上机候选执行一次指定机台单日重排，次日由外层日驱动绑定续排。
     *
     * @param context 排程上下文
     * @param dayContext 当前业务日班次切片
     * @param candidate 已通过正常准入的当日候选
     * @param directive 指定机台及最早切换时间
     * @return 本次新生成的正计划量结果
     */
    public List<LhScheduleResult> scheduleCurrentDay(LhScheduleContext context,
                                                     DayScheduleContext dayContext,
                                                     DailyNewSpecCandidate candidate,
                                                     ScheduleSubstitutionDirective directive) {
        Objects.requireNonNull(dayContext, "当日置换必须指定业务日");
        Objects.requireNonNull(candidate, "当日置换必须指定已准入候选");
        return this.schedule(context, candidate.getSku(), directive, dayContext, candidate);
    }

    /**
     * 在隔离待排视图和指令作用域内复用原排产内核，退出时恢复调用方日期和临时视图。
     *
     * @param context 排程上下文
     * @param sku 置换目标 SKU
     * @param directive 临时指定机台指令
     * @param dayContext 单日重排上下文；为空时保持原窗口联动置换行为
     * @param candidate 单日已准入候选；窗口模式为空
     * @return 本次新生成的正计划量结果
     */
    private List<LhScheduleResult> schedule(LhScheduleContext context, SkuScheduleDTO sku,
                                           ScheduleSubstitutionDirective directive,
                                           DayScheduleContext dayContext,
                                           DailyNewSpecCandidate candidate) {
        Date originalCurrentScheduleDate = context.getCurrentScheduleDate();
        List<SkuScheduleDTO> originalPendingSkuList =
                new ArrayList<SkuScheduleDTO>(context.getNewSpecSkuList());
        Set<LhScheduleResult> baselineResultSet =
                Collections.newSetFromMap(
                        new IdentityHashMap<LhScheduleResult, Boolean>());
        baselineResultSet.addAll(context.getScheduleResultList());
        Map<SkuScheduleDTO, EarlyProductionRuntimePlan>
                originalEarlyProductionRuntimePlanMap =
                new IdentityHashMap<SkuScheduleDTO, EarlyProductionRuntimePlan>(
                        context.getEarlyProductionRuntimePlanMap());
        context.setNewSpecSkuList(
                new ArrayList<SkuScheduleDTO>(Collections.singletonList(sku)));
        context.setScheduleSubstitutionDirective(directive);
        try {
            IProductionStrategy strategy = strategyFactory.getProductionStrategy(
                    ScheduleTypeEnum.NEW_SPEC.getCode());
            IMachineMatchStrategy machineMatchStrategy =
                    strategyFactory.getMachineMatchStrategy();
            IMouldChangeBalanceStrategy mouldChangeStrategy =
                    strategyFactory.getMouldChangeBalanceStrategy();
            IFirstInspectionBalanceStrategy inspectionStrategy =
                    strategyFactory.getFirstInspectionBalanceStrategy();
            ICapacityCalculateStrategy capacityStrategy =
                    strategyFactory.getCapacityCalculateStrategy();
            if (Objects.nonNull(dayContext)) {
                // 单日置换直接调用公共单机提交能力，不重新遍历 T～T+2 或提前写次日结果。
                context.setCurrentScheduleDate(dayContext.getDayShifts().get(0).getWorkDate());
                boolean committed = ((NewSpecProductionStrategy) strategy).executeSpecifiedMachine(
                        context, dayContext, candidate,
                        context.getMachineScheduleMap().get(directive.getSpecifiedMachineCode()),
                        machineMatchStrategy, mouldChangeStrategy, inspectionStrategy, capacityStrategy);
                if (!committed) {
                    return Collections.emptyList();
                }
            } else {
                // 窗口后置的共用模具联动仍保留原有完整窗口排产行为。
                strategy.scheduleNewSpecs(context, machineMatchStrategy, mouldChangeStrategy,
                        inspectionStrategy, capacityStrategy);
                strategy.allocateShiftPlanQty(context);
            }
            return this.collectNewResults(context, sku, baselineResultSet);
        } finally {
            /*
             * 协调器会在更外层使用通用快照决定提交或回滚；本层只恢复待排视图和临时运行指令，
             * 不能撤销已经生成的结果和资源账本。
             */
            context.setEarlyProductionRuntimePlanMap(
                    originalEarlyProductionRuntimePlanMap);
            context.setNewSpecSkuList(originalPendingSkuList);
            context.clearScheduleSubstitutionDirective();
            context.setCurrentScheduleDate(originalCurrentScheduleDate);
        }
    }

    /**
     * 按对象身份排除调用前结果，仅收集本次 A 或 B 新生成的正计划量结果。
     *
     * @param context 排程上下文
     * @param sku 本次隔离排产 SKU
     * @param baselineResultSet 调用前结果身份集合
     * @return 本次新增结果
     */
    private List<LhScheduleResult> collectNewResults(
            LhScheduleContext context,
            SkuScheduleDTO sku,
            Set<LhScheduleResult> baselineResultSet) {
        List<LhScheduleResult> resultList = new ArrayList<LhScheduleResult>(2);
        String skuKey = MonthPlanDateResolver.buildMaterialStatusKey(
                sku.getMaterialCode(), sku.getProductStatus());
        for (LhScheduleResult result : context.getScheduleResultList()) {
            if (baselineResultSet.contains(result)
                    || ShiftFieldUtil.resolveScheduledQty(result) <= 0
                    || !StringUtils.equals(skuKey,
                    MonthPlanDateResolver.buildMaterialStatusKey(
                            result.getMaterialCode(), result.getProductStatus()))) {
                continue;
            }
            resultList.add(result);
        }
        return resultList;
    }
}
