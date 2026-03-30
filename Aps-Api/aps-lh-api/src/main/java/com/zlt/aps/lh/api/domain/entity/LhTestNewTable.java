package com.zlt.aps.lh.api.domain.entity;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.common.core.annotation.Excel;
import com.ruoyi.common.core.web.domain.BaseEntity;
import com.zlt.aps.common.core.annotation.ImportValidated;
import com.zlt.common.annotation.ImportExcelValidated;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：LhTestNewTable.java
 * 描    述：硫化排程结果对象 t_lh_schedule_result
 *@author zlt
 *@date 2025-03-05
 *@version 1.0
 *
 *  修改记录：
 *     修改时间：...
 *     修 改 人：zlt
 *     修改内容：...
 */

@ApiModel(value = "TEST对象", description = "TEST对象 ")
@Data
@TableName(value = "newtable2")
public class LhTestNewTable extends BaseEntity implements Serializable {


    private static final long serialVersionUID = 2597208202828961199L;

    /** 生胎编号 */
    @ApiModelProperty(value = "生胎编号", name = "embryoCode")
    @TableField(value = "EMBRYO_CODE")
    private String embryoCode;

    @ApiModelProperty(value = "列2", name = "col2")
    @TableField(value = "col2")
    private String col2;

    @ApiModelProperty(value = "列3", name = "col3")
    @TableField(value = "col3")
    private String col3;

    @ApiModelProperty(value = "列4", name = "col4")
    @TableField(value = "col4")
    private String col4;

    @ApiModelProperty(value = "列5", name = "col5")
    @TableField(value = "col5")
    private String col5;

    @ApiModelProperty(value = "列6", name = "col6")
    @TableField(value = "col6")
    private String col6;

}