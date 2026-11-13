package com.zlt.aps.common.engine.domain;

import com.zlt.aps.mp.api.domain.entity.FactoryMonthPlanProductionFinalResult;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
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
    /**
     * 欠产余量标识
     */
    private boolean overdueSurplusFlag;
    /**
     * 余量段起始日（含）：该物料「窗口之后仍有余量」的连续段起点。
     * <p>供成型胎胚维度合并余量账户使用；为空表示无可靠段，调用方须回退自有区间口径。</p>
     */
    private LocalDate segmentStart;
    /**
     * 余量段断点日（含）：与硫化余量算法同源的断点，遇连续 N+1 天空档即截断。
     * <p>为空表示无可靠段，调用方须回退自有区间口径。</p>
     */
    private LocalDate segmentEnd;

    public LhSurplusResultVo(FactoryMonthPlanProductionFinalResult skuInfo, Integer monthOverdueQty, Integer planQty, Integer sumPlanQty, Integer finishedQty) {
        this(skuInfo, monthOverdueQty, planQty, sumPlanQty, finishedQty, null, null);
    }

    /**
     * 带余量段的构造函数。
     *
     * @param skuInfo          Sku信息
     * @param monthOverdueQty  上个月超欠产量
     * @param planQty          当前计划量
     * @param sumPlanQty       总计划量
     * @param finishedQty      完成量
     * @param segmentStart     余量段起始日，可空
     * @param segmentEnd       余量段断点日，可空
     */
    public LhSurplusResultVo(FactoryMonthPlanProductionFinalResult skuInfo, Integer monthOverdueQty, Integer planQty,
                             Integer sumPlanQty, Integer finishedQty, LocalDate segmentStart, LocalDate segmentEnd) {
        this.skuInfo = skuInfo;
        this.monthOverdueQty = monthOverdueQty;
        this.planQty = planQty;
        this.sumPlanQty = sumPlanQty;
        this.finishedQty = finishedQty;
        this.overdueSurplusFlag = false;
        this.segmentStart = segmentStart;
        this.segmentEnd = segmentEnd;
    }

    /**
     * 余量信息
     * 计划量 - 已完成量 - 上月超欠产
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
        Integer surplusQty = planQty - finishedQty - lastMonthOverDueQty;
        if (null == surplusQty) {
            return BigDecimal.ZERO.intValue();
        }
        return surplusQty;
    }

    /**
     * 总的有效计划量，加上上月超欠产
     * 总的有效计划量 = 月断点计划总量 - 上月超欠产
     *
     * @return
     */
    public Integer getAllPlanQty() {
        if (null == skuInfo || StringUtils.isBlank(skuInfo.getMaterialCode())) {
            return BigDecimal.ZERO.intValue();
        }
        Integer planQty = Optional.ofNullable(this.planQty).orElse(BigDecimal.ZERO.intValue());
        Integer lastMonthOverDueQty = Optional.ofNullable(this.monthOverdueQty).orElse(BigDecimal.ZERO.intValue());
        Integer sum = planQty - lastMonthOverDueQty;
        if (null == sum) {
            return BigDecimal.ZERO.intValue();
        }
        return sum;
    }

    /**
     * 总的计划量，加上上月超欠产
     * 总计划量 = 月总计划量 - 上月超欠产
     *
     * @return
     */
    public Integer getAllSumPlanQty() {
        if (null == skuInfo || StringUtils.isBlank(skuInfo.getMaterialCode())) {
            return BigDecimal.ZERO.intValue();
        }
        Integer sumPlanQty = Optional.ofNullable(this.sumPlanQty).orElse(BigDecimal.ZERO.intValue());
        Integer lastMonthOverDueQty = Optional.ofNullable(this.monthOverdueQty).orElse(BigDecimal.ZERO.intValue());
        Integer sum = sumPlanQty - lastMonthOverDueQty;
        if (null == sum) {
            return BigDecimal.ZERO.intValue();
        }
        return sum;
    }

    public void markBeforeSurplusQty() {
        this.overdueSurplusFlag = true;
    }
}
