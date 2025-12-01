package com.zlt.aps.monthplan.api.domain.itf;

import lombok.Data;

import java.io.Serializable;

/**
 * 内销销售订单请求参数对象
 *
 * @author Chen
 * @date 2025/4/10
 */
@Data
public class InSaleOrderRequestVo implements Serializable {

    private static final long serialVersionUID = 1L;

    private Integer years;

    private Integer months;

    private Integer pageNo;
}
