package com.zlt.aps.tm.engine.strategy;

import com.ruoyi.common.exception.ServiceException;
import com.zlt.aps.tm.api.enums.TmScheduleErrorCodeEnum;
import com.zlt.aps.tm.engine.domain.TmPlanQtyResult;
import com.zlt.aps.tm.engine.domain.TmScheduleContext;
import com.zlt.aps.tm.engine.domain.TmTaskDraft;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 胎面默认计划量策略。
 *
 * <p>非收尾按基础需求、库存抵扣、最小起排、卷数取整、工装限制顺序计算计划量。
 * 库存抵扣使用任务草稿中的当前班初滚动库存，对当前班需求与保证范围需求各冲减后取大。
 * 本策略只依赖任务草稿中已明确的数据，不读取数据库，不修改任务链；机台产能由机台分配步骤统一处理。
 * 通过 {@link Component} 注册为 Spring Bean，由 {@link TmStrategyRegistry} 按编码 "DEFAULT" 收集。</p>
 */
@Component
public class TmDefaultPlanQtyStrategy implements ITmPlanQtyStrategy {

    /** 是 */
    private static final String YES = "1";

    /**
     * 获取策略编码。
     *
     * @return 策略编码
     */
    @Override
    public String getStrategyCode() {
        return "DEFAULT";
    }

    /**
     * 计算胎面计划量。
     *
     * @param taskDraft 胎面任务草稿
     * @param context   胎面排程上下文（当前算法暂不使用，预留扩展）
     * @return 计划量分量和最终计划量
     * @throws ServiceException 任务为空时抛出
     */
    @Override
    public TmPlanQtyResult calculate(TmTaskDraft taskDraft, TmScheduleContext context) {
        if (taskDraft == null) {
            throw new ServiceException(TmScheduleErrorCodeEnum.TM_TASK_NOT_FOUND.getDefaultMessage());
        }
        // steve's TODO：试验胶版本化口径待确认后接入
        BigDecimal currentDemand = nvl(taskDraft.getCurrentShiftDemandQty());
        BigDecimal guardDemand = nvl(taskDraft.getGuardDemandQty());
        BigDecimal grossDemand = currentDemand.max(guardDemand);
        // 库存抵扣以当前班初滚动库存为准，基础应排量取当前班缺口和保证缺口的较大值。
        BigDecimal stock = nvl(taskDraft.getRollingStockQty());
        BigDecimal stockDeductQty = stock.min(grossDemand);
        BigDecimal planQty = currentDemand.subtract(stock)
                .max(guardDemand.subtract(stock))
                .max(BigDecimal.ZERO);
        taskDraft.setStockDeductQty(stockDeductQty);

        TmPlanQtyResult result = new TmPlanQtyResult();
        result.setBaseDemandQty(planQty);
        result.setLossAddQty(BigDecimal.ZERO);
        result.setMinStartAdjustQty(BigDecimal.ZERO);
        result.setTailRoundAdjustQty(BigDecimal.ZERO);
        result.setToolLimitAdjustQty(BigDecimal.ZERO);
        result.setToolOverflowQty(BigDecimal.ZERO);
        result.setCapacityAdjustQty(BigDecimal.ZERO);

        boolean tailTask = isTailTask(taskDraft, planQty);
        if (tailTask) {
            BigDecimal beforeTail = planQty;
            BigDecimal tailBaseQty = nvl(taskDraft.getTailBalanceQty()).multiply(nvl(taskDraft.getTreadShoulderLength()));
            BigDecimal lossAddQty = calculateLossAddQty(tailBaseQty, taskDraft.getLossRate());
            planQty = tailBaseQty.add(lossAddQty);
            result.setLossAddQty(lossAddQty);
            result.setTailRoundAdjustQty(planQty.subtract(beforeTail));
        } else {
            BigDecimal beforeMinStart = planQty;
            planQty = applyMinStartQty(taskDraft, planQty);
            result.setMinStartAdjustQty(planQty.subtract(beforeMinStart));

            BigDecimal beforeRound = planQty;
            planQty = roundToCurlLength(taskDraft, planQty);
            result.setTailRoundAdjustQty(planQty.subtract(beforeRound));
        }

        BigDecimal beforeTool = planQty;
        planQty = applyToolLimit(taskDraft, planQty);
        result.setToolLimitAdjustQty(planQty.subtract(beforeTool));
        result.setToolOverflowQty(beforeTool.subtract(planQty).max(BigDecimal.ZERO));

        result.setFinalPlanQty(planQty);
        taskDraft.setPlanStockQty(calculateHandoverStock(stock, currentDemand, planQty));
        result.setCalcFormulaDesc(tailTask
                ? "收尾余量->损耗补偿->工装限制"
                : "基础需求->库存抵扣->最小起排->卷数取整->工装限制");
        taskDraft.setPlanQty(planQty);
        return result;
    }

    /**
     * 判断是否按收尾规格计算计划量。
     *
     * @param taskDraft     胎面任务草稿
     * @param baseDemandQty 基础应排需求量
     * @return true 表示收尾规格、成型余量和肩长有效，且收尾基础量不大于基础需求
     */
    private boolean isTailTask(TmTaskDraft taskDraft, BigDecimal baseDemandQty) {
        BigDecimal tailBaseQty = nvl(taskDraft.getTailBalanceQty()).multiply(nvl(taskDraft.getTreadShoulderLength()));
        return YES.equals(taskDraft.getTailFlag())
                && nvl(taskDraft.getTailBalanceQty()).compareTo(BigDecimal.ZERO) > 0
                && nvl(taskDraft.getTreadShoulderLength()).compareTo(BigDecimal.ZERO) > 0
                && tailBaseQty.compareTo(nvl(baseDemandQty)) <= 0;
    }

