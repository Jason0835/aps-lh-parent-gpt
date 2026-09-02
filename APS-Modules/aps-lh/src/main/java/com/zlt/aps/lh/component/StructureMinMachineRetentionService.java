package com.zlt.aps.lh.component;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zlt.aps.lh.api.constant.LhScheduleConstant;
import com.zlt.aps.lh.api.constant.LhScheduleParamConstant;
import com.zlt.aps.lh.api.domain.dto.MachineCleaningWindowDTO;
import com.zlt.aps.lh.api.domain.dto.MachineMaintenanceWindowDTO;
import com.zlt.aps.lh.api.domain.dto.MachineScheduleDTO;
import com.zlt.aps.lh.api.domain.dto.SkuScheduleDTO;
import com.zlt.aps.lh.api.domain.entity.LhScheduleResult;
import com.zlt.aps.lh.api.domain.vo.LhShiftConfigVO;
import com.zlt.aps.lh.api.enums.DeleteFlagEnum;
import com.zlt.aps.lh.context.LhScheduleContext;
import com.zlt.aps.lh.util.LhScheduleTimeUtil;
import com.zlt.aps.lh.util.LhSingleControlMachineUtil;
import com.zlt.aps.lh.util.ShiftCapacityResolverUtil;
import com.zlt.aps.lh.util.ShiftFieldUtil;
import com.zlt.aps.maindata.mapper.FactoryParamMapper;
import com.zlt.aps.maindata.mapper.MdmMonCycleSchStruConfEntityMapper;
import com.zlt.aps.mdm.api.domain.entity.MdmMaterialInfo;
import com.zlt.aps.mp.api.domain.entity.FactoryParam;
import com.zlt.aps.mp.api.domain.entity.MdmMonCycleSchStruConf;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * 结构最低硫化机台配置解析与在机统计工具。
 *
 * <p>本服务只承担两类职责：</p>
 * <ul>
 *   <li>在S4.3冻结全部有效结构的SKU快照，并按月计划结构类型解析最低硫化机台数
 *       （周期结构读{@code T_DP_MONTH_CYCLE_STRUCT_CONFIG.MIN_VULCANIZING_MACHINE}且来源=01，
 *       常规结构读工厂参数SYS0204012）；</li>
 *   <li>按“结构+班次”统计真实在机物理机台，供{@link StructureShiftInMachineIndex}构建
 *       在机统计缓存，以及{@link StructureEndingAlignmentService}做结构收尾对齐实时判断。</li>
 * </ul>
 *
 * <p>在机统计口径：班次计划量大于0直接计入；计划量为0或空时，只有机台仍归属当前结构，
 * 且清洗、精度保养、计划性维修等业务停机与班次重叠，或处于续作停产保机（SYS0304030）
 * 占用期内才计入。真实释放边界已早于目标班次或后物料已接管的机台会被排除。
 * 单控L/R按物理整机去重。原“续作排产后结构停产保机”阶段判断、补零占位、保机状态冻结
 * 与统一释放时间已整体废弃，本服务不再保留任何保机运行态。</p>
 *
 * @author APS
 */
@Slf4j
@Component
public class StructureMinMachineRetentionService {

    /** 周期结构 */
    private static final String CYCLE_STRUCTURE_TYPE = "01";
    /** 常规结构 */
    private static final String REGULAR_STRUCTURE_TYPE = "02";
    /** 周期结构配置来源类型：01-正常计划 */
    private static final String CYCLE_SOURCE_TYPE = "01";
    /** 最低机台数解析失败（配置或数据异常）时的跳过哨兵值 */
    private static final int SKIP_MIN_MACHINE_COUNT = -1;

    @Resource
    private MdmMonCycleSchStruConfEntityMapper cycleStructureConfigMapper;
    @Resource
    private FactoryParamMapper factoryParamMapper;

    /**
     * 初始化全部有效结构的SKU快照及最低硫化机台数。
     *
     * <p>调用方必须在续作、新增分类前执行，避免后续SKU出队导致结构配置丢失。
     * 这里只校验结构配置是否完整一致并解析最低机台数；配置异常沿用现有行为，
     * 记录告警并跳过该结构（等价规则不触发）。</p>
     *
     * @param context 排程上下文
     */
    public void initializeStructureMinimumMachineConfigs(LhScheduleContext context) {
        Map<String, List<SkuScheduleDTO>> structureSnapshotMap =
                new LinkedHashMap<String, List<SkuScheduleDTO>>(16);
        Map<String, Integer> minimumMachineMap = new LinkedHashMap<String, Integer>(16);
        if (Objects.isNull(context)) {
            return;
        }
        if (CollectionUtils.isEmpty(context.getStructureSkuMap())) {
            context.setStructureMinMachineSkuSnapshotMap(structureSnapshotMap);
            context.setStructureMinVulcanizingMachineMap(minimumMachineMap);
            return;
        }
        for (Map.Entry<String, List<SkuScheduleDTO>> entry : context.getStructureSkuMap().entrySet()) {
            String structureName = entry.getKey();
            List<SkuScheduleDTO> structureSkuList = entry.getValue();
            if (StringUtils.isEmpty(structureName) || CollectionUtils.isEmpty(structureSkuList)) {
                continue;
            }
            List<SkuScheduleDTO> structureSnapshot = new ArrayList<SkuScheduleDTO>(structureSkuList);
            int minimumMachineCount = resolveMinimumMachineCount(context, structureName, structureSnapshot);
            // 配置或结构数据异常时沿用现有安全跳过行为，不改变本批其他结构的排程。
            if (minimumMachineCount < 0) {
                continue;
            }
            structureSnapshotMap.put(structureName, structureSnapshot);
            minimumMachineMap.put(structureName, minimumMachineCount);
        }
        context.setStructureMinMachineSkuSnapshotMap(structureSnapshotMap);
        context.setStructureMinVulcanizingMachineMap(minimumMachineMap);
        log.info("结构最低机台数规则初始化完成, factoryCode: {}, scheduleDate: {}, structureCount: {}, config: {}",
                context.getFactoryCode(), LhScheduleTimeUtil.formatDate(context.getScheduleDate()),
                structureSnapshotMap.size(), minimumMachineMap);
    }

