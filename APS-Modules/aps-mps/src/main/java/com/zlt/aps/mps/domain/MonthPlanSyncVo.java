package com.zlt.aps.mps.domain;

import lombok.Data;

/**
 * @author Gim
 */
@Data
public class MonthPlanSyncVo {


    /**
     * 年
     */
    private Integer year;

    /**
     * 月
     */
    private Integer month;

    /**
     * 是否定稿 0是1否
     */
    private Integer isFinal;

    /**
     * 分厂版本
     */
    private String version;
}
