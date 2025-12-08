package com.zlt.aps.monthplan.api.domain.entity;

import java.math.BigDecimal;
import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.zlt.common.annotation.ImportExcelValidated;
import lombok.Data;
import com.ruoyi.common.core.annotation.Excel;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.zlt.common.domain.CommonBusiEntity;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：RawMaterialOutboundRecord.java
 * 描    述：原材料出库量对象 t_raw_material_outbound_record
 *@author zlt
 *@date 2025-12-08
 *@version 1.0
 *
 *  修改记录：
 *     修改时间：...
 *     修 改 人：zlt
 *     修改内容：...
 */

@ApiModel(value = "原材料出库量对象", description = "原材料出库量对象 ")
@Data
@TableName(value = "T_RAW_MATERIAL_OUTBOUND_RECORD")
@KeySequence(value = "SEQ_MATERIAL_OUTBOUND_RECORD")
public class RawMaterialOutboundRecord extends CommonBusiEntity{

    private static final long serialVersionUID = 1L;

     /** 工厂 */
    @Excel(name = "ui.data.column.rawMaterialOutboundRecord.factoryCode")
    @ImportExcelValidated(required = true, maxLength = 10)
    @ApiModelProperty(value = "工厂", name = "factoryCode")
    @TableField(value = "FACTORY_CODE")
    private String factoryCode;

    /** 物料编码 */
    @Excel(name = "ui.data.column.rawMaterialOutboundRecord.materialCode")
    @ImportExcelValidated(required = true, maxLength = 10)
    @ApiModelProperty(value = "物料编码", name = "materialCode")
    @TableField(value = "MATERIAL_CODE")
    private String materialCode;

    /** MES物料编码 */
    @Excel(name = "ui.data.column.rawMaterialOutboundRecord.mesMaterialCode")
    @ImportExcelValidated(required = true, maxLength = 10)
    @ApiModelProperty(value = "MES物料编码", name = "mesMaterialCode")
    @TableField(value = "MES_MATERIAL_CODE")
    private String mesMaterialCode;

    /** 物料类型            数据字典 biz_rawMaterial_type 01 常规产品 02 特殊材料            匹配特殊原材料，则 类型 = 02 */
    @Excel(name = "ui.data.column.rawMaterialOutboundRecord.materialType")
    @ImportExcelValidated(required = true, dictType = "biz_rawMaterial_type")
    @ApiModelProperty(value = "物料类型")
    @TableField(value = "MATERIAL_TYPE")
    private String materialType;

    /** 物料描述 */
    @Excel(name = "ui.data.column.rawMaterialOutboundRecord.materialDesc")
    @ImportExcelValidated(required = true, maxLength = 100)
    @ApiModelProperty(value = "物料描述", name = "materialDesc")
    @TableField(value = "MATERIAL_DESC")
    private String materialDesc;

    /** 日期格式：YYYY-MM-DD hh:mm:ss */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "ui.data.column.rawMaterialOutboundRecord.outboundDate", width = 30, dateFormat = "yyyy-MM-dd")
    @ImportExcelValidated(required = true, date = true)
    @ApiModelProperty(value = "日期格式：YYYY-MM-DD hh:mm:ss", name = "outboundDate")
    @TableField(value = "OUTBOUND_DATE")
    private Date outboundDate;

    /** 单位 */
    @Excel(name = "ui.data.column.rawMaterialOutboundRecord.unit")
    @ImportExcelValidated(required = true, maxLength = 10)
    @ApiModelProperty(value = "单位", name = "unit")
    @TableField(value = "UNIT")
    private String unit;

    /** 出库数量 */
    @Excel(name = "ui.data.column.rawMaterialOutboundRecord.outboundQty")
    @ImportExcelValidated(required = true,  number = true, min = 0, max = 999999)
    @ApiModelProperty(value = "出库数量", name = "outboundQty")
    @TableField(value = "OUTBOUND_QTY")
    private BigDecimal outboundQty;


}