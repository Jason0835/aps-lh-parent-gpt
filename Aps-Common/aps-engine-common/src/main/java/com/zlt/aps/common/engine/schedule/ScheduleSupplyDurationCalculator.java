package com.zlt.aps.common.engine.schedule;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 库存供应时长逐班计算器。
 *
 * <p>按成型班次顺序逐班扣减库存。完整覆盖某班时累计该班实际时长；
 * 首个不能完整覆盖的班次按剩余库存占该班需求的比例折算时长并停止。</p>
 */
public final class ScheduleSupplyDurationCalculator {

    private static final int RESULT_SCALE = 2;

    private static final int CALCULATION_SCALE = 8;

    private ScheduleSupplyDurationCalculator() {
    }

    /**
     * 逐班计算库存供应时长。
     *
     * @param rollingStockQty 班初滚动库存，单位米
     * @param shiftDemandQtyMap 有序的成型班次需求量明细，键为逻辑班次
     * @param shiftHoursMap 与需求明细同键的实际班次时长
     * @return 供应时长计算结果；明细缺失或无效时返回带原因的空结果
     */
    public static ScheduleSupplyDurationResult calculate(BigDecimal rollingStockQty,
                                                          Map<Integer, BigDecimal> shiftDemandQtyMap,
                                                          Map<Integer, BigDecimal> shiftHoursMap) {
        ScheduleSupplyDurationResult result = new ScheduleSupplyDurationResult();
        String invalidReason = validateWindow(shiftDemandQtyMap, shiftHoursMap);
        if (invalidReason != null) {
            result.setInvalidReason(invalidReason);
            result.setCalculationDetail("库存供应时长未计算：" + invalidReason + "。");
            return result;
        }

        BigDecimal remainingStockQty = nvl(rollingStockQty).max(BigDecimal.ZERO);
        BigDecimal accumulatedHours = BigDecimal.ZERO;
        List<String> stepDetails = new ArrayList<>();
        for (Map.Entry<Integer, BigDecimal> demandEntry : shiftDemandQtyMap.entrySet()) {
            Integer shiftOrder = demandEntry.getKey();
            BigDecimal demandQty = demandEntry.getValue();
            BigDecimal shiftHours = shiftHoursMap.get(shiftOrder);
            if (demandQty.compareTo(BigDecimal.ZERO) == 0) {
                accumulatedHours = accumulatedHours.add(shiftHours);
                stepDetails.add("班" + shiftOrder + "需求=0米，不消耗库存，累计实际时长"
                        + display(shiftHours) + "H");
                continue;
            }
            if (remainingStockQty.compareTo(demandQty) >= 0) {
                BigDecimal beforeStockQty = remainingStockQty;
                remainingStockQty = remainingStockQty.subtract(demandQty);
                accumulatedHours = accumulatedHours.add(shiftHours);
                stepDetails.add("班" + shiftOrder + "完整覆盖：库存" + display(beforeStockQty)
                        + "米-需求" + display(demandQty) + "米=" + display(remainingStockQty)
                        + "米，累计实际时长+" + display(shiftHours) + "H");
                continue;
            }
            BigDecimal partialHours = remainingStockQty.divide(demandQty, CALCULATION_SCALE, RoundingMode.HALF_UP)
                    .multiply(shiftHours);
            accumulatedHours = accumulatedHours.add(partialHours);
            stepDetails.add("班" + shiftOrder + "部分覆盖：剩余库存" + display(remainingStockQty)
                    + "米÷该班需求" + display(demandQty) + "米×该班实际时长"
                    + display(shiftHours) + "H=" + display(partialHours) + "H，随后停止");
            result.setSupplyHours(accumulatedHours.setScale(RESULT_SCALE, RoundingMode.HALF_UP));
            result.setCalculationDetail(buildDetail(rollingStockQty, stepDetails, result.getSupplyHours(), false));
            return result;
        }

        result.setFullWindowCovered(true);
        result.setSupplyHours(accumulatedHours.setScale(RESULT_SCALE, RoundingMode.HALF_UP));
        result.setCalculationDetail(buildDetail(rollingStockQty, stepDetails, result.getSupplyHours(), true));
        return result;
    }

    /**
     * 校验逐班需求和时长窗口。
     *
     * @param shiftDemandQtyMap 逐班需求量
     * @param shiftHoursMap 逐班实际时长
     * @return 无效原因；有效时返回空
     */
    private static String validateWindow(Map<Integer, BigDecimal> shiftDemandQtyMap,
                                         Map<Integer, BigDecimal> shiftHoursMap) {
        if (shiftDemandQtyMap == null || shiftDemandQtyMap.isEmpty()) {
            return "缺少逐班成型需求明细";
        }
        if (shiftHoursMap == null || shiftHoursMap.isEmpty()) {
            return "缺少逐班实际时长明细";
        }
        for (Map.Entry<Integer, BigDecimal> demandEntry : shiftDemandQtyMap.entrySet()) {
            if (demandEntry.getKey() == null || demandEntry.getValue() == null
                    || demandEntry.getValue().compareTo(BigDecimal.ZERO) < 0) {
                return "逐班成型需求明细无效";
            }
            BigDecimal shiftHours = shiftHoursMap.get(demandEntry.getKey());
            if (shiftHours == null || shiftHours.compareTo(BigDecimal.ZERO) <= 0) {
                return "班" + demandEntry.getKey() + "实际时长缺失或非正数";
            }
        }
        return null;
    }

    /**
     * 组装逐班计算说明。
     *
     * @param rollingStockQty 班初库存
     * @param stepDetails 逐班明细
     * @param supplyHours 最终供应时长
     * @param fullWindowCovered 是否覆盖完整窗口
     * @return 中文计算过程
     */
    private static String buildDetail(BigDecimal rollingStockQty, List<String> stepDetails,
                                      BigDecimal supplyHours, boolean fullWindowCovered) {
        return "库存供应逐班计算：班初滚动库存=" + display(nvl(rollingStockQty).max(BigDecimal.ZERO))
                + "米；" + String.join("；", stepDetails) + "；库存供应时长="
                + display(supplyHours) + "H" + (fullWindowCovered ? "（至少覆盖整个保证窗口）" : "") + "。";
    }

    /**
     * 空值转零。
     *
     * @param value 原始数值
     * @return 非空数值
     */
    private static BigDecimal nvl(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    /**
     * 格式化日志数值。
     *
     * @param value 数值
     * @return 去除无意义末尾零的文本
     */
    private static String display(BigDecimal value) {
        return nvl(value).stripTrailingZeros().toPlainString();
    }
}
