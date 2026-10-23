package com.zlt.aps.lh.service.impl;
import com.zlt.aps.lh.api.constant.LhScheduleConstant;
import com.zlt.aps.lh.api.constant.LhScheduleParamConstant;
import com.zlt.aps.lh.api.domain.dto.CleaningScheduleDateFillItem;
import com.zlt.aps.lh.api.domain.dto.MachineCleaningWindowDTO;
import com.zlt.aps.lh.api.domain.entity.LhMachineOnlineInfo;
import com.zlt.aps.lh.api.domain.vo.LhShiftConfigVO;
import com.zlt.aps.lh.api.enums.CleaningTypeEnum;
import com.zlt.aps.lh.api.enums.TrialStatusEnum;
import com.zlt.aps.lh.component.MonthPlanDateResolver;
import com.zlt.aps.lh.context.LhScheduleConfig;
import com.zlt.aps.lh.context.LhScheduleContext;
import com.zlt.aps.lh.util.LeftRightMouldUtil;
import com.zlt.aps.lh.util.LhMouldCodeUtil;
import com.zlt.aps.lh.util.LhScheduleTimeUtil;
import com.zlt.aps.lh.util.LhSingleControlMachineUtil;
import com.zlt.aps.lh.util.MachineCleaningOverlapUtil;
import com.zlt.aps.mdm.api.domain.entity.MdmDevicePlanShut;
import com.zlt.aps.mdm.api.domain.entity.MdmSkuMouldRel;
import com.zlt.aps.mdm.api.domain.entity.MdmWorkCalendar;
import com.zlt.aps.mp.api.domain.entity.FactoryMonthPlanProductionFinalResult;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import javax.annotation.Resource;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import com.zlt.aps.lh.engine.strategy.support.CleaningPlanResult;
import com.zlt.aps.lh.engine.strategy.support.CleaningPlanRuntimeState;
import com.zlt.aps.lh.engine.strategy.support.CleaningPlanStatus;
/**
 * 设备停机来源清洗初始化解析，干冰与喷砂分治并复用现有时间、模具和额度规则。
 * 初始化允许发布约束，排产候选的只读解析由重叠解析器负责。
 *
 * @author APS
 * @since 2026/10/19
 */
