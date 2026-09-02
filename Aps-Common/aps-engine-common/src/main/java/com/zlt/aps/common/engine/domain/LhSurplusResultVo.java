package com.zlt.aps.common.engine.domain;

import com.zlt.aps.mp.api.domain.entity.FactoryMonthPlanProductionFinalResult;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Optional;

/**
 * Sku硫化余量计算结果信息
 *
 * @author ZLT
 * @date 20260830
 */
@Getter
public class LhSurplusResultVo implements Serializable {
    /**
     * Sku信息
     */
    private FactoryMonthPlanProductionFinalResult skuInfo;
    /**
     * 上个月超欠产量
     */
    private Integer monthOverdueQty;
    /**
     * 当前计划量
     */
    private Integer planQty;
    /**
     * 总计划量
     */
    private Integer sumPlanQty;
    /**
     * 完成量
     */
    private Integer finishedQty;


    public LhSurplusResultVo(FactoryMonthPlanProductionFinalResult skuInfo, Integer monthOverdueQty, Integer planQty, Integer sumPlanQty, Integer finishedQty) {
        this.skuInfo = skuInfo;
        this.monthOverdueQty = monthOverdueQty;
        this.planQty = planQty;
        this.sumPlanQty = sumPlanQty;
        this.finishedQty = finishedQty;
    }

    /**
     * 余量信息
     *
     * @return
     */
    public Integer getSurplusQty() {
        if (null == skuInfo || StringUtils.isBlank(skuInfo.getMaterialCode())) {
            return BigDecimal.ZERO.intValue();
        }
        Integer planQty = Optional.ofNullable(this.planQty).orElse(BigDecimal.ZERO.intValue());
        Integer finishedQty = Optional.ofNullable(this.finishedQty).orElse(BigDecimal.ZERO.intValue());
        Integer lastMonthOverDueQty = Optional.ofNullable(this.monthOverdueQty).orElse(BigDecimal.ZERO.intValue());
        Integer surplusQty = planQty - finishedQty + lastMonthOverDueQty;
        if (null == surplusQty) {
            return BigDecimal.ZERO.intValue();
        }
        return surplusQty;
    }

    /**
     * 总的有效计划量，加上上月超欠产
     *
     * @return
     */
    public Integer getAllPlanQty() {
        if (null == skuInfo || StringUtils.isBlank(skuInfo.getMaterialCode())) {
            return BigDecimal.ZERO.intValue();
        }
        Integer planQty = Optional.ofNullable(this.planQty).orElse(BigDecimal.ZERO.intValue());
        Integer lastMonthOverDueQty = Optional.ofNullable(this.monthOverdueQty).orElse(BigDecimal.ZERO.intValue());
        Integer sum = planQty + lastMonthOverDueQty;
        if (null == sum) {
            return BigDecimal.ZERO.intValue();
        }
        return sum;
    }

    /**
     * 总的计划量，加上上月超欠产
     *
     * @return
     */
    public Integer getAllSumPlanQty() {
        if (null == skuInfo || StringUtils.isBlank(skuInfo.getMaterialCode())) {
            return BigDecimal.ZERO.intValue();
        }
        Integer sumPlanQty = Optional.ofNullable(this.sumPlanQty).orElse(BigDecimal.ZERO.intValue());
        Integer lastMonthOverDueQty = Optional.ofNullable(this.monthOverdueQty).orElse(BigDecimal.ZERO.intValue());
        Integer sum = sumPlanQty + lastMonthOverDueQty;
        if (null == sum) {
            return BigDecimal.ZERO.intValue();
        }
        return sum;
    }
}
