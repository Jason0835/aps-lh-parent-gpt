package com.zlt.aps.monthplan.api.domain.itf;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ruoyi.common.core.annotation.Excel;
import com.ruoyi.common.core.web.domain.BaseEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.Date;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：ItfInterfaceLog.java
 * 描    述：接口请求日志对象 t_itf_interface_log
 *
 * @author zlt
 * @version 1.0
 * <p>
 * 修改记录：
 * 修改时间：...
 * 修 改 人：zlt
 * 修改内容：...
 * @date 2025-04-10
 */

@ApiModel(value = "接口请求日志对象", description = "接口请求日志对象 ")
@Data
@TableName(value = "T_ITF_INTERFACE_LOG")
public class ItfInterfaceLog extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 请求发起时间（自动填充当前时间）
     */
    @Excel(name = "ui.data.column.itfInterfaceLog.requestTime")
    @ApiModelProperty(value = "请求发起时间", name = "requestTime")
    @TableField(value = "REQUEST_TIME")
    private Date requestTime;

    /**
     * 接口名称
     */
    @Excel(name = "ui.data.column.itfInterfaceLog.interfaceName")
    @ApiModelProperty(value = "接口名称", name = "interfaceName")
    @TableField(value = "INTERFACE_NAME")
    private String interfaceName;

    /**
     * 请求的完整URL（包含域名、路径、查询参数）
     */
    @Excel(name = "ui.data.column.itfInterfaceLog.requestUrl")
    @ApiModelProperty(value = "请求的完整URL", name = "requestUrl")
    @TableField(value = "REQUEST_URL")
    private String requestUrl;

    /**
     * 请求方法（GET/POST/PUT/DELETE等）
     */
    @Excel(name = "ui.data.column.itfInterfaceLog.requestMethod")
    @ApiModelProperty(value = "请求方法", name = "requestMethod")
    @TableField(value = "REQUEST_METHOD")
    private String requestMethod;

    /**
     * 请求头（JSON格式存储）
     */
    @Excel(name = "ui.data.column.itfInterfaceLog.requestHeaders")
    @ApiModelProperty(value = "请求头", name = "requestHeaders")
    @TableField(value = "REQUEST_HEADERS")
    private String requestHeaders;

    /**
     * 请求体（JSON/XML或原始文本）
     */
    @Excel(name = "ui.data.column.itfInterfaceLog.requestBody")
    @ApiModelProperty(value = "请求体", name = "requestBody")
    @TableField(value = "REQUEST_BODY")
    private String requestBody;

    /**
     * 响应接收时间（可为NULL）
     */
    @Excel(name = "ui.data.column.itfInterfaceLog.responseTime")
    @ApiModelProperty(value = "响应接收时间", name = "responseTime")
    @TableField(value = "RESPONSE_TIME")
    private Date responseTime;

    /**
     * 响应状态码（可为NULL，如200/404/500）
     */
    @Excel(name = "ui.data.column.itfInterfaceLog.responseStatusCode")
    @ApiModelProperty(value = "响应状态码", name = "responseStatusCode")
    @TableField(value = "RESPONSE_STATUS_CODE")
    private Integer responseStatusCode;

    /**
     * 响应头（JSON格式存储）
     */
    @Excel(name = "ui.data.column.itfInterfaceLog.responseHeaders")
    @ApiModelProperty(value = "响应头", name = "responseHeaders")
    @TableField(value = "RESPONSE_HEADERS")
    private String responseHeaders;

    /**
     * 响应体（JSON/XML或原始文本）
     */
    @Excel(name = "ui.data.column.itfInterfaceLog.responseBody")
    @ApiModelProperty(value = "响应体", name = "responseBody")
    @TableField(value = "RESPONSE_BODY")
    private String responseBody;

    /**
     * 请求耗时（毫秒，响应时间-请求时间的差值）
     */
    @Excel(name = "ui.data.column.itfInterfaceLog.requestDurationMs")
    @ApiModelProperty(value = "请求耗时", name = "requestDurationMs")
    @TableField(value = "REQUEST_DURATION_MS")
    private Long requestDurationMs;

    /**
     * 请求是否成功（1=成功，0=失败，默认0）
     */
    @Excel(name = "ui.data.column.itfInterfaceLog.isSuccess")
    @ApiModelProperty(value = "请求是否成功", name = "isSuccess")
    @TableField(value = "IS_SUCCESS")
    private Integer isSuccess;


}