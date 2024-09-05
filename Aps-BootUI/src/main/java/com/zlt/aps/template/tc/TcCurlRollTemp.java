package com.zlt.aps.template.tc;

import java.math.BigDecimal;

import com.ruoyi.common.core.annotation.Excel;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
@ApiModel(value = "胎侧卷曲信息维护导入模板", description = "胎侧卷曲信息维护导入模板")
public class TcCurlRollTemp {

    @ApiModelProperty(value = "胎侧代码")
    @Excel(name = "ui.data.column.quota.sidewallCode")
    private String sidewallCode;

    @ApiModelProperty(value = "卷曲长度。次胎侧一卷的最大长度，单位：米。")
    @Excel(name = "ui.curlRoll.column.length")
    private BigDecimal curlLength;

    @ApiModelProperty(value = "备注", position = 500)
    @Excel(name = "ui.common.column.remark")
    private String remark;
}
