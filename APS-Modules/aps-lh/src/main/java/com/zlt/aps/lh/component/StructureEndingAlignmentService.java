package com.zlt.aps.lh.component;

import com.zlt.aps.lh.api.constant.LhScheduleConstant;
import com.zlt.aps.lh.api.domain.dto.MachineScheduleDTO;
import com.zlt.aps.lh.api.domain.dto.SkuScheduleDTO;
import com.zlt.aps.lh.api.domain.entity.LhScheduleResult;
import com.zlt.aps.lh.api.domain.vo.LhShiftConfigVO;
import com.zlt.aps.lh.context.LhScheduleContext;
import com.zlt.aps.lh.util.LhScheduleTimeUtil;
import com.zlt.aps.lh.util.ShiftFieldUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.util.Date;
import java.util.List;
import java.util.Objects;

/**
 * 结构收尾对齐选机判断服务。
 *
 * <p>规则定位：在新增排产物料每次实际选机时实时判断，不再做“续作排产后统一停产保机”的
 * 阶段式预判。对每个候选机台独立执行：</p>
 * <ol>
 *   <li>取候选机台收尾时间所在班次（缺失回退最早可换模时间所在班次）作为统计班次；</li>
 *   <li>从{@link StructureShiftInMachineIndex}读取该班次内待排SKU所属结构的在机物理机台数
 *       （包含该SKU当前已在机的机台，单控L/R已按物理整机去重）；</li>
 *   <li>当 {@code 同结构在机机台数 < 最低硫化机台数 - 1} 时触发约束；</li>
 *   <li>触发后仅允许选择前物料（机台当前在产物料）与待排SKU同结构的候选机台，
 *       不同结构及无前物料的空机台直接排除；</li>
 *   <li>命中且最终选中时，结果行复用 {@code IS_STRUCTURE_MIN_MACHINE_RETAINED}=1、
 *       机台运行态打结构收尾对齐标识，并在首个生产班次原因分析追加“结构收尾对齐”。</li>
 * </ol>
 *
 * <p>判断入口 {@link #evaluateCandidate} 为公共方法，后续换活字块、特殊材料等场景
 * 可直接复用，避免各入口重复实现结构比较与触发条件。</p>
 *
 * @author APS
 */
@Slf4j
@Component
public class StructureEndingAlignmentService {

    /** 结构收尾对齐命中标识值（复用IS_STRUCTURE_MIN_MACHINE_RETAINED字段） */
    private static final String ALIGNED_FLAG = "1";
    /** 结构收尾对齐班次原因分析 */
    public static final String ENDING_ALIGNMENT_ANALYSIS = "结构收尾对齐";
    /** 无效班次哨兵值 */
    private static final int INVALID_SHIFT_INDEX = -1;

    @Resource
    private StructureMinMachineRetentionService structureMinMachineRetentionService;

    /**
     * 在S4.5新增选机开始前构建结构收尾对齐在机统计缓存。
     *
     * <p>构建基于续作+换活字块排产完成后的实时排程结果，之后每次选机只读缓存，
     * 并在结果提交时增量更新，避免SKU×候选×全量结果反复扫描。</p>
     *
     * @param context 排程上下文
     */
    public void prepareStructureEndingAlignmentIndex(LhScheduleContext context) {
        StructureShiftInMachineIndex structureShiftInMachineIndex =
                new StructureShiftInMachineIndex();
        structureShiftInMachineIndex.build(context, structureMinMachineRetentionService);
        context.setStructureShiftInMachineIndex(structureShiftInMachineIndex);
    }

