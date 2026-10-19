package com.zlt.aps.lh.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.zlt.aps.lh.api.domain.dto.MachineCleaningWindowDTO;
import com.zlt.aps.lh.api.domain.dto.MachineScheduleDTO;
import com.zlt.aps.lh.api.domain.dto.SkuScheduleDTO;
import com.zlt.aps.lh.api.domain.entity.LhScheduleResult;
import com.zlt.aps.lh.api.domain.vo.LhShiftConfigVO;
import com.zlt.aps.lh.component.MonthPlanDateResolver;
import com.zlt.aps.lh.component.SharedEmbryoEndingAllocationService;
import com.zlt.aps.lh.component.TargetScheduleQtyResolver;
import com.zlt.aps.lh.context.LhScheduleContext;
import com.zlt.aps.lh.engine.strategy.support.ContinuationEndingMachineProfile;
import com.zlt.aps.lh.engine.strategy.support.ContinuationMachineCapacitySupport;
import com.zlt.aps.lh.engine.strategy.support.ContinuationResultSupport;
import com.zlt.aps.lh.engine.strategy.support.ContinuationTemporaryFaultTransferEvent;
import com.zlt.aps.lh.engine.strategy.support.PreviousAlternatePlanReleaseEvent;
import com.zlt.aps.lh.util.LhScheduleTimeUtil;
import com.zlt.aps.lh.util.LhSingleControlMachineUtil;
import com.zlt.aps.lh.util.PriorityTraceLogHelper;
import com.zlt.aps.lh.util.ShiftCapacityResolverUtil;
import com.zlt.aps.lh.util.ShiftFieldUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 独立共用胎胚收尾的分组、原机分配、整组提交及真实释放服务。
 * 不调用普通余量收尾或时间下机算法，消费和快照仍沿用统一上下文。
 *
 * @author APS
 * @date 2026/10/19
 */
@Slf4j
@Component
public class SharedEmbryoEndingScheduleService {

    /**
     * 无状态的两层数量分配算法。
     */
    private final SharedEmbryoEndingAllocationService sharedEmbryoEndingAllocationService;

    /**
     * 唯一数量及消费账本入口。
     */
    private final TargetScheduleQtyResolver targetScheduleQtyResolver;

    /**
     * 共用的真实容量和物理约束。
     */
    private final ContinuationMachineCapacitySupport capacitySupport;

    /**
     * 共用结果校验、来源及资源状态登记。
     */
    private final ContinuationResultSupport resultSupport;

    /**
     * 保留无容器直接构造的兼容入口，所有批次状态仍由调用上下文提供。
     */
    public SharedEmbryoEndingScheduleService() {
        this(new SharedEmbryoEndingAllocationService(), new TargetScheduleQtyResolver(),
                new ContinuationMachineCapacitySupport(), new ContinuationResultSupport());
    }

    /**
     * 生产环境注入同一分配、数量及基础支持组件，不依赖原续作策略。
     *
     * @param sharedEmbryoEndingAllocationService 数量分配算法
     * @param targetScheduleQtyResolver 统一数量解析器
     * @param capacitySupport 基础容量支持
     * @param resultSupport 结果与资源状态支持
     */
    @Autowired
    public SharedEmbryoEndingScheduleService(SharedEmbryoEndingAllocationService sharedEmbryoEndingAllocationService,
            TargetScheduleQtyResolver targetScheduleQtyResolver, ContinuationMachineCapacitySupport capacitySupport,
            ContinuationResultSupport resultSupport) {
        this.sharedEmbryoEndingAllocationService = sharedEmbryoEndingAllocationService;
        this.targetScheduleQtyResolver = targetScheduleQtyResolver;
        this.capacitySupport = capacitySupport;
        this.resultSupport = resultSupport;
    }

