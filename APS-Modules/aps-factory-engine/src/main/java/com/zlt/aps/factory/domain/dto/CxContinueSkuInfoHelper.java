package com.zlt.aps.factory.domain.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 在机结构-续作Sku信息
 * 包含基础信息及当时使用的模具总数
 * 基础信息：分组名-结构、物料编码、物料描述、英寸
 * 胎胚号、规格、主花纹、花纹
 *
 * @author zlt
 * @date 20251224
 */
@Data
public class CxContinueSkuInfoHelper implements Serializable {

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
     * 计划需求量--高优先级或是总排产量？
     */
    private Long planDemandQty;
}
