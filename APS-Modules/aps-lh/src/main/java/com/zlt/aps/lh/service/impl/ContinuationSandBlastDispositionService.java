package com.zlt.aps.lh.service.impl;

import com.zlt.aps.lh.component.NewProductionDateAdmissionPolicy;
import com.zlt.aps.lh.engine.strategy.support.SharedMouldFuturePlanPolicy;
import com.zlt.aps.lh.component.MonthPlanDateResolver;
import com.zlt.aps.mdm.api.domain.entity.MdmMaterialInfo;
import com.zlt.aps.mp.api.domain.entity.FactoryMonthPlanProductionFinalResult;
import cn.hutool.core.bean.BeanUtil;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.aps.lh.api.domain.dto.MachineCleaningWindowDTO;
import com.zlt.aps.lh.api.domain.dto.MachineScheduleDTO;
import com.zlt.aps.lh.api.domain.dto.SkuScheduleDTO;
import com.zlt.aps.lh.api.domain.entity.LhMachineOnlineInfo;
import com.zlt.aps.lh.api.domain.entity.LhScheduleResult;
import com.zlt.aps.lh.api.domain.vo.LhShiftConfigVO;
import com.zlt.aps.lh.api.enums.ScheduleTypeEnum;
import com.zlt.aps.lh.context.LhScheduleContext;
import com.zlt.aps.lh.component.CapsuleReplacementRuleService;
import com.zlt.aps.lh.engine.strategy.impl.DefaultMouldChangeBalanceStrategy;
import com.zlt.aps.lh.engine.strategy.support.MouldResourceAllocationResult;
import com.zlt.aps.lh.engine.strategy.support.MouldResourceContext;
import com.zlt.aps.lh.engine.strategy.support.DeviceStopMachineSource;
import com.zlt.aps.lh.engine.strategy.support.ContinuationReplacementInspectionSupport;
import com.zlt.aps.lh.util.LhMouldCodeUtil;
import com.zlt.aps.lh.util.DeviceStopMachineResolver;
import com.zlt.aps.lh.util.FirstInspectionAllocationUtil;
import com.zlt.aps.lh.util.CleaningDispositionResolver;
import com.zlt.aps.lh.util.LhScheduleTimeUtil;
import com.zlt.aps.lh.util.LhSingleControlMachineUtil;
import com.zlt.aps.lh.util.MachineCleaningOverlapUtil;
import com.zlt.aps.lh.util.PriorityTraceLogHelper;
import com.zlt.aps.lh.util.ShiftCapacityResolverUtil;
import com.zlt.aps.lh.util.ShiftFieldUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 续作仅喷砂处置：负责精确模具交接、置换额度和清洗事件，不改变数量账本及选机算法。
 * 本协作服务无外部依赖和实例运行态，由使用方持有，全部批次状态均写入排程上下文。
 *
 * @author APS
 * @since 2026/10/21
 */
@Slf4j
public class ContinuationSandBlastDispositionService {

    /**
     * 同 SKU 原机更换模具的业务原因。
     */
    public static final String REPLACEMENT_ANALYSIS = "模具置换";

    /**
     * 复用普通换模的动态额度及登记、回退，全部运行态仍保存在批次上下文中。
     */
    private final DefaultMouldChangeBalanceStrategy mouldChangeBalanceStrategy =
            new DefaultMouldChangeBalanceStrategy();

    /**
     * 续作计算产能前登记旧装机喷砂事实并优先落实续作可用模具。
     * 单控同源事件先整体检查足量模具及一次物理换模额度，再按运行侧精确绑定。
     *
     * @param context 排程上下文
     */
    public void prepare(LhScheduleContext context) {
        if (Objects.isNull(context)
                || CollectionUtils.isEmpty(context.getScheduleWindowShifts())
                || !CollectionUtils.isEmpty(context.getContinuationSandBlastWindowMap())) {
            return;
        }
        Map<String, SkuScheduleDTO> sources = context.getContinuousSkuList().stream()
                .filter(Objects::nonNull)
                .filter(sku -> StringUtils.isNotEmpty(sku.getContinuousMachineCode()))
                .collect(Collectors.toMap(SkuScheduleDTO::getContinuousMachineCode,
                        sku -> sku, (first, second) -> first, LinkedHashMap::new));
        this.prepareIdleMachineSandBlast(context, sources.keySet());
        Map<String, MachineCleaningWindowDTO> candidates = new LinkedHashMap<>(sources.size());
        for (String machineCode : sources.keySet()) {
            MachineScheduleDTO machine = context.getMachineScheduleMap().get(machineCode);
            MachineCleaningWindowDTO window = this.resolveWindow(context, machine);
            if (Objects.nonNull(window)
                    && !LhCleaningRepairOverlapPolicy.hasPlannedSandBlastHandoff(context, window)) {
                candidates.put(machineCode, window);
            }
        }
        if (CollectionUtils.isEmpty(candidates)) {
            context.setMouldResourceContext(MouldResourceContext.from(context));
            return;
        }
        List<String> orderedMachines = new ArrayList<>(candidates.keySet());
        orderedMachines.sort(Comparator.comparing((String code) -> candidates.get(code).getCleanStartTime())
                .thenComparing(code -> code));
        Set<String> processed = new LinkedHashSet<>(orderedMachines.size());
        Date previousDate = context.getCurrentScheduleDate();
        try {
            for (String machineCode : orderedMachines) {
                if (processed.contains(machineCode)) {
                    continue;
                }
                MachineCleaningWindowDTO window = candidates.get(machineCode);
                // 同源同 SKU 的运行侧作为一次物理模具交接。
                List<String> sides = this.resolveEventSides(machineCode, orderedMachines, sources, candidates);
                processed.addAll(sides);
                // 按喷砂实际业务日刷新到货状态，退出后恢复主流程日期。
                context.setCurrentScheduleDate(LhScheduleTimeUtil.clearTime(window.getCleanStartTime()));
                this.prepareEvent(context, MouldResourceContext.from(context), sources, candidates, sides);
            }
        } finally {
            context.setCurrentScheduleDate(previousDate);
        }
        // 后续阶段从已登记交接事实重建资源，不保留预计算日期的可用性视图。
        context.setMouldResourceContext(MouldResourceContext.from(context));
    }

