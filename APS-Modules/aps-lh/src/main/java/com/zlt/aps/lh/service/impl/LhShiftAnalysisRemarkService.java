package com.zlt.aps.lh.service.impl;

import com.zlt.aps.lh.api.constant.LhScheduleConstant;
import com.zlt.aps.lh.api.domain.dto.MachineFaultWindowDTO;
import com.zlt.aps.lh.api.domain.dto.MachineCleaningWindowDTO;
import com.zlt.aps.lh.api.domain.dto.MachineScheduleDTO;
import com.zlt.aps.lh.api.domain.entity.LhMouldChangePlan;
import com.zlt.aps.lh.api.domain.entity.LhScheduleResult;
import com.zlt.aps.lh.api.domain.vo.LhShiftConfigVO;
import com.zlt.aps.lh.context.LhScheduleContext;
import com.zlt.aps.lh.engine.strategy.support.LhShiftDelayEvidence;
import com.zlt.aps.lh.engine.strategy.support.LhShiftRemarkRuntime;
import com.zlt.aps.lh.util.LhSingleControlMachineUtil;
import com.zlt.aps.lh.util.ShiftFieldUtil;
import com.zlt.aps.lh.util.CleaningDispositionResolver;
import com.zlt.aps.lh.util.MachineCleaningOverlapUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.BitSet;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 最终班次备注投影，只读取已确认的延后证据、历史来源、故障事实及最终正量。
 * 一次建立机台班次占用索引，不重新调用约束、模具分配或数据库查询。
 *
 * @author APS
 * @date 2026/10/23
 */
@Service
public class LhShiftAnalysisRemarkService {

    /**
     * 正式历史指令的固定备注项。
     */
    private static final String SPECIFIED_MACHINE_ANALYSIS = "指定上机";
    /**
     * 故障与班次有实际交集时展示的固定备注项。
     */
    private static final String TEMPORARY_FAULT_ANALYSIS = "临时性故障维修";
    /**
     * 来源生产行被清理后仍需展示的独立喷砂占用，不代表后料在该班已经生产。
     */
    private static final String SAND_BLAST_CLEANING_ANALYSIS = "喷砂清洗";
    /**
     * 用于固定备注项的完整项识别，不改写旧文本分隔符。
     */
    private static final Pattern ANALYSIS_DELIMITERS = Pattern.compile("[,，;；]");
    /**
     * 沿用硫化正规产品状态的空值口径。
     */
    private static final String FORMAL_PRODUCT_STATUS = "S";

