package com.zlt.aps.tm.api.domain.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ruoyi.common.core.annotation.Excel;
import com.ruoyi.common.core.web.domain.BaseEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@ApiModel(value = "胎面胶料顺序对象", description = "胎面胶料顺序对象")
@Data
@TableName(value = "T_TM_GLUE_ORDER")
public class TmGlueOrder extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @Excel(name = "ui.data.column.tm.GlueOrder.factoryCode")
    @ApiModelProperty(value = "工厂编号", name = "factoryCode")
    @TableField(value = "FACTORY_CODE")
    private String factoryCode;

    @Excel(name = "ui.data.column.tm.GlueOrder.glueGroupId")
    @ApiModelProperty(value = "胶料组ID", name = "glueGroupId")
    @TableField(value = "GLUE_GROUP_ID")
    private Long glueGroupId;

    @Excel(name = "ui.data.column.tm.GlueOrder.glueCode")
    @ApiModelProperty(value = "胶料号", name = "glueCode")
    @TableField(value = "GLUE_CODE")
    private String glueCode;

    @Excel(name = "ui.data.column.tm.GlueOrder.machineCode")
    @ApiModelProperty(value = "机台编码", name = "machineCode")
    @TableField(value = "MACHINE_CODE")
    private String machineCode;

    @Excel(name = "ui.data.column.tm.GlueOrder.orderNum")
    @ApiModelProperty(value = "排序号", name = "orderNum")
    @TableField(value = "ORDER_NUM")
    private Integer orderNum;

}
