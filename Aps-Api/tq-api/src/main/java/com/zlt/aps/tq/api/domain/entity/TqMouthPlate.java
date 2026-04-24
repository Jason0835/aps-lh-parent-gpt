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
@TableName("T_TQ_MOUTH_PLATE")
@ApiModel(value = "胎圈口型板信息对象", description = "胎圈口型板信息对象")
public class TqMouthPlate extends BaseEntity {
    private static final long serialVersionUID = 1L;

    @Excel(name = "ui.data.column.mouthPlateCode")
    @ApiModelProperty(value = "口型板编号", position = 20)
    @TableField("MOUTH_PLATE_CODE")
    @ImportValidated(required = true, isCode = true, maxLength = 30)
    private String mouthPlateCode;

    @ApiModelProperty(value = "机台id", position = 30)
    @TableField("MACHINE_ID")
    private Long machineId;

    @Excel(name = "ui.data.column.mouthPlateStatus", dictType = "STATUS")
    @ApiModelProperty(value = "状态，0--启用，1--禁用", position = 40)
    @TableField("STATUS")
    @ImportValidated(required = true, maxLength = 6)
    private String status;

    @Excel(name = "ui.specifyMachine.column.machineName")
    @ApiModelProperty(value = "机台名称", position = 50)
    @TableField(exist = false)
    private String machineName;

    @Excel(name = "ui.common.column.remark")
    @ApiModelProperty(value = "备注", position = 500)
    @TableField("REMARK")
    @ImportValidated(maxLength = 300)
    private String remark;
}
