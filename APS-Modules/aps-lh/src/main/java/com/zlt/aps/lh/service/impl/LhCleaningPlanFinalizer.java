package com.zlt.aps.lh.service.impl;
import com.zlt.aps.lh.api.constant.*;
import com.zlt.aps.lh.api.domain.dto.*;
import com.zlt.aps.lh.api.domain.entity.*;
import com.zlt.aps.lh.api.domain.vo.LhShiftConfigVO;
import com.zlt.aps.lh.api.enums.*;
import com.zlt.aps.lh.context.LhScheduleContext;
import com.zlt.aps.lh.util.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
/**
 * 清洗最终输出组件，在原后处理时点核验喷砂首检、终裁处置、摘要、交替类型和日期回填。
 * 不重新排量，不消费生产账本，不替代交替计划公共编号和前料滚动能力。
 *
 * @author APS
 * @since 2026/10/19
 */
@Slf4j
@Component
public class LhCleaningPlanFinalizer {
    /**
     * 复用无状态清洗组合解析器。
     */
    private final LhCleaningOverlapResolver cleaningOverlapResolver = new LhCleaningOverlapResolver();
    /**
     * 现有清洗规则常量：1。
     */
    private static final int ENABLED = 1;
    /**
     * 现有清洗规则常量：0。
     */
    private static final String CLEANING_DATA_SOURCE_MANUAL = "0";
    /**
     * 现有清洗规则常量：模具置换。
     */
    private static final String MOULD_REPLACEMENT_REMARK = "模具置换";
    /**
     * 现有清洗规则常量：换模+喷砂。
     */
    private static final String MOULD_CHANGE_AND_SAND_BLAST_REMARK = "换模+喷砂";
    /**
     * 现有清洗规则常量：干冰清洗+收尾。
     */
    private static final String DRY_ICE_ENDING_ANALYSIS = "干冰清洗+收尾";
    /**
     * 现有清洗规则常量：喷砂清洗+收尾。
     */
    private static final String SAND_BLAST_ENDING_ANALYSIS = "喷砂清洗+收尾";
    /**
     * 现有清洗规则常量：喷砂清洗首检。
     */
    private static final String SAND_BLAST_FIRST_INSPECTION_ANALYSIS = "喷砂清洗首检";
    /**
     * 新增链在实际动作确定前可能遗留的喷砂换模备注。
     */
    private static final String SAND_BLAST_MOULD_CHANGE_ANALYSIS = "喷砂清洗+换模";
    /**
     * 现有清洗规则常量：干冰清洗+换活字块。
     */
    private static final String TYPE_BLOCK_DRY_ICE_CLEANING_ANALYSIS = "干冰清洗+换活字块";
    /**
     * 现有清洗规则常量：喷砂清洗+换活字块。
     */
    private static final String TYPE_BLOCK_SAND_BLAST_CLEANING_ANALYSIS = "喷砂清洗+换活字块";
    /**
     * 核验主链已经分配的喷砂首检并绑定摘要，不新增数量、不从其他班次挪量。
     * <p>清洗、首检和正常生产的真实结束时间已由共享时间内核计算；这里只将首个
     * 有量班次的展示起点对齐到首检起点，并记录可对账的数量与时间。</p>
     *
     * @param context 排程上下文
     */
    public void applySandBlastFirstInspectionToFinalResults(LhScheduleContext context) {
        if (Objects.isNull(context) || CollectionUtils.isEmpty(context.getScheduleResultList())
                || CollectionUtils.isEmpty(context.getScheduleWindowShifts())) {
            return;
        }
        List<Map.Entry<MachineScheduleDTO, MachineCleaningWindowDTO>> items = collectCleaningPlanItems(context);
        Set<String> processed = new HashSet<>(items.size());
        for (Map.Entry<MachineScheduleDTO, MachineCleaningWindowDTO> item : items) {
            MachineCleaningWindowDTO window = item.getValue();
            String machineCode = resolveCleaningMachineCode(item.getKey(), window);
            if (window.isOffMachineCleaning() || !MachineCleaningOverlapUtil.isSandBlastCleaning(window)
                    || Objects.isNull(window.getSandBlastFirstInspectionQty())
                    || window.getSandBlastFirstInspectionQty() <= 0 || Objects.isNull(window.getReadyTime())) {
                continue;
            }
            String eventKey = window.getSourcePlanId() + "|" + machineCode + "|" + window.getReadyTime().getTime();
            if (!processed.add(eventKey)
                    || CleaningDispositionResolver.resolve(context, machineCode, window).getDisposition()
                    != CleaningDispositionResolver.Disposition.INDEPENDENT) {
                continue;
            }
            boolean combinedWithTypeBlock = this.applySandBlastTypeBlockAnalysis(context, machineCode, window);
            Date countingTime = window.getSandBlastRepairPrecisionPlanId() == null
                    ? window.getReadyTime() : window.getSandBlastInspectionShiftStartTime();
            LhShiftConfigVO shift = LhScheduleTimeUtil.resolveShiftByTime(
                    context.getScheduleWindowShifts(), countingTime);
            if (Objects.isNull(shift)) {
                continue;
            }
            // 只认主链实际覆盖首检完成点的有量结果，禁止从后续结果借量承接首检。
            List<LhScheduleResult> candidates = context.getScheduleResultList().stream()
                    .filter(Objects::nonNull)
                    .filter(result -> StringUtils.equals(machineCode, result.getLhMachineCode()))
                    .filter(result -> this.isSandBlastInspectionAllocated(result, shift, window))
                    .sorted(this::compareResultTime)
                    .collect(Collectors.toList());
            if (CollectionUtils.isEmpty(candidates)) {
                log.info("喷砂首检未形成实际产量, batchNo: {}, 计划日期: {}, 机台: {}, 喷砂序号: {}, 首检结束: {}",
                        context.getBatchNo(), LhScheduleTimeUtil.formatDate(window.getSourcePlanStartTime()),
                        machineCode, window.getSandBlastSequence(),
                                LhScheduleTimeUtil.formatDateTime(window.getReadyTime()));
                continue;
            }
            this.bindSandBlastInspectionSummary(context, candidates.get(0), shift, item.getKey(), window,
                    combinedWithTypeBlock);
        }
    }
    /**
     * 按实际换活字块动作统一同一喷砂事件的组合备注，只在准备动作所属班次展示一次。
     * 新增历史承接可能先按换模构建结果再确定实际动作，因此在最终结果阶段修正名称；
     * 喷砂首检仍沿原时间轴核验，后续班次不再为同一组合事件重复追加首检说明。
     *
     * @param context 已确定实际动作及正量结果的排程上下文
     * @param machineCode 喷砂来源运行侧机台
     * @param window 当前独立执行的喷砂事件
     * @return 已在重叠换活字块动作班次绑定组合备注时返回 true
     */
    private boolean applySandBlastTypeBlockAnalysis(LhScheduleContext context, String machineCode,
                                                     MachineCleaningWindowDTO window) {
        List<LhScheduleResult> candidates = context.getScheduleResultList().stream()
                .filter(Objects::nonNull)
                .filter(result -> StringUtils.equals(machineCode, result.getLhMachineCode()))
                .filter(result -> "1".equals(result.getIsChangeMould()) && "1".equals(result.getIsTypeBlock()))
                .filter(result -> Objects.nonNull(result.getDailyPlanQty()) && result.getDailyPlanQty() > 0)
                .filter(result -> Objects.nonNull(result.getMouldChangeStartTime()))
                .sorted(Comparator.comparing(LhScheduleResult::getMouldChangeStartTime))
                .collect(Collectors.toList());
        for (LhScheduleResult result : candidates) {
            Date switchStart = result.getMouldChangeStartTime();
            Date switchEnd = LhScheduleTimeUtil.addHours(switchStart,
                    LhScheduleTimeUtil.getTypeBlockChangeTotalHours(context));
            if (!MachineCleaningOverlapUtil.isOverlap(window, switchStart, switchEnd)) {
                continue;
            }
            int analysisShift = MachineCleaningOverlapUtil.resolveLastOverlapShiftIndex(
                    context.getScheduleWindowShifts(), switchStart, switchEnd);
            if (analysisShift <= 0) {
                continue;
            }
            Date cleanEnd = MachineCleaningOverlapUtil.resolveEffectiveCleanEndTime(window);
            Date cleanupEnd = switchEnd.after(cleanEnd) ? switchEnd : cleanEnd;
            // 等待喷砂曾把组合备注写到后续班次，按本事件范围移回准备班次；只移除组合项。
            context.getScheduleWindowShifts().stream()
                    .filter(shift -> shift.getShiftStartDateTime().before(cleanupEnd)
                            && shift.getShiftEndDateTime().after(switchStart))
                    .forEach(shift -> {
                        ShiftFieldUtil.removeShiftAnalysis(result, shift.getShiftIndex(),
                                SAND_BLAST_MOULD_CHANGE_ANALYSIS);
                        ShiftFieldUtil.removeShiftAnalysis(result, shift.getShiftIndex(),
                                TYPE_BLOCK_SAND_BLAST_CLEANING_ANALYSIS);
                    });
            ShiftFieldUtil.appendShiftAnalysis(result, analysisShift, TYPE_BLOCK_SAND_BLAST_CLEANING_ANALYSIS);
            this.removeCombinedSandBlastInspectionAnalysis(context, machineCode, window, candidates);
            log.info("喷砂换活字块组合备注归属, batchNo: {}, 机台: {}, 物料: {}, 来源停机计划ID: {}, "
                            + "换活字块开始: {}, 换活字块结束: {}, 备注班次: {}, 后续喷砂首检不重复备注: true",
                    context.getBatchNo(), machineCode, result.getMaterialCode(), window.getSourcePlanId(),
                    LhScheduleTimeUtil.formatDateTime(switchStart), LhScheduleTimeUtil.formatDateTime(switchEnd),
                    analysisShift);
            return true;
        }
        return false;
    }
    /**
     * 清理组合喷砂事件在专用首检班次遗留的重复说明，保留同班其他来源的独立喷砂首检。
     *
     * @param context 最终排程结果及本侧清洗事件
     * @param machineCode 当前喷砂运行侧机台
     * @param window 已绑定组合备注的喷砂事件
     * @param switchResults 本侧已提交正量换活字块结果
     */
    private void removeCombinedSandBlastInspectionAnalysis(LhScheduleContext context, String machineCode,
            MachineCleaningWindowDTO window, List<LhScheduleResult> switchResults) {
        LhShiftConfigVO inspectionShift = LhScheduleTimeUtil.resolveShiftByTime(
                context.getScheduleWindowShifts(), window.getReadyTime());
        if (Objects.isNull(inspectionShift)) {
            return;
        }
        // 原因字段没有事件主键；同班存在另一来源的独立首检时，不能移除它仍需展示的同名原因。
        boolean hasIndependentInspection = cleaningOverlapResolver.resolveMachineWindows(context, machineCode).stream()
                .filter(other -> !other.isOffMachineCleaning() && MachineCleaningOverlapUtil.isSandBlastCleaning(other))
                .filter(other -> !Objects.equals(other.getSourcePlanId(), window.getSourcePlanId())
                        || !Objects.equals(other.getReadyTime(), window.getReadyTime()))
                .filter(other -> Objects.nonNull(other.getSandBlastFirstInspectionQty())
                        && other.getSandBlastFirstInspectionQty() > 0)
                .filter(other -> Objects.equals(inspectionShift, LhScheduleTimeUtil.resolveShiftByTime(
                        context.getScheduleWindowShifts(), other.getReadyTime())))
                .anyMatch(other -> switchResults.stream().noneMatch(result -> MachineCleaningOverlapUtil.isOverlap(
                        other, result.getMouldChangeStartTime(), LhScheduleTimeUtil.addHours(
                                result.getMouldChangeStartTime(),
                                LhScheduleTimeUtil.getTypeBlockChangeTotalHours(context)))));
        if (hasIndependentInspection) {
            return;
        }
        context.getScheduleResultList().stream().filter(Objects::nonNull)
                .filter(result -> StringUtils.equals(machineCode, result.getLhMachineCode()))
                .forEach(result -> ShiftFieldUtil.removeShiftAnalysis(result, inspectionShift.getShiftIndex(),
                        SAND_BLAST_FIRST_INSPECTION_ANALYSIS));
    }
    /**
     * 判断本班结果是否在首检之前进入生产链并覆盖首检完成点。
     *
     * @param result 排程结果
     * @param shift 首检归属班次
     * @param window 喷砂窗口
     * @return 是否承接首检
     */
    private boolean isSandBlastInspectionAllocated(LhScheduleResult result, LhShiftConfigVO shift,
                                                   MachineCleaningWindowDTO window) {
        Integer qty = ShiftFieldUtil.getShiftPlanQty(result, shift.getShiftIndex());
        Date start = ShiftFieldUtil.getShiftStartTime(result, shift.getShiftIndex());
        Date end = ShiftFieldUtil.getShiftEndTime(result, shift.getShiftIndex());
        return Objects.nonNull(qty) && qty > 0 && Objects.nonNull(start) && Objects.nonNull(end)
                && !CollectionUtils.isEmpty(CleaningScheduleRuleUtil.resolveSandBlastInspectionWindows(
                        Collections.singletonList(window), start, shift.getShiftEndDateTime()))
                && (start.before(window.getReadyTime())
                    || (start.equals(window.getSandBlastInspectionShiftStartTime())
                        && Objects.nonNull(this.resolveProductionStartTime(result))
                        && this.resolveProductionStartTime(result).before(window.getReadyTime())))
                && !end.before(window.getReadyTime());
    }
    /**
     * 绑定喷砂首检说明并输出正常生产量，数量及真实结束时间保持主链结果。
     *
     * @param context 排程上下文
     * @param result 承接结果
     * @param shift 归属班次
     * @param machine 清洗事件所在的运行机台
     * @param window 清洗及首检事件
     * @param combinedWithTypeBlock 当前喷砂事件是否已在换活字块动作班次统一备注
     */
    private void bindSandBlastInspectionSummary(LhScheduleContext context, LhScheduleResult result,
                                                LhShiftConfigVO shift, MachineScheduleDTO machine,
                                                MachineCleaningWindowDTO window, boolean combinedWithTypeBlock) {
        int shiftIndex = shift.getShiftIndex();
        Date inspectionStart = MachineCleaningOverlapUtil.resolveSandBlastInspectionStartTime(window);
        int planQty = ShiftFieldUtil.getShiftPlanQty(result, shiftIndex);
        Date originalStart = ShiftFieldUtil.getShiftStartTime(result, shiftIndex);
        // 默认喷砂结束班次在首检前没有生产；确有清洗前生产时保留原起点。
        List<MachineCleaningWindowDTO> cleaningWindows = machine.getCleaningWindowList();
        List<MachineMaintenanceWindowDTO> maintenanceWindows =
                ShiftCapacityResolverUtil.resolveCapacityMaintenanceWindowList(
                context, context.getDevicePlanShutList(), result.getLhMachineCode(),
                machine.getMaintenanceWindowList());
        long beforeSeconds = ShiftCapacityResolverUtil.resolveNetProductiveSeconds(
                context.getDevicePlanShutList(), cleaningWindows, maintenanceWindows,
                result.getLhMachineCode(), originalStart, inspectionStart);
        int baseCapacity = ShiftCapacityResolverUtil.resolveActualShiftPlanQty(
                Objects.isNull(result.getSingleMouldShiftQty()) ? 0 : result.getSingleMouldShiftQty(), shift,
                ShiftCapacityResolverUtil.resolveOddShiftCapacityPlusShiftType(context), result.getScheduleType());
        int beforeQty = ShiftCapacityResolverUtil.resolveShiftCapacity(baseCapacity,
                Objects.isNull(result.getLhTime()) ? 0 : result.getLhTime(),
                Objects.isNull(result.getMouldQty()) ? 0 : result.getMouldQty(),
                ShiftCapacityResolverUtil.resolveShiftDurationSeconds(shift), beforeSeconds);
        int inspectionQty = Math.min(Math.max(0, planQty - beforeQty),
                ShiftCapacityResolverUtil.resolveSandBlastInspectionQty(context, window));
        if (beforeSeconds <= 0) {
            ShiftFieldUtil.setShiftPlanQty(result, shiftIndex, planQty,
                    inspectionStart, ShiftFieldUtil.getShiftEndTime(result, shiftIndex));
        }
        // 组合事件已在准备班次展示，实际首检数量和时间核验继续执行，仅抑制同源重复备注。
        if (!combinedWithTypeBlock) {
            ShiftFieldUtil.appendShiftAnalysis(result, shiftIndex, SAND_BLAST_FIRST_INSPECTION_ANALYSIS);
        }
        log.info("喷砂班次产量核验, batchNo: {}, 计划日期: {}, 机台: {}, SKU: {}, 当天喷砂序号: {}, "
                        + "喷砂开始: {}, 喷砂结束: {}, 首检开始: {}, 首检结束: {}, "
                        + "首检条数: {}, 首检完成后正常生产量: {}, 最终班次计划量: {}, 班次: {}",
                context.getBatchNo(), LhScheduleTimeUtil.formatDate(window.getSourcePlanStartTime()),
                result.getLhMachineCode(), result.getMaterialCode(), window.getSandBlastSequence(),
                LhScheduleTimeUtil.formatDateTime(window.getCleanStartTime()),
                LhScheduleTimeUtil.formatDateTime(window.getCleanEndTime()),
                LhScheduleTimeUtil.formatDateTime(inspectionStart),
                LhScheduleTimeUtil.formatDateTime(window.getReadyTime()), inspectionQty,
                Math.max(0, planQty - beforeQty - inspectionQty), planQty, shiftIndex);
    }
    /**
     * 按机台、实际交接时间及清洗类型识别本次已执行的离机清洗计划。
     *
     * @param context 排程上下文
     * @param plan 待保存交替计划
     * @return 对应清洗事件；普通交替计划返回 null
     */
    public MachineCleaningWindowDTO resolveOffMachineCleaningEvent(LhScheduleContext context, LhMouldChangePlan plan) {
        if (Objects.isNull(plan)) {
            return null;
        }
        MachineCleaningWindowDTO event = context.getContinuationSandBlastWindowMap().get(plan.getLhMachineCode());
        return Objects.nonNull(event) && event.isOffMachineCleaning()
                && StringUtils.equals(plan.getChangeMouldType(), this.resolveSandBlastPlanType(event))
                && Objects.equals(event.getCleanStartTime(), plan.getChangeTime())
                && StringUtils.equals(event.getContinuationMaterialCode(), plan.getBeforeMaterialCode())
                && StringUtils.equals(plan.getBeforeMaterialCode(), plan.getAfterMaterialCode()) ? event : null;
    }
    /**
     * 判断交替计划是否必须保留喷砂交接中的精确模具。
     *
     * @param context 排程上下文
     * @param plan 当前交替计划
     * @param productStatus 后料产品状态
     * @return true-交接事件或释放后正规换模，禁止派生模具号
     */
    public boolean isSandBlastExactMouldPlan(
            LhScheduleContext context, LhMouldChangePlan plan, String productStatus) {
        if (Objects.nonNull(this.resolveOffMachineCleaningEvent(context, plan))) {
            return true;
        }
        if (Objects.isNull(plan)) {
            return false;
        }
        if (MouldChangeTypeEnum.containsCode(
                plan.getChangeMouldType(), MouldChangeTypeEnum.SAND_BLAST.getCode())) {
            // 喷砂清洗交替计划的模具号按业务口径固定为空，禁止关系模具回填。
            return true;
        }
        if (!MouldChangeTypeEnum.containsCode(
                plan.getChangeMouldType(), MouldChangeTypeEnum.REGULAR.getCode())
                && !MouldChangeTypeEnum.containsCode(
                plan.getChangeMouldType(), MouldChangeTypeEnum.TYPE_BLOCK.getCode())) {
            return false;
        }
        return Objects.nonNull(this.resolveOnlySandBlastSourcePlanEvent(context, plan))
                || Objects.nonNull(this.resolveOnlySandBlastReceivingPlanEvent(
                context, plan, productStatus));
    }
    /**
     * 解析喷砂交接计划类型，置换为换模，原模具离机清洗为喷砂。
     *
     * @param event 已确认交接事件
     * @return 交替类型编码
     */
    public String resolveSandBlastPlanType(MachineCleaningWindowDTO event) {
        return StringUtils.isNotEmpty(event.getReplacementMouldCode())
                ? MouldChangeTypeEnum.REGULAR.getCode() : MouldChangeTypeEnum.SAND_BLAST.getCode();
    }
    /**
     * 解析交接计划模具号，置换记录新模具，喷砂清洗记录固定为空。
     *
     * @param event 已确认交接事件
     * @return 精确模具号
     */
    public String resolveSandBlastPlanMouldCode(MachineCleaningWindowDTO event) {
        return StringUtils.isNotEmpty(event.getReplacementMouldCode())
                ? event.getReplacementMouldCode() : "";
    }
    /**
     * 对仅喷砂释放原机台的实际交替计划补充备注并清空模具号。
     *
     * @param context 排程上下文
     * @param plan 当前实际交替计划
     */
    public void applyOnlySandBlastSourcePlanPresentation(
            LhScheduleContext context, LhMouldChangePlan plan) {
        if (Objects.isNull(plan)
                || Objects.isNull(this.resolveOnlySandBlastSourcePlanEvent(context, plan))) {
            return;
        }
        plan.setCleanType(MouldChangeTypeEnum.SAND_BLAST.getCode());
        plan.setMouldCode("");
        String existingRemark = plan.getRemark();
        plan.setRemark(StringUtils.isNotEmpty(existingRemark)
                ? existingRemark + "；" + MOULD_CHANGE_AND_SAND_BLAST_REMARK
                : MOULD_CHANGE_AND_SAND_BLAST_REMARK);
    }
    /**
     * 识别仅喷砂释放原机台在触发后的首类实际交替计划。
     *
     * @param context 排程上下文
     * @param plan 待判断计划
     * @return 对应离机喷砂事件；非原机交替返回null
     */
    public MachineCleaningWindowDTO resolveOnlySandBlastSourcePlanEvent(
            LhScheduleContext context, LhMouldChangePlan plan) {
        if (Objects.isNull(context) || Objects.isNull(plan)
                || StringUtils.isEmpty(plan.getLhMachineCode())
                || Objects.isNull(plan.getPlanDate())) {
            return null;
        }
        MachineCleaningWindowDTO event = context.getOnlySandBlastContinuationReleaseWindowMap()
                .get(plan.getLhMachineCode());
        return Objects.nonNull(event)
                && CleaningDispositionResolver.resolve(context, plan.getLhMachineCode(), event).getDisposition()
                == CleaningDispositionResolver.Disposition.INDEPENDENT
                && Objects.nonNull(event.getCleanStartTime())
                && !plan.getPlanDate().before(event.getCleanStartTime())
                && StringUtils.equals(event.getContinuationMaterialCode(), plan.getBeforeMaterialCode())
                && !StringUtils.equals(plan.getBeforeMaterialCode(), plan.getAfterMaterialCode())
                ? event : null;
    }
    /**
     * 识别仅喷砂释放 SKU 在其他机台形成的实际接收计划。
     *
     * @param context 排程上下文
     * @param plan 待判断计划
     * @param productStatus 后物料产品状态
     * @return 实际模具整组匹配的离机喷砂事件；普通同物料计划返回null
     */
    public MachineCleaningWindowDTO resolveOnlySandBlastReceivingPlanEvent(
            LhScheduleContext context, LhMouldChangePlan plan, String productStatus) {
        if (Objects.isNull(context) || Objects.isNull(plan)
                || StringUtils.isEmpty(plan.getLhMachineCode())
                || Objects.isNull(plan.getPlanDate())
                || CollectionUtils.isEmpty(context.getOnlySandBlastContinuationReleaseWindowMap())) {
            return null;
        }
        String targetPhysicalMachine = LhSingleControlMachineUtil.resolvePhysicalMachineCode(
                plan.getLhMachineCode());
        return context.getOnlySandBlastContinuationReleaseWindowMap().entrySet().stream()
                .filter(entry -> Objects.nonNull(entry.getValue()))
                .filter(entry -> !StringUtils.equals(targetPhysicalMachine,
                        LhSingleControlMachineUtil.resolvePhysicalMachineCode(entry.getKey())))
                .map(Map.Entry::getValue)
                .filter(event -> Objects.nonNull(event.getCleanEndTime())
                        && !plan.getPlanDate().before(event.getCleanEndTime()))
                .filter(event -> StringUtils.equals(
                        event.getContinuationMaterialCode(), plan.getAfterMaterialCode()))
                .filter(event -> StringUtils.equals(
                        this.normalizeProductStatus(event.getContinuationProductStatus()),
                        this.normalizeProductStatus(productStatus)))
                // 同物料普通补机可能使用其他模具，只有实际接走该事件整组原模具才属于喷砂接收。
                .filter(event -> StringUtils.isNotEmpty(event.getMouldCode())
                        && LhMouldCodeUtil.splitMouldCode(event.getMouldCode())
                        .equals(LhMouldCodeUtil.splitMouldCode(plan.getMouldCode())))
                .min(Comparator.comparing(MachineCleaningWindowDTO::getCleanEndTime))
                .orElse(null);
    }
    /**
     * 基于清洗窗口追加交替计划，同SKU模具置换仅输出换模动作。
     *
     * @param context 排程上下文
     * @param plans 模具交替计划列表
     * @param planOrder 当前计划顺序
     * @param outputSupport 原交替计划公共字段及对账适配
     * @param planProductStatusMap 交替计划对应产品状态
     * @return 下一个计划顺序
     */
    public int appendCleaningMouldChangePlans(LhScheduleContext context,
                                               List<LhMouldChangePlan> plans,
                                               int planOrder,
                                               PlanOutputSupport outputSupport,
                                               Map<LhMouldChangePlan, String> planProductStatusMap) {
        List<Map.Entry<MachineScheduleDTO, MachineCleaningWindowDTO>> cleaningPlanItems =
                collectCleaningPlanItems(context);
        for (Map.Entry<MachineScheduleDTO, MachineCleaningWindowDTO> item : cleaningPlanItems) {
            MachineScheduleDTO machine = item.getKey();
            MachineCleaningWindowDTO cleaningWindow = item.getValue();
            String machineCode = resolveCleaningMachineCode(machine, cleaningWindow);
            // 同一处置同时决定普通清洗和离机喷砂，取消后不再绑定清洗类型。
            CleaningDispositionResolver.Decision decision = CleaningDispositionResolver.resolve(
                    context, machineCode, cleaningWindow);
            LhScheduleResult endingResult = decision.getEndingResult();
            if (Objects.nonNull(endingResult)) {
                log.info("清洗交替计划跳过，最终结果命中三天内收尾, 机台: {}, 物料: {}, 清洗类型: {}, "
                                + "来源停机计划ID: {}, 收尾时间: {}",
                        machineCode, endingResult.getMaterialCode(), cleaningWindow.getCleanType(),
                        cleaningWindow.getSourcePlanId(),
                        LhScheduleTimeUtil.formatDateTime(endingResult.getSpecEndTime()));
                continue;
            }
            if (decision.getDisposition() == CleaningDispositionResolver.Disposition.MOULD_CHANGE_CANCELLED) {
                log.info("清洗交替计划取消，只保留实际正规换模, 机台: {}, 清洗类型: {}, "
                                + "来源停机计划ID: {}, 清洗开始: {}",
                        machineCode, cleaningWindow.getCleanType(), cleaningWindow.getSourcePlanId(),
                        LhScheduleTimeUtil.formatDateTime(cleaningWindow.getCleanStartTime()));
                continue;
            }
            if (cleaningWindow.isOffMachineCleaning()
                    && StringUtils.isEmpty(cleaningWindow.getReplacementMouldCode())) {
                // 无替换模具时，清洗事实并入原机台实际交替计划；不得额外生成03喷砂计划。
                this.appendOnlySandBlastRequeueProcessLog(
                        context, plans, planProductStatusMap, machineCode, cleaningWindow);
                continue;
            }
            LhMouldChangePlan plan = outputSupport.createSeed(machineCode, cleaningWindow);
            String changeMouldType = resolveCleaningMouldChangeType(cleaningWindow);
            // 清洗计划物料对应的产品状态：离机清洗取事件中的续作物料状态，其余取清洗时点机台前物料状态。
            String planProductStatus = cleaningWindow.isOffMachineCleaning()
                    ? cleaningWindow.getContinuationProductStatus() : plan.getProductStatus();
            plan.setFactoryCode(context.getFactoryCode());
            plan.setLhResultBatchNo(context.getBatchNo());
            plan.setScheduleDate(context.getScheduleTargetDate());
            plan.setPlanDate(cleaningWindow.getCleanStartTime());
            plan.setPlanOrder(planOrder++);
            plan.setLhMachineCode(machineCode);
            plan.setLhMachineName(machine != null ? machine.getMachineName() : null);
            // 清洗场景：双模机台赋值 LR，单模机台按编码后缀赋值 L/R
            plan.setLeftRightMould(LeftRightMouldUtil.resolveCleaningLeftRightMould(machineCode));
            if (cleaningWindow.isOffMachineCleaning()) {
                // 触发后机台可能已选入其他物料，清洗计划始终归属事件中的原续作 SKU。
                plan.setBeforeMaterialCode(cleaningWindow.getContinuationMaterialCode());
                plan.setAfterMaterialCode(cleaningWindow.getContinuationMaterialCode());
                plan.setBeforeMaterialDesc(cleaningWindow.getContinuationMaterialDesc());
                plan.setAfterMaterialDesc(cleaningWindow.getContinuationMaterialDesc());
            }
            plan.setChangeMouldType(changeMouldType);
            // 独立清洗保留清洗类型；成功置换由下方按真实事件清除机台清洗标记。
            plan.setCleanType(changeMouldType);
            plan.setChangeTime(cleaningWindow.getCleanStartTime());
            plan.setMouldCode(MouldChangeTypeEnum.containsCode(
                    changeMouldType, MouldChangeTypeEnum.SAND_BLAST.getCode())
                    ? "" : cleaningWindow.getMouldCode());
            plan.setIsRelease("0");
            plan.setMouldStatus("0");
            plan.setRemark(cleaningWindow.getRemark());
            if (cleaningWindow.isOffMachineCleaning()) {
                // 置换额度已由主链预占，此处仅输出事实，不重复登记首检、次数或数量扣账。
                plan.setChangeMouldType(this.resolveSandBlastPlanType(cleaningWindow));
                plan.setMouldCode(this.resolveSandBlastPlanMouldCode(cleaningWindow));
                if (StringUtils.isNotEmpty(cleaningWindow.getReplacementMouldCode())) {
                    // 新模具接替生产，原模具离机清洗不作为本次机台交替计划的清洗动作。
                    plan.setCleanType("");
                    plan.setRemark(MOULD_REPLACEMENT_REMARK);
                }
            }
            plan.setIsDelete(0);
            // 保存本条清洗计划物料对应的产品状态。
            plan.setProductStatus(planProductStatus);
            // 清洗计划同样按清洗发生时点的前物料判断，不能读取排程结束后的机台 ending 状态。
            outputSupport.bindEndType(plan, planProductStatus);
            plans.add(plan);
            log.info("续作清洗交替计划生成, 机台: {}, 前物料: {}, 后物料: {}, 原模具: {}, 替换模具: {}, 备注: {}, 已生成交替计划: true, 类型: {}, 保存模具: {}",
                    machineCode, plan.getBeforeMaterialCode(), plan.getAfterMaterialCode(),
                            cleaningWindow.getMouldCode(),
                    cleaningWindow.getReplacementMouldCode(), plan.getRemark(), plan.getChangeMouldType(),
                            plan.getMouldCode());
            planProductStatusMap.put(plan, planProductStatus);
            if (cleaningWindow.isOffMachineCleaning()) {
                outputSupport.recordOffMachineLog(plan, cleaningWindow);
            }
        }
        return planOrder;
    }
    /**
     * 保存无替换模具时两端实际交替计划闭合证据。
     *
     * @param context 排程上下文
     * @param plans 已生成交替计划
     * @param planProductStatusMap 交替计划产品状态
     * @param sourceMachineCode 触发喷砂的原机台
     * @param event 离机喷砂事件
     */
    private void appendOnlySandBlastRequeueProcessLog(
            LhScheduleContext context, List<LhMouldChangePlan> plans,
            Map<LhMouldChangePlan, String> planProductStatusMap,
            String sourceMachineCode, MachineCleaningWindowDTO event) {
        LhMouldChangePlan sourcePlan = plans.stream()
                .filter(plan -> Objects.nonNull(this.resolveOnlySandBlastSourcePlanEvent(context, plan)))
                .filter(plan -> StringUtils.equals(sourceMachineCode, plan.getLhMachineCode()))
                .min(Comparator.comparing(LhMouldChangePlan::getPlanDate,
                        Comparator.nullsLast(Date::compareTo)))
                .orElse(null);
        LhMouldChangePlan receivingPlan = plans.stream()
                .filter(plan -> {
                    MachineCleaningWindowDTO receivingEvent = this.resolveOnlySandBlastReceivingPlanEvent(
                            context, plan, planProductStatusMap.get(plan));
                    return Objects.nonNull(receivingEvent)
                            && StringUtils.equals(sourceMachineCode, receivingEvent.getLhCode());
                })
                .min(Comparator.comparing(LhMouldChangePlan::getPlanDate,
                        Comparator.nullsLast(Date::compareTo)))
                .orElse(null);
        Integer releasedRemaining = context.getOnlySandBlastContinuationRemainingQtyMap()
                .get(sourceMachineCode);
        String detail = String.format(
                "机台=%s，SKU=%s，产品状态=%s，喷砂开始=%s，喷砂结束=%s，原模具=%s，"
                        + "是否生成独立喷砂计划=false，原机交替=%s，原机交替类型=%s，原机保存模具=%s，"
                        + "接收机台=%s，接收交替类型=%s，接收保存模具=%s，SKU下机时剩余量=%s",
                sourceMachineCode, event.getContinuationMaterialCode(), event.getContinuationProductStatus(),
                LhScheduleTimeUtil.formatDateTime(event.getCleanStartTime()),
                LhScheduleTimeUtil.formatDateTime(event.getCleanEndTime()), event.getMouldCode(),
                Objects.nonNull(sourcePlan), Objects.nonNull(sourcePlan) ? sourcePlan.getChangeMouldType() : null,
                Objects.nonNull(sourcePlan) ? sourcePlan.getMouldCode() : null,
                Objects.nonNull(receivingPlan) ? receivingPlan.getLhMachineCode() : null,
                Objects.nonNull(receivingPlan) ? receivingPlan.getChangeMouldType() : null,
                Objects.nonNull(receivingPlan) ? receivingPlan.getMouldCode() : null,
                releasedRemaining);
        PriorityTraceLogHelper.appendProcessLog(context, "续作仅喷砂最终处置", detail);
        log.info("续作仅喷砂两端交替计划复核, {}", detail);
    }
    /**
     * 按最终排程结果重建清洗处置与设备停机计划日期回填项。
     * <p>初始化阶段只负责生成候选窗口和保留来源计划信息；最终阶段统一执行
     * “三天内收尾取消 > 正规换模取消 > 独立清洗”的优先级，并将回填日期统一归零到自然日。</p>
     *
     * @param context 排程上下文
     */
    public void finalizeCleaningDisposition(LhScheduleContext context) {
        if (Objects.isNull(context)) {
            return;
        }
        this.synchronizeCancelledCleaningSummaries(context);
        List<CleaningScheduleDateFillItem> sourceFillList =
                new ArrayList<>(context.getCleaningScheduleDateFillList());
        Set<Long> sourceIds = sourceFillList.stream().filter(Objects::nonNull)
                .map(CleaningScheduleDateFillItem::getPlanId)
                .collect(Collectors.toSet());
        context.getCleaningPlanRuntimeState().getSourceWindowMap().forEach((machineCode, sources) -> {
            for (MachineCleaningWindowDTO source : sources) {
                if (sourceIds.contains(source.getSourcePlanId())
                        || LhCleaningRepairOverlapPolicy.resolveCommittedSwitch(context, machineCode, source) == null) {
                    continue;
                }
                // 独立清洗额度满不等于正规换模未覆盖来源；仅为已兑现组合补本来源回填。
                CleaningScheduleDateFillItem fill = new CleaningScheduleDateFillItem();
                fill.setPlanId(source.getSourcePlanId());
                fill.setMachineCode(machineCode);
                fill.setCleanType(source.getCleanType());
                sourceFillList.add(fill);
                sourceIds.add(source.getSourcePlanId());
            }
        });
        Map<Long, CleaningScheduleDateFillItem> finalFillMap = new LinkedHashMap<>(sourceFillList.size());
        for (CleaningScheduleDateFillItem sourceFill : sourceFillList) {
            if (Objects.isNull(sourceFill) || Objects.isNull(sourceFill.getPlanId())) {
                continue;
            }
            MachineCleaningWindowDTO cleaningWindow = this.resolveSourceCleaningWindow(context, sourceFill);
            String machineCode = StringUtils.isNotEmpty(sourceFill.getMachineCode())
                    ? sourceFill.getMachineCode()
                    : Objects.nonNull(cleaningWindow) ? cleaningWindow.getLhCode() : null;
            CleaningDispositionResolver.Decision decision = CleaningDispositionResolver.resolve(
                    context, machineCode, cleaningWindow);
            LhScheduleResult endingResult = decision.getEndingResult();
            CleaningScheduleDateFillItem finalFill = this.copyCleaningScheduleDateFillItem(sourceFill);
            if (Objects.nonNull(endingResult)) {
                this.applyCleaningEndingAnalysis(endingResult, sourceFill.getCleanType());
                // 同一物理事件的左右侧分别绑定本侧真实收尾，来源日期仍只回填一次。
                context.getCleaningPlanRuntimeState().getSourceWindowMap().forEach((side, windows) -> windows.stream()
                        .filter(window -> Objects.equals(sourceFill.getPlanId(), window.getSourcePlanId()))
                        .map(window -> CleaningDispositionResolver.resolve(context, side, window).getEndingResult())
                        .filter(Objects::nonNull)
                        .forEach(ending -> this.applyCleaningEndingAnalysis(ending, sourceFill.getCleanType())));
                finalFill.setScheduleDate(LhScheduleTimeUtil.clearTime(endingResult.getSpecEndTime()));
                finalFill.setFillReason("收尾未安排清洗");
                log.info("清洗最终处置, 处置: 三天内收尾跳过, 机台: {}, 物料: {}, 产品状态: {}, "
                                + "清洗类型: {}, 来源停机计划ID: {}, 回填日期: {}",
                        machineCode, endingResult.getMaterialCode(), endingResult.getProductStatus(),
                        sourceFill.getCleanType(), sourceFill.getPlanId(),
                        LhScheduleTimeUtil.formatDate(finalFill.getScheduleDate()));
            } else if (Objects.nonNull(cleaningWindow)) {
                boolean mouldChangeOverlap = decision.getDisposition()
                        == CleaningDispositionResolver.Disposition.MOULD_CHANGE_CANCELLED;
                finalFill.setScheduleDate(LhScheduleTimeUtil.clearTime(cleaningWindow.getCleanStartTime()));
                String disposition;
                if (mouldChangeOverlap) {
                    disposition = "正规换模取消清洗";
                } else if (cleaningWindow.isOffMachineCleaning()) {
                    disposition = StringUtils.isNotEmpty(cleaningWindow.getReplacementMouldCode())
                            ? "模具置换" : "离机喷砂并入实际交替";
                } else {
                    disposition = "独立清洗";
                }
                finalFill.setFillReason(disposition);
                log.info("清洗最终处置, 处置: {}, 机台: {}, 清洗类型: {}, 来源停机计划ID: {}, 回填日期: {}",
                        disposition,
                        machineCode, sourceFill.getCleanType(), sourceFill.getPlanId(),
                        LhScheduleTimeUtil.formatDate(finalFill.getScheduleDate()));
            } else if (Objects.nonNull(sourceFill.getScheduleDate())) {
                // 初始化阶段已确认三天内收尾但本批未生成对应结果时，保留原业务日期，不伪造结果时间。
                finalFill.setScheduleDate(LhScheduleTimeUtil.clearTime(sourceFill.getScheduleDate()));
            }
            if (Objects.nonNull(finalFill.getScheduleDate())) {
                finalFillMap.putIfAbsent(finalFill.getPlanId(), finalFill);
            }
        }
        context.getCleaningScheduleDateFillList().clear();
        context.getCleaningScheduleDateFillList().addAll(finalFillMap.values());
        this.applyCleaningRepairCombinationAnalysis(context);
    }
    /**
     * 将已兑现三项组合的准备说明写入切换开始班次，首检和数量仍使用主链冻结的真实时间轴。
     * 同一来源旧收尾说明仅在没有其他同类型独立清洗来源时移除，保留换胶囊等无关原因。
     *
     * @param context 已完成正式排量的结果和来源事件
     */
    private void applyCleaningRepairCombinationAnalysis(LhScheduleContext context) {
        if (context.getCleaningPlanRuntimeState().getSourceWindowMap().isEmpty()) {
            return;
        }
        Map<String, List<MachineCleaningWindowDTO>> effectiveWindows = this.collectCleaningPlanItems(context).stream()
                .filter(item -> !item.getValue().isOffMachineCleaning())
                .filter(item -> CleaningDispositionResolver.resolve(context,
                        this.resolveCleaningMachineCode(item.getKey(), item.getValue()), item.getValue())
                        .getDisposition() == CleaningDispositionResolver.Disposition.INDEPENDENT)
                .collect(Collectors.groupingBy(item -> this.resolveCleaningMachineCode(item.getKey(), item.getValue()),
                        Collectors.mapping(Map.Entry::getValue, Collectors.toList())));
        context.getCleaningPlanRuntimeState().getSourceWindowMap().forEach((machineCode, sources) -> {
            for (MachineCleaningWindowDTO source : sources) {
                LhScheduleResult result = LhCleaningRepairOverlapPolicy.resolveCommittedSwitch(
                        context, machineCode, source);
                if (result == null) {
                    continue;
                }
                Date switchStart = result.getMouldChangeStartTime();
                Date switchEnd = LhCleaningRepairOverlapPolicy.resolveSwitchEnd(context, result);
                LhShiftConfigVO preparationShift = LhScheduleTimeUtil.resolveShiftByTime(
                        context.getScheduleWindowShifts(), switchStart);
                if (preparationShift == null) {
                    continue;
                }
                boolean typeBlock = "1".equals(result.getIsTypeBlock());
                boolean sandBlast = MachineCleaningOverlapUtil.isSandBlastCleaning(source);
                String analysis = sandBlast ? "喷砂清洗+维修+换活字块"
                        : typeBlock ? "维修+换活字块+预热" : "干冰清洗+换模+维修";
                Date preparationEnd = LhCleaningRepairOverlapPolicy.resolvePreparationEnd(
                        context, machineCode, switchStart, switchEnd, typeBlock);
                boolean otherDryIceSource = sources.stream().anyMatch(other -> MachineCleaningOverlapUtil
                        .isDryIceCleaning(other) && !Objects.equals(other.getSourcePlanId(), source.getSourcePlanId()));
                List<MachineMaintenanceWindowDTO> repairWindows = LhPlannedRepairResolver.resolveCapacityWindows(
                        context, context.getDevicePlanShutList(), machineCode);
                int firstProductionShiftIndex = ShiftFieldUtil.resolveFirstPlannedShiftIndex(result);
                for (LhShiftConfigVO shift : context.getScheduleWindowShifts()) {
                    boolean preparationOverlap = shift.getShiftStartDateTime().before(preparationEnd)
                            && shift.getShiftEndDateTime().after(switchStart);
                    // 维修末端校正写在首个有量班次；即使首检被供胚门禁推迟，也应移除同源旧组合说明。
                    if (!preparationOverlap && shift.getShiftIndex() != firstProductionShiftIndex) {
                        continue;
                    }
                    int shiftIndex = shift.getShiftIndex();
                    boolean independentRepair = repairWindows.stream()
                            .anyMatch(repair -> repair.getMaintenanceStartTime().before(shift.getShiftEndDateTime())
                                    && repair.getProductionResumeTime().after(shift.getShiftStartDateTime())
                                    && !(repair.getMaintenanceStartTime().before(switchEnd)
                                    && repair.getMaintenanceEndTime().after(switchStart)));
                    if (!independentRepair) {
                        ShiftFieldUtil.removeShiftAnalysis(result, shiftIndex, "计划性维修+换活字块+预热");
                        ShiftFieldUtil.removeShiftAnalysis(result, shiftIndex, "计划性维修+换活字块（含预热）");
                        ShiftFieldUtil.removeShiftAnalysis(result, shiftIndex, "计划性维修+换模+预热");
                        ShiftFieldUtil.removeShiftAnalysis(result, shiftIndex, "计划性维修+换模（含预热）");
                    }
                    ShiftFieldUtil.removeShiftAnalysis(result, shiftIndex, TYPE_BLOCK_DRY_ICE_CLEANING_ANALYSIS);
                    ShiftFieldUtil.removeShiftAnalysis(result, shiftIndex, analysis);
                    if (sandBlast) {
                        ShiftFieldUtil.removeShiftAnalysis(result, shiftIndex, "喷砂清洗+换模");
                        ShiftFieldUtil.removeShiftAnalysis(result, shiftIndex, "喷砂清洗+换活字块");
                        ShiftFieldUtil.removeShiftAnalysis(result, shiftIndex, "喷砂清洗首检");
                    }
                    if (typeBlock && !otherDryIceSource) {
                        ShiftFieldUtil.removeShiftAnalysis(result, shiftIndex, "干冰清洗");
                    }
                }
                if (!otherDryIceSource) {
                    context.getScheduleResultList().stream().filter(Objects::nonNull)
                            .filter(ending -> StringUtils.equals(machineCode, ending.getLhMachineCode()))
                            .filter(ending -> StringUtils.equals(
                                    source.getSourceMaterialCode(), ending.getMaterialCode())
                                    && StringUtils.equals(source.getSourceProductStatus(), ending.getProductStatus()))
                            .forEach(ending -> context.getScheduleWindowShifts().forEach(shift ->
                                    ShiftFieldUtil.removeShiftAnalysis(ending, shift.getShiftIndex(),
                                            DRY_ICE_ENDING_ANALYSIS)));
                }
                if (typeBlock && !sandBlast) {
                    ShiftFieldUtil.appendShiftAnalysis(result, preparationShift.getShiftIndex(), "干冰清洗");
                }
                ShiftFieldUtil.appendShiftAnalysis(result, preparationShift.getShiftIndex(), analysis);
                MachineCleaningWindowDTO projected = LhCleaningRepairOverlapPolicy.projectWindow(
                        context, source, switchStart, switchEnd, typeBlock);
                if (typeBlock) {
                    ResultDowntimeSummaryUtil.refreshCancelledCleaningSummary(result,
                            effectiveWindows.getOrDefault(machineCode, Collections.emptyList()),
                            Collections.emptyList(), context.getScheduleWindowShifts());
                }
                String detail = "工厂=" + context.getFactoryCode() + ", 批次=" + context.getBatchNo()
                        + ", 机台=" + machineCode + ", 物料=" + result.getMaterialCode()
                        + ", 状态=" + result.getProductStatus() + ", 来源=" + source.getSourcePlanId()
                        + ", 清洗开始=" + LhScheduleTimeUtil.formatDateTime(projected.getCleanStartTime())
                        + ", 清洗结束=" + LhScheduleTimeUtil.formatDateTime(projected.getCleanEndTime())
                        + ", 切换开始=" + LhScheduleTimeUtil.formatDateTime(switchStart)
                        + ", 准备说明班次=" + preparationShift.getShiftIndex() + ", 组合说明=" + analysis;
                PriorityTraceLogHelper.appendProcessLog(context, "清洗维修切换组合最终处置", detail);
                log.info("清洗维修切换组合最终处置, {}", detail);
            }
        });
    }
    /**
     * 将最终来源事件处置同步到同机台全部结果，清除后料继承的无效清洗摘要。
     *
     * @param context 已确定最终结果的排程上下文
     */
    private void synchronizeCancelledCleaningSummaries(LhScheduleContext context) {
        Map<String, List<MachineCleaningWindowDTO>> windowsByMachine = this.collectCleaningPlanItems(context).stream()
                .collect(Collectors.groupingBy(item -> this.resolveCleaningMachineCode(item.getKey(), item.getValue()),
                        Collectors.mapping(Map.Entry::getValue, Collectors.toList())));
        windowsByMachine.forEach((machineCode, windows) -> {
            List<MachineCleaningWindowDTO> cancelled = windows.stream()
                    .filter(window -> CleaningDispositionResolver.resolve(context, machineCode, window).getDisposition()
                            != CleaningDispositionResolver.Disposition.INDEPENDENT)
                    .collect(Collectors.toList());
            if (cancelled.isEmpty()) {
                return;
            }
            List<MachineCleaningWindowDTO> effective = CleaningDispositionResolver.resolveEffectiveWindows(
                    context, machineCode, windows).stream().filter(window -> !window.isOffMachineCleaning())
                    .collect(Collectors.toList());
            context.getScheduleResultList().stream().filter(Objects::nonNull)
                    .filter(result ->
                            CleaningDispositionResolver.coversMachineSide(machineCode, result.getLhMachineCode()))
                    .forEach(result -> ResultDowntimeSummaryUtil.refreshCancelledCleaningSummary(
                            result, effective, cancelled, context.getScheduleWindowShifts()));
        });
    }
    /**
     * 将清洗加收尾原因写入最终有计划量班次，并清除已跳过清洗的时间摘要。
     *
     * @param result 最终收尾结果
     * @param cleanType 清洗类型
     */
    private void applyCleaningEndingAnalysis(LhScheduleResult result, String cleanType) {
        if (Objects.isNull(result)) {
            return;
        }
        int endingShiftIndex = ShiftFieldUtil.resolveLastPlannedShiftIndex(result);
        String analysis = StringUtils.equals(CleaningTypeEnum.DRY_ICE.getCode(), cleanType)
                ? DRY_ICE_ENDING_ANALYSIS
                : StringUtils.equals(CleaningTypeEnum.SAND_BLAST.getCode(), cleanType)
                ? SAND_BLAST_ENDING_ANALYSIS : null;
        if (endingShiftIndex > 0 && StringUtils.isNotEmpty(analysis)) {
            ShiftFieldUtil.appendShiftAnalysis(result, endingShiftIndex, analysis);
        }
        result.setCleaningStartTime(null);
        result.setCleaningEndTime(null);
    }
    /**
     * 按来源计划主键查找原始清洗窗口，优先使用来源停机计划机台，避免单控配对侧抢占回填归属。
     *
     * @param context 排程上下文
     * @param sourceFill 初始化阶段回填项
     * @return 来源清洗窗口；初始化已按收尾跳过时返回 null
     */
    private MachineCleaningWindowDTO resolveSourceCleaningWindow(LhScheduleContext context,
                                                                  CleaningScheduleDateFillItem sourceFill) {
        MachineCleaningWindowDTO matchedWindow = null;
        for (Map.Entry<MachineScheduleDTO, MachineCleaningWindowDTO> item : collectCleaningPlanItems(context)) {
            MachineCleaningWindowDTO cleaningWindow = item.getValue();
            if (!Objects.equals(sourceFill.getPlanId(), cleaningWindow.getSourcePlanId())) {
                continue;
            }
            if (StringUtils.equals(sourceFill.getMachineCode(),
                    resolveCleaningMachineCode(item.getKey(), cleaningWindow))) {
                return cleaningWindow;
            }
            if (Objects.isNull(matchedWindow)) {
                matchedWindow = cleaningWindow;
            }
        }
        if (matchedWindow != null) {
            return matchedWindow;
        }
        // 初始化收尾跳过没有机台容量窗口，但完整来源仍可按最终正量结果校正日期。
        return context.getCleaningPlanRuntimeState().getSourceWindowMap()
                .getOrDefault(sourceFill.getMachineCode(), Collections.emptyList()).stream()
                .filter(window -> Objects.equals(sourceFill.getPlanId(), window.getSourcePlanId()))
                .findFirst().orElse(null);
    }
    /**
     * 复制清洗日期回填项，最终阶段不修改初始化阶段的只读来源信息。
     *
     * @param sourceFill 来源回填项
     * @return 新回填项
     */
    private CleaningScheduleDateFillItem copyCleaningScheduleDateFillItem(CleaningScheduleDateFillItem sourceFill) {
        CleaningScheduleDateFillItem targetFill = new CleaningScheduleDateFillItem();
        targetFill.setPlanId(sourceFill.getPlanId());
        targetFill.setScheduleDate(sourceFill.getScheduleDate());
        targetFill.setCleanType(sourceFill.getCleanType());
        targetFill.setMachineCode(sourceFill.getMachineCode());
        targetFill.setFillReason(sourceFill.getFillReason());
        return targetFill;
    }
    /**
     * 诊断周日手工喷砂是否满足交替计划条数阈值。
     *
     * @param context 排程上下文
     */
    public void validateManualSundaySandBlastThreshold(LhScheduleContext context) {
        if (context.getParamIntValue(LhScheduleParamConstant.SAND_BLAST_SKIP_SUNDAY_ENABLED,
                LhScheduleConstant.SAND_BLAST_SKIP_SUNDAY_ENABLED) != ENABLED
                || context.getParamIntValue(LhScheduleParamConstant.SAND_BLAST_ALLOW_SUNDAY_MANUAL_ENABLED,
                LhScheduleConstant.SAND_BLAST_ALLOW_SUNDAY_MANUAL_ENABLED) != ENABLED) {
            return;
        }
        int threshold = context.getParamIntValue(LhScheduleParamConstant.SAND_BLAST_SUNDAY_MIN_ALTERNATE_PLAN_COUNT,
                LhScheduleConstant.SAND_BLAST_SUNDAY_MIN_ALTERNATE_PLAN_COUNT);
        for (Map.Entry<MachineScheduleDTO, MachineCleaningWindowDTO> item : collectCleaningPlanItems(context)) {
            MachineCleaningWindowDTO cleaningWindow = item.getValue();
            if (cleaningWindow == null
                    || !StringUtils.equals(CleaningTypeEnum.SAND_BLAST.getCode(), cleaningWindow.getCleanType())
                    || !StringUtils.equals(CLEANING_DATA_SOURCE_MANUAL, cleaningWindow.getDataSource())
                    || cleaningWindow.getCleanStartTime() == null
                    || !this.isSunday(cleaningWindow.getCleanStartTime())) {
                continue;
            }
            String dateKey = LhScheduleTimeUtil.formatDate(cleaningWindow.getCleanStartTime());
            long alternatePlanCount = context.getMouldChangePlanList().stream()
                    .filter(plan -> Objects.nonNull(plan.getPlanDate())
                            && StringUtils.equals(dateKey, LhScheduleTimeUtil.formatDate(plan.getPlanDate()))
                            && !isCleaningMouldChangePlan(plan)
                            && Objects.isNull(this.resolveOffMachineCleaningEvent(context, plan)))
                    .count();
            if (alternatePlanCount >= threshold) {
                log.warn("周日手工喷砂交替计划数量达到诊断阈值, 日期: {}, 机台: {}, 阈值: {}, 实际条数: {}",
                        dateKey, resolveCleaningMachineCode(item.getKey(), cleaningWindow),
                        threshold, alternatePlanCount);
            }
        }
    }
    /**
     * 在自动排程保存前拆分交替计划类型，纯清洗不补造换模动作。
     *
     * @param context 已完成模具号与备注处理的排程上下文
     */
    public void splitMouldChangePlanCleaningTypes(LhScheduleContext context) {
        for (LhMouldChangePlan plan : context.getMouldChangePlanList()) {
            String changeMouldType = StringUtils.defaultString(plan.getChangeMouldType());
            String cleanType = this.resolvePlanCleaningTypes(
                    StringUtils.defaultString(plan.getCleanType()) + "," + changeMouldType);
            plan.setCleanType(cleanType);
            plan.setChangeMouldType(Arrays.stream(changeMouldType.split(","))
                    .map(String::trim).filter(StringUtils::isNotEmpty)
                    .filter(code -> !MouldChangeTypeEnum.containsAnyCode(code,
                            MouldChangeTypeEnum.SAND_BLAST.getCode(), MouldChangeTypeEnum.DRY_ICE.getCode()))
                    .distinct().collect(Collectors.joining(",")));
            if (StringUtils.isNotEmpty(cleanType)) {
                log.info("交替计划清洗类型拆分, factoryCode: {}, batchNo: {}, machineCode: {}, "
                                + "planDate: {}, beforeMaterial: {}, afterMaterial: {}, "
                                        + "changeMouldType: {}, cleanType: {}",
                        plan.getFactoryCode(), plan.getLhResultBatchNo(), plan.getLhMachineCode(),
                        LhScheduleTimeUtil.formatDateTime(plan.getPlanDate()), plan.getBeforeMaterialCode(),
                        plan.getAfterMaterialCode(), plan.getChangeMouldType(), plan.getCleanType());
            }
        }
    }
    /**
     * 提取交替计划清洗编码，不复用清洗来源的01/02编码。
     *
     * @param source 英文逗号分隔的交替计划类型
     * @return 去重后的清洗编码03/04，无清洗时返回空字符串
     */
    private String resolvePlanCleaningTypes(String source) {
        return Arrays.stream(StringUtils.defaultString(source).split(","))
                .map(String::trim).filter(StringUtils::isNotEmpty)
                .filter(code -> MouldChangeTypeEnum.containsAnyCode(code,
                        MouldChangeTypeEnum.SAND_BLAST.getCode(), MouldChangeTypeEnum.DRY_ICE.getCode()))
                .distinct().collect(Collectors.joining(","));
    }
    /**
     * 判断是否为清洗类交替计划。
     *
     * @param plan 模具交替计划
     * @return true-清洗类；false-非清洗类
     */
    public boolean isCleaningMouldChangePlan(LhMouldChangePlan plan) {
        return Objects.nonNull(plan)
                && (MouldChangeTypeEnum.containsAnyCode(plan.getChangeMouldType(),
                MouldChangeTypeEnum.SAND_BLAST.getCode(), MouldChangeTypeEnum.DRY_ICE.getCode())
                || (StringUtils.isEmpty(plan.getChangeMouldType())
                && StringUtils.isNotEmpty(plan.getCleanType())));
    }
    /**
     * 收集清洗计划项。
     *
     * @param context 排程上下文
     * @return 清洗计划项
     */
    public List<Map.Entry<MachineScheduleDTO, MachineCleaningWindowDTO>> collectCleaningPlanItems(
            LhScheduleContext context) {
        List<Map.Entry<MachineScheduleDTO, MachineCleaningWindowDTO>> itemList = new ArrayList<>();
        for (MachineScheduleDTO machine : context.getMachineScheduleMap().values()) {
            if (machine == null || CollectionUtils.isEmpty(machine.getCleaningWindowList())) {
                continue;
            }
            for (MachineCleaningWindowDTO cleaningWindow : machine.getCleaningWindowList()) {
                if (cleaningWindow == null
                        || cleaningWindow.getCleanStartTime() == null
                        || StringUtils.isEmpty(resolveCleaningMouldChangeType(cleaningWindow))) {
                    continue;
                }
                itemList.add(new java.util.AbstractMap.SimpleEntry<>(machine, cleaningWindow));
            }
        }
        // 离机清洗不再位于机台停机列表，但仍须生成交替计划及回填来源计划日期。
        for (Map.Entry<String, MachineCleaningWindowDTO> entry
                : context.getContinuationSandBlastWindowMap().entrySet()) {
            if (entry.getValue().isOffMachineCleaning()) {
                itemList.add(
                        new java.util.AbstractMap.SimpleEntry<>(
                                context.getMachineScheduleMap().get(entry.getKey()), entry.getValue()));
            }
        }
        // 初始化收尾跳过的来源只有在正式三项组合兑现后才加入输出，不补造独立清洗。
        context.getCleaningPlanRuntimeState().getSourceWindowMap().forEach((machineCode, sources) -> {
            for (MachineCleaningWindowDTO source : sources) {
                LhScheduleResult result = LhCleaningRepairOverlapPolicy.resolveCommittedSwitch(
                        context, machineCode, source);
                if (result == null) {
                    continue;
                }
                MachineCleaningWindowDTO projected = LhCleaningRepairOverlapPolicy.projectWindow(context, source,
                        result.getMouldChangeStartTime(),
                        LhCleaningRepairOverlapPolicy.resolveSwitchEnd(context, result),
                        "1".equals(result.getIsTypeBlock()));
                itemList.removeIf(item -> StringUtils.equals(machineCode,
                        this.resolveCleaningMachineCode(item.getKey(), item.getValue()))
                        && Objects.equals(source.getSourcePlanId(), item.getValue().getSourcePlanId()));
                itemList.add(new AbstractMap.SimpleEntry<>(
                        context.getMachineScheduleMap().get(machineCode), projected));
            }
        });
        itemList.sort(Comparator
                .comparing(
                        (Map.Entry<MachineScheduleDTO,
                                MachineCleaningWindowDTO> item) -> item.getValue().getCleanStartTime(),
                        Comparator.nullsLast(Date::compareTo))
                .thenComparing(item -> resolveCleaningMachineCode(item.getKey(), item.getValue()),
                        Comparator.nullsLast(String::compareTo)));
        return itemList;
    }
    /**
     * 解析清洗计划对应机台。
     *
     * @param machine 机台
     * @param cleaningWindow 清洗窗口
     * @return 机台编码
     */
    public String resolveCleaningMachineCode(MachineScheduleDTO machine, MachineCleaningWindowDTO cleaningWindow) {
        if (cleaningWindow != null && StringUtils.isNotEmpty(cleaningWindow.getLhCode())) {
            return cleaningWindow.getLhCode();
        }
        return machine != null ? machine.getMachineCode() : null;
    }
    /**
     * 解析清洗交替类型。
     *
     * @param cleaningWindow 清洗窗口
     * @return 模具交替类型
     */
    public String resolveCleaningMouldChangeType(MachineCleaningWindowDTO cleaningWindow) {
        if (Objects.isNull(cleaningWindow)) {
            return null;
        }
        if (CleaningTypeEnum.SAND_BLAST.getCode().equals(cleaningWindow.getCleanType())) {
            return MouldChangeTypeEnum.SAND_BLAST.getCode();
        }
        if (CleaningTypeEnum.DRY_ICE.getCode().equals(cleaningWindow.getCleanType())) {
            return MouldChangeTypeEnum.DRY_ICE.getCode();
        }
        return null;
    }
    /**
     * 统一产品状态口径：空状态按正规 S 处理。
     *
     * @param productStatus 原产品状态
     * @return 标准产品状态
     */
    private String normalizeProductStatus(String productStatus) {
        String normalizedStatus = StringUtils.trimToEmpty(productStatus);
        return StringUtils.isEmpty(normalizedStatus)
                ? TrialStatusEnum.FORMAL.getCode() : normalizedStatus;
    }
    /**
     * 按结果生产时间比较先后。
     *
     * @param left 左结果
     * @param right 右结果
     * @return 小于0-左结果更早；大于0-左结果更晚
     */
    private int compareResultTime(LhScheduleResult left, LhScheduleResult right) {
        Date leftTime = this.resolveProductionStartTime(left);
        Date rightTime = this.resolveProductionStartTime(right);
        if (Objects.isNull(leftTime)) {
            leftTime = left.getSpecEndTime();
        }
        if (Objects.isNull(rightTime)) {
            rightTime = right.getSpecEndTime();
        }
        if (Objects.isNull(leftTime)) {
            return Objects.isNull(rightTime) ? 0 : -1;
        }
        return Objects.isNull(rightTime) ? 1 : leftTime.compareTo(rightTime);
    }
    /**
     * 判断指定日期是否为周日。
     *
     * @param date 日期
     * @return true-周日；false-非周日
     */
    private boolean isSunday(Date date) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        return calendar.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY;
    }
    /**
     * 命中清洗与换活字块组合场景时，写入最后一个重叠班次的原因分析。
     *
     * @param context 排程上下文
     * @param result 排程结果
     * @param shifts 班次
     */
    public void applyTypeBlockCleaningAnalysis(LhScheduleContext context,
                                                LhScheduleResult result,
                                                List<LhShiftConfigVO> shifts) {
        if (context == null || result == null || CollectionUtils.isEmpty(shifts)) {
            return;
        }
        Date switchStartTime = result.getMouldChangeStartTime();
        Date productionStartTime = this.resolveFirstPlannedShiftStartTime(result);
        if (switchStartTime == null || productionStartTime == null) {
            return;
        }
        int firstPlannedShiftIndex = ShiftFieldUtil.resolveFirstPlannedShiftIndex(result);
        if (firstPlannedShiftIndex <= 0) {
            return;
        }
        // 换活字块完成时间 = 切换开始 + 换活字块总时长；取完成时间与首个生产班次开始时间的较大者
        // 作为重叠判定上界，避免切换开始时间与生产开始时间相同时（零时长区间）重叠检测失效。
        Date switchCompleteTime = LhScheduleTimeUtil.addHours(switchStartTime,
                LhScheduleTimeUtil.getTypeBlockChangeTotalHours(context));
        Date overlapEndTime = switchCompleteTime.after(productionStartTime)
                ? switchCompleteTime : productionStartTime;
        List<MachineCleaningWindowDTO> cleaningWindowList =
                cleaningOverlapResolver.resolveMachineWindows(context, result.getLhMachineCode());
        int analysisShiftIndex = MachineCleaningOverlapUtil.resolveLastOverlapShiftIndex(
                shifts, switchStartTime, overlapEndTime);
        if (analysisShiftIndex <= 0) {
            analysisShiftIndex = firstPlannedShiftIndex;
        }
        // 换活字块调用处只写清洗固定枚举原因，不再沿用旧“模具清洗+换活字块”泛化文案。
        if (MachineCleaningOverlapUtil.hasCleaningTypeBlockingOverlap(
                cleaningWindowList, CleaningTypeEnum.DRY_ICE.getCode(), switchStartTime, overlapEndTime)) {
            ShiftFieldUtil.appendShiftAnalysis(result, analysisShiftIndex, TYPE_BLOCK_DRY_ICE_CLEANING_ANALYSIS);
        }
        if (MachineCleaningOverlapUtil.hasCleaningTypeBlockingOverlap(
                cleaningWindowList, CleaningTypeEnum.SAND_BLAST.getCode(), switchStartTime, overlapEndTime)) {
            ShiftFieldUtil.appendShiftAnalysis(result, analysisShiftIndex, TYPE_BLOCK_SAND_BLAST_CLEANING_ANALYSIS);
        }
    }
    /**
     * 干冰清洗与续作收尾重叠时，写入最后一个重叠班次原因。
     *
     * @param result 续作排程结果
     * @param shifts 排程窗口班次
     * @param cleaningWindowList 机台清洗窗口
     * @param isEnding 是否收尾
     */
    public void applyDryIceCleaningEndingAnalysis(LhScheduleResult result,
                                                   List<LhShiftConfigVO> shifts,
                                                   List<MachineCleaningWindowDTO> cleaningWindowList,
                                                   boolean isEnding) {
        if (!isEnding || Objects.isNull(result) || CollectionUtils.isEmpty(cleaningWindowList)) {
            return;
        }
        Date productionStartTime = this.resolveFirstPlannedShiftStartTime(result);
        Date productionEndTime = result.getSpecEndTime();
        if (Objects.isNull(productionStartTime)
                || Objects.isNull(productionEndTime)
                || !productionStartTime.before(productionEndTime)) {
            return;
        }
        for (MachineCleaningWindowDTO cleaningWindow : cleaningWindowList) {
            if (!MachineCleaningOverlapUtil.isDryIceCleaning(cleaningWindow)
                    || Objects.isNull(cleaningWindow.getCleanStartTime())
                    || Objects.isNull(cleaningWindow.getCleanEndTime())
                    || !cleaningWindow.getCleanStartTime().before(cleaningWindow.getCleanEndTime())) {
                continue;
            }
            Date overlapStartTime = cleaningWindow.getCleanStartTime().after(productionStartTime)
                    ? cleaningWindow.getCleanStartTime() : productionStartTime;
            Date overlapEndTime = cleaningWindow.getCleanEndTime().before(productionEndTime)
                    ? cleaningWindow.getCleanEndTime() : productionEndTime;
            if (!overlapStartTime.before(overlapEndTime)) {
                continue;
            }
            int shiftIndex = MachineCleaningOverlapUtil.resolveLastOverlapShiftIndex(
                    shifts, overlapStartTime, overlapEndTime);
            if (shiftIndex <= 0) {
                continue;
            }
            ShiftFieldUtil.appendShiftAnalysis(result, shiftIndex, DRY_ICE_ENDING_ANALYSIS);
        }
    }
    /**
     * 读取首个有计划量班次起点，保持策略既有备注归属。
     *
     * @param result 排程结果
     * @return 首个有量班次起点
     */
    private Date resolveFirstPlannedShiftStartTime(LhScheduleResult result) {
        int shiftIndex = ShiftFieldUtil.resolveFirstPlannedShiftIndex(result);
        return shiftIndex <= 0 ? null : ShiftFieldUtil.getShiftStartTime(result, shiftIndex);
    }
    /**
     * 读取原最终核验使用的最早班次时间，保留无起点时使用规格结束时间的口径。
     *
     * @param result 排程结果
     * @return 原核验口径的最早时间
     */
    private Date resolveProductionStartTime(LhScheduleResult result) {
        return IntStream.rangeClosed(1, LhScheduleConstant.MAX_SHIFT_SLOT_COUNT)
                .mapToObj(index -> ShiftFieldUtil.getShiftStartTime(result, index)).filter(Objects::nonNull)
                .min(Date::compareTo).orElse(result.getSpecEndTime());
    }
    /**
     * 交替计划公共能力适配，清洗层复用主流程编号、前料滚动和下机口径。
     */
    public interface PlanOutputSupport {
        /**
         * 取得当前机台和时间对应的公共计划基础字段。
         *
         * @param machineCode 清洗事实所属机台
         * @param window 当前清洗事实
         * @return 已带编号、班次及前料信息的计划
         */
        LhMouldChangePlan createSeed(String machineCode, MachineCleaningWindowDTO window);
        /**
         * 按公共交替规则绑定前料下机类型。
         *
         * @param plan 已确定前后料的交替计划
         * @param productStatus 对应产品状态
         */
        void bindEndType(LhMouldChangePlan plan, String productStatus);
        /**
         * 记录离机交接与主流程剩余账本的对账证据。
         *
         * @param plan 最终交替计划
         * @param window 离机事件
         */
        void recordOffMachineLog(LhMouldChangePlan plan, MachineCleaningWindowDTO window);
    }
}
