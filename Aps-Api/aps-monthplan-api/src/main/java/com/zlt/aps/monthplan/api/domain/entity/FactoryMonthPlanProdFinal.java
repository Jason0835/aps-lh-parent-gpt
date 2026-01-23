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
public class FactoryMonthPlanProdFinal extends FactoryMonthPlanProductionFinalResult {

    private static final long serialVersionUID = 1L;

    /**
     * 生产物料编号
     */
    @Excel(name = "ui.data.column.factoryMonthPlanProdFinal.productCode")
    @TableField(exist = false)
    @ApiModelProperty(value = "生产物料编号", name = "productCode")
    private String productCode;

    /**
     * 生产规格描述
     */
    @Excel(name = "ui.data.column.factoryMonthPlanProdFinal.productDesc")
    @TableField(exist = false)
    @ApiModelProperty(value = "生产规格描述", name = "productDesc")
    private String productDesc;



    /**
     * 成型法
     */
    @Excel(name = "ui.data.column.mdmProductConstruction.mouldMethod", dictType = "MACHINE_TYPE")
    @ImportExcelValidated(required = true)
    @ApiModelProperty(value = "成型法", name = "mouldMethod")
    @TableField(exist = false)
    private String mouldMethod;

    /**
     * 全规格代号信息 包含规格代号及对应的成型法
     */
    @ApiModelProperty(value = "全规格代号信息", name = "specCodeInfo")
    @TableField(exist = false)
    private String specCodeInfo;

    /**
     * 库位类别
     */
    @Excel(name = "ui.data.column.factoryMonthPlanProdFinal.locationType", dictType = "biz_stor_type")
    @ImportExcelValidated(required = true)
    @ApiModelProperty(value = "库位类别", name = "locationType")
    @TableField(exist = false)
    private String locationType;

    /**
     * 渠道
     */
    @Excel(name = "ui.data.column.factoryMonthPlanProdFinal.channel", dictType = "biz_channel_type")
    @ApiModelProperty(value = "渠道", name = "channel")
    @TableField(exist = false)
    private String channel;




    /**
     * 层级
     */
    // @Excel(name = "ui.data.column.factoryMonthPlanProdFinal.hierarchy")
    @ApiModelProperty(value = "层级", name = "hierarchy")
    @TableField(exist = false)
    private String hierarchy;



    /**
     * 品名
     */
    // @Excel(name = "ui.data.column.factoryMonthPlanProdFinal.productTypeName")
    @ApiModelProperty(value = "品名", name = "productTypeName")
    @TableField(exist = false)
    private String productTypeName;

    /**
     * 等级码
     */
    // @Excel(name = "ui.data.column.factoryMonthPlanProdFinal.levelCode")
    @ApiModelProperty(value = "等级码", name = "levelCode", hidden = true)
    @TableField(exist = false)
    private String levelCode;

    /**
     * 等级名称
     */
    // @Excel(name = "ui.data.column.factoryMonthPlanProdFinal.levelName")
    @ApiModelProperty(value = "等级名称", name = "levelName", hidden = true)
    @TableField(exist = false)
    private String levelName;

    /**
     * 规格代号
     */
    @Excel(name = "ui.data.column.factoryMonthPlanProdFinal.specCode")
    @ImportExcelValidated(required = true, maxLength = 20)
    @ApiModelProperty(value = "规格代号", name = "specCode")
     @TableField(exist = false)
    private String specCode;


    /**
     * 模具
     */
    @Excel(name = "ui.data.column.factoryMonthPlanProdFinal.mouldNo")
    @ApiModelProperty(value = "模具", name = "mouldNo")
     @TableField(exist = false)
    private String mouldNo;

    /**
     * 模数
     */
    @Excel(name = "ui.data.column.factoryMonthPlanProdFinal.mouldQty")
    @ImportExcelValidated(required = true, digits = true, min = 0, max = 99999999)
    @ApiModelProperty(value = "模数", name = "mouldQty")
     @TableField(exist = false)
    private Integer mouldQty;

    /**
     * 模具编码集合，多个以,分隔
     */
    @ApiModelProperty(value = "模具编码集合，多个以,分隔", name = "mouldInfo", hidden = true)
     @TableField(exist = false)
    private String mouldInfo;

    /**
     * 合并信息json串
     */
    @ApiModelProperty(value = "合并信息json串", name = "mergeInfo", hidden = true)
     @TableField(exist = false)
    private String mergeInfo;

    /**
     * 是否有交期（0：默认没有，1：有）
     */
    @Excel(name = "ui.data.column.factoryMonthPlanProdFinal.isDeliveryDate", dictType = "biz_yes_no")
    @ImportExcelValidated(required = true)
    @ApiModelProperty(value = "是否有交期", name = "isDeliveryDate")
     @TableField(exist = false)
    private Integer isDeliveryDate;



    /**
     * 开始日期
     */
    @Excel(name = "ui.data.column.factoryMonthPlanProdFinal.beginDate")
    @ImportExcelValidated(min = 1, max = 31)
    @ApiModelProperty(value = "开始日期", name = "beginDate")
     @TableField(exist = false)
    private Integer beginDate;




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


    /**
     * eudr数量
     */
    @ApiModelProperty(value = "eudr数量", name = "eudrQty")
    @TableField(exist = false)
    private Integer eudrQty;

    /**
     * 非eudr数量
     */
    @ApiModelProperty(value = "非eudr数量", name = "nonEudrQty")
    @TableField(exist = false)
    private Integer nonEudrQty;
}
