package com.zlt.aps.tm.domain.vo;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 胎面自动排程施工胎面属性查询行对象。
 *
 * <p>对应 {@code TM_VERSION_MATCH_MODE=RECIPE} 模式下按 (CONSTRUCTION_CODE, CONSTRUCTION_VERSION)
 * 查询施工信息表，仅取胎面排程所需的胎面编码、肩长、口型板和胶料类别字段，
 * 供 Java 中按成型班次示方书编号逐班关联解析。</p>
 */
@Data
public class TmConstructionTreadRowVo {

    /** 施工号，对应成型胎胚代码 */
    private String constructionCode;

    /** 施工版本，对应成型班次示方书编号 */
    private String constructionVersion;

    /** 胎面编码 */
    private String treadCode;

    /** 胎面肩长 */
    private BigDecimal treadShoulderLength;

    /** 胎面口型板 */
    private String treadMouthPlate;

    /** 胎面胶料类别 */
    private String treadRubberCategory;
}
