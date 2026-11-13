package com.zlt.aps.common.core.domain.vo;

import com.alibaba.fastjson.annotation.JSONField;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

/**
 * 压出自动排程异常明细对象。
 *
 * <p>用于向前端返回本次自动排程过程中收集到的错误或警告，
 * 不参与排程计算和数据库实体映射。</p>
 */
@Data
@ApiModel(value = "压出自动排程异常明细对象", description = "自动排程异常明细对象")
public class AutoScheduleIssueVo implements Serializable {

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

    /** 胎面/胎侧编码 */
    @ApiModelProperty(value = "胎面/胎侧编码", name = "processCode")
    @JSONField(alternateNames = {"treadCode", "sidewallCode"})
    private String processCode;

    /**
     * 发生问题的机台编码，自动排程问题为空时兼容现有数据。
     */
    @ApiModelProperty(value = "机台编码", name = "machineCode")
    private String machineCode;

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

    /**
     * 建议处理
     */
    @ApiModelProperty(value = "建议处理", name = "suggestion")
    private String suggestion;
}
