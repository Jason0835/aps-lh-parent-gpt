package com.zlt.aps.monthplan.api.domain.entity;

import com.ruoyi.common.core.web.domain.BaseEntity;
import lombok.Data;
import com.ruoyi.common.core.annotation.Excel;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import com.zlt.common.annotation.EntityMapping;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.zlt.common.domain.CommonBusiEntity;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：MpCheckItemRecord.java
 * 描    述：S2-1202 检测项记录对象 t_mp_check_item_record
 *@author hsc
 *@date 2026-01-29
 *@version 1.0
 *
 *  修改记录：
 *     修改时间：...
 *     修 改 人：hsc
 *     修改内容：...
 */

@ApiModel(value = "S2-1202 检测项记录对象", description = "S2-1202 检测项记录对象 ")
@Data
@TableName(value = "T_MP_CHECK_ITEM_RECORD")
public class MpCheckItemRecord extends BaseEntity {

    private static final long serialVersionUID = 1L;

     /** 检测项 */
    @Excel(name = "ui.data.column.checkItemRecord.checkItem")
    @ApiModelProperty(value = "检测项", name = "checkItem")
    @TableField(value = "CHECK_ITEM")
    private String checkItem;

    /** 检测内容 */
    @Excel(name = "ui.data.column.checkItemRecord.checkContent")
    @ApiModelProperty(value = "检测内容", name = "checkContent")
    @TableField(value = "CHECK_CONTENT")
    private String checkContent;


}