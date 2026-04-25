package com.zlt.aps.tq.api.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
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
@TableName("T_TQ_LOSS_SETTING")
@ApiModel(value = "胎圈损耗率设定对象", description = "胎圈损耗率设定对象")
public class TqLossSetting extends BaseEntity {
    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "主键ID")
    @TableId(value = "ID", type = IdType.AUTO)
    private Long id;

    @Excel(name = "ui.data.column.loss.beadCode", sort = 10)
    @ApiModelProperty(value = "胎圈编码", position = 20)
    @TableField("MATERIAL_CODE")
    @ImportValidated(isCode = true, maxLength = 20)
    private String materialCode;

    @ApiModelProperty(value = "机台id", position = 30)
    @TableField("MACHINE_ID")
    private Long machineId;

    @ApiModelProperty(value = "机台编号", position = 35)
    @TableField(exist = false)
    private String machineCode;

    @Excel(name = "ui.data.column.loss.line", sort = 20)
    @ApiModelProperty(value = "机台名称", position = 36)
    @TableField(exist = false)
    private String machineName;

    @Excel(name = "ui.data.column.loss.lossRate", suffix = "%", sort = 30)
    @ApiModelProperty(value = "损耗率(百分比)", position = 40)
    @TableField("LOSS_RATE")
    @ImportValidated(required = true, number = true, min = 0, max = 99.99)
    private Double lossRate;

    @Excel(name = "ui.common.column.remark", sort = 40)
    @ApiModelProperty(value = "备注", position = 500)
    @TableField("REMARK")
    @ImportValidated(maxLength = 300)
    private String remark;

    @ApiModelProperty(value = "删除标识", position = 600)
    @TableField("IS_DELETE")
    private Integer isDelete;
}
