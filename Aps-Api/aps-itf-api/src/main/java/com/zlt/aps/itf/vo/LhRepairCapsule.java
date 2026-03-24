package com.zlt.aps.itf.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

/**
 * 胶囊已使用次数中间表
 *
 * @author zlt
 * @since 2025/12/25
 */
@Getter
@Setter
@ApiModel(value = "胶囊已使用次数中间表", description = "胶囊已使用次数中间表")
public class LhRepairCapsule extends SyncBaseEntity {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "主键ID")
    private Long id;

    @ApiModelProperty(value = "获取日期")
    private String obtainTime;

    @ApiModelProperty(value = "硫化机台")
    private String lhCode;

    @ApiModelProperty(value = "当前生产的物料编码")
    private String materialCode;

    @ApiModelProperty(value = "已使用次数")
    private Integer replaceCapsuleCount;

    @ApiModelProperty(value = "已使用次数2")
    private Integer replaceCapsuleCount2;

    @ApiModelProperty(value = "品牌")
    private String brand;

    @ApiModelProperty(value = "备注")
    private String remark;

    @ApiModelProperty(value = "版本号")
    private String dataVersion;

    @ApiModelProperty(value = "分公司编码")
    private String companyCode;

    @ApiModelProperty(value = "分厂")
    private String factoryCode;

}
