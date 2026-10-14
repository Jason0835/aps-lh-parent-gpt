package com.zlt.aps.lh.service.impl;

import com.zlt.aps.lh.api.domain.dto.MachineScheduleDTO;
import com.zlt.aps.lh.api.domain.dto.SkuScheduleDTO;
import com.zlt.aps.lh.component.EarlyProductionRuntimePlanService;
import com.zlt.aps.lh.context.LhScheduleContext;
import com.zlt.aps.lh.engine.strategy.support.DailySchedulePhase;
import com.zlt.aps.lh.engine.strategy.support.StructureSwitchPlan;
import com.zlt.aps.lh.engine.strategy.support.StructureSwitchSchedulingPolicy;
import com.zlt.aps.lh.engine.strategy.support.DayScheduleContext;
import com.zlt.aps.lh.engine.strategy.support.EarlyProductionRuntimePlan;
import com.zlt.aps.lh.engine.strategy.support.MachineSkuMatchResult;
import com.zlt.aps.lh.engine.strategy.support.NewSpecMachineAssignmentPlan;
import com.zlt.aps.lh.engine.strategy.support.NewSpecMachineAvailabilityPlan;
import com.zlt.aps.lh.engine.strategy.support.DailyNewSpecCandidate;
import com.zlt.aps.lh.engine.strategy.support.NewSpecCandidateAttemptService;
import com.zlt.aps.lh.engine.strategy.support.StructureMachineLimitDecision;
import com.zlt.aps.lh.engine.strategy.IMachineMatchStrategy;
import com.zlt.aps.lh.util.LhSingleControlMachineUtil;
import com.zlt.aps.lh.util.MachineStatusUtil;
import com.zlt.aps.lh.util.LhScheduleTimeUtil;
import org.springframework.stereotype.Service;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.IntSupplier;
import java.util.function.Function;

/**
 * 已确定机台与物料组合的公共执行服务。
 * <p>不查询历史、不重新选型。普通选型和前置指定组合都经本服务激活需求、提交实际时间轴，
 * 换模、换活字块、首检、数量与机台状态仍由同一个正式执行动作更新。</p>
 */
@Service
public class SpecifiedMachineProductionService {

    @Resource
    private NewSpecScheduleCommitService newSpecScheduleCommitService;
    @Resource
    private EarlyProductionRuntimePlanService earlyProductionRuntimePlanService;

    /**
     * 普通新增、历史指定及结构切换共用真实首检/生产班次准入。
     */
    @Resource
    private NewSpecCandidateAttemptService candidateAttemptService;

    /**
     * 公共单组合预演入口：先声明或硬匹配，再解析唯一时间轴，不分配资源。
     *
     * @param context 共享上下文
     * @param day 当前业务日
     * @param candidate 已准入物料
     * @param machine 当前候选机台
     * @param machineMatch 原硬匹配策略
     * @param fixedMachine 是否为原有指定执行入口，结构切换普通候选必须为false
     * @param resolver 原时间轴解析能力
     * @return 完整合法提案；失败保留候选及原因
     */
    public NewSpecMachineAssignmentPlan preview(LhScheduleContext context, DayScheduleContext day,
            DailyNewSpecCandidate candidate, MachineScheduleDTO machine, IMachineMatchStrategy machineMatch,
            boolean fixedMachine, Function<MachineSkuMatchResult, NewSpecMachineAvailabilityPlan> resolver) {
        MachineSkuMatchResult match = fixedMachine ? this.declareMachine(context, machine, candidate.getSku())
                : machineMatch.matchSkuOnMachine(context, machine, candidate.getSku());
        if (!match.isMatched()) {
            candidate.setLastFailure(match.getFailureReason());
            return null;
        }
        if (!fixedMachine) {
            String failure = candidateAttemptService.resolveReadOnlyEligibilityFailure(context, candidate, match);
            if (StringUtils.isNotEmpty(failure)) {
                candidate.setLastFailure(failure);
                return null;
            }
        }
        NewSpecMachineAvailabilityPlan availability;
        context.enterNewSpecProposalPreviewScope();
        try {
            availability = resolver.apply(match);
        } finally {
            context.exitNewSpecProposalPreviewScope();
        }
        if (Objects.isNull(availability) || !availability.isAvailable()
                || Objects.isNull(availability.getCompetitionTargetShift())) {
            candidate.setLastFailure(Objects.isNull(availability)
                    ? "未形成有效排产时间轴" : availability.getUnavailableReason());
            return null;
        }
        NewSpecMachineAssignmentPlan assignment = new NewSpecMachineAssignmentPlan(candidate, match,
                availability.getCompetitionTargetShift().getShiftIndex(), candidate.getTargetPlanDate(),
                    availability, true);
        if (fixedMachine) {
            assignment.asFixedMachineExecution();
        }
        if (!this.validateStructure(context, day, assignment)) {
            return null;
        }
        return assignment;
    }

