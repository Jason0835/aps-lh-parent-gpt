package com.zlt.aps.tm.api.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 胎面人工插单请求。
 *
 * <p>仅暴露人工插单允许编辑的业务字段，批次、工单、施工快照、发布状态和完成量等字段
 * 由胎面服务根据可信基础资料生成。</p>
 */
@Data
@ApiModel(value = "胎面人工插单请求")
public class TmInsertTaskRequestVo implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 工厂编码。 */
    @ApiModelProperty(value = "工厂编码", required = true)
    private String factoryCode;

    /** 排程日期。 */
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    @ApiModelProperty(value = "排程日期", required = true)
    private Date scheduleDate;

    /** 目标机台编码。 */
    @ApiModelProperty(value = "目标机台编码", required = true)
    private String machineCode;

    /** 胎面编码。 */
    @ApiModelProperty(value = "胎面编码", required = true)
    private String treadCode;

    /** 中班计划量，对应 class1。 */
    @ApiModelProperty(value = "中班计划量")
    private BigDecimal class1PlanQty;

    /** 中班顺序，对应 class1。 */
    @ApiModelProperty(value = "中班顺序")
    private Integer class1Sequence;

    /** 中班原因分析，对应 class1。 */
    @ApiModelProperty(value = "中班原因分析")
    private String class1Analysis;

    /** 夜班计划量，对应 class2。 */
    @ApiModelProperty(value = "夜班计划量")
    private BigDecimal class2PlanQty;

    /** 夜班顺序，对应 class2。 */
    @ApiModelProperty(value = "夜班顺序")
    private Integer class2Sequence;

    /** 夜班原因分析，对应 class2。 */
    @ApiModelProperty(value = "夜班原因分析")
    private String class2Analysis;

    /** 早班计划量，对应 class3。 */
    @ApiModelProperty(value = "早班计划量")
    private BigDecimal class3PlanQty;

    /** 早班顺序，对应 class3。 */
    @ApiModelProperty(value = "早班顺序")
    private Integer class3Sequence;

    /** 早班原因分析，对应 class3。 */
    @ApiModelProperty(value = "早班原因分析")
    private String class3Analysis;

    /** 中班计划量，对应 class4。 */
    @ApiModelProperty(value = "中班计划量（第二天）")
    private BigDecimal class4PlanQty;

    /** 中班顺序，对应 class4。 */
    @ApiModelProperty(value = "中班顺序（第二天）")
    private Integer class4Sequence;

    /** 中班原因分析，对应 class4。 */
    @ApiModelProperty(value = "中班原因分析（第二天）")
    private String class4Analysis;

    /** 夜班计划量，对应 class5。 */
    @ApiModelProperty(value = "夜班计划量（第二天）")
    private BigDecimal class5PlanQty;

    /** 夜班顺序，对应 class5。 */
    @ApiModelProperty(value = "夜班顺序（第二天）")
    private Integer class5Sequence;

    /** 夜班原因分析，对应 class5。 */
    @ApiModelProperty(value = "夜班原因分析（第二天）")
    private String class5Analysis;

    /** 早班计划量，对应 class6。 */
    @ApiModelProperty(value = "早班计划量（第二天）")
    private BigDecimal class6PlanQty;

    /** 早班顺序，对应 class6。 */
    @ApiModelProperty(value = "早班顺序（第二天）")
    private Integer class6Sequence;

    /** 早班原因分析，对应 class6。 */
    @ApiModelProperty(value = "早班原因分析（第二天）")
    private String class6Analysis;

    /** 插单备注。 */
    @ApiModelProperty(value = "备注")
    private String remark;
}
