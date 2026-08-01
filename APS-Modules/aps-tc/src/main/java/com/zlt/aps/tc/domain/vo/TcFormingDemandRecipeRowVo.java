package com.zlt.aps.tc.domain.vo;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 胎侧自动排程成型需求查询行对象（按示方书版本匹配）。
 *
 * <p>对应 {@code TC_VERSION_MATCH_MODE=RECIPE} 模式下仅查询成型排程结果表（不 JOIN 施工表），
 * 承载 CLASS1~8 的计划量与示方书编号，施工信息由后续按 (EMBRYO_CODE, CLASSn_RECIPE_NO) 在 Java 中关联解析。</p>
 */
@Data
public class TcFormingDemandRecipeRowVo {

    /** 成型排程源记录主键，用于生成稳定业务键 */
    private Long sourceRecordId;

    /** 成型工单号 */
    private String orderNo;

    /** 胚胎编码 */
    private String embryoCode;

    /** 硫化机编码，多个编码使用英文逗号分隔 */
    private String lhMachineCode;

    /** 成型机台编码。 */
    private String cxMachineCode;

    /** 成型物料描述。 */
    private String materialDesc;

    /** 1班成型计划量 */
    private BigDecimal class1PlanQty;

    /** 2班成型计划量 */
    private BigDecimal class2PlanQty;

    /** 3班成型计划量 */
    private BigDecimal class3PlanQty;

    /** 4班成型计划量 */
    private BigDecimal class4PlanQty;

    /** 5班成型计划量 */
    private BigDecimal class5PlanQty;

    /** 6班成型计划量 */
    private BigDecimal class6PlanQty;

    /** 7班成型计划量 */
    private BigDecimal class7PlanQty;

    /** 8班成型计划量 */
    private BigDecimal class8PlanQty;

    /** 1班示方书编号，对应施工信息 CONSTRUCTION_VERSION */
    private String class1RecipeNo;

    /** 2班示方书编号 */
    private String class2RecipeNo;

    /** 3班示方书编号 */
    private String class3RecipeNo;

    /** 4班示方书编号 */
    private String class4RecipeNo;

    /** 5班示方书编号 */
    private String class5RecipeNo;

    /** 6班示方书编号 */
    private String class6RecipeNo;

    /** 7班示方书编号 */
    private String class7RecipeNo;

    /** 8班示方书编号 */
    private String class8RecipeNo;

    /** 收尾提示标识，0 表示提示收尾 */
    private String markCloseOutTip;

    /** 成型余量，单位条 */
    private BigDecimal cxRemainQty;

    /** 硫化余量，单位条 */
    private BigDecimal lhRemainQty;
}
