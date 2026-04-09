package com.zlt.aps.cx.api.domain.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.time.LocalDate;

/**
 * 排程生成请求VO
 *
 * @author APS Team
 */
@Data
@ApiModel(value = "排程生成请求对象")
public class ScheduleGenerateVo {

    @ApiModelProperty(value = "排程日期", required = true)
    private LocalDate scheduleDate;

    @ApiModelProperty(value = "天数", required = true)
    private Integer days;

    @ApiModelProperty(value = "是否覆盖已有排程")
    private Boolean overwrite;

    @ApiModelProperty(value = "工厂编码")
    private String factoryCode;

    @ApiModelProperty(value = "排程类型")
    private String scheduleType;

    @ApiModelProperty(value = "排程模式")
    private String scheduleMode;
}
