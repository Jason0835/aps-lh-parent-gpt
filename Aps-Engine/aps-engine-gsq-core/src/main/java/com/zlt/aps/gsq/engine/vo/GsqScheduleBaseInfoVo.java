package com.zlt.aps.gsq.engine.vo;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;

/**
 * 钢丝圈排程基础信息VO。
 *
 * <p>用于S1阶段加载施工表关联信息，包括BOM用量、关联胎圈、钢丝直径等。</p>
 *
 * @author APS
 */
@Data
public class GsqScheduleBaseInfoVo {

    @ApiModelProperty(value = "钢丝圈代码")
    private String steelRingCode;

    @ApiModelProperty(value = "钢丝类型")
    private String steelType;

    @ApiModelProperty(value = "物料编码")
    private String materialCode;

    @ApiModelProperty(value = "胎胚描述")
    private String embryoSpecDesc;

    @ApiModelProperty(value = "排列")
    private String rank;

    @ApiModelProperty(value = "单耗")
    private Double unitConsume;

    @ApiModelProperty(value = "寸口")
    private String dimension;

    @ApiModelProperty(value = "英寸")
    private String proSize;

    @ApiModelProperty(value = "BOM用量（每个胎圈需要的钢丝圈数量，默认1）")
    private Double bomQty;

    @ApiModelProperty(value = "钢丝直径（用于机台过滤）")
    private String wireDiameter;

    @ApiModelProperty(value = "关联胎圈代码列表（一个钢丝圈可能对应多个胎圈）")
    private List<String> tireRingCodeList;

    @ApiModelProperty(value = "关联胎胚代码列表（一个钢丝圈可能对应多个胎胚）")
    private List<String> embryoCodeList;

    /** 对应成型一班的钢丝圈胶计划量 */
    private Double cxClass1Plan;

    /** 对应成型二班的钢丝圈胶计划量 */
    private Double cxClass2Plan;

    /** 对应成型三班的钢丝圈胶计划量 */
    private Double cxClass3Plan;

    /** 对应成型次一班的钢丝圈胶计划量 */
    private Double cxClass4Plan;

    /** 对应成型次二班的钢丝圈胶计划量 */
    private Double cxClass5Plan;

    /** 机台code$胎胚代码，多个逗号分割， 用来计算成型平均定额使用 */
    private String quotaKeys;
}
