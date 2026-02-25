package com.zlt.aps.mp.api.domain.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ruoyi.common.core.annotation.Excel;
import com.ruoyi.common.core.web.domain.BaseEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：DpArea.java
 * 描    述：区域对象 t_dp_area
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
@ApiModel(value = "区域对象", description = "区域对象 ")
@Data
@TableName(value = "T_DP_AREA")
public class DpArea extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 编码
     */
    @Excel(name = "ui.data.column.dpArea.areaCode")
    @ApiModelProperty(value = "编码", name = "areaCode")
    @TableField(value = "AREA_CODE")
    private String areaCode;

    /**
     * 名称
     */
    @Excel(name = "ui.data.column.dpArea.areaName")
    @ApiModelProperty(value = "名称", name = "areaName")
    @TableField(value = "AREA_NAME")
    private String areaName;

    /**
     * 类型01大洲，02国家/地区,03省/州，04市，05区/县
     */
    @Excel(name = "ui.data.column.dpArea.areaType")
    @ApiModelProperty(value = "类型01大洲，02国家/地区,03省/州，04市，05区/县", name = "areaType")
    @TableField(value = "AREA_TYPE")
    private String areaType;

    /**
     * 上级编码
     */
    @Excel(name = "ui.data.column.dpArea.parentCode")
    @ApiModelProperty(value = "上级编码", name = "parentCode")
    @TableField(value = "PARENT_CODE")
    private String parentCode;

    /**
     * CRM的编码
     */
    @Excel(name = "ui.data.column.dpArea.crmCode")
    @ApiModelProperty(value = "CRM的编码", name = "crmCode")
    @TableField(value = "CRM_CODE")
    private String crmCode;

    /**
     * EUDR要求
     */
    @Excel(name = "ui.data.column.dpArea.isEudr")
    @ApiModelProperty(value = "EUDR要求", name = "isEudr")
    @TableField(value = "IS_EUDR")
    private String isEudr;

    /**
     * NC编码
     */
    @Excel(name = "ui.data.column.dpArea.ncCode")
    @ApiModelProperty(value = "NC编码", name = "ncCode")
    @TableField(value = "NC_CODE")
    private String ncCode;

    /**
     * 区域名称国际化
     */
    @ApiModelProperty(value = "区域名称国际化", name = "areaNameI18n")
    @TableField(exist = false)
    private String areaNameI18n;

}
