package com.zlt.aps.template.tm;

import java.math.BigDecimal;

import com.ruoyi.common.core.annotation.Excel;

import com.zlt.aps.common.core.domain.ApsBaseEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
@ApiModel(value = "胎面卷曲信息维护导入模板", description = "胎面卷曲信息维护导入模板")
public class TmCurlRollTemp extends ApsBaseEntity {

    @ApiModelProperty(value = "胎面代码")
    @Excel(name = "ui.data.column.quota.treadCode")
    private String treadCode;

    @ApiModelProperty(value = "卷曲长度。次胎面一卷的最大长度，单位：米。")
    @Excel(name = "ui.curlRoll.column.length")
    private BigDecimal curlLength;

    @ApiModelProperty(value = "备注", position = 500)
    @Excel(name = "ui.common.column.remark")
    private String remark;
}
