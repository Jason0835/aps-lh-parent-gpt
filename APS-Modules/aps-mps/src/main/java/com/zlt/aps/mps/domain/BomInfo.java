package com.zlt.aps.mps.domain;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.common.core.annotation.Excel;
import com.zlt.aps.common.core.domain.ApsBaseEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * BOM信息对象 t_bom_info
 *
 * @author Chen
 * @date 2021-06-11
 */
@Data
@TableName("T_BOM_INFO")
@ApiModel(value = "BomInfo对象", description = "BOM信息表")
@KeySequence(value = "SEQ_BOM_INFO",dbType = DbType.ORACLE)
public class BomInfo implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键ID */
    @ApiModelProperty(value = "${comment}")
    private Long id;

    /** 子物料品号 */
    @Excel(name = "ui.data.column.info.childMaterialCode")
    @ApiModelProperty(value = "子物料品号")
    private String childMaterialCode;

    /** 子物料名称 */
    @Excel(name = "ui.data.column.info.childMaterialName")
    @ApiModelProperty(value = "子物料名称")
    private String childMaterialName;

    /** 子物料名称编码(名称中文映射) */
    @Excel(name = "ui.data.column.info.childMaterialNameCode")
    @ApiModelProperty(value = "子物料名称编码(名称中文映射)")
    private String childMaterialNameCode;

    /** 子物料代码 */
    @Excel(name = "ui.data.column.info.childCode")
    @ApiModelProperty(value = "子物料代码")
    private String childCode;

    /** 单位描述 */
    @Excel(name = "ui.data.column.info.unit")
    @ApiModelProperty(value = "单位描述")
    private String unit;

    /** 用量，单胎消耗量 */
    @Excel(name = "ui.data.column.info.dosage")
    @ApiModelProperty(value = "用量，单胎消耗量")
    private Long dosage;

    /** 组成用量，单胎需要的数量 */
    @Excel(name = "ui.data.column.info.dosageForm")
    @ApiModelProperty(value = "组成用量，单胎需要的数量")
    private Long dosageForm;

    /** 父物料品号 */
    @Excel(name = "ui.data.column.info.parentMaterialCode")
    @ApiModelProperty(value = "父物料品号")
    private String parentMaterialCode;

    /** 父物料名称 */
    @Excel(name = "ui.data.column.info.parentMaterialName")
    @ApiModelProperty(value = "父物料名称")
    private String parentMaterialName;

    /** 生产阶段 */
    @Excel(name = "ui.data.column.info.productionStage")
    @ApiModelProperty(value = "生产阶段")
    private String productionStage;

    /** SAP版本信息 */
    @Excel(name = "ui.data.column.info.sapVersion")
    @ApiModelProperty(value = "SAP版本信息")
    private String sapVersion;

    /** 父物料代码 */
    @Excel(name = "ui.data.column.info.parentCode")
    @ApiModelProperty(value = "父物料代码")
    private String parentCode;

    /** BOM信息版本 */
    @Excel(name = "ui.data.column.info.bomVersion")
    @ApiModelProperty(value = "BOM信息版本")
    private String bomVersion;

    /** 子物料版本 */
    @Excel(name = "ui.data.column.info.childMaterialVersion")
    @ApiModelProperty(value = "子物料版本")
    private String childMaterialVersion;

    /** 生产阶段中文映射（0：投产阶段；1试做阶段） */
    @Excel(name = "ui.data.column.info.productionStageCode", readConverterExp = "0=：投产阶段；1试做阶段")
    @ApiModelProperty(value = "生产阶段中文映射")
    private String productionStageCode;

    /** BOM类型 */
    @Excel(name = "ui.data.column.info.bomType")
    @ApiModelProperty(value = "BOM类型")
    private String bomType;

    /** 状态(1正常3废止) */
    @Excel(name = "ui.data.column.info.status")
    @ApiModelProperty(value = "状态(1正常3废止)")
    private String status;

    /** MES系统创建时间 */
    @JsonFormat(pattern = "yyyy-MM-dd",timezone = "GMT+8")
    @Excel(name = "ui.data.column.info.mesCreateDate", width = 30, dateFormat = "yyyy-MM-dd")
    @ApiModelProperty(value = "MES系统创建时间")
    private Date mesCreateDate;

    /** MES更新时间 */
    @JsonFormat(pattern = "yyyy-MM-dd",timezone = "GMT+8")
    @Excel(name = "ui.data.column.info.mesUpdateDate", width = 30, dateFormat = "yyyy-MM-dd")
    @ApiModelProperty(value = "MES更新时间")
    private Date mesUpdateDate;

    /** 创建时间 */
    @JsonFormat(pattern = "yyyy-MM-dd",timezone = "GMT+8")
    @Excel(name = "ui.data.column.info.createDate", width = 30, dateFormat = "yyyy-MM-dd")
    @ApiModelProperty(value = "创建时间")
    private Date createDate;

    /** 更新时间 */
    @JsonFormat(pattern = "yyyy-MM-dd",timezone = "GMT+8")
    @Excel(name = "ui.data.column.info.updateDate", width = 30, dateFormat = "yyyy-MM-dd")
    @ApiModelProperty(value = "更新时间")
    private Date updateDate;

    @ApiModelProperty(value = "备注", position = 500)
    @TableField("REMARK")
    private String remark;

    /** 删除标识：0--正常，1-删除 */
    @Excel(name = "ui.data.column.info.isDelete")
    @ApiModelProperty(value = "删除标识：0--正常，1-删除")
    private Long isDelete;}
