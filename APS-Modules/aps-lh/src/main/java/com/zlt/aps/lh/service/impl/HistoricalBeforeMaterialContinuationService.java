package com.zlt.aps.lh.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.zlt.aps.lh.api.domain.dto.MachineScheduleDTO;
import com.zlt.aps.lh.api.domain.dto.SkuScheduleDTO;
import com.zlt.aps.lh.api.domain.entity.LhMachineOnlineInfo;
import com.zlt.aps.lh.api.domain.entity.LhMouldChangePlan;
import com.zlt.aps.lh.api.domain.entity.LhScheduleResult;
import com.zlt.aps.lh.api.domain.vo.LhShiftConfigVO;
import com.zlt.aps.lh.api.enums.MouldChangeTypeEnum;
import com.zlt.aps.lh.api.enums.ScheduleTypeEnum;
import com.zlt.aps.lh.api.enums.ShiftEnum;
import com.zlt.aps.lh.api.enums.SkuTagEnum;
import com.zlt.aps.lh.component.MonthPlanDateResolver;
import com.zlt.aps.lh.component.TargetScheduleQtyResolver;
import com.zlt.aps.lh.context.LhScheduleContext;
import com.zlt.aps.lh.engine.chain.validators.ContinuationOnlineMouldValidator;
import com.zlt.aps.lh.engine.strategy.support.ContinuationSkuMatcher;
import com.zlt.aps.lh.service.ILhDailyMouldCalcService;
import com.zlt.aps.lh.util.LhMouldCodeUtil;
import com.zlt.aps.lh.util.LhScheduleTimeUtil;
import com.zlt.aps.lh.util.LhSingleControlMachineUtil;
import com.zlt.aps.lh.util.PriorityTraceLogHelper;
import com.zlt.aps.lh.util.ShiftCapacityResolverUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Resource;

/**
 * T日历史交替前料续作来源服务，在保留MES快照的同时恢复原机精确收尾身份。
 *
 * @author Codex
 * @date 2026/10/25
 */
@Slf4j
@Service
public class HistoricalBeforeMaterialContinuationService {

    /**
     * 沿用最新有效历史批次及动作日期资格。
     */
    @Resource
    private PreviousAlternatePlanEligibilityService previousAlternatePlanEligibilityService;

    /**
     * 统一判断库存硬目标和真实剩余需求。
     */
    @Resource
    private TargetScheduleQtyResolver targetScheduleQtyResolver;

    /**
     * 读取月计划按业务日期计算的统一目标物理机台数。
     */
    @Resource
    private ILhDailyMouldCalcService lhDailyMouldCalcService;

    /**
     * 复用正式续作的模具关系校验。
     */
    @Resource
    private ContinuationOnlineMouldValidator continuationOnlineMouldValidator;

    /**
     * 在SKU准入过滤前保存候选来源，仅接受T日首次异料动作及前批可证明的完整装机组。
     *
     * @param context 已加载前批结果、历史计划及MES快照的上下文
     */
    public void prepareCandidates(LhScheduleContext context) {
        context.getHistoricalBeforeMaterialCandidateMap().clear();
        context.getHistoricalBeforeMaterialContinuationMap().clear();
        Map<String, LhMouldChangePlan> firstPlans = new LinkedHashMap<>();
        previousAlternatePlanEligibilityService.buildPlanIndex(context).values().stream()
                .flatMap(List::stream).sorted(PreviousAlternatePlanEligibilityService.planOrder())
                .forEach(plan -> firstPlans.putIfAbsent(plan.getLhMachineCode(), plan));
        firstPlans.values().stream().filter(plan -> this.isCandidatePlan(context, plan))
                .forEach(plan -> this.prepareCandidate(context, plan));
    }

