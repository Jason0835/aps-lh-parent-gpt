package com.zlt.aps.cd15.api.domain.entity;

import com.ruoyi.common.core.annotation.Excel;
import com.zlt.aps.common.core.annotation.ImportValidated;
import com.zlt.aps.common.core.domain.ApsBaseEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 15°裁断机台信息对象 t_cd15_machine_info
 *
 * @author zlt
 * @date 2021-05-28
 */
@Data
@ApiModel(value = "15°裁断机台信息对象", description = "15°裁断机台信息对象 ")
public class Cd15MachineInfo extends ApsBaseEntity {
    private static final long serialVersionUID = 1L;

    /**
     * 主键ID，对应自增序列为：SEQ_PUBLIC
     */
    @ApiModelProperty(value = "主键ID", position = 10)
    private Long id;

    /**
     * 机台编号
     */
    @ApiModelProperty(value = "机台编号", position = 20)
    @Excel(name = "ui.data.column.machine.machineCode")
    @ImportValidated(required = true, isCode = true, maxLength = 30)
    private String machineCode;

    /**
     * 机台名称，比如：1线、2线
     */
    @ApiModelProperty(value = "机台名称", position = 30)
    @Excel(name = "ui.data.column.machine.machineName")
    @ImportValidated(required = true, maxLength = 20)
    private String machineName;

    /**
     * 生产定额，是指单班一次能生产的量，单位：吨/班
     */
    @ApiModelProperty(value = "生产定额", position = 75)
    @Excel(name = "ui.data.column.machine.quata")
    @ImportValidated(number = true, min = 0, max = 999999)
    private BigDecimal quata;

    /**
     * 班制，如：三班制，两班制；对应数据字典CLASS_SHIFT
     */
    @ApiModelProperty(value = "班制", position = 80)
    @Excel(name = "ui.data.column.machine.classShift", dictType = "CLASS_SHIFT")
    @ImportValidated(maxLength = 20 ,required = true)
    private String classShift;

    /**
     * 开机班次，如：中班、夜班；对应数据字典CLASS_NUM
     */
    @ApiModelProperty(value = "开机班次", position = 85)
    @Excel(name = "ui.data.column.machine.openMachineClass", dictType = "CLASS_NUM_THREE",dictTypeToExcelEnable = false)
    @ImportValidated(maxLength = 20)
    private String openMachineClass;

    /**
     * 机台状态，0--启用，1--禁用。对应数据字典STATUS
     */
    @ApiModelProperty(value = "机台状态", position = 90)
    @Excel(name = "ui.data.column.machine.status", dictType = "STATUS")
    @ImportValidated(maxLength = 2 ,required = true)
    private String status;

    /**
     * 是否支持一出二，0支持，1不支持。对应数据字典IS_SUPPORTED
     */
    @ApiModelProperty(value = "支持一出二", position = 100)
    @Excel(name = "ui.data.column.machine.isOutTwo", dictType = "IS_SUPPORTED")
    @ImportValidated(maxLength = 2 ,required = true)
    private String isOutTwo;

    /**
     * 支持的钢带宽度
     */
    @ApiModelProperty(value = "支持的钢带宽度", position = 110)
    @Excel(name = "ui.data.column.machine.steelStripWidth")
    @ImportValidated(number = true)
    private Double steelStripWidth;

    @Excel(name = "ui.common.column.remark")
    @ImportValidated(maxLength = 300)
    private String remark;
    
    private String factoryCode;

    /**
     * 删除标识：0--正常，1-删除.对应数据字典DEL_FLAG
     */
    private String delFlag;
}
