    package com.zlt.aps.common.engine.domain;

import com.zlt.aps.common.core.domain.ApsBaseEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
  * 成型工序定额对象
  * @ClassName CxEngineQuotaSetting
  * @Description TODO
  * @Author Joran.Zhang
  * @Date 2021/6/29 19:36
  * @Version 1.0
**/
@Data
@ApiModel(
        value = "CxEngineQuotaSetting",
        description = "成型定额设定信息"
)
public class CxEngineQuotaSetting extends ApsBaseEntity {

    @ApiModelProperty(
            value = "成型机台机型类别"
    )
    private String type;

    @ApiModelProperty(
            value = "外胎规格尺寸信息"
    )
    private Double specDimension;

    @ApiModelProperty(
            value = "胎体布层数"
    )
    private Integer carcassBothLayer;

    @ApiModelProperty(
            value = "是否补强"
    )
    private String reinforce;
    @ApiModelProperty(
            value = "轮胎类型"
    )
    private String tireType;
    @ApiModelProperty(
            value = "断面宽(下限)"
    )
    private Integer sectionWidthMinimum;
    @ApiModelProperty(
            value = "断面宽(上限)"
    )
    private Integer sectionWidthMaximum;
    @ApiModelProperty(
            value = "两人定额"
    )
    private Integer twoPersonQuota;
    @ApiModelProperty(
            value = "单人折合定额"
    )
    private Integer onePersonQuota;

    /**
     * 根据成型机操作人员数确定最终定额
     */
    @ApiModelProperty(
            value = "最终定额"
    )
    private Integer finalQuota;

    /**
     * 成型机台编号
     */
    @ApiModelProperty(
            value = "成型机台编号"
    )
    private String cxMachineCode;

    /**
     * 成型机台系数
     */
    private Double quotaRatio;
}
