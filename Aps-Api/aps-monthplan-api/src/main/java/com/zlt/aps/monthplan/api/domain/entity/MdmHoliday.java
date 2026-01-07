package com.zlt.aps.monthplan.api.domain.entity;

import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.common.core.web.domain.BaseEntity;
import lombok.Data;
import com.ruoyi.common.core.annotation.Excel;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.zlt.common.domain.CommonBusiEntity;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：MdmHoliday.java
 * 描    述：0150基础数据_节假日配置对象 t_mdm_holiday
 *@author zlt
 *@date 2026-01-06
 *@version 1.0
 *
 *  修改记录：
 *     修改时间：...
 *     修 改 人：zlt
 *     修改内容：...
 */

@ApiModel(value = "0150基础数据_节假日配置对象", description = "0150基础数据_节假日配置对象 ")
@Data
@TableName(value = "T_MDM_HOLIDAY")
@KeySequence(value = "SEQ_HOLIDAY")
public class MdmHoliday extends BaseEntity {

    private static final long serialVersionUID = 1L;

     /** 日期 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "ui.data.column.mdmHoliday.holidayDate", width = 30, dateFormat = "yyyy-MM-dd")
    @ApiModelProperty(value = "日期", name = "holidayDate")
    @TableField(value = "HOLIDAY_DATE")
    private Date holidayDate;

    /** 节假日名称 */
    @Excel(name = "ui.data.column.mdmHoliday.holidayNames")
    @ApiModelProperty(value = "节假日名称", name = "holidayNames")
    @TableField(value = "HOLIDAY_NAMES")
    private String holidayNames;


}