package com.zlt.aps.lh.api.domain.entity;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ruoyi.common.core.annotation.Excel;
import com.zlt.aps.common.core.domain.ApsBaseEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import com.ruoyi.common.core.web.domain.BaseEntity;
import lombok.Data;

/**
 * 密炼机台信息对象 t_mix_machine_info
 * 
 * @author zlt
 * @date 2021-11-09
 */
@ApiModel(value = "密炼机台信息对象", description = "密炼机台信息对象 ")
@Data
public class MixMachineInfo extends ApsBaseEntity{

    private static final long serialVersionUID = 1L;

    /** 主键 */
    @ApiModelProperty(value = "${comment}")
    private Long id;

    /** 分厂编号 */
    @Excel(name = "ui.data.column.mixMachine.factoryCode")
    @ApiModelProperty(value = "分厂编号")
    private String factoryCode;

    /** 机台编号 */
    @Excel(name = "ui.data.column.mixMachine.machineCode")
    @ApiModelProperty(value = "机台编号")
    private String machineCode;

    /** 机台名称 */
    @Excel(name = "ui.data.column.mixMachine.machineName")
    @ApiModelProperty(value = "机台名称")
    private String machineName;

    /** 机台类型 */
    @Excel(name = "ui.data.column.mixMachine.machineType")
    @ApiModelProperty(value = "机台类型")
    private String machineType;

    /** 机台状态 */
    @Excel(name = "ui.data.column.mixMachine.status")
    @ApiModelProperty(value = "机台状态")
    private String status;

    /** 删除标识 */
    @ApiModelProperty(value = "机台状态")
    private String delFlag;





}
