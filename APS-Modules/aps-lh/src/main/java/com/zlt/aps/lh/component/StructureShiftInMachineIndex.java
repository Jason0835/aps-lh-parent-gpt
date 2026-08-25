package com.zlt.aps.lh.component;

import com.zlt.aps.lh.api.constant.LhScheduleConstant;
import com.zlt.aps.lh.api.domain.entity.LhScheduleResult;
import com.zlt.aps.lh.context.LhScheduleContext;
import com.zlt.aps.lh.util.LhSingleControlMachineUtil;
import com.zlt.aps.lh.util.ShiftFieldUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.CollectionUtils;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * 结构班次在机机台统计缓存。
 *
 * <p>按【结构 × 班次】维护去重后的物理机台集合，避免新增选机阶段
 * “SKU × 候选机台 × 全量排程结果”反复全表扫描。</p>
 *
 * <p>使用方式：</p>
 * <ul>
 *   <li>S4.4换活字块前基于续作稳定结果构建，换活字块结果提交后增量更新；</li>
 *   <li>S4.5新增选机前基于续作和换活字块最终结果重建，新增结果提交后继续增量更新；</li>
 *   <li>机台运行态被后置逻辑回写时调用 {@link #refreshMachine} 按单台重算兜底。</li>
 * </ul>
 *
 * <p>统计口径与 {@link StructureMinMachineRetentionService#isMachineInStructureAtShift} 保持一致：
 * 正量班次、清洗/精度/计划性维修零量班次、续作停产保机占位均算在机；真实释放边界和后物料接管不算。
 * 单控L/R统一按物理整机编码去重。</p>
 *
 * @author APS
 */
@Slf4j
public class StructureShiftInMachineIndex {

    /** 结构 → 班次 → 去重物理机台编码集合 */
    private final Map<String, Map<Integer, Set<String>>> structureShiftPhysicalMachineMap =
            new LinkedHashMap<String, Map<Integer, Set<String>>>(16);

    /**
     * 基于当前实时排程结果与机台运行态一次性构建在机统计缓存。
     *
     * <p>构建前会清空旧数据，复用同一上下文时保证只统计本次窗口的最新结果。</p>
     *
     * @param context 排程上下文
     * @param retentionService 结构在机统计工具
     */
    public void build(LhScheduleContext context,
                      StructureMinMachineRetentionService retentionService) {
        clear();
        if (Objects.isNull(context) || Objects.isNull(retentionService)
                || CollectionUtils.isEmpty(context.getStructureMinMachineSkuSnapshotMap())) {
            return;
        }
        for (String structureName : context.getStructureMinMachineSkuSnapshotMap().keySet()) {
            if (StringUtils.isEmpty(structureName)) {
                continue;
            }
            for (int shiftIndex = 1;
                 shiftIndex <= LhScheduleConstant.MAX_SHIFT_SLOT_COUNT; shiftIndex++) {
                Set<String> physicalMachineCodes = retentionService
                        .collectStructureInMachinePhysicalCodes(context, structureName, shiftIndex);
                if (CollectionUtils.isEmpty(physicalMachineCodes)) {
                    continue;
                }
                registerStructureShiftPhysicalCodes(structureName, shiftIndex, physicalMachineCodes);
            }
        }
        log.info("结构班次在机统计缓存构建完成, structureCount: {}, shiftMachineCount: {}",
                structureShiftPhysicalMachineMap.size(),
                structureShiftPhysicalMachineMap.values().stream()
                        .mapToInt(Map::size).sum());
    }

    /**
     * 换活字块或新增结果提交后增量更新缓存。
     *
     * <p>新结果代表机台从首个占用班次起可能切换到当前SKU。每个后续班次继续复用
     * {@link StructureMinMachineRetentionService#isMachineInStructureAtShift} 判断正量生产、
     * 停产保机和业务停机，计划量为0且已经真实下机的班次不得继续占用结构机台名额。
     * 单控整机配对侧结果按物理机台编码去重。</p>
     *
     * @param context 排程上下文
     * @param retentionService 结构在机统计工具
     * @param result 刚提交的排程结果
     */
    public void onResultCommitted(LhScheduleContext context,
                                  StructureMinMachineRetentionService retentionService,
                                  LhScheduleResult result) {
        if (Objects.isNull(result) || StringUtils.isEmpty(result.getLhMachineCode())) {
            return;
        }
        int firstOccupiedShiftIndex = resolveFirstOccupiedShiftIndex(result);
        if (firstOccupiedShiftIndex < 1) {
            // 无任何占用班次的结果不改变在机关系（例如纯未生产占位），交给 refreshMachine 兜底。
            refreshMachine(context, retentionService, result.getLhMachineCode());
            return;
        }
        String structureName = resolveResultStructureName(context, retentionService, result);
        if (StringUtils.isEmpty(structureName)) {
            // 结果结构归属缺失时按机台运行态整体重算，避免脏写缓存。
            refreshMachine(context, retentionService, result.getLhMachineCode());
            return;
        }
        String physicalMachineCode =
                LhSingleControlMachineUtil.resolvePhysicalMachineCode(result.getLhMachineCode());
        for (int shiftIndex = firstOccupiedShiftIndex;
             shiftIndex <= LhScheduleConstant.MAX_SHIFT_SLOT_COUNT; shiftIndex++) {
            this.removePhysicalCodeFromAllStructures(physicalMachineCode, shiftIndex);
            if (retentionService.isMachineInStructureAtShift(
                    context, structureName, result.getLhMachineCode(), shiftIndex)) {
                this.addPhysicalCodeToStructureShift(
                        structureName, shiftIndex, physicalMachineCode);
            }
        }
        log.debug("结构班次在机缓存增量更新, batchNo: {}, machineCode: {}, physicalMachineCode: {}, "
                        + "structureName: {}, firstOccupiedShift: {}",
                context.getBatchNo(), result.getLhMachineCode(), physicalMachineCode,
                structureName, firstOccupiedShiftIndex);
    }

    /**
     * 按单台机台重算全部班次的结构归属，用于机台运行态被回写后的兜底刷新。
     *
     * @param context 排程上下文
     * @param retentionService 结构在机统计工具
     * @param machineCode 运行态机台编码
     */
    public void refreshMachine(LhScheduleContext context,
                               StructureMinMachineRetentionService retentionService,
                               String machineCode) {
        if (Objects.isNull(context) || Objects.isNull(retentionService)
                || StringUtils.isEmpty(machineCode)) {
            return;
        }
        String physicalMachineCode =
                LhSingleControlMachineUtil.resolvePhysicalMachineCode(machineCode);
        for (int shiftIndex = 1;
             shiftIndex <= LhScheduleConstant.MAX_SHIFT_SLOT_COUNT; shiftIndex++) {
            removePhysicalCodeFromAllStructures(physicalMachineCode, shiftIndex);
        }
        for (String structureName : context.getStructureMinMachineSkuSnapshotMap().keySet()) {
            if (StringUtils.isEmpty(structureName)) {
                continue;
            }
            for (int shiftIndex = 1;
                 shiftIndex <= LhScheduleConstant.MAX_SHIFT_SLOT_COUNT; shiftIndex++) {
                if (retentionService.isMachineInStructureAtShift(
                        context, structureName, machineCode, shiftIndex)) {
                    addPhysicalCodeToStructureShift(structureName, shiftIndex, physicalMachineCode);
                }
            }
        }
    }

    /**
     * 获取指定结构在指定班次的在机物理机台数。
     *
     * @param structureName 结构名称
     * @param shiftIndex 班次索引
     * @return 在机物理机台数；缓存缺失时返回0
     */
    public int resolveInMachineCount(String structureName, int shiftIndex) {
        Set<String> physicalMachineCodes =
                resolveInMachinePhysicalCodes(structureName, shiftIndex);
        return CollectionUtils.isEmpty(physicalMachineCodes)
                ? 0 : physicalMachineCodes.size();
    }

    /**
     * 获取指定结构在指定班次的在机物理机台编码集合（只读快照）。
     *
     * @param structureName 结构名称
     * @param shiftIndex 班次索引
     * @return 去重物理机台编码集合；不存在时返回空集合
     */
    public Set<String> resolveInMachinePhysicalCodes(String structureName, int shiftIndex) {
        Map<Integer, Set<String>> shiftMachineMap =
                structureShiftPhysicalMachineMap.get(structureName);
        if (Objects.isNull(shiftMachineMap)) {
            return Collections.emptySet();
        }
        Set<String> physicalMachineCodes = shiftMachineMap.get(shiftIndex);
        return CollectionUtils.isEmpty(physicalMachineCodes)
                ? Collections.<String>emptySet()
                : Collections.unmodifiableSet(physicalMachineCodes);
    }

    /**
     * 判断候选机台对应的物理整机是否已经计入指定结构、指定班次。
     *
     * <p>单控 L/R 在写入索引时已经按物理整机去重，查询时继续统一解析物理机台编码，
     * 避免调用方直接操作缓存集合并重复实现单控口径。</p>
     *
     * @param structureName 结构名称
     * @param shiftIndex 班次索引
     * @param machineCode 候选运行态机台编码
     * @return true-已经计入；false-尚未计入
     */
    public boolean containsPhysicalMachine(String structureName,
                                           int shiftIndex,
                                           String machineCode) {
        if (StringUtils.isEmpty(machineCode)) {
            return false;
        }
        String physicalMachineCode =
                LhSingleControlMachineUtil.resolvePhysicalMachineCode(machineCode);
        return StringUtils.isNotEmpty(physicalMachineCode)
                && this.resolveInMachinePhysicalCodes(structureName, shiftIndex)
                .contains(physicalMachineCode);
    }

    /**
     * 清空全部缓存数据。
     */
    public void clear() {
        structureShiftPhysicalMachineMap.clear();
    }

    /**
     * 解析结果首个占用班次：优先正量班次，其次有明确班次计划量，最后回退有班次分析或起止时间的班次。
     *
     * @param result 排程结果
     * @return 首个占用班次；不存在返回-1
     */
    private int resolveFirstOccupiedShiftIndex(LhScheduleResult result) {
        int fallbackShiftIndex = -1;
        for (int shiftIndex = 1;
             shiftIndex <= LhScheduleConstant.MAX_SHIFT_SLOT_COUNT; shiftIndex++) {
            Integer planQty = ShiftFieldUtil.getShiftPlanQty(result, shiftIndex);
            if (Objects.nonNull(planQty) && planQty > 0) {
                return shiftIndex;
            }
            if (Objects.nonNull(planQty) || StringUtils.isNotEmpty(
                    ShiftFieldUtil.getShiftAnalysis(result, shiftIndex))
                    || Objects.nonNull(ShiftFieldUtil.getShiftStartTime(result, shiftIndex))) {
                if (fallbackShiftIndex < 1) {
                    fallbackShiftIndex = shiftIndex;
                }
            }
        }
        return fallbackShiftIndex;
    }

    /**
     * 解析结果所属结构名称，优先结果结构字段，其次按物料归属结构。
     *
     * @param context 排程上下文
     * @param retentionService 结构在机统计工具
     * @param result 排程结果
     * @return 结构名称；无法解析返回null
     */
    private String resolveResultStructureName(
            LhScheduleContext context,
            StructureMinMachineRetentionService retentionService,
            LhScheduleResult result) {
        if (StringUtils.isNotEmpty(result.getStructureName())) {
            return result.getStructureName();
        }
        return Objects.isNull(retentionService)
                ? null
                : retentionService.resolveStructureNameByMaterial(context, result.getMaterialCode());
    }

    /**
     * 批量登记结构、班次下的物理机台编码。
     */
    private void registerStructureShiftPhysicalCodes(String structureName,
                                                     int shiftIndex,
                                                     Set<String> physicalMachineCodes) {
        for (String physicalMachineCode : physicalMachineCodes) {
            addPhysicalCodeToStructureShift(structureName, shiftIndex, physicalMachineCode);
        }
    }

    /**
     * 把物理机台编码加入指定结构班次集合。
     */
    private void addPhysicalCodeToStructureShift(String structureName,
                                                 int shiftIndex,
                                                 String physicalMachineCode) {
        Map<Integer, Set<String>> shiftMachineMap = structureShiftPhysicalMachineMap
                .computeIfAbsent(structureName, key -> new LinkedHashMap<Integer, Set<String>>(8));
        Set<String> physicalMachineCodes = shiftMachineMap
                .computeIfAbsent(shiftIndex, key -> new LinkedHashSet<String>(8));
        physicalMachineCodes.add(physicalMachineCode);
    }

    /**
     * 从全部结构的指定班次集合中移除物理机台编码，并清理空集合。
     */
    private void removePhysicalCodeFromAllStructures(String physicalMachineCode,
                                                     int shiftIndex) {
        for (Map<Integer, Set<String>> shiftMachineMap
                : structureShiftPhysicalMachineMap.values()) {
            Set<String> physicalMachineCodes = shiftMachineMap.get(shiftIndex);
            if (CollectionUtils.isEmpty(physicalMachineCodes)) {
                continue;
            }
            physicalMachineCodes.remove(physicalMachineCode);
            if (physicalMachineCodes.isEmpty()) {
                shiftMachineMap.remove(shiftIndex);
            }
        }
    }
}
