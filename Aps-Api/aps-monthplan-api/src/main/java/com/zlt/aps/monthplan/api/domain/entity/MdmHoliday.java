package com.zlt.aps.monthplan.api.domain.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.common.core.annotation.Excel;
import com.ruoyi.common.core.web.domain.BaseEntity;
import com.zlt.common.annotation.ImportExcelValidated;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.Date;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：MdmHoliday.java
 * 描    述：0150基础数据_节假日配置对象 t_mdm_holiday
 *
 * @author zlt
 * @version 1.0
 * <p>
 * 修改记录：
 * 修改时间：...
 * 修 改 人：zlt
 * 修改内容：...
 * @date 2026-01-06
 */
@ApiModel(value = "0150基础数据_节假日配置对象", description = "0150基础数据_节假日配置对象")
@Data
@TableName(value = "T_MDM_HOLIDAY")
public class MdmHoliday extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 日期
     */
    @ImportExcelValidated(required = true, date = true)
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "ui.data.column.mdmHoliday.holidayDate", width = 30, dateFormat = "yyyy-MM-dd")
    @ApiModelProperty(value = "日期", name = "holidayDate")
    @TableField(value = "HOLIDAY_DATE")
    private Date holidayDate;

    /**
     * 节假日名称
     */
    @ImportExcelValidated(required = true, maxLength = 50)
    @Excel(name = "ui.data.column.mdmHoliday.holidayNames")
    @ApiModelProperty(value = "节假日名称", name = "holidayNames")
    @TableField(value = "HOLIDAY_NAMES")
    private String holidayNames;

    /**
     * 节假日-开始时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @ApiModelProperty(value = "节假日-开始时间", name = "holidayDateStartTime")
    @TableField(exist = false)
    private Date holidayDateStartTime;

    /**
     * 节假日-结束时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @ApiModelProperty(value = "节假日-结束时间", name = "holidayDateEndTime")
    @TableField(exist = false)
    private Date holidayDateEndTime;

}
