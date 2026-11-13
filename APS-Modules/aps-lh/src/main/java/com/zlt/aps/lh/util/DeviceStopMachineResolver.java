package com.zlt.aps.lh.util;

import com.zlt.aps.common.core.enums.DeviceShutMachineTypeEnums;
import com.zlt.aps.lh.api.enums.MachineStopTypeEnum;
import com.zlt.aps.lh.context.LhScheduleContext;
import com.zlt.aps.lh.engine.strategy.support.DeviceStopMachineSource;
import com.zlt.aps.mdm.api.domain.entity.MdmDevicePlanShut;
import org.apache.commons.lang3.StringUtils;

import java.util.Objects;

/**
 * 设备停机来源机台公共解析器，统一物理机台、运行侧及来源侧身份。
 * 只读取已加载配置，不查询数据库、不改写来源、不分配清洗或换模资源。
 *
 * @author APS
 * @since 2026/11/13
 */
public final class DeviceStopMachineResolver {
    /**
     * 禁止创建无状态解析器实例。
     */
    private DeviceStopMachineResolver() {
    }

    /**
     * 优先读取初始化冻结的来源身份，尚未初始化时按当前机台配置只读解析。
     * 硫化喷砂的侧来源可归属实际存在的整机；真正运行侧优先保留。
     *
     * @param context 已加载机台配置及清洗运行态的上下文
     * @param source 原始设备停机计划
     * @return 来源身份；上下文或来源缺失时为空
     */
    public static DeviceStopMachineSource resolve(LhScheduleContext context, MdmDevicePlanShut source) {
        if (context == null || source == null) {
            return null;
        }
        DeviceStopMachineSource registered = source.getId() == null ? null
                : context.getCleaningPlanRuntimeState().getMachineSourceMap().get(source.getId());
        if (registered != null) {
            return registered;
        }
        String sourceCode = source.getMachineCode();
        String physicalCode = LhSingleControlMachineUtil.resolvePhysicalMachineCode(sourceCode);
        String sourceSide = LhSingleControlMachineUtil.resolveSplitSide(sourceCode);
        String runtimeCode = sourceCode;
        boolean sideMappedToPhysical = false;
        if (StringUtils.isNotBlank(sourceCode) && !containsMachine(context, sourceCode)) {
            if (sourceSide != null && containsMachine(context, physicalCode)
                    && DeviceShutMachineTypeEnums.LH.getCode().equals(source.getMachineType())
                    && MachineStopTypeEnum.SANDBLASTING_CLEANING.getCode().equals(source.getMachineStopType())) {
                runtimeCode = physicalCode;
                sideMappedToPhysical = true;
            } else if (sourceSide == null
                    && containsMachine(context, LhSingleControlMachineUtil.resolveLeftMachineCode(sourceCode))
                    && containsMachine(context, LhSingleControlMachineUtil.resolveRightMachineCode(sourceCode))) {
                // 整机清洗来源沿用主侧挂载，配对侧和额度仍由现有清洗流程处理。
                runtimeCode = LhSingleControlMachineUtil.resolveLeftMachineCode(sourceCode);
            }
        }
        return new DeviceStopMachineSource(source.getId(), sourceCode, physicalCode, runtimeCode, sourceSide,
                LhMouldCodeUtil.splitMouldCode(source.getInMachineMouldCode()), sideMappedToPhysical);
    }

    /**
     * 按窗口来源主键读取公共身份，兼容只持有原始清洗快照的业务入口。
     *
     * @param context 当前批次上下文
     * @param sourcePlanId 清洗窗口关联的原计划主键
     * @return 对应来源身份；主键或原始来源缺失时为空
     */
    public static DeviceStopMachineSource resolveSource(LhScheduleContext context, Long sourcePlanId) {
        if (context == null || sourcePlanId == null) {
            return null;
        }
        DeviceStopMachineSource registered = context.getCleaningPlanRuntimeState()
                .getMachineSourceMap().get(sourcePlanId);
        if (registered != null) {
            return registered;
        }
        return context.getLoadedCleaningPlanShutList().stream().filter(Objects::nonNull)
                .filter(source -> sourcePlanId.equals(source.getId()))
                .map(source -> resolve(context, source)).findFirst().orElse(null);
    }

    /**
     * 检查机台在初始化主数据或已构建运行态中是否真实存在。
     *
     * @param context 已加载的排程上下文
     * @param machineCode 待核对机台编码
     * @return 存在对应机台时返回true
     */
    private static boolean containsMachine(LhScheduleContext context, String machineCode) {
        return StringUtils.isNotBlank(machineCode) && (context.getMachineInfoMap().containsKey(machineCode)
                || context.getMachineScheduleMap().containsKey(machineCode));
    }
}
