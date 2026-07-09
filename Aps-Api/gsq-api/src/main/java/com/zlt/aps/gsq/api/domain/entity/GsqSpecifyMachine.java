package com.zlt.aps.gsq.api.domain.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ruoyi.common.core.annotation.Excel;
import com.ruoyi.common.core.web.domain.BaseEntity;
import com.zlt.aps.common.core.annotation.ImportValidated;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 钢丝圈定点机台对象 t_gsq_specify_machine
 *
 * @author zlt
 * @date 2026-07-08
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("T_GSQ_SPECIFY_MACHINE")
@ApiModel(value = "钢丝圈定点机台对象", description = "钢丝圈定点机台对象")
public class GsqSpecifyMachine extends BaseEntity {
    private static final long serialVersionUID = 1L;

    /** 钢丝圈代码 */
    @Excel(name = "ui.data.column.gsq.specifyMachine.steelRingCode")
    @ApiModelProperty(value = "钢丝圈代码", position = 20)
    @TableField("STEEL_RING_CODE")
    @ImportValidated(required = true, isCode = true, maxLength = 50)
    private String steelRingCode;

    /** 生产线（机台编码，对应T_GSQ_MACHINE_INFO表MACHINE_CODE） */
    @Excel(name = "ui.data.column.gsq.specifyMachine.machineCode")
    @ApiModelProperty(value = "生产线（机台编码）", position = 30)
    @TableField("MACHINE_CODE")
    @ImportValidated(required = true, maxLength = 50)
    private String machineCode;

    /** 生产线名称（反显字段，非数据库字段） */
    @Excel(name = "ui.data.column.gsq.specifyMachine.machineName")
    @ApiModelProperty(value = "生产线名称", position = 35)
    @TableField(exist = false)
    private String machineName;

    /** 线路类型，数据字典：LINE_TYPE */
    @Excel(name = "ui.data.column.gsq.specifyMachine.lineType", dictType = "LINE_TYPE")
    @ApiModelProperty(value = "线路类型", position = 40)
    @TableField("LINE_TYPE")
    @ImportValidated(maxLength = 10)
    private String lineType;

    /** 作业类型，数据字典：JOB_TYPE */
    @Excel(name = "ui.data.column.gsq.specifyMachine.jobType", dictType = "JOB_TYPE")
    @ApiModelProperty(value = "作业类型", position = 50)
    @TableField("JOB_TYPE")
    @ImportValidated(maxLength = 10)
    private String jobType;

    /** 备注 */
    @Excel(name = "ui.common.column.remark")
    @ApiModelProperty(value = "备注", position = 500)
    @TableField("REMARK")
    @ImportValidated(maxLength = 900)
    private String remark;
}
