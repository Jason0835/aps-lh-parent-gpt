package com.zlt.aps.monthplan.api.domain.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ruoyi.common.core.annotation.Excel;
import com.ruoyi.common.core.web.domain.BaseEntity;
import com.zlt.common.annotation.ImportExcelValidated;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：ProductMoldingLimit.java
 * 描    述：基础数据-品种限制成型机对象 t_mdm_molding_limit
 *
 * @author zlt
 * @version 1.0
 * <p>
 * 修改记录：
 * 修改时间：...
 * 修 改 人：zlt
 * 修改内容：...
 * @date 2025-02-20
 */

@Data
@TableName(value = "T_MDM_MOLDING_LIMIT")
@ApiModel(value = "基础数据-品种限制成型机对象", description = "基础数据-品种限制成型机对象 ")
public class ProductMoldingLimit extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 分厂编号
     */
    @ImportExcelValidated(required = true)
    @Excel(name = "ui.data.column.ProductMoldingLimit.factoryCode", dictType = "biz_factory_name")
    @ApiModelProperty(value = "分厂编号，字典：biz_factory_name", name = "factoryCode")
    @TableField(value = "FACTORY_CODE")
    private String factoryCode;

    /**
     * 外胎编码
     */
    @ImportExcelValidated(required = true)
    @Excel(name = "ui.data.column.ProductMoldingLimit.sapCode")
    @ApiModelProperty(value = "外胎编码", name = "sapCode")
    @TableField(value = "SAP_CODE")
    private String sapCode;

    /**
     * 胎胚编码
     */
    @ImportExcelValidated(required = true)
    @Excel(name = "ui.data.column.ProductMoldingLimit.embryoCode")
    @ApiModelProperty(value = "胎胚编码", name = "embryoCode")
    @TableField(value = "EMBRYO_CODE")
    private String embryoCode;

    /**
     * 成型机编号
     */
    @ImportExcelValidated(required = true)
    @Excel(name = "ui.data.column.ProductMoldingLimit.machineCode")
    @ApiModelProperty(value = "成型机编号", name = "machineCode")
    @TableField(value = "MACHINE_CODE")
    private String machineCode;

    /**
     * 限制生产:0-限制生产，1-禁止生产
     */
    @ImportExcelValidated(required = true)
    @Excel(name = "ui.data.column.ProductMoldingLimit.jobType", dictType = "JOB_TYPE")
    @ApiModelProperty(value = "限制生产，字典：JOB_TYPE", name = "jobType")
    @TableField(value = "JOB_TYPE")
    private String jobType ;
}