@Slf4j
@Component
public class LhCleaningPlanResolver {
    /**
     * 设备停机计划数据来源标识
     */
    private static final String DEVICE_STOP_PLAN_DATA_SOURCE = "DEVICE_STOP_PLAN";
    /**
     * SKU 从清洗时间点开始 3 天内可收尾时跳过清洗
     */
    private static final int CLEANING_SKIP_ENDING_DAYS = 3;
    /**
     * 单控物理机台由左右两个运行侧组成
     */
    private static final int SINGLE_CONTROL_SIDE_COUNT = 2;
    /**
     * 工作日历停产标记
     */
    private static final String WORK_CALENDAR_STOP_FLAG = "0";
    /**
     * 启用配置值
     */
    private static final int ENABLED = 1;
    /**
     * 多值分隔符
     */
    private static final String VALUE_SEPARATOR = ",";
    /**
     * 设备停机来源能力，沿用项目现有候选查询规则。
     */
    @Resource
    private LhDeviceStopPlanScheduleService deviceStopPlanScheduleService;
    /**
     * 按清洗约束生成本次排程实际生效的清洗窗口。
     * <p>最终口径：干冰清洗、喷砂清洗统一从设备停机计划读取，旧模具清洗计划列表不再作为排程来源。</p>
     *
     * @param context 排程上下文
     * @return 机台清洗窗口Map
     */
    public Map<String, List<MachineCleaningWindowDTO>> initializePlans(LhScheduleContext context) {
        Map<String, List<MachineCleaningWindowDTO>> cleaningWindowMap = new HashMap<>(16);
        if (Objects.isNull(context)) {
            return cleaningWindowMap;
        }
        List<MdmDevicePlanShut> cleaningPlanList =
                this.getDeviceStopPlanScheduleService().queryCleaningStopPlans(context);
        log.info("清洗计划约束采用设备停机计划运行态调度处理, 原始清洗停机计划数: {}", cleaningPlanList.size());
        Map<String, Integer> dryIceDailyCountMap = new HashMap<>(16);
        Map<String, Integer> dryIceMorningCountMap = new HashMap<>(16);
        Map<String, Integer> dryIceAfternoonCountMap = new HashMap<>(16);
        Map<String, Integer> sandBlastDailyCountMap = new HashMap<>(16);
        for (MdmDevicePlanShut sourcePlan : cleaningPlanList) {
            MdmDevicePlanShut cleaningPlan = this.resolveRuntimeCleaningPlan(context, sourcePlan);
            if (cleaningPlan == null) {
                continue;
            }
            boolean followsOccupation = !StringUtils.equals(sourcePlan.getMachineCode(), cleaningPlan.getMachineCode())
                    && StringUtils.isBlank(sourcePlan.getInMachineMouldCode());
            // 调用设备停机清洗专用调度：沿用计划自然日和既有候选顺序，不提前分配未来候选。
            MachineCleaningWindowDTO cleaningWindow = this.scheduleDeviceStopCleaningWindow(context, cleaningPlan,
                    dryIceDailyCountMap, dryIceMorningCountMap, dryIceAfternoonCountMap, sandBlastDailyCountMap,
                    followsOccupation);
            if (Objects.isNull(cleaningWindow)) {
                continue;
            }
            cleaningWindowMap.computeIfAbsent(cleaningWindow.getLhCode(), key -> new ArrayList<>()).add(cleaningWindow);
            // 单控机台（如 K1501L/K1501R）左右模生产同物料时需一起清洗：
            // 为配对侧机台创建相同时间、相同类型的清洗窗口，不额外消耗每日上限名额。
            MachineCleaningWindowDTO pairedWindow =
                    this.createPairedSingleControlCleaningWindow(context, cleaningWindow);
            if (pairedWindow != null && pairedWindow.getSandBlastFirstInspectionQty() != null) {
                cleaningWindow.setSandBlastFirstInspectionQty(cleaningWindow.getSandBlastFirstInspectionQty()
                        - pairedWindow.getSandBlastFirstInspectionQty());
            }
            this.recordInitialization(context, cleaningWindow, CleaningPlanStatus.SCHEDULED, "初始化安排");
            if (Objects.nonNull(pairedWindow)) {
                this.recordInitialization(context, pairedWindow, CleaningPlanStatus.SCHEDULED, "初始化安排");
                cleaningWindowMap.computeIfAbsent(pairedWindow.getLhCode(), key -> new ArrayList<>()).add(pairedWindow);
            }
        }
        // 清洗类停机已转换为运行态清洗窗口；剥离后普通停机产能扣减只处理维修、精度等真实设备停机。
        context.setDevicePlanShutList(this.getDeviceStopPlanScheduleService()
                .filterNonCleaningStopPlans(context.getDevicePlanShutList()));
        return cleaningWindowMap;
    }
    /**
     * 为单控机台配对侧创建清洗窗口。
     * <p>单控机台（如 K1501L/K1501R）左右模需一起清洗，当一侧机台生成了清洗窗口时，
     * 为配对侧机台创建相同时间、相同类型的清洗窗口，左右模标识按配对侧机台编码确定。
     * 配对窗口不额外消耗每日上限名额（与原窗口共享同一次清洗占用）。</p>
     *
     * @param context 排程上下文
     * @param cleaningWindow 原始清洗窗口
     * @return 配对侧清洗窗口；非单控机台或无配对侧时返回 null
     */
    private MachineCleaningWindowDTO createPairedSingleControlCleaningWindow(LhScheduleContext context,
                                                                             MachineCleaningWindowDTO cleaningWindow) {
        if (Objects.isNull(cleaningWindow) || Objects.isNull(context)
                || StringUtils.isEmpty(cleaningWindow.getLhCode())) {
            return null;
        }
        String machineCode = cleaningWindow.getLhCode();
        // 非单控机台无需配对
        if (!LhSingleControlMachineUtil.isSingleMouldMachine(machineCode)) {
            return null;
        }
        String pairMachineCode = LhSingleControlMachineUtil.resolvePairMachineCode(machineCode);
        if (StringUtils.isEmpty(pairMachineCode)) {
            return null;
        }
        // 配对侧机台需在排程机台中存在
        Map<String, ?> machineScheduleMap = context.getMachineScheduleMap();
        // 初始化阶段运行态机台Map尚未构建完成，喷砂配对存在性使用已加载机台主数据。
        // 此次只补齐喷砂事件的两侧守恒，干冰沿用原有运行态配对准入。
        boolean pairExists = (Objects.nonNull(machineScheduleMap) && machineScheduleMap.containsKey(pairMachineCode))
                || context.getMachineInfoMap().containsKey(pairMachineCode);
        if (!pairExists) {
            return null;
        }
        // 创建配对侧清洗窗口，时间、类型与原窗口一致，左右模按配对侧机台编码确定
        MachineCleaningWindowDTO pairedWindow = new MachineCleaningWindowDTO();
        // 配对窗口继承来源计划主键，最终阶段按同一主键去重回填，不产生第二条停机计划更新。
        pairedWindow.setSourcePlanId(cleaningWindow.getSourcePlanId());
        pairedWindow.setSourceMaterialCode(cleaningWindow.getSourceMaterialCode());
        pairedWindow.setSourceProductStatus(cleaningWindow.getSourceProductStatus());
        pairedWindow.setSourceFollowsMachineOccupation(cleaningWindow.isSourceFollowsMachineOccupation());
        LhMachineOnlineInfo pairedOnline = this.resolveMachineOnlineInfo(context, pairMachineCode);
        if (Objects.nonNull(pairedOnline)) {
            pairedWindow.setSourceMaterialCode(pairedOnline.getMaterialCode());
            pairedWindow.setSourceProductStatus(this.normalizeProductStatus(pairedOnline.getProductStatus()));
        }
        pairedWindow.setLhCode(pairMachineCode);
        pairedWindow.setCleanType(cleaningWindow.getCleanType());
        pairedWindow.setLeftRightMould(LeftRightMouldUtil.resolveCleaningLeftRightMould(pairMachineCode));
        pairedWindow.setMouldCode(this.resolveCleaningMouldCode(context, pairMachineCode));
        pairedWindow.setCleanStartTime(cleaningWindow.getCleanStartTime());
        pairedWindow.setCleanEndTime(cleaningWindow.getCleanEndTime());
        pairedWindow.setReadyTime(cleaningWindow.getReadyTime());
        pairedWindow.setSandBlastSequence(cleaningWindow.getSandBlastSequence());
        pairedWindow.setSandBlastInspectionShiftStartTime(cleaningWindow.getSandBlastInspectionShiftStartTime());
        // 同一物理喷砂事件共享参数条数；奇数尾差固定给编码靠前的运行侧，保证总量守恒。
        Integer inspectionQty = cleaningWindow.getSandBlastFirstInspectionQty();
        if (Objects.nonNull(inspectionQty)) {
            int firstSideQty = inspectionQty / SINGLE_CONTROL_SIDE_COUNT + inspectionQty % SINGLE_CONTROL_SIDE_COUNT;
            int secondSideQty = inspectionQty / SINGLE_CONTROL_SIDE_COUNT;
            boolean originalFirst = machineCode.compareTo(pairMachineCode) < 0;
            pairedWindow.setSandBlastFirstInspectionQty(originalFirst ? secondSideQty : firstSideQty);
        }
        pairedWindow.setSourcePlanStartTime(cleaningWindow.getSourcePlanStartTime());
        pairedWindow.setSourcePlanEndTime(cleaningWindow.getSourcePlanEndTime());
        pairedWindow.setDataSource(cleaningWindow.getDataSource());
        pairedWindow.setRemark(cleaningWindow.getRemark());
        log.info("单控机台配对侧清洗窗口创建, 原机台: {}, 配对机台: {}, 清洗类型: {}, 清洗开始: {}",
                machineCode, pairMachineCode, cleaningWindow.getCleanType(),
                LhScheduleTimeUtil.formatDateTime(cleaningWindow.getCleanStartTime()));
        return pairedWindow;
    }
    /**
     * 调度单条设备停机清洗计划。
     *
     * @param context 排程上下文
     * @param cleaningPlan 设备停机清洗计划
     * @param dryIceDailyCountMap 干冰每日已安排台数
     * @param dryIceMorningCountMap 干冰早班已安排台数
     * @param dryIceAfternoonCountMap 干冰中班已安排台数
     * @param sandBlastDailyCountMap 喷砂每日已安排台数
     * @param followsOccupation 未指定模具的物理整机来源是否按执行时真实占用确定物料
     * @return 生效清洗窗口；不满足规则时返回 null
     */
    private MachineCleaningWindowDTO scheduleDeviceStopCleaningWindow(LhScheduleContext context,
                                                                      MdmDevicePlanShut cleaningPlan,
                                                                      Map<String, Integer> dryIceDailyCountMap,
                                                                      Map<String, Integer> dryIceMorningCountMap,
                                                                      Map<String, Integer> dryIceAfternoonCountMap,
                                                                      Map<String, Integer> sandBlastDailyCountMap,
                                                                      boolean followsOccupation) {
        String cleanType = this.getDeviceStopPlanScheduleService().resolveCleaningType(cleaningPlan);
        if (StringUtils.isEmpty(cleanType)) {
            this.recordSkip(context, cleaningPlan, cleanType, "非清洗类型");
            return null;
        }
        // 清洗窗口占用机台前，按本批次参数决定是否校验计划与实际在机模具。
        if (!this.isCleaningPlanMouldMatched(context, cleaningPlan, cleanType)) {
            this.recordSkip(context, cleaningPlan, cleanType, "在机模具校验未通过");
            return null;
        }
        int cleanDurationHours = this.resolveCleanDurationHours(context, cleanType);
        if (cleanDurationHours <= 0) {
            this.recordSkip(context, cleaningPlan, cleanType, "清洗时长配置非法");
            log.warn("设备停机清洗时长配置非法，跳过清洗, 机台: {}, 清洗类型: {}, 计划开始: {}",
                    cleaningPlan.getMachineCode(), cleanType,
                    LhScheduleTimeUtil.formatDateTime(cleaningPlan.getBeginDate()));
            return null;
        }
        LhMachineOnlineInfo machineOnlineInfo = this.resolveMachineOnlineInfo(context, cleaningPlan.getMachineCode());
        String machineMaterial = Objects.isNull(machineOnlineInfo) ? null : machineOnlineInfo.getMaterialCode();
        String productStatus = Objects.isNull(machineOnlineInfo) ? null : machineOnlineInfo.getProductStatus();
        if (CleaningTypeEnum.DRY_ICE.getCode().equals(cleanType) && cleaningPlan.getBeginDate() != null) {
            // 正规换模覆盖来源时不执行独立清洗，不能用额度顺延后的备用时间否定原计划组合。
            Date normalizedStart = this.resolveDryIceShiftStartTime(context, cleaningPlan.getBeginDate());
            if (normalizedStart != null) {
                MachineCleaningWindowDTO source = this.buildCleaningWindow(
                        context, cleaningPlan, cleanType, normalizedStart, cleanDurationHours);
                source.setSourceMaterialCode(machineMaterial);
                source.setSourceProductStatus(this.normalizeProductStatus(productStatus));
                source.setSourceFollowsMachineOccupation(followsOccupation);
                this.rememberSourceWindow(context, source);
            }
        }
        // 按计划日期（beginDate）定位实际清洗班次，不再从排程窗口 T 日提前寻找空档
        Date cleanStartTime = this.resolveNextDeviceStopCleaningStartTime(context, cleanType,
                cleaningPlan.getBeginDate(),
                dryIceDailyCountMap, dryIceMorningCountMap, dryIceAfternoonCountMap, sandBlastDailyCountMap);
        if (Objects.isNull(cleanStartTime)) {
            this.recordSkip(context, cleaningPlan, cleanType, "额度或日期条件不满足");
            log.info("清洗计划超过可安排上限，本次不纳入, 机台: {}, 类型: {}, 计划开始: {}",
                    cleaningPlan.getMachineCode(), cleanType,
                            LhScheduleTimeUtil.formatDateTime(cleaningPlan.getBeginDate()));
            return null;
        }
        MachineCleaningWindowDTO cleaningWindow =
                this.buildCleaningWindow(context, cleaningPlan, cleanType, cleanStartTime, cleanDurationHours);
        cleaningWindow.setSourceMaterialCode(machineMaterial);
        cleaningWindow.setSourceProductStatus(this.normalizeProductStatus(productStatus));
        cleaningWindow.setSourceFollowsMachineOccupation(followsOccupation);
        if (CleaningTypeEnum.SAND_BLAST.getCode().equals(cleanType)) {
            cleaningWindow.setSandBlastSequence(sandBlastDailyCountMap.getOrDefault(
                    LhScheduleTimeUtil.formatDate(cleanStartTime), 0) + 1);
            // 组合资格和最终承接必须比较实际装机组，不能把该物料所有可用模具当成原模具。
            MachineCleaningWindowDTO exactSource = CleaningPlanRuntimeState.copyWindow(cleaningWindow);
            exactSource.setMouldCode(LhMouldCodeUtil.resolveInMachineMouldCode(
                    context, cleaningPlan.getMachineCode()));
            if (LhCleaningRepairOverlapPolicy.hasPlannedSandBlastHandoff(context, exactSource)) {
                cleaningWindow.setMouldCode(exactSource.getMouldCode());
            }
            this.rememberSourceWindow(context, cleaningWindow);
        }
        // 普通收尾预判不占清洗名额；干冰与05重叠时保留名额供后续真实组合兑现。
        boolean sandBlastHandoff = LhCleaningRepairOverlapPolicy.hasPlannedSandBlastHandoff(context, cleaningWindow);
        if (!followsOccupation && !sandBlastHandoff
                && this.isMachineEndingWithinThreeDays(context, cleaningPlan.getMachineCode(), cleanStartTime,
                machineMaterial, productStatus)) {
            log.info("机台当前物料3天内可收尾，跳过清洗, 机台: {}, 物料: {}, 清洗类型: {}, 清洗时间: {}",
                    cleaningPlan.getMachineCode(),
                    StringUtils.isEmpty(machineMaterial) ? "N/A" : machineMaterial,
                    cleanType, LhScheduleTimeUtil.formatDateTime(cleanStartTime));
            // 因 SKU 收尾未安排清洗：回填写该 SKU 收尾日期（候选清洗开始日 + 剩余排产天数 N）
            // 与05重叠的干冰保留本班名额供真实切换组合使用，但此时不占用前料生产容量。
            if (LhCleaningRepairOverlapPolicy.hasRepairOverlap(context, cleaningWindow)) {
                this.increaseDeviceStopCleaningUsage(context, dryIceDailyCountMap, dryIceMorningCountMap,
                        dryIceAfternoonCountMap, sandBlastDailyCountMap, cleanType, cleanStartTime);
                this.rememberIndependentQuota(context, cleaningWindow);
            }
            this.recordInitialization(context, cleaningWindow, CleaningPlanStatus.SKIPPED,
                    "三天内收尾预判跳过，保留来源供最终组合复核");
            this.collectEndingSkipScheduleDateFill(context, cleaningPlan, cleanType, cleanStartTime,
                    machineMaterial, productStatus);
            return null;
        }
        this.increaseDeviceStopCleaningUsage(context, dryIceDailyCountMap, dryIceMorningCountMap,
                dryIceAfternoonCountMap, sandBlastDailyCountMap, cleanType, cleanStartTime);
        this.rememberIndependentQuota(context, cleaningWindow);
        if (CleaningTypeEnum.SAND_BLAST.getCode().equals(cleanType)) {
            cleaningWindow.setSandBlastSequence(
                    sandBlastDailyCountMap.get(LhScheduleTimeUtil.formatDate(cleanStartTime)));
        }
        log.info("设备停机清洗纳入本次窗口, 机台: {}, 类型: {}, 计划开始: {}, 实际清洗开始: {}, "
                        + "清洗结束: {}, 机台释放: {}, 清洗时长小时: {}, 喷砂首检时长小时: {}, 是否纳入清洗窗口: {}",
                cleaningPlan.getMachineCode(), cleanType,
                LhScheduleTimeUtil.formatDateTime(cleaningPlan.getBeginDate()),
                LhScheduleTimeUtil.formatDateTime(cleanStartTime),
                LhScheduleTimeUtil.formatDateTime(cleaningWindow.getCleanEndTime()),
                LhScheduleTimeUtil.formatDateTime(cleaningWindow.getReadyTime()), cleanDurationHours,
                CleaningTypeEnum.SAND_BLAST.getCode().equals(cleanType)
                        ? context.getParamIntValue(LhScheduleParamConstant.SAND_BLAST_FIRST_INSPECTION_HOURS,
                        LhScheduleConstant.SAND_BLAST_FIRST_INSPECTION_HOURS) : 0, true);
        // 清洗成功排程：回填实际清洗开始时间到设备停机计划排程日期
        this.collectCleaningScheduleDateFill(context, cleaningPlan, cleanType, cleanStartTime, "清洗成功");
        return cleaningWindow;
    }

