package com.zlt.aps.mp.api.domain.entity;

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
 * 文件名称：MdmSkuScheduleCategory.java
 * 描    述：SKU排产分类对象 t_mdm_sku_schedule_category
 *
 * @author zlt
 * @version 1.0
 * <p>
 * 修改记录：
 * 修改时间：...
 * 修 改 人：zlt
 * 修改内容：...
 * @date 2025-12-11
 */
@ApiModel(value = "SKU排产分类对象", description = "SKU排产分类对象 ")
@Data
@TableName(value = "T_MDM_SKU_SCHEDULE_CATEGORY")
public class MdmSkuScheduleCategory extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 工厂编号，字典：biz_factory_name
     */
    @ImportExcelValidated(required = true)
    @Excel(name = "ui.data.column.mdmSkuScheduleCategory.factoryCode", dictType = "biz_factory_name")
    @ApiModelProperty(value = "工厂编号，字典：biz_factory_name", name = "factoryCode")
    @TableField(value = "FACTORY_CODE")
    private String factoryCode;

    /**
     * 物料编码
     */
    @ImportExcelValidated(required = true, maxLength = 32)
    @Excel(name = "ui.data.column.mdmSkuScheduleCategory.materialCode")
    @ApiModelProperty(value = "物料编码", name = "materialCode")
    @TableField(value = "MATERIAL_CODE")
    private String materialCode;

    /**
     * 排产类型，字典：schedule_type，01-主销产品、02-常规产品、03-常规周期产品、04-波动性产品、05-按单排产产品
     */
    @ImportExcelValidated(required = true)
    @Excel(name = "ui.data.column.mdmSkuScheduleCategory.scheduleType", dictType = "biz_schedule_type")
    @ApiModelProperty(value = "排产类型，字典：schedule_type，01-主销产品、02-常规产品、03-常规周期产品、04-波动性产品、05-按单排产产品", name = "scheduleType")
    @TableField(value = "SCHEDULE_TYPE")
    private String scheduleType;

    /**
     * 生成日期
     */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "ui.data.column.mdmSkuScheduleCategory.generateDate", width = 30, dateFormat = "yyyy-MM-dd")
    @ApiModelProperty(value = "生成日期", name = "genrateDate")
    @TableField(value = "GENRATE_DATE")
    private Date genrateDate;


}
