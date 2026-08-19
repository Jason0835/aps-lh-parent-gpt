package com.zlt.aps.tq.api.domain.entity;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * MES胎圈库存中间表VO
 * 对应MES中间表 MES_TQ_STOCK
 *
 * <p>注意：stockDate 定义为 String（格式 yyyy-MM-dd），而非 Date，
 * 以彻底规避 SQL Server JDBC 驱动在跨时区（JVM GMT+7 / DB GMT+8）环境下
 * 读取 DATE/DATETIME 字段时的时区偏移问题。读取侧 SQL 需用
 * CONVERT(VARCHAR(10), STOCK_DATE, 23) 输出纯日期字符串。</p>
 *
 * @author APS Team
 */
@Data
@ApiModel(value = "MES胎圈库存", description = "MES胎圈库存中间表数据")
public class TqMesStock implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 库存日期，格式：yyyy-MM-dd（字符串类型，避免JDBC跨时区偏移） */
    @ApiModelProperty(value = "库存日期(yyyy-MM-dd)")
    private String stockDate;

    /** 物料编码 */
    @ApiModelProperty(value = "物料编码")
    private String materialCode;

    /** 可用库存 */
    @ApiModelProperty(value = "可用库存")
    private BigDecimal availableStock;
}
