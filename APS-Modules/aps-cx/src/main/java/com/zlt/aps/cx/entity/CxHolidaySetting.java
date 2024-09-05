package com.zlt.aps.cx.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.zlt.aps.common.core.domain.ApsBaseEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;

/**
 * <p>
 * 假日设定表
 * </p>
 *
 * @author chen
 * @since 2021-06-30
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("T_CX_HOLIDAY_SETTING")
@ApiModel(value = "CxHolidaySetting对象", description = "假日设定表")
@KeySequence(value = "SEQ_PUBLIC", clazz = Long.class)
public class CxHolidaySetting extends ApsBaseEntity {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "主键ID，对应自增序列为：SEQ_PUBLIC")
    @TableId(value = "ID", type = IdType.INPUT)
    private Long id;

    @ApiModelProperty(value = "假日名称")
    @TableField("HOLIDAY_NAME")
    private String holidayName;

    @ApiModelProperty(value = "假日日期")
    @TableField("HOLIDAY_DAY")
    private Date holidayDay;

    //接收前端查询条件开始日期
    @ApiModelProperty(value = "查询条件开始日期", position = 40)
    private String startTime;

    //接收前端查询条件结束日期
    @ApiModelProperty(value = "查询条件结束日期", position = 50)
    private String endTime;
}