    /**
     * 在全部最终数量及设备摘要确定后追加备注，不创建或删除结果。
     *
     * @param context 本轮最终结果上下文
     */
    public void appendFinalRemarks(LhScheduleContext context) {
        List<LhShiftConfigVO> shifts = context.getScheduleWindowShifts().stream()
                .filter(Objects::nonNull)
                .filter(shift -> shift.getShiftIndex() != null && shift.getShiftIndex() > 0
                        && shift.getShiftIndex() <= LhScheduleConstant.MAX_SHIFT_SLOT_COUNT)
                .sorted(Comparator.comparing(LhShiftConfigVO::getShiftStartDateTime))
                .collect(Collectors.toList());
        Map<String, BitSet> occupied = new HashMap<>();
        Map<LhScheduleResult, LhShiftConfigVO> firstPositiveShifts = new IdentityHashMap<>();
        for (LhScheduleResult result : context.getScheduleResultList()) {
            for (LhShiftConfigVO shift : shifts) {
                if (this.quantity(result, shift.getShiftIndex()) > 0) {
                    occupied.computeIfAbsent(result.getLhMachineCode(), key -> new BitSet())
                            .set(shift.getShiftIndex());
                    occupied.computeIfAbsent(LhSingleControlMachineUtil.resolvePhysicalMachineCode(
                            result.getLhMachineCode()), key -> new BitSet()).set(shift.getShiftIndex());
                    firstPositiveShifts.putIfAbsent(result, shift);
                }
            }
        }
        Set<String> written = new HashSet<>();
        Map<LhScheduleResult, List<LhShiftDelayEvidence>> confirmedEvidence =
                context.getShiftRemarkRuntime().getResultEvidence();
        // 使用最终列表顺序保证多段结果的备注唯一归属，不遍历已撤销的身份映射。
        for (LhScheduleResult result : context.getScheduleResultList()) {
            LhShiftConfigVO firstPositive = firstPositiveShifts.get(result);
            List<LhShiftDelayEvidence> evidenceList = LhShiftRemarkRuntime.merge(
                    confirmedEvidence.get(result),
                    context.getShiftRemarkRuntime().getHistoricalEvidence(context.getPreviousAlternateResultPlanMap().get(result)));
            if (firstPositive == null || evidenceList.isEmpty()) {
                continue;
            }
            for (LhShiftDelayEvidence evidence : evidenceList) {
                if (!this.matches(result, evidence)) {
                    continue;
                }
                BitSet machineOccupied = occupied.get(evidence.getMachineCode());
                for (LhShiftConfigVO shift : shifts) {
                    int shiftIndex = shift.getShiftIndex();
                    if (shift.getShiftStartDateTime().getTime() >= evidence.getBlockedUntil()
                            || shift.getShiftEndDateTime().getTime() <= evidence.getBlockedFrom()
                            || !shift.getShiftStartDateTime().before(firstPositive.getShiftStartDateTime())
                            || this.quantity(result, shiftIndex) > 0
                            || (machineOccupied != null && machineOccupied.get(shiftIndex))) {
                        continue;
                    }
                    String key = result.getLhMachineCode() + "|" + shiftIndex + "|" + evidence.getReason();
                    if (written.add(key)) {
                        this.appendItem(result, shiftIndex, evidence.getReason().getAnalysis());
                    }
                }
            }
        }
        this.appendSpecifiedMachine(context, firstPositiveShifts);
        this.appendTemporaryFaultRemarks(context, shifts, firstPositiveShifts);
        this.appendOrphanSandBlastRemarks(context, shifts, firstPositiveShifts);
    }

    /**
     * 将来源生产行已移除的独立喷砂占用投影到同侧后续结果，避免零量清理丢失空班原因。
     * 只补备注，不生成喷砂首检产量，也不把已取消或离机清洗展示为原机占用。
     *
     * @param context 最终清洗处置及结果
     * @param shifts 本次固定班次窗口
     * @param firstPositiveShifts 最终有效结果的首个有量班次
     */
    private void appendOrphanSandBlastRemarks(LhScheduleContext context, List<LhShiftConfigVO> shifts,
            Map<LhScheduleResult, LhShiftConfigVO> firstPositiveShifts) {
        Map<String, List<LhScheduleResult>> resultsBySide = context.getScheduleResultList().stream()
                .filter(firstPositiveShifts::containsKey)
                .sorted(Comparator.comparing(result -> firstPositiveShifts.get(result).getShiftIndex()))
                .collect(Collectors.groupingBy(LhScheduleResult::getLhMachineCode));
        for (MachineScheduleDTO machine : context.getMachineScheduleMap().values()) {
            List<LhScheduleResult> results = resultsBySide.get(machine.getMachineCode());
            if (Objects.isNull(results)) {
                continue;
            }
            for (MachineCleaningWindowDTO window : machine.getCleaningWindowList()) {
                if (window.isOffMachineCleaning() || !MachineCleaningOverlapUtil.isSandBlastCleaning(window)
                        || StringUtils.isBlank(window.getSourceMaterialCode())
                        || Objects.isNull(window.getCleanStartTime()) || Objects.isNull(window.getReadyTime())
                        || CleaningDispositionResolver.resolve(context, machine.getMachineCode(), window).getDisposition()
                        != CleaningDispositionResolver.Disposition.INDEPENDENT
                        || results.stream().anyMatch(result -> StringUtils.equals(
                        window.getSourceMaterialCode(), result.getMaterialCode())
                        && StringUtils.equals(StringUtils.defaultIfBlank(window.getSourceProductStatus(), FORMAL_PRODUCT_STATUS),
                        StringUtils.defaultIfBlank(result.getProductStatus(), FORMAL_PRODUCT_STATUS)))) {
                    continue;
                }
                // 后续首行在喷砂恢复后才开始生产，才能承载已失去来源行的机台等待事实。
                LhScheduleResult carrier = results.stream().filter(result -> {
                    Date start = ShiftFieldUtil.getShiftStartTime(result,
                            firstPositiveShifts.get(result).getShiftIndex());
                    return Objects.nonNull(start) && !start.before(window.getReadyTime());
                }).findFirst().orElse(null);
                if (Objects.isNull(carrier)) {
                    continue;
                }
                // 清洗跨班仍沿用最后一个重叠班次展示一次的规则，不为补备注改变原归属口径。
                shifts.stream().filter(shift -> shift.getShiftStartDateTime().before(window.getReadyTime())
                        && shift.getShiftEndDateTime().after(window.getCleanStartTime()))
                        .max(Comparator.comparing(LhShiftConfigVO::getShiftEndDateTime))
                        .ifPresent(shift -> this.appendItem(carrier, shift.getShiftIndex(), SAND_BLAST_CLEANING_ANALYSIS));
            }
        }
    }

