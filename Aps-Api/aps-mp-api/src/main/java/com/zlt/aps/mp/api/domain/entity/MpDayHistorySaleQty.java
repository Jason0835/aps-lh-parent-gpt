package com.zlt.aps.mp.api.domain.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.common.core.annotation.Excel;
import com.ruoyi.common.core.web.domain.BaseEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.Date;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：MpDayHistorySaleQty.java
 * 描    述：日历史销售记录对象 t_mp_day_history_sale_qty
 *
 * @author zlt
 * @version 1.0
 * <p>
 * 修改记录：
 * 修改时间：...
 * 修 改 人：zlt
 * 修改内容：...
 * @date 2025-04-09
 */
@ApiModel(value = "日历史销售记录对象", description = "日历史销售记录对象 ")
@Data
@TableName(value = "T_MP_DAY_HISTORY_SALE_QTY")
public class MpDayHistorySaleQty extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 订单日期
     */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "ui.data.column.mpDayHistorySaleQty.orderDate", width = 30, dateFormat = "yyyy-MM-dd")
    @ApiModelProperty(value = "订单日期", name = "orderDate")
    @TableField(value = "ORDER_DATE")
    private Date orderDate;

    /**
     * 分厂编码 默认116
     */
    @Excel(name = "ui.data.column.mpDayHistorySaleQty.factoryCode")
    @ApiModelProperty(value = "分厂编码 默认116", name = "factoryCode")
    @TableField(value = "FACTORY_CODE")
    private String factoryCode;

    /**
     * 物料编码
     */
    @Excel(name = "ui.data.column.mpDayHistorySaleQty.productCode")
    @ApiModelProperty(value = "物料编码", name = "productCode")
    @TableField(value = "PRODUCT_CODE")
    private String productCode;

    /**
     * 物料描述
     */
    @Excel(name = "ui.data.column.mpDayHistorySaleQty.productDesc")
    @ApiModelProperty(value = "物料描述", name = "productDesc")
    @TableField(value = "PRODUCT_DESC")
    private String productDesc;

    /**
     * 库位类型 1 内销 2 外销 3 OE
     */
    @Excel(name = "ui.data.column.mpDayHistorySaleQty.locationType")
    @ApiModelProperty(value = "库位类型 1 内销 2 外销 3 OE", name = "locationType")
    @TableField(value = "LOCATION_TYPE")
    private String locationType;

    /**
     * 销售量
     */
    @Excel(name = "ui.data.column.mpDayHistorySaleQty.saleQty")
    @ApiModelProperty(value = "销售量", name = "saleQty")
    @TableField(value = "SALE_QTY")
    private Long saleQty;

    /**
     * 订单数量
     */
    @Excel(name = "ui.data.column.mpDayHistorySaleQty.orderQty")
    @ApiModelProperty(value = "订单数量", name = "orderQty")
    @TableField(value = "ORDER_QTY")
    private Long orderQty;

    /**
     * 品牌
     */
    @TableField(exist = false)
    private String brand;

    /**
     * 导入更新的key值
     * 订单日期+分厂+物料编码+库位类别
     * +客户+订单号
     *
     * @return 结果
     */
    public String getImportUpdateKey() {
        String keyFormat = "%tF %<tT|*|%s|*|%s|*|%s";
        return String.format(keyFormat, orderDate, factoryCode, productCode, locationType);
    }
}
