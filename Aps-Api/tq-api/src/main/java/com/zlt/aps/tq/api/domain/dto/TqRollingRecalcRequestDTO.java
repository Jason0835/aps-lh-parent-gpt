package com.zlt.aps.tq.api.domain.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 胎圈自动滚动重算请求。
 *
 * <p>对齐胎面 TmRollingRecalcRequestDTO，指定工厂、排程日期和目标逻辑班次执行滚动重算。</p>
 *
 * @author APS
 */
@Data
@ApiModel(value = "胎圈自动滚动重算请求", description = "指定工厂、排程日期和目标逻辑班次执行滚动重算")
public class TqRollingRecalcRequestDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 工厂编号。 */
    @ApiModelProperty(value = "工厂编号", required = true)
    private String factoryCode;

    /** 排程日期。 */
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    @ApiModelProperty(value = "排程日期", required = true)
    private Date scheduleDate;

    /** MES库存物理日期；自动入口必传，人工入口可由班次配置解析。 */
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    @ApiModelProperty(value = "MES库存物理日期")
    private Date stockDate;

    /** 目标逻辑班次，取值一至六。 */
    @ApiModelProperty(value = "目标逻辑班次", required = true)
    private Integer targetShiftOrder;

    /**
     * 操作人由 BootUI、微服务控制器或定时任务覆盖，禁止直接信任外部请求值。
     */
    @ApiModelProperty(value = "操作人", hidden = true)
    private String operator;
}
