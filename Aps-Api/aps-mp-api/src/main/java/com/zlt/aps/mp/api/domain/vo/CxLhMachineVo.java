package com.zlt.aps.mp.api.domain.vo;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

/**
 * @author Chen
 * @date 2025/6/30
 */
@Data
public class CxLhMachineVo implements Serializable {

    /**
     * 年份
     */
    @ApiModelProperty(value = "年份", name = "year")
    private Integer year;

    /**
     * 月份
     */
    @ApiModelProperty(value = "月份", name = "month")
    private Integer month;

    /**
     * 设备类型
     */
    @ApiModelProperty(value = "设备类型", name = "machineType")
    private Integer machineType;

    /**
     * 展示名称
     */
    @ApiModelProperty(value = "展示名称", name = "machineTypeName")
    private String machineTypeName;

    /**
     * 可用机台数/维修总小时
     */
    @ApiModelProperty(value = "可用机台数/维修总小时", name = "qty")
    private Double qty;

}