    /**
     * 按SKU固定额度和原机真实容量独立分配任务，整组校验后提交，不消费普通下机决定。
     *
     * @param context 已冻结SKU份额和已有续作结果的上下文
     * @throws IllegalStateException 已完成正式扣账，或原机数据、整组方案不满足硬约束
     */
    public void allocateSharedEmbryoEndingMachines(LhScheduleContext context) {
        if (context.getSharedEmbryoStockEndingIdentityMap().isEmpty()) {
            return;
        }
        if (context.isContinuousDailyQuotaSynced()) {
            throw new IllegalStateException("续作已扣账，禁止重新分配胎胚收尾机台任务");
        }
        List<LhShiftConfigVO> shifts = LhScheduleTimeUtil.getScheduleShifts(context, context.getScheduleDate());
        Map<String, List<LhScheduleResult>> groups = new LinkedHashMap<>();
        for (LhScheduleResult result : context.getScheduleResultList()) {
            SkuScheduleDTO sku = resultSupport.resolveResultSourceSku(context, result);
            if (resultSupport.isPureContinuousResult(result)
                    && targetScheduleQtyResolver.isSharedEmbryoStockEnding(context, sku)) {
                groups.computeIfAbsent(MonthPlanDateResolver.buildMaterialStatusKey(
                        sku.getMaterialCode(), sku.getProductStatus()), key -> new ArrayList<>()).add(result);
            }
        }
        if (groups.isEmpty()) {
            return;
        }
        Map<String, List<LhScheduleResult>> occupiedByMachine = context.getScheduleResultList().stream()
                .collect(Collectors.groupingBy(LhScheduleResult::getLhMachineCode));
        context.getMachineAssignmentMap().forEach((machineCode, assigned) -> {
            if (!CollectionUtils.isEmpty(assigned)) {
                occupiedByMachine.computeIfAbsent(machineCode, key -> new ArrayList<>()).addAll(assigned);
            }
        });
        for (List<LhScheduleResult> results : groups.values()) {
            if (results.stream().allMatch(context.getSharedEmbryoStockEndingMachineQuotaMap()::containsKey)) {
                continue;
            }
            this.allocateSharedEmbryoEndingGroup(context, resultSupport.resolveResultSourceSku(context, results.get(0)),
                    results, shifts, occupiedByMachine);
        }
    }

    /**
     * 冻结物理机台画像并提交一个SKU的最早完工方案；不足或奇数不可配对的量保留在该SKU下。
     *
     * @param context 排程上下文
     * @param sku 固定份额来源
     * @param results 本SKU全部原机结果，包含临时零量结果
     * @param shifts 完整窗口
     * @param occupiedByMachine 本阶段一次构造的机台占用索引
     */
    private void allocateSharedEmbryoEndingGroup(LhScheduleContext context, SkuScheduleDTO sku,
            List<LhScheduleResult> results, List<LhShiftConfigVO> shifts,
            Map<String, List<LhScheduleResult>> occupiedByMachine) {
        targetScheduleQtyResolver.applyEmbryoStockEndingTargetQtyIfNecessary(context, sku, "独立机台分配");
        Set<LhScheduleResult> ownResults = Collections.newSetFromMap(new IdentityHashMap<>());
        ownResults.addAll(results);
        Set<String> codes = results.stream().map(LhScheduleResult::getLhMachineCode).collect(Collectors.toSet());
        Map<String, Date> cutoffs = new HashMap<>();
        Map<String, Date> nextActions = new HashMap<>();
        for (String code : codes) {
            if (!capacitySupport.collectOccupiedCutoffs(occupiedByMachine.get(code), ownResults,
                    codes, shifts, cutoffs, nextActions)) {
                throw new IllegalStateException("胎胚收尾原机已有占用缺少真实时间");
            }
        }
        List<ContinuationEndingMachineProfile> profiles = this.buildSharedEmbryoEndingProfiles(context, results, shifts, cutoffs);
        int fixedQty = context.getScheduleResultList().stream().filter(result -> !ownResults.contains(result))
                .filter(result -> StringUtils.equals(sku.getMaterialCode(), result.getMaterialCode())
                        && StringUtils.equals(sku.getProductStatus(), result.getProductStatus()))
                .mapToInt(ShiftFieldUtil::resolveScheduledQty).sum();
        int target = Math.max(0, targetScheduleQtyResolver.resolveEmbryoStockSkuQuotaLimit(context, sku) - fixedQty);
        List<SharedEmbryoEndingAllocationService.MachineCapacity> capacities = profiles.stream()
                .map(profile -> new SharedEmbryoEndingAllocationService.MachineCapacity(
                        this.resolveSharedEmbryoAllocationUnitCode(profile), profile.getMultiple(), profile::quantityUntil)).collect(Collectors.toList());
        Map<String, Integer> quantities = sharedEmbryoEndingAllocationService.allocateMachines(
                capacities, target, shifts.get(0).getShiftStartDateTime(), shifts.get(shifts.size() - 1).getShiftEndDateTime());
        this.commitSharedEmbryoEndingMachinePlan(context, sku, results, shifts, profiles, quantities);
        String detail = String.format("工厂=%s, 日期=%s, 批次=%s, 胎胚=%s, 物料=%s, 状态=%s, 待分配=%s, 物理机台分配=%s, 未完成=%s",
                context.getFactoryCode(), context.getScheduleDate(), context.getBatchNo(), sku.getEmbryoCode(),
                sku.getMaterialCode(), sku.getProductStatus(), target, quantities,
                target - quantities.values().stream().mapToInt(Integer::intValue).sum());
        PriorityTraceLogHelper.appendProcessLog(context, "共用胎胚同SKU原机分配", detail);
        log.info("共用胎胚同SKU原机分配, {}", detail);
    }

