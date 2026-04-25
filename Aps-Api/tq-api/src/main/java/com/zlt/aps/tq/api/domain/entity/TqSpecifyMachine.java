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
@TableName("T_TQ_SPECIFY_MACHINE")
@ApiModel(value = "胎圈定点机台信息对象", description = "胎圈定点机台信息对象")
public class TqSpecifyMachine extends BaseEntity {
    private static final long serialVersionUID = 1L;

    @Excel(name = "ui.tq.specifyMachine.column.materialCode")
    @ApiModelProperty(value = "胎圈编码", position = 20)
    @TableField("MATERIAL_CODE")
    @ImportValidated(required = true, isCode = true, maxLength = 20)
    private String materialCode;

    @ApiModelProperty(value = "机台id", position = 30)
    @TableField("MACHINE_ID")
    private Long machineId;

    @ApiModelProperty(value = "机台编号", position = 35)
    @TableField(exist = false)
    private String machineCode;

    @Excel(name = "ui.specifyMachine.column.machineName")
    @ApiModelProperty(value = "机台名称", position = 36)
    @TableField(exist = false)
    private String machineName;

    @Excel(name = "ui.specifyMachine.column.lineType", dictType = "LINE_TYPE")
    @ApiModelProperty(value = "线路：0-生产线、1-备用线", position = 40)
    @TableField("LINE_TYPE")
    @ImportValidated(required = true, maxLength = 9)
    private String lineType;

    @Excel(name = "ui.specifyMachine.column.jobType", dictType = "JOB_TYPE")
    @ApiModelProperty(value = "作业类型：0-限制作业；1-不可作业", position = 50)
    @TableField("JOB_TYPE")
    @ImportValidated(required = true, maxLength = 12)
    private String jobType;

    @Excel(name = "ui.data.column.stock.remark")
    @ApiModelProperty(value = "备注", position = 500)
    @TableField("REMARK")
    @ImportValidated(maxLength = 300)
    private String remark;
}
