package com.zlt.aps.gsq.api.domain.entity;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * MES钢丝圈库存中间表VO
 * 对应MES中间表 MES_GSQ_STOCK
 *
 * <p>注意：stockDate 定义为 String（格式 yyyy-MM-dd），而非 Date，
 * 以彻底规避 SQL Server JDBC 驱动在跨时区（JVM GMT+7 / DB GMT+8）环境下
 * 读取 DATE/DATETIME 字段时的时区偏移问题。读取侧 SQL 需用
 * CONVERT(VARCHAR(10), STOCK_DATE, 23) 输出纯日期字符串。</p>
 *
 * @author APS Team
 */
@Data
@ApiModel(value = "MES钢丝圈库存", description = "MES钢丝圈库存中间表数据")
public class GsqMesStock implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 库存日期，格式：yyyy-MM-dd（字符串类型，避免JDBC跨时区偏移） */
    @ApiModelProperty(value = "库存日期(yyyy-MM-dd)")
    private String stockDate;

    /** 物料编码（MES字段名MATERIAL_CODE，对应APS钢丝圈代码STEEL_RING_CODE） */
    @ApiModelProperty(value = "物料编码")
    private String materialCode;

    /** 可用库存（MES字段名AVAILABLE_STOCK，对应APS库存量STOCK_NUM） */
    @ApiModelProperty(value = "可用库存")
    private BigDecimal availableStock;
}
