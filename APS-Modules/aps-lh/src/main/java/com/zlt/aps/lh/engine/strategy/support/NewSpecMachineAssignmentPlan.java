package com.zlt.aps.lh.engine.strategy.support;

import com.zlt.aps.lh.api.domain.dto.MachineScheduleDTO;
import com.zlt.aps.lh.api.domain.dto.SkuScheduleDTO;
import com.zlt.aps.lh.util.LhSingleControlMachineUtil;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.time.LocalDate;

/**
 * 单次机台驱动竞争形成的 Machine→SKU 唯一分配计划。
 *
 * <p>每轮只提交一个物理机台组，确保候选 A 失败后可以重新回到同一机台尝试候选 B，
 * 并避免把多台机台一次性交给同一 SKU 后再轮到其他 SKU。</p>
 *
 * @author APS
 */
public final class NewSpecMachineAssignmentPlan {

    /** 结构上限失败键前缀，避免被资源竞争班次的通用提交失败键提前误拦截。 */
    private static final String STRUCTURE_LIMIT_FAILURE_PREFIX = "STRUCTURE_LIMIT|";

    /** 本轮指定 SKU */
    private final DailyNewSpecCandidate candidate;
    /** 反向匹配结果 */
    private final MachineSkuMatchResult matchResult;
    /** 机台资源本轮归属的目标班次 */
    private final Integer targetShiftIndex;
    /** 候选原始日期池 */
    private final LocalDate poolDate;
    /** 试算阶段冻结的真实可开产计划 */
    private final NewSpecMachineAvailabilityPlan availabilityPlan;
    /** 是否按实际可开产时间归属机台资源班次 */
    private final boolean actualAvailableTimeMode;
    /** 对象身份映射，避免同物料补偿 DTO 互相覆盖 */
    private final Map<SkuScheduleDTO, String> assignedMachineCodeMap =
            new IdentityHashMap<SkuScheduleDTO, String>(1);

    public NewSpecMachineAssignmentPlan(DailyNewSpecCandidate candidate,
                                        MachineSkuMatchResult matchResult,
                                        Integer targetShiftIndex,
                                        LocalDate poolDate,
                                        NewSpecMachineAvailabilityPlan availabilityPlan,
                                        boolean actualAvailableTimeMode) {
        this.candidate = Objects.requireNonNull(candidate, "机台驱动候选不能为空");
        this.matchResult = Objects.requireNonNull(matchResult, "反向匹配结果不能为空");
        this.targetShiftIndex = targetShiftIndex;
        this.poolDate = poolDate;
        this.availabilityPlan = availabilityPlan;
        this.actualAvailableTimeMode = actualAvailableTimeMode;
        this.assignedMachineCodeMap.put(candidate.getSku(),
                matchResult.getMachine().getMachineCode());
    }

    public DailyNewSpecCandidate getCandidate() {
        return candidate;
    }

    public MachineSkuMatchResult getMatchResult() {
        return matchResult;
    }

    public Integer getTargetShiftIndex() {
        return targetShiftIndex;
    }

    public LocalDate getPoolDate() {
        return poolDate;
    }

    public NewSpecMachineAvailabilityPlan getAvailabilityPlan() {
        return availabilityPlan;
    }

    public boolean isActualAvailableTimeMode() {
        return actualAvailableTimeMode;
    }

    public boolean isAssigned(SkuScheduleDTO sku) {
        return Objects.nonNull(sku) && assignedMachineCodeMap.containsKey(sku);
    }

    /**
     * 将正向机台匹配结果收口到本轮唯一物理机台。
     *
     * @param sku 当前 SKU
     * @param candidates 正向硬匹配和固定指令排序后的候选
     * @return 仅包含本轮代表机台的列表；不存在时返回空列表
     */
    public List<MachineScheduleDTO> retainAssignedMachine(
            SkuScheduleDTO sku,
            List<MachineScheduleDTO> candidates) {
        if (!this.isAssigned(sku) || candidates == null || candidates.isEmpty()) {
            return Collections.emptyList();
        }
        String assignedMachineCode = assignedMachineCodeMap.get(sku);
        boolean wholeMachineDeclaration = matchResult.getDeclaredMachineCodes().size() > 1;
        String assignedPhysicalMachineCode = wholeMachineDeclaration
                ? LhSingleControlMachineUtil.resolvePhysicalMachineCode(assignedMachineCode)
                : null;
        List<MachineScheduleDTO> retained = new ArrayList<MachineScheduleDTO>(1);
        for (MachineScheduleDTO machine : candidates) {
            if (Objects.isNull(machine) || StringUtils.isEmpty(machine.getMachineCode())) {
                continue;
            }
            boolean sameMachineCode = StringUtils.equals(
                    assignedMachineCode, machine.getMachineCode());
            boolean samePhysicalMachine = wholeMachineDeclaration
                    && StringUtils.equals(
                    assignedPhysicalMachineCode,
                    LhSingleControlMachineUtil.resolvePhysicalMachineCode(
                            machine.getMachineCode()));
            if (sameMachineCode || samePhysicalMachine) {
                retained.add(machine);
                break;
            }
        }
        return retained;
    }

