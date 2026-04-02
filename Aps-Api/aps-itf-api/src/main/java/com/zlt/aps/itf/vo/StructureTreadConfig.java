package com.zlt.aps.itf.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

/**
 * MES结构整车胎面配置中间表
 *
 * @author zlt
 * @since 2025/12/25
 */
@ApiModel(value = "MES结构整车胎面配置", description = "MES结构整车胎面配置")
@Data
public class StructureTreadConfig implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "主键")
    private Long id;

    @ApiModelProperty(value = "结构")
    private String structureCode;

    @ApiModelProperty(value = "整车胎面条数")
    private Integer treadCount;

    @ApiModelProperty(value = "删除标识")
    private String delFlag;

    @ApiModelProperty(value = "版本号")
    private String dataVersion;

    @ApiModelProperty(value = "分公司编码")
    private String companyCode;

    @ApiModelProperty(value = "厂别")
    private String factoryCode;

}
