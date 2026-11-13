package com.zlt.aps.common.core.utils;

import com.zlt.aps.common.core.domain.vo.AutomaticRollingDecision;
import com.zlt.aps.common.core.domain.vo.AutomaticRollingDemandWindowResult;
import com.zlt.aps.common.core.domain.vo.AutomaticRollingPlanRow;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

/**
 * TM、TC 定时自动滚动共用的阈值公式、需求窗口累计和数量分配纯计算能力。
 */
public final class AutomaticRollingCalculationUtils {

    /**
     * 结果表计划数量精度。
     */
    private static final int QTY_SCALE = 2;

    private AutomaticRollingCalculationUtils() {
    }

    /**
     * 按实际逐班需求计算指定班数窗口的累计需求。
     *
     * @param shiftDemandQtyList 从下一物理班开始的逐班需求；空列表、空数量及超出列表的需求按零处理
     * @param shiftCount         窗口班数，可包含小数
     * @return 累计需求；仅班数为空或非正数时 complete 为 false
     */
    public static AutomaticRollingDemandWindowResult accumulateDemand(List<BigDecimal> shiftDemandQtyList,
                                                                      BigDecimal shiftCount) {
        AutomaticRollingDemandWindowResult result = new AutomaticRollingDemandWindowResult();
        result.setDemandQty(BigDecimal.ZERO);
        result.setComplete(false);
        if (shiftCount == null || shiftCount.signum() <= 0) {
            return result;
        }
        List<BigDecimal> demands = shiftDemandQtyList == null ? java.util.Collections.emptyList()
                : shiftDemandQtyList;
        BigDecimal fullShiftCount = shiftCount.setScale(0, RoundingMode.DOWN);
        BigDecimal fraction = shiftCount.subtract(fullShiftCount);
        // 只遍历实际存在的班次，窗口之外的缺失需求无需构造大量零值。
        int existingFullShiftCount = fullShiftCount.min(BigDecimalUtils.valueOf(demands.size())).intValue();
        BigDecimal demandQty = BigDecimal.ZERO;
        for (int index = 0; index < existingFullShiftCount; index++) {
            demandQty = demandQty.add(value(demands.get(index)));
        }
        if (fraction.signum() > 0 && fullShiftCount.compareTo(BigDecimalUtils.valueOf(demands.size())) < 0) {
            demandQty = demandQty.add(value(demands.get(fullShiftCount.intValue())).multiply(fraction));
        }
        result.setDemandQty(demandQty);
        result.setComplete(true);
        return result;
    }

    /**
     * 根据库存、有效计算侧计划和完成量执行统一上下修公式，双侧齐全时使用主侧。
     *
     * @param expectedStock    目标班预计库存，可为负数
     * @param primaryPlanQty   有效计算侧滚动前目标班计划量
     * @param primaryFinishQty 有效计算侧目标班完成量
     * @param upDemand         上修阈值需求窗口
     * @param downDemand       下修阈值需求窗口
     * @param downTargetDemand 下修目标需求窗口
     * @return 方向、公式目标和计算侧完成量约束后的最终目标
     */
    public static AutomaticRollingDecision decide(BigDecimal expectedStock, BigDecimal primaryPlanQty,
                                                  BigDecimal primaryFinishQty,
                                                  AutomaticRollingDemandWindowResult upDemand,
                                                  AutomaticRollingDemandWindowResult downDemand,
                                                  AutomaticRollingDemandWindowResult downTargetDemand) {
        if (upDemand == null || !upDemand.isComplete()) {
            throw new IllegalArgumentException("上修需求窗口不完整");
        }
        BigDecimal stockQty = value(expectedStock);
        BigDecimal planQty = value(primaryPlanQty);
        BigDecimal finishQty = value(primaryFinishQty);
        BigDecimal availableQty = stockQty.add(planQty);
        BigDecimal formulaPlanQty = planQty;
        String direction = "NONE";
        if (availableQty.compareTo(upDemand.getDemandQty()) < 0) {
            formulaPlanQty = upDemand.getDemandQty().subtract(stockQty);
            direction = "UP";
        } else if (downDemand != null && downDemand.isComplete()
                && downTargetDemand != null && downTargetDemand.isComplete()
                && availableQty.compareTo(downDemand.getDemandQty()) > 0) {
            formulaPlanQty = downTargetDemand.getDemandQty().subtract(stockQty);
            direction = "DOWN";
        }
        BigDecimal targetPlanQty = formulaPlanQty.max(BigDecimal.ZERO).max(finishQty)
                .setScale(QTY_SCALE, RoundingMode.HALF_UP);
        AutomaticRollingDecision decision = new AutomaticRollingDecision();
        decision.setDirection(direction);
        decision.setUpDemandQty(upDemand.getDemandQty());
        decision.setDownDemandQty(downDemand == null ? null : downDemand.getDemandQty());
        decision.setDownTargetDemandQty(downTargetDemand == null ? null : downTargetDemand.getDemandQty());
        decision.setFormulaPlanQty(formulaPlanQty.setScale(QTY_SCALE, RoundingMode.HALF_UP));
        decision.setTargetPlanQty(targetPlanQty);
        return decision;
    }