    /**
     * 将物理整机来源映射到已加载的主运行侧，来源主键与原始计划时刻保持不变。
     * 配对侧由原共享事件入口派生，不能重复消耗清洗名额或回填同一来源。
     *
     * @param context 已加载的机台主数据
     * @param source 原始设备清洗计划
     * @return 运行侧副本；非拆分整机时复用原来源，整机模具校验失败时为空
     */
    private MdmDevicePlanShut resolveRuntimeCleaningPlan(LhScheduleContext context, MdmDevicePlanShut source) {
        String physical = source.getMachineCode();
        if (StringUtils.isBlank(physical) || context.getMachineInfoMap().containsKey(physical)
                || !context.getMachineInfoMap().containsKey(physical + "L")
                || !context.getMachineInfoMap().containsKey(physical + "R")) {
            return source;
        }
        MdmDevicePlanShut runtime = com.zlt.aps.lh.engine.strategy.support.RepairPlanRuntimeState.copySource(source);
        runtime.setMachineCode(physical + "L");
        if (context.getScheduleConfig() != null && context.getScheduleConfig().isCleaningOnlineMouldCheckEnabled()
                && StringUtils.isNotBlank(source.getInMachineMouldCode())) {
            String leftMoulds = LhMouldCodeUtil.resolveInMachineMouldCode(context, physical + "L");
            String rightMoulds = LhMouldCodeUtil.resolveInMachineMouldCode(context, physical + "R");
            String actualMoulds = StringUtils.join(new String[] {leftMoulds, rightMoulds}, ",");
            if (StringUtils.isBlank(leftMoulds) || StringUtils.isBlank(rightMoulds)
                    || !LhMouldCodeUtil.isSameMouldCodeSet(actualMoulds, source.getInMachineMouldCode())) {
                this.recordSkip(context, source,
                        this.getDeviceStopPlanScheduleService().resolveCleaningType(source), "整机在机模具集合不匹配");
                log.info("整机清洗模具校验未通过, 来源: {}, 物理机台: {}, 实际模具: {}, 计划模具: {}",
                        source.getId(), physical, actualMoulds, source.getInMachineMouldCode());
                return null;
            }
            // 整机集合已完整校验，运行侧继续使用本侧集合，不能让一侧匹配替代整机匹配。
            runtime.setInMachineMouldCode(leftMoulds);
        }
        return runtime;
    }
    /**
     * 按来源主键和运行侧冻结完整窗口，初始化收尾预判不能抹去最终裁决所需的来源身份。
     * 配对侧沿用原资格判断，只有实际建立配对清洗时才保留对应运行侧。
     *
     * @param context 当前批次清洗运行态
     * @param window 已通过日期和模具校验的归一来源窗口
     */
    private void rememberSourceWindow(LhScheduleContext context, MachineCleaningWindowDTO window) {
        List<MachineCleaningWindowDTO> sources = context.getCleaningPlanRuntimeState().getSourceWindowMap()
                .computeIfAbsent(window.getLhCode(), machineCode -> new ArrayList<>());
        sources.removeIf(source -> Objects.equals(source.getSourcePlanId(), window.getSourcePlanId()));
        sources.add(CleaningPlanRuntimeState.copyWindow(window));
        MachineCleaningWindowDTO paired = this.createPairedSingleControlCleaningWindow(context, window);
        if (paired != null) {
            List<MachineCleaningWindowDTO> pairedSources = context.getCleaningPlanRuntimeState().getSourceWindowMap()
                    .computeIfAbsent(paired.getLhCode(), machineCode -> new ArrayList<>());
            pairedSources.removeIf(source -> Objects.equals(source.getSourcePlanId(), paired.getSourcePlanId()));
            pairedSources.add(CleaningPlanRuntimeState.copyWindow(paired));
        }
    }
    /**
     * 记录独立干冰已取得名额的实际班次；配对侧共享同一次物理清洗，不重复递增额度。
     *
     * @param context 当前批次
     * @param window 已在本次初始化登记独立清洗名额的窗口
     */
    private void rememberIndependentQuota(LhScheduleContext context, MachineCleaningWindowDTO window) {
        if (!MachineCleaningOverlapUtil.isDryIceCleaning(window)
                && !MachineCleaningOverlapUtil.isSandBlastCleaning(window)) {
            return;
        }
        Map<String, Date> reservations = context.getCleaningPlanRuntimeState().getIndependentQuotaStartMap();
        reservations.put(LhCleaningRepairOverlapPolicy.sourceKey(window), (Date) window.getCleanStartTime().clone());
        MachineCleaningWindowDTO paired = this.createPairedSingleControlCleaningWindow(context, window);
        if (paired != null) {
            reservations.put(LhCleaningRepairOverlapPolicy.sourceKey(paired),
                    (Date) paired.getCleanStartTime().clone());
        }
    }
    /**
     * 校验设备停机清洗计划与当前实际在机模具是否一致。
     *
     * <p>开关默认关闭，关闭时直接继续后续清洗规则；开启时才执行以下模具比较。</p>
     * <p>实际值只使用本次排程加载的 MES 在机模具快照，不根据在机物料回查 SKU 模具关系，
     * 避免把可用模具误当成当前实际在机模具。喷砂和干冰均从统一清洗入口调用本方法。</p>
     * <p>计划未维护有效模具号时，仅免除本次一致性校验，后续清洗时间、名额、收尾及窗口逻辑保持不变。</p>
     *
     * @param context 排程上下文
     * @param cleaningPlan 设备停机清洗计划
     * @param cleanType 清洗类型
     * @return 关闭校验、计划模具为空或集合一致时返回true；开启且实际模具缺失或不一致时返回false
     */
    private boolean isCleaningPlanMouldMatched(LhScheduleContext context,
                                               MdmDevicePlanShut cleaningPlan,
                                               String cleanType) {
        LhScheduleConfig scheduleConfig = context.getScheduleConfig();
        if (Objects.isNull(scheduleConfig) || !scheduleConfig.isCleaningOnlineMouldCheckEnabled()) {
            log.info("清洗在机模具一致性校验开关关闭，跳过一致性校验，继续后续清洗规则, "
                            + "工厂: {}, 批次: {}, 机台: {}, 清洗类型: {}, 清洗计划ID: {}, 是否纳入清洗窗口: {}",
                    context.getFactoryCode(), context.getBatchNo(), cleaningPlan.getMachineCode(), cleanType,
                    cleaningPlan.getId(), "待后续规则确认");
            return true;
        }
        String plannedMouldCode = cleaningPlan.getInMachineMouldCode();
        boolean plannedMouldMissing = CollectionUtils.isEmpty(LhMouldCodeUtil.normalizeAndSortMouldCodes(
                Collections.singletonList(plannedMouldCode)));
        if (plannedMouldMissing) {
            // 计划未维护模具号时保持原清洗逻辑，不以缺失字段阻断喷砂或干冰清洗。
            log.info("清洗计划未维护 IN_MACHINE_MOULD_CODE，跳过在机模具一致性校验，继续按原清洗规则处理, "
                            + "机台编码: {}, 清洗类型: {}, 清洗计划ID: {}, 是否纳入清洗窗口: {}",
                    cleaningPlan.getMachineCode(), cleanType, cleaningPlan.getId(), "待后续规则确认");
            return true;
        }
        String actualMouldCode = LhMouldCodeUtil.resolveInMachineMouldCode(
                context, cleaningPlan.getMachineCode());
        boolean matched = LhMouldCodeUtil.isSameMouldCodeSet(actualMouldCode, plannedMouldCode);
        if (!matched) {
            String skipReason = this.resolveCleaningPlanMouldSkipReason(actualMouldCode);
            log.info("跳过清洗计划：当前在机模具与清洗计划维护的 IN_MACHINE_MOULD_CODE 不一致, "
                            + "机台编码: {}, 清洗类型: {}, 清洗计划ID: {}, 实际在机模具: {}, "
                            + "IN_MACHINE_MOULD_CODE: {}, 是否匹配: {}, 是否纳入清洗窗口: {}, 跳过原因: {}",
                    cleaningPlan.getMachineCode(), cleanType, cleaningPlan.getId(), actualMouldCode,
                    plannedMouldCode, false, false, skipReason);
            return false;
        }
        log.info("清洗计划在机模具校验通过, 机台编码: {}, 清洗类型: {}, 清洗计划ID: {}, "
                        + "实际在机模具: {}, IN_MACHINE_MOULD_CODE: {}, 是否匹配: {}, 是否纳入清洗窗口: {}",
                cleaningPlan.getMachineCode(), cleanType, cleaningPlan.getId(), actualMouldCode,
                plannedMouldCode, true, "待后续规则确认");
        return true;
    }
    /**
     * 解析清洗计划在机模具校验失败原因。
     *
     * @param actualMouldCode 实际在机模具号
     * @return 校验失败原因
     */
    private String resolveCleaningPlanMouldSkipReason(String actualMouldCode) {
        boolean actualMouldMissing = CollectionUtils.isEmpty(LhMouldCodeUtil.normalizeAndSortMouldCodes(
                Collections.singletonList(actualMouldCode)));
        if (actualMouldMissing) {
            return "实际在机模具缺失";
        }
        return "模具集合不一致";
    }
    /**
     * 解析下一条设备停机来源清洗的实际清洗开始时间。
     * <p>以计划日期（beginDate）为基准定位清洗班次，不再从排程窗口 T 日提前寻找空档；
     * 额度满时沿用原规则跳过；干冰仅尝试当天中班。</p>
     *
     * @param context 排程上下文
     * @param cleanType 清洗类型
     * @param planBeginTime 设备停机清洗计划开始时间
     * @param dryIceDailyCountMap 干冰每日已安排台数
     * @param dryIceMorningCountMap 干冰早班已安排台数
     * @param dryIceAfternoonCountMap 干冰中班已安排台数
     * @param sandBlastDailyCountMap 喷砂每日已安排台数
     * @return 实际清洗开始时间；搜索上限内无可用班次时返回 null
     */
    private Date resolveNextDeviceStopCleaningStartTime(LhScheduleContext context,
                                                        String cleanType,
                                                        Date planBeginTime,
                                                        Map<String, Integer> dryIceDailyCountMap,
                                                        Map<String, Integer> dryIceMorningCountMap,
                                                        Map<String, Integer> dryIceAfternoonCountMap,
                                                        Map<String, Integer> sandBlastDailyCountMap) {
        if (CleaningTypeEnum.SAND_BLAST.getCode().equals(cleanType)) {
            return this.resolveSandBlastPlan(context, planBeginTime, sandBlastDailyCountMap);
        }
        if (CleaningTypeEnum.DRY_ICE.getCode().equals(cleanType)) {
            return this.resolveDryIcePlan(context, planBeginTime, dryIceDailyCountMap,
                    dryIceMorningCountMap, dryIceAfternoonCountMap);
        }
        return null;
    }
    /**
     * 解析设备停机来源干冰清洗从计划日期开始的早班/中班开始时间。
     * <p>按计划日期安排，不提前、不顺延：计划日期当天班次额度满时直接放过（返回 null）。</p>
     *
     * @param context 排程上下文
     * @param planBeginTime 设备停机清洗计划开始时间
     * @param dryIceDailyCountMap 干冰每日已安排台数
     * @param dryIceMorningCountMap 干冰早班已安排台数
     * @param dryIceAfternoonCountMap 干冰中班已安排台数
     * @return 实际干冰清洗开始时间；无可用班次时返回 null
     */
    private Date resolveDryIcePlan(LhScheduleContext context,
                                                      Date planBeginTime,
                                                      Map<String, Integer> dryIceDailyCountMap,
                                                      Map<String, Integer> dryIceMorningCountMap,
                                                      Map<String, Integer> dryIceAfternoonCountMap) {
        if (Objects.isNull(planBeginTime)) {
            return null;
        }
        int dryIceDailyLimit = context.getParamIntValue(
                LhScheduleParamConstant.DRY_ICE_DAILY_LIMIT, LhScheduleConstant.DRY_ICE_DAILY_LIMIT);
        int morningLimit = context.getParamIntValue(LhScheduleParamConstant.DRY_ICE_MORNING_SHIFT_LIMIT,
                LhScheduleConstant.DRY_ICE_MORNING_SHIFT_LIMIT);
        int afternoonLimit = context.getParamIntValue(LhScheduleParamConstant.DRY_ICE_AFTERNOON_SHIFT_LIMIT,
                LhScheduleConstant.DRY_ICE_AFTERNOON_SHIFT_LIMIT);
        // 起点按计划时间点归一为干冰可执行班次：早班前/早班内→当天早班；中班内→当天中班；中班后/夜班→次日早班
        Date candidateStartTime = this.resolveDryIceShiftStartTime(context, planBeginTime);
        Date currentDate = LhScheduleTimeUtil.clearTime(candidateStartTime);
        String dateKey = LhScheduleTimeUtil.formatDate(currentDate);
        // 按计划日期安排，不提前不顺延：当日额度满就放过
        if (dryIceDailyCountMap.getOrDefault(dateKey, 0) >= dryIceDailyLimit) {
            return null;
        }
        if (LhScheduleTimeUtil.isMorningShift(context, candidateStartTime)) {
            if (dryIceMorningCountMap.getOrDefault(dateKey, 0) < morningLimit) {
                return candidateStartTime;
            }
            // 早班额度满，尝试当日中班
            Date afternoonStartTime = LhScheduleTimeUtil.getAfternoonShiftStart(context, currentDate);
            if (dryIceAfternoonCountMap.getOrDefault(dateKey, 0) < afternoonLimit) {
                return afternoonStartTime;
            }
        } else if (LhScheduleTimeUtil.isAfternoonShift(context, candidateStartTime)) {
            if (dryIceAfternoonCountMap.getOrDefault(dateKey, 0) < afternoonLimit) {
                return candidateStartTime;
            }
        }
        return null;
    }
    /**
     * 将计划时间点归一为干冰可执行班次开始时间。
     *
     * @param context 排程上下文
     * @param planBeginTime 设备停机清洗计划开始时间
     * @return 归一后的班次开始时间（早班或中班）
     */
    private Date resolveDryIceShiftStartTime(LhScheduleContext context, Date planBeginTime) {
        if (LhScheduleTimeUtil.isMorningShift(context, planBeginTime)) {
            return LhScheduleTimeUtil.getMorningShiftStart(context, planBeginTime);
        }
        if (LhScheduleTimeUtil.isAfternoonShift(context, planBeginTime)) {
            return LhScheduleTimeUtil.getAfternoonShiftStart(context, planBeginTime);
        }
        Date morningStart = LhScheduleTimeUtil.getMorningShiftStart(context, planBeginTime);
        if (planBeginTime.before(morningStart)) {
            return morningStart;
        }
        // 中班后或夜班 → 次日早班
        return LhScheduleTimeUtil.getMorningShiftStart(context, LhScheduleTimeUtil.addDays(planBeginTime, 1));
    }
    /**
     * 按计划自然日及当天实际安排顺序确定喷砂开始时间。
     * <p>沿用设备计划排序和跳过规则，第一台、第二台分别读取两档开始时刻；
     * 超额或禁止日期不顺延，收尾跳过的候选不消耗序号。</p>
     *
     * @param context 排程上下文
     * @param planBeginTime 设备计划开始时间，仅取自然日
     * @param sandBlastDailyCountMap 每日已经安排的喷砂台数
     * @return 喷砂开始时间；当天不能安排时返回空
     */
    private Date resolveSandBlastPlan(LhScheduleContext context,
                                                         Date planBeginTime,
                                                         Map<String, Integer> sandBlastDailyCountMap) {
        if (Objects.isNull(planBeginTime)) {
            return null;
        }
        String dateKey = LhScheduleTimeUtil.formatDate(planBeginTime);
        int usedCount = sandBlastDailyCountMap.getOrDefault(dateKey, 0);
        int dailyLimit = Math.min(LhScheduleConstant.SAND_BLAST_MAX_DAILY_LIMIT,
                context.getParamIntValue(LhScheduleParamConstant.SAND_BLAST_DAILY_LIMIT,
                        LhScheduleConstant.SAND_BLAST_DAILY_LIMIT));
        if (usedCount >= dailyLimit || this.isSandBlastForbiddenDate(context, planBeginTime)) {
            return null;
        }
        String startTimeText = usedCount == 0
                ? context.getParamValue(LhScheduleParamConstant.SAND_BLAST_FIRST_START_TIME,
                        LhScheduleConstant.SAND_BLAST_FIRST_START_TIME)
                : context.getParamValue(LhScheduleParamConstant.SAND_BLAST_SECOND_START_TIME,
                        LhScheduleConstant.SAND_BLAST_SECOND_START_TIME);
        LocalTime startTime = LocalTime.parse(startTimeText);
        return LhScheduleTimeUtil.buildTime(planBeginTime, startTime.getHour(), startTime.getMinute(), 0);
    }
    /**
     * 构建设备停机来源的清洗窗口。
     *
     * @param context 排程上下文
     * @param cleaningPlan 设备停机清洗计划
     * @param cleanType 清洗类型
     * @param cleanStartTime 实际清洗开始时间
     * @param cleanDurationHours 清洗持续小时数
     * @return 清洗窗口
     */
    private MachineCleaningWindowDTO buildCleaningWindow(LhScheduleContext context,
                                                         MdmDevicePlanShut cleaningPlan,
                                                         String cleanType,
                                                         Date cleanStartTime,
                                                         int cleanDurationHours) {
        MachineCleaningWindowDTO cleaningWindow = new MachineCleaningWindowDTO();
        cleaningWindow.setSourcePlanId(cleaningPlan.getId());
        cleaningWindow.setLhCode(cleaningPlan.getMachineCode());
        cleaningWindow.setCleanType(cleanType);
        // 单控机台按机台编码后缀确定左/右模，双模机台统一 LR
        cleaningWindow.setLeftRightMould(
                LeftRightMouldUtil.resolveCleaningLeftRightMould(cleaningPlan.getMachineCode()));
        cleaningWindow.setMouldCode(this.resolveCleaningMouldCode(context, cleaningPlan.getMachineCode()));
        cleaningWindow.setCleanStartTime(cleanStartTime);
        // 喷砂清洗结束与首检结束分开记录，后续机台可用时间统一取 readyTime。
        int readyDurationHours = this.resolveReadyDurationHours(context, cleanType, cleanDurationHours);
        Date cleanEndTime = LhScheduleTimeUtil.addHours(cleanStartTime, cleanDurationHours);
        Date readyTime = LhScheduleTimeUtil.addHours(cleanStartTime, readyDurationHours);
        cleaningWindow.setCleanEndTime(cleanEndTime);
        cleaningWindow.setReadyTime(readyTime);
        // 初始化一次参数快照，候选预演、正式排量和保存核验共用同一份首检事件。
        this.initializeSandBlastInspection(context, cleaningWindow);
        // 保留来源设备停机计划窗口，后续换模重叠判断用计划窗口识别“清洗+换模”，但不作为实际清洗时间。
        cleaningWindow.setSourcePlanStartTime(cleaningPlan.getBeginDate());
        cleaningWindow.setSourcePlanEndTime(cleaningPlan.getEndDate());
        cleaningWindow.setDataSource(DEVICE_STOP_PLAN_DATA_SOURCE);
        cleaningWindow.setRemark(cleaningPlan.getRemark());
        return cleaningWindow;
    }
    /**
     * 登记设备停机来源清洗占用的每日/班次名额。
     *
     * @param context 排程上下文
     * @param dryIceDailyCountMap 干冰每日已安排台数
     * @param dryIceMorningCountMap 干冰早班已安排台数
     * @param dryIceAfternoonCountMap 干冰中班已安排台数
     * @param sandBlastDailyCountMap 喷砂每日已安排台数
     * @param cleanType 清洗类型
     * @param cleanStartTime 实际清洗开始时间
     */
    private void increaseDeviceStopCleaningUsage(LhScheduleContext context,
                                                 Map<String, Integer> dryIceDailyCountMap,
                                                 Map<String, Integer> dryIceMorningCountMap,
                                                 Map<String, Integer> dryIceAfternoonCountMap,
                                                 Map<String, Integer> sandBlastDailyCountMap,
                                                 String cleanType,
                                                 Date cleanStartTime) {
        String dateKey = LhScheduleTimeUtil.formatDate(cleanStartTime);
        if (CleaningTypeEnum.SAND_BLAST.getCode().equals(cleanType)) {
            this.increaseCount(sandBlastDailyCountMap, dateKey);
            return;
        }
        if (CleaningTypeEnum.DRY_ICE.getCode().equals(cleanType)) {
            this.increaseCount(dryIceDailyCountMap, dateKey);
            if (this.getDeviceStopPlanScheduleService().isMorningShift(context, cleanStartTime)) {
                this.increaseCount(dryIceMorningCountMap, dateKey);
            } else if (this.getDeviceStopPlanScheduleService().isAfternoonShift(context, cleanStartTime)) {
                this.increaseCount(dryIceAfternoonCountMap, dateKey);
            }
        }
    }
    /**
     * 收集清洗排程日期回填项（通用）。
     * <p>清洗成功排程或因 SKU 收尾跳过清洗时，将回填项追加到排程上下文，
     * 由排程结果落库事务统一回填到 {@code T_MDM_DEVICE_PLAN_SHUT.SCHEDULE_DATE}。</p>
     *
     * @param context      排程上下文
     * @param cleaningPlan 设备停机清洗计划
     * @param cleanType    清洗类型（干冰/喷砂）
     * @param scheduleDate 回填排程日期（实际清洗开始时间 或 收尾日期）
     * @param fillReason   回填原因
     */
    private void collectCleaningScheduleDateFill(LhScheduleContext context,
                                                MdmDevicePlanShut cleaningPlan,
                                                String cleanType,
                                                Date scheduleDate,
                                                String fillReason) {
        if (Objects.isNull(context) || Objects.isNull(cleaningPlan) || Objects.isNull(scheduleDate)) {
            return;
        }
        CleaningScheduleDateFillItem fillItem = new CleaningScheduleDateFillItem();
        fillItem.setPlanId(cleaningPlan.getId());
        fillItem.setScheduleDate(scheduleDate);
        fillItem.setCleanType(cleanType);
        fillItem.setMachineCode(cleaningPlan.getMachineCode());
        fillItem.setFillReason(fillReason);
        context.getCleaningScheduleDateFillList().add(fillItem);
    }
    /**
     * 因 SKU 收尾未安排清洗时，收集收尾日期回填项。
     * <p>收尾日期 = 候选清洗开始时间所在自然日 + 剩余排产天数 N（N = 月计划余量 / 日硫化量 向上取整），
     * 与 {@link #isMachineEndingWithinThreeDays} 的 3 天收尾判断同一口径。
     * isMachineEndingWithinThreeDays 命中时 N 必为 [0,3]，故无需额外兜底。</p>
     *
     * @param context         排程上下文
     * @param cleaningPlan    设备停机清洗计划
     * @param cleanType       清洗类型
     * @param cleanStartTime  候选清洗开始时间
     * @param machineMaterial 机台当前在机物料
     * @param productStatus 机台当前在机物料产品状态
     */
    private void collectEndingSkipScheduleDateFill(LhScheduleContext context,
                                                   MdmDevicePlanShut cleaningPlan,
                                                   String cleanType,
                                                   Date cleanStartTime,
                                                   String machineMaterial,
                                                   String productStatus) {
        if (Objects.isNull(context) || Objects.isNull(cleaningPlan) || Objects.isNull(cleanStartTime)) {
            return;
        }
        int remainingDays = this.resolveEndingRemainingDays(context, machineMaterial, productStatus);
        // isMachineEndingWithinThreeDays 命中时 remainingDays 必为 [0,3]，收尾日 = 候选清洗开始日 + 剩余天数
        Date endingDate = LhScheduleTimeUtil.addDays(LhScheduleTimeUtil.clearTime(cleanStartTime), remainingDays);
        this.collectCleaningScheduleDateFill(context, cleaningPlan, cleanType, endingDate, "收尾未安排清洗");
    }
    /**
     * 判断机台当前在机物料是否 3 天内可收尾。
     *
     * @param context 排程上下文
     * @param machineCode 机台编码
     * @param cleanStartTime 清洗开始时间
     * @param machineMaterial 机台当前在机物料
     * @param productStatus 机台当前在机物料产品状态
     * @return true-应跳过清洗；false-允许继续判断清洗纳入
     */
    private boolean isMachineEndingWithinThreeDays(LhScheduleContext context,
                                                   String machineCode,
                                                   Date cleanStartTime,
                                                   String machineMaterial,
                                                   String productStatus) {
        if (Objects.isNull(context) || StringUtils.isEmpty(machineCode)
                || Objects.isNull(cleanStartTime) || StringUtils.isEmpty(machineMaterial)) {
            return false;
        }
        int remainingDays = this.resolveEndingRemainingDays(context, machineMaterial, productStatus);
        return remainingDays >= 0 && remainingDays <= CLEANING_SKIP_ENDING_DAYS;
    }
    /**
     * 取得设备停机计划服务。
     *
     * @return 按既有清洗规则解析的结果
     */
    private LhDeviceStopPlanScheduleService getDeviceStopPlanScheduleService() {
        return Objects.nonNull(deviceStopPlanScheduleService)
                ? deviceStopPlanScheduleService : new LhDeviceStopPlanScheduleService();
    }
    /**
     * 为喷砂窗口保存运行侧首检条数及结束归属班次，干冰窗口不参与。
     *
     * @param context 排程上下文
     * @param cleaningWindow 已计算清洗及首检结束时间的窗口
     */
    private void initializeSandBlastInspection(LhScheduleContext context, MachineCleaningWindowDTO cleaningWindow) {
        if (!CleaningTypeEnum.SAND_BLAST.getCode().equals(cleaningWindow.getCleanType())) {
            return;
        }
        cleaningWindow.setSandBlastFirstInspectionQty(context.getParamIntValue(
                LhScheduleParamConstant.SAND_BLAST_FIRST_INSPECTION_QTY,
                LhScheduleConstant.SAND_BLAST_FIRST_INSPECTION_QTY));
        LhShiftConfigVO shift = LhScheduleTimeUtil.resolveShiftByTime(
                LhScheduleTimeUtil.getScheduleShifts(context, context.getScheduleDate()),
                        cleaningWindow.getReadyTime());
        if (Objects.nonNull(shift)) {
            cleaningWindow.setSandBlastInspectionShiftStartTime(shift.getShiftStartDateTime());
        }
    }
    /**
     * 读取清洗持续时长。
     *
     * @param context 排程上下文
     * @param cleanType 清洗类型
     * @return 按既有清洗规则解析的结果
     */
    private int resolveCleanDurationHours(LhScheduleContext context, String cleanType) {
        if (CleaningTypeEnum.DRY_ICE.getCode().equals(cleanType)) {
            return context.getParamIntValue(LhScheduleParamConstant.DRY_ICE_DURATION_HOURS,
                    LhScheduleConstant.DRY_ICE_DURATION_HOURS);
        }
        if (CleaningTypeEnum.SAND_BLAST.getCode().equals(cleanType)) {
            // 喷砂与首检分别读取硫化参数，避免原固定时长绕过配置。
            return context.getParamIntValue(LhScheduleParamConstant.SAND_BLAST_DURATION_HOURS,
                    LhScheduleConstant.SAND_BLAST_DURATION_HOURS);
        }
        return 0;
    }
    /**
     * 读取含喷砂首检的恢复时长。
     *
     * @param context 排程上下文
     * @param cleanType 清洗类型
     * @param cleanDurationHours 实际清洗小时数
     * @return 按既有清洗规则解析的结果
     */
    private int resolveReadyDurationHours(LhScheduleContext context, String cleanType, int cleanDurationHours) {
        // 首检紧接喷砂结束，正常生产只能从两段占用均完成后开始。
        if (CleaningTypeEnum.SAND_BLAST.getCode().equals(cleanType)) {
            return cleanDurationHours + context.getParamIntValue(
                    LhScheduleParamConstant.SAND_BLAST_FIRST_INSPECTION_HOURS,
                    LhScheduleConstant.SAND_BLAST_FIRST_INSPECTION_HOURS);
        }
        return cleanDurationHours;
    }
    /**
     * 递增初始化清洗名额。
     *
     * @param String 对应业务输入
     * @param countMap 已安排计数
     * @param dateKey 计划自然日
     */
    private void increaseCount(Map<String, Integer> countMap, String dateKey) {
        countMap.put(dateKey, countMap.getOrDefault(dateKey, 0) + 1);
    }
    /**
     * 按现有维保日、周日和节假日参数判断喷砂禁排日期。
     *
     * @param context 排程上下文
     * @param cleanTime 清洗时点
     * @return 按既有清洗规则解析的结果
     */
    private boolean isSandBlastForbiddenDate(LhScheduleContext context, Date cleanTime) {
        if (Objects.isNull(cleanTime)) {
            return false;
        }
        // 维保日：仅当"维保日允许喷砂"开关未启用时才禁止
        if (this.isSandBlastMaintenanceDate(context, cleanTime)
                && context.getParamIntValue(LhScheduleParamConstant.SAND_BLAST_ALLOW_ON_MAINTENANCE_DATE,
                LhScheduleConstant.SAND_BLAST_ALLOW_ON_MAINTENANCE_DATE) != ENABLED) {
            return true;
        }
        if (this.isSunday(cleanTime)
                && context.getParamIntValue(LhScheduleParamConstant.SAND_BLAST_SKIP_SUNDAY_ENABLED,
                LhScheduleConstant.SAND_BLAST_SKIP_SUNDAY_ENABLED) == ENABLED) {
            return true;
        }
        return this.isHoliday(context, cleanTime)
                && context.getParamIntValue(LhScheduleParamConstant.SAND_BLAST_SKIP_HOLIDAY_ENABLED,
                LhScheduleConstant.SAND_BLAST_SKIP_HOLIDAY_ENABLED) == ENABLED;
    }
    /**
     * 解析机台当前在机模具号。
     *
     * @param context 排程上下文
     * @param machineCode 机台编号
     * @return 模具号
     */
    private String resolveCleaningMouldCode(LhScheduleContext context, String machineCode) {
        if (Objects.isNull(context) || StringUtils.isEmpty(machineCode)) {
            return null;
        }
        LhMachineOnlineInfo onlineInfo = context.getMachineOnlineInfoMap().get(machineCode);
        if (Objects.isNull(onlineInfo) || StringUtils.isEmpty(onlineInfo.getMaterialCode())) {
            return null;
        }
        List<MdmSkuMouldRel> mouldRelList = context.getSkuMouldRelMap().get(onlineInfo.getMaterialCode());
        if (CollectionUtils.isEmpty(mouldRelList)) {
            return null;
        }
        return mouldRelList.stream()
                .map(MdmSkuMouldRel::getMouldCode)
                .filter(StringUtils::isNotEmpty)
                .distinct()
                .collect(Collectors.joining(VALUE_SEPARATOR));
    }
    /**
     * 判断是否为喷砂机维保日。
     *
     * @param context 排程上下文
     * @param cleanTime 清洗时间
     * @return true-维保日；false-非维保日
     */
    private boolean isSandBlastMaintenanceDate(LhScheduleContext context, Date cleanTime) {
        String maintenanceDates = context.getParamValue(LhScheduleParamConstant.SAND_BLAST_MAINTENANCE_DATES,
                LhScheduleConstant.SAND_BLAST_MAINTENANCE_DATES);
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(cleanTime);
        String dayOfMonth = String.valueOf(calendar.get(Calendar.DAY_OF_MONTH));
        for (String maintenanceDate : maintenanceDates.split(VALUE_SEPARATOR)) {
            if (StringUtils.equals(dayOfMonth, maintenanceDate.trim())) {
                return true;
            }
        }
        return false;
    }
    /**
     * 判断是否为周日。
     *
     * @param cleanTime 清洗时间
     * @return true-周日；false-非周日
     */
    private boolean isSunday(Date cleanTime) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(cleanTime);
        return calendar.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY;
    }
    /**
     * 判断是否为节假日。
     *
     * @param context 排程上下文
     * @param cleanTime 清洗时间
     * @return true-节假日；false-非节假日
     */
    private boolean isHoliday(LhScheduleContext context, Date cleanTime) {
        if (CollectionUtils.isEmpty(context.getWorkCalendarList())) {
            return false;
        }
        String cleanDate = LhScheduleTimeUtil.formatDate(cleanTime);
        for (MdmWorkCalendar workCalendar : context.getWorkCalendarList()) {
            if (Objects.isNull(workCalendar)
                    || Objects.isNull(workCalendar.getProductionDate())
                    || !StringUtils.equals(cleanDate,
                            LhScheduleTimeUtil.formatDate(workCalendar.getProductionDate()))) {
                continue;
            }
            return StringUtils.equals(WORK_CALENDAR_STOP_FLAG, workCalendar.getDayFlag());
        }
        return false;
    }
    /**
     * 计算机台当前在机物料的剩余排产天数。
     * <p>按 月计划余量 / 日硫化量 向上取整，与收尾判定策略规则2口径一致。</p>
     * <p>余量取值：优先使用 differenceQty（月计划定稿中"差异量/未排产数量"），
     * 该字段由月计划流程写入，与 ScheduleAdjustHandler 中 surplusQty = monthPlanQty - finishedQty
     * 语义等价；differenceQty 缺失时回退到 totalQty 作为保守估计。</p>
     *
     * @param context      排程上下文
     * @param materialCode 物料编码
     * @param productStatus 产品状态；空状态按正规 S 处理
     * @return 剩余排产天数；-1 表示无法判定（无月计划/日产能为0等）
     * @author APS
     * @since 2026-05-09
     */
    private int resolveEndingRemainingDays(LhScheduleContext context, String materialCode, String productStatus) {
        if (context == null || StringUtils.isEmpty(materialCode)) {
            return -1;
        }
        List<FactoryMonthPlanProductionFinalResult> monthPlanList = context.getMonthPlanList();
        if (CollectionUtils.isEmpty(monthPlanList)) {
            return -1;
        }
        String materialStatusKey = MonthPlanDateResolver.buildMaterialStatusKey(
                materialCode, this.normalizeProductStatus(productStatus));
        FactoryMonthPlanProductionFinalResult matchedPlan = null;
        for (FactoryMonthPlanProductionFinalResult plan : monthPlanList) {
            if (plan != null && StringUtils.equals(materialStatusKey,
                    MonthPlanDateResolver.buildMaterialStatusKey(
                            plan.getMaterialCode(), this.normalizeProductStatus(plan.getProductStatus())))) {
                matchedPlan = plan;
                break;
            }
        }
        if (matchedPlan == null) {
            return -1;
        }
        // 余量优先使用 differenceQty（未排产数量），与 ScheduleAdjustHandler 中 surplusQty 语义等价；
        // differenceQty 缺失时回退到 totalQty 作为保守估计
        int remainingQty = matchedPlan.getDifferenceQty() != null
                ? matchedPlan.getDifferenceQty() : 0;
        if (remainingQty <= 0 && matchedPlan.getTotalQty() != null) {
            remainingQty = Math.max(matchedPlan.getTotalQty(), 0);
        }
        if (remainingQty <= 0) {
            return 0;
        }
        int dailyCapacity = matchedPlan.getDayVulcanizationQty() != null
                ? matchedPlan.getDayVulcanizationQty() : 0;
        if (dailyCapacity <= 0) {
            return -1;
        }
        return (int) Math.ceil((double) remainingQty / dailyCapacity);
    }
    /**
     * 获取机台 MES 在机信息，供物料与产品状态使用同一条来源记录。
     *
     * @param context 排程上下文
     * @param machineCode 机台编码
     * @return 在机信息；无匹配记录时返回 null
     */
    private LhMachineOnlineInfo resolveMachineOnlineInfo(LhScheduleContext context, String machineCode) {
        if (Objects.isNull(context) || StringUtils.isEmpty(machineCode)
                || CollectionUtils.isEmpty(context.getMachineOnlineInfoMap())) {
            return null;
        }
        return context.getMachineOnlineInfoMap().get(machineCode);
    }
    /**
     * 统一产品状态口径：MES 或月计划产品状态为空时按正规 S 处理。
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
     * 保存未安排事件的显式原因，回填候选仍由原收尾路径独立处理。
     *
     * @param context 当前批次
     * @param plan 来源计划
     * @param cleanType 清洗类型
     * @param reason 跳过原因
     */
    private void recordSkip(LhScheduleContext context, MdmDevicePlanShut plan, String cleanType, String reason) {
        MachineCleaningWindowDTO source = new MachineCleaningWindowDTO();
        source.setSourcePlanId(plan.getId());
        source.setLhCode(plan.getMachineCode());
        source.setCleanType(cleanType);
        source.setSourcePlanStartTime(plan.getBeginDate());
        source.setSourcePlanEndTime(plan.getEndDate());
        this.recordInitialization(context, source, CleaningPlanStatus.SKIPPED, reason);
    }
    /**
     * 发布初始化事实，配对侧共享来源主键且不增加名额或回填。
     *
     * @param context 当前批次
     * @param window 来源及运行侧窗口
     * @param status 初始化状态
     * @param reason 原因
     */
    private void recordInitialization(LhScheduleContext context, MachineCleaningWindowDTO window,
            CleaningPlanStatus status, String reason) {
        context.getCleaningPlanRuntimeState().apply(new CleaningPlanResult.Event(window, status, reason));
    }
}