    /**
     * 将独立故障事实投影到最终结果的相交班次，空班及部分停机班均保留说明。
     *
     * @param context 含原始故障事实的最终上下文
     * @param shifts 本轮实际班次
     * @param firstPositiveShifts 最终结果的首个正量班次
     */
    private void appendTemporaryFaultRemarks(LhScheduleContext context, List<LhShiftConfigVO> shifts,
            Map<LhScheduleResult, LhShiftConfigVO> firstPositiveShifts) {
        if (context.getTemporaryFaultWindowMap().isEmpty()) {
            return;
        }
        Map<String, List<LhScheduleResult>> resultsBySide = context.getScheduleResultList().stream()
                .filter(firstPositiveShifts::containsKey)
                .sorted(Comparator.comparing(result -> firstPositiveShifts.get(result).getShiftIndex()))
                .collect(Collectors.groupingBy(LhScheduleResult::getLhMachineCode));
        for (Map.Entry<String, List<LhScheduleResult>> entry : resultsBySide.entrySet()) {
            String physicalMachineCode = LhSingleControlMachineUtil.resolvePhysicalMachineCode(entry.getKey());
            List<MachineFaultWindowDTO> faults = context.getTemporaryFaultWindowMap().values().stream()
                    .filter(fault -> Objects.equals(physicalMachineCode,
                            LhSingleControlMachineUtil.resolvePhysicalMachineCode(fault.getMachineCode())))
                    .collect(Collectors.toList());
            for (LhShiftConfigVO shift : shifts) {
                boolean affected = faults.stream().anyMatch(fault ->
                        fault.getStartTime().before(shift.getShiftEndDateTime())
                                && fault.getEndTime().after(shift.getShiftStartDateTime()));
                if (!affected) {
                    continue;
                }
                // 同侧同班仅选一条承载结果；原续作行被裁空移除后仍可由后续物料展示机台故障。
                LhScheduleResult carrier = this.resolveFaultRemarkCarrier(
                        entry.getValue(), firstPositiveShifts, shift.getShiftIndex());
                this.appendItem(carrier, shift.getShiftIndex(), TEMPORARY_FAULT_ANALYSIS);
            }
        }
    }

    /**
     * 为同一运行侧的故障备注选择承载行：当班有量优先，其次后续首行，最后前序末行。
     *
     * @param results 按首个正量班次升序排列的同侧有效结果，至少一条
     * @param firstPositiveShifts 最终结果的首个正量班次
     * @param shiftIndex 故障相交班次
     * @return 唯一承载结果，不新增零产量行
     */
    private LhScheduleResult resolveFaultRemarkCarrier(List<LhScheduleResult> results,
            Map<LhScheduleResult, LhShiftConfigVO> firstPositiveShifts, int shiftIndex) {
        LhScheduleResult producing = results.stream()
                .filter(result -> this.quantity(result, shiftIndex) > 0).findFirst().orElse(null);
        if (Objects.nonNull(producing)) {
            return producing;
        }
        return results.stream()
                .filter(result -> firstPositiveShifts.get(result).getShiftIndex() > shiftIndex)
                .findFirst().orElse(results.get(results.size() - 1));
    }