    /**
     * 限定为本窗口T日、早中班01/02异料动作，MES必须精确命中历史后料状态。
     *
     * @param context 当前窗口和MES来源
     * @param plan 本机首次有效历史动作
     * @return 满足精确场景时返回true
     */
    private boolean isCandidatePlan(LhScheduleContext context, LhMouldChangePlan plan) {
        LhMachineOnlineInfo online = context.getMachineOnlineInfoMap().get(plan.getLhMachineCode());
        return Objects.nonNull(context.getScheduleDate()) && Objects.nonNull(plan.getPlanDate())
                && LhScheduleTimeUtil.isSameDay(context.getScheduleDate(), plan.getPlanDate())
                && (ShiftEnum.MORNING_SHIFT.getCode().equals(plan.getClassIndex())
                || ShiftEnum.AFTERNOON_SHIFT.getCode().equals(plan.getClassIndex()))
                && MouldChangeTypeEnum.containsAnyCode(plan.getChangeMouldType(),
                MouldChangeTypeEnum.REGULAR.getCode(), MouldChangeTypeEnum.TYPE_BLOCK.getCode())
                && !StringUtils.equals(plan.getBeforeMaterialCode(), plan.getAfterMaterialCode())
                && Objects.nonNull(online)
                && StringUtils.equals(plan.getAfterMaterialCode(), online.getMaterialCode())
                && StringUtils.equals(plan.getProductStatus(), this.normalizeStatus(online.getProductStatus()))
                && context.getMachineScheduleMap().containsKey(plan.getLhMachineCode());
    }

    /**
     * 用同机前批结果证明前料状态和模具，歧义或模具已归属其他机台时不推测生产身份。
     *
     * @param context 当前批次输入
     * @param plan 已通过日期及后料身份判断的计划
     */
    private void prepareCandidate(LhScheduleContext context, LhMouldChangePlan plan) {
        List<LhScheduleResult> sources = context.getPreviousScheduleResultList().stream()
                .filter(Objects::nonNull)
                .filter(result -> StringUtils.equals(plan.getLhMachineCode(), result.getLhMachineCode())
                        && StringUtils.equals(plan.getBeforeMaterialCode(), result.getMaterialCode()))
                .collect(Collectors.toList());
        if (sources.isEmpty()) {
            return;
        }
        LhScheduleResult source = sources.get(0);
        Set<String> moulds = LhMouldCodeUtil.splitMouldCode(source.getMouldCode());
        int requiredMouldQty = ShiftCapacityResolverUtil.resolveMachineMouldQty(
                context.getMachineScheduleMap().get(plan.getLhMachineCode()));
        boolean ambiguous = sources.stream().anyMatch(result ->
                !StringUtils.equals(this.normalizeStatus(source.getProductStatus()),
                        this.normalizeStatus(result.getProductStatus()))
                        || !moulds.equals(LhMouldCodeUtil.splitMouldCode(result.getMouldCode())));
        boolean occupiedElsewhere = context.getMachineOnlineInfoMap().entrySet().stream()
                .filter(entry -> !StringUtils.equals(plan.getLhMachineCode(), entry.getKey()))
                .filter(entry -> Objects.nonNull(entry.getValue()))
                .anyMatch(entry -> !Collections.disjoint(moulds,
                        LhMouldCodeUtil.splitMouldCode(entry.getValue().getInMachineMouldCode())));
        if (ambiguous || moulds.size() != requiredMouldQty || occupiedElsewhere) {
            log.info("历史前料续作来源未确认, batchNo: {}, planId: {}, machineCode: {}, beforeMaterial: {}, "
                            + "ambiguous: {}, moulds: {}, requiredMouldQty: {}, occupiedElsewhere: {}",
                    context.getBatchNo(), plan.getId(), plan.getLhMachineCode(), plan.getBeforeMaterialCode(),
                    ambiguous, moulds, requiredMouldQty, occupiedElsewhere);
            return;
        }
        LhMachineOnlineInfo candidate = BeanUtil.copyProperties(
                context.getMachineOnlineInfoMap().get(plan.getLhMachineCode()), LhMachineOnlineInfo.class);
        candidate.setMaterialCode(source.getMaterialCode());
        candidate.setProductStatus(this.normalizeStatus(source.getProductStatus()));
        candidate.setSpecDesc(source.getMaterialDesc());
        candidate.setInMachineMouldCode(LhMouldCodeUtil.joinMouldCode(moulds));
        context.getHistoricalBeforeMaterialCandidateMap().put(plan.getLhMachineCode(), candidate);
    }

