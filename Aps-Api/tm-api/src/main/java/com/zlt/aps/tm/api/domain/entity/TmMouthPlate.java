package com.zlt.aps.tm.api.domain.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ruoyi.common.core.annotation.Excel;
import com.ruoyi.common.core.web.domain.BaseEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@ApiModel(value = "胎面口型板信息对象", description = "胎面口型板信息对象")
@Data
@TableName(value = "T_TM_MOUTH_PLATE")
public class TmMouthPlate extends BaseEntity {
    private static final long serialVersionUID = 1L;

    @Excel(name = "ui.data.column.tm.MouthPlate.factoryCode")
    @ApiModelProperty(value = "工厂编号", name = "factoryCode")
    @TableField(value = "FACTORY_CODE")
    private String factoryCode;

    @Excel(name = "ui.data.column.tm.MouthPlate.mouthPlateCode")
    @ApiModelProperty(value = "口型板编码", name = "mouthPlateCode")
    @TableField(value = "MOUTH_PLATE_CODE")
    private String mouthPlateCode;

    @Excel(name = "ui.data.column.tm.MouthPlate.machineCode")
    @ApiModelProperty(value = "机台编码", name = "machineCode")
    @TableField(value = "MACHINE_CODE")
    private String machineCode;

    @Excel(name = "ui.data.column.tm.MouthPlate.plateStatus")
    @ApiModelProperty(value = "口型板状态", name = "plateStatus")
    @TableField(value = "PLATE_STATUS")
    private String plateStatus;
}
