package com.zlt.aps.mp.api.domain.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ruoyi.common.core.annotation.Excel;
import com.ruoyi.common.core.web.domain.BaseEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：FixedPointVulcanizingConfiguration.java
 * 描    述：基础数据-定点机台_硫化机列对象 t_mdm_fixed_point_vulcanizing_rela
 *
 * @author zlt
 * @version 1.0
 * <p>
 * 修改记录：
 * 修改时间：...
 * 修 改 人：zlt
 * 修改内容：...
 * @date 2025-02-21
 */

@Data
@TableName(value = "T_MDM_FIXED_POINT_VULCANIZING_RELA")
@ApiModel(value = "基础数据-定点机台_硫化机列对象", description = "基础数据-定点机台_硫化机列对象 ")
public class FixedPointVulcanizingConfiguration extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 定点机台ID
     */
    @Excel(name = "ui.data.column.fixedPointVulcanizingConfiguration.fixedId")
    @ApiModelProperty(value = "定点机台ID", name = "fixedId")
    @TableField(value = "FIXED_ID")
    private Long fixedId;
    /**
     * 硫化机机ID
     */
    @Excel(name = "ui.data.column.fixedPointVulcanizingConfiguration.lineId")
    @ApiModelProperty(value = "硫化机机ID", name = "lineId")
    @TableField(value = "LINE_ID")
    private Long lineId;

}