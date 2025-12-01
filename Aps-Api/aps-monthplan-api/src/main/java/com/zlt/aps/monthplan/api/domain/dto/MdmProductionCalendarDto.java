package com.zlt.aps.monthplan.api.domain.dto;

import lombok.Data;

/**
 * 分厂可生产日期的最后一天
 */
@Data
public class MdmProductionCalendarDto {

    /**
     * 分厂编号
     */
    private String factoryCode;

    /**
     * 可生产的字段
     */
    private String productionDayField;

}
