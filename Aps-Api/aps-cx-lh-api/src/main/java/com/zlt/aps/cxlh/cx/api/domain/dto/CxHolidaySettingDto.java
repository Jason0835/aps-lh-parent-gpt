package com.zlt.aps.cxlh.cx.api.domain.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.common.core.annotation.Excel;
import com.zlt.aps.common.core.annotation.ImportValidated;
import com.zlt.aps.common.core.domain.ApsBaseDto;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.Date;

/**
 * 假日设定对象 t_cx_holiday_setting
 *
 * @author chen
 * @date 2021-06-30
 */
@Data
@ApiModel(value = "假日设定对象", description = "假日设定对象 ")
public class CxHolidaySettingDto extends ApsBaseDto {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID，对应自增序列为：SEQ_PUBLIC
     */
    @ApiModelProperty(value = "id", position = 10)
    private Long id;

    /**
     * 假日名称
     */
    @ApiModelProperty(value = "假日名称", position = 20)
    private String holidayName;

    /**
     * 假日日期
     */
    @JsonFormat(pattern = "yyyy-MM-dd",timezone = "GMT+8")
    @ImportValidated(required = true, date = true)
    @Excel(name = "ui.data.column.holiday.holidayDay", width = 30, dateFormat = "yyyy-MM-dd")
    @ApiModelProperty(value = "假日日期", position = 30)
    private Date holidayDay;

    @Excel(name = "ui.common.column.remark")
    @ImportValidated(maxLength = 300)
    @ApiModelProperty(value = "备注", position = 500)
    private String remark;

    //接收前端查询条件开始日期
    @ApiModelProperty(value = "查询条件开始日期", position = 40)
    private String startTime;

    //接收前端查询条件结束日期
    @ApiModelProperty(value = "查询条件结束日期", position = 50)
    private String endTime;
}
