package com.zlt.aps.monthplan.api.domain.dto;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.common.core.annotation.Excel;
import com.ruoyi.common.core.web.domain.BaseEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：SalesOrderPool.java
 * 描    述：销售订单池值对象 t_mp_sales_order_pool
 *@author zlt
 *@date 2025-12-04
 *@version 1.0
 *
 *  修改记录：
 *     修改时间：...
 *     修 改 人：zlt
 *     修改内容：...
 */

@Data
@ApiModel(value = "销售订单池对象", description = "销售订单池对象 ")
public class SalesOrderPoolDto{

	private Long id;
	
    private String factoryCode;

    /** PO号 */
    @ApiModelProperty(value = "PO号", name = "salCodePo")
    private String salCodePo;

    /** 供应链优先级 */
    @ApiModelProperty(value = "供应链优先级", name = "scmPriority")
    private String scmPriority;
}
