package com.zlt.aps.common.engine.schedule;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 公共默认计划量算法模板。
 *
 * <p>模板统一处理两班库存覆盖、基础需求扣库存、收尾限制、最小起排、卷数取整、
 * 交接库存和结果分量。TM/TC 通过字段及结果适配钩子提供领域对象，不在子类复制主流程。</p>
 *
 * @param <C> 排程上下文类型
 * @param <T> 待排任务类型
 * @param <R> 计划量结果类型
 */
public abstract class AbstractDefaultPlanQtyStrategy<C, T, R> {

    /**
     * 执行公共默认计划量计算。
     *
     * @param task    待排任务
     * @param context 排程上下文
     * @return 计划量计算结果
     * @throws RuntimeException 任务为空或领域校验失败时由适配器抛出业务异常
     */
    public final R calculate(T task, C context) {
        this.validateTask(task, context);
        BigDecimal currentDemand = this.nvl(this.getCurrentShiftDemand(task));
        BigDecimal nextShiftDemand = this.nvl(this.getNextShiftDemand(task));
        BigDecimal guardDemand = this.nvl(this.getGuardDemand(task));
        BigDecimal stock = this.nvl(this.getRollingStockQty(task));
        if (this.isFormingShutdownCloseOut(task)) {
            return this.calculateFormingShutdownCloseOut(task, stock);
        }

        BigDecimal twoShiftDemand = currentDemand.add(nextShiftDemand);
        BigDecimal twoShiftStockGap = twoShiftDemand.subtract(stock);
        this.setTwoShiftDemandQty(task, twoShiftDemand);
        this.setTwoShiftStockGapQty(task, twoShiftStockGap);
        if (this.isTwoShiftStockCoverageApplicable(task)
                && twoShiftStockGap.compareTo(BigDecimal.ZERO) <= 0) {
            this.setTwoShiftStockCovered(task, Boolean.TRUE);
            return this.buildTwoShiftCoveredResult(task, currentDemand, twoShiftDemand, stock);
        }
        if (this.isTwoShiftStockCoverageApplicable(task)) {
            this.setTwoShiftStockCovered(task, Boolean.FALSE);
        }

        BigDecimal grossDemand = currentDemand.add(guardDemand);
        BigDecimal stockDeductQty = stock.min(grossDemand);
        BigDecimal planQty = grossDemand.subtract(stock).max(BigDecimal.ZERO);
        this.setStockDeductQty(task, stockDeductQty);

        R result = this.newResult();
        this.setResultBaseDemandQty(result, planQty);
        this.initializeZeroAdjustments(result);

        BigDecimal preLossPlanQty = planQty;
        boolean tailTask = this.isTailTask(task, planQty);
        if (tailTask) {
            // 收尾规格取需求量和收尾余量对应长度的较小值，不执行最小起排和卷数取整。
            BigDecimal beforeTail = planQty;
            BigDecimal tailBaseQty = this.nvl(this.getTailBalanceQty(task))
                    .multiply(this.nvl(this.getProductLength(task)));
            planQty = planQty.min(tailBaseQty);
            preLossPlanQty = planQty;
            this.setResultTailRoundAdjustQty(result, planQty.subtract(beforeTail));
        } else {
            BigDecimal beforeMinStart = planQty;
            planQty = this.applyMinStartQty(task, planQty);
            this.setResultMinStartAdjustQty(result, planQty.subtract(beforeMinStart));

            BigDecimal beforeRound = planQty;
            planQty = this.roundToCurlLength(task, planQty);
            this.setResultTailRoundAdjustQty(result, planQty.subtract(beforeRound));
        }

        // 损耗前计划量必须保留真实基础量，不能被最小起排或卷数取整后的估算量覆盖。
        this.setResultPreLossPlanQty(result, preLossPlanQty);
        this.setResultPlanQtyBeforeToolLimit(result, planQty);
        this.setResultFinalPlanQty(result, planQty);
        this.setPlanStockQty(task, this.calculateHandoverStock(stock, currentDemand, planQty));
        this.setResultCalcFormulaDesc(result,
                tailTask ? "收尾余量" : "基础需求->库存抵扣->派机前最小起排与卷数取整估算");
        this.setPlanQty(task, planQty);
        return result;
    }

