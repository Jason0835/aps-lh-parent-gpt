package com.zlt.aps.cx.api.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.common.core.annotation.Excel;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.Date;

/**
 * PLM施工参数对象 t_plm_construction_info
 * 
 * @author zlt
 * @date 2021-10-30
 */
@Data
@TableName("T_PLM_CONSTRUCTION_INFO")
@ApiModel(value = "PLM施工参数对象", description = "PLM施工参数对象 ")
@KeySequence(value = "SEQ_PLM_SCHEDULE", clazz = Long.class)
public class PlmConstructionInfo {

    private static final long serialVersionUID = 1L;

    /** 主键ID */
    @ApiModelProperty(value = "主键ID，对应自增序列为：SEQ_PUBLIC")
    @TableId(value = "ID", type = IdType.INPUT)
    private Long id;

    /** 品号 */
    @Excel(name = "ui.data.column.plm.sapCode")
    @ApiModelProperty(value = "品号")
    private String sapCode;

    /** 系列 */
    @Excel(name = "ui.data.column.plm.series")
    @ApiModelProperty(value = "系列")
    private String series;

    /** 代码 */
    @Excel(name = "ui.data.column.plm.code")
    @ApiModelProperty(value = "代码")
    private String code;

    /** 版本 */
    @Excel(name = "ui.data.column.plm.version")
    @ApiModelProperty(value = "版本")
    private String version;

    /** 参数代码 */
    @Excel(name = "ui.data.column.plm.paramCode")
    @ApiModelProperty(value = "参数代码")
    private String paramCode;

    /** 参数名称 */
    @Excel(name = "ui.data.column.plm.paramName")
    @ApiModelProperty(value = "参数名称")
    private String paramName;

    /** 参数值 */
    @Excel(name = "ui.data.column.plm.paramValue")
    @ApiModelProperty(value = "参数值")
    private String paramValue;

    /** 参数类型 */
    @Excel(name = "ui.data.column.plm.paramType")
    @ApiModelProperty(value = "参数类型")
    private String paramType;

    /** 创建时间 */
    @JsonFormat(pattern = "yyyy-MM-dd",timezone = "GMT+8")
    @ApiModelProperty(value = "创建时间")
    private Date createDate;

    /** 更新时间 */
    @JsonFormat(pattern = "yyyy-MM-dd",timezone = "GMT+8")
    @ApiModelProperty(value = "更新时间")
    private Date updateDate;

    @ApiModelProperty(value = "是否删除")
    private Integer isDelete;

    @TableField(select = false)
    @ApiModelProperty(value = "排序字段sql")
    private String orderStr;
}
