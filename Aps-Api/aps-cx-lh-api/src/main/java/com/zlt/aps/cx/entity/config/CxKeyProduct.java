package com.zlt.aps.cx.entity.config;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.common.core.annotation.Excel;
import com.ruoyi.common.core.web.domain.BaseEntity;
import com.zlt.aps.common.core.annotation.ImportValidated;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Date;

/**
 * 关键产品配置实体
 * 
 * 定义关键产品列表，用于开产首班排除等场景判断
 *
 * @author APS Team
 */
@Data
@TableName("T_CX_KEY_PRODUCT")
@ApiModel(value = "关键产品配置")
public class CxKeyProduct extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @Excel(name = "ui.data.column.cxKeyProduct.structureName")
    @ApiModelProperty(value = "结构名称")
    @TableField("STRUCTURE_NAME")
    @ImportValidated(maxLength = 100)
    private String structureName;

    @Excel(name = "ui.data.column.cxKeyProduct.embryoCode")
    @ApiModelProperty(value = "胎胚编码")
    @TableField("EMBRYO_CODE")
    @ImportValidated(required = true, maxLength = 50)
    private String embryoCode;

    @Excel(name = "ui.data.column.cxKeyProduct.embryoDesc")
    @ApiModelProperty(value = "胎胚描述")
    @TableField("EMBRYO_DESC")
    @ImportValidated(maxLength = 100)
    private String embryoDesc;

    @Excel(name = "ui.data.column.cxKeyProduct.isActive", dictType = "biz_yes_no")
    @ApiModelProperty(value = "是否启用：0-禁用 1-启用")
    @TableField("IS_ACTIVE")
    private Integer isActive;
}
