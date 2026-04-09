package com.zlt.aps.cx.api.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.ruoyi.common.core.annotation.Excel;
import com.ruoyi.common.core.web.domain.BaseEntity;
import com.zlt.aps.common.core.annotation.ImportValidated;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：CxKeyProduct.java
 * 描    述：关键产品配置对象 T_CX_KEY_PRODUCT
 * @author APS Team
 * @date 2026-04-09
 * @version 1.0
 *
 * 修改记录：
 *     修改时间：...
 *     修 改 人：...
 *     修改内容：...
 */
@Data
@TableName("T_CX_KEY_PRODUCT")
@ApiModel(value = "关键产品配置")
public class CxKeyProduct extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "主键ID")
    @TableId(value = "ID", type = IdType.AUTO)
    private Long id;

    @Excel(name = "ui.data.column.cxKeyProduct.embryoCode")
    @ApiModelProperty(value = "胎胚编码")
    @TableField("EMBRYO_CODE")
    @ImportValidated(required = true, maxLength = 50)
    private String embryoCode;

    @Excel(name = "ui.data.column.cxKeyProduct.structureName")
    @ApiModelProperty(value = "结构名称")
    @TableField("STRUCTURE_NAME")
    @ImportValidated(maxLength = 100)
    private String structureName;

    @Excel(name = "ui.data.column.cxKeyProduct.isActive", dictType = "biz_yes_no")
    @ApiModelProperty(value = "是否启用：0-禁用 1-启用")
    @TableField("IS_ACTIVE")
    private Integer isActive;
}
