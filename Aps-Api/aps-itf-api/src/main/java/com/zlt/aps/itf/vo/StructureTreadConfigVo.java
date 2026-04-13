package com.zlt.aps.itf.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

/**
 * MES结构整车胎面配置中间表VO
 *
 * @author zlt
 * @since 2025/12/25
 */
@ApiModel(value = "MES结构整车胎面配置", description = "MES结构整车胎面配置")
@Data
public class StructureTreadConfigVo implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "结构")
    private String structureCode;

    @ApiModelProperty(value = "结构")
    private Integer treadCount;

    @ApiModelProperty(value = "厂别")
    private String factoryCode;

    @ApiModelProperty(value = "删除标识：0-正常，1-已删除")
    private String delFlag;

    @ApiModelProperty(value = "版本号")
    private String dataVersion;

    @ApiModelProperty(value = "分公司编码")
    private String companyCode;
}
