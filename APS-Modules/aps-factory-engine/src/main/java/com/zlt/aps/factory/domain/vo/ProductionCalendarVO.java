package com.zlt.aps.factory.domain.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 生产日历信息对象
 *
 * @author ZLT
 * 20250220
 */
@Data
public class ProductionCalendarVO implements Serializable {

    /**
     * 年份
     */
    private Integer year;

    /**
     * 月份
     */
    private Integer month;

    /**
     * 开始日期
     */
    private Date beginDate;

    /**
     * 结束日期
     */
    private Date endDate;
}
