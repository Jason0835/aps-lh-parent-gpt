package com.zlt.aps.tm.api.domain.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

/**
 * 胎面自动排程异常明细对象。
 *
 * <p>用于向前端返回本次自动排程过程中收集到的错误或警告，
 * 不参与排程计算和数据库实体映射。</p>
 */
@Data
@ApiModel(value = "胎面自动排程异常明细对象", description = "胎面自动排程异常明细对象")
public class TmAutoScheduleIssueVo implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 异常级别，ERROR 表示阻断执行，WARN 表示可继续执行 */
    @ApiModelProperty(value = "异常级别", name = "level")
    private String level;

    /** 阶段编码 */
    @ApiModelProperty(value = "阶段编码", name = "stageCode")
    private String stageCode;

    /** 阶段名称 */
    @ApiModelProperty(value = "阶段名称", name = "stageName")
    private String stageName;

    /** 异常类别 */
    @ApiModelProperty(value = "异常类别", name = "category")
    private String category;

    /** 来源工单号 */
    @ApiModelProperty(value = "来源工单号", name = "sourceOrderNo")
    private String sourceOrderNo;

    /** 胎胚代码 */
    @ApiModelProperty(value = "胎胚代码", name = "embryoCode")
    private String embryoCode;

    /** 示方书编号 */
    @ApiModelProperty(value = "示方书编号", name = "recipeNo")
    private String recipeNo;

    /** 班次顺序 */
    @ApiModelProperty(value = "班次顺序", name = "shiftOrder")
    private Integer shiftOrder;

    /** 字段名称 */
    @ApiModelProperty(value = "字段名称", name = "fieldName")
    private String fieldName;

    /** 异常说明 */
    @ApiModelProperty(value = "异常说明", name = "message")
    private String message;
}