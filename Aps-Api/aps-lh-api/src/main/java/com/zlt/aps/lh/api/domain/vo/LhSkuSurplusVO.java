package com.zlt.aps.lh.api.domain.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

/**
 * 硫化余量计算结果 VO。
 *
 * <p>由排程算法中的硫化余量计算口径导出，供日排产进度报表等只读场景复用，
 * 保证报表取值与排程落库值同源。</p>
 *
 * @author APS
 */
@Data
@ApiModel("硫化余量计算结果")
public class LhSkuSurplusVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 硫化余量：Max(月度计划总量 - 已完成量 - 有效上月超欠产量, 0)
     */
    @ApiModelProperty(value = "硫化余量", name = "surplusQty")
    private Integer surplusQty;

    /**
     * 合格产量（硫化余量计算使用的已完成量 = 月累计完成量 + T日班次完成量）
     */
    @ApiModelProperty(value = "合格产量（已完成量）", name = "finishedQty")
    private Integer finishedQty;

    /**
     * 硫化月计划总量（断点口径）
     */
    @ApiModelProperty(value = "硫化月计划总量", name = "monthPlanTotal")
    private Integer monthPlanTotal;

    /**
     * 欠产余量标识：true-存在欠产余量
     */
    @ApiModelProperty(value = "欠产余量标识", name = "overdueSurplusFlag")
    private Boolean overdueSurplusFlag;
}