    /**
     * 登记未取得续作资格的旧装机原模喷砂事实，机台与原模分别释放。
     * 冻结备用模具组或原模恢复事实，续作收口后以现有需求账本登记原机同料任务。
     *
     * @param context 当前批次与有效清洗窗口
     * @param continuousMachines 已取得续作资格的运行侧机台
     */
    private void prepareIdleMachineSandBlast(LhScheduleContext context, Set<String> continuousMachines) {
        Date windowDate = LhScheduleTimeUtil.clearTime(
                context.getScheduleWindowShifts().get(0).getShiftStartDateTime());
        for (MachineScheduleDTO machine : context.getMachineScheduleMap().values()) {
            if (Objects.isNull(machine) || continuousMachines.contains(machine.getMachineCode())
                    || context.getOnlySandBlastContinuationReleaseWindowMap().containsKey(machine.getMachineCode())) {
                continue;
            }
            LhMachineOnlineInfo online = context.getMachineOnlineInfoMap().get(machine.getMachineCode());
            if (Objects.isNull(online) || Objects.isNull(online.getOnlineDate())
                    || !online.getOnlineDate().before(windowDate)) {
                continue;
            }
            MachineCleaningWindowDTO source = this.resolveWindow(context, machine);
            Set<String> originalMoulds = LhMouldCodeUtil.resolveInMachineMouldCodeSet(
                    context, machine.getMachineCode());
            if (Objects.isNull(source) || source.isSourceFollowsMachineOccupation()
                    || LhCleaningRepairOverlapPolicy.hasPlannedSandBlastHandoff(context, source)
                    || CollectionUtils.isEmpty(originalMoulds)
                    || !this.matchesSourcePlanMoulds(context, source, originalMoulds)
                    || !StringUtils.equals(online.getMaterialCode(), source.getSourceMaterialCode())
                    || !StringUtils.equals(StringUtils.defaultIfBlank(online.getProductStatus(), "S"),
                    StringUtils.defaultIfBlank(source.getSourceProductStatus(), "S"))) {
                continue;
            }
            MachineCleaningWindowDTO event = BeanUtil.copyProperties(source, MachineCleaningWindowDTO.class);
            event.setContinuationMaterialCode(online.getMaterialCode());
            event.setContinuationProductStatus(StringUtils.defaultIfBlank(online.getProductStatus(), "S"));
            event.setContinuationMaterialDesc(machine.getCurrentMaterialDesc());
            event.setMouldCode(LhMouldCodeUtil.joinMouldCode(originalMoulds));
            int requiredQty = ShiftCapacityResolverUtil.resolveMachineMouldQty(machine);
            List<String> freeMoulds = this.resolveIdleReplacementMoulds(context, event, originalMoulds, requiredQty);
            if (freeMoulds.size() >= requiredQty && this.isReplacementProtected(context,
                    online.getMaterialCode(), online.getProductStatus(), originalMoulds,
                    Collections.singletonList(machine.getMachineCode()))) {
                // 保留原机清洗，不将禁止置换伪装为无模具离机事件。
                continue;
            }
            event.setIdleSandBlastRecoveryRequired(freeMoulds.size() < requiredQty);
            if (!event.isIdleSandBlastRecoveryRequired()) {
                event.setDeferredReplacementMouldCode(LhMouldCodeUtil.joinMouldCode(
                        freeMoulds.subList(0, requiredQty)));
                context.getCleaningPlanRuntimeState().getPendingRecoveryMachineCodes().add(machine.getMachineCode());
            }
            this.detach(context, machine, source, event);
            context.getContinuationSandBlastWindowMap().put(machine.getMachineCode(), event);
            context.getOnlySandBlastContinuationReleaseWindowMap().put(machine.getMachineCode(), event);
            // 保留物理释放事实，后续原机恢复与历史承接共同使用真实占用边界。
            machine.setPreviousMaterialCode(online.getMaterialCode());
            machine.setPreviousMaterialDesc(machine.getCurrentMaterialDesc());
            machine.setCurrentMaterialCode(null);
            machine.setCurrentMaterialDesc(null);
            machine.setEstimatedEndTime(event.getCleanStartTime());
            machine.setEnding(false);
            log.info("旧装机独立喷砂资源登记, factoryCode: {}, batchNo: {}, machineCode: {}, "
                            + "materialCode: {}, mouldCodes: {}, machineReadyTime: {}, mouldReadyTime: {}, "
                            + "recoveryRequired: {}, replacementMouldCodes: {}",
                    context.getFactoryCode(), context.getBatchNo(), machine.getMachineCode(), online.getMaterialCode(),
                    event.getMouldCode(), event.getCleanStartTime(), event.getCleanEndTime(),
                    event.isIdleSandBlastRecoveryRequired(), event.getDeferredReplacementMouldCode());
        }
    }

    /**
     * 按喷砂开始业务日读取完整空闲模具候选，退出后恢复调用方日期。
     *
     * @param context 当前资源上下文
     * @param event 原机同料喷砂来源
     * @param originalMoulds 应排除的原模具组
     * @param requiredQty 当前运行侧所需模数
     * @return 当时有效且未占用的备用模具
     */
    private List<String> resolveIdleReplacementMoulds(LhScheduleContext context,
            MachineCleaningWindowDTO event, Set<String> originalMoulds, int requiredQty) {
        Date previousDate = context.getCurrentScheduleDate();
        try {
            context.setCurrentScheduleDate(LhScheduleTimeUtil.clearTime(event.getCleanStartTime()));
            return this.resolveReplacementMoulds(context, MouldResourceContext.from(context),
                    event.getContinuationMaterialCode(), originalMoulds, event.getCleanStartTime(),
                    Collections.singletonList(event.getLhCode()), requiredQty);
        } finally {
            context.setCurrentScheduleDate(previousDate);
        }
    }

    /**
     * 使用公共来源身份核对指定原模，区分双模整机的侧来源与整组来源。
     * 后续置换仍使用完整装机组，不能把来源单副模具扩写到原始停机记录。
     *
     * @param context 持有公共来源身份及原始清洗快照的上下文
     * @param window 待登记的清洗窗口
     * @param originalMoulds 实际完整装机模具集合
     * @return 来源明确指定且与当前装机匹配时返回true
     */
    private boolean matchesSourcePlanMoulds(LhScheduleContext context, MachineCleaningWindowDTO window,
            Set<String> originalMoulds) {
        DeviceStopMachineSource identity = DeviceStopMachineResolver.resolveSource(context, window.getSourcePlanId());
        return identity != null && identity.matchesMoulds(originalMoulds);
    }

