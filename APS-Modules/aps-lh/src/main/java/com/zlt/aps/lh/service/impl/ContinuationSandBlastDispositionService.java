package com.zlt.aps.lh.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.aps.lh.api.domain.dto.MachineCleaningWindowDTO;
import com.zlt.aps.lh.api.domain.dto.MachineScheduleDTO;
import com.zlt.aps.lh.api.domain.dto.SkuScheduleDTO;
import com.zlt.aps.lh.api.domain.entity.LhScheduleResult;
import com.zlt.aps.lh.api.domain.vo.LhShiftConfigVO;
import com.zlt.aps.lh.api.enums.ScheduleTypeEnum;
import com.zlt.aps.lh.context.LhScheduleContext;
import com.zlt.aps.lh.engine.strategy.support.MouldResourceAllocationResult;
import com.zlt.aps.lh.engine.strategy.support.MouldResourceContext;
import com.zlt.aps.lh.util.LhMouldCodeUtil;
import com.zlt.aps.lh.util.LhScheduleTimeUtil;
import com.zlt.aps.lh.util.LhSingleControlMachineUtil;
import com.zlt.aps.lh.util.MachineCleaningOverlapUtil;
import com.zlt.aps.lh.util.ShiftCapacityResolverUtil;
import com.zlt.aps.lh.util.ShiftFieldUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 续作仅喷砂处置：只负责精确模具交接和清洗事件，不改变数量账本及选机算法。
 * 本协作服务无外部依赖和实例运行态，由使用方持有，全部批次状态均写入排程上下文。
 */
@Slf4j
public class ContinuationSandBlastDispositionService {

    /** 同 SKU 原机更换模具的业务原因。 */
    public static final String REPLACEMENT_ANALYSIS = "模具置换";

    /**
     * 续作计算产能前优先落实可用模具，防止置换后仍扣减原模具的清洗停机产能。
     * 单控同源事件先整体检查足量模具，再按运行侧精确绑定，不允许半套置换。
     * @param context 排程上下文
     */
    public void prepare(LhScheduleContext context) {
        if (Objects.isNull(context) || CollectionUtils.isEmpty(context.getContinuousSkuList())
                || CollectionUtils.isEmpty(context.getScheduleWindowShifts())
                || !CollectionUtils.isEmpty(context.getContinuationSandBlastWindowMap())) {
            return;
        }
        Map<String, SkuScheduleDTO> sources = context.getContinuousSkuList().stream()
                .filter(Objects::nonNull)
                .filter(sku -> StringUtils.isNotEmpty(sku.getContinuousMachineCode()))
                .collect(Collectors.toMap(SkuScheduleDTO::getContinuousMachineCode,
                        sku -> sku, (first, second) -> first, LinkedHashMap::new));
        Map<String, MachineCleaningWindowDTO> candidates = new LinkedHashMap<>(sources.size());
        for (String machineCode : sources.keySet()) {
            MachineScheduleDTO machine = context.getMachineScheduleMap().get(machineCode);
            MachineCleaningWindowDTO window = this.resolveWindow(context, machine);
            if (Objects.nonNull(window)) {
                candidates.put(machineCode, window);
            }
        }
        if (CollectionUtils.isEmpty(candidates)) {
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
     * 整组预占替换模具；没有足量模具时只记录决策，截断和扣账仍由续作主链完成。
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
        List<String> freeMoulds = resources.resolveFreeValidMouldCodes(materialCode, oldMoulds, startTime);
        boolean replacement = freeMoulds.size() >= requiredQty;
        int offset = 0;
        // 同源左右侧可能共用原模具集合，恢复边界必须统一取整组交接前快照。
        Map<String, Date> originalReleaseTimes = new LinkedHashMap<>(oldMoulds.size());
        oldMoulds.forEach(code -> originalReleaseTimes.put(code, context.getPreScheduledMouldReleaseTimeMap().get(code)));
        for (String side : sides) {
            MachineScheduleDTO machine = context.getMachineScheduleMap().get(side);
            MachineCleaningWindowDTO event = BeanUtil.copyProperties(candidates.get(side), MachineCleaningWindowDTO.class);
            event.setContinuationMaterialCode(materialCode);
            event.setContinuationProductStatus(sources.get(side).getProductStatus());
            event.setContinuationMaterialDesc(sources.get(side).getMaterialDesc());
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
                event.setRemark(StringUtils.isEmpty(event.getRemark()) ? REPLACEMENT_ANALYSIS
                        : event.getRemark() + "；" + REPLACEMENT_ANALYSIS);
                // 只解除原模具对机台的占用；清洗事件另存，供最终计划与回填消费。
                this.detach(context, machine, candidates.get(side), event);
                event.getMouldReleaseTimeBeforeReplacement().replaceAll((code, time) -> originalReleaseTimes.get(code));
            }
            context.getContinuationSandBlastWindowMap().put(side, event);
            log.info("续作仅喷砂处置, 机台: {}, SKU: {}, 产品状态: {}, 喷砂开始: {}, 是否仅喷砂: true, "
                            + "是否找到闲置可用模具: {}, 原模具: {}, 替换模具: {}, 处理方式: {}",
                    side, materialCode, event.getContinuationProductStatus(), startTime, replacement,
                    event.getMouldCode(), event.getReplacementMouldCode(),
                    replacement ? REPLACEMENT_ANALYSIS : "SKU下机重新排产");
        }
    }

    /**
     * 清洗移到离机模具后解除原机台阻塞，并登记模具清洗结束前不可用。
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
            if (!event.isOffMachineCleaning()) {
                event.getMouldReleaseTimeBeforeReplacement().put(mouldCode,
                        context.getPreScheduledMouldReleaseTimeMap().get(mouldCode));
            }
            context.getPreScheduledMouldReleaseTimeMap().merge(mouldCode, event.getCleanEndTime(),
                    (previous, current) -> previous.after(current) ? previous : current);
        }
        event.setOffMachineCleaning(true);
    }

    /**
     * 最终续作未跨过触发时刻时取消预占；禁止为已经提前结束的 SKU 生成置换事件。
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
                if (!Objects.equals(context.getPreScheduledMouldReleaseTimeMap().get(previous.getKey()), event.getCleanEndTime())) {
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
        context.setMouldResourceContext(MouldResourceContext.from(context));
    }

    /**
     * 最终数量稳定后追加置换班次原因，不改续作身份、计划量或正常首检计数。
     * @param context 排程上下文
     */
    public void appendReplacementAnalysis(LhScheduleContext context) {
        for (LhScheduleResult result : context.getScheduleResultList()) {
            MachineCleaningWindowDTO event = context.getContinuationSandBlastWindowMap().get(result.getLhMachineCode());
            if (Objects.isNull(event) || StringUtils.isEmpty(event.getReplacementMouldCode())
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
        }
    }
}
