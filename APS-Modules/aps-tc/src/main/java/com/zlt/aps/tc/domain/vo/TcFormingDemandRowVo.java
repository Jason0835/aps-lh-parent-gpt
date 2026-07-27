package com.zlt.aps.tc.domain.vo;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 胎侧自动排程成型需求查询行对象。
 *
 * <p>用于承接成型排程结果与施工信息关联查询结果，避免业务层通过 Map 字符串 key 读取字段。</p>
 */
@Data
public class TcFormingDemandRowVo {

    /** 成型工单号 */
    private String orderNo;

    /** 胚胎编码 */
    private String embryoCode;

    /** BOM 数据版本 */
    private String bomDataVersion;

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

    /** 收尾提示标识，0 表示提示收尾 */
    private String markCloseOutTip;

    /** 成型余量，单位条 */
    private BigDecimal cxRemainQty;

    /** 胎侧编码 */
    private String sidewallCode;

    /** 胎侧施工版本 */
    private String constructionVersion;

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
