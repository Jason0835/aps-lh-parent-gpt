package com.zlt.aps.factory.domain.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * 物料平均销量信息对象
 *
 * @author ZLT
 * @date 20250219
 */
@Data
public class ProductAverageSaleVo implements Serializable {
    /**
     * 物料规格
     */
    private String productCode;
    /**
     * 月平均销量值
     */
    private Integer averageValue;
}
