package com.zlt.aps.cx.api.domain.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.common.core.annotation.Excel;
import com.zlt.aps.common.core.domain.ApsBaseDto;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.Date;

/**
 * BOM信息对象 t_bom_info
 *
 * @author Chen
 * @date 2021-06-11
 */
@Data
@ApiModel(value = "BomInfo对象", description = "Bom信息")
public class BomInfoDto extends ApsBaseDto {
    private static final long serialVersionUID = 1L;

    /** 主键ID */
    @ApiModelProperty(value = "id")
    private Long id;

    /** 子物料品号 */
    @ApiModelProperty(value = "子物料品号")
    private String childMaterialCode;

    /** 子物料名称 */
    @ApiModelProperty(value = "子物料名称")
    private String childMaterialName;

    /** 子物料名称编码 */
    @ApiModelProperty(value = "子物料名称编码")
    private String childMaterialNameCode;

    /** 子物料代码 */
    @ApiModelProperty(value = "子物料代码")
    private String childCode;

    /** 单位描述 */
    @ApiModelProperty(value = "单位描述")
    private String unit;

    /** 用量 */
    @ApiModelProperty(value = "用量")
    private Long dosage;

    /** 组成用量 */
    @ApiModelProperty(value = "组成用量")
    private Long dosageForm;

    /** 父物料品号 */
    @ApiModelProperty(value = "父物料品号")
    private String parentMaterialCode;

    /** 父物料名称 */
    @ApiModelProperty(value = "父物料名称")
    private String parentMaterialName;

    /** 父物料代码 */
    @ApiModelProperty(value = "父物料代码")
    private String parentCode;

    /** 生产阶段 */
    @ApiModelProperty(value = "生产阶段")
    private String productionStage;

    /** 生产阶段码值 */
    @ApiModelProperty(value = "生产阶段码值")
    private String productionStageCode;

    /** SAP版本信息 */
    @ApiModelProperty(value = "SAP版本信息")
    private String sapVersion;

    /** BOM信息版本 */
    @ApiModelProperty(value = "BOM信息版本")
    private String bomVersion;

    /** 子物料版本 */
    @ApiModelProperty(value = "子物料版本")
    private String childMaterialVersion;

    /** BOM类型 */
    @ApiModelProperty(value = "BOM类型")
    private String bomType;

    /** 状态 */
    @ApiModelProperty(value = "状态")
    private String status;

    /** MES系统创建时间 */
    @JsonFormat(pattern = "yyyy-MM-dd",timezone = "GMT+8")
    @ApiModelProperty(value = "MES系统创建时间")
    private Date mesCreateDate;

    /** MES更新时间 */
    @JsonFormat(pattern = "yyyy-MM-dd",timezone = "GMT+8")
    @ApiModelProperty(value = "MES更新时间")
    private Date mesUpdateDate;

    /** 删除标识 */
    @ApiModelProperty(value = "删除标识")
    private Integer isDelete;

}
