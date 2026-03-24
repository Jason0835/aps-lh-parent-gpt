package com.zlt.aps.itf.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

/**
 * 模具清洗预警计划中间表
 *
 * @author zlt
 * @since 2025/12/25
 */
@Getter
@Setter
@ApiModel(value = "模具清洗预警计划中间表", description = "模具清洗预警计划中间表")
public class MouldCleanPlan extends SyncBaseEntity {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "主键ID")
    private Long id;

    @ApiModelProperty(value = "硫化机台")
    private String lhCode;

    @ApiModelProperty(value = "上机时间")
    private String operTime;

    @ApiModelProperty(value = "首次清洗时间")
    private String firstWashTime;

    @ApiModelProperty(value = "二次清洗时间")
    private String secondWashTime;

    @ApiModelProperty(value = "备注")
    private String remark;

    @ApiModelProperty(value = "删除标识：0-正常，1-已删除")
    private Integer delFlag;

    @ApiModelProperty(value = "版本号")
    private String dataVersion;

    @ApiModelProperty(value = "分公司编码")
    private String companyCode;

    @ApiModelProperty(value = "厂别")
    private String factoryCode;

}
