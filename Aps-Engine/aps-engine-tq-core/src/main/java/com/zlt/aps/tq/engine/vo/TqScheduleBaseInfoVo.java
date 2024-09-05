package com.zlt.aps.tq.engine.vo;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
public class TqScheduleBaseInfoVo {

    /**
     * 对应的成型批次号
     */
    private String cxBatchNo;

    @ApiModelProperty(value = "胎圈代码")
    private String beadCode;

    @ApiModelProperty(value = "钢丝圈代码")
    private String steelRingCode;

    @ApiModelProperty(value = "三角胶代码")
    private String triangleGlueCode;

    @ApiModelProperty(value = "胶料代码")
    private String glueCode;

    @ApiModelProperty(value = "口型板代码")
    private String mouthPlateCode;

    @ApiModelProperty(value = "尺寸")
    private String specSize;

    @ApiModelProperty(value = "单耗")
    private Double unitConsume;

    /**
     * 对应成型一班的胎圈计划量
     */
    private Double cxClass1Plan;

    /**
     * 对应成型二班的胎圈计划量
     */
    private Double cxClass2Plan;

    /**
     * 对应成型三班的胎圈计划量
     */
    private Double cxClass3Plan;

    /**
     * 对应成型次一班的胎圈计划量
     */
    private Double cxClass4Plan;

    /**
     * 对应成型次二班的胎圈计划量
     */
    private Double cxClass5Plan;

    /**
     * 机台code$胎胚代码，多个逗号分割， 用来计算成型平均定额使用
     */
    private String quotaKeys;
}
