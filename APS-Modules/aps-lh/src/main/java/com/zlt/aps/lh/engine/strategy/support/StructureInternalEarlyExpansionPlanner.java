package com.zlt.aps.lh.engine.strategy.support;

import cn.hutool.core.bean.BeanUtil;
import com.zlt.aps.lh.api.domain.dto.SkuScheduleDTO;
import com.zlt.aps.lh.api.enums.ConstructionStageEnum;
import com.zlt.aps.lh.api.enums.ScheduleTypeEnum;
import com.zlt.aps.lh.api.enums.SkuScheduleSourceTypeEnum;
import com.zlt.aps.lh.component.MonthPlanDateResolver;
import com.zlt.aps.lh.component.TargetScheduleQtyResolver;
import com.zlt.aps.lh.component.UnscheduledResultCollector;
import com.zlt.aps.lh.context.LhScheduleContext;
import com.zlt.aps.lh.service.ILhDailyMouldCalcService;
import com.zlt.aps.lh.util.PriorityTraceLogHelper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 结构内下一次未满足增机需求的提前候选生成器。
 * 原机绑定继续消费原账本；后续增机使用独立候选复用提前生产中心，不重置已生效的运行视图。
 *
 * @author APS
 * @date 2026/10/12
 */
@Slf4j
public final class StructureInternalEarlyExpansionPlanner {

    /**
     * 工具类不允许实例化。
     */
    private StructureInternalEarlyExpansionPlanner() {
    }

    /**
     * 正常任务收口后登记结构内下一次未来增机候选，不分配机台或扣减数量。
     *
     * @param context 排程上下文
     * @param state 已完成当前日正常竞争的日驱动状态
     * @param businessDate 当前实际执行日
     * @param machineCountService 原始目标机台Map查询服务
     * @param quantityResolver 实际消费账本入口
     * @param unscheduledCollector 需求身份关联入口
     */
    public static void registerCandidates(LhScheduleContext context, DayDrivenScheduleState state,
                                         LocalDate businessDate, ILhDailyMouldCalcService machineCountService,
                                         TargetScheduleQtyResolver quantityResolver,
                                         UnscheduledResultCollector unscheduledCollector) {
        if (context.isIsolatedNextShiftPlan() || context.isStopProductionMode()) {
            return;
        }
        // 同一物料状态优先沿用新增链路对象的排序和需求身份，续作副本只补充尚未入池的物料。
        Map<String, SkuScheduleDTO> sourceMap = Stream.concat(
                        state.getOrderedSkuList().stream(), context.getContinuousSkuList().stream())
                .filter(Objects::nonNull)
                .filter(sku -> ConstructionStageEnum.FORMAL.getCode().equals(sku.getConstructionStage()))
                .collect(Collectors.toMap(sku -> MonthPlanDateResolver.buildMaterialStatusKey(
                                sku.getMaterialCode(), sku.getProductStatus()),
                        sku -> sku, (firstSku, duplicateSku) -> firstSku, LinkedHashMap::new));
        for (SkuScheduleDTO sourceSku : sourceMap.values()) {
            if (context.getStructurePlanMachineCount(businessDate, sourceSku.getStructureName()) <= 0
                    || context.getStructureSwitchSourceMap().containsKey(sourceSku.getStructureName())) {
                continue;
            }
            int committedCount = DailyMachineExpansionPlanner.countCommittedMachineDemand(
                    context, sourceSku, businessDate);
            int remainingQty = quantityResolver.previewProductionRemainingQty(context, sourceSku);
            if (committedCount <= 0 || remainingQty <= 0) {
                continue;
            }
            LocalDate requirementDate = resolveNextFutureRequirementDate(
                    context, sourceSku, businessDate, committedCount, machineCountService);
            if (Objects.isNull(requirementDate) || hasPendingRequirement(state, sourceSku, requirementDate)) {
                continue;
            }
            int targetCount = machineCountService.getRequiredMachineCount(
                    context, sourceSku.getMaterialCode(), sourceSku.getProductStatus(), requirementDate);
            SkuScheduleDTO candidate = copyExpansionCandidate(
                    sourceSku, requirementDate, committedCount, targetCount, remainingQty,
                    MonthPlanDateResolver.resolveDayQty(context, sourceSku.getMaterialCode(),
                            sourceSku.getProductStatus(), requirementDate));
            unscheduledCollector.bindDerivedDemand(context, sourceSku, candidate);
            context.getNewSpecSkuList().add(candidate);
            state.registerPendingSku(candidate);
            String detail = String.format("工厂=%s, 批次=%s, 业务日=%s, 物料=%s, 状态=%s, "
                            + "需求日期=%s, 已落实物理机台=%d, 目标机台=%d, 缺口=%d, 实际剩余量=%d",
                    context.getFactoryCode(), context.getBatchNo(), businessDate,
                    sourceSku.getMaterialCode(), sourceSku.getProductStatus(), requirementDate,
                    committedCount, targetCount, targetCount - committedCount, remainingQty);
            log.info("结构内后续增机提前候选, {}", detail);
            PriorityTraceLogHelper.appendProcessLog(context, "结构内后续增机提前候选", detail);
        }
    }

