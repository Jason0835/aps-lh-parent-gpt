package com.zlt.aps.mp.api.domain.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * 日排产统计信息对象
 *
 * @author ZLT
 * @date 20250509
 */
@Data
public class DayProductionTotalVo implements Serializable {
    /**
     * 日期 1-31
     */
    private Integer days;
    /**
     * SAP代码个数
     */
    private Integer dayCount;
    /**
     * 排产总量
     */
    private Integer qty;

}