    /**
     * 归集同一物理机台、同一 SKU 状态及同一清洗来源的运行侧。
     * @param machineCode 当前运行侧
     * @param orderedMachines 已按清洗时间排序的机台
     * @param sources 续作来源
     * @param candidates 实际清洗窗口
     * @return 本次必须一起决策的运行侧
     */
    private List<String> resolveEventSides(String machineCode, List<String> orderedMachines,
                                           Map<String, SkuScheduleDTO> sources,
                                           Map<String, MachineCleaningWindowDTO> candidates) {
        String physicalCode = LhSingleControlMachineUtil.resolvePhysicalMachineCode(machineCode);
        MachineCleaningWindowDTO window = candidates.get(machineCode);
        return orderedMachines.stream()
                .filter(code -> StringUtils.equals(physicalCode, LhSingleControlMachineUtil.resolvePhysicalMachineCode(code)))
                .filter(code -> StringUtils.equals(sources.get(machineCode).getMaterialCode(), sources.get(code).getMaterialCode())
                        && StringUtils.equals(sources.get(machineCode).getProductStatus(), sources.get(code).getProductStatus()))
                .filter(code -> Objects.equals(window.getSourcePlanId(), candidates.get(code).getSourcePlanId())
                        && Objects.equals(window.getCleanStartTime(), candidates.get(code).getCleanStartTime()))
                .collect(Collectors.toList());
    }

    /**
     * 查找最早的独立喷砂事件；重叠场景保持原窗口。
     * @param context 排程上下文
     * @param machine 续作机台
     * @return 最早独立喷砂窗口，无匹配返回 null
     */
    private MachineCleaningWindowDTO resolveWindow(LhScheduleContext context, MachineScheduleDTO machine) {
        if (Objects.isNull(machine) || CollectionUtils.isEmpty(machine.getCleaningWindowList())) {
            return null;
        }
        return machine.getCleaningWindowList().stream()
                .filter(MachineCleaningOverlapUtil::isSandBlastCleaning)
                .filter(window -> CleaningDispositionResolver.resolve(context, machine.getMachineCode(), window)
                        .getDisposition() == CleaningDispositionResolver.Disposition.INDEPENDENT)
                // 历史正规换模尚未提交时仍保留普通清洗；不能先执行离机喷砂改变其前料和模具。
                .filter(window -> !CleaningDispositionResolver.hasPendingRegularChange(
                        context, machine.getMachineCode(), window))
                .filter(window -> Objects.nonNull(window.getCleanStartTime())
                        && Objects.nonNull(window.getCleanEndTime())
                        && window.getCleanStartTime().before(window.getCleanEndTime()))
                .filter(window -> window.getCleanStartTime().before(context.getScheduleWindowShifts()
                        .get(context.getScheduleWindowShifts().size() - 1).getShiftEndDateTime()))
                .filter(window -> {
                    boolean overlap = MachineCleaningOverlapUtil.hasOtherDowntimeOverlap(context, machine.getMachineCode(), window);
                    if (overlap) {
                        log.info("续作喷砂与其他停机重叠，保持原逻辑, 机台: {}, SKU: {}, 喷砂开始: {}, 是否仅喷砂: false",
                                machine.getMachineCode(), machine.getCurrentMaterialCode(), window.getCleanStartTime());
                    }
                    return !overlap;
                })
                .min(Comparator.comparing(MachineCleaningWindowDTO::getCleanStartTime)).orElse(null);
    }