    /**
     * 按稳定顺序把目标总量分配到已有结果行。
     *
     * @param rows             已有物料结果行快照
     * @param targetPlanQty    目标计划合计
     * @param respectFinishQty 计算侧传true；双侧齐全时同步侧传false，仅同步侧存在时传true
     * @return 按稳定排序返回的行级分配结果
     */
    public static List<AutomaticRollingPlanRow> distributePlanQty(List<AutomaticRollingPlanRow> rows,
                                                                  BigDecimal targetPlanQty,
                                                                  boolean respectFinishQty) {
        if (rows == null || rows.isEmpty()) {
            throw new IllegalArgumentException("物料结果行为空，不能分配滚动计划量");
        }
        BigDecimal normalizedTargetQty = value(targetPlanQty).max(BigDecimal.ZERO)
                .setScale(QTY_SCALE, RoundingMode.HALF_UP);
        List<AutomaticRollingPlanRow> orderedRows = new ArrayList<>(rows);
        orderedRows.sort(Comparator
                .comparing((AutomaticRollingPlanRow row) -> !hasPositiveTargetValue(row))
                .thenComparing(AutomaticRollingPlanRow::getSequence, Comparator.nullsLast(Integer::compareTo))
                .thenComparing(AutomaticRollingPlanRow::getResultId, Comparator.nullsLast(Long::compareTo)));
        BigDecimal allocatedTotalQty = BigDecimal.ZERO;
        for (AutomaticRollingPlanRow row : orderedRows) {
            BigDecimal planQty = value(row.getPlanQty());
            BigDecimal minimumQty = respectFinishQty ? value(row.getFinishQty()).max(BigDecimal.ZERO)
                    .setScale(QTY_SCALE, RoundingMode.CEILING)
                    : BigDecimal.ZERO;
            BigDecimal allocatedQty = planQty.max(minimumQty).setScale(QTY_SCALE, RoundingMode.HALF_UP);
            row.setAllocatedPlanQty(allocatedQty);
            allocatedTotalQty = allocatedTotalQty.add(allocatedQty);
        }
        BigDecimal changeQty = normalizedTargetQty.subtract(allocatedTotalQty);
        if (changeQty.signum() > 0) {
            AutomaticRollingPlanRow selectedRow = orderedRows.get(0);
            selectedRow.setAllocatedPlanQty(selectedRow.getAllocatedPlanQty().add(changeQty));
        } else if (changeQty.signum() < 0) {
            BigDecimal remainingReduceQty = changeQty.abs();
            for (int index = orderedRows.size() - 1; index >= 0 && remainingReduceQty.signum() > 0; index--) {
                AutomaticRollingPlanRow row = orderedRows.get(index);
                BigDecimal minimumQty = respectFinishQty ? value(row.getFinishQty()).max(BigDecimal.ZERO)
                        .setScale(QTY_SCALE, RoundingMode.CEILING)
                        : BigDecimal.ZERO;
                BigDecimal reducibleQty = row.getAllocatedPlanQty().subtract(minimumQty).max(BigDecimal.ZERO);
                BigDecimal reduceQty = reducibleQty.min(remainingReduceQty);
                row.setAllocatedPlanQty(row.getAllocatedPlanQty().subtract(reduceQty));
                remainingReduceQty = remainingReduceQty.subtract(reduceQty);
            }
            if (remainingReduceQty.signum() > 0) {
                throw new IllegalArgumentException("目标计划量低于结果行完成量下限");
            }
        }
        BigDecimal actualTotalQty = orderedRows.stream().map(AutomaticRollingPlanRow::getAllocatedPlanQty)
                .filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);
        if (actualTotalQty.compareTo(normalizedTargetQty) != 0) {
            throw new IllegalStateException("结果行计划分配合计与目标数量不一致");
        }
        return orderedRows;
    }

    /**
     * 对计算侧已分配的各结果行补偿损耗，并对有效卷长的非收尾行向上取整。
     *
     * @param rows 已完成阈值目标分配的计算侧结果行
     * @return 结算后的结果行；行级allocatedPlanQty更新为最终计划量
     * @throws IllegalArgumentException 结果行集合为空或基础分配数量为空时抛出
     */
    public static List<AutomaticRollingPlanRow> settleLossAndCurl(List<AutomaticRollingPlanRow> rows) {
        if (rows == null || rows.isEmpty()) {
            throw new IllegalArgumentException("物料结果行为空，不能结算损耗和卷长");
        }
        for (AutomaticRollingPlanRow row : rows) {
            BigDecimal baseQty = row.getAllocatedPlanQty();
            if (baseQty == null) {
                throw new IllegalArgumentException("结果行基础计划量为空，不能结算损耗和卷长");
            }
            BigDecimal lossRate = value(row.getLossRate()).max(BigDecimal.ZERO);
            BigDecimal lossAddQty = baseQty.multiply(lossRate)
                    .divide(BigDecimal.valueOf(100), 6, RoundingMode.HALF_UP);
            BigDecimal afterLossQty = baseQty.add(lossAddQty).setScale(6, RoundingMode.HALF_UP);
            BigDecimal finalQty = afterLossQty;
            BigDecimal roundAddQty = BigDecimal.ZERO;
            BigDecimal curlLength = row.getCurlLength();
            if (!row.isTailTask() && baseQty.signum() > 0 && curlLength != null
                    && curlLength.signum() > 0) {
                finalQty = afterLossQty.divide(curlLength, 0, RoundingMode.CEILING)
                        .multiply(curlLength).setScale(2, RoundingMode.HALF_UP);
                roundAddQty = finalQty.subtract(afterLossQty).max(BigDecimal.ZERO)
                        .setScale(6, RoundingMode.HALF_UP);
                row.setCurlRoundSkipReason(null);
            } else {
                finalQty = finalQty.setScale(2, RoundingMode.HALF_UP);
                row.setCurlRoundSkipReason(row.isTailTask() ? "TAIL_TASK" :
                        baseQty.signum() <= 0 ? "ZERO_BASE_QTY" : "CURL_LENGTH_UNAVAILABLE");
            }
            row.setLossAddQty(lossAddQty.setScale(6, RoundingMode.HALF_UP));
            row.setCurlRoundAddQty(roundAddQty);
            row.setFinalPlanQty(finalQty);
            row.setAllocatedPlanQty(finalQty);
        }
        return rows;
    }

    /**
     * 汇总行级分配或结算后的计划量。
     *
     * @param rows 已分配的计划行
     * @return 行级计划量合计，空集合返回零
     */
    public static BigDecimal sumAllocatedPlanQty(List<AutomaticRollingPlanRow> rows) {
        return (rows == null ? Collections.<AutomaticRollingPlanRow>emptyList() : rows).stream()
                .map(AutomaticRollingPlanRow::getAllocatedPlanQty)
                .map(AutomaticRollingCalculationUtils::value)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * 判断目标班是否已有正计划或完成量，作为加量优先行的排序条件。
     *
     * @param row 目标班分配行
     * @return 计划或完成量为正时返回true
     */
    private static boolean hasPositiveTargetValue(AutomaticRollingPlanRow row) {
        return value(row.getPlanQty()).signum() > 0 || value(row.getFinishQty()).signum() > 0;
    }

    /**
     * 将空数量按业务规则归一为零。
     *
     * @param quantity 原始数量
     * @return 非空数量或零
     */
    private static BigDecimal value(BigDecimal quantity) {
        return quantity == null ? BigDecimal.ZERO : quantity;
    }

}
