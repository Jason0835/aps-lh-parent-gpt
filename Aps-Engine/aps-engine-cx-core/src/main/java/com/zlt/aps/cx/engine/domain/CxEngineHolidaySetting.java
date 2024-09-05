package com.zlt.aps.cx.engine.domain;

import com.zlt.aps.common.core.domain.ApsBaseEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.Date;

/**
 * 成型节假日设定对象 t_cx_holiday_setting
 * 
 * @author Joran.zhang
 * @date 2021-06-29
 */
@Data
@ApiModel(value = "成型节假日设定对象", description = "成型节假日设定对象 ")
public class CxEngineHolidaySetting extends ApsBaseEntity{

    private static final long serialVersionUID = 1L;

    /** 主键ID，对应自增序列为：SEQ_HOLIDAY_SETTING */
    @ApiModelProperty(value = "主键")
    private Long id;



    /** 日期 */
    @ApiModelProperty(value = "日期")
    private Date holidayDay;

    /**
     * 节假日开始时间
     */
    private Date holidayStartDate;

    /**
     * 节假日结束日期
     */
    private Date holidayEndDate;


}
