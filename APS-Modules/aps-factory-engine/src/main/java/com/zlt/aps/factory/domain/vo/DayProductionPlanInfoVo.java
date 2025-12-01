package com.zlt.aps.factory.domain.vo;

import lombok.Getter;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 日排产计划信息对象
 *
 * @author ZLT
 * @date 20250312
 */
@Getter
public class DayProductionPlanInfoVo implements Serializable {
    /**
     * 排产计划ID
     */
    private Long monthPlanId;
    /**
     * 排产规格
     */
    private String productCode;
    /**
     * 寸口|*|成型法|*|胎体布层级
     */
    private String sizeCapacityKey;
    /**
     * 排产日
     */
    private Integer productionDate;
    /**
     * 还需排产量
     */
    private Long needProductionQty;
    /**
     * 单条硫化时间-到秒(包含单条间隔硫化时间)
     */
    private BigDecimal singleCuringTime;
    /**
     * 是否续作模具排产
     *
     */
    private boolean continueProduction;
    /**
     * @param monthPlanId       排产计划ID
     * @param productCode       排产规格
     * @param sizeCapacityKey   寸口|*|成型法|*|胎体布层级
     * @param productionDate    排产日
     * @param needProductionQty 需排产量
     * @param singleCuringTime  单条硫化时间 = 物料硫化时间 + 单条间隔硫化时间 单位秒
     */
    public DayProductionPlanInfoVo(Long monthPlanId, String productCode, String sizeCapacityKey, Integer productionDate, Long needProductionQty, BigDecimal singleCuringTime, boolean continueProduction) {
        this.monthPlanId = monthPlanId;
        this.productCode = productCode;
        this.sizeCapacityKey = sizeCapacityKey;
        this.productionDate = productionDate;
        this.needProductionQty = needProductionQty;
        this.singleCuringTime = singleCuringTime;
        this.continueProduction = continueProduction;
    }

}