    public String getAssignmentKey() {
        return buildAssignmentKey(
                matchResult, candidate.getSku(), targetShiftIndex);
    }

    /**
     * 根据匹配声明范围构造失败键：单边按运行态编码，整机按物理编码。
     *
     * @param matchResult 反向匹配结果
     * @param sku SKU
     * @param targetShiftIndex 资源目标班次
     * @return 稳定失败键
     */
    public static String buildAssignmentKey(MachineSkuMatchResult matchResult,
                                            SkuScheduleDTO sku,
                                            Integer targetShiftIndex) {
        MachineScheduleDTO machine = Objects.isNull(matchResult)
                ? null : matchResult.getMachine();
        boolean wholeMachineDeclaration = Objects.nonNull(matchResult)
                && matchResult.getDeclaredMachineCodes().size() > 1;
        String machineResourceCode = Objects.isNull(machine) ? ""
                : wholeMachineDeclaration
                ? StringUtils.defaultString(LhSingleControlMachineUtil.resolvePhysicalMachineCode(
                machine.getMachineCode()))
                : StringUtils.defaultString(machine.getMachineCode());
        return buildAssignmentKey(
                machineResourceCode, sku, targetShiftIndex);
    }

    /**
     * 构造按正式目标班次隔离的结构上限失败键。
     *
     * <p>结构准入发生在完整时间轴之后，正式目标班次可能与资源竞争班次不同。独立前缀
     * 防止class5的结构拒绝被通用资源失败键误用于class6及后续班次。</p>
     *
     * @param matchResult 反向匹配结果
     * @param sku 当前SKU
     * @param formalTargetShiftIndex 正式目标班次
     * @return 结构上限失败键
     */
    public static String buildStructureLimitAssignmentKey(
            MachineSkuMatchResult matchResult,
            SkuScheduleDTO sku,
            Integer formalTargetShiftIndex) {
        return STRUCTURE_LIMIT_FAILURE_PREFIX + buildAssignmentKey(
                matchResult, sku, formalTargetShiftIndex);
    }

    /**
     * 构造物理机台与 SKU 的失败去重键。
     *
     * @param machine 机台
     * @param sku SKU
     * @return 稳定失败键
     */
    public static String buildAssignmentKey(MachineScheduleDTO machine, SkuScheduleDTO sku) {
        return buildAssignmentKey(machine, sku, null);
    }

    /**
     * 构造带目标班次的失败去重键。
     *
     * @param machine 机台
     * @param sku SKU
     * @param targetShiftIndex 资源目标班次
     * @return 稳定失败键
     */
    public static String buildAssignmentKey(MachineScheduleDTO machine,
                                            SkuScheduleDTO sku,
                                            Integer targetShiftIndex) {
        String physicalMachineCode = Objects.isNull(machine) ? "" : StringUtils.defaultString(
                LhSingleControlMachineUtil.resolvePhysicalMachineCode(machine.getMachineCode()));
        return buildAssignmentKey(physicalMachineCode, sku, targetShiftIndex);
    }

    private static String buildAssignmentKey(String machineResourceCode,
                                             SkuScheduleDTO sku,
                                             Integer targetShiftIndex) {
        String materialCode = Objects.isNull(sku) ? "" : StringUtils.defaultString(sku.getMaterialCode());
        String productStatus = Objects.isNull(sku) ? "" : StringUtils.defaultString(sku.getProductStatus());
        return StringUtils.defaultString(machineResourceCode) + "|" + materialCode + "|" + productStatus
                + "|class" + (Objects.isNull(targetShiftIndex) ? 0 : targetShiftIndex);
    }
}
