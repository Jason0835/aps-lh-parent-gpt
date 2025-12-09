package com.zlt.aps.factory.domain.vo;

import com.zlt.aps.monthplan.api.enums.ProductionTypeEnum;
import lombok.Getter;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 排产信息对象
 *
 * @author ZLT
 * @date 20250312
 */
@Getter
public class ProductionInfoVo implements Serializable {
    /**
     * 排产日
     */
    private Integer productionDate;
    /**
     * 排产数量
     */
    private Long productionQty;
    /**
     * 消耗的硫化时间--到秒
     */
    private BigDecimal usedCuringTime;
    /**
     * 换规格扣减时间-单位秒
     * 不换规格，则为零
     */
    private BigDecimal changeSubSecond;
    /**
     * 洗模扣减时间-单位秒
     * 不洗模，则为零
     */
    private BigDecimal cleanMouldSubSecond;
    /**
     * 排产类型
     */
    private ProductionTypeEnum productionType;
    /**
     * 第二天是否需要扣减产能
     * 换规格时，可能出现跨天
     */
    private BigDecimal nextDaySubtractTime;
    /**
     * 单条硫化时间(包含间隔增加时间)
     */
    private BigDecimal singleCuringTime;

    /**
     * 构建排产信息对象
     *
     * @param productionDate      排产日
     * @param productionQty       排产数量
     * @param productionType      排产类型 正常日 停工日 维修日 洗模日
     * @param usedCuringTime      排产量使用的硫化时间，单位到秒(包含换规格、洗模消耗的时间)
     * @param nextDaySubtractTime 用于第二天是否需要提前扣减产能(换规格跨天)--小于零表示第二天需要扣减产能
     * @param singleCuringTime    单条硫化时间(包含间隔增加时间)
     */
    public ProductionInfoVo(Integer productionDate, Long productionQty, ProductionTypeEnum productionType, BigDecimal usedCuringTime, BigDecimal changeSubSecond, BigDecimal cleanMouldSubSecond, BigDecimal nextDaySubtractTime, BigDecimal singleCuringTime) {
        this.productionDate = productionDate;
        this.productionQty = productionQty;
        this.productionType = productionType;
        this.usedCuringTime = usedCuringTime;
        this.changeSubSecond = changeSubSecond;
        this.cleanMouldSubSecond = cleanMouldSubSecond;
        this.nextDaySubtractTime = nextDaySubtractTime;
        this.singleCuringTime = singleCuringTime;
    }

}
