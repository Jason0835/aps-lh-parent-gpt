package com.zlt.aps.lh.engine.strategy.support;

import com.zlt.aps.lh.api.domain.dto.SkuScheduleDTO;
import com.zlt.aps.lh.api.domain.entity.LhMouldChangePlan;
import com.zlt.aps.lh.api.domain.entity.LhScheduleResult;
import com.zlt.aps.lh.context.LhScheduleContext;
import com.zlt.aps.lh.util.LhSingleControlMachineUtil;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.Objects;

/**
 * 历史指定前料的原机余量身份判定，数量及设备可行性仍由公共续作分配器校验。
 *
 * @author Codex
 * @date 2026/10/30
 */
public final class HistoricalBeforeRemainderPolicy {
    /**
     * 工具类不允许实例化。
     */
    private HistoricalBeforeRemainderPolicy() {
    }

    /**
     * 判断无日计划前料是否仍有必须在历史原机消化的旧余量，不借用未来月计划。
     *
     * @param context 已冻结历史事件和真实机台的上下文
     * @param sku 当前续作SKU
     * @return 原机、物料、状态及原模具均有明确历史依据时返回true
     */
    public static boolean hasOriginalRemainder(LhScheduleContext context, SkuScheduleDTO sku) {
        return Objects.nonNull(sku) && sku.getSurplusQty() > 0
                && matches(context, sku.getContinuousMachineCode(), sku.getMaterialCode(), sku.getProductStatus());
    }

    /**
     * 仅允许独立普通原机申请历史班内完整收尾，多机分摊及单控侧别继续执行固定前缀。
     *
     * @param context 已确认的历史关系
     * @param originals 本组全部原机结果，不能只传过滤后的部分机台
     * @return 单台纯续作且精确匹配历史前料时返回true
     */
    public static boolean isSingleOriginalMachine(LhScheduleContext context, List<LhScheduleResult> originals) {
        if (Objects.isNull(originals) || originals.size() != 1) {
            return false;
        }
        LhScheduleResult result = originals.get(0);
        return Objects.nonNull(result) && StringUtils.equals("01", result.getScheduleType())
                && !StringUtils.equals("1", result.getIsTypeBlock())
                && matches(context, result.getLhMachineCode(), result.getMaterialCode(), result.getProductStatus());
    }

    /**
     * 核对冻结事件的精确前料身份，避免同料动作、第三种在机料或仅后料命中获得例外。
     *
     * @param context 本批历史事件与机台
     * @param machineCode 原机编码
     * @param materialCode 前料编码
     * @param productStatus 前料状态
     * @return 有效普通原机历史异料关系时返回true
     */
    private static boolean matches(LhScheduleContext context, String machineCode,
            String materialCode, String productStatus) {
        if (Objects.isNull(context) || StringUtils.isBlank(machineCode) || StringUtils.isBlank(productStatus)
                || !context.getMachineScheduleMap().containsKey(machineCode)
                || LhSingleControlMachineUtil.isVirtualMachineCode(machineCode)
                || LhSingleControlMachineUtil.isConfiguredSingleControlMachine(context, machineCode)
                || context.isContinuousStopHoldMachine(machineCode)) {
            return false;
        }
        PreviousAlternatePlanReleaseEvent event = context.getPreviousAlternateReleaseEventMap().get(machineCode);
        if (Objects.isNull(event) || Objects.isNull(event.getPlan())
                || Objects.isNull(event.getPlannedShiftStartTime()) || Objects.isNull(event.getPlannedShiftEndTime())
                || event.getOriginalMouldCodes().isEmpty()) {
            return false;
        }
        LhMouldChangePlan plan = event.getPlan();
        return StringUtils.equals(machineCode, event.getMachineCode())
                && StringUtils.equals(machineCode, plan.getLhMachineCode())
                && StringUtils.equals(materialCode, event.getMaterialCode())
                && StringUtils.equals(materialCode, plan.getBeforeMaterialCode())
                && StringUtils.equals(productStatus, event.getProductStatus())
                && StringUtils.equals(productStatus, plan.getProductStatus())
                && StringUtils.isNotBlank(plan.getAfterMaterialCode())
                && !StringUtils.equals(materialCode, plan.getAfterMaterialCode());
    }
}
