package com.zlt.aps.monthplan.api.domain.entity;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ruoyi.common.core.annotation.Excel;
import com.ruoyi.common.core.web.domain.BaseEntity;
import com.zlt.aps.monthplan.api.domain.vo.SinglePlanInfoHelper;
import com.zlt.common.annotation.ImportExcelValidated;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：FactoryMonthPlanProdFinal.java
 * 描    述：分厂月生产计划排产结果-生产计划排产结果对象 t_mp_month_plan_prod_final
 *
 * @author zlt
 * @version 1.0
 * <p>
 * 修改记录：
 * 修改时间：...
 * 修 改 人：zlt
 * 修改内容：...
 * @date 2025-03-14
 */

@Data
@TableName(value = "T_MP_MONTH_PLAN_PROD_FINAL")
@ApiModel(value = "分厂月生产计划排产结果-生产计划排产结果对象", description = "分厂月生产计划排产结果-生产计划排产结果对象 ")
public class FactoryMonthPlanProdFinal extends FactoryMonthPlanProdFinal_JY {

    private static final long serialVersionUID = 1L;

    /**
     * 排产制造单号
     */
    @Excel(name = "ui.data.column.factoryMonthPlanProdFinal.productionNo")
    @ApiModelProperty(value = "排产制造单号", name = "productionNo")
    @TableField(value = "PRODUCTION_NO")
    private String productionNo;

    /**
     * 生产分厂编号
     */
    @Excel(name = "ui.data.column.factoryMonthPlanProdFinal.factoryCode", dictType = "biz_factory_name")
    @ImportExcelValidated(required = true)
    @ApiModelProperty(value = "生产分厂编号", name = "factoryCode")
    @TableField(value = "FACTORY_CODE")
    private String factoryCode;

    /**
     * 年份
     */
    @Excel(name = "ui.data.column.factoryMonthPlanProdFinal.year")
    @ImportExcelValidated(required = true, digits = true, min = 1000, max = 9999)
    @ApiModelProperty(value = "年份", name = "year")
    @TableField(value = "YEAR")
    private Integer year;

    /**
     * 月份
     */
    @Excel(name = "ui.data.column.factoryMonthPlanProdFinal.month")
    @ImportExcelValidated(required = true, digits = true, min = 1, max = 12)
    @ApiModelProperty(value = "月份", name = "month")
    @TableField(value = "MONTH")
    private Integer month;

    /**
     * 年月:YYYYMM
     */
    @Excel(name = "ui.data.column.factoryMonthPlanProdFinal.yearMonth")
    @ApiModelProperty(value = "年月:YYYYMM", name = "yearMonth")
    @TableField(value = "`YEAR_MONTH`")
    private Integer yearMonth;

    /**
     * 销售生产需求计划版本
     */
    @Excel(name = "ui.data.column.factoryMonthPlanProdFinal.monthPlanVersion")
    @ImportExcelValidated(required = true, maxLength = 20)
    @ApiModelProperty(value = "销售生产需求计划版本", name = "monthPlanVersion")
    @TableField(value = "MONTH_PLAN_VERSION")
    private String monthPlanVersion;

    /**
     * 分厂版本
     */
    @Excel(name = "ui.data.column.factoryMonthPlanProdFinal.productionVersion")
    @ImportExcelValidated(required = true, maxLength = 20)
    @ApiModelProperty(value = "分厂版本", name = "productionVersion")
    @TableField(value = "PRODUCTION_VERSION")
    private String productionVersion;

    /**
     * 生产物料编号
     */
    @Excel(name = "ui.data.column.factoryMonthPlanProdFinal.productCode")
    @ApiModelProperty(value = "生产物料编号", name = "productCode")
    @TableField(value = "PRODUCT_CODE")
    private String productCode;

    /**
     * 生产规格描述
     */
    @Excel(name = "ui.data.column.factoryMonthPlanProdFinal.productDesc")
    @ApiModelProperty(value = "生产规格描述", name = "productDesc")
    @TableField(value = "PRODUCT_DESC")
    private String productDesc;

    /**
     * 施工阶段
     */
    @Excel(name = "ui.data.column.productionMonthPlanInit.constructionStage", dictType = "biz_construction_stage")
    @ImportExcelValidated(required = true, maxLength = 20)
    @ApiModelProperty(value = "施工阶段", name = "constructionStage")
    @TableField(value = "CONSTRUCTION_STAGE")
    private String constructionStage;

