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
    /** 需求版本号（来自余量表 REQUIRE_VERSION，用于 Java 层按版本精确匹配库存抓取日） */
    private String requireVersion;
    /** 库存抓取日（Java 层根据 LAST_MONTH_PLAN_VERSION 解析或回退到余量表） */
    private Date stockCaptureDate;
    /** 是否强制置零（当月定稿表的 LAST_MONTH_PLAN_VERSION 为当月ADJ版本时，超欠产直接为0，跳过计划量/完成量累加） */
    private boolean forceZero;
    /**
     * 库存抓取日是否缺失（版本非ADJ【含空值】且余量表也匹配不到时为true，此时超欠产值/有效标识置空）
     * 触发条件：LAST_MONTH_PLAN_VERSION 不以 ADJ 开头（含 null/空），且余量表按 (分厂+物料+REQUIRE_VERSION) 匹配不到
     * 注意：ADJ 前缀但日期超出数据来源月范围的不触发本标记，仍走原有回退到月初逻辑
     */
    private boolean stockCaptureDateMissing;
}
