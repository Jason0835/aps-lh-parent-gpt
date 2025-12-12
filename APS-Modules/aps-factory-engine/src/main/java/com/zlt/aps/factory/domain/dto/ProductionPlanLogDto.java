package com.zlt.aps.factory.domain.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 排产计划日志对象
 *
 * @author ZLT
 * @date 20251210
 */
@Data
public class ProductionPlanLogDto implements Serializable {
    /**
     * 排产计划ID
     */
    private Long productionPlanId;
    /**
     * 物料编码
     */
    private String productCode;
    /**
     * 物料描述
     */
    private String productDesc;
    /**
     * 空的信息
     */
    private static final ProductionPlanLogDto EMPTY = new ProductionPlanLogDto(null, "", "");

    /**
     * 构造函数
     *
     * @param productionPlanId
     * @param productCode
     * @param productDesc
     */
    public ProductionPlanLogDto(Long productionPlanId, String productCode, String productDesc) {
        this.productionPlanId = productionPlanId;
        this.productCode = productCode;
        this.productDesc = productDesc;
    }

    /**
     * 返回空的计划对象信息
     *
     * @return
     */
    public static ProductionPlanLogDto getEmpty() {
        return EMPTY;
    }
}
