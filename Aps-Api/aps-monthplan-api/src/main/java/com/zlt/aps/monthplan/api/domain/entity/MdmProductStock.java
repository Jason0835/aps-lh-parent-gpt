package com.zlt.aps.monthplan.api.domain.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.common.core.annotation.Excel;
import com.ruoyi.common.core.web.domain.BaseEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.apache.ibatis.type.JdbcType;

import java.util.Date;


/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：MdmProductStock.java
 * 描    述：成品库存对象 t_mdm_product_stock
 *
 * @author yelq
 * @version 1.0
 * <p>
 * 修改记录：
 * 修改时间：...
 * 修 改 人：yelq
 * 修改内容：...
 * @date 2025-12-20
 */
@ApiModel(value = "成品库存对象", description = "成品库存对象 ")
@Data
@TableName(value = "T_MDM_PRODUCT_STOCK")
public class MdmProductStock extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 工厂编号
     */
    @Excel(name = "ui.data.column.productStock.factoryCode", dictType = "biz_factory_name")
    @ApiModelProperty(value = "工厂编号", name = "factoryCode")
    @TableField(value = "FACTORY_CODE")
    private String factoryCode;

    /**
     * 库存日期
     */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "ui.data.column.productStock.stockDate", width = 30, dateFormat = "yyyy-MM-dd")
    @ApiModelProperty(value = "库存日期", name = "stockDate")
    @TableField(value = "STOCK_DATE")
    private Date stockDate;

    /**
     * 产品品类，TBR 全钢 PCR 半钢
     */
    @Excel(name = "ui.data.column.productStock.productTypeCode", dictType = "biz_product_type")
    @ApiModelProperty(value = "产品品类", name = "productTypeCode")
    @TableField(value = "PRODUCT_TYPE_CODE")
    private String productTypeCode;

    /**
     * 品牌
     */
    @Excel(name = "ui.data.column.productStock.brand", dictType = "biz_brand_type")
    @ApiModelProperty(value = "品牌", name = "brand")
    @TableField(value = "BRAND")
    private String brand;

    /**
     * 产品结构
     */
    @Excel(name = "ui.data.column.productStock.structureName")
    @ApiModelProperty(value = "产品结构", name = "structureName")
    @TableField(value = "STRUCTURE_NAME")
    private String structureName;

    /**
     * 库位类别
     */
    @Excel(name = "ui.data.column.productStock.locationType", dictType = "biz_stor_type")
    @ApiModelProperty(value = "库位类别", name = "locationType")
    @TableField(value = "LOCATION_TYPE")
    private String locationType;

    /**
     * MES物料编码
     */
//    @Excel(name = "ui.data.column.productStock.mesMaterialCode")
    @ApiModelProperty(value = "MES物料编码", name = "mesMaterialCode")
    @TableField(value = "MES_MATERIAL_CODE")
    private String mesMaterialCode;

    /**
     * 物料编码
     */
    @Excel(name = "ui.data.column.productStock.materialCode")
    @ApiModelProperty(value = "物料编码", name = "materialCode")
    @TableField(value = "MATERIAL_CODE")
    private String materialCode;

    /**
     * 物料描述
     */
    @Excel(name = "ui.data.column.productStock.materialDesc")
    @ApiModelProperty(value = "物料描述", name = "materialDesc")
    @TableField(value = "MATERIAL_DESC")
    private String materialDesc;

    /**
     * 库存数量
     */
    @Excel(name = "ui.data.column.productStock.stockQty", cellType = Excel.ColumnType.NUMERIC)
    @ApiModelProperty(value = "库存数量", name = "stockQty")
    @TableField(value = "STOCK_QTY")
    private Integer stockQty;

    /**
     * 年周号
     */
    @Excel(name = "ui.data.column.productStock.weekYear")
    @ApiModelProperty(value = "年周号", name = "weekYear")
    @TableField(value = "WEEK_YEAR")
    private String weekYear;

    /**
     * 动平衡
     */
//    @Excel(name = "ui.data.column.productStock.isDynamicBalance", dictType = "biz_yes_no")
    @ApiModelProperty(value = "动平衡", name = "isDynamicBalance")
    @TableField(value = "IS_DYNAMIC_BALANCE")
    private String isDynamicBalance;

    /**
     * 均匀性
     */
