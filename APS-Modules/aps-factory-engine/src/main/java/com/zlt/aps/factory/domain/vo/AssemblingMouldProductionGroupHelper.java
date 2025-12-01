package com.zlt.aps.factory.domain.vo;

import lombok.Getter;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 拼模排产计划信息计划辅助类
 *
 * @author ZLT
 * @date 20250723
 */
@Getter
public class AssemblingMouldProductionGroupHelper implements Serializable {
    /**
     * SAP编码-物料编码
     */
    private String productCode;
    /**
     * 硫化规格代号
     */
    private String specCode;
    /**
     * 生胎代码
     */
    private String embryoCode;
    /**
     * 寸口
     */
    private BigDecimal proSize;
    /**
     * 模具编号
     */
    private String mouldCode;
    /**
     * 模具
     */
    private String mouldNo;

    /**
     * 拼模排产规格信息对象
     *
     * @param productCode 物料编码-SAP代码
     * @param specCode    规格代号
     * @param embryoCode  生胎代码
     * @param proSize     寸口
     * @param mouldCode   模具编码
     * @param mouldNo     模具号
     */
    public AssemblingMouldProductionGroupHelper(String productCode, String specCode, String embryoCode, BigDecimal proSize, String mouldCode, String mouldNo) {
        this.productCode = productCode;
        this.specCode = specCode;
        this.embryoCode = embryoCode;
        this.proSize = proSize;
        this.mouldCode = mouldCode;
        this.mouldNo = mouldNo;
    }
}
