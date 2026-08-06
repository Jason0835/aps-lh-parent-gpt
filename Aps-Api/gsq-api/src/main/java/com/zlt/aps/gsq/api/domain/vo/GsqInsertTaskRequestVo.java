package com.zlt.aps.gsq.api.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 钢丝圈人工插单请求。
 *
 * <p>支持锚点插入：当 anchorTaskId 不为空时，在锚点任务之后插入；
 * 否则按 class1Sequence 等顺序字段插入，顺序为空时追加链尾。</p>
 */
@Data
@ApiModel(value = "钢丝圈人工插单请求")
public class GsqInsertTaskRequestVo implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "工厂编码", required = true)
    private String factoryCode;

    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    @ApiModelProperty(value = "排程日期", required = true)
    private Date scheduleDate;

    @ApiModelProperty(value = "目标机台编码", required = true)
    private String machineCode;

    @ApiModelProperty(value = "钢丝圈编码", required = true)
    private String steelRingCode;

    @ApiModelProperty(value = "英寸尺寸")
    private String proSize;

    @ApiModelProperty(value = "1班计划量")
    private Integer class1PlanQty;
    @ApiModelProperty(value = "1班顺序")
    private Integer class1Sequence;
    @ApiModelProperty(value = "1班原因分析")
    private String class1Analysis;

    @ApiModelProperty(value = "2班计划量")
    private Integer class2PlanQty;
    @ApiModelProperty(value = "2班顺序")
    private Integer class2Sequence;
    @ApiModelProperty(value = "2班原因分析")
    private String class2Analysis;

    @ApiModelProperty(value = "3班计划量")
    private Integer class3PlanQty;
    @ApiModelProperty(value = "3班顺序")
    private Integer class3Sequence;
    @ApiModelProperty(value = "3班原因分析")
    private String class3Analysis;

    @ApiModelProperty(value = "4班计划量")
    private Integer class4PlanQty;
    @ApiModelProperty(value = "4班顺序")
    private Integer class4Sequence;
    @ApiModelProperty(value = "4班原因分析")
    private String class4Analysis;

    @ApiModelProperty(value = "5班计划量")
    private Integer class5PlanQty;
    @ApiModelProperty(value = "5班顺序")
    private Integer class5Sequence;
    @ApiModelProperty(value = "5班原因分析")
    private String class5Analysis;

    @ApiModelProperty(value = "6班计划量")
    private Integer class6PlanQty;
    @ApiModelProperty(value = "6班顺序")
    private Integer class6Sequence;
    @ApiModelProperty(value = "6班原因分析")
    private String class6Analysis;

    @ApiModelProperty(value = "锚点任务ID（在锚点之后插入，为空时追加链尾）")
    private String anchorTaskId;

    @ApiModelProperty(value = "备注")
    private String remark;
}
