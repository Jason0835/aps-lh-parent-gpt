package com.zlt.aps.factory.domain.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 工作日历信息
 *
 * @author ZLT
 * 20251212
 */
@Data
public class ProductionDayInfoVo implements Serializable {

    /**
     * 排产日期
     */
    private Date productionDate;

    /**
     * 停产标识 0 停 1 开
     */
    private String dayFlag;
    /**
     * 产能比例 1~100
     */
    private Integer rate;
}
