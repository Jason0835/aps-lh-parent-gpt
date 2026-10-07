package com.zlt.aps.lh.handler;

import com.zlt.aps.lh.api.domain.dto.SkuScheduleDTO;
import com.zlt.aps.lh.api.domain.entity.LhScheduleResult;
import com.zlt.aps.lh.api.domain.entity.LhUnscheduledResult;
import com.zlt.aps.lh.api.enums.ScheduleStepEnum;
import com.zlt.aps.lh.api.enums.ScheduleTypeEnum;
import com.zlt.aps.lh.component.DayPlanAdjustRequireAssembler;
import com.zlt.aps.lh.component.MonthPlanDateResolver;
import com.zlt.aps.lh.component.TargetScheduleQtyResolver;
import com.zlt.aps.lh.context.LhScheduleContext;
import com.zlt.aps.lh.engine.factory.ScheduleStrategyFactory;
import com.zlt.aps.lh.engine.strategy.ICapacityCalculateStrategy;
import com.zlt.aps.lh.engine.strategy.IFirstInspectionBalanceStrategy;
import com.zlt.aps.lh.engine.strategy.IMachineMatchStrategy;
import com.zlt.aps.lh.engine.strategy.IMouldChangeBalanceStrategy;
import com.zlt.aps.lh.engine.strategy.IProductionStrategy;
import com.zlt.aps.lh.util.LhScheduleTimeUtil;
import com.zlt.aps.lh.util.ShiftFieldUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * S4.5.2 硫化日计划调整排产处理器。
 *
 * <p>该步骤严格在 S4.5 新增排产全部完成后执行：先由 {@link DayPlanAdjustRequireAssembler}
 * 复用历史承接前已准备的“本月没有正向原始月计划量、但日计划调整需求存在”物料及剩余账本，再复用新增排产
 * {@code scheduleNewSpecs} 的选机、换模、首检、产能、结果落库与账本扣减主链完成排产。
 * 本 Handler 不修改新增排产既有业务语义，仅通过临时替换待排列表实现独立入口。</p>
 *
 * @author APS
 */
@Slf4j
@Component
public class DayPlanAdjustProductionHandler extends AbsScheduleStepHandler {

    @Resource
    private ScheduleStrategyFactory strategyFactory;

    @Resource
    private DayPlanAdjustRequireAssembler dayPlanAdjustRequireAssembler;

    @Resource
    private TargetScheduleQtyResolver targetScheduleQtyResolver;

    @Override
    protected void doHandle(LhScheduleContext context) {
        log.info("硫化日计划调整排产处理开始, 工厂: {}, 目标日: {}, 当前结果数: {}, 未排产数: {}",
                context.getFactoryCode(), LhScheduleTimeUtil.formatDate(context.getScheduleTargetDate()),
                context.getScheduleResultList().size(), context.getUnscheduledResultList().size());

        List<SkuScheduleDTO> dayPlanAdjustSkuList = dayPlanAdjustRequireAssembler.prepare(context);
        if (CollectionUtils.isEmpty(dayPlanAdjustSkuList)) {
            log.info("硫化日计划调整待排清单为空, 工厂: {}, 批次: {}",
                    context.getFactoryCode(), context.getBatchNo());
            return;
        }

        // 只消费指定承接后的真实余额；原始严格目标和共享日计划账本保持不变。
        List<SkuScheduleDTO> pendingSkuList = dayPlanAdjustSkuList.stream()
                .filter(sku -> targetScheduleQtyResolver.resolveProductionRemainingQty(context, sku) > 0)
                .collect(Collectors.toList());
        pendingSkuList.forEach(sku -> sku.setRemainingScheduleQty(
                targetScheduleQtyResolver.resolveProductionRemainingQty(context, sku)));
        if (CollectionUtils.isEmpty(pendingSkuList)) {
            this.logDayPlanAdjustSummary(context, dayPlanAdjustSkuList);
            return;
        }

        IProductionStrategy strategy = strategyFactory.getProductionStrategy(
                ScheduleTypeEnum.NEW_SPEC.getCode());
        IMachineMatchStrategy machineMatchStrategy = strategyFactory.getMachineMatchStrategy();
        IMouldChangeBalanceStrategy mouldChangeStrategy = strategyFactory.getMouldChangeBalanceStrategy();
        IFirstInspectionBalanceStrategy inspectionStrategy = strategyFactory.getFirstInspectionBalanceStrategy();
        ICapacityCalculateStrategy capacityStrategy = strategyFactory.getCapacityCalculateStrategy();

        // 保存 S4.5 后仍保留的待排新增列表，日计划调整阶段仅临时消费独立候选，完成后原样恢复。
        List<SkuScheduleDTO> originalNewSpecSkuList = new ArrayList<>(context.getNewSpecSkuList());
        try {
            context.setNewSpecSkuList(new ArrayList<>(pendingSkuList));
            strategy.scheduleNewSpecs(context, machineMatchStrategy, mouldChangeStrategy,
                    inspectionStrategy, capacityStrategy);
        } finally {
            context.setNewSpecSkuList(originalNewSpecSkuList);
        }

        this.logDayPlanAdjustSummary(context, dayPlanAdjustSkuList);
        log.info("硫化日计划调整排产处理完成, 工厂: {}, 结果数: {}, 未排产数: {}",
                context.getFactoryCode(), context.getScheduleResultList().size(),
                context.getUnscheduledResultList().size());
    }