    /**
     * 仅从实际占用、故障和清洗等物理硬边界构造整机容量，不读取普通下机请求。
     *
     * @param context 排程上下文
     * @param results 同SKU原机结果
     * @param shifts 全窗口班次
     * @param cutoffs 已提交资源的真实占用截止
     * @return 按物理编码排序的只读原机画像
     */
    private List<ContinuationEndingMachineProfile> buildSharedEmbryoEndingProfiles(LhScheduleContext context,
            List<LhScheduleResult> results, List<LhShiftConfigVO> shifts, Map<String, Date> cutoffs) {
        Set<String> wholeMachines = resultSupport.resolveWholeSingleControlMachineCodes(context, results);
        Map<String, List<LhScheduleResult>> physicalGroups = results.stream().collect(Collectors.groupingBy(
                result -> wholeMachines.contains(result.getLhMachineCode())
                        ? LhSingleControlMachineUtil.resolvePhysicalMachineCode(result.getLhMachineCode()) : result.getLhMachineCode(),
                java.util.TreeMap::new, Collectors.toList()));
        List<ContinuationEndingMachineProfile> profiles = new ArrayList<>(physicalGroups.size());
        for (List<LhScheduleResult> sides : physicalGroups.values()) {
            sides.sort(Comparator.comparing(LhScheduleResult::getLhMachineCode));
            boolean paired = sides.stream().anyMatch(side -> wholeMachines.contains(side.getLhMachineCode()));
            if (sides.size() > 2 || (paired && sides.size() != 2)
                    || sides.stream().map(LhScheduleResult::getLhMachineCode).distinct().count() != sides.size()) {
                throw new IllegalStateException("胎胚收尾物理机台或L/R来源不完整");
            }
            for (LhScheduleResult side : sides) {
                MachineCleaningWindowDTO sandBlast = context.getOnlySandBlastContinuationReleaseWindowMap().get(side.getLhMachineCode());
                if (Objects.nonNull(sandBlast)) {
                    cutoffs.merge(side.getLhMachineCode(), sandBlast.getCleanStartTime(), capacitySupport::earlier);
                }
                ContinuationTemporaryFaultTransferEvent fault = context.getContinuationTemporaryFaultTransferEventMap().get(side.getLhMachineCode());
                if (Objects.nonNull(fault)) {
                    cutoffs.merge(side.getLhMachineCode(), fault.getFaultStartTime(), capacitySupport::earlier);
                }
            }
            ContinuationEndingMachineProfile profile = capacitySupport.buildMachineProfile(
                    context, sides, shifts, cutoffs, Collections.emptySet(), true);
            if (Objects.isNull(profile)) {
                throw new IllegalStateException("胎胚收尾原机真实容量或时间轴不完整");
            }
            Date deadline = sides.stream().map(side -> cutoffs.get(side.getLhMachineCode()))
                    .filter(Objects::nonNull).min(Date::compareTo).orElse(null);
            profile.restrictToReleaseDeadline(deadline);
            profiles.add(profile);
        }
        return profiles;
    }

    /**
     * 取得冻结运行模式对应的生产单元编码，整机归一化，独立单侧保留L/R编码。
     *
     * @param profile 已按运行模式构造的容量画像
     * @return 唯一生产单元编码
     */
    private String resolveSharedEmbryoAllocationUnitCode(ContinuationEndingMachineProfile profile) {
        String code = profile.getOriginals().get(0).getLhMachineCode();
        return profile.getOriginals().size() > 1 ? LhSingleControlMachineUtil.resolvePhysicalMachineCode(code) : code;
    }

