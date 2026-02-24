package com.zlt.aps.monthplan.api.domain.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ruoyi.common.core.annotation.Excel;
import com.ruoyi.common.core.web.domain.BaseEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：FixedPointConfiguration.java
 * 描    述：基础数据-定点机台主对象 t_mdm_fixed_point
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
@TableName(value = "T_MDM_FIXED_POINT")
@ApiModel(value = "基础数据-定点机台主对象", description = "基础数据-定点机台主对象 ")
public class FixedPointConfiguration extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 分厂编号
     */
    @Excel(name = "ui.data.column.fixedPointConfiguration.factoryCode")
    @ApiModelProperty(value = "分厂编号", name = "factoryCode")
    @TableField(value = "FACTORY_CODE")
    private String factoryCode;

    /**
     * 生产线:0-生产线，1-备用线
     */
    @Excel(name = "ui.data.column.fixedPointConfiguration.productionLineType")
    @ApiModelProperty(value = "生产线:0-生产线，1-备用线", name = "productionLineType")
    @TableField(value = "PRODUCTION_LINE_TYPE")
    private Integer productionLineType;

    /**
     * 是否封存:0-可用，1-封存
     */
    @Excel(name = "ui.data.column.fixedPointConfiguration.isClosed")
    @ApiModelProperty(value = "是否封存:0-可用，1-封存", name = "isClosed")
    @TableField(value = "IS_CLOSED")
    private Integer isClosed;

}