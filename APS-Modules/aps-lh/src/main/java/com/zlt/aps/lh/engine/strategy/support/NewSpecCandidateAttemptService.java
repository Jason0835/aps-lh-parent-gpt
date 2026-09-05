package com.zlt.aps.lh.engine.strategy.support;

import com.zlt.aps.lh.api.domain.dto.MachineScheduleDTO;
import com.zlt.aps.lh.api.domain.vo.LhShiftConfigVO;
import com.zlt.aps.lh.component.StructureEndingAlignmentDecision;
import com.zlt.aps.lh.component.StructureEndingAlignmentService;
import com.zlt.aps.lh.context.LhScheduleContext;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.time.LocalDate;
import java.util.Objects;

/**
 * 新增排产单个Machine×SKU组合的无副作用试算服务。
 *
 * <p>服务只消费反向硬匹配结果和正式时间轴预演，不写任何共享运行态。参数为1时，普通候选
 * 必须在当前机台驱动班次真实可开产；目标日跨日准备候选按换模资源班次竞争，并保留紧邻
 * 下一夜班作为正式生产班次。参数为0时，当前班次只按机台收尾时间归属，但提案仍要求完整
 * 真实时间轴存在合法生产班次。</p>
 *
 * @author APS
 */
@Component
public class NewSpecCandidateAttemptService {

    /** 结构收尾对齐只读候选准入入口 */
    @Resource
    private StructureEndingAlignmentService structureEndingAlignmentService;
    /** 普通新增、续作加机和提前生产共用的结构机台上限准入入口 */
    @Resource
    private StructureMachineLimitAdmissionService structureMachineLimitAdmissionService;

    /**
     * 在完整时间轴试算和运行态快照前执行机台相关只读准入。
     *
     * <p>结构收尾对齐原本只在正式新增内核中执行，导致明确不同结构的Machine×SKU仍然
     * 捕获并恢复完整上下文。这里直接复用公共判断，并严格校验正式内核实际选择的代表机台；
     * 正式内核仍保留提交前复核，防止未来调用链绕过本入口。</p>
     *
     * @param context 排程上下文
     * @param candidate 当前SKU候选
     * @param matchResult 反向硬匹配结果
     * @return 空值表示准入通过；非空表示明确拒绝原因
     */
    public String resolveReadOnlyEligibilityFailure(
            LhScheduleContext context,
            DailyNewSpecCandidate candidate,
            MachineSkuMatchResult matchResult) {
        if (Objects.isNull(context) || Objects.isNull(candidate)
                || Objects.isNull(candidate.getSku()) || Objects.isNull(matchResult)
                || !matchResult.isMatched() || Objects.isNull(matchResult.getMachine())) {
            return "机台驱动只读准入缺少有效的上下文、SKU或机台匹配结果";
        }
        MachineScheduleDTO selectedMachine = context.getMachineScheduleMap().get(
                matchResult.getMachine().getMachineCode());
        if (Objects.isNull(selectedMachine)) {
            return new StringBuilder("机台驱动声明机台不存在，machineCode=")
                    .append(matchResult.getMachine().getMachineCode()).toString();
        }
        StructureEndingAlignmentDecision decision = structureEndingAlignmentService
                .evaluateCandidate(context, candidate.getSku(), selectedMachine, false);
        if (!decision.isAllowed()) {
            return new StringBuilder("结构收尾对齐只读准入拒绝，machineCode=")
                    .append(selectedMachine.getMachineCode())
                    .append(", reason=")
                    .append(StringUtils.defaultIfEmpty(
                            decision.getExcludedReason(), "候选机台不允许选择"))
                    .toString();
        }
        return null;
    }

