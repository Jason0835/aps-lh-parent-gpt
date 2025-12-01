package com.zlt.aps.template.nc;

import java.math.BigDecimal;

import com.ruoyi.common.core.annotation.Excel;

import com.ruoyi.common.core.web.domain.BaseEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
@ApiModel(value = "内衬卷曲信息维护导入模板", description = "内衬卷曲信息维护导入模板")
public class NcCurlRollTemp extends BaseEntity {

    @ApiModelProperty(value = "内衬代码")
    @Excel(name = "ui.data.column.quota.liningCode")
    private String liningCode;

    @ApiModelProperty(value = "卷曲长度。次内衬一卷的最大长度，单位：米。")
    @Excel(name = "ui.curlRoll.column.length")
    private BigDecimal curlLength;

    @ApiModelProperty(value = "备注", position = 500)
    @Excel(name = "ui.common.column.remark")
    private String remark;
}