    /**
     * 整组预占替换模具及一次换模额度，并冻结新模具的准备和计件首检时间。
     * 没有足量模具时沿原离机处置；有模具但额度不足时保留原机台清洗窗口。
     *
     * @param context 排程上下文
     * @param resources 共享模具资源
     * @param sources 续作来源
     * @param candidates 清洗候选
     * @param sides 同源物理事件的运行侧
     */
    private void prepareEvent(LhScheduleContext context, MouldResourceContext resources,
                              Map<String, SkuScheduleDTO> sources,
                              Map<String, MachineCleaningWindowDTO> candidates, List<String> sides) {
        String materialCode = sources.get(sides.get(0)).getMaterialCode();
        Date startTime = candidates.get(sides.get(0)).getCleanStartTime();
        Set<String> oldMoulds = new LinkedHashSet<>(sides.size() * 2);
        int requiredQty = 0;
        for (String side : sides) {
            if (!StringUtils.equals(materialCode, sources.get(side).getMaterialCode())
                    || !StringUtils.equals(sources.get(sides.get(0)).getProductStatus(), sources.get(side).getProductStatus())) {
                // 不同物料的物理机台不能按同 SKU 整组置换，交给已有单控规则处理。
                return;
            }
            oldMoulds.addAll(LhMouldCodeUtil.resolveInMachineMouldCodeSet(context, side));
            requiredQty += ShiftCapacityResolverUtil.resolveMachineMouldQty(context.getMachineScheduleMap().get(side));
        }
        List<String> freeMoulds = this.resolveReplacementMoulds(
                context, resources, materialCode, oldMoulds, startTime, sides, requiredQty);
        boolean replacement = freeMoulds.size() >= requiredQty;
        SkuScheduleDTO originalSku = sources.get(sides.get(0));
        if (replacement && this.isReplacementProtected(context, materialCode,
                originalSku.getProductStatus(), oldMoulds, sides)) {
            // 必须在延期预留、换模额度及模具领取之前退出，普通喷砂仍由原窗口驱动。
            return;
        }
        LhShiftConfigVO triggerShift = LhScheduleTimeUtil.resolveShiftByTime(context.getScheduleWindowShifts(), startTime);
        if (replacement && Objects.nonNull(triggerShift)
                && !NewProductionDateAdmissionPolicy.isAllowed(context, triggerShift.getWorkDate().toInstant()
                        .atZone(java.time.ZoneId.systemDefault()).toLocalDate())) {
            // T日先冻结整组备用模具，实际换模、首检和数量由次日原机任务统一提交。
            int offset = 0;
            for (String side : sides) {
                MachineScheduleDTO machine = context.getMachineScheduleMap().get(side);
                MachineCleaningWindowDTO event = BeanUtil.copyProperties(candidates.get(side), MachineCleaningWindowDTO.class);
                SkuScheduleDTO sourceSku = sources.get(side);
                int mouldQty = ShiftCapacityResolverUtil.resolveMachineMouldQty(machine);
                event.setContinuationMaterialCode(sourceSku.getMaterialCode());
                event.setContinuationProductStatus(sourceSku.getProductStatus());
                event.setContinuationMaterialDesc(sourceSku.getMaterialDesc());
                event.setMouldCode(LhMouldCodeUtil.joinMouldCode(LhMouldCodeUtil.resolveInMachineMouldCodeSet(context, side)));
                event.setDeferredReplacementMouldCode(LhMouldCodeUtil.joinMouldCode(
                        freeMoulds.subList(offset, offset + mouldQty)));
                offset += mouldQty;
                this.detach(context, machine, candidates.get(side), event);
                context.getContinuationSandBlastWindowMap().put(side, event);
                context.getOnlySandBlastContinuationReleaseWindowMap().put(side, event);
                context.getCleaningPlanRuntimeState().getPendingRecoveryMachineCodes().add(side);
            }
            return;
        }
        Date readyTime = LhScheduleTimeUtil.addHours(startTime,
                LhScheduleTimeUtil.getMouldChangeTotalHours(context));
        LhShiftConfigVO inspectionShift = LhScheduleTimeUtil.resolveShiftByTime(
                context.getScheduleWindowShifts(), new Date(readyTime.getTime() - 1L));
        int inspectionQty = LhScheduleTimeUtil.getFirstInspectionQty(context);
        int physicalCapacity = inspectionShift == null ? 0 : sides.stream().mapToInt(side ->
                ShiftCapacityResolverUtil.resolveActualShiftPlanQty(sources.get(side).getShiftCapacity(),
                        inspectionShift, ShiftCapacityResolverUtil.resolveOddShiftCapacityPlusShiftType(context),
                        ScheduleTypeEnum.CONTINUOUS.getCode())).sum();
        long inspectionSeconds = replacement && inspectionShift != null
                ? FirstInspectionAllocationUtil.resolveInspectionDurationSeconds(inspectionQty,
                ShiftCapacityResolverUtil.resolveShiftDurationSeconds(inspectionShift), physicalCapacity) : 0L;
        Date inspectionStart = new Date(readyTime.getTime() - inspectionSeconds * 1000L);
        if (replacement && inspectionShift != null && (inspectionSeconds <= 0L || inspectionStart.before(startTime))) {
            log.error("续作置换首检时间配置无效, 工厂: {}, 批次: {}, 机台: {}, 物料: {}, 班产: {}, 首检条数: {}",
                    context.getFactoryCode(), context.getBatchNo(), sides, materialCode, physicalCapacity, inspectionQty);
            throw new IllegalStateException(I18nUtil.getMessage("ui.data.column.lhScheduleResult.errorCode.systemError"));
        }
        if (replacement && !this.reserveReplacementQuota(context, sides.get(0),
                candidates.get(sides.get(0)), sources.get(sides.get(0)))) {
            // 未取得额度时不能发布离机事件，否则续作主链会误将其按无模具分支强制下机。
            return;
        }
        int offset = 0;
        int sideIndex = 0;
        // 同源左右侧可能共用原模具集合，恢复边界必须统一取整组交接前快照。
        Map<String, Date> originalReleaseTimes = new LinkedHashMap<>(oldMoulds.size());
        oldMoulds.forEach(code -> originalReleaseTimes.put(code, context.getPreScheduledMouldReleaseTimeMap().get(code)));
        for (String side : sides) {
            MachineScheduleDTO machine = context.getMachineScheduleMap().get(side);
            MachineCleaningWindowDTO event = BeanUtil.copyProperties(candidates.get(side), MachineCleaningWindowDTO.class);
            event.setLhCode(side);
            event.setContinuationMaterialCode(materialCode);
            event.setContinuationProductStatus(sources.get(side).getProductStatus());
            event.setContinuationMaterialDesc(sources.get(side).getMaterialDesc());
            event.setMouldReleaseTimeBeforeReplacement(new LinkedHashMap<>(originalReleaseTimes));
            // 始终保留 MES 实际原模具，不能拿替换模具填写清洗交替计划。
            event.setMouldCode(LhMouldCodeUtil.joinMouldCode(LhMouldCodeUtil.resolveInMachineMouldCodeSet(context, side)));
            if (replacement) {
                int mouldQty = ShiftCapacityResolverUtil.resolveMachineMouldQty(machine);
                List<String> selected = new ArrayList<>(freeMoulds.subList(offset, offset + mouldQty));
                MouldResourceAllocationResult allocation = resources.tryAllocateExact(materialCode, side, selected, startTime);
                if (!allocation.isAllowed()) {
                    log.error("续作喷砂置换精确模具绑定与预检不一致, 机台: {}, SKU: {}, 替换模具: {}", side, materialCode, selected);
                    throw new IllegalStateException(I18nUtil.getMessage("ui.data.column.lhScheduleResult.errorCode.systemError"));
                }
                offset += mouldQty;
                event.setReplacementMouldCode(LhMouldCodeUtil.joinMouldCode(selected));
                // 换模总时长已经包含首检，按物理事件分摊条数，避免左右侧各新增一次首检。
                event.setReplacementInspectionStartTime(inspectionStart);
                event.setReplacementReadyTime(readyTime);
                event.setReplacementFirstInspectionQty(inspectionQty / sides.size()
                        + (sideIndex < inspectionQty % sides.size() ? 1 : 0));
                event.setReplacementInspectionShiftStartTime(inspectionShift == null ? null
                        : inspectionShift.getShiftStartDateTime());
                event.setRemark(StringUtils.isEmpty(event.getRemark()) ? REPLACEMENT_ANALYSIS
                        : event.getRemark() + "；" + REPLACEMENT_ANALYSIS);
                // 只解除原模具对机台的占用；清洗事件另存，供最终计划与回填消费。
                this.detach(context, machine, candidates.get(side), event);
                event.getMouldReleaseTimeBeforeReplacement().replaceAll((code, time) -> originalReleaseTimes.get(code));
            }
            sideIndex++;
            context.getContinuationSandBlastWindowMap().put(side, event);
            log.info("续作仅喷砂处置, 机台: {}, SKU: {}, 产品状态: {}, 喷砂开始: {}, 是否仅喷砂: true, "
                            + "是否找到闲置可用模具: {}, 原模具: {}, 替换模具: {}, 处理方式: {}",
                    side, materialCode, event.getContinuationProductStatus(), startTime, replacement,
                    event.getMouldCode(), event.getReplacementMouldCode(),
                    replacement ? REPLACEMENT_ANALYSIS : "SKU下机重新排产");
        }
    }

