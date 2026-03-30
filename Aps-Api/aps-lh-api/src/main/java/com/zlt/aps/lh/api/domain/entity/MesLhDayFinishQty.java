package com.zlt.aps.lh.api.domain.entity;

import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import com.ruoyi.common.core.annotation.Excel;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import com.zlt.common.annotation.EntityMapping;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.zlt.common.domain.CommonBusiEntity;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：MesLhDayFinishQty.java
 * 描    述：硫化排程日完成量回报接口对象 t_mes_lh_day_finish_qty
 *@author zlt
 *@date 2025-02-21
 *@version 1.0
 *
 *  修改记录：
 *     修改时间：...
 *     修 改 人：zlt
 *     修改内容：...
 */

@ApiModel(value = "硫化排程日完成量回报接口对象", description = "硫化排程日完成量回报接口对象 ")
@Data
@TableName(value = "T_MES_LH_DAY_FINISH_QTY")
public class MesLhDayFinishQty extends CommonBusiEntity{

    private static final long serialVersionUID = 1L;

     /** 完成日期 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "ui.data.column.mesLhDayFinishQty.finishDate", width = 30, dateFormat = "yyyy-MM-dd")
    @ApiModelProperty(value = "完成日期", name = "finishDate")
    @TableField(value = "FINISH_DATE")
    private Date finishDate;

    /** 物料编号 */
    @ApiModelProperty(value = "物料编号", name = "productCode")
    @TableField(value = "PRODUCT_CODE")
    private String productCode;

    /** 规格代码 */
    @Excel(name = "ui.data.column.mesLhDayFinishQty.specCode")
    @ApiModelProperty(value = "规格代码", name = "specCode")
    @TableField(value = "SPEC_CODE")
    private String specCode;


    /** 胎胚日完成量 */
    @Excel(name = "ui.data.column.mesLhDayFinishQty.dayFinishQty")
    @ApiModelProperty(value = "胎胚日完成量", name = "dayFinishQty")
    @TableField(value = "DAY_FINISH_QTY")
    private Integer dayFinishQty;

    /** 分厂编号 */
    @Excel(name = "ui.data.column.mesLhDayFinishQty.factoryCode")
    @ApiModelProperty(value = "分厂编号", name = "factoryCode")
    @TableField(value = "FACTORY_CODE")
    private String factoryCode;

    /** 版本号 */
    @ApiModelProperty(value = "版本号", name = "dataVersion")
    @TableField(value = "DATA_VERSION")
    private String dataVersion;


}