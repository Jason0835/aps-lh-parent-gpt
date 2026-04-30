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
@TableName("T_TQ_TOOLING")
@ApiModel(value = "胎圈工装管理对象", description = "胎圈工装管理对象")
public class TqTooling extends BaseEntity {
    private static final long serialVersionUID = 1L;

    @Excel(name = "ui.tq.tooling.column.toolingCode")
    @ApiModelProperty(value = "工装编码", position = 20)
    @TableField("TOOLING_CODE")
    @ImportValidated(required = true, isCode = true, maxLength = 50)
    private String toolingCode;

    @Excel(name = "ui.tq.tooling.column.toolingName")
    @ApiModelProperty(value = "工装名称", position = 30)
    @TableField("TOOLING_NAME")
    @ImportValidated(maxLength = 100)
    private String toolingName;

    @Excel(name = "ui.tq.tooling.column.totalQty")
    @ApiModelProperty(value = "工装总数", position = 40)
    @TableField("TOTAL_QTY")
    private Integer totalQty;

    @Excel(name = "ui.common.column.remark")
    @ApiModelProperty(value = "备注", position = 500)
    @TableField("REMARK")
    @ImportValidated(maxLength = 500)
    private String remark;
}