    /**
     * 对单个候选机台执行结构收尾对齐实时判断。
     *
     * @param context 排程上下文
     * @param sku 当前待排SKU
     * @param machine 候选机台
     * @return 结构收尾对齐判断结果（触发、是否放行、统计班次、在机数、排除原因等）
     */
    public StructureEndingAlignmentDecision evaluateCandidate(LhScheduleContext context,
                                                              SkuScheduleDTO sku,
                                                              MachineScheduleDTO machine) {
        StructureEndingAlignmentDecision decision = new StructureEndingAlignmentDecision();
        if (Objects.isNull(context) || Objects.isNull(sku) || Objects.isNull(machine)
                || StringUtils.isEmpty(machine.getMachineCode())) {
            return decision;
        }
        String structureName = sku.getStructureName();
        decision.setStructureName(structureName);
        if (StringUtils.isEmpty(structureName)) {
            log.info("结构收尾对齐跳过, batchNo: {}, materialCode: {}, machineCode: {}, reason: 待排SKU无结构",
                    context.getBatchNo(), sku.getMaterialCode(), machine.getMachineCode());
            return decision;
        }
        Integer minimumMachineCount =
                context.getStructureMinVulcanizingMachineMap().get(structureName);
        if (Objects.isNull(minimumMachineCount) || minimumMachineCount <= 0) {
            log.info("结构收尾对齐跳过, batchNo: {}, materialCode: {}, structureName: {}, "
                            + "machineCode: {}, reason: 最低机台数未配置或为0",
                    context.getBatchNo(), sku.getMaterialCode(), structureName,
                    machine.getMachineCode());
            return decision;
        }
        decision.setMinimumMachineCount(minimumMachineCount);
        StructureShiftInMachineIndex structureShiftInMachineIndex =
                context.getStructureShiftInMachineIndex();
        if (Objects.isNull(structureShiftInMachineIndex)) {
            log.warn("结构收尾对齐在机缓存未构建，跳过触发判断, batchNo: {}, materialCode: {}, "
                            + "structureName: {}, machineCode: {}",
                    context.getBatchNo(), sku.getMaterialCode(), structureName,
                    machine.getMachineCode());
            return decision;
        }
        int countingShiftIndex = resolveCountingShiftIndex(context, machine);
        decision.setCountingShiftIndex(countingShiftIndex);
        if (countingShiftIndex < 1) {
            log.info("结构收尾对齐跳过, batchNo: {}, materialCode: {}, structureName: {}, "
                            + "machineCode: {}, reason: 无法定位机台收尾时间所在班次",
                    context.getBatchNo(), sku.getMaterialCode(), structureName,
                    machine.getMachineCode());
            return decision;
        }
        int inMachineCount = structureShiftInMachineIndex.resolveInMachineCount(
                structureName, countingShiftIndex);
        decision.setInMachineCount(inMachineCount);
        boolean triggered = inMachineCount < minimumMachineCount - 1;
        decision.setTriggered(triggered);
        if (!triggered) {
            log.info("结构收尾对齐未触发, batchNo: {}, materialCode: {}, structureName: {}, "
                            + "machineCode: {}, countingShift: {}, inMachineCount: {}, minimumMachineCount: {}",
                    context.getBatchNo(), sku.getMaterialCode(), structureName,
                    machine.getMachineCode(), countingShiftIndex, inMachineCount,
                    minimumMachineCount);
            return decision;
        }
        // 触发后按“候选机台前物料（机台当前在产物料）结构”比较，禁止使用后物料或其他当前排程物料。
        String previousMaterialCode = machine.getCurrentMaterialCode();
        decision.setPreviousMaterialCode(previousMaterialCode);
        if (StringUtils.isEmpty(previousMaterialCode)) {
            decision.setAllowed(false);
            decision.setExcludedReason("候选机台当前无在产物料，无法同结构匹配");
            log.info("结构收尾对齐空机台排除, batchNo: {}, materialCode: {}, structureName: {}, "
                            + "machineCode: {}, countingShift: {}, inMachineCount: {}, minimumMachineCount: {}",
                    context.getBatchNo(), sku.getMaterialCode(), structureName,
                    machine.getMachineCode(), countingShiftIndex, inMachineCount,
                    minimumMachineCount);
            return decision;
        }
        String previousStructureName = structureMinMachineRetentionService
                .resolveStructureNameByMaterial(context, previousMaterialCode);
        decision.setPreviousStructureName(previousStructureName);
        if (StringUtils.isEmpty(previousStructureName)) {
            decision.setAllowed(false);
            decision.setExcludedReason("候选机台前物料无法归属结构，按不同结构排除");
            log.info("结构收尾对齐前物料无结构排除, batchNo: {}, materialCode: {}, structureName: {}, "
                            + "machineCode: {}, previousMaterialCode: {}, countingShift: {}, "
                            + "inMachineCount: {}, minimumMachineCount: {}",
                    context.getBatchNo(), sku.getMaterialCode(), structureName,
                    machine.getMachineCode(), previousMaterialCode, countingShiftIndex,
                    inMachineCount, minimumMachineCount);
            return decision;
        }
        if (StringUtils.equals(previousStructureName, structureName)) {
            log.info("结构收尾对齐同结构放行, batchNo: {}, materialCode: {}, structureName: {}, "
                            + "machineCode: {}, previousMaterialCode: {}, previousStructureName: {}, "
                            + "countingShift: {}, inMachineCount: {}, minimumMachineCount: {}, reason: 同结构",
                    context.getBatchNo(), sku.getMaterialCode(), structureName,
                    machine.getMachineCode(), previousMaterialCode, previousStructureName,
                    countingShiftIndex, inMachineCount, minimumMachineCount);
            return decision;
        }
        decision.setAllowed(false);
        decision.setExcludedReason("候选机台前物料结构与待排SKU不同结构");
        log.info("结构收尾对齐不同结构排除, batchNo: {}, materialCode: {}, structureName: {}, "
                        + "machineCode: {}, previousMaterialCode: {}, previousStructureName: {}, "
                        + "countingShift: {}, inMachineCount: {}, minimumMachineCount: {}, reason: 不同结构",
                context.getBatchNo(), sku.getMaterialCode(), structureName,
                machine.getMachineCode(), previousMaterialCode, previousStructureName,
                countingShiftIndex, inMachineCount, minimumMachineCount);
        return decision;
    }