    /**
     * 成型法
     */
    @Excel(name = "ui.data.column.mdmProductConstruction.mouldMethod", dictType = "MACHINE_TYPE")
    @ImportExcelValidated(required = true)
    @ApiModelProperty(value = "成型法", name = "mouldMethod")
    @TableField(value = "MOULD_METHOD")
    private String mouldMethod;

    /**
     * 全规格代号信息 包含规格代号及对应的成型法
     */
    @ApiModelProperty(value = "全规格代号信息", name = "specCodeInfo")
    @TableField(value = "SPEC_CODE_INFO")
    private String specCodeInfo;

    /**
     * 库位类别
     */
    @Excel(name = "ui.data.column.factoryMonthPlanProdFinal.locationType", dictType = "biz_stor_type")
    @ImportExcelValidated(required = true)
    @ApiModelProperty(value = "库位类别", name = "locationType")
    @TableField(value = "LOCATION_TYPE")
    private String locationType;

    /**
     * 渠道
     */
    @Excel(name = "ui.data.column.factoryMonthPlanProdFinal.channel", dictType = "biz_channel_type")
    @ApiModelProperty(value = "渠道", name = "channel")
    @TableField(value = "CHANNEL")
    private String channel;

    /**
     * 品牌
     */
    @Excel(name = "ui.data.column.factoryMonthPlanProdFinal.brand", dictType = "biz_brand_type")
    @ApiModelProperty(value = "品牌", name = "brand")
    @TableField(value = "BRAND")
    private String brand;



    /**
     * 规格
     */
    @Excel(name = "ui.data.column.factoryMonthPlanProdFinal.specifications")
    @ApiModelProperty(value = "规格", name = "specifications")
    @TableField(value = "SPECIFICATIONS")
    private String specifications;

    /**
     * 花纹
     */
    @Excel(name = "ui.data.column.factoryMonthPlanProdFinal.pattern")
    @ApiModelProperty(value = "花纹", name = "pattern")
    @TableField(value = "PATTERN")
    private String pattern;

    /**
     * 层级
     */
    // @Excel(name = "ui.data.column.factoryMonthPlanProdFinal.hierarchy")
    @ApiModelProperty(value = "层级", name = "hierarchy")
    @TableField(value = "HIERARCHY")
    private String hierarchy;

    /**
     * 品名编码
     */
    @Excel(name = "ui.data.column.factoryMonthPlanProdFinal.productTypeCode", dictType = "biz_product_name")
    @ApiModelProperty(value = "品名编码", name = "productTypeCode")
    @TableField(value = "PRODUCT_TYPE_CODE")
    private String productTypeCode;

    /**
     * 品名
     */
    // @Excel(name = "ui.data.column.factoryMonthPlanProdFinal.productTypeName")
    @ApiModelProperty(value = "品名", name = "productTypeName")
    @TableField(value = "PRODUCT_TYPE_NAME")
    private String productTypeName;

    /**
     * 等级码
     */
    // @Excel(name = "ui.data.column.factoryMonthPlanProdFinal.levelCode")
    @ApiModelProperty(value = "等级码", name = "levelCode", hidden = true)
    @TableField(value = "LEVEL_CODE")
    private String levelCode;

    /**
     * 等级名称
     */
    // @Excel(name = "ui.data.column.factoryMonthPlanProdFinal.levelName")
    @ApiModelProperty(value = "等级名称", name = "levelName", hidden = true)
    @TableField(value = "LEVEL_NAME")
    private String levelName;

    /**
     * 规格代号
     */
    @Excel(name = "ui.data.column.factoryMonthPlanProdFinal.specCode")
    @ImportExcelValidated(required = true, maxLength = 20)
    @ApiModelProperty(value = "规格代号", name = "specCode")
    @TableField(value = "SPEC_CODE")
    private String specCode;

    /**
     * 生胎代码
     */
    @Excel(name = "ui.data.column.factoryMonthPlanProdFinal.embryoCode")
    @ImportExcelValidated(required = true, maxLength = 20)
    @ApiModelProperty(value = "生胎代码", name = "embryoCode")
    @TableField(value = "EMBRYO_CODE")
    private String embryoCode;

