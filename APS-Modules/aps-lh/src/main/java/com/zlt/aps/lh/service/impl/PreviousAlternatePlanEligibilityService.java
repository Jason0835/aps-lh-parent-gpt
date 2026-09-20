/**
 * Copyright (c) 2008, 智立通（厦门）科技有限公司 All rights reserved。
 */
package com.zlt.aps.lh.service.impl;

import com.zlt.aps.lh.api.domain.dto.SkuScheduleDTO;
import com.zlt.aps.lh.api.domain.entity.LhMouldChangePlan;
import com.zlt.aps.lh.component.EarlyProductionQuantityCalculator;
import com.zlt.aps.lh.context.LhScheduleContext;
import com.zlt.aps.lh.engine.strategy.support.DailyMachineExpansionPlanner;
import com.zlt.aps.lh.engine.strategy.support.DailyNewSpecCandidate;
import com.zlt.aps.lh.service.ILhDailyMouldCalcService;
import com.zlt.aps.lh.util.LhSingleControlMachineUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
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
 * 前日交替计划资格公共服务。
 *
 * <p>统一最近有效批次、精确机台加前物料关系、后物料排产资格和目标机台缺口口径，
 * 供续作降模排序与后续独立复用阶段共同使用。历史关系只提供释放优先级，
 * 不预占机台、模具、月计划、日计划或生产余量。</p>
 */
@Service
public class PreviousAlternatePlanEligibilityService {

    /** 历史日期只用于排序；同日期采用记录主键保持顺序稳定。 */
    private static final Comparator<LhMouldChangePlan> PLAN_ORDER = Comparator
            .comparing(LhMouldChangePlan::getPlanDate, Comparator.nullsLast(Date::compareTo))
            .thenComparing(LhMouldChangePlan::getId, Comparator.nullsLast(Long::compareTo));

    @Resource
    private NewSpecMaterialEligibilityService materialEligibilityService;
    @Resource
    private ILhDailyMouldCalcService lhDailyMouldCalcService;

    /**
     * 冻结前日最近有效批次的全部交替关系。
     *
     * @param context 排程上下文
     * @return 机台加前物料对应的有序计划
     */
    Map<String, List<LhMouldChangePlan>> buildPlanIndex(LhScheduleContext context) {
        if (Objects.isNull(context)
                || CollectionUtils.isEmpty(context.getHistoricalReverseMouldChangePlanList())) {
            return Collections.emptyMap();
        }
        List<LhMouldChangePlan> source = context.getHistoricalReverseMouldChangePlanList();
        LhMouldChangePlan latest = source.stream()
                .filter(Objects::nonNull)
                .filter(plan -> StringUtils.isNotEmpty(plan.getLhResultBatchNo()))
                .max(Comparator.comparing(LhMouldChangePlan::getCreateTime,
                                Comparator.nullsFirst(Date::compareTo))
                        .thenComparing(LhMouldChangePlan::getId,
                                Comparator.nullsFirst(Long::compareTo)))
                .orElse(null);
        if (Objects.isNull(latest)) {
            return Collections.emptyMap();
        }
        Map<String, List<LhMouldChangePlan>> index =
                new LinkedHashMap<String, List<LhMouldChangePlan>>(source.size());
        source.stream()
                .filter(Objects::nonNull)
                .filter(plan -> StringUtils.equals(
                        latest.getLhResultBatchNo(), plan.getLhResultBatchNo()))
                .filter(plan -> StringUtils.isNotEmpty(plan.getLhMachineCode())
                        && StringUtils.isNotEmpty(plan.getBeforeMaterialCode())
                        && StringUtils.isNotEmpty(plan.getAfterMaterialCode()))
                .sorted(PLAN_ORDER)
                .forEach(plan -> index.computeIfAbsent(
                        this.buildKey(plan.getLhMachineCode(), plan.getBeforeMaterialCode()),
                        ignored -> new ArrayList<LhMouldChangePlan>(2)).add(plan));
        return index;
    }

    /**
     * 获取前日交替计划的稳定排序规则。
     *
     * @return 先按计划日期、再按主键排序的比较器
     */
    Comparator<LhMouldChangePlan> getPlanOrder() {
        return PLAN_ORDER;
    }

