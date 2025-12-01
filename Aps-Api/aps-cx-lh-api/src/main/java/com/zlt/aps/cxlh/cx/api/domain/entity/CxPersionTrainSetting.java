package com.zlt.aps.cxlh.cx.api.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.common.core.web.domain.BaseEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.Date;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：CxPersionTrainSetting.java
 * 描    述：成型工序开机档数对象 t_cx_persion_train_setting
 *@author zlt
 *@date 2025-02-17
 *@version 1.0
 *
 *  修改记录：
 *     修改时间：...
 *     修 改 人：zlt
 *     修改内容：...
 */

@ApiModel(value = "成型工序开机档数对象", description = "成型工序开机档数对象 ")
@Data
@TableName(value = "T_CX_PERSION_TRAIN_SETTING")
// @KeySequence(value = "SEQ_ERSION_TRAIN_SETTING")
public class CxPersionTrainSetting extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @TableId(value = "ID", type = IdType.AUTO)
    private Long id;

     /** 排程日期 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @ApiModelProperty(value = "排程日期", name = "scheduleDate")
    @TableField(value = "SCHEDULE_DATE")
    private Date scheduleDate;

    /** 成型法:来源于数据字典molding_method */
    @ApiModelProperty(value = "成型法:来源于数据字典molding_method", name = "mouldMethod")
    @TableField(value = "MOULD_METHOD")
    private Integer mouldMethod;

    /** 多个使用/分割，1班机台-定额 */
    @ApiModelProperty(value = "多个使用/分割，1班机台-定额", name = "quotaClass1")
    @TableField(value = "QUOTA_CLASS1")
    private String quotaClass1;

    /** 多个使用/分割，2班机台-定额 */
    @ApiModelProperty(value = "多个使用/分割，2班机台-定额", name = "quotaClass2")
    @TableField(value = "QUOTA_CLASS2")
    private String quotaClass2;

    /** 多个使用/分割，3班机台-定额 */
    @ApiModelProperty(value = "多个使用/分割，3班机台-定额", name = "quotaClass3")
    @TableField(value = "QUOTA_CLASS3")
    private String quotaClass3;

    /**
     * 开始时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @ApiModelProperty(value = "beginDate")
    @TableField(exist = false)
    private Date beginDate;
    
    /**
     * 结束时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @ApiModelProperty(value = "endDate")
    @TableField(exist = false)
    private Date endDate;

}