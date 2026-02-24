package com.zlt.aps.dp.api.domain.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ruoyi.common.core.annotation.Excel;
import com.ruoyi.common.core.web.domain.BaseEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：DpNation.java
 * 描    述：国家地区对象 T_DP_NATION
 *
 * @author zlt
 * @version 1.0
 * <p>
 * 修改记录：
 * 修改时间：...
 * 修 改 人：zlt
 * 修改内容：...
 * @date 2025-12-29
 */
@ApiModel(value = "国家地区对象", description = "国家地区对象")
@Data
@TableName(value = "T_DP_NATION")
public class DpNation extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 编码
     */
    @Excel(name = "ui.data.column.dpNation.nationCode")
    @ApiModelProperty(value = "编码", name = "nationCode")
    @TableField(value = "NATION_CODE")
    private String nationCode;

    /**
     * 名称
     */
    @Excel(name = "ui.data.column.dpNation.nationName")
    @ApiModelProperty(value = "名称", name = "nationName")
    @TableField(value = "NATION_NAME")
    private String nationName;

    /**
     * 类型01大洲，02国家/地区,03省/州，04市，05区/县
     */
    @Excel(name = "ui.data.column.dpNation.nationType")
    @ApiModelProperty(value = "类型01大洲，02国家/地区,03省/州，04市，05区/县", name = "nationType")
    @TableField(value = "NATION_TYPE")
    private String nationType;

    /**
     * 上级编码
     */
    @Excel(name = "ui.data.column.dpNation.parentCode")
    @ApiModelProperty(value = "上级编码", name = "parentCode")
    @TableField(value = "PARENT_CODE")
    private String parentCode;

    /**
     * CRM的编码
     */
    @Excel(name = "ui.data.column.dpNation.crmCode")
    @ApiModelProperty(value = "CRM的编码", name = "crmCode")
    @TableField(value = "CRM_CODE")
    private String crmCode;

    /**
     * EUDR要求
     */
    @Excel(name = "ui.data.column.dpNation.isEudr")
    @ApiModelProperty(value = "EUDR要求", name = "isEudr")
    @TableField(value = "IS_EUDR")
    private String isEudr;

    /**
     * NC编码
     */
    @Excel(name = "ui.data.column.dpNation.ncCode")
    @ApiModelProperty(value = "NC编码", name = "ncCode")
    @TableField(value = "NC_CODE")
    private String ncCode;

    /**
     * 区域名称国际化
     */
    @ApiModelProperty(value = "区域名称国际化", name = "nationNameI18n")
    @TableField(exist = false)
    private String nationNameI18n;

}