    /**
     * 模具
     */
    @Excel(name = "ui.data.column.factoryMonthPlanProdFinal.mouldNo")
    @ApiModelProperty(value = "模具", name = "mouldNo")
    @TableField(value = "MOULD_NO")
    private String mouldNo;

    /**
     * 模数
     */
    @Excel(name = "ui.data.column.factoryMonthPlanProdFinal.mouldQty")
    @ImportExcelValidated(required = true, digits = true, min = 0, max = 99999999)
    @ApiModelProperty(value = "模数", name = "mouldQty")
    @TableField(value = "MOULD_QTY")
    private Integer mouldQty;

    /**
     * 模具编码集合，多个以,分隔
     */
    @ApiModelProperty(value = "模具编码集合，多个以,分隔", name = "mouldInfo", hidden = true)
    @TableField(value = "MOULD_INFO")
    private String mouldInfo;

    /**
     * 合并信息json串
     */
    @ApiModelProperty(value = "合并信息json串", name = "mergeInfo", hidden = true)
    @TableField(value = "MERGE_INFO")
    private String mergeInfo;

    /**
     * 是否有交期（0：默认没有，1：有）
     */
    @Excel(name = "ui.data.column.factoryMonthPlanProdFinal.isDeliveryDate", dictType = "biz_yes_no")
    @ImportExcelValidated(required = true)
    @ApiModelProperty(value = "是否有交期", name = "isDeliveryDate")
    @TableField(value = "IS_DELIVERY_DATE")
    private Integer isDeliveryDate;





    /**
     * 未排产原因
     */
    @Excel(name = "ui.data.column.factoryMonthPlanProdFinal.reason")
    @ImportExcelValidated(maxLength = 1000)
    @ApiModelProperty(value = "未排产原因", name = "reason")
    @TableField(value = "REASON")
    private String reason;

    /**
     * 开始日期
     */
    @Excel(name = "ui.data.column.factoryMonthPlanProdFinal.beginDate")
    @ImportExcelValidated(min = 1, max = 31)
    @ApiModelProperty(value = "开始日期", name = "beginDate")
    @TableField(value = "BEGIN_DATE", updateStrategy = FieldStrategy.IGNORED)
    private Integer beginDate;

    /**
     * 结束日期
     */
    @Excel(name = "ui.data.column.factoryMonthPlanProdFinal.endDay")
    @ImportExcelValidated(min = 1, max = 31)
    @ApiModelProperty(value = "结束日期", name = "endDay")
    @TableField(value = "END_DAY", updateStrategy = FieldStrategy.IGNORED)
    private Integer endDay;
    /**
     * 备注说明
     */
    @Excel(name = "ui.common.column.remark")
    @ApiModelProperty("备注")
    @TableField("REMARK")
    private String remark;
    /**
     * PRE_DAY_1
     */
    // @Excel(name = "ui.data.column.factoryMonthPlanProdFinal.preDay1")
    @ApiModelProperty(value = "PRE_DAY_1", name = "preDay1", hidden = true)
    @TableField(value = "PRE_DAY_1")
    private Long preDay1;

    /**
     * PRE_DAY_2
     */
    // @Excel(name = "ui.data.column.factoryMonthPlanProdFinal.preDay2")
    @ApiModelProperty(value = "PRE_DAY_2", name = "preDay2", hidden = true)
    @TableField(value = "PRE_DAY_2")
    private Long preDay2;

    /**
     * PRE_DAY_3
     */
    // @Excel(name = "ui.data.column.factoryMonthPlanProdFinal.preDay3")
    @ApiModelProperty(value = "PRE_DAY_3", name = "preDay3", hidden = true)
    @TableField(value = "PRE_DAY_3")
    private Long preDay3;

    /**
     * PRE_DAY_4
     */
    // @Excel(name = "ui.data.column.factoryMonthPlanProdFinal.preDay4")
    @ApiModelProperty(value = "PRE_DAY_4", name = "preDay4", hidden = true)
    @TableField(value = "PRE_DAY_4")
    private Long preDay4;

    /**
     * PRE_DAY_5
     */
    // @Excel(name = "ui.data.column.factoryMonthPlanProdFinal.preDay5")
    @ApiModelProperty(value = "PRE_DAY_5", name = "preDay5", hidden = true)
    @TableField(value = "PRE_DAY_5")
    private Long preDay5;

