package com.zlt.aps.mp.factory.helper;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 备货计划需求信息
 *
 * @author ZLT
 * @date 20250922
 */
@Data
public class MonthPlanProductionStockUpRequirementHelper implements Serializable {
    /**
     * 物料编码
     */
    private String productCode;
    /**
     * 总备货数
     */
    private Long stockUpQty;
    /**
     * 总月均销量
     */
    private Integer averageValue;
    /**
     * 近几个月
     */
    private Integer averageType;
    /**
     * 备货系数
     */
    private BigDecimal factor;
}