    /**
     * 对选中的历史前料一次性恢复生产身份，正式续作分类随后统一建立共享账本副本。
     *
     * @param context 已归集SKU且尚未分配续作机台的上下文
     */
    public void activate(LhScheduleContext context) {
        // 已提交的恢复身份不得重复选择，避免重入时补回库存或改变既定机台集合。
        if (!context.getHistoricalBeforeMaterialContinuationMap().isEmpty()) {
            return;
        }
        Map<String, LhMachineOnlineInfo> selected = this.selectCandidates(context);
        Map<String, List<SkuScheduleDTO>> skuIndex = this.buildSkuIndex(context);
        selected.forEach((machineCode, candidate) -> {
            SkuScheduleDTO sku = skuIndex.get(this.buildSkuKey(candidate)).get(0);
            context.getHistoricalBeforeMaterialContinuationMap().put(machineCode, candidate);
            this.applyProductionIdentity(context.getMachineScheduleMap().get(machineCode), candidate, sku);
            this.applyProductionIdentity(context.getInitialMachineScheduleMap().get(machineCode), candidate, sku);
            // 只确认来源，余量、日计划及胎胚库存交由原续作中心账本消费一次。
            sku.setSkuTag(SkuTagEnum.ENDING.getCode());
            sku.setEndingDaysRemaining(1);
            String detail = String.format("工厂=%s, 批次=%s, T日=%s, 机台=%s, 前料=%s, 状态=%s, "
                            + "原始余量=%d, 胎胚库存=%d, 前料模具=%s, 处理=前料先续作收尾后指定承接",
                    context.getFactoryCode(), context.getBatchNo(),
                    LhScheduleTimeUtil.formatDate(context.getScheduleDate()),
                    machineCode, candidate.getMaterialCode(), candidate.getProductStatus(), sku.getSurplusQty(),
                    sku.getEmbryoStock(), candidate.getInMachineMouldCode());
            PriorityTraceLogHelper.appendProcessLog(context, "历史前料优先续作收尾", detail);
            log.info("历史前料优先续作收尾, {}", detail);
        });
        if (!selected.isEmpty()) {
            // 资源尚未分配，旧MES后料模具缓存须按确认后的生产身份重新建立。
            context.setMouldResourceContext(null);
        }
    }

    /**
     * 冻结原续作列表后按前料状态分组选机，仅返回决策，不修改生产身份和数量账本。
     *
     * @param context 原MES、已证明历史来源和月计划目标上下文
     * @return 按历史顺序选择的运行侧及前料身份；L/R按同一物理机台计数
     */
    public Map<String, LhMachineOnlineInfo> selectCandidates(LhScheduleContext context) {
        Set<String> originalContinuousKeys = ContinuationSkuMatcher.resolve(
                context, context.getMachineOnlineInfoMap()::get).values().stream()
                .map(sku -> MonthPlanDateResolver.buildMaterialStatusKey(sku.getMaterialCode(),
                        this.normalizeStatus(sku.getProductStatus())))
                .collect(Collectors.toCollection(LinkedHashSet::new));
        context.getContinuousSkuList().stream().filter(Objects::nonNull)
                .map(sku -> MonthPlanDateResolver.buildMaterialStatusKey(sku.getMaterialCode(),
                        this.normalizeStatus(sku.getProductStatus())))
                .forEach(originalContinuousKeys::add);
        Map<String, List<SkuScheduleDTO>> skuIndex = this.buildSkuIndex(context);
        Map<String, List<Map.Entry<String, LhMachineOnlineInfo>>> groups = context
                .getHistoricalBeforeMaterialCandidateMap().entrySet().stream()
                .collect(Collectors.groupingBy(entry -> this.buildSkuKey(entry.getValue()),
                        LinkedHashMap::new, Collectors.toList()));
        Map<String, LhMachineOnlineInfo> selected = new LinkedHashMap<>();
        Set<String> selectedPhysicalMachines = new LinkedHashSet<>();
        Set<String> selectedMoulds = new LinkedHashSet<>();
        groups.forEach((skuKey, candidates) -> {
            List<SkuScheduleDTO> matches = skuIndex.getOrDefault(skuKey, Collections.emptyList());
            if (matches.size() != 1 || originalContinuousKeys.contains(skuKey)) {
                this.traceSelection(context, skuKey, -1, Collections.emptySet(),
                        originalContinuousKeys.contains(skuKey) ? "前料已在原续作列表，不额外切换" : "前料SKU来源不唯一");
                return;
            }
            SkuScheduleDTO sku = matches.get(0);
            if (!this.hasContinuationDemand(context, sku)) {
                this.traceSelection(context, skuKey, -1, Collections.emptySet(), "无硫化余量且无胎胚收尾库存");
                return;
            }
            int targetCount = this.resolveTargetMachineCount(context, sku);
            if (targetCount < 0) {
                this.traceSelection(context, skuKey, targetCount, Collections.emptySet(), "T日月计划机台数维度缺失");
                return;
            }
            Map<String, List<Map.Entry<String, LhMachineOnlineInfo>>> physicalGroups = candidates.stream()
                    .collect(Collectors.groupingBy(entry ->
                            LhSingleControlMachineUtil.resolvePhysicalMachineCode(entry.getKey()),
                            LinkedHashMap::new, Collectors.toList()));
            Set<String> groupSelected = new LinkedHashSet<>();
            for (Map.Entry<String, List<Map.Entry<String, LhMachineOnlineInfo>>> physical : physicalGroups.entrySet()) {
                if (groupSelected.size() >= targetCount) {
                    break;
                }
                Set<String> moulds = physical.getValue().stream()
                        .flatMap(entry -> LhMouldCodeUtil.splitMouldCode(
                                entry.getValue().getInMachineMouldCode()).stream())
                        .collect(Collectors.toCollection(LinkedHashSet::new));
                int mouldCount = physical.getValue().stream().mapToInt(entry ->
                        LhMouldCodeUtil.splitMouldCode(entry.getValue().getInMachineMouldCode()).size()).sum();
                if (selectedPhysicalMachines.contains(physical.getKey())
                        || !Collections.disjoint(moulds, selectedMoulds) || moulds.size() != mouldCount
                        || physical.getValue().stream().anyMatch(entry ->
                        !this.isAvailableCandidate(context, sku, entry.getKey(), entry.getValue()))) {
                    continue;
                }
                physical.getValue().forEach(entry -> selected.put(entry.getKey(), entry.getValue()));
                groupSelected.add(physical.getKey());
                selectedPhysicalMachines.add(physical.getKey());
                selectedMoulds.addAll(moulds);
            }
            this.traceSelection(context, skuKey, targetCount, groupSelected,
                    groupSelected.size() < targetCount ? "合法候选不足，按可用机台恢复" : "按月计划目标恢复前料续作");
        });
        return selected;
    }

