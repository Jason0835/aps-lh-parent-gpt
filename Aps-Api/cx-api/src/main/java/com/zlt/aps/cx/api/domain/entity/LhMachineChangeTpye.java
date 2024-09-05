package com.zlt.aps.cx.api.domain.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.Date;

@Data
@ApiModel(value = "成型-硫化机台变换类型", description = "成型-硫化机台变换类型 ")
public class LhMachineChangeTpye {

    private String machineName; //机台名称
    private String machineCode; //机台编号
    private String changeType; //变更类型

    @ApiModelProperty(value = "胎胚库存")
    private Integer embryoStock;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm", timezone = "GMT+8")
    @ApiModelProperty(value = "换模时间")
    private Date changeMoldTime;

    private int molds=0;   //模具数量

}
