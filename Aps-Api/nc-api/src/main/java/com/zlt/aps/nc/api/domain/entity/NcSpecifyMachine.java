package com.zlt.aps.nc.api.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.ruoyi.common.core.annotation.Excel;
import com.zlt.aps.common.core.domain.ApsBaseEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * <p>
 * 定点机台表
 * </p>
 *
 * @author zlt
 * @since 2026-06-04
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("T_NC_SPECIFY_MACHINE")
@ApiModel(value = "NcSpecifyMachine对象", description = "定点机台表")
public class NcSpecifyMachine extends ApsBaseEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "工厂编码")
    @TableField(value = "FACTORY_CODE")
    private String factoryCode;

    @Excel(name="ui.nc.specifyMachine.column.liningCode")
    @ApiModelProperty(value = "内衬代码")
    @TableField("LINING_CODE")
    private String liningCode;

    @Excel(name="ui.specifyMachine.column.machineName")
    @ApiModelProperty(value = "机台Code（对应T_NC_MACHINE_INFO表machineCode）")
    @TableField("MACHINE_CODE")
    private Long machineCode;

    @Excel(name="ui.specifyMachine.column.lineType", dictType="LINE_TYPE")
    @ApiModelProperty(value = "线路，数据维护在数据字典：0-生产线、1-备用线")
    @TableField("LINE_TYPE")
    private String lineType;

    @Excel(name="ui.specifyMachine.column.jobType", dictType="JOB_TYPE")
    @ApiModelProperty(value = "作业类型，数据维护在数据字典：0-限制作业；1-不可作业")
    @TableField("JOB_TYPE")
    private String jobType;
}
