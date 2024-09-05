package com.zlt.aps.tc.api.domain.dto;

import java.math.BigDecimal;

import com.ruoyi.common.core.annotation.Excel;
import com.zlt.aps.common.core.annotation.ImportValidated;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
@ApiModel(value = "胎侧卷曲信息维护导入模板", description = "胎侧卷曲信息维护导入模板")
public class TcCurlRollDto {
	
	private Long id;

    @ApiModelProperty(value = "胎侧代码")
    @Excel(name = "ui.data.column.quota.sidewallCode")
    @ImportValidated(name = "ui.data.column.quota.sidewallCode", required = true, isCode = true, maxLength = 30)
    private String sidewallCode;

    @ApiModelProperty(value = "卷曲长度。次胎侧一卷的最大长度，单位：米。")
    @Excel(name = "ui.curlRoll.column.length")
    @ImportValidated(name = "ui.curlRoll.column.length", required = true)
    private String curlLength;

    @ApiModelProperty(value = "备注", position = 500)
    @Excel(name = "ui.common.column.remark")
    @ImportValidated(name = "ui.common.column.remark", maxLength = 300)
    private String remark;
}
