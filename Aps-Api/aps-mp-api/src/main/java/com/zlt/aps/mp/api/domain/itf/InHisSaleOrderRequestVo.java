package com.zlt.aps.mp.api.domain.itf;

import lombok.Data;

import java.io.Serializable;

/**
 * 内销历史销售订单请求参数对象
 *
 * @author Chen
 * @date 2025/4/10
 */
@Data
public class InHisSaleOrderRequestVo implements Serializable {

    private static final long serialVersionUID = 1L;

    private String dates1;

    private String dates2;

    private Integer pageNo;
}
