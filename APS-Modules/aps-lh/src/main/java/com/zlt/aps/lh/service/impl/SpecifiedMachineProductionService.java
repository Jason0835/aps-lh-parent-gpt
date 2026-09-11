package com.zlt.aps.lh.service.impl;

import com.zlt.aps.lh.api.domain.dto.MachineScheduleDTO;
import com.zlt.aps.lh.api.domain.dto.SkuScheduleDTO;
import com.zlt.aps.lh.component.EarlyProductionRuntimePlanService;
import com.zlt.aps.lh.context.LhScheduleContext;
import com.zlt.aps.lh.engine.strategy.support.DailySchedulePhase;
import com.zlt.aps.lh.engine.strategy.support.DayScheduleContext;
import com.zlt.aps.lh.engine.strategy.support.EarlyProductionRuntimePlan;
import com.zlt.aps.lh.engine.strategy.support.MachineSkuMatchResult;
import com.zlt.aps.lh.engine.strategy.support.NewSpecMachineAssignmentPlan;
import com.zlt.aps.lh.util.LhSingleControlMachineUtil;
import com.zlt.aps.lh.util.MachineStatusUtil;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.IntSupplier;

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
            return executeAction.getAsInt();
        };
        if (assignmentPlan.isFixedMachineExecution()) {
            return newSpecScheduleCommitService.commitResultOnly(context, activatedAction);
        }
        return newSpecScheduleCommitService.commit(
                context, assignmentPlan.getCandidate().getSku(), activatedAction);
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
