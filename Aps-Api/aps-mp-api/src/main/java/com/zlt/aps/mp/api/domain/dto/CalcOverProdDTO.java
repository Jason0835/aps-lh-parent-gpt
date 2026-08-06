package com.zlt.aps.mp.api.domain.dto;

import lombok.Data;

/**
 * 上月超欠产计算结果 DTO
 * 用于把 calc 子查询从 UPDATE 语句中拆出来独立 SELECT，避免 MySQL 在 UPDATE 语句中
 * 对同一张表（T_MP_MONTH_PLAN_PROD_FINAL）做子查询时读取到 UPDATE 驱动表当前行的行为异常问题。
 * 维度：(分厂+物料+产品状态)
 */
@Data
public class CalcOverProdDTO {
    /** 分厂编码 */
    private String factoryCode;
    /** 物料编码 */
    private String materialCode;
    /** 产品状态 */
    private String productStatus;
    /** 计划量（从库存抓取日日号到月底累加 DAY_x） */
    private Long planQty;
    /** 完成量（库存抓取日~月底的硫化日完成量累加） */
    private Long finishQty;
    /** 是否强制置零（当月定稿表的 LAST_MONTH_PLAN_VERSION 为当月ADJ版本时，超欠产直接为0） */
    private Integer forceZero;
    /** 库存抓取日是否缺失（1=缺失，超欠产值/有效标识置空；0=正常）。版本非ADJ【含空值】且余量表匹配不到时为1 */
    private Integer stockCaptureDateMissing;
}
