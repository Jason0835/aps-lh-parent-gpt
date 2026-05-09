package com.zlt.aps.mdm.api.domain.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ruoyi.common.core.web.domain.BaseEntity;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Copyright (c) 2022, All rights reserved。 文件名称：MdmConstructionProcess.java 描
 * 述：示方书工艺信息 t_mdm_construction_process
 * 
 * @author zlt
 * @date 2026-05-08
 * @version 1.0
 *
 *          修改记录： 修改时间：... 修 改 人：zlt 修改内容：...
 */

@ApiModel(value = "示方书工艺信息", description = "示方书工艺信息")
@Data
@EqualsAndHashCode(callSuper = false)
@TableName(value = "T_MDM_CONSTRUCTION_PROCESS")
public class MdmConstructionProcess extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /** 工厂编号 */
    @ApiModelProperty(value = "工厂编号", name = "factoryCode")
    @TableField(value = "FACTORY_CODE")
    private String factoryCode;

    /** 物料品号 */
    @ApiModelProperty(value = "物料品号", name = "materialCode")
    @TableField(value = "MATERIAL_CODE")
    private String materialCode;

    /** 关联的物料版本 */
    @ApiModelProperty(value = "关联的物料版本", name = "materialVersion")
    @TableField(value = "MATERIAL_VERSION")
    private String materialVersion;

    /** 物料描述 */
    @ApiModelProperty(value = "物料描述", name = "materialDesc")
    @TableField(value = "MATERIAL_DESC")
    private String materialDesc;

    /** 工艺类型。1、长度，2、宽度，3、幅宽 */
    @ApiModelProperty(value = "工艺类型。1、长度，2、宽度，3、幅宽", name = "processCode")
    @TableField(value = "PROCESS_CODE")
    private String processCode;

    /** 产品品类 biz_product_type TBR 全钢 PCR 半钢 */
    @ApiModelProperty(value = "产品品类 biz_product_type TBR 全钢 PCR 半钢", name = "productTypeCode")
    @TableField(value = "PRODUCT_TYPE_CODE")
    private String productTypeCode;

    /** 工艺数值 */
    @ApiModelProperty(value = "工艺数值", name = "processValue")
    @TableField(value = "PROCESS_VALUE")
    private String processValue;

    /** 单位标准标准缩写，例如kg（公斤）、mm(毫米) */
    @ApiModelProperty(value = "单位标准标准缩写，例如kg（公斤）、mm(毫米)", name = "unit")
    @TableField(value = "UNIT")
    private String unit;
}