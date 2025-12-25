package com.zlt.aps.monthplan.api.domain.deduct;

import lombok.Data;

import java.time.LocalDate;

@Data
public class DailyScheduleVo {

    /**
     * SKU编码
     */
    private String materialCode;

    /**
     * 排产日期
     */
    private LocalDate scheduleDate;

    /**
     * 当日产量
     */
    private Integer skuQuantity;

    /**
     * 当日使用机台数
     */
    private Integer skuMachines;

    /**
     * 前日产量
     */
    private Integer skuPreQty;

}