    /**
     * 按精确物料状态索引前料，不以历史后料状态或跨状态合计替代真实需求。
     *
     * @param context 已归集SKU的上下文
     * @return 精确SKU键及来源列表，保留歧义供准入拒绝
     */
    private Map<String, List<SkuScheduleDTO>> buildSkuIndex(LhScheduleContext context) {
        return context.getStructureSkuMap().values().stream().flatMap(List::stream)
                .filter(Objects::nonNull).collect(Collectors.groupingBy(sku ->
                        MonthPlanDateResolver.buildMaterialStatusKey(sku.getMaterialCode(),
                                this.normalizeStatus(sku.getProductStatus())),
                        LinkedHashMap::new, Collectors.toList()));
    }

    /**
     * 判断前料是否有本次续作需求；共用胎胚资格和额度仍由原续作准入及分摊流程裁定。
     *
     * @param context 胎胚收尾标识上下文
     * @param sku 前料原始需求
     * @return 原始硫化余量为正，或胎胚已收尾且库存为正时返回true
     */
    private boolean hasContinuationDemand(LhScheduleContext context, SkuScheduleDTO sku) {
        return sku.getSurplusQty() > 0 || (sku.getEmbryoStock() > 0
                && targetScheduleQtyResolver.isEmbryoStockEndingFlagYes(context, sku.getEmbryoCode()));
    }

    /**
     * 使用T日统一月计划目标计算需恢复的物理机台数，配置0或1均恢复1台。
     *
     * @param context 本次窗口及统一目标Map
     * @param sku 前料及产品状态
     * @return 目标物理机台数，维度缺失返回-1而不是配置0
     */
    private int resolveTargetMachineCount(LhScheduleContext context, SkuScheduleDTO sku) {
        LocalDate productionDate = context.getScheduleDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        if (!lhDailyMouldCalcService.hasRequiredMachineCount(
                context, sku.getMaterialCode(), sku.getProductStatus(), productionDate)) {
            return -1;
        }
        return Math.max(1, lhDailyMouldCalcService.getRequiredMachineCount(
                context, sku.getMaterialCode(), sku.getProductStatus(), productionDate));
    }

