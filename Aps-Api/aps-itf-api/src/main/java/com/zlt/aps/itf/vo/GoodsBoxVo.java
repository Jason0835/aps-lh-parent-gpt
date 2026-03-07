package com.zlt.aps.itf.vo;

import com.baomidou.mybatisplus.annotation.TableField;
import com.ruoyi.common.core.annotation.Excel;
import com.ruoyi.common.core.web.domain.BaseEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：GoodsBox.java
 * 描    述：1020.基础_商品_装箱规格对象 t_bd_goods_box
 *@author zlt
 *@date 2023-11-28
 *@version 1.0
 *
 *  修改记录：
 *     修改时间：...
 *     修 改 人：zlt
 *     修改内容：...
 */
@ApiModel(value = "商品装箱规格对象", description = "商品装箱规格对象 ")
@Data
public class GoodsBoxVo extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /** 商品编码 */
    @Excel(name = "ui.data.column.GoodsBox.gCode")
    @ApiModelProperty(value = "商品编码", name = "gCode")
    @TableField(value = "G_CODE")
    private String gCode;

    /** 装箱方式40/20 */
    @Excel(name = "ui.data.column.GoodsBox.packMethod")
    @ApiModelProperty(value = "装箱方式40/20", name = "packMethod")
    @TableField(value = "PACK_METHOD")
    private String packMethod;

    /** 商品工厂 */
    @Excel(name = "ui.data.column.GoodsBox.facCode")
    @ApiModelProperty(value = "商品工厂", name = "facCode")
    @TableField(value = "FAC_CODE")
    private String facCode;

    /** 装箱单位 */
    @Excel(name = "ui.data.column.GoodsBox.boxUnit")
    @ApiModelProperty(value = "装箱单位 32", name = "boxUnit")
    @TableField(value = "BOX_UNIT")
    private String boxUnit;

    /** 工厂重量 */
    @Excel(name = "ui.data.column.GoodsBox.orgWeight")
    @ApiModelProperty(value = "工厂重量", name = "orgWeight")
    @TableField(value = "ORG_WEIGHT")
    private BigDecimal orgWeight;

    /** 工厂体积 */
    @Excel(name = "ui.data.column.GoodsBox.orgVolume")
    @ApiModelProperty(value = "工厂体积", name = "orgVolume")
    @TableField(value = "ORG_VOLUME")
    private BigDecimal orgVolume;

    /** 紧凑数量 */
    @Excel(name = "ui.data.column.GoodsBox.compactQty")
    @ApiModelProperty(value = "紧凑数量", name = "compactQty")
    @TableField(value = "COMPACT_QTY")
    private BigDecimal compactQty;

    /** 内装数量 */
    @Excel(name = "ui.data.column.GoodsBox.boxInnerQty")
    @ApiModelProperty(value = "内装数量 20,8", name = "boxInnerQty")
    @TableField(value = "BOX_INNER_QTY")
    private BigDecimal boxInnerQty;

    /** 质控状态 */
    @Excel(name = "ui.data.column.GoodsBox.qualityStateCode")
    @ApiModelProperty(value = "质控状态", name = "qualityStateCode")
    @TableField(value = "QUALITY_STATE_CODE")
    private String qualityStateCode;

    /** 长度单位 */
    @Excel(name = "ui.data.column.GoodsBox.lengthUnit")
    @ApiModelProperty(value = "长度单位 10", name = "lengthUnit")
    @TableField(value = "LENGTH_UNIT")
    private String lengthUnit;

    /** 长 */
    @Excel(name = "ui.data.column.GoodsBox.packLong")
    @ApiModelProperty(value = "长 20,8", name = "packLong")
    @TableField(value = "PACK_LONG")
    private BigDecimal packLong;

    /** 宽 */
    @Excel(name = "ui.data.column.GoodsBox.packWidth")
    @ApiModelProperty(value = "宽 20,8", name = "packWidth")
    @TableField(value = "PACK_WIDTH")
    private BigDecimal packWidth;

    /** 高 */
    @Excel(name = "ui.data.column.GoodsBox.packHigh")
    @ApiModelProperty(value = "高 20,8", name = "packHigh")
    @TableField(value = "PACK_HIGH")
    private BigDecimal packHigh;

    /** 体积(m3) */
    @Excel(name = "ui.data.column.GoodsBox.volume")
    @ApiModelProperty(value = "体积(m3) 20,8", name = "volume")
    @TableField(value = "VOLUME")
    private BigDecimal volume;

    /** 毛重(kg) */
    @Excel(name = "ui.data.column.GoodsBox.grossWeight")
    @ApiModelProperty(value = "毛重(kg) 20,8", name = "grossWeight")
    @TableField(value = "GROSS_WEIGHT")
    private BigDecimal grossWeight;

    /** 净重(kg) */
    @Excel(name = "ui.data.column.GoodsBox.netWeight")
    @ApiModelProperty(value = "净重(kg) 20,8", name = "netWeight")
    @TableField(value = "NET_WEIGHT")
    private BigDecimal netWeight;

    /** 尺寸描述 */
    @Excel(name = "ui.data.column.GoodsBox.sizeDesc")
    @ApiModelProperty(value = "尺寸描述 64", name = "sizeDesc")
    @TableField(value = "SIZE_DESC")
    private String sizeDesc;

    /** 是否默认(0-否,1-是) */
    @Excel(name = "ui.data.column.GoodsBox.isDefault")
    @ApiModelProperty(value = "是否默认(0-否,1-是)", name = "isDefault")
    @TableField(value = "IS_DEFAULT")
    private String isDefault;


    /** BIPID */
    @Excel(name = "ui.data.column.customer.bipId")
    @ApiModelProperty(value = "BIPID", name = "bipId", hidden = true)
    @TableField(value = "BIP_ID")
    private String bipId;

    /** 商品名称 */
    @Excel(name = "ui.data.column.Goods.gName")
    @ApiModelProperty(value = "商品名称", name = "gName")
    @TableField(exist = false)
    private String goodsName;


    /** 商品名称 */
    @Excel(name = "ui.data.column.Goods.gName")
    @ApiModelProperty(value = "商品名称I18n", name = "gNameI18n")
    @TableField(exist = false)
    private String goodsNameI18n;

    /** 单位 */
    @Excel(name = "ui.data.column.Goods.unit")
    @ApiModelProperty(value = "单位Name", name = "unitName")
    @TableField(exist = false)
    private String unitName;

    /** 装箱单位 */
    @Excel(name = "ui.data.column.GoodsBox.boxUnit")
    @ApiModelProperty(value = "装箱单位", name = "boxUnitName")
    @TableField(exist = false)
    private String boxUnitName;



    /**  */
    @ApiModelProperty(value = "商品编号（多）", name = "gCodeList")
    @TableField(exist = false)
    private List<String> gCodeList;


    @TableField(exist = false)
    private List<GoodsBoxVo> goodsBoxes;

    /** 工厂名称 */
    @Excel(name = "ui.data.column.GoodsFac.facName")
    @ApiModelProperty(value = "工厂名称 64", name = "facName")
    @TableField(exist = false)
    private String facName;

    /** 工厂名称i18n */
    @Excel(name = "ui.data.column.GoodsFac.facName")
    @ApiModelProperty(value = "工厂名称i18n", name = "facNameI18n")
    @TableField(exist = false)
    private String facNameI18n;

    /** 物料描述 */
    @ApiModelProperty(value = "物料描述", name = "materialDesc", hidden = true)
    @TableField(exist = false)
    private String materialDesc;

    /** 质控状态 */
    @ApiModelProperty(value = "质控状态", name = "qualityStateCodeName")
    @TableField(exist = false)
    private String qualityStateCodeName;

    public String getgCode() {
        return gCode;
    }

    public void setgCode(String gCode) {
        this.gCode = gCode;
    }

    public String getGCode() {
        return gCode;
    }

    public void setGCode(String gCode) {
        this.gCode = gCode;
    }

    public List<String> getgCodeList() {
        return gCodeList;
    }

    public void setgCodeList(List<String> gCodeList) {
        this.gCodeList = gCodeList;
    }

    public List<String> getGCodeList() {
        return gCodeList;
    }

    public void setGCodeList(List<String> gCodeList) {
        this.gCodeList = gCodeList;
    }
}