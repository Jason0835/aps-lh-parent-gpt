package com.zlt.aps.itf.vo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

@ApiModel(value = "设备保养计划中间表", description = "设备保养计划中间表")
@Data
@TableName("DEV_MAINTENANCE_PLAN")
public class DevMaintenancePlan implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "主键ID")
    @TableId(value = "ID", type = IdType.INPUT)
    private Long id;

    @ApiModelProperty(value = "设备机台")
    @TableField(value = "DEV_CODE")
    private String devCode;

    @ApiModelProperty(value = "精度类型")
    @TableField(value = "PRECISION_TYPE")
    private String precisionType;

    @ApiModelProperty(value = "计划时间")
    @TableField(value = "OPER_TIME")
    private String operTime;

    @ApiModelProperty(value = "实际时间")
    @TableField(value = "FIRST_WASH_TIME")
    private String firstWashTime;

    @ApiModelProperty(value = "删除标识：0-正常，1-已删除")
    @TableField(value = "DEL_FLAG")
    private String delFlag;

    @ApiModelProperty(value = "版本号")
    @TableField(value = "DATA_VERSION")
    private String dataVersion;

    @ApiModelProperty(value = "分公司编码")
    @TableField(value = "COMPANY_CODE")
    private String companyCode;

    @ApiModelProperty(value = "厂别")
    @TableField(value = "FACTORY_CODE")
    private String factoryCode;
}
