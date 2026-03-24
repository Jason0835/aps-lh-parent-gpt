package com.zlt.aps.itf.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

/**
 * 设备保养计划中间表实体
 * 对应中间库表 DEV_MAINTENANCE_PLAN
 */
@Getter
@Setter
@ApiModel(value = "设备保养计划中间表", description = "设备保养计划中间表")
public class DevMaintenancePlan extends SyncBaseEntity {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "主键ID")
    private Long id;

    @ApiModelProperty(value = "设备机台")
    private String devCode;

    @ApiModelProperty(value = "精度类型")
    private String precisionType;

    @ApiModelProperty(value = "计划时间")
    private String operTime;

    @ApiModelProperty(value = "实际时间")
    private String firstWashTime;

    @ApiModelProperty(value = "删除标识：0-正常，1-已删除")
    private Integer delFlag;

    @ApiModelProperty(value = "版本号")
    private String dataVersion;

    @ApiModelProperty(value = "分公司编码")
    private String companyCode;

    @ApiModelProperty(value = "厂别")
    private String factoryCode;
}
