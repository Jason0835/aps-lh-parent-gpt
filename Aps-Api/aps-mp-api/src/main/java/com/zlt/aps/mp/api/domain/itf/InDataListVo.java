package com.zlt.aps.mp.api.domain.itf;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 内销销售计划结果Vo
 *
 * @author Chen
 * @date 2025/4/8
 */
@Data
public class InDataListVo implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 市场类别
     */
    private String clientExtendName;

    /**
     * 客户编码
     */
    private String clientnum;

    /**
     * 商品编号
     */
    private String goodsNum;

    /**
     * 创建时间
     */
    private Date innerDate;

    /**
     * 报价数量
     */
    private Long num;

    /**
     * 单号
     */
    private String numbers;


    /**
     * 订单日期
     */
    private String date;

    /**
     * 销售数量
     */
    private Long sellNum;

    /**
     * 订单数量
     */
    private Long orderNum;

    /**
     * 备注
     */
    private String remark;
}