    /**
     * 校验计划量计算输入。
     *
     * @param task    待排任务
     * @param context 排程上下文
     * @throws RuntimeException 校验失败时抛出领域业务异常
     */
    protected abstract void validateTask(T task, C context);

    /**
     * 创建领域计划量结果对象。
     *
     * @return 空计划量结果
     */
    protected abstract R newResult();

    /**
     * 获取当班需求量。
     *
     * @param task 待排任务
     * @return 当班需求量
     */
    protected abstract BigDecimal getCurrentShiftDemand(T task);

    /**
     * 获取下一班需求量。
     *
     * @param task 待排任务
     * @return 下一班需求量
     */
    protected abstract BigDecimal getNextShiftDemand(T task);

    /**
     * 获取库存保证需求量。
     *
     * @param task 待排任务
     * @return 库存保证需求量
     */
    protected abstract BigDecimal getGuardDemand(T task);

    /**
     * 获取班初滚动库存。
     *
     * @param task 待排任务
     * @return 班初滚动库存
     */
    protected abstract BigDecimal getRollingStockQty(T task);

    /**
     * 判断是否为成型连续停产收尾任务。
     *
     * @param task 待排任务
     * @return 是否为停产收尾任务
     */
    protected abstract boolean isFormingShutdownCloseOut(T task);

    /**
     * 判断是否应用两班库存覆盖门槛。
     *
     * @param task 待排任务
     * @return 是否应用两班库存覆盖
     */
    protected abstract boolean isTwoShiftStockCoverageApplicable(T task);

    /**
     * 获取收尾余量。
     *
     * @param task 待排任务
     * @return 收尾余量
     */
    protected abstract BigDecimal getTailBalanceQty(T task);

    /**
     * 获取胎面或胎侧标准长度。
     *
     * @param task 待排任务
     * @return 产品标准长度
     */
    protected abstract BigDecimal getProductLength(T task);

    /**
     * 判断任务是否为有效收尾任务。
     *
     * @param task          待排任务
     * @param baseDemandQty 库存抵扣后的基础需求量
     * @return 是否执行收尾计划量口径
     */
    protected abstract boolean isTailTask(T task, BigDecimal baseDemandQty);

    /**
     * 获取最小起排量。
     *
     * @param task 待排任务
     * @return 最小起排量
     */
    protected abstract BigDecimal getMinStartQty(T task);

    /**
     * 获取任务卷曲长度。
     *
     * @param task 待排任务
     * @return 任务卷曲长度
     */
    protected abstract BigDecimal getCurlRollLength(T task);

    /**
     * 获取默认卷曲长度。
     *
     * @param task 待排任务
     * @return 默认卷曲长度
     */
    protected abstract BigDecimal getDefaultCurlRollLength(T task);

    /**
     * 写入两班需求合计。
     *
     * @param task 任务
     * @param value 两班需求合计
     */
    protected abstract void setTwoShiftDemandQty(T task, BigDecimal value);

    /**
     * 写入两班库存缺口。
     *
     * @param task 任务
     * @param value 两班库存缺口
     */
    protected abstract void setTwoShiftStockGapQty(T task, BigDecimal value);

    /**
     * 写入两班库存覆盖标识。
     *
     * @param task 任务
     * @param value 是否覆盖
     */
    protected abstract void setTwoShiftStockCovered(T task, Boolean value);

    /**
     * 写入库存抵扣量。
     *
     * @param task 任务
     * @param value 库存抵扣量
     */
    protected abstract void setStockDeductQty(T task, BigDecimal value);

    /**
     * 写入交接库存。
     *
     * @param task 任务
     * @param value 交接库存
     */
    protected abstract void setPlanStockQty(T task, BigDecimal value);

    /**
     * 写入最终计划量。
     *
     * @param task 任务
     * @param value 最终计划量
     */
    protected abstract void setPlanQty(T task, BigDecimal value);

