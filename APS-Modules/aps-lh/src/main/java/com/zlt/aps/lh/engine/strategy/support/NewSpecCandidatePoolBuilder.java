package com.zlt.aps.lh.engine.strategy.support;

import com.zlt.aps.lh.api.domain.dto.SkuScheduleDTO;
import com.zlt.aps.lh.component.EarlyProductionQuantityCalculator;
import com.zlt.aps.lh.context.LhScheduleContext;
import com.zlt.aps.lh.engine.strategy.ISkuPriorityStrategy;
import com.zlt.aps.lh.service.ILhDailyMouldCalcService;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;

/**
 * S4.5 新增排产日期候选池构建器。
 *
 * <p>日期池只保存 SKU 引用、原始归属日期和剩余物理机台机会，不复制 dayN、
 * 胎胚、模具或目标量账本。池内排序唯一复用 {@link ISkuPriorityStrategy#sortNewSpecByPriority}。</p>
 *
 * @author APS
 */
@Component
public class NewSpecCandidatePoolBuilder {

    /** SKU 日期池内排序唯一入口 */
    @Resource
    private ISkuPriorityStrategy skuPriorityStrategy;
    /** 当前业务日目标机台数唯一入口 */
    @Resource
    private ILhDailyMouldCalcService lhDailyMouldCalcService;

    /**
     * 构建并排序当前阶段的日期候选池。
     *
     * @param context 排程上下文
     * @param windowStartDate T 日
     * @param currentDate 当前竞争业务日
     * @param phase 当前阶段
     * @param candidateList 当前阶段已通过业务准入的候选
     * @return 日期升序的候选池
     */
    public Map<LocalDate, List<DailyNewSpecCandidate>> buildOrderedPools(
            LhScheduleContext context,
            LocalDate windowStartDate,
            LocalDate currentDate,
            DailySchedulePhase phase,
            List<DailyNewSpecCandidate> candidateList) {
        if (Objects.isNull(context) || Objects.isNull(windowStartDate)
                || Objects.isNull(currentDate) || CollectionUtils.isEmpty(candidateList)) {
            return Collections.emptyMap();
        }
        List<SkuScheduleDTO> skuList = new ArrayList<SkuScheduleDTO>(candidateList.size());
        for (DailyNewSpecCandidate candidate : candidateList) {
            if (Objects.nonNull(candidate) && Objects.nonNull(candidate.getSku())) {
                skuList.add(candidate.getSku());
            }
        }
        boolean requiresSpecialSkuClassification = candidateList.stream()
                .filter(Objects::nonNull)
                .anyMatch(candidate -> !candidate.isSpecialSkuClassified());
        Set<SkuScheduleDTO> specialSkuSet = requiresSpecialSkuClassification
                ? skuPriorityStrategy.resolveSpecialNewSpecSkus(context, skuList)
                : Collections.<SkuScheduleDTO>emptySet();
        Map<LocalDate, List<DailyNewSpecCandidate>> poolMap =
                new TreeMap<LocalDate, List<DailyNewSpecCandidate>>();
        for (DailyNewSpecCandidate candidate : candidateList) {
            if (Objects.isNull(candidate) || Objects.isNull(candidate.getSku())) {
                continue;
            }
            LocalDate poolDate = this.resolvePoolDate(
                    windowStartDate, phase, candidate);
            candidate.setPoolDate(poolDate);
            if (!candidate.isSpecialSkuClassified()) {
                candidate.setSpecialSku(specialSkuSet.contains(candidate.getSku()));
            }
            this.refreshRemainingMachineCount(context, currentDate, candidate);
            poolMap.computeIfAbsent(poolDate,
                    key -> new ArrayList<DailyNewSpecCandidate>(4)).add(candidate);
        }
        for (List<DailyNewSpecCandidate> poolCandidates : poolMap.values()) {
            this.sortPoolCandidates(context, poolCandidates);
        }
        return new LinkedHashMap<LocalDate, List<DailyNewSpecCandidate>>(poolMap);
    }