    /**
     * 以来源身份和冻结装机组调用公共保护规则，并关联实际运行侧。
     *
     * @param context 当前批次观察数据
     * @param materialCode 来源物料
     * @param productStatus 来源产品状态
     * @param originals 实际原装机组
     * @param sides 同源运行侧
     * @return 命中未来共模保护时返回true
     */
    private boolean isReplacementProtected(LhScheduleContext context, String materialCode,
            String productStatus, Set<String> originals, List<String> sides) {
        FactoryMonthPlanProductionFinalResult plan = MonthPlanDateResolver.resolvePlan(context,
                materialCode, productStatus, context.getScheduleDate().toInstant()
                        .atZone(java.time.ZoneId.systemDefault()).toLocalDate());
        MdmMaterialInfo material = context.getMaterialInfoMap().get(materialCode);
        String structure = null;
        String pattern = null;
        if (plan != null) {
            structure = plan.getStructureName();
            pattern = plan.getMainPattern();
        } else if (material != null) {
            structure = material.getStructureName();
            pattern = material.getMainPattern();
        }
        boolean blocked = SharedMouldFuturePlanPolicy.isReplacementProhibited(
                context, materialCode, structure, pattern, originals);
        log.info("仅喷砂模具置换资格, factoryCode: {}, batchNo: {}, machineCodes: {}, materialCode: {}, "
                        + "productStatus: {}, originalMoulds: {}, futurePlanBlocked: {}",
                context.getFactoryCode(), context.getBatchNo(), sides, materialCode, productStatus, originals, blocked);
        return blocked;
    }

    /**
     * 各喷砂来源统一扣除历史指定的预留组，记录因此失去完整替换组的来源。
     *
     * @param context 当前批次清洗运行态
     * @param resources 当前资源视图
     * @param materialCode 喷砂来源物料
     * @param originalMoulds 原模具集合
     * @param startTime 喷砂实际触发时刻
     * @param sides 必须一起决策的运行侧
     * @param requiredQty 整组所需模数
     * @return 扣除指定预留后的空闲模具
     */
    private List<String> resolveReplacementMoulds(LhScheduleContext context, MouldResourceContext resources,
            String materialCode, Set<String> originalMoulds, Date startTime, List<String> sides, int requiredQty) {
        List<String> available = resources.resolveSandBlastReplacementMouldCodes(materialCode, originalMoulds, startTime);
        if (available.size() < requiredQty
                && !context.getCleaningPlanRuntimeState().getHistoricalMouldReservationMap().isEmpty()
                && resources.resolveFreeValidMouldCodes(materialCode, originalMoulds, startTime).size() >= requiredQty) {
            context.getCleaningPlanRuntimeState().getHistoricalReservationBlockedMachines().addAll(sides);
            PriorityTraceLogHelper.appendProcessLog(context, "喷砂置换让位历史指定", String.format(
                    "工厂=%s, 批次=%s, 机台=%s, 物料=%s, 喷砂开始=%s, 所需模数=%s, 剩余空闲模具=%s, 历史预留=%s",
                    context.getFactoryCode(), context.getBatchNo(), sides, materialCode, startTime, requiredQty,
                    available, context.getCleaningPlanRuntimeState().getHistoricalMouldReservationMap()));
        }
        return available;
    }

    /**
     * 历史阶段结束后复评曾让位的喷砂来源，完整空闲组交由原机恢复内核提交。
     * 原续作已按喷砂节点截断，恢复段独立保存实际模具和产量，避免重算或重复扣账。
     *
     * @param context 已结束历史预留并完成续作扣账的上下文
     */
    public void reconcileHistoricalReservations(LhScheduleContext context) {
        Set<String> blocked = context.getCleaningPlanRuntimeState().getHistoricalReservationBlockedMachines();
        if (blocked.isEmpty()) {
            return;
        }
        Date previousDate = context.getCurrentScheduleDate();
        Set<String> processed = new LinkedHashSet<>();
        try {
            for (String source : new ArrayList<>(blocked)) {
                MachineCleaningWindowDTO event = context.getOnlySandBlastContinuationReleaseWindowMap().get(source);
                if (processed.contains(source) || event == null
                        || !context.getCleaningPlanRuntimeState().getPendingRecoveryMachineCodes().contains(source)) {
                    continue;
                }
                List<String> sides = blocked.stream().filter(side -> {
                    MachineCleaningWindowDTO other = context.getOnlySandBlastContinuationReleaseWindowMap().get(side);
                    return other != null && Objects.equals(event.getSourcePlanId(), other.getSourcePlanId())
                            && Objects.equals(event.getCleanStartTime(), other.getCleanStartTime())
                            && StringUtils.equals(event.getContinuationMaterialCode(), other.getContinuationMaterialCode())
                            && StringUtils.equals(event.getContinuationProductStatus(), other.getContinuationProductStatus())
                            && StringUtils.equals(LhSingleControlMachineUtil.resolvePhysicalMachineCode(source),
                                    LhSingleControlMachineUtil.resolvePhysicalMachineCode(side));
                }).collect(Collectors.toList());
                processed.addAll(sides);
                this.restoreAvailableReplacement(context, sides, event);
            }
        } finally {
            blocked.clear();
            context.setCurrentScheduleDate(previousDate);
            context.setMouldResourceContext(MouldResourceContext.from(context));
        }
    }

    /**
     * 只为仍待原机恢复且整组空闲的来源登记替换组合，实际次数和首检由恢复提交消费。
     *
     * @param context 历史阶段后的实时资源
     * @param sides 同源物理运行侧
     * @param sourceEvent 来源喷砂窗口
     */
    private void restoreAvailableReplacement(LhScheduleContext context, List<String> sides,
            MachineCleaningWindowDTO sourceEvent) {
        Set<String> originals = new LinkedHashSet<>();
        int requiredQty = 0;
        for (String side : sides) {
            MachineScheduleDTO machine = context.getMachineScheduleMap().get(side);
            MachineCleaningWindowDTO event = context.getOnlySandBlastContinuationReleaseWindowMap().get(side);
            if (machine == null || !context.getCleaningPlanRuntimeState().getPendingRecoveryMachineCodes().contains(side)
                    || StringUtils.isNotBlank(event.getDeferredReplacementMouldCode())
                    || StringUtils.isNotBlank(machine.getCurrentMaterialCode())
                    || CleaningDispositionResolver.resolve(context, side, event).getDisposition()
                            != CleaningDispositionResolver.Disposition.INDEPENDENT) {
                return;
            }
            originals.addAll(LhMouldCodeUtil.splitMouldCode(event.getMouldCode()));
            requiredQty += ShiftCapacityResolverUtil.resolveMachineMouldQty(machine);
        }
        context.setCurrentScheduleDate(LhScheduleTimeUtil.clearTime(sourceEvent.getCleanStartTime()));
        List<String> available = MouldResourceContext.from(context).resolveFreeValidMouldCodes(
                sourceEvent.getContinuationMaterialCode(), originals, sourceEvent.getCleanStartTime());
        if (available.size() < requiredQty || this.isReplacementProtected(context,
                sourceEvent.getContinuationMaterialCode(), sourceEvent.getContinuationProductStatus(),
                originals, sides)) {
            return;
        }
        int offset = 0;
        for (String side : sides) {
            int mouldQty = ShiftCapacityResolverUtil.resolveMachineMouldQty(context.getMachineScheduleMap().get(side));
            context.getOnlySandBlastContinuationReleaseWindowMap().get(side).setDeferredReplacementMouldCode(
                    LhMouldCodeUtil.joinMouldCode(available.subList(offset, offset + mouldQty)));
            offset += mouldQty;
        }
        PriorityTraceLogHelper.appendProcessLog(context, "历史指定后喷砂模具复评", String.format(
                "工厂=%s, 批次=%s, 机台=%s, 物料=%s, 状态=%s, 替换模具=%s, 处置=原机恢复入口重新校验换模首检与正量",
                context.getFactoryCode(), context.getBatchNo(), sides, sourceEvent.getContinuationMaterialCode(),
                sourceEvent.getContinuationProductStatus(), available.subList(0, requiredQty)));
    }

