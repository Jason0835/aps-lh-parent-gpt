package com.zlt.aps.monthplan.api.domain.entity;

import com.ruoyi.common.core.web.domain.BaseEntity;
import lombok.Data;
import com.ruoyi.common.core.annotation.Excel;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.zlt.common.domain.CommonBusiEntity;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：MdmCapsuleChuck.java
 * 描    述：胶囊卡盘台账对象 t_mdm_capsule_chuck
 *@author zlt
 *@date 2025-12-12
 *@version 1.0
 *
 *  修改记录：
 *     修改时间：...
 *     修 改 人：zlt
 *     修改内容：...
 */

@ApiModel(value = "胶囊卡盘台账对象", description = "胶囊卡盘台账对象 ")
@Data
@TableName(value = "T_MDM_CAPSULE_CHUCK")
public class MdmCapsuleChuck extends BaseEntity {

    private static final long serialVersionUID = 1L;

     /** 工厂编号 */
    @Excel(name = "ui.data.column.mdmCapsuleChuck.factoryCode", dictType = "biz_factory_name")
    @ApiModelProperty(value = "工厂编号", name = "factoryCode")
    @TableField(value = "FACTORY_CODE")
    private String factoryCode;

    /** TBR卡盘英寸 多个以,分隔 */
    @Excel(name = "ui.data.column.mdmCapsuleChuck.proSize")
    @ApiModelProperty(value = "TBR卡盘英寸 多个以,分隔", name = "proSize")
    @TableField(value = "PRO_SIZE")
    private String proSize;

    /** TBR卡盘 多个以,分隔 */
    @Excel(name = "ui.data.column.mdmCapsuleChuck.specifications")
    @ApiModelProperty(value = "TBR卡盘 多个以,分隔", name = "specifications")
    @TableField(value = "SPECIFICATIONS")
    private String specifications;

    /** 国内转移 */
    @Excel(name = "ui.data.column.mdmCapsuleChuck.internalQty")
    @ApiModelProperty(value = "国内转移", name = "internalQty")
    @TableField(value = "INTERNAL_QTY")
    private Integer internalQty;

    /** JINYU新卡盘 */
    @Excel(name = "ui.data.column.mdmCapsuleChuck.newChuckQty")
    @ApiModelProperty(value = "JINYU新卡盘", name = "newChuckQty")
    @TableField(value = "NEW_CHUCK_QTY")
    private Integer newChuckQty;


}