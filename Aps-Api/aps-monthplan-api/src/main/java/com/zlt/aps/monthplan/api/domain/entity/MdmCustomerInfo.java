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
 * 文件名称：MdmCustomerInfo.java
 * 描    述：客户信息对象 t_mdm_customer_info
 *@author zlt
 *@date 2025-03-04
 *@version 1.0
 *
 *  修改记录：
 *     修改时间：...
 *     修 改 人：zlt
 *     修改内容：...
 */
@ApiModel(value = "客户信息对象", description = "客户信息对象 ")
@Data
@TableName(value = "T_MDM_CUSTOMER_INFO")
public class MdmCustomerInfo extends BaseEntity {

    private static final long serialVersionUID = 1L;

     /** 分厂编号 */
    @ImportExcelValidated(required = true)
    @Excel(name = "ui.data.column.mdmCustomerInfo.factoryCode", dictType = "biz_factory_name")
    @ApiModelProperty(value = "分厂编号，字典：biz_factory_name", name = "factoryCode")
    @TableField(value = "FACTORY_CODE")
    private String factoryCode;

    /** 客户编号 */
    @ImportExcelValidated(required = true, maxLength = 20)
    @Excel(name = "ui.data.column.mdmCustomerInfo.customCode")
    @ApiModelProperty(value = "客户编号", name = "customCode")
    @TableField(value = "CUSTOM_CODE")
    private String customCode;

    /** 客户名称 */
    @ImportExcelValidated(required = true, maxLength = 66)
    @Excel(name = "ui.data.column.mdmCustomerInfo.customName")
    @ApiModelProperty(value = "客户名称", name = "customName")
    @TableField(value = "CUSTOM_NAME")
    private String customName;

    /** 是否重要客户 0 不重要 1 重要 默认 0 */
    @ImportExcelValidated(required = true)
    @Excel(name = "ui.data.column.mdmCustomerInfo.isImportantCustom", dictType = "biz_yes_no")
    @ApiModelProperty(value = "是否重要客户，字典：biz_yes_no", name = "isImportantCustom")
    @TableField(value = "IS_IMPORTANT_CUSTOM")
    private Integer isImportantCustom;

    @TableField(exist = false)
    private String remark;
}