    /**
     * 解析当前续作 SKU 中存在有效前日交替后料需求的物理机台。
     *
     * @param context 排程上下文
     * @param sourceSku 当前续作 SKU
     * @param businessDateList 本次排程窗口业务日期
     * @return 应优先释放给前日交替后料的物理机台编码
     */
    public Set<String> resolveEligibleReleasePhysicalMachineCodes(
            LhScheduleContext context,
            SkuScheduleDTO sourceSku,
            List<LocalDate> businessDateList) {
        if (Objects.isNull(context) || Objects.isNull(sourceSku)
                || StringUtils.isEmpty(sourceSku.getMaterialCode())
                || CollectionUtils.isEmpty(businessDateList)
                || CollectionUtils.isEmpty(context.getNewSpecSkuList())) {
            return Collections.emptySet();
        }
        Map<String, List<LhMouldChangePlan>> planIndex = this.buildPlanIndex(context);
        if (CollectionUtils.isEmpty(planIndex)) {
            return Collections.emptySet();
        }
        Set<String> machineCodeSet = new LinkedHashSet<String>(planIndex.size());
        planIndex.values().stream()
                .flatMap(List::stream)
                .filter(plan -> StringUtils.equals(
                        sourceSku.getMaterialCode(), plan.getBeforeMaterialCode()))
                .filter(plan -> !StringUtils.equals(
                        sourceSku.getMaterialCode(), plan.getAfterMaterialCode()))
                .filter(plan -> this.hasEligibleAfterMaterialDemand(
                        context, plan.getAfterMaterialCode(), businessDateList))
                .map(LhMouldChangePlan::getLhMachineCode)
                .map(LhSingleControlMachineUtil::resolvePhysicalMachineCode)
                .filter(StringUtils::isNotEmpty)
                .forEach(machineCodeSet::add);
        return machineCodeSet;
    }

    /**
     * 根据统一目标 Map 和已提交物理机台数刷新候选需求份数。
     *
     * @param context 排程上下文
     * @param businessDate 实际业务日
     * @param candidate 当前新增候选
     */
    void fillMachineDemand(LhScheduleContext context,
                           LocalDate businessDate,
                           DailyNewSpecCandidate candidate) {
        SkuScheduleDTO sku = candidate.getSku();
        LocalDate requiredDate = EarlyProductionQuantityCalculator.resolveRequiredMachineCountDate(
                context, sku, candidate.getEarlyProductionPreview(), businessDate);
        candidate.setTargetMachineCount(this.lhDailyMouldCalcService.getRequiredMachineCount(
                context, sku.getMaterialCode(), sku.getProductStatus(), requiredDate));
        int scheduledMachineCount = DailyMachineExpansionPlanner.countCommittedMachineDemand(
                context, sku, businessDate);
        Set<String> forcedReleasedMachineCodeSet =
                context.resolveForcedReleasedPhysicalMachineCodes(sku);
        if (!CollectionUtils.isEmpty(forcedReleasedMachineCodeSet)) {
            int activeMachineCount = context.getSkuScheduledMachineCountExcluding(
                    businessDate, sku.getMaterialCode(), sku.getProductStatus(),
                    forcedReleasedMachineCodeSet);
            candidate.setTargetMachineCount(Math.max(candidate.getTargetMachineCount(),
                    activeMachineCount + Math.max(1, sku.getContinuationShortageMachineCount())));
        }
        candidate.setScheduledMachineCount(scheduledMachineCount);
    }

    /**
     * 判断历史后物料在窗口内是否仍有可消费的有效机台需求。
     *
     * @param context 排程上下文
     * @param afterMaterialCode 历史后物料编码
     * @param businessDateList 本次排程窗口业务日期
     * @return 是否存在有效资格且目标机台仍有缺口
     */
    private boolean hasEligibleAfterMaterialDemand(LhScheduleContext context,
                                                   String afterMaterialCode,
                                                   List<LocalDate> businessDateList) {
        List<SkuScheduleDTO> candidateSkuList = context.getNewSpecSkuList().stream()
                .filter(Objects::nonNull)
                .filter(sku -> StringUtils.equals(afterMaterialCode, sku.getMaterialCode()))
                .collect(Collectors.toList());
        for (SkuScheduleDTO sku : candidateSkuList) {
            for (LocalDate businessDate : businessDateList) {
                DailyNewSpecCandidate candidate = this.materialEligibilityService.resolveCandidate(
                        context, sku, businessDate);
                if (candidate.getReasons().isEmpty()) {
                    continue;
                }
                this.fillMachineDemand(context, businessDate, candidate);
                if (candidate.getTargetMachineCount() > candidate.getScheduledMachineCount()) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * 构建精确运行态机台与前物料键，不合并左右侧历史关系。
     *
     * @param machineCode 机台编码
     * @param beforeMaterial 前物料
     * @return 历史关系索引键
     */
    String buildKey(String machineCode, String beforeMaterial) {
        return new StringBuilder(64)
                .append(machineCode)
                .append('|')
                .append(beforeMaterial)
                .toString();
    }
}