    /**
     * PRE_DAY_6
     */
    // @Excel(name = "ui.data.column.factoryMonthPlanProdFinal.preDay6")
    @ApiModelProperty(value = "PRE_DAY_6", name = "preDay6", hidden = true)
    @TableField(value = "PRE_DAY_6")
    private Long preDay6;

    /**
     * PRE_DAY_7
     */
    // @Excel(name = "ui.data.column.factoryMonthPlanProdFinal.preDay7")
    @ApiModelProperty(value = "PRE_DAY_7", name = "preDay7", hidden = true)
    @TableField(value = "PRE_DAY_7")
    private Long preDay7;


    /**
     * 硫化总工时
     */
//    @Excel(name = "ui.data.column.factoryMonthPlanProdFinal.totalVulcanizationMinutes", cellType = Excel.ColumnType.NUMERIC)
    @ApiModelProperty(value = "硫化总工时", name = "totalVulcanizationMinutes")
    @TableField(value = "TOTAL_VULCANIZATION_MINUTES", updateStrategy = FieldStrategy.IGNORED)
    private BigDecimal totalVulcanizationMinutes;

    /**
     * 合模压力
     */
    @Excel(name = "ui.data.column.mdmProductConstruction.mouldClampingPressure")
    @ApiModelProperty(value = "合模压力", name = "mouldClampingPressure")
    @TableField(exist = false)
    private BigDecimal mouldClampingPressure;

    /**
     * 模具型腔
     */
    @Excel(name = "ui.data.column.mdmProductConstruction.moldCavity")
    @ApiModelProperty(value = "模具型腔", name = "moldCavity")
    @TableField(exist = false)
    private String moldCavity;

    /**
     * 获取原始的合并计划集合
     *
     * @return
     */
    @ApiModelProperty(hidden = true)
    public List<SinglePlanInfoHelper> getMergePlanList() {
        if (StringUtils.isBlank(mergeInfo)) {
            return Collections.emptyList();
        }
        return JSON.parseArray(mergeInfo, SinglePlanInfoHelper.class);
    }

    /**
     * 获取更新值
     * 生产版本号、物料编码
     * 库位类别、品牌、渠道、
     * 是否有交期
     * 规格代号
     *
     * @return
     */
    @ApiModelProperty(hidden = true)
    public String getUpdateImportValue() {
        String summaryFormat = "%s|*|%s|*|%s|*|%s|*|%s|*|%d|*|%s";
        return String.format(summaryFormat, getProductionVersion(), getProductCode(),
                getLocationType(), getBrand(), getChannel(), getIsDeliveryDate(), getSpecCode());
    }

    /**
     * 获取生胎代码与硫化规格代码唯一组合键
     *
     * @return
     */
    public String getProductConstructionKey() {
        String duplicateKeyFormat = "%s|*|%s";
        return String.format(duplicateKeyFormat, getEmbryoCode(), getSpecCode());
    }

    /**
     * 获取SAP+规格代号唯一组合键
     *
     * @return
     */
    public String getProductMouldKey() {
        String duplicateKeyFormat = "%s|*|%s";
        return String.format(duplicateKeyFormat, getProductCode(), getSpecCode());
    }

    /**
     * 判断是否重复
     * 生产版本号、物料编码
     * 库位类别、品牌、渠道、
     * 是否有交期
     * 规格代号
     *
     * @return
     */
    public String getImportDuplicateKey() {
        String duplicateKeyFormat = "%s|*|%s|*|%s|*|%s|*|%s|*|%d|*|%s";
        return String.format(duplicateKeyFormat, getProductionVersion(), getProductCode(),
                getLocationType(), getBrand(), getChannel(), getIsDeliveryDate(), getSpecCode());
    }

    /**
     * 获取同版本的值
     * 分厂、年、月
     * 需求版本、排产版本
     *
     * @return
     */
    public String getSameProductionVersionKey() {
        String sameProductionVersionKeyFormat = "%s|*|%s|*|%s|*|%s|*|%s";
        return String.format(sameProductionVersionKeyFormat, getFactoryCode(), getYear(), getMonth(), getMonthPlanVersion(), getProductionVersion());
    }

    /**
     * 未排产原因国际化
     */
    @ApiModelProperty(value = "未排产原因国际化", name = "reasonI18n")
    @TableField(exist = false)
    private String reasonI18n;
}
