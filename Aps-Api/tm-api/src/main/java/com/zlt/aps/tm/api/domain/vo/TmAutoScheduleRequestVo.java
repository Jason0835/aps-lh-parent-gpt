package com.zlt.aps.tm.api.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 胎面自动排程请求对象。
 *
 * <p>用于承载前端或后续服务调用自动排程入口时传入的基础条件。该对象只作为契约层参数，
 * 不承载排程算法，不修改任务链。</p>
 */
@Data
@ApiModel(value = "胎面自动排程请求对象", description = "胎面自动排程请求对象")
public class TmAutoScheduleRequestVo implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 工厂编号 */
    @ApiModelProperty(value = "工厂编号", name = "factoryCode")
    private String factoryCode;

    /** 排程日期 */
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    @ApiModelProperty(value = "排程日期", name = "scheduleDate")
    private Date scheduleDate;

    /** 操作人 */
    @ApiModelProperty(value = "操作人", name = "operator")
    private String operator;

    /** 触发来源，例如AUTO、MANUAL_REBUILD */
    @ApiModelProperty(value = "触发来源", name = "dataSource")
    private String dataSource;

    /** 外部追踪标识，不传时由引擎入口生成 */
    @ApiModelProperty(value = "外部追踪标识", name = "traceId")
    private String traceId;
}