    /**
     * 构建单个无副作用排产提案。
     *
     * @param context 排程上下文
     * @param dayContext 当前业务日
     * @param shift 当前机台资源归属班次
     * @param machineResource 当前机台资源
     * @param poolDate 候选原始日期池
     * @param candidate 当前SKU候选
     * @param matchResult 反向硬匹配结果
     * @param availabilityResolver 正式时间轴解析器
     * @param actualAvailableTimeMode 是否按实际可开产时间归班
     * @return 试算通过的不可变提案；当前班次不可执行时返回null
     */
    public NewSpecScheduleProposal preview(
            LhScheduleContext context,
            DayScheduleContext dayContext,
            LhShiftConfigVO shift,
            MachineResource machineResource,
            LocalDate poolDate,
            DailyNewSpecCandidate candidate,
            MachineSkuMatchResult matchResult,
            NewSpecMachineAvailabilityResolver availabilityResolver,
            boolean actualAvailableTimeMode) {
        if (Objects.isNull(context) || Objects.isNull(dayContext) || Objects.isNull(shift)
                || Objects.isNull(shift.getShiftIndex()) || Objects.isNull(machineResource)
                || Objects.isNull(candidate) || Objects.isNull(matchResult)
                || !matchResult.isMatched() || Objects.isNull(availabilityResolver)) {
            return null;
        }
        NewSpecMachineAvailabilityPlan availabilityPlan = availabilityResolver.resolve(
                context, dayContext, candidate, matchResult.getMachine());
        return this.previewWithPlan(
                context, dayContext, shift, machineResource, poolDate,
                candidate, matchResult, availabilityPlan,
                actualAvailableTimeMode);
    }

    /**
     * 使用当前机台×候选池短生命周期缓存中的时间计划构建提案。
     */
    public NewSpecScheduleProposal previewWithPlan(
            LhScheduleContext context,
            DayScheduleContext dayContext,
            LhShiftConfigVO shift,
            MachineResource machineResource,
            LocalDate poolDate,
            DailyNewSpecCandidate candidate,
            MachineSkuMatchResult matchResult,
            NewSpecMachineAvailabilityPlan availabilityPlan,
            boolean actualAvailableTimeMode) {
        return this.previewWithPlan(
                context, dayContext, shift, machineResource, poolDate,
                candidate, matchResult, availabilityPlan,
                actualAvailableTimeMode, null);
    }

    /**
     * 使用已冻结结构准入结果构建提案，避免同一Machine×SKU重复统计结构机台。
     *
     * @param context 排程上下文
     * @param dayContext 当前业务日
     * @param shift 当前机台资源归属班次
     * @param machineResource 当前机台资源
     * @param poolDate 候选原始日期池
     * @param candidate 当前SKU候选
     * @param matchResult 反向匹配结果
     * @param availabilityPlan 冻结真实时间轴
     * @param actualAvailableTimeMode 是否按实际开产时间归班
     * @param structureLimitDecision 已冻结结构准入结果；为空时现场计算
     * @return 可提交提案；不满足结构或时间轴约束时返回null
     */
    public NewSpecScheduleProposal previewWithPlan(
            LhScheduleContext context,
            DayScheduleContext dayContext,
            LhShiftConfigVO shift,
            MachineResource machineResource,
            LocalDate poolDate,
            DailyNewSpecCandidate candidate,
            MachineSkuMatchResult matchResult,
            NewSpecMachineAvailabilityPlan availabilityPlan,
            boolean actualAvailableTimeMode,
            StructureMachineLimitDecision structureLimitDecision) {
        LhShiftConfigVO productionOccupationShift = Objects.isNull(availabilityPlan)
                ? null : availabilityPlan.getProductionOccupationShift();
        if (Objects.isNull(availabilityPlan) || !availabilityPlan.isAvailable()
                || Objects.isNull(availabilityPlan.getFormalTargetShift())
                || Objects.isNull(availabilityPlan.getFormalTargetShift().getShiftIndex())
                || Objects.isNull(productionOccupationShift)
                || Objects.isNull(productionOccupationShift.getShiftIndex())) {
            if (StringUtils.isEmpty(candidate.getLastFailure())) {
                candidate.setLastFailure(Objects.isNull(availabilityPlan)
                        ? "未形成真实可开产时间计划"
                        : StringUtils.defaultIfEmpty(
                        availabilityPlan.getUnavailableReason(), "完整时间轴无合法可开产班次"));
            }
            return null;
        }
        LhShiftConfigVO competitionTargetShift =
                availabilityPlan.getCompetitionTargetShift();
        if (actualAvailableTimeMode
                && (Objects.isNull(competitionTargetShift)
                || !Objects.equals(shift.getShiftIndex(),
                competitionTargetShift.getShiftIndex()))) {
            return null;
        }
        StructureMachineLimitDecision effectiveStructureLimitDecision =
                Objects.nonNull(structureLimitDecision)
                        ? structureLimitDecision
                        : this.resolveStructureMachineLimitDecision(
                        context, dayContext, candidate, matchResult,
                        availabilityPlan, poolDate, null);
        if (Objects.nonNull(effectiveStructureLimitDecision)
                && effectiveStructureLimitDecision.isApplicable()
                && !effectiveStructureLimitDecision.isAllowed()) {
            candidate.setLastFailure(effectiveStructureLimitDecision.getReason());
            this.logStructureMachineLimitDecision(
                    "PREVIEW_REJECT", effectiveStructureLimitDecision, candidate);
            return null;
        }
        return new NewSpecScheduleProposal(
                candidate, matchResult, shift.getShiftIndex(), poolDate,
                availabilityPlan, actualAvailableTimeMode);
    }