    /**
     * 从原始目标Map查找下一次增机日，当天仍有缺口时留给正常竞争处理。
     *
     * @param context 排程上下文
     * @param sku 需求来源SKU
     * @param businessDate 当前业务日
     * @param committedCount 已落实物理机台数
     * @param machineCountService 原始目标机台查询入口
     * @return 固定提前范围内最早未来增机日；无未来需求或当天未满足时返回null
     */
    public static LocalDate resolveNextFutureRequirementDate(LhScheduleContext context, SkuScheduleDTO sku,
                                                            LocalDate businessDate, int committedCount,
                                                            ILhDailyMouldCalcService machineCountService) {
        if (machineCountService.getRequiredMachineCount(
                context, sku.getMaterialCode(), sku.getProductStatus(), businessDate) > committedCount) {
            return null;
        }
        LocalDate maxDate = EarlyProductionChecker.resolveEarlyProductionMaxDate(context, null);
        for (LocalDate date = businessDate.plusDays(1); Objects.nonNull(maxDate) && !date.isAfter(maxDate);
             date = date.plusDays(1)) {
            if (MonthPlanDateResolver.resolveDayQty(context, sku.getMaterialCode(), sku.getProductStatus(), date) > 0
                    && machineCountService.getRequiredMachineCount(
                    context, sku.getMaterialCode(), sku.getProductStatus(), date) > committedCount) {
                return date;
            }
        }
        return null;
    }

    /**
     * 防止同物料状态、同一未来需求日被重复生成；已绑定候选只负责原机续排。
     *
     * @param state 日驱动状态
     * @param sourceSku 来源SKU
     * @param requirementDate 未来需求日期
     * @return 已有未绑定的同需求续作增机候选时返回true
     */
    private static boolean hasPendingRequirement(DayDrivenScheduleState state, SkuScheduleDTO sourceSku,
                                                 LocalDate requirementDate) {
        return state.getPendingSkuListInOriginalOrder().stream().anyMatch(candidate ->
                candidate.isContinuousCompensationSku()
                        && StringUtils.equals(candidate.getSourceType(),
                        SkuScheduleSourceTypeEnum.CONTINUATION_ADD_MACHINE.getCode())
                        && StringUtils.equals(candidate.getMaterialCode(), sourceSku.getMaterialCode())
                        && StringUtils.equals(candidate.getProductStatus(), sourceSku.getProductStatus())
                        && requirementDate.equals(candidate.getFirstAddMachineProductionDate())
                        && !state.hasActiveBinding(candidate));
    }

    /**
     * 复制独立增机候选，继承数量上限和原账本身份，不修改来源对象与已有运行视图。
     *
     * @param sourceSku 原机生产SKU
     * @param requirementDate 下一次增机需求日
     * @param committedCount 已落实机台数
     * @param targetCount 需求日原始目标台数
     * @param remainingQty 共享消费账本剩余量
     * @param dayPlanQty 需求日原始计划量
     * @return 等待共享提前生产中心准入的独立候选
     */
    private static SkuScheduleDTO copyExpansionCandidate(SkuScheduleDTO sourceSku, LocalDate requirementDate,
                                                         int committedCount, int targetCount, int remainingQty,
                                                         int dayPlanQty) {
        SkuScheduleDTO candidate = new SkuScheduleDTO();
        BeanUtil.copyProperties(sourceSku, candidate);
        candidate.setScheduleType(ScheduleTypeEnum.NEW_SPEC.getCode());
        candidate.setSourceType(SkuScheduleSourceTypeEnum.CONTINUATION_ADD_MACHINE.getCode());
        candidate.setContinuousCompensationSku(true);
        candidate.setContinuousMachineCode(null);
        candidate.setPreferredContinuousMachineCode(null);
        candidate.setFirstAddMachineProductionDate(requirementDate);
        candidate.setContinuationActiveMachineCount(committedCount);
        candidate.setContinuationRequiredMachineCount(targetCount);
        candidate.setContinuationShortageMachineCount(targetCount - committedCount);
        candidate.setContinuationAddMachineTriggerQty(targetCount - committedCount);
        candidate.setContinuationAddMachineDayPlanQty(dayPlanQty);
        candidate.setPendingQty(remainingQty);
        candidate.setRemainingScheduleQty(remainingQty);
        // 同物料状态实际消费以原日计划Map为身份锚点，不复制或重新初始化已消费量。
        candidate.setDailyPlanQuotaMap(sourceSku.getDailyPlanQuotaMap());
        if (Objects.nonNull(sourceSku.getMouldCodeList())) {
            candidate.setMouldCodeList(new ArrayList<>(sourceSku.getMouldCodeList()));
        }
        return candidate;
    }
}
