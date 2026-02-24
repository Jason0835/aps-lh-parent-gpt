package com.zlt.aps.monthplan.api.domain.dto;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

/**
 * @author Chen
 * @since 2025/8/4
 */
@Data
public class SystemRunReportDto implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 开始日期
     */
    @ApiModelProperty(value = "开始日期", name = "startDate")
    private String startDate;

    /**
     * 结束日期
     */
    @ApiModelProperty(value = "结束日期", name = "endDate")
    private String endDate;

    /**
     * 工序
     */
    @ApiModelProperty(value = "工序", name = "productProcess")
    private String productProcess;
}