    /**
     * 按提案生产占用班次和候选物理机台统一校验结构机台数上限。
     *
     * @param context 排程上下文
     * @param dayContext 当前业务日
     * @param candidate 当前SKU候选
     * @param matchResult 反向匹配结果
     * @param availabilityPlan 冻结真实时间轴
     * @param poolDate SKU原始候选池日期
     * @param roundCache 当前运行态版本轻量缓存
     * @return 不适用时返回null；否则返回结构准入结果
     */
    public StructureMachineLimitDecision resolveStructureMachineLimitDecision(
            LhScheduleContext context,
            DayScheduleContext dayContext,
            DailyNewSpecCandidate candidate,
            MachineSkuMatchResult matchResult,
            NewSpecMachineAvailabilityPlan availabilityPlan,
            LocalDate poolDate,
            NewSpecProposalRoundCache roundCache) {
        LhShiftConfigVO productionOccupationShift = Objects.isNull(availabilityPlan)
                ? null : availabilityPlan.getProductionOccupationShift();
        if (Objects.isNull(dayContext) || Objects.isNull(candidate)
                || Objects.isNull(candidate.getSku())
                || StringUtils.isEmpty(candidate.getSku().getStructureName())
                || Objects.isNull(matchResult) || Objects.isNull(matchResult.getMachine())
                || Objects.isNull(productionOccupationShift)
                || Objects.isNull(productionOccupationShift.getShiftIndex())) {
            return null;
        }
        EarlyProductionRuntimePlan earlyProductionPlan =
                candidate.getEarlyProductionPreview();
        EarlyProductionDecision earlyProductionDecision =
                Objects.isNull(earlyProductionPlan)
                        ? null : earlyProductionPlan.getDecision();
        return structureMachineLimitAdmissionService.evaluate(
                context, dayContext.getCurrentPhase(), candidate.getSku(),
                earlyProductionDecision,
                productionOccupationShift,
                availabilityPlan.getProductionOccupationStartTime(),
                matchResult.getMachine().getMachineCode(), poolDate, roundCache);
    }

    /**
     * 输出包含候选生命周期状态的结构准入日志。
     *
     * @param checkStage 校验阶段
     * @param decision 结构准入结果
     * @param candidate 当前SKU候选
     */
    public void logStructureMachineLimitDecision(
            String checkStage,
            StructureMachineLimitDecision decision,
            DailyNewSpecCandidate candidate) {
        structureMachineLimitAdmissionService.logDecision(
                checkStage, decision,
                Objects.isNull(candidate)
                        ? null : candidate.getRemainingMachineCount(),
                Objects.isNull(candidate)
                        ? null : candidate.isMachineCompetitionBlocked());
    }
}