    /**
     * 整组验证副本后原子回写机台任务，正式数量仍由原统一入口一次扣账。
     *
     * @param context 排程上下文
     * @param sku 固定额度来源
     * @param results 原机结果
     * @param shifts 完整班次
     * @param profiles 原机只读容量画像
     * @param quantities 物理机台任务量
     */
    private void commitSharedEmbryoEndingMachinePlan(LhScheduleContext context, SkuScheduleDTO sku,
            List<LhScheduleResult> results, List<LhShiftConfigVO> shifts,
            List<ContinuationEndingMachineProfile> profiles, Map<String, Integer> quantities) {
        Map<LhScheduleResult, LhScheduleResult> proposed = new IdentityHashMap<>();
        Map<LhScheduleResult, Date> releases = new IdentityHashMap<>();
        int[] machineQuantities = new int[profiles.size()];
        for (int index = 0; index < profiles.size(); index++) {
            ContinuationEndingMachineProfile profile = profiles.get(index);
            String machineCode = this.resolveSharedEmbryoAllocationUnitCode(profile);
            int quantity = quantities.get(machineCode);
            machineQuantities[index] = quantity;
            if (!profile.isLegalQuantity(quantity)) {
                throw new IllegalStateException("胎胚收尾机台任务违反数量单位");
            }
            List<LhScheduleResult> copies = new ArrayList<>();
            for (LhScheduleResult original : profile.getOriginals()) {
                LhScheduleResult copy = BeanUtil.copyProperties(original, LhScheduleResult.class);
                proposed.put(original, copy);
                copies.add(copy);
                releases.put(original, profile.physicalReadyTime(profile.switchReadyTime(quantity)));
            }
            profile.writePlan(copies, quantity);
        }
        if (!resultSupport.validatePlan(context, sku, proposed, machineQuantities, shifts)) {
            throw new IllegalStateException("胎胚收尾机台方案未通过整组数量和时间校验");
        }
        Set<LocalDate> productionDates = resultSupport.groupShiftsByWorkDate(shifts).keySet();
        for (LhScheduleResult original : results) {
            BeanUtil.copyProperties(proposed.get(original), original);
            int quantity = ShiftFieldUtil.resolveScheduledQty(original);
            context.getSharedEmbryoStockEndingMachineQuotaMap().put(original, quantity);
            context.getSharedEmbryoStockEndingMachineReleaseMap().put(original, releases.get(original));
            context.getEndingFillAllowedOverQtyMap().remove(original);
            context.getSharedEmbryoEndingStaggerAllowedOverQtyMap().remove(original);
            for (LocalDate date : productionDates) {
                if (resultSupport.isMachineStillScheduledOnWorkDate(context, shifts, original, date)) {
                    context.recordScheduledMachine(date, original.getStructureName(), original.getMaterialCode(),
                            original.getProductStatus(), original.getLhMachineCode());
                } else {
                    context.removeScheduledMachine(date, original.getStructureName(), original.getMaterialCode(),
                            original.getProductStatus(), original.getLhMachineCode());
                }
            }
        }
    }

