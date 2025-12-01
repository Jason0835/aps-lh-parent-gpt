package com.zlt.aps.factory.domain.vo;

import com.zlt.aps.common.core.utils.BigDecimalUtils;
import lombok.Getter;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 模具日排产剩余对象
 * 剩余硫化时间以及换规格扣减时间
 *
 * @author ZLT
 * @date 20250312
 */
@Getter
public class MouldDayProductionLeftOverVo {
    /**
     * 排产日
     */
    private Integer productionDate;
    /**
     * 剩余时间-单位秒
     */
    private BigDecimal leftOverSecond;
    /**
     * 换规格扣减时间-单位秒
     * 不换规格，则为零
     */
    private BigDecimal changeSubSecond;
    /**
     * 是否换规格
     */
    private boolean isChangeProduct;
    /**
     * 是否需要洗模
     */
    private boolean isCleanMould;
    /**
     * 洗模扣减时间-单位秒
     * 不洗模，则为零
     */
    private BigDecimal cleanMouldSubSecond;

    /**
     * 模具排产日剩余信息对象
     *
     * @param productionDate      排产日
     * @param leftOverSecond      剩余硫化时间(如果换规格，则会扣减换规格扣减的时间)
     * @param changeSubSecond     如果是换规格则为换规格配置的扣减时间，否则为零
     * @param isChangeProduct     是否换规格
     * @param isCleanMould        是否洗模
     * @param cleanMouldSubSecond 如果是洗模，则为洗模配置的扣减时间，否则为零
     */
    public MouldDayProductionLeftOverVo(Integer productionDate, BigDecimal leftOverSecond, BigDecimal changeSubSecond, boolean isChangeProduct, boolean isCleanMould, BigDecimal cleanMouldSubSecond) {
        this.productionDate = productionDate;
        this.leftOverSecond = leftOverSecond;
        this.changeSubSecond = changeSubSecond;
        this.isChangeProduct = isChangeProduct;
        this.isCleanMould = isCleanMould;
        this.cleanMouldSubSecond = cleanMouldSubSecond;
    }

    /**
     * 获取排产日真实产能预占量
     *
     * @param productionQty    实际能排产量
     * @param singleCuringTime 单条硫化时间(包含间隔增加时间)
     * @return
     */
    public Long getRealPreemptionQty(Long productionQty, BigDecimal singleCuringTime) {
        //实际消耗时间：排产量消耗的时间 + 换规格消耗时间 + 洗模消耗时间
        BigDecimal usedCuringTime = singleCuringTime.multiply(BigDecimal.valueOf(productionQty)).add(changeSubSecond).add(cleanMouldSubSecond);
        if (BigDecimalUtils.safeCompare(leftOverSecond, BigDecimal.ZERO) < BigDecimal.ZERO.intValue()) {
            //leftOverSecond小于0，则表示换规格、或是洗模消耗出现了跨天，则需要扣减跨天消耗
            usedCuringTime = usedCuringTime.add(leftOverSecond);
        }
        return usedCuringTime.divide(singleCuringTime, 0, RoundingMode.DOWN).longValue();
    }

}
