package com.zlt.aps.mp.api.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.common.core.annotation.Excel;
import com.ruoyi.common.core.web.domain.BaseEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;


/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：MdmProductionCalendar.java
 * 描    述：生产日历对象 t_mdm_production_calendar
 *
 * @author zlt
 * @version 1.0
 * <p>
 * 修改记录：
 * 修改时间：...
 * 修 改 人：zlt
 * 修改内容：...
 * @date 2025-02-17
 */

@ApiModel(value = "生产日历对象", description = "生产日历对象 ")
@Data
@TableName(value = "T_MDM_PRODUCTION_CALENDAR")
public class MdmProductionCalendar extends BaseEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "ID", type = IdType.AUTO)
    private Long id;

    /**
     * 分厂编号
     */
    @Excel(name = "ui.data.column.productionCalendar.factoryCode", dictType = "biz_factory_name")
    @ApiModelProperty(value = "分厂编号", name = "factoryCode")
    @TableField(value = "FACTORY_CODE")
    private String factoryCode;

    /**
     * 年份
     */
    @Excel(name = "ui.data.column.productionCalendar.year")
    @ApiModelProperty(value = "年份", name = "year")
    @TableField(value = "YEAR")
    private Integer year;

    /**
     * 月份
     */
    @Excel(name = "ui.data.column.productionCalendar.month")
    @ApiModelProperty(value = "月份", name = "month")
    @TableField(value = "MONTH")
    private Integer month;

    /**
     * 停车开始日期
     */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "ui.data.column.productionCalendar.beginDate", width = 30, dateFormat = "yyyy-MM-dd")
    @ApiModelProperty(value = "停车开始日期", name = "beginDate")
    @TableField(value = "BEGIN_DATE")
    private Date beginDate;

    /**
     * 停车结束日期
     */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "ui.data.column.productionCalendar.endDate", width = 30, dateFormat = "yyyy-MM-dd")
    @ApiModelProperty(value = "停车结束日期", name = "endDate")
    @TableField(value = "END_DATE")
    private Date endDate;

    @TableField(exist = false)
    private String remark;

    @TableField(exist = false)
    private Integer isDelete;
}