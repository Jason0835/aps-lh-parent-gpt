package com.zlt.aps.tq.api.domain.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.Date;

/**
 * 胎圈排程跨班次推迟请求DTO
 *
 * @author APS
 */
@Data
@ApiModel(value = "胎圈排程跨班次推迟请求", description = "跨班次推迟请求参数")
public class TqPostponeRequestDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 排程日期 */
    @NotNull(message = "排程日期不能为空")
    @ApiModelProperty(value = "排程日期", name = "scheduleDate", required = true)
    private Date scheduleDate;

    /** 机台编号 */
    @NotNull(message = "机台编号不能为空")
    @ApiModelProperty(value = "机台编号", name = "machineCode", required = true)
    private String machineCode;

    /** 源班次索引（1~6） */
    @NotNull(message = "源班次索引不能为空")
    @ApiModelProperty(value = "源班次索引（1~6）", name = "sourceShiftIndex", required = true)
    private Integer sourceShiftIndex;

    /** 胎圈代码（可选，为空则推迟该班次所有未完成任务） */
    @ApiModelProperty(value = "胎圈代码（为空则推迟该班次所有未完成任务）", name = "beadCode")
    private String beadCode;

    /** 排程记录ID（可选，指定具体推迟哪条记录） */
    @ApiModelProperty(value = "排程记录ID（指定具体推迟哪条记录）", name = "scheduleId")
    private Long scheduleId;

    /** 是否部分推迟（true-仅推迟未完成部分；false-整体推迟） */
    @ApiModelProperty(value = "是否部分推迟（true-仅推迟未完成部分；false-整体推迟）", name = "partialPostpone")
    private Boolean partialPostpone;
}