    /**
     * 预演和正式提交共用无缓存复核，冻结时间轴的首检和生产班次均须满足名额。
     *
     * @param context 共享上下文
     * @param day 实际业务日
     * @param assignment 冻结提案
     * @return 是否允许提交
     */
    public boolean validateStructure(LhScheduleContext context, DayScheduleContext day,
            NewSpecMachineAssignmentPlan assignment) {
        StructureMachineLimitDecision decision = candidateAttemptService.resolveStructureMachineLimitDecision(
                context, day, assignment.getCandidate(), assignment.getMatchResult(),
                assignment.getAvailabilityPlan(), assignment.isFixedMachineExecution()
                        ? assignment.getCandidate().getPoolDate() : assignment.getPoolDate(), null);
        if (Objects.nonNull(decision) && !decision.isAllowed()) {
            assignment.getCandidate().setLastFailure(decision.getReason());
            candidateAttemptService.logStructureMachineLimitDecision("指定组合提交复核", decision, assignment.getCandidate());
            return false;
        }
        return true;
    }

    /**
     * 原绑定逐班增量也按同一快照原子提交；零量或结构名额复核失败时完整恢复。
     *
     * @param context 共享资源及账本
     * @param action 原机增量排产动作
     * @return 是否提交了正量增量
     */
    public boolean commitContinuation(LhScheduleContext context, IntSupplier action) {
        return newSpecScheduleCommitService.commitResultOnly(context, action).getScheduledCount() > 0;
    }

    /**
     * 声明指定组合占用的物理资源，不执行尺寸匹配、优先级比较或机台选择。
     * @param context 排程上下文
     * @param machine 已明确的机台
     * @param sku 已具备本次资格的物料
     * @return 执行所需机台范围；单控整机两侧必须完整且可用
     */
    public MachineSkuMatchResult declareMachine(
            LhScheduleContext context, MachineScheduleDTO machine, SkuScheduleDTO sku) {
        if (!this.isAvailableMachine(context, machine)) {
            return MachineSkuMatchResult.failed(machine, sku, "指定机台未启用或处于续作停产保机状态");
        }
        boolean wholeMachine = LhSingleControlMachineUtil.isConfiguredSingleControlMachine(
                context, machine.getMachineCode())
                && LhSingleControlMachineUtil.isWholeMachineGranularitySku(context, sku);
        if (!wholeMachine) {
            return MachineSkuMatchResult.matched(machine, sku,
                    Collections.singletonList(machine.getMachineCode()), null, null);
        }
        MachineScheduleDTO leftMachine = context.getMachineScheduleMap().get(
                LhSingleControlMachineUtil.resolveLeftMachineCode(machine.getMachineCode()));
        MachineScheduleDTO rightMachine = context.getMachineScheduleMap().get(
                LhSingleControlMachineUtil.resolveRightMachineCode(machine.getMachineCode()));
        if (!this.isAvailableMachine(context, leftMachine) || !this.isAvailableMachine(context, rightMachine)) {
            return MachineSkuMatchResult.failed(machine, sku, "单控整机左右侧运行态不完整或不可用");
        }
        List<String> machineCodes = new ArrayList<String>(2);
        machineCodes.add(leftMachine.getMachineCode());
        machineCodes.add(rightMachine.getMachineCode());
        return MachineSkuMatchResult.matched(leftMachine, sku, machineCodes, null, null);
    }

