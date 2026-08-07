package com.zlt.aps.tq.api.domain.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 胎圈自动滚动窗口检查请求。
 *
 * <p>对齐胎面 TmRollingCheckRequestDTO，承载平台任务触发的班次窗口检查参数。</p>
 *
 * @author APS
 */
@Data
@ApiModel(value = "胎圈自动滚动窗口检查请求", description = "平台任务触发的班次窗口检查参数")
public class TqRollingCheckRequestDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 指定工厂，为空时检查全部配置工厂。 */
    @ApiModelProperty(value = "指定工厂")
    private String factoryCode;

    /** 触发时间。 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @ApiModelProperty(value = "触发时间")
    private Date triggerTime;
}
