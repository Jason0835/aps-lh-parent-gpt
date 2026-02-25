package com.zlt.aps.mp.api.domain.vo;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 分类差异表基础结果数据Vo
 *
 * @author Chen
 * @date 2025/3/19
 */
@Data
public class ClassificationGapVo implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 物料号
     */
    private String productCode;

    /**
     * 品牌
     */
    private String brand;

    /**
     * 寸口
     */
    private String proSize;

    /**
     * 库位类型
     */
    private String locationType;

    /**
     * 渠道
     */
    private String channel;

    /**
     * 库存数量
     */
    private BigDecimal stockQty;

    /**
     * 订单sku数量
     */
    private BigDecimal saleSkuCount;

    /**
     * 订单数量
     */
    private BigDecimal salePlanQty;

    /**
     * 排产sku数量
     */
    private BigDecimal produceSkuCount;

    /**
     * 排产数量
     */
    private BigDecimal producePlanQty;

    /**
     * 缺口sku数量
     */
    private BigDecimal gapSkuCount;

    /**
     * 订单缺口数量
     */
    private BigDecimal orderGapQty;
}
