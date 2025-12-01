package com.zlt.aps.monthplan.api.domain.vo;

import lombok.Data;

import java.io.Serializable;


@Data
public class PeriodInfo implements Serializable {

    /**
     * 分公司
     */
    private String companyCode;
    /**
     * 分厂
     */
    private String factoryCode;
    /**
     * from年份
     */
    private Integer fromyear;

    /**
     * from月份
     */
    private Integer frommonth;
    /**
     * to年份
     */
    private Integer toyear;

    /**
     * to月份
     */
    private Integer tomonth;

    /**
     * to月份天数
     */
    private Integer monthdays;
}