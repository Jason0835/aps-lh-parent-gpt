package com.zlt.aps.lh.util;

import com.zlt.aps.lh.api.enums.MachineStopTypeEnum;
import com.zlt.aps.mdm.api.domain.entity.MdmDevicePlanShut;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.CollectionUtils;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * 统一硫化盘点的硫化线优先匹配，保留原始计划和各消费入口的机台范围。
 * 日期、工厂及状态过滤由原加载入口负责，本类只选择有效时间的盘点配置，不计算时间交集。
 *
 * @author APS
 * @date 2026/11/03
 */
public final class LhInventoryPlanMatcher {

    /**
     * 禁止创建无状态匹配器实例。
     */
    private LhInventoryPlanMatcher() {
    }

    /**
     * 按硫化线优先、机台编码精确匹配兜底选择盘点，保留同级全部有效计划。
     *
     * @param loadedPlans 原入口已加载的有效范围内设备计划
     * @param machineCode 目标运行侧编码，不扩大机台级盘点的左右侧范围
     * @return 按来源顺序返回的盘点计划，未命中时为空
     */
    public static List<MdmDevicePlanShut> matchInventoryPlans(List<MdmDevicePlanShut> loadedPlans,
                                                            String machineCode) {
        return matchStopPlans(loadedPlans, machineCode).stream()
                .filter(plan -> MachineStopTypeEnum.TAKE_STOCK.getCode().equals(plan.getMachineStopType()))
                .collect(Collectors.toList());
    }

    /**
     * 为精确机台消费入口选择停机计划，仅盘点增加硫化线优先匹配。
     *
     * @param loadedPlans 原入口已加载的设备计划
     * @param machineCode 目标运行侧编码
     * @return 命中的原始计划，其他停机类型继续精确匹配机台
     */
    public static List<MdmDevicePlanShut> matchStopPlans(List<MdmDevicePlanShut> loadedPlans,
                                                       String machineCode) {
        return matchStopPlans(loadedPlans, machineCode,
                sourceMachineCode -> StringUtils.equals(machineCode, sourceMachineCode));
    }

    /**
     * 为原物理机冲突检查选择停机计划，不改变其既有左右侧合并范围。
     *
     * @param loadedPlans 原入口已加载的设备计划
     * @param physicalMachineCode 原入口已解析的物理机台编码
     * @return 盘点按硫化线优先选择，其他计划继续按原物理机台匹配
     */
    public static List<MdmDevicePlanShut> matchPhysicalStopPlans(List<MdmDevicePlanShut> loadedPlans,
                                                               String physicalMachineCode) {
        return matchStopPlans(loadedPlans, physicalMachineCode,
                sourceMachineCode -> StringUtils.equals(physicalMachineCode,
                        LhSingleControlMachineUtil.resolvePhysicalMachineCode(sourceMachineCode)));
    }

    /**
     * 按原机台匹配范围保留其他类型，并在时间交集计算前统一决定盘点配置优先级。
     *
     * @param loadedPlans 原入口已加载的设备计划
     * @param machineCode 用于硫化线前缀匹配的目标机台编码
     * @param machineMatcher 原入口的精确或物理机台匹配规则
     * @return 按原顺序返回的来源计划，不复制或改写实体
     */
    private static List<MdmDevicePlanShut> matchStopPlans(List<MdmDevicePlanShut> loadedPlans,
                                                        String machineCode,
                                                        Predicate<String> machineMatcher) {
        if (CollectionUtils.isEmpty(loadedPlans) || StringUtils.isEmpty(machineCode)) {
            return Collections.emptyList();
        }
        boolean hasLineInventory = loadedPlans.stream()
                .filter(LhInventoryPlanMatcher::isValidInventory)
                .anyMatch(plan -> StringUtils.isNotBlank(plan.getVulcanizingLine())
                        && machineCode.startsWith(plan.getVulcanizingLine()));
        return loadedPlans.stream().filter(Objects::nonNull).filter(plan -> {
            if (!MachineStopTypeEnum.TAKE_STOCK.getCode().equals(plan.getMachineStopType())) {
                return machineMatcher.test(plan.getMachineCode());
            }
            if (!isValidInventory(plan)) {
                return false;
            }
            // 有线级配置时不按当前班次恢复机台级盘点，避免扣产能与备注使用不同优先级。
            return hasLineInventory
                    ? StringUtils.isNotBlank(plan.getVulcanizingLine())
                    && machineCode.startsWith(plan.getVulcanizingLine())
                    : machineMatcher.test(plan.getMachineCode());
        }).collect(Collectors.toList());
    }

    /**
     * 检查盘点类型及原始时间有效性，非法线级计划不得覆盖有效机台计划。
     *
     * @param plan 待检查的来源计划
     * @return 是否为开始时间早于结束时间的有效盘点
     */
    private static boolean isValidInventory(MdmDevicePlanShut plan) {
        return Objects.nonNull(plan)
                && MachineStopTypeEnum.TAKE_STOCK.getCode().equals(plan.getMachineStopType())
                && Objects.nonNull(plan.getBeginDate()) && Objects.nonNull(plan.getEndDate())
                && plan.getBeginDate().before(plan.getEndDate());
    }
}
