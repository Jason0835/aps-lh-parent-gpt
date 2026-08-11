package com.zlt.aps.tc.api.domain.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ruoyi.common.core.annotation.Excel;
import com.ruoyi.common.core.web.domain.BaseEntity;
import com.zlt.common.annotation.ImportExcelValidated;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@ApiModel(value = "胎侧胶料顺序对象", description = "胎侧胶料顺序对象")
@Data
@TableName(value = "T_TC_GLUE_ORDER")
public class TcGlueOrder extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @Excel(name = "ui.data.column.tc.glueOrder.factoryCode", dictType = "biz_factory_name")
    @ImportExcelValidated(required = true, isCode = true, maxLength = 50)
    @ApiModelProperty(value = "工厂编号", name = "factoryCode")
    @TableField(value = "FACTORY_CODE")
    private String factoryCode;

    @Excel(name = "ui.data.column.tc.glueOrder.glueGroupCode")
    @ImportExcelValidated(required = true, isCode = true, maxLength = 30)
    @ApiModelProperty(value = "胶料组编码", name = "glueGroupCode")
    @TableField(value = "GLUE_GROUP_CODE")
    private String glueGroupCode;

    @Excel(name = "ui.data.column.tc.glueOrder.glueCode")
    @ImportExcelValidated(required = true, isCode = true, maxLength = 30)
    @ApiModelProperty(value = "胶料号", name = "glueCode")
    @TableField(value = "GLUE_CODE")
    private String glueCode;

    @Excel(name = "ui.data.column.tc.glueOrder.orderNum")
    @ImportExcelValidated(required = true, digits = true, min = 0, max = 999)
    @ApiModelProperty(value = "排序号", name = "orderNum")
    @TableField(value = "ORDER_NUM")
    private Integer orderNum;

    @Excel(name = "ui.common.column.remark")
    @ImportExcelValidated(maxLength = 500)
    @ApiModelProperty("备注")
    @TableField("REMARK")
    private String remark;
}