    /**
     * 解除原机台喷砂阻塞，置换时原模按实际换模释放，下机恢复时等待清洗结束。
     *
     * @param context 排程上下文
     * @param machine 运行态机台
     * @param source 原有效窗口
     * @param event 清洗事实
     */
    public void detach(LhScheduleContext context, MachineScheduleDTO machine,
                       MachineCleaningWindowDTO source, MachineCleaningWindowDTO event) {
        List<MachineCleaningWindowDTO> remaining = machine.getCleaningWindowList().stream()
                .filter(window -> window != source && !(Objects.equals(window.getSourcePlanId(), source.getSourcePlanId())
                        && Objects.equals(window.getCleanStartTime(), source.getCleanStartTime())
                        && Objects.equals(window.getCleanType(), source.getCleanType())))
                .collect(Collectors.toList());
        machine.setCleaningWindowList(remaining);
        if (StringUtils.isNotEmpty(event.getReplacementMouldCode())
                && !CollectionUtils.isEmpty(context.getScheduleWindowShifts())) {
            // 初始化可能已被喷砂推迟到 readyTime，置换后按剩余真实停机重新解析首班可用时间。
            Date windowStart = context.getScheduleWindowShifts().get(0).getShiftStartDateTime();
            machine.setEstimatedEndTime(MachineCleaningOverlapUtil.resolveEarliestAvailableTime(windowStart,
                    remaining, machine.getPlanStopStartTime(), machine.getPlanStopEndTime()));
        }
        for (String mouldCode : LhMouldCodeUtil.splitMouldCode(event.getMouldCode())) {
            if (!event.isOffMachineCleaning()
                    && !event.getMouldReleaseTimeBeforeReplacement().containsKey(mouldCode)) {
                event.getMouldReleaseTimeBeforeReplacement().put(mouldCode,
                        context.getPreScheduledMouldReleaseTimeMap().get(mouldCode));
            }
            // 有备用模具置换时取消本来源喷砂，原模仅保留实际换模释放下限。
            Date releaseTime = StringUtils.isNotEmpty(event.getReplacementMouldCode())
                    ? event.getCleanStartTime() : event.getCleanEndTime();
            context.getPreScheduledMouldReleaseTimeMap().merge(mouldCode, releaseTime,
                    (previous, current) -> previous.after(current) ? previous : current);
        }
        event.setOffMachineCleaning(true);
    }

    /**
     * 最终续作未跨过触发时刻时取消模具、额度预占及置换时间，恢复原清洗窗口。
     *
     * @param context 排程上下文
     */
    public void cancelUnreachedReplacements(LhScheduleContext context) {
        if (CollectionUtils.isEmpty(context.getContinuationSandBlastWindowMap())) {
            return;
        }
        List<String> cancelled = new ArrayList<>(context.getContinuationSandBlastWindowMap().size());
        for (Map.Entry<String, MachineCleaningWindowDTO> entry : context.getContinuationSandBlastWindowMap().entrySet()) {
            MachineCleaningWindowDTO event = entry.getValue();
            if (StringUtils.isEmpty(event.getReplacementMouldCode())) {
                continue;
            }
            boolean reached = context.getScheduleResultList().stream().filter(Objects::nonNull)
                    .filter(result -> StringUtils.equals(LhSingleControlMachineUtil.resolvePhysicalMachineCode(entry.getKey()),
                            LhSingleControlMachineUtil.resolvePhysicalMachineCode(result.getLhMachineCode())))
                    .filter(result -> StringUtils.equals(event.getContinuationMaterialCode(), result.getMaterialCode()))
                    .filter(result -> StringUtils.equals(event.getContinuationProductStatus(), result.getProductStatus()))
                    .anyMatch(result -> context.getScheduleWindowShifts().stream().anyMatch(shift -> {
                        Integer qty = ShiftFieldUtil.getShiftPlanQty(result, shift.getShiftIndex());
                        Date end = ShiftFieldUtil.getShiftEndTime(result, shift.getShiftIndex());
                        return Objects.nonNull(qty) && qty > 0 && Objects.nonNull(end)
                                && end.after(event.getCleanStartTime());
                    }));
            if (reached) {
                continue;
            }
            for (Map.Entry<String, Date> previous : event.getMouldReleaseTimeBeforeReplacement().entrySet()) {
                if (!Objects.equals(context.getPreScheduledMouldReleaseTimeMap().get(previous.getKey()), event.getCleanStartTime())) {
                    continue;
                }
                if (Objects.isNull(previous.getValue())) {
                    context.getPreScheduledMouldReleaseTimeMap().remove(previous.getKey());
                } else {
                    context.getPreScheduledMouldReleaseTimeMap().put(previous.getKey(), previous.getValue());
                }
            }
            // 恢复原清洗事件交给既有最终收尾处置，不能把取消的置换备注保留下来。
            MachineScheduleDTO machine = context.getMachineScheduleMap().get(entry.getKey());
            MachineCleaningWindowDTO original = BeanUtil.copyProperties(event, MachineCleaningWindowDTO.class);
            original.setOffMachineCleaning(false);
            original.setReplacementMouldCode(null);
            original.setReplacementInspectionStartTime(null);
            original.setReplacementReadyTime(null);
            original.setReplacementFirstInspectionQty(null);
            original.setReplacementInspectionShiftStartTime(null);
            original.setRemark(StringUtils.removeEnd(StringUtils.removeEnd(event.getRemark(), REPLACEMENT_ANALYSIS), "；"));
            List<MachineCleaningWindowDTO> windows = new ArrayList<>(machine.getCleaningWindowList());
            windows.add(original);
            windows.sort(Comparator.comparing(MachineCleaningWindowDTO::getCleanStartTime));
            machine.setCleaningWindowList(windows);
            cancelled.add(entry.getKey());
            log.info("续作未生产至喷砂触发时间，取消模具置换预占, 机台: {}, SKU: {}, 喷砂开始: {}",
                    entry.getKey(), event.getContinuationMaterialCode(), event.getCleanStartTime());
        }
        cancelled.forEach(context.getContinuationSandBlastWindowMap()::remove);
        ContinuationReplacementInspectionSupport.releaseCancelled(context);
        this.reconcileReplacementQuotas(context);
        context.setMouldResourceContext(MouldResourceContext.from(context));
    }

