package com.zlt.aps.cd90.api.domain.entity;

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
 * 文件名称：Cd90CurlLength.java
 * 描    述：纤维直裁卷曲长度对象 t_cd90_curl_length
 *@author zlt
 *@date 2025-03-11
 *@version 1.0
 *
 *  修改记录：
 *     修改时间：...
 *     修 改 人：zlt
 *     修改内容：...
 */
@ApiModel(value = "纤维直裁卷曲长度对象", description = "纤维直裁卷曲长度对象")
@Data
@TableName(value = "T_CD90_CURL_LENGTH")
public class Cd90CurlLength extends BaseEntity {

    private static final long serialVersionUID = 1L;

     /** 帘布代码 */
    @Excel(name = "ui.data.column.cd90CurlLength.clothCode")
    @ApiModelProperty(value = "帘布代码", name = "clothCode")
    @TableField(value = "CLOTH_CODE")
    private String clothCode;

    /** 卷曲长度 */
    @Excel(name = "ui.data.column.cd90CurlLength.curlLength")
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