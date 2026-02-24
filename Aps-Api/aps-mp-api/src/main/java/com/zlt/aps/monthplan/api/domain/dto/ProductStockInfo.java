package com.zlt.aps.monthplan.api.domain.dto;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 物料库存信息对象表
 *
 * @author ZLT
 * @data 20250217
 */
@Data
public class ProductStockInfo implements Serializable {
    /**
     * 物料编码
     */
    private String productCode;
    /**
     * 分厂编码
     */
    private String factoryCode;
    /**
     * 年份
     */
    private Integer year;
    /**
     * 月份
     */
    private Integer month;
    /**
     * 内销库存数量
     */
    private Long domesticStockQty;
    /**
     * 外销库存数量
     */
    private Long foreignStockQty;
    /**
     * OE库存数量
     */
    private Long oeStockQty;
    /**
     * 20250512 对冲后，剩余库存总量
     */
    private Long leftOverQty;

    /**
     * 以分厂+物料为维度，转换库存
     *
     * @return
     */
    public String getGroupKey() {
        String keyFormat = "%s|*|%s";
        return String.format(keyFormat, factoryCode, productCode);
    }

    /**
     * 获取库存汇总量
     *
     * @return
     */
    public Long getSumStockQty() {
        Long sum = BigDecimal.ZERO.longValue();
        if (null != oeStockQty) {
            sum = sum + oeStockQty;
        }
        if (null != domesticStockQty) {
            sum = sum + domesticStockQty;
        }
        if (null != foreignStockQty) {
            sum = sum + foreignStockQty;
        }
        return sum;
    }
}
