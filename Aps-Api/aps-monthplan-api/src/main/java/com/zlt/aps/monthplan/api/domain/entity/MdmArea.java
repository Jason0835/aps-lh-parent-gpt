package com.zlt.aps.monthplan.api.domain.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ruoyi.common.core.annotation.Excel;
import com.ruoyi.common.core.web.domain.BaseEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：MdmAreaCapaAllocation.java
 * 描    述：区域产能分配对象 t_mdm_area_capa_allocation
 *
 * @author zlt
 * @version 1.0
 * <p>
 * 修改记录：
 * 修改时间：...
 * 修 改 人：zlt
 * 修改内容：...
 * @date 2025-12-08
 */
@ApiModel(value = "区域对象", description = "区域对象")
@Data
@TableName(value = "T_MDM_AREA")
public class MdmArea extends BaseEntity {

    /**
     * 工厂编号，字典：biz_factory_name
     */
    @Excel(name = "ui.data.column.mdmArea.factoryCode")
    @ApiModelProperty(value = "区域编码", name = "areaCode")
    @TableField(value = "AREA_CODE")
    private String areaCode;

    @Excel(name = "ui.data.column.mdmArea.areaName")
    @ApiModelProperty(value = "区域名称", name = "areaName")
    @TableField(value = "AREA_NAME")
    private String areaName;

    @Excel(name = "ui.data.column.mdmArea.areaType")
    @ApiModelProperty(value = "类型01大洲，02国家/地区,03省/州，04市，05区/县", name = "areaType")
    @TableField(value = "AREA_TYPE")
    private String areaType;

    @Excel(name = "ui.data.column.mdmArea.parentCode")
    @ApiModelProperty(value = "上级编码", name = "parentCode")
    @TableField(value = "PARENT_CODE")
    private String parentCode;

    @Excel(name = "ui.data.column.mdmArea.crmCode")
    @ApiModelProperty(value = "CRM的编码", name = "crmCode")
    @TableField(value = "CRM_CODE")
    private String crmCode;

    @Excel(name = "ui.data.column.mdmArea.isEudr")
    @ApiModelProperty(value = "EUDR要求", name = "isEudr")
    @TableField(value = "IS_EUDR")
    private String isEudr;

    @Excel(name = "ui.data.column.mdmArea.ncCode")
    @ApiModelProperty(value = "NC编码", name = "ncCode")
    @TableField(value = "NC_CODE")
    private String ncCode;
}