    /**
     * 收集指定结构在目标班次仍保持在机关系的物理机台编码。
     *
     * <p>统计优先使用实际排程结果：班次计划量大于0直接计入；计划量为0或空时，只有机台仍归属
     * 当前结构，且清洗、精度保养、计划性维修等业务停机与班次重叠，或者处于续作停产保机
     * 占用期内才计入。真实释放边界已早于目标班次或后物料已接管的机台会被排除。</p>
     *
     * <p>返回值统一转换为物理机台编码，因此单控L/R、重复结果行和同机台同班次天然去重。</p>
     *
     * @param context 排程上下文
     * @param structureName 结构名称
     * @param shiftIndex 目标班次索引
     * @return 目标班次仍在机的去重物理机台编码集合
     */
    public Set<String> collectStructureInMachinePhysicalCodes(LhScheduleContext context,
                                                               String structureName,
                                                               int shiftIndex) {
        Set<String> physicalMachineCodes = new LinkedHashSet<String>(8);
        if (Objects.isNull(context) || StringUtils.isEmpty(structureName) || shiftIndex < 1
                || Objects.isNull(resolveShift(context, shiftIndex))) {
            return physicalMachineCodes;
        }
        for (String machineCode : collectStructureRuntimeMachineCodes(context, structureName)) {
            if (isMachineInStructureAtShift(context, structureName, machineCode, shiftIndex)) {
                physicalMachineCodes.add(
                        LhSingleControlMachineUtil.resolvePhysicalMachineCode(machineCode));
            }
        }
        return physicalMachineCodes;
    }

    /**
     * 统计提前生产资格班次内仍应计入结构的物理机台。
     *
     * <p>先复用现有结构班次在机口径得到原始物理机台集合，再按排程结果真实收尾时间、
     * 班次收尾标识、续作降模释放边界和机台下机状态排除“在当前班次内完成生产并下机”
     * 的机台。计划量大于0只表示参与过当前班次生产，不能单独证明班次结束时仍在机。</p>
     *
     * <p>单控L/R先归并为物理整机；只有同一物理机台下全部有效结构占用均能证明已在
     * 当前班次内释放时才排除。任一侧仍继续生产或缺少明确结束证据时，整机继续计数。</p>
     *
     * @param context 排程上下文
     * @param structureName 产品结构
     * @param shift 资格判断使用的当天最后班次
     * @return 班次收尾调整后的结构物理机台统计
     */
    public StructureEarlyProductionAdmission resolveEarlyProductionMachineStatistics(
            LhScheduleContext context,
            String structureName,
            LhShiftConfigVO shift) {
        return this.resolveEffectiveStructureMachineStatistics(
                context, structureName, shift);
    }

    /**
     * 统计正式目标班次内仍有效占用结构名额的物理机台。
     *
     * <p>普通新增、续作加机和提前生产统一复用本方法。原始结构索引只作为历史在机视图，
     * 本方法按目标班次边界排除已经完成生产且无后续正量的机台，不回写或删除原始索引。</p>
     *
     * @param context 排程上下文
     * @param structureName 产品结构
     * @param shift 正式目标班次
     * @return 班次收尾调整后的结构物理机台统计
     */
    public StructureEarlyProductionAdmission resolveEffectiveStructureMachineStatistics(
            LhScheduleContext context,
            String structureName,
            LhShiftConfigVO shift) {
        StructureEarlyProductionAdmission admission =
                new StructureEarlyProductionAdmission();
        admission.setStructureName(structureName);
        if (Objects.isNull(context) || StringUtils.isEmpty(structureName)
                || Objects.isNull(shift) || Objects.isNull(shift.getShiftIndex())) {
            return admission;
        }
        int shiftIndex = shift.getShiftIndex();
        admission.setAdmissionShiftIndex(shiftIndex);
        Set<String> rawPhysicalMachineCodes =
                Objects.nonNull(context.getStructureShiftInMachineIndex())
                        ? context.getStructureShiftInMachineIndex()
                        .resolveInMachinePhysicalCodes(structureName, shiftIndex)
                        : this.collectStructureInMachinePhysicalCodes(
                        context, structureName, shiftIndex);
        admission.getRawScheduledPhysicalMachineCodes().addAll(
                rawPhysicalMachineCodes);
        /*
         * 同一结构班次只收集一次运行态机台编码，后续按物理机台核对收尾边界时复用，
         * 避免每台候选重复扫描全部机台运行态。
         */
        Set<String> structureRuntimeMachineCodes =
                this.collectStructureRuntimeMachineCodes(context, structureName);
        for (String physicalMachineCode : rawPhysicalMachineCodes) {
            if (this.isPhysicalMachineEndingWithinShift(
                    context, structureName, physicalMachineCode, shift,
                    structureRuntimeMachineCodes)) {
                admission.getExcludedEndingPhysicalMachineCodes().add(
                        physicalMachineCode);
                continue;
            }
            admission.getScheduledPhysicalMachineCodes().add(
                    physicalMachineCode);
        }
        admission.setScheduledStructureCount(
                admission.getScheduledPhysicalMachineCodes().size());
        return admission;
    }

