package com.zlt.aps.itf.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

/**
 * 模具清洗预警中间表VO
 *
 * @author zlt
 * @since 2025/12/25
 */
@ApiModel(value = "模具清洗预警中间表", description = "模具清洗预警中间表")
@Data
public class MouldCleanPlanVo implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "硫化机台")
    private String lhCode;

    @ApiModelProperty(value = "上机时间")
    private String operTime;

    @ApiModelProperty(value = "首次清洗时间")
    private String firstWashTime;

    @ApiModelProperty(value = "二次清洗时间")
    private String secondWashTime;

    @ApiModelProperty(value = "删除标识：0-正常，1-已删除")
    private String delFlag;

    @ApiModelProperty(value = "版本号")
    private String dataVersion;

    @ApiModelProperty(value = "分公司编码")
    private String companyCode;

    @ApiModelProperty(value = "厂别")
    private String factoryCode;
}
