package com.zlt.aps.cd90.engine.algorithm;

import com.zlt.aps.cd90.engine.model.Cd90CloseOutDecision;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

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
                    .build();
        }
        if (planSurplusQuantity.signum() < 0) {
            throw new IllegalArgumentException("月计划剩余量不能小于0");
        }
        return Cd90CloseOutDecision.builder()
                .closeOut(planSurplusQuantity.compareTo(netDemandQuantity) <= 0)
                .missingPlanSurplusWarning(false)
                .build();
    }
}
