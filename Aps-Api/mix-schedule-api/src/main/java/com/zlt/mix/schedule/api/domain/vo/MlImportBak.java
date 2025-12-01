package com.zlt.mix.schedule.api.domain.vo;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.common.core.annotation.Excel;
import com.ruoyi.common.core.web.domain.BaseEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.Date;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：MlImportBak.java
 * 描    述：密炼线下计划操作功能对象 ml_import_bak
 *
 * @author zlt
 * @version 1.0
 * <p>
 * 修改记录：
 * 修改时间：...
 * 修 改 人：zlt
 * 修改内容：...
 * @date 2025-09-05
 */
@ApiModel(value = "密炼线下计划操作功能对象", description = "密炼线下计划操作功能对象 ")
@Data
@TableName(value = "ML_IMPORT_BAK")
public class MlImportBak extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @TableField(exist = false)
    private Long id;

    /**
     * 排程日期
     */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "ui.data.column.MlImportBak.rq", width = 30, dateFormat = "yyyy-MM-dd")
    @ApiModelProperty(value = "排程日期", name = "rq")
    @TableField(value = "RQ")
    private Date rq;

    /**
     *
     */
    @Excel(name = "ui.data.column.MlImportBak.clazz")
    @ApiModelProperty(value = "", name = "clazz")
    @TableField(value = "CLAZZ")
    private String clazz;

    /**
     *
     */
    @Excel(name = "ui.data.column.MlImportBak.m101")
    @ApiModelProperty(value = "", name = "m101")
    @TableField(value = "M101")
    private Integer m101;

    /**
     *
     */
    @Excel(name = "ui.data.column.MlImportBak.m102")
    @ApiModelProperty(value = "", name = "m102")
    @TableField(value = "M102")
    private String m102;

    /**
     *
     */
    @Excel(name = "ui.data.column.MlImportBak.m103")
    @ApiModelProperty(value = "", name = "m103")
    @TableField(value = "M103")
    private String m103;

    /**
     *
     */
    @Excel(name = "ui.data.column.MlImportBak.m104")
    @ApiModelProperty(value = "", name = "m104")
    @TableField(value = "M104")
    private Integer m104;

    /**
     *
     */
    @Excel(name = "ui.data.column.MlImportBak.m105")
    @ApiModelProperty(value = "", name = "m105")
    @TableField(value = "M105")
    private String m105;

    /**
     *
     */
    @Excel(name = "ui.data.column.MlImportBak.m106")
    @ApiModelProperty(value = "", name = "m106")
    @TableField(value = "M106")
    private String m106;

    /**
     *
     */
    @Excel(name = "ui.data.column.MlImportBak.m201")
    @ApiModelProperty(value = "", name = "m201")
    @TableField(value = "M201")
    private Integer m201;

    /**
     *
     */
    @Excel(name = "ui.data.column.MlImportBak.m202")
    @ApiModelProperty(value = "", name = "m202")
    @TableField(value = "M202")
    private String m202;

    /**
     *
     */
    @Excel(name = "ui.data.column.MlImportBak.m203")
    @ApiModelProperty(value = "", name = "m203")
    @TableField(value = "M203")
    private String m203;

    /**
     *
     */
    @Excel(name = "ui.data.column.MlImportBak.m204")
    @ApiModelProperty(value = "", name = "m204")
    @TableField(value = "M204")
    private Integer m204;

    /**
     *
     */
    @Excel(name = "ui.data.column.MlImportBak.m205")
    @ApiModelProperty(value = "", name = "m205")
    @TableField(value = "M205")
    private String m205;

    /**
     *
     */
    @Excel(name = "ui.data.column.MlImportBak.m206")
    @ApiModelProperty(value = "", name = "m206")
    @TableField(value = "M206")
    private String m206;

    /**
     *
     */
    @Excel(name = "ui.data.column.MlImportBak.m301")
    @ApiModelProperty(value = "", name = "m301")
    @TableField(value = "M301")
    private Integer m301;

    /**
     *
     */
    @Excel(name = "ui.data.column.MlImportBak.m302")
    @ApiModelProperty(value = "", name = "m302")
    @TableField(value = "M302")
    private String m302;

    /**
     *
     */
    @Excel(name = "ui.data.column.MlImportBak.m303")
    @ApiModelProperty(value = "", name = "m303")
    @TableField(value = "M303")
    private String m303;

    /**
     *
     */
    @Excel(name = "ui.data.column.MlImportBak.m304")
    @ApiModelProperty(value = "", name = "m304")
    @TableField(value = "M304")
    private Integer m304;

    /**
     *
     */
    @Excel(name = "ui.data.column.MlImportBak.m305")
    @ApiModelProperty(value = "", name = "m305")
    @TableField(value = "M305")
    private String m305;

    /**
     *
     */
    @Excel(name = "ui.data.column.MlImportBak.m306")
    @ApiModelProperty(value = "", name = "m306")
    @TableField(value = "M306")
    private String m306;

    /**
     *
     */
    @Excel(name = "ui.data.column.MlImportBak.m401")
    @ApiModelProperty(value = "", name = "m401")
    @TableField(value = "M401")
    private Integer m401;

    /**
     *
     */
    @Excel(name = "ui.data.column.MlImportBak.m402")
    @ApiModelProperty(value = "", name = "m402")
    @TableField(value = "M402")
    private String m402;

    /**
     *
     */
    @Excel(name = "ui.data.column.MlImportBak.m403")
    @ApiModelProperty(value = "", name = "m403")
    @TableField(value = "M403")
    private String m403;

    /**
     *
     */
    @Excel(name = "ui.data.column.MlImportBak.m404")
    @ApiModelProperty(value = "", name = "m404")
    @TableField(value = "M404")
    private Integer m404;

    /**
     *
     */
    @Excel(name = "ui.data.column.MlImportBak.m405")
    @ApiModelProperty(value = "", name = "m405")
    @TableField(value = "M405")
    private String m405;

    /**
     *
     */
    @Excel(name = "ui.data.column.MlImportBak.m406")
    @ApiModelProperty(value = "", name = "m406")
    @TableField(value = "M406")
    private String m406;

    /**
     *
     */
    @Excel(name = "ui.data.column.MlImportBak.m501")
    @ApiModelProperty(value = "", name = "m501")
    @TableField(value = "M501")
    private Integer m501;

    /**
     *
     */
    @Excel(name = "ui.data.column.MlImportBak.m502")
    @ApiModelProperty(value = "", name = "m502")
    @TableField(value = "M502")
    private String m502;

    /**
     *
     */
    @Excel(name = "ui.data.column.MlImportBak.m503")
    @ApiModelProperty(value = "", name = "m503")
    @TableField(value = "M503")
    private String m503;

    /**
     *
     */
    @Excel(name = "ui.data.column.MlImportBak.m504")
    @ApiModelProperty(value = "", name = "m504")
    @TableField(value = "M504")
    private Integer m504;

    /**
     *
     */
    @Excel(name = "ui.data.column.MlImportBak.m505")
    @ApiModelProperty(value = "", name = "m505")
    @TableField(value = "M505")
    private String m505;

    /**
     *
     */
    @Excel(name = "ui.data.column.MlImportBak.m506")
    @ApiModelProperty(value = "", name = "m506")
    @TableField(value = "M506")
    private String m506;

    /**
     *
     */
    @Excel(name = "ui.data.column.MlImportBak.m601")
    @ApiModelProperty(value = "", name = "m601")
    @TableField(value = "M601")
    private Integer m601;

    /**
     *
     */
    @Excel(name = "ui.data.column.MlImportBak.m602")
    @ApiModelProperty(value = "", name = "m602")
    @TableField(value = "M602")
    private String m602;

    /**
     *
     */
    @Excel(name = "ui.data.column.MlImportBak.m603")
    @ApiModelProperty(value = "", name = "m603")
    @TableField(value = "M603")
    private String m603;

    /**
     *
     */
    @Excel(name = "ui.data.column.MlImportBak.m604")
    @ApiModelProperty(value = "", name = "m604")
    @TableField(value = "M604")
    private Integer m604;

    /**
     *
     */
    @Excel(name = "ui.data.column.MlImportBak.m605")
    @ApiModelProperty(value = "", name = "m605")
    @TableField(value = "M605")
    private String m605;

    /**
     *
     */
    @Excel(name = "ui.data.column.MlImportBak.m606")
    @ApiModelProperty(value = "", name = "m606")
    @TableField(value = "M606")
    private String m606;


    @ApiModelProperty("创建者")
    @TableField(exist = false)
    private String createBy;

    @ApiModelProperty("创建时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField(exist = false)
    private Date createTime;

    @ApiModelProperty("更新者")
    @TableField(exist = false)
    private String updateBy;

    @ApiModelProperty("更新时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField(exist = false)
    private Date updateTime;

    @ApiModelProperty("备注")
    @TableField(exist = false)
    private String remark;

    @ApiModelProperty("删除标识：0--正常，1-删除")
    @TableField(exist = false)
    private Integer isDelete = 0;
}
