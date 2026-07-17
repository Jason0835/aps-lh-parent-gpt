package com.zlt.aps.tc.domain.vo;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 胎侧自动排程施工胎侧属性查询行对象。
 *
 * <p>对应 {@code TC_VERSION_MATCH_MODE=RECIPE} 模式下按 (CONSTRUCTION_CODE, CONSTRUCTION_VERSION)
 * 查询施工信息表，仅取胎侧排程所需的胎侧编码、肩长、口型板和胶料类别字段，
 * 供 Java 中按成型班次示方书编号逐班关联解析。</p>
 */
@Data
public class TcConstructionSidewallRowVo {

    /** 施工号，对应成型胎胚代码 */
    private String constructionCode;

    /** 施工版本，对应成型班次示方书编号 */
    private String constructionVersion;

    /** 胎侧编码 */
    private String sidewallCode;

    /** 胎侧施工版本快照，来源 SIDEWALL_VERSION */
    private String sidewallVersion;

    /** 胎侧工艺 */
    private String sidewallCraft;

    /** 胎侧肩长 */
    private BigDecimal sidewallLength;

    /** 胎侧口型板 */
    private String sidewallMouthPlate;

    /** 胎侧胶料类别 */
    private String sidewallRubber;

    /** 胎侧胶重量 */
    private BigDecimal sidewallWeight;

    /** 胎侧耐磨胶重量 */
    private BigDecimal sidewallWearpRubberWeight;
}
