package com.zlt.aps.cx.engine.domain;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
  *  获取成型工序定点机台相关信息
  * @ClassName CxEngineSpecifyMachine
  * @Description TODO
  * @Author Joran.Zhang
  * @Date 2021/6/24 16:38
  * @Version 1.0
**/
@Data
@ApiModel(
        value = "成型定点机台对象",
        description = "成型定点机台信息"
)
public class CxEngineSpecifyMachine {

    /**
     * SAP品号
     */
    @ApiModelProperty(value = "SAP品号")
    private String sapCode;

    /**
     * 胎胚代码
     */
    @ApiModelProperty(value = "胎胚代码")
    private String embryoCode;

    /**
     * 作业类型，数据维护在数据字典：0-限制作业；1-不可作业
     */
    @ApiModelProperty(value = "作业类型")
    private String jobType;

    /**
     * 成型机台编号
     */
    @ApiModelProperty(value = "成型机台编号")
    private String cxMachineCode;

    /**
     * 成型机台名称
     */
    @ApiModelProperty(value = "成型机台名称")
    private String cxMachineName;
}