    /**
     * 正式结果提交后解除已取消喷砂的模具清洗边界，保留来源事件供最终计划及回填追溯。
     * 只恢复仍等于本事件实际释放时刻的边界，禁止覆盖后续合法事件；候选预演不得调用。
     * 已取消置换的换模额度同步幂等释放，不影响其他真实动作。
     *
     * @param context 已正式提交结果的当前批次上下文
     */
    public void reconcileCancelledEvents(LhScheduleContext context) {
        boolean resourceChanged = false;
        for (Map.Entry<String, MachineCleaningWindowDTO> entry : context.getContinuationSandBlastWindowMap().entrySet()) {
            MachineCleaningWindowDTO event = entry.getValue();
            CleaningDispositionResolver.Decision decision = CleaningDispositionResolver.resolve(
                    context, entry.getKey(), event);
            if (decision.getDisposition() == CleaningDispositionResolver.Disposition.INDEPENDENT) {
                continue;
            }
            boolean changed = false;
            Date eventReleaseTime = StringUtils.isNotEmpty(event.getReplacementMouldCode())
                    ? event.getCleanStartTime() : event.getCleanEndTime();
            for (Map.Entry<String, Date> original : event.getMouldReleaseTimeBeforeReplacement().entrySet()) {
                if (!Objects.equals(context.getPreScheduledMouldReleaseTimeMap().get(original.getKey()),
                        eventReleaseTime) || Objects.equals(original.getValue(), eventReleaseTime)) {
                    continue;
                }
                Date restoredTime = original.getValue();
                if (Objects.nonNull(decision.getMouldChangeStartTime())
                        && (Objects.isNull(restoredTime) || decision.getMouldChangeStartTime().after(restoredTime))) {
                    // 取消清洗只解除清洗占用，原模具仍不能早于实际换模下模时刻被其他机台借用。
                    restoredTime = decision.getMouldChangeStartTime();
                }
                if (Objects.isNull(restoredTime)) {
                    context.getPreScheduledMouldReleaseTimeMap().remove(original.getKey());
                } else {
                    context.getPreScheduledMouldReleaseTimeMap().put(original.getKey(), restoredTime);
                }
                changed = true;
            }
            changed |= Objects.nonNull(context.getOnlySandBlastContinuationReleaseWindowMap().remove(entry.getKey()));
            context.getOnlySandBlastContinuationRemainingQtyMap().remove(entry.getKey());
            context.getCleaningPlanRuntimeState().getPendingRecoveryMachineCodes().remove(entry.getKey());
            if (changed) {
                log.info("取消喷砂并解除清洗模具占用, 工厂: {}, 批次: {}, 机台: {}, 来源计划: {}, 处置: {}",
                        context.getFactoryCode(), context.getBatchNo(), entry.getKey(), event.getSourcePlanId(),
                        decision.getDisposition());
                resourceChanged = true;
            }
        }
        if (resourceChanged) {
            context.setMouldResourceContext(MouldResourceContext.from(context));
        }
        this.reconcileReplacementQuotas(context);
        ContinuationReplacementInspectionSupport.releaseCancelled(context);
    }

    /**
     * 在实际模具绑定前按固定触发时刻占用统一额度，同源左右侧仅登记一次。
     *
     * @param context 当前批次上下文
     * @param machineCode 本次物理事件的代表运行侧
     * @param window 原喷砂清洗窗口
     * @param source 来源续作SKU
     * @return 已占用额度或本次成功占用时返回 true，额度不足时返回 false
     */
    private boolean reserveReplacementQuota(LhScheduleContext context, String machineCode,
                                             MachineCleaningWindowDTO window, SkuScheduleDTO source) {
        String eventKey = this.buildReplacementQuotaKey(machineCode, window,
                source.getMaterialCode(), source.getProductStatus());
        Map<String, Date> reservations = context.getCleaningPlanRuntimeState().getReplacementQuotaReservationMap();
        if (reservations.containsKey(eventKey)) {
            return true;
        }
        Date changeTime = window.getCleanStartTime();
        boolean reserved = mouldChangeBalanceStrategy.tryReserveFixedMouldChange(context, changeTime);
        if (reserved) {
            reservations.put(eventKey, (Date) changeTime.clone());
        }
        String dateKey = LhScheduleTimeUtil.formatDate(changeTime);
        int[] counts = context.getDailyMouldChangeCountMap().getOrDefault(dateKey, new int[]{0, 0});
        String detail = "工厂=" + context.getFactoryCode() + "，批次=" + context.getBatchNo()
                + "，机台=" + machineCode + "，物料=" + source.getMaterialCode()
                + "，状态=" + source.getProductStatus() + "，置换开始="
                + LhScheduleTimeUtil.formatDateTime(changeTime)
                + "，早班次数=" + counts[0] + "/" + LhScheduleTimeUtil.getMorningMouldChangeLimit(context, dateKey)
                + "，中班次数=" + counts[1] + "/" + LhScheduleTimeUtil.getAfternoonMouldChangeLimit(context, dateKey)
                + "，每日次数=" + (counts[0] + counts[1]) + "/"
                + LhScheduleTimeUtil.getDailyMouldChangeLimit(context, dateKey)
                + "，处置=" + (reserved ? "占用一次置换额度" : "额度不足，保留原清洗处理");
        PriorityTraceLogHelper.appendProcessLog(context, "喷砂置换换模额度", detail);
        log.info("{}", detail);
        return reserved;
    }

