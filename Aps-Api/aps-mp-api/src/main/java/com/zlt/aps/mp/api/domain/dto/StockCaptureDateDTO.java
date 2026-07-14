package com.zlt.aps.mp.api.domain.dto;

import lombok.Data;
import java.util.Date;

/**
 * 库存抓取日映射 DTO
 * 用于 Java 层计算库存抓取日后传入 SQL，替代 SQL 中直接关联 T_MDM_MONTH_SURPLUS
 * 维度：(分厂+物料+产品状态)
 */
@Data
public class StockCaptureDateDTO {
    /** 分厂编码 */
    private String factoryCode;
    /** 物料编码 */
    private String materialCode;
    /** 产品状态 */
    private String productStatus;
    /** 库存抓取日（Java 层根据 LAST_MONTH_PLAN_VERSION 解析或回退到余量表） */
    private Date stockCaptureDate;
}
