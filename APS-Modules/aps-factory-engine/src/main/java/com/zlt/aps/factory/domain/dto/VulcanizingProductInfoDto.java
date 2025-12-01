package com.zlt.aps.factory.domain.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 硫化排程日正在硫化的物料品种
 *
 * @author ZLT
 * @date 20250423
 */
@Data
public class VulcanizingProductInfoDto implements Serializable {
    /**
     * 硫化机台编号
     */
    private String lhMachineCode;
    /**
     * 硫化规格代号
     */
    private String specCode;
    /**
     * 模台数
     */
    private Integer moldQty;
    /**
     * 本身模台数
     */
    private Integer mouldNumber;
    /**
     * 物料编码
     */
    private String productCode;
    /**
     * 模具编号
     */
    private String mouldCode;
    /**
     * 生胎号
     */
    private String embryoCode;
    /**
     * SAP与施工关系中的物料编码
     */
    private String constructionProductCode;
}