    /**
     * 计算收尾损耗补偿量。
     *
     * @param tailBaseQty 收尾基础量
     * @param lossRate    损耗率，百分比
     * @return 损耗补偿量
     */
    private BigDecimal calculateLossAddQty(BigDecimal tailBaseQty, BigDecimal lossRate) {
        BigDecimal rate = nvl(lossRate).divide(BigDecimal.valueOf(100), 6, RoundingMode.HALF_UP);
        if (tailBaseQty == null || tailBaseQty.compareTo(BigDecimal.ZERO) <= 0
                || rate.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        return tailBaseQty.multiply(rate);
    }

    /**
     * 按工装数量限制计划量。
     *
     * @param taskDraft 胎面任务草稿
     * @param planQty   当前计划量
     * @return 工装限制后的计划量
     */
    private BigDecimal applyToolLimit(TmTaskDraft taskDraft, BigDecimal planQty) {
        BigDecimal curlLength = resolveCurlLength(taskDraft);
        if (curlLength.compareTo(BigDecimal.ZERO) <= 0) {
            return planQty;
        }
        BigDecimal availableToolQty = taskDraft.getAvailableToolQty();
        if (availableToolQty == null) {
            if (taskDraft.getTotalToolQty() == null) {
                return planQty;
            }
            BigDecimal usedToolQty = nvl(taskDraft.getRollingStockQty()).divide(curlLength, 6, RoundingMode.HALF_UP);
            availableToolQty = taskDraft.getTotalToolQty().subtract(usedToolQty);
        }
        BigDecimal maxPlanQty = availableToolQty.max(BigDecimal.ZERO).multiply(curlLength);
        return planQty.min(maxPlanQty);
    }

    /**
     * 按最小起排量补足计划量。
     *
     * @param taskDraft 胎面任务草稿
     * @param planQty   当前计划量
     * @return 最小起排调整后的计划量
     */
    private BigDecimal applyMinStartQty(TmTaskDraft taskDraft, BigDecimal planQty) {
        BigDecimal minStartQty = nvl(taskDraft.getMinStartQty());
        if (planQty.compareTo(BigDecimal.ZERO) > 0 && minStartQty.compareTo(BigDecimal.ZERO) > 0
                && planQty.compareTo(minStartQty) < 0) {
            return minStartQty;
        }
        return planQty;
    }

    /**
     * 按卷曲长度向上取整。
     *
     * @param taskDraft 胎面任务草稿
     * @param planQty   当前计划量
     * @return 取整后的计划量
     */
    private BigDecimal roundToCurlLength(TmTaskDraft taskDraft, BigDecimal planQty) {
        BigDecimal curlLength = resolveCurlLength(taskDraft);
        if (planQty.compareTo(BigDecimal.ZERO) <= 0 || curlLength.compareTo(BigDecimal.ZERO) <= 0) {
            return planQty;
        }
        return planQty.divide(curlLength, 0, RoundingMode.CEILING).multiply(curlLength);
    }

    /**
     * 按机台剩余产能压缩计划量。
     *
     * @param taskDraft 胎面任务草稿
     * @param planQty   当前计划量
     * @return 产能压缩后的计划量
     */
    private BigDecimal applyCapacity(TmTaskDraft taskDraft, BigDecimal planQty) {
        if (taskDraft.getMachineRemainCapacity() == null) {
            return planQty;
        }
        BigDecimal switchHours = nvl(taskDraft.getMaintenanceHours())
                .add(nvl(taskDraft.getPreviousSpecSwitchHours()))
                .add(nvl(taskDraft.getPreviousGlueSwitchHours()));
        BigDecimal deductCapacity = switchHours.multiply(nvl(taskDraft.getMachineSpeed()));
        BigDecimal remainCapacity = taskDraft.getMachineRemainCapacity().subtract(deductCapacity).max(BigDecimal.ZERO);
        return planQty.min(remainCapacity);
    }

    /**
     * 解析卷曲长度，优先胎面卷曲长度，其次默认工装卷曲长度。
     *
     * @param taskDraft 胎面任务草稿
     * @return 卷曲长度，无法取得时返回 0
     */
    private BigDecimal resolveCurlLength(TmTaskDraft taskDraft) {
        if (taskDraft.getCurlRollLength() != null && taskDraft.getCurlRollLength().compareTo(BigDecimal.ZERO) > 0) {
            return taskDraft.getCurlRollLength();
        }
        return nvl(taskDraft.getDefaultCurlRollLength());
    }

    /**
     * 计算当前任务完成后的交接班预计库存。
     *
     * @param shiftStartStock 当前班初滚动库存
     * @param currentDemand 当前班成型胎面需求量
     * @param finalPlanQty 最终计划量
     * @return 交接班预计库存，低于 0 时按 0 处理
     */
    private BigDecimal calculateHandoverStock(BigDecimal shiftStartStock, BigDecimal currentDemand, BigDecimal finalPlanQty) {
        return nvl(shiftStartStock).add(nvl(finalPlanQty)).subtract(nvl(currentDemand)).max(BigDecimal.ZERO);
    }

    /**
     * 空值转 0。
     *
     * @param value 原始数值
     * @return 非空数值
     */
    private BigDecimal nvl(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