    /**
     * 按当前正式结果刷新候选剩余物理机台机会。
     *
     * @param context 排程上下文
     * @param currentDate 当前竞争业务日
     * @param candidate 日期池候选
     */
    public void refreshRemainingMachineCount(LhScheduleContext context,
                                             LocalDate currentDate,
                                             DailyNewSpecCandidate candidate) {
        if (Objects.isNull(context) || Objects.isNull(currentDate)
                || Objects.isNull(candidate) || Objects.isNull(candidate.getSku())) {
            return;
        }
        SkuScheduleDTO sku = candidate.getSku();
        LocalDate requiredMachineCountDate =
                EarlyProductionQuantityCalculator.resolveRequiredMachineCountDate(
                        context, sku, candidate.getEarlyProductionPreview(), currentDate);
        int requiredMachineCount = lhDailyMouldCalcService.getRequiredMachineCount(
                context, sku.getMaterialCode(), sku.getProductStatus(),
                requiredMachineCountDate);
        if (sku.getContinuationShortageMachineCount() > 0) {
            requiredMachineCount = Math.max(
                    requiredMachineCount,
                    sku.getContinuationActiveMachineCount()
                            + sku.getContinuationShortageMachineCount());
        }
        int scheduledMachineCount = context.getSkuScheduledMachineCount(
                currentDate, sku.getMaterialCode(), sku.getProductStatus());
        if (candidate.isStrictEndingClearance()
                && sku.getRemainingScheduleQty() > 0) {
            /*
             * 严格收尾已由新增主链确认按真实余量清量。跨业务日重建候选池时至少
             * 保留下一台真实尝试机会，不能再次被统一Map已满足的计算结果归零。
             */
            requiredMachineCount = Math.max(
                    requiredMachineCount, scheduledMachineCount + 1);
        }
        if (requiredMachineCount <= 0) {
            // 收尾、固定指令等既有合法场景没有 dayN 理论台数时，仍保留一次真实尝试机会。
            requiredMachineCount = 1;
        }
        candidate.reconcileRemainingMachineCount(
                requiredMachineCount, scheduledMachineCount, currentDate);
    }

    private LocalDate resolvePoolDate(LocalDate windowStartDate,
                                      DailySchedulePhase phase,
                                      DailyNewSpecCandidate candidate) {
        if (Objects.nonNull(candidate.getPoolDate())) {
            // 已在前序业务日进入候选池的SKU必须保留原日期；部分成功和跨日在机不得改写来源池。
            return candidate.getPoolDate();
        }
        if (Objects.nonNull(candidate.getTargetPlanDate())) {
            /*
             * targetPlanDate 是候选首次归属日期：正常阶段的历史延期保留 deferredFromDate，
             * 提前阶段保留 futurePlanDate。只有尚未形成明确来源时才由 delayDays 推导。
             */
            return candidate.getTargetPlanDate();
        }
        Integer delayDays = candidate.getSku().getDelayDays();
        int normalizedDelayDays = Objects.isNull(delayDays) ? 0 : Math.max(0, delayDays);
        return windowStartDate.plusDays(normalizedDelayDays);
    }

    private void sortPoolCandidates(LhScheduleContext context,
                                    List<DailyNewSpecCandidate> poolCandidates) {
        if (CollectionUtils.isEmpty(poolCandidates)) {
            return;
        }
        List<SkuScheduleDTO> orderedSkuList = new ArrayList<SkuScheduleDTO>(poolCandidates.size());
        Map<SkuScheduleDTO, DailyNewSpecCandidate> candidateMap =
                new IdentityHashMap<SkuScheduleDTO, DailyNewSpecCandidate>(poolCandidates.size());
        for (DailyNewSpecCandidate candidate : poolCandidates) {
            orderedSkuList.add(candidate.getSku());
            candidateMap.put(candidate.getSku(), candidate);
        }
        skuPriorityStrategy.sortNewSpecPoolByPriority(context, orderedSkuList);
        poolCandidates.clear();
        for (SkuScheduleDTO sku : orderedSkuList) {
            DailyNewSpecCandidate candidate = candidateMap.get(sku);
            if (Objects.nonNull(candidate)) {
                poolCandidates.add(candidate);
            }
        }
    }
}
