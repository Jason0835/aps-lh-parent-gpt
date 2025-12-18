package com.zlt.aps.factory.domain.dto;

import lombok.Data;
import org.springframework.beans.BeanUtils;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 排产计划-在机结构续作信息
 * 成型机续作Sku的信息
 * 包含 Sku的物料描述、编码、英寸
 * 规格，胎胚号、主花纹、花纹
 * 使用的模具数-能转化为硫化机台数
 *
 * @author ZLT
 * @date 20251215
 */
@Data
public class CxContinueProductInfoHelper implements Serializable {

    /**
     * 分组信息--TBR结构名
     */
    private String groupName;

    /**
     * 物料编码
     */
    private String materialCode;

    /**
     * 物料描述
     */
    private String materialDesc;

    /**
     * 英寸
     */
    private String proSize;

    /**
     * 胎胚号
     */
    private String embryoCode;

    /**
     * 规格
     */
    private String specifications;

    /**
     * 主花纹
     */
    private String mainPattern;

    /**
     * 花纹
     */
    private String pattern;

    /**
     * 模具数
     */
    private Integer mouldNumber;

    /**
     * 创建续作sku信息
     *
     * @param continueProductInfo
     * @return
     */
    public static CxContinueProductInfoHelper create(ContinueProductInfo continueProductInfo) {
        CxContinueProductInfoHelper helper = new CxContinueProductInfoHelper();
        BeanUtils.copyProperties(continueProductInfo, helper);
        helper.setMouldNumber(BigDecimal.ZERO.intValue());
        return helper;
    }
}