    /**
     * 结构收尾对齐命中且同结构选中后，对结果行和机台运行态打标识。
     *
     * <p>结果行复用 {@code IS_STRUCTURE_MIN_MACHINE_RETAINED}=1，并在首个生产班次的
     * 原因分析追加“结构收尾对齐”；机台运行态内存标识用于审计与后续场景快速读取。</p>
     *
     * @param machine 当前机台运行态
     * @param result 刚生成的新增排程结果
     */
    public void markStructureEndingAligned(MachineScheduleDTO machine,
                                           LhScheduleResult result) {
        if (Objects.nonNull(machine)) {
            machine.setStructureEndingAligned(true);
        }
        if (Objects.isNull(result)) {
            return;
        }
        result.setIsStructureMinMachineRetained(ALIGNED_FLAG);
        int firstProductionShiftIndex = resolveFirstProductionShiftIndex(result);
        if (firstProductionShiftIndex > 0) {
            ShiftFieldUtil.appendShiftAnalysis(
                    result, firstProductionShiftIndex, ENDING_ALIGNMENT_ANALYSIS);
        }
    }

    /**
     * 清除机台运行态的结构收尾对齐标识。
     *
     * <p>机台切换到新物料时调用，避免旧标识污染后续SKU的判断与审计。</p>
     *
     * @param machine 当前机台运行态
     */
    public void clearMachineStructureEndingAligned(MachineScheduleDTO machine) {
        if (Objects.nonNull(machine)) {
            machine.setStructureEndingAligned(false);
        }
    }

    /**
     * 新增排程结果提交后增量更新结构收尾对齐在机统计缓存。
     *
     * <p>只做机台结构归属的增量搬移，避免SKU×候选×全量结果反复扫描；
     * 缓存未构建时（非S4.5新增链路）静默跳过。</p>
     *
     * @param context 排程上下文
     * @param result 刚提交的排程结果
     */
    public void onResultCommitted(LhScheduleContext context,
                                  LhScheduleResult result) {
        if (Objects.isNull(context) || Objects.isNull(result)) {
            return;
        }
        StructureShiftInMachineIndex structureShiftInMachineIndex =
                context.getStructureShiftInMachineIndex();
        if (Objects.isNull(structureShiftInMachineIndex)) {
            return;
        }
        structureShiftInMachineIndex.onResultCommitted(
                context, structureMinMachineRetentionService, result);
    }

    /**
     * 解析结构收尾对齐在机统计班次。
     *
     * <p>优先取机台收尾时间（{@code estimatedEndTime}，缺失时取已登记有效结果最新结束时间，
     * 再缺失取排程日）所在班次；仍无法定位时回退“最早可换模时间”所在班次。
     * 班次归属与项目现有班次边界保持一致（半开区间[班次开始, 班次结束)）。</p>
     *
     * @param context 排程上下文
     * @param machine 候选机台
     * @return 统计班次序号；无法定位返回无效班次
     */
    private int resolveCountingShiftIndex(LhScheduleContext context,
                                          MachineScheduleDTO machine) {
        Date referenceTime = resolveCandidateReferenceTime(context, machine);
        if (Objects.isNull(referenceTime)) {
            return INVALID_SHIFT_INDEX;
        }
        Date windowStartTime = resolveWindowStartTime(context);
        if (Objects.nonNull(windowStartTime) && referenceTime.before(windowStartTime)) {
            referenceTime = windowStartTime;
        }
        int shiftIndex = LhScheduleTimeUtil.resolveShiftIndexByTime(context, referenceTime);
        if (isValidShiftIndex(shiftIndex)) {
            return shiftIndex;
        }
        // 收尾时间无法命中班次时，回退最早可换模时间所在班次。
        Date switchStartTime = resolveCandidateSwitchStartTime(context, referenceTime);
        int fallbackShiftIndex =
                LhScheduleTimeUtil.resolveShiftIndexByTime(context, switchStartTime);
        return isValidShiftIndex(fallbackShiftIndex)
                ? fallbackShiftIndex : INVALID_SHIFT_INDEX;
    }