    /**
     * 判断指定运行态机台在目标班次是否仍属于结构在机机台。
     *
     * <p>本方法供结构收尾对齐缓存构建与单台增量刷新共用，保证缓存与实时口径一致。
     * 原结构停产保机补零占位、统一释放时间判断已废弃，不再参与在机统计。</p>
     *
     * @param context 排程上下文
     * @param structureName 结构名称
     * @param machineCode 运行态机台编码
     * @param shiftIndex 班次索引
     * @return true-仍在机；false-已释放或被后物料接管
     */
    public boolean isMachineInStructureAtShift(LhScheduleContext context,
                                               String structureName,
                                               String machineCode,
                                               int shiftIndex) {
        if (Objects.isNull(context) || StringUtils.isEmpty(structureName)
                || StringUtils.isEmpty(machineCode) || shiftIndex < 1) {
            return false;
        }
        if (hasPositiveStructurePlan(context, structureName, machineCode, shiftIndex)) {
            return true;
        }
        if (hasPositiveOtherStructurePlan(context, structureName, machineCode, shiftIndex)
                || !isStructureRelationshipIntact(context, structureName, machineCode, shiftIndex)) {
            return false;
        }
        LhShiftConfigVO shift = resolveShift(context, shiftIndex);
        if (Objects.isNull(shift)) {
            return false;
        }
        Integer releaseBoundary =
                context.getContinuousReducedMachineReleaseBoundaryShiftIndex(machineCode);
        if (Objects.nonNull(releaseBoundary) && releaseBoundary < shiftIndex) {
            return false;
        }
        if (isContinuousStopHoldAtShift(context, machineCode, shift)) {
            return true;
        }
        return hasBusinessDowntimeAtShift(context, machineCode, shift);
    }

    /**
     * 解析物料编码所属的结构名称。
     *
     * <p>结构归属统一按以下顺序解析：</p>
     * <ol>
     *   <li>S4.3冻结的结构SKU快照，保持当前批次月计划结构口径优先；</li>
     *   <li>当前批次排程结果，识别排程过程中已经生成或接管的实时物料；</li>
     *   <li>数据初始化阶段已加载的有效物料主数据，识别不在本批月计划中的MES在机前物料。</li>
     * </ol>
     * <p>全部结构判断必须复用本方法，避免在机缓存与候选机台比较使用不同的数据源，
     * 导致主数据结构完整的MES前物料被误判为“无法归属结构”。</p>
     *
     * @param context 排程上下文
     * @param materialCode 物料编码
     * @return 结构名称；无法归属时返回null
     */
    public String resolveStructureNameByMaterial(LhScheduleContext context,
                                                 String materialCode) {
        if (Objects.isNull(context) || StringUtils.isEmpty(materialCode)) {
            return null;
        }
        for (Map.Entry<String, List<SkuScheduleDTO>> entry
                : context.getStructureMinMachineSkuSnapshotMap().entrySet()) {
            if (isSnapshotStructureMaterial(context, entry.getKey(), materialCode)) {
                return entry.getKey();
            }
        }
        for (LhScheduleResult result : context.getScheduleResultList()) {
            if (Objects.nonNull(result) && StringUtils.equals(materialCode, result.getMaterialCode())
                    && StringUtils.isNotEmpty(result.getStructureName())) {
                return result.getStructureName();
            }
        }
        /*
         * MES在机物料可能没有进入当前月计划和待排SKU快照，但数据初始化已经按工厂加载全部
         * 有效物料主数据。这里直接复用上下文主数据，不新增查询，也不改变结构配置来源。
         */
        MdmMaterialInfo materialInfo = context.getMaterialInfoMap().get(materialCode);
        if (Objects.nonNull(materialInfo) && StringUtils.isNotEmpty(materialInfo.getStructureName())) {
            return materialInfo.getStructureName();
        }
        return null;
    }

    /**
     * 解析结构类型，用于结构收尾对齐日志。
     *
     * @param context 排程上下文
     * @param structureName 结构名称
     * @return 结构类型编码（01-周期结构、02-常规结构）；无法解析返回null
     */
    public String resolveStructureType(LhScheduleContext context,
                                       String structureName) {
        List<SkuScheduleDTO> skuList =
                context.getStructureMinMachineSkuSnapshotMap().get(structureName);
        return CollectionUtils.isEmpty(skuList) || Objects.isNull(skuList.get(0))
                ? null : skuList.get(0).getStructureType();
    }

    /**
     * 解析结构最低硫化机台数。
     *
     * @param context 排程上下文
     * @param structureName 结构名称
     * @param structureSkuList 结构SKU快照
     * @return 配置的最低硫化机台数；配置或数据异常时返回跳过哨兵值
     */
    public int resolveMinimumMachineCount(LhScheduleContext context,
                                          String structureName,
                                          List<SkuScheduleDTO> structureSkuList) {
        if (CollectionUtils.isEmpty(structureSkuList)) {
            warnConfigSkip(context, structureName, "结构SKU为空，无法解析最低机台数");
            return SKIP_MIN_MACHINE_COUNT;
        }
        SkuScheduleDTO firstSku = structureSkuList.get(0);
        if (!isConsistentStructureConfig(context, structureName, structureSkuList, firstSku)) {
            return SKIP_MIN_MACHINE_COUNT;
        }
        if (StringUtils.equals(CYCLE_STRUCTURE_TYPE, firstSku.getStructureType())) {
            return resolveCycleStructureMinimum(context, structureName, firstSku);
        }
        if (StringUtils.equals(REGULAR_STRUCTURE_TYPE, firstSku.getStructureType())) {
            return resolveRegularStructureMinimum(context, structureName);
        }
        warnConfigSkip(context, structureName,
                "月计划STRUCTURE_TYPE为空或不支持，实际值=" + firstSku.getStructureType());
        return SKIP_MIN_MACHINE_COUNT;
    }

