package com.zlt.aps.lh.api.domain.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 日排产进度报表行 VO。
 *
 * <p>固定列为模板 A~U 列，日期列为动态列（T 日～报表末端），
 * 日期列的值取自月计划定稿表对应自然日的排产量。</p>
 *
 * @author APS
 */
@Data
@ApiModel("日排产进度报表行")
public class LhDailyPlanProgressReportRowVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 胶种：由物料+胎胚+示方类型关联 BOM 消耗明细中的 AQT 原材料
     */
    @ApiModelProperty(value = "胶种", name = "rubberType")
    private String rubberType;

    /**
     * 产品状态（S-正规、T-量试、X-试制）
     */
    @ApiModelProperty(value = "产品状态", name = "productStatus")
    private String productStatus;

    /**
     * 产品结构
     */
    @ApiModelProperty(value = "产品结构", name = "structureName")
    private String structureName;

    /**
     * 主花纹
     */
    @ApiModelProperty(value = "主花纹", name = "mainPattern")
    private String mainPattern;

    /**
     * 型腔
     */
    @ApiModelProperty(value = "型腔", name = "mouldCavityQty")
    private Integer mouldCavityQty;

    /**
     * 活块
     */
    @ApiModelProperty(value = "活块", name = "typeBlockQty")
    private Integer typeBlockQty;

    /**
     * 胎胚描述
     */
    @ApiModelProperty(value = "胎胚描述", name = "mainMaterialDesc")
    private String mainMaterialDesc;

    /**
     * NC物料编码
     */
    @ApiModelProperty(value = "NC物料编码", name = "materialCode")
    private String materialCode;

    /**
     * 物料描述
     */
    @ApiModelProperty(value = "物料描述", name = "materialDesc")
    private String materialDesc;

    /**
     * 净需求（月计划定稿 prod_req_plan）
     */
    @ApiModelProperty(value = "净需求", name = "prodReqPlan")
    private Integer prodReqPlan;

    /**
     * 排产量（月计划定稿定稿排产量 original_total_qty）
     */
    @ApiModelProperty(value = "排产量", name = "originalTotalQty")
    private Integer originalTotalQty;

    /**
     * 调整1
     */
    @ApiModelProperty(value = "调整1", name = "adjustQty1")
    private Integer adjustQty1;

    /**
     * 调整2
     */
    @ApiModelProperty(value = "调整2", name = "adjustQty2")
    private Integer adjustQty2;

    /**
     * 调整3
     */
    @ApiModelProperty(value = "调整3", name = "adjustQty3")
    private Integer adjustQty3;

    /**
     * 分类（结构类型）：01-周期产品、02-常规产品
     */
    @ApiModelProperty(value = "分类（结构类型）", name = "structureType")
    private String structureType;

    /**
     * 合格产量（硫化余量计算使用的已完成量）
     */
    @ApiModelProperty(value = "合格产量", name = "finishQty")
    private Integer finishQty;

    /**
     * 余量（硫化余量）
     */
    @ApiModelProperty(value = "余量", name = "surplusQty")
    private Integer surplusQty;

    /**
     * 高优先级（取月计划定稿 HEIGHT_PRODUCTION_QTY 高优先级排产数量）
     */
    @ApiModelProperty(value = "高优先级", name = "heightQty")
    private Integer heightQty;

    /**
     * 日产（= 月计划定稿单模硫化量 × 2，与排程算法「日硫化量」口径一致）
     */
    @ApiModelProperty(value = "日产", name = "dayLhQty")
    private Integer dayLhQty;

    /**
     * 硫化机数量（基准 T+2 机台数）
     */
    @ApiModelProperty(value = "硫化机数量", name = "lhMachineQty")
    private Integer lhMachineQty;

    /**
     * T+2日排产信息（机台+ 新开/换活字块/提前/延迟/超过月计划N台）
     */
    @ApiModelProperty(value = "T+2日排产信息", name = "t2Info")
    private String t2Info;

    /**
     * 日期列：按报表起始日～末端顺序存放月计划定稿排产量，无计划时为空
     */
    @ApiModelProperty(value = "日期列排产量", name = "dayQtyList")
    private List<Integer> dayQtyList;
}