//    @Excel(name = "ui.data.column.productStock.isUniformity", dictType = "biz_yes_no")
    @ApiModelProperty(value = "均匀性", name = "isUniformity")
    @TableField(value = "IS_UNIFORMITY")
    private String isUniformity;

    /**
     * 是否超3个月胎
     */
    @Excel(name = "ui.data.column.productStock.isExceedThreeMonth", dictType = "biz_yes_no")
    @ApiModelProperty(value = "是否超3个月胎", name = "isExceedThreeMonth")
    @TableField(value = "IS_EXCEED_THREE_MONTH")
    private String isExceedThreeMonth;

    /**
     * 是否超6个月胎
     */
    @Excel(name = "ui.data.column.productStock.isExceedSixMonth", dictType = "biz_yes_no")
    @ApiModelProperty(value = "是否超6个月胎", name = "isExceedSixMonth")
    @TableField(value = "IS_EXCEED_SIX_MONTH")
    private String isExceedSixMonth;

    /**
     * 是否超9个月胎
     */
//    @Excel(name = "ui.data.column.productStock.isExceedNineMonth", dictType = "biz_yes_no")
    @ApiModelProperty(value = "是否超9个月胎", name = "isExceedNineMonth")
    @TableField(value = "IS_EXCEED_NINE_MONTH")
    private String isExceedNineMonth;

    /**
     * 是否超12个月胎
     */
    @Excel(name = "ui.data.column.productStock.isExceedTwelveMonth", dictType = "biz_yes_no")
    @ApiModelProperty(value = "是否超12个月胎", name = "isExceedTwelveMonth")
    @TableField(value = "IS_EXCEED_TWELVE_MONTH")
    private String isExceedTwelveMonth;

    /**
     * 是否超龄胎
     */
    @Excel(name = "ui.data.column.productStock.isExceedTire", dictType = "biz_yes_no")
    @ApiModelProperty(value = "是否超龄胎 ", name = "isExceedTire")
    @TableField(value = "IS_EXCEED_TIRE")
    private String isExceedTire;

    @Excel(name = "ui.data.column.demandPlan.updateTime", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty("更新时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField(value = "UPDATE_TIME", fill = FieldFill.INSERT_UPDATE, jdbcType = JdbcType.TIMESTAMP)
    private Date updateTime;

    /**
     * 年周号整数值
     */
    @TableField(exist = false)
    private Integer stockWeekYear;

    /**
     * 对冲后，剩余库存总量
     */
    @TableField(exist = false)
    private Integer leftOverQty;

    /**
     * 花纹
     */
    @ApiModelProperty(value = "花纹", name = "pattern")
    @TableField(exist = false)
    private String pattern;
    /**
     * 规格
     */
    @ApiModelProperty(value = "规格", name = "specifications")
    @TableField(exist = false)
    private String specifications;

    /**
     * 以分厂+物料为维度，转换库存
     *
     * @return
     */
    public String getGroupKey() {
        String keyFormat = "%s|*|%s";
        return String.format(keyFormat, factoryCode, materialDesc);
    }

    /**
     * 是按年周号 + 动平衡 + 均匀性匹配的库存数
     */
    public String getStockGroupKey() {
        String keyFormat = "%s|*|%s|*|%s";
        return String.format(keyFormat, weekYear, isDynamicBalance,isUniformity);
    }

    public String getStockWithoutOrderGroupKey() {
        String keyFormat = "%s|%s|*|%s|*|%s";
        return String.format(keyFormat, materialCode,weekYear, isDynamicBalance,isUniformity);
    }

    public String getAlternateMaterialGroupKey() {
        String keyFormat = "%s|*|%s|*|%s";
        return String.format(keyFormat, brand,specifications, pattern);
    }



    /**
     * 初始化超龄状态
     *
     * @param code 状态码
     * @return 对象
     */
    public void initExceedTireStatus(String code) {
        this.setIsExceedTire(code);
        this.setIsExceedThreeMonth(code);
        this.setIsExceedSixMonth(code);
        this.setIsExceedNineMonth(code);
        this.setIsExceedTwelveMonth(code);
    }

    /**
     * 设置超期状态为 YES（按需指定哪些字段需要赋值）
     *
     * @param yesCode        是-编码
     * @param isExceedTire   是否超期（总标识）
     * @param isExceedThree  是否超3个月
     * @param isExceedSix    是否超6个月
     * @param isExceedNine   是否超9个月
     * @param isExceedTwelve 是否超12个月
     */
    public void setExceedStatusToYes(String yesCode,
                                     boolean isExceedTire,
                                     boolean isExceedThree,
                                     boolean isExceedSix,
                                     boolean isExceedNine,
                                     boolean isExceedTwelve) {
        if (isExceedTire) {
            this.setIsExceedTire(yesCode);
        }
        if (isExceedThree) {
            this.setIsExceedThreeMonth(yesCode);
        }
        if (isExceedSix) {
            this.setIsExceedSixMonth(yesCode);
        }
        if (isExceedNine) {
            this.setIsExceedNineMonth(yesCode);
        }
        if (isExceedTwelve) {
            this.setIsExceedTwelveMonth(yesCode);
        }
    }
}
