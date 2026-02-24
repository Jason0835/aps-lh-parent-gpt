package com.zlt.aps.monthplan.api.domain.itf;

import lombok.Data;

import java.io.Serializable;

/**
 * 内销历史销售计划结果Vo
 *
 * @author Chen
 * @date 2025/4/8
 */
@Data
public class InHisSaleOrderDataListVo implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 市场类别
     */
    private String clientExtendName;

    /**
     * 订单日期
     */
    private String date;

    /**
     * 商品编号
     */
    private String goodsNum;

    /**
     * 销售数量
     */
    private Integer sellNum;

    /**
     * 订单数量
     */
    private Integer orderNum;

    /**
     * 备注
     */
    private String remark;
}
