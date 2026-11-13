package com.zlt.aps.common.engine.schedule;

import com.zlt.aps.common.engine.schedule.engine.SchedulePlanQtyResultModel;
import com.zlt.aps.common.engine.schedule.engine.SchedulePostTailPlanQtyCalculator;
import com.zlt.aps.common.engine.schedule.engine.ScheduleTaskDraftModel;
import com.zlt.aps.common.engine.schedule.engine.TwoPhaseScheduleContext;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 公共默认计划量算法模板。
 *
 * <p>模板统一处理库存覆盖、基础需求扣库存、收尾限制、最小起排、卷数取整、
 * 交接库存和结果分量。TM/TC 通过字段及结果适配钩子提供领域对象，不在子类复制主流程。</p>
 *
 * @param <C> 排程上下文类型
 * @param <T> 待排任务类型
 * @param <R> 计划量结果类型
 */
public abstract class AbstractDefaultPlanQtyStrategy<C, T extends ScheduleTaskDraftModel,
        R extends SchedulePlanQtyResultModel> {

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
        boolean twoPhase = context instanceof TwoPhaseScheduleContext
                && ((TwoPhaseScheduleContext) context).isTwoPhaseEnabled();
        // 两阶段仍按配置窗口判断库存覆盖，基础请求另由两阶段请求计算器按当前班和下一班计算。
        boolean stockCoverageApplicable = twoPhase || this.isTwoShiftStockCoverageApplicable(task);
        if (stockCoverageApplicable) {
            BigDecimal coverageDemand = task.getStockCoverageDemandQty() == null
                    ? twoShiftDemand : this.nvl(task.getStockCoverageDemandQty());
            task.setStockCoverageDemandQty(coverageDemand);
            BigDecimal coverageStockGap = coverageDemand.subtract(stock);
            task.setStockCoverageStockGapQty(coverageStockGap);
            this.setTwoShiftStockCovered(task, twoShiftStockGap.compareTo(BigDecimal.ZERO) <= 0);
            if (coverageStockGap.compareTo(BigDecimal.ZERO) <= 0) {
                task.setStockCoverageCovered(Boolean.TRUE);
                R coveredResult = this.buildStockCoverageCoveredResult(task, currentDemand, coverageDemand, stock);
                if (twoPhase) {
                    this.setResultCalcFormulaDesc(coveredResult,
                            "配置库存覆盖窗口已覆盖，两阶段基础请求和备库目标分别按各自需求窗口计算");
                }
                return coveredResult;
            }
            task.setStockCoverageCovered(Boolean.FALSE);
        } else {
            task.setStockCoverageDemandQty(null);
            task.setStockCoverageCovered(null);
            task.setStockCoverageStockGapQty(null);
            this.setTwoShiftStockCovered(task, null);
        }

        if (Boolean.TRUE.equals(task.getPostTailShiftBlocked())) {
            return SchedulePostTailPlanQtyCalculator.calculate(task, this.newResult());
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
            // 已下发当日成型总余量额度时以额度为上限；未启用额度必须回退既有口径，
            // 直接 nvl(tailQuotaAvailableQty) 会让未配置余量的产品收尾上限变成零。
            // 额度本身已是米数（余量条数×单位长度），不能再乘单位长度；只有回退口径才需要折算。
            BigDecimal tailBaseQty;
            if (task.getTailQuotaAvailableQty() != null) {
                // 整数米取整允许实际产量比额度多不足1米，台账剩余额度可能为负；可执行量必须非负
                tailBaseQty = task.getTailQuotaAvailableQty().max(BigDecimal.ZERO);
            } else {
                tailBaseQty = this.nvl(this.getTailBalanceQty(task))
                        .multiply(this.nvl(this.getProductLength(task)));
            }
            BigDecimal cappedQty = planQty.min(tailBaseQty);
            preLossPlanQty = cappedQty;
            this.setResultTailRoundAdjustQty(result, cappedQty.subtract(beforeTail));

            BigDecimal tailMinPlanQty = this.nvl(this.getTailMinPlanQty(task));
            planQty = cappedQty;
            BigDecimal minStartAdjust = BigDecimal.ZERO;
            if (cappedQty.compareTo(BigDecimal.ZERO) > 0 && tailMinPlanQty.compareTo(BigDecimal.ZERO) > 0
                    && cappedQty.compareTo(tailMinPlanQty) < 0) {
                planQty = tailMinPlanQty;
                minStartAdjust = tailMinPlanQty.subtract(cappedQty);
            }
            this.setResultMinStartAdjustQty(result, minStartAdjust);
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
                task.getDemandOnlyMinimumQty() != null
                        ? com.ruoyi.common.i18n.utils.I18nUtil.getMessage("ui.schedule.demandOnly.estimateFormula")
                        : tailTask ? "收尾余量" : "基础需求->库存抵扣->派机前最小起排与卷数取整估算");
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
     * @return 当班需求量，默认从待排任务草稿获取
     */
    protected BigDecimal getCurrentShiftDemand(T task) {
        return task != null ? task.getCurrentShiftDemandQty() : null;
    }

    /**
     * 获取下一班需求量。
     *
     * @param task 待排任务
     * @return 下一班需求量，默认从待排任务草稿获取
     */
    protected BigDecimal getNextShiftDemand(T task) {
        return task != null ? task.getNextShiftDemandQty() : null;
    }

    /**
     * 获取库存保证需求量。
     *
     * @param task 待排任务
     * @return 库存保证需求量，默认从待排任务草稿获取
     */
    protected BigDecimal getGuardDemand(T task) {
        return task != null ? task.getGuardDemandQty() : null;
    }

    /**
     * 获取班初滚动库存。
     *
     * @param task 待排任务
     * @return 班初滚动库存，默认从待排任务草稿获取
     */
    protected BigDecimal getRollingStockQty(T task) {
        return task != null ? task.getRollingStockQty() : null;
    }

    /**
     * 判断是否为成型连续停产收尾任务。
     *
     * @param task 待排任务
     * @return 是否为停产收尾任务，默认读取待排任务草稿成型连续停产收尾标记
     */
    protected boolean isFormingShutdownCloseOut(T task) {
        return task != null && Boolean.TRUE.equals(task.getFormingShutdownCloseOutFlag());
    }

    /**
     * 判断是否应用库存覆盖门槛。
     *
     * <p>默认排除新规格提前排产与实验规格任务；领域子类可根据特殊需求重写。</p>
     *
     * @param task 待排任务
     * @return 是否应用库存覆盖
     */
    protected boolean isTwoShiftStockCoverageApplicable(T task) {
        if (task == null) {
            return false;
        }
        boolean newSpecAdvanceTask = task.getNewSpecInfo() != null
                && task.getNewSpecInfo().isNewSpecHit();
        boolean experimentSpecTask = task.getExperimentSpecInfo() != null
                && task.getExperimentSpecInfo().isExperimentSpecHit();
        return !newSpecAdvanceTask && !experimentSpecTask;
    }

    /**
     * 获取收尾余量。
     *
     * @param task 待排任务
     * @return 收尾余量，默认从待排任务草稿获取
     */
    protected BigDecimal getTailBalanceQty(T task) {
        return task != null ? task.getTailBalanceQty() : null;
    }

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
    protected boolean isTailTask(T task, BigDecimal baseDemandQty) {
        return task != null
                && "1".equals(task.getTailFlag())
                && this.nvl(this.getTailBalanceQty(task)).compareTo(BigDecimal.ZERO) > 0
                && this.nvl(this.getProductLength(task)).compareTo(BigDecimal.ZERO) > 0;
    }

    /**
     * 获取最小起排量。
     *
     * @param task 待排任务
     * @return 最小起排量，默认从待排任务草稿获取
     */
    protected BigDecimal getMinStartQty(T task) {
        return task != null ? task.getMinStartQty() : null;
    }

    /**
     * 获取收尾规格最小计划量。
     *
     * @param task 任务
     * @return 收尾规格最小计划量，默认尝试从公共模型获取，无法取得时返回空值
     */
    protected BigDecimal getTailMinPlanQty(T task) {
        return task != null ? task.getTailMinPlanQty() : null;
    }

    /**
     * 获取任务卷曲长度。
     *
     * @param task 待排任务
     * @return 任务卷曲长度，默认从待排任务草稿获取
     */
    protected BigDecimal getCurlRollLength(T task) {
        return task != null ? task.getCurlRollLength() : null;
    }

    /**
     * 获取默认卷曲长度。
     *
     * @param task 待排任务
     * @return 默认卷曲长度，默认从待排任务草稿获取
     */
    protected BigDecimal getDefaultCurlRollLength(T task) {
        return task != null ? task.getDefaultCurlRollLength() : null;
    }

    /**
     * 写入历史两班需求合计。
     *
     * @param task 任务
     * @param value 历史两班需求合计
     */
    protected void setTwoShiftDemandQty(T task, BigDecimal value) {
        if (task != null) {
            task.setTwoShiftDemandQty(value);
        }
    }

    /**
     * 写入历史两班库存缺口。
     *
     * @param task 任务
     * @param value 历史两班库存缺口
     */
    protected void setTwoShiftStockGapQty(T task, BigDecimal value) {
        if (task != null) {
            task.setTwoShiftStockGapQty(value);
        }
    }

    /**
     * 写入历史两班库存覆盖标识。
     *
     * @param task 任务
     * @param value 是否覆盖
     */
    protected void setTwoShiftStockCovered(T task, Boolean value) {
        if (task != null) {
            task.setTwoShiftStockCovered(value);
        }
    }

    /**
     * 写入库存抵扣量。
     *
     * @param task 任务
     * @param value 库存抵扣量
     */
    protected void setStockDeductQty(T task, BigDecimal value) {
        if (task != null) {
            task.setStockDeductQty(value);
        }
    }

    /**
     * 写入交接库存。
     *
     * @param task 任务
     * @param value 交接库存
     */
    protected void setPlanStockQty(T task, BigDecimal value) {
        if (task != null) {
            task.setPlanStockQty(value);
        }
    }

    /**
     * 写入最终计划量。
     *
     * @param task 任务
     * @param value 最终计划量
     */
    protected void setPlanQty(T task, BigDecimal value) {
        if (task != null) {
            task.setPlanQty(value);
        }
    }

    /**
     * 设置结果基础需求量。
     *
     * @param result 计划量结果
     * @param value 基础需求量
     */
    protected void setResultBaseDemandQty(R result, BigDecimal value) {
        if (result != null) {
            result.setBaseDemandQty(value);
        }
    }

    /**
     * 设置结果损耗补偿量。
     *
     * @param result 计划量结果
     * @param value 损耗补偿量
     */
    protected void setResultLossAddQty(R result, BigDecimal value) {
        if (result != null) {
            result.setLossAddQty(value);
        }
    }

    /**
     * 设置结果最小起排调整量。
     *
     * @param result 计划量结果
     * @param value 最小起排调整量
     */
    protected void setResultMinStartAdjustQty(R result, BigDecimal value) {
        if (result != null) {
            result.setMinStartAdjustQty(value);
        }
    }

    /**
     * 设置结果卷数取整调整量。
     *
     * @param result 计划量结果
     * @param value 卷数取整调整量
     */
    protected void setResultTailRoundAdjustQty(R result, BigDecimal value) {
        if (result != null) {
            result.setTailRoundAdjustQty(value);
        }
    }

    /**
     * 设置结果工装限制调整量。
     *
     * @param result 计划量结果
     * @param value 工装限制调整量
     */
    protected void setResultToolLimitAdjustQty(R result, BigDecimal value) {
        if (result != null) {
            result.setToolLimitAdjustQty(value);
        }
    }

    /**
     * 设置结果工装溢出量。
     *
     * @param result 计划量结果
     * @param value 工装溢出量
     */
    protected void setResultToolOverflowQty(R result, BigDecimal value) {
        if (result != null) {
            result.setToolOverflowQty(value);
        }
    }

    /**
     * 设置结果产能调整量。
     *
     * @param result 计划量结果
     * @param value 产能调整量
     */
    protected void setResultCapacityAdjustQty(R result, BigDecimal value) {
        if (result != null) {
            result.setCapacityAdjustQty(value);
        }
    }

    /**
     * 设置结果损耗前计划量。
     *
     * @param result 计划量结果
     * @param value 损耗前计划量
     */
    protected void setResultPreLossPlanQty(R result, BigDecimal value) {
        if (result != null) {
            result.setPreLossPlanQty(value);
        }
    }

    /**
     * 设置结果工装限制前计划量。
     *
     * @param result 计划量结果
     * @param value 工装限制前计划量
     */
    protected void setResultPlanQtyBeforeToolLimit(R result, BigDecimal value) {
        if (result != null) {
            result.setPlanQtyBeforeToolLimit(value);
        }
    }

    /**
     * 设置结果最终计划量。
     *
     * @param result 计划量结果
     * @param value 最终计划量
     */
    protected void setResultFinalPlanQty(R result, BigDecimal value) {
        if (result != null) {
            result.setFinalPlanQty(value);
        }
    }

    /**
     * 设置结果计算说明。
     *
     * @param result 计划量结果
     * @param value 计算说明
     */
    protected void setResultCalcFormulaDesc(R result, String value) {
        if (result != null) {
            result.setCalcFormulaDesc(value);
        }
    }

    /**
     * 计算停产收尾任务计划量。
     *
     * @param task 任务
     * @param stock 班初滚动库存
     * @return 停产收尾计划量结果
     */
    private R calculateFormingShutdownCloseOut(T task, BigDecimal stock) {
        task.setStockCoverageCovered(null);
        task.setStockCoverageStockGapQty(null);
        BigDecimal closeOutDemandQty = this.nvl(this.getFormingShutdownCloseOutDemandQty(task));
        BigDecimal stockDeductQty = stock.min(closeOutDemandQty);
        BigDecimal basePlanQty = closeOutDemandQty.subtract(stock).max(BigDecimal.ZERO);
        BigDecimal tailLimitQty = this.nvl(this.getFormingShutdownCloseOutTailLimitQty(task));
        BigDecimal cappedQty = basePlanQty.min(tailLimitQty);
        BigDecimal tailRoundAdjust = cappedQty.subtract(basePlanQty);

        BigDecimal preLossPlanQty = cappedQty;
        BigDecimal planQty = cappedQty;
        BigDecimal minStartAdjust = BigDecimal.ZERO;
        BigDecimal tailMinPlanQty = this.nvl(this.getTailMinPlanQty(task));
        if (cappedQty.compareTo(BigDecimal.ZERO) > 0 && tailMinPlanQty.compareTo(BigDecimal.ZERO) > 0
                && cappedQty.compareTo(tailMinPlanQty) < 0) {
            planQty = tailMinPlanQty;
            minStartAdjust = tailMinPlanQty.subtract(cappedQty);
        }

        this.setTwoShiftDemandQty(task, closeOutDemandQty);
        this.setTwoShiftStockGapQty(task, closeOutDemandQty.subtract(stock));
        this.setTwoShiftStockCovered(task, planQty.compareTo(BigDecimal.ZERO) == 0);
        this.setStockDeductQty(task, stockDeductQty);
        this.setPlanStockQty(task, stock.add(planQty).subtract(closeOutDemandQty).max(BigDecimal.ZERO));
        this.setPlanQty(task, planQty);

        R result = this.newResult();
        this.initializeZeroAdjustments(result);
        this.setResultBaseDemandQty(result, basePlanQty);
        this.setResultTailRoundAdjustQty(result, tailRoundAdjust);
        this.setResultMinStartAdjustQty(result, minStartAdjust);
        this.setResultPreLossPlanQty(result, preLossPlanQty);
        this.setResultPlanQtyBeforeToolLimit(result, planQty);
        this.setResultFinalPlanQty(result, planQty);
        this.setResultCalcFormulaDesc(result, minStartAdjust.signum() > 0
                ? "成型连续停产收尾需求->库存抵扣->收尾上限->收尾最小计划量补齐"
                : "成型连续停产收尾需求->库存抵扣->收尾上限");
        return result;
    }

    /**
     * 获取停产收尾需求量。
     *
     * @param task 任务
     * @return 停产收尾需求量，默认从待排任务草稿获取
     */
    protected BigDecimal getFormingShutdownCloseOutDemandQty(T task) {
        return task != null ? task.getFormingShutdownCloseOutDemandQty() : null;
    }

    /**
     * 获取成型连续停产收尾的收尾上限；成型余量无效时回退到停产前原始计划汇总。
     *
     * @param task 任务
     * @return 收尾上限，无法取得时返回空值
     */
    protected BigDecimal getFormingShutdownCloseOutTailLimitQty(T task) {
        if (task == null) {
            return BigDecimal.ZERO;
        }
        BigDecimal configuredTailLimitQty = this.nvl(this.getTailBalanceQty(task))
                .multiply(this.nvl(this.getProductLength(task)));
        if (configuredTailLimitQty.compareTo(BigDecimal.ZERO) > 0) {
            return configuredTailLimitQty;
        }
        int lastOpenFormingShiftOrder = task.getFormingLogicalShiftOrder() == null
                ? 0 : task.getFormingLogicalShiftOrder();
        if (task.getFormingClassQtyMap() == null || task.getFormingClassQtyMap().isEmpty()) {
            return BigDecimal.ZERO;
        }
        BigDecimal formingPlanQty = task.getFormingClassQtyMap().entrySet().stream()
                .filter(entry -> entry.getKey() != null && entry.getKey() <= lastOpenFormingShiftOrder)
                .map(entry -> this.nvl(entry.getValue()))
                .filter(value -> value.compareTo(BigDecimal.ZERO) > 0)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return formingPlanQty.multiply(this.nvl(this.getProductLength(task)));
    }

    /**
     * 构建库存覆盖已命中的零计划结果。
     *
     * @param task            任务
     * @param currentDemand  当班需求量
     * @param coverageDemand 覆盖窗口需求合计
     * @param stock           班初滚动库存
     * @return 零计划结果
     */
    private R buildStockCoverageCoveredResult(T task, BigDecimal currentDemand,
                                              BigDecimal coverageDemand, BigDecimal stock) {
        BigDecimal zero = BigDecimal.ZERO;
        this.setStockDeductQty(task, stock.min(coverageDemand));
        this.setPlanStockQty(task, stock.subtract(currentDemand).max(zero));
        task.setLossAddQty(zero);
        task.setToolLimitAdjustQty(zero);
        task.setToolOverflowQty(zero);
        task.setMinStartAdjustQty(zero);
        task.setTailRoundAdjustQty(zero);
        task.setCapacityAdjustQty(zero);
        task.setBaseDemandQty(zero);
        task.setPreLossPlanQty(zero);
        task.setPlanQtyBeforeToolLimit(zero);
        this.clearAdditionalAdjustments(task);
        this.setPlanQty(task, zero);

        R result = this.newResult();
        this.setResultBaseDemandQty(result, zero);
        this.initializeZeroAdjustments(result);
        this.setResultPreLossPlanQty(result, zero);
        this.setResultPlanQtyBeforeToolLimit(result, zero);
        this.setResultFinalPlanQty(result, zero);
        Integer coverageShiftCount = task.getStockCoverageWindow() == null
                ? null : task.getStockCoverageWindow().getShiftCount();
        String coverageDescription = coverageShiftCount == null
                ? "库存覆盖需求已由班初滚动库存覆盖，当班无需排产"
                : coverageShiftCount + "班需求已由班初滚动库存覆盖，当班无需排产";
        this.setResultCalcFormulaDesc(result, coverageDescription);
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
     * 清理领域策略扩展的计划量调整分量。
     *
     * <p>公共模型只包含 TM/TC 共用字段；领域策略可覆盖此方法清理本模块专有调整量。</p>
     *
     * @param task 已命中库存覆盖的任务
     */
    protected void clearAdditionalAdjustments(T task) {
        // 默认无额外调整分量。
    }

    /**
     * 应用最小起排量规则。
     *
     * @param task    任务
     * @param planQty 当前计划量
     * @return 调整后的计划量
     */
    private BigDecimal applyMinStartQty(T task, BigDecimal planQty) {
        BigDecimal minStartQty = task.getDemandOnlyMinimumQty() == null
                ? this.nvl(this.getMinStartQty(task)) : task.getDemandOnlyMinimumQty();
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
        if (task.getDemandOnlyMinimumQty() != null) {
            return planQty.setScale(0, RoundingMode.CEILING);
        }
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
