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
 * 文件名称：MdmStructureName.java
 * 描    述：结构信息(SKU与结构关系选择结构使用)对象 t_mdm_structure_name
 *@author zlt
 *@date 2026-02-26
 *@version 1.0
 *
 *  修改记录：
 *     修改时间：...
 *     修 改 人：zlt
 *     修改内容：...
 */
@ApiModel(value = "结构信息(SKU与结构关系选择结构使用)对象", description = "结构信息(SKU与结构关系选择结构使用)对象")
@Data
@TableName(value = "T_MDM_STRUCTURE_NAME")
public class MdmStructureName extends BaseEntity {

    private static final long serialVersionUID = 1L;

     /** 结构 */
    @Excel(name = "ui.data.column.mdmStructureName.structureName")
    @ApiModelProperty(value = "结构", name = "structureName")
    @TableField(value = "STRUCTURE_NAME")
    private String structureName;

}