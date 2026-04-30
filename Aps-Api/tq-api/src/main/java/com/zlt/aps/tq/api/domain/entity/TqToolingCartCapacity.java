package com.zlt.aps.tq.api.domain.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ruoyi.common.core.annotation.Excel;
import com.ruoyi.common.core.web.domain.BaseEntity;
import com.zlt.aps.common.core.annotation.ImportValidated;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = false)
@TableName("T_TQ_TOOLING_CART_CAPACITY")
@ApiModel(value = "胎圈工装车容量管理对象", description = "胎圈工装车容量管理对象")
public class TqToolingCartCapacity extends BaseEntity {
    private static final long serialVersionUID = 1L;

    @Excel(name = "ui.tq.toolingCartCapacity.column.cartCode")
    @ApiModelProperty(value = "工装车编码", position = 20)
    @TableField("CART_CODE")
    @ImportValidated(required = true, isCode = true, maxLength = 50)
    private String cartCode;

    @Excel(name = "ui.tq.toolingCartCapacity.column.materialCode")
    @ApiModelProperty(value = "胎圈编码", position = 30)
    @TableField("MATERIAL_CODE")
    @ImportValidated(required = true, isCode = true, maxLength = 60)
    private String materialCode;

    @Excel(name = "ui.tq.toolingCartCapacity.column.cartCapacity")
    @ApiModelProperty(value = "整车容量", position = 40)
    @TableField("CART_CAPACITY")
    private Integer cartCapacity;

    @Excel(name = "ui.common.column.remark")
    @ApiModelProperty(value = "备注", position = 500)
    @TableField("REMARK")
    @ImportValidated(maxLength = 500)
    private String remark;
}