    /**
     * 复用正式续作的模具关系与设备产能约束，试算对象不写入本轮生产身份。
     *
     * @param context 已加载设备及班次的上下文
     * @param sku 前料需求
     * @param machineCode 候选运行侧
     * @param candidate 已证明的前料来源
     * @return 模具合法且本窗口存在真实续作产能时返回true
     */
    private boolean isAvailableCandidate(LhScheduleContext context, SkuScheduleDTO sku,
            String machineCode, LhMachineOnlineInfo candidate) {
        MachineScheduleDTO original = context.getMachineScheduleMap().get(machineCode);
        if (Objects.isNull(original) || !continuationOnlineMouldValidator.isMouldGroupAllowed(
                context, sku.getMaterialCode(), LhMouldCodeUtil.splitMouldCode(candidate.getInMachineMouldCode()))) {
            return false;
        }
        List<LhShiftConfigVO> shifts = context.getScheduleWindowShifts();
        Date start = shifts.stream().map(LhShiftConfigVO::getShiftStartDateTime)
                .filter(Objects::nonNull).min(Date::compareTo).orElse(null);
        if (Objects.isNull(start)) {
            return false;
        }
        MachineScheduleDTO previewMachine = BeanUtil.copyProperties(original, MachineScheduleDTO.class);
        SkuScheduleDTO previewSku = BeanUtil.copyProperties(sku, SkuScheduleDTO.class);
        previewSku.setContinuousMachineCode(machineCode);
        this.applyProductionIdentity(previewMachine, candidate, previewSku);
        return targetScheduleQtyResolver.calcMachineAvailableCapacityByStartTime(context, previewSku,
                previewMachine, null, start, shifts, ScheduleTypeEnum.CONTINUOUS.getCode()) > 0;
    }

    /**
     * 构造历史前料身份键。
     *
     * @param candidate 已证明的前料来源
     * @return 物料和归一化产品状态复合键
     */
    private String buildSkuKey(LhMachineOnlineInfo candidate) {
        return MonthPlanDateResolver.buildMaterialStatusKey(candidate.getMaterialCode(),
                this.normalizeStatus(candidate.getProductStatus()));
    }

    /**
     * 记录触发、目标及实际选机，供本批次结果和未覆盖原因对账。
     *
     * @param context 工厂、批次及窗口信息
     * @param skuKey 前料物料状态键
     * @param targetCount 目标台数，未计算时为-1
     * @param selectedMachines 选中的物理机台
     * @param reason 本次选择或跳过原因
     */
    private void traceSelection(LhScheduleContext context, String skuKey, int targetCount,
            Set<String> selectedMachines, String reason) {
        String detail = String.format("工厂=%s, 批次=%s, T日=%s, 前料SKU=%s, "
                        + "目标台数=%d, 实选台数=%d, 机台=%s, 原因=%s",
                context.getFactoryCode(), context.getBatchNo(), LhScheduleTimeUtil.formatDate(context.getScheduleDate()),
                skuKey, targetCount, selectedMachines.size(), selectedMachines, reason);
        PriorityTraceLogHelper.appendProcessLog(context, "历史前料续作选机", detail);
        log.info("历史前料续作选机, {}", detail);
    }

    /**
     * 更新本次运行机台的前料身份和选机属性，保留设备窗口及原始MES来源。
     *
     * @param machine 本次机台或恢复快照
     * @param candidate 已确认前料来源
     * @param sku 前料SKU
     */
    private void applyProductionIdentity(MachineScheduleDTO machine,
            LhMachineOnlineInfo candidate, SkuScheduleDTO sku) {
        if (Objects.isNull(machine)) {
            return;
        }
        machine.setCurrentMaterialCode(candidate.getMaterialCode());
        machine.setCurrentMaterialDesc(candidate.getSpecDesc());
        machine.setPreviousSpecCode(sku.getSpecCode());
        machine.setPreviousProSize(sku.getProSize());
    }

    /**
     * 按MES续作口径归一化空状态为正规S。
     *
     * @param status 原产品状态
     * @return 非空产品状态
     */
    private String normalizeStatus(String status) {
        return ContinuationSkuMatcher.normalizeStatus(status);
    }
}
