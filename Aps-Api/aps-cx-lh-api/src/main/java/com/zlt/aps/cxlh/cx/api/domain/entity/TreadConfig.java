package com.zlt.aps.cxlh.cx.api.domain.entity;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 胎面配置实体
 */
@Data
public class TreadConfig {

    /** ID */
    private Long id;

    /** 物料编码 */
    private String materialCode;

    /** 胎面编码 */
    private String treadCode;

    /** 胎面名称 */
    private String treadName;

    /** 整车数量 */
    private BigDecimal vehicleQuantity;

    /** 创建时间 */
    private java.util.Date createTime;

    /** 更新时间 */
    private java.util.Date updateTime;
}
