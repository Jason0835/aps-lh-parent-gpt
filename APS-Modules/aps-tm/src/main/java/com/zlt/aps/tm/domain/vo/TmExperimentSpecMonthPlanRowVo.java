package com.zlt.aps.tm.domain.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 胎面实验规格月计划定稿查询行对象。
 *
 * <p>用于承接月计划定稿表和施工信息表关联后的实验规格数据，数据加载阶段据此生成或叠加胎面实验计划。</p>
 */
@Data
public class TmExperimentSpecMonthPlanRowVo {

    /** 月计划定稿主键 */
    private Long monthPlanId;

    /** 月计划定稿工单号 */
    private String productionNo;

    /** 工厂编号 */
    private String factoryCode;

    /** 月计划定稿生产日期 */
    private Date experimentPlanDate;

    /** 月计划定稿日期列数量，仅用于判断当天是否有实验计划 */
    private BigDecimal dayQty;

    /** 胎胚号 */
    private String embryoCode;

    /** 施工阶段，01 表示实验 */
    private String constructionStage;

    /** 胎面编码 */
    private String treadCode;

    /** 胎面肩长 */
    private BigDecimal treadShoulderLength;

    /** 胎面口型板 */
    private String treadMouthPlate;

    /** 胎面胶料类别 */
    private String treadRubberCategory;
}
