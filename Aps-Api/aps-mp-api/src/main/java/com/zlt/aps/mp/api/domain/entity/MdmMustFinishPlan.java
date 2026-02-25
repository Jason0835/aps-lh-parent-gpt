package com.zlt.aps.mp.api.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
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
 * 文件名称：MdmMustFinishPlan.java
 * 描    述：必须保证的客户月计划对象 t_mdm_must_finish_plan
 *
 * @author zlt
 * @version 1.0
 * <p>
 * 修改记录：
 * 修改时间：...
 * 修 改 人：zlt
 * 修改内容：...
 * @date 2025-02-25
 */

@ApiModel(value = "必须保证的客户月计划对象", description = "必须保证的客户月计划对象 ")
@Data
@TableName(value = "T_MDM_MUST_FINISH_PLAN")
public class MdmMustFinishPlan extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @TableId(value = "ID", type = IdType.AUTO)
    private Long id;

    /**
     * 分厂编号
     */
    @Excel(name = "ui.data.column.mustFinishPlan.factoryCode", dictType = "biz_factory_name", sort = 10)
    @ImportExcelValidated(required = true, isCode = true, maxLength = 20)
    @ApiModelProperty(value = "分厂编号", name = "factoryCode")
    @TableField(value = "FACTORY_CODE")
    private String factoryCode;

    /**
     * 年份
     */
    @Excel(name = "ui.data.column.mustFinishPlan.year", sort = 50)
    @ImportExcelValidated(required = true, digits = true, min = 1000, max = 9999)
    @ApiModelProperty(value = "年份", name = "year")
    @TableField(value = "YEAR")
    private Integer year;

    /**
     * 月份
     */
    @Excel(name = "ui.data.column.mustFinishPlan.month", sort = 60)
    @ImportExcelValidated(required = true, digits = true, min = 1, max = 12)
    @ApiModelProperty(value = "月份", name = "month")
    @TableField(value = "MONTH")
    private Integer month;

    /**
     * 客户编号
     */
    @Excel(name = "ui.data.column.mustFinishPlan.customCode", sort = 20)
    @ImportExcelValidated(required = true, isCode = true, maxLength = 20)
    @ApiModelProperty(value = "客户编号", name = "customCode")
    @TableField(value = "CUSTOM_CODE")
    private String customCode;

    /**
     * 客户名称
     */
    @Excel(name = "ui.data.column.mustFinishPlan.customName", sort = 30)
    @ApiModelProperty(value = "客户名称", name = "customName")
    @TableField(exist = false)
    private String customName;

    /**
     * 物料编号
     */
    @Excel(name = "ui.data.column.mustFinishPlan.productCode", sort = 70)
    @ImportExcelValidated(required = true, isCode = true, maxLength = 20)
    @ApiModelProperty(value = "物料编号", name = "productCode")
    @TableField(value = "PRODUCT_CODE")
    private String productCode;

    /**
     * 库位类别
     */
    @Excel(name = "ui.data.column.mustFinishPlan.locationType", dictType = "biz_stor_type", sort = 90)
    @ImportExcelValidated(required = true, isCode = true, maxLength = 10)
    @ApiModelProperty(value = "库位类别", name = "locationType")
    @TableField(value = "LOCATION_TYPE")
    private String locationType;

    /**
     * 产品描述
     */
    @Excel(name = "ui.data.column.mustFinishPlan.productDesc", sort = 80)
    @ApiModelProperty(value = "产品描述", name = "productDesc")
    @TableField(value = "PRODUCT_DESC")
    private String productDesc;

    /**
     * 序号
     */
    @ApiModelProperty(value = "序号", name = "serialno")
    @TableField(value = "SERIALNO")
    private String serialno;

    @TableField(exist = false)
    private String createBy;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField(exist = false)
    private Date createTime;

    @TableField(exist = false)
    private String updateBy;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField(exist = false)
    private Date updateTime;

    @TableField(exist = false)
    private String remark;

    @TableField(exist = false)
    private Integer isDelete = 0;
}