    /**
     * 核对最终结果的物料状态与实际作用侧，整机指令才允许关联两侧。
     *
     * @param result 最终有效结果
     * @param evidence 已确认的拒绝附件
     * @return 身份及作用范围是否仍一致
     */
    private boolean matches(LhScheduleResult result, LhShiftDelayEvidence evidence) {
        String scope = evidence.getMachineCode();
        return Objects.equals(evidence.getMaterialCode(), result.getMaterialCode())
                && StringUtils.equals(StringUtils.defaultIfBlank(evidence.getProductStatus(), FORMAL_PRODUCT_STATUS),
                StringUtils.defaultIfBlank(result.getProductStatus(), FORMAL_PRODUCT_STATUS))
                && (Objects.equals(scope, result.getLhMachineCode())
                || (Objects.equals(scope, LhSingleControlMachineUtil.resolvePhysicalMachineCode(scope))
                && Objects.equals(scope, LhSingleControlMachineUtil.resolvePhysicalMachineCode(result.getLhMachineCode()))));
    }

    /**
     * 按正式历史任务与实际侧位选择最终首个正量班，整机拆两侧分别标首次。
     *
     * @param context 正式历史来源映射
     * @param firstPositiveShifts 最终有效结果的首个正量班次索引
     */
    private void appendSpecifiedMachine(LhScheduleContext context,
            Map<LhScheduleResult, LhShiftConfigVO> firstPositiveShifts) {
        Map<LhMouldChangePlan, Map<String, LhScheduleResult>> earliestByPlan = new IdentityHashMap<>();
        for (LhScheduleResult result : context.getScheduleResultList()) {
            LhMouldChangePlan plan = context.getPreviousAlternateResultPlanMap().get(result);
            LhShiftConfigVO shift = firstPositiveShifts.get(result);
            if (plan == null || shift == null || !context.matchesPreviousAlternateAfterMaterial(
                    plan, result.getMaterialCode(), result.getProductStatus())) {
                continue;
            }
            String scope = plan.getLhMachineCode();
            if (!Objects.equals(scope, result.getLhMachineCode())
                    && !(Objects.equals(scope, LhSingleControlMachineUtil.resolvePhysicalMachineCode(scope))
                    && Objects.equals(scope, LhSingleControlMachineUtil.resolvePhysicalMachineCode(result.getLhMachineCode())))) {
                continue;
            }
            Map<String, LhScheduleResult> bySide = earliestByPlan.computeIfAbsent(plan, key -> new HashMap<>());
            String sideKey = result.getLhMachineCode() + "|" + result.getMaterialCode() + "|"
                    + StringUtils.defaultIfBlank(result.getProductStatus(), FORMAL_PRODUCT_STATUS);
            LhScheduleResult previous = bySide.get(sideKey);
            if (previous == null || shift.getShiftStartDateTime().before(
                    firstPositiveShifts.get(previous).getShiftStartDateTime())) {
                bySide.put(sideKey, result);
            }
        }
        earliestByPlan.values().forEach(bySide -> bySide.values().forEach(result ->
                this.appendItem(result, firstPositiveShifts.get(result).getShiftIndex(), SPECIFIED_MACHINE_ANALYSIS)));
    }

    /**
     * 读取动态班次的最终计划量。
     *
     * @param result 最终结果
     * @param shiftIndex 实际字段槽位
     * @return 空值按零处理的计划量
     */
    private int quantity(LhScheduleResult result, int shiftIndex) {
        Integer quantity = ShiftFieldUtil.getShiftPlanQty(result, shiftIndex);
        return quantity == null ? 0 : quantity;
    }

    /**
     * 对固定新增备注执行完整项去重，再复用既有追加工具保留旧文本。
     *
     * @param result 承载结果
     * @param shiftIndex 班次字段槽位
     * @param item 本次固定备注项
     */
    private void appendItem(LhScheduleResult result, int shiftIndex, String item) {
        String current = ShiftFieldUtil.getShiftAnalysis(result, shiftIndex);
        if (StringUtils.isNotBlank(current) && Arrays.stream(ANALYSIS_DELIMITERS.split(current))
                .map(String::trim).anyMatch(item::equals)) {
            return;
        }
        ShiftFieldUtil.appendShiftAnalysis(result, shiftIndex, item);
    }
}