    /**
     * 判断班次序号是否落在当前排程窗口有效范围内。
     *
     * @param shiftIndex 班次序号
     * @return true-有效班次；false-窗口外哨兵值或非法值
     */
    private boolean isValidShiftIndex(int shiftIndex) {
        return shiftIndex >= 1
                && shiftIndex <= LhScheduleConstant.MAX_SHIFT_SLOT_COUNT;
    }

    /**
     * 解析候选机台参考收尾时间。
     *
     * <p>与既有候选画像口径保持一致：优先机台运行态预计结束时间，其次已登记有效结果
     * 最新结束时间，最后回退排程日。原结构停产保机统一释放时间参与校正的逻辑已废弃。</p>
     *
     * @param context 排程上下文
     * @param machine 候选机台
     * @return 参考收尾时间；无法解析返回null
     */
    private Date resolveCandidateReferenceTime(LhScheduleContext context,
                                               MachineScheduleDTO machine) {
        Date referenceTime = machine.getEstimatedEndTime();
        if (Objects.isNull(referenceTime)) {
            referenceTime = resolveLatestAssignedEndTime(context, machine.getMachineCode());
        }
        if (Objects.isNull(referenceTime)) {
            referenceTime = Objects.nonNull(context.getScheduleDate())
                    ? context.getScheduleDate() : context.getScheduleTargetDate();
        }
        return referenceTime;
    }

    /**
     * 解析机台已登记有效结果的最新结束时间。
     *
     * @param context 排程上下文
     * @param machineCode 机台编码
     * @return 最新有效结果结束时间；不存在返回null
     */
    private Date resolveLatestAssignedEndTime(LhScheduleContext context,
                                              String machineCode) {
        if (Objects.isNull(context) || StringUtils.isEmpty(machineCode)
                || CollectionUtils.isEmpty(context.getMachineAssignmentMap())) {
            return null;
        }
        List<LhScheduleResult> assignedResults =
                context.getMachineAssignmentMap().get(machineCode);
        if (CollectionUtils.isEmpty(assignedResults)) {
            return null;
        }
        Date latestEndTime = null;
        for (LhScheduleResult result : assignedResults) {
            if (Objects.isNull(result) || Objects.isNull(result.getSpecEndTime())
                    || Objects.isNull(result.getDailyPlanQty()) || result.getDailyPlanQty() <= 0) {
                continue;
            }
            if (Objects.isNull(latestEndTime) || result.getSpecEndTime().after(latestEndTime)) {
                latestEndTime = result.getSpecEndTime();
            }
        }
        return latestEndTime;
    }

    /**
     * 解析候选机台可发起换模的最早时间。
     *
     * @param context 排程上下文
     * @param referenceTime 机台参考收尾时间
     * @return 最早可换模时间
     */
    private Date resolveCandidateSwitchStartTime(LhScheduleContext context,
                                                 Date referenceTime) {
        if (Objects.isNull(referenceTime)) {
            return null;
        }
        if (LhScheduleTimeUtil.isNoMouldChangeTime(context, referenceTime)) {
            return LhScheduleTimeUtil.resolveNextMorningAfterNoMouldChangeWindow(
                    context, referenceTime);
        }
        return referenceTime;
    }

    /**
     * 解析排程窗口首班开始时间。
     *
     * @param context 排程上下文
     * @return 窗口首班开始时间；窗口为空返回null
     */
    private Date resolveWindowStartTime(LhScheduleContext context) {
        if (Objects.isNull(context)
                || CollectionUtils.isEmpty(context.getScheduleWindowShifts())) {
            return null;
        }
        LhShiftConfigVO firstShift = context.getScheduleWindowShifts().get(0);
        return Objects.isNull(firstShift) ? null : firstShift.getShiftStartDateTime();
    }

    /**
     * 解析结果首个生产班次（计划量大于0的班次）。
     *
     * @param result 排程结果
     * @return 首个生产班次；不存在时返回-1
     */
    private int resolveFirstProductionShiftIndex(LhScheduleResult result) {
        if (Objects.isNull(result)) {
            return INVALID_SHIFT_INDEX;
        }
        for (int shiftIndex = 1;
             shiftIndex <= LhScheduleConstant.MAX_SHIFT_SLOT_COUNT; shiftIndex++) {
            Integer planQty = ShiftFieldUtil.getShiftPlanQty(result, shiftIndex);
            if (Objects.nonNull(planQty) && planQty > 0) {
                return shiftIndex;
            }
        }
        return INVALID_SHIFT_INDEX;
    }
}