    /**
     * 提交一个确定组合。固定组合失败只撤回本次尝试，普通排产保留原终局未排语义。
     * @param context 排程上下文
     * @param dayContext 本次业务日
     * @param assignmentPlan 已冻结的组合与时间轴
     * @param executeAction 共用的实际排产内核
     * @return 正式提交或完整恢复结果
     */
    public NewSpecScheduleCommitResult commit(
            LhScheduleContext context, DayScheduleContext dayContext,
            NewSpecMachineAssignmentPlan assignmentPlan, IntSupplier executeAction) {
        IntSupplier activatedAction = () -> {
            if (dayContext.getCurrentPhase() == DailySchedulePhase.EARLY_PRODUCTION
                    && Objects.nonNull(assignmentPlan.getCandidate().getEarlyProductionPreview())) {
                EarlyProductionRuntimePlan activated = earlyProductionRuntimePlanService.activateRuntimePlan(
                        context, assignmentPlan.getCandidate().getSku(),
                        assignmentPlan.getCandidate().getEarlyProductionPreview());
                if (Objects.isNull(activated) || !activated.isActive()) {
                    assignmentPlan.getCandidate().setLastFailure("提前生产运行视图激活失败");
                    return 0;
                }
            }
            if (!this.validateStructure(context, dayContext, assignmentPlan)) {
                return 0;
            }
            // 关闭总开关时直接执行原提交动作，不创建切换计划绑定作用域。
            if (!StructureSwitchSchedulingPolicy.isEnabled(context)) {
                return executeAction.getAsInt();
            }
            StructureSwitchPlan switchPlan = assignmentPlan.getAvailabilityPlan().getStructureSwitchPlan();
            Map<String, StructureSwitchPlan> previousPlans = new LinkedHashMap<>(context.getStructureSwitchAttemptPlanMap());
            try {
                // 仅进入实际提交后提供分量策略，不更新S0或结构计数。
                if (Objects.nonNull(switchPlan)) {
                    assignmentPlan.getMatchResult().getDeclaredMachineCodes().forEach(machineCode ->
                            context.getStructureSwitchAttemptPlanMap().put(machineCode, switchPlan));
                }
                int beforeCount = context.getScheduleResultList().size();
                Map<String, int[]> beforeChangeCounts = new LinkedHashMap<>();
                if (context.isStructureSwitchScheduling()) {
                    context.getDailyMouldChangeCountMap().forEach((date, counts) -> beforeChangeCounts.put(date,
                        counts.clone()));
                }
                int scheduledCount = executeAction.getAsInt();
                if (context.isStructureSwitchScheduling() && !this.withinStructureSwitchQuotas(context,
                    beforeChangeCounts)) {
                    assignmentPlan.getCandidate().setLastFailure("结构切换换模和换活字块合并次数超过实际班次或每日上限");
                    return 0;
                }
                for (int index = beforeCount; index < context.getScheduleResultList().size(); index++) {
                    StructureSwitchSchedulingPolicy.bindResult(context, context.getScheduleResultList().get(index));
                }
                return scheduledCount;
            } finally {
                context.setStructureSwitchAttemptPlanMap(previousPlans);
            }
        };
        if (assignmentPlan.isPreScheduledExecution()) {
            return newSpecScheduleCommitService.commitResultOnly(context, activatedAction);
        }
        return newSpecScheduleCommitService.commit(
                context, assignmentPlan.getCandidate().getSku(), activatedAction);
    }

    /**
     * 复核本次新增的实际动作，防止共用内核的时段重试绕过独立步骤硬上限。
     * 只核查本次增长的日期，不把其他日期既有计数问题转嫁给当前合法组合。
     *
     * @param context 正式执行后的实际次数
     * @param before 执行前次数快照
     * @return 本次动作是否满足合并日次数和班次次数
     */
    private boolean withinStructureSwitchQuotas(LhScheduleContext context, Map<String, int[]> before) {
        return context.getDailyMouldChangeCountMap().entrySet().stream().allMatch(entry -> {
            int[] previous = before.getOrDefault(entry.getKey(), new int[]{0, 0});
            int[] current = entry.getValue();
            if (current[0] <= previous[0] && current[1] <= previous[1]) {
                return true;
            }
            return current[0] <= LhScheduleTimeUtil.getMorningMouldChangeLimit(context, entry.getKey())
                    && current[1] <= LhScheduleTimeUtil.getAfternoonMouldChangeLimit(context, entry.getKey())
                    && current[0] + current[1] <= LhScheduleTimeUtil.getDailyMouldChangeLimit(context, entry.getKey());
        });
    }

    /**
     * 校验资源池成员身份，不附加任何物料选型条件。
     * @param context 排程上下文
     * @param machine 机台运行态
     * @return 是否为启用且未被保机锁定的机台
     */
    private boolean isAvailableMachine(LhScheduleContext context, MachineScheduleDTO machine) {
        return Objects.nonNull(machine) && MachineStatusUtil.isEnabled(machine.getStatus())
                && !context.isContinuousStopHoldMachine(machine.getMachineCode());
    }
}
