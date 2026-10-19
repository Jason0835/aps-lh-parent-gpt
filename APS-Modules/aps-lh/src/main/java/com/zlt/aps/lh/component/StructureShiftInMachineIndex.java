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
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

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
 * 正量班次、清洗/精度/计划性维修零量班次算在机；续作降模停产保机、真实释放边界和后物料接管不算。
 * 单控L/R统一按物理整机编码去重。实际集合保留资源占用，有效集合只扣除全部占用均符合成型收尾豁免的机台。</p>
 *
 * @author APS
 */
@Slf4j
public class StructureShiftInMachineIndex {

    /**
     * 结构、班次对应的实际在机物理机台，资源释放查询继续使用本集合。
     */
    private final Map<String, Map<Integer, Set<String>>> structureShiftPhysicalMachineMap =
            new LinkedHashMap<String, Map<Integer, Set<String>>>(16);

    /**
     * 结构、班次对应的有效配额机台；存在任一非豁免占用的物理机台仍保留。
     */
    private final Map<String, Map<Integer, Set<String>>> effectiveStructureShiftPhysicalMachineMap =
            new LinkedHashMap<>(16);

    /**
     * 深复制实际、有效两类结构班次集合，供候选预演回滚及独立排程副本使用。
     * <p>各层容器均独立，后续增量提交、单机刷新和清空不会改写已捕获的占用状态。</p>
     *
     * @return 与当前统计内容相同、可独立修改的索引
     */
    public StructureShiftInMachineIndex copy() {
        StructureShiftInMachineIndex copied = new StructureShiftInMachineIndex();
        structureShiftPhysicalMachineMap.forEach((structureName, shiftMachines) -> {
            Map<Integer, Set<String>> copiedShiftMachines = new LinkedHashMap<>(shiftMachines.size());
            shiftMachines.forEach((shiftIndex, machines) ->
                    copiedShiftMachines.put(shiftIndex, new LinkedHashSet<>(machines)));
            copied.structureShiftPhysicalMachineMap.put(structureName, copiedShiftMachines);
        });
        effectiveStructureShiftPhysicalMachineMap.forEach((structureName, shiftMachines) -> {
            Map<Integer, Set<String>> copiedShiftMachines = new LinkedHashMap<>(shiftMachines.size());
            shiftMachines.forEach((shiftIndex, machines) ->
                    copiedShiftMachines.put(shiftIndex, new LinkedHashSet<>(machines)));
            copied.effectiveStructureShiftPhysicalMachineMap.put(structureName, copiedShiftMachines);
        });
        return copied;
    }

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
        this.clear();
        if (Objects.isNull(context) || Objects.isNull(retentionService)) {
            return;
        }
        for (String structureName : retentionService.collectStatisticsStructureNames(context)) {
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
                this.registerStructureShiftPhysicalCodes(structureName, shiftIndex, physicalMachineCodes);
                Set<String> effectiveMachines = retentionService.collectEffectiveStructureInMachinePhysicalCodes(
                        context, structureName, shiftIndex);
                effectiveStructureShiftPhysicalMachineMap.computeIfAbsent(structureName, key -> new LinkedHashMap<>())
                        .put(shiftIndex, new LinkedHashSet<>(effectiveMachines));
            }
        }
        if (log.isDebugEnabled()) {
            log.debug("结构班次在机统计缓存构建完成, structureCount: {}, shiftMachineCount: {}",
                    structureShiftPhysicalMachineMap.size(),
                    structureShiftPhysicalMachineMap.values().stream().mapToInt(Map::size).sum());
        }
    }

    /**
     * 换活字块或新增结果提交后增量更新缓存。
     *
     * <p>新结果提交后重算该物理机台各班次的实际结构归属，继续复用
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
        // 与全量构建共用实际在机口径，保留同班前后料及另一单控侧的真实占用。
        this.refreshMachine(context, retentionService, result.getLhMachineCode());
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
        Set<String> runtimeMachineCodes = new LinkedHashSet<>(context.getMachineScheduleMap().keySet());
        context.getScheduleResultList().stream().filter(Objects::nonNull)
                .map(LhScheduleResult::getLhMachineCode).filter(StringUtils::isNotEmpty)
                .forEach(runtimeMachineCodes::add);
        runtimeMachineCodes.add(machineCode);
        runtimeMachineCodes.removeIf(runtimeMachineCode -> !StringUtils.equals(physicalMachineCode,
                LhSingleControlMachineUtil.resolvePhysicalMachineCode(runtimeMachineCode)));
        for (int shiftIndex = 1;
             shiftIndex <= LhScheduleConstant.MAX_SHIFT_SLOT_COUNT; shiftIndex++) {
            this.removePhysicalCodeFromAllStructures(physicalMachineCode, shiftIndex);
        }
        for (String structureName : retentionService.collectStatisticsStructureNames(context)) {
            if (StringUtils.isEmpty(structureName)) {
                continue;
            }
            for (int shiftIndex = 1;
                 shiftIndex <= LhScheduleConstant.MAX_SHIFT_SLOT_COUNT; shiftIndex++) {
                for (String runtimeMachineCode : runtimeMachineCodes) {
                    if (retentionService.isMachineInStructureAtShift(
                            context, structureName, runtimeMachineCode, shiftIndex)) {
                        this.addPhysicalCodeToStructureShift(structureName, shiftIndex, physicalMachineCode);
                        if (!retentionService.isMachineExemptAtShift(
                                context, structureName, runtimeMachineCode, shiftIndex)) {
                            effectiveStructureShiftPhysicalMachineMap.computeIfAbsent(structureName,
                                            key -> new LinkedHashMap<>())
                                    .computeIfAbsent(shiftIndex, key -> new LinkedHashSet<>()).add(physicalMachineCode);
                        }
                    }
                }
            }
        }
    }

    /**
     * 获取指定结构在指定班次扣除成型收尾豁免后的有效物理机台数。
     *
     * @param structureName 结构名称
     * @param shiftIndex 班次索引
     * @return 有效结构机台数；缓存缺失时返回0
     */
    public int resolveInMachineCount(String structureName, int shiftIndex) {
        Set<String> physicalMachineCodes =
                resolveInMachinePhysicalCodes(structureName, shiftIndex);
        return CollectionUtils.isEmpty(physicalMachineCodes)
                ? 0 : physicalMachineCodes.size();
    }

    /**
     * 获取指定结构在指定班次的有效配额物理机台集合（只读视图）。
     *
     * @param structureName 结构名称
     * @param shiftIndex 班次索引
     * @return 去重物理机台编码集合；不存在时返回空集合
     */
    public Set<String> resolveInMachinePhysicalCodes(String structureName, int shiftIndex) {
        return this.resolvePhysicalCodes(effectiveStructureShiftPhysicalMachineMap, structureName, shiftIndex);
    }

    /**
     * 返回扣减前实际在机集合，供实际占用审计和资源释放查询使用。
     *
     * @param structureName 结构名称
     * @param shiftIndex 窗口班次序号
     * @return 实际在机物理机台的只读集合
     */
    public Set<String> resolveRawInMachinePhysicalCodes(String structureName, int shiftIndex) {
        return this.resolvePhysicalCodes(structureShiftPhysicalMachineMap, structureName, shiftIndex);
    }

    /**
     * 读取指定统计视图的物理机台只读集合。
     *
     * @param statisticsMap 实际或有效索引
     * @param structureName 结构名称
     * @param shiftIndex 窗口班次序号
     * @return 只读集合，不存在返回空集合
     */
    private Set<String> resolvePhysicalCodes(Map<String, Map<Integer, Set<String>>> statisticsMap,
            String structureName, int shiftIndex) {
        Map<Integer, Set<String>> shiftMachineMap = statisticsMap.get(structureName);
        if (Objects.isNull(shiftMachineMap)) {
            return Collections.emptySet();
        }
        Set<String> physicalMachineCodes = shiftMachineMap.get(shiftIndex);
        return CollectionUtils.isEmpty(physicalMachineCodes)
                ? Collections.<String>emptySet()
                : Collections.unmodifiableSet(physicalMachineCodes);
    }

    /**
     * 判断候选物理整机是否已占用指定结构、指定班次的有效配额。
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
        effectiveStructureShiftPhysicalMachineMap.clear();
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
        this.removePhysicalCodeFromMap(structureShiftPhysicalMachineMap, physicalMachineCode, shiftIndex);
        this.removePhysicalCodeFromMap(effectiveStructureShiftPhysicalMachineMap, physicalMachineCode, shiftIndex);
    }

    /**
     * 清除单台物理机台的旧班次集合，避免只刷新L侧时遗漏R侧的非豁免占用。
     *
     * @param statisticsMap 待更新的实际或有效索引
     * @param physicalMachineCode 物理机台编码
     * @param shiftIndex 窗口班次序号
     */
    private void removePhysicalCodeFromMap(Map<String, Map<Integer, Set<String>>> statisticsMap,
            String physicalMachineCode, int shiftIndex) {
        for (Map<Integer, Set<String>> shiftMachineMap : statisticsMap.values()) {
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

    /**
     * 在排程阶段结束后输出当前实际及有效机台配额，不在每次试排或回滚重建中重复扫描结果。
     *
     * @param context 当前阶段已稳定的排程上下文
     */
    public void logExemptionSnapshot(LhScheduleContext context) {
        if (Objects.isNull(context) || !log.isInfoEnabled()
                || context.getCxEmbryoEndingShiftIndex().isEmpty()) {
            return;
        }
        structureShiftPhysicalMachineMap.forEach((structureName, shiftMachines) ->
                shiftMachines.keySet().forEach(shiftIndex -> this.logExemption(context, structureName, shiftIndex)));
    }

    /**
     * 输出阶段末单结构、单班次成型收尾配额证据，实际占用不因日志或豁免发生变化。
     *
     * @param context 排程上下文
     * @param structureName 统计结构
     * @param shiftIndex 窗口班次序号
     */
    private void logExemption(LhScheduleContext context, String structureName, int shiftIndex) {
        if (!log.isInfoEnabled() || context.getCxEmbryoEndingShiftIndex().isEmpty()) {
            return;
        }
        Set<String> rawMachines = this.resolveRawInMachinePhysicalCodes(structureName, shiftIndex);
        Set<String> effectiveMachines = this.resolveInMachinePhysicalCodes(structureName, shiftIndex);
        Set<String> exemptMachines = new LinkedHashSet<>(rawMachines);
        exemptMachines.removeAll(effectiveMachines);
        if (rawMachines.isEmpty()) {
            return;
        }
        List<LhScheduleResult> occupancyResults = context.getScheduleResultList().stream().filter(Objects::nonNull)
                .filter(result -> StringUtils.equals(structureName, result.getStructureName()))
                .filter(result -> rawMachines.contains(LhSingleControlMachineUtil.resolvePhysicalMachineCode(
                        result.getLhMachineCode())))
                .collect(Collectors.toList());
        Map<String, Set<String>> endingShifts = occupancyResults.stream()
                .collect(Collectors.toMap(
                        result -> result.getMaterialCode() + "/" + result.getProductStatus()
                                + "=" + result.getEmbryoCode(),
                        result -> EmbryoEndingStructureMachineExemptionPolicy.resolveEndingShifts(
                                context, result.getEmbryoCode()),
                        (existing, duplicate) -> existing, LinkedHashMap::new));
        String shiftKey = context.getScheduleWindowShifts().stream()
                .filter(shift -> Objects.equals(shift.getShiftIndex(), shiftIndex))
                .map(shift -> EmbryoEndingStructureMachineExemptionPolicy.resolveShiftKey(
                        shift.getWorkDate(), shift.getShiftType()))
                .filter(Objects::nonNull).findFirst().orElse(null);
        log.info("成型收尾结构机台统计, factoryCode: {}, batchNo: {}, phase: {}, shiftKey: {}, shiftIndex: {}, structureName: {}, "
                        + "skuEmbryos: {}, endingShifts: {}, rawMachineCount: {}, exemptMachines: {}, "
                        + "deductionCount: {}, effectiveMachineCount: {}",
                context.getFactoryCode(), context.getBatchNo(), context.getCurrentStep(), shiftKey, shiftIndex, structureName,
                endingShifts.keySet(), endingShifts, rawMachines.size(), exemptMachines,
                exemptMachines.size(), effectiveMachines.size());
    }

}
