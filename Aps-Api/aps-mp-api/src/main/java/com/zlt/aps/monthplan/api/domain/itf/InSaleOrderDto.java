package com.zlt.aps.monthplan.api.domain.itf;

import lombok.Data;

import java.io.Serializable;

/**
 * @author Chen
 * @date 2025/4/8
 */
@Data
public class InSaleOrderDto implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 年份
     */
    private Integer years;

    /**
     * 月份
     */
    private Integer months;


    private String dates1;

    private String dates2;
}