    /**
     * 设置结果基础需求量。
     *
     * @param result 计划量结果
     * @param value 基础需求量
     */
    protected abstract void setResultBaseDemandQty(R result, BigDecimal value);

    /**
     * 设置结果损耗补偿量。
     *
     * @param result 计划量结果
     * @param value 损耗补偿量
     */
    protected abstract void setResultLossAddQty(R result, BigDecimal value);

    /**
     * 设置结果最小起排调整量。
     *
     * @param result 计划量结果
     * @param value 最小起排调整量
     */
    protected abstract void setResultMinStartAdjustQty(R result, BigDecimal value);

    /**
     * 设置结果卷数取整调整量。
     *
     * @param result 计划量结果
     * @param value 卷数取整调整量
     */
    protected abstract void setResultTailRoundAdjustQty(R result, BigDecimal value);

    /**
     * 设置结果工装限制调整量。
     *
     * @param result 计划量结果
     * @param value 工装限制调整量
     */
    protected abstract void setResultToolLimitAdjustQty(R result, BigDecimal value);

    /**
     * 设置结果工装溢出量。
     *
     * @param result 计划量结果
     * @param value 工装溢出量
     */
    protected abstract void setResultToolOverflowQty(R result, BigDecimal value);

    /**
     * 设置结果产能调整量。
     *
     * @param result 计划量结果
     * @param value 产能调整量
     */
    protected abstract void setResultCapacityAdjustQty(R result, BigDecimal value);

    /**
     * 设置结果损耗前计划量。
     *
     * @param result 计划量结果
     * @param value 损耗前计划量
     */
    protected abstract void setResultPreLossPlanQty(R result, BigDecimal value);

    /**
     * 设置结果工装限制前计划量。
     *
     * @param result 计划量结果
     * @param value 工装限制前计划量
     */
    protected abstract void setResultPlanQtyBeforeToolLimit(R result, BigDecimal value);

    /**
     * 设置结果最终计划量。
     *
     * @param result 计划量结果
     * @param value 最终计划量
     */
    protected abstract void setResultFinalPlanQty(R result, BigDecimal value);

    /**
     * 设置结果计算说明。
     *
     * @param result 计划量结果
     * @param value 计算说明
     */
    protected abstract void setResultCalcFormulaDesc(R result, String value);

    /**
     * 计算停产收尾任务计划量。
     *
     * @param task 任务
     * @param stock 班初滚动库存
     * @return 停产收尾计划量结果
     */
    private R calculateFormingShutdownCloseOut(T task, BigDecimal stock) {
        BigDecimal closeOutDemandQty = this.nvl(this.getFormingShutdownCloseOutDemandQty(task));
        BigDecimal stockDeductQty = stock.min(closeOutDemandQty);
        BigDecimal planQty = closeOutDemandQty.subtract(stock).max(BigDecimal.ZERO);
        this.setTwoShiftDemandQty(task, closeOutDemandQty);
        this.setTwoShiftStockGapQty(task, closeOutDemandQty.subtract(stock));
        this.setTwoShiftStockCovered(task, planQty.compareTo(BigDecimal.ZERO) == 0);
        this.setStockDeductQty(task, stockDeductQty);
        this.setPlanStockQty(task, stock.add(planQty).subtract(closeOutDemandQty).max(BigDecimal.ZERO));
        this.setPlanQty(task, planQty);

        R result = this.newResult();
        this.setResultBaseDemandQty(result, planQty);
        this.initializeZeroAdjustments(result);
        this.setResultPreLossPlanQty(result, planQty);
        this.setResultPlanQtyBeforeToolLimit(result, planQty);
        this.setResultFinalPlanQty(result, planQty);
        this.setResultCalcFormulaDesc(result, "成型连续停产收尾需求->库存抵扣");
        return result;
    }

    /**
     * 获取停产收尾需求量。
     *
     * @param task 任务
     * @return 停产收尾需求量
     */
    protected abstract BigDecimal getFormingShutdownCloseOutDemandQty(T task);