    /**
     * 释放已经取消或未达到触发时刻的置换额度；同源L/R仍有有效侧时保留该物理次数。
     * 额度身份与清洗事件同属运行态快照，预演恢复后可再次按真实结果正确撤销。
     *
     * @param context 已完成事件取消或正式处置复核的上下文
     */
    private void reconcileReplacementQuotas(LhScheduleContext context) {
        Map<String, Date> reservations = context.getCleaningPlanRuntimeState().getReplacementQuotaReservationMap();
        if (reservations.isEmpty()) {
            return;
        }
        Set<String> activeKeys = context.getContinuationSandBlastWindowMap().entrySet().stream()
                .filter(entry -> StringUtils.isNotEmpty(entry.getValue().getReplacementMouldCode()))
                .filter(entry -> CleaningDispositionResolver.resolve(context, entry.getKey(), entry.getValue())
                        .getDisposition() == CleaningDispositionResolver.Disposition.INDEPENDENT)
                .map(entry -> this.buildReplacementQuotaKey(entry.getKey(), entry.getValue(),
                        entry.getValue().getContinuationMaterialCode(),
                        entry.getValue().getContinuationProductStatus()))
                .collect(Collectors.toSet());
        List<String> cancelledKeys = reservations.keySet().stream()
                .filter(key -> !activeKeys.contains(key)).collect(Collectors.toList());
        for (String eventKey : cancelledKeys) {
            Date changeTime = reservations.remove(eventKey);
            mouldChangeBalanceStrategy.rollbackMouldChange(context, changeTime);
            log.info("喷砂置换额度取消, 工厂: {}, 批次: {}, 事件: {}, 置换开始: {}",
                    context.getFactoryCode(), context.getBatchNo(), eventKey,
                    LhScheduleTimeUtil.formatDateTime(changeTime));
        }
    }

    /**
     * 为同一物理置换构建稳定身份，保留来源、SKU状态及开始时刻以区分独立真实动作。
     *
     * @param machineCode 运行侧机台编码
     * @param window 来源喷砂清洗窗口
     * @param materialCode 触发时刻的续作物料
     * @param productStatus 触发时刻的产品状态
     * @return 同源左右侧一致、不同动作独立的额度身份
     */
    private String buildReplacementQuotaKey(String machineCode, MachineCleaningWindowDTO window,
                                            String materialCode, String productStatus) {
        return LhSingleControlMachineUtil.resolvePhysicalMachineCode(machineCode) + "|" + window.getSourcePlanId()
                + "|" + window.getCleanStartTime().getTime() + "|" + materialCode + "|" + productStatus;
    }

    /**
     * 最终数量稳定后核对完整置换首检，并按置换、首检、换胶囊顺序保留同班原因。
     * 数量及换胶囊事实由主链提供，本方法只追加去重和整理本次事件原因。
     *
     * @param context 排程上下文
     */
    public void appendReplacementAnalysis(LhScheduleContext context) {
        for (LhScheduleResult result : context.getScheduleResultList()) {
            MachineCleaningWindowDTO event = context.getContinuationSandBlastWindowMap().get(result.getLhMachineCode());
            if (Objects.isNull(event) || StringUtils.isEmpty(event.getReplacementMouldCode())
                    || CleaningDispositionResolver.resolve(context, result.getLhMachineCode(), event).getDisposition()
                    != CleaningDispositionResolver.Disposition.INDEPENDENT
                    || !StringUtils.equals(ScheduleTypeEnum.CONTINUOUS.getCode(), result.getScheduleType())
                    || !StringUtils.equals(event.getContinuationMaterialCode(), result.getMaterialCode())
                    || !StringUtils.equals(event.getContinuationProductStatus(), result.getProductStatus())) {
                continue;
            }
            LhShiftConfigVO shift = LhScheduleTimeUtil.resolveShiftByTime(context.getScheduleWindowShifts(), event.getCleanStartTime());
            if (Objects.nonNull(shift) && Objects.nonNull(result.getSpecEndTime())
                    && result.getSpecEndTime().after(event.getCleanStartTime())) {
                ShiftFieldUtil.appendShiftAnalysis(result, shift.getShiftIndex(), REPLACEMENT_ANALYSIS);
            }
            if (event.getReplacementReadyTime() == null || event.getReplacementInspectionShiftStartTime() == null) {
                continue;
            }
            LhShiftConfigVO inspectionShift = LhScheduleTimeUtil.resolveShiftByTime(
                    context.getScheduleWindowShifts(), event.getReplacementInspectionShiftStartTime());
            if (inspectionShift == null) {
                continue;
            }
            Integer quantity = ShiftFieldUtil.getShiftPlanQty(result, inspectionShift.getShiftIndex());
            Date actualEnd = ShiftFieldUtil.getShiftEndTime(result, inspectionShift.getShiftIndex());
            boolean pureReplacementInspection = ContinuationReplacementInspectionSupport
                    .isReplacementInspectionShift(context, result, inspectionShift);
            if (quantity != null && quantity > 0 && actualEnd != null
                    && event.getReplacementFirstInspectionQty() != null && event.getReplacementFirstInspectionQty() > 0
                    && (!pureReplacementInspection || quantity >= event.getReplacementFirstInspectionQty())
                    && !actualEnd.before(event.getReplacementReadyTime())) {
                int inspectionShiftIndex = inspectionShift.getShiftIndex();
                boolean capsuleReplaced = ShiftFieldUtil.hasShiftAnalysis(result, inspectionShiftIndex,
                        CapsuleReplacementRuleService.CAPSULE_REPLACEMENT_ANALYSIS);
                ShiftFieldUtil.removeShiftAnalysis(result, inspectionShiftIndex, REPLACEMENT_ANALYSIS);
                ShiftFieldUtil.removeShiftAnalysis(result, inspectionShiftIndex, "首检");
                ShiftFieldUtil.removeShiftAnalysis(result, inspectionShiftIndex,
                        CapsuleReplacementRuleService.CAPSULE_REPLACEMENT_ANALYSIS);
                ShiftFieldUtil.appendShiftAnalysis(result, inspectionShiftIndex, REPLACEMENT_ANALYSIS);
                ShiftFieldUtil.appendShiftAnalysis(result, inspectionShiftIndex, "首检");
                if (capsuleReplaced) {
                    ShiftFieldUtil.appendShiftAnalysis(result, inspectionShiftIndex,
                            CapsuleReplacementRuleService.CAPSULE_REPLACEMENT_ANALYSIS);
                }
                log.info("续作置换首检核验, 工厂: {}, 批次: {}, 机台: {}, 物料: {}, 状态: {}, "
                                + "准备开始: {}, 首检开始: {}, 首检结束: {}, 首检条数: {}, 班次: {}, 班次计划量: {}",
                        context.getFactoryCode(), context.getBatchNo(), result.getLhMachineCode(),
                        result.getMaterialCode(), result.getProductStatus(), event.getCleanStartTime(),
                        event.getReplacementInspectionStartTime(), event.getReplacementReadyTime(),
                        Math.min(quantity, event.getReplacementFirstInspectionQty()), inspectionShift.getShiftIndex(), quantity);
            }
        }
    }
}
