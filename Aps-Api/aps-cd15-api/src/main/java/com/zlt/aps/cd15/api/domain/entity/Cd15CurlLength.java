package com.zlt.aps.cd15.api.domain.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ruoyi.common.core.annotation.Excel;
import com.ruoyi.common.core.web.domain.BaseEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.math.BigDecimal;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：Cd15CurlLength.java
 * 描    述：钢丝斜裁卷曲长度对象 t_cd15_curl_length
 *@author zlt
 *@date 2025-03-11
 *@version 1.0
 *
 *  修改记录：
 *     修改时间：...
 *     修 改 人：zlt
 *     修改内容：...
 */
@ApiModel(value = "钢丝斜裁卷曲长度对象", description = "钢丝斜裁卷曲长度对象")
@Data
@TableName(value = "T_CD15_CURL_LENGTH")
public class Cd15CurlLength extends BaseEntity {

    private static final long serialVersionUID = 1L;

     /** 钢带代码 */
    @Excel(name = "ui.data.column.cd15CurlLength.steelStripCode")
    @ApiModelProperty(value = "钢带代码", name = "steelStripCode")
    @TableField(value = "STEEL_STRIP_CODE")
    private String steelStripCode;

    /** 卷曲长度 */
    @Excel(name = "ui.data.column.cd15CurlLength.curlLength")
    @ApiModelProperty(value = "卷曲长度", name = "curlLength")
    @TableField(value = "CURL_LENGTH")
    private BigDecimal curlLength;

    /**
     * 查询编号，用于精确查询
     */
    @ApiModelProperty(value = "查询编号，用于精确查询")
    @TableField(exist = false)
    private String queryCode;

}