    /**
     * 构建两班库存已覆盖的零计划结果。
     *
     * @param task            任务
     * @param currentDemand  当班需求量
     * @param twoShiftDemand 两班需求合计
     * @param stock           班初滚动库存
     * @return 零计划结果
     */
    private R buildTwoShiftCoveredResult(T task, BigDecimal currentDemand,
                                         BigDecimal twoShiftDemand, BigDecimal stock) {
        BigDecimal zero = BigDecimal.ZERO;
        this.setStockDeductQty(task, stock.min(twoShiftDemand));
        this.setPlanStockQty(task, stock.subtract(currentDemand).max(zero));
        this.setPlanQty(task, zero);

        R result = this.newResult();
        this.setResultBaseDemandQty(result, zero);
        this.initializeZeroAdjustments(result);
        this.setResultPreLossPlanQty(result, zero);
        this.setResultPlanQtyBeforeToolLimit(result, zero);
        this.setResultFinalPlanQty(result, zero);
        this.setResultCalcFormulaDesc(result, "两班需求已由班初滚动库存覆盖，当班无需排产");
        return result;
    }

    /**
     * 初始化计划量结果中的公共调整分量。
     *
     * @param result 计划量结果
     */
    private void initializeZeroAdjustments(R result) {
        this.setResultLossAddQty(result, BigDecimal.ZERO);
        this.setResultMinStartAdjustQty(result, BigDecimal.ZERO);
        this.setResultTailRoundAdjustQty(result, BigDecimal.ZERO);
        this.setResultToolLimitAdjustQty(result, BigDecimal.ZERO);
        this.setResultToolOverflowQty(result, BigDecimal.ZERO);
        this.setResultCapacityAdjustQty(result, BigDecimal.ZERO);
    }

    /**
     * 应用最小起排量规则。
     *
     * @param task    任务
     * @param planQty 当前计划量
     * @return 调整后的计划量
     */
    private BigDecimal applyMinStartQty(T task, BigDecimal planQty) {
        BigDecimal minStartQty = this.nvl(this.getMinStartQty(task));
        if (planQty.compareTo(BigDecimal.ZERO) > 0 && minStartQty.compareTo(BigDecimal.ZERO) > 0
                && planQty.compareTo(minStartQty) < 0) {
            return minStartQty;
        }
        return planQty;
    }

    /**
     * 按卷曲长度向上取整。
     *
     * @param task    任务
     * @param planQty 当前计划量
     * @return 取整后的计划量
     */
    private BigDecimal roundToCurlLength(T task, BigDecimal planQty) {
        BigDecimal curlLength = this.resolveCurlLength(task);
        if (planQty.compareTo(BigDecimal.ZERO) <= 0 || curlLength.compareTo(BigDecimal.ZERO) <= 0) {
            return planQty;
        }
        return planQty.divide(curlLength, 0, RoundingMode.CEILING).multiply(curlLength);
    }

    /**
     * 解析任务卷曲长度，优先使用任务快照，缺失时使用领域默认值。
     *
     * @param task 任务
     * @return 有效卷曲长度或零
     */
    private BigDecimal resolveCurlLength(T task) {
        BigDecimal curlLength = this.getCurlRollLength(task);
        if (curlLength != null && curlLength.compareTo(BigDecimal.ZERO) > 0) {
            return curlLength;
        }
        return this.nvl(this.getDefaultCurlRollLength(task));
    }

    /**
     * 计算班末交接库存。
     *
     * @param shiftStartStock 班初库存
     * @param currentDemand   当班需求量
     * @param finalPlanQty    最终计划量
     * @return 班末交接库存
     */
    private BigDecimal calculateHandoverStock(BigDecimal shiftStartStock, BigDecimal currentDemand,
                                               BigDecimal finalPlanQty) {
        return this.nvl(shiftStartStock).add(this.nvl(finalPlanQty))
                .subtract(this.nvl(currentDemand)).max(BigDecimal.ZERO);
    }

    /**
     * 将空数值归一为零。
     *
     * @param value 原始数值
     * @return 非空数值
     */
    protected BigDecimal nvl(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
