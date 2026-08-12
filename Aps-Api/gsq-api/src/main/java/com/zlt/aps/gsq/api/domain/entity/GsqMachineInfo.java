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

import java.math.BigDecimal;

/**
 * 钢丝圈机台信息对象 t_gsq_machine_info
 *
 * @author zlt
 * @date 2021-05-28
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("t_gsq_machine_info")
@ApiModel(value = "钢丝圈机台信息对象", description = "钢丝圈机台信息对象 ")
public class GsqMachineInfo extends BaseEntity {
    private static final long serialVersionUID = 1L;

    /**
     * 机台编号
     */
    @ApiModelProperty(value = "机台编号", position = 20)
    @Excel(name = "ui.data.column.machine.machineCode")
    @TableField("MACHINE_CODE")
    @ImportValidated(required = true, isCode = true, maxLength = 30)
    private String machineCode;

    /**
     * 机台名称，比如：1线、2线
     */
    @ApiModelProperty(value = "机台名称", position = 30)
    @Excel(name = "ui.data.column.machine.machineName")
    @TableField("MACHINE_NAME")
    @ImportValidated(required = true, maxLength = 20)
    private String machineName;

    /**
     * 生产定额，是指单班一次能生产的量，单位：吨/班
     */
    @ApiModelProperty(value = "生产定额", position = 75)
    @Excel(name = "ui.data.column.machine.quata")
    @TableField("QUATA")
    @ImportValidated(number = true, min = 0, max = 999999)
    private BigDecimal quata;

    /**
     * 班制，如：三班制，两班制；对应数据字典LH_CLASS_SHIFT
     */
    @ApiModelProperty(value = "班制", position = 80)
    @Excel(name = "ui.data.column.machine.classShift", dictType = "LH_CLASS_SHIFT")
    @TableField("CLASS_SHIFT")
    @ImportValidated(maxLength = 20, required = true)
    private String classShift;

    /**
     * 开机班次，如：中班、夜班；对应数据字典class_num_three_plan
     */
    @ApiModelProperty(value = "开机班次", position = 85)
    @Excel(name = "ui.data.column.machine.openMachineClass", dictType = "class_num_three_plan", dictTypeToExcelEnable = false)
    @TableField("OPEN_MACHINE_CLASS")
    @ImportValidated(maxLength = 20)
    private String openMachineClass;

    /**
     * 机台状态，1--启用，0--禁用。对应数据字典STATUS
     */
    @ApiModelProperty(value = "机台状态", position = 90)
    @Excel(name = "ui.data.column.machine.status", dictType = "STATUS")
    @TableField("STATUS")
    @ImportValidated(maxLength = 2, required = true)
    private String status;

    /**
     * 备注
     */
    @Excel(name = "ui.common.column.remark")
    @TableField("REMARK")
    @ImportValidated(maxLength = 300)
    private String remark;

    /**
     * 排序字段（非数据库字段，用于自定义排序）
     */
    @TableField(exist = false)
    private String orderStr;

    @Override
    public String getRemark() {
        return remark;
    }

    @Override
    public void setRemark(String remark) {
        this.remark = remark;
    }
}