    /**
     * 共用胎胚前料按分摊额度和真实生产完成时刻发布独立释放，历史班初不得截断其产量。
     *
     * @param context 已成立分摊身份及本轮结果
     * @param event 历史指定前后料关系
     * @param sourceSku 前料来源SKU
     * @param sourceResults 本机前料结果
     * @param shifts 实际班次窗口
     * @return 已完成时的物理释放；未完成时保留原事件时间但禁止发布释放
     */
    public Date resolveSharedEmbryoEndingReleaseTime(LhScheduleContext context,
            PreviousAlternatePlanReleaseEvent event, SkuScheduleDTO sourceSku,
            List<LhScheduleResult> sourceResults, List<LhShiftConfigVO> shifts) {
        boolean wholeMachine = resultSupport.isMaterialWholeSingleControlSku(context, event.getMaterialCode());
        List<LhScheduleResult> allocatedSources = context.getSharedEmbryoStockEndingMachineQuotaMap().keySet().stream()
                .filter(result -> StringUtils.equals(event.getMaterialCode(), result.getMaterialCode())
                        && StringUtils.equals(event.getProductStatus(), result.getProductStatus()))
                .filter(result -> StringUtils.equals(event.getMachineCode(), result.getLhMachineCode())
                        || (wholeMachine
                        && StringUtils.equals(LhSingleControlMachineUtil.resolvePhysicalMachineCode(event.getMachineCode()),
                        LhSingleControlMachineUtil.resolvePhysicalMachineCode(result.getLhMachineCode()))))
                .collect(Collectors.toList());
        boolean allocated = !allocatedSources.isEmpty();
        int targetQty = allocated ? allocatedSources.stream()
                .mapToInt(context.getSharedEmbryoStockEndingMachineQuotaMap()::get).sum()
                : targetScheduleQtyResolver.resolveEmbryoStockSkuQuotaLimit(context, sourceSku);
        int scheduledQty = allocated ? allocatedSources.stream().mapToInt(ShiftFieldUtil::resolveScheduledQty).sum()
                : resultSupport.resolveContinuousMachineResults(context, sourceSku).stream()
                .mapToInt(ShiftFieldUtil::resolveScheduledQty).sum();
        Date actualEnd = allocated ? allocatedSources.stream()
                .map(context.getSharedEmbryoStockEndingMachineReleaseMap()::get)
                .filter(Objects::nonNull).max(Date::compareTo).orElse(null)
                : sourceResults.stream().map(resultSupport::resolveLastPositiveProductionEndTime)
                .filter(Objects::nonNull).max(Date::compareTo).orElse(null);
        boolean incompleteSide = allocatedSources.stream().anyMatch(result ->
                ShiftFieldUtil.resolveScheduledQty(result) < context.getSharedEmbryoStockEndingMachineQuotaMap().get(result));
        event.setSharedEmbryoEndingPending((!allocated && targetQty <= 0)
                || incompleteSide || scheduledQty < targetQty || Objects.isNull(actualEnd));
        if (event.isSharedEmbryoEndingPending()) {
            resultSupport.logPreviousAlternateReleaseDecision(context, event, "共用胎胚分摊未完成，保留前料及物理占用");
            return event.getOfflineTime();
        }
        actualEnd = ShiftCapacityResolverUtil.resolveCapsuleReplacementReadyTime(
                context, event.getMachineCode(), actualEnd);
        event.setOfflineTime(actualEnd);
        event.setCompletedStrictEndingReleaseTime(actualEnd);
        event.setCompletedEmbryoEndingWindowEndTime(shifts.get(shifts.size() - 1).getShiftEndDateTime());
        resultSupport.logPreviousAlternateReleaseDecision(context, event,
                "共用胎胚分摊额度已完成，按独立真实释放承接后料；释放="
                        + LhScheduleTimeUtil.formatDateTime(actualEnd));
        return actualEnd;
    }

    /**
     * 发布已完成的独立机台任务及真实资源释放，零量原机也保留其物理就绪时刻。
     *
     * @param context 最终扣账后的独立任务及机台状态
     */
    public void publishSharedEmbryoEndingMachineRelease(LhScheduleContext context) {
        Map<LhScheduleResult, Integer> tasks = context.getSharedEmbryoStockEndingMachineQuotaMap();
        if (tasks.isEmpty()) {
            return;
        }
        Set<String> wholeMachines = resultSupport.resolveWholeSingleControlMachineCodes(context, new ArrayList<>(tasks.keySet()));
        Map<String, List<LhScheduleResult>> units = tasks.keySet().stream().collect(Collectors.groupingBy(result ->
                MonthPlanDateResolver.buildMaterialStatusKey(result.getMaterialCode(), result.getProductStatus()) + "|"
                        + (wholeMachines.contains(result.getLhMachineCode())
                        ? LhSingleControlMachineUtil.resolvePhysicalMachineCode(result.getLhMachineCode()) : result.getLhMachineCode())));
        for (List<LhScheduleResult> sides : units.values()) {
            boolean incomplete = sides.stream().anyMatch(result -> ShiftFieldUtil.resolveScheduledQty(result) < tasks.get(result)
                    || Objects.isNull(context.getSharedEmbryoStockEndingMachineReleaseMap().get(result)));
            if (incomplete || sides.stream().anyMatch(result -> context.isContinuousStopHoldMachine(result.getLhMachineCode())
                    || context.getOnlySandBlastContinuationReleaseWindowMap().containsKey(result.getLhMachineCode())
                    || context.getContinuationTemporaryFaultTransferEventMap().containsKey(result.getLhMachineCode()))) {
                continue;
            }
            Date release = sides.stream().map(context.getSharedEmbryoStockEndingMachineReleaseMap()::get)
                    .max(Date::compareTo).orElse(null);
            for (LhScheduleResult result : sides) {
                MachineScheduleDTO machine = context.getMachineScheduleMap().get(result.getLhMachineCode());
                if (Objects.nonNull(machine)) {
                    machine.setEnding(true);
                    machine.setEstimatedEndTime(release);
                    resultSupport.registerReleasedContinuousMachine(context, result.getLhMachineCode(), result.getMaterialCode(), "独立胎胚机台任务完成");
                }
            }
        }
        context.setMouldResourceContext(null);
    }
}
