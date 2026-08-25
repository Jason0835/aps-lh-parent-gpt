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
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 结构收尾对齐选机判断服务。
 *
 * <p>规则定位：在新增排产物料每次实际选机时实时判断，不再做“续作排产后统一停产保机”的
 * 阶段式预判。对每个候选机台独立执行：</p>
 * <ol>
 *   <li>先读取S4.2按结构聚合的转产最大收尾日期，仅当其位于[T,T+2]内时继续对齐判断；</li>
 *   <li>取候选机台收尾时间所在班次（缺失回退最早可换模时间所在班次）作为统计班次；</li>
 *   <li>从{@link StructureShiftInMachineIndex}读取该班次内待排SKU所属结构的在机物理机台数
 *       （包含该SKU当前已在机的机台，单控L/R已按物理整机去重）；</li>
 *   <li>当 {@code 同结构在机机台数 < 最低硫化机台数 - 1} 时触发约束；</li>
 *   <li>触发后允许选择前物料与待排SKU同结构的候选机台；机台当前物料为空时，
 *       先结合实时排程结果区分真实空机与运行态数据缺失，真实空机允许继续选机；</li>
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
     * 构建结构班次在机统计缓存。
     *
     * <p>S4.4换活字块前基于续作稳定结果构建一次，供换活字块提前生产班次机台数门禁读取；
     * S4.5新增选机前再基于续作和换活字块最终结果重建。两个阶段共用同一索引类型和增量更新
     * 入口，不查询数据库，也不创建平行统计账本。</p>
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
        /*
         * 三天收尾是现有结构对齐规则的前置业务门禁。门禁未通过时必须立即按默认结果放行，
         * 不读取最低机台数、不统计在机机台，也不改变后续正常候选列表及既有机台优先级。
         */
        if (!this.isStructureEndingWithinScheduleWindow(
                context, sku, machine, structureName)) {
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
        /*
         * 触发后按候选机台有效前物料结构比较，禁止使用后物料或其他当前排程物料。
         * 机台运行态当前物料为空时，必须先读取实时排程归属：存在归属表示运行态数据缺失，
         * 仍按有效前物料比较；不存在任何归属才是真实空机，允许继续参与原有选机流程。
         */
        String previousMaterialCode = machine.getCurrentMaterialCode();
        String previousMaterialSource = "机台运行态";
        if (StringUtils.isEmpty(previousMaterialCode)) {
            LhScheduleResult activeOwnerResult = structureMinMachineRetentionService
                    .resolveActiveOwnerResultAtShift(
                            context, machine.getMachineCode(), countingShiftIndex);
            if (Objects.isNull(activeOwnerResult)) {
                decision.setRealIdleMachine(true);
                log.info("结构收尾对齐真实空机放行, batchNo: {}, materialCode: {}, structureName: {}, "
                                + "machineCode: {}, previousMaterialCode: null, previousStructureName: null, "
                                + "countingShift: {}, inMachineCount: {}, minimumMachineCount: {}, "
                                + "triggered: true, reason: 机台无运行态物料且无实时排程归属",
                        context.getBatchNo(), sku.getMaterialCode(), structureName,
                        machine.getMachineCode(), countingShiftIndex, inMachineCount,
                        minimumMachineCount);
                return decision;
            }
            previousMaterialCode = activeOwnerResult.getMaterialCode();
            previousMaterialSource = "实时排程结果";
        }
        decision.setPreviousMaterialCode(previousMaterialCode);
        if (StringUtils.isEmpty(previousMaterialCode)) {
            decision.setAllowed(false);
            decision.setExcludedReason("候选机台存在实时排程归属，但有效前物料为空");
            log.warn("结构收尾对齐运行态数据异常排除, batchNo: {}, materialCode: {}, structureName: {}, "
                            + "machineCode: {}, previousMaterialSource: {}, countingShift: {}, "
                            + "inMachineCount: {}, minimumMachineCount: {}, triggered: true, "
                            + "reason: 存在实时排程归属但有效前物料为空",
                    context.getBatchNo(), sku.getMaterialCode(), structureName,
                    machine.getMachineCode(), previousMaterialSource, countingShiftIndex, inMachineCount,
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
                            + "machineCode: {}, previousMaterialCode: {}, previousMaterialSource: {}, countingShift: {}, "
                            + "inMachineCount: {}, minimumMachineCount: {}",
                    context.getBatchNo(), sku.getMaterialCode(), structureName,
                    machine.getMachineCode(), previousMaterialCode, previousMaterialSource, countingShiftIndex,
                    inMachineCount, minimumMachineCount);
            return decision;
        }
        if (StringUtils.equals(previousStructureName, structureName)) {
            log.info("结构收尾对齐同结构放行, batchNo: {}, materialCode: {}, structureName: {}, "
                            + "machineCode: {}, previousMaterialCode: {}, previousMaterialSource: {}, previousStructureName: {}, "
                            + "countingShift: {}, inMachineCount: {}, minimumMachineCount: {}, reason: 同结构",
                    context.getBatchNo(), sku.getMaterialCode(), structureName,
                    machine.getMachineCode(), previousMaterialCode, previousMaterialSource, previousStructureName,
                    countingShiftIndex, inMachineCount, minimumMachineCount);
            return decision;
        }
        decision.setAllowed(false);
        decision.setExcludedReason("候选机台前物料结构与待排SKU不同结构");
        log.info("结构收尾对齐不同结构排除, batchNo: {}, materialCode: {}, structureName: {}, "
                        + "machineCode: {}, previousMaterialCode: {}, previousMaterialSource: {}, previousStructureName: {}, "
                        + "countingShift: {}, inMachineCount: {}, minimumMachineCount: {}, reason: 不同结构",
                context.getBatchNo(), sku.getMaterialCode(), structureName,
                machine.getMachineCode(), previousMaterialCode, previousMaterialSource, previousStructureName,
                countingShiftIndex, inMachineCount, minimumMachineCount);
        return decision;
    }

    /**
     * 判断结构转产配置中的最大收尾日期是否位于固定三天排程窗口内。
     *
     * <p>S4.2已按工厂、年月、排产版本、正常计划类型和结构名称查询配置，并对跨月多条记录
     * 取完整自然日最大值。本方法只负责按自然日执行闭区间判断：
     * {@code 最大收尾日期 >= T && 最大收尾日期 <= T+2}。配置不存在、END_DAY全部为空、
     * 排程日期缺失或最大日期在窗口外时均返回false，使候选机台继续原正常选机流程。</p>
     *
     * @param context       排程上下文
     * @param sku           当前待排SKU
     * @param machine       当前候选机台
     * @param structureName 待排SKU结构名称
     * @return true-允许进入现有结构对齐内部规则；false-不触发结构对齐
     */
    private boolean isStructureEndingWithinScheduleWindow(
            LhScheduleContext context,
            SkuScheduleDTO sku,
            MachineScheduleDTO machine,
            String structureName) {
        LocalDate windowStartDate = this.toLocalDate(context.getScheduleDate());
        LocalDate windowEndDate = Objects.isNull(windowStartDate)
                ? null : windowStartDate.plusDays(
                        LhScheduleConstant.STRUCTURE_ENDING_ALIGNMENT_WINDOW_DAYS - 1L);
        Map<String, LocalDate> maxEndingDateMap = context.getStructureMaxEndingDateMap();
        LocalDate maxEndingDate = CollectionUtils.isEmpty(maxEndingDateMap)
                ? null : maxEndingDateMap.get(structureName);
        if (Objects.isNull(windowStartDate) || Objects.isNull(windowEndDate)) {
            log.warn("结构收尾对齐三天门禁未通过, batchNo: {}, materialCode: {}, structureName: {}, "
                            + "machineCode: {}, windowStartDate: {}, windowEndDate: {}, maxEndingDate: {}, "
                            + "reason: 排程窗口日期不完整，继续正常选机",
                    context.getBatchNo(), sku.getMaterialCode(), structureName,
                    machine.getMachineCode(), windowStartDate, windowEndDate, maxEndingDate);
            return false;
        }
        if (Objects.isNull(maxEndingDate)) {
            log.info("结构收尾对齐三天门禁未通过, batchNo: {}, materialCode: {}, structureName: {}, "
                            + "machineCode: {}, windowStartDate: {}, windowEndDate: {}, maxEndingDate: null, "
                            + "reason: 未查询到结构转产配置或END_DAY为空，继续正常选机",
                    context.getBatchNo(), sku.getMaterialCode(), structureName,
                    machine.getMachineCode(), windowStartDate, windowEndDate);
            return false;
        }
        boolean withinWindow = !maxEndingDate.isBefore(windowStartDate)
                && !maxEndingDate.isAfter(windowEndDate);
        if (!withinWindow) {
            log.info("结构收尾对齐三天门禁未通过, batchNo: {}, materialCode: {}, structureName: {}, "
                            + "machineCode: {}, windowStartDate: {}, windowEndDate: {}, maxEndingDate: {}, "
                            + "reason: 结构最大收尾日期不在三天窗口内，继续正常选机",
                    context.getBatchNo(), sku.getMaterialCode(), structureName,
                    machine.getMachineCode(), windowStartDate, windowEndDate, maxEndingDate);
            return false;
        }
        log.info("结构收尾对齐三天门禁通过, batchNo: {}, materialCode: {}, structureName: {}, "
                        + "machineCode: {}, windowStartDate: {}, windowEndDate: {}, maxEndingDate: {}, "
                        + "reason: 结构最大收尾日期位于三天窗口内，继续执行现有结构对齐规则",
                context.getBatchNo(), sku.getMaterialCode(), structureName,
                machine.getMachineCode(), windowStartDate, windowEndDate, maxEndingDate);
        return true;
    }

    /**
     * 将带时间部分的排程日期转换为系统时区自然日，避免时分秒影响结构收尾窗口判断。
     *
     * @param date 排程日期
     * @return 自然日；入参为空时返回null
     */
    private LocalDate toLocalDate(Date date) {
        if (Objects.isNull(date)) {
            return null;
        }
        return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
    }

    /**
     * 结构收尾对齐命中且候选机台放行选中后，对结果行和机台运行态打标识。
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
