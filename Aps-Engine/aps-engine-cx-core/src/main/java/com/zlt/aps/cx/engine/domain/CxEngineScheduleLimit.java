package com.zlt.aps.cx.engine.domain;

import com.zlt.aps.common.core.domain.ApsBaseEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
  * 成型排产限制设置信息
  * @ClassName CxEngineScheduleLimit
  * @Description TODO
  * @Author Joran.Zhang
  * @Date 2021/6/25 14:26
  * @Version 1.0
**/
@Data
@ApiModel(value="CxScheduleLimit对象", description="成型排产限制表")
public class CxEngineScheduleLimit extends ApsBaseEntity {

    private  static final  long serialVersionUID=1L;

    @ApiModelProperty(value = "主键ID")
    private Long id;

    @ApiModelProperty(value = "成型机台机型类型，数据来源数据字典。如一次法：1；二次法：2；")
    private String machineType;

    @ApiModelProperty(value = "外胎规格尺寸信息")
    private Double specDimension;

    @ApiModelProperty(value = "胎胚平均库存在硫化班产数（下限）")
    private Double tireAvgLhStockMinimun;

    @ApiModelProperty(value = "胎胚平均库存在硫化班产数（上限）")
    private Double tireAveLhStockMaximun;

    @ApiModelProperty(value = "最大硫化班次")
    private Double maxLhClass;

    @ApiModelProperty(value = "成型机台编号")
    private String  machineCode;
}
