package com.zlt.aps.xwyy.api.domain.vo;

import com.ruoyi.common.core.web.domain.BaseEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 压延排程结果导出线下模板对象
 *
 * @author Chen
 * @date 2025/7/22
 */
@Data
@ApiModel(value = "压延排程结果导出线下模板对象", description = "压延排程结果导出线下模板对象")
public class HalfYyExportDataVo extends BaseEntity {

    /**
     * 压延工序编码
     */
    @ApiModelProperty(value = "压延工序编码")
    private String code;

    /**
     * 用量
     */
    @ApiModelProperty(value = "用量")
    private Double cxPlanQty;

    /**
     * 数量-excel公式计算
     */
    @ApiModelProperty(value = "数量")
    private Double qty;

    /**
     * 库存
     */
    @ApiModelProperty(value = "库存")
    private Double stockQty;

    /**
     * 原线长度
     */
    @ApiModelProperty(value = "原线长度")
    private Double oriLineLength;

    /**
     * 生产日期-excel公式计算
     */
    @ApiModelProperty(value = "生产日期-excel公式计算")
    private Double scrq;

    /**
     * 顺序
     */
    @ApiModelProperty(value = "顺序")
    private Integer sort;

    /**
     * 排产日 0-对应排程日期前一天
     */
    @ApiModelProperty(value = "排产日0-对应排程日期前一天")
    private Double day0;

    /**
     * 排产日 1-对应排程日期当天，后续日期依次类推
     */
    @ApiModelProperty(value = "排产日1-对应排程日期当天，后续日期依次类推")
    private Double day1;

    /**
     * 排产日 2
     */
    @ApiModelProperty(value = "排产日2")
    private Double day2;

    /**
     * 排产日 3
     */
    @ApiModelProperty(value = "排产日3")
    private Double day3;

    /**
     * 排产日 4
     */
    @ApiModelProperty(value = "排产日4")
    private Double day4;

    /**
     * 排产日 5
     */
    @ApiModelProperty(value = "排产日5")
    private Double day5;

    /**
     * 排产日 6
     */
    @ApiModelProperty(value = "排产日6")
    private Double day6;

    /**
     * 排产日 7
     */
    @ApiModelProperty(value = "排产日7")
    private Double day7;

    /**
     * 排产日 8
     */
    @ApiModelProperty(value = "排产日8")
    private Double day8;

    /**
     * 排产日 9
     */
    @ApiModelProperty(value = "排产日9")
    private Double day9;

    /**
     * 排产日 10
     */
    @ApiModelProperty(value = "排产日10")
    private Double day10;

    /**
     * 排产日 11
     */
    @ApiModelProperty(value = "排产日11")
    private Double day11;

    /**
     * 排产日 12
     */
    @ApiModelProperty(value = "排产日12")
    private Double day12;

    /**
     * 排产日 13
     */
    @ApiModelProperty(value = "排产日13")
    private Double day13;

    /**
     * 排产日 14
     */
    @ApiModelProperty(value = "排产日14")
    private Double day14;

    /**
     * 排产日 15
     */
    @ApiModelProperty(value = "排产日15")
    private Double day15;

    /**
     * 排程日期，导入使用
     */
    @ApiModelProperty(value = "排程日期，导入使用")
    private Date scheduleDate;

    /**
     * 卷长
     */
    @ApiModelProperty(value = "卷长")
    private BigDecimal standSize;
}