    /**
     * 输出日计划调整阶段的可对账汇总日志。
     *
     * <p>逐物料记录“物料、产品状态、汇总调整量、硫化余量、实际排产量、所选机台及未排原因”，
     * 其中汇总调整量与硫化余量取自候选 DTO，实际排产量与机台合并本批历史承接及后置结果。</p>
     *
     * @param context            排程上下文
     * @param dayPlanAdjustSkuList 日计划调整候选列表
     */
    private void logDayPlanAdjustSummary(LhScheduleContext context,
                                        List<SkuScheduleDTO> dayPlanAdjustSkuList) {
        if (CollectionUtils.isEmpty(dayPlanAdjustSkuList)) {
            return;
        }
        List<LhScheduleResult> batchResultList = new ArrayList<>(context.getScheduleResultList());
        for (SkuScheduleDTO sku : dayPlanAdjustSkuList) {
            int scheduledQty = 0;
            Set<String> machineCodeSet = new LinkedHashSet<>();
            for (LhScheduleResult result : batchResultList) {
                if (Objects.isNull(result) || !Objects.equals(sku.getMaterialCode(), result.getMaterialCode())
                        || !Objects.equals(sku.getProductStatus(), result.getProductStatus())) {
                    continue;
                }
                scheduledQty += ShiftFieldUtil.resolveScheduledQty(result);
                if (Objects.nonNull(result.getLhMachineCode())) {
                    machineCodeSet.add(result.getLhMachineCode());
                }
            }
            String unscheduledReason = this.resolveUnscheduledReason(context, sku);
            log.info("硫化日计划调整排产结果, 工厂: {}, 物料: {}, 产品状态: {}, 汇总调整量: {}, "
                            + "硫化余量: {}, 实际排产量: {}, 所选机台: {}, 未排原因: {}",
                    context.getFactoryCode(), sku.getMaterialCode(), sku.getProductStatus(),
                    sku.getWindowPlanQty(), sku.getSurplusQty(), scheduledQty,
                    machineCodeSet.isEmpty() ? "" : String.join(",", machineCodeSet),
                    unscheduledReason);
        }
    }

    /**
     * 反查日计划调整物料对应的未排原因。
     *
     * @param context 排程上下文
     * @param sku     日计划调整候选 SKU
     * @return 未排原因，未命中返回空字符串
     */
    private String resolveUnscheduledReason(LhScheduleContext context, SkuScheduleDTO sku) {
        if (CollectionUtils.isEmpty(context.getUnscheduledResultList())) {
            return "";
        }
        String materialStatusKey = MonthPlanDateResolver.buildMaterialStatusKey(
                sku.getMaterialCode(), sku.getProductStatus());
        for (LhUnscheduledResult unscheduled : context.getUnscheduledResultList()) {
            if (Objects.isNull(unscheduled)) {
                continue;
            }
            String unscheduledKey = MonthPlanDateResolver.buildMaterialStatusKey(
                    unscheduled.getMaterialCode(), unscheduled.getProductStatus());
            if (Objects.equals(materialStatusKey, unscheduledKey)
                    && Objects.nonNull(unscheduled.getUnscheduledReason())) {
                return unscheduled.getUnscheduledReason();
            }
        }
        return "";
    }

    @Override
    protected String getStepName() {
        return ScheduleStepEnum.S4_5_2_DAY_PLAN_ADJUST.getDescription();
    }
}
