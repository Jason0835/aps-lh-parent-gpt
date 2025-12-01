package com.zlt.aps.monthplan.api.domain.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ruoyi.common.core.annotation.Excel;
import com.ruoyi.common.core.web.domain.BaseEntity;
import com.zlt.common.annotation.ImportExcelValidated;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：LocationChannelConfiguration.java
 * 描    述：库位类别渠道品牌配置对象 t_mdm_location_channel
 *@author ZLT
 *@date 2025-02-28
 *@version 1.0
 *
 *  修改记录：
 *     修改时间：...
 *     修 改 人：ZLT
 *     修改内容：...
 */
@ApiModel(value = "库位类别渠道品牌配置对象", description = "库位类别渠道品牌配置对象")
@Data
@TableName(value = "T_MDM_LOCATION_CHANNEL")
public class LocationChannelConfiguration extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 市场类别
     */
    @Excel(name = "ui.data.column.LocationChannelConfiguration.marketCategory")
    @ImportExcelValidated(required = true)
    @ApiModelProperty(value = "市场类别", name = "marketCategory")
    @TableField(value = "MARKET_CATEGORY")
    private String marketCategory;

     /** 分厂编号，字典：biz_factory_name */
    @Excel(name = "ui.data.column.LocationChannelConfiguration.factoryCode", dictType = "biz_factory_name")
    @ImportExcelValidated(required = true)
    @ApiModelProperty(value = "分厂编号，字典：biz_factory_name", name = "factoryCode")
    @TableField(value = "FACTORY_CODE")
    private String factoryCode;

    /** 库位类别，字典：biz_stor_type */
    @Excel(name = "ui.data.column.LocationChannelConfiguration.locationType", dictType = "biz_stor_type")
    @ImportExcelValidated(required = true)
    @ApiModelProperty(value = "库位类别，字典：biz_stor_type", name = "locationType")
    @TableField(value = "LOCATION_TYPE")
    private Integer locationType;

    /**
     * 渠道，字典：biz_channel_type
     */
    @Excel(name = "ui.data.column.LocationChannelConfiguration.channel", dictType = "biz_channel_type")
    @ImportExcelValidated(required = true)
    @ApiModelProperty(value = "渠道，字典：biz_channel_type", name = "channel")
    @TableField(value = "CHANNEL")
    private String channel;

    /**
     * 品牌，字典：biz_brand_type
     */
    @Excel(name = "ui.data.column.LocationChannelConfiguration.brand", dictType = "biz_brand_type")
    @ImportExcelValidated(required = true)
    @ApiModelProperty(value = "品牌，字典：biz_brand_type", name = "brand")
    @TableField(value = "BRAND")
    private String brand;


}