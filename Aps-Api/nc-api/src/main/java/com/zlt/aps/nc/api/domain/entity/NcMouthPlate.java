package com.zlt.aps.nc.api.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.ruoyi.common.core.annotation.Excel;
import com.zlt.aps.common.core.domain.ApsBaseEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * <p>
 * 内衬口型板信息维护
 * </p>
 *
 * @author zlt
 * @since 2026-07-02
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("T_NC_MOUTH_PLATE")
@ApiModel(value = "NcMouthPlate对象", description = "内衬口型板信息维护")
public class NcMouthPlate extends ApsBaseEntity {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "工厂编码")
    @TableField(value = "FACTORY_CODE")
    private String factoryCode;

    @Excel(name = "ui.data.column.mouthPlateCode", sort = 10)
    @ApiModelProperty(value = "口型板编号。一个口型板编号可以对应多个机台。", position = 20)
    @TableField("MOUTH_PLATE_CODE")
    private String mouthPlateCode;

    @ApiModelProperty(value = "机台编码", position = 30)
    @TableField("MACHINE_CODE")
    private Long machineCode;

    @Excel(name = "ui.data.column.mouthPlateStatus", sort = 30, dictType = "STATUS")
    @ApiModelProperty(value = "状态，0--启用，1--禁用。", position = 40)
    @TableField("STATUS")
    private String status;

    @Excel(name = "ui.data.column.machine.machineName", importName = "ui.data.column.machine.machineName", sort = 20)
    @TableField(exist = false)
    @ApiModelProperty(value = "机台名称", position = 50)
    private String machineName;
}
