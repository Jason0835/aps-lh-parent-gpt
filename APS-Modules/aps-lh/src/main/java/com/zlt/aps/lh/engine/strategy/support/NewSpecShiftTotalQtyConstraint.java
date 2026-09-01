package com.zlt.aps.lh.engine.strategy.support;

import com.zlt.aps.lh.api.constant.LhScheduleConstant;
import com.zlt.aps.lh.api.constant.LhScheduleParamConstant;
import com.zlt.aps.lh.api.domain.entity.LhScheduleResult;
import com.zlt.aps.lh.context.LhScheduleContext;
import com.zlt.aps.lh.util.ShiftFieldUtil;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.Map;
import java.util.Objects;

/**
 * S4.5 首次起排及独立提案的班次总计划量原子约束。
 *
 * <p>当前量只读取 {@code context.scheduleResultList} 正式结果；首次起排、首检落位及独立
 * 后置提案以完整正向增量一次校验，不维护第二套班次总量账本。同一 SKU 连续链已经上机后，
 * 由调用方显式传入豁免语义，避免班次总量上限打断连续生产。</p>
 *
 * @author APS
 */
@Component
public class NewSpecShiftTotalQtyConstraint {

    /**
     * 校验单个结果的班次正向增量。
     *
     * @param context 排程上下文
     * @param result 当前结果，尚未加入正式列表时其已有量计入当前基数
     * @param shiftIndex 班次索引
     * @param deltaQty 本次正向增量
     * @return 原子校验结果
     */
    public NewSpecShiftTotalQtyValidation validateIncrease(
            LhScheduleContext context,
            LhScheduleResult result,
            Integer shiftIndex,
            int deltaQty) {
        int shiftLimit = this.resolveShiftLimit(context);
        if (Objects.isNull(shiftIndex) || deltaQty <= 0 || shiftLimit <= 0) {
            return new NewSpecShiftTotalQtyValidation(
                    true, 0, Math.max(0, deltaQty), Math.max(0, deltaQty), shiftLimit);
        }
        int currentShiftQty = this.resolveCurrentShiftQty(context, shiftIndex);
        if (!this.isPersistedResult(context, result)) {
            currentShiftQty += this.resolvePositiveShiftQty(result, shiftIndex);
        }
        int projectedShiftQty = currentShiftQty + deltaQty;
        return new NewSpecShiftTotalQtyValidation(
                projectedShiftQty <= shiftLimit,
                currentShiftQty, deltaQty, projectedShiftQty, shiftLimit);
    }

    /**
     * 校验同一班次一组结果重分配后的完整目标量。
     *
     * @param context 排程上下文
     * @param shiftIndex 班次索引
     * @param targetQtyMap 结果到重分配后目标量
     * @return 原子校验结果
     */
    public NewSpecShiftTotalQtyValidation validateTargets(
            LhScheduleContext context,
            Integer shiftIndex,
            Map<LhScheduleResult, Integer> targetQtyMap) {
        int shiftLimit = this.resolveShiftLimit(context);
        if (Objects.isNull(shiftIndex) || CollectionUtils.isEmpty(targetQtyMap)
                || shiftLimit <= 0) {
            return new NewSpecShiftTotalQtyValidation(true, 0, 0, 0, shiftLimit);
        }
        int currentShiftQty = this.resolveCurrentShiftQty(context, shiftIndex);
        int projectedShiftQty = 0;
        if (Objects.nonNull(context) && !CollectionUtils.isEmpty(context.getScheduleResultList())) {
            for (LhScheduleResult result : context.getScheduleResultList()) {
                if (Objects.isNull(result)) {
                    continue;
                }
                Integer targetQty = targetQtyMap.get(result);
                projectedShiftQty += targetQtyMap.containsKey(result)
                        ? Math.max(0, Objects.isNull(targetQty) ? 0 : targetQty)
                        : this.resolvePositiveShiftQty(result, shiftIndex);
            }
        }
        for (Map.Entry<LhScheduleResult, Integer> entry : targetQtyMap.entrySet()) {
            if (this.isPersistedResult(context, entry.getKey())) {
                continue;
            }
            projectedShiftQty += Math.max(0,
                    Objects.isNull(entry.getValue()) ? 0 : entry.getValue());
        }
        int deltaQty = Math.max(0, projectedShiftQty - currentShiftQty);
        return new NewSpecShiftTotalQtyValidation(
                projectedShiftQty <= shiftLimit,
                currentShiftQty, deltaQty, projectedShiftQty, shiftLimit);
    }

    /**
     * 解析班次总计划量上限。
     *
     * @param context 排程上下文
     * @return 上限，值小于等于0表示不限制
     */
    public int resolveShiftLimit(LhScheduleContext context) {
        if (Objects.isNull(context)) {
            return LhScheduleConstant.CLASS_TOTAL_QTY_UP_LIMIT;
        }
        return context.getParamIntValue(
                LhScheduleParamConstant.CLASS_TOTAL_QTY_UP_LIMIT,
                LhScheduleConstant.CLASS_TOTAL_QTY_UP_LIMIT);
    }

    /**
     * 汇总正式结果中指定班次的当前计划量。
     *
     * @param context 排程上下文
     * @param shiftIndex 班次索引
     * @return 当前班次总计划量
     */
    public int resolveCurrentShiftQty(LhScheduleContext context, Integer shiftIndex) {
        if (Objects.isNull(context) || Objects.isNull(shiftIndex)
                || CollectionUtils.isEmpty(context.getScheduleResultList())) {
            return 0;
        }
        int totalQty = 0;
        for (LhScheduleResult result : context.getScheduleResultList()) {
            totalQty += this.resolvePositiveShiftQty(result, shiftIndex);
        }
        return totalQty;
    }

    private int resolvePositiveShiftQty(LhScheduleResult result, Integer shiftIndex) {
        if (Objects.isNull(result) || Objects.isNull(shiftIndex)) {
            return 0;
        }
        Integer planQty = ShiftFieldUtil.getShiftPlanQty(result, shiftIndex);
        return Math.max(0, Objects.isNull(planQty) ? 0 : planQty);
    }

    private boolean isPersistedResult(LhScheduleContext context, LhScheduleResult result) {
        if (Objects.isNull(context) || Objects.isNull(result)
                || CollectionUtils.isEmpty(context.getScheduleResultList())) {
            return false;
        }
        for (LhScheduleResult scheduleResult : context.getScheduleResultList()) {
            if (scheduleResult == result) {
                return true;
            }
        }
        return false;
    }
}
