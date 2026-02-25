package com.zlt.aps.mp.api.domain.vo;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * @author Chen
 * @date 2025/6/28
 */
@Data
public class HomePage4ProductProcessesVo implements Serializable {

    /**
     * 工序缩写
     */
    @ApiModelProperty(value = "工序缩写", name = "productProcess")
    private String productProcess;

    /**
     * 工序名
     */
    @ApiModelProperty(value = "工序名", name = "productProcessName")
    private String productProcessName;

    /**
     * 排程日期
     */
    @ApiModelProperty(value = "排程日期", name = "scheduleDate")
    private Date scheduleDate;

    /**
     * 计划量
     */
    @ApiModelProperty(value = "计划量", name = "planQty")
    private Double planQty = 0D;

    /**
     * 完成量
     */
    @ApiModelProperty(value = "完成量", name = "finishQty")
    private Double finishQty = 0D;

    /**
     * 完成率
     */
    @ApiModelProperty(value = "完成率", name = "finishRate")
    private Double finishRate = 0D;

}
