package com.zlt.aps.tq.api.domain.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

/**
 * 胎圈排程转机台DTO
 *
 * @author APS
 */
@Data
@ApiModel(value = "胎圈排程转机台DTO", description = "胎圈排程转机台请求参数")
public class TqChangeMachineDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 排程记录ID */
    @ApiModelProperty(value = "排程记录ID", name = "id")
    private Long id;

    /** 排程日期（格式：yyyy-MM-dd） */
    @ApiModelProperty(value = "排程日期", name = "scheduleDate")
    private String scheduleDate;

    /** 原机台编号 */
    @ApiModelProperty(value = "原机台编号", name = "oldMachineCode")
    private String oldMachineCode;

    /** 新机台编号 */
    @ApiModelProperty(value = "新机台编号", name = "newMachineCode")
    private String newMachineCode;
}
