package com.zlt.aps.mdm.api.domain.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ruoyi.common.core.annotation.Excel;
import com.ruoyi.common.core.web.domain.BaseEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.math.BigDecimal;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：MdmProSizeWeight.java
 * 描    述：基础数据库位寸口重量对象 t_mdm_pro_size_weight
 *
 * @author zlt
 * @version 1.0
 * <p>
 * 修改记录：
 * 修改时间：...
 * 修 改 人：zlt
 * 修改内容：...
 * @date 2025-04-08
 */
@ApiModel(value = "基础数据库位寸口重量对象", description = "基础数据库位寸口重量对象 ")
@Data
@TableName(value = "T_MDM_PRO_SIZE_WEIGHT")
public class MdmProSizeWeight extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 寸口（保留2位小数）
     */
    @Excel(name = "ui.data.column.mdmProSizeWeight.proSize")
    @ApiModelProperty(value = "寸口", name = "proSize")
    @TableField(value = "PRO_SIZE")
    private BigDecimal proSize;

    /**
     * 渠道,字典:biz_channel_type
     */
    @Excel(name = "ui.data.column.mdmProSizeWeight.channel", dictType = "biz_channel_type")
    @ApiModelProperty(value = "渠道,字典:biz_channel_type", name = "channel")
    @TableField(value = "CHANNEL")
    private String channel;

    /**
     * 重量
     */
    @Excel(name = "ui.data.column.mdmProSizeWeight.singleTireWeight")
    @ApiModelProperty(value = "重量", name = "singleTireWeight")
    @TableField(value = "SINGLE_TIRE_WEIGHT")
    private BigDecimal singleTireWeight;


}