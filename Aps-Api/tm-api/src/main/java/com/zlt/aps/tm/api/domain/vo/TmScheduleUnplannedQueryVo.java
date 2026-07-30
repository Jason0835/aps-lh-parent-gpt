package com.zlt.aps.tm.api.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 胎面未排任务查询条件。
 */
@Data
@ApiModel(value = "胎面未排任务查询条件")
public class TmScheduleUnplannedQueryVo implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 工厂编码。 */
    @ApiModelProperty(value = "工厂编码", required = true)
    private String factoryCode;

    /** 排程日期。 */
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    @ApiModelProperty(value = "排程日期", required = true)
    private Date scheduleDate;

    /** 自动排程批次号；为空时查询当前有效范围。 */
    @ApiModelProperty(value = "自动排程批次号")
    private String batchNo;

    /** 胎面编码，支持模糊查询。 */
    @ApiModelProperty(value = "胎面编码")
    private String treadCode;

    /** 当前页码。 */
    @ApiModelProperty(value = "当前页码", example = "1")
    private Integer pageNum = 1;

    /** 每页条数。 */
    @ApiModelProperty(value = "每页条数", example = "20")
    private Integer pageSize = 20;
}
