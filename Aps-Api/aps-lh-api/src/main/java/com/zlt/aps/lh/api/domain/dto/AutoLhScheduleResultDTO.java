package com.zlt.aps.lh.api.domain.dto;

import com.baomidou.mybatisplus.annotation.TableField;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * @author xh
 * @version 1.0
 * @Description 硫化自动排程DTO类
 * @date 2025/2/19
 */
@Data
public class AutoLhScheduleResultDTO implements Serializable {

    private static final long serialVersionUID = 552208486450337750L;

    @ApiModelProperty(value = "排程时间")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private Date scheduleTime;

    @ApiModelProperty(value = "分厂编号")
    private String factoryCode;

    @ApiModelProperty(value = "月度计划版本号")
    private String monthPlanVersion;

}
