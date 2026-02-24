package com.zlt.aps.dp.api.domain.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.annotations.ApiModel;
import lombok.Data;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：SalesOrderPoolRecord.java
 * 描    述：销售订单池对象 t_mp_sales_order_pool
 *@author zlt
 *@date 2025-12-24
 *@version 1.0
 *
 *  修改记录：
 *     修改时间：...
 *     修 改 人：zlt
 *     修改内容：...
 */

@Data
@TableName(value = "T_DP_SALES_ORDER_POOL_RECORD")
@ApiModel(value = "销售订单池对象", description = "销售订单池同步记录对象 ")
public class SalesOrderPoolRecord extends SalesOrderPool{

    private static final long serialVersionUID = 1L;

    /** 同步年份 */
    @TableField(value = "YEAR")
    private Integer year;

    /** 同步月份 */
    @TableField(value = "MONTH")
    private Integer month;
}
