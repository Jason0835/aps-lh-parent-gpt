package com.zlt.aps.monthplan.api.domain.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.common.core.annotation.Excel;
import com.ruoyi.common.core.web.domain.BaseEntity;
import com.zlt.common.annotation.ImportExcelValidated;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.Date;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：MpMouldDeliveryPlan.java
 * 描    述：模具到货计划对象 t_mp_mould_delivery_plan
 *
 * @author zlt
 * @version 1.0
 * <p>
 * 修改记录：
 * 修改时间：...
 * 修 改 人：zlt
 * 修改内容：...
 * @date 2025-12-05
 */
@ApiModel(value = "模具到货计划对象", description = "模具到货计划对象 ")
@Data
@TableName(value = "T_MP_MOULD_DELIVERY_PLAN")
public class MpMouldDeliveryPlan extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 工厂编号
     */
    @ImportExcelValidated(required = true)
    @Excel(name = "ui.data.column.mpMouldDeliveryPlan.factoryCode", dictType = "biz_factory_name")
    @ApiModelProperty(value = "工厂编号", name = "factoryCode")
    @TableField(value = "FACTORY_CODE")
    private String factoryCode;

    /**
     * 型腔模号
     */
    @ImportExcelValidated(required = true, maxLength = 32)
    @Excel(name = "ui.data.column.mpMouldDeliveryPlan.mouldCode")
    @ApiModelProperty(value = "型腔模号", name = "mouldCode")
    @TableField(value = "MOULD_CODE")
    private String mouldCode;

    /**
     * 物料编码
     */
    @ImportExcelValidated(required = true, maxLength = 32)
    @Excel(name = "ui.data.column.mpMouldDeliveryPlan.materialCode")
    @ApiModelProperty(value = "物料编码", name = "materialCode")
    @TableField(value = "MATERIAL_CODE")
    private String materialCode;

    /**
     * MES物料编码
     */
//    @ImportExcelValidated(required = true, maxLength = 32)
//    @Excel(name = "ui.data.column.mpMouldDeliveryPlan.mesMaterialCode")
    @ApiModelProperty(value = "MES物料编码", name = "mesMaterialCode")
    @TableField(value = "MES_MATERIAL_CODE")
    private String mesMaterialCode;

    /**
     * 物料描述
     */
    @ImportExcelValidated(required = true, maxLength = 64)
    @Excel(name = "ui.data.column.mpMouldDeliveryPlan.materialDesc")
    @ApiModelProperty(value = "物料描述", name = "materialDesc")
    @TableField(value = "MATERIAL_DESC")
    private String materialDesc;

    /**
     * 计划发货日期
     */
    @ImportExcelValidated(required = true, date = true)
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "ui.data.column.mpMouldDeliveryPlan.shipmentDate", width = 30, dateFormat = "yyyy-MM-dd")
    @ApiModelProperty(value = "计划发货日期", name = "shipmentDate")
    @TableField(value = "SHIPMENT_DATE")
    private Date shipmentDate;

    /**
     * 计划上机日期
     */
//    @ImportExcelValidated(required = true, date = true)
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "ui.data.column.mpMouldDeliveryPlan.boardingDate", width = 30, dateFormat = "yyyy-MM-dd", type = Excel.Type.EXPORT)
    @ApiModelProperty(value = "计划上机日期", name = "boardingDate")
    @TableField(value = "BOARDING_DATE")
    private Date boardingDate;

    /**
     * 计划发货日期-开始时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @ApiModelProperty(value = "计划发货日期-开始时间", name = "shipmentDateStartTime")
    @TableField(exist = false)
    private Date shipmentDateStartTime;

    /**
     * 计划发货日期-结束时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @ApiModelProperty(value = "计划发货日期-结束时间", name = "shipmentDateEndTime")
    @TableField(exist = false)
    private Date shipmentDateEndTime;

    /**
     * 计划上机日期-开始时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @ApiModelProperty(value = "计划上机日期-开始时间", name = "boardingDateStartTime")
    @TableField(exist = false)
    private Date boardingDateStartTime;

    /**
     * 计划上机日期-结束时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @ApiModelProperty(value = "计划上机日期-结束时间", name = "boardingDateEndTime")
    @TableField(exist = false)
    private Date boardingDateEndTime;


}