    /**
     * 收集结构相关的运行态机台编码。
     *
     * @param context 排程上下文
     * @param structureName 结构名称
     * @return 运行态机台编码集合
     */
    private Set<String> collectStructureRuntimeMachineCodes(LhScheduleContext context,
                                                            String structureName) {
        Set<String> machineCodes = new LinkedHashSet<String>(8);
        for (LhScheduleResult result : collectStructureResults(context, structureName)) {
            if (StringUtils.isNotEmpty(result.getLhMachineCode())) {
                machineCodes.add(result.getLhMachineCode());
            }
        }
        for (MachineScheduleDTO machine : context.getMachineScheduleMap().values()) {
            if (Objects.nonNull(machine) && StringUtils.isNotEmpty(machine.getMachineCode())
                    && isStructureMaterial(context, structureName, machine.getCurrentMaterialCode())) {
                machineCodes.add(machine.getMachineCode());
            }
        }
        return machineCodes;
    }

    /**
     * 判断零量/空量班次下机台与结构的物料关系是否仍未解除。
     *
     * @param context 排程上下文
     * @param structureName 结构名称
     * @param machineCode 机台编码
     * @param shiftIndex 班次索引
     * @return true-仍属于当前结构；false-已无结构占用关系
     */
    private boolean isStructureRelationshipIntact(LhScheduleContext context,
                                                  String structureName,
                                                  String machineCode,
                                                  int shiftIndex) {
        MachineScheduleDTO machine = context.getMachineScheduleMap().get(machineCode);
        if (Objects.nonNull(machine) && StringUtils.isNotEmpty(machine.getCurrentMaterialCode())) {
            return isStructureMaterial(context, structureName, machine.getCurrentMaterialCode());
        }
        LhScheduleResult latestOwner = resolveLatestOwnerResult(context, machineCode, shiftIndex);
        if (Objects.nonNull(latestOwner)) {
            return StringUtils.equals(structureName, latestOwner.getStructureName());
        }
        for (LhScheduleResult result : collectStructureResults(context, structureName)) {
            if (StringUtils.equals(machineCode, result.getLhMachineCode())) {
                return true;
            }
        }
        return false;
    }

    /**
     * 解析目标班次前最后一个实际生产结果，作为机台物料关系来源。
     *
     * @param context 排程上下文
     * @param machineCode 机台编码
     * @param shiftIndex 目标班次
     * @return 最后生产结果；不存在返回null
     */
    private LhScheduleResult resolveLatestOwnerResult(LhScheduleContext context,
                                                      String machineCode,
                                                      int shiftIndex) {
        LhScheduleResult latestOwner = null;
        int latestOwnerShiftIndex = -1;
        for (LhScheduleResult result : context.getScheduleResultList()) {
            if (Objects.isNull(result) || !StringUtils.equals(machineCode, result.getLhMachineCode())) {
                continue;
            }
            for (int currentShiftIndex = 1; currentShiftIndex <= shiftIndex; currentShiftIndex++) {
                Integer planQty = ShiftFieldUtil.getShiftPlanQty(result, currentShiftIndex);
                if (Objects.nonNull(planQty) && planQty > 0 && currentShiftIndex >= latestOwnerShiftIndex) {
                    latestOwner = result;
                    latestOwnerShiftIndex = currentShiftIndex;
                }
            }
        }
        return latestOwner;
    }

    /**
     * 解析候选机台在目标班次仍有效的实时排程归属。
     *
     * <p>本方法只在机台运行态当前物料为空时用于区分“真实空机”和“运行态数据缺失”：
     * 优先读取目标班次前最后一个正量结果；若该机台已在目标班次前登记释放，则返回空；
     * 若没有正量结果但当前批次仍存在未释放结果，则保守返回最新结果，避免清洗、精度或
     * 计划性维修形成的零量结果被误判为真实空机。</p>
     *
     * @param context 排程上下文
     * @param machineCode 候选机台编码
     * @param shiftIndex 结构收尾对齐统计班次
     * @return 目标班次仍有效的实时排程结果；无任何实时归属时返回null
     */
    public LhScheduleResult resolveActiveOwnerResultAtShift(LhScheduleContext context,
                                                            String machineCode,
                                                            int shiftIndex) {
        if (Objects.isNull(context) || StringUtils.isEmpty(machineCode) || shiftIndex < 1) {
            return null;
        }
        LhScheduleResult latestOwner = this.resolveLatestOwnerResult(context, machineCode, shiftIndex);
        if (Objects.nonNull(latestOwner)) {
            Integer currentShiftPlanQty = ShiftFieldUtil.getShiftPlanQty(latestOwner, shiftIndex);
            if (Objects.nonNull(currentShiftPlanQty) && currentShiftPlanQty > 0) {
                return latestOwner;
            }
        }
        Integer releaseBoundary =
                context.getContinuousReducedMachineReleaseBoundaryShiftIndex(machineCode);
        if (Objects.nonNull(releaseBoundary) && releaseBoundary < shiftIndex) {
            return null;
        }
        if (Objects.nonNull(latestOwner)) {
            return latestOwner;
        }
        return this.resolveLatestMachineResult(context, machineCode);
    }

    /**
     * 查询当前批次中机台最后一条排程结果。
     *
     * <p>只有机台当前物料为空且没有正量归属时才调用，用于识别全零业务停机结果。
     * 同时读取实时结果列表和新增排产机台分配记录，结果按规格结束时间取最新；结束时间
     * 同时为空时沿用对应集合中的最后一条。</p>
     *
     * @param context 排程上下文
     * @param machineCode 机台编码
     * @return 最后一条机台排程结果；不存在返回null
     */
    private LhScheduleResult resolveLatestMachineResult(LhScheduleContext context,
                                                        String machineCode) {
        if (Objects.isNull(context)) {
            return null;
        }
        LhScheduleResult latestResult = null;
        if (!CollectionUtils.isEmpty(context.getScheduleResultList())) {
            latestResult = this.resolveLatestMachineResult(
                    context.getScheduleResultList(), machineCode, latestResult);
        }
        if (!CollectionUtils.isEmpty(context.getMachineAssignmentMap())) {
            latestResult = this.resolveLatestMachineResult(
                    context.getMachineAssignmentMap().get(machineCode), machineCode, latestResult);
        }
        return latestResult;
    }

