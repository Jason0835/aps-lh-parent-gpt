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
 * 文件名称：MpHistorySaleQty.java
 * 描    述：历史销售记录对象 T_MDM_HISTORY_SALE_RECORD
 *@author hsc
 *@date 2025-02-13
 *@version 1.0
 *
 *  修改记录：
 *     修改时间：...
 *     修 改 人：hsc
 *     修改内容：...
 */

@ApiModel(value = "历史销售记录对象", description = "历史销售记录对象 ")
@Data
@TableName(value = "T_MDM_HISTORY_SALE_RECORD")
//@KeySequence(value = "SEQ_ISTORY_SALE_QTY")
public class MpHistorySaleQty extends BaseEntity {

    private static final long serialVersionUID = 1L;


     /** 年份 */
    @Excel(name = "ui.data.column.mpHistorySaleQty.year")
    @ApiModelProperty(value = "年份", name = "year")
    @TableField(value = "YEAR")
    private Integer year;

    /** 月份 */
    @Excel(name = "ui.data.column.mpHistorySaleQty.month")
    @ApiModelProperty(value = "月份", name = "month")
    @TableField(value = "MONTH")
    private Integer month;

    /**
     * 分厂编码 默认116
     */
    @Excel(name = "ui.data.column.mpHistorySaleQty.factoryCode")
    @ApiModelProperty(value = "分厂编码 默认116", name = "factoryCode")
    @TableField(value = "FACTORY_CODE")
    private String factoryCode;

    /** 物料编码 */
    @Excel(name = "ui.data.column.mpHistorySaleQty.productCode")
    @ApiModelProperty(value = "物料编码", name = "productCode")
    @TableField(value = "PRODUCT_CODE")
    private String productCode;

    /** 物料描述 */
    @Excel(name = "ui.data.column.mpHistorySaleQty.productDesc")
    @ApiModelProperty(value = "物料描述", name = "productDesc")
    @TableField(value = "PRODUCT_DESC")
    private String productDesc;

    /** 库位类型 1 内销 2 外销 3 OE */
    @Excel(name = "ui.data.column.mpHistorySaleQty.locationType")
    @ApiModelProperty(value = "库位类型 1 内销 2 外销 3 OE", name = "locationType")
    @TableField(value = "LOCATION_TYPE")
    private String locationType;

    /** 销售量 */
    @Excel(name = "ui.data.column.mpHistorySaleQty.saleQty")
    @ApiModelProperty(value = "销售量", name = "saleQty")
    @TableField(value = "SALE_QTY")
    private Long saleQty;

    /** 订单数量 */
    @Excel(name = "ui.data.column.mpHistorySaleQty.orderQty")
    @ApiModelProperty(value = "订单数量", name = "orderQty")
    @TableField(value = "ORDER_QTY")
    private Long orderQty;

    @ApiModelProperty("创建时间")
    @JsonFormat(
            pattern = "yyyy-MM-dd"
    )
    @TableField(
            value = "CREATE_TIME",
            fill = FieldFill.INSERT,
            jdbcType = JdbcType.TIMESTAMP
    )
    private Date createTime;

    /** 创建人人 */
    @ApiModelProperty(value = "创建人", notes = "虚字段从createBy转义", name = "createByName")
    @TableField(exist = false)
    private String createByName;

}
