package com.zlt.aps.mp.api.domain.vo;

import com.baomidou.mybatisplus.annotation.TableField;
import com.ruoyi.common.core.annotation.Excel;
import com.ruoyi.common.core.web.domain.BaseEntity;
import com.zlt.common.annotation.ImportExcelValidated;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * @author Chen
 * @date 2025/5/21
 */
@Data
@ApiModel(value = "产品版本数据结果Vo", description = "产品版本数据结果Vo")
public class ProductVersionReportVo extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 分厂
     */
//    @Excel(name = "ui.data.column.mdmMaterialInfo.factoryCode")
    @ApiModelProperty(value = "分厂", name = "factoryCode")
    private String factoryCode;

    /**
     * 施工阶段
     */
    @Excel(name = "ui.data.column.report.schedulingType", dictType = "biz_construction_stage")
    @ApiModelProperty(value = "施工阶段", name = "constructionStage")
    private String constructionStage;

    /**
     * 生胎代码
     */
    @Excel(name = "ui.data.column.monthPlanMouldingDayResult.embryoCode")
    @ApiModelProperty(value = "生胎代码", name = "embryoCode")
    private String embryoCode;

    /**
     * 规格代号
     */
    @ImportExcelValidated(required = true)
    @Excel(name = "ui.data.column.mdmProductConstruction.specCode")
    @ApiModelProperty(value = "规格代号", name = "specCode")
    @TableField(value = "SPEC_CODE")
    private String specCode;

    /**
     * 施工代号
     */
    @Excel(name = "ui.data.column.mdmProductConstruction.constructionCode")
    @ApiModelProperty(value = "施工代号", name = "constructionCode")
    @TableField(value = "CONSTRUCTION_CODE")
    private String constructionCode;

    /**
     * 物料号
     */
    @Excel(name = "ui.data.column.mdmMaterialInfo.productCode")
    @ApiModelProperty(value = "物料号", name = "productCode")
    private String productCode;

    /**
     * 规格描述
     */
    @Excel(name = "ui.data.column.mdmMaterialInfo.productDesc")
    @ApiModelProperty(value = "规格描述", name = "productDesc")
    private String productDesc;

    /**
     * 是否必保
     */
    @Excel(name = "ui.data.column.report.isEnsurePlan", dictType = "IS_HAVE")
    @ApiModelProperty(value = "是否必保", name = "isEnsurePlan")
    private String isEnsurePlan;

    /**
     * 是否紧急
     */
    @Excel(name = "ui.data.column.report.isEmergency", dictType = "IS_HAVE")
    @ApiModelProperty(value = "是否紧急", name = "isEmergency")
    private String isEmergency;

    /**
     * 品牌
     */
    @Excel(name = "ui.data.column.mdmMaterialInfo.brand", dictType = "biz_brand_type")
    @ApiModelProperty(value = "品牌", name = "brand")
    private String brand;

    /**
     * 寸口
     */
    @Excel(name = "ui.data.column.mdmMaterialInfo.proSize")
    @ApiModelProperty(value = "寸口", name = "proSize")
    private String proSize;

    /**
     * 花纹
     */
    @Excel(name = "ui.data.column.mdmMaterialInfo.pattern")
    @ApiModelProperty(value = "花纹", name = "pattern")
    private String pattern;

    /**
     * 渠道
     */
    @ApiModelProperty(value = "渠道", name = "channel")
    private String channel;

    /**
     * 内销备货量
     */
    @Excel(name = "ui.data.column.report.inStockUpPlanQty")
    @ApiModelProperty(value = "内销备货量", name = "inStockUpPlanQty")
    private Integer inStockUpPlanQty;

    /**
     * 外销备货量
     */
    @Excel(name = "ui.data.column.report.outStockUpPlanQty")
    @ApiModelProperty(value = "外销备货量", name = "outStockUpPlanQty")
    private Integer outStockUpPlanQty;

    /**
     * OE备货量
     */
    @Excel(name = "ui.data.column.report.oeStockUpPlanQty")
    @ApiModelProperty(value = "OE备货量", name = "oeStockUpPlanQty")
    private Integer oeStockUpPlanQty;

    /**
     * 汇总备货量
     */
    @Excel(name = "ui.data.column.report.sumStockUpPlanQty")
    @ApiModelProperty(value = "汇总备货量", name = "sumStockUpPlanQty")
    private Integer sumStockUpPlanQty;

    /**
     * 内销需求量
     */
    @Excel(name = "ui.data.column.report.inDemandQty")
    @ApiModelProperty(value = "内销需求量", name = "inDemandQty")
    private Integer inDemandQty = 0;

    /**
     * 内销需求量-配套
     */
    @Excel(name = "ui.data.column.report.inDemandQty_01")
    @ApiModelProperty(value = "内销需求量-配套", name = "inDemandQty_01")
    private Integer inDemandQty_01 = 0;

    /**
     * 内销需求量-线上途虎
     */
    @Excel(name = "ui.data.column.report.inDemandQty_02")
    @ApiModelProperty(value = "内销需求量-线上途虎", name = "inDemandQty_02")
    private Integer inDemandQty_02 = 0;

    /**
     * 内销需求量-内销快准
     */
    @Excel(name = "ui.data.column.report.inDemandQty_03")
    @ApiModelProperty(value = "内销需求量-内销快准", name = "inDemandQty_03")
    private Integer inDemandQty_03 = 0;

    /**
     * 内销需求量-内销RT
     */
    @Excel(name = "ui.data.column.report.inDemandQty_04")
    @ApiModelProperty(value = "内销需求量-内销RT", name = "inDemandQty_04")
    private Integer inDemandQty_04 = 0;

    /**
     * 内销需求量-外贸
     */
    @Excel(name = "ui.data.column.report.inDemandQty_05")
    @ApiModelProperty(value = "内销需求量-外贸", name = "inDemandQty_05")
    private Integer inDemandQty_05 = 0;

    /**
     * 内销需求量-内销KA
     */
    @Excel(name = "ui.data.column.report.inDemandQty_09")
    @ApiModelProperty(value = "内销需求量-内销KA", name = "inDemandQty_09")
    private Integer inDemandQty_09 = 0;

    /**
     * 外销需求量
     */
    @Excel(name = "ui.data.column.report.outDemandQty")
    @ApiModelProperty(value = "外销需求量", name = "outDemandQty")
    private Integer outDemandQty = 0;

    /**
     * OE需求量
     */
    @Excel(name = "ui.data.column.report.oeDemandQty")
    @ApiModelProperty(value = "OE需求量", name = "oeDemandQty")
    private Integer oeDemandQty = 0;

    /**
     * 汇总需求量
     */
    @Excel(name = "ui.data.column.report.sumDemandQty")
    @ApiModelProperty(value = "汇总需求量", name = "sumDemandQty")
    private Integer sumDemandQty;

    /**
     * 内销库存量
     */
    @Excel(name = "ui.data.column.report.inStockQty")
    @ApiModelProperty(value = "内销库存量", name = "inStockQty")
    private Integer inStockQty = 0;

    /**
     * 外销库存量
     */
    @Excel(name = "ui.data.column.report.outStockQty")
    @ApiModelProperty(value = "外销库存量", name = "outStockQty")
    private Integer outStockQty = 0;

    /**
     * OE库存量
     */
    @Excel(name = "ui.data.column.report.oeStockQty")
    @ApiModelProperty(value = "OE库存量", name = "oeStockQty")
    private Integer oeStockQty = 0;

    /**
     * 汇总库存量
     */
    @Excel(name = "ui.data.column.report.sumStockQty")
    @ApiModelProperty(value = "汇总库存量", name = "sumStockQty")
    private Integer sumStockQty;

    /**
     * 内销排产量
     */
    @Excel(name = "ui.data.column.report.inProductQty")
    @ApiModelProperty(value = "内销排产量", name = "inProductQty")
    private Integer inProductQty = 0;

    /**
     * 外销排产量
     */
    @Excel(name = "ui.data.column.report.outProductQty")
    @ApiModelProperty(value = "外销排产量", name = "outProductQty")
    private Integer outProductQty = 0;

    /**
     * OE排产量
     */
    @Excel(name = "ui.data.column.report.oeProductQty")
    @ApiModelProperty(value = "OE排产量", name = "oeProductQty")
    private Integer oeProductQty = 0;

    /**
     * 汇总排产量
     */
    @Excel(name = "ui.data.column.report.sumProductQty")
    @ApiModelProperty(value = "汇总排产量", name = "sumProductQty")
    private Integer sumProductQty;

    /**
     * 内销实际备货量
     */
    @Excel(name = "ui.data.column.report.inStockUpActQty")
    @ApiModelProperty(value = "内销实际备货量", name = "inStockUpActQty")
    private Integer inStockUpActQty;

    /**
     * 外销实际备货量
     */
    @Excel(name = "ui.data.column.report.outStockUpActQty")
    @ApiModelProperty(value = "外销实际备货量", name = "outStockUpActQty")
    private Integer outStockUpActQty;

    /**
     * OE实际备货量
     */
    @Excel(name = "ui.data.column.report.oeStockUpActQty")
    @ApiModelProperty(value = "OE实际备货量", name = "oeStockUpActQty")
    private Integer oeStockUpActQty;

    /**
     * 汇总实际备货量
     */
    @Excel(name = "ui.data.column.report.sumStockUpActQty")
    @ApiModelProperty(value = "汇总实际备货量", name = "sumStockUpActQty")
    private Integer sumStockUpActQty;

    /**
     * 内销未排量
     */
    @Excel(name = "ui.data.column.report.inUnProductQty")
    @ApiModelProperty(value = "内销实际备货量", name = "inUnProductQty")
    private Integer inUnProductQty;

    /**
     * 外销未排量
     */
    @Excel(name = "ui.data.column.report.outUnProductQty")
    @ApiModelProperty(value = "外销未排量", name = "outUnProductQty")
    private Integer outUnProductQty;

    /**
     * OE未排量
     */
    @Excel(name = "ui.data.column.report.oeUnProductQty")
    @ApiModelProperty(value = "OE未排量", name = "oeUnProductQty")
    private Integer oeUnProductQty;

    /**
     * 汇总未排量
     */
    @Excel(name = "ui.data.column.report.sumUnProductQty")
    @ApiModelProperty(value = "汇总未排量", name = "sumUnProductQty")
    private Integer sumUnProductQty;

    /**
     * 外销库存缺口
     */
    @Excel(name = "ui.data.column.report.outStockGapQty")
    @ApiModelProperty(value = "外销库存缺口", name = "outStockGapQty")
    private Integer outStockGapQty;

    /**
     * 内销库存缺口
     */
    @Excel(name = "ui.data.column.report.inStockGapQty")
    @ApiModelProperty(value = "内销库存缺口", name = "inStockGapQty")
    private Integer inStockGapQty;

    /**
     * 库存总缺口
     */
    @Excel(name = "ui.data.column.report.sumStockGapQty")
    @ApiModelProperty(value = "库存总缺口", name = "sumStockGapQty")
    private Integer sumStockGapQty;

    /**
     * 排产后外销缺口
     */
    @Excel(name = "ui.data.column.report.outPlanStockGapQty")
    @ApiModelProperty(value = "排产后外销缺口", name = "outPlanStockGapQty")
    private Integer outPlanStockGapQty;

    /**
     * 排产后内销缺口
     */
    @Excel(name = "ui.data.column.report.inPlanStockGapQty")
    @ApiModelProperty(value = "排产后内销缺口", name = "inPlanStockGapQty")
    private Integer inPlanStockGapQty;

    /**
     * 排产后总缺口
     */
    @Excel(name = "ui.data.column.report.sumPlanStockGapQty")
    @ApiModelProperty(value = "排产后总缺口", name = "sumPlanStockGapQty")
    private Integer sumPlanStockGapQty;

    /**
     * 模具
     */
    @Excel(name = "ui.data.column.mdmModelInfo.mouldNo")
    @ApiModelProperty(value = "模具", name = "mouldNo")
    private String mouldNo;

    /**
     * 可用模具数量
     */
    @Excel(name = "ui.data.column.productionMonthPlanInit.mouldQty")
    @ApiModelProperty(value = "可用模具数量", name = "mouldQty")
    @TableField(value = "MOULD_QTY")
    private Integer mouldQty;

    /**
     * 计算内销需求量并赋值
     */
    public void calculateInDemandQty() {
        this.inDemandQty = this.inDemandQty_01 + this.inDemandQty_02 + this.inDemandQty_03 + this.inDemandQty_04 + this.inDemandQty_05 + this.inDemandQty_09;
        this.sumDemandQty = this.inDemandQty + this.outDemandQty + this.oeDemandQty;
    }
}
