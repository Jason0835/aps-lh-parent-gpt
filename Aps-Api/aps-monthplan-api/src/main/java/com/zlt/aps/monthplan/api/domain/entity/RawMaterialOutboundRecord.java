package com.zlt.aps.monthplan.api.domain.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.common.core.annotation.Excel;
import com.ruoyi.common.core.web.domain.BaseEntity;
import com.zlt.common.annotation.ImportExcelValidated;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.apache.ibatis.type.JdbcType;

import java.math.BigDecimal;
import java.util.Date;

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
@ApiModel(value = "原材料出库量对象", description = "原材料出库量对象")
@Data
@TableName(value = "T_RAW_MATERIAL_OUTBOUND_RECORD")
public class RawMaterialOutboundRecord extends BaseEntity {

    private static final long serialVersionUID = 1L;

     /** 工厂 */
    @Excel(name = "ui.data.column.rawMaterialOutboundRecord.factoryCode", dictType = "biz_factory_name")
    @ImportExcelValidated(required = true, maxLength = 10, dictType = "biz_factory_name")
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
    @ApiModelProperty(value = "MES物料编码", name = "mesMaterialCode")
    @TableField(value = "MES_MATERIAL_CODE")
    private String mesMaterialCode;

    /**
     * 物料类型，数据字典 biz_rawMaterial_type 01 常规产品 02 特殊材料，匹配特殊原材料，则 类型 = 02
     */
    @Excel(name = "ui.data.column.rawMaterialOutboundRecord.materialType", dictType = "biz_rawMaterial_type")
    @ImportExcelValidated(required = true, dictType = "biz_rawMaterial_type")
    @ApiModelProperty(value = "物料类型  biz_rawMaterial_type")
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
    @ApiModelProperty(value = "出库日期", name = "outboundDate")
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

    /**
     * 数据版本
     */
    @ApiModelProperty(value = "数据版本", name = "dataVersion")
    @TableField(exist = false)
    private String dataVersion;


    @Excel(name = "ui.data.column.rawSpecialMaterialRecord.remark")
    @ImportExcelValidated(maxLength = 300)
    @ApiModelProperty("备注")
    @TableField("REMARK")
    private String remark;

    @Excel(name = "ui.data.column.mdmMonCycleSchStruConf.updateDate")
    @ApiModelProperty("更新时间")
    @JsonFormat(
            pattern = "yyyy-MM-dd HH:mm:ss"
    )
    @TableField(
            value = "UPDATE_TIME",
            fill = FieldFill.INSERT_UPDATE,
            jdbcType = JdbcType.TIMESTAMP
    )
    private Date updateTime;

}
