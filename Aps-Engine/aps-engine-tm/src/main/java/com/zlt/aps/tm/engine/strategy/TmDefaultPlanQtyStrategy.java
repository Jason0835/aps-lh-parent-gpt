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
 * <p>按基础需求、工装限制、最小起排、卷数取整、产能压缩顺序计算计划量。
 * 本策略只依赖任务草稿中已明确的数据，不读取数据库，不修改任务链。
 * 通过 {@link Component} 注册为 Spring Bean，由 {@link TmStrategyRegistry} 按编码 "DEFAULT" 收集。</p>
 */
@Component
public class TmDefaultPlanQtyStrategy implements ITmPlanQtyStrategy {

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
        BigDecimal planQty = nvl(taskDraft.getCurrentShiftDemandQty())
                .max(nvl(taskDraft.getGuardDemandQty()).subtract(nvl(taskDraft.getRollingStockQty())).max(BigDecimal.ZERO));
        TmPlanQtyResult result = new TmPlanQtyResult();
        result.setBaseDemandQty(planQty);

        BigDecimal beforeTool = planQty;
        planQty = applyToolLimit(taskDraft, planQty);
        result.setToolLimitAdjustQty(planQty.subtract(beforeTool));

        BigDecimal beforeMinStart = planQty;
        planQty = applyMinStartQty(taskDraft, planQty);
        result.setMinStartAdjustQty(planQty.subtract(beforeMinStart));

        BigDecimal beforeRound = planQty;
        planQty = roundToCurlLength(taskDraft, planQty);
        result.setTailRoundAdjustQty(planQty.subtract(beforeRound));

        BigDecimal beforeCapacity = planQty;
        planQty = applyCapacity(taskDraft, planQty);
        result.setCapacityAdjustQty(planQty.subtract(beforeCapacity));

        result.setFinalPlanQty(planQty);
        result.setCalcFormulaDesc("基础需求->工装限制->最小起排->卷数取整->产能压缩");
        taskDraft.setPlanQty(planQty);
        return result;
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
        if (taskDraft.getTotalToolQty() == null || curlLength.compareTo(BigDecimal.ZERO) <= 0) {
            return planQty;
        }
        BigDecimal usedToolQty = nvl(taskDraft.getRollingStockQty()).divide(curlLength, 6, RoundingMode.HALF_UP);
        BigDecimal availableToolQty = taskDraft.getTotalToolQty().subtract(usedToolQty).max(BigDecimal.ZERO);
        BigDecimal maxPlanQty = availableToolQty.multiply(curlLength);
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
     * 空值转 0。
     *
     * @param value 原始数值
     * @return 非空数值
     */
    private BigDecimal nvl(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
