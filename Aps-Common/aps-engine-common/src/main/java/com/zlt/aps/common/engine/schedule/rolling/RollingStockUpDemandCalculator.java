package com.zlt.aps.common.engine.schedule.rolling;

import com.zlt.aps.common.core.utils.BigDecimalUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 备库未来窗口的纯计算器：以库存投影已消耗列为锚点，超八班按末三班均值外推。
 */
public class RollingStockUpDemandCalculator {
    private static final int SOURCE_SHIFT_COUNT = 8;
    private static final int AVERAGE_SHIFT_COUNT = 3;

    /**
     * 解析一个可信成型来源的未来备库需求，不读取或回写外部资源。
     *
     * @param quantities          来源CLASS1至CLASS8成型条数，空值与真实零分开处理
     * @param consumedShiftCount  已由当前库存投影消耗的成型班数
     * @param guardShiftCount     最终有效备库班数
     * @param unitLength          施工单位长度，米/条
     * @param remainingFormingQty 成型余量条数，空值沿用普通排程不封顶口径
     * @return 包含未来需求、封顶、实际班次与外推区间的只读快照
     */
    public RollingStockUpDemandSnapshot calculate(List<BigDecimal> quantities, int consumedShiftCount,
                                                  int guardShiftCount, BigDecimal unitLength,
                                                  BigDecimal remainingFormingQty) {
        RollingStockUpDemandSnapshot result = new RollingStockUpDemandSnapshot();
        if (quantities == null || quantities.size() != SOURCE_SHIFT_COUNT || consumedShiftCount < 0
                || guardShiftCount <= 0 || !AutomaticRollingPlanner.positive(unitLength)) {
            result.setSkipReason("STOCK_UP_WINDOW_INVALID");
            return result;
        }
        long endExclusive = (long) consumedShiftCount + guardShiftCount;
        long estimatedShiftCount = Math.max(0L, endExclusive - Math.max(consumedShiftCount, SOURCE_SHIFT_COUNT));
        List<Map<String, Object>> details = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;
        for (int index = Math.min(consumedShiftCount, SOURCE_SHIFT_COUNT);
             index < Math.min(endExclusive, SOURCE_SHIFT_COUNT); index++) {
            BigDecimal quantity = quantities.get(index);
            if (quantity == null || quantity.signum() < 0) {
                result.setSkipReason(quantity == null ? "STOCK_UP_DEMAND_SOURCE_MISSING" : "STOCK_UP_NEGATIVE_DEMAND");
                return result;
            }
            total = total.add(quantity);
            Map<String, Object> detail = new LinkedHashMap<>();
            detail.put("sourceClass", index + 1);
            detail.put("formingQty", quantity);
            detail.put("estimated", false);
            details.add(detail);
        }
        if (estimatedShiftCount > 0) {
            BigDecimal average = BigDecimal.ZERO;
            for (int index = SOURCE_SHIFT_COUNT - AVERAGE_SHIFT_COUNT; index < SOURCE_SHIFT_COUNT; index++) {
                BigDecimal quantity = quantities.get(index);
                if (quantity == null || quantity.signum() < 0) {
                    result.setSkipReason("STOCK_UP_ESTIMATE_SOURCE_INVALID");
                    return result;
                }
                average = average.add(quantity);
            }
            average = average.divide(BigDecimalUtils.valueOf(AVERAGE_SHIFT_COUNT), 6, RoundingMode.HALF_UP);
            total = total.add(average.multiply(BigDecimalUtils.valueOf(estimatedShiftCount)));
            Map<String, Object> detail = new LinkedHashMap<>();
            detail.put("sourceClassStart", Math.max(consumedShiftCount, SOURCE_SHIFT_COUNT) + 1L);
            detail.put("sourceClassEnd", endExclusive);
            detail.put("estimatedShiftCount", estimatedShiftCount);
            detail.put("formingQtyPerShift", average);
            detail.put("estimated", true);
            detail.put("estimateRule", "CLASS6_CLASS8_AVERAGE");
            details.add(detail);
        }
        BigDecimal capped = remainingFormingQty == null ? total : total.min(remainingFormingQty.max(BigDecimal.ZERO));
        result.setDemandQty(capped.multiply(unitLength));
        result.setComplete(true);
        result.getEvidence().put("windowStartClass", consumedShiftCount + 1L);
        result.getEvidence().put("windowEndClass", endExclusive);
        result.getEvidence().put("effectiveGuardShiftCount", guardShiftCount);
        result.getEvidence().put("rawFormingQty", total);
        result.getEvidence().put("remainingFormingQty", remainingFormingQty);
        result.getEvidence().put("cappedFormingQty", capped);
        result.getEvidence().put("unitLengthMeters", unitLength);
        result.getEvidence().put("demandQty", result.getDemandQty());
        result.getEvidence().put("classes", details);
        return result;
    }
}
