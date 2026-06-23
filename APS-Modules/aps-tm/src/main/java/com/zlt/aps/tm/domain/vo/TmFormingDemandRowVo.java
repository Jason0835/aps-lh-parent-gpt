package com.zlt.aps.tm.domain.vo;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 胎面自动排程成型需求查询行对象。
 *
 * <p>用于承接成型排程结果与施工信息关联查询结果，避免业务层通过 Map 字符串 key 读取字段。</p>
 */
@Data
public class TmFormingDemandRowVo {

    /** 成型工单号 */
    private String orderNo;

    /** 胚胎编码 */
    private String embryoCode;

    /** BOM 数据版本 */
    private String bomDataVersion;

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

    /** 胎面编码 */
    private String treadCode;

    /** 胎面肩长 */
    private BigDecimal treadShoulderLength;

    /** 胎面口型板 */
    private String treadMouthPlate;

    /** 胎面胶料类别 */
    private String treadRubberCategory;
}
