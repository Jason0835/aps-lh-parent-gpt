package com.zlt.aps.lh.engine.strategy.support;

import com.zlt.aps.lh.api.domain.dto.SkuScheduleDTO;
import com.zlt.aps.lh.api.enums.ScheduleStepEnum;
import com.zlt.aps.lh.context.LhScheduleContext;
import com.zlt.aps.lh.util.LhSingleControlMachineUtil;
import com.zlt.aps.mdm.api.domain.entity.MdmMaterialInfo;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.ToIntFunction;
import java.util.stream.Collectors;

/**
 * K21/K22下机顺序与普通新增原胎胚优先策略，只读取业务快照。
 *
 * @author APS
 * @date 2026/11/06
 */
public final class K21K22EmbryoMatchPolicy {

    /**
     * 本批适用机台的初始化胎胚，历史指定机台不进入映射。
     */
    private final Map<String, String> originalEmbryos = new LinkedHashMap<>();

    /**
     * 一次解析本批原在机胎胚与历史排除范围，供逐机台逐SKU常数时间查询。
     *
     * @param context 已初始化的排程上下文
     */
    public K21K22EmbryoMatchPolicy(LhScheduleContext context) {
        Set<String> historicalMachines = context.getEligiblePreviousAlternatePlans().stream()
                .map(plan -> LhSingleControlMachineUtil.resolvePhysicalMachineCode(plan.getLhMachineCode()))
                .collect(Collectors.toSet());
        context.getInitialMachineScheduleMap().forEach((machineCode, machine) -> {
            if (isTargetMachine(machineCode) && !historicalMachines.contains(
                    LhSingleControlMachineUtil.resolvePhysicalMachineCode(machineCode))) {
                MdmMaterialInfo material = context.getMaterialInfoMap().get(machine.getCurrentMaterialCode());
                String embryo = material == null ? null : StringUtils.trimToNull(material.getEmbryoCode());
                if (embryo != null) {
                    originalEmbryos.put(machineCode, embryo);
                }
            }
        });
    }

    /**
     * 识别普通新增及其负荷预演范围，独立结构处理和班次9继续由原流程控制。
     *
     * @param context 当前阶段上下文
     * @return 当前是否执行普通新增原胎胚优先规则
     */
    public static boolean isEnabled(LhScheduleContext context) {
        return ScheduleStepEnum.S4_5_NEW_PRODUCTION.getCode().equals(context.getCurrentStep())
                && !context.isStructureSwitchScheduling()
                && !context.isStructureEndingContinuationScheduling()
                && !context.isIsolatedNextShiftPlan();
    }

    /**
     * 按物理机台编码识别目标产线，L/R沿用原归一规则。
     *
     * @param machineCode 运行态或物理机台编码
     * @return 是否为K21或K22机台
     */
    public static boolean isTargetMachine(String machineCode) {
        String physicalCode = LhSingleControlMachineUtil.resolvePhysicalMachineCode(machineCode);
        return StringUtils.startsWithAny(physicalCode, "K21", "K22");
    }

    /**
     * 读取适用机台的原胎胚，供现有软匹配指标使用。
     *
     * @param machineCode 运行态机台编码
     * @return 初始化原胎胚编码，历史指定、非目标机台或缺失原胎胚时为空
     */
    public String getOriginalEmbryo(String machineCode) {
        return originalEmbryos.get(machineCode);
    }

    /**
     * 判断候选是否命中原胎胚优先级，未命中仍可参加其它适配比较。
     *
     * @param machineCode 运行态机台编码
     * @param sku 当前候选SKU
     * @return 原胎胚存在且编码相同时返回true
     */
    public boolean matchesOriginalEmbryo(String machineCode, SkuScheduleDTO sku) {
        String embryo = originalEmbryos.get(machineCode);
        return embryo != null && embryo.equals(StringUtils.trimToNull(sku.getEmbryoCode()));
    }

    /**
     * 将适用机台拆为独立竞争组，在同专供层的原位置按产线、胎胚、编号回填。
     * 非适用机台保留原尺寸组的完整竞争集合，跨日双桶仍由原引擎决胜。
     *
     * @param sourceGroups 已按原尺寸及专供范围分组的资源
     * @param priority 原专供优先层级
     * @return 实际按序消费的竞争组
     */
    public List<List<MachineResource>> orderGroups(List<List<MachineResource>> sourceGroups,
                                                  ToIntFunction<MachineResource> priority) {
        List<List<MachineResource>> ordered = new ArrayList<>();
        Map<Integer, List<Integer>> positions = new LinkedHashMap<>();
        for (List<MachineResource> group : sourceGroups) {
            List<MachineResource> otherResources = new ArrayList<>();
            for (MachineResource resource : group) {
                if (originalEmbryos.containsKey(resource.getMachine().getMachineCode())) {
                    positions.computeIfAbsent(priority.applyAsInt(resource), ignored -> new ArrayList<>())
                            .add(ordered.size());
                    ordered.add(Collections.singletonList(resource));
                } else {
                    if (otherResources.isEmpty()) {
                        ordered.add(otherResources);
                    }
                    otherResources.add(resource);
                }
            }
        }
        Comparator<MachineResource> order = Comparator
                .comparing((MachineResource resource) -> resource.getPhysicalMachineCode().substring(0, 3))
                .thenComparing(resource -> originalEmbryos.get(resource.getMachine().getMachineCode()))
                .thenComparing(MachineResource::getPhysicalMachineCode)
                .thenComparing(resource -> resource.getMachine().getMachineCode());
        positions.values().forEach(slots -> {
            List<List<MachineResource>> targets = slots.stream().map(ordered::get)
                    .sorted(Comparator.comparing(resources -> resources.get(0), order)).collect(Collectors.toList());
            for (int index = 0; index < slots.size(); index++) {
                ordered.set(slots.get(index), targets.get(index));
            }
        });
        return ordered;
    }

    /**
     * 在同历史优先级与同胎胚的合法候选位置内，按物理机台编号降序回填。
     *
     * @param candidates 已按原规则排序的可变候选列表
     * @param machineCode 运行态机台编码解析器
     * @param embryoCode 实际在产物料胎胚解析器
     * @param historicalRank 既有历史优先层级解析器
     * @param <T> 下机候选类型
     */
    public static <T> void orderRemoval(List<T> candidates, Function<T, String> machineCode,
                                       Function<T, String> embryoCode, ToIntFunction<T> historicalRank) {
        Map<String, List<Integer>> positions = new LinkedHashMap<>();
        for (int index = 0; index < candidates.size(); index++) {
            T candidate = candidates.get(index);
            String embryo = StringUtils.trimToNull(embryoCode.apply(candidate));
            if (isTargetMachine(machineCode.apply(candidate)) && embryo != null) {
                positions.computeIfAbsent(historicalRank.applyAsInt(candidate) + "|" + embryo,
                        ignored -> new ArrayList<>()).add(index);
            }
        }
        Comparator<T> order = Comparator.comparing(candidate -> LhSingleControlMachineUtil
                .resolvePhysicalMachineCode(machineCode.apply(candidate)), Comparator.reverseOrder());
        positions.values().forEach(slots -> {
            List<T> targets = slots.stream().map(candidates::get).sorted(order).collect(Collectors.toList());
            for (int index = 0; index < slots.size(); index++) {
                candidates.set(slots.get(index), targets.get(index));
            }
        });
    }
}
