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
 * <p>用于承载自动排程入口的兼容契约参数，只描述调用条件和用户确认状态，
 * 不承载排程算法，不直接修改任务链。</p>
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

    /** 触发来源，例如 AUTO、MANUAL_REBUILD */
    @ApiModelProperty(value = "触发来源", name = "dataSource")
    private String dataSource;

    /** 外部追踪标识，不传时由服务端生成 */
    @ApiModelProperty(value = "外部追踪标识", name = "traceId")
    private String traceId;

    /** 是否已确认覆盖全部未发布的旧批次 */
    @ApiModelProperty(value = "是否已确认覆盖全部未发布的旧批次", name = "confirmOverwrite")
    private Boolean confirmOverwrite;

    /** 提交任务时的界面语言，支持 zh_CN、en_US、vi_VN */
    @ApiModelProperty(value = "提交任务时的界面语言", name = "language")
    private String language;
}
