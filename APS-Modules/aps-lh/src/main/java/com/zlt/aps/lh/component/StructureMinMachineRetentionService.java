package com.zlt.aps.lh.component;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
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
     * <p>优先按S4.3冻结的结构SKU快照归属；快照未命中时回退扫描当前排程结果中的结构名称，
     * 保证机台实时当前物料也能映射到结构。</p>
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
        if (StringUtils.isEmpty(materialCode)) {
            return false;
        }
        if (isSnapshotStructureMaterial(context, structureName, materialCode)) {
            return true;
        }
        for (LhScheduleResult result : collectStructureResults(context, structureName)) {
            if (StringUtils.equals(materialCode, result.getMaterialCode())) {
                return true;
            }
        }
        return false;
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
            if (Objects.nonNull(result) && StringUtils.equals(machineCode, result.getLhMachineCode())
                    && (StringUtils.equals(structureName, result.getStructureName())
                    || isSnapshotStructureMaterial(
                    context, structureName, result.getMaterialCode()))) {
                Integer planQty = ShiftFieldUtil.getShiftPlanQty(result, shiftIndex);
                if (Objects.nonNull(planQty) && planQty > 0) {
                    return true;
                }
            }
        }
        return false;
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