    /**
     * 从指定结果集合中选择机台最后一条排程结果。
     *
     * @param resultList 待检查结果集合
     * @param machineCode 机台编码
     * @param currentLatestResult 当前已找到的最新结果
     * @return 合并比较后的最新结果
     */
    private LhScheduleResult resolveLatestMachineResult(List<LhScheduleResult> resultList,
                                                        String machineCode,
                                                        LhScheduleResult currentLatestResult) {
        LhScheduleResult latestResult = currentLatestResult;
        if (CollectionUtils.isEmpty(resultList)) {
            return latestResult;
        }
        for (LhScheduleResult result : resultList) {
            if (Objects.isNull(result)
                    || !StringUtils.equals(machineCode, result.getLhMachineCode())) {
                continue;
            }
            if (Objects.isNull(latestResult)
                    || Objects.isNull(latestResult.getSpecEndTime())
                    || (Objects.nonNull(result.getSpecEndTime())
                    && result.getSpecEndTime().after(latestResult.getSpecEndTime()))) {
                latestResult = result;
            }
        }
        return latestResult;
    }

    /**
     * 判断业务停机是否与目标班次重叠。
     *
     * @param context 排程上下文
     * @param machineCode 机台编码
     * @param shift 班次配置
     * @return true-清洗、精度保养或设备停机与班次重叠；false-无重叠
     */
    private boolean hasBusinessDowntimeAtShift(LhScheduleContext context,
                                               String machineCode,
                                               LhShiftConfigVO shift) {
        if (Objects.isNull(shift) || Objects.isNull(shift.getShiftStartDateTime())
                || Objects.isNull(shift.getShiftEndDateTime())) {
            return false;
        }
        MachineScheduleDTO machine = context.getMachineScheduleMap().get(machineCode);
        List<MachineCleaningWindowDTO> cleaningWindowList =
                Objects.isNull(machine) ? null : machine.getCleaningWindowList();
        List<MachineMaintenanceWindowDTO> maintenanceWindowList =
                Objects.isNull(machine) ? null : machine.getMaintenanceWindowList();
        long overlapSeconds = ShiftCapacityResolverUtil.resolveDowntimeOverlapSeconds(
                context.getDevicePlanShutList(), cleaningWindowList, maintenanceWindowList, machineCode,
                shift.getShiftStartDateTime(), shift.getShiftEndDateTime());
        if (overlapSeconds > 0) {
            return true;
        }
        String physicalMachineCode = LhSingleControlMachineUtil.resolvePhysicalMachineCode(machineCode);
        return !StringUtils.equals(machineCode, physicalMachineCode)
                && ShiftCapacityResolverUtil.resolveDowntimeOverlapSeconds(
                context.getDevicePlanShutList(), cleaningWindowList, maintenanceWindowList, physicalMachineCode,
                shift.getShiftStartDateTime(), shift.getShiftEndDateTime()) > 0;
    }

    /**
     * 判断续作停产保机状态是否覆盖目标班次。
     *
     * @param context 排程上下文
     * @param machineCode 机台编码
     * @param shift 班次配置
     * @return true-目标业务日登记续作停产保机；false-未登记
     */
    private boolean isContinuousStopHoldAtShift(LhScheduleContext context,
                                                String machineCode,
                                                LhShiftConfigVO shift) {
        if (Objects.isNull(shift) || Objects.isNull(shift.getShiftStartDateTime())) {
            return context.isContinuousStopHoldMachine(machineCode);
        }
        LocalDate productionDate = shift.getShiftStartDateTime().toInstant()
                .atZone(ZoneId.systemDefault()).toLocalDate();
        return context.isContinuousStopHoldDate(machineCode, productionDate)
                || context.isContinuousStopHoldMachine(machineCode);
    }

    /**
     * 判断物料是否属于指定结构。
     *
     * @param context 排程上下文
     * @param structureName 结构名称
     * @param materialCode 物料编码
     * @return true-属于结构；false-不属于或物料为空
     */
    private boolean isStructureMaterial(LhScheduleContext context,
                                        String structureName,
                                        String materialCode) {
        // 在机统计与候选选机统一复用同一结构归属入口，禁止分别维护结构数据源。
        return StringUtils.isNotEmpty(structureName)
                && StringUtils.equals(structureName,
                        this.resolveStructureNameByMaterial(context, materialCode));
    }

    /**
     * 仅按S4.3冻结的SKU快照判断物料结构归属。
     *
     * @param context 排程上下文
     * @param structureName 结构名称
     * @param materialCode 物料编码
     * @return true-快照中存在该物料；false-不存在
     */
    private boolean isSnapshotStructureMaterial(LhScheduleContext context,
                                                String structureName,
                                                String materialCode) {
        List<SkuScheduleDTO> snapshotList =
                context.getStructureMinMachineSkuSnapshotMap().get(structureName);
        if (CollectionUtils.isEmpty(snapshotList)) {
            return false;
        }
        for (SkuScheduleDTO sku : snapshotList) {
            if (Objects.nonNull(sku)
                    && StringUtils.equals(materialCode, sku.getMaterialCode())) {
                return true;
            }
        }
        return false;
    }

