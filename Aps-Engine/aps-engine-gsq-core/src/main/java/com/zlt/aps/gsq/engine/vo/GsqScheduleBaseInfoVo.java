package com.zlt.aps.gsq.engine.vo;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
public class GsqScheduleBaseInfoVo {

    @ApiModelProperty(value = "钢丝圈代码")
    private String steelRingCode;

    @ApiModelProperty(value = "钢丝类型")
    private String steelType;

    @ApiModelProperty(value = "排列")
    private String rank;

    @ApiModelProperty(value = "单耗")
    private Double unitConsume;

    /**
     * 对应成型一班的钢丝圈胶计划量
     */
    private Double cxClass1Plan;

    /**
     * 对应成型二班的钢丝圈胶计划量
     */
    private Double cxClass2Plan;

    /**
     * 对应成型三班的钢丝圈胶计划量
     */
    private Double cxClass3Plan;

    /**
     * 对应成型次一班的钢丝圈胶计划量
     */
    private Double cxClass4Plan;

    /**
     * 对应成型次二班的钢丝圈胶计划量
     */
    private Double cxClass5Plan;

    /**
     * 机台code$胎胚代码，多个逗号分割， 用来计算成型平均定额使用
     */
    private String quotaKeys;
}
