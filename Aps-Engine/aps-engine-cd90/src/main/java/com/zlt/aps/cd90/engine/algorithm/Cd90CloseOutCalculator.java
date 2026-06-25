package com.zlt.aps.cd90.engine.algorithm;

import com.zlt.aps.cd90.engine.model.Cd90CloseOutDecision;
import com.zlt.aps.cd90.engine.model.Cd90EmbryoCloseOutItem;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 直裁收尾规格判定器。
 */
@Component
public class Cd90CloseOutCalculator {

    /**
     * 使用月计划剩余量判定收尾规格。
     *
     * <p>月计划剩余量只参与收尾标识判断，不截断后续实际排产量。</p>
     *
     * @param planSurplusQuantity 月计划剩余量，允许为空
     * @param netDemandQuantity 净需求量
     * @return 收尾判定结果
     */
    public Cd90CloseOutDecision decide(BigDecimal planSurplusQuantity, BigDecimal netDemandQuantity) {
        if (netDemandQuantity == null || netDemandQuantity.signum() < 0) {
            throw new IllegalArgumentException("净需求量不能小于0");
        }
        if (planSurplusQuantity == null) {
            return Cd90CloseOutDecision.builder()
                    .closeOut(false)
                    .missingPlanSurplusWarning(true)
                    .embryoItems(Collections.emptyList())
                    .build();
        }
        if (planSurplusQuantity.signum() < 0) {
            throw new IllegalArgumentException("月计划剩余量不能小于0");
        }
        return Cd90CloseOutDecision.builder()
                .closeOut(planSurplusQuantity.compareTo(netDemandQuantity) <= 0)
                .missingPlanSurplusWarning(false)
                .embryoItems(Collections.emptyList())
                .build();
    }

    /**
     * 按胎胚逐项比较计划数和月计划剩余量，全部达到才判定直裁规格收尾。
     */
    public Cd90CloseOutDecision decide(List<Cd90EmbryoCloseOutItem> items) {
        List<Cd90EmbryoCloseOutItem> source = items == null ? Collections.emptyList() : items;
        if (source.isEmpty()) {
            return Cd90CloseOutDecision.builder().closeOut(false)
                    .missingPlanSurplusWarning(true).embryoItems(new ArrayList<>()).build();
        }
        List<Cd90EmbryoCloseOutItem> details = source.stream().map(item -> {
            BigDecimal plan = item == null ? null : item.getCalculatedPlanQuantity();
            BigDecimal surplus = item == null ? null : item.getPlanSurplusQuantity();
            boolean reached = plan != null && plan.signum() >= 0 && surplus != null
                    && surplus.signum() >= 0 && plan.compareTo(surplus) >= 0;
            return Cd90EmbryoCloseOutItem.builder()
                    .embryoCode(item == null ? null : item.getEmbryoCode())
                    .calculatedPlanQuantity(plan).planSurplusQuantity(surplus)
                    .reached(reached).build();
        }).collect(Collectors.toList());
        boolean missing = details.stream().anyMatch(item -> item.getPlanSurplusQuantity() == null);
        boolean closeOut = !missing && details.stream().allMatch(Cd90EmbryoCloseOutItem::isReached);
        return Cd90CloseOutDecision.builder().closeOut(closeOut)
                .missingPlanSurplusWarning(missing).embryoItems(details).build();
    }
}
