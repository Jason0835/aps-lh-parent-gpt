package com.zlt.aps.factory.deduct;

import lombok.Data;

/**
 * 降模排产结果对象
 * 到日
 *
 * @author Sandy
 * @date 2025/12/24
 */
@Data
public class DailyScheduleVo {

    /**
     * SKU编码
     */
    private String materialCode;

    /**
     * 排产日期
     */
    private Integer scheduleDate;

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