    /**
     * 判断结构在指定机台班次是否有正量计划。
     */
    private boolean hasPositiveStructurePlan(LhScheduleContext context,
                                             String structureName,
                                             String machineCode,
                                             int shiftIndex) {
        for (LhScheduleResult result : context.getScheduleResultList()) {
            if (this.isResultForStructure(
                    context, result, structureName, machineCode)) {
                Integer planQty = ShiftFieldUtil.getShiftPlanQty(result, shiftIndex);
                if (Objects.nonNull(planQty) && planQty > 0) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * 判断物理机台是否能够证明在目标班次内完成当前结构生产并下机。
     *
     * @param context 排程上下文
     * @param structureName 产品结构
     * @param physicalMachineCode 物理机台编码
     * @param shift 目标班次
     * @return true-物理整机在本班内已经明确释放；false-仍占用或证据不足
     */
    private boolean isPhysicalMachineEndingWithinShift(
            LhScheduleContext context,
            String structureName,
            String physicalMachineCode,
            LhShiftConfigVO shift,
            Set<String> structureRuntimeMachineCodes) {
        boolean activeMachineFound = false;
        boolean releaseEvidenceFound = false;
        for (String machineCode : structureRuntimeMachineCodes) {
            if (!StringUtils.equals(
                    physicalMachineCode,
                    LhSingleControlMachineUtil.resolvePhysicalMachineCode(machineCode))
                    || !this.isMachineInStructureAtShift(
                    context, structureName, machineCode, shift.getShiftIndex())) {
                continue;
            }
            activeMachineFound = true;
            List<LhScheduleResult> positiveResultList =
                    this.collectPositiveStructureResults(
                            context, structureName, machineCode,
                            shift.getShiftIndex());
            if (CollectionUtils.isEmpty(positiveResultList)) {
                if (!this.isRuntimeMachineEndingWithinShift(
                        context, machineCode, shift)) {
                    return false;
                }
                releaseEvidenceFound = true;
                continue;
            }
            for (LhScheduleResult result : positiveResultList) {
                if (!this.isResultEndingWithinShift(
                        context, result, shift)) {
                    return false;
                }
                releaseEvidenceFound = true;
            }
        }
        return activeMachineFound && releaseEvidenceFound;
    }

    /**
     * 收集指定结构、机台在目标班次的正量排程结果。
     *
     * @param context 排程上下文
     * @param structureName 产品结构
     * @param machineCode 运行态机台编码
     * @param shiftIndex 目标班次索引
     * @return 正量结果列表
     */
    private List<LhScheduleResult> collectPositiveStructureResults(
            LhScheduleContext context,
            String structureName,
            String machineCode,
            int shiftIndex) {
        List<LhScheduleResult> resultList = new ArrayList<LhScheduleResult>(2);
        for (LhScheduleResult result : context.getScheduleResultList()) {
            if (!this.isResultForStructure(
                    context, result, structureName, machineCode)) {
                continue;
            }
            Integer planQty = ShiftFieldUtil.getShiftPlanQty(result, shiftIndex);
            if (Objects.nonNull(planQty) && planQty > 0) {
                resultList.add(result);
            }
        }
        return resultList;
    }

    /**
     * 判断排程结果是否属于指定结构和机台。
     *
     * @param context 排程上下文
     * @param result 排程结果
     * @param structureName 产品结构
     * @param machineCode 机台编码
     * @return true-属于；false-不属于
     */
    private boolean isResultForStructure(
            LhScheduleContext context,
            LhScheduleResult result,
            String structureName,
            String machineCode) {
        return Objects.nonNull(result)
                && StringUtils.equals(machineCode, result.getLhMachineCode())
                && (StringUtils.equals(structureName, result.getStructureName())
                || this.isSnapshotStructureMaterial(
                context, structureName, result.getMaterialCode()));
    }

    /**
     * 判断正量结果是否在目标班次内完成生产并释放机台。
     *
     * @param context 排程上下文
     * @param result 排程结果
     * @param shift 目标班次
     * @return true-班次内完成并下机；false-仍生产或证据不足
     */
    private boolean isResultEndingWithinShift(
            LhScheduleContext context,
            LhScheduleResult result,
            LhShiftConfigVO shift) {
        int shiftIndex = shift.getShiftIndex();
        if (this.resolveLastPositiveShiftIndex(result) > shiftIndex) {
            // 后续班次仍有正量是“尚未下机”的直接证据，任何当前班收尾标识都不得覆盖该事实。
            return false;
        }
        Date actualEndingTime = ShiftFieldUtil.getShiftEndTime(
                result, shiftIndex);
        if (Objects.isNull(actualEndingTime)) {
            actualEndingTime = result.getSpecEndTime();
        }
        if (!this.isEndingTimeWithinShift(actualEndingTime, shift)) {
            return false;
        }
        boolean shiftEnding = StringUtils.equals(
                "1", ShiftFieldUtil.getShiftIsEnd(result, shiftIndex));
        boolean resultEnding = StringUtils.equals("1", result.getIsEnd())
                && this.resolveLastPositiveShiftIndex(result) == shiftIndex;
        Integer releaseBoundary =
                context.getContinuousReducedMachineReleaseBoundaryShiftIndex(
                        result.getLhMachineCode());
        boolean reducedOffline = Objects.nonNull(releaseBoundary)
                && releaseBoundary <= shiftIndex;
        MachineScheduleDTO machine =
                context.getMachineScheduleMap().get(result.getLhMachineCode());
        boolean runtimeEnding = Objects.nonNull(machine)
                && machine.isEnding()
                && this.isEndingTimeWithinShift(
                machine.getEstimatedEndTime(), shift);
        /*
         * 日驱动新增在当前业务日收口时可能临时把结果标记为收尾，但同一SKU实际消费账本
         * 仍有余量，下一业务日还会继续在原机台生产。只有账本已经归零时，收尾标识和机台
         * ending状态才代表真正完成并下机；真正降模释放边界属于明确下机事实，不受此限制。
         */
        boolean productionCompleted =
                this.isResultProductionCompleted(context, result);
        return reducedOffline
                || (productionCompleted
                && (shiftEnding || resultEnding || runtimeEnding));
    }

    /**
     * 判断无正量结果的运行态机台是否在目标班次内明确下机。
     *
     * @param context 排程上下文
     * @param machineCode 机台编码
     * @param shift 目标班次
     * @return true-班次内明确下机；false-继续占用或证据不足
     */
    private boolean isRuntimeMachineEndingWithinShift(
            LhScheduleContext context,
            String machineCode,
            LhShiftConfigVO shift) {
        MachineScheduleDTO machine =
                context.getMachineScheduleMap().get(machineCode);
        LhScheduleResult latestResult =
                this.resolveLatestMachineResult(context, machineCode);
        if (Objects.nonNull(machine) && machine.isEnding()
                && this.isEndingTimeWithinShift(
                machine.getEstimatedEndTime(), shift)
                && this.isResultProductionCompleted(
                context, latestResult)) {
            return true;
        }
        Integer releaseBoundary =
                context.getContinuousReducedMachineReleaseBoundaryShiftIndex(
                        machineCode);
        if (Objects.isNull(releaseBoundary)
                || releaseBoundary > shift.getShiftIndex()) {
            return false;
        }
        return Objects.nonNull(latestResult)
                && this.isEndingTimeWithinShift(
                latestResult.getSpecEndTime(), shift);
    }

    /**
     * 判断结果对应 SKU 的实际生产剩余账本是否已经归零。
     *
     * <p>能够定位来源 SKU 时优先读取“物料+产品状态”实际消费账本；账本尚未初始化时，
     * 再使用 SKU 当前剩余量和待排量。历史结果缺少来源映射时保持原收尾标识语义。</p>
     *
     * @param context 排程上下文
     * @param result 排程结果
     * @return true-已实际完成；false-仍有后续待排量
     */
    private boolean isResultProductionCompleted(
            LhScheduleContext context,
            LhScheduleResult result) {
        if (Objects.isNull(context) || Objects.isNull(result)) {
            return false;
        }
        SkuScheduleDTO sourceSku =
                context.getScheduleResultSourceSkuMap().get(result);
        if (Objects.isNull(sourceSku)) {
            return true;
        }
        String skuKey = MonthPlanDateResolver.buildMaterialStatusKey(
                sourceSku.getMaterialCode(), sourceSku.getProductStatus());
        Integer ledgerRemainingQty =
                context.getSkuProductionRemainingQtyMap().get(skuKey);
        if (Objects.nonNull(ledgerRemainingQty)) {
            return ledgerRemainingQty <= 0;
        }
        return Math.max(0, sourceSku.getRemainingScheduleQty()) <= 0
                && Math.max(0, sourceSku.getPendingQty()) <= 0;
    }

    /**
     * 判断结束时刻是否位于目标班次内或恰好落在班次结束边界。
     *
     * @param endingTime 实际结束时刻
     * @param shift 目标班次
     * @return true-本班内完成；false-不在本班或时间缺失
     */
    private boolean isEndingTimeWithinShift(
            Date endingTime,
            LhShiftConfigVO shift) {
        return Objects.nonNull(endingTime)
                && Objects.nonNull(shift)
                && Objects.nonNull(shift.getShiftStartDateTime())
                && Objects.nonNull(shift.getShiftEndDateTime())
                && endingTime.after(shift.getShiftStartDateTime())
                && !endingTime.after(shift.getShiftEndDateTime());
    }

    /**
     * 获取结果最后一个正量班次。
     *
     * @param result 排程结果
     * @return 最后正量班次索引；无正量返回-1
     */
    private int resolveLastPositiveShiftIndex(LhScheduleResult result) {
        if (Objects.isNull(result)) {
            return -1;
        }
        for (int shiftIndex = LhScheduleConstant.MAX_SHIFT_SLOT_COUNT;
             shiftIndex >= 1; shiftIndex--) {
            Integer planQty = ShiftFieldUtil.getShiftPlanQty(
                    result, shiftIndex);
            if (Objects.nonNull(planQty) && planQty > 0) {
                return shiftIndex;
            }
        }
        return -1;
    }

    /**
     * 判断其他结构是否已在指定机台班次接管生产。
     */
    private boolean hasPositiveOtherStructurePlan(LhScheduleContext context,
                                                  String structureName,
                                                  String machineCode,
                                                  int shiftIndex) {
        for (LhScheduleResult result : context.getScheduleResultList()) {
            if (Objects.nonNull(result) && StringUtils.equals(machineCode, result.getLhMachineCode())
                    && !StringUtils.equals(structureName, result.getStructureName())
                    && !isSnapshotStructureMaterial(
                    context, structureName, result.getMaterialCode())) {
                Integer planQty = ShiftFieldUtil.getShiftPlanQty(result, shiftIndex);
                if (Objects.nonNull(planQty) && planQty > 0) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * 收集指定结构的全部排程结果。
     */
    private List<LhScheduleResult> collectStructureResults(LhScheduleContext context,
                                                           String structureName) {
        List<LhScheduleResult> structureResults = new ArrayList<LhScheduleResult>(8);
        if (Objects.isNull(context) || CollectionUtils.isEmpty(context.getScheduleResultList())) {
            return structureResults;
        }
        for (LhScheduleResult result : context.getScheduleResultList()) {
            if (Objects.nonNull(result)
                    && (StringUtils.equals(structureName, result.getStructureName())
                    || isSnapshotStructureMaterial(
                    context, structureName, result.getMaterialCode()))) {
                structureResults.add(result);
            }
        }
        return structureResults;
    }

    /**
     * 按班次索引读取当前窗口班次。
     */
    private LhShiftConfigVO resolveShift(LhScheduleContext context, int shiftIndex) {
        if (Objects.isNull(context) || CollectionUtils.isEmpty(context.getScheduleWindowShifts())) {
            return null;
        }
        for (LhShiftConfigVO shift : context.getScheduleWindowShifts()) {
            if (Objects.nonNull(shift) && Objects.equals(shift.getShiftIndex(), shiftIndex)) {
                return shift;
            }
        }
        return null;
    }

    /**
     * 校验同结构SKU的结构类型及年月一致。
     */
    private boolean isConsistentStructureConfig(LhScheduleContext context,
                                                String structureName,
                                                List<SkuScheduleDTO> structureSkuList,
                                                SkuScheduleDTO firstSku) {
        if (Objects.isNull(firstSku)) {
            warnConfigSkip(context, structureName, "结构SKU为空，无法解析最低机台数");
            return false;
        }
        for (SkuScheduleDTO sku : structureSkuList) {
            if (Objects.isNull(sku)
                    || !StringUtils.equals(firstSku.getStructureType(), sku.getStructureType())
                    || !Objects.equals(firstSku.getMonthPlanYear(), sku.getMonthPlanYear())
                    || !Objects.equals(firstSku.getMonthPlanMonth(), sku.getMonthPlanMonth())) {
                warnConfigSkip(context, structureName, "同结构SKU的STRUCTURE_TYPE或月计划年月不一致");
                return false;
            }
        }
        return true;
    }

    /**
     * 查询周期结构最低硫化机台数。
     */
    private int resolveCycleStructureMinimum(LhScheduleContext context,
                                             String structureName,
                                             SkuScheduleDTO sku) {
        if (Objects.isNull(sku.getMonthPlanYear()) || Objects.isNull(sku.getMonthPlanMonth())) {
            warnConfigSkip(context, structureName, "周期结构月计划年份或月份为空");
            return SKIP_MIN_MACHINE_COUNT;
        }
        List<MdmMonCycleSchStruConf> configs = cycleStructureConfigMapper.selectList(
                new LambdaQueryWrapper<MdmMonCycleSchStruConf>()
                        .eq(MdmMonCycleSchStruConf::getFactoryCode, context.getFactoryCode())
                        .eq(MdmMonCycleSchStruConf::getYear, sku.getMonthPlanYear())
                        .eq(MdmMonCycleSchStruConf::getMonth, sku.getMonthPlanMonth())
                        .eq(MdmMonCycleSchStruConf::getStructureName, structureName)
                        .eq(MdmMonCycleSchStruConf::getSourceType, CYCLE_SOURCE_TYPE)
                        .eq(MdmMonCycleSchStruConf::getIsDelete, DeleteFlagEnum.NORMAL.getCode()));
        if (CollectionUtils.isEmpty(configs) || configs.size() != 1) {
            warnConfigSkip(context, structureName,
                    "周期结构最低机台配置应唯一，实际记录数="
                            + (CollectionUtils.isEmpty(configs) ? 0 : configs.size()));
            return SKIP_MIN_MACHINE_COUNT;
        }
        return validateMinimumMachineCount(context, structureName,
                configs.get(0).getMinVulcanizingMachine(),
                "T_DP_MONTH_CYCLE_STRUCT_CONFIG.MIN_VULCANIZING_MACHINE");
    }

    /**
     * 查询常规结构最低硫化机台数。
     */
    private int resolveRegularStructureMinimum(LhScheduleContext context, String structureName) {
        List<FactoryParam> params = factoryParamMapper.selectList(
                new LambdaQueryWrapper<FactoryParam>()
                        .eq(FactoryParam::getFactoryCode, context.getFactoryCode())
                        .eq(FactoryParam::getParamCode,
                                LhScheduleParamConstant.REGULAR_STRUCTURE_MIN_VULCANIZING_MACHINE)
                        .eq(FactoryParam::getIsDelete, DeleteFlagEnum.NORMAL.getCode()));
        if (CollectionUtils.isEmpty(params) || params.size() != 1) {
            warnConfigSkip(context, structureName,
                    "工厂参数SYS0204012应唯一，实际记录数="
                            + (CollectionUtils.isEmpty(params) ? 0 : params.size()));
            return SKIP_MIN_MACHINE_COUNT;
        }
        String paramValue = StringUtils.trim(params.get(0).getParamValue());
        if (StringUtils.isEmpty(paramValue)) {
            warnConfigSkip(context, structureName, "工厂参数SYS0204012的PARAM_VALUE为空");
            return SKIP_MIN_MACHINE_COUNT;
        }
        try {
            return validateMinimumMachineCount(context, structureName, Integer.valueOf(paramValue),
                    "T_MP_FACTORY_PARAM.SYS0204012");
        } catch (NumberFormatException e) {
            warnConfigSkip(context, structureName,
                    "工厂参数SYS0204012格式错误，paramValue=" + paramValue);
            return SKIP_MIN_MACHINE_COUNT;
        }
    }

    /**
     * 校验最低机台数配置。
     */
    private int validateMinimumMachineCount(LhScheduleContext context,
                                            String structureName,
                                            Integer minimumMachineCount,
                                            String configSource) {
        if (Objects.isNull(minimumMachineCount) || minimumMachineCount < 0) {
            warnConfigSkip(context, structureName,
                    configSource + "缺失或小于0，实际值=" + minimumMachineCount);
            return SKIP_MIN_MACHINE_COUNT;
        }
        return minimumMachineCount;
    }

    /**
     * 记录结构最低机台数配置异常并安全跳过，不抛异常中断整体排程。
     */
    private void warnConfigSkip(LhScheduleContext context, String structureName, String reason) {
        log.warn("结构[{}]最低硫化机台数配置异常，安全跳过结构收尾对齐（等价规则未触发）：factoryCode={}, batchNo={}, reason={}",
                structureName, Objects.isNull(context) ? null : context.getFactoryCode(),
                Objects.isNull(context) ? null : context.getBatchNo(), reason);
